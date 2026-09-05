#!/usr/bin/env python3
"""m6b 驱动 v5 = 接力白嫖 + 局部拔钉（probe 实证 + 离线 BFS 定线）。

 S0 塔(22,50)：9 远程上帖 + 3 冲车贴啃 + 5 剑士南 staging（probe 已证 idle 自动开火）
 S1 全军东道走廊 → 门南 staging（个人化终点防互堵）
 S2 拔门钉：5 剑士逐个集火门区 3 近战剑士（伤撤门南）
 S3 拔独钉 (32,16)：5v1
 S4 北穿 → row0/1 带（个人化终点 17 格散开）
 S5 NW 帖：9 远程上 9 帖（引擎自动开火清 马厩/兵营/攻城坊/铁匠铺/城堡/大学/射箭场；
    (6,0) 帖顺带打塔2(6,4)+猫弹溅射实测）
 S6 口袋裁决：守军被溅射掉血→站桩磨；否则 5 剑士进袋逐个集火；
    守军清空后 (1,2)/(0,11)/(5,11) 三帖收 TC/塔0/塔1
 S7 塔(26,28)：远程南返 (23,27) 等帖
 S8 兜底：剩余建筑逐栋查覆盖,无人打的派最近闲远程
纪律：retask 只在阶段切换/掉队/换焦发；守军按 tile 匹配（敌槽位死亡压缩）；
卡死=同 tgt 连发 8 拍未动→换邻格；我方全灭即退。
"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6b'
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

HOPS_NORTH = [(29, 34), (29, 29), (30, 28), (30, 27), (30, 26), (29, 25),
              (30, 22), (33, 20), (34, 19), (36, 18), (37, 16), (37, 12),
              (36, 11), (35, 10), (30, 10), (25, 10), (20, 9), (18, 8),
              (16, 8), (14, 8), (14, 6), (14, 5), (13, 4), (13, 3), (13, 2),
              (12, 1)]
HOPS_S7 = [(14, 5), (14, 8), (16, 8), (18, 8), (20, 9), (25, 10), (30, 10),
           (33, 12), (34, 16), (36, 18), (36, 22), (33, 26), (31, 30),
           (31, 33), (31, 35), (29, 33), (28, 30), (26, 28), (24, 27),
           (23, 27)]
STRIP = [(12, 0), (12, 1), (13, 1), (13, 2), (13, 3), (13, 4), (14, 4),
         (14, 5), (11, 0), (10, 0), (9, 0), (10, 1), (11, 1), (12, 2),
         (14, 3), (14, 2), (14, 1)]

POSTS_A = [(2, 0), (1, 0), (3, 0), (4, 0), (0, 1), (0, 0), (6, 0)]
SOFT7 = {(1, 1), (2, 1), (1, 3), (2, 3), (4, 2), (5, 1), (2, 5)}
SOFT_ALL = SOFT7 | {(4, 4), (2, 5)}
POSTS_B = [(1, 0), (2, 0), (0, 1), (3, 0), (4, 0), (1, 2), (0, 2), (6, 0),
           (0, 4)]
POCKET_BOX = lambda t: 2 <= t[0] <= 9 and 3 <= t[1] <= 10
POCKET_ORDER = [(8, 5), (6, 7), (5, 5), (3, 7), (4, 9)]
BAIT_AMBUSH = [(11, 1), (12, 1), (13, 2), (12, 3), (11, 3)]
RET_S6 = [(13, 2), (13, 1)]
POST_TC = (1, 2)
POST_SJ = (0, 4)
POST_T0 = (0, 11)
POST_T1 = (5, 11)
POSTS_T3 = [(23, 27), (25, 25), (23, 26), (24, 25), (22, 28), (26, 24)]

# ---------- 状态 ----------
stage = os.environ.get('M6_STAGE', 'S0')
assign = {}
hop_i = {}
issue = {}
focus = None
stage_t = 0
pocket0 = {}
sent_s6_posts = False
sent_s7 = False
remaining = set()
lastpos = {}         # slot -> [pos, 无位移计数]  (行军冻结守卫)


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
    idle = list(u['target']) == [u['tile'][0], u['tile'][1]]
    lp = lastpos.setdefault(s, [tuple(u['tile']), 0])
    if tuple(u['tile']) == lp[0]:
        lp[1] += 1
    else:
        lp[0], lp[1] = tuple(u['tile']), 0
    stuck = (not idle) and lp[1] >= 20
    while i < len(hops) and d2(tuple(u['tile']), hops[i]) <= 2 and idle:
        i += 1
        lp[1] = 0
        idle = list(u['target']) == [u['tile'][0], u['tile'][1]]
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


log(f'=== m6bdrv v5 start poll={POLL} ===')
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

    # 掉队重发 + 硬钉（防 auto-acquire 游走进袋——boot1 六杀教训）
    for u in mine:
        a = assign.get(u['slot'])
        if not a:
            continue
        idle = list(u['target']) == [u['tile'][0], u['tile'][1]]
        if idle and d2(tuple(u['tile']), a) > 2:
            set_target(u, a, f'{stage}/掉队')
        elif not idle and list(u['target']) != list(a):
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
            log(f't={tick} S2 完成: 门钉清空 (foe={len(foes)})')
            stage, stage_t, assign, focus = 'S3', 0, {}, None
            continue
        g0 = min(cands, key=lambda g: d2(g['tile'], (34, 34)))
        gt = tuple(g0['tile'])
        if focus != gt:
            focus = gt
            log(f't={tick} S2 集火门钉 {gt}')
        for u in melee:
            if u['hp'] < 130:
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
            if u['hp'] < 130:
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
        # 口袋裁决: 全军伏击带 → 诱饵+伏击逐杀口袋守军 (近帖围城前置条件)
        pg = [u for u in foes if POCKET_BOX(tuple(u['tile']))]
        if stage_t == 0:
            log(f't={tick} S5 口袋伏击开局 pg={[tuple(g["tile"]) for g in pg]}')
            for i, u in enumerate(mine):
                t = BAIT_AMBUSH[i % len(BAIT_AMBUSH)]
                assign[u['slot']] = t
                set_target(u, t, 'S5伏')
        elif not pg:
            log(f't={tick} S5 完成: 口袋守军清空')
            stage, stage_t, assign, focus = 'S6', 0, {}, None
            continue
        elif melee:
            tgt = None
            gcur = None
            for ph in POCKET_ORDER:
                for g in pg:
                    if d2(ph, tuple(g['tile'])) <= 4:
                        tgt, gcur = ph, g
                        break
                if tgt:
                    break
            if gcur is None:
                gcur = pg[0]
                tgt = tuple(gcur['tile'])
            gt = tuple(gcur['tile'])
            home = focus if (focus and d2(focus, gt) <= 64) else gt
            if focus != gt:
                log(f't={tick} S5 诱饵目标 {gt} (home={home}) pg={[tuple(g["tile"]) for g in pg]}')
                focus = gt
            bait = max(melee, key=lambda u: u['hp'])
            amb = [u for u in melee if u['slot'] != bait['slot']]
            chased = d2(gt, home) >= 4
            near_bait = d2(gt, tuple(bait['tile'])) <= 9
            if chased or near_bait:
                assign[bait['slot']] = (12, 2)
                set_target(bait, (12, 2), 'S5诱撤')
                for u in amb:
                    assign[u['slot']] = gt
                    set_target(u, gt, 'S5伏杀')
            else:
                dx, dy = 12 - home[0], 2 - home[1]
                m = max(abs(dx), abs(dy)) or 1
                prov = clamp((home[0] + dx * 3 // m, home[1] + dy * 3 // m))
                assign[bait['slot']] = prov
                set_target(bait, prov, 'S5挑衅')
                for i, u in enumerate(amb):
                    a2 = BAIT_AMBUSH[(i + 1) % len(BAIT_AMBUSH)]
                    assign[u['slot']] = a2
                    set_target(u, a2, 'S5伏位')
        # 远程/冲车: 钉在伏击带 (无射程覆盖=安全等)
        for u in ranged + rams:
            if u['slot'] not in assign or d2(tuple(u['tile']),
                                             assign.get(u['slot'], (0, 0))) > 4                     or list(u['target']) != [u['tile'][0], u['tile'][1]]:
                a3 = assign.get(u['slot']) or BAIT_AMBUSH[0]
                assign[u['slot']] = a3
                set_target(u, a3, 'S5钉')
    elif stage == 'S6':
        rest = [b for b in ebs if tuple(b['tile']) in SOFT_ALL]
        if not rest:
            log(f't={tick} S6 完成: NW 全软+塔2 平')
            stage, stage_t, assign, sent_s6_posts = 'S7', 0, {}, False
            continue
        if stage_t == 0:
            # 全军先撤到伏击带（关死 auto-acquire 游走窗）
            log(f't={tick} S6 开局: 先撤伏击带 (pg={[tuple(g["tile"]) for g in pg]})')
            for i, u in enumerate(mine):
                t = BAIT_AMBUSH[i % len(BAIT_AMBUSH)]
                assign[u['slot']] = t
                set_target(u, t, 'S6伏')
        elif len(melee) >= 4 and pg:
            # 诱饵+伏击: bait 引守军离位北追, 伏击线围杀
            tgt = None
            gcur = None
            for p in POCKET_ORDER:
                for g in pg:
                    if d2(p, tuple(g['tile'])) <= 4:
                        tgt, gcur = p, g
                        break
                if tgt:
                    break
            if tgt is None and pg:
                gcur = pg[0]
                tgt = tuple(gcur['tile'])
            if gcur is not None:
                gt = tuple(gcur['tile'])
                home = focus if (focus and d2(focus, gt) <= 64) else gt
                if focus != gt:
                    log(f't={tick} S6 诱饵目标 {gt} (home={home})')
                    focus = gt
                bait = max(melee, key=lambda u: u['hp'])
                amb = [u for u in melee if u['slot'] != bait['slot']]
                chased = d2(gt, home) >= 4
                near_bait = d2(gt, tuple(bait['tile'])) <= 9
                if chased or near_bait:
                    # 守军已离位/咬到诱饵: 诱饵撤, 伏击线上压
                    assign[bait['slot']] = (12, 2)
                    set_target(bait, (12, 2), 'S6诱撤')
                    for u in amb:
                        assign[u['slot']] = gt
                        set_target(u, gt, 'S6伏杀')
                else:
                    # 诱饵到家侧 3 格挑衅
                    dx, dy = 12 - home[0], 2 - home[1]
                    m = max(abs(dx), abs(dy)) or 1
                    prov = clamp((home[0] + dx * 3 // m, home[1] + dy * 3 // m))
                    assign[bait['slot']] = prov
                    set_target(bait, prov, 'S6挑衅')
                    for i, u in enumerate(amb):
                        a = BAIT_AMBUSH[(i + 1) % len(BAIT_AMBUSH)]
                        assign[u['slot']] = a
                        set_target(u, a, 'S6伏位')
        elif pg and 0 < len(melee) < 4:
            # 近战不足: 步兵撤出袋区, 冲车换楼, 远程站桩溅射
            for u in melee:
                assign[u['slot']] = RET_S6[u['slot'] % 2]
                set_target(u, RET_S6[u['slot'] % 2], 'S6撤')
            for i, u in enumerate(rams):
                t = [(3, 4), (2, 9), (5, 8)][i % 3]
                assign[u['slot']] = t
                set_target(u, t, 'S6ram换')
        if rest and not sent_s6_posts:
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
                # 帖位: 建筑四邻 3-4 格取可站方向
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
                            and list(u['target']) == [u['tile'][0], u['tile'][1]]]
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
            f'avg_hp={avg} retask={_n} st={stage_t}')

    time.sleep(POLL)

r = result()
log('RESULT ' + r if r else f'NO RESULT dead={_dead} {time.time() - _t0:.0f}s retask={_n}')
