#!/usr/bin/env python3
"""m5 守城关驱动 v3（run2/3 尸检改）：
败因：v2 岗位分散=接战即 1v1 散兵（骑士波 5:3 惨换），残部被投石机点名。
v3 战术：单点集火——引擎攻击按目标格结算(resolveAttack 读 slot[5] 格)，
全体自由单位 retask 到同一敌 tile = 集火，一个一个点名。
- 布防：骑兵(t6)×5=密集前哨簇（北 (37-39,33) / 南 (37-39,55)），波间预置下一侧。
- 剑士(t3)×5=城堡环（半径 2）。
- 接战门（防风筝）：仅当 敌 d2≤100(10格)贴任意自由单位 或 敌 d2≤25 贴城堡
  (炮击态=静止) 时全局集火该敌（取离城堡最近者=最前排）；否则守岗位不放令。
- action==1 接敌中不打断；已在途同目标不复发。"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m5a'
FIFO = W + '/fifo'
AIS = W + '/fifo.aistate.json'
LOG = W + '/play.log'
POLL = float(os.environ.get('M5_POLL', '0.3'))
TIMEOUT = float(os.environ.get('M5_TIMEOUT', '420'))
CASTLE = (37, 47)
RING = [(37, 45), (39, 47), (37, 49), (35, 47), (38, 48)]
CLUMP = {'N': [(37, 33), (38, 33), (39, 33), (38, 32), (38, 34)],
         'S': [(37, 55), (38, 55), (39, 55), (38, 56), (38, 54)],
         'W': [(25, 46), (25, 47), (24, 46), (24, 48), (26, 47)]}
SIDE_SEQ = ['N', 'S', 'N', 'S']  # wave_idx 从 base 后首波(=脚本 wave2)起数

_t0 = time.time()
_dead = False


def log(msg):
    print(f"[{time.time() - _t0:7.1f}s] {msg}", flush=True)


def cmd(c, timeout=3.0):
    global _dead
    if _dead:
        return False
    try:
        subprocess.run(["sh", "-c", f"echo '{c}' > {FIFO}"], timeout=timeout)
        return True
    except subprocess.TimeoutExpired:
        _dead = True
        log(f'FIFO 无读者(进程退场?) at cmd={c!r}')
        return False


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


def aistate():
    for _ in range(4):
        if not cmd('aistate'):
            return None
        time.sleep(0.12)
        try:
            with open(AIS) as f:
                return json.load(f)
        except Exception:
            time.sleep(0.15)
    return None


def dist2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def side_of(enemies):
    if not enemies:
        return None
    cx = sum(e['tile'][0] for e in enemies) / len(enemies)
    cy = sum(e['tile'][1] for e in enemies) / len(enemies)
    dx, dy = cx - CASTLE[0], cy - CASTLE[1]
    if abs(dy) >= abs(dx):
        return 'N' if dy < 0 else 'S'
    return 'W' if dx < 0 else 'E'


last_tick = -1
n_retask = 0
wave_idx = 0
enemies_seen = False
cur_side = 'N'
last_order = {}
focusing = None  # 当前集火敌 (type,tile)

log('=== m5adrv v3 start ===')
while time.time() - _t0 < TIMEOUT and not _dead:
    r = result()
    if r:
        log('RESULT ' + r)
        break
    st = aistate()
    if st is None:
        if _dead:
            break
        continue
    tick = st['tick']
    if last_tick > 0 and tick < last_tick - 50:
        log(f'ar 倒退守卫 {last_tick}->{tick},弃局')
        break
    last_tick = tick
    units = st.get('units', [])
    enemies = [u for u in units if u['p'] == 1]
    castle = None
    for b in st.get('buildingRecs', []):
        if b['p'] == 0 and b['type'] == 3:
            castle = b
            break
    my = [u for u in units if u['p'] == 0 and u['type'] >= 2]
    cav = [u for u in my if u['type'] in (5, 6)]
    inf = [u for u in my if not (u['type'] in (5, 6))]

    if enemies and not enemies_seen:
        enemies_seen = True
        comp = {}
        for e in enemies:
            comp[e['type']] = comp.get(e['type'], 0) + 1
        log(f"WAVE{wave_idx + 1} SPAWN t={tick} side={side_of(enemies)} comp={comp} "
            f"castle_hp={castle['hp'] if castle else '?'} my={len(my)} "
            f"tiles={[(e['tile'][0], e['tile'][1]) for e in enemies]}")
        cur_side = side_of(enemies)
    elif not enemies and enemies_seen:
        enemies_seen = False
        wave_idx += 1
        log(f"WAVE{wave_idx} CLEAR t={tick} castle_hp={castle['hp'] if castle else '?'} my={len(my)}")
        cur_side = SIDE_SEQ[wave_idx] if wave_idx < len(SIDE_SEQ) else None
        log(f'预置下一波侧: {cur_side}')
        focusing = None

    if castle is None:
        log(f't={tick} 城堡没了 p0={len(my)}')
        time.sleep(POLL)
        continue

    line = CLUMP.get(cur_side) if cur_side else None
    assign = {}
    for i, u in enumerate(cav):
        assign[u['slot']] = line[i % len(line)] if line else RING[u['slot'] % len(RING)]
    for i, u in enumerate(inf):
        assign[u['slot']] = RING[(i + 2) % len(RING)]

    # ---- 全局集火目标 ----
    tgt = None
    if enemies:
        # 焦点滞回:旧焦点格 3 格内仍有敌 → 不换(防 retask 风暴/追移动靶)
        if focusing and any(dist2(e['tile'], focusing) <= 9 for e in enemies):
            tgt = focusing
        else:
            # 候选: 贴城堡25内(炮击/临城) 或 贴任意自由单位100内(接敌带)
            cands = []
            for e in enemies:
                near_castle = dist2(e['tile'], CASTLE) <= 25
                near_force = any(dist2(e['tile'], u['tile']) <= 100 for u in my)
                if near_castle or near_force:
                    cands.append((dist2(e['tile'], CASTLE), near_castle, e))
            if cands:
                # 炮击态(贴城堡)优先,其余取离城堡最近=最前排
                cands.sort(key=lambda c: (not c[1], c[0]))
                tgt = (cands[0][2]['tile'][0], cands[0][2]['tile'][1])
    if tgt != focusing:
        if tgt:
            log(f"t={tick} FOCUS t?@({tgt[0]},{tgt[1]}) castle_hp={castle['hp']} my={len(my)} "
                f"n_e={len(enemies)}")
        focusing = tgt

    issued = 0
    for u in my:
        s = u['slot']
        if u['action'] == 1:
            continue
        dest = tgt if tgt else assign.get(s)
        if dest is None:
            continue
        if u['target'] == list(dest) and u['tile'] == list(dest):
            continue
        if u['target'] == list(dest) and last_order.get(s) == (dest,):
            continue
        if cmd(f"retask {s} {dest[0]} {dest[1]}", timeout=2.0):
            n_retask += 1
            issued += 1
            last_order[s] = (dest,)
    time.sleep(POLL)

if not result():
    log(f'NO RESULT (dead={_dead} {time.time() - _t0:.0f}s) retasks={n_retask}')
else:
    log(f'done retasks={n_retask}')
