#!/usr/bin/env python3
"""m1drv 端到端 dry 测试（隔离工作目录 /tmp/aoe-camp/m1v2/drysim — r67 事故纪律:
dry 工具绝不碰活局 play.log）。feeder 模拟: 单位向 target 每拍走 1 格(贪心 8 向);
≥1 军事贴敌 d²≤2 满 3 拍 → 敌死(写 [combat] 进 drysim/play.log);
walker 静止在胜利区 x[50,57)y[57,64) 满 2 拍 → [result] WIN。
布阵=探针局 aistate 实录(p0 slot0 村民@31,58 等; p1 16 守敌)。
PASS 判据: 驱动 stdout 见 PH0→SWEEP→WALK 全程 + RESULT: WIN。"""
import json
import os
import re
import subprocess
import sys
import time

W = '/tmp/aoe-camp/m1v2/drysim'
SEQ = W + '/dry-live'
os.makedirs(SEQ, exist_ok=True)
for f in os.listdir(SEQ):
    os.remove(os.path.join(SEQ, f))
for f in ('play.log', 'dry-drv.out'):
    p = os.path.join(W, f)
    if os.path.exists(p):
        os.remove(p)

RECT = (50, 57, 57, 64)


def u(p, s, t, tile):
    return {'p': p, 'slot': s, 'type': t, 'tile': list(tile),
            'prevTile': list(tile), 'target': list(tile), 'hp': 255,
            'action': 0, 'sel': False}


# 探针局实录布阵（2026-09-06 aistate）
MINE = [u(0, 0, 0, (31, 58)), u(0, 1, 0, (31, 59)), u(0, 2, 0, (32, 59)),
        u(0, 3, 3, (29, 59)), u(0, 4, 3, (30, 59)), u(0, 5, 3, (30, 60)),
        u(0, 6, 3, (29, 60)), u(0, 7, 4, (30, 57)), u(0, 8, 4, (30, 56)),
        u(0, 9, 4, (30, 55)), u(0, 10, 6, (28, 58))]
FOES = [u(1, 0, 3, (48, 48)), u(1, 1, 3, (40, 34)), u(1, 2, 3, (60, 55)),
        u(1, 3, 3, (61, 45)), u(1, 4, 3, (49, 24)), u(1, 5, 3, (48, 25)),
        u(1, 6, 3, (54, 19)), u(1, 7, 4, (36, 23)), u(1, 8, 4, (37, 22)),
        u(1, 9, 4, (49, 33)), u(1, 10, 3, (32, 17)), u(1, 11, 4, (25, 21)),
        u(1, 12, 3, (22, 26)), u(1, 13, 3, (23, 27)), u(1, 14, 3, (15, 47)),
        u(1, 15, 3, (16, 54))]
EBS = ([{'p': 0, 'slot': i, 'type': t, 'tile': list(tl), 'hp': 255, 'uc': False}
        for i, (t, tl) in enumerate(
            [(9, (53, 59)), (12, (53, 57)), (12, (53, 61)), (12, (55, 59)),
             (12, (51, 59))])]
       + [{'p': 1, 'slot': j, 'type': 12, 'tile': list(tl), 'hp': 255,
           'uc': False} for j, tl in
          enumerate([(48, 50), (50, 50), (36, 37), (49, 52), (16, 36)])])
tick = [400]
idx = [0]
contact = {}


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def write():
    st = {'tick': tick[0], 'aA': 6, 'gameMode': 32,
          'techFlags': [0] * 127,
          'players': [
              {'age': 0, 'units': len(MINE), 'popCap': 30, 'buildings': 5,
               'res': [0, 0, 0], 'tcTile': (53 << 8) | 59, 'hq9': -1,
               'trainQueue': 0, 'armyValue': 0, 'produced': 0, 'lost': 0,
               'built': 5, 'buildingsLost': 0, 'deliveries': 0},
              {'age': 0, 'units': len(FOES), 'popCap': 30, 'buildings': 5,
               'res': [0, 0, 0], 'tcTile': -1, 'hq9': -1, 'trainQueue': 0,
               'armyValue': 0, 'produced': 0, 'lost': 0, 'built': 5,
               'buildingsLost': 0, 'deliveries': 0}],
          'explored': 4096,
          'units': MINE + FOES, 'buildingRecs': EBS}
    json.dump(st, open(f'{SEQ}/dry-seq-{idx[0]:04d}.json', 'w'))
    idx[0] += 1


def combat(line):
    with open(W + '/play.log', 'a') as f:
        f.write(line + '\n')


def step():
    tick[0] += 25
    # 移动: 向 target 走 1 格 (贪心 8 向)
    for uu in MINE + FOES:
        tx, ty = uu['target']
        x, y = uu['tile']
        dx, dy = tx - x, ty - y
        if dx == 0 and dy == 0:
            continue
        uu['tile'] = [x + (dx > 0) - (dx < 0), y + (dy > 0) - (dy < 0)]
    # 清障战: 军事贴敌 d²≤2 满 3 拍 → 敌死
    for foe in list(FOES):
        near = [m for m in MINE if m['type'] >= 2
                and d2(m['tile'], foe['tile']) <= 2]
        if near:
            contact[foe['slot']] = contact.get(foe['slot'], 0) + 1
        else:
            contact[foe['slot']] = 0
        if contact.get(foe['slot'], 0) >= 3:
            combat(f'[combat] p1 type{foe["type"]} died at '
                   f'({foe["tile"][0]},{foe["tile"][1]}) ar={tick[0]} '
                   f'remaining={len(FOES)-1}')
            FOES.remove(foe)
    # WIN: walker(slot0) 静止在胜利区 满 2 拍
    wk = MINE[0]
    x, y = wk['tile']
    tx, ty = wk['target']
    if RECT[0] <= x < RECT[2] and RECT[1] <= y < RECT[3] and (x, y) == (tx, ty):
        contact['win'] = contact.get('win', 0) + 1
        if contact['win'] >= 2:
            combat(f'[result] WIN ticks={tick[0]}')
    else:
        contact['win'] = 0


def consume_cmds():
    try:
        with open(W + '/dry-drv.out') as f:
            txt = f.read()
    except FileNotFoundError:
        return
    for m in re.finditer(r'retask (\d+) (\d+) (\d+)', txt):
        s, x, y = int(m.group(1)), int(m.group(2)), int(m.group(3))
        pool = [q for q in MINE if q['slot'] == s]
        if pool:
            pool[0]['target'] = [x, y]


def main():
    cap = float(sys.argv[1]) if len(sys.argv) > 1 else 180
    env = dict(os.environ, M1_DRY='1', M1_DRY_SEQ=SEQ, M1_POLL='0.02',
               M1_TIMEOUT=str(cap + 30))
    p = subprocess.Popen([sys.executable, '/tmp/aoe-camp/m1v2/m1drv.py', W],
                         env=env, stdout=open(W + '/dry-drv.out', 'w'),
                         stderr=subprocess.STDOUT)
    t0 = time.time()
    write()
    while time.time() - t0 < cap:
        if p.poll() is not None:
            break
        consume_cmds()
        step()
        write()
        time.sleep(0.02)
    else:
        p.terminate()
    time.sleep(0.3)
    out = open(W + '/dry-drv.out').read()
    keep = re.findall(r'\] (t=\d+ \[[A-Z0-9]+\][^\n]*|锚\([^\n]*|t=\d+ sweep[^\n]*'
                      r'|t=\d+ walk[^\n]*|RESULT[^\n]*|TIMEOUT[^\n]*'
                      r'|!!![^\n]*|t=\d+ SWEEP[^\n]*|t=\d+ PH0[^\n]*)', out)
    print('\n'.join(keep))
    print('----')
    print('retask 总数=', len(re.findall(r'\[dry-send\] retask', out)))
    if (re.search(r'RESULT: \[result\] WIN', out) and 'sweep 11/11' in out
            and 'walk 15/15' in out):
        print('DRY-E2E: PASS (PH0→SWEEP 11/11→WALK 15/15→WIN)')
        return 0
    print('DRY-E2E: FAIL')
    return 1


if __name__ == '__main__':
    sys.exit(main())
