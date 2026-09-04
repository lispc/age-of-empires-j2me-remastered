#!/usr/bin/env python3
"""m4c 制胜驱动 v5（第 3 轮）—— m4bdrv v4 + 第 3 轮修正案 a-e。

修正案（r39 定稿，本轮逐条落地）：
  a. 金矿×4 起步（PLAN 槽 2-5 全金；民兵对耗 19.5金/1000t）
  b. raid 提前：militia>=3 且 ar>=1400 即 raid（不等 wave1_seen）
  c. TC 庭院 (44,58) 定点决战（承 v4 FRONT）
  d. TC 常备 1 民兵兜底（keeper=离 TC 最近民兵，驻 KEEPER_SPOT）
  e. 封建后贴 TC 补 1 塔（type12，治残余远程；TC 危急时黑暗期也允许）
v4 继承：aistate 全量观测 / ar>=1400 预防性撤离 / PLAN 槽位序重建自愈 /
  TC 血线警报 / 雾格候选表 build / combat 去重。
v4 修复：cursor_path 符号反了（a 应为 NW 计数=-(dx+dy)/2、b 为 NE 计数=(dx-dy)/2，
  原版算反导致升时代舞步向反方向漂）——本版修正，mocksim 内有方向断言。
军事调度改为 per-slot retask（raid 分队与 keeper 分离；rally 是全体原语无法部分调度）。
DRY 模式（M4C_DRY=1）：命令注入 SIM（mocksim.py），不碰 FIFO，离线跑状态机。
"""
import json
import os
import subprocess
import sys
import time

DRY = os.environ.get('M4C_DRY') == '1'
WORK = os.environ.get('M4C_WORK', '/tmp/aoe-camp/m4c')
FIFO = WORK + '/fifo'
LOG = WORK + '/play.log'
AISTATE = WORK + '/fifo.aistate.json'
STATEJ = WORK + '/fifo.json'

TC = (43, 57)
FRONT = (44, 58)          # c: TC 庭院决战位
KEEPER_SPOT = (44, 58)    # d: 兜底民兵驻点
RAID = (15, 40)           # 敌村民/西金矿 blob 东缘
SAFE = [(49, 62), (48, 63), (47, 62)]

WOOD_NEAR = (32, 52)
WOOD_NEAR2 = (33, 51)
WOOD_SAFE = [(29, 56), (28, 57), (28, 58), (27, 59)]
GOLD = [(34, 36), (35, 37), (36, 37), (34, 36)]      # a: ×4（34,36 双工）
STONE = [(37, 40), (39, 40)]

# a: 槽 0-1 近木（Barracks 20木 最高优先），槽 2-5 金 ×4，槽 6-7 石，槽 8-9 木/金
PLAN_EARLY = [WOOD_NEAR, WOOD_NEAR2, GOLD[0], STONE[0], GOLD[1], STONE[1],
              WOOD_SAFE[3], STONE[0], WOOD_SAFE[2], GOLD[1]]
PLAN_LATE = [WOOD_SAFE[0], WOOD_SAFE[1], GOLD[0], STONE[0], GOLD[1], STONE[1],
             WOOD_SAFE[2], STONE[0], WOOD_SAFE[3], GOLD[1]]

# 候选格全部偶和格（sum%2==0）：完工吸光标后方向键 sum 奇偶不变，
# 奇和格完工=光标永久不可达 TC=升时代锁死（dry-run 实证）。Barracks 完工早于
# 一切研究且必须抢建，保留奇格兜底但偶格优先。
B_CANDS = [(44, 59), (45, 59), (42, 59), (43, 58), (42, 58)]
HOUSE_CANDS = [(44, 60), (43, 61), (42, 60), (45, 59), (42, 62), (44, 62),
               (41, 61), (45, 61)]
MILL_CANDS = [(44, 56), (42, 56), (45, 55), (41, 59)]
BS_CANDS = [(43, 55), (45, 57), (40, 58)]
TOWER_CANDS = [(45, 57), (42, 58), (44, 56)]
UNIV_CANDS = [(44, 56), (42, 56), (45, 57), (40, 58)]

GOLD4 = [(34, 36), (35, 37), (36, 37), (34, 36)]
WOOD_ALL = set(WOOD_SAFE) | {WOOD_NEAR, WOOD_NEAR2}
MIL_TARGET = 7
VIL_TARGET = 8
RAID_MIN = 2              # b: militia>=2 即 raid（m>=3 才留 keeper；boot2 实证 m=3 够不到）
RAID_EARLIEST = 1400      # b/c: 预防撤离同拍
RAIDCHASE_R2 = 625        # 只追 RAID 中心 25 格内的敌村民（防深追致死，boot3 教训）

# DRY 注入口（mocksim.py 设置）
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
    nNW-nSE = -(dx+dy)/2；nNE-nSW = (dx-dy)/2。（v4 此函数符号反了，已修）"""
    dx, dy = tx - cx, ty - cy
    if (dx + dy) % 2 != 0 or (dx - dy) % 2 != 0:
        return None
    a = -(dx + dy) // 2      # >0 → NW(-1) 步数
    b = (dx - dy) // 2       # >0 → NE(-4) 步数
    return ([-1] * max(a, 0) + [-2] * max(-a, 0)
            + [-4] * max(b, 0) + [-3] * max(-b, 0))


def build_fb(cands, btype, tag):
    for (x, y) in cands:
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
    """光标舞步升时代（feudal 15/15/15，castle 20/20/20）。扣款≥10/桶 判成功。"""
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
    resume = os.environ.get('M4C_RESUME') == '1'
    jobs = {}
    if resume:
        print('RESUME 模式：跳过开局工序', flush=True)
        seq = PLAN_LATE
        jobs = {s: seq[i] for i, s in enumerate(vslots) if i < len(seq)}
        for u in a['units']:
            if u['p'] == 0 and u['type'] <= 1:
                j = jobs.get(u['slot'])
                if j:
                    cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
    else:
        cmd(f"retask {vslots[0]} {WOOD_NEAR[0]} {WOOD_NEAR[1]}", 0.3)
        if len(vslots) > 1:
            cmd(f"retask {vslots[1]} {WOOD_NEAR2[0]} {WOOD_NEAR2[1]}", 0.3)
        print('retask 开局近木 x2', flush=True)

    last_nv = len(vslots)
    late_open = resume
    wave1_seen = resume
    prev_p1m = 0
    retreated = False
    mode = 'idle'
    last_rally = {'t': 0.0, 'md': ''}
    last_raid = {'t': 0.0, 'tgt': None}
    last_keeper = 0.0
    idle_poll = {}
    last_pos = {}
    now = 0.0
    it = 0
    t_end = time.time() + 1500
    last_line = ''
    research_cd = 0.0
    b_cd = m_cd = bs_cd = t_cd = u_cd = mil_cd = vil_cd = hb_cd = rb_cd = 0.0
    prev_tc_hp = 255
    tc_alarm = False
    last_wave_birth = -10**9

    while DRY and it < 2500 or (not DRY and time.time() < t_end):
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
        if prev_p1m > 0 and not p1mil:
            wave1_seen = True
            print(f'ar={ar} *** 波次歼灭 → RAID 窗口 ***', flush=True)
        if len(p1mil) > prev_p1m:
            last_wave_birth = ar
        prev_p1m = len(p1mil)
        # 走廊穿越区预警：波次出生后 450t 内（敌抵达穿越区前），北半场村民回撤
        if p1mil and ar - last_wave_birth < 450:
            for i, u in enumerate(vils):
                if u['tile'][1] < 50 and u['tile'][1] > 0:
                    sxy = SAFE[i % len(SAFE)]
                    cmd(f"retask {u['slot']} {sxy[0]} {sxy[1]}", 0.22)
        gate_armed = p1army >= 30 and myarmy < p1army * 1.25

        # ---- 分工维护：村民数变化 → 按槽位序整体重建（自愈死亡压缩）----
        seq = PLAN_LATE if late_open else PLAN_EARLY
        if len(vils) != last_nv:
            last_nv = len(vils)
            jobs = {u['slot']: seq[i] for i, u in enumerate(vils) if i < len(seq)}
            if not retreated:
                for u in vils:
                    j = jobs.get(u['slot'])
                    if j:
                        cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
            print(f'ar={ar} 分工重建 n={len(vils)} stage={"LATE" if late_open else "EARLY"}',
                  flush=True)

        # ---- 预防性撤离：ar>=1400 或 TC 一旦遇袭 → 近木永久换安全木 ----
        if not late_open and (ar >= 1400 or tc_alarm):
            late_open = True
            seq = PLAN_LATE
            jobs = {u['slot']: seq[i] for i, u in enumerate(vils) if i < len(seq)}
            if not retreated:
                for u in vils:
                    j = jobs.get(u['slot'])
                    if j:
                        cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
            print(f'ar={ar} 预防撤离：近木→安全木', flush=True)

        # ---- 村民避险/复工（近身 vd 触发；tickms=10 下只兜底）----
        vd = min((dist(m['tile'], v['tile']) for m in p1mil for v in vils), default=9e9)
        if not retreated and vd < 81 and vils:
            retreated = True
            for i, u in enumerate(vils):
                s = SAFE[i % len(SAFE)]
                cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
            print(f"ar={ar} 敌近(vd={vd}) 村民避险→SAFE", flush=True)
        elif retreated and vd > 169 and not tc_alarm:
            retreated = False
            for u in vils:
                j = jobs.get(u['slot'])
                if j:
                    cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
            print(f'ar={ar} 复工', flush=True)

        # ---- 闲置卡死重派 ----
        if not retreated:
            for u in vils:
                sl = u['slot']
                j = jobs.get(sl)
                if j is None or u['action'] != 0:
                    continue
                pos = tuple(u['tile'])
                if last_pos.get(sl) == pos:
                    idle_poll[sl] = idle_poll.get(sl, 0) + 1
                else:
                    idle_poll[sl] = 0
                last_pos[sl] = pos
                if idle_poll[sl] >= 3 and pos != tuple(j):
                    cmd(f"retask {sl} {j[0]} {j[1]}", 0.22)
                    idle_poll[sl] = 0

        # ---- a 补强：金饥荒动态转金（修正案 a 的自愈版）----
        if G < 15 and now > rb_cd and jobs:
            wj = [sl for sl, j in jobs.items() if tuple(j) in WOOD_ALL]
            gj = [sl for sl, j in jobs.items() if tuple(j) in GOLD4]
            if len(gj) < 4 and len(wj) > 2:
                use = {}
                for g in GOLD4:
                    use[g] = use.get(g, 0)
                for g in gj:
                    use[tuple(jobs[g])] = use.get(tuple(jobs[g]), 0) + 1
                gfree = min(GOLD4, key=lambda g: use[g])
                sl = wj[-1]
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
        mills = done(5)
        bss = done(6)
        towers = done(12)
        pop_room = cap - popu - queued

        # ---- TC 血线警报（hp<200：避险+全员回防；<160：黑暗期急造塔）----
        tcrec = [b for b in brecs if b['type'] == 9]
        tc_hp = tcrec[0]['hp'] if tcrec else 0
        near_tc = min((dist(m['tile'], TC) for m in p1mil), default=9e9)
        tc_alarm = bool(p1mil) and (near_tc < 400 or tc_hp < prev_tc_hp - 1)
        prev_tc_hp = tc_hp
        if tc_alarm:
            if not retreated and vils:
                retreated = True
                for i, u in enumerate(vils):
                    s = SAFE[i % len(SAFE)]
                    cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
            if mymil and now - last_rally['t'] > 6:
                cmd(f'rally {TC[0]} {TC[1] + 1}', 0.3)
                last_rally = {'t': now, 'md': 'front'}
            if tc_hp < 160 and p0['age'] < 2 and not towers and not ucon(12) \
                    and W >= 20 and S >= 15 and now > t_cd:
                tv = build_fb(TOWER_CANDS, 12, 'T-emerg')
                t_cd = now + 12
                if tv:
                    print(f'ar={ar} !!! 紧急塔 {tv}（TC hp={tc_hp}）', flush=True)
            if ar % 100 < 12:
                print(f'ar={ar} !!! TC 遇袭 hp={tc_hp} 回防/避险 mode={mode}', flush=True)
            mode = 'front'

        # ---- 军事调度（threat > raid > 庭院待命）----
        threat = bool(p1mil) and near_tc < 1100
        in_fight = any(u['hp'] < 240 for u in mymil)
        keeper = None
        if mymil:
            keeper = min(mymil, key=lambda u: dist(u['tile'], TC))
        field_wave = bool(p1mil) and near_tc >= 900
        raid_ready = (len(mymil) >= RAID_MIN and ar >= RAID_EARLIEST
                      and not threat and not field_wave and bool(barracks))
        if tc_hp and tc_hp < 200:
            pass                                    # 已在上面回防分支处理
        elif threat and mymil:
            if len(mymil) >= len(p1mil) and in_fight:
                mode = 'fight'                      # 交战中且不吃亏，勿拉
            else:
                if last_rally['md'] != 'front' or now - last_rally['t'] > 7:
                    cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                    last_rally = {'t': now, 'md': 'front'}
                    print(f'ar={ar} FRONT 拦截 (我m={len(mymil)} 敌m={len(p1mil)} '
                          f'army {myarmy}/{p1army})', flush=True)
                mode = 'front'
        elif raid_ready and not in_fight:
            # b: raid 提前。keeper 守庭院，其余 per-slot retask 压制敌村民（驻留）。
            mode = 'raid'
            def in_base_zone(t):
                return t[0] < 11 and t[1] < 33
            vtgt = [u for u in p1any if u['type'] < 2
                    and dist(u['tile'], RAID) < RAIDCHASE_R2
                    and not in_base_zone(u['tile'])]
            if not vtgt and len(mymil) >= 3:
                vtgt = [u for u in p1any if u['type'] < 2
                        and dist(u['tile'], RAID) < RAIDCHASE_R2]
            tgt = tuple(vtgt[0]['tile']) if vtgt else RAID
            if vtgt:
                tgt = min(vtgt, key=lambda u: dist(u['tile'], RAID))['tile']
                tgt = tuple(tgt)
            need = (last_raid['tgt'] is None
                    or dist(last_raid['tgt'], tgt) > 16
                    or now - last_raid['t'] > 10)
            if need:
                keep_slot = keeper['slot'] if (keeper and len(mymil) >= 3) else None
                for u in mymil:
                    if keep_slot is not None and u['slot'] == keep_slot:
                        continue
                    if u['hp'] < 100:
                        cmd(f"retask {u['slot']} {FRONT[0]} {FRONT[1]}", 0.22)
                        continue
                    cmd(f"retask {u['slot']} {tgt[0]} {tgt[1]}", 0.22)
                last_raid = {'t': now, 'tgt': list(tgt)}
                n_raid = len(mymil) - (1 if (keeper and len(mymil) >= 3) else 0)
                print(f"ar={ar} RAID keeper={'Y' if (keeper and len(mymil) >= 3) else 'N'} "
                      f"分队{n_raid} → ({tgt[0]},{tgt[1]}) "
                      f"(m={len(mymil)} army {myarmy}/{p1army} "
                      f"gate={'ARMED' if gate_armed else 'off'})", flush=True)
            if keeper and dist(keeper['tile'], KEEPER_SPOT) > 9 \
                    and now - last_keeper > 12:
                cmd(f"retask {keeper['slot']} {KEEPER_SPOT[0]} {KEEPER_SPOT[1]}", 0.22)
                last_keeper = now
        elif mymil and not in_fight:
            # 兵力不足 raid 门槛：全员庭院待命
            if last_rally['md'] != 'front' or now - last_rally['t'] > 12:
                cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                last_rally = {'t': now, 'md': 'front'}
            mode = 'standby'
        elif in_fight:
            mode = 'fight'
        else:
            mode = 'idle'

        # ---- 建筑 ----
        if not barracks and not ucon(10) and W >= 20 and S >= 10 and now > b_cd:
            print(f'ar={ar} 建 Barracks res={[W, G, S]}', flush=True)
            build_fb(B_CANDS, 10, 'B')
            b_cd = now + 12
            continue
        # House：militia#1 可在初始 pop 5 内出，先 Barracks 后 House（r39 boot2 抢木教训）
        if not ucon(11) and len(houses) < 4 \
                and pop_room <= 2 and W >= 5 and now > hb_cd:
            print(f'ar={ar} 建 House (room={pop_room})', flush=True)
            build_fb(HOUSE_CANDS, 11, 'H')
            hb_cd = now + 8
            continue
        if not ucon(11) and not houses and len(vils) < 3 and W >= 5:
            build_fb(HOUSE_CANDS, 11, 'H0')
            continue
        if p0['age'] >= 1 and not mills and not ucon(5) and W >= 15 and S >= 10 \
                and now > m_cd:
            build_fb(MILL_CANDS, 5, 'M')
            m_cd = now + 12
            continue
        if p0['age'] >= 1 and mills and not bss and not ucon(6) and W >= 25 \
                and S >= 20 and now > bs_cd:
            build_fb(BS_CANDS, 6, 'BS')
            bs_cd = now + 12
            continue
        # e: 封建后贴 TC 补 1 塔（治残余远程；Mill 后即排，TC 危急走上面急造分支）
        if p0['age'] >= 1 and mills and len(towers) < 1 and not ucon(12) \
                and W >= 20 and G >= 5 and S >= 25 and now > t_cd:
            tv = build_fb(TOWER_CANDS, 12, 'T')
            t_cd = now + 12
            if tv:
                print(f'ar={ar} 塔 {tv} 落成（治远程）', flush=True)
            continue
        if p0['age'] >= 2 and not any(b['type'] == 4 for b in brecs) and not ucon(4) \
                and W >= 25 and S >= 25 and now > u_cd:
            uv = build_fb(UNIV_CANDS, 4, 'UNIV')
            u_cd = now + 12
            if uv:
                print(f'ar={ar} *** UNIVERSITY {uv} — 50t 后应 WIN ***', flush=True)
            continue

        # ---- 生产 ----
        nmil = len(mymil)
        want_mil = MIL_TARGET if (mode in ('front', 'fight') or p1army >= 15) else 6
        if barracks and nmil < want_mil and W >= 5 and G >= 5 and pop_room >= 2 \
                and now > mil_cd:
            bx, by = barracks[0]['tile']
            cmd(f'train {bx} {by} 1', 0.3)
            if DRY:
                ok = SIM.last_train_ok
            else:
                tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
                ok = bool(tr and '排队 1/1' in tr[-1])
            if ok:
                mil_cd = now + 5.0
                print(f'ar={ar} TRAIN 民兵 (m={nmil} army {myarmy}/{p1army})',
                      flush=True)
        nvil = len(vils)
        mil_blocked = not (barracks and nmil < want_mil and W >= 5 and G >= 5
                           and pop_room >= 2)
        if len(houses) and nvil < VIL_TARGET and (nmil >= 2 or ar > 2400) \
                and W >= 5 and pop_room >= 1 and now > vil_cd \
                and (mil_blocked or mode not in ('front', 'fight')
                     or nmil >= want_mil):
            hx, hy = houses[-1]['tile']
            cmd(f'train {hx} {hy} 1', 0.3)
            if DRY:
                ok = SIM.last_train_ok
            else:
                tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
                ok = bool(tr and '排队 1/1' in tr[-1])
            if ok:
                vil_cd = now + 5.0
                print(f'ar={ar} TRAIN 村民 (v={nvil})', flush=True)

        # ---- 升时代 ----
        safe_win = (not p1mil) or near_tc > 1600
        if not p0['age'] and barracks and W >= 15 and G >= 15 and S >= 15 \
                and safe_win and now > research_cd:
            rr = try_research('FEUDAL')
            research_cd = now + (3 if rr == 'paid' else 12)
            print(f'ar={ar} 封建尝试: {rr}', flush=True)
            continue
        if p0['age'] == 1 and mills and bss and W >= 20 and G >= 20 and S >= 20 \
                and safe_win and now > research_cd:
            rr = try_research('CASTLE')
            research_cd = now + (3 if rr == 'paid' else 12)
            print(f'ar={ar} 城堡尝试: {rr}', flush=True)
            continue

        st = (f"ar={ar} res={[W, G, S]} pop={popu}/{cap} v={len(vils)} m={nmil} "
              f"p1m={len(p1mil)} p1v={len(p1any) - len(p1mil)} "
              f"army {myarmy}/{p1army} gate={'ARMED' if gate_armed else 'off'} "
              f"age={p0['age']} B={bool(barracks)} H={len(houses)} M={bool(mills)} "
              f"BS={bool(bss)} T={len(towers)} mode={mode}")
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
