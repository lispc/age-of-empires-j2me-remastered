#!/usr/bin/env python3
"""m6e 会合区计算器 (离线): 给决斗梯 (bug#3 修法) 标定每个击杀目标的会合区。

口径 (r62 校准): 敌弓威胁 d²≤16 (安全 ≥25), 敌猫威胁 d²≤25 (安全 ≥37);
会合区 M 定义: ①M 在目标射程内 (d²(M,T)≤16, 触发交火) ②M 对其余活守军
处于「射程并集外 >1 格」(d²(M,o) > r²+2r+1, 含 ±1 游走余量)
③M 从 REST (13,6) 走「全程并集外」的路可达 (饵入场不挨别人的打)。
击杀序 (本轮设计): A1(8,5)弓 → C1(5,5)猫 → C2(3,7)猫 → A2(6,7)弓 → A3(4,9)弓
(A1 独占东区已被 probe 实证; 猫不死则 A2/A3 的会合区全被猫环罩住 → 必须先杀猫)。
"""
import sys
from collections import deque

sys.path.insert(0, '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/tools')
import aoesave  # noqa: E402

SAVE = '/tmp/aoe-camp/m6d/base.aoesave'   # 同图确定性, 沿用四轮验证的 base
W = H = 64
REST = (13, 6)
RNG = {4: 16, 8: 25}          # 保守射程² (弓16 / 猫25)
import math  # noqa: E402
# 并集外 >1 格: d² > r²+2r+1 = (r+1)², 输入是射程²
MARGIN = lambda rsq: rsq + 2 * int(math.isqrt(rsq)) + 1

# NW 口袋守军 (nominal, probe-layout p1 s5/s6/s7/s17/s18)
GUARDS = {'A1': ((8, 5), 4), 'A2': ((6, 7), 4), 'A3': ((4, 9), 4),
          'C1': ((5, 5), 8), 'C2': ((3, 7), 8)}
ORDER = ['A1', 'C1', 'C2', 'A2', 'A3']


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def load_reach():
    s = aoesave.load(SAVE)
    mt = s.mt
    reach = set()
    for ty in range(H):
        for tx in range(W):
            k = aoesave.decode_tile(mt[ty * W + tx])['kind']
            if k not in ('资源', '建筑占位', '雾-资源', '雾-建筑', '雾-占位'):
                reach.add((tx, ty))
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
    return comp


def covered(m, gname):
    """m 是否在守军 gname 的「射程+1 格」圈内。"""
    g, t = GUARDS[gname]
    return d2(m, g) <= MARGIN(RNG[t])


def main():
    comp = load_reach()
    print(f'连通格总数={len(comp)}  NW 区样例: '
          f'{sorted(c for c in comp if c[0] <= 18 and c[1] <= 18)[:10]}')
    for gname in GUARDS:
        print(f'  {gname}{GUARDS[gname][0]} 可走性: '
              f'{GUARDS[gname][0] in comp}')

    alive = list(ORDER)
    print('\n=== 击杀序逐级会合区 ===')
    for step, tname in enumerate(ORDER):
        t, tt = GUARDS[tname]
        others = [g for g in alive if g != tname]
        # 安全场 = 全部其余活守军并集外 >1 格
        safe = {c for c in comp
                if not any(covered(c, o) for o in others)}
        # 会合区: 目标射程内 + 安全场内
        zone = sorted((c for c in safe if d2(c, t) <= RNG[tt]),
                      key=lambda c: (d2(c, REST), c))
        # REST 是否仍在安全场内 (饵收兵柱有效性)
        rest_ok = REST in safe and REST in comp
        # 入场路: REST → M 全程安全场 BFS
        path = None
        if zone:
            start = REST if rest_ok else zone[0]
            parent = {start: None}
            q = deque([start])
            while q and path is None:
                cur = q.popleft()
                if cur in zone:
                    path = []
                    while cur is not None:
                        path.append(cur)
                        cur = parent[cur]
                    path = path[::-1]
                    break
                x, y = cur
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (x + dx, y + dy)
                    if n in safe and n not in parent:
                        parent[n] = cur
                        q.append(n)
        print(f'[{step}] {tname}{t}(t{tt}) others={others}')
        print(f'    会合区 {len(zone)} 格: {zone[:6]}')
        print(f'    REST 安全={rest_ok}  安全路={path}')
        # 游走 ±2 后仍成立? 目标向 M 走 2 格收紧触发距离, 其余向 M 走 2 格扩圈
        ok_wander = []
        for m in zone[:6]:
            good = all(
                d2(m, (GUARDS[o][0][0] + (1 if m[0] > GUARDS[o][0][0] else -1),
                        GUARDS[o][0][1] + (1 if m[1] > GUARDS[o][0][1] else -1)))
                > MARGIN(RNG[GUARDS[o][1]]) for o in others)
            ok_wander.append((m, good))
        print(f'    游走±2 后仍安全: {ok_wander}')
        alive.remove(tname)

    # 附加: 猫全死后弓对弓会合区 (复核 A2/A3)
    print('\n=== 参考: 全猫死后 弓目标会合区 (others=仅剩弓) ===')
    arch = ['A2', 'A3']
    for tname in arch:
        t, tt = GUARDS[tname]
        others = [g for g in arch if g != tname]
        safe = {c for c in comp if not any(covered(c, o) for o in others)}
        zone = sorted((c for c in safe if d2(c, t) <= RNG[tt]),
                      key=lambda c: (d2(c, REST), c))
        print(f'{tname}{t}: {zone[:8]}')

    # 附加: A1 会合区当 A2 游走到极限时的临界复核
    print('\n=== 参考: A1 duel, A2 游走 ±2 扫描 ===')
    t, tt = GUARDS['A1']
    for ax in range(4, 9):
        for ay in range(5, 10):
            m = (12, 5)
            others_d = d2(m, (ax, ay))
            if others_d <= MARGIN(RNG[4]):
                print(f'  A2@({ax},{ay}) 时 (12,5) 进入其+1圈 d²={others_d}')


if __name__ == '__main__':
    main()
