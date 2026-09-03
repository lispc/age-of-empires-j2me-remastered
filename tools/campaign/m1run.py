#!/usr/bin/env python3
"""m1 全程配方（游戏已 boot+base 存档后运行）:
清西敌 → 勘察前排 → hdr9 伪交存 → 塔区围栏砍隧道 → 护送入堡 → 等 WIN"""
import os
import sys
import time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import Camp

import sys as _s
WORK = _s.argv[1] if len(_s.argv) > 1 else '/tmp/aoe-camp/m1b'
d = Camp(WORK)

# 1. 清西部固定敌 (15,47)/(16,54)
d.rally_seq([(16, 52)], settle=40)

# 2. 勘察树墙前排（有可走西邻的行）
sv = d.save_probe()
rows = {}
for ty in range(53, 63):
    for tx in range(30, 53):
        dd = sv.tile(tx, ty)
        if dd['kind'] == '雾-资源' or (dd['kind'] == '资源' and (dd['raw'] & 3) == 1):
            w = sv.tile(tx - 1, ty)['kind']
            if w in ('地形', '雾', '单位占位'):
                rows[ty] = tx
            break
print('fronts:', rows, flush=True)
sel = dict(sorted(rows.items())[:3])
print('选定行:', sel, flush=True)

# 3. 伪交存点 + 砍隧道（塔区围栏: x>45 且 y<57 禁入）
d.cmd("hdr9 33 58")
time.sleep(0.3)
rows = d.chop_rows(sel, 50, 2000, fence=(27, 52, 63, 51, 57), home=(33, 58))
print('final fronts', rows, flush=True)
if d.result():
    print(d.result()); sys.exit()

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
