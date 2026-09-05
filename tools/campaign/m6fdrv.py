#!/usr/bin/env python3
"""m6f 驱动 v6.6 = v6.5 (帖诱反杀状态机) + boot1 尸检五修。

 boot1 (v6.5) 尸检: S5 钓鱼链真杀 4 守军弓 (报复反杀成立), 但 3:4 亏 +
 猫阶段 3:1 熔断。v6.6 修:
 [1 拉回点最小暴露] fish_rear=守军射程外帖中离 F 最近者 (v6.5 用 max-d²,
   饵横穿 4 格火力, 双饵阵亡主因之一)。
 [2 ram_sweep 猫圈豁免作废] avoid_cats: d²≤37 内建筑不认领——猫=最近单位
   冲车照杀 (r63), v6.4「守军不理冲车」冻结保险烧光 boot1 三冲车。
 [3 S1 整队行军] 步速=最慢者+2 + 全员到齐才转 S2 (boot1 两弓落单死于巢穴
   (33,40)(34,39), v6.4 东移 hops 不足以防落单)。
 [4 S5→S6 无害转段] 剩余 pg 全是「钓点全封+对 S6 帖/顶廊 margin 外」的弓
   且口袋无猫 → 忽略 pg 转 S6 (WIN 只看建筑, 东缘 3 弓不必清)。
 [5 S6 ignore_pg] 无害守军在场时照常收顶廊+帖扫。
 v6.5 相对 v6.4: import 闸/S4 漂移监测/帖诱反杀 (动态帖+钓鱼+envelope 风筝+
 猫对猫+围杀/诱离降级链), S1 hops 东移 = v6.4 原样。S0-S4 = v5.5 原样。
"""
import json
import math
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6f'
DRY = os.environ.get('M6_DRY', '') == '1'
DRY_STATE = os.environ.get('M6_DRY_STATE', '')
FIFO = os.path.join(W, 'fifo')
if DRY:
    # [自检隔离] 假路径: 即使闸失效也绝无可能写到活局 fifo
    FIFO = '/tmp/aoe-camp/m6f/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M6_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M6_TIMEOUT', '1500'))
PROBE = os.environ.get('M6_MODE', 'main') == 'probe'
PROBE_CAP = float(os.environ.get('M6_PROBE_CAP', '210'))
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

HOPS_EAST = [(18, 55), (17, 52), (19, 50), (25, 49), (31, 48), (36, 48),
             (40, 46), (43, 44), (43, 40), (41, 38), (38, 37), (36, 36),
             (35, 35)]
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
PGF = lambda t: t[0] <= 19 and t[1] <= 19

# ---------- S5 帖诱反杀 (v6.5) ----------
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
probe_stop = False
probe_stop_wall = 0.0
probe_last = None
evf = None
_last_sweep_warn = 0.0
s6_ignore_pg = False
# v6.5 S5 状态
s5 = {'posts': [], 'park': [], 'ramhome': [], 'skip': set(), 'fail': {}}
fish_d = None
ramdrag_sent = 0
drift_seen = {}
_freeze_last = 0.0


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
    """[alive 误判修法] 猎物追踪: 同 type + 邻域 d²≤20; 优先掉血者。"""
    qhp = duel.get('qhp', 255)
    qhp0 = duel.get('qhp0', 255)
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
        if tuple(rt) not in WALK_NW:
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
    if duel['phase'] == 'shield':
        if b is None:
            fs, fk = duel.get('fslot'), duel.get('kind')
            _duel_reset('饵亡(盾)')
            _fail_bump(fs, fk)
            return
        sh = next((x for x in mine if x['slot'] == duel.get('shield')), None) \
            if duel.get('shield') is not None else None
        engaged = (b['hp'] < duel['bhp']) or (duel['qhp'] < prev_qhp) \
            or (duel['qt'] != prev_qt)
        if engaged:
            duel['phase'] = 'fight'
            duel['quiet'] = 0
            log(f'  盾接火 (盾hp={sh["hp"] if sh else "?"}) → 自由对射')
            return
        if sh is None:
            duel['phase'] = 'fight'
            log('  盾亡于途中 → 饵猫单挑 (硬换)')
            return
        if idle(sh) and d2(tuple(sh['tile']), duel['qt']) > 16:
            assign[duel['shield']] = duel['btile']
            set_target(sh, duel['qt'], '盾贴脸')
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
        if tuple(nb) in WALK_NW and zone_ok(nb, others):
            duel['btile'] = nb
            duel['home_n'] += 1
            assign[b['slot']] = nb
            set_target(b, nb, f'饵home{nb}')
            log(f'  饵 home→{nb} (12s无火, 会合区重算)')
        else:
            log(f'  home→{nb} 落会合区外, 放弃 (待超时重选)')


def ramshield_spot(E, pg):
    """冲车盾点位: 盾 S 在 E 射程内 (d²∈[9,16]) 且对其他守军 >20;
    我猫 B 在 S 后 (d²(B,E)∈[9,16], S 在 B 与 E 之间)。返回 (S,B) 或 None。"""
    others = [(tuple(h['tile']), h['type']) for h in pg
              if tuple(h['tile']) != tuple(E)]
    for sy in range(0, 13):
        for sx in range(6, 19):
            S = (sx, sy)
            if S not in WALK_NW or not (9 <= d2(S, E) <= 16):
                continue
            if any(d2(S, o) <= 20 for o, ty in others):
                continue
            for by in range(max(0, sy - 3), min(13, sy + 4)):
                for bx in range(max(6, sx - 3), min(19, sx + 4)):
                    B = (bx, by)
                    if B == S or B not in WALK_NW:
                        continue
                    if not (9 <= d2(B, E) <= 16):
                        continue
                    if d2(B, S) > 4 or d2(B, S) >= d2(S, E):
                        continue
                    if any(d2(B, o) <= 20 for o, ty in others):
                        continue
                    return (S, B)
    return None


def duel_open_cat(g, ranged, melee, pg, rams=None):
    """猫目标开局: [v6.7 首选] 冲车盾猫对猫 (盾=最近单位吃火, 我猫盾后
    输出, 1 冲车换 1 猫, 猫全保) → 猫对猫硬换 → 近战围杀 (fail≥2 近战≥4)。"""
    global duel, _freeze_last
    tt = tuple(g['tile'])
    others = [(tuple(h['tile']), h['type']) for h in pg if h is not g]
    fails = s5['fail'].get(g['slot'], 0)
    cats0 = sorted([u for u in ranged if u['type'] == 8],
                   key=lambda u: -u['hp'])
    if rams and cats0:
        shield = max(rams, key=lambda u: u['hp'])
        spot = ramshield_spot(tt, pg)
        if spot is not None and slot_types.get(cats0[0]['slot']) == 8:
            duel = {'tgt': tt, 'gtype': 8, 'tag': f'Cs{g["slot"]}', 'qt': tt,
                    'qhp': g['hp'], 'qhp0': g['hp'], 'bait': cats0[0]['slot'],
                    'btype': 8, 'kind': '冲车盾猫对猫', 'phase': 'shield',
                    'btile': spot[1], 'shield': shield['slot'],
                    'bhp': cats0[0]['hp'], 'quiet': 0, 'home_n': 0,
                    't0': time.time(), 'prog': time.time(),
                    'fslot': g['slot']}
            assign[shield['slot']] = spot[0]
            set_target(shield, spot[0], f'盾入{spot[0]}')
            assign[cats0[0]['slot']] = spot[1]
            set_target(cats0[0], spot[1], f'盾后猫{spot[1]}')
            log(f'  冲车盾开局: {duel["tag"]}{tt} 盾=s{shield["slot"]}→{spot[0]} '
                f'猫=s{cats0[0]["slot"]}→{spot[1]}')
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
            if not cats:
                continue
            zone = meeting_zone(tt, 4, others)
            if zone is None:
                log(f'  {tag}{tt} 弓: 会合区无解 — 试下个目标')
                continue
            bait = cats[0]
            if slot_types.get(bait['slot']) != 8:
                return
            assign[bait['slot']] = DUEL_REST
            set_target(bait, DUEL_REST, '饵收柱')
            duel = {'tgt': tt, 'gtype': 4, 'tag': tag, 'qt': tt,
                    'qhp': tgt['hp'], 'qhp0': tgt['hp'],
                    'bait': bait['slot'], 'btype': 8, 'kind': '猫饵对射',
                    'phase': 'rest', 'btile': None, 'bhp': bait['hp'],
                    'quiet': 0, 'home_n': 0, 't0': time.time(),
                    'prog': time.time()}
            log(f'  决斗开局: {tag}{tt}(t4,hp{tgt["hp"]}) 饵=s{bait["slot"]}')
            return
        if tgt['type'] == 8:
            if melee:
                duel = {'tgt': tt, 'gtype': 8, 'tag': tag, 'qt': tt,
                        'qhp': tgt['hp'], 'qhp0': tgt['hp'], 'bait': None,
                        'btype': None, 'kind': '近战围杀', 'phase': 'rush',
                        'btile': None, 'bhp': 255, 'quiet': 0, 'home_n': 0,
                        't0': time.time(), 'prog': time.time()}
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
                        'prog': time.time()}
                log(f'  猫对猫开局: {tag}{tt}(t8) 饵=s{bait["slot"]}')
                return


# ---------- v6.5 帖诱反杀 (fish) ----------
def region_posts(pg, n, xr, yr, anchor, pairwise):
    """安全帖贪心: 对全部 pg margin 外 (弓>25/猫>37), 锚点就近, pairwise 分散。"""
    cands = []
    for y in yr:
        for x in xr:
            t = (x, y)
            if t not in WALK_NW or MID_BAND(t):
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
                if t not in WALK_NW or MID_BAND(t):
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


def fish_start(g, ranged, pg):
    global fish_d
    G = tuple(g['tile'])
    F, strict = fish_spot(G, pg)
    if F is None:
        return False
    cands = [u for u in ranged if u['type'] in (4, 9)
             and slot_types.get(u['slot']) in (4, 9)]
    if not cands:
        return False
    cands.sort(key=lambda u: (-u['hp'], d2(tuple(u['tile']), F)))
    b = cands[0]
    posts = s5['posts'] or [SQ_ANCHOR]
    rear = fish_rear(G, F, posts)
    fish_d = {'tslot': g['slot'], 'ttile': G, 'F': F, 'strict': strict,
              'bait': b['slot'], 'btype': b['type'], 'phase': 'go',
              'bhp': b['hp'], 't0': time.time(), 'att': 1, 'hold0': 0.0,
              'tag': f'arch@{G}', 'rear': rear}
    assign[b['slot']] = F
    set_target(b, F, f'fish go{F}')
    log(f'  钓鱼开局 {fish_d["tag"]} F={F} ({"严格" if strict else "宽松"}) '
        f'饵=s{b["slot"]} hp{b["hp"]}')
    return True


def fish_tick(mine, pg):
    global fish_d
    now = time.time()
    t = next((g for g in pg if g['slot'] == fish_d['tslot'] and g['type'] == 4
              and d2(tuple(g['tile']), fish_d['ttile']) <= 30), None)
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
        log(f'  饵亡 s{fish_d["bait"]} @{fish_d["tag"]} (hp{fish_d["bhp"]}) — 重选')
        fish_d = None
        return
    env = bool(s5['posts']) and any(d2(fish_d['ttile'], p) <= 16
                                    for p in s5['posts'])
    if fish_d['phase'] in ('go', 'pull') and env:
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
            log(f'  饵中箭 hp{b["hp"]} — 拉回 {fish_d["rear"]} (守军应报复追来)')
            fish_d['phase'] = 'pull'
        assign[b['slot']] = fish_d['rear']
        set_target(b, fish_d['rear'], '钓拉回')
        fish_d['bhp'] = b['hp']
        return
    fish_d['bhp'] = b['hp']
    if fish_d['phase'] == 'go':
        if idle(b) and d2(tuple(b['tile']), fish_d['F']) > 2:
            set_target(b, fish_d['F'], '钓重发')
    else:  # pull
        if idle(b) and d2(tuple(b['tile']), fish_d['rear']) <= 2:
            fish_d['att'] += 1
            if fish_d['att'] > 4:
                log(f'  钓鱼放弃 {fish_d["tag"]} ×{fish_d["att"] - 1} 次拉回无果')
                s5['skip'].add(fish_d['tslot'])
                fish_d = None
                return
            F, strict = fish_spot(fish_d['ttile'], pg)
            if F is None:
                log(f'  钓点全封 {fish_d["tag"]} — 暂弃 (待局面变化)')
                fish_d = None
                return
            fish_d['F'], fish_d['strict'] = F, strict
            fish_d['phase'] = 'go'
            fish_d['t0'] = now
            fish_d['rear'] = fish_rear(fish_d['ttile'], F,
                                       s5['posts'] or [SQ_ANCHOR])
            assign[b['slot']] = F
            set_target(b, F, f'钓再入{F}')
            log(f'  再钓 #{fish_d["att"]} F={F}')


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
        spots = [s for s in spots if tuple(s) in WALK_NW
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
    arch = sorted([g for g in pg if g['type'] == 4],
                  key=lambda g: -g['tile'][0])
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


log(f'=== m6fdrv v6.7 start poll={POLL} mode={"probe" if PROBE else "main"} '
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
        pg = [u for u in foes if PGF(tuple(u['tile']))]

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
                top = sorted(drift_seen.items(), key=lambda kv: -kv[1])[:8]
                log(f't={tick} S5 预告阵型 pg={[tuple(g["tile"]) for g in pg]} '
                    f'pgt={[g["type"] for g in pg]} drift_top={top} '
                    f'ranged={len(ranged)} rams={len(rams)} melee={len(melee)}')
                pgtt = [(tuple(g['tile']), g['type']) for g in pg]
                s5['posts'] = region_posts(pgtt, 5, range(12, 18),
                                           range(2, 9), SQ_ANCHOR, 2)
                s5['park'] = region_posts(pgtt, 5, range(15, 19),
                                          range(3, 9), (17, 6), 1)
                s5['ramhome'] = region_posts(pgtt, 3, range(16, 19),
                                             range(2, 7), (18, 4), 1)
                log(f'  方阵帖={s5["posts"]} 停车={s5["park"]} '
                    f'冲车驻={s5["ramhome"]}')
                if not s5['posts']:
                    log('  !!! 无安全方阵帖 (pg 覆盖过大) — hold 等漂移')
                for i, u in enumerate(melee):
                    p = (s5['park'][i % len(s5['park'])] if s5['park']
                         else MEL_PARK[i % len(MEL_PARK)])
                    assign[u['slot']] = p
                    set_target(u, p, 'S5近战驻')
                for i, u in enumerate(rams):
                    p = (s5['ramhome'][i % len(s5['ramhome'])] if s5['ramhome']
                         else (18, 4))
                    assign[u['slot']] = p
                    set_target(u, p, 'S5冲车驻')
                for i, u in enumerate(ranged):
                    if s5['posts']:
                        p = s5['posts'][i % len(s5['posts'])]
                        assign[u['slot']] = p
                        set_target(u, p, 'S5方阵')
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
                hold_retreat(mine)
                if breaker_n >= 2 and rams:
                    ram_sweep(ebs, rams, '冻结冲车扫',
                              avoid_cats=[tuple(g['tile']) for g in pg
                                          if g['type'] == 8])
            else:
                # 帖位重算 (pg 漂移自适应) — 只在无活动钓局/决斗时重算:
                # 守军进圈瞬间若重算, 帖全撤东, envelope 蒸发, 风筝永不触发
                if fish_d is None and duel['tgt'] is None:
                    pgtt = [(tuple(g['tile']), g['type']) for g in pg]
                    s5['posts'] = region_posts(pgtt, 5, range(12, 18),
                                               range(2, 9), SQ_ANCHOR, 2) \
                        or s5['posts']
                if fish_d is not None:
                    fish_tick(mine, pg)
                elif duel['tgt'] is not None:
                    duel_drive(mine, ranged, melee, rams, pg)
                else:
                    kind, g, F, strict = s5_pick(mine, ranged, melee, pg)
                    if kind == 'done':
                        log(f't={tick} S5 完成: 剩余守军全无害 '
                            f'{[(tuple(g2["tile"]), g2["type"]) for g2 in pg]} '
                            f'— 转 S6 忽略 pg')
                        stage, stage_t, assign, focus = 'S6', 0, {}, None
                        s6_phase = 0
                        s6_ignore_pg = True
                        duel = dict(DUEL_EMPTY)
                        continue
                    elif kind == 'fish':
                        fish_start(g, ranged, pg)
                    elif kind == 'cat':
                        # 开局失败 (无会合区/无饵) 不计 fail——瞬态封死不该烧
                        # 尝试额度; fail 只由真实决斗的超时/饵亡累积
                        duel_open_cat(g, ranged, melee, pg, rams=rams)
                    elif kind == 'swarm':
                        duel_open_cat(g, ranged, melee, pg, rams=rams)
                    elif kind == 'ramdrag':
                        ramdrag_tick(rams, pg)
                        if time.time() - _freeze_last > 10:
                            _freeze_last = time.time()
                            log(f'  无可行猎物 (pg={[(tuple(g2["tile"]), g2["type"]) for g2 in pg]} '
                                f'skip={sorted(s5["skip"])} fail={s5["fail"]}) '
                                f'— 冲车诱离/冻结')
                s5_home(mine, ranged, melee, rams)
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
