#!/usr/bin/env python3
"""m0 拆堡关驱动 v1.1 (missionIndex 0, r67)。

胜利 = 拆敌 TC(39,28) (onThingDestroyed case 9, missionIndex==0 不跳过)。
判负 = 我方全灭 (0 建筑开局, 8 兵全死即负)。

[r67 机制定案 — 读码+探针双证]
 A. p1 自动索敌被「每玩家每 tick 1 单位」预算饿死 (tickAutoEngage `if(bl)break;`,
    c.java:8326-8338): p1 槽 0 守军 (38,29) 永远第一个 eligible, acquireTarget 失败
    (我方远) + void_b 失败 (我方 0 建筑) → break。⇒ 其余敌兵永不主动索敌、近战被打
    零反击 (探针实测: s0 单挑游哨 (57,51) 65t 击杀, 自身零掉血, 游哨站死原地)。
 B. 敌兵唯一主动威胁 = 槽 0 守军在我方进 d²≤9 (t2 索敌半径) 时 auto-engage;
    guardA 进战斗后 guardB 接管预算同样索敌。⇒ 守军战不可免, 6 近战齐射点杀。
 C. 唯一不可控伤害 = 哨塔 (自动索敌, range²=hdr[12]=16, 瞄准=槽序第一个进圈目标,
    ~1发/17t, 对 t2 32伤/发)。⇒ 路线上的 twS(37,61)/twNW(17,31) 用冲车啃平
    (塔对冲车 10伤/发); pike/scout 留塔程外 (d²>16)。twW/twSW 路线绕开 (d²≥18)。
 D. 我方 idle 军事 void_b 自动索敌建筑 d²≤9 (塔优先, 同型取 buildingTable 序=近者,
    平手先放者赢 — TC 是 p1 表 slot0) ⇒ CHEW 帖位按此选; idle 长枪在敌兵 d²≤9 会
    1v1 单挑 (48v48 同归) — 滞留位全部按 d²>9 (scout 16 靠预算饿死兜底) 设计。
 E. 战斗发起链: retask 到敌兵格 → blocked-arrival 改写 slot[2]=自身 → idle eligible
    → acquireTarget d²≤1 双方接战 (探针实证)。

阶段: A1行军→PK1(齐射游哨57,51)→A2行军→TWS(冲车平塔37,61+顺杀pk4)→B行军
      →TWNW(冲车平塔17,31)→C行军→GUARD(齐射双守军)→CHEW(全员啃TC)→WIN。
纪律: 全程 retask 宏 (回放可重放); [combat] 死亡差分+幽灵过滤; M0_DRY=1 假 fifo 闸;
      死亡熔断 (45s 丢3 → hold 40s); TIMEOUT 兜底。
"""
import json
import os
import re
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m0'
DRY = os.environ.get('M0_DRY', '') == '1'
DRY_SEQ = os.environ.get('M0_DRY_SEQ', '')   # dry 快照目录 (dry-seq-*.json)
FIFO = os.path.join(W, 'fifo')
if DRY:
    FIFO = '/tmp/aoe-camp/m0/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M0_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M0_TIMEOUT', '1800'))

TC = (39, 28)
TWS = (37, 61)
TWNW = (17, 31)
PK1 = (57, 51)
PK4 = (41, 61)
GUARDS = [(38, 29), (37, 29)]

# ---- 环线路由 (mapdump 真值逐 hop 验证, /tmp/aoe-camp/m0/route_m0.py) ----
# v1.2: 合并单一路线 + 门控索引 (boot1 死锁根因: 无 cap 时先头单位停在段尾
# 占住走廊, 冻跳在最后 hop = min(i+1,len-1) 是 no-op, set_target 被引擎 target
# 去重盲区吞掉 → 全军冻结。修法: 段尾单位沿合并路线继续向前走 (走廊自清空),
# 阶段切换按 min(hop_i) 过门控索引; 重挂/续跑用 argmin 初始化。)
SEG_A1 = [(60, 39), (61, 41), (62, 43), (62, 45), (62, 47), (62, 49),
          (62, 51)]
SEG_A2 = [(61, 52), (59, 53), (58, 54), (57, 55), (56, 56), (55, 57),
          (52, 57), (49, 57), (47, 58), (45, 58)]
SEG_B = [(44, 59), (43, 60), (41, 60), (40, 60), (38, 60), (36, 60),
         (33, 60), (31, 60), (28, 60), (26, 60), (23, 60), (20, 60),
         (17, 60), (15, 59), (14, 58), (13, 57), (12, 55), (11, 54),
         (10, 52), (9, 50), (8, 48), (8, 46), (7, 44), (7, 42), (6, 40),
         (6, 38), (7, 36), (8, 35), (9, 34), (11, 34), (13, 34)]
SEG_C = [(14, 33), (16, 33), (18, 33), (20, 33), (21, 32), (24, 32),
         (27, 32), (30, 32), (33, 32), (36, 32), (38, 32), (39, 32)]
ROUTE = SEG_A1 + SEG_A2 + SEG_B + SEG_C
GATE_PK1 = len(SEG_A1)                                  # 7
GATE_TWS = GATE_PK1 + len(SEG_A2)                       # 17
GATE_TWNW = GATE_TWS + len(SEG_B)                       # 48
GATE_GUARD = GATE_TWNW + len(SEG_C)                     # 60
# 顺序门闩: (阶段名, 门索引, 是否只看 melee)
GATES = [('PK1', GATE_PK1, False), ('TWS', GATE_TWS, False),
         ('TWNW', GATE_TWNW, False), ('GUARD', GATE_GUARD, True)]
HOLD_TWS = [(45, 58), (47, 58), (46, 57), (44, 57), (47, 57), (45, 57)]
HOLD_TWNW = [(12, 34), (11, 34), (12, 33), (13, 35), (11, 35)]
# v1.3: 门候安全位 (钳制单位不得越过当前门; 全部 d²塔>16 / d²敌兵>16 / d²建筑>9)
GATE_HOLD = {'PK1': [(62, 51), (61, 52), (62, 49), (63, 50), (62, 48)],
             'TWS': HOLD_TWS, 'TWNW': HOLD_TWNW, 'GUARD': HOLD_GUARD}
HOLD_GUARD = [(35, 32), (34, 32), (36, 33), (33, 32)]   # 全部 d²守军>9, 离建筑>9
RAM_HOLD_GUARD = [(34, 32), (35, 32)]
ASSEMBLE = (39, 32)   # GUARD 集结判定心 (d²≤100)
CHEW_POSTS = {'ram': [(40, 28), (39, 29)],
              'pike': [(39, 27), (40, 27), (41, 28), (41, 27)],
              'scout': [(38, 27), (40, 29)]}

_t0 = time.time()
_dead = False
stage = 'MARCH'
stage_t = 0
stage_ar = 0
seg = SEG_A1
assign = {}
hop_i = {}
issue = {}
lastpos = {}
ghosts = set()
_combat_seen = set()
_combat_m = re.compile(r'\[combat\] p1 type(\d+) died at \((\d+),(\d+)\) ar=(\d+)')
death_marks = []
breaker_n = 0
hold_until = 0.0
gate_i = 0
prev_my = -1
_dry_i = 0


def log(m):
    print(f"[{time.time() - _t0:7.1f}s] {m}", flush=True)


def send(cmds):
    global _dead
    if not cmds:
        return
    if DRY:
        for c in cmds:
            log(f'[dry-send] {c}')
        return
    script = "; ".join(f"echo '{c}' > {FIFO}" for c in cmds)
    try:
        subprocess.run(["sh", "-c", script], timeout=6.0)
    except subprocess.TimeoutExpired:
        _dead = True
        log('FIFO 无读者(进程退场?)')


def aistate():
    global _dead, _dry_i
    if DRY:
        if DRY_SEQ:
            files = sorted(f for f in os.listdir(DRY_SEQ)
                           if f.startswith('dry-seq-'))
            if not files:
                return None
            f = files[min(_dry_i, len(files) - 1)]
            _dry_i += 1
            try:
                with open(os.path.join(DRY_SEQ, f)) as fh:
                    return json.load(fh)
            except Exception:
                return None
        return None
    for _ in range(4):
        if _dead:
            return None
        try:
            subprocess.run(["sh", "-c", f"echo 'aistate' > {FIFO}"],
                           timeout=4.0)
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


def idle(u):
    return list(u['target']) == [u['tile'][0], u['tile'][1]]


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


def combat_new_deaths():
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
        pool.remove(best)
        gt = (best['type'], tuple(best['tile']))
        ghosts.add(gt)
        marked.append(gt)
        log(f'  ☠[combat] type{dty}@{dxy} → pg {tuple(best["tile"])} 记亡')
    return marked


def filter_ghosts(foes):
    out = []
    for u in foes:
        t = tuple(u['tile'])
        if any(gty == u['type'] and d2(t, gxy) <= 2 for gty, gxy in ghosts):
            continue
        out.append(u)
    return out


def building_at(ebs, tile):
    for b in ebs:
        if tuple(b['tile']) == tuple(tile):
            return b
    return None


def set_target(u, tgt, tag=''):
    s = u['slot']
    tgt = (max(1, min(62, tgt[0])), max(1, min(62, tgt[1])))
    if list(u['target']) == list(tgt):
        return
    rec = issue.get(s)
    if rec and rec[0] == tgt and rec[2] == [u['tile'][0], u['tile'][1]]:
        rec[1] += 1
        if rec[1] >= 8:
            alt = (max(1, min(62, tgt[0] + (1 if s % 2 else -1))),
                   max(1, min(62, tgt[1] + (1 if s % 3 else -1))))
            send([f'retask {s} {alt[0]} {alt[1]}'])
            issue[s] = [alt, 0, list(u['tile'])]
            log(f'  s{s} STUCK→{alt} {tag}')
        return
    issue[s] = [tgt, 1, list(u['tile'])]
    send([f'retask {s} {tgt[0]} {tgt[1]}'])


def follow_seg(u, sgm, tag):
    """无 cap 版: 单位按自身 hop 前进; 阶段切换点由 seg_done 兜底。"""
    s = u['slot']
    i = hop_i.get(s, 0)
    idl = idle(u)
    lp = lastpos.setdefault(s, [tuple(u['tile']), 0])
    if tuple(u['tile']) == lp[0]:
        lp[1] += 1
    else:
        lp[0], lp[1] = tuple(u['tile']), 0
    stuck = (not idl) and lp[1] >= 20
    while i < len(sgm) and d2(tuple(u['tile']), sgm[i]) <= 2 and idl:
        i += 1
        lp[1] = 0
        idl = idle(u)
    if i >= len(sgm):
        hop_i[s] = i
        return True
    hop_i[s] = i
    if stuck:
        i2 = min(i + 1, len(sgm) - 1)
        hop_i[s] = i2
        set_target(u, sgm[i2], tag + '/冻跳')
        return False
    set_target(u, sgm[i], tag)
    return False


def seg_done(sgm, mine):
    return bool(mine) and all(hop_i.get(u['slot'], 0) >= len(sgm)
                              for u in mine)


def resume_seg(sgm, mine):
    """阶段切换后: 各单位 hop_i = 距自身最近的 seg 格 (不回头走)。"""
    for u in mine:
        best, bi = 1 << 30, 0
        for j, t in enumerate(sgm):
            dd = d2(tuple(u['tile']), t)
            if dd < best:
                best, bi = dd, j
        hop_i[u['slot']] = bi


def hold_at(u, tiles, tag):
    t = tuple(u['tile'])
    if any(d2(t, h) <= 2 for h in tiles):
        return
    near = min(tiles, key=lambda h: d2(t, h))
    set_target(u, near, tag)


def focus_all(units6, foe, tag):
    for u in units6:
        set_target(u, tuple(foe['tile']), f'{tag}/focus')


def main():
    global stage, stage_t, stage_ar, prev_my, hold_until, breaker_n
    global death_marks, gate_i
    log(f'm0drv v1.3 start DRY={DRY} W={W}')
    inited = False
    while True:
        if time.time() - _t0 > TIMEOUT:
            log('TIMEOUT 止损退出')
            return 1
        r = result()
        if r:
            log(f'RESULT: {r}')
            return 0
        stt = aistate()
        if not stt:
            if _dead:
                log('aistate 链路死, 退出')
                return 1
            time.sleep(POLL)
            continue
        tick = stt['tick']
        mine = [u for u in stt['units'] if u['p'] == 0]
        ebs = stt.get('buildingRecs', [])
        if stage_t == 0:
            log(f't={tick} [{stage}] my={len(mine)} eb={len(ebs)}')
            stage_ar = tick
        elif stage_t % 150 == 0:
            hops = sorted(hop_i.values())
            log(f't={tick} [{stage}] 进度 hop_min={hops[0] if hops else "?"} '
                f'my={len(mine)} eb={len(ebs)}')
        stage_t += 1
        now = time.time()
        if prev_my >= 0 and len(mine) < prev_my:
            death_marks.extend([now] * (prev_my - len(mine)))
        prev_my = len(mine)
        death_marks = [t for t in death_marks if now - t <= 45]
        if len(death_marks) >= 3 and breaker_n < 2:
            breaker_n += 1
            death_marks = []
            hold_until = now + 40
            log(f't={tick} !!! 熔断#{breaker_n}: 45s 丢3+ → hold40s my={len(mine)}')
        if not mine:
            log(f't={tick} 我方全灭 (等通用判负)')
            time.sleep(POLL)
            continue
        if not inited:
            resume_seg(ROUTE, mine)   # 重挂/续跑: hop_i=各自最近路线点
            inited = True
            log('hop_i argmin 初始化: ' +
                ' '.join(f"s{u['slot']}={hop_i.get(u['slot'], '?')}"
                         for u in mine))
        pikes = [u for u in mine if u['type'] == 2]
        scouts = [u for u in mine if u['type'] == 5]
        rams = [u for u in mine if u['type'] == 7]
        melee = pikes + scouts
        foes_all = [u for u in stt['units'] if u['p'] == 1]
        mark_dead_from_combat(foes_all)
        foes = filter_ghosts(foes_all)
        if now < hold_until:
            time.sleep(POLL)
            continue

        if stage == 'MARCH':
            gname, gidx, gmelee = GATES[gate_i] if gate_i < len(GATES) \
                else (None, None, None)
            for u in mine:
                hi = hop_i.get(u['slot'], 0)
                if gidx is not None and hi >= gidx:
                    hold_at(u, GATE_HOLD[gname], f'MARCH/{gname}门候')
                else:
                    follow_seg(u, ROUTE, 'MARCH')
            # 顺序门闩: 只按当前待触发门判定, 触发后推进指针
            if gate_i < len(GATES):
                gname, gidx, gmelee = GATES[gate_i]
                hmin = min((hop_i.get(u['slot'], 0)
                            for u in (melee if gmelee else mine)),
                           default=999)
                if hmin >= gidx:
                    if gname == 'PK1':
                        pk1_there = any(tuple(g['tile']) == PK1
                                        for g in foes)
                        pk1_dead = any(g[0] == 2 and d2(g[1], PK1) <= 2
                                       for g in ghosts)
                        if pk1_there and not pk1_dead:
                            log(f'门控 PK1 (hmin={hmin}) → PK1')
                            stage, stage_t = 'PK1', 0
                            continue
                        gate_i += 1      # pk1 已没了: 跳过本门
                        log(f'门控 PK1 跳过 (pk1 已亡) hmin={hmin}')
                        continue
                    log(f'门控 {gname} (hmin={hmin}) → {gname}')
                    gate_i += 1
                    stage, stage_t = gname, 0
                    continue
        elif stage == 'PK1':
            tgt = [g for g in foes if tuple(g['tile']) == PK1]
            dead = any(g[0] == 2 and d2(g[1], PK1) <= 2 for g in ghosts)
            if dead or not tgt:
                if dead or stage_t > 2:
                    log(f'PK1 清完 (dead={dead}) → 回 MARCH')
                    stage, stage_t = 'MARCH', 0
                    continue
            else:
                focus_all(melee, tgt[0], 'PK1')
        elif stage == 'TWS':
            tw = building_at(ebs, TWS)
            if tw is None or tw.get('hp', 0) <= 0:
                log('twS 平 → 回 MARCH')
                stage, stage_t = 'MARCH', 0
                continue
            for u in rams:
                set_target(u, TWS, 'TWS/ram')
            others = pikes + scouts
            pk4 = [g for g in foes if tuple(g['tile']) == PK4]
            if pk4:
                focus_all(others, pk4[0], 'PK4')
            else:
                for u in others:
                    hold_at(u, HOLD_TWS, 'TWS/hold')
        elif stage == 'TWNW':
            tw = building_at(ebs, TWNW)
            if tw is None or tw.get('hp', 0) <= 0:
                log('twNW 平 → 回 MARCH')
                stage, stage_t = 'MARCH', 0
                continue
            for u in rams:
                set_target(u, TWNW, 'TWNW/ram')
            for u in pikes + scouts:
                hold_at(u, HOLD_TWNW, 'TWNW/hold')
        elif stage == 'GUARD':
            gs = [g for g in foes
                  if any(d2(tuple(g['tile']), gt) <= 25 for gt in GUARDS)]
            if not gs:
                log('守军清空 → CHEW')
                stage, stage_t = 'CHEW', 0
                continue
            spread = [u for u in melee
                      if d2(tuple(u['tile']), ASSEMBLE) > 100]
            if spread:
                for u in mine:
                    if u['type'] == 7:
                        hold_at(u, RAM_HOLD_GUARD, 'GUARD/ramhold')
                    elif u in spread:
                        follow_seg(u, ROUTE, 'GUARD/收拢')
                    else:
                        hold_at(u, HOLD_GUARD, 'GUARD/hold')
            else:
                tgt = min(gs, key=lambda g: (g['slot'], tuple(g['tile'])))
                focus_all(melee, tgt, 'GUARD')
                for u in rams:
                    hold_at(u, RAM_HOLD_GUARD, 'GUARD/ramhold')
        elif stage == 'CHEW':
            tc = building_at(ebs, TC)
            if tc is None or tc.get('hp', 0) <= 0:
                log(f't={tick} TC 平! 等 [result]')
                time.sleep(POLL * 2)
                continue
            for u in rams:
                a = CHEW_POSTS['ram']
                set_target(u, a[u['slot'] % len(a)], 'CHEW/ram')
            for i, u in enumerate(pikes):
                set_target(u, CHEW_POSTS['pike'][i % 4], 'CHEW/pk')
            for i, u in enumerate(scouts):
                set_target(u, CHEW_POSTS['scout'][i % 2], 'CHEW/sc')
        time.sleep(POLL)


if __name__ == '__main__':
    sys.exit(main())
