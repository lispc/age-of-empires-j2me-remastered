#!/usr/bin/env python3
"""dryv8 — v8.0 帖扫链 dry (M6_DRY=1 假 fifo 闸, 仓库只读, 产物落工作目录)。

场景 A [S5 帖扫→团杀A→收队→团杀B→猫清→(2,5)解锁→转段]:
  帖扫开局 → 三塔移除(模拟帖扫烧平) → 停滞 → 团杀 gate (猫离活塔>24,
  melee≥4) → 集火猫A一只 → [combat] 猫A死 → 团杀A得手收队 → 团杀B →
  猫B消失 → 猫清回帖扫 → blocked 全口袋外 → 转段 S6。
场景 B [S2 兜底豁免]: melee=2 不足, 从未上钩 → S2T_SILENT 到点 →
  留守豁免转 S3 (禁现: 无 S2 转杀)。
"""
import json
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
FIX = os.path.join(HERE, 'dry-fixture.json')
LOG = os.path.join(HERE, 'dry-drv-v8.log')
DRV = os.path.join(HERE, 'm6hdrv.py')
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


def s5_fixture():
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
    foes = [unit(1, 0, (3, 5), 8), unit(1, 1, (4, 7), 8)]
    return {'tick': 17000, 'units': mine + foes, 'buildingRecs': ebs}


def s2_fixture():
    mine = [unit(0, i, pos, 3) for i, pos in
            enumerate([(34, 34), (33, 35), (34, 33)])]
    foes = [unit(1, 9, (26, 30), 3), unit(1, 10, (27, 29), 3)]
    ebs = [{'p': 1, 'slot': i, 'type': ty, 'tile': list(t), 'hp': 255,
            'uc': False} for i, (ty, t) in enumerate(
        [(12, (26, 28)), (12, (22, 50))])]
    return {'tick': 9000, 'units': mine + foes, 'buildingRecs': ebs}


def find(fx, p, slot):
    for u in fx['units']:
        if u['p'] == p and u['slot'] == slot:
            return u
    raise KeyError((p, slot))


def remove(fx, p, slot):
    fx['units'] = [u for u in fx['units']
                   if not (u['p'] == p and u['slot'] == slot)]


def log_text():
    with open(LOG, errors='replace') as f:
        return f.read()


def combat_line(ty, x, y, ar):
    with open(PLAY, 'a') as f:
        f.write(f'[combat] p1 type{ty} died at ({x},{y}) ar={ar} '
                f'remaining=12\n')


def run_scene(name, fx, stage, director, timeout, extra_env=None):
    subprocess.run(['pkill', '-f', 'm6h/m6hdrv.py'], capture_output=True)
    time.sleep(0.3)
    json.dump(fx, open(FIX, 'w'))
    if os.path.exists(PLAY):
        os.remove(PLAY)
    env = dict(os.environ, M6_DRY='1', M6_DRY_STATE=FIX, M6_POLL='0.05',
               M6_TIMEOUT=str(timeout), M6_STAGE=stage)
    env.update(extra_env or {})
    proc = subprocess.Popen([sys.executable, DRV, HERE], env=env,
                            stdout=open(LOG, 'w'), stderr=subprocess.STDOUT)
    t0 = time.time()
    tick_base = 17000 if stage == 'S5' else 9000
    try:
        while proc.poll() is None:
            el = time.time() - t0
            fx = json.load(open(FIX))
            fx['tick'] = tick_base + int(el * 100)
            director(fx, el, log_text())
            json.dump(fx, open(FIX, 'w'))
            time.sleep(0.05)
    finally:
        if proc.poll() is None:
            proc.kill()
        proc.wait()
    out = log_text()
    open(os.path.join(HERE, f'dry-drv-v8-{stage}.log'), 'w').write(out)
    return out


def check(out, checks):
    ok = True
    for name, passed in checks:
        print(('PASS ' if passed else 'FAIL ') + name)
        ok = ok and passed
    return ok


def main():
    with open(MAP, 'w') as f:
        f.write('# mapdump (dry)\n')
        for y, row in enumerate(MAP_ROWS):
            f.write(f'{y if y >= 10 else " " + str(y)}{row}\n')

    # ---- 场景 A: S5 帖扫→团杀→转段 ----
    steps = [0]

    def s5_dir(fx, el, out):
        if steps[0] == 0 and el > 2.0:
            assert 'S5 顶廊帖扫开局' in out, '帖扫开局未发'
            assert 'S5帖' in out, '弓未上帖'
            # 帖扫烧平口袋三塔 (v8.0 tws 门控按活塔算)
            fx['buildingRecs'] = [b for b in fx['buildingRecs']
                                  if tuple(b['tile']) not in
                                  ((6, 4), (5, 7), (2, 8))]
            steps[0] = 1
        elif steps[0] == 1 and el > 7.5:
            assert '停滞 → 近战团杀开局' in out, '团杀未触发'
            assert '集火猫A(4, 7)' in out, '猫A 选择错 (应取近 DUEL_REST 者)'
            assert out.count('团杀A') >= 1, '近战未贴猫A'
            steps[0] = 2
        elif steps[0] == 2 and el > 9.5:
            combat_line(8, 4, 7, 17200)      # 猫A 死 (combat 差分)
            steps[0] = 3
        elif steps[0] == 3 and el > 11.5:
            assert '团杀 A 得手' in out, 'A 死未收队'
            steps[0] = 4
        elif steps[0] == 4 and el > 14.5:
            assert out.count('停滞 → 近战团杀开局') >= 2, '团杀B 未再开局'
            remove(fx, 1, 0)                 # 猫B(s0) 消失 (纯差分路径)
            steps[0] = 5
        elif steps[0] == 5 and el > 17.5:
            assert '猫全灭/近战耗尽' in out, '猫灭未回帖扫'
            steps[0] = 6

    print('==== 场景 A: S5 帖扫→团杀A→收队→团杀B→转段 ====')
    out5 = run_scene('S5', s5_fixture(), 'S5', s5_dir, 45,
                     {'M6_STALL': '1.5'})
    c5 = [
        ('S5 顶廊帖扫开局 + BAD_TILES 重建', 'S5 顶廊帖扫开局' in out5
         and 'BAD_TILES' in out5),
        ('archers 上安全帖', 'S5帖' in out5),
        ('停滞→团杀开局 (猫离活塔>24 gate)', '停滞 → 近战团杀开局' in out5),
        ('集火猫A一只 (不再 i%len 分摊)', '集火猫A(4, 7)' in out5),
        ('[combat] 差分记猫A死', out5.count('☠[combat] type8') >= 1),
        ('团杀A得手 → 收队 (转决斗口径)', '团杀 A 得手' in out5),
        ('团杀B 再开局', out5.count('停滞 → 近战团杀开局') >= 2),
        ('猫清回帖扫', '猫全灭/近战耗尽' in out5),
        ('blocked 全口袋外 → 转段 S6', 'S5 完成: 口袋清/解锁' in out5
         and 'S6 收顶廊' in out5),
        ('禁现: 无 drag/钓鱼/会合区无解', 'drag' not in out5
         and '钓鱼开局' not in out5 and '会合区无解' not in out5),
    ]
    print(out5[-2200:])
    ok5 = check(out5, c5)

    # ---- 场景 B: S2 兜底豁免 ----
    print()
    print('==== 场景 B: S2 melee=2 从未上钩 → 留守豁免 ====')
    out2 = run_scene('S2', s2_fixture(), 'S2', lambda fx, el, out: None, 30,
                     {'M6_S2T_SILENT': '3'})
    c2 = [
        ('S2 引离开局 (饵+埋伏)', 'S2 引离开局' in out2),
        ('兜底豁免 (无硬打)', 'S2 兜底豁免' in out2 and '留守门钉转 S3' in out2),
        ('转入 S3', 'S3' in out2),
        ('禁现: 无 S2 转杀', 'S2 转杀' not in out2),
    ]
    print(out2[-1500:])
    ok2 = check(out2, c2)

    print('DRYV8', 'ALL PASS' if (ok5 and ok2) else 'HAS FAILURES')
    return 0 if (ok5 and ok2) else 1


if __name__ == '__main__':
    sys.exit(main())
