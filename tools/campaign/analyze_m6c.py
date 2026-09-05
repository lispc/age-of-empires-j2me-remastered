#!/usr/bin/env python3
"""m6 分析 v3：兵种化威胁余量（t3 近战 d²≥16 / t4 弓 d²≥26 / t8 猫 d²≥32 /
敌塔 d²≥9），重算 standoff 站位表 + 分阶段走廊 BFS。"""
import json
import sys
from collections import deque

sys.path.insert(0, '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/tools')
import aoesave  # noqa: E402

SAVE = '/tmp/aoe-camp/m6b/probe/base.aoesave'
W = H = 64

# (tile, type, 标签) —— probe P0 实录
FOES = [((33, 43), 4, 'nest'), ((31, 41), 4, 'nest'), ((33, 41), 4, 'nest'),
        ((18, 15), 4, 'NE'), ((19, 14), 4, 'NE'), ((4, 9), 4, 'NW'),
        ((6, 7), 4, 'NW'), ((8, 5), 4, 'NW'), ((18, 14), 4, 'NE'),
        ((29, 57), 3, 'exit'), ((26, 30), 3, 'gate'), ((27, 29), 3, 'gate'),
        ((28, 28), 3, 'gate'), ((32, 16), 3, 'solo'),
        ((13, 23), 8, 'pile'), ((14, 22), 8, 'pile'), ((12, 24), 8, 'pile'),
        ((5, 5), 8, 'NWcat'), ((3, 7), 8, 'NWcat')]
SOFT = {'TC': (4, 4), '城堡': (4, 2), '兵营': (2, 1), '攻城坊': (1, 3),
        '射箭场': (2, 5), '铁匠铺': (2, 3), '大学': (5, 1), '马厩': (1, 1)}
TOWERS = [(2, 8), (5, 7), (6, 4), (26, 28), (22, 50)]
RAD = {3: 16, 4: 26, 8: 32}


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def load_map():
    s = aoesave.load(SAVE)
    mt = s.mt
    wall = [[False] * W for _ in range(H)]
    for ty in range(H):
        for tx in range(W):
            k = aoesave.decode_tile(mt[ty * W + tx])['kind']
            if k in ('资源', '建筑占位', '雾-资源', '雾-建筑', '雾-占位'):
                wall[ty][tx] = True
    return wall


def bfs_path(ok, start, goal):
    if start not in ok or goal not in ok:
        return None
    parent = {start: None}
    q = deque([start])
    while q:
        cur = q.popleft()
        if cur == goal:
            p = []
            while cur is not None:
                p.append(cur)
                cur = parent[cur]
            return p[::-1]
        x, y = cur
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n in ok and n not in parent:
                parent[n] = cur
                q.append(n)
    return None


def main():
    wall = load_map()
    reach = {(x, y) for y in range(H) for x in range(W)
             if not wall[y][x]}
    # 连通性: 从出生点
    comp = set()
    q = deque([(13, 57)])
    comp.add((13, 57))
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n in reach and n not in comp:
                comp.add(n)
                q.append(n)

    def safe_region(foes, extra_rad=0):
        ok = set()
        for y in range(H):
            for x in range(W):
                c = (x, y)
                if c not in comp:
                    continue
                if any(d2(c, g) < RAD[t] + extra_rad for (g, t, _s) in foes):
                    continue
                if any(d2(c, tb) < 9 for tb in TOWERS):
                    continue
                ok.add(c)
        return ok

    # ---- 站位表（当前全守军）----
    print('=== 站位表（全守军在场）===')
    posts = {}
    for name, eb in list(SOFT.items()) + [(f'塔{i}', t) for i, t in enumerate(TOWERS)]:
        cands = []
        for y in range(H):
            for x in range(W):
                c = (x, y)
                if c not in comp:
                    continue
                if d2(c, eb) > 16:
                    continue
                if any(d2(c, g) < RAD[t] for (g, t, _s) in FOES):
                    continue
                if any(d2(c, tb) < 9 for tb in TOWERS):
                    continue
                cands.append(c)
        cands.sort(key=lambda c: (d2(c, eb), c))
        posts[name] = cands
        print(f"{name}{eb}: {len(cands):3d}  {cands[:8]}")

    # ---- 分阶段走廊 ----
    print('=== 走廊 ===')
    now = safe_region(FOES)
    stages = [
        ('S1 东道→门南 staging (34,34)', (34, 34), FOES),
        ('S1 猫→(6,0) 塔2帖', (6, 0), FOES),
    ]
    for label, goal, foes in stages:
        ok = safe_region(foes)
        p = bfs_path(ok, (13, 57), goal)
        print(f"{label}: {'FAIL' if p is None else f'{len(p)}格: {p}'}")

    # 门区拔钉后（gate trio + solo 移除）→ (13,5)
    after_gate = [f for f in FOES if f[2] not in ('gate', 'solo')]
    ok2 = safe_region(after_gate)
    p = bfs_path(ok2, (34, 34), (13, 5))
    print(f"S4 门后→(13,5): {'FAIL' if p is None else f'{len(p)}格: {p}'}")
    p = bfs_path(ok2, (13, 5), (12, 0))
    print(f"S4 (13,5)→(12,0): {'FAIL' if p is None else f'{len(p)}格: {p}'}")
    for goal in [(2, 0), (1, 0), (0, 1), (1, 2), (6, 0)]:
        p = bfs_path(ok2, (12, 0), goal)
        print(f"S4 (12,0)→{goal}: {'FAIL' if p is None else f'{len(p)}格'}"
              f"{' ' + str(p) if p and len(p) < 12 else ''}")

    # 口袋拔钉后（NW 5 守军也移除）→ 西南帖
    after_pocket = [f for f in after_gate if f[2] not in ('NW', 'NWcat')]
    ok3 = safe_region(after_pocket)
    for goal in [(0, 11), (5, 11), (0, 4), (23, 27)]:
        p = bfs_path(ok3, (2, 0), goal)
        print(f"S7 口袋后 (2,0)→{goal}: {'FAIL' if p is None else f'{len(p)}格: {p[-6:] if p else []}'}")
    # 门拔钉后 → (23,27) 塔3帖
    p = bfs_path(safe_region(after_gate), (34, 34), (23, 27))
    print(f"S9 门后 (34,34)→(23,27): {'FAIL' if p is None else f'{len(p)}格: {p}'}")

    with open('/tmp/aoe-camp/m6b/standoff3.json', 'w') as f:
        json.dump(posts, f)


if __name__ == '__main__':
    main()
