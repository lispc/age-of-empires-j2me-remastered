#!/usr/bin/env python3
"""m4ddrv 离线 dry-run 模拟器：解释驱动命令、演化简化世界，验证状态机分支。
跑法：python3 tools/campaign/mocksim4d.py   （内部 import m4ddrv 并注入 SIM）

覆盖分支：build 雾格候选回退 / **奇和格硬断言（升时代锁死防线）** / train 排队与
pop / 弹窗冻结与 -6 / 敌波时间线（gate=armyValue 门）/ 庭院决战（含塔加成）/
raid 分队+keeper / 波出生回撤与复工 / 阶段 EARLY→MID→FULL / 矿仓(交存点)收益
建模 / 升时代舞步（方向断言）/ Mill-BS-塔-University 链 / WIN / LOSS。
"""
import sys
import time

sys.path.insert(0, '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/tools/campaign')
import m4ddrv

TC = (43, 57)
GOLD_T = {(35, 36), (36, 36), (37, 36), (35, 35), (36, 35), (34, 36),
          (34, 37), (37, 35)}
STONE_T = {(39, 40), (41, 40), (41, 38), (38, 40), (40, 40), (39, 39)}
WOOD_T = {(32, 52), (33, 51)} | set(m4ddrv.WOOD_SAFE)
BCOST = {0: (15, 0, 0), 1: (15, 0, 0), 10: (20, 0, 10), 11: (5, 0, 0),
         5: (15, 0, 10), 6: (25, 0, 20), 12: (20, 5, 15), 4: (25, 0, 25)}
# 需要时代门的建筑：techFlags 语义（[15]/[16] 封建解锁 Mill/BS，[14] 城堡解锁 Univ）
AGE_GATE = {5: 1, 6: 1, 4: 2}
E_MINE = (16, 40)          # 敌矿工作业区中心


def assert_cursor():
    p = m4ddrv.cursor_path(44, 60, 43, 57)
    assert p == [-1, -1, -4], p
    p = m4ddrv.cursor_path(0, 0, 2, 0)
    assert p == [-2, -4], p
    p = m4ddrv.cursor_path(43, 57, 43, 57)
    assert p == [], p
    p = m4ddrv.cursor_path(45, 58, 43, 57)
    assert p is None, p
    # 偶和格断言：驱动所有候选表必须全偶和
    for name in ['B_CANDS', 'HOUSE_CANDS', 'CAMP_CANDS', 'MILL_CANDS',
                 'BS_CANDS', 'TOWER_CANDS', 'UNIV_CANDS']:
        for (x, y) in getattr(m4ddrv, name):
            assert (x + y) % 2 == 0, f'{name} 含奇和格 ({x},{y})'
    assert (m4ddrv.TC[0] + m4ddrv.TC[1]) % 2 == 0
    assert (m4ddrv.FRONT[0] + m4ddrv.FRONT[1]) % 2 == 0
    print('cursor_path/偶和候选断言通过')


class Sim:
    def __init__(self):
        self.ar = 420
        self.clock = 0.0
        self.aA = 6
        self.res = [10, 10, 10]
        self.age = 0
        self.cap = 5
        self.queue = []
        self.units = [dict(slot=0, p=0, type=0, tile=[41, 56], hp=255, action=0),
                      dict(slot=1, p=0, type=1, tile=[42, 55], hp=255, action=0)]
        self.bld = [dict(slot=0, p=0, type=9, tile=list(TC), uc=False, hp=255)]
        self.nextslot = 2
        self.cursor = list(TC)
        self.menu = 0
        self.jobs = {}
        self.last_build_ok = None
        self.last_train_ok = False
        self.result = None
        self.parity_violation = False
        self.first_done = set()
        self.marks = {}
        # 敌情
        self.p1vil = [dict(slot=40, type=0, tile=[15, 40], hp=255),
                      dict(slot=41, type=0, tile=[14, 39], hp=255),
                      dict(slot=42, type=0, tile=[17, 41], hp=255),
                      dict(slot=43, type=0, tile=[16, 42], hp=255)]
        self.p1mil = []
        self.p1pool = []
        self.p1res = [20, 20, 20]
        self.p1av = 0
        self.myav = 0
        self.log = []

    # ---------- 驱动命令入口 ----------
    def handle(self, c):
        self.log.append(c)
        ps = c.split()
        if ps[0] == 'retask':
            self.jobs[int(ps[1])] = (int(ps[2]), int(ps[3]))
        elif ps[0] == 'build':
            x, y, t = int(ps[1]), int(ps[2]), int(ps[3])
            cost = BCOST.get(t)
            ok = False
            if cost:
                gate = AGE_GATE.get(t, 0)
                if self.age < gate:
                    self.simprint(f'build FAIL t{t} 时代门 age={self.age}<{gate}')
                elif any(self.res[i] < cost[i] for i in range(3)):
                    self.simprint(f'build FAIL t{t} 资源不足 {self.res} < {cost}')
                elif any(tuple(b['tile']) == (x, y) for b in self.bld):
                    self.simprint(f'build FAIL ({x},{y}) 占格')
                else:
                    for i in range(3):
                        self.res[i] -= cost[i]
                    self.bld.append(dict(slot=len(self.bld), p=0, type=t,
                                         tile=[x, y], uc=True, hp=1))
                    ok = True
                    self.simprint(f'build OK t{t} ({x},{y}) res={self.res}')
            self.last_build_ok = (x, y) if ok else None
        elif ps[0] == 'train':
            x, y, n = int(ps[1]), int(ps[2]), int(ps[3])
            b = next((b for b in self.bld if tuple(b['tile']) == (x, y)
                      and b['p'] == 0 and not b['uc']), None)
            kind = None
            if b and b['type'] == 10:
                kind = 'mil'
            elif b and b['type'] == 11:
                kind = 'vil'
            ok = False
            if kind:
                need = 2 if kind == 'mil' else 1
                room = self.cap - self.pop_used() - len(self.queue)
                if room >= need:
                    self.queue.append([kind, len(self.queue), 6])
                    ok = True
                    self.simprint(f'train {kind} 排队 1/1 q={len(self.queue)}')
                else:
                    self.simprint(f'train {kind} 拒 pop room={room}')
            else:
                self.simprint(f'train FAIL 非己方/不可生产 ({x},{y})')
            self.last_train_ok = ok
        elif ps[0] == 'rally':
            tx, ty = int(ps[1]), int(ps[2])
            for u in self.units:
                if u['type'] >= 2:
                    u['tile'] = [tx, ty]
        elif ps[0] == 'sel':
            pass
        elif ps[0] == 'key':
            self.key(int(ps[1]))

    def key(self, k):
        if self.aA == 2:
            if k == -6:
                self.aA = 6
            return
        if k == -6:
            return
        if k == -1:
            self.cursor = [max(0, self.cursor[0] - 1), max(0, self.cursor[1] - 1)]
        elif k == -2:
            self.cursor = [min(63, self.cursor[0] + 1), min(63, self.cursor[1] + 1)]
        elif k == -3:
            self.cursor = [max(0, self.cursor[0] - 1), min(63, self.cursor[1] + 1)]
        elif k == -4:
            self.cursor = [min(63, self.cursor[0] + 1), max(0, self.cursor[1] - 1)]
        elif k == -5:
            if self.menu == 0:
                if tuple(self.cursor) == TC and any(
                        b['type'] == 9 and not b['uc'] for b in self.bld):
                    self.menu = 1
            elif self.menu == 2:
                self.research()
                self.menu = 0
        elif k == 49:
            if self.menu == 1:
                self.menu = 2

    def research(self):
        cost = (15, 15, 15) if self.age == 0 else (20, 20, 20)
        gate = (self.age == 0 and any(b['type'] == 10 and not b['uc'] for b in self.bld)) \
            or (self.age == 1 and self.cnt_done(5) and self.cnt_done(6))
        if not gate:
            self.simprint(f'research FAIL 门未满足 age={self.age}')
            return
        if not all(self.res[i] >= cost[i] for i in range(3)):
            self.simprint(f'research FAIL 资源 {self.res} < {cost}')
            return
        for i in range(3):
            self.res[i] -= cost[i]
        self.age += 1
        if self.age == 1:
            for u in self.units:
                if u['type'] == 2:
                    u['type'] = 3       # convertUnitType(2,3)
        self.aA = 2
        self.mark(f'age{self.age}')
        self.simprint(f'*** research PAID age->{self.age} res={self.res}')

    # ---------- 世界演化 ----------
    def pop_used(self):
        return sum(1 if u['type'] <= 1 else 2 for u in self.units) + len(self.queue)

    def cnt_done(self, t):
        return sum(1 for b in self.bld if b['type'] == t and not b['uc'])

    def simprint(self, s):
        print(f'  SIM[ar={self.ar}] {s}', flush=True)

    def mark(self, key):
        if key not in self.marks:
            self.marks[key] = self.ar
            print(f'  == MILESTONE {key} @ ar={self.ar}', flush=True)

    def tick_world(self):
        if self.aA == 2:
            self.clock += 1.0
            return
        self.clock += 1.0
        self.ar += 90
        camp_up = self.cnt_done(1) > 0
        # 施工（含奇和格违规检测）
        for b in self.bld:
            if b['uc']:
                b['hp'] += 64
                if b['hp'] >= 255:
                    b['uc'] = False
                    b['hp'] = 255
                    if (b['tile'][0] + b['tile'][1]) % 2 != 0:
                        self.parity_violation = True
                        self.simprint(f'!!! 奇和格建筑完工 ({b["tile"]}) 升时代锁死风险')
                    if b['type'] not in self.first_done:
                        self.first_done.add(b['type'])
                        self.aA = 2
                        self.simprint(f'首座 t{b["type"]} 完工 → 弹窗')
                    self.cursor = list(b['tile'])
                    if b['type'] == 11:
                        self.cap += 5
                    if b['type'] == 1:
                        self.mark('camp')
                    if b['type'] == 4:
                        self.mark('univ')
                    self.simprint(f'完工 t{b["type"]} ({b["tile"][0]},{b["tile"][1]})')
        # 训练出兵（费用随时代：黑暗 t2=5木5金 / 封建后 t3=5木10金）
        for q in self.queue:
            q[2] -= 1
        for q in list(self.queue):
            if q[2] <= 0:
                kind = q[0]
                cost = (5, 10, 0) if (kind == 'mil' and self.age >= 1) else \
                       (5, 5, 0) if kind == 'mil' else (5, 0, 0)
                if all(self.res[i] >= cost[i] for i in range(3)):
                    for i in range(3):
                        self.res[i] -= cost[i]
                    t = (3 if self.age >= 1 else 2) if kind == 'mil' else 1
                    self.units.append(dict(slot=self.nextslot, p=0, type=t,
                                           tile=list(TC), hp=255, action=0))
                    self.nextslot += 1
                    self.myav += 10
                    self.queue.remove(q)
                    self.simprint(f'出生 {kind}(t{t}) slot={self.nextslot - 1} '
                                  f'res={self.res}')
        # 村民作业（矿仓后金/石本地交存=短趟；否则 20+ 格长趟）
        for u in self.units:
            if u['type'] <= 1:
                j = self.jobs.get(u['slot'])
                if not j:
                    continue
                jt = tuple(j)
                d = abs(u['tile'][0] - j[0]) + abs(u['tile'][1] - j[1])
                if d > 2:
                    u['tile'][0] += max(-8, min(8, j[0] - u['tile'][0]))
                    u['tile'][1] += max(-8, min(8, j[1] - u['tile'][1]))
                    u['action'] = 1
                else:
                    u['tile'] = list(j)
                    u['action'] = 2
                    if jt in GOLD_T or jt in STONE_T:
                        trip = 4 if camp_up else 8
                    elif jt in WOOD_T:
                        trip = 4
                    else:
                        continue          # 避险点等非资源格：无收入
                    if self.ar % (trip * 90) < 90:
                        if jt in GOLD_T:
                            self.res[1] += 3
                        elif jt in STONE_T:
                            self.res[2] += 3
                        else:
                            self.res[0] += 5
        # 闲置回血
        for u in self.units:
            if 0 < u['hp'] < 255:
                near_foe = any((v['tile'][0] - u['tile'][0]) ** 2
                               + (v['tile'][1] - u['tile'][1]) ** 2 < 25
                               for v in self.p1mil)
                if not near_foe:
                    u['hp'] = min(255, u['hp'] + 30)
        # 我民兵走 job（raid 分队/驻点微调）
        for u in self.units:
            if u['type'] >= 2:
                j = self.jobs.get(u['slot'])
                if j:
                    d2 = (u['tile'][0] - j[0]) ** 2 + (u['tile'][1] - j[1]) ** 2
                    if d2 > 1:
                        u['tile'][0] += max(-14, min(14, j[0] - u['tile'][0]))
                        u['tile'][1] += max(-14, min(14, j[1] - u['tile'][1]))
        # raid 击杀：分队 job 贴身敌村民
        for u in self.units:
            if u['type'] >= 2:
                j = self.jobs.get(u['slot'])
                if j:
                    for v in self.p1vil:
                        if (v['tile'][0] - j[0]) ** 2 + (v['tile'][1] - j[1]) ** 2 <= 4:
                            v['hp'] -= 130
        for v in list(self.p1vil):
            if v['hp'] <= 0:
                self.p1vil.remove(v)
                self.simprint(f'敌村民 死于 raid 余{len(self.p1vil)}')
        # 敌经济
        for v in self.p1vil:
            if self.ar % 270 < 90:
                self.p1res[1] += 2
                self.p1res[0] += 1
        if len(self.p1vil) < 6 and self.p1res[0] >= 5 and self.ar > 1200 \
                and self.ar % 720 < 90:
            self.p1res[0] -= 5
            self.p1vil.append(dict(slot=40 + len(self.p1vil) * 7, type=0,
                                   tile=[7, 28], hp=255))
            self.simprint(f'敌复训村民（压制对象+1 余{len(self.p1vil)}）')
        for v in self.p1vil:
            d = abs(v['tile'][0] - E_MINE[0]) + abs(v['tile'][1] - E_MINE[1])
            if d > 2:
                v['tile'][0] += max(-3, min(3, E_MINE[0] - v['tile'][0]))
                v['tile'][1] += max(-3, min(3, E_MINE[1] - v['tile'][1]))
        # 敌练兵
        if self.ar > 1600 and self.ar % 720 < 90 and self.p1res[1] >= 5:
            self.p1res[1] -= 5
            self.p1pool.append(dict(slot=60 + len(self.p1pool), type=2,
                                    tile=[8, 27], hp=255))
            self.p1av += 10
        if self.ar % 2700 < 90 and self.p1res[0] >= 10 and self.p1res[1] >= 10:
            self.p1res[0] -= 10
            self.p1res[1] -= 10
            self.p1pool.append(dict(slot=60 + len(self.p1pool), type=4,
                                    tile=[8, 27], hp=255))
            self.p1av += 20
        # all-in 门 → 波次出发（boot1-3 实测波间隔 ≥400-1200t，加 900t 下限——
        # 否则 sim 波背背靠，驱动永无静默窗，不符合真实节奏）
        if self.p1av >= 25 and self.myav < self.p1av * 1.25 and len(self.p1pool) >= 2 \
                and self.ar - getattr(self, 'last_wave_ar', 0) >= 900:
            self.last_wave_ar = self.ar
            n = max(1, len(self.p1pool) * 3 // 4)
            for u in self.p1pool[:n]:
                self.p1mil.append(u)
            self.p1pool = self.p1pool[n:]
            self.simprint(f'敌 WAVE 出发 n={len(self.p1mil)} av={self.p1av}')
        # 敌行军 → TC
        for u in self.p1mil:
            dx, dy = TC[0] - u['tile'][0], TC[1] - u['tile'][1]
            u['tile'][0] += max(-20, min(20, dx))
            u['tile'][1] += max(-20, min(20, dy))
        # 庭院战斗（塔加成）
        near_front = [u for u in self.p1mil
                      if (u['tile'][0] - TC[0]) ** 2 + (u['tile'][1] - TC[1]) ** 2 < 64]
        defen = [u for u in self.units if u['type'] >= 2
                 and (u['tile'][0] - TC[0]) ** 2 + (u['tile'][1] - TC[1]) ** 2 < 100]
        twr = [b for b in self.bld if b['type'] == 12 and not b['uc']
               and (b['tile'][0] - TC[0]) ** 2 + (b['tile'][1] - TC[1]) ** 2 < 25]
        if near_front:
            if defen or twr:
                dpower = 130 * max(1, len(defen)) + 65 * len(twr)
                for u in near_front:
                    u['hp'] -= dpower // max(1, len(near_front))
                for u in defen:
                    u['hp'] -= 100 * max(1, len(near_front)) // max(1, len(defen) + len(twr))
            else:
                self.bld[0]['hp'] -= 30
        for u in list(self.p1mil):
            if u['hp'] <= 0:
                self.p1mil.remove(u)
                self.p1av = max(0, self.p1av - 10)
                self.simprint('敌军 死于庭院')
        for u in list(self.units):
            if u['type'] >= 2 and u['hp'] <= 0:
                self.units.remove(u)
                self.myav = max(0, self.myav - 10)
                self.jobs.pop(u['slot'], None)
                self.simprint(f'我民兵 阵亡 slot={u["slot"]} 余 '
                              f'{sum(1 for x in self.units if x["type"] >= 2)}')
        # 胜负
        if self.bld[0]['hp'] <= 0 and not self.result:
            self.result = f'[result] LOSS ticks={self.ar}'
            self.simprint('TC 被拆 → LOSS')
        if self.age >= 2 and self.cnt_done(4) and not self.result:
            self.win_t = getattr(self, 'win_t', self.ar)
            if self.ar >= self.win_t + 50:
                self.result = f'[result] WIN ticks={self.ar}'
                self.simprint('University +50t → WIN')

    # ---------- 观测端 ----------
    def snapshot(self):
        us = [dict(slot=u['slot'], p=0, type=u['type'], tile=list(u['tile']),
                   hp=u['hp'], action=u.get('action', 0), sel=False)
              for u in self.units]
        us += [dict(slot=v['slot'], p=1, type=v['type'], tile=list(v['tile']),
                    hp=v['hp'], action=0, sel=False)
               for v in self.p1vil + self.p1mil]
        return {'tick': self.ar, 'aA': self.aA, 'gameMode': 32,
                'players': [
                    {'res': list(self.res), 'units': self.pop_used(),
                     'popCap': self.cap, 'trainQueue': len(self.queue),
                     'armyValue': self.myav, 'age': self.age,
                     'tcTile': TC[0] << 8 | TC[1],
                     'buildings': len(self.bld)},
                    {'res': list(self.p1res), 'armyValue': self.p1av, 'age': 0}],
                'units': us,
                'buildingRecs': [dict(slot=b['slot'], p=b['p'], type=b['type'],
                                      tile=list(b['tile']), uc=b['uc'], hp=b['hp'])
                                 for b in self.bld],
                'techFlags': [1] * 47}

    def ui_state(self):
        return {'aA': self.aA, 'cursor': list(self.cursor), 'res': list(self.res),
                'ar': self.ar}


assert_cursor()
m4ddrv.SIM = Sim()
t0 = time.time()
m4ddrv.main()
sim = m4ddrv.SIM
print(f'DRY-RUN 完成（{time.time() - t0:.1f}s）：result = {sim.result}')
print('milestones:', sim.marks)
if sim.parity_violation:
    print('!! 奇和格违规 —— 修复候选表后再上机')
print('WIN' if sim.result and 'WIN' in sim.result else 'NOT-WIN')
