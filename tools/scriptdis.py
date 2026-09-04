#!/usr/bin/env python3
"""scriptdis.py — data.res 战役任务脚本反汇编器（只读，纯标准库）。

考证来源 = src/main/java/AgeOfEmpires/c.java 解释器本体（逐条对照指针推进）：
    tickMissionScript()  @ ~8648   块循环；首字节<0 = 已禁用块（运行时翻转，文件里原始为正）
    evalScriptCondition()@ ~8671   条件 opcode 1..7（参数数见 COND_PARAMS）
    runScriptActions()   @ ~8771   动作 opcode 0..9 + 126=块结束
    skipScriptBlock()    @ ~8893 / skipScriptActions() @ ~8926  参数长度仲裁依据
    spawnMission()       @ ~5846   初始布阵资源格式（op1 建筑 / op2 单位 / op3 资源格）
    com/ulysseo/mad/c.java         data.res 打包格式

脚本结构：块* 127。块 = 条件 + 动作* 126。块首字节 = 条件 opcode（运行时被翻负 = 禁用）。
条件为真 → 执行动作；块不会自动一次性，需动作 2(-1) 自我禁用——否则每 tick 重复触发。

用法：
    python3 tools/scriptdis.py list            # 全部资源号+长度
    python3 tools/scriptdis.py dis <resId>     # 反汇编任务脚本（110..116 战役 / 124..126 教学）
    python3 tools/scriptdis.py dump <resId>    # hex dump 兜底
    python3 tools/scriptdis.py spawn <resId>   # 解码初始布阵资源（103..109 / 118..120）
"""
import struct
import sys

RES_PATH = "src/main/resources/res/data.res"

# ---- 名称表（docs/unit-stats.md，res#66/69 名字表）----
UNIT_NAMES = {
    0: "村民t0", 1: "村民t1", 2: "长枪兵", 3: "剑士", 4: "弓兵", 5: "侦察骑兵",
    6: "骑兵", 7: "冲车", 8: "投石机", 9: "征服者",
}
BUILDING_NAMES = {
    0: "伐木场", 1: "采矿场", 2: "攻城工坊", 3: "城堡", 4: "大学", 5: "磨坊",
    6: "铁匠铺", 7: "射箭场", 8: "马厩", 9: "城镇中心", 10: "兵营", 11: "房屋",
    12: "哨塔", 13: "瞭望塔", 14: "护卫塔", 15: "要塞塔",
}
HEADER_NAMES = {
    0: "时代", 2: "单位数", 3: "人口上限", 4: "建筑数",
    5: "木", 6: "金", 7: "石", 8: "TC交存点位置",
    12: "索敌半径²", 45: "塔护甲", 46: "塔攻击", 47: "塔级别", 49: "在训队列",
}
EVENT_KINDS = {0: "单位死亡", 1: "建筑被毁", 2: "单位训练完成", 3: "建筑建成"}
RES_KIND_NAMES = {0: "?(0)", 1: "木", 2: "金", 3: "石"}

# opcode -> 参数字节数。仲裁依据：
#   条件 = evalScriptCondition 的指针推进（enabled 路径权威）；
#   动作 = runScriptActions / skipScriptActions。
# 注意 cond op6：skipScriptBlock 跳过 3 参数字节，但 evalScriptCondition 只读 2 个
# （CFR/Vineflower 双证实）——res114 按 2 参数解析完美对齐 127，按 3 参数错位。
# 运行时能容忍是因为 skipScriptActions 的 switch 无 default，未知字节逐字节滑过自愈；
# 但块被启用后动作指针由 eval 决定，故权威参数数 = 2。
COND_PARAMS = {1: 7, 2: 2, 3: 1, 4: 5, 5: 3, 6: 2, 7: 4}
ACT_PARAMS = {0: 2, 1: 1, 2: 1, 3: 1, 4: 1, 5: 1, 6: 4, 7: 4, 8: 3, 9: 2}


def s8(b):
    return b - 256 if b >= 128 else b


# ---- data.res 取址（com.ulysseo.mad.c：头 int32BE=索引区大小，其后 int32BE 绝对偏移表，
#      每条记录 = int32BE 长度 + 载荷）----
def load_resources(path=RES_PATH):
    with open(path, "rb") as f:
        blob = f.read()
    # 头 int32BE = 余下整个文件的大小（索引区+数据区），不是索引区大小；
    # 资源条数由第 0 项的绝对偏移反推（docs/game-mechanics.md「134 个资源」同值）。
    (first_off,) = struct.unpack_from(">i", blob, 4)
    count = (first_off - 4) // 4
    res = []
    for i in range(count):
        (off,) = struct.unpack_from(">i", blob, 4 + i * 4)
        (length,) = struct.unpack_from(">i", blob, off)
        res.append(blob[off + 4: off + 4 + length])
    return res


# ---- 任务脚本反汇编 ----
def fmt_cond(op, p):
    if op == 1:
        neg, pl, slot, x0, y0, w, h = p
        s = "p%d 单位#%d 静止位于 x∈[%d,%d) y∈[%d,%d)" % (pl, slot, x0, x0 + w, y0, y0 + h)
        return ("NOT(%s)" % s) if neg == 1 else s
    if op == 2:
        return "计数器c%d ≥ %d tick" % (s8(p[0]), p[1] * 10)
    if op == 3:
        return "screenState == %d" % s8(p[0])
    if op == 4:
        neg, pl, mark_hi, typ, slot = p
        t = "任意" if s8(typ) == -1 else UNIT_NAMES.get(typ, "type%d" % typ)
        s = "选中: p%d slot#%d mark=0x%02X00 类型=%s" % (pl, slot, mark_hi, t)
        return ("NOT(%s)" % s) if neg == 1 else s
    if op == 5:
        pl, kind, typ = p
        ts = s8(typ)
        # 事件第三值的命名空间随 kind 走（c.java 调用处：6840/7060/7373/7473）：
        # kind 0=单位死亡(类型) 1=建筑被毁(建筑类型) 2=单位训练完成(类型) 3=建筑建成(建筑类型)
        building_ns = kind in (1, 3)
        if ts == -1:
            tname = "任意类型"
        elif ts == 0:
            tname = "建筑类型<2(伐木场/采矿场)" if building_ns else "村民(类型<2)"
        else:
            tname = (BUILDING_NAMES if building_ns else UNIT_NAMES).get(ts, "type%d" % ts)
        return "事件: p%d %s(%s)" % (pl, EVENT_KINDS.get(kind, "kind%d" % kind), tname)
    if op == 6:
        return "techFlags[%d] == %d" % (p[0], p[1])
    if op == 7:
        pl, idx, cmpop, val = p
        hname = HEADER_NAMES.get(idx, "hdr[%d]" % idx)
        return "p%d %s %s %d" % (pl, hname, {0: "==", 1: ">", 2: "<"}.get(cmpop, "?%d" % cmpop), val)
    return "条件op%d %s" % (op, p)


def fmt_act(op, p):
    if op == 0:
        return "简报弹窗 briefing(as=%d, res=本关I, 页=%d)" % (p[0], p[1])
    if op == 1:
        return "启用块 #%d" % p[0]
    if op == 2:
        v = s8(p[0])
        return "禁用块 #%d" % v if v >= 0 else "禁用本块(一次性)"
    if op == 3:
        return "计数器c%d = 0" % p[0]
    if op == 4:
        return "★胜利 (briefing 98,0; 参数字节=%d 被解释器丢弃)" % p[0]
    if op == 5:
        return "★失败 (briefing 98,1; 参数字节=%d 被解释器丢弃)" % p[0]
    if op == 6:
        pl, typ, x, y = p
        return "刷兵: p%d %s @(%d,%d)" % (pl, UNIT_NAMES.get(typ, "type%d" % typ), x, y)
    if op == 7:
        pl, sel, x, y = p
        s = s8(sel)
        if s == -1:
            who = "全体村民(类型<2)"
        elif s < 0:
            who = "所有%s(类型%d)" % (UNIT_NAMES.get(-s, "?"), -s)
        else:
            who = "单位slot#%d(原码此分支传脚本指针给 tickMoveTimer，疑似原版 bug)" % s
        return "移动令: p%d %s → (%d,%d)" % (pl, who, x, y)
    if op == 8:
        pl, idx, val = p
        who = "双方" if s8(pl) == -1 else "p%d" % pl
        return "写头: %s %s = %d" % (who, HEADER_NAMES.get(idx, "hdr[%d]" % idx), s8(val))
    if op == 9:
        return "techFlags[%d] = %d" % (p[0], p[1])
    return "动作op%d %s" % (op, p)


class ScriptError(Exception):
    pass


def disassemble(data):
    """返回 (blocks, end_ok, warn)。blocks = [(offset, disabled, cond, [actions])]，
    cond/actions = (offset, opcode, params, text)。127 必须是最后一个字节。"""
    blocks = []
    pos = 0
    n = len(data)
    while True:
        if pos >= n:
            raise ScriptError("脚本耗尽而未遇 127 结束标记 (pos=%d)" % pos)
        if data[pos] == 127:
            end_ok = (pos == n - 1)
            return blocks, end_ok, None if end_ok else "127 后残留 %d 字节" % (n - 1 - pos)
        start = pos
        op = data[pos]
        pos += 1
        disabled = op >= 128
        if disabled:
            op = 256 - op  # 运行时符号翻转: 存储值 = -opcode（模 256）
        if op not in COND_PARAMS:
            raise ScriptError("块 @%d: 未知条件 opcode %d" % (start, op))
        np = COND_PARAMS[op]
        if pos + np > n:
            raise ScriptError("块 @%d: 条件参数越界" % start)
        params = list(data[pos:pos + np])
        pos += np
        cond = (start, op, params, fmt_cond(op, params))
        actions = []
        while True:
            if pos >= n:
                raise ScriptError("块 @%d: 动作耗尽而未遇 126" % start)
            aoff = pos
            aop = data[pos]
            pos += 1
            if aop == 126:
                break
            if aop == 127:
                raise ScriptError("块 @%d: 动作区遇 127（缺 126）" % start)
            if aop not in ACT_PARAMS:
                raise ScriptError("块 @%d: 未知动作 opcode %d @%d" % (start, aop, aoff))
            np = ACT_PARAMS[aop]
            if pos + np > n:
                raise ScriptError("块 @%d: 动作参数越界" % start)
            aparams = list(data[pos:pos + np])
            pos += np
            actions.append((aoff, aop, aparams, fmt_act(aop, aparams)))
        blocks.append((start, disabled, cond, actions))


def cmd_dis(res, res_id):
    data = res[res_id]
    print("资源 #%d: %d 字节" % (res_id, len(data)))
    blocks, end_ok, warn = disassemble(data)
    for i, (start, disabled, cond, actions) in enumerate(blocks):
        mark = " [文件内即禁用]" if disabled else ""
        print("块#%d @%d (0x%02X)%s" % (i, start, start, mark))
        print("  条件 @%d: op%d %s" % (cond[0], cond[1], cond[3]))
        for aoff, aop, aparams, text in actions:
            print("  动作 @%d: op%d %s" % (aoff, aop, text))
    if end_ok:
        print("✓ 127 结束标记校验通过（%d 块，无残留字节）" % len(blocks))
        return 0
    print("✗ %s" % warn)
    return 1


def cmd_dump(res, res_id):
    data = res[res_id]
    print("资源 #%d: %d 字节" % (res_id, len(data)))
    for base in range(0, len(data), 16):
        chunk = data[base:base + 16]
        hexs = " ".join("%02x" % b for b in chunk)
        asc = "".join(chr(b) if 32 <= b < 127 else "." for b in chunk)
        print("%04x  %-47s  %s" % (base, hexs, asc))
    return 0


# ---- spawnMission 初始布阵解码（c.java:5846）----
def cmd_spawn(res, res_id):
    d = res[res_id]
    if len(d) < 13:
        print("资源 #%d: 仅 %d 字节，不足布阵头(13)" % (res_id, len(d)))
        return 1
    seed_lo, seed_hi, age = d[0], d[1], d[2]
    print("资源 #%d: %d 字节" % (res_id, len(d)))
    if seed_lo | seed_hi:
        print("RNG 种子: lo=%d hi=%d" % (seed_lo, seed_hi))
    else:
        print("RNG 种子: (0,0) → randomMap=true（随机图，非战役布阵）")
    print("玩家时代 hdr[0][0] = %d" % age)
    print("起始资源 p0: 木%d 金%d 石%d | p1: 木%d 金%d 石%d"
          % (d[3], d[4], d[5], d[6], d[7], d[8]))
    print("光标: (%d,%d)" % (d[9], d[10]))
    count = d[11] | (d[12] << 8)  # 小端
    print("布阵记录数: %d" % count)
    pos = 13
    for i in range(count):
        if pos >= len(d):
            print("✗ 记录 #%d 越界" % i)
            return 1
        op = d[pos]
        pos += 1
        if op == 1:  # 建筑: type, player, x, y, flags → a(player,type,x,y,flags,false)
            typ, pl, x, y, flags = d[pos:pos + 5]
            pos += 5
            print("  #%d 建筑: p%d %s(%d) @(%d,%d) flags=0x%02X"
                  % (i, pl, BUILDING_NAMES.get(typ, "?"), typ, x, y, flags))
        elif op == 2:  # 单位: type, player, x, y → a(player,type,x,y,false)
            typ, pl, x, y = d[pos:pos + 4]
            pos += 4
            print("  #%d 单位: p%d %s(%d) @(%d,%d)"
                  % (i, pl, UNIT_NAMES.get(typ, "?"), typ, x, y))
        elif op == 3:  # 资源格: tileByte, x, y → void_a(tile|0x300, x, y, 31)
            tile, x, y = d[pos:pos + 3]
            pos += 3
            print("  #%d 资源格: %s @(%d,%d) (31 趟满载, 原始字节=0x%02X)"
                  % (i, RES_KIND_NAMES.get(tile & 3, "?"), x, y, tile))
        else:
            # spawnMission 的 switch 只有 case 1/2/3 且无 default（CFR/VF 双证实）：
            # 未知 op 在运行时 = 1 字节 no-op。res104 尾部即此形态（op0 + 4 字节死数据）。
            print("  #%d op%d: 运行时 no-op(1 字节, 原码 switch 无此 case)" % (i, op))
    if pos != len(d):
        print("  (尾部 %d 字节永不被运行时消费: %s)"
              % (len(d) - pos, " ".join("%02x" % b for b in d[pos:])))
    print("✓ %d 条记录全部扫描完" % count)
    return 0


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    res = load_resources()
    cmd = argv[1]
    if cmd == "list":
        print("data.res: %d 个资源" % len(res))
        for i, r in enumerate(res):
            print("  #%3d: %d 字节" % (i, len(r)))
        return 0
    if cmd in ("dis", "dump", "spawn"):
        if len(argv) < 3:
            print("缺 resId")
            return 2
        res_id = int(argv[2])
        if not (0 <= res_id < len(res)):
            print("resId 越界 (0..%d)" % (len(res) - 1))
            return 2
        try:
            if cmd == "dis":
                return cmd_dis(res, res_id)
            if cmd == "dump":
                return cmd_dump(res, res_id)
            return cmd_spawn(res, res_id)
        except ScriptError as e:
            print("✗ 反汇编失败: %s" % e)
            return 1
    print("未知命令: %s" % cmd)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
