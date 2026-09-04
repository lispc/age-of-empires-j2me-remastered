#!/usr/bin/env python3
"""m4d 制胜驱动 v6（第 4 轮）—— 纯防守 boom + 前置 Mining Camp 矿仓。

r40 定稿方案 + 本轮读码升级（证据见 BUGS-m4d.md）：
  1. 前置 Mining Camp(type1, 15木, 黑暗可建) 建在矿区旁：金/石交存点 =
     nearestDropOff(TC hdr[8], 矿仓 hdr[10]/hdr[11] 取最近) —— 矿工到岗后不再
     走暴露走廊（c.java:8743/6157）。
  2. 全部建筑候选只留偶和格（sum%2==0）：boot3 Barracks(44,59) 奇和格 = 完工吸
     光标入奇类 → 升时代舞步永久 nopath（本轮复盘新发现的潜在锁）。
  3. 石矿工第 3-4 村民就位（城堡链 100 石）；金矿工在 Mining Camp 落成后出岗。
  4. 波次防御：TC 庭院 (44,58) 定点决战（r40 八波全歼）+ 波出生预防回撤；
     矿工在矿位（off-corridor）不撤，只撤路中段 (42<y<54)。
  5. raid 弱化保用：m>=4 且静默窗 → <=2 分队压敌矿工（禁入 base 区 x<11∧y<33），
     波出生即回防；keeper 常驻庭院。
  6. 塔 = 石富余才建（S>=60，贴 TC），城堡链优先。
v5 继承：aistate 全量观测 / 弹窗 -6 / TC 血线警报 / PLAN 槽位序重建自愈 /
  闲置重派 / 金饥荒转金 / cursor_path 修复版升时代舞步 / build 雾格候选链。
DRY 模式（M4D_DRY=1）：命令注入 SIM（mocksim4d.py），不碰 FIFO。
"""
import json
import os
import subprocess
import sys
import time

DRY = os.environ.get('M4D_DRY') == '1'
WORK = os.environ.get('M4D_WORK', '/tmp/aoe-camp/m4d')
FIFO = WORK + '/fifo'
LOG = WORK + '/play.log'
AISTATE = WORK + '/fifo.aistate.json'
STATEJ = WORK + '/fifo.json'

TC = (43, 57)
FRONT = (44, 58)            # TC 庭院决战位（偶和 102）
KEEPER_SPOT = (44, 58)
RAID = (15, 40)             # 敌矿工 blob 东缘（base 区外）
SAFE = [(49, 62), (48, 63), (47, 62)]        # 波时避险点（TC 东南，off-corridor）

WOOD_NEAR = (32, 52)
WOOD_NEAR2 = (33, 51)
WOOD_SAFE = [(28, 58), (27, 59), (28, 57), (29, 56)]   # 前两位避开波走廊 10 格圈（d2e<100 误避险）
WOOD_ALL = set(WOOD_SAFE) | {WOOD_NEAR, WOOD_NEAR2}
STONE = [(39, 40), (41, 40), (41, 38), (38, 40), (40, 40)]
GOLD = [(35, 36), (36, 36), (37, 36), (35, 35), (36, 35), (34, 36)]

# 分工计划（按村民槽位序；金工从槽 2-3 就位——boot1 教训：民兵死光后金=0=永远
# 造不出兵。矿仓只是缩短趟程的经济优化。北岗部署只走静默窗——boot2 教训。）
# boot1 尸检（真实收入 ~2-4x 慢于 sim）：3木3金2石1木，金工提前到 3 人
PLAN = [WOOD_SAFE[0], WOOD_SAFE[1], GOLD[0], GOLD[1], GOLD[2],
        STONE[0], STONE[1], WOOD_SAFE[2]]

# 建筑候选——全部偶和格（硬性，见头部说明 2）
B_CANDS = [(45, 59), (42, 58), (46, 58)]                 # Barracks 20木10石
HOUSE_CANDS = [(42, 60), (44, 60), (46, 60), (40, 60), (42, 62), (44, 62), (40, 62)]
CAMP_CANDS = [(36, 40), (36, 38), (42, 40), (40, 42), (38, 42)]   # Mining Camp 15木
MILL_CANDS = [(44, 56), (42, 56), (46, 56)]              # Mill 15木10石
BS_CANDS = [(43, 55), (45, 57), (40, 58)]                # BS 25木20石
TOWER_CANDS = [(45, 57), (42, 58), (44, 56)]             # Tower 20木5金15石
UNIV_CANDS = [(46, 56), (44, 56), (42, 56), (40, 58)]    # Univ 25木25石

MIL_TARGET = 8
VIL_TARGET = 8
RAID_MIN = 3               # m>=3 即 raid（庭院至少留 2；m=3 出 1 分队，m>=4 出 2）
RAID_EARLIEST = 2800
RAID_MAX = 2
RAIDCHASE_R2 = 900         # 追 RAID 中心 30 格内的敌村民

ROAD_Y0, ROAD_Y1 = 42, 54  # 路中暴露段：波出生时此段村民回撤
MINE_YMAX = 42             # y<=42 视为已到矿位（off-corridor，不撤）

# DRY 注入口
SIM = None


def cmd(c, wait=0.3):
    if DRY:
        SIM.handle(c)
        return
    try:
        subprocess.run(['sh', '-c', f"echo '{c}' > {FIFO}"], check=True, timeout=5)
    except subprocess.TimeoutExpired:
        print(f'!! FIFO 死锁: {c}（进程可能已退）', flush=True)
        r = result()
        print('RESULT_CHECK:', r, flush=True)
        sys.exit(2)
    time.sleep(wait)


def tail(n=200):
    try:
        with open(LOG, errors='replace') as f:
            return f.readlines()[-n:]
    except FileNotFoundError:
        return []


def result():
    if DRY:
        return SIM.result
    for ln in tail(400):
        if '[result]' in ln:
            return ln.strip()
    return None


def aistate():
    if DRY:
        return SIM.snapshot()
    for _ in range(6):
        try:
            cmd('aistate', 0.15)
            time.sleep(0.35)
            return json.load(open(AISTATE))
        except Exception:
            time.sleep(0.4)
    raise RuntimeError('aistate 无响应')


def fifo_state():
    if DRY:
        return SIM.ui_state()
    for _ in range(6):
        try:
            cmd('state', 0.1)
            time.sleep(0.4)
            return json.load(open(STATEJ))
        except Exception:
            time.sleep(0.35)
    raise RuntimeError('state 无响应')


_seen = set()


def new_combat():
    out = []
    for ln in tail(80):
        if '[combat]' in ln and ln not in _seen:
            _seen.add(ln)
            out.append(ln.strip())
    return out


def cursor_path(cx, cy, tx, ty):
    """光标方向键路径。NW(-1,-1)/SE(+1,+1) 改 sum、SW(-1,+1)/NE(+1,-1) 改差。
    nNW-nSE = -(dx+dy)/2；nNE-nSW = (dx-dy)/2。（v4 符号 bug 已修，mocksim 有断言）"""
    dx, dy = tx - cx, ty - cy
    if (dx + dy) % 2 != 0 or (dx - dy) % 2 != 0:
        return None
    a = -(dx + dy) // 2
    b = (dx - dy) // 2
    return ([-1] * max(a, 0) + [-2] * max(-a, 0)
            + [-4] * max(b, 0) + [-3] * max(-b, 0))


def build_fb(cands, btype, tag):
    for (x, y) in cands:
        if (x + y) % 2 != 0:
            print(f'  !! 候选 ({x},{y}) 非偶和格，跳过（纪律）', flush=True)
            continue
        cmd(f'build {x} {y} {btype}', 0.3)
        if DRY:
            if SIM.last_build_ok:
                print(f'  build OK ({x},{y}) t{btype} [{tag}]', flush=True)
                return (x, y)
            print(f'  build FAIL ({x},{y}) t{btype}', flush=True)
            continue
        ls = [ln for ln in tail(25) if 'devMouse] build' in ln]
        if ls and ' OK ' in ls[-1]:
            print(f'  build OK ({x},{y}) t{btype} [{tag}]', flush=True)
            return (x, y)
        if ls:
            print(f'  build FAIL ({x},{y}) t{btype}: {ls[-1].strip()[-60:]}', flush=True)
    return None


def dist(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def try_research(tag):
    """光标舞步升时代（feudal 15/15/15，castle 20/20/20）。扣款>=10/桶 判成功。"""
    st = fifo_state()
    if st.get('aA') != 6:
        return 'aA%d' % st.get('aA')
    cx, cy = st.get('cursor') or (0, 0)
    seq = cursor_path(cx, cy, *TC)
    if seq is None:
        return 'nopath'
    for k in seq:
        cmd(f'key {k}', 0.22)
    st2 = fifo_state()
    if st2.get('aA') == 4:
        cmd('key -5', 0.5)
        return 'menu_accident'
    if st2.get('aA') != 6:
        return 'aA%d_mid' % st2.get('aA')
    if tuple(st2.get('cursor') or (0, 0)) != TC:
        return 'cursor_miss'
    r0 = st2.get('res')
    cmd('sel 55 15', 0.3)           # 清选中（FAIL 即清）
    st3 = fifo_state()
    if st3.get('aA') != 6:
        return 'popup_mid'
    cmd('key -5', 0.35)
    cmd('key 49', 0.35)
    cmd('key -5', 0.35)
    st4 = st3
    for rnd in range(2):
        for _ in range(6):
            time.sleep(0.5 if not DRY else 0)
            st4 = fifo_state()
            if st4.get('aA') == 2:
                cmd('key -6', 0.5)
                continue
            r1 = st4.get('res')
            if r1 and any(r1[i] < r0[i] - 9 for i in range(3)):
                print(f'{tag}: PAID {r0} -> {r1}', flush=True)
                return 'paid'
        if rnd == 0 and st4.get('aA') == 6:
            cmd('key -5', 0.4)
    return 'nopay'


def main():
    if DRY and SIM is None:
        print('DRY 需要注入 SIM', flush=True)
        sys.exit(1)
    a = aistate()
    print(f"start tick={a['tick']} aA={a['aA']} res={a['players'][0]['res']} "
          f"tc={a['players'][0]['tcTile']:#06x}", flush=True)
    if a['players'][0]['tcTile'] != (TC[0] << 8 | TC[1]):
        print('!! TC 不在预期格，地图代次不对，中止', flush=True)
        sys.exit(3)

    vslots = [u['slot'] for u in a['units'] if u['p'] == 0 and u['type'] <= 1]
    jobs = {}
    # 开局：两村民近木（wave1 出生前的安全窗，ar>=1400 或首波后切 PLAN_MID）
    cmd(f"retask {vslots[0]} {WOOD_NEAR[0]} {WOOD_NEAR[1]}", 0.3)
    if len(vslots) > 1:
        cmd(f"retask {vslots[1]} {WOOD_NEAR2[0]} {WOOD_NEAR2[1]}", 0.3)
    print('retask 开局近木 x2', flush=True)

    last_nv = len(vslots)
    idle_poll = {}
    idle_n = {}
    prev_p1m = 0
    last_b = False          # Barracks 完成沿（触发槽1 石矿出岗重派）
    last_camp = False       # 矿仓出现沿（触发槽0/1 分工切换重派）
    scouted = False         # 矿区已探雾（民兵到位或矿仓已建成）
    fleeing = set()          # 处于避险状态的村民槽（per-vil，不冻结全局经济）
    mode = 'idle'
    last_rally = {'t': 0.0, 'md': ''}
    last_raid = {'t': 0.0, 'tgt': None}
    raiders = set()
    now = 0.0
    it = 0
    t_end = time.time() + 2400
    last_line = ''
    research_cd = 0.0
    b_cd = m_cd = bs_cd = t_cd = u_cd = mil_cd = vil_cd = hb_cd = c_cd = 0.0
    rb_cd = 0.0
    prev_tc_hp = 255
    tc_alarm = False
    last_wave_birth = -10**9
    last_enemy_seen = 0      # 敌军最后一次存在的 ar（raid 静默窗 = ar-此值）
    last_wave_dead = -10**9  # 波被歼时刻（微调 c：出岗门槛放宽用）
    prev_had_p1m = False

    while DRY and it < 3000 or (not DRY and time.time() < t_end):
        it += 1
        now = SIM.clock if (DRY and SIM) else time.time()
        r = result()
        if r:
            print('RESULT:', r, flush=True)
            break
        a = aistate()
        if a.get('aA') == 2:
            cmd('key -6', 0.45)
            continue
        if a.get('aA') != 6:
            print(f"warn aA={a.get('aA')} 非战斗态", flush=True)
            time.sleep(0.8 if not DRY else 0)
            continue
        p0, p1 = a['players'][0], a['players'][1]
        W, G, S = p0['res']
        ar = a['tick']
        popu, cap = p0['units'], p0['popCap']
        queued = p0['trainQueue']
        myarmy, p1army = p0['armyValue'], p1['armyValue']
        units = a['units']
        vils = [u for u in units if u['p'] == 0 and u['type'] <= 1]
        mymil = [u for u in units if u['p'] == 0 and u['type'] >= 2]
        p1mil = [u for u in units if u['p'] == 1 and u['type'] >= 2]
        p1any = [u for u in units if u['p'] == 1]
        for c in new_combat():
            print('CMB:', c, flush=True)
        if len(p1mil) > prev_p1m:
            last_wave_birth = ar
            print(f'ar={ar} *** 敌波出生 n={len(p1mil)} ***', flush=True)
            raiders = set()          # 波出生：raid 分队召回
        prev_p1m = len(p1mil)
        if p1mil:
            last_enemy_seen = ar
        if not p1mil and prev_had_p1m:
            last_wave_dead = ar
            print(f'ar={ar} *** 波已全歼（+300t 矿工可出岗）***', flush=True)
        prev_had_p1m = bool(p1mil)

        gate_armed = p1army >= 30 and myarmy < p1army * 1.25

        camp_up = any(b['type'] == 1 and not b['uc'] for b in a['buildingRecs']
                      if b['p'] == 0)
        camp_any = camp_up or any(b['type'] == 1 and b['uc']
                                  for b in a['buildingRecs'] if b['p'] == 0)
        barracks_done = any(b['type'] == 10 and not b['uc']
                            for b in a['buildingRecs'] if b['p'] == 0)

        # 槽 0/1 动态（boot3 教训：矿仓是"到岗后不再穿走廊"的承重结构，木料
        # 优先给它——矿仓起before两村民分开）：双木工抢到矿仓款（W≥15），矿仓
        # 一交款/落成，槽1 才出岗石矿、槽0 转安全木
        seq = list(PLAN)
        if not camp_any:
            seq[0] = WOOD_NEAR
            seq[1] = WOOD_NEAR2
        elif not barracks_done:
            seq[1] = WOOD_NEAR2
        elif p0['age'] < 1:
            # 微调 b 补丁：封建前无石需求（Barracks 10S 开局自带），槽1 留木——
            # dry3 教训：camp 落成即派石矿=木材收入减半=House/vil/民兵全线断粮
            seq[1] = WOOD_NEAR2

        # 民兵探雾：矿仓候选格 (36,40)± 在 TC 视野外，需有人走到。m>=2 且静默时
        # 派非 keeper 民兵去 (37,41)（矿区东侧，off-corridor），到位即视为已探雾；
        # 之后 stray-recall 会自动把它拉回庭院（keeper 常驻，庭院不空防）
        if not scouted and len(mymil) >= 1 and not p1mil and ar >= 1500 \
                and not camp_any:
            far = max(mymil, key=lambda u: dist(u['tile'], TC))
            cmd(f"retask {far['slot']} 37 41", 0.22)
            scouted = True
            print(f"ar={ar} 民兵探雾 slot{far['slot']} → (37,41)", flush=True)
        if not scouted and mymil:
            if any(dist(u['tile'], (37, 41)) < 16 for u in mymil):
                scouted = True
                print(f"ar={ar} 矿区已探明（民兵到位）", flush=True)

        # 北岗部署窗（boot2 教训：敌行军 ~22 格/poll，反应式永远来不及——
        # 过路只允许在静默窗（无敌军且距上次见敌 >450t）或首波前的自由窗）
        def north_ok(u, j):
            if j[1] > MINE_YMAX:
                return True
            if u['tile'][1] <= MINE_YMAX:
                return True
            if ar < 1400 and last_wave_birth < 0:
                return True
            # 微调 c（BUGS-m4d）：出岗门槛放宽——静默窗 或 波全歼后 150t
            return (not p1mil) and (ar - last_enemy_seen > 450
                                    or ar - last_wave_dead > 150)

        # boot2 尸检：金工 3 人同时走北撞上 wave2 团灭于 (35,50-53)——
        # 改错峰制：同一时刻只允许 1 人在北向路上（到岗 y<=42 后下一个才出发）
        def north_walkers():
            n = 0
            for v_ in vils:
                if v_['slot'] in fleeing:
                    continue    # 避险者不在路上（dry12 尸检：意图占位困死金工）
                j_ = jobs.get(v_['slot'])
                if j_ and j_[1] <= MINE_YMAX and v_['tile'][1] > MINE_YMAX:
                    n += 1
            return n

        def can_go_north(u, j):
            if j[1] > MINE_YMAX or u['tile'][1] <= MINE_YMAX:
                return True
            return north_ok(u, j) and north_walkers() == 0

        # ---- 分工维护：村民数变化/Barracks 或矿仓状态沿 → 按槽位序重建 ----
        if len(vils) != last_nv or not jobs or barracks_done != last_b \
                or camp_any != last_camp:
            if barracks_done != last_b or camp_any != last_camp:
                jobs = {}          # 强制全量重派（槽1 石矿出岗 / 槽0 转安全木）
            last_b = barracks_done
            last_camp = camp_any
            last_nv = len(vils)
            jobs = {u['slot']: seq[i] for i, u in enumerate(vils) if i < len(seq)}
            for u in vils:
                if u['slot'] in fleeing:
                    continue
                j = jobs.get(u['slot'])
                if j and can_go_north(u, j):
                    cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
            print(f'ar={ar} 分工重建 n={len(vils)}', flush=True)

        # ---- per-vil 威胁规则（boot1 教训：全局复工门被 TC 营敌卡死=经济冻结）----
        # 入坑：敌距自身 <10 格或身处路中段且处于波后 450t 窗 → 避险 SAFE；
        # 出坑：敌距**岗位**全部 >12 格且部署窗允许 → 复工。
        for u in vils:
            sl = u['slot']
            j = jobs.get(sl)
            if j is None:
                continue
            if sl in fleeing:
                clear = all(dist(m['tile'], j) > 144 for m in p1mil) if p1mil \
                    else True
                if clear and can_go_north(u, j):
                    fleeing.discard(sl)
                    cmd(f"retask {sl} {j[0]} {j[1]}", 0.22)
                    idle_n[f'{sl}'] = 0
                continue
            if p1mil:
                d2e = min(dist(m['tile'], u['tile']) for m in p1mil)
                road_exposed = (ROAD_Y0 < u['tile'][1] < ROAD_Y1)
                if d2e < 100 or (road_exposed and ar - last_wave_birth < 450):
                    fleeing.add(sl)
                    s = SAFE[sl % len(SAFE)]
                    cmd(f"retask {sl} {s[0]} {s[1]}", 0.22)
                    idle_n[f'{sl}'] = 0

        # ---- 闲置卡死重派（仅非避险村民）----
        for u in vils:
            sl = u['slot']
            if sl in fleeing:
                continue
            j = jobs.get(sl)
            if j is None or u['action'] != 0:
                continue
            key = f'{sl}'
            pos = tuple(u['tile'])
            if idle_poll.get(key) == pos:
                idle_n[key] = idle_n.get(key, 0) + 1
            else:
                idle_n[key] = 0
            idle_poll[key] = pos
            if idle_n[key] >= 2 and pos != tuple(j):
                if can_go_north(u, j):
                    cmd(f"retask {sl} {j[0]} {j[1]}", 0.22)
                idle_n[key] = 0

        # ---- 金饥荒动态转金（仅中盘金断供时；开局金恒 10 不许触发——dry4 教训：
        # ar510 误抽木工挖金 → Barracks 拖 1300t → 无兵防波）----
        if G < 5 and ar > 3000 and now > rb_cd and jobs:
            wj = [sl for sl, j in jobs.items() if tuple(j) in WOOD_ALL]
            gj = [sl for sl, j in jobs.items() if tuple(j) in GOLD]
            if len(gj) < 2 and len(wj) > 1:
                sl = wj[-1]
                use = {}
                for g in GOLD:
                    use[g] = use.get(g, 0)
                for g in gj:
                    use[tuple(jobs[g])] = use.get(tuple(jobs[g]), 0) + 1
                gfree = min(GOLD, key=lambda g: use[g])
                jobs[sl] = gfree
                cmd(f"retask {sl} {gfree[0]} {gfree[1]}", 0.22)
                rb_cd = now + 12
                print(f'ar={ar} 转金: slot{sl} → {gfree} (金工{len(gj) + 1})',
                      flush=True)

        # ---- 建筑快照 ----
        brecs = [b for b in a['buildingRecs'] if b['p'] == 0]

        def done(t):
            return [b for b in brecs if b['type'] == t and not b['uc']]

        def ucon(t):
            return any(b['type'] == t and b['uc'] for b in brecs)

        houses = done(11)
        barracks = done(10)
        camps = done(1)
        mills = done(5)
        bss = done(6)
        towers = done(12)
        pop_room = cap - popu - queued

        # ---- TC 血线警报 ----
        tcrec = [b for b in brecs if b['type'] == 9]
        tc_hp = tcrec[0]['hp'] if tcrec else 0
        near_tc = min((dist(m['tile'], TC) for m in p1mil), default=9e9)
        tc_alarm = bool(p1mil) and (near_tc < 400 or tc_hp < prev_tc_hp - 1)
        prev_tc_hp = tc_hp
        if tc_alarm:
            # TC 遇袭：靠近 TC 的村民进避险（远处岗位照常）；军事回防
            for u in vils:
                if u['slot'] not in fleeing and dist(u['tile'], TC) < 144:
                    fleeing.add(u['slot'])
                    s = SAFE[u['slot'] % len(SAFE)]
                    cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
                    idle_n[f"{u['slot']}"] = 0
            if mymil and now - last_rally['t'] > 6:
                cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                last_rally = {'t': now, 'md': 'front'}
            if ar % 100 < 12:
                print(f'ar={ar} !!! TC 遇袭 hp={tc_hp} mode={mode}', flush=True)
            mode = 'front'

        # ---- 军事调度（threat > raid > 庭院驻守）----
        threat = bool(p1mil) and near_tc < 1100
        in_fight = any(u['hp'] < 240 for u in mymil)
        keeper = None
        if mymil:
            keeper = min(mymil, key=lambda u: dist(u['tile'], TC))
        raid_ready = (len(mymil) >= RAID_MIN and ar >= RAID_EARLIEST
                      and not p1mil and ar - last_enemy_seen > 300
                      and not tc_alarm and bool(barracks))
        if threat and mymil:
            if len(mymil) >= len(p1mil) and in_fight:
                mode = 'fight'
            else:
                if last_rally['md'] != 'front' or now - last_rally['t'] > 7:
                    cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                    last_rally = {'t': now, 'md': 'front'}
                    print(f'ar={ar} FRONT 拦截 (我m={len(mymil)} 敌m={len(p1mil)} '
                          f'army {myarmy}/{p1army})', flush=True)
                mode = 'front'
        elif raid_ready and not in_fight:
            mode = 'raid'
            def in_base_zone(t):
                return t[0] < 11 and t[1] < 33
            vtgt = [u for u in p1any if u['type'] < 2
                    and dist(u['tile'], RAID) < RAIDCHASE_R2
                    and not in_base_zone(u['tile'])]
            tgt = None
            if vtgt:
                tt = min(vtgt, key=lambda u: dist(u['tile'], RAID))['tile']
                tgt = tuple(tt)
            else:
                tgt = RAID
            need = (last_raid['tgt'] is None
                    or dist(last_raid['tgt'], tgt) > 16
                    or now - last_raid['t'] > 12)
            if need:
                home = sorted(mymil, key=lambda u: dist(u['tile'], TC))
                n_home = max(2, len(mymil) - RAID_MAX
                             if len(mymil) > RAID_MIN else RAID_MIN - 1)
                send = home[n_home:]
                if not send:
                    send = home[-1:]
                for u in mymil:
                    if u in send:
                        cmd(f"retask {u['slot']} {tgt[0]} {tgt[1]}", 0.22)
                        raiders.add(u['slot'])
                    elif u['slot'] in raiders or dist(u['tile'], FRONT) > 9:
                        cmd(f"retask {u['slot']} {FRONT[0]} {FRONT[1]}", 0.22)
                        raiders.discard(u['slot'])
                last_raid = {'t': now, 'tgt': list(tgt)}
                print(f"ar={ar} RAID 分队{len(send)} → ({tgt[0]},{tgt[1]}) "
                      f"(m={len(mymil)} army {myarmy}/{p1army} "
                      f"gate={'ARMED' if gate_armed else 'off'})", flush=True)
        elif mymil and not in_fight:
            # 驻守庭院（非 threat 态也把离院的散兵拉回，raider 除外）
            stray = [u for u in mymil
                     if u['slot'] not in raiders
                     and dist(u['tile'], FRONT) > 9]
            if stray and now - last_rally['t'] > 10:
                cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                last_rally = {'t': now, 'md': 'front'}
                mode = 'standby'
            elif not p1mil:
                mode = 'standby' if mymil else 'idle'
        elif in_fight:
            mode = 'fight'
        else:
            mode = 'idle'

        # ---- 建筑（顺序：Barracks → House → MiningCamp → Mill → BS → Tower(富余) → Univ）----
        if not barracks and not ucon(10) and W >= 20 and S >= 10 and now > b_cd:
            print(f'ar={ar} 建 Barracks res={[W, G, S]}', flush=True)
            build_fb(B_CANDS, 10, 'B')
            b_cd = now + 12
            continue
        if not ucon(11) and len(houses) < 4 and pop_room <= 2 and W >= 5 \
                and (camp_any or W >= 20) and now > hb_cd:
            # House 让位于矿仓木款（boot3：House 抢 5 木 → 矿仓永欠款）
            print(f'ar={ar} 建 House (room={pop_room})', flush=True)
            build_fb(HOUSE_CANDS, 11, 'H')
            hb_cd = now + 8
            continue
        if not camps and not ucon(1) and W >= 15 and scouted and now > c_cd:
            # 矿仓：木 >=15 + 已探雾即建（黑暗可建，15木0石）——
            # 它是"矿工到岗后永不再走走廊"的承重结构（金/石交存点取最近）
            print(f'ar={ar} 建 Mining Camp（矿仓，res={[W, G, S]}）', flush=True)
            cv = build_fb(CAMP_CANDS, 1, 'CAMP')
            c_cd = now + 12
            if cv:
                print(f'ar={ar} *** 矿仓 {cv} 落成：金/石本地交存 ***', flush=True)
            continue
        if p0['age'] >= 1 and len(mills) < 2 and not ucon(5) and W >= 15 \
                and S >= 10 and now > m_cd:
            # boot2 路线：双 Mill 凑城堡门（计数>=2，NOTES §4.4），省 BS 的 10W10S
            build_fb(MILL_CANDS, 5, 'M')
            m_cd = now + 12
            continue
        if p0['age'] >= 1 and bss and len(towers) < 1 and not ucon(12) \
                and S >= 60 and W >= 20 and G >= 5 and now > t_cd:
            tv = build_fb(TOWER_CANDS, 12, 'T')
            t_cd = now + 12
            if tv:
                print(f'ar={ar} 塔 {tv}（石富余 {S}）', flush=True)
            continue
        if p0['age'] >= 2 and not any(b['type'] == 4 for b in brecs) and not ucon(4) \
                and W >= 25 and S >= 25 and now > u_cd:
            uv = build_fb(UNIV_CANDS, 4, 'UNIV')
            u_cd = now + 12
            if uv:
                print(f'ar={ar} *** UNIVERSITY {uv} — 50t 后应 WIN ***', flush=True)
            continue

        # ---- 生产（boot2：村民块前移——民兵别抢断村民的 5 木）----
        nmil = len(mymil)
        want_mil = MIL_TARGET if (mode in ('front', 'fight') or p1army >= 15) else 5
        nvil_ = len(vils)
        if len(houses) and nvil_ < VIL_TARGET and (nmil >= 1 or ar > 1500) \
                and W >= 5 and pop_room >= 1 and now > vil_cd \
                and (nmil >= 3 or W >= 10):
            # 微调 b：村民与民兵并行排队（dry5 教训：mil_blocked 门让民兵独占
            # 生产位，村民 540t/个，金工永远晚一班）
            hx, hy = houses[-1]['tile']
            cmd(f'train {hx} {hy} 1', 0.3)
            if DRY:
                ok = SIM.last_train_ok
            else:
                tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
                ok = bool(tr and '排队 1/1' in tr[-1])
            if ok:
                vil_cd = now + 3.0
                print(f'ar={ar} TRAIN 村民 (v={nvil_})', flush=True)
        if barracks and nmil < want_mil and W >= (5 if (p1mil or nmil < 3) else 10) \
                and G >= 5 and pop_room >= 2 and now > mil_cd:
            bx, by = barracks[0]['tile']
            cmd(f'train {bx} {by} 1', 0.3)
            if DRY:
                ok = SIM.last_train_ok
            else:
                tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
                ok = bool(tr and '排队 1/1' in tr[-1])
            if ok:
                mil_cd = now + 5.0
                print(f'ar={ar} TRAIN 兵 (m={nmil} army {myarmy}/{p1army})',
                      flush=True)
        # ---- 升时代 ----
        safe_win = (not p1mil) or near_tc > 1600
        if not p0['age'] and barracks and W >= 15 and G >= 15 and S >= 15 \
                and safe_win and now > research_cd:
            rr = try_research('FEUDAL')
            research_cd = now + (3 if rr == 'paid' else 12)
            print(f'ar={ar} 封建尝试: {rr}', flush=True)
            continue
        if p0['age'] == 1 and len(mills) + len(bss) >= 2 and W >= 20 \
                and G >= 20 and S >= 20 and safe_win and now > research_cd:
            rr = try_research('CASTLE')
            research_cd = now + (3 if rr == 'paid' else 12)
            print(f'ar={ar} 城堡尝试: {rr}', flush=True)
            continue

        st = (f"ar={ar} res={[W, G, S]} pop={popu}/{cap} v={len(vils)} m={nmil} "
              f"p1m={len(p1mil)} p1v={len(p1any) - len(p1mil)} "
              f"army {myarmy}/{p1army} gate={'ARMED' if gate_armed else 'off'} "
              f"age={p0['age']} B={bool(barracks)} C={len(camps)} H={len(houses)} "
              f"M={bool(mills)} BS={bool(bss)} T={len(towers)} mode={mode}")
        if st != last_line:
            print(st, flush=True)
            last_line = st
        if DRY:
            SIM.tick_world()
        else:
            time.sleep(0.55)
    else:
        if not DRY:
            print('TIMEOUT', flush=True)
    print(f'driver end it={it}', flush=True)


if __name__ == '__main__':
    main()
