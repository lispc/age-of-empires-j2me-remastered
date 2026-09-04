#!/usr/bin/env python3
"""m4b 反 raid 制胜驱动 v3（第 2 轮）。

制胜方案（NOTES m4 档案 + 本轮读码校准）：
  wave1 前沿拦截 → 民兵 raid 敌村民集群 (15,40) 断收入 → all-in 门
  （敌army≥30 且 我army<敌×1.25，c.java:8458）永久短路 → boom 封建→Mill+BS→城堡
  →University(25木25石) → 50t → WIN。
boot2/3 尸检教训（本轮实测）：
  - tickms=10 下敌行军一 poll 跨 ~29 格，反应式避险物理无效 ⇒ ar>=1400 预防性撤离近木营。
  - Barracks 20木必须最先保障（村民训练不许抢木）；2 民兵前不训村民。
  - 民兵死亡→槽位压缩 ⇒ 分工按"槽位序索引 PLAN"整体重建，不做 slot 身份跟踪。
  - 拦截一律 FRONT 定点（boot3 单民兵被远端出生单位拖进走廊深追致死）。
只发可重放宏：retask/build/train/rally/key/sel。观测只读：aistate/state。
"""
import json
import subprocess
import sys
import time

WORK = '/tmp/aoe-camp/m4b'
FIFO = WORK + '/fifo'
LOG = WORK + '/play.log'
AISTATE = WORK + '/fifo.aistate.json'
STATEJ = WORK + '/fifo.json'

TC = (43, 57)
FRONT = (44, 58)          # v4: TC 庭院决战位（集中兵力迎击抵达敌军）
RAID = (15, 40)           # 敌村民/西金矿 blob 东缘
SAFE = [(49, 62), (48, 63), (47, 62)]   # 村民终极避险（走廊反向）

WOOD_NEAR = (32, 52)
WOOD_SAFE = [(29, 56), (28, 57), (28, 58), (27, 59)]
GOLD = [(34, 36), (35, 37), (36, 37)]
STONE = [(37, 40), (39, 40), (37, 39)]

B_CANDS = [(44, 59), (43, 58), (44, 58), (42, 58)]
HOUSE_CANDS = [(44, 60), (43, 61), (42, 60), (45, 59), (42, 62), (44, 62),
               (41, 61), (45, 61)]
MILL_CANDS = [(44, 55), (42, 56), (45, 55), (44, 56), (40, 57), (41, 59)]
BS_CANDS = [(45, 56), (45, 57), (44, 55), (43, 55), (41, 56), (40, 58)]
UNIV_CANDS = [(44, 56), (42, 56), (45, 57), (44, 55), (42, 59), (41, 58), (40, 58)]

# 分工 PLAN：按"槽位序"索引。EARLY=开局（0/1 在近木抢 Barracks 木），
# LATE=ar>=1400 起（近木营在 wave1 路径上，永久换安全木）。
PLAN_EARLY = [WOOD_NEAR, (33, 51), GOLD[0], STONE[0],
              WOOD_SAFE[2], GOLD[1], STONE[1], WOOD_SAFE[3], GOLD[2], STONE[2]]
PLAN_LATE = [WOOD_SAFE[0], WOOD_SAFE[1], GOLD[0], STONE[0],
             WOOD_SAFE[2], GOLD[1], GOLD[2], GOLD[0], STONE[1], WOOD_SAFE[3]]

MIL_TARGET = 5
VIL_TARGET = 8

def cmd(c, wait=0.3):
    try:
        subprocess.run(['sh', '-c', f"echo '{c}' > {FIFO}"], check=True, timeout=5)
    except subprocess.TimeoutExpired:
        print(f'!! FIFO 死锁: {c}', flush=True)
        sys.exit(2)
    time.sleep(wait)

def tail(n=200):
    try:
        with open(LOG, errors='replace') as f:
            return f.readlines()[-n:]
    except FileNotFoundError:
        return []

def result():
    for ln in tail(400):
        if '[result]' in ln:
            return ln.strip()
    return None

def aistate():
    for _ in range(6):
        try:
            cmd('aistate', 0.15)
            time.sleep(0.35)
            return json.load(open(AISTATE))
        except Exception:
            time.sleep(0.4)
    raise RuntimeError('aistate 无响应')

def fifo_state():
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
    dx, dy = tx - cx, ty - cy
    if (dx + dy) % 2 != 0:
        return None
    a = (dx + dy) // 2
    b = (dx - dy) // 2
    return ([-1] * max(a, 0) + [-2] * max(-a, 0)
            + [-3] * max(b, 0) + [-4] * max(-b, 0))

def build_fb(cands, btype, tag):
    for (x, y) in cands:
        cmd(f'build {x} {y} {btype}', 0.3)
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
    """光标舞步升时代（费用 feudal 15/15/15 castle 20/20/20）。扣款≥10/桶 判成功。"""
    st = fifo_state()
    if st.get('aA') != 6:
        return 'aA%d' % st.get('aA')
    cx, cy = st.get('cursor') or (0, 0)
    if (cx + cy) % 2 != 0:
        return 'oddclass'
    seq = cursor_path(cx, cy, *TC)
    if seq is None:
        return 'nopath'
    for k in seq:
        cmd(f'key {k}', 0.22)
    st2 = fifo_state()
    if st2.get('aA') == 4:
        cmd('key -5', 0.5)          # 误开菜单 → Continue
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
            time.sleep(0.5)
            st4 = fifo_state()
            if st4.get('aA') == 2:
                cmd('key -6', 0.5)
                continue
            r1 = st4.get('res')
            if r1 and any(r1[i] < r0[i] - 9 for i in range(3)):
                print(f'{tag}: PAID {r0} -> {r1}', flush=True)
                return 'paid'
        if rnd == 0 and st4.get('aA') == 6:
            cmd('key -5', 0.4)      # 确认框可能没按上，补一枪
    return 'nopay'

def main():
    a = aistate()
    print(f"start tick={a['tick']} aA={a['aA']} res={a['players'][0]['res']} "
          f"tc={a['players'][0]['tcTile']:#06x}", flush=True)
    if a['players'][0]['tcTile'] != (TC[0] << 8 | TC[1]):
        print('!! TC 不在预期格，地图代次不对，中止', flush=True)
        sys.exit(3)

    vslots = [u['slot'] for u in a['units'] if u['p'] == 0 and u['type'] <= 1]
    import os as _os
    resume = _os.environ.get('M4B_RESUME') == '1'
    jobs = {s: (PLAN_LATE if resume else PLAN_EARLY)[i]
            for i, s in enumerate(vslots) if i < len(PLAN_EARLY)}
    if resume:
        print('RESUME 模式：跳过开局工序', flush=True)
        for u in a['units']:
            if u['p'] == 0 and u['type'] <= 1:
                j = jobs.get(u['slot'])
                if j:
                    cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
    else:
        cmd(f"retask {vslots[0]} {WOOD_NEAR[0]} {WOOD_NEAR[1]}", 0.3)
        if len(vslots) > 1:
            cmd(f"retask {vslots[1]} 33 51", 0.3)
        print('retask 开局近木 x2', flush=True)

    last_nv = len(vslots)
    late_open = resume
    wave1_seen = resume
    wave1_seen = False
    prev_p1m = 0
    retreated = False
    mode = 'idle'
    last_rally = {'t': 0, 'tgt': None, 'md': ''}
    idle_poll = {}
    last_pos = {}
    t_end = time.time() + 1500
    last_line = ''
    research_cd = 0
    b_cd = m_cd = bs_cd = u_cd = mil_cd = vil_cd = 0

    while time.time() < t_end:
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
            time.sleep(0.8)
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
            print(f'ar={ar} *** wave 歼灭 → RAID 窗口 ***', flush=True)
        prev_p1m = len(p1mil)
        now = time.time()

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

        # ---- 预防性撤离：ar>=1400 近木营永久换安全木 ----
        if not late_open and ar >= 1400:
            late_open = True
            seq = PLAN_LATE
            jobs = {u['slot']: seq[i] for i, u in enumerate(vils) if i < len(seq)}
            if not retreated:
                for u in vils:
                    j = jobs.get(u['slot'])
                    if j:
                        cmd(f"retask {u['slot']} {j[0]} {j[1]}", 0.22)
            print(f'ar={ar} 预防撤离：近木→安全木 (wave1 出生窗)', flush=True)

        # ---- 村民避险/复工（近身 vd 触发；tickms=10 下只兜底）----
        vd = min((dist(m['tile'], v['tile']) for m in p1mil for v in vils), default=9e9)
        if not retreated and vd < 81 and vils:
            retreated = True
            for i, u in enumerate(vils):
                s = SAFE[i % len(SAFE)]
                cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
            print(f"ar={ar} 敌近(vd={vd}) 村民避险→SAFE", flush=True)
        elif retreated and vd > 169:
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

        # ---- 军事调度 ----
        near_tc = min((dist(m['tile'], TC) for m in p1mil), default=9e9)
        threat = bool(p1mil) and near_tc < 1100
        in_fight = any(u['hp'] < 240 for u in mymil)
        raid_ok = (len(mymil) >= 2 and (wave1_seen or ar > 3500)
                   and (not p1mil or near_tc > 1500))
        if threat and mymil:
            if len(mymil) >= len(p1mil) and in_fight:
                mode = 'fight'                    # 交战中且不吃亏，勿拉
            else:
                if (last_rally['md'] != 'front' or now - last_rally['t'] > 7):
                    cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                    last_rally = {'t': now, 'tgt': None, 'md': 'front'}
                    print(f'ar={ar} FRONT 拦截 (我m={len(mymil)} 敌m={len(p1mil)} '
                          f'army {myarmy}/{p1army})', flush=True)
                mode = 'front'
        elif mymil and raid_ok and not in_fight:
            vtgt = [u for u in p1any if u['type'] < 2]
            if vtgt:
                tgt = min(vtgt, key=lambda u: dist(u['tile'], RAID))
                if dist(tgt['tile'], RAID) < 625:
                    if (last_rally['md'] != 'raid' or last_rally['tgt'] is None
                            or dist(last_rally['tgt'], tgt['tile']) > 16
                            or now - last_rally['t'] > 8):
                        cmd(f"rally {tgt['tile'][0]} {tgt['tile'][1]}", 0.3)
                        last_rally = {'t': now, 'tgt': list(tgt['tile']), 'md': 'raid'}
                        print(f"ar={ar} RAID →村民({tgt['tile'][0]},{tgt['tile'][1]}) "
                              f"(m={len(mymil)})", flush=True)
                    mode = 'raid'
                    vtgt = None
            if mode != 'raid':
                if last_rally['md'] != 'raid' or now - last_rally['t'] > 15:
                    cmd(f'rally {RAID[0]} {RAID[1]}', 0.3)
                    last_rally = {'t': now, 'tgt': None, 'md': 'raid'}
                    print(f'ar={ar} RAID 驻点 ({RAID[0]},{RAID[1]}) (m={len(mymil)})',
                          flush=True)
                mode = 'raid'
        elif mymil and not in_fight:
            # 前沿常驻待命（wave1 前的既定拦截位）
            if last_rally['md'] != 'front' or now - last_rally['t'] > 12:
                cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                last_rally = {'t': now, 'tgt': None, 'md': 'front'}
                print(f'ar={ar} 前沿常驻 (m={len(mymil)})', flush=True)
            mode = 'standby'
        elif in_fight:
            mode = 'fight'
        else:
            mode = 'idle'

        # ---- 建筑 ----
        brecs = [b for b in a['buildingRecs'] if b['p'] == 0]
        def done(t):
            return [b for b in brecs if b['type'] == t and not b['uc']]
        def ucon(t):
            return any(b['type'] == t and b['uc'] for b in brecs)
        houses = done(11)
        barracks = done(10)
        mills = done(5)
        bss = done(6)
        pop_room = cap - popu - queued

        # ---- TC 血线警报（boot3 死因：TC 被啃穿；hp 见 buildingRecs）----
        tcrec = [b for b in brecs if b['type'] == 9]
        tc_hp = tcrec[0]['hp'] if tcrec else 0
        if tc_hp and tc_hp < 200:
            if not retreated and vils:
                retreated = True
                for i, u in enumerate(vils):
                    s = SAFE[i % len(SAFE)]
                    cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
            if mymil and now - last_rally['t'] > 6:
                cmd(f'rally {TC[0]} {TC[1] + 1}', 0.3)
                last_rally = {'t': now, 'tgt': None, 'md': 'front'}
            if tc_hp < 200 and ar % 100 < 12:
                print(f'ar={ar} !!! TC hp={tc_hp} 回防/避险 mode={mode}', flush=True)
            mode = 'front'

        if not barracks and not ucon(10) and W >= 20 and S >= 10 and now > b_cd:
            print(f'ar={ar} 建 Barracks res={[W,G,S]}', flush=True)
            build_fb(B_CANDS, 10, 'B')
            b_cd = time.time() + 12
            continue
        if not ucon(11) and len(houses) < 4 and pop_room <= 2 and W >= 5:
            print(f'ar={ar} 建 House (room={pop_room})', flush=True)
            build_fb(HOUSE_CANDS, 11, 'H')
            continue
        if not ucon(11) and not houses and len(vils) < 3 and W >= 5:
            build_fb(HOUSE_CANDS, 11, 'H0')
            continue
        if p0['age'] >= 1 and not mills and not ucon(5) and W >= 15 and S >= 10 and now > m_cd:
            build_fb(MILL_CANDS, 5, 'M')
            m_cd = time.time() + 12
            continue
        if p0['age'] >= 1 and mills and not bss and not ucon(6) and W >= 25 and S >= 20 and now > bs_cd:
            build_fb(BS_CANDS, 6, 'BS')
            bs_cd = time.time() + 12
            continue
        if p0['age'] >= 2 and not any(b['type'] == 4 for b in brecs) and not ucon(4) \
                and W >= 25 and S >= 25 and now > u_cd:
            uv = build_fb(UNIV_CANDS, 4, 'UNIV')
            u_cd = time.time() + 12
            if uv:
                print(f'ar={ar} *** UNIVERSITY {uv} — 50t 后应 WIN ***', flush=True)
            continue

        # ---- 生产 ----
        nmil = len(mymil)
        want_mil = MIL_TARGET if (mode in ('front', 'fight') or p1army >= 15) else 4
        if barracks and nmil < want_mil and W >= 5 and G >= 5 and pop_room >= 2 \
                and now > mil_cd:
            bx, by = barracks[0]['tile']
            cmd(f'train {bx} {by} 1', 0.3)
            tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
            if tr and '排队 1/1' in tr[-1]:
                mil_cd = now + 5.0
                print(f'ar={ar} TRAIN 民兵 (m={nmil} army {myarmy}/{p1army})', flush=True)
        nvil = len(vils)
        if len(houses) and nvil < VIL_TARGET and nmil >= 2 and W >= 5 \
                and pop_room >= 1 and now > vil_cd \
                and (mode not in ('front', 'fight') or nmil >= want_mil):
            hx, hy = houses[-1]['tile']
            cmd(f'train {hx} {hy} 1', 0.3)
            tr = [ln for ln in tail(15) if 'devMouse] train' in ln]
            if tr and '排队 1/1' in tr[-1]:
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

        st = (f"ar={ar} res={[W,G,S]} pop={popu}/{cap} v={len(vils)} m={nmil} "
              f"p1m={len(p1mil)} p1v={len(p1any)-len(p1mil)} army {myarmy}/{p1army} "
              f"age={p0['age']} B={bool(barracks)} H={len(houses)} M={bool(mills)} "
              f"BS={bool(bss)} mode={mode}")
        if st != last_line:
            print(st, flush=True)
            last_line = st
        time.sleep(0.55)
    else:
        print('TIMEOUT', flush=True)
    print('driver end', flush=True)

if __name__ == '__main__':
    main()
