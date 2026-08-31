import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * AST 改名器（非文本暴力替换）：用 JDK 自带 javac Tree API 做符号解析，
 * 只重写"解析到目标元素"的标识符位置——被局部变量遮蔽的同名标识符不会误伤；
 * 注释里的旧名按词边界同步更新（注释不影响语义，另有 RENAME_MAP 对照）。
 *
 * 用法:
 *   java Renamer.java apply  <srcRoot> <map.tsv> [--dry-run]
 *   java Renamer.java check  <srcRoot> <map.tsv>     # 校验旧名在代码标识符里绝迹
 *
 * map.tsv（Tab 分隔 5 列，# 注释）:
 *   F  <ownerClass>  <oldName>  -             <newName>   # 字段
 *   M  <ownerClass>  <oldName>  (T1,T2,...)  <newName>   # 方法；重载按参数区分，
 *                                                       参数写简单名，如 (Graphics,I,I)
 *
 * apply 后必跑校验链: ./gradlew classes → tools/regress.sh
 */
public final class Renamer {
    record Entry(String kind, String owner, String oldName, String params, String newName) {
    }

    record Edit(int start, int end, String newName) {
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        String srcRoot = args[1];
        String mapFile = args[2];
        boolean dryRun = args.length > 3 && args[3].equals("--dry-run");
        List<Entry> map = readMap(mapFile);
        checkCollisions(map);

        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(Paths.get(srcRoot))) {
            walk.filter(p -> p.toString().endsWith(".java")).sorted().forEach(files::add);
        }

        if (mode.equals("apply")) {
            apply(files, map, dryRun);
        } else if (mode.equals("check")) {
            checkGone(files, map);
        } else {
            System.err.println("未知模式: " + mode);
            System.exit(2);
        }
    }

    private static void apply(List<Path> files, List<Entry> map, boolean dryRun) throws IOException {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        StandardJavaFileManager fm = jc.getStandardFileManager(diag, null, null);
        JavacTask task = (JavacTask) jc.getTask(null, fm, diag, List.of("-proc:none"), null,
            fm.getJavaFileObjects(files.toArray(new Path[0])));
        Iterable<? extends CompilationUnitTree> units = task.parse();
        task.analyze();     // 归因：标识符 → 元素
        boolean err = false;
        for (Diagnostic<? extends JavaFileObject> d : diag.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println("[javac] " + d);
                err = true;
            }
        }
        if (err) {
            System.exit(1);
        }
        Trees trees = Trees.instance(task);
        SourcePositions pos = trees.getSourcePositions();

        // 定位目标元素（声明处）：字段精确匹配；方法按 名字+参数表 消歧
        Map<Element, Entry> targetEls = new IdentityHashMap<>();
        for (Entry e : map) {
            Element owner = task.getElements().getTypeElement(e.owner());
            if (owner == null) {
                System.err.println("[map] owner not found: " + e.owner());
                System.exit(1);
            }
            List<Element> matched = new ArrayList<>();
            for (Element m : owner.getEnclosedElements()) {
                if (!m.getSimpleName().contentEquals(e.oldName())) {
                    continue;
                }
                if (e.kind().equals("F") && m instanceof VariableElement) {
                    matched.add(m);
                }
                if (e.kind().equals("M") && m instanceof ExecutableElement ex && paramsMatch(ex, e.params())) {
                    matched.add(m);
                }
            }
            if (matched.size() != 1) {
                System.err.println("[map] " + e.kind() + " " + e.owner() + "." + e.oldName()
                    + e.params() + " matched " + matched.size());
                System.exit(1);
            }
            targetEls.put(matched.get(0), e);
        }

        for (CompilationUnitTree cu : units) {
            Path path = Paths.get(cu.getSourceFile().toUri());
            String text = Files.readString(path, StandardCharsets.UTF_8);
            List<Edit> edits = new ArrayList<>();

            new TreePathScanner<Void, Void>() {
                @Override public Void visitIdentifier(IdentifierTree t, Void __) {
                    Element e = trees.getElement(getCurrentPath());
                    Entry hit = e == null ? null : targetEls.get(e);
                    if (hit != null) {
                        addTokenEdit(hit, pos.getStartPosition(cu, t),
                            pos.getEndPosition(cu, t));
                    }
                    return null;
                }

                @Override public Void visitMemberSelect(MemberSelectTree t, Void __) {
                    Element e = trees.getElement(getCurrentPath());
                    Entry hit = e == null ? null : targetEls.get(e);
                    if (hit != null) {
                        int e2 = (int) pos.getEndPosition(cu, t);
                        addTokenEdit(hit, e2 - t.getIdentifier().length(), e2);
                    }
                    return super.visitMemberSelect(t, __);
                }

                @Override public Void visitMethod(MethodTree t, Void __) {
                    Element e = trees.getElement(getCurrentPath());
                    Entry hit = e == null ? null : targetEls.get(e);
                    if (hit != null) {
                        addDeclEdit(hit, pos.getStartPosition(cu, t), pos.getEndPosition(cu, t));
                    }
                    return super.visitMethod(t, __);
                }

                @Override public Void visitVariable(VariableTree t, Void __) {
                    Element e = trees.getElement(getCurrentPath());
                    Entry hit = e == null ? null : targetEls.get(e);
                    if (hit != null) {
                        addDeclEdit(hit, pos.getStartPosition(cu, t), pos.getEndPosition(cu, t));
                    }
                    return super.visitVariable(t, __);
                }

                private void addTokenEdit(Entry hit, long s, long e2) {
                    if (s >= 0 && e2 > s) {
                        edits.add(new Edit((int) s, (int) e2, hit.newName()));
                    }
                }

                /** 声明名改名：只在"头"里找名字——方法头到第一个 ( 之前，字段头到
                 *  = 或 ; 之前。全 span 搜索会把方法体内的同名调用/注释误当声明名。 */
                private void addDeclEdit(Entry hit, long s, long e2) {
                    if (s < 0 || e2 <= s) {
                        return;
                    }
                    int from = (int) s;
                    int to = (int) Math.min(text.length(), e2);
                    String span = text.substring(from, to);
                    int head = span.length();
                    for (int i = 0; i < span.length(); ++i) {
                        char c = span.charAt(i);
                        if (c == '(' || c == '=' || c == ';') {
                            head = i;
                            break;
                        }
                    }
                    Pattern p = Pattern.compile("\\b" + Pattern.quote(hit.oldName()) + "\\b");
                    java.util.regex.Matcher m = p.matcher(span.substring(0, head));
                    int last = -1;
                    while (m.find()) {
                        last = m.start();
                    }
                    if (last >= 0) {
                        edits.add(new Edit(from + last, from + last + hit.oldName().length(),
                            hit.newName()));
                    }
                }
            }.scan(cu, null);

            // 应用代码编辑（从后往前；同名嵌套（如 this.x 的 select+identifier）只取一次）
            edits.sort(Comparator.comparingInt(Edit::start).reversed()
                .thenComparing(Comparator.comparingInt(Edit::end).reversed()));
            StringBuilder sb = new StringBuilder(text);
            int applied = 0;
            int lastStart = Integer.MAX_VALUE;
            for (Edit e : edits) {
                if (e.end() > lastStart) {
                    continue;
                }
                sb.replace(e.start(), e.end(), e.newName());
                lastStart = e.start();
                ++applied;
            }
            // 注释里的旧名：对编辑后的新文本重扫注释 span（词边界，span 词法避开字符串字面量）
            applied += replaceInComments(sb, map);
            if (!dryRun && applied > 0) {
                Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("[apply] " + path + " (" + applied + " edits)" + (dryRun ? " [dry-run]" : ""));
        }
    }

    private static void checkGone(List<Path> files, List<Entry> map) throws IOException {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = jc.getStandardFileManager(null, null, null);
        JavacTask task = (JavacTask) jc.getTask(null, fm, null, List.of("-proc:none"), null,
            fm.getJavaFileObjects(files.toArray(new Path[0])));
        Iterable<? extends CompilationUnitTree> units = task.parse();
        Set<String> oldNames = new HashSet<>();
        map.forEach(e -> oldNames.add(e.oldName()));
        List<String> left = new ArrayList<>();
        for (CompilationUnitTree cu : units) {
            new TreePathScanner<Void, Void>() {
                @Override public Void visitIdentifier(IdentifierTree t, Void __) {
                    if (oldNames.contains(t.getName().toString())) {
                        left.add(cu.getSourceFile().getName() + ": " + t.getName());
                    }
                    return null;
                }
            }.scan(cu, null);
        }
        if (!left.isEmpty()) {
            System.out.println("CHECK FAIL: " + left.size() + " 个旧标识符仍在代码中出现:");
            left.forEach(s -> System.out.println("  " + s));
            System.exit(1);
        }
        System.out.println("CHECK ok: 旧标识符在代码中已绝迹");
    }

    private static boolean paramsMatch(ExecutableElement ex, String params) {
        if (params.equals("*")) {
            return true;
        }
        String want = params.substring(1, params.length() - 1);
        List<String> wantList = want.isEmpty() ? List.of() : List.of(want.split(","));
        List<? extends VariableElement> ps = ex.getParameters();
        if (ps.size() != wantList.size()) {
            return false;
        }
        for (int i = 0; i < ps.size(); ++i) {
            String t = ps.get(i).asType().toString();
            String simple = t.substring(t.lastIndexOf('.') + 1).replaceAll("[<\\[].*", "");
            if (!simple.equals(wantList.get(i).trim())) {
                return false;
            }
        }
        return true;
    }

    /** 注释替换：每轮只替换第一个命中的旧名（替换会移动偏移，故整体重扫）。
     *  命中数有限（几百），O(n^2) 可接受。 */
    private static int replaceInComments(StringBuilder sb, List<Entry> map) {
        Set<String> olds = new TreeSet<>((a, b) -> b.length() - a.length());
        Map<String, String> old2new = new HashMap<>();
        for (Entry e : map) {
            olds.add(e.oldName());
            old2new.putIfAbsent(e.oldName(), e.newName());
        }
        StringBuilder pat = new StringBuilder("\\b(");
        boolean first = true;
        for (String s : olds) {
            if (!first) {
                pat.append('|');
            }
            pat.append(Pattern.quote(s));
            first = false;
        }
        pat.append(")\\b");
        Pattern p = Pattern.compile(pat.toString());
        int count = 0;
        while (true) {
            String text = sb.toString();
            int at = -1, end = -1;
            String nn = null;
            outer:
            for (int[] span : commentSpans(text)) {
                java.util.regex.Matcher m = p.matcher(text);
                m.region(span[0], span[1]);
                while (m.find()) {
                    at = m.start();
                    end = m.end();
                    nn = old2new.get(m.group(1));
                    break outer;
                }
            }
            if (at < 0) {
                return count;
            }
            sb.replace(at, end, nn);
            ++count;
        }
    }

    /** 注释 span 扫描（跳过字符串/字符字面量内的 //、/ **）。 */
    private static List<int[]> commentSpans(String text) {
        List<int[]> spans = new ArrayList<>();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                char q = c;
                ++i;
                while (i < n) {
                    if (text.charAt(i) == '\\') {
                        i += 2;
                        continue;
                    }
                    if (text.charAt(i) == q) {
                        ++i;
                        break;
                    }
                    ++i;
                }
            } else if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                int s = i;
                while (i < n && text.charAt(i) != '\n') {
                    ++i;
                }
                spans.add(new int[]{s, i});
            } else if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                int s = i;
                i += 2;
                while (i + 1 < n && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    ++i;
                }
                i = Math.min(n, i + 2);
                spans.add(new int[]{s, i});
            } else {
                ++i;
            }
        }
        return spans;
    }

    private static List<Entry> readMap(String mapFile) throws IOException {
        List<Entry> map = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(mapFile), StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] p = line.split("\t");
            if (p.length != 5) {
                System.err.println("[map] 格式错误（需 5 列 Tab 分隔）: " + line);
                System.exit(2);
            }
            map.add(new Entry(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim()));
        }
        if (map.isEmpty()) {
            System.err.println("[map] 空映射");
            System.exit(2);
        }
        return map;
    }

    private static void checkCollisions(List<Entry> map) {
        Set<String> seen = new HashSet<>();
        for (Entry e : map) {
            if (!seen.add(e.kind() + "." + e.owner() + "." + e.newName())) {
                System.err.println("[map] 新名冲突: " + e.newName());
                System.exit(2);
            }
        }
    }
}
