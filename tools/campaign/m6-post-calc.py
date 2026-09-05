#!/usr/bin/env python3
"""m6f 漂移容错帖表计算器 (离线)。

对 4 套守军布点 (nominal / boot1 / boot2 / 漂移并集) 各算:
  1) 5 弓方阵帖 (x∈[12,17] y∈[2,7] 可走, 对全部守军 margin 外: 弓 d²>25 猫 d²>37,
     含 +1 游走 → 用 margin 公式; 帖间 pairwise d²≥2), 锚点 (15,5) 就近;
  2) 每个守军弓的钓鱼点 F: d²(F,G)∈[9,16] 且对其余守军 margin 外, 离方阵近优先;
  3) 近战/冲车停车场 (16-17, 6-8) 对该布点是否安全。
margin: 并集外>1格 = d² > r²+2r+1 → 弓 25 / 猫 37 (v6.4 口径)。
"""
import math
import sys

sys.path.insert(0, '/tmp/aoe-camp/m6f')
from m6fdrv import WALK_NW, RNG  # noqa: E402


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def margin(rsq):
    return rsq + 2 * int(math.isqrt(rsq)) + 1


LAYOUTS = {
    'nominal': [((8, 5), 4), ((6, 7), 4), ((4, 9), 4), ((5, 5), 8), ((3, 7), 8)],
    'boot1-S5': [((4, 9), 4), ((6, 7), 4), ((5, 5), 8), ((3, 7), 8)],
    'boot2-S5': [((3, 7), 8), ((4, 9), 4), ((6, 7), 4), ((10, 3), 4), ((5, 5), 8)],
    'drift-union': [((10, 3), 4), ((7, 4), 4), ((5, 6), 4), ((6, 5), 4),
                    ((5, 5), 8), ((3, 7), 8)],
}


def safe_posts(pg):
    """方阵帖候选: 5 个, pairwise d²≥2, 全部对 pg margin 外, 锚 (15,5) 就近。"""
    cands = []
    for y in range(2, 8):
        for x in range(12, 18):
            t = (x, y)
            if t not in WALK_NW:
                continue
            if all(d2(t, g) > margin(RNG[ty]) for g, ty in pg):
                cands.append(t)
    # 贪心: 按离锚点距离排序, 依次取与已取帖 pairwise d²≥2 的
    cands.sort(key=lambda t: (d2(t, (15, 5)), t))
    picked = []
    for t in cands:
        if all(d2(t, p) >= 2 for p in picked):
            picked.append(t)
        if len(picked) == 5:
            break
    return picked, cands


def fish_spots(pg):
    out = {}
    for g, ty in pg:
        if ty != 4:
            continue
        others = [(h, uy) for h, uy in pg if (h, uy) != (g, ty)]
        spots = []
        for y in range(0, 13):
            for x in range(8, 19):
                t = (x, y)
                if t not in WALK_NW:
                    continue
                if 9 <= d2(t, g) <= 16 and \
                        all(d2(t, h) > margin(RNG[uy]) for h, uy in others):
                    spots.append(t)
        spots.sort(key=lambda t: (d2(t, (15, 5)), t))
        out[g] = spots[:6]
    return out


def park_check(pg):
    ok = []
    for t in [(16, 6), (17, 6), (16, 7), (17, 7), (16, 8), (17, 5), (18, 5),
              (17, 3), (18, 4)]:
        if t in WALK_NW and all(d2(t, g) > margin(RNG[ty]) for g, ty in pg):
            ok.append(t)
    return ok


for name, pg in LAYOUTS.items():
    picked, cands = safe_posts(pg)
    print(f'=== {name}: pg={pg}')
    print(f'  方阵帖 (5): {picked}   候选总数={len(cands)}')
    fs = fish_spots(pg)
    for g, spots in fs.items():
        print(f'  钓 {g}: {spots if spots else "无 F(全封)"}')
    print(f'  停车场安全格: {park_check(pg)}')
    print()
