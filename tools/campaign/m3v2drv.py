#!/usr/bin/env python3
"""m3 拆家关驱动 v1 (missionIndex 3, r69)。目标: WIN < 10k ticks (旧配方 89191)。

胜利 = p1 建筑数==0 (res113, 20t 结算)。
判负 = 我方 8 兵全灭 (无建筑开局)。单兵死亡合法。

[r69 战术定案 — 离线规划器 plan3.py 双扫描实证]
 A. 弓兵帖扫在守军存活期无安全位: 全图扫描「walk ∧ d²目标楼≤16 ∧ d²守军>25」
    对 LC/House (双 scout 贴楼) 和 MC (3 剑士贴楼+塔) 都是空集 ⇒ 守军先歼。
 B. 守军清除 = 近战集火 (m0 勘误: 近战击杀零 reprisal; 6 兵集火 255hp/敌
    ≈71t/敌)。歼灭顺序按组: 组0=2×t5 scouts → 组1=3×t3 swords →
    组2=3×t4 archers; 组完成判定 = [combat] 死亡按 type 计数 (守军组型唯一:
    t5 只在组0, t3 只在组1, t4 只在组2)。
 C. 塔程规避: 组1→组2 行军走廊 (x14-22,y45-53) 是塔程带, 组2 开打前先收拢
    (24,44) (d²tower=61, plan3 验证可走) 再咬。组0/组1 战场本身塔程外。
 D. 拆楼分工: A=melee 前3 → LC+House → Mill; B=melee 后3 → MC 西帖
    (塔程外 d²tower≥29) → 塔; 我方 2 弓兵: 组0 死后即可帖 LC/House
    (d²LC≤16), 完工后补刀 Mill。塔 18,49 程²16 攻2/17t: 6 近战齐啃
    3.6/t → 71t < 首个被瞄单位 8 发阵亡线 136t ⇒ 零损窗口; hp<80 撤
    (12,53) 回血 ≥240 归队 (CampaignAi v3 轮换, 预期休眠)。
 E. 2 弓兵全程不对单位作战 (r59: 闲置远程无对单位手段), 只拆楼。

纪律: 决策只读 aistate/[combat]; retask 宏 (回放可重放); M3_DRY=1 假 fifo 闸;
     死亡熔断 (15s 丢3 → hold 8s); TIMEOUT 兜底; 幽灵过滤 ([combat] 唯一死源)。
"""
import json
import os
import re
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m3v2'
DRY = os.environ.get('M3_DRY', '') == '1'
DRY_SEQ = os.environ.get('M3_DRY_SEQ', '')
FIFO = os.path.join(W, 'fifo')
if DRY:
    FIFO = '/tmp/aoe-camp/m3v2/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M3_POLL', '0.3'))
TIMEOUT = float(os.environ.get('M3_TIMEOUT', '1500'))

# ---- 敌情 (spawn106 + 旧 base 直读, 开局 aistate buildingRecs 再对账) ----
GROUP_SPEC = [(5, 2), (3, 3), (4, 3)]   # 歼灭顺序: (守军type, 数量)
STG = [(37, 25), (16, 43), (40, 46)]     # 每组集结位 (aggro 圈外, plan3 验证)
RETREAT = [(14, 51), (12, 53)]           # 塔程外回血桩 (d²tower 20/52, 近)
ARCH_LC = [(34, 31), (33, 32)]           # 弓兵射 LC/House 帖 (d²LC 4/2)
A_LC = [(33, 31), (30, 29), (32, 29)]    # A 组啃 LC/House
A_MILL = [(35, 52), (36, 52), (35, 54)]  # A 组啃 Mill
B_MC = [(12, 48), (13, 47), (12, 50)]    # B 组啃 MC (塔程外)
B_TOWER = [(17, 48), (17, 49), (18, 48), (18, 50), (19, 48), (17, 50)]
ARCH_MILL = [(38, 51), (38, 53)]         # 弓兵补刀 Mill
TOWER = (18, 49)
LC, HOUSE, MC, MILL = (32, 31), (31, 31), (14, 49), (36, 53)

_t0 = time.time()
_dead = False
stage = 'M1'
stage_t = 0
gi = 0                    # 歼灭组指针
issue = {}
ROSTER = []               # 守军名册 (type/tile/dead), [combat] 独占记亡
_combat_seen = set()
_combat_m = re.compile(r'\[combat\] p1 type(\d+) died at \((\d+),(\d+)\) ar=(\d+)')
deaths_by_type = {}
prev_my = -1
death_marks = []
breaker_n = 0
hold_until = 0.0
_dry_i = 0
send_count = 0
stall_i = 0
stall_mark = (0, 0, '')
PENDING = []              # 本拍待发指令 (帧首批量应用)
healing = set()           # 撤退回血中的 melee slot


def log(m):
    print(f"[{time.time() - _t0:7.1f}s] {m}", flush=True)


def send(cmds):
    global _dead, send_count
    if not cmds:
        return
    send_count += len(cmds)
    if DRY:
        for c in cmds:
            log(f'[dry-send] {c}')
        return
    script = "; ".join(f"echo '{c}' > {FIFO}" for c in cmds)
    try:
        subprocess.run(["sh", "-c", script], timeout=6.0)
    except subprocess.TimeoutExpired:
        _dead = True
        log('FIFO 无读者(进程退场?)')


def aistate():
    global _dead, _dry_i
    if DRY:
        if DRY_SEQ:
            files = sorted(f for f in os.listdir(DRY_SEQ)
                           if f.startswith('dry-seq-'))
            if not files:
                return None
            f = files[min(_dry_i, len(files) - 1)]
            _dry_i += 1
            try:
                with open(os.path.join(DRY_SEQ, f)) as fh:
                    return json.load(fh)
            except Exception:
                return None
        return None
    for _ in range(4):
        if _dead:
            return None
        try:
            subprocess.run(["sh", "-c", f"echo 'aistate' > {FIFO}"],
                           timeout=4.0)
        except subprocess.TimeoutExpired:
            _dead = True
            return None
        time.sleep(0.10)
        try:
            with open(AIS) as f:
                return json.load(f)
        except Exception:
            time.sleep(0.15)
    return None


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


_log_pos = [0]


def combat_new_deaths():
    """增量读 play.log 新增段 (同 tick 同格双杀不去重——两条都是真死亡)。"""
    out = []
    try:
        with open(LOG, errors='replace') as f:
            f.seek(_log_pos[0])
            chunk = f.read()
            _log_pos[0] = f.tell()
        for ln in chunk.splitlines():
            if '[combat]' not in ln or 'p1 type' not in ln:
                continue
            m = _combat_m.search(ln)
            if not m:
                continue
            out.append((int(m.group(1)),
                        (int(m.group(2)), int(m.group(3)))))
    except Exception:
        pass
    return out


def mark_dead_from_combat():
    """[combat] 死亡 → roster 就近同型独占记亡 (防同型相邻误滤)。"""
    news = combat_new_deaths()
    for dty, dxy in sorted(news, key=lambda k: k[1]):
        deaths_by_type[dty] = deaths_by_type.get(dty, 0) + 1
        best, bd = None, 1 << 30
        for e in ROSTER:
            if e['dead'] or e['type'] != dty:
                continue
            dd = d2(e['tile'], dxy)
            if dd < bd:
                best, bd = e, dd
        if best is not None:
            best['dead'] = True
            log(f'  ☠[combat] p1 type{dty}@{dxy} 记亡 '
                f'(type{dty} 累计 {deaths_by_type[dty]})')
        else:
            log(f'  ☠[combat] p1 type{dty}@{dxy} 记亡 (无 roster 匹配!)')


def refresh_roster(p1units):
    """roster 活员位置从 aistate 就近独占刷新 (贪心, d²≤6 窗)。"""
    pool = [(u['type'], tuple(u['tile'])) for u in p1units]
    used = set()
    for e in ROSTER:
        if e['dead']:
            continue
        best, bd, bi = None, 7, -1
        for i, (ty, tl) in enumerate(pool):
            if i in used or ty != e['type']:
                continue
            dd = d2(e['tile'], tl)
            if dd < bd:
                best, bd, bi = tl, dd, i
        if best is not None:
            e['tile'] = best
            used.add(bi)


def living_of_type(gty):
    return [e for e in ROSTER if not e['dead'] and e['type'] == gty]


def building_at(ebs, tile):
    for b in ebs:
        if tuple(b['tile']) == tuple(tile):
            return b
    return None


def set_target(u, tgt, tag=''):
    s = u['slot']
    tgt = (max(1, min(62, tgt[0])), max(1, min(62, tgt[1])))
    # 沉淀守卫: 已 idle 且贴目标 d²≤2 → 别再打扰 (idle 才能自动接战/啃楼)
    if list(u['target']) == [u['tile'][0], u['tile'][1]] \
            and d2(tuple(u['tile']), tgt) <= 2:
        return
    if list(u['target']) == list(tgt):
        return
    rec = issue.get(s)
    if rec and rec[0] == tgt and rec[2] == [u['tile'][0], u['tile'][1]]:
        rec[1] += 1
        if rec[1] >= 8:
            alt = (max(1, min(62, tgt[0] + (1 if s % 2 else -1))),
                   max(1, min(62, tgt[1] + (1 if s % 3 else -1))))
            PENDING.append(f'retask {s} {alt[0]} {alt[1]}')
            issue[s] = [alt, 0, list(u['tile'])]
            log(f'  s{s} STUCK→{alt} {tag}')
        return
    issue[s] = [tgt, 1, list(u['tile'])]
    PENDING.append(f'retask {s} {tgt[0]} {tgt[1]}')


def flush():
    global PENDING
    if PENDING:
        send(list(PENDING))
        PENDING = []


def hold_at(u, tiles, tag):
    t = tuple(u['tile'])
    if any(d2(t, h) <= 2 for h in tiles):
        return
    near = min(tiles, key=lambda h: d2(t, h))
    set_target(u, near, tag)


def focus_all(units6, foe, tag):
    ft = tuple(foe['tile'])
    for u in units6:
        if d2(tuple(u['tile']), ft) <= 2:
            continue   # 已贴身: 沉淀自动接战, 别洗指令
        set_target(u, ft, f'{tag}/focus')


def main():
    global stage, stage_t, prev_my, hold_until, breaker_n, gi
    global death_marks, issue, healing
    log(f'm3v2drv v1 start DRY={DRY} W={W}')
    inited = False
    while True:
        if time.time() - _t0 > TIMEOUT:
            log('TIMEOUT 止损退出')
            return 1
        r = result()
        if r:
            log(f'RESULT: {r}')
            return 0
        stt = aistate()
        if not stt:
            if _dead:
                log('aistate 链路死, 退出')
                return 1
            time.sleep(POLL)
            continue
        tick = stt['tick']
        mine = [u for u in stt['units'] if u['p'] == 0]
        ebs = [b for b in stt.get('buildingRecs', []) if b['p'] == 1]
        if stage_t == 0:
            log(f't={tick} [{stage}] my={len(mine)} eb={len(ebs)}')
        elif stage_t % 30 == 0:
            log(f't={tick} [{stage}] my={len(mine)} eb={len(ebs)} '
                f'hp={[u["hp"] for u in mine]} gi={gi}')
        stage_t += 1
        now = time.time()
        if prev_my >= 0 and len(mine) < prev_my:
            death_marks.extend([now] * (prev_my - len(mine)))
        prev_my = len(mine)
        death_marks = [t for t in death_marks if now - t <= 12]
        if len(death_marks) >= 4 and breaker_n < 2:
            breaker_n += 1
            death_marks = []
            hold_until = now + 2
            log(f't={tick} !!! 熔断#{breaker_n}: 12s 丢4+ → hold2s my={len(mine)}')
        if not mine:
            log(f't={tick} 我方全灭 (等通用判负)')
            time.sleep(POLL)
            continue
        if not inited:
            log(f'aistate 对账: 敌楼={sorted(tuple(b["tile"]) for b in ebs)} '
                f'守军={[tuple(u["tile"]) for u in stt["units"] if u["p"] == 1]}')
            inited = True
        melee = sorted((u for u in mine if u['type'] in (3, 5)),
                       key=lambda u: u['slot'])
        archers = sorted((u for u in mine if u['type'] == 4),
                         key=lambda u: u['slot'])
        p1u = [u for u in stt['units'] if u['p'] == 1]
        if not ROSTER and p1u:
            ROSTER.extend({'type': u['type'], 'tile': tuple(u['tile']),
                           'dead': False} for u in p1u)
            log('roster 初始化: ' + ' '.join(
                f't{e["type"]}@{e["tile"]}' for e in ROSTER))
        mark_dead_from_combat()
        refresh_roster(p1u)
        if now < hold_until:
            time.sleep(POLL)
            continue

        if stage == 'M1':
            for u in mine:
                hold_at(u, [STG[0]], 'M1/行军')
            if all(d2(tuple(u['tile']), STG[0]) <= 64 for u in mine):
                stage, stage_t = 'KILL', 0
                log('M1 集结完成 (8格内) → KILL')
        elif stage == 'HEAL':
            # 战间回血检查点: idle 自愈 ~0.5/t, 伤员 150→255 ≈ 210t。
            # probe#5 实证: 带伤 (~150hp) 冲锋在齐射下活不过 26t。
            lo = min(u['hp'] for u in mine)
            if lo >= 240 or stage_t > 160:
                stage, stage_t = 'KILL', 0
                log(f't={tick} 回血完毕 (min hp={lo}) → KILL 组{gi}')
            else:
                if stage_t % 30 == 0:
                    log(f'  HEAL min_hp={lo} hp={[u["hp"] for u in mine]}')
                for u in melee:
                    hold_at(u, [tuple(u['tile'])], 'HEAL/原地')
        elif stage == 'KILL':
            gty, gcnt = GROUP_SPEC[gi] if gi < len(GROUP_SPEC) else (None, 0)
            if gty is None or deaths_by_type.get(gty, 0) >= gcnt:
                gi += 1
                if gi >= len(GROUP_SPEC):
                    stage, stage_t = 'RAZE', 0
                    log(f't={tick} 守军全清 → RAZE (直攻全歼)')
                else:
                    stage, stage_t = 'HEAL', 0
                    log(f't={tick} 组{gi-1}清完 → HEAL (战间回血, 下组=型'
                        f'{GROUP_SPEC[gi][0]})')
                continue
            liv = sorted(living_of_type(gty), key=lambda e: e['tile'])
            if not liv:
                # roster 无该型活员但 [combat] 未记满: 等 2 拍再放行
                if stage_t > 6:
                    log(f't={tick} 组{gi} 型{gty} roster 无活员 '
                        f'(deaths={deaths_by_type.get(gty, 0)}/{gcnt}) 放行')
                    deaths_by_type[gty] = gcnt
                time.sleep(POLL)
                continue
            if not melee:
                log(f't={tick} 近战全灭且组{gi}未清 → 无法胜利, 弃局')
                return 2
            # 分战场预集结: 全员 8 格内齐装才一波齐上 (probe#3 教训: 松散
            # 行军被迎击接待, 落单者 1v1 亏 3 兵)
            if any(d2(tuple(u['tile']), STG[gi]) > 100 for u in melee):
                for u in melee:
                    hold_at(u, [STG[gi]], f'K{gi}/集结')
            else:
                # 深轮换: hp<130 撤回集结位站桩回血 (0.5/t), ≥200 后下拍
                # focus 自动拉回 (main-1 教训: 前排挨 3.6-5.7/t 不轮换必死)
                for u in melee:
                    if u['hp'] < 130:
                        hold_at(u, [STG[gi]], f'K{gi}/轮换撤')
                    else:
                        set_target(u, tuple(liv[0]['tile']), f'K{gi}/focus')
                # 弓兵入堆: 相邻自动开打 (r59), 多 2 个 255hp 池+输出
                for a in archers:
                    if a['hp'] < 130:
                        hold_at(a, [STG[gi]], f'K{gi}/轮换撤')
                    else:
                        set_target(a, tuple(liv[0]['tile']), f'K{gi}/focus')
        elif stage == 'RAZE':
            lc = building_at(ebs, LC)
            hs = building_at(ebs, HOUSE)
            mc = building_at(ebs, MC)
            mill = building_at(ebs, MILL)
            tw = building_at(ebs, TOWER)
            left = sum(1 for x in (lc, hs, mc, mill, tw) if x is not None)
            if left == 0:
                log(f't={tick} 敌楼全平 (等 [result])')
                time.sleep(POLL * 2)
                continue
            if not melee and (mc is not None or tw is not None):
                log(f't={tick} 近战全灭且 MC/塔仍立 → 无法胜利, 弃局')
                return 2
            if len(melee) >= 4:
                half = len(melee) // 2
                by_hp = sorted(melee, key=lambda u: -u['hp'])
                grpB, grpA = by_hp[:half], by_hp[half:]   # 健康者啃塔/MC
            else:
                grpA, grpB = melee, []
            for i, u in enumerate(grpA):
                s = u['slot']
                if s in healing:
                    if u['hp'] >= 240:
                        healing.discard(s)
                        log(f'  s{s} 回血完毕归队')
                    else:
                        hold_at(u, RETREAT, 'RAZE/回血')
                        continue
                if lc is not None or hs is not None:
                    hold_at(u, [A_LC[i % len(A_LC)]], 'RAZE/A-LC')
                elif mill is not None:
                    hold_at(u, [A_MILL[i % len(A_MILL)]], 'RAZE/A-Mill')
                elif tw is not None:
                    hold_at(u, [B_TOWER[i % len(B_TOWER)]], 'RAZE/A-Tower')
            for i, u in enumerate(grpB):
                s = u['slot']
                if s in healing:
                    if u['hp'] >= 240:
                        healing.discard(s)
                        log(f'  s{s} 回血完毕归队')
                    else:
                        hold_at(u, RETREAT, 'RAZE/回血')
                        continue
                if mc is not None:
                    hold_at(u, [B_MC[i % len(B_MC)]], 'RAZE/B-MC')
                elif tw is not None:
                    if u['hp'] < 150 and d2(tuple(u['tile']), TOWER) <= 16:
                        healing.add(s)
                        log(f'  s{s} hp={u["hp"]} 撤退回血')
                        continue
                    hold_at(u, [B_TOWER[(i + 3) % len(B_TOWER)]],
                            'RAZE/B-Tower')
                elif mill is not None:
                    hold_at(u, [B_TOWER[i % len(B_TOWER)]], 'RAZE/B-Mill2')
            for i, a in enumerate(archers):
                if lc is not None or hs is not None:
                    hold_at(a, [ARCH_LC[i % len(ARCH_LC)]], 'RAZE/弓-LC')
                elif mill is not None:
                    hold_at(a, [ARCH_MILL[i % len(ARCH_MILL)]], 'RAZE/弓-Mill')
        flush()
        time.sleep(POLL)


if __name__ == '__main__':
    sys.exit(main())
