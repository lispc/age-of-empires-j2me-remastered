#!/usr/bin/env python3
"""m1 护送关驱动 v2 (missionIndex 1, r68) — 环路配方,移植 player-ai CampaignAi.tickEscort v2 (20/20)。

胜负（res111 解码）:
  WIN  = p0 单位#0 (slot0,村民) 静止位于 x[50,57)×y[57,64) 持续 20t。
  LOSS = 任一村民 (type<2) 死亡。军事死亡合法。

三阶段（与 CampaignAi v2 逐条对齐,坐标直接移植）:
  PH0   军事清口袋 12 格内固定敌（实测锚 (31,58) 12 格内无敌 → 直接跳 phase1）。
  SWEEP 军事沿 11 清扫点逐点集火（守敌接近触发 4-5 格 aggro,不清干净路过必死）;
        村民钉口袋围栏 x[20,52)×y[57,64)（出界拉回锚 (31,58)）。
  WALK  只送 slot0 沿 15 路径点走完全程（终点 (53,60) 在胜利区内,到位静止收尾）;
        军事拖后一路径点当保镖（walker 8 格内有敌 → 强制集火拦截）;
        其余村民留口袋（暴露面 ×1）。卡死 20 poll 重发当前腿。

探针实测布阵（2026-09-06 aistate）:
  p0: slot0 村 (31,58)[胜利单位] + slot1/2 村 (31,59)(32,59) + 4×t3 + 3×t4 + 1×t6
  p1: 16 守敌全怠机; SWEEP 覆盖 14 名（(40,34)/(48,48) 离环路 d²≥145 永不 aggro）;
      敌塔实位 (48,50)(50,50)(49,52)（任务简报的 (50,48)(52,49)(50,50) 是建筑层
      转置 bug 时代坐标）; WALK 全程离塔 d²≥58。

确定性契约: 决策只读 aistate/state（每 boot 起点确定性）; 禁墙钟差分/随机。
DRY 闸: M1_DRY=1 → 假 fifo + M1_DRY_SEQ 快照序列（dry 工具一律隔离工作目录）。
"""
import json
import os
import re
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m1v2'
DRY = os.environ.get('M1_DRY', '') == '1'
DRY_SEQ = os.environ.get('M1_DRY_SEQ', '')
FIFO = os.path.join(W, 'fifo')
if DRY:
    FIFO = '/tmp/aoe-camp/m1v2/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M1_POLL', '0.35'))
TIMEOUT = float(os.environ.get('M1_TIMEOUT', '1800'))

# ---- 环路配方坐标（CampaignAi.ESCORT_SWEEP / ESCORT_WALK 原表）----
SWEEP = [(16, 54), (15, 47), (23, 27), (25, 21), (32, 17), (37, 22),
         (49, 24), (54, 19), (49, 33), (61, 45), (60, 55)]
WALK = [(27, 54), (18, 55), (19, 46), (19, 36), (23, 30), (26, 23),
        (31, 18), (38, 21), (47, 22), (51, 28), (60, 29), (60, 39),
        (60, 49), (56, 55), (53, 60)]
FENCE = (20, 57, 52, 64)          # x[20,52) y[57,64)
WALK_STALL = int(os.environ.get('M1_WALK_STALL', '20'))   # poll 数,≈700t

_t0 = time.time()
_dead = False
stage = 'PH0'
stage_t = 0
anchor = None                     # 村民质心（首拍冻结）
sweep_i = 0
walk_i = 0
walk_stall = 0
walk_last_pos = None
issue = {}                        # 军事 stuck 表 slot -> [tgt, n, frompos]
lastpos = {}                      # 军事漂移监测
_dry_i = 0
ghosts = set()
_combat_seen = set()
_combat_m = re.compile(r'\[combat\] p(\d) type(\d+) died at \((\d+),(\d+)\) ar=(\d+)')
vill_death = False


def log(m):
    print(f"[{time.time() - _t0:7.1f}s] {m}", flush=True)


def send(cmds):
    global _dead
    if not cmds:
        return
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


def combat_lines():
    out = []
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[combat]' not in ln:
                    continue
                m = _combat_m.search(ln)
                if not m:
                    continue
                out.append((int(m.group(1)), int(m.group(2)),
                            (int(m.group(3)), int(m.group(4))),
                            int(m.group(5))))
    except Exception:
        pass
    return out


def update_ghosts(foes):
    """[combat] p1 死亡差分 → 幽灵过滤（死亡判定唯一真源=[combat]）。"""
    pool = list(foes)
    for p, ty, dxy, ar in combat_lines():
        if p != 1:
            if p == 0 and ty < 2 and not vill_death:
                global_vill_death()
            continue
        key = (ty, dxy, ar)
        if key in _combat_seen:
            continue
        _combat_seen.add(key)
        best, bd = None, 51
        for g in pool:
            if g['type'] != ty:
                continue
            dd = d2(tuple(g['tile']), dxy)
            if dd < bd:
                best, bd = g, dd
        if best is None:
            continue
        pool.remove(best)
        gt = (best['type'], tuple(best['tile']))
        ghosts.add(gt)
        log(f'  ☠[combat] p1 type{ty}@{dxy} ar={ar} → 记亡')


def global_vill_death():
    global vill_death
    vill_death = True
    log('!!! 村民死亡=[combat] p0 type<2 — res111 判负链已触发')


def filter_ghosts(foes):
    out = []
    for u in foes:
        t = tuple(u['tile'])
        if any(gty == u['type'] and d2(t, gxy) <= 2 for gty, gxy in ghosts):
            continue
        out.append(u)
    return out


def set_target(u, tgt, tag='', force=False):
    """AiKit.orderUnit 语义: 接敌(action=1)非 force 不打断; 同目标不重发。
    军事 stuck 8 次 → ±1 格 alt 跳（破 blocked-arrival 改写循环）。"""
    s = u['slot']
    tgt = (max(1, min(62, tgt[0])), max(1, min(62, tgt[1])))
    if not force and u.get('action') == 1:
        return False
    if list(u['target']) == list(tgt):
        return False
    rec = issue.get(s)
    if rec and rec[0] == tgt and rec[2] == [u['tile'][0], u['tile'][1]]:
        rec[1] += 1
        if rec[1] >= 8:
            alt = (max(1, min(62, tgt[0] + (1 if s % 2 else -1))),
                   max(1, min(62, tgt[1] + (1 if s % 3 else -1))))
            send([f'retask {s} {alt[0]} {alt[1]}'])
            issue[s] = [alt, 0, list(u['tile'])]
            log(f'  s{s} STUCK→{alt} {tag}')
        return False
    issue[s] = [tgt, 1, list(u['tile'])]
    send([f'retask {s} {tgt[0]} {tgt[1]}'])
    return True


def issue_walk(u, wp, tag):
    """walker 专用: 不 alt 跳（路线已验证,只重发当前腿）; 20 poll 无位移强重发。"""
    global walk_stall, walk_last_pos
    s = u['slot']
    pos = [u['tile'][0], u['tile'][1]]
    if walk_last_pos == pos:
        walk_stall += 1
    else:
        walk_stall = 0
        walk_last_pos = pos
    if list(u['target']) == list(wp) and walk_stall < WALK_STALL:
        return False
    if walk_stall >= WALK_STALL:
        log(f'  walker 卡死 {walk_stall} poll 重发当前腿 {wp}')
    send([f'retask {s} {wp[0]} {wp[1]}'])
    walk_stall = 0
    return True


def fence(vids, slots_by_slot, skip_slot=None):
    """村民围栏: 出 x[20,52)×y[57,64) 拉回锚。"""
    xmin, ymin, xmax, ymax = FENCE
    for v in vids:
        if skip_slot is not None and v['slot'] == skip_slot:
            continue
        px, py = v['tile']
        if px < xmin or px >= xmax or py < ymin or py >= ymax:
            set_target(v, anchor, f'fence/s{v["slot"]}', force=True)


def main():
    global stage, stage_t, anchor, sweep_i, walk_i
    log(f'm1drv v2 start DRY={DRY} W={W}')
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
        if stage_t == 0:
            log(f't={tick} [{stage}] my={len(mine)}')
        elif stage_t % 40 == 0:
            log(f't={tick} [{stage}] sweep={sweep_i}/11 walk={walk_i}/15 '
                f'my={len(mine)}')
        stage_t += 1
        if not mine:
            time.sleep(POLL)
            continue
        vids = sorted([u for u in mine if u['type'] < 2], key=lambda u: u['slot'])
        mil = [u for u in mine if u['type'] >= 2]
        if not vids:
            time.sleep(POLL)
            continue
        if anchor is None:
            ax = sum(v['tile'][0] for v in vids) // len(vids)
            ay = sum(v['tile'][1] for v in vids) // len(vids)
            anchor = (ax, ay)
            log(f'锚(村民质心)={anchor} 村民=' +
                ' '.join(f"s{v['slot']}@{v['tile'][0]},{v['tile'][1]}"
                         for v in vids) +
                ' 军事=' + ' '.join(f"s{u['slot']}t{u['type']}"
                                    for u in mil))
        foes_all = [u for u in stt['units'] if u['p'] == 1]
        update_ghosts(foes_all)
        foes = filter_ghosts(foes_all)
        walker = vids[0]              # slot0 = 胜利单位（探针实证 type0）
        wx, wy = walker['tile']

        if stage == 'PH0':
            tgt, best = None, 1 << 30
            for g in foes:
                dd = d2(tuple(g['tile']), anchor)
                if dd <= 144 and dd < best:
                    best, tgt = dd, g
            if tgt is None:
                log(f't={tick} PH0 空(口袋12格无敌) → SWEEP')
                stage, stage_t = 'SWEEP', 0
                continue
            for u in mil:
                set_target(u, tuple(tgt['tile']), 'PH0/focus')
        elif stage == 'SWEEP':
            fence(vids, None)
            if sweep_i >= len(SWEEP):
                log(f't={tick} SWEEP 11/11 完 → WALK')
                stage, stage_t = 'WALK', 0
                continue
            sp = SWEEP[sweep_i]
            tgt, best = None, 1 << 30
            for g in foes:
                dd = d2(tuple(g['tile']), sp)
                if dd <= 64 and dd < best:
                    best, tgt = dd, g
            if tgt is not None:
                for u in mil:
                    set_target(u, tuple(tgt['tile']), f'SWEEP{sweep_i}/focus')
            else:
                mc = [(u['tile'][0], u['tile'][1]) for u in mil]
                if mc:
                    mcx = sum(p[0] for p in mc) // len(mc)
                    mcy = sum(p[1] for p in mc) // len(mc)
                    if d2((mcx, mcy), sp) <= 16:
                        sweep_i += 1
                        log(f't={tick} sweep {sweep_i}/{len(SWEEP)} '
                            f'(质心@{mcx},{mcy})')
                        continue
                for u in mil:
                    set_target(u, sp, f'SWEEP{sweep_i}/go')
        elif stage == 'WALK':
            fence(vids, None, skip_slot=walker['slot'])
            danger, dbest = None, 1 << 30
            for g in foes:
                dd = d2(tuple(g['tile']), (wx, wy))
                if dd <= 64 and dd < dbest:
                    dbest, danger = dd, g
            if danger is not None:
                for u in mil:
                    set_target(u, tuple(danger['tile']), 'WALK/intercept',
                               force=True)
            else:
                tw = WALK[max(0, walk_i - 1)]
                for u in mil:
                    set_target(u, tw, 'WALK/trail')
            if walk_i < len(WALK):
                wp = WALK[walk_i]
                if d2((wx, wy), wp) <= 2:
                    walk_i += 1
                    log(f't={tick} walk {walk_i}/{len(WALK)} (walker@{wx},{wy})')
                else:
                    issue_walk(walker, wp, f'WALK{walk_i}')
        time.sleep(POLL)


if __name__ == '__main__':
    sys.exit(main())
