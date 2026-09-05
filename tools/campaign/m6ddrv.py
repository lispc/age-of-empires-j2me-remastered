#!/usr/bin/env python3
"""m6d 驱动 v6.1 = v5.5 前四阶段原样 + S5「决斗梯」(probe 实证迭代)。

 probe (本轮 boot1) 实证: ①守军不抱团 aggro——只有射程覆盖我方帖位的那一个
 守军开火 (A1 射 (12,5) 猫饵, 其余 4 个全程不动); ②报复链=112/发, 猫饵 2-3 发
 杀 255hp 弓, 但杀完追击链进交叉火 1:1 换; ③敌回血 ~+20/40t, 消耗战无效;
 ④冲车钓饵被无视 (255hp 原样)。结论: 每个守军存在「独占火力区」, 串行单挑。

 S0 塔(22,50)帖火 / S1 东道 / S2 门钉+同拍撤 / S3 独钉 / S4 x16 走廊: v5.5 原样。
 S5 决斗梯 (串行): 弓兵目标→猫饵钉在独占区对射 (单射手 35t 射击周期 vs 驱动
   10t 重钉, 钉得住; 多射手交叉火才拉不住——v5.5 教训反推); 猫目标→冲车盾+
   近战 retask 同 tile 围杀 (m5/m6c 真集火实证)。目标死→饵回 REST, 下一目标。
   pg 清空 → S6 两段式: 先全员收 y≤2 顶廊 (塔火外) 再帖扫 SOFT_ALL。
 防串: slot→type 快照每拍维护, assign 槽位 type 不符即弃 (BUGS-m6c #6)。
 M6_MODE=probe: S0-S4 照跑 + S5 同逻辑, 另落 JSONL 事件流, 观察窗满停驱。
 支持 attach 热挂: 对活局直接跑本驱动 (M6_STAGE=S5), 不烧 boot。
"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6d'
FIFO = os.path.join(W, 'fifo')
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
    script = "; ".join(f"echo '{c}' > {FIFO}" for c in cmds)
    try:
        subprocess.run(["sh", "-c", script], timeout=6.0)
        _n += len(cmds)
    except subprocess.TimeoutExpired:
        _dead = True
        log('FIFO 无读者(进程退场?)')


def aistate():
    global _dead
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

HOPS_EAST = [(18, 55), (26, 49), (31, 49), (37, 49), (41, 47), (41, 43),
             (40, 41), (39, 40), (38, 37), (36, 36), (35, 35)]
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

# ---------- S5 决斗梯 ----------
# 方阵 (开局/整队用): 前排 x12 只被最东守军覆盖 (probe: A1 独射 (12,5))
SQ_FRONT = [(12, 4), (12, 5), (12, 6)]
SQ_MID = [(13, 4), (13, 5), (13, 6)]
SQ_BACK = [(14, 4), (14, 5), (14, 6)]
# 守军: A1(8,5)已死(probe) A2(6,7) A3(4,9) 弓 + C1(6,5) C2(3,7) 猫。
# 独占区实测+离线复核 (base.aoesave 连通域): 梯位只被目标一人覆盖 (串行前提)。
DUEL_LADDER = {(6, 7): (9, 8), (4, 9): (7, 10), (8, 5): (12, 5),
               (5, 5): (9, 4), (6, 5): (9, 5), (3, 7): (7, 5)}
DUEL_REST = (13, 6)
# 弓程校准: probe 实证 A3 对 d²=20 的饵 800t 不开火 → 弓程 <20 (疑 16)。
# 饵 auto-home: 入梯后 12s 挨不到打且距目标 d²>9 → 向目标挪 1-2 格再试。
# 近战停车场: base.aoesave 连通域实测 (18-20,y0-2) 全是地图墙; 选 x16-17,y6-8
# (离口袋守军 d²>=65, 离 NE 弓 (18,15)/(19,14) d²>=53, 全部可走)
MEL_PARK = [(16, 6), (17, 6), (16, 7), (17, 7), (16, 8)]
MEL_PARK_FB = [(15, 4), (16, 5), (17, 5), (18, 5), (15, 3)]
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
duel = {'tgt': None, 'btile': None, 'bait': None, 'btype': None, 'kind': None}
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


DUEL_EMPTY = {'tgt': None, 'btile': None, 'bait': None, 'btype': None,
              'kind': None, 'phase': None}


def duel_supervise(mine, ranged, melee, rams, pg):
    """串行决斗梯 (probe 实证迭代):
    - 弓兵目标: 猫饵两段接近 (先收 DUEL_REST 全射程外安全柱, 再入独占梯位),
      钉位对射——单射手射击周期 ~35t vs 驱动 10t 重钉, 钉得住 (probe: 多射手
      交叉火才拉不住); 猫报复 112/发, 3 发杀 255hp。
    - 猫目标: 冲车盾(255hp 不还击, 吸 112×3) + 近战 retask 同 tile 围杀。
    - 目标死→饵/近战回撤, 下一目标; 全清→(调用方转 S6)。"""
    global duel
    if duel['tgt'] is not None:
        alive = [g for g in pg if d2(tuple(g['tile']), duel['tgt']) == 0]
        b = next((x for x in mine if x['slot'] == duel['bait']
                  and slot_types.get(duel['bait']) == duel['btype']),
                 None) if duel['bait'] is not None else None
        if not alive:
            log(f'  决斗胜: {duel["tgt"]} ({duel["kind"]}) pg剩={len(pg)}')
            if b is not None:
                assign[b['slot']] = DUEL_REST
                set_target(b, DUEL_REST, '饵收兵')
            if duel['kind'] == '盾冲围杀':
                for u in melee:
                    a = MEL_PARK[u['slot'] % len(MEL_PARK)]
                    assign[u['slot']] = a
                    set_target(u, a, '决斗收兵')
            duel = dict(DUEL_EMPTY)
        elif duel['kind'] == '盾冲围杀':
            if not melee:
                log(f'  盾冲步兵耗尽 @{duel["tgt"]} — 弃攻重选')
                duel = dict(DUEL_EMPTY)
            else:
                if b is not None and not idle(b) \
                        and list(b['target']) != list(duel['btile']):
                    set_target(b, duel['btile'], '盾钉')
                return
        elif b is None:
            log(f'  饵 s{duel["bait"]} 阵亡 @决斗 {duel["tgt"]} — 重选')
            duel = dict(DUEL_EMPTY)
        elif duel['phase'] == 'rest':
            if idle(b) and d2(tuple(b['tile']), DUEL_REST) <= 2:
                duel['phase'] = 'bait'
                duel['bhp'], duel['thp'], duel['quiet'] = b['hp'], \
                    alive[0]['hp'], 0
                assign[b['slot']] = duel['btile']
                set_target(b, duel['btile'], '饵入梯')
            elif not idle(b) and list(b['target']) != list(DUEL_REST):
                set_target(b, DUEL_REST, '饵收')
            elif idle(b):
                set_target(b, DUEL_REST, '饵收')
            return
        else:
            # bait 钉位 + auto-home: 12s(~35拍) 双方无伤=梯位出程 → 向目标挪 1 格
            fired = (b['hp'] < duel.get('bhp', 255)) \
                or (alive[0]['hp'] < duel.get('thp', 255))
            duel['bhp'], duel['thp'] = b['hp'], alive[0]['hp']
            duel['quiet'] = 0 if fired else duel.get('quiet', 0) + 1
            if not idle(b) and list(b['target']) != list(duel['btile']):
                set_target(b, duel['btile'], '决斗钉')
            elif duel['quiet'] >= 35 \
                    and d2(tuple(duel['btile']), duel['tgt']) > 9:
                dx = -1 if duel['tgt'][0] < duel['btile'][0] \
                    else (1 if duel['tgt'][0] > duel['btile'][0] else 0)
                dy = -1 if duel['tgt'][1] < duel['btile'][1] \
                    else (1 if duel['tgt'][1] > duel['btile'][1] else 0)
                nb = clamp((duel['btile'][0] + dx, duel['btile'][1] + dy))
                duel['btile'] = nb
                duel['quiet'] = 0
                assign[b['slot']] = nb
                set_target(b, nb, f'饵home{nb}')
                log(f'  饵 auto-home→{nb} (12s无火, 出程重试)')
            return
    if not pg:
        return
    # 2) 选下一目标: 活弓兵优先 (猫饵对射稳赢), 猫目标=盾冲围杀 (由东向西)
    arch = sorted([g for g in pg if g['type'] == 4],
                  key=lambda g: d2(tuple(g['tile']), (13, 5)))
    catg = sorted([g for g in pg if g['type'] == 8],
                  key=lambda g: d2(tuple(g['tile']), (13, 5)))
    tgt = arch[0] if arch else (catg or pg)[0]
    tt = tuple(tgt['tile'])
    btile = DUEL_LADDER.get(tt, clamp((tt[0] + 5, tt[1] + 1)))
    cats = sorted([u for u in ranged if u['type'] == 8],
                  key=lambda u: -u['hp'])
    if tgt['type'] == 4 and cats:
        bait = cats[0]
        assign[bait['slot']] = DUEL_REST
        set_target(bait, DUEL_REST, '饵收柱')
        duel = {'tgt': tt, 'btile': btile, 'bait': bait['slot'],
                'btype': 8, 'kind': '猫饵对射', 'phase': 'rest'}
        log(f'  决斗开局: {tt}(t{tgt["type"]}) 饵=s{bait["slot"]} '
            f'rest→{btile}')
        return
    # 3) 猫目标: 有近战→冲车盾+围杀; 无近战有猫→猫对猫 1:1 硬换 (末位手段)
    ram0 = max(rams, key=lambda u: u['hp']) if rams else None
    adj = clamp((tt[0] + 1, tt[1]))
    if melee:
        if ram0 is not None:
            assign[ram0['slot']] = adj
            set_target(ram0, adj, '盾冲')
        for u in melee:
            assign[u['slot']] = tt
            set_target(u, tt, '盾冲围杀')
        duel = {'tgt': tt, 'btile': adj,
                'bait': ram0['slot'] if ram0 else None,
                'btype': 7, 'kind': '盾冲围杀', 'phase': 'rush'}
        log(f'  盾冲开局: {tt}(t{tgt["type"]}) ram='
            f'{ram0["slot"] if ram0 else None} melee={len(melee)}')
    elif cats:
        bait = cats[0]
        assign[bait['slot']] = DUEL_REST
        set_target(bait, DUEL_REST, '饵收柱')
        duel = {'tgt': tt, 'btile': DUEL_LADDER.get(
                    tt, clamp((tt[0] + 3, tt[1]))),
                'bait': bait['slot'], 'btype': 8, 'kind': '猫对猫硬换',
                'phase': 'rest'}
        log(f'  猫对猫开局: {tt}(t8) 饵=s{bait["slot"]} (1:1硬换, 无近战)')
    else:
        log(f'  无近战无猫 — 无法杀 {tt}, 冻结等待')
    return


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


log(f'=== m6ddrv v6.0 start poll={POLL} mode={"probe" if PROBE else "main"} ===')
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
        if not holding:
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
            # 漏网/回袋守军: 继续决斗梯, 帖不发
            if not holding:
                duel_supervise(mine, ranged, melee, rams, pg)
            elif stage_t == 0:
                log(f't={tick} S6 hold: pg={len(pg)} 冻结观察')
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
