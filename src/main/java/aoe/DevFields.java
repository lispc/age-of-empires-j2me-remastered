package aoe;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * dev 验证基建：把游戏对象图里"AgeOfEmpires.* 声明的字段"（含 AgeOfEmpires.payCost/b 的
 * 静态字段）导出成可 diff 的文本。存档工具 SaveState 的完整性就靠它验收：
 * 存→立刻载→两次 dump 逐行 diff，多出的差异行 = 快照漏字段。
 *
 * 数组打印长度 + CRC32（小数组附带全部元素）；跳过非 AgeOfEmpires.* 声明的字段
 * （Canvas/资源加载器那些不是游戏状态）。
 */
public final class DevFields {
    private DevFields() {
    }

    public static void dump(Object instance, Class<?> staticsClass, PrintWriter out) {
        out.println("# DevFields " + (instance == null ? "null" : instance.getClass().getName()));
        if (instance != null) {
            dumpObject(instance, "", out, 0);
        }
        if (staticsClass != null) {
            out.println("# statics " + staticsClass.getName());
            dumpStatics(staticsClass, out);
        }
        out.flush();
    }

    private static void dumpStatics(Class<?> cls, PrintWriter out) {
        Map<String, java.lang.reflect.Field> fields = new TreeMap<>();
        for (Class<?> k = cls; k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    fields.putIfAbsent(f.getName(), f);
                }
            }
        }
        for (Map.Entry<String, java.lang.reflect.Field> e : fields.entrySet()) {
            out.println("static." + e.getKey() + " = " + valueOf(e.getValue(), null, out, 0));
        }
    }

    private static void dumpObject(Object obj, String prefix, PrintWriter out, int depth) {
        Map<String, java.lang.reflect.Field> fields = new TreeMap<>();
        for (Class<?> k = obj.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            if (!k.getName().startsWith("AgeOfEmpires.")) {
                continue;       // 只看游戏类声明的字段
            }
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    fields.putIfAbsent(f.getName(), f);
                }
            }
        }
        for (Map.Entry<String, java.lang.reflect.Field> e : fields.entrySet()) {
            String name = prefix + e.getKey();
            java.lang.reflect.Field f = e.getValue();
            try {
                Object v = f.get(obj);
                if (v != null && "AgeOfEmpires.d".equals(v.getClass().getName()) && depth < 2) {
                    out.println(name + " :");
                    dumpObject(v, name + ".", out, depth + 1);
                } else {
                    out.println(name + " = " + valueOf(f, obj, out, depth));
                }
            } catch (IllegalAccessException ex) {
                out.println(name + " = <" + ex + ">");
            }
        }
    }

    private static String valueOf(java.lang.reflect.Field f, Object obj, PrintWriter out, int depth) {
        try {
            Object v = f.get(obj);
            return render(v, out, depth);
        } catch (IllegalAccessException ex) {
            return "<" + ex + ">";
        }
    }

    private static String render(Object v, PrintWriter out, int depth) {
        if (v == null) {
            return "null";
        }
        Class<?> c = v.getClass();
        if (c == String.class) {
            return "\"" + v + "\"";
        }
        if (v instanceof Number || v instanceof Boolean || v instanceof Character) {
            return String.valueOf(v);       // 装箱基本类型按值打印（f.get 对原始字段返回装箱）
        }
        if (c.isArray()) {
            int len = java.lang.reflect.Array.getLength(v);
            CRC32 crc = new CRC32();
            StringBuilder elems = new StringBuilder();
            boolean show = len <= 16;
            for (int i = 0; i < len; ++i) {
                Object e = java.lang.reflect.Array.get(v, i);
                String s;
                if (e != null && e.getClass().isArray()) {
                    s = render(e, out, depth + 1);      // 嵌套数组：递归取稳定内容
                } else {
                    s = String.valueOf(e);
                }
                crc.update(s.getBytes());
                if (show) {
                    elems.append(' ').append(s);
                }
            }
            return c.getSimpleName() + "[" + len + "] crc=" + Long.toHexString(crc.getValue())
                + (show ? elems.toString() : "");
        }
        return c.getName() + "@" + Integer.toHexString(System.identityHashCode(v));
    }
}
