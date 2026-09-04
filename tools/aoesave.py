#!/usr/bin/env python3
"""aoesave.py — .aoesave 只读解析库/CLI（r32 整合：play22/25 的 parse.py+probe.py+res.py）。

存档格式（AOE1 魔数，大端）——来源 triage r22-r31 逆向：
    header: magic[4] | (7 字节杂项) | navSpec 串 | gameMode/missionIndex/missionResId
            | randomMap bool | nfoData | campaign/tutorial 进度 | 6×i32
            | mapTiles(4096×i16) | 4×i32数组 | playerCount
    player: headers(i32数组) slots(i16数组,stride8) buildings(i32数组,stride4) secondSlots(i16数组)
    tail:   15×i32 | bool | tick(i32)

用法：
    python3 tools/aoesave.py <save.aoesave>            # 摘要（tick/双方资源人口/单位构成）
    python3 tools/aoesave.py <save> res                # 资源簇列表（雾码可读，含未探索区）
    python3 tools/aoesave.py <save> bld [p]            # p 玩家建筑表（类型/位置/进度）
    python3 tools/aoesave.py <save> units [p]          # p 玩家单位表（类型/位置/任务）
    python3 tools/aoesave.py <save> tile <tx> <ty>     # 单格类型化解码
    python3 tools/aoesave.py <save> json               # 全量 JSON（管道给 jq）
作库：import aoesave; s = aoesave.load(path); s.mt, s.players[p].slots ...
只读工具，绝不写存档。agent 纪律：存档一律 saveDir 内裸文件名，不碰 ~/.aoe-desktop。
"""
import json
import struct
import sys

# ---- mapTiles 类型化解码表（r27-r31 实测钉死，见 docs/symbols.md）----
# 高位标志：0x8000=未探索雾 0x300 类=地表对象 0x4000=?(雾下单位相关)
# 0x83xx：雾下资源（低 2 位 1木 2金 3石；低字节 0x7d/e/f=雾占位符非剩余量）
# 0x85xx：雾下建筑（低字节=建筑 type——雾中信息可读，免侦察地图术）
# 0x0：虚空/废墟  0x1604：建筑废墟(rubble)  -32768(Short.MIN)：未初始化
BUILDING_TYPE_NAMES = {
    1: "Mining Camp", 2: "?(研究?)", 3: "?(贸易?)", 5: "Mill", 6: "Blacksmith",
    7: "Archery Range", 8: "Stable", 9: "TC", 10: "Barracks", 11: "House",
    12: "Tower/Outpost",
}
RESOURCE_NAMES = {0: "-", 1: "木", 2: "金", 3: "石"}
TASK_NAMES = {0: "闲置", 1: "行军", 2: "采集", 3: "回送"}


def decode_tile(v):
    """mapTiles short → 类型化描述 dict。"""
    u = v & 0xFFFF
    d = {"raw": u, "raw_signed": v}
    if v == -32768:
        d["kind"] = "未初始化"
    elif u & 0x8000:
        low = u & 0xFF
        hi = (u >> 8) & 0xFF
        if hi == 0x83 or hi == 0x7d or hi == 0x7e:
            d["kind"] = "雾-资源" if (u & 0x300) == 0x300 or hi == 0x83 else "雾-占位"
            d["res"] = RESOURCE_NAMES.get(u & 3, "?")
        elif hi == 0x85:
            d["kind"] = "雾-建筑"
            d["btype"] = low
            d["bname"] = BUILDING_TYPE_NAMES.get(low, "?")
        else:
            d["kind"] = "雾"
    elif u == 0:
        d["kind"] = "虚空/废墟"
    elif u == 0x1604:
        d["kind"] = "rubble(建筑废墟)"
    else:
        cls = u & 0x300
        if cls == 0x300:
            d["kind"] = "资源"
            d["res"] = RESOURCE_NAMES.get(u & 3, "?")
            d["left"] = (u >> 8) & 0xFF  # 剩余量近似（非种类！r27 判别式）
        elif cls == 0x200:
            d["kind"] = "单位占位"
            d["slot"] = low = u & 0xFF
            d["owner"] = (u >> 10) & 3
        elif cls == 0x100:
            d["kind"] = "建筑占位"
            d["ord"] = u & 0xFF
            d["owner"] = (u >> 10) & 3
        else:
            d["kind"] = "地形"
    return d


class Player:
    def __init__(self, hdr, slots, bld):
        self.hdr = hdr
        self.slots = slots or []
        self.bld = bld or []

    @property
    def age(self):
        return self.hdr[0]

    @property
    def unit_count(self):
        return self.hdr[2]

    @property
    def pop_cap(self):
        return self.hdr[3]

    @property
    def res(self):
        return tuple(self.hdr[5:8])  # 木/金/石

    @property
    def queue(self):
        return self.hdr[49] if len(self.hdr) > 49 else -1

    def units(self):
        """[(slot, tx, ty, type, task, taskword)]，stride8；i<hdr[2] 全活。"""
        out = []
        for i in range(min(self.unit_count, len(self.slots) // 8)):
            s = self.slots[i * 8:(i + 1) * 8]
            out.append({
                "slot": i,
                "tx": (s[0] >> 8) & 0xFF,
                "ty": s[0] & 0xFF,
                "type": s[3] & 0xFF,
                "task": TASK_NAMES.get(s[7] & 0xF, s[7] & 0xF),
                "taskword": s[7] & 0xFFFF,
                "hp": s[4] & 0xFF,
                "tgt": ((s[2] >> 8) & 0xFF, s[2] & 0xFF),
            })
        return out

    def buildings(self):
        """[{tile,type,status}]，stride4 建筑表；0x40000000=施工中 &0xFF=进度 255=完。"""
        out = []
        for i in range(self.hdr[4] if self.hdr else 0):
            e = self.bld[i * 4:(i + 1) * 4]
            if len(e) < 4:
                break
            st = e[2] & 0xFFFFFFFF
            out.append({
                "idx": i,
                "tx": (e[0] >> 8) & 0x3F,
                "ty": e[0] & 0x3F,
                "type": e[3] & 0xFF,
                "name": BUILDING_TYPE_NAMES.get(e[3] & 0xFF, "?"),
                "status": hex(st),
                "building": bool(st & 0x40000000),
                "progress": st & 0xFF,
                "done": (st & 0xFF) == 255,
            })
        return out


class Save:
    def __init__(self, mt, players, tick, game_mode, mission_index):
        self.mt = mt
        self.players = players
        self.tick = tick
        self.game_mode = game_mode
        self.mission_index = mission_index

    def tile(self, tx, ty):
        return decode_tile(self.mt[tx + (ty << 6)])

    def resources(self):
        """全图资源簇 [(kind, [(tx,ty)...])]——雾码也算（免侦察地图术）。"""
        clusters = {1: [], 2: [], 3: []}
        for ty in range(64):
            for tx in range(64):
                d = self.tile(tx, ty)
                if d["kind"] in ("资源", "雾-资源") and d.get("res") in ("木", "金", "石"):
                    key = {"木": 1, "金": 2, "石": 3}[d["res"]]
                    clusters[key].append((tx, ty))
        return clusters


def load(path):
    d = open(path, "rb").read()
    o = 0

    def i32():
        nonlocal o
        v = struct.unpack_from(">i", d, o)[0]
        o += 4
        return v

    def sarr():
        nonlocal o
        n = i32()
        if n < 0:
            o += 0
            return None
        v = list(struct.unpack_from(">%dh" % n, d, o))
        o += 2 * n
        return v

    def iarr():
        nonlocal o
        n = i32()
        if n < 0:
            return None
        v = list(struct.unpack_from(">%di" % n, d, o))
        o += 4 * n
        return v

    assert d[0:4] == b"AOE1", "bad magic（不是 .aoesave？）"
    o = 8
    n = i32()
    o += n  # navSpec 串
    i32()   # gameMode
    game_mode = 0
    o -= 4
    game_mode = i32()
    i32()   # missionIndex 占位读
    o -= 4
    mission_index = i32()
    i32()   # missionResId
    o += 1  # randomMap bool
    n = i32()  # nfoData
    if n > 0:
        o += n
    i32(); i32()  # campaign/tutorial
    barr_n = i32()
    if barr_n > 0:
        o += barr_n
    barr_n = i32()
    if barr_n > 0:
        o += barr_n
    for _ in range(6):
        i32()
    mt = sarr()
    for _ in range(4):
        iarr()
    pcount = i32()
    players = []
    for _ in range(pcount):
        hdr = iarr()
        slots = sarr()
        bld = iarr()
        sarr()
        players.append(Player(hdr, slots, bld))
    for _ in range(15):
        i32()
    o += 1
    tick = i32()
    return Save(mt, players, tick, game_mode, mission_index)


def _summary(s):
    print("tick=%d gameMode=%d mission=%d" % (s.tick, s.game_mode, s.mission_index))
    for p, pl in enumerate(s.players):
        from collections import Counter
        comp = Counter(u["type"] for u in pl.units())
        comp = " ".join("t%dx%d" % (t, c) for t, c in sorted(comp.items()))
        idle = sum(1 for u in pl.units() if u["task"] == "闲置")
        print("p%d age=%d res=%s pop=%d/%d queue=%d 单位[%s] 闲置=%d 建筑=%d" % (
            p, pl.age, "/".join(map(str, pl.res)), pl.unit_count, pl.pop_cap,
            pl.queue, comp, idle, len(pl.buildings())))


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    s = load(argv[1])
    cmd = argv[2] if len(argv) > 2 else "summary"
    if cmd == "summary":
        _summary(s)
    elif cmd == "res":
        for key, name in ((1, "木"), (2, "金"), (3, "石")):
            pts = s.resources()[key]
            print("%s ×%d: %s" % (name, len(pts), pts))
    elif cmd == "bld":
        p = int(argv[3]) if len(argv) > 3 else 0
        for b in s.players[p].buildings():
            tag = " 施工中" if b["building"] else (" 完工" if b["done"] else "")
            print(f"b{b['idx']} ({b['tx']},{b['ty']}) t{b['type']} "
                  f"{b['name']} {b['status']}{tag}")
    elif cmd == "units":
        p = int(argv[3]) if len(argv) > 3 else 0
        for u in s.players[p].units():
            print("slot%(slot)d (%(tx)d,%(ty)d) t%(type)d %(task)s hp%(hp)d tgt%(tgt)s [w=%(taskword)04x]" % u)
    elif cmd == "tile":
        print(json.dumps(s.tile(int(argv[3]), int(argv[4])), ensure_ascii=False))
    elif cmd == "json":
        print(json.dumps({
            "tick": s.tick, "gameMode": s.game_mode, "mission": s.mission_index,
            "players": [{
                "age": pl.age, "res": pl.res, "pop": pl.unit_count,
                "popCap": pl.pop_cap, "queue": pl.queue,
                "units": pl.units(), "buildings": pl.buildings(),
            } for pl in s.players],
        }, ensure_ascii=False))
    else:
        print("未知命令：%s" % cmd)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
