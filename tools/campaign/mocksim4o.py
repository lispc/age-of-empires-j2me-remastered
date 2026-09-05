#!/usr/bin/env python3
"""m4n 驱动离线 dry-run 模拟器（mocksim4n = mocksim4m + BUGS-m4m 交棒 5 补口）：
解释驱动命令、演化简化世界，验证状态机分支。
跑法：M4D_DRY=1 python3 mocksim4o.py   （内部 import m4odrv 并注入 SIM）
压力注入：M4D_WAVE0=<tick> 首波最早时刻（真实抽签下界 1168，种子阈值 2100）；
M4D_WAVEGAP=<tick> 波间隔（默认 1000，压力线用 700 模拟 all-in 连发）；
M4D_BURST=<n> 敌 burst 连发波数（自 M4D_BURST0=<tick> 默认 5100 起，每 230t
一发 1 单位直插 TC——对照 boot3 5101-6135 实测 1150t 内 n1→5）。

v6.9-n 新增（交棒 5 补口）：
  5① 避险/通勤路径穿敌格=死：村民（type<=1）与任一 p1mil 距离 d²<4（2 格内）
      即阵亡——boot1 slot1 村民死于 (42,57) TC±1 交叉火力在旧 sim 不可见
      （「TC 警报避险把村民派进火线」这类驱动 bug 从此 dry 可见）。
  5② M4D_BURST/M4D_BURST0 burst 注入（见上）。
  5③ raid 击杀敌村民 → p1av-5 回写（r42 实证「敌村民被清才 reserve 干涸」
      的简化代理——杀村民断收入 → 未来波变薄、all-in 门松动）。

覆盖分支：build 雾格候选回退 / **奇和格硬断言（升时代锁死防线）** / train 排队与
pop / 弹窗冻结与 -6 / 敌波时间线（gate=armyValue 门）/ 庭院决战（含塔加成）/
raid 分队+keeper / 波出生回撤与复工 / 阶段 EARLY→MID→FULL / 矿仓(交存点)收益
建模 / 升时代舞步（方向断言）/ Mill-BS-塔-University 链 / WIN / LOSS /
**木银行（t0@WOOD_BANK 放格→西木岗豁免宵禁，波期木入账）**。

════ 已知分叉点清单（sim ≠ 真实引擎，r46 定版；dry WIN 只证分支正确）═══════
1. action 语义已同构：资源格到站=作业(2)、锚点/避险格=idle(0)（dry2 实锤后修）；
   v6.6-k 再补回送态 action=3（满载 ret，word7 低 nibble==3 的 dry 代理）。
2. 雾模型已同构：每 tick 单位 3×3 + 完工建筑菱形 r3（塔 r6），位末补 reveal。
3. TC 无攻击力已同构（r45 定案，sim 无 TC 防御分支）；v6.8-m 起围城掉血已
   入模（~0.1/unit/tick + 塔仇恨分流，BUGS-m4l 交棒 3）——围城僵局现在会
   以 LOSS 收场而非冻结。
4. 波模型温和：真实 all-in AV 滚 97+、250-600t 连发；sim 波距 ≥WAVEGAP、
   n≤pool/2 —— 压力线 M4D_WAVE0=1173 只是下界近似，真实级联更凶。
   v6.6-k 补走廊双腿行军（boot3 实证波扫矿区 ±7 格）+ 7.5t/格速度。
5. 收入偏高 ~30%：v6.6-k 起改为 carry 相位机（go/work/ret/back），趟时含走腿，
   比旧全局节拍模型更接近真实，但仍无地形绕行/占格堵塞。
6. 村民不死：sim 波不杀村民（真实 earliest 档死于线尾）——回送态硬闯波线的
   保命收益/代价在 dry 均不可见，上机才能验证。
7. 敌 AI 简化：无 n7==0 冻结漏洞利用、无补村民时点对齐、练兵抽签非 tick%10。
8. v6.6-k 新增 carry/flee 语义：retask 吞货（满载/部分载同吞，偏保守）、
   ret→交存建筑格入账→back 返岗；改派吞货计数与交付趟数在收尾打印。
"""
import os
import sys
import time

# r44 教训自检：忘带 M4D_DRY=1 = 驱动走真 FIFO 路径（上轮报假 LOSS@5358 事故）
assert os.environ.get('M4D_DRY') == '1', '需 M4D_DRY=1 跑 dry（否则驱动走 FIFO 路径）'

# 使用本目录的 m4odrv 副本（v6.5-j）
import m4odrv

# 压力注入（r46 反思 2①：波模型无法指定首波时刻/间隔——本轮补上）
WAVE0 = int(os.environ.get('M4D_WAVE0', '0') or 0)
WAVEGAP = int(os.environ.get('M4D_WAVEGAP', '0') or 0)
BURST_N = int(os.environ.get('M4D_BURST', '0') or 0)        # 交棒 5②：连发数
BURST0 = int(os.environ.get('M4D_BURST0', '5100') or 5100)  # burst 起始 ar

TC = (43, 57)
GOLD_T = {(35, 36), (36, 36), (37, 36), (35, 35), (36, 35), (34, 36),
          (34, 37), (37, 35)}
STONE_T = {(39, 40), (41, 40), (41, 38), (38, 40), (40, 40), (39, 39)}
WOOD_T = {(32, 52), (33, 51)} | set(m4odrv.WOOD_SAFE)
BCOST = {0: (15, 0, 0), 1: (15, 0, 0), 10: (20, 0, 10), 11: (5, 0, 0),
         5: (15, 0, 10), 6: (25, 0, 20), 12: (20, 5, 15), 4: (25, 0, 25)}
# 需要时代门的建筑：techFlags 语义（[15]/[16] 封建解锁 Mill/BS，[14] 城堡解锁 Univ）
AGE_GATE = {5: 1, 6: 1, 4: 2}
E_MINE = (16, 40)          # 敌矿工作业区中心


def assert_cursor():
    p = m4odrv.cursor_path(44, 60, 43, 57)
    assert p == [-1, -1, -4], p
    p = m4odrv.cursor_path(0, 0, 2, 0)
    assert p == [-2, -4], p
    p = m4odrv.cursor_path(43, 57, 43, 57)
    assert p == [], p
    p = m4odrv.cursor_path(45, 58, 43, 57)
    assert p is None, p
    # 偶和格断言：驱动所有候选表必须全偶和
    for name in ['B_CANDS', 'HOUSE_CANDS', 'CAMP_CANDS', 'MILL_CANDS',
                 'BS_CANDS', 'TOWER_CANDS', 'UNIV_CANDS']:
        for (x, y) in getattr(m4odrv, name):
            assert (x + y) % 2 == 0, f'{name} 含奇和格 ({x},{y})'
    assert sum(m4odrv.WOOD_BANK) % 2 == 0, '木帽格须偶和'
    # 注：WEST_FLEE/BANK_SCOUT 是行走目标，不吃光标偶和不变量（WOOD_SAFE 本身
    # 就有奇和格 (28,57)）——偶和只约束建造候选。
    assert (m4odrv.TC[0] + m4odrv.TC[1]) % 2 == 0
    assert (m4odrv.FRONT[0] + m4odrv.FRONT[1]) % 2 == 0
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
        self.p1pool = [dict(slot=60, type=2, tile=[8, 27], hp=255),
                       dict(slot=61, type=2, tile=[8, 27], hp=255)]
        self.p1res = [20, 20, 20]
        # 压力线种子：M4D_WAVE0<2100 时敌预攒 av=30（boot1/2 实测形态：
        # n=1 小波在 ~1173-2013 即出发，而 av 门 25 会让首波拖到 ~2040；
        # v6.9-n 阈值 2000→2100：中段签线 M4D_WAVE0=2013 也吃种子）
        self.p1av = 30 if (WAVE0 and WAVE0 < 2100) else 20
        self.myav = 0
        self.swallowed = 0      # v6.6-k：改派吞货计数（carry/flee 模型）
        self.deliv = {'g': 0, 's': 0, 'w': 0}   # 交付趟数（G 曲线观测）
        self.burst_fired = 0    # 交棒 5②：burst 已发波数
        self.last_burst = -10**9
        self.log = []
        # N4 雾模型（对齐 c.java 实测语义）：探索集=并集[全体 p0 单位当前格 3×3
        # （每 tick，c.java:5876 p()→5978 revealFogAroundUnit）+ 完工 p0 建筑曼哈顿
        # 菱形 r3（c.java:5888-5893 void_a，塔 r6；真实实现为每 tick 轮询一栋，
        # 幂等故此处等价）]。装载时全图 0x8000，探索单调不清零。build 判定
        # mapTiles<0（c.java:1479）。TC 开局即完工 → 菱形 r3 照亮 (42,58) 而
        # 照不到 (45,59)/(46,58)（d=4）——与 r42 boot 实测逐格一致。
        self.fog = set()
        self.reveal_diamond(TC[0], TC[1], 3)
        for u in self.units:
            self.reveal3(*u['tile'])

    def reveal3(self, x, y):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                self.fog.add(((x + dx) & 0x3F, (y + dy) & 0x3F))

    def reveal_diamond(self, x, y, r):
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if abs(dx) + abs(dy) <= r:
                    self.fog.add(((x + dx) & 0x3F, (y + dy) & 0x3F))

    def _dropoff(self, kind):
        """nearestDropOff 简化：木→木帽(有则)/TC；金/石→矿仓(有则)/TC。
        真实取近（c.java:8742），此处特殊交存点恒更近（矿仓贴矿区、木帽贴西树）。"""
        if kind == 'w':
            for b in self.bld:
                if b['type'] == 0:
                    return (b['tile'][0], b['tile'][1])
            return TC
        for b in self.bld:
            if b['type'] == 1:
                return (b['tile'][0], b['tile'][1])
        return TC

    # ---------- 驱动命令入口 ----------
    def handle(self, c):
        self.log.append(c)
        ps = c.split()
        if ps[0] == 'retask':
            sl = int(ps[1])
            u = next((x for x in self.units if x['slot'] == sl), None)
            if u is not None and u['type'] <= 1:
                # v6.6-k carry 模型（BUGS-m4j 交棒：不补则 dry 无真值）——
                # 改派满载村民 = 吞货（boot3 实锤：flee=retask → 满载金被吞）。
                # 部分载量同吞（retask 重置采集 FSM；只吞满载会低估断流）。
                if u.get('carg'):
                    self.swallowed += u['carg']
                    self.simprint(f'改派吞货 slot{sl} -{u["carg"]}{u["kind"]} '
                                  f'(累计吞 G/S/W 按 kind 计={self.swallowed})')
                    u['carg'] = 0
                u['ph'] = 'go'
                u['wt'] = 0
                u['tgt'] = [int(ps[2]), int(ps[3])]
                self.jobs[sl] = (int(ps[2]), int(ps[3]))
            else:
                self.jobs[sl] = (int(ps[2]), int(ps[3]))
        elif ps[0] == 'build':
            x, y, t = int(ps[1]), int(ps[2]), int(ps[3])
            cost = BCOST.get(t)
            ok = False
            if cost:
                gate = AGE_GATE.get(t, 0)
                if self.age < gate:
                    self.simprint(f'build FAIL t{t} 时代门 age={self.age}<{gate}')
                elif (x, y) not in self.fog:
                    self.simprint(f'build FAIL ({x},{y}) 迷雾格未探索')
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
                    self.queue.append([kind, len(self.queue), 4,
                                       list(b['tile'])])
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
                    # 真实语义：rally=selectUnits+orderMove，覆盖旧 retask job
                    # （dry6 尸检：探雾民兵 job 残留 → 波2 时 1v2 阵亡）
                    self.jobs.pop(u['slot'], None)
        elif ps[0] == 'sel':
            pass
        elif ps[0] == 'key':
            self.key(int(ps[1]))

    def key(self, k):
        # v7.0-o 菜单语义对齐读码（c.java confirmAtCursor/openBuildingMenu/l 卡片分支）：
        # aA=7=研究卡片（TC 专属，进入帧 T=0=确认位）；卡片内 -5/-6(22/38)=确认，
        # 数字键/方向键无效；aA=6 光标在己方完工 TC 上 -5 → 门过则开卡片。
        # 旧语义（-5 开菜单→49 换页→-5 确认）是 r43-r51 dry 假阳性源，废除。
        if self.aA == 2:
            if k == -6:
                self.aA = 6
            return
        if self.aA == 7:
            if k in (-5, -6):
                self.research()
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
            # confirmAtCursor：光标在己方完工 TC → openBuildingMenu case 9
            # 门：封建=完工兵营>=1；城堡=Mill+BS>=2（age>=2 无可研=拒绝）
            if tuple(self.cursor) == TC and any(
                    b['type'] == 9 and not b['uc'] for b in self.bld):
                gate = (self.age == 0 and self.cnt_done(10) >= 1) \
                    or (self.age == 1 and self.cnt_done(5) + self.cnt_done(6) >= 2)
                if gate:
                    self.aA = 7

    def research(self):
        # 真实：canAfford(0,2,21+age) 失败=静默回 6（卡片确认分支 bl2=false 不付钱）
        cost = (15, 15, 15) if self.age == 0 else (20, 20, 20)
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
        if self.aA in (2, 7):
            # aA=7 研究卡片=全场冻结（c.java dispatchRender：case 7 只跑
            # tickMissionScript，units/buildings/AI/弹道全停）——v7.0-o 对齐
            self.clock += 1.0
            return
        self.clock += 1.0
        self.ar += 90
        # N4 雾模型：每 tick 全体 p0 单位 3×3 + 完工 p0 建筑菱形 r3（塔 r6）
        for u in self.units:
            self.reveal3(*u['tile'])
        for b in self.bld:
            if b['p'] == 0 and not b['uc']:
                self.reveal_diamond(b['tile'][0], b['tile'][1],
                                    6 if b['type'] == 12 else 3)
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
                        self.mark('h%d' % self.cnt_done(11))
                    if b['type'] == 1:
                        self.mark('camp')
                    if b['type'] == 0:
                        self.mark('bank')     # 木银行放格/完工（hdr[9] 改道）
                    if b['type'] == 10:
                        self.mark('barracks')
                    if b['type'] == 12:
                        self.mark('tower%d' % self.cnt_done(12))
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
                    # 出生点=训练建筑格（boot3 实测：m#1 出膛于兵营 (45,59)，
                    # 非 TC——旧 TC 出生在围城期=出生即撞敌团，dry 假阳性源）
                    spawn = q[3] if len(q) > 3 else list(TC)
                    self.units.append(dict(slot=self.nextslot, p=0, type=t,
                                           tile=list(spawn), hp=255, action=0))
                    self.nextslot += 1
                    self.myav += 10
                    self.queue.remove(q)
                    self.simprint(f'出生 {kind}(t{t}) slot={self.nextslot - 1} '
                                  f'res={self.res}')
        # 村民作业（v6.6-k carry/flee 相位机：go→work→ret→back 循环）。
        # boot3 实证链补模：ret=满载回送态（action=3，word7 低 nibble==3），
        # 此态被 retask=吞货（handle 已建模）；避险折返耗时随真实走位产生
        # ——「波间隔<趟时→G=0」在 dry 可复现（对照基线）。
        # 趟时构成：mine(g/s)=540/720ar(矿仓有无)、mine(w)=270ar + 走腿
        # （8 格/tick：矿区→矿仓 10 manh≈180ar，与 boot3「往返趟时 600-900t」
        # 同量级；m2 实测 ~7t/格）。
        for u in self.units:
            if u['type'] > 1:
                continue
            if 'ph' not in u:
                u.update(ph='idle', tgt=list(u['tile']), wt=0, carg=0,
                         kind='', rt=None)
            j = self.jobs.get(u['slot'])
            ph = u['ph']
            if ph == 'idle':
                if j:
                    u['ph'] = 'go'
                    u['tgt'] = list(j)
                else:
                    u['action'] = 0
                ph = u['ph']
            if ph == 'go':
                t = u['tgt']
                d = abs(u['tile'][0] - t[0]) + abs(u['tile'][1] - t[1])
                if d > 0:
                    u['tile'][0] += max(-12, min(12, t[0] - u['tile'][0]))
                    u['tile'][1] += max(-12, min(12, t[1] - u['tile'][1]))
                    u['action'] = 1
                else:
                    u['tile'] = list(t)
                    # 到站动作语义对齐真实引擎：资源格=作业中(2)，非资源格
                    # （锚点/避险格）=闲置(0)——驱动"锚点推进/闲置重派"依赖
                    # action==0 判定（dry2 实锤：锚点到站 action=2 → 永不推进，
                    # 矿工全堵 (49,48)，G 全程 0）
                    tt = tuple(t)
                    if tt in GOLD_T:
                        u.update(ph='work', wt=0, kind='g', rt=[t[0], t[1]])
                    elif tt in STONE_T:
                        u.update(ph='work', wt=0, kind='s', rt=[t[0], t[1]])
                    elif tt in WOOD_T:
                        u.update(ph='work', wt=0, kind='w', rt=[t[0], t[1]])
                    else:
                        u['ph'] = 'idle'
                        u['action'] = 0
            elif ph == 'work':
                u['action'] = 2
                u['wt'] += 1
                if u['kind'] == 'w':
                    # v6.9-n 重校准（boot1 三连锚点）：pre-bank work 3→1。旧
                    # 「trip3+2+2=7it」与 live 矛盾：boot1/2/3 三局同驱动 B 完工
                    # 1243-1483（10W/654ar，1.5 工）→ live 趟时 ~340ar；且 sim 村民
                    # 走速 8/it=11ar/格 本身比 live（~7ar/格，m2 实测）慢 1.6x——
                    # 走速改 12/it（与敌波/民兵同级）后 work=1 即对齐：
                    #   pre-bank（TC 16 manh 双程 ~2.7it）：trip≈3.7it≈333ar/5W
                    #   ✓ live boot1 早期 10W/654ar；
                    #   post-bank（bank 贴树 6 manh 双程 ~1it）：trip≈2it≈180ar
                    #   （live boot3 263ar 锚点偏快 ~40%，收入偏高 doctrine 内）。
                    # 旧 7it 使 sim B@1950、m#1 与波 1 同拍出膛 → 拦截永不成立 →
                    # 双村民死于围城 TC（live 从未发生=dry 假阳性，dryA-mid 实锤）。
                    trip = 1
                elif u['kind'] == 's':
                    trip = 1   # v6.8-m 校准：石 trip 总长对齐 boot1 实测 ~240-260t
                               # （S=15@4316，S0 约 3000 出岗 → 5 趟×240）；camp 贴
                               # 石场 3 manh 腿 1it×2 + work 1it = 270ar/趟（旧
                               # work6=720ar 慢 live 2.8x，塔门 S>=15 永赶不上）
                else:
                    trip = 6 if camp_up else 8   # boot1 实测：金矿区 ~1.1/100t/vil
                if u['wt'] >= trip:
                    u['carg'] = 5 if u['kind'] == 'w' else 3   # 载量 hdr[50..52]
                    u['ph'] = 'ret'
                    u['action'] = 3   # 采满当拍即回送态（真实 word7 立即翻 3；
                                      # 留 act=2 一拍会被每-tick 轮询的驱动
                                      # 100% 命中=guard 永远输竞态）
                    u['tgt'] = list(self._dropoff(u['kind']))
            elif ph == 'ret':
                u['action'] = 3    # 回送态（真实 word7 低 nibble==3）
                t = u['tgt']
                d = abs(u['tile'][0] - t[0]) + abs(u['tile'][1] - t[1])
                if d > 0:
                    u['tile'][0] += max(-12, min(12, t[0] - u['tile'][0]))
                    u['tile'][1] += max(-12, min(12, t[1] - u['tile'][1]))
                else:
                    # 入账：到站=交存建筑格/TC（真实 onUnitArrived case 256）
                    idx = {'w': 0, 'g': 1, 's': 2}[u['kind']]
                    self.res[idx] += u['carg']
                    if u['kind'] == 'g':
                        self.deliv['g'] += 1
                        self.mark('g_first')
                    elif u['kind'] == 's':
                        self.deliv['s'] += 1
                    else:
                        self.deliv['w'] += 1
                    u['carg'] = 0
                    u['ph'] = 'back'
                    u['tgt'] = list(u['rt'] or t)
            elif ph == 'back':
                u['action'] = 1
                t = u['tgt']
                d = abs(u['tile'][0] - t[0]) + abs(u['tile'][1] - t[1])
                if d > 0:
                    u['tile'][0] += max(-12, min(12, t[0] - u['tile'][0]))
                    u['tile'][1] += max(-12, min(12, t[1] - u['tile'][1]))
                else:
                    u['ph'] = 'work'
                    u['wt'] = 0
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
                # 交棒 5③：raid 战果回写 armyValue（r42 简化代理：杀村民断
                # 收入 → 未来波变薄；旧 sim/驱动都看不到 raid 对 all-in 门）
                self.p1av = max(0, self.p1av - 5)
                self.simprint(f'敌村民 死于 raid 余{len(self.p1vil)} '
                              f'(p1av->{self.p1av})')
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
        # boot3 实录波型校准（n 序列 1,2,2,2,3,4 @2207-5548）：练兵 1/900t
        # （旧 540t 让 n=4-5 波早至，p1m 永不归零→探雾窗永不开→矿仓永不成，
        # sim 自锁死，live 无此形态）
        if self.ar > 1900 and self.ar % 900 < 90 and self.p1res[1] >= 5:
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
        # all-in 门 → 波次出发（m4f boot1 锚点校准：首发 2378、爆发间隔 ~1000-1400t
        # 内含 1-3 发、波间静默窗 650-850t；敌练兵 ~1/540t、av 斜率 20→90@6300）
        # v6.5-j：M4D_WAVE0/WAVEGAP 压力注入——最早抽签（真实下界 1168）+ 连发。
        gap = WAVEGAP or 1000
        # WAVE0 显式设置时不再被 2000 下限钳制（v6.5-j 的 max(2000,·) 让
        # M4D_WAVE0=1173 形同虚设——dry0 复盘发现）
        earliest = WAVE0 if WAVE0 else 2000
        if self.ar >= earliest and self.p1av >= 25 \
                and self.myav < self.p1av * 1.25 and len(self.p1pool) >= 2 \
                and self.ar - getattr(self, 'last_wave_ar', 0) >= gap:
            self.last_wave_ar = self.ar
            n = max(1, len(self.p1pool) // 2)   # 实测波 n=1（boot1/2 wave1），3/4 太肥
            for u in self.p1pool[:n]:
                # v6.6-k 走廊腿：boot3 实证波途经矿区走廊（camp@(42,40) 在位时
                # 北岗五矿工集体 flee；驱动北岗判据只有 d2e<49/d2_line<81，
                # 即波曾进矿区 ±7 格）。腿1 (8,27)→走廊点(34-38,47-51=boot1 尸位线
                # (34-37,47-53) 按 slot 抖动)，腿2 → TC±1（直扑 TC±1 的实测聚合）。
                u['wp'] = [34 + u['slot'] % 5, 47 + u['slot'] % 5]
                u['phase'] = 0
                u['tgt2'] = [TC[0] + u['slot'] % 3 - 1, TC[1] + u['slot'] % 2]
                self.p1mil.append(u)
            self.p1pool = self.p1pool[n:]
            self.simprint(f'敌 WAVE 出发 n={len(self.p1mil)} av={self.p1av}')
        # 交棒 5②：burst 跟波注入——boot3 实测 5101-6135（1150t）内 5 连发，
        # 每发 ~1 单位、间隔 200-450t（取 230t），出生在走廊点直插 TC±1。
        if BURST_N and self.burst_fired < BURST_N \
                and self.ar >= BURST0 and self.ar - self.last_burst >= 230:
            k = self.burst_fired
            self.burst_fired = k + 1
            self.last_burst = self.ar
            u = dict(slot=90 + k, type=2, tile=[34 + k % 5, 47 + k % 5],
                     hp=255)
            u['wp'] = [34 + k % 5, 47 + k % 5]
            u['phase'] = 0
            u['tgt2'] = [TC[0] + k % 3 - 1, TC[1] + k % 2]
            self.p1mil.append(u)
            self.p1av += 10
            self.simprint(f'敌 BURST {self.burst_fired}/{BURST_N} '
                          f'n={len(self.p1mil)} ar={self.ar}')
        # 敌行军 → 走廊点 → TC（速度 12 格/tick ≈7.5t/格：boot3 波 2207 出生→
        # 2486 首杀 ~250-280t 走 35 格）
        for u in self.p1mil:
            if 'wp' not in u:
                u['wp'] = [34 + u['slot'] % 5, 47 + u['slot'] % 5]
                u['phase'] = 0
                u['tgt2'] = [TC[0] + u['slot'] % 3 - 1, TC[1] + u['slot'] % 2]
            tgt = u['wp'] if u.get('phase', 0) == 0 else u['tgt2']
            dx, dy = tgt[0] - u['tile'][0], tgt[1] - u['tile'][1]
            d = max(abs(dx), abs(dy))
            if d > 0:
                s = min(1.0, 12.0 / d)
                u['tile'][0] += int(dx * s)
                u['tile'][1] += int(dy * s)
            elif u.get('phase', 0) == 0:
                u['phase'] = 1
        # 交棒 5①：避险/通勤单位与敌波同格(±1)判定——村民（含回送/通勤态）与
        # 任一 p1mil 距离 d²<4 即阵亡。boot1 slot1 村民死于 (42,57) TC±1
        # 交叉火力（TC警报避险把人派进火线）在旧 sim 不可见=dry 盲区。
        for u in list(self.units):
            if u['type'] > 1:
                continue
            for m in self.p1mil:
                dx = u['tile'][0] - m['tile'][0]
                dy = u['tile'][1] - m['tile'][1]
                if dx * dx + dy * dy < 4:
                    self.units.remove(u)
                    self.jobs.pop(u['slot'], None)
                    self.simprint(f'村民 slot{u["slot"]} 死于战区 '
                                  f'@{tuple(u["tile"])} 敌@{tuple(m["tile"])}')
                    break
        # 庭院战斗。r45 定案：TC 无攻击力（boot3 实测：孤敌围 1470t 无一次
        # combat 击杀，hp 255→103 全是被拆且掉血极慢）——删旧虚构 TC 防御分支
        # （`bld[0]['hp'] -= 30`，dry 假阳性源，BUGS-m4h 残局 1b）。交战范围同步
        # 收紧到相邻级（dist²<9，原 100=10 格远超真实"相邻触发"语义，否则风筝位
        # (46,60) d²=18 会被 sim 误判接战）。塔加成保留（塔射击真实存在，m1 实测）。
        near_front = [u for u in self.p1mil
                      if (u['tile'][0] - TC[0]) ** 2 + (u['tile'][1] - TC[1]) ** 2 < 64]
        defen = [u for u in self.units if u['type'] >= 2
                 and (u['tile'][0] - TC[0]) ** 2 + (u['tile'][1] - TC[1]) ** 2 < 9]
        twr = [b for b in self.bld if b['type'] == 12 and not b['uc']
               and (b['tile'][0] - TC[0]) ** 2 + (b['tile'][1] - TC[1]) ** 2 < 25]
        if near_front and (defen or twr):
            # v6.6-k 近战配对：每守军每 tick 只与 1 敌换血（live 相邻触发语义；
            # boot3 实录 m#2 1:1 交换）。旧全团焦点火（100×n/tick）让围城期
            # 出膛民兵出生即蒸发 → m 恒 0 → 村民训练门(nmil>=1)卡死 → v=2
            # 经济死锁，dry 死亡螺旋源（dry0 对照基线实锤）。
            for i, d_ in enumerate(defen):
                a_ = near_front[i % len(near_front)]
                d_['hp'] -= 100
                a_['hp'] -= 130
            for u in near_front:
                u['hp'] -= 65 * len(twr)   # 塔加成保留（塔射击真实存在，m1 实测）
        # v6.8-m 校准（BUGS-m4l 交棒 3）：围城啃建筑入模——注意 sim 1 迭代=90ar，
        # live 速率 ×90 换算成每迭代：
        #   TC 0.1/unit/ar → 9/unit/迭代（boot3 实测 hp229→0/895t，n=2-3 围城；
        #   n=3 全血 ~850ar 拆平，旧「极慢」估计系 m 分流仇恨时测得，低估 6x）；
        #   塔仇恨分流：敌优先啃完工塔 0.28/unit/ar → 25/unit/迭代（boot1：塔
        #   ~6100 完工→6300 殉，n=4-5），塔存活期 TC 免伤——塔价值=替 TC 挨打。
        free_siege = len(near_front) - min(len(defen), len(near_front))
        if near_front and twr:
            twr[0]['hp'] -= 25.0 * len(near_front)
        elif free_siege > 0:
            self.bld[0]['hp'] -= 9.0 * free_siege
        # 被拆的非 TC 建筑移除（塔殉职后不得再供塔加成/交存）
        for b in list(self.bld):
            if b is not self.bld[0] and not b['uc'] and b['hp'] <= 0:
                self.bld.remove(b)
                self.simprint(f'我方建筑 t{b["type"]} 被拆 '
                              f'({b["tile"][0]},{b["tile"][1]})')
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
        # 末尾再 reveal 一轮（单位本 tick 实际占据过的末位格）：真实引擎每 tick
        # 对单位当前格 3×3 揭雾，只 reveal 位首会漏掉"传送/拉回前最后一站"
        # （dry1 实锤：探雾民兵路过 (41,43) 未及 reveal 即被 rally 拉走 →
        # (40,42) 永久迷雾 → 矿仓 FAIL 死循环——dry 假阳性定律再现）。
        for u in self.units:
            self.reveal3(*u['tile'])

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
m4odrv.SIM = Sim()
if WAVE0 or WAVEGAP:
    print(f'*** 压力注入：M4D_WAVE0={WAVE0 or "(default)"} '
          f'M4D_WAVEGAP={WAVEGAP or 1000} ***', flush=True)
t0 = time.time()
m4odrv.main()
sim = m4odrv.SIM
print(f'DRY-RUN 完成（{time.time() - t0:.1f}s）：result = {sim.result}')
print('milestones:', sim.marks)
print(f'G/S/W 交付趟数: {sim.deliv}  改派吞货累计: {sim.swallowed}')
if sim.parity_violation:
    print('!! 奇和格违规 —— 修复候选表后再上机')
print('WIN' if sim.result and 'WIN' in sim.result else 'NOT-WIN')
