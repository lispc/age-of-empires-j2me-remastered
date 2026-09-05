#!/usr/bin/env python3
"""m6h 驱动 v8.0 = v7.3 + r65 交棒配方（第八轮, 把 m6 当攻城器械问题）。

 [v8.0-1 塔优先] sweep_claims 排序 (0 if ty==12 else 1, y, x)——进猫火圈前
   三塔 (5,7)(6,4)(2,8) 全平; tw_dead 同步把 (2,8) 纳入 (boot3 尸检: 只查
   (5,7)(6,4) 时 (2,8) 活到终局)。
 [v8.0-2 近战保全] ①S1 hops 南段再南移 1 格 ((31,49)(36,49), 巢穴弓圈外);
   ②S2 转杀唯一条件=clear&&aggro&&melee≥3, 两个超时兜底不再硬打 (boot3
   -3 近战根因), 走 v7.3 留守豁免转 S3; ③团杀 gate melee≥4。
 [v8.0-3 顺序定死] 停滞后: 团杀猫 A (近战集火一只, 不再 i%len 分摊两只——
   分摊=两只都杀不死) → A 死即收队 → 盾决斗猫 B (冲车+我猫 1:0 换, A 死后
   B 的盾位解锁) → 帖扫收尾 (2,5) → S7 清南塔 → WIN。
 [v8.0-4 探针钩子] M6_RAMCHEW=1 时 idle 冲车啃最近活塔 ((5,7)(2,8)(26,28)),
   供接管期启用 (主线默认关, 等 DPS 探针定案)。
 v7.x 底座: S2 引离/顶廊帖扫三层口径/sweep_region 洪泛/sweep_claims/盾两
 段式/M6_STAGE 断点续跑/v7.3 留守豁免/[combat] 死亡差分 ghosts。

 核心思路=「守军自投罗网」: 中局零期望损耗, 不可反转的 reprisal 当武器。
 [1 S2 门钉改引离] 1 近战贴 GATE_BOX 东缘 (30,31) 引守军出塔 (26,28) 程²6,
   沿 (31,33)→(33,35)→(34,37) 风筝到开阔地, 其余近战 KILL_PARK 收——
   宁多花 60s 不打塔下混战 (三门钉战 3:0/4:3/2:2 方差全来自塔下)。
 [2 S5 方阵常驻+双饵轮换] 帖=离线表 nominal/boot2 型二选一 (entry 时定,
   不每 poll 重算——v6.6 动态重算在守军逼近时把 envelope 撤没了); 饵=
   征服者 t9/近战 t3 (死了不亏), 职责=挨第一箭让守军 reprisal 追进
   envelope 被帖弓齐射; 饵中箭后不再拉回 (reprisal 覆盖 retask, 拉回=
   空操作), 直接写为消耗品; 弓 t4 绝不做饵 (S6 唯一劳动力)。
 [3 死亡真源=[combat]] 差分 play.log `p1 typeT died` 行, 匹配 pg 条目
   (type+坐标窗 d²≤50) 进 ghosts; pg 过滤 ghosts 后, "消失=死"才成立
   (aistate 幽灵单位假胜/死锁根除); aistate 只取位置。
 [4 盾先猫后] 冲车盾两段式: 盾先走到盾位 (d²(S,E)∈[9,16]) idle 确认 →
   猫才进盾后溅射位 (d²(B,S)≤5 且 d²(B,E)>d²(S,E) 严格盾后); 盾亡于
   途中 (猫未出发) → 弃攻不送猫; ramshield_spot 扫描窗西扩 sx≥0
   (修 C2 西缘猫 (3,7) 盲)。两猫分两次打 (C1 先 C2 后, pg 序)。
 v6.7 底座: S1 南线 hops (y≥48 巢穴零暴露) / ram_sweep 猫圈豁免 /
 S5→S6 'done' 转段 / S4 漂移监测 / import 闸 / drysim 闸。S0/S3/S6-S8 原样。
 附: mapdump FIFO 指令 (S4 每 45s 守军漂移监测 + S5 帖位可走性核对)。
"""
import json
import math
import os
import re
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6h'
DRY = os.environ.get('M6_DRY', '') == '1'
DRY_STATE = os.environ.get('M6_DRY_STATE', '')
FIFO = os.path.join(W, 'fifo')
if DRY:
    # [自检隔离] 假路径: 即使闸失效也绝无可能写到活局 fifo
    FIFO = '/tmp/aoe-camp/m6h/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M6_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M6_TIMEOUT', '1500'))
PROBE = os.environ.get('M6_MODE', 'main') == 'probe'
PROBE_CAP = float(os.environ.get('M6_PROBE_CAP', '210'))
# [v8.0] 冲车啃塔主闸 (探针定案后由 boot 脚本注入 M6_RAMCHEW=1)
RAMCHEW = os.environ.get('M6_RAMCHEW', '0') == '1'
# S2 兜底豁免计时 (env 供 dry 加速; 主线 90/120 原样)
S2T_AGGRO = float(os.environ.get('M6_S2T_AGGRO', '90'))
S2T_SILENT = float(os.environ.get('M6_S2T_SILENT', '120'))
EVJ = os.path.join(W, 'probe-events.jsonl')
_MAIN = (__name__ == '__main__')

_t0 = time.time()
_dead = False
_n = 0
_last_sum = -99.0


def log(m):
    print(f"[{time.time() - _t0:7.1f}s] {m}", flush=True)


def send(cmds):
    global _dead, _n
    if not cmds:
        return
    if DRY:
        # [自检隔离] dry 模式: 只落日志, 永不触 fifo
        for c in cmds:
            log(f'[dry-send] {c}')
        _n += len(cmds)
        return
    script = "; ".join(f"echo '{c}' > {FIFO}" for c in cmds)
    try:
        subprocess.run(["sh", "-c", script], timeout=6.0)
        _n += len(cmds)
    except subprocess.TimeoutExpired:
        _dead = True
        log('FIFO 无读者(进程退场?)')


def aistate():
    global _dead
    if DRY:
        try:
            with open(DRY_STATE) as f:
                return json.load(f)
        except Exception:
            return None
    for _ in range(4):
        if _dead:
            return None
        try:
            subprocess.run(["sh", "-c", f"echo 'aistate' > {FIFO}"], timeout=4.0)
        except subprocess.TimeoutExpired:
            _dead = True
            return None
        time.sleep(0.10)
        try:
            with open(AIS) as f:
                return json.load(f)
        except Exception:
            time.sleep(0.15)
    return None


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


# ---------- [combat] 死亡真源 (r64: aistate 幽灵单位假胜/死锁根除) ----------
_combat_seen = set()   # 已消费的 (type, x, y, ar) 死亡行
ghosts = set()         # 已确认死亡: (type, tile) — aistate 幽灵过滤键
_combat_m = re.compile(r'\[combat\] p1 type(\d+) died at \((\d+),(\d+)\) '
                       r'ar=(\d+)')


def combat_new_deaths():
    """增量解析 play.log 的 p1 死亡行 → [(type, (x, y)), ...] (本拍新见)。"""
    out = []
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[combat]' not in ln or 'p1 type' not in ln:
                    continue
                m = _combat_m.search(ln)
                if not m:
                    continue
                key = (int(m.group(1)), int(m.group(2)), int(m.group(3)),
                       int(m.group(4)))
                if key in _combat_seen:
                    continue
                _combat_seen.add(key)
                out.append((key[0], (key[1], key[2])))
    except Exception:
        pass
    return out


def mark_dead_from_combat(pg):
    """新死亡行 → 按 type+坐标窗 (d²≤50, 死亡坐标仅提示) 匹配 pg 条目进
    ghosts。返回本拍被标死的 [(type, tile), ...]。匹配不上不硬标 (等坐标
    收敛), 同拍多死按近度贪心一一配。"""
    marked = []
    news = combat_new_deaths()
    if not news:
        return marked
    pool = list(pg)
    for dty, dxy in sorted(news, key=lambda k: k[1]):
        best, bd = None, 51
        for g in pool:
            if g['type'] != dty:
                continue
            dd = d2(tuple(g['tile']), dxy)
            if dd < bd:
                best, bd = g, dd
        if best is None:
            continue
        # [v8.0] 死因歧义守卫: 同型第二只距死点 ≤4 (双猫聚拢追同一目标时
        # 必现) → 本次死亡不标 (等下一发 [combat] 或 slot 消失差分),
        # 否则 filter_ghosts d²≤2 会把两只一起滤掉 = 假「猫全灭」
        amb = [g for g in pool
               if g is not best and g['type'] == dty
               and d2(tuple(g['tile']), dxy) <= 4]
        if amb:
            log(f'  ☠[combat] type{dty}@{dxy} 死因歧义 '
                f'(同型{len(amb)}只在 d²≤4) — 不标, 防双猫假灭')
            pool.remove(best)   # 死点仍从池中除名, 防同拍重复配
            continue
        pool.remove(best)
        gt = (best['type'], tuple(best['tile']))
        ghosts.add(gt)
        marked.append(gt)
        log(f'  ☠[combat] type{dty}@{dxy} → pg s{best["slot"]}'
            f'{tuple(best["tile"])} 记亡 (ghosts={len(ghosts)})')
    return marked


def filter_ghosts(foes):
    """aistate 只取位置: 与 ghosts 同 type 且 d²≤2 的条目 = 死亡幽灵, 滤除。"""
    out = []
    for u in foes:
        t = tuple(u['tile'])
        if any(gty == u['type'] and d2(t, gxy) <= 2
               for gty, gxy in ghosts):
            continue
        out.append(u)
    return out


def mapdump(fname, box=None):
    """mapdump FIFO 指令 (服务端直读 mapTiles 全量真值, 不吃雾)。"""
    cmd = f'mapdump {fname}'
    if box:
        cmd += ' ' + ' '.join(str(v) for v in box)
    send([cmd])
    time.sleep(0.5)
    try:
        rows = {}
        with open(fname, errors='replace') as f:
            for ln in f:
                m = re.match(r'\s?(\d+)(.*)', ln)
                if not m or ln.startswith('#'):
                    continue
                rows[int(m.group(1))] = m.group(2)
        return rows
    except Exception:
        return None


def clamp(t):
    return (max(1, min(62, t[0])), max(1, min(62, t[1])))


def idle(u):
    return list(u['target']) == [u['tile'][0], u['tile'][1]]


# ---------- 静态计划 (S0-S4 = v5.5 原样) ----------
TOWER50 = (22, 50)
POSTS_S0 = [(25, 50), (19, 50), (22, 53), (25, 49), (19, 51), (25, 51),
            (23, 53), (21, 53), (20, 53)]
RAMS_S0 = [(21, 50), (22, 49), (23, 51)]
MELEE_S0 = [(19, 54), (20, 55), (21, 55), (22, 56), (23, 55)]

# [v8.0] 南段 y≥49 (巢穴弓圈 d²36 全程外; 旧 (31,48) 距 (31,43) 仅 d²25 贴圈)
HOPS_EAST = [(18, 55), (17, 52), (19, 50), (25, 49), (31, 49), (36, 49),
             (40, 46), (43, 44), (43, 40), (41, 38), (38, 37), (36, 36),
             (35, 35)]
STAGING_GATE = [(34, 34), (35, 34), (33, 34), (34, 35), (35, 33), (36, 34),
                (33, 35), (36, 35), (35, 32), (34, 36), (33, 33), (36, 36),
                (34, 33), (35, 31), (33, 36), (36, 33), (34, 32)]
GATE_BOX = lambda t: 25 <= t[0] <= 30 and 26 <= t[1] <= 32
SOLO = (32, 16)
RET_S2 = [(34, 36), (35, 33)]
# [v7.0 S2 引离] 塔 (26,28) 程²6; 饵贴东缘 (30,31) 引守军出塔, 沿 wp 风筝
# 到开阔地, 其余近战埋伏 KILL_PARK 收 (不打塔下混战)
LURE0 = (30, 31)
LURE_WP = [(31, 33), (33, 35), (34, 37), (35, 38)]
KILL_PARK = [(35, 36), (36, 37), (34, 38), (36, 36), (35, 35)]
# S2 目标域=箱内 ∪ 被引出追饵者 (离饵 7 格内)——只看 GATE_BOX 会把追出箱的
# 守军误判成「已清空」转 S3, 留 3 追兵在身后
S2_FOE = lambda t: GATE_BOX(t) or d2(t, LURE0) <= 49

HOPS_NORTH = [(29, 34), (29, 29), (30, 28), (30, 27), (30, 26), (29, 25),
              (30, 22), (33, 20), (34, 19), (36, 18), (37, 16), (37, 12),
              (36, 11), (35, 10), (30, 10), (25, 10), (20, 9), (18, 8),
              (16, 8), (16, 6), (16, 4), (16, 2), (16, 1)]
STRIP = [(15, 0), (16, 0), (17, 0), (15, 1), (16, 1), (17, 1), (15, 2),
         (16, 2), (17, 2), (14, 0), (14, 1), (14, 2), (16, 3), (15, 3),
         (17, 3), (15, 4), (16, 4)]

SOFT7 = {(1, 1), (2, 1), (1, 3), (2, 3), (4, 2), (5, 1), (2, 5)}
SOFT_ALL = SOFT7 | {(4, 4), (2, 5)}
POSTS_B = [(1, 0), (2, 0), (0, 1), (3, 0), (4, 0), (1, 2), (0, 2), (6, 0),
           (0, 4)]
TOWER_POSTS = {(2, 8): (0, 11), (5, 7): (5, 11), (6, 4): (6, 0)}
POSTS_T3 = [(23, 27), (25, 25), (23, 26), (24, 25), (22, 28), (26, 24)]
PGF = lambda t: t[0] <= 19 and t[1] <= 19

# ---------- S5 顶廊帖扫 (v7.1) ----------
RNG = {4: 16, 8: 25}
TRIG = {4: 16, 8: 20}
# 候选开火位不再手列 (v7.1b): 安全区洪泛生成 — 区内任意两格间路径全程安全
# [v8.0b boot1 尸检] 驻军挪 y0 行: 从 y0 直线西行的 BFS 全程 d²≥25 于口袋弓
# (8,5), 不再掠 (8,1)/(9,1) 弓圈边 (d²16/17) — boot1 走弓圈边→reprisal→
# 全军 spiral 进口袋 (archer 死在 (5,3)(5,4)(2,6)(7,3))
SWEEP_PARK = [(12, 0), (13, 0), (14, 0), (15, 0), (16, 0), (17, 0),
              (13, 1), (16, 1)]


def sweep_safe(P, tier, pgtt, tws):
    for g, ty in pgtt:
        lim = (_margin(RNG[ty]), TRIG[ty] + 4, TRIG[ty])[tier]
        if d2(P, g) <= lim:
            return False
    for t in tws:
        if d2(P, t) <= (12, 9, 7)[tier]:
            return False
    return True


def sweep_region(pgtt, tws, tier, seeds):
    """[v7.1b] 安全区洪泛: 从 seeds 出发只沿「对该 tier 安全」的可走格扩散
    — 区内任意两格间路径全程安全 (行军不被猫/塔点着)。"""
    from collections import deque
    seen = set()
    dq = deque(s for s in seeds if wk(s) and sweep_safe(s, tier, pgtt, tws))
    seen.update(dq)
    while dq:
        x, y = dq.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n in seen or not (0 <= n[0] <= 21 and 0 <= n[1] <= 21):
                continue
            if not wk(n) or not sweep_safe(n, tier, pgtt, tws):
                continue
            seen.add(n)
            dq.append(n)
    return seen


def sweep_claims(ebs, pg, seeds):
    """建筑→(帖位, tier): tier0 严格→tier2 (trig) 递降, 取首个有 ≥8 格安全
    区的 tier; 帖=区内 d²(P,B)≤16 离军队锚 (10,1) 最近者。无解建筑不进
    claims (blocked → 猫死后下拍自动解锁)。
    [v8.0] 塔优先: type12 排最前——确保进猫火圈前 (5,7)(6,4)(2,8) 全平
    (boot3 反例: 塔最后剩 2 座活到终局)。"""
    pgtt = [(tuple(g['tile']), g['type']) for g in pg]
    tws = [tuple(b['tile']) for b in ebs if b['type'] == 12]
    # [v8.0b] 洪泛用 tier1 口径 (TRIG+4: 弓20/猫24): y0 走廊全程在弓 TRIG16
    # 外 ≥1 格, 而 tier0 margin(25) 会把 (8,0) (恰 d²25) 封死 → claims=0;
    # tier2 深帖 ((0,11) 型) 仍被 tier1 洪泛挡在西柱外 (boot1 walk-trap 根除)
    region = sweep_region(pgtt, tws, 1, seeds)
    regions = {0: region, 1: region, 2: region}
    # [v8.0c] 帖位双闸: 连通域用 tier1 (TRIG+4, 保走廊), 帖本身要求
    # aggro 圈外严格 d²>25 (4-5 格圈是全兵种统一半径; boot2 实测 (5,0)/
    # (8,0) 恰 d²25 被闲时守军 aggro → reprisal spiral 10s 团灭)。守军
    # aiEnabled=false 永不主动, d²>25 即永久安全
    pgt = [t for t, _ in pgtt]
    out = {}
    taken = set()
    for b in sorted(ebs, key=lambda b: (0 if b['type'] == 12 else 1,
                                        b['tile'][1], b['tile'][0])):
        bt = tuple(b['tile'])
        for tier in (0, 1, 2):
            if len(regions[tier]) < 8:
                continue
            cands = [P for P in regions[tier]
                     if d2(P, bt) <= 16 and P not in taken
                     and all(d2(P, g) > 25 for g in pgt)]
            if cands:
                P0 = min(cands, key=lambda q: (d2(q, (10, 1)), q))
                out[bt] = (P0, tier)
                taken.add(P0)
                break
    return out
# [v7.0] 离线帖表二选一 (post_calc.py nominal/boot2 型, entry 时定案不重算)
POSTS_NOMINAL = [(15, 5), (14, 4), (14, 6), (16, 4), (16, 6)]
POSTS_BOOT2 = [(15, 5), (16, 4), (16, 6), (15, 7), (17, 5)]


def choose_posts(pgtt):
    """按 live pg 二选一: nominal 全安全优先, 次 boot2 型; 都残→取安全帖多
    的表的安全子集; 全 0 → None (调用方 region_posts 兜底一次)。"""
    def safe(t):
        return (wk(t) and not MID_BAND(t)
                and all(d2(t, g) > _margin(RNG[ty]) for g, ty in pgtt))
    n_n = [p for p in POSTS_NOMINAL if safe(p)]
    n_b = [p for p in POSTS_BOOT2 if safe(p)]
    if len(n_n) == 5:
        return list(POSTS_NOMINAL), 'nominal'
    if len(n_b) == 5:
        return list(POSTS_BOOT2), 'boot2'
    if len(n_b) > len(n_n) and n_b:
        return n_b, f'boot2-part{len(n_b)}'
    if n_n:
        return n_n, f'nominal-part{len(n_n)}'
    return None, 'none'


def _margin(rsq):
    return rsq + 2 * int(math.isqrt(rsq)) + 1


# NW 口袋可走格 (zone_calc.py 从 base.aoesave 提取, x,y ≤21; 同图确定性)
_WALK_ROWS = {
    0: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17],
    1: [0, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17],
    2: [0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17],
    3: [0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17],
    4: [0, 1, 2, 3, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18],
    5: [0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21],
    6: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21],
    7: [0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21],
    8: [0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21],
    9: list(range(0, 22)),
    10: list(range(0, 22)),
    11: list(range(0, 22)),
    12: list(range(0, 22)),
    13: list(range(0, 22)),
    14: list(range(0, 22)),
    15: list(range(0, 22)),
    16: list(range(0, 22)),
    17: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 19, 20, 21],
    18: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 20, 21],
    19: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 20, 21],
    20: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 20, 21],
    21: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 21],
}
WALK_NW = frozenset((x, y) for y, xs in _WALK_ROWS.items() for x in xs)

# [v7.0] WALK_NW 把部分敌建筑格当可走 (mapdump 实证 (3,1)(3,2)(7,5)…=B/#) —
# S5 entry 用 mapdump 真值修正: BAD_TILES=非'.'格; 选点一律过 wk()
BAD_TILES = set()


def wk(t):
    return t in WALK_NW and t not in BAD_TILES


def load_bad_tiles(rows):
    """mapdump rows → BAD_TILES (x,y≤21 与 WALK_NW 同域)。
    [v8.0] 重建式: 先清再加 — 建筑死后格子转 '.', 释放死建筑格打开北廊
    ((2,1)(3,1) 等活时把 y0-2 西段封死, (2,5) 因此永远无洪泛可达帖)。"""
    BAD_TILES.clear()
    for y, row in (rows or {}).items():
        for x, ch in enumerate(row):
            if x <= 21 and y <= 21 and ch != '.' and ch not in 'vme':
                if ch in 'TBGS#BH':
                    BAD_TILES.add((x, y))
    return len(BAD_TILES)

DUEL_REST = (13, 6)
SQ_ANCHOR = (15, 5)
# 中场猫堆铁律 (防御性, 一切 S5 落点校验): y≥20 的 x12-14 带永不进入
MID_BAND = lambda t: 12 <= t[0] <= 14 and t[1] >= 20
SWARM_RING = [(1, 0), (-1, 0), (0, 1), (1, 1), (-1, 1), (0, -1), (1, -1),
              (-1, -1)]
CORRIDOR_REST = [(16, 4), (16, 5), (16, 6), (17, 6), (16, 7), (17, 7),
                 (16, 8), (15, 4), (16, 3), (15, 3), (16, 2), (17, 8)]
MEL_PARK = [(16, 6), (17, 6), (16, 7), (17, 7), (16, 8)]
NORTH_CORRIDOR = [(14, 0), (15, 0), (16, 0), (17, 0), (14, 1), (15, 1),
                  (16, 1), (17, 1), (14, 2), (15, 2), (16, 2), (17, 2),
                  (13, 1), (12, 1), (13, 2)]
# [S6 兜底击杀序] (v6.3 原)
DUEL_ORDER = [((8, 5), 4, 'A1'), ((5, 5), 8, 'C1'), ((3, 7), 8, 'C2'),
              ((6, 7), 4, 'A2'), ((4, 9), 4, 'A3')]

# ---------- 状态 ----------
stage = os.environ.get('M6_STAGE', 'S0')
assign = {}
hop_i = {}
issue = {}
focus = None
stage_t = 0
sent_s6_posts = False
sent_s7 = False
remaining = set()
lastpos = {}
prev_my = -1
death_marks = []
breaker_n = 0
hold_until = 0.0
ram_trading = False
slot_types = {}
seen_units = {}
duel = {'tgt': None, 'gtype': None, 'tag': None, 'qt': None,
        'qhp': 255, 'qhp0': 255, 'bait': None, 'btype': None,
        'kind': None, 'phase': None, 'btile': None, 'bhp': 255,
        'quiet': 0, 'home_n': 0, 't0': 0.0, 'prog': 0.0}
DUEL_EMPTY = {'tgt': None, 'gtype': None, 'tag': None, 'qt': None,
              'qhp': 255, 'qhp0': 255, 'bait': None, 'btype': None,
              'kind': None, 'phase': None, 'btile': None, 'bhp': 255,
              'quiet': 0, 'home_n': 0, 't0': 0.0, 'prog': 0.0}
s6_phase = 0
s5_wall0 = 0.0
# [v7.0 S2 引离] 状态
s2d = {'bait': None, 'aggro': False, 'wp': 0, 't0': 0.0, 'kill': False,
       'armed': False, 'nudged': False}
probe_stop = False
probe_stop_wall = 0.0
probe_last = None
evf = None
_last_sweep_warn = 0.0
s6_ignore_pg = False
# v7.1 S5 顶廊帖扫状态
s5 = {'posts': [], 'park': [], 'ramhome': [], 'skip': set(), 'fail': {},
      'anchors': {}, 'stall0': 0.0, 'stall_sweep': 0.0, 'swarm': False,
      'swarm_cats0': 9, 'prog_sig': None, 'prog_t': 0.0}
fish_d = None
ramdrag_sent = 0
drift_seen = {}
_freeze_last = 0.0
_last_mapdump = 0.0
_last_s5dump = 0.0


def hold_retreat(mine):
    """[熔断 hold] 全军撤 x16 走廊: 守军胜后会成波前压, 原地 hold=白送。"""
    for i, u in enumerate(mine):
        t = CORRIDOR_REST[i % len(CORRIDOR_REST)]
        assign[u['slot']] = t
        if idle(u) and d2(tuple(u['tile']), t) > 2:
            set_target(u, t, '熔断撤')


def zone_ok(m, others):
    """m 对全部其余活守军处于「射程并集外>1格」。others=[(tile,type)]"""
    return all(d2(m, o[0]) > _margin(RNG[o[1]]) for o in others)


def wander_ok(m, others):
    """m 在其余守军各向 m 游走 ±1 格后仍在并集外。"""
    for gtile, gt in others:
        g2 = (gtile[0] + (1 if m[0] > gtile[0] else
                          -1 if m[0] < gtile[0] else 0),
              gtile[1] + (1 if m[1] > gtile[1] else
                          -1 if m[1] < gtile[1] else 0))
        if d2(m, g2) <= _margin(RNG[gt]):
            return False
    return True


def meeting_zone(ttile, ttype, others):
    """会合区: 目标射程内 (触发交火, TRIG 口径) + 其余守军并集外>1格;
    优先游走安全格, 次选离 DUEL_REST 近的。无解返回 None。"""
    r = TRIG.get(ttype, 16)
    cands = [m for m in WALK_NW
             if wk(m) and 4 <= d2(m, ttile) <= r and zone_ok(m, others)]
    if not cands:
        return None
    cands.sort(key=lambda m: (0 if wander_ok(m, others) else 1,
                              d2(m, DUEL_REST)))
    return cands[0]


def _others_now(pg, skip_tile):
    return [(tuple(g['tile']), g['type']) for g in pg
            if tuple(g['tile']) != skip_tile]


def track_quarry(pg):
    """[alive 误判修法 v7.0] 猎物追踪: slot 主匹配 (fslot+type+宽窗);
    兜底=紧邻 d²≤2 或掉血者 (防 slot 压缩串位; 也防邻位同型顶替 → 假活)。"""
    qhp = duel.get('qhp', 255)
    qhp0 = duel.get('qhp0', 255)
    if duel.get('fslot') is not None:
        s = [g for g in pg if g['slot'] == duel['fslot']
             and g['type'] == duel['gtype']
             and d2(tuple(g['tile']), duel['qt']) <= 30]
        if s:
            return s[0]
    cands = [g for g in pg if g['type'] == duel['gtype']
             and (d2(tuple(g['tile']), duel['qt']) <= 2
                  or g['hp'] < qhp0 - 10)]
    dmg = [g for g in cands if g['hp'] < qhp0 - 10]
    if dmg:
        dmg.sort(key=lambda g: (abs(g['hp'] - qhp),
                                d2(tuple(g['tile']), duel['qt'])))
        return dmg[0]
    near = [g for g in cands if d2(tuple(g['tile']), duel['qt']) <= 2]
    if near:
        near.sort(key=lambda g: (abs(g['hp'] - qhp),
                                 d2(tuple(g['tile']), duel['qt'])))
        return near[0]
    return None


def _duel_reset(why):
    global duel
    log(f'  决斗复位({why})')
    duel = dict(DUEL_EMPTY)


def _fail_bump(fslot, fk):
    if fslot is not None and fk in ('猫对猫硬换', '冲车盾猫对猫'):
        s5['fail'][fslot] = s5['fail'].get(fslot, 0) + 1


def _recall(u, why=''):
    if u is None:
        return
    if s5['posts']:
        p = s5['posts'][u['slot'] % len(s5['posts'])]
    else:
        p = DUEL_REST
    assign[u['slot']] = p
    set_target(u, p, '收兵' + why)


def _duel_win(melee, b):
    log(f'  决斗胜: {duel["tag"]}{duel["qt"]}({duel["kind"]}) '
        f'耗时{time.time() - duel["t0"]:.0f}s')
    _recall(b, '饵')
    if duel['kind'] == '近战围杀':
        for u in melee:
            a = MEL_PARK[u['slot'] % len(MEL_PARK)]
            assign[u['slot']] = a
            set_target(u, a, '决斗收兵')
    _duel_reset('胜')


def _swarm_tick(melee):
    """近战围杀: 散环入位; 猫挪位时 idle 偏离者重发。"""
    qt = duel['qt']
    for i, u in enumerate(melee):
        rx, ry = SWARM_RING[i % len(SWARM_RING)]
        rt = (qt[0] + rx, qt[1] + ry)
        if not wk(tuple(rt)):
            rt = qt
        assign[u['slot']] = rt
        if idle(u) and d2(tuple(u['tile']), rt) > 2:
            set_target(u, rt, '围杀')
        elif not idle(u) and list(u['target']) != list(rt) \
                and d2(tuple(u['tile']), rt) > 4:
            set_target(u, rt, '围杀修')


def build_plan(pg):
    """[S6 兜底] DUEL_ORDER 一一认领 (同 type 内最近 nominal 者认领该序位),
    序外游走出窗的守军兜底追加。"""
    claimed = set()
    plan = []
    for tt, gt, tag in DUEL_ORDER:
        m = [g for g in pg if g['type'] == gt and id(g) not in claimed
             and d2(tuple(g['tile']), tt) <= 30]
        if not m:
            continue
        m.sort(key=lambda g: d2(tuple(g['tile']), tt))
        claimed.add(id(m[0]))
        plan.append((m[0], f'{tag}s{m[0]["slot"]}'))
    for g in pg:
        if id(g) not in claimed:
            plan.append((g, f't{g["type"]}s{g["slot"]}'))
    return plan


def duel_drive(mine, ranged, melee, rams, pg):
    """活动决斗的驱动 (v6.3 fight/rest 逻辑原样, 开局由 duel_open_cat 控制)。"""
    global duel
    now = time.time()
    if duel['tgt'] is None:
        return
    q = track_quarry(pg)
    b = next((x for x in mine if x['slot'] == duel['bait']
              and slot_types.get(duel['bait']) == duel['btype']),
             None) if duel['bait'] is not None else None
    if q is None:
        if duel.get('shield') is not None:
            sh = next((x for x in mine if x['slot'] == duel['shield']), None)
            if sh is not None:
                p = (s5['ramhome'][sh['slot'] % len(s5['ramhome'])]
                     if s5['ramhome'] else (18, 4))
                assign[sh['slot']] = p
                set_target(sh, p, '盾收兵')
        _duel_win(melee, b)
        return
    prev_qt, prev_qhp = duel['qt'], duel['qhp']
    duel['qt'], duel['qhp'] = tuple(q['tile']), q['hp']
    if duel['qt'] != prev_qt or duel['qhp'] < prev_qhp:
        duel['prog'] = now
    if now - duel['prog'] > 120:
        log(f'  决斗超时(120s无进展) {duel["tag"]}@{duel["qt"]} — 强制重选')
        fs, fk = duel.get('fslot'), duel.get('kind')
        _duel_reset('超时')
        _fail_bump(fs, fk)
        return
    if duel['phase'] in ('shield_go', 'shield_wait', 'shield'):
        sh = next((x for x in mine if x['slot'] == duel.get('shield')), None) \
            if duel.get('shield') is not None else None
        engaged = (b is not None and b['hp'] < duel['bhp']) \
            or (duel['qhp'] < prev_qhp) or (duel['qt'] != prev_qt) \
            or (duel['phase'] == 'shield' and sh is not None
                and sh['hp'] < duel.get('shp', 255))
        if duel['phase'] == 'shield_go':
            if sh is None:
                # 盾亡于途中, 猫未出发 → 弃攻不送猫 (送=白给, boot3 实录)
                fs, fk = duel.get('fslot'), duel.get('kind')
                _duel_reset('盾亡于途中(猫未发)')
                _fail_bump(fs, fk)
                return
            if idle(sh) and d2(tuple(sh['tile']), duel['spot_s']) <= 2:
                cat = next((x for x in mine if x['slot'] == duel['bait']
                            and slot_types.get(duel['bait']) == 8), None)
                if cat is None:
                    fs, fk = duel.get('fslot'), duel.get('kind')
                    _duel_reset('猫消失(盾位)')
                    _fail_bump(fs, fk)
                    return
                duel['cat_sent'] = True
                duel['phase'] = 'shield_wait'
                assign[cat['slot']] = duel['spot_b']
                set_target(cat, duel['spot_b'], f'盾确认,猫进{duel["spot_b"]}')
                log(f'  盾就位@{sh["tile"]} hp{sh["hp"]} → 猫进盾后'
                    f'{duel["spot_b"]}')
            elif time.time() - duel['prog'] > 90:
                fs, fk = duel.get('fslot'), duel.get('kind')
                _duel_reset('盾到不了位90s')
                _fail_bump(fs, fk)
            return
        if duel['phase'] == 'shield_wait':
            if sh is None:
                duel['phase'] = 'fight'      # 猫已出发, 不可拉回 → 自由对射
                log('  盾亡(猫在途) → 猫自由对射')
                return
            cat = next((x for x in mine if x['slot'] == duel['bait']), None)
            if cat is not None and idle(cat) \
                    and d2(tuple(cat['tile']), duel['spot_b']) <= 2:
                duel['phase'] = 'shield'
                duel['prog'] = time.time()
                duel['shp'] = sh['hp']
                duel['bhp'] = cat['hp']
                log(f'  盾猫就位 (盾@{sh["tile"]} 猫@{cat["tile"]}) '
                    f'— 等接火')
            return
        # phase == 'shield': 双双就位, 等接火 (任何掉血/守军位移)
        if engaged:
            duel['phase'] = 'fight'
            duel['quiet'] = 0
            log(f'  盾接火 (盾hp={sh["hp"] if sh else "?"}) → 自由对射')
            return
        if sh is None:
            duel['phase'] = 'fight'
            log('  盾亡(接火前) → 猫自由对射')
            return
        if time.time() - duel['prog'] > 45:
            # 敌猫不打盾 (射程外/目标选择) → 盾进逼贴脸引火 (20s 节流)
            duel['prog'] = time.time()
            assign[duel['shield']] = duel['qt']
            set_target(sh, duel['qt'], '盾贴脸引火')
        return
    if duel['kind'] == '近战围杀':
        melee_near = [u for u in melee
                      if d2(tuple(u['tile']), DUEL_REST) <= 100]
        if not melee_near:
            log(f'  围杀步兵耗尽 @{duel["tag"]} — 弃攻重选')
            fs = duel.get('fslot')
            _duel_reset('无步兵')
            if fs is not None:
                s5['fail'][fs] = s5['fail'].get(fs, 0) + 1
            return
        _swarm_tick(melee_near)
        return
    if b is None:
        log(f'  饵 s{duel["bait"]} 阵亡/易主 @决斗 {duel["tag"]} — 重选')
        fs, fk = duel.get('fslot'), duel.get('kind')
        _duel_reset('饵亡')
        _fail_bump(fs, fk)
        return
    if duel['phase'] == 'rest':
        if idle(b) and d2(tuple(b['tile']), DUEL_REST) <= 2:
            others = _others_now(pg, duel['qt'])
            m = meeting_zone(duel['qt'], duel['gtype'], others)
            if m is None:
                fs, fk = duel.get('fslot'), duel.get('kind')
                _duel_reset('会合区失效')
                _fail_bump(fs, fk)
                return
            duel['phase'] = 'fight'
            duel['btile'] = m
            duel['bhp'], duel['quiet'], duel['home_n'] = b['hp'], 0, 0
            assign[b['slot']] = m
            set_target(b, m, f'饵入会合区{m}')
            log(f'  {duel["tag"]} 会合区={m} others='
                f'{[(t, ty) for (t, ty) in others]}')
        elif not idle(b) and list(b['target']) != list(DUEL_REST):
            set_target(b, DUEL_REST, '饵收')
        elif idle(b):
            set_target(b, DUEL_REST, '饵收')
        return
    # phase == 'fight': free-fight 不钉位; 12s 全静默准 home ≤3 次
    # (v6.4: changed 只认掉血——回血不再清 quiet)
    changed = (b['hp'] < duel['bhp']) or (duel['qhp'] < prev_qhp) \
        or (duel['qt'] != prev_qt)
    duel['bhp'] = b['hp']
    duel['quiet'] = 0 if changed else duel.get('quiet', 0) + 1
    if duel['quiet'] >= 35 and duel['home_n'] < 3 \
            and d2(tuple(duel['btile']), duel['qt']) > 9:
        bx, by = duel['btile']
        qx, qy = duel['qt']
        dx = -1 if qx < bx else (1 if qx > bx else 0)
        dy = -1 if qy < by else (1 if qy > by else 0)
        nb = clamp((bx + dx, by + dy))
        others = _others_now(pg, duel['qt'])
        duel['quiet'] = 0
        if wk(tuple(nb)) and zone_ok(nb, others):
            duel['btile'] = nb
            duel['home_n'] += 1
            assign[b['slot']] = nb
            set_target(b, nb, f'饵home{nb}')
            log(f'  饵 home→{nb} (12s无火, 会合区重算)')
        else:
            log(f'  home→{nb} 落会合区外, 放弃 (待超时重选)')


def ramshield_spot(E, pg, tws=()):
    """冲车盾点位 [v7.2 塔安全]: 盾 S 在 E 射程内 (d²∈[9,16]) 且对其他守军
    >20 且对活塔 d²>9; 我猫 B 盾后溅射位: d²(B,S)≤5 且 d²(B,E)>d²(S,E)
    (严格盾后——猫比盾离敌远, 敌猫=最近单位必先打盾) 且 d²(B,E)≤20。
    扫描窗 sx≥0 (v7.0 西扩)。返回 (S,B) 或 None。"""
    others = [(tuple(h['tile']), h['type']) for h in pg
              if tuple(h['tile']) != tuple(E)]
    for sy in range(0, 13):
        for sx in range(0, 19):
            S = (sx, sy)
            if not wk(S) or not (9 <= d2(S, E) <= 16):
                continue
            if any(d2(S, t) <= 9 for t in tws):
                continue
            if any(d2(S, o) <= 20 for o, ty in others):
                continue
            for by in range(max(0, sy - 3), min(13, sy + 4)):
                for bx in range(max(0, sx - 3), min(19, sx + 4)):
                    B = (bx, by)
                    if B == S or not wk(B):
                        continue
                    if any(d2(B, t) <= 9 for t in tws):
                        continue
                    if d2(B, S) > 5:
                        continue
                    if not (9 <= d2(B, E) <= 20):
                        continue
                    if d2(B, E) <= d2(S, E):
                        continue      # 必须严格盾后 (猫比盾离敌远)
                    if any(d2(B, o) <= 20 for o, ty in others):
                        continue
                    return (S, B)
    return None


def duel_open_cat(g, ranged, melee, pg, rams=None):
    """猫目标开局: [v7.0 首选] 冲车盾猫对猫 两段式——盾先入盾位 idle 确认,
    猫才进盾后溅射位 (盾未就位猫先进圈=被点名, boot3 实录); 盾亡于途中
    (猫未出发) → 弃攻不送猫 → 猫对猫硬换 → 近战围杀 (fail≥2 近战≥4)。"""
    global duel, _freeze_last
    tt = tuple(g['tile'])
    others = [(tuple(h['tile']), h['type']) for h in pg if h is not g]
    fails = s5['fail'].get(g['slot'], 0)
    cats0 = sorted([u for u in ranged if u['type'] == 8],
                   key=lambda u: -u['hp'])
    if rams and cats0:
        shield = max(rams, key=lambda u: u['hp'])
        spot = ramshield_spot(tt, pg, tws=duel_tws)
        if spot is not None and slot_types.get(cats0[0]['slot']) == 8:
            duel = {'tgt': tt, 'gtype': 8, 'tag': f'Cs{g["slot"]}', 'qt': tt,
                    'qhp': g['hp'], 'qhp0': g['hp'], 'bait': cats0[0]['slot'],
                    'btype': 8, 'kind': '冲车盾猫对猫', 'phase': 'shield_go',
                    'btile': spot[1], 'spot_s': spot[0], 'spot_b': spot[1],
                    'shield': shield['slot'],
                    'bhp': cats0[0]['hp'], 'quiet': 0, 'home_n': 0,
                    'cat_sent': False,
                    't0': time.time(), 'prog': time.time(),
                    'fslot': g['slot']}
            assign[shield['slot']] = spot[0]
            set_target(shield, spot[0], f'盾先入{spot[0]}')
            log(f'  冲车盾开局(两段式): {duel["tag"]}{tt} 盾=s{shield["slot"]}'
                f'→{spot[0]} 猫待命={cats0[0]["slot"]}→{spot[1]} (盾确认后再进)')
            return True
    melee_near = [u for u in melee
                  if d2(tuple(u['tile']), DUEL_REST) <= 100]
    if len(melee_near) >= 4 and fails >= 2:
        duel = {'tgt': tt, 'gtype': 8, 'tag': f'Cs{g["slot"]}', 'qt': tt,
                'qhp': g['hp'], 'qhp0': g['hp'], 'bait': None,
                'btype': None, 'kind': '近战围杀', 'phase': 'rush',
                'btile': None, 'bhp': 255, 'quiet': 0, 'home_n': 0,
                't0': time.time(), 'prog': time.time(), 'fslot': g['slot']}
        log(f'  围杀开局: {duel["tag"]}{tt}(t8,hp{g["hp"]}) '
            f'melee_near={len(melee_near)} (猫对猫已败×{fails})')
        _swarm_tick(melee_near)
        return True
    cats = sorted([u for u in ranged if u['type'] == 8], key=lambda u: -u['hp'])
    if not cats:
        return False
    z = meeting_zone(tt, 8, others)
    if z is None:
        if time.time() - _freeze_last > 10:
            _freeze_last = time.time()
            log(f'  猫{tt} 会合区无解 others={others} — 冻结等待 (10s 节流)')
        return False
    bait = cats[0]
    if slot_types.get(bait['slot']) != 8:
        log(f'  s{bait["slot"]} 入口串位 (快照t{slot_types.get(bait["slot"])}) — 本拍弃')
        return False
    duel = {'tgt': tt, 'gtype': 8, 'tag': f'Cs{g["slot"]}', 'qt': tt,
            'qhp': g['hp'], 'qhp0': g['hp'], 'bait': bait['slot'],
            'btype': 8, 'kind': '猫对猫硬换', 'phase': 'fight', 'btile': z,
            'bhp': bait['hp'], 'quiet': 0, 'home_n': 0,
            't0': time.time(), 'prog': time.time(), 'fslot': g['slot']}
    assign[bait['slot']] = z
    set_target(bait, z, f'猫饵入{z}')
    log(f'  猫对猫开局: {duel["tag"]}{tt}(t8,hp{g["hp"]}) 饵=s{bait["slot"]} '
        f'zone={z} others={others}')
    return True


def duel_supervise_s6(mine, ranged, melee, rams, pg):
    """[S6 兜底] 漏网守军: 无活动决斗时按 DUEL_ORDER 开局 (v6.4 逻辑)。"""
    global duel
    if duel['tgt'] is not None:
        duel_drive(mine, ranged, melee, rams, pg)
        return
    for tgt, tag in build_plan(pg):
        tt = tuple(tgt['tile'])
        others = [(tuple(g['tile']), g['type']) for g in pg if g is not tgt]
        cats = sorted([u for u in ranged if u['type'] == 8],
                      key=lambda u: -u['hp'])
        if tgt['type'] == 4:
            zone = meeting_zone(tt, 4, others)
            if zone is None:
                log(f'  {tag}{tt} 弓: 会合区无解 — 试下个目标')
                continue
            bait = pick_bait(mine, zone)
            if bait is None:
                return
            assign[bait['slot']] = DUEL_REST
            set_target(bait, DUEL_REST, '饵收柱')
            duel = {'tgt': tt, 'gtype': 4, 'tag': tag, 'qt': tt,
                    'qhp': tgt['hp'], 'qhp0': tgt['hp'],
                    'bait': bait['slot'], 'btype': bait['type'],
                    'kind': '饵对射',
                    'phase': 'rest', 'btile': None, 'bhp': bait['hp'],
                    'quiet': 0, 'home_n': 0, 't0': time.time(),
                    'prog': time.time(), 'fslot': tgt['slot']}
            log(f'  决斗开局: {tag}{tt}(t4,hp{tgt["hp"]}) '
                f'饵=s{bait["slot"]}(t{bait["type"]})')
            return
        if tgt['type'] == 8:
            if melee:
                duel = {'tgt': tt, 'gtype': 8, 'tag': tag, 'qt': tt,
                        'qhp': tgt['hp'], 'qhp0': tgt['hp'], 'bait': None,
                        'btype': None, 'kind': '近战围杀', 'phase': 'rush',
                        'btile': None, 'bhp': 255, 'quiet': 0, 'home_n': 0,
                        't0': time.time(), 'prog': time.time(),
                        'fslot': tgt['slot']}
                log(f'  围杀开局: {tag}{tt}(t8,hp{tgt["hp"]}) melee={len(melee)}')
                _swarm_tick(melee)
                return
            if cats:
                zone = meeting_zone(tt, 8, others)
                if zone is None:
                    log(f'  {tag}{tt} 猫对猫: 会合区无解 — 试下个目标')
                    continue
                bait = cats[0]
                assign[bait['slot']] = DUEL_REST
                set_target(bait, DUEL_REST, '饵收柱')
                duel = {'tgt': tt, 'gtype': 8, 'tag': tag, 'qt': tt,
                        'qhp': tgt['hp'], 'qhp0': tgt['hp'],
                        'bait': bait['slot'], 'btype': 8, 'kind': '猫对猫硬换',
                        'phase': 'rest', 'btile': None, 'bhp': bait['hp'],
                        'quiet': 0, 'home_n': 0, 't0': time.time(),
                        'prog': time.time(), 'fslot': tgt['slot']}
                log(f'  猫对猫开局: {tag}{tt}(t8) 饵=s{bait["slot"]}')
                return


# ---------- v6.5 帖诱反杀 (fish) ----------
def region_posts(pg, n, xr, yr, anchor, pairwise):
    """安全帖贪心: 对全部 pg margin 外 (弓>25/猫>37), 锚点就近, pairwise 分散。"""
    cands = []
    for y in yr:
        for x in xr:
            t = (x, y)
            if not wk(t) or MID_BAND(t):
                continue
            if all(d2(t, g) > _margin(RNG[ty]) for g, ty in pg):
                cands.append(t)
    cands.sort(key=lambda t: (d2(t, anchor), t))
    picked = []
    for t in cands:
        if all(d2(t, p) >= pairwise for p in picked):
            picked.append(t)
            if len(picked) >= n:
                break
    return picked


def fish_spot(G, pg):
    """钓鱼点两档: 严格 (猫 margin37) → 宽松 (猫 >20 实际射程)。
    其余弓恒 margin25 (弓见持械即射)。返回 (F, strict) 或 (None, False)。"""
    for strict in (True, False):
        best = None
        for y in range(0, 13):
            for x in range(7, 19):
                t = (x, y)
                if not wk(t) or MID_BAND(t):
                    continue
                if not (9 <= d2(t, G) <= 16):
                    continue
                ok = True
                for g in pg:
                    if g['type'] == 4 and tuple(g['tile']) == G:
                        continue
                    if g['type'] == 4:
                        if d2(t, tuple(g['tile'])) <= _margin(16):
                            ok = False
                    else:
                        lim = _margin(25) if strict else 20
                        if d2(t, tuple(g['tile'])) <= lim:
                            ok = False
                if ok and (best is None or (d2(t, SQ_ANCHOR), t)
                           < (d2(best, SQ_ANCHOR), best)):
                    best = t
        if best is not None:
            return best, strict
    return None, False


def fish_rear(G, F, posts):
    """拉回点 = 守军射程外 (d²>16) 的帖中离 F 最近者 (最小暴露 pull;
    v6.5 用 max-d² 帖, 饵要横穿 4 格火力, boot1 两饵阵亡主因之一)。"""
    out = [p for p in posts if d2(p, G) > 16]
    pool = out or posts
    return min(pool, key=lambda p: (d2(p, F), d2(p, G)))


def pick_bait(mine, F):
    """[v7.0] 饵=消耗品: 征服者 t9 优先 (射程反击可伤守军→reprisal 链),
    次近战 t3; 弓 t4 绝不做饵 (S6 唯一劳动力)。"""
    for ty in (9, 3):
        cs = [u for u in mine if u['type'] == ty
              and slot_types.get(u['slot']) == ty]
        if cs:
            cs.sort(key=lambda u: (d2(tuple(u['tile']), F), -u['hp']))
            return cs[0]
    return None


def fish_start(g, mine, pg):
    global fish_d
    G = tuple(g['tile'])
    F, strict = fish_spot(G, pg)
    if F is None:
        return False
    b = pick_bait(mine, F)
    if b is None:
        return False
    fish_d = {'tslot': g['slot'], 'ttile': G, 'F': F, 'strict': strict,
              'bait': b['slot'], 'btype': b['type'], 'phase': 'go',
              'bhp': b['hp'], 't0': time.time(), 'att': 1, 'hold0': 0.0,
              'bleed0': 0.0,
              'tag': f'arch@{G}'}
    assign[b['slot']] = F
    set_target(b, F, f'fish go{F}')
    log(f'  钓鱼开局 {fish_d["tag"]} F={F} ({"严格" if strict else "宽松"}) '
        f'饵=s{b["slot"]}(t{b["type"]}) hp{b["hp"]} — 消耗品, 中箭不拉回')
    return True


def fish_tick(mine, pg):
    global fish_d
    now = time.time()
    # slot 主匹配 (combat 差分滤幽灵后 slot 即真身); 紧邻 d²≤9 兜底
    # (slot 压缩漂移)。宽窗 type+近度会把邻位弓误当活目标 → 永不判胜
    t = next((g for g in pg if g['slot'] == fish_d['tslot']
              and g['type'] == 4
              and d2(tuple(g['tile']), fish_d['ttile']) <= 30), None)
    if t is None:
        t = next((g for g in pg if g['type'] == 4
                  and d2(tuple(g['tile']), fish_d['ttile']) <= 4), None)
    if t is None:
        b = next((u for u in mine if u['slot'] == fish_d['bait']
                  and slot_types.get(u['slot']) == fish_d['btype']), None)
        _recall(b, '钓胜')
        log(f'  钓鱼胜 {fish_d["tag"]} 尝试={fish_d["att"]} '
            f'耗时{now - fish_d["t0"]:.0f}s')
        fish_d = None
        return
    fish_d['ttile'] = tuple(t['tile'])
    b = next((u for u in mine if u['slot'] == fish_d['bait']
              and slot_types.get(u['slot']) == fish_d['btype']), None)
    if b is None:
        # 饵亡=预期内消耗; pg 已滤 ghosts, 目标若死上分支已胜 — 这里重开新饵
        log(f'  饵亡(消耗) s{fish_d["bait"]} @{fish_d["tag"]} — 轮换新饵')
        fish_d = None
        return
    env = bool(s5['posts']) and any(d2(fish_d['ttile'], p) <= 16
                                    for p in s5['posts'])
    if fish_d['phase'] in ('go', 'bleed') and env:
        fish_d['phase'] = 'hold'
        fish_d['hold0'] = now
        log(f'  守军入envelope {fish_d["ttile"]} — 风筝冻结 (帖弓报复齐射)')
        return
    if fish_d['phase'] == 'hold':
        if now - fish_d['hold0'] > 90:
            log('  风筝90s未杀 — 复 lure')
            fish_d['phase'] = 'go'
            fish_d['t0'] = now
        return
    if b['hp'] < fish_d['bhp']:
        if fish_d['phase'] == 'go':
            # [v7.0] 中箭即写为消耗: reprisal 覆盖 retask, 拉回是空操作
            # (boot3 双饵全追进口袋死猫口); 任其追, 守军吃反击伤跟进 envelope
            fish_d['phase'] = 'bleed'
            fish_d['bleed0'] = now
            log(f'  饵中箭 hp{b["hp"]} — 写入消耗, 任其 reprisal (不拉回)')
        fish_d['bhp'] = b['hp']
        if fish_d['phase'] == 'bleed':
            if now - fish_d['bleed0'] > 75:
                log(f'  钓鱼放弃 {fish_d["tag"]} (bleed 75s 无果) — skip')
                s5['skip'].add(fish_d['tslot'])
                fish_d = None
        return
    fish_d['bhp'] = b['hp']
    if fish_d['phase'] == 'go':
        if idle(b) and d2(tuple(b['tile']), fish_d['F']) > 2:
            set_target(b, fish_d['F'], '钓重发')
    elif fish_d['phase'] == 'bleed':
        pass    # 中箭后不干预: reprisal 自管, 只盯 [combat] 死亡差分


def ramdrag_tick(rams, pg):
    """冲车诱离: 送 1 冲车到猫圈缘 (d²12-16, 弓圈外), 猫杀冲车后停车不回位
    (r59 实证) → 猫的布阵覆盖被拖走。每次 1 条, 全局上限 2。"""
    global ramdrag_sent
    cats = [g for g in pg if g['type'] == 8]
    if not cats or not rams or ramdrag_sent >= 2:
        return
    for u in rams:
        if not idle(u):
            continue
        c = min(cats, key=lambda g: d2(tuple(g['tile']), tuple(u['tile'])))
        ct = tuple(c['tile'])
        spots = [(ct[0] + dx, ct[1] + dy)
                 for dx in range(-4, 5) for dy in range(-4, 5)
                 if 12 <= dx * dx + dy * dy <= 16]
        spots = [s for s in spots if wk(tuple(s))
                 and all(d2(s, tuple(h['tile'])) > _margin(16)
                         for h in pg if h['type'] == 4)]
        if not spots:
            continue
        s0 = min(spots, key=lambda s: d2(s, tuple(u['tile'])))
        assign[u['slot']] = s0
        set_target(u, s0, f'冲车诱猫{ct}')
        ramdrag_sent += 1
        log(f'  冲车 s{u["slot"]} 诱猫{ct}→{s0} ({ramdrag_sent}/2)')
        break


def s5_pick(mine, ranged, melee, pg):
    """优先级: 可钓的弓 (东→西) → 猫 (猫对猫; fail≥2 且近战≥4 → 围杀;
    弓全 skip 而猫仍在 → 冲车诱离)。
    ('done') = 剩余 pg 全是无害弓 (钓点全封+对 S6 帖/顶廊全部 margin 外,
    且口袋无猫) → 转段 S6 忽略 pg (WIN 只看建筑, 守军不必清光)。"""
    # [v7.0] 口袋弓 (x≤13) 优先钓; 东缘弓 (18-19,14-15, spawn 位) 排最后
    # — 它们对 S6 帖 margin 外不必杀, 别为它们烧饵
    arch = sorted([g for g in pg if g['type'] == 4],
                  key=lambda g: (0 if g['tile'][0] <= 13 else 1,
                                 -g['tile'][0]))
    for a in arch:
        if a['slot'] in s5['skip']:
            continue
        F, strict = fish_spot(tuple(a['tile']), pg)
        if F is not None:
            return ('fish', a, F, strict)
    cats = sorted([g for g in pg if g['type'] == 8],
                  key=lambda g: s5['fail'].get(g['slot'], 0))
    melee_near = [u for u in melee
                  if d2(tuple(u['tile']), DUEL_REST) <= 100]
    for c in cats:
        fails = s5['fail'].get(c['slot'], 0)
        if fails >= 2:
            if len(melee_near) >= 4:
                return ('swarm', c, None, False)
            continue
        return ('cat', c, None, False)
    if cats:
        return ('ramdrag', None, None, False)
    if arch:
        s6_tiles = list(POSTS_B) + list(NORTH_CORRIDOR)
        harmless = all(
            all(d2(tuple(g['tile']), t) > _margin(RNG[g['type']])
                for t in s6_tiles)
            for g in pg)
        if harmless:
            return ('done', None, None, False)
    return (None, None, None, False)


def s5_home(mine, ranged, melee, rams):
    """非交战单位归位 (帖/停车场); fish hold 与围杀期间冻结对应兵种。"""
    if fish_d is not None and fish_d['phase'] == 'hold':
        return
    swarm_on = duel.get('kind') == '近战围杀' and duel.get('tgt') is not None
    busy = set()
    if fish_d is not None:
        busy.add(fish_d['bait'])
    if duel.get('bait') is not None:
        busy.add(duel['bait'])
    if duel.get('shield') is not None:
        busy.add(duel['shield'])
    for u in ranged:
        if u['slot'] in busy or not s5['posts']:
            continue
        p = s5['posts'][u['slot'] % len(s5['posts'])]
        if idle(u) and d2(tuple(u['tile']), p) > 2:
            set_target(u, p, 'S5归位')
    for i, u in enumerate(melee):
        if u['slot'] in busy or swarm_on:
            continue
        p = (s5['park'][i % len(s5['park'])] if s5['park']
             else MEL_PARK[i % len(MEL_PARK)])
        if idle(u) and d2(tuple(u['tile']), p) > 2:
            set_target(u, p, 'S5近战驻')
    for i, u in enumerate(rams):
        if u['slot'] in busy:
            continue
        p = (s5['ramhome'][i % len(s5['ramhome'])] if s5['ramhome']
             else (18, 4))
        if idle(u) and d2(tuple(u['tile']), p) > 2:
            set_target(u, p, 'S5冲车驻')


def set_target(u, tgt, tag=''):
    s = u['slot']
    tgt = clamp(tgt)
    if list(u['target']) == list(tgt):
        return
    rec = issue.get(s)
    if rec and rec[0] == tgt and rec[2] == [u['tile'][0], u['tile'][1]]:
        rec[1] += 1
        if rec[1] >= 8:
            alt = clamp((tgt[0] + (1 if s % 2 else -1),
                         tgt[1] + (1 if s % 3 else -1)))
            send([f'retask {s} {alt[0]} {alt[1]}'])
            issue[s] = [alt, 0, list(u['tile'])]
            log(f'  s{s} STUCK→{alt} {tag}')
        return
    issue[s] = [tgt, 1, list(u['tile'])]
    send([f'retask {s} {tgt[0]} {tgt[1]}'])


def follow_hops(u, hops, tag, cap=None):
    s = u['slot']
    i = hop_i.get(s, 0)
    idl = idle(u)
    lp = lastpos.setdefault(s, [tuple(u['tile']), 0])
    if tuple(u['tile']) == lp[0]:
        lp[1] += 1
    else:
        lp[0], lp[1] = tuple(u['tile']), 0
    stuck = (not idl) and lp[1] >= 20
    while i < len(hops) and d2(tuple(u['tile']), hops[i]) <= 2 and idl:
        i += 1
        lp[1] = 0
        idl = idle(u)
    if cap is not None and i > cap:
        i = cap            # 整队行军: 不得领先于最慢者+2
        idl = idle(u)
    if i >= len(hops):
        hop_i[s] = i
        return True
    hop_i[s] = i
    if stuck:
        i2 = min(i + 1, len(hops) - 1)
        hop_i[s] = i2
        set_target(u, hops[i2], tag + '/冻跳')
        log(f'  s{s} 冻结@{u["tile"]} tgt={u["target"]} →跳{hops[i2]}')
        return False
    set_target(u, hops[i], tag)
    return False


def pick_focus(pg, melee):
    global focus
    if focus is not None:
        near = [g for g in pg if d2(tuple(g['tile']), focus) <= 2]
        if near:
            return tuple(near[0]['tile'])
    cx = sum(u['tile'][0] for u in melee) // len(melee)
    cy = sum(u['tile'][1] for u in melee) // len(melee)
    cats = [g for g in pg if g['type'] == 8
            and d2(tuple(g['tile']), (cx, cy)) <= 36]
    pool = cats or pg
    g0 = min(pool, key=lambda g: d2(tuple(g['tile']), (cx, cy)))
    return tuple(g0['tile'])


def ram_trade(ebs, rams, tag):
    pref = [b for b in ebs if tuple(b['tile']) in {(2, 8), (5, 7)}]
    pool = pref or sorted(ebs, key=lambda b: d2(tuple(b['tile']), (4, 6)))
    for i, u in enumerate(rams):
        bt = tuple(pool[i % len(pool)]['tile'])
        assign[u['slot']] = bt
        set_target(u, bt, tag)


def ram_sweep(ebs, rams, tag, avoid_cats=()):
    """无远程时的纯冲车扫荡: 每条 idle 冲车认领最近的无主敌建筑格
    (retask 建筑格→blocked-arrival 停邻格→auto-chew d²≤9 接管)。
    [v6.6] avoid_cats: 猫圈 (d²≤37) 内建筑一概不认领——猫=最近单位,
    冲车照杀 (r63 实证, v6.4「守军不理冲车」冻结保险烧掉 boot1 三冲车)。"""
    taken = {}
    for u in rams:
        a = assign.get(u['slot'])
        if a:
            taken[tuple(a)] = u['slot']
    sent = 0
    for u in rams:
        if not idle(u):
            continue
        a = assign.get(u['slot'])
        if a and any(d2(tuple(b['tile']), tuple(a)) == 0 for b in ebs):
            continue      # 认领的建筑还立着, 原地啃
        pool = [b for b in ebs if tuple(b['tile']) not in taken] or ebs
        pool = [b for b in pool
                if all(d2(tuple(b['tile']), c) > 37 for c in avoid_cats)]
        if not pool:
            global _last_sweep_warn
            if time.time() - _last_sweep_warn > 15:
                _last_sweep_warn = time.time()
                log(f'  {tag}: 可认领建筑全在猫圈内, 冲车按兵不动 (avoid={avoid_cats})')
            continue
        bt = min(pool, key=lambda b: d2(tuple(b['tile']), tuple(u['tile'])))
        t = tuple(bt['tile'])
        taken[t] = u['slot']
        assign[u['slot']] = t
        set_target(u, t, tag)
        sent += 1


def probe_events(tick, mine, foes):
    """probe: 全单位 (type,tile,target,hp) diff → JSONL。"""
    global probe_last, evf
    snap = {}
    for u in mine + foes:
        snap[f"p{u['p']}s{u['slot']}"] = [u['type'], list(u['tile']),
                                          list(u['target']), u['hp']]
    if evf is None:
        evf = open(EVJ, 'a')
    if probe_last is not None:
        for k, v in snap.items():
            if probe_last.get(k) != v:
                evf.write(json.dumps({'t': tick, 'u': k,
                                      'was': probe_last.get(k), 'now': v}) + '\n')
    probe_last = snap
    evf.flush()


def death_diff(tick, units):
    """双方阵亡差分 (slot 消失即记; 带最后目击 tile/type)。坐标仅提示 (stale)。"""
    global seen_units
    cur = {}
    for u in units:
        cur[(u['p'], u['slot'])] = (u['type'], tuple(u['tile']))
    for k, v in seen_units.items():
        if k not in cur:
            p, s = k
            if p == 0:
                log(f't={tick} ✝(diff) 我方 s{s} t{v[0]} 最后@{v[1]}')
            else:
                log(f't={tick} ☠(diff) 守军 s{s} t{v[0]} 死于@{v[1]}')
    seen_units = cur


log(f'=== m6hdrv v8.0 start poll={POLL} mode={"probe" if PROBE else "main"} '
    f'DRY={DRY} fifo={FIFO} stage={stage} MAIN={_MAIN} ===')
if _MAIN:
    while time.time() - _t0 < TIMEOUT and not _dead:
        r = result()
        if r:
            log('RESULT ' + r)
            break
        stt = aistate()
        if stt is None:
            if _dead:
                break
            continue
        tick = stt['tick']
        mine = [u for u in stt['units'] if u['p'] == 0]
        foes = [u for u in stt['units'] if u['p'] == 1]
        ebs = stt.get('buildingRecs', [])
        if not mine:
            log(f't={tick} 我方全灭(等通用判负)')
            time.sleep(POLL)
            continue
        if not ebs:
            log(f't={tick} 敌建筑清零, 等 20t 计时 WIN')
            time.sleep(POLL)
            continue
        btiles = {tuple(b['tile']) for b in ebs}
        by_type = {}
        for u in mine:
            by_type.setdefault(u['type'], []).append(u)
        ranged = by_type.get(4, []) + by_type.get(8, []) + by_type.get(9, [])
        melee = by_type.get(3, [])
        rams = by_type.get(7, [])
        pg_all = [u for u in foes if PGF(tuple(u['tile']))]
        # [v7.0 死亡真源] [combat] 差分标死 → aistate 幽灵过滤 (只取位置)
        mark_dead_from_combat(pg_all)
        pg = filter_ghosts(pg_all)

        # 防串: slot→type 快照每拍维护, assign 槽位 type 不符即弃
        for u in mine:
            t0 = slot_types.get(u['slot'])
            if t0 is not None and t0 != u['type'] and u['slot'] in assign:
                del assign[u['slot']]
                log(f'  s{u["slot"]} 槽位易主 t{t0}→t{u["type"]}, 弃旧 assign (防串)')
            slot_types[u['slot']] = u['type']

        death_diff(tick, stt['units'])

        # [漂移监测] 口袋区守军布点计数 (S4/S5)
        if stage in ('S4', 'S5'):
            for g in foes:
                t = tuple(g['tile'])
                if t[0] <= 21 and t[1] <= 21:
                    drift_seen[t] = drift_seen.get(t, 0) + 1

        # 死亡率熔断
        now = time.time()
        if prev_my >= 0 and len(mine) < prev_my:
            death_marks.extend([now] * (prev_my - len(mine)))
        prev_my = len(mine)
        death_marks = [t for t in death_marks if now - t <= 45]
        if len(death_marks) >= 3 and breaker_n < 2:
            breaker_n += 1
            death_marks = []
            hold_until = now + 40
            log(f't={tick} !!! 熔断#{breaker_n}: 45s 丢3+ → hold40s '
                f'(my={len(mine)} foe={len(foes)})')
        frozen = breaker_n >= 2
        holding = frozen or now < hold_until

        # probe 停发窗口: 只记录不指挥
        if probe_stop:
            if PROBE and stage in ('S4', 'S5'):
                probe_events(tick, mine, foes)
            if time.time() - probe_stop_wall > 20:
                log('probe 采样完毕, 驱动退出')
                break
            time.sleep(POLL)
            continue

        # 掉队重发 + 硬钉 — S2 引离/S5 全程自管; S6 收廊/有 pg 期间自管 (护报复链)
        guard_off = stage in ('S2', 'S5') \
            or (stage == 'S6' and (s6_phase == 0 or pg))
        if not guard_off:
            for u in mine:
                if pg and stage in ('S6',) and u['type'] == 3:
                    continue
                a = assign.get(u['slot'])
                if not a:
                    continue
                idl = idle(u)
                if idl and d2(tuple(u['tile']), a) > 2:
                    set_target(u, a, f'{stage}/掉队')
                elif not idl and list(u['target']) != list(a):
                    tgt = tuple(u['target'])
                    is_eb = any(d2(tgt, tuple(b['tile'])) == 0 for b in ebs)
                    if not is_eb or d2(tgt, a) > 16:
                        set_target(u, a, f'{stage}/硬钉')

        if stage == 'S0':
            if stage_t == 0:
                log(f't={tick} S0 塔(22,50) standoff my={len(mine)} foe={len(foes)} eb={len(ebs)}')
                for i, u in enumerate(ranged):
                    t = POSTS_S0[i % len(POSTS_S0)]
                    assign[u['slot']] = t
                    set_target(u, t, 'S0')
                for i, u in enumerate(rams):
                    t = RAMS_S0[i % len(RAMS_S0)]
                    assign[u['slot']] = t
                    set_target(u, t, 'S0ram')
                for i, u in enumerate(melee):
                    t = MELEE_S0[i % len(MELEE_S0)]
                    assign[u['slot']] = t
                    set_target(u, t, 'S0mel')
            if TOWER50 not in btiles:
                log(f't={tick} S0 完成: 塔(22,50) 平 (eb={len(ebs)})')
                stage, stage_t, assign, hop_i = 'S1', 0, {}, {}
                continue
        elif stage == 'S1':
            if stage_t == 0:
                log(f't={tick} S1 东道走廊→门南 (整队行军版)')
                for u in mine:
                    hop_i[u['slot']] = 0
            # [v6.6 整队行军] 步速=最慢者+2: 落单者是 boot1 巢穴 2 弓的根因
            cap = min(hop_i.get(u['slot'], 0) for u in mine) + 2
            for u in mine:
                follow_hops(u, HOPS_EAST, 'S1', cap=cap)
            for i, u in enumerate(mine):
                if hop_i.get(u['slot'], 0) >= len(HOPS_EAST):
                    t = STAGING_GATE[i % len(STAGING_GATE)]
                    assign[u['slot']] = t
            arrived = sum(1 for u in mine
                          if d2(tuple(u['tile']), (34, 34)) <= 81)
            all_in = all(hop_i.get(u['slot'], 0) >= len(HOPS_EAST)
                         for u in mine)
            if all_in or stage_t > 80:
                log(f't={tick} S1 完成 {arrived}/{len(mine)} 到门南 '
                    f'(all_in={all_in} st={stage_t})')
                stage, stage_t, assign, hop_i = 'S2', 0, {}, {}
                continue
        elif stage == 'S2':
            cands = [u for u in foes if S2_FOE(tuple(u['tile']))]
            if len(melee) < 2 and not s2d['kill'] and stage_t > 30:
                # [v7.3] 近战不足: 门钉留守不追 (aiEnabled=false 不动), S4 走
                # x29 走廊吃 1-2 下追击可容忍 — 不喂塔
                log(f't={tick} S2 豁免: melee={len(melee)} 不足, 留守门钉 '
                    f'{[tuple(g["tile"]) for g in cands]} — 转 S3')
                stage, stage_t, assign, focus = 'S3', 0, {}, None
                s2d.update(bait=None, aggro=False, wp=0, kill=False,
                           armed=False)
                continue
            if not cands:
                for u in melee:
                    rt = RET_S2[u['slot'] % 2]
                    assign[u['slot']] = rt
                    set_target(u, rt, 'S2清场撤')
                log(f't={tick} S2 完成: 门钉清空 (引离={"成" if s2d["kill"] else "弃"})')
                stage, stage_t, assign, focus = 'S3', 0, {}, None
                s2d.update(bait=None, aggro=False, wp=0, kill=False,
                           armed=False)
                continue
            if stage_t == 0 and melee:
                # [v7.0 引离开局] 饵=离门最近的近战, 其余埋伏 KILL_PARK
                bt = min(melee, key=lambda u: d2(tuple(u['tile']), LURE0))
                s2d['bait'] = bt['slot']
                s2d['aggro'] = False
                s2d['wp'] = 0
                s2d['kill'] = False
                s2d['armed'] = False
                s2d['t0'] = time.time()
                s2d['nudged'] = False
                assign[bt['slot']] = LURE0
                set_target(bt, LURE0, 'S2饵入')
                for i, u in enumerate(melee):
                    if u['slot'] == bt['slot']:
                        continue
                    p = KILL_PARK[i % len(KILL_PARK)]
                    assign[u['slot']] = p
                    set_target(u, p, 'S2埋伏')
                log(f't={tick} S2 引离开局 饵=s{bt["slot"]}→{LURE0} '
                    f'埋伏={len(melee) - 1} 守军={[tuple(g["tile"]) for g in cands]}')
            bait = next((u for u in melee
                         if u['slot'] == s2d.get('bait')
                         and slot_types.get(s2d.get('bait')) == 3), None)
            if bait is None and melee:
                bait = min(melee, key=lambda u: d2(tuple(u['tile']), LURE0))
                s2d['bait'] = bait['slot']
                log(f'  S2 饵亡/换人 → s{bait["slot"]}')
            if not s2d['armed']:
                # 埋伏位维护 (非饵近战)
                for i, u in enumerate(melee):
                    if bait is not None and u['slot'] == bait['slot']:
                        continue
                    p = KILL_PARK[i % len(KILL_PARK)]
                    if idle(u) and d2(tuple(u['tile']), p) > 2:
                        set_target(u, p, 'S2埋伏')
                # [v8.0b] 30s 未上钩: 饵贴进一步 (boot1 三钉对 d²13 干瞪眼
                # 120s; (29,30) 距钉 d²5, 仍在塔程²6 外 (13))
                if not s2d['aggro'] and not s2d.get('nudged') \
                        and bait is not None \
                        and time.time() - s2d['t0'] > 30:
                    s2d['nudged'] = True
                    assign[bait['slot']] = (29, 30)
                    set_target(bait, (29, 30), 'S2饵贴进')
                    log('  S2 饵贴进 (29,30) (30s 未上钩)')
                # aggro 判定: 守军起步 或 饵掉血
                if not s2d['aggro']:
                    moving = [tuple(g['tile']) for g in cands if not idle(g)]
                    if moving or (bait is not None and bait['hp'] < 255):
                        s2d['aggro'] = True
                        s2d['t0'] = time.time()
                        log(f'  S2 守军上钩 (动={moving} '
                            f'饵hp={bait["hp"] if bait else "?"})')
                # 饵风筝: 守军逼近 (d²≤12) 就退下一 wp; 守军远就停在 wp 勾引
                if bait is not None:
                    near = min((d2(tuple(g['tile']), tuple(bait['tile']))
                                for g in cands), default=999)
                    if near <= 12 and s2d['wp'] < len(LURE_WP) - 1:
                        s2d['wp'] += 1
                        log(f'  S2 饵退 {LURE_WP[s2d["wp"]]} (近距d²={near})')
                    tgt_b = LURE_WP[s2d['wp']]
                    assign[bait['slot']] = tgt_b
                    if idle(bait) and d2(tuple(bait['tile']), tgt_b) > 2 \
                            and not (bait['hp'] < 255 and near > 20):
                        set_target(bait, tgt_b, 'S2饵风筝')
                # [v8.0] 转杀唯一条件=全员离塔 d²>24 && 上钩 && melee≥3
                # (近战保全: 两个超时兜底不再硬打——boot3 兜底 -3 近战 =
                # melee=0 死锁根因; 不满足 → 留守豁免转 S3, S4 走廊吃
                # 1-2 下追击可容忍)
                clear = all(d2(tuple(g['tile']), (26, 28)) > 24
                            for g in cands)
                if clear and s2d['aggro'] and len(melee) >= 3:
                    s2d['kill'] = True
                    s2d['armed'] = True
                    log(f'  S2 转杀 (clear={clear} aggro={s2d["aggro"]} '
                        f'melee={len(melee)} '
                        f'守军={[tuple(g["tile"]) for g in cands]})')
                elif (not s2d['aggro']
                      and time.time() - s2d['t0'] > S2T_SILENT) \
                        or (s2d['aggro']
                            and time.time() - s2d['t0'] > S2T_AGGRO):
                    log(f't={tick} S2 兜底豁免: clear={clear} '
                        f'aggro={s2d["aggro"]} melee={len(melee)} — '
                        f'留守门钉转 S3')
                    stage, stage_t, assign, focus = 'S3', 0, {}, None
                    s2d.update(bait=None, aggro=False, wp=0, kill=False,
                               armed=False)
                    continue
            if s2d['kill']:
                # 集火离饵最近者 (护饵); 饵 hp≥120 也参战, 否则继续风筝
                g0 = min(cands, key=lambda g: (
                    d2(g['tile'], tuple(bait['tile'])) if bait else 0,
                    d2(g['tile'], (35, 37))))
                gt = tuple(g0['tile'])
                if focus != gt:
                    focus = gt
                    log(f'  S2 集火 {gt} (余 {len(cands)})')
                for u in melee:
                    if bait is not None and u['slot'] == bait['slot'] \
                            and u['hp'] < 120:
                        continue    # 重伤饵继续风筝 (reprisal 自管)
                    assign[u['slot']] = gt
                    set_target(u, gt, 'S2杀')
        elif stage == 'S3':
            solo = [u for u in foes if d2(tuple(u['tile']), SOLO) <= 9]
            if not solo:
                log(f't={tick} S3 完成: (32,16) 清空')
                stage, stage_t, assign, focus = 'S4', 0, {}, None
                continue
            gt = tuple(solo[0]['tile'])
            if focus != gt:
                focus = gt
                log(f't={tick} S3 集火独钉 {gt}')
            for u in melee:
                if u['hp'] < 150:
                    assign[u['slot']] = (34, 20)
                    set_target(u, (34, 20), 'S3撤')
                else:
                    assign[u['slot']] = gt
                    set_target(u, gt, 'S3焦')
        elif stage == 'S4':
            if stage_t == 0:
                log(f't={tick} S4 北穿 ({len(HOPS_NORTH)} hops)')
                for u in mine:
                    hop_i[u['slot']] = 0
            # [v7.0] mapdump 守军漂移监测 (每 45s, 服务端真值)
            if time.time() - _last_mapdump > 45:
                _last_mapdump = time.time()
                rows = mapdump(os.path.join(W, 'map-s4.txt'))
                if rows:
                    ens = [(x, y) for y, row in rows.items()
                           for x, ch in enumerate(row)
                           if ch == 'e' and x <= 21 and y <= 21]
                    log(f'  [mapdump] 口袋区敌标 ({len(ens)}): {ens}')
            done = 0
            for i, u in enumerate(mine):
                hops = HOPS_NORTH + [STRIP[i % len(STRIP)]]
                if follow_hops(u, hops, 'S4'):
                    done += 1
            if done >= max(1, len(mine) - 1) or stage_t > 60:
                log(f't={tick} S4 完成/超时推进: {done}/{len(mine)}')
                stage, stage_t, assign, hop_i, focus = 'S5', 0, {}, {}, None
                continue
        elif stage == 'S5':
            # [v7.2 顶廊帖扫+盾猫清障] 阶段序:
            #  1) 帖扫: tier0/1 安全帖自动烧建筑 (strict, 永不进猫火+4 内)
            #  2) 停滞 (blocked 残留) → 盾先猫后决斗清最近猫 (塔安全位)
            #  3) 盾位无解 → 近战团杀兜底 (塔清后)
            #  4) blocked 全为口袋外 → 转 S6/S7 收南塔
            cats = [g for g in pg if g['type'] == 8]
            if stage_t == 0:
                s5_wall0 = time.time()
                s5['anchors'] = {g['slot']: tuple(g['tile']) for g in cats}
                s5['stall0'] = time.time()
                s5['stall_sweep'] = 0.0
                s5['swarm'] = False
                log(f't={tick} S5 顶廊帖扫开局 pg={[tuple(g["tile"]) for g in pg]} '
                    f'pgt={[g["type"] for g in pg]} eb={len(ebs)} '
                    f'ranged={len(ranged)} rams={len(rams)} melee={len(melee)}')
                rows = mapdump(os.path.join(W, 'map-s5.txt'), (0, 0, 22, 22))
                if rows:
                    nbad = load_bad_tiles(rows)
                    _last_s5dump = time.time()
                    log(f'  mapdump BAD_TILES 重建={nbad}')
                for i, u in enumerate(mine):
                    p = SWEEP_PARK[i % len(SWEEP_PARK)]
                    assign[u['slot']] = p
                    set_target(u, p, 'S5顶廊')
            # --- 决斗驱动 (盾先猫后) ---
            duel_tws = [tuple(b['tile']) for b in ebs if b['type'] == 12]
            if duel['tgt'] is not None:
                duel_drive(mine, ranged, melee, rams, pg)
                if not cats:
                    log('  决斗胜 (猫清) — 回帖扫')
                    duel = dict(DUEL_EMPTY)
            # --- claims: 每 poll 重算 (cat 死亡/挪窝自动解锁) ---
            # [v8.0] BAD_TILES 周期重建 (30s): 建筑死后格转'.', 释放死建筑
            # 格 → 北廊 y0-2 打开 → (2,5) 西北安全帖 (2,2) 可达 (离线推演:
            # 活建筑自墙 + 口袋弓 d²16 是 (2,5) 假锁的主体)
            if time.time() - _last_s5dump > 30:
                _last_s5dump = time.time()
                rows = mapdump(os.path.join(W, 'map-s5.txt'), (0, 0, 22, 22))
                if rows:
                    nbad = load_bad_tiles(rows)
                    log(f'  [mapdump refresh] BAD_TILES 重建={nbad}')
            seeds = set(SWEEP_PARK) | {tuple(u['tile']) for u in mine}
            claims = sweep_claims(ebs, pg, seeds)
            tws_now = [tuple(b['tile']) for b in ebs if b['type'] == 12]
            blocked = [tuple(b['tile']) for b in ebs
                       if tuple(b['tile']) not in claims]
            posts_now = [v[0] for v in claims.values()]
            # --- 决斗成员不参与帖位分配 ---
            busy = set()
            if duel.get('shield') is not None:
                busy.add(duel['shield'])
            if duel.get('bait') is not None:
                busy.add(duel['bait'])
            # [v8.0c] 帖位延后 2 拍: 先让全员落 y0 驻, 再派帖 — 防 S4 掉队者
            # 从 y1/y2 直插帖位时 BFS 走 y1 掠猫火圈 (boot2 (6,1) 死法)
            if posts_now and stage_t >= 2:
                for i, u in enumerate(ranged):
                    if u['slot'] in busy:
                        continue
                    p = posts_now[i % len(posts_now)]
                    assign[u['slot']] = p
                    if idle(u) and d2(tuple(u['tile']), p) > 2:
                        set_target(u, p, f'S5帖{p}')
            for i, u in enumerate(melee):
                if u['slot'] in busy or s5['swarm']:
                    continue    # [v8.0b] 团杀期驻位循环与贴猫互相 retask
                p = SWEEP_PARK[i % len(SWEEP_PARK)]
                if idle(u) and d2(tuple(u['tile']), p) > 2:
                    set_target(u, p, 'S5驻')
            # [v8.0b 冲车啃塔] 只在帖扫真停滞 (claims+eb 连续 2×M6_STALL 无
            # 变化) 时出动; 有猫时只啃塔 (非塔建筑啃了也引猫停到帖脉上 —
            # boot1: 3 冲车早出动全灭, 猫停 y0 动脉封死全场)
            prog_now = (len(claims), len(ebs))
            if prog_now != s5.get('prog_sig'):
                s5['prog_sig'] = prog_now
                s5['prog_t'] = time.time()
            frozen_for = time.time() - s5.get('prog_t', time.time())
            stall_on = frozen_for > float(os.environ.get('M6_STALL', '20')) * 2
            chew_pools = [b for b in ebs if tuple(b['tile']) not in claims
                          and b['tile'][0] <= 21]
            if any(g['type'] == 8 for g in pg):
                chew_pools = [b for b in chew_pools if b['type'] == 12]
            chew_pools.sort(key=lambda b: (0 if b['type'] == 12 else 1,
                                           d2(tuple(b['tile']), (10, 1))))
            chewed_log = [0]
            for i, u in enumerate(rams):
                if u['slot'] in busy or not idle(u):
                    continue
                if RAMCHEW and stall_on and chew_pools:
                    # 全部冲车集中第一目标 (塔优先): 集中啃+引猫漂移 —
                    # 猫杀冲车后停在杀点, claims 下拍自动重算 (自保护)
                    bt = tuple(chew_pools[0]['tile'])
                    assign[u['slot']] = bt
                    set_target(u, bt, 'S5冲车啃塔')
                    if not chewed_log[0]:
                        chewed_log[0] = 1
                        log(f'  冲车出动啃塔: {bt} '
                            f'(frozen {frozen_for:.0f}s)')
                    continue
                set_target(u, (17, 3), 'S5冲车驻')
            # --- 停滞 → [v8.0 顺序定死] 团杀猫A (melee≥4 且 A 离活塔远)
            #     → 盾决斗猫B → (无猫仍 blocked) 冲车啃塔 ---
            if blocked and duel['tgt'] is None and not s5['swarm']:
                if not s5['stall_sweep']:
                    s5['stall_sweep'] = time.time()
                    log(f'  blocked={blocked} — 停滞计时开始 (M6_STALL='
                        f'{os.environ.get("M6_STALL", "20")})')
                if time.time() - s5['stall_sweep'] > \
                        float(os.environ.get('M6_STALL', '20')):
                    # 团杀 gate: 目标猫离全部活塔 >24 (程²6 外 >2 格) —
                    # 塔火下团杀=1.5s 团灭 (boot1 接管实录), 门控按目标算
                    swarmable = [g for g in cats
                                 if all(d2(tuple(g['tile']), t) > 24
                                        for t in tws_now)]
                    if len(melee) >= 4 and swarmable:
                        s5['swarm'] = True
                        s5['swarm_cats0'] = len(cats)
                        a0 = min(swarmable, key=lambda g:
                                 d2(tuple(g['tile']), DUEL_REST))
                        log(f'  停滞 → 近战团杀开局: 集火猫A'
                            f'{tuple(a0["tile"])} (melee={len(melee)})')
                    elif cats:
                        c0 = min(cats,
                                 key=lambda g: d2(tuple(g['tile']), (12, 1)))
                        opened = duel_open_cat(c0, ranged, melee, pg,
                                               rams=rams)
                        if opened:
                            s5['stall_sweep'] = time.time()
                        elif time.time() - s5['stall0'] > 40:
                            log(f'  盾位无解且近战不足 '
                                f'(melee={len(melee)} swarmable='
                                f'{len(swarmable)} tws={tws_now})')
                    elif time.time() - s5['stall0'] > 40:
                        log(f'  blocked={blocked} 无猫可决斗 — 冲车啃塔'
                            f'{"待命" if not RAMCHEW else ""} '
                            f'(melee={len(melee)} rams={len(rams)})')
            elif not blocked:
                s5['stall_sweep'] = 0.0
            # --- 团杀期贴猫 [v8.0: 全部近战集火猫A一只; A 死即收队转盾决斗B] ---
            if s5['swarm']:
                if not cats or not melee:
                    s5['swarm'] = False
                    log('  猫全灭/近战耗尽 — 回帖扫')
                elif len(cats) < s5.get('swarm_cats0', 9):
                    s5['swarm'] = False
                    s5['stall_sweep'] = time.time()
                    log('  团杀 A 得手 — 收队, 猫B 转盾决斗 (1:0)')
                else:
                    cats.sort(key=lambda g: d2(tuple(g['tile']), DUEL_REST))
                    gt = tuple(cats[0]['tile'])
                    for i, u in enumerate(melee):
                        rx, ry = SWARM_RING[i % len(SWARM_RING)]
                        rt = (gt[0] + rx, gt[1] + ry)
                        if not wk(tuple(rt)):
                            rt = gt
                        if d2(tuple(u['tile']), gt) > 1:
                            set_target(u, rt, '团杀A')
            if blocked and time.time() - s5['stall0'] > 45:
                s5['stall0'] = time.time()
                log(f'  blocked={blocked} claims={len(claims)} cats='
                    f'{[tuple(g["tile"]) for g in cats]} rams={len(rams)} '
                    f'melee={len(melee)} swarm={s5["swarm"]} '
                    f'duel={duel["tgt"] is not None}')
            # 转段: blocked 全为口袋外建筑 (门塔/南塔, 洪泛域 x≤21 够不到)
            if blocked and all(b[0] > 21 for b in blocked) \
                    and not s5['swarm'] and duel['tgt'] is None:
                log(f't={tick} S5 完成: 口袋清/解锁, blocked={blocked} — 转段')
                stage, stage_t, assign, focus = 'S6', 0, {}, None
                s6_ignore_pg = True
                s6_phase = 0
                continue
            if not ebs:
                log(f't={tick} S5 完成: 敌建筑清零, 等 WIN')
                time.sleep(POLL)
                continue
        elif stage == 'S6':
            rest = [b for b in ebs if tuple(b['tile']) in SOFT_ALL]
            if not rest and s6_phase >= 1:
                log(f't={tick} S6 完成: NW 全软+塔2 平')
                stage, stage_t, assign, sent_s6_posts = 'S7', 0, {}, False
                continue
            if pg and not s6_ignore_pg:
                # 漏网/回袋守军: 继续决斗梯, 帖不发; hold 时撤走廊+冲车保险
                if not holding:
                    duel_supervise_s6(mine, ranged, melee, rams, pg)
                else:
                    hold_retreat(mine)
                    if breaker_n >= 2 and rams:
                        ram_sweep(ebs, rams, '冻结冲车扫',
                                  avoid_cats=[tuple(g['tile']) for g in pg
                                              if g['type'] == 8])
                if stage_t == 0:
                    log(f't={tick} S6 pg={len(pg)} 决斗续行')
            elif s6_phase == 0:
                s6_phase = 1
                log(f't={tick} S6 收顶廊 (y<=2, 防塔火穿越)')
                for i, u in enumerate(mine):
                    t = NORTH_CORRIDOR[i % len(NORTH_CORRIDOR)]
                    assign[u['slot']] = t
                    set_target(u, t, 'S6廊')
            else:
                arrived = sum(1 for u in mine if tuple(u['tile'])[1] <= 2
                              and idle(u))
                if arrived >= max(1, (len(mine) * 3) // 4) or stage_t > 80:
                    ram_trading = False
                    if rest and not sent_s6_posts:
                        sent_s6_posts = True
                        log(f't={tick} S6 帖扫荡 NW 全软+塔2 (到廊 {arrived}/{len(mine)})')
                        for i, u in enumerate(ranged):
                            t = POSTS_B[i % len(POSTS_B)]
                            assign[u['slot']] = t
                            set_target(u, t, 'S6帖')
                        for i, u in enumerate(rams):
                            t = [(3, 4), (6, 5), (6, 3)][i % 3]
                            assign[u['slot']] = t
                            set_target(u, t, 'S6ram')
                        for u in melee:
                            assign[u['slot']] = (6, 1)
                            set_target(u, (6, 1), 'S6mel')
                    elif rest and not ranged:
                        ram_sweep(ebs, rams, 'S6ram扫')
        elif stage == 'S7':
            rest_t = [b for b in ebs if b['type'] == 12]
            if not rest_t:
                log(f't={tick} S7 完成: 全塔平 eb={len(ebs)}')
                stage = 'S8'
                continue
            if stage_t == 0:
                log(f't={tick} S7 收尾残余塔 {[tuple(b["tile"]) for b in rest_t]}')
                sent_s7 = False
            if not ranged:
                ram_sweep(ebs, rams, 'S7ram扫')
            elif not sent_s7:
                sent_s7 = True
                tiles_s = [tuple(b['tile']) for b in rest_t]
                for i, u in enumerate(ranged):
                    bt = tiles_s[i % len(tiles_s)]
                    t = TOWER_POSTS.get(bt)
                    if t is None:
                        t = clamp((bt[0] + 3, bt[1] - 1)) if bt[1] > 8 else \
                            clamp((bt[0] - 3, bt[1] + 1))
                    assign[u['slot']] = t
                    set_target(u, t, 'S7帖')
                for i, u in enumerate(rams):
                    bt = tiles_s[i % len(tiles_s)]
                    t = clamp((bt[0] + 1, bt[1] + 1))
                    assign[u['slot']] = t
                    set_target(u, t, 'S7ram')
                for u in melee:
                    assign[u['slot']] = (5, 11)
                    set_target(u, (5, 11), 'S7mel')
        else:  # S8 兜底
            cur = {tuple(b['tile']) for b in ebs}
            new = cur - remaining
            if new:
                log(f't={tick} S8 残余建筑 {sorted(cur)}')
            remaining = cur
            if not ranged:
                ram_sweep(ebs, rams, 'S8ram扫')
            for b in ebs:
                bt = tuple(b['tile'])
                cover = sum(1 for u in ranged if d2(tuple(u['tile']), bt) <= 16)
                if cover == 0:
                    idle_far = [u for u in ranged
                                if d2(tuple(u['tile']), bt) > 16
                                and idle(u)]
                    if idle_far:
                        u0 = min(idle_far, key=lambda u: d2(tuple(u['tile']), bt))
                        dx = 3 if bt[0] > 8 else -3 if bt[0] < 8 else 0
                        dy = 3 if bt[1] > 8 else -3 if bt[1] < 8 else 0
                        t = clamp((bt[0] + (dx or 4), bt[1] + (dy or -4)))
                        set_target(u0, t, f'S8补{bt}')

        stage_t += 1
        if PROBE and stage in ('S4', 'S5'):
            probe_events(tick, mine, foes)
        if PROBE and stage == 'S5' and not probe_stop \
                and time.time() - s5_wall0 > PROBE_CAP:
            probe_stop = True
            probe_stop_wall = time.time()
            log(f't={tick} PROBE 观察窗满 ({PROBE_CAP:.0f}s) — 停发指令, 再采 20s')

        if time.time() - _last_sum > 5.0:
            _last_sum = time.time()
            avg = sum(u['hp'] for u in mine) // len(mine)
            log(f't={tick} {stage} my={len(mine)} foe={len(foes)} eb={len(ebs)} '
                f'avg_hp={avg} retask={_n} st={stage_t}'
                + (f' MELT#{breaker_n}' if breaker_n else ''))

        time.sleep(POLL)

    if evf is not None:
        evf.close()
    r = result()
    log('RESULT ' + r if r else f'NO RESULT dead={_dead} {time.time() - _t0:.0f}s retask={_n}')
