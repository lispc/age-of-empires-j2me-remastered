#!/usr/bin/env python3
"""m6c 驱动 v5.4 = v5.3 的 S5 删诱饵 → 全员点名集火 (m5 真集火模式)。

 S0 塔(22,50)：9 远程上帖 + 3 冲车贴啃 + 5 剑士南 staging（probe 实证零损）
 S1 全军东道走廊 → 门南 staging
 S2 拔门钉：5 剑士逐个集火门区 3 近战（混战白拿塔(26,28)）
 S3 拔独钉 (32,16)：5v1
 S4 北穿 → row0/1 带
 S5 口袋点名战：5 剑士全员 retask 同一守军 tile 逐个点名（攻击按目标格结算=
    真集火）；远程+冲车钉 row0 x14-17 绝不进袋；伤<130 撤西南 (10-11,12-14)
    （不撤东北——boot3 证明守军追移动目标可拖 8+ 格，撤向钉线=引狼入室）；
    近战<3 → 冲车换楼（塔0(2,8)/塔1(5,7) 1:1 硬换）。
    死亡率熔断：45s 丢 ≥3 兵 → 近战全撤 hold 40s，两次即冻结。
 S6 NW 帖扫 8 软（pg 清空才发帖）+ S7 收塔0/1/2（显式帖 (0,11)/(5,11)/(6,0)）
 S8 兜底
纪律：守军按 tile 匹配（敌槽位死亡压缩）；pg=x<=14,y<=14（含被拖出袋者）；
retask 只在目标变化时发；卡死=同 tgt 连发 8 拍未动→换邻格。
"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6c'
FIFO = os.path.join(W, 'fifo')
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M6_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M6_TIMEOUT', '1500'))

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


# ---------- 静态计划 ----------
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

# v5.5: 尾段走 x16 走廊——boot1 实证 x12-14 y1-5 擦 (8,5) aggro 圈(猫报复追到
# (6,5) 袋口阵亡); x16 全程离口袋守军 >=8 格
HOPS_NORTH = [(29, 34), (29, 29), (30, 28), (30, 27), (30, 26), (29, 25),
              (30, 22), (33, 20), (34, 19), (36, 18), (37, 16), (37, 12),
              (36, 11), (35, 10), (30, 10), (25, 10), (20, 9), (18, 8),
              (16, 8), (16, 6), (16, 4), (16, 2), (16, 1)]
STRIP = [(15, 0), (16, 0), (17, 0), (15, 1), (16, 1), (17, 1), (15, 2),
         (16, 2), (17, 2), (14, 0), (14, 1), (14, 2), (16, 3), (15, 3),
         (17, 3), (15, 4), (16, 4)]

# S5: 远程+冲车钉 row0 x14-17（离口袋家 >=8 格, 守军追击圈外）
PIN_ROW = [(14, 0), (15, 0), (16, 0), (17, 0), (14, 1), (15, 1), (16, 1),
           (17, 1), (14, 2), (15, 2), (16, 2), (17, 2)]
# S5: 伤兵撤退位=口袋西南开阔地（拖离钉线, 离中场猫堆 (12-14,22-24) >=9 格）
RET_S5 = [(10, 12), (9, 13), (11, 13), (10, 14), (8, 12)]
SOFT7 = {(1, 1), (2, 1), (1, 3), (2, 3), (4, 2), (5, 1), (2, 5)}
SOFT_ALL = SOFT7 | {(4, 4), (2, 5)}
POSTS_B = [(1, 0), (2, 0), (0, 1), (3, 0), (4, 0), (1, 2), (0, 2), (6, 0),
           (0, 4)]
TOWER_POSTS = {(2, 8): (0, 11), (5, 7): (5, 11), (6, 4): (6, 0)}
POSTS_T3 = [(23, 27), (25, 25), (23, 26), (24, 25), (22, 28), (26, 24)]
# pg 过滤: 口袋盒 (2-9,3-10) + 被追出袋的余波 (boot1 实证猫追到 (8,1))
PGF = lambda t: t[0] <= 16 and t[1] <= 16

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
lastpos = {}         # slot -> [pos, 无位移计数]  (行军冻结守卫)
prev_my = -1         # 熔断: 我方计数
death_marks = []     # 熔断: 我方死亡墙钟时刻
breaker_n = 0        # 熔断触发次数
hold_until = 0.0     # 熔断后 hold 截止墙钟
ram_trading = False


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
    """hops 末元素=个人终点。返回 True=到终点闲置。
    冻结守卫: 同一 hop 上位置 20 拍不动且非闲置(target≠tile)→视为被堵,
    强制进下一 hop(引擎 blocked-arrival 永不改写卡死目标)。"""
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
    """点名目标: 焦点 tile 上/旁(<=2格)仍有活守军→跟随同一守军(滞回);
    否则猫(t8)优先(溅射是集火球最大威胁, boot1 实证), 无猫取离近战质心最近者。"""
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
    """冲车换楼: 优先塔0(2,8)/塔1(5,7), 否则最近建筑。"""
    pref = [b for b in ebs if tuple(b['tile']) in {(2, 8), (5, 7)}]
    pool = pref or sorted(ebs, key=lambda b: d2(tuple(b['tile']), (4, 6)))
    for i, u in enumerate(rams):
        bt = tuple(pool[i % len(pool)]['tile'])
        assign[u['slot']] = bt
        set_target(u, bt, tag)


log(f'=== m6cdrv v5.4 start poll={POLL} ===')
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

    # 死亡率熔断: 45s 内丢 >=3 → 近战全撤 hold 40s; 两次即冻结(转守到底)
    now = time.time()
    if prev_my >= 0 and len(mine) < prev_my:
        death_marks.extend([now] * (prev_my - len(mine)))
    prev_my = len(mine)
    death_marks = [t for t in death_marks if now - t <= 45]
    if len(death_marks) >= 3 and breaker_n < 2:
        breaker_n += 1
        death_marks = []
        hold_until = now + 40
        log(f't={tick} !!! 熔断#{breaker_n}: 45s 丢3+ → 近战全撤 hold40s '
            f'(my={len(mine)} foe={len(foes)})')
    frozen = breaker_n >= 2
    holding = frozen or now < hold_until

    # 掉队重发 + 硬钉（防 auto-acquire 游走进袋——boot1 六杀教训）
    # S5/S6 点名战期间近战由阶段块接管(assign=守军 tile 非建筑, 硬钉会打架)
    for u in mine:
        if pg and stage in ('S5', 'S6') and u['type'] == 3:
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
                # 目标不是帖位射程内的敌建筑 = 被 auto-acquire 拖走 → 拉回
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
        # 个人化终点: 已走完公共 hops 的送往个人 staging 位
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
            # 守军死净同拍把近战拉出塔(26,28)火力——boot1 有伤兵在检测
            # 延迟窗(~100t)内被塔射死; 拉完再切 S3
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
        # 口袋点名战: 近战全员 retask 同一守军 tile 逐个点名(猫优先); 远程钉 row0
        if stage_t == 0:
            log(f't={tick} S5 点名战开局 pg={[tuple(g["tile"]) for g in pg]} '
                f'melee={len(melee)}')
            for i, u in enumerate(ranged + rams):
                t = PIN_ROW[i % len(PIN_ROW)]
                assign[u['slot']] = t
                set_target(u, t, 'S5钉')
            # 熔断重置: 门钉战的熔断不占口袋战的三振(两场独立战役)
            breaker_n = 0
            death_marks = []
        if not pg:
            log(f't={tick} S5 完成: 口袋守军清空 (my={len(mine)})')
            stage, stage_t, assign, focus = 'S6', 0, {}, None
            continue
        ram_trading = False
        if holding:
            for i, u in enumerate(melee):
                rt = RET_S5[i % len(RET_S5)]
                assign[u['slot']] = rt
                set_target(u, rt, 'S5hold撤')
        elif len(melee) >= 3:
            gt = pick_focus(pg, melee)
            if focus != gt:
                focus = gt
                log(f't={tick} S5 点名 {gt} pg={[tuple(g["tile"]) for g in pg]} '
                    f'mel={[(u["slot"], u["hp"]) for u in melee]}')
            for u in melee:
                if u['hp'] < 150:
                    rt = RET_S5[u['slot'] % len(RET_S5)]
                    assign[u['slot']] = rt
                    set_target(u, rt, 'S5伤撤')
                else:
                    assign[u['slot']] = gt
                    set_target(u, gt, 'S5点名')
        else:
            # 近战<3: boot1 实证 1-2 近战冲锋=送死; 冲车换楼 1:1 硬换
            ram_trading = True
            for i, u in enumerate(melee):
                rt = RET_S5[i % len(RET_S5)]
                assign[u['slot']] = rt
                set_target(u, rt, 'S5撤')
            ram_trade(ebs, rams, 'S5换楼')
    elif stage == 'S6':
        rest = [b for b in ebs if tuple(b['tile']) in SOFT_ALL]
        if not rest:
            log(f't={tick} S6 完成: NW 全软+塔2 平')
            stage, stage_t, assign, sent_s6_posts = 'S7', 0, {}, False
            continue
        if stage_t == 0:
            log(f't={tick} S6 开局 (pg={len(pg)} melee={len(melee)} '
                f'ranged={len(ranged)})')
        ram_trading = False
        if pg:
            # 漏网/回袋守军: 点名战延续, 帖不发
            if holding:
                for i, u in enumerate(melee):
                    rt = RET_S5[i % len(RET_S5)]
                    assign[u['slot']] = rt
                    set_target(u, rt, 'S6hold撤')
            elif len(melee) >= 3:
                gt = pick_focus(pg, melee)
                if focus != gt:
                    focus = gt
                    log(f't={tick} S6 点名 {gt} pg={[tuple(g["tile"]) for g in pg]}')
                for u in melee:
                    if u['hp'] < 150:
                        rt = RET_S5[u['slot'] % len(RET_S5)]
                        assign[u['slot']] = rt
                        set_target(u, rt, 'S6伤撤')
                    else:
                        assign[u['slot']] = gt
                        set_target(u, gt, 'S6点名')
            else:
                ram_trading = True
                for i, u in enumerate(melee):
                    rt = RET_S5[i % len(RET_S5)]
                    assign[u['slot']] = rt
                    set_target(u, rt, 'S6撤')
                ram_trade(ebs, rams, 'S6换楼')
        elif rest and not sent_s6_posts:
            sent_s6_posts = True
            log(f't={tick} S6 帖扫荡 NW 全软+塔2')
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
    elif stage == 'S7':
        rest_t = [b for b in ebs if b['type'] == 12]
        if not rest_t:
            log(f't={tick} S7 完成: 全塔平 eb={len(ebs)}')
            stage = 'S8'
            continue
        if stage_t == 0:
            log(f't={tick} S7 收尾残余塔 {[tuple(b["tile"]) for b in rest_t]}')
            sent_s7 = False
        if not sent_s7:
            sent_s7 = True
            tiles_s = [tuple(b['tile']) for b in rest_t]
            for i, u in enumerate(ranged):
                bt = tiles_s[i % len(tiles_s)]
                # 显式帖: 塔0(2,8)→(0,11) 塔1(5,7)→(5,11) 塔2(6,4)→(6,0)
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
        for b in ebs:
            bt = tuple(b['tile'])
            cover = sum(1 for u in ranged if d2(tuple(u['tile']), bt) <= 16)
            if cover == 0:
                idle_far = [u for u in ranged
                            if d2(tuple(u['tile']), bt) > 16
                            and idle(u)]
                if idle_far:
                    u0 = min(idle_far, key=lambda u: d2(tuple(u['tile']), bt))
                    # 走到 d²≈9-16 的帖位: 取建筑四邻方向 3-4 格
                    dx = 3 if bt[0] > 8 else -3 if bt[0] < 8 else 0
                    dy = 3 if bt[1] > 8 else -3 if bt[1] < 8 else 0
                    t = clamp((bt[0] + (dx or 4), bt[1] + (dy or -4)))
                    set_target(u0, t, f'S8补{bt}')

    stage_t += 1
    if time.time() - _last_sum > 5.0:
        _last_sum = time.time()
        avg = sum(u['hp'] for u in mine) // len(mine)
        log(f't={tick} {stage} my={len(mine)} foe={len(foes)} eb={len(ebs)} '
            f'avg_hp={avg} retask={_n} st={stage_t}'
            + (f' MELT#{breaker_n}' if breaker_n else ''))

    time.sleep(POLL)

r = result()
log('RESULT ' + r if r else f'NO RESULT dead={_dead} {time.time() - _t0:.0f}s retask={_n}')
