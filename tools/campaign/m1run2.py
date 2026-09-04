#!/usr/bin/env python3
"""m1 全程配方 v2（尸检后改进版，2026-09-04 r35）:
v1→v2 变更（依据 res111 解码 + attempt2 尸检）:
  0. 新增：开局先派护送军事清掉西部固定敌 (15,47)/(16,54)——res111 只有村民(type<2)
     死亡才判负，军事死亡合法；西部杀器拔除后 v1 式西漂不再致命。
  1. fence 收紧：y<57 全线禁入（旧 fence 只拦 x>48 且 y<57，x∈[27,48] 北漂不拦）。
  2. 判负=村民死亡(20tick 延迟)，胜利=slot0 进 x[50,57)×y[57,64) 闲置 20tick。
     护送阶段保持全村民推 (51,60)。
游戏已 boot+base 存档后运行: python3 m1run2.py <work>"""
import os
import sys
import time

sys.path.insert(0, '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/tools')
import aoesave
sys.path.insert(0, '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/tools/campaign')
from lib import Camp

import sys as _s
WORK = _s.argv[1] if len(_s.argv) > 1 else '/tmp/aoe-camp/m1b'
d = Camp(WORK)

# 0. 清西部固定敌（军事死亡合法；敌仅 2×t3，我方 4×t3+3×t4+1×t6 碾压）
for _ in range(20):
    if d.aA() == 6:
        break
    d.cmd("key -6")
    time.sleep(1)
print('phase0: 清西部敌', flush=True)
mil = d.military()
print('military:', [(u['i'], u['t'], u['x'], u['y']) for u in mil], flush=True)
for wave in [(16, 47), (16, 54), (15, 47)]:
    us1 = d.slots(1)
    if len(us1) <= 14:  # 16-2 = 西敌已清
        break
    for u in mil:
        d.cmd(f"sel {u['x']} {u['y']}")
        time.sleep(0.25)
        d.cmd(f"goto {wave[0]} {wave[1]}")
        time.sleep(0.25)
    for _ in range(40):  # 最多 ~2min 等接敌
        time.sleep(3)
        us1 = d.slots(1)
        cnt = len(us1)
        mil = d.military()
        if cnt <= 14 or d.result():
            break
    print(f'wave {wave}: 敌余 {len(d.slots(1))} 我军 {[ (u["i"],u["t"],u["x"],u["y"]) for u in mil]}', flush=True)
    if d.result():
        print(d.result()); sys.exit()

# 1. 勘察树墙前排（读 base 存档）
sv = aoesave.load(d.work + '/base.aoesave')
rows = {}
for ty in range(53, 63):
    for tx in range(30, 53):
        dd = sv.tile(tx, ty)
        if dd['kind'] == '雾-资源' or (dd['kind'] == '资源' and (dd['raw'] & 3) == 1):
            w = sv.tile(tx - 1, ty)['kind']
            if w in ('地形', '雾', '未初始化', '单位占位'):
                rows[ty] = tx
            break
print('fronts:', rows, flush=True)
safe = {y: x for y, x in rows.items() if y >= 57}
if not safe:
    print('REROLL: 无安全行')
    sys.exit(2)
sel = dict(sorted(safe.items())[:3])
print('选定行:', sel, flush=True)

# 2. 伪交存点 + 砍隧道（fence: x∈[27,51], y∈[57,63] 全程禁北漂）
d.cmd("hdr9 33 58")
time.sleep(0.3)
breached = False
for attempt in range(6):
    rows = d.chop_rows(sel, 50, 1800, fence=(27, 57, 63, 51), home=(33, 58))
    print('fronts', rows, flush=True)
    if d.result():
        print(d.result()); sys.exit()
    if all(v >= 50 for v in rows.values()):
        breached = True
        break
if not breached:
    print('REROLL: 预算内未贯通')
    sys.exit(2)

# 3. 护送：全体村民进堡（堡内矩形 x[50,57)×y[57,64)，slot0 进区闲置 20tick = WIN）
for _ in range(90):
    if d.result():
        break
    vs = d.villagers()
    if not vs:
        print('NO VILLAGERS'); break
    out = [u for u in vs if not (50 <= u['x'] < 57 and 57 <= u['y'] < 64)]
    for u in out:
        d.cmd(f"retask {u['i']} 51 60")
        time.sleep(0.05)
    if not out:
        time.sleep(3)
    time.sleep(1.5)
print('END:', d.result(), flush=True)
