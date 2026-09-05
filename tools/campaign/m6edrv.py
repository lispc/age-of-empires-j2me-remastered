#!/usr/bin/env python3
"""m6e 驱动 v6.4 = v6.3 + BUGS-m6e 三修 (帖诱反杀/猫对猫口径版)。

 v6.4 修法对照 BUGS-m6e「bug/配方清单」:
 [1 auto-home] changed 只认掉血 (b['hp'] < bhp)——回血不再重置 quiet,
   静默 35 拍后 home 能真正出手 (v6.3: 饵回血当 changed → home 永不发)。
 [2 猫触发口径] TRIG={4:16, 8:20} 与威胁余量 RNG={4:16, 8:25} 分离:
   meeting_zone 触发用 TRIG (猫实测 d²=20 开火/d²=25 不开火, [20,25) 口径),
   zone_ok/wander_ok 的安全余量仍按上限 25 派生 (v6.3: 触发=25 → 会合区
   落在猫射程外干瞪眼, boot2 死锁根因)。
 [3 S1 尾段 hops 东移] (41,43)(40,41)(39,40) → (43,44)(43,40)(41,38),
   远离巢穴弓圈 (31-33,41-43)——巢穴报复追击 1:1 两 boot 复现, BFS 偏移
   +拥挤让纵队边缘擦圈, m6d 四连零损是运气尾巴。

 v6.3 = v6.2 + BUGS-m6d 五修 (决斗梯 1v1 收敛版)。

 修法对照 BUGS-m6d「事故与 bug」:
 [1 自检隔离] M6_DRY=1 时 FIFO 强制指向假路径且 send 只落日志——任何加载
   自检/dry-run 都不可能碰到活局 fifo (m6d 事故: exec_module 连活局发 14 条)。
 [2 alive 误判] 决斗目标改「type+猎物追踪」判定: 每拍在 d²≤20 内找同 type
   且 hp 差 ≤50 的守军更新猎物位/血; 找不到=真死。另加 120s 无进展强制重选。
 [3 梯位=会合区] 梯位改为动态会合区: 在目标射程内 (d²≤16) 且对其余活守军
   处于「射程并集外>1格」(弓>25/猫>37, 含±1游走余量), 优先游走安全格。
   饵入梯后 free-fight 不钉位 (报复追击是位移机制, 钉位=幻想); 击杀点由
   会合区约束离其余守军并集>1格。杀完 recall REST。
   击杀序 (zone_calc.py 离线标定): A1(8,5)弓→C1(5,5)猫→C2(3,7)猫→
   A2(6,7)弓→A3(4,9)弓。猫不先死则弓的会合区全被猫环罩住; 猫目标=近战
   围杀 (散环入位, 不再用冲车盾——冲车不被守军索敌, 盾是虚设, 留作 S6 扫楼)。
 [4 slot 防串] 决斗入口/猎物追踪都带 (slot→type) 快照校验, 槽位易主即弃。
 [5 熔断 hold] 45s 丢≥3 熔断后姿势=全军撤 x16 走廊 (守军胜后会前压,
   原地 hold=送), hold 结束后决斗状态机按当前局面重选。
 附: S6 起冲车全程 ram_sweep 保险——守军不理冲车 (probe 实证), 即使决斗
   受挫, 冲车也能无干扰啃平全部建筑 (WIN 条件只看建筑)。
 S0-S4 = v5.5 原样 (四轮零损基线)。M6_MODE=probe 兼容保留。
"""
import json
import math
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6e'
DRY = os.environ.get('M6_DRY', '') == '1'
DRY_STATE = os.environ.get('M6_DRY_STATE', '')
FIFO = os.path.join(W, 'fifo')
if DRY:
    # [自检隔离] 假路径: 即使闸失效也绝无可能写到活局 fifo
    FIFO = '/tmp/aoe-camp/m6e/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M6_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M6_TIMEOUT', '1500'))
PROBE = os.environ.get('M6_MODE', 'main') == 'probe'
PROBE_CAP = float(os.environ.get('M6_PROBE_CAP', '210'))
EVJ = os.path.join(W, 'probe-events.jsonl')

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

HOPS_EAST = [(18, 55), (26, 49), (31, 49), (37, 49), (41, 47), (43, 44),
             (43, 40), (41, 38), (38, 37), (36, 36), (35, 35)]
STAGING_GATE = [(34, 34), (35, 34), (33, 34), (34, 35), (35, 33), (36, 34),
                (33, 35), (36, 35), (35, 32), (34, 36), (33, 33), (36, 36),
                (34, 33), (35, 31), (33, 36), (36, 33), (34, 32)]
GATE_BOX = lambda t: 25 <= t[0] <= 30 and 26 <= t[1] <= 32
SOLO = (32, 16)
RET_S2 = [(34, 36), (35, 33)]

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
PGF = lambda t: t[0] <= 16 and t[1] <= 16

# ---------- S5 决斗梯 (v6.3 会合区版) ----------
# 方阵 (开局/整队用): x≥13 全部在 A1(8,5) 射程并集外 (d²(13,y)≥26 > 25)
SQ_FRONT = [(13, 4), (13, 5), (13, 6)]
SQ_MID = [(14, 4), (14, 5), (14, 6)]
SQ_BACK = [(15, 4), (15, 5), (15, 6)]
DUEL_REST = (13, 6)
# [梯位=会合区] 两套口径 (v6.4 分离, r62+m6e 校准):
#   RNG=威胁余量口径 (安全侧, 按上限派生 margin): 敌弓 ≤16 / 敌猫 ≤25;
#   TRIG=交火触发口径 (实测开火下界): 猫 d²=20 开火 / d²=25 不开火 → 20。
# 「并集外>1格」= d² > r²+2r+1 → 弓 >25 / 猫 >37 (含 ±1 游走余量)
RNG = {4: 16, 8: 25}
TRIG = {4: 16, 8: 20}


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

# 击杀序 (zone_calc.py 离线标定): A1 东缘弓独占区(probe 实证) → 两猫(近战围杀,
# 猫不死则弓会合区全被猫环罩住) → A2 → A3 (全猫死后 zone=(10,7)/(8,9) 游走安全)
DUEL_ORDER = [((8, 5), 4, 'A1'), ((5, 5), 8, 'C1'), ((3, 7), 8, 'C2'),
              ((6, 7), 4, 'A2'), ((4, 9), 4, 'A3')]
# 近战围杀散环 (猫溅射 ≥2.2 格, 同格堆叠=全吃; 散环入位分摊)
# 顺序按 NW 口袋可走性调优: 东/西/南/东南/西南 优先, 北向多建筑占格
SWARM_RING = [(1, 0), (-1, 0), (0, 1), (1, 1), (-1, 1), (0, -1), (1, -1),
              (-1, -1)]
# [熔断 hold] 全军撤 x16 走廊 (离口袋家 ≥8 格, S4 实证零损距离; 守军胜后前压,
# 原地 hold=白送)
CORRIDOR_REST = [(16, 4), (16, 5), (16, 6), (17, 6), (16, 7), (17, 7),
                 (16, 8), (15, 4), (16, 3), (15, 3), (16, 2), (17, 8)]
# 近战停车场 (base.aoesave 连通域实证)
MEL_PARK = [(16, 6), (17, 6), (16, 7), (17, 7), (16, 8)]
# S6 两段式: 先收 y<=2 顶廊 (塔(2,8)/(5,7)/(6,4) 程²<=6 全部罩不到), 再帖扫
NORTH_CORRIDOR = [(14, 0), (15, 0), (16, 0), (17, 0), (14, 1), (15, 1),
                  (16, 1), (17, 1), (14, 2), (15, 2), (16, 2), (17, 2),
                  (13, 1), (12, 1), (13, 2)]

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
slot_types = {}          # slot -> 最近一次 type 快照 (防 slot 压缩串位)
seen_units = {}          # (p,slot) -> (type, tile) 死亡差分
duel = {'tgt': None, 'gtype': None, 'tag': None, 'qt': None,
        'qhp': 255, 'qhp0': 255, 'bait': None, 'btype': None,
        'kind': None, 'phase': None, 'btile': None, 'bhp': 255,
        'quiet': 0, 'home_n': 0, 't0': 0.0, 'prog': 0.0}
s6_phase = 0
s5_wall0 = 0.0
probe_stop = False
probe_stop_wall = 0.0
probe_last = None
evf = None


def sq_posts():
    return (list(SQ_FRONT), list(SQ_MID), list(SQ_BACK))


def post_square(ranged):
    fr, md, bk = sq_posts()
    cats = [u for u in ranged if u['type'] == 8]
    oth = [u for u in ranged if u['type'] not in (4, 8)]
    arch = [u for u in ranged if u['type'] == 4]
    for i, u in enumerate(cats):
        t = fr[i % 3]
        assign[u['slot']] = t
        set_target(u, t, 'S5阵猫')
    for i, u in enumerate(oth):
        t = bk[i % 3]
        assign[u['slot']] = t
        set_target(u, t, 'S5阵特')
    for i, u in enumerate(arch):
        t = (md + bk)[i % 6]
        assign[u['slot']] = t
        set_target(u, t, 'S5阵弓')


DUEL_EMPTY = {'tgt': None, 'gtype': None, 'tag': None, 'qt': None,
              'qhp': 255, 'qhp0': 255, 'bait': None, 'btype': None,
              'kind': None, 'phase': None, 'btile': None, 'bhp': 255,
              'quiet': 0, 'home_n': 0, 't0': 0.0, 'prog': 0.0}


def hold_retreat(mine):
    """[熔断 hold 修法] 全军撤 x16 走廊: 守军胜后会成波前压, 原地 hold=白送。
    assign 一次性改写, 之后仅对 idle 且离位者重发 (防 retask 风暴)。"""
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
    """会合区: 目标射程内 (触发交火) + 其余守军并集外>1格;
    优先游走安全格, 次选离 REST 近的。无解返回 None。"""
    r = TRIG.get(ttype, 16)
    cands = [m for m in WALK_NW
             if 4 <= d2(m, ttile) <= r and zone_ok(m, others)]
    if not cands:
        return None
    cands.sort(key=lambda m: (0 if wander_ok(m, others) else 1,
                              d2(m, DUEL_REST)))
    return cands[0]


def _others_now(pg, skip_tile):
    return [(tuple(g['tile']), g['type']) for g in pg
            if tuple(g['tile']) != skip_tile]


def track_quarry(pg):
    """[alive 误判修法] 猎物追踪: 同 type + 邻域 d²≤20;
    优先「正在掉血」者; 未掉血者须贴脸 (d²≤2) 才续认——同类邻陪在旁边
    不算猎物 (旧 bug: 目标游走/死亡后误认邻居)。None=猎物已死。"""
    qhp = duel.get('qhp', 255)
    qhp0 = duel.get('qhp0', 255)   # 注册时血: 「掉过血」的稳定判据
    cands = [g for g in pg if g['type'] == duel['gtype']
             and d2(tuple(g['tile']), duel['qt']) <= 20]
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


def _duel_win(pg, melee, b):
    log(f'  决斗胜: {duel["tag"]}{duel["qt"]}({duel["kind"]}) '
        f'耗时{time.time() - duel["t0"]:.0f}s pg余={len(pg)}')
    if b is not None:
        assign[b['slot']] = DUEL_REST
        set_target(b, DUEL_REST, '饵收兵')
    if duel['kind'] == '近战围杀':
        for u in melee:
            a = MEL_PARK[u['slot'] % len(MEL_PARK)]
            assign[u['slot']] = a
            set_target(u, a, '决斗收兵')
    _duel_reset('胜')


def _swarm_tick(melee):
    """近战围杀: 散环入位 (猫溅射≥2.2格, 同格堆叠=全吃);
    猫挪位 (报复位移) 时 idle 偏离者重发。"""
    qt = duel['qt']
    for i, u in enumerate(melee):
        rx, ry = SWARM_RING[i % len(SWARM_RING)]
        rt = (qt[0] + rx, qt[1] + ry)
        if tuple(rt) not in WALK_NW:
            rt = qt
        assign[u['slot']] = rt
        if idle(u) and d2(tuple(u['tile']), rt) > 2:
            set_target(u, rt, '围杀')
        elif not idle(u) and list(u['target']) != list(rt) \
                and d2(tuple(u['tile']), rt) > 4:
            set_target(u, rt, '围杀修')


_freeze_last = 0.0


def build_plan(pg):
    """[击杀序] DUEL_ORDER 一一认领 (同 type 内最近 nominal 者认领该序位),
    序外游走出窗的守军兜底追加。返回 [(guard, tag)] 按击杀序排列。"""
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


def duel_supervise(mine, ranged, melee, rams, pg):
    """v6.3 决斗梯 (1v1 收敛版):
    - 弓目标: 猫饵 rest→会合区 (目标射程内+其余守军并集外>1格), 入位后
      free-fight 不钉位 (报复追击是位移机制; 猫 112×3 vs 弓小箭收敛稳赢)。
    - 猫目标: 近战散环围杀 (不用冲车盾——守军不索敌冲车, 盾是虚设)。
    - [alive 误判修法] 猎物 type+邻域+掉血追踪, 120s 无进展强制重选。
    - [slot 防串] 饵槽位每拍 type 快照校验。"""
    global duel, _freeze_last
    now = time.time()
    if duel['tgt'] is not None:
        q = track_quarry(pg)
        b = next((x for x in mine if x['slot'] == duel['bait']
                  and slot_types.get(duel['bait']) == duel['btype']),
                 None) if duel['bait'] is not None else None
        if q is None:
            _duel_win(pg, melee, b)
            return
        prev_qt, prev_qhp = duel['qt'], duel['qhp']
        duel['qt'], duel['qhp'] = tuple(q['tile']), q['hp']
        if duel['qt'] != prev_qt or duel['qhp'] < prev_qhp:
            duel['prog'] = now
        if now - duel['prog'] > 120:
            log(f'  决斗超时(120s无进展) {duel["tag"]}@{duel["qt"]} — 强制重选')
            _duel_reset('超时')
            return
        if duel['kind'] == '近战围杀':
            if not melee:
                log(f'  围杀步兵耗尽 @{duel["tag"]} — 弃攻重选')
                _duel_reset('无步兵')
                return
            _swarm_tick(melee)
            return
        if b is None:
            log(f'  饵 s{duel["bait"]} 阵亡/易主 @决斗 {duel["tag"]} — 重选')
            _duel_reset('饵亡')
            return
        if duel['phase'] == 'rest':
            if idle(b) and d2(tuple(b['tile']), DUEL_REST) <= 2:
                others = _others_now(pg, duel['qt'])
                m = meeting_zone(duel['qt'], duel['gtype'], others)
                if m is None:
                    # 守军游走后原有会合区被封死 → 复位重选 (按新局面重算)
                    _duel_reset('会合区失效')
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
        # phase == 'fight': free-fight 不钉位; 只在 12s 全静默时准会合区地
        # 向猎物 home 1 格 (≤3 次), home 落点同样过会合区校验
        # (v6.4: changed 只认掉血——饵回血也算 changed 会把 quiet 清零,
        # home 永远发不出去, 见 BUGS-m6e #1)
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
            if tuple(nb) in WALK_NW and zone_ok(nb, others):
                duel['btile'] = nb
                duel['home_n'] += 1
                assign[b['slot']] = nb
                set_target(b, nb, f'饵home{nb}')
                log(f'  饵 home→{nb} (12s无火, 会合区重算)')
            else:
                log(f'  home→{nb} 落会合区外, 放弃 (待超时重选)')
        return
    if not pg:
        return
    # ---- 决斗开局: 按击杀序找第一个「可行」目标 ----
    for tgt, tag in build_plan(pg):
        tt = tuple(tgt['tile'])
        others = [(tuple(g['tile']), g['type']) for g in pg if g is not tgt]
        cats = sorted([u for u in ranged if u['type'] == 8],
                      key=lambda u: -u['hp'])
        if tgt['type'] == 4:
            if not cats:
                continue
            zone = meeting_zone(tt, 4, others)
            if zone is None:
                log(f'  {tag}{tt} 弓: 会合区无解 others='
                    f'{[(t, ty) for (t, ty) in others]} — 试下个目标')
                continue
            bait = cats[0]
            # [slot 防串] 决斗入口 (slot→type) 快照校验
            if slot_types.get(bait['slot']) != 8:
                log(f'  s{bait["slot"]} 入口串位 '
                    f'(快照t{slot_types.get(bait["slot"])}) — 本拍弃')
                return
            assign[bait['slot']] = DUEL_REST
            set_target(bait, DUEL_REST, '饵收柱')
            duel = {'tgt': tt, 'gtype': 4, 'tag': tag, 'qt': tt,
                    'qhp': tgt['hp'], 'qhp0': tgt['hp'],
                    'bait': bait['slot'], 'btype': 8, 'kind': '猫饵对射', 'phase': 'rest', 'btile': None,
                    'bhp': bait['hp'], 'quiet': 0, 'home_n': 0,
                    't0': time.time(), 'prog': time.time()}
            log(f'  决斗开局: {tag}{tt}(t4,hp{tgt["hp"]}) 饵=s{bait["slot"]} '
                f'rest→会合区')
            return
        if tgt['type'] == 8:
            if melee:
                duel = {'tgt': tt, 'gtype': 8, 'tag': tag, 'qt': tt,
                        'qhp': tgt['hp'], 'qhp0': tgt['hp'], 'bait': None,
                        'btype': None, 'kind': '近战围杀', 'phase': 'rush', 'btile': None,
                        'bhp': 255, 'quiet': 0, 'home_n': 0,
                        't0': time.time(), 'prog': time.time()}
                log(f'  围杀开局: {tag}{tt}(t8,hp{tgt["hp"]}) '
                    f'melee={len(melee)}')
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
                        'bait': bait['slot'], 'btype': 8, 'kind': '猫对猫硬换', 'phase': 'rest', 'btile': None,
                        'bhp': bait['hp'], 'quiet': 0, 'home_n': 0,
                        't0': time.time(), 'prog': time.time()}
                log(f'  猫对猫开局: {tag}{tt}(t8) 饵=s{bait["slot"]} '
                    f'(1:1硬换, 无近战)')
                return
    if time.time() - _freeze_last > 10:
        _freeze_last = time.time()
        log(f'  击杀序内无可行目标 (pg={[(tuple(g["tile"]), g["type"]) for g in pg]}'
            '), 冻结等待 (10s 节流)')


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


def follow_hops(u, hops, tag):
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


def ram_sweep(ebs, rams, tag):
    """无远程时的纯冲车扫荡: 每条 idle 冲车认领最近的无主敌建筑格
    (retask 建筑格→blocked-arrival 停邻格→auto-chew d²≤9 接管)。"""
    taken = {}
    for u in rams:
        a = assign.get(u['slot'])
        if a:
            taken[tuple(a)] = u['slot']
    for u in rams:
        if not idle(u):
            continue
        a = assign.get(u['slot'])
        if a and any(d2(tuple(b['tile']), tuple(a)) == 0 for b in ebs):
            continue      # 认领的建筑还立着, 原地啃
        pool = [b for b in ebs if tuple(b['tile']) not in taken] or ebs
        bt = min(pool, key=lambda b: d2(tuple(b['tile']), tuple(u['tile'])))
        t = tuple(bt['tile'])
        taken[t] = u['slot']
        assign[u['slot']] = t
        set_target(u, t, tag)


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
    """双方阵亡差分 (slot 消失即记; 带最后目击 tile/type)。"""
    global seen_units
    cur = {}
    for u in units:
        cur[(u['p'], u['slot'])] = (u['type'], tuple(u['tile']))
    for k, v in seen_units.items():
        if k not in cur:
            p, s = k
            if p == 0:
                log(f't={tick} ✝ 我方 s{s} t{v[0]} 最后@{v[1]}')
            else:
                log(f't={tick} ☠ 守军 s{s} t{v[0]} 死于@{v[1]}')
    seen_units = cur


log(f'=== m6edrv v6.4 start poll={POLL} mode={"probe" if PROBE else "main"} '
    f'DRY={DRY} fifo={FIFO} stage={stage} ===')
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
    pg = [u for u in foes if PGF(tuple(u['tile']))]

    # 防串: slot→type 快照每拍维护, assign 槽位 type 不符即弃
    for u in mine:
        t0 = slot_types.get(u['slot'])
        if t0 is not None and t0 != u['type'] and u['slot'] in assign:
            del assign[u['slot']]
            log(f'  s{u['slot']} 槽位易主 t{t0}→t{u['type']}, 弃旧 assign (防串)')
        slot_types[u['slot']] = u['type']

    death_diff(tick, stt['units'])

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

    # 掉队重发 + 硬钉 — S5 全程自管; S6 收廊/有 pg 期间自管 (护报复链)
    guard_off = stage == 'S5' or (stage == 'S6' and (s6_phase == 0 or pg))
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
            log(f't={tick} S1 东道走廊→门南')
            for u in mine:
                hop_i[u['slot']] = 0
        for u in mine:
            follow_hops(u, HOPS_EAST, 'S1')
        for i, u in enumerate(mine):
            if hop_i.get(u['slot'], 0) >= len(HOPS_EAST):
                t = STAGING_GATE[i % len(STAGING_GATE)]
                assign[u['slot']] = t
        arrived = sum(1 for u in mine
                      if d2(tuple(u['tile']), (34, 34)) <= 81)
        if arrived >= max(1, (len(mine) * 3) // 4):
            log(f't={tick} S1 完成 {arrived}/{len(mine)} 到门南')
            stage, stage_t, assign, hop_i = 'S2', 0, {}, {}
            continue
    elif stage == 'S2':
        cands = [u for u in foes if GATE_BOX(tuple(u['tile']))]
        if not cands:
            for u in melee:
                rt = RET_S2[u['slot'] % 2]
                assign[u['slot']] = rt
                set_target(u, rt, 'S2清场撤')
            log(f't={tick} S2 完成: 门钉清空 (foe={len(foes)})')
            stage, stage_t, assign, focus = 'S3', 0, {}, None
            continue
        g0 = min(cands, key=lambda g: d2(g['tile'], (34, 34)))
        gt = tuple(g0['tile'])
        if focus != gt:
            focus = gt
            log(f't={tick} S2 集火门钉 {gt}')
        for u in melee:
            if u['hp'] < 150:
                rt = RET_S2[u['slot'] % 2]
                assign[u['slot']] = rt
                set_target(u, rt, 'S2撤')
            else:
                assign[u['slot']] = gt
                set_target(u, gt, 'S2焦')
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
        if stage_t == 0:
            s5_wall0 = time.time()
            log(f't={tick} S5 决斗梯开局 pg={[tuple(g["tile"]) for g in pg]} '
                f'pgt={[g["type"] for g in pg]} ranged={len(ranged)} '
                f'rams={len(rams)} melee={len(melee)}')
            for i, u in enumerate(melee):
                t = MEL_PARK[i % len(MEL_PARK)]
                assign[u['slot']] = t
                set_target(u, t, 'S5押后')
            post_square(ranged)
            breaker_n = 0
            death_marks = []
        if not pg:
            log(f't={tick} S5 完成: 口袋守军清空 (my={len(mine)})')
            stage, stage_t, assign, focus = 'S6', 0, {}, None
            s6_phase = 0
            duel = dict(DUEL_EMPTY)
            continue
        ram_trading = False
        if holding:
            # [熔断 hold 修法] 撤 x16 走廊, 不原地挨打
            hold_retreat(mine)
            if breaker_n >= 2 and rams:
                # 冻结保险: 守军不理冲车, 让冲车自己啃完建筑 (WIN 只看建筑)
                ram_sweep(ebs, rams, '冻结冲车扫')
        else:
            duel_supervise(mine, ranged, melee, rams, pg)
            # 非饵单位 idle 归位 (报复完者回帖; 饵由决斗状态机自管)
            for u in mine:
                if u['slot'] == duel.get('bait'):
                    continue
                a = assign.get(u['slot'])
                if a and idle(u) and d2(tuple(u['tile']), a) > 2:
                    set_target(u, a, 'S5归位')
    elif stage == 'S6':
        rest = [b for b in ebs if tuple(b['tile']) in SOFT_ALL]
        if not rest and s6_phase >= 1:
            log(f't={tick} S6 完成: NW 全软+塔2 平')
            stage, stage_t, assign, sent_s6_posts = 'S7', 0, {}, False
            continue
        if pg:
            # 漏网/回袋守军: 继续决斗梯, 帖不发; hold 时撤走廊+冲车保险
            if not holding:
                duel_supervise(mine, ranged, melee, rams, pg)
            else:
                hold_retreat(mine)
                if breaker_n >= 2 and rams:
                    ram_sweep(ebs, rams, '冻结冲车扫')
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
