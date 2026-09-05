#!/usr/bin/env python3
"""m0 环线路由定案: 逐 hop 可走性 + 威胁对账 (塔 range²16 需避开, 敌兵 aggro²25 可打)."""
import re
from collections import deque

GRID = {}
for ln in open('/tmp/aoe-camp/m0/mapdump.txt'):
    m = re.match(r'\s?(\d+)(.*)', ln)
    if not m or ln.startswith('#'):
        continue
    for x, ch in enumerate(m.group(2)):
        GRID[(x, int(m.group(1)))] = ch
WALK = {'.', 'm', 'v', 'e'}
def walk(t):
    return GRID.get(t, 'S') in WALK

FOES = {'guardA': (38, 29), 'guardB': (37, 29), 'scout': (30, 25),
        'pk1': (57, 51), 'pk2': (23, 49), 'pk3': (34, 41), 'pk4': (41, 61),
        'pk5': (16, 32)}
TOWERS = {'twS': (37, 61), 'twSW': (30, 52), 'twW': (15, 52), 'twNW': (17, 31)}
def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2

# 环线: 南缘 x58-63 下行 → 西行 y53-60 (杀 pk1/pk4, 绕 twS 北 y57) →
# 西带 x8-15 北上 (绕 twW) → y33-32 走廊东行 (杀 pk5, 绕 twNW) → 口袋
ROUTE = [
    (60, 39), (61, 41), (62, 43), (62, 45), (62, 47),      # SE 下行
    (62, 49), (62, 51), (62, 53), (60, 55),                 # 东缘过 pk1
    (59, 57), (57, 57), (55, 57), (52, 57), (49, 57),       # y57 过 twS 北界
    (46, 57), (44, 58), (42, 59), (40, 59), (38, 60),       # twS/pk4 区
    (35, 60), (32, 60), (29, 60), (26, 60), (23, 60),       # y60 西行
    (20, 60), (17, 60), (15, 59), (13, 57), (11, 55),       # 西带北上
    (10, 53), (9, 51), (8, 49), (8, 47), (7, 45),           # 绕 twW 西侧
    (6, 43), (6, 41), (6, 39), (7, 37), (8, 35),            # 西区北上
    (10, 34), (12, 33), (14, 33),                            # 接 y33 走廊
    (17, 34), (20, 34), (23, 33), (26, 33), (29, 33),       # y33-34 东行
    (32, 33), (35, 32), (37, 32), (39, 32),                  # 口袋入口
]
STAGING = [(43, 31), (44, 31), (43, 30), (44, 30)]
FIGHT = (39, 30)          # 近战 trigger 位 (guards d²5/2)
CHEW = {'ram0': (40, 28), 'ram1': (39, 29), 'p0': (39, 27), 'p1': (40, 27),
        'p2': (41, 28), 'p3': (41, 27), 's0': (38, 27), 's1': (41, 29)}

print('== ROUTE 逐 hop ==')
bad = 0
for i, h in enumerate(ROUTE):
    if not walk(h):
        print(f'  ✗#{i} {h} 不可走')
        bad += 1
        continue
    tw = min((d2(h, t), nm) for nm, t in TOWERS.items())
    fo = min((d2(h, t), nm) for nm, t in FOES.items())
    warn = ''
    if tw[0] <= 16:
        warn += f' ⚠️塔程内 {tw[1]} d²={tw[0]}'
    if tw[0] <= 25:
        warn += f' (塔余量薄 {tw[1]} d²={tw[0]})'
    if fo[0] <= 25:
        warn += f' ⚔️将aggro {fo[1]} d²={fo[0]}'
    print(f'  #{i:2d} {h} 塔min d²={tw[0]:3d} 敌min {fo[1]:7s} d²={fo[0]:3d}{warn}')
print('staging:')
for s in STAGING:
    tw = min((d2(s, t), nm) for nm, t in TOWERS.items())
    fo = min((d2(s, t), nm) for nm, t in FOES.items())
    mill = d2(s, (40, 32))
    print(f'  {s} walk={walk(s)} 塔d²={tw[0]} 守d²={fo[0]} mill d²={mill}'
          + ('' if walk(s) else ' ✗不可走'))
print(f'fight {FIGHT} walk={walk(FIGHT)}')
print('chew posts:')
for nm, t in CHEW.items():
    print(f'  {nm} {t} walk={walk(t)} d²TC={d2(t,(39,28))} '
          f'd²house={d2(t,(40,30))} d²lumber={d2(t,(37,28))}')

# 8向 BFS 全局核对: ROUTE 每相邻 hop 引擎可达 (同一洪泛分量)
NB = [(1,0),(-1,0),(0,1),(0,-1),(1,1),(1,-1),(-1,1),(-1,-1)]
comp = set()
q = deque([ROUTE[0]])
comp.add(ROUTE[0])
while q:
    t = q.popleft()
    for dx, dy in NB:
        n = (t[0]+dx, t[1]+dy)
        if n not in comp and walk(n):
            comp.add(n)
            q.append(n)
off = [h for h in ROUTE + STAGING + [FIGHT] if h not in comp]
print('洪泛分量外 hops:', off if off else '无 (全连通)')
