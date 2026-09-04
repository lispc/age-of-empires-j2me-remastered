#!/usr/bin/env python3
# m4 第 1 轮(r38)最终驱动, 5 boot 全 LOSS 但模式已硬化: 雾回退 build/同余类光标舞步/
# combat 去重/弹窗清扫/前沿 rally。下一轮 m4 直接改造本文件(反 raid 方案见 NOTES m4 档案)。
"""m4 制胜驱动 v4（最终版）。
开局: 采木→Barracks(44,59)→House(44,60,TC 同余类)→民兵→石/金分工→封建→Mill+BS→城堡→大学。
防御: 村民早避险(p1 军 x>=22 即撤), rally 前沿 (39,53) 迎敌(勿在 TC 下,尸位远离 TC)。
弹窗: aA==2 → key -6 永远清。
只发可重放宏: retask/build/train/rally/goto/key。state/sitrep 仅观测。
"""
import json, subprocess, time

WORK = '/tmp/aoe-camp/m4'
FIFO = WORK + '/fifo'
LOG = WORK + '/play.log'
TC = (43, 57)

def cmd(c, wait=0.3):
    subprocess.run(['sh', '-c', f"echo '{c}' > {FIFO}"], check=True)
    time.sleep(wait)

def state():
    for _ in range(6):
        try:
            cmd('state', 0.0)
            time.sleep(0.55)
            return json.load(open(WORK + '/fifo.json'))
        except Exception:
            time.sleep(0.35)
    raise RuntimeError('state no reply')

def tail(n=150):
    with open(LOG, errors='replace') as f:
        return f.readlines()[-n:]

def result():
    for ln in tail(500):
        if '[result]' in ln:
            return ln.strip()
    return None

_seen = set()
def new_combat():
    out = []
    for ln in tail(60):
        if '[combat]' in ln and ln not in _seen:
            _seen.add(ln)
            out.append(ln.strip())
    return out

def build_fb(tx, ty, btype, cands):
    for (x, y) in [(tx, ty)] + cands:
        cmd(f'build {x} {y} {btype}')
        time.sleep(0.25)
        ls = [ln for ln in tail(25) if 'devMouse] build' in ln]
        if ls and ' OK ' in ls[-1]:
            print(f'  build OK ({x},{y}) t{btype}', flush=True)
            return (x, y)
        print(f'  build FAIL ({x},{y}): {ls[-1].strip()[-40:] if ls else "?"}', flush=True)
    return None

def cursor_path(cx, cy, tx, ty):
    """返回方向键序列把光标从 (cx,cy) 移到 (tx,ty); 不可达返回 None。
    NW(-1,-1) SE(+1,+1) SW(-1,+1) NE(+1,-1) 各 = key -1/-2/-3/-4"""
    dx, dy = tx - cx, ty - cy
    if (dx + dy) % 2 != 0:
        return None
    seq = []
    # 先用 NE(+)x / SW(-)x 调 dx-dy, 再 NW/SE 调总和
    a = (dx + dy) // 2   # NW 数(负=SE)
    b = (dx - dy) // 2   # SW 数(负=NE)
    seq += [-1] * max(a, 0) + [-2] * max(-a, 0)
    seq += [-3] * max(b, 0) + [-4] * max(-b, 0)
    return seq

def cursor_to_tc(cx, cy):
    seq = cursor_path(cx, cy, *TC)
    if seq is None:
        return False
    for k in seq:
        cmd(f'key {k}', 0.22)
    return True

def research_now(res, tag):
    """光标已在 TC: -5 开菜单 → key 49 → -5 (→ -5 确认). 验证 res 扣款。"""
    d0 = state()
    r0 = d0.get('res')
    cmd('key -5', 0.3)
    cmd('key 49', 0.3)
    cmd('key -5', 0.3)
    time.sleep(1.0)
    d1 = state()
    r1 = d1.get('res')
    paid = r1 != r0
    print(f'{tag}: res {r0} -> {r1} paid={paid} aA={d1.get("aA")}', flush=True)
    if d1.get('aA') == 2:
        cmd('key -6', 0.4)
    return paid

def main():
    d = state()
    print(f'start ar={d["ar"]} res={d["res"]} cursor={d["cursor"]}', flush=True)
    cmd('retask 0 33 51'); cmd('retask 1 32 52')
    print('retask wood x2', flush=True)

    B = H1 = H2 = MILL = BS = UNIV = FEU = CAS = None
    trained = 0
    parked = False
    rallied = False
    t_end = time.time() + 1300
    last = ''
    while time.time() < t_end:
        r = result()
        if r:
            print('RESULT:', r, flush=True)
            return
        d = state()
        if d.get('aA') == 2:
            cmd('key -6', 0.4)
            continue
        res = d.get('res') or [0, 0, 0]
        ar = d.get('ar')
        cur = tuple(d.get('cursor') or (44, 58))
        p0 = [u for u in d.get('units', []) if u['p'] == 0]
        p1 = [u for u in d.get('units', []) if u['p'] == 1]
        p1mil = [u for u in p1 if u['type'] >= 2]
        vs = [u for u in p0 if u['type'] in (0, 1)]
        ms = [u for u in p0 if u['type'] >= 2]
        for c in new_combat():
            print('CMB:', c, flush=True)
        W, G, S = res

        # ---- 防御: 敌军越过 x=22 → 村民避险; 敌灭 → 复工 ----
        danger = any(u['tile'][0] >= 22 for u in p1mil)
        if danger and not parked and vs:
            parked = True
            for i, u in enumerate(vs):
                cmd(f'retask {i} {46 + (i % 2)} {60 + (i % 2)}', 0.25)
            print(f'ar={ar} 敌进({len(p1mil)}) 村民避险', flush=True)
        elif not danger and parked:
            parked = False
            jobs = job_plan(len(vs), W, G, S, FEU, CAS)
            for i in range(len(vs)):
                cmd(f'retask {i} {jobs[i][0]} {jobs[i][1]}', 0.25)
            print(f'ar={ar} 复工 {jobs}', flush=True)
        # ---- 迎敌: 前沿 rally(勿 TC) ----
        near = any((u['tile'][0] - TC[0]) ** 2 + (u['tile'][1] - TC[1]) ** 2 < 500 for u in p1mil)
        if near and ms and not rallied:
            cmd('rally 39 53')
            rallied = True
            print(f'ar={ar} rally 前沿 (militia={len(ms)})', flush=True)
        if not p1mil:
            rallied = False

        # ---- 建筑序列 ----
        if not B and W >= 20:
            print(f'ar={ar} 建 Barracks res={res}', flush=True)
            B = build_fb(44, 59, 10, [(45, 59), (45, 58), (44, 60)])
            continue
        if B and not H1 and W >= 5:
            H1 = build_fb(44, 60, 11, [(43, 61), (45, 60), (44, 61)])
            continue
        if B and H1 and not H2 and W >= 5 and len(vs) >= 3:
            H2 = build_fb(43, 61, 11, [(44, 61), (42, 61), (45, 61)])
            continue
        if B and FEU and not MILL and W >= 15 and S >= 10:
            MILL = build_fb(45, 61, 5, [(44, 62), (45, 62), (43, 62)])
            continue
        if FEU and MILL and not BS and W >= 25 and S >= 20:
            BS = build_fb(42, 61, 6, [(41, 61), (42, 62), (41, 62)])
            continue
        if CAS and not UNIV and W >= 25 and S >= 25:
            UNIV = build_fb(45, 60, 4, [(44, 62), (43, 62), (42, 62)])
            print(f'ar={ar} *** UNIVERSITY ({UNIV}) — WIN 应在放置+50t ***', flush=True)
            continue

        # ---- 练兵(民兵) & 村民 ----
        mymil = len(ms)
        want_mil = 3 if not CAS else 2
        if B and mymil + trained_pending(d) < want_mil and W >= 5 and G >= 5:
            pop = d.get('pop') or [0, 0]
            if pop[0] + 2 <= pop[1]:
                cmd(f'train {B[0]} {B[1]} 1')
                time.sleep(0.3)
                tr = [ln for ln in tail(20) if 'devMouse] train' in ln]
                if tr and '排队 1/1' in tr[-1]:
                    print(f'ar={ar} TRAIN militia (pop {pop})', flush=True)
                    time.sleep(2.0)
        if H1 and len(vs) < 5 and W >= 5:
            pop = d.get('pop') or [0, 0]
            if pop[0] + 1 <= pop[1]:
                cmd(f'train {H1[0]} {H1[1]} 1')
                time.sleep(0.3)
                tr = [ln for ln in tail(20) if 'devMouse] train' in ln]
                if tr and '排队 1/1' in tr[-1]:
                    print(f'ar={ar} TRAIN villager (pop {pop})', flush=True)
                    time.sleep(2.0)

        # ---- 升时代 ----
        if not FEU and B and W >= 15 and G >= 15 and S >= 15:
            if cursor_to_tc(*cur):
                print(f'ar={ar} 升封建尝试 res={res}', flush=True)
                if research_now(res, 'FEUDAL'):
                    FEU = True
            continue
        if FEU and MILL and BS and not CAS and W >= 20 and G >= 20 and S >= 20:
            if cursor_to_tc(*cur):
                print(f'ar={ar} 升城堡尝试 res={res}', flush=True)
                if research_now(res, 'CASTLE'):
                    CAS = True
            continue

        st = f'ar={ar} res={res} v={len(vs)} m={mymil} p1m={len(p1mil)} B={bool(B)} H={bool(H1)}/{bool(H2)} F={bool(FEU)} M={bool(MILL)} BS={bool(BS)} C={bool(CAS)} U={bool(UNIV)}'
        if st != last:
            print(st, flush=True)
            last = st
        time.sleep(0.9)
    print('TIMEOUT', flush=True)

def trained_pending(d):
    return d.get('queued') or 0

def job_plan(n, W, G, S, feu, cas):
    """复工分工: 石需求大→1-2 石, 金 1-2, 其余木。"""
    jobs = []
    for i in range(n):
        if i == 0 and S < 60:
            jobs.append((41, 40))
        elif i == 1 and G < 55:
            jobs.append((36, 37))
        elif i == 2 and S < 60:
            jobs.append((40, 40))
        elif i == 3 and G < 55:
            jobs.append((35, 36))
        else:
            jobs.append((33, 51) if i % 2 == 0 else (32, 52))
    return jobs

if __name__ == '__main__':
    main()
