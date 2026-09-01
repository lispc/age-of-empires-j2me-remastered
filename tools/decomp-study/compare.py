#!/usr/bin/env python3
"""decompile→recompile→归一化字节码对拍
比较 dumps/orig vs dumps/cfr vs dumps/vf (javap -c -p) 与 dumps/*-sig (javap -p -s)。

分级:
  L1 exact   : 规范化操作数后指令序列完全一致(字段/方法引用按 描述符+声明类内序号 规范化 → 重命名不变量)
  L2 mnem    : 槽位族归一化后的助记符序列一致(布局/槽位噪声)
  L3 idiom   : 老/新 javac 惯用法归一化(if+goto 反演、dup;store 展开、布尔比较、iinc 折叠、
               switch 仅比匹配键、iinc 只比增量)后一致(编译器代差噪声)
  L4 MISMATCH: 以上都不一致 → 真实不一致,需人工裁决
"""
import re, json, itertools

ROOT = '/tmp/decomp-study'
CLASSES = ['AgeOfEmpires/a', 'AgeOfEmpires/AgeOfEmpires', 'AgeOfEmpires/b', 'AgeOfEmpires/c',
           'AgeOfEmpires/d', 'com/ulysseo/mad/a', 'com/ulysseo/mad/b', 'com/ulysseo/mad/c',
           'com/ulysseo/mad/d', 'com/ulysseo/mad/e']
SIDES = ['cfr', 'vf']

SUPER = {
    'AgeOfEmpires/c': ['com/ulysseo/mad/a'],
}
INVERT = {'ifeq':'ifne','ifne':'ifeq','iflt':'ifge','ifge':'iflt','ifgt':'ifle','ifle':'ifgt',
          'if_icmpeq':'if_icmpne','if_icmpne':'if_icmpeq','if_icmplt':'if_icmpge','if_icmpge':'if_icmplt',
          'if_icmpgt':'if_icmple','if_icmple':'if_icmpgt','if_acmpeq':'if_acmpne','if_acmpne':'if_acmpeq',
          'ifnull':'ifnonnull','ifnonnull':'ifnull'}
SLOT_RE = re.compile(r'^(i|a|l|f|d)(load|store)(?:_(\d))?$')
SLOTARG_RE = re.compile(r'^(?:i|a|l|f|d)(?:load|store)(?:_(\d)|\s+(\d+))$')

def normmnem(m):
    m = m.replace('iinc_w', 'iinc')
    mm = SLOT_RE.match(m)
    return (mm.group(1) + mm.group(2)) if mm else m

def slot_of(mn, op=''):
    # javap: 槽位<4 记入助记符(istore_2); >=4 记为操作数(istore 9)
    m = re.match(r'^(?:i|a|l|f|d)(?:load|store)_(\d)$', mn)
    if m:
        return m.group(1)
    if re.match(r'^(?:i|a|l|f|d)(?:load|store)$', mn):
        return op or None
    return None

def const_of(mn, op):
    if mn == 'iconst_m1': return '-1'
    if mn.startswith('iconst_'): return mn[len('iconst_'):]
    if mn in ('bipush', 'sipush'): return op
    return None

# ---------------- javap -p -s 解析 ----------------
def parse_sig(path):
    fields, methods = [], []
    lines = open(path, encoding='utf-8', errors='replace').read().splitlines()
    cls_simple = None
    m = re.search(r'(?:class|interface|enum) ([\w.$]+)', '\n'.join(lines[:3]))
    if m:
        cls_simple = m.group(1).split('.')[-1].split('/')[-1]
    i = 0
    while i < len(lines):
        s = lines[i].strip()
        if s.endswith(';') and 'descriptor:' not in s and not s.startswith('Compiled') and s and not s.startswith('{') and not s.startswith('}'):
            j = i + 1
            while j < len(lines) and not lines[j].strip().startswith('descriptor:'):
                j += 1
            if j < len(lines):
                desc = lines[j].strip()[len('descriptor:'):].strip()
                body = s[:-1].strip()
                mm = re.match(r'^(?:[\w.<>\[\], ]+?\s)?([A-Za-z_$][\w$]*)\s*(\((?:.*)\))?$', body)
                if mm:
                    name = mm.group(1)
                    if mm.group(2):
                        if name == cls_simple and body.startswith(cls_simple + '('):
                            name = '<init>'
                        methods.append((name, desc))
                    else:
                        fields.append((name, desc))
                i = j + 1
                continue
        i += 1
    return fields, methods

# ---------------- javap -c -p 解析 ----------------
def parse_code(path):
    methods = []
    cur = None
    lines = open(path, encoding='utf-8', errors='replace').read().splitlines()
    i = 0
    while i < len(lines):
        l = lines[i]
        s = l.strip()
        if (s.endswith(';') and '(' in s and 'descriptor:' not in s and
                not re.match(r'^\d+:', s) and not s.startswith('Compiled') and not s.startswith('}')):
            cur = {'sig': s, 'instrs': [], 'exc': []}
            methods.append(cur)
            i += 1
            continue
        if s == 'static {};' or s == 'static {};':
            cur = {'sig': s, 'instrs': [], 'exc': []}
            methods.append(cur)
            i += 1
            continue
        m = re.match(r'^\s*(\d+): (\S+)(.*)$', l)
        if m and cur is not None:
            addr = int(m.group(1)); mnem = m.group(2); rest = m.group(3).strip()
            comment = None
            if '//' in rest:
                rest, comment = rest.split('//', 1)
                rest = rest.strip(); comment = comment.strip()
            if mnem in ('lookupswitch', 'tableswitch'):
                cases = []
                i += 1
                while i < len(lines) and not lines[i].strip().startswith('}'):
                    cm = re.match(r'^(.+?):\s*(-?\d+)$', lines[i].strip())
                    if cm:
                        val = -1 if cm.group(1).strip() == 'default' else int(cm.group(1))
                        cases.append((val, int(cm.group(2))))
                    i += 1
                cur['instrs'].append((addr, mnem, tuple(sorted(cases)), None))
                i += 1
                continue
            cur['instrs'].append((addr, mnem, rest, comment))
            i += 1
            continue
        if s.startswith('Exception table:'):
            i += 1
            while i < len(lines) and lines[i].strip().startswith('Class'):
                cur['exc'].append(lines[i].strip().split()[1])
                i += 1
            continue
        i += 1
    return methods

# ---------------- 引用规范化(重命名不变量) ----------------
class Canon:
    def __init__(self, side):
        self.members = {}
        for c in CLASSES:
            f, m = parse_sig(f'{ROOT}/dumps/{side}-sig/{c}.txt')
            self.members[c] = (f, m)
        self.cur = None

    def resolve_owner(self, owner):
        chain = [owner]
        seen = set()
        while chain:
            cur = chain.pop(0)
            if cur in seen:
                continue
            seen.add(cur)
            if cur in self.members:
                return cur
            chain.extend(SUPER.get(cur, []))
        return None

    def idx_of(self, cls, kind, name, desc):
        fl, ml = self.members[cls]
        for k, (n, d) in enumerate(fl if kind == 'F' else ml):
            if n == name and d == desc:
                return k
        return None

    def canon_ref(self, kind, text):
        mm = re.match(r'^(?:([\w/.]+)\.)?([\w$<>_]+):(.+)$', text)
        if not mm:
            return f'{kind}?{text}'
        owner, name, desc = mm.group(1), mm.group(2), mm.group(3)
        if owner is None:
            owner = self.cur
        # 沿继承链找到真正声明该成员的本项目类
        chain = [owner]
        seen = set()
        while chain:
            cur = chain.pop(0)
            if cur in seen:
                continue
            seen.add(cur)
            if cur in self.members:
                if self.idx_of(cur, kind, name, desc) is not None:
                    return f'{kind}{desc}@{cur}#{self.idx_of(cur, kind, name, desc)}'
                chain.extend(SUPER.get(cur, []))
            else:
                chain.extend(SUPER.get(cur, []))
        # 本项目链上未找到(外部成员或非重命名字段): 保留原名
        return f'{kind}{desc}@{owner}.{name}'

    def canon_comment(self, comment):
        if comment.startswith('Field '):
            return self.canon_ref('F', comment[6:])
        if comment.startswith('InterfaceMethod '):
            return self.canon_ref('M', comment[16:])
        if comment.startswith('Method '):
            return self.canon_ref('M', comment[7:])
        return comment

    def tokens(self, meth):
        out = []
        for (addr, mnem, operand, comment) in meth['instrs']:
            op = operand
            if comment is not None:
                if mnem in ('getstatic','putstatic','getfield','putfield','invokevirtual',
                            'invokespecial','invokestatic','invokeinterface'):
                    op = self.canon_comment(comment)
                else:
                    op = comment
            out.append((mnem, op))
        return out

def norm_sig_types(sig):
    def strip_pkg(t):
        return '.'.join(x.split('.')[-1] for x in t.split('<')) if t else t
    body = sig[:-1] if sig.endswith(';') else sig
    m = re.match(r'^.*?\((.*)\)\s*([\w.<>\[\], ]*)$', body)
    if not m:
        return (None, None)
    params, ret = m.group(1), m.group(2).strip()
    ps = tuple(strip_pkg(x.strip()) for x in params.split(',') if x.strip())
    return (strip_pkg(ret), ps)

# ---------------- 惯用法归一化 ----------------
def idiom_norm(instrs):
    ins = list(instrs)
    for _ in range(60):
        changed = False
        out = []
        i = 0
        while i < len(ins):
            addr, mn, op, cm = ins[i]
            # (a) [dup, Xstore_n] => [Xstore_n, Xload_n]
            if mn == 'dup' and i + 1 < len(ins):
                mn2, op2 = ins[i+1][1], ins[i+1][2]
                sm = re.match(r'^(a|i)store(?:_(\d))?$', mn2)
                if sm:
                    k = sm.group(1)
                    slot = sm.group(2) if sm.group(2) is not None else op2
                    if slot != '' and int(slot) >= 0:
                        short = int(slot) < 4
                        out.append((addr, f'{k}store_{slot}' if short else f'{k}store', '' if short else slot, cm))
                        out.append((addr + 1, f'{k}load_{slot}' if short else f'{k}load', '' if short else slot, None))
                        i += 2
                        changed = True
                        continue
            # (b) [ifXX -> 下一条 goto] => [!ifXX -> goto 目标]
            if mn in INVERT and i + 1 < len(ins) and ins[i+1][1] == 'goto':
                try:
                    tgt = int(op); gtgt = int(ins[i+1][2])
                except ValueError:
                    tgt = gtgt = None
                if tgt is not None and tgt == ins[i+1][0] + 3 and gtgt is not None:
                    out.append((addr, INVERT[mn], str(gtgt), cm))
                    i += 2
                    changed = True
                    continue
            # (c) [goto -> 下一条] 删除
            if mn == 'goto':
                try:
                    tgt = int(op)
                except ValueError:
                    tgt = None
                if tgt is not None and tgt == addr + 3:
                    i += 1
                    changed = True
                    continue
            # (d) [iconst_0/1, if_icmpeq/ne] => [ifeq/ifne]
            if mn in ('iconst_0', 'iconst_1') and i + 1 < len(ins):
                mn2, op2 = ins[i+1][1], ins[i+1][2]
                try:
                    tgt2 = int(op2)
                except ValueError:
                    tgt2 = None
                if mn2 in ('if_icmpeq', 'if_icmpne') and tgt2 is not None:
                    val1 = (mn == 'iconst_1')
                    eq = (mn2 == 'if_icmpeq')
                    newmn = 'ifne' if (val1 and eq) or ((not val1) and (not eq)) else 'ifeq'
                    out.append((addr, newmn, str(tgt2), None))
                    i += 2
                    changed = True
                    continue
            # (e) [xload_n, push c, iadd/isub, xstore_n] => [iinc n, ±c]
            if i + 3 < len(ins) and mn.startswith(('iload', 'lload')) and slot_of(mn, op) is not None:
                m2, m3, m4 = ins[i+1], ins[i+2], ins[i+3]
                cv = const_of(m2[1], m2[2])
                if cv is not None and m3[1] in ('iadd', 'isub') and m4[1].startswith(('istore', 'lstore')) and slot_of(m4[1], m4[2]) is not None:
                    if slot_of(mn, op) == slot_of(m4[1], m4[2]):
                        delta = int(cv) * (1 if m3[1] == 'iadd' else -1)
                        out.append((addr, 'iinc', f'{slot_of(mn, op)}, {delta}', None))
                        i += 4
                        changed = True
                        continue
            out.append(ins[i])
            i += 1
        ins = out
        if not changed:
            break
    return ins

def l2_keys(ins):
    return [normmnem(x[1]) for x in ins]

def l3_keys(ins):
    keys = []
    for (addr, mn, op, cm) in ins:
        mn2 = normmnem(mn)
        if mn2 == 'iinc':
            parts = str(op).split(',')
            keys.append(('iinc', parts[1].strip() if len(parts) > 1 else '?'))
        elif mn2 in ('lookupswitch', 'tableswitch'):
            keys.append((mn2, tuple(k for k, _ in op)))
        elif mn2 == 'invokeinterface':
            keys.append(('invokevirtual',))  # 接口 vs 类调用: shim 中 Player 为 class, 视为等价
        else:
            keys.append((mn2,))
    return keys

def first_diff(a, b):
    for k, (x, y) in enumerate(itertools.zip_longest(a, b)):
        if x != y:
            return k, x, y
    return None

def excerpt(meth, pos, n=5):
    ins = meth['instrs']
    if not ins:
        return ''
    lo = max(0, pos - n); hi = min(len(ins), pos + n)
    out = []
    for k in range(lo, hi):
        addr, mn, op, cm = ins[k]
        line = f'  {addr}: {mn} {op}' + (f'  // {cm}' if cm else '')
        out.append(('  >> ' if k == pos else '     ') + line)
    return '\n'.join(out)

def main():
    canon = {s: Canon(s) for s in ['orig'] + SIDES}
    parsed = {}
    for c in CLASSES:
        parsed[c] = {s: parse_code(f'{ROOT}/dumps/{s}/{c}.txt') for s in ['orig'] + SIDES}

    result, anomalies = {}, []
    for c in CLASSES:
        for s in ['orig'] + SIDES:
            canon[s].cur = c
        lists = parsed[c]
        n0 = len(lists['orig'])
        cres = {'counts': {'methods': n0}}
        for s in SIDES:
            f0, m0 = canon['orig'].members[c]
            f1, m1 = canon[s].members[c]
            if [d for _, d in f0] != [d for _, d in f1]:
                anomalies.append((c, f'field desc order differs ({s})'))
            if [d for _, d in m0] != [d for _, d in m1]:
                anomalies.append((c, f'method desc order differs ({s})'))
        for s in SIDES:
            stats = {'exact': 0, 'mnem': 0, 'idiom': 0, 'mismatch': 0}
            details = []
            for idx in range(min(n0, len(lists[s]))):
                mo, m1 = lists['orig'][idx], lists[s][idx]
                pair_ok = norm_sig_types(mo['sig']) == norm_sig_types(m1['sig'])
                if not pair_ok:
                    anomalies.append((c, f'pair mismatch idx={idx}', mo['sig'], m1['sig']))
                t0 = canon['orig'].tokens(mo)
                t1 = canon[s].tokens(m1)
                if t0 == t1:
                    stats['exact'] += 1
                    details.append({'idx': idx, 'sig': mo['sig'], 'pair_ok': pair_ok, 'bucket': 'exact'})
                    continue
                d = {'idx': idx, 'sig': mo['sig'], 'pair_ok': pair_ok,
                     'sig_engine': m1['sig'], 'bucket': None,
                     'first_diff': None, 'excerpt_orig': None, 'excerpt_engine': None}
                i0 = idiom_norm(mo['instrs'])
                i1 = idiom_norm(m1['instrs'])
                k2_0, k2_1 = l2_keys(mo['instrs']), l2_keys(m1['instrs'])
                fd = None
                if k2_0 == k2_1:
                    bucket = 'mnem'; stats['mnem'] += 1
                    fd0 = first_diff(t0, t1)
                    if fd0:
                        fd = {'pos': fd0[0], 'orig': str(fd0[1]), s: str(fd0[2]), 'kind': 'L1'}
                elif l3_keys(i0) == l3_keys(i1):
                    bucket = 'idiom'; stats['idiom'] += 1
                    fd0 = first_diff(k2_0, k2_1)
                    if fd0:
                        fd = {'pos': fd0[0], 'orig': str(fd0[1]), s: str(fd0[2]), 'kind': 'L2'}
                else:
                    bucket = 'MISMATCH'; stats['mismatch'] += 1
                    k3_0, k3_1 = l3_keys(i0), l3_keys(i1)
                    fdm = first_diff(k3_0, k3_1)
                    if fdm:
                        pos = fdm[0]
                        fd = {'pos': pos, 'kind': 'L3',
                              'orig': str(k3_0[pos]) if pos < len(k3_0) else f'END(len{len(k3_0)})',
                              s: str(k3_1[pos]) if pos < len(k3_1) else f'END(len{len(k3_1)})'}
                    else:
                        fd = {'pos': -1, 'note': 'length differs', 'orig': str(len(k3_0)), s: str(len(k3_1))}
                d['bucket'] = bucket
                d['first_diff'] = fd
                if fd and fd.get('pos') is not None and fd['pos'] >= 0:
                    pos = fd['pos']
                    d['excerpt_orig'] = excerpt(mo, min(pos, len(mo['instrs']) - 1))
                    d['excerpt_engine'] = excerpt(m1, min(pos, len(m1['instrs']) - 1))
                details.append(d)
            cres[s] = details
            cres.setdefault('stats', {})[s] = stats
        result[c] = cres

    with open(f'{ROOT}/compare-results.json', 'w') as f:
        json.dump({'result': result, 'anomalies': anomalies}, f, indent=1, ensure_ascii=False)

    print(f"{'class':28s} {'engine':6s} {'methods':>7s} {'exact':>6s} {'mnem':>5s} {'idiom':>6s} {'MISMATCH':>9s}")
    tot = {s: dict.fromkeys(['methods','exact','mnem','idiom','mismatch'], 0) for s in SIDES}
    for c in CLASSES:
        for s in SIDES:
            st = result[c]['stats'][s]
            for k in tot[s]:
                tot[s][k] += st[k] if k != 'methods' else result[c]['counts']['methods']
            print(f"{c:28s} {s:6s} {result[c]['counts']['methods']:7d} {st['exact']:6d} {st['mnem']:5d} {st['idiom']:6d} {st['mismatch']:9d}")
    for s in SIDES:
        t = tot[s]
        print(f"{'TOTAL':28s} {s:6s} {t['methods']:7d} {t['exact']:6d} {t['mnem']:5d} {t['idiom']:6d} {t['mismatch']:9d}")
    if anomalies:
        print('\nANOMALIES:')
        for a in anomalies[:30]:
            print(' ', a)
        print('  total:', len(anomalies))
    mism = [(c, s, d) for c in CLASSES for s in SIDES for d in result[c][s] if d['bucket'] == 'MISMATCH']
    print(f"\nTotal MISMATCH: {len(mism)}")
    for c, s, d in mism:
        print(f"  {s:4s} {c} idx={d['idx']} {d['sig']}")

if __name__ == '__main__':
    main()
