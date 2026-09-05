#!/usr/bin/env python3
"""drysweep — v7.2 顶廊帖扫+盾猫清障 dry (M6_DRY=1 假 fifo 闸)。

链: 帖扫开局 (claims 安全帖) → 停滞 → 盾先猫后决斗开局 (塔安全位) →
盾就位→确认→猫进盾后 → 盾猫就位 → 掉火接战 → 猫1死 (combat 差分) →
决斗2 → 猫2死 → 猫清回帖扫 → (2,5) 解锁 → blocked 全口袋外 → 转段 S6。
"""
import json
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
FIX = os.path.join(HERE, 'dry-fixture.json')
LOG = os.path.join(HERE, 'dry-drv-sweep.log')
DRV = os.path.join(HERE, 'm6gdrv.py')
MAP = os.path.join(HERE, 'map-s5.txt')
PLAY = os.path.join(HERE, 'play.log')

MAP_ROWS = [
    '.S.................',
    '.B#B.#.............',
    '.B.B#B..B..........',
    '.##................',
    '..B.B.#............',
    '.B#..e.Be..........',
    '....B..............',
    '...e.#e............',
    '..#................',
    '....e..............',
]


def unit(p, slot, tile, t, hp=255):
    return {'p': p, 'slot': slot, 'tile': list(tile), 'prevTile': list(tile),
            'target': list(tile), 'type': t, 'hp': hp, 'action': 0,
            'sel': False}


def fixture():
    mine = [unit(0, i, pos, 3) for i, pos in
            enumerate([(14, 1), (15, 2), (16, 1), (14, 2), (16, 2)])]
    mine += [unit(0, 5 + i, pos, 8) for i, pos in
             enumerate([(15, 1), (16, 1), (15, 2)])]          # 3 猫
    mine += [unit(0, 8 + i, pos, 7) for i, pos in
             enumerate([(17, 1), (17, 2), (18, 1)])]          # 3 冲车
    mine += [unit(0, 11 + i, pos, 4) for i, pos in
             enumerate([(13, 1), (14, 1), (15, 1), (16, 1), (17, 1)])]
    EBS = [(9, (4, 4)), (3, (4, 2)), (10, (2, 1)), (7, (1, 3)), (4, (2, 5)),
           (2, (2, 3)), (6, (5, 1)), (8, (1, 1)), (12, (2, 8)), (12, (5, 7)),
           (12, (6, 4)), (12, (26, 28)), (12, (22, 50))]
    ebs = [{'p': 1, 'slot': i, 'type': ty, 'tile': list(t), 'hp': 255,
            'uc': False} for i, (ty, t) in enumerate(EBS)]
    foes = [unit(1, 0, (4, 6), 8), unit(1, 1, (4, 7), 8)]
    return {'tick': 17000, 'units': mine + foes, 'buildingRecs': ebs}


def find(fx, p, slot):
    for u in fx['units']:
        if u['p'] == p and u['slot'] == slot:
            return u
    raise KeyError((p, slot))


def remove(fx, p, slot):
    fx['units'] = [u for u in fx['units']
                   if not (u['p'] == p and u['slot'] == slot)]


def place(fx, p, slot, tile, target=None):
    u = find(fx, p, slot)
    u['tile'] = list(tile)
    u['target'] = list(target if target is not None else tile)


def log_text():
    try:
        with open(LOG, errors='replace') as f:
            return f.read()
    except Exception:
        return ''


def combat_line(ty, x, y, ar):
    with open(PLAY, 'a') as f:
        f.write(f'[combat] p1 type{ty} died at ({x},{y}) ar={ar} remaining=12\n')


def main():
    with open(MAP, 'w') as f:
        f.write('# mapdump (dry)\n')
        for y, row in enumerate(MAP_ROWS):
            f.write(f'{y if y >= 10 else " " + str(y)}{row}\n')
    if os.path.exists(PLAY):
        os.remove(PLAY)
    json.dump(fixture(), open(FIX, 'w'))
    env = dict(os.environ, M6_DRY='1', M6_DRY_STATE=FIX, M6_POLL='0.05',
               M6_TIMEOUT='45', M6_STAGE='S5', M6_STALL='1.5')
    proc = subprocess.Popen([sys.executable, DRV, HERE], env=env,
                            stdout=open(LOG, 'w'), stderr=subprocess.STDOUT)
    t0 = time.time()
    step = [0]
    try:
        while proc.poll() is None:
            el = time.time() - t0
            fx = json.load(open(FIX))
            fx['tick'] = 17000 + int(el * 100)
            out = log_text()
            if step[0] == 0 and el > 2.0:
                assert 'S5 顶廊帖扫开局' in out, '帖扫开局未发'
                assert 'S5帖' in out, '弓未上帖'
                # 帖扫烧平 NW 双塔 (6,4)(5,7)
                fx['buildingRecs'] = [b for b in fx['buildingRecs']
                                      if tuple(b['tile']) not in
                                      ((6, 4), (5, 7))]
                step[0] = 1
            elif step[0] == 1 and el > 4.5:
                assert '停滞 → 近战团杀开局' in out, '团杀未触发'
                assert '团杀' in out, '近战未贴猫'
                step[0] = 2
            elif step[0] == 2 and el > 7.0:
                combat_line(8, 4, 6, 17200)      # 猫1 死 (combat 差分)
                step[0] = 3
            elif step[0] == 3 and el > 9.0:
                remove(fx, 1, 1)                 # 猫2 消失 (纯差分路径)
                step[0] = 4
            elif step[0] == 4 and el > 11.5:
                assert '猫全灭 — 通道开' in out, '猫灭未转回帖扫'
                step[0] = 5
            json.dump(fx, open(FIX, 'w'))
            time.sleep(0.05)
    finally:
        if proc.poll() is None:
            proc.kill()
        proc.wait()
    out = log_text()
    checks = [
        ('S5 顶廊帖扫开局 + BAD_TILES 加载', 'S5 顶廊帖扫开局' in out
         and 'BAD_TILES' in out),
        ('archers 上安全帖', 'S5帖' in out),
        ('停滞→近战团杀开局 (塔清门控)', '停滞 → 近战团杀开局' in out),
        ('[combat] 差分记猫死', out.count('☠[combat] type8') >= 1),
        ('猫清回帖扫 / (2,5) 解锁', '猫全灭' in out or '决斗胜' in out),
        ('blocked 全口袋外 → 转段 S6', 'S5 完成: 口袋清/解锁' in out
         and 'S6 收顶廊' in out),
        ('禁现: 无 drag/钓鱼/会合区', 'drag' not in out
         and '钓鱼开局' not in out and '会合区无解' not in out),
    ]
    print(out[-2600:])
    ok = True
    for name, passed in checks:
        print(('PASS ' if passed else 'FAIL ') + name)
        ok = ok and passed
    print('DRYSWEEP', 'ALL PASS' if ok else 'HAS FAILURES')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
