#!/usr/bin/env python3
"""m0drv 端到端 dry 测试: 外置 feeder 模拟移动/战斗, 驱动应一路打到 RESULT: WIN。
模拟规则: 单位向最后 retask 目标每拍走 1 格 (贪心 8 向, 不查地形);
  ≥4 近战贴敌兵 d²≤2 → 敌死 (写 [combat] 进 play.log);
  双冲车贴塔 d²≤2 → 塔 hp -8/拍; ≥6 单位贴 TC → TC -12/拍 → 0 写 [result] WIN。"""
import json
import os
import re
import subprocess
import sys
import time

W = '/tmp/aoe-camp/m0-drysim'
SEQ = W + '/dry-live'
os.makedirs(SEQ, exist_ok=True)
for f in os.listdir(SEQ):
    os.remove(os.path.join(SEQ, f))
open(W + '/play.log', 'w').close()

def u(p, s, t, tile):
    return {'p': p, 'slot': s, 'type': t, 'tile': list(tile),
            'target': list(tile), 'hp': 255}

MINE = [u(0,0,2,(59,40)),u(0,1,2,(60,40)),u(0,2,2,(61,40)),u(0,3,2,(58,40)),
        u(0,4,5,(60,41)),u(0,5,5,(60,42)),u(0,6,7,(61,37)),u(0,7,7,(60,37))]
FOES = [u(1,0,2,(38,29)),u(1,1,2,(37,29)),u(1,2,5,(30,25)),u(1,3,2,(57,51)),
        u(1,4,2,(23,49)),u(1,5,2,(34,41)),u(1,6,2,(41,61)),u(1,7,2,(16,32))]
EBS = [{'p':1,'slot':0,'type':9,'tile':[39,28],'hp':255},
       {'p':1,'slot':1,'type':0,'tile':[37,28],'hp':255},
       {'p':1,'slot':2,'type':11,'tile':[40,30],'hp':255},
       {'p':1,'slot':3,'type':5,'tile':[40,32],'hp':255},
       {'p':1,'slot':4,'type':10,'tile':[35,28],'hp':255},
       {'p':1,'slot':5,'type':12,'tile':[37,61],'hp':255},
       {'p':1,'slot':6,'type':12,'tile':[30,52],'hp':255},
       {'p':1,'slot':7,'type':12,'tile':[15,52],'hp':255},
       {'p':1,'slot':8,'type':12,'tile':[17,31],'hp':255}]
tick = [1000]
idx = [0]
segs = {7: 0, 8: 0}

def d2(a, b):
    return (a[0]-b[0])**2 + (a[1]-b[1])**2

def write():
    st = {'tick': tick[0], 'units': MINE + FOES, 'buildingRecs': EBS}
    json.dump(st, open(f'{SEQ}/dry-seq-{idx[0]:04d}.json', 'w'))
    idx[0] += 1

def combat(line):
    with open(W + '/play.log', 'a') as f:
        f.write(line + '\n')

def step():
    tick[0] += 5
    # 移动: 向 target 走 1 格
    for uu in MINE + FOES:
        tx, ty = uu['target']
        x, y = uu['tile']
        dx, dy = tx - x, ty - y
        if dx == 0 and dy == 0:
            continue
        sx = (dx > 0) - (dx < 0)
        sy = (dy > 0) - (dy < 0)
        uu['tile'] = [x + sx, y + sy]
    # 清障战: ≥4 我方近战贴敌兵 → 敌死
    for foe in list(FOES):
        if foe['type'] == 2:
            near = sum(1 for m in MINE if m['type'] in (2, 5)
                       and d2(m['tile'], foe['tile']) <= 2)
            if near >= 4:
                combat(f'[combat] p1 type{foe["type"]} died at '
                       f'({foe["tile"][0]},{foe["tile"][1]}) ar={tick[0]} '
                       f'remaining={len(FOES)-1}')
                FOES.remove(foe)
    # 塔: ≥2 冲车贴 → -8/拍
    for b in list(EBS):
        if b['type'] == 12 and b['hp'] > 0:
            near = sum(1 for m in MINE if m['type'] == 7
                       and d2(m['tile'], b['tile']) <= 2)
            if near >= 2:
                b['hp'] = max(0, b['hp'] - 8)
                if b['hp'] == 0:
                    combat(f'[combat] p1 tower down {b["tile"]} ar={tick[0]}')
    # TC: ≥6 单位贴 → -12/拍
    tc = EBS[0]
    near = sum(1 for m in MINE if d2(m['tile'], tc['tile']) <= 2)
    if near >= 6:
        tc['hp'] = max(0, tc['hp'] - 12)
        if tc['hp'] == 0:
            combat(f'[result] WIN ticks={tick[0]}')

def main():
    cap = float(sys.argv[1]) if len(sys.argv) > 1 else 240
    env = dict(os.environ, M0_DRY='1', M0_DRY_SEQ=SEQ, M0_POLL='0.02',
               M0_TIMEOUT=str(cap + 30))
    p = subprocess.Popen([sys.executable, '/tmp/aoe-camp/m0/m0drv.py', W], env=env,
                         stdout=open(W + '/dry-drv.out', 'w'),
                         stderr=subprocess.STDOUT)
    t0 = time.time()
    write()
    while time.time() - t0 < cap:
        line = p.poll()
        if line is not None:
            break
        # 消费驱动命令 (从 dry-drv.out 尾读)
        try:
            with open(W + '/dry-drv.out') as f:
                txt = f.read()
            for m in re.finditer(r'retask (\d+) (\d+) (\d+)', txt):
                s, x, y = int(m.group(1)), int(m.group(2)), int(m.group(3))
                pool = [q for q in MINE if q['slot'] == s]
                if pool:
                    pool[0]['target'] = [x, y]
            # 命令消费后截断已读部分 (简化: 全量重读代价小, 不截断)
        except Exception:
            pass
        step()
        write()
        time.sleep(0.02)
    p.terminate()
    out = open(W + '/dry-drv.out').read()
    stages = re.findall(r'\] (t=\d+ \[[A-Z0-9]+\][^\n]*|A1 到位[^\n]*|A2 到位[^\n]*'
                        r'|B 到位[^\n]*|C 到位[^\n]*|PK1[^\n]*|twS 平[^\n]*'
                        r'|twNW 平[^\n]*|守军清空[^\n]*|TC 平[^\n]*|RESULT[^\n]*'
                        r'|TIMEOUT[^\n]*)', out)
    print('\n'.join(stages))
    print('----')
    retasks = len(re.findall(r'retask', out))
    print(f'retask 总数={retasks}')
    if 'RESULT: WIN' in out:
        print('DRY-E2E: PASS (驱动打到 RESULT: WIN)')
        return 0
    print('DRY-E2E: FAIL (未见 WIN)')
    return 1

if __name__ == '__main__':
    sys.exit(main())
