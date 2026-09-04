#!/usr/bin/env python3
"""m1 全程配方（游戏已 boot+base 存档后运行）:
清西敌 → 勘察前排 → hdr9 伪交存 → 塔区围栏砍隧道 → 护送入堡 → 等 WIN"""
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import aoesave
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import Camp

import sys as _s
WORK = _s.argv[1] if len(_s.argv) > 1 else '/tmp/aoe-camp/m1b'
d = Camp(WORK)

# 1. 注意：m1 脚本"任何单位死亡=判负"（含军事）——全程禁止接敌，军事原地留守。
#    西部固定敌 (15,47)/(16,54) 用围栏规避（fence x>=27），不要 rally 清场。

# 2. 勘察树墙前排（有可走西邻的行）——直接读 base 存档（FIFO 探针会被 aA!=6 拒绝）
for _ in range(20):
    if d.aA() == 6:
        break
    d.cmd("key -6")
    time.sleep(1)
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
# 行筛选：y>=56 才安全——敌塔 (49,52) 射程 4 覆盖 y<=56 的出口区（上轮 x=44 折损根因）
safe = {y: x for y, x in rows.items() if y >= 57}
if not safe:
    print('REROLL: 无安全行(墙整条在塔区内)')
    sys.exit(2)
sel = dict(sorted(safe.items())[:3])
print('选定行:', sel, flush=True)

# 3. 伪交存点 + 砍隧道（塔区围栏: x>45 且 y<57 禁入）
d.cmd("hdr9 33 58")
time.sleep(0.3)
breached = False
for attempt in range(6):
    rows = d.chop_rows(sel, 50, 1800, fence=(27, 52, 63, 51, 57), home=(33, 58))
    print('fronts', rows, flush=True)
    if d.result():
        print(d.result()); sys.exit()
    if all(v >= 50 for v in rows.values()):
        breached = True
        break
if not breached:
    print('REROLL: 预算内未贯通')
    sys.exit(2)

# 4. 护送：全体村民进堡（塔区外沿走进 (51,60) 附近）
for _ in range(60):
    if d.result():
        break
    vs = d.villagers()
    if not vs:
        print('NO VILLAGERS'); break
    out = [u for u in vs if u['x'] < 50]
    for u in out:
        d.cmd(f"retask {u['i']} 51 60")
        time.sleep(0.05)
    if not out:
        time.sleep(3)
    time.sleep(1.5)
print('END:', d.result(), flush=True)
