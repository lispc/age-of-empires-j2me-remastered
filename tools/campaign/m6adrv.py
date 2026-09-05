#!/usr/bin/env python3
"""m6 总攻关驱动 v3 = tickFinalAssault v8 骨架 + m5 v3 稳定器 + boot3 冻结尸检修复。

v8 战术骨架（player-ai 5/5 WIN，平均 7330t，WORKLOG #6）：
- 废除野战：守军 aiEnabled=false 不追击，集结完直接拔塔，塔周守军经
  「反应式威胁」就地清算（质心 12 格 d²≤144 内敌兵=全军焦点）。
- 就地轮换回血（healTile）：siege hp<165 / 其它 hp<120 撤 → 威胁源 7 格外
  站桩回血到 250 归队；已在威胁源 8 格外=原地站。回家回血=添油，禁止。
- 逐塔稳拆：塔目标=距出发质心最近的塔；孤立塔（10 格内无第二塔）全员进场
  （近战 slot 低=天然肉盾）；集群塔非攻城在「塔朝锚点 8 格」戒备。塔全灭
  → 全员拆软建筑（距锚点最近优先）。软建筑无投射物，只有塔还手。
- orderUnit 门：action==1 不打断；目标相同不复发；healing 强制。

v2→v3→v4（boot4 尸检：17→6 只换来 n_e 19→9——弓兵站桩自废武功 + 回血线太晚）：
- 弓兵(t4) 回归 v8 行为：威胁态上威胁 tile（引擎认知：闲置远程只自动索敌
  建筑，对单位仅相邻开打——站桩远程对威胁零输出，boot3 冻结 + boot4 交换比
  崩坏双实证）。威胁态站桩只保留给真攻城器(t7/t8/t9)=v8 原样。
- 回血线 v8(165/120)→(175/130)：15t 轮询延迟 + 走出火线时间，boot4 实测
  120 线的剑士在撤离路上死亡（39hp→15→0 两个 poll 之间）。
- 焦点吸附/粘性站位/卡死检测保留（v3 三修复，boot4 无冻结复现）。
v3（boot3 尸检修复：焦点幽灵 + 站桩同格互堵 + 引擎 blocked-arrival 改写
slot[2] → 5 单位在 (9,4) 冻死 15000t）：
- 焦点吸附：滞回改为「焦点 tile 4 格内最近活敌」——焦点敌死后焦点前移，
  不再钉尸位（v2 的 glue 把活敌误当焦点还魂）。
- 按单位站桩：远程/攻城威胁态 = stance(威胁→该单位自身, 3)（每人自己一侧的
  站位点，d²=9≤16 在射程内），替代全员挤同一个质心侧点（boot3 的 (9,4) 互堵）。
- 粘性目标：focus/src 不变时复用上次站位点——单位行走中 pos 漂移不再把
  目标点拖着走（防「路径永远走不完」式重算）。
- 在位不打断：远程闲置(目标=自身)且已在射程 d²≤16 → 不发令（保装填）。
- 发令侧卡死检测：同一 tgt 连续 10 拍重发且位置纹丝不动 → 换锚点散点
  （覆盖引擎 blocked-arrival 改写 slot[2]=pos 的冻结模式）。
- 焦点优先级：投石机(t8) 优先（112/发 vs 甲 2）。"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m6a'
FIFO = os.path.join(W, 'fifo')
AIS = FIFO + '.aistate.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M6_POLL', '0.15'))
TIMEOUT = float(os.environ.get('M6_TIMEOUT', '900'))
NOSUM = os.environ.get('M6_QUIET', '0') == '1'

SIEGE_TYPES = (7, 8, 9)
RETREAT_SIEGE = 175   # v8 用 165；FIFO 15t 轮询延迟 → 提前撤离补偿
RETREAT_OTHER = 130   # v8 用 120；同上（boot4 实测：120 线的剑士走出火线前死亡）
HEAL_FULL = 250
THREAT_D2 = 144        # 质心 12 格（v8）
CLUSTER_D2 = 100       # 塔集群判定：目标塔 10 格内第二塔（v8）
SAFE_D2 = 64           # 距威胁源 ≥8 格 = 已脱险原地站（v8 healTile）
FOCUS_GLUE_D2 = 16     # 焦点吸附：焦点 tile 4 格内的活敌=焦点前移到它
RANGE_D2 = 16          # 远程射程（²≤16）
STUCK_POLLS = 10       # 同一 tgt 连续重发 10 拍(~1.5s)且未动 = 卡死

_t0 = time.time()
_dead = False
_n_retask = 0
_last_summary = -999.0


def log(msg):
    print(f"[{time.time() - _t0:7.1f}s] {msg}", flush=True)


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


def _run(script, timeout):
    global _dead
    try:
        subprocess.run(["sh", "-c", script], timeout=timeout)
        return True
    except subprocess.TimeoutExpired:
        _dead = True
        log(f'FIFO 无读者(进程退场?) script={script[:80]!r}')
        return False


def send_cmds(cmds):
    if not cmds:
        return True
    script = "; ".join(f"echo '{c}' > {FIFO}" for c in cmds)
    return _run(script, timeout=6.0)


def aistate():
    for _ in range(4):
        if not _run(f"echo 'aistate' > {FIFO}", timeout=4.0):
            return None
        time.sleep(0.12)
        try:
            with open(AIS) as f:
                return json.load(f)
        except Exception:
            time.sleep(0.15)
    return None


def d2(ax, ay, bx, by):
    return (ax - bx) ** 2 + (ay - by) ** 2


def stance(fx, fy, tx, ty, dist):
    """CampaignAi.stanceTile：from→to 方向 dist 格处（Chebyshev 归一，钳图界）。"""
    dx, dy = tx - fx, ty - fy
    m = max(abs(dx), abs(dy))
    if m == 0:
        return fx, fy
    return (max(1, min(62, fx + dx * dist // m)),
            max(1, min(62, fy + dy * dist // m)))


ANCHOR = None
healing = {}      # slot -> bool（razeHealing）
focus = None      # 焦点 tile（吸附活敌）
sticky = {}       # slot -> (srcTile, tgtTile) 威胁站位/回血点粘性
issue = {}        # slot -> [tgt, 连发计数, 上次 pos]
alt_k = {}        # slot -> 散点轮换序号（只进不退）
ALT = [(2, -2), (-2, 2), (2, 2), (-2, -2), (0, 3), (3, 0), (-3, 0), (0, -3)]


def retreat_tile(slot):
    ax, ay = ANCHOR
    return (max(1, min(62, ax + slot % 3 - 1)),
            max(1, min(62, ay + slot // 3 % 3 - 1)))


def retreat_alt(slot, k):
    ax, ay = ANCHOR
    dx, dy = ALT[k % len(ALT)]
    return (max(1, min(62, ax + dx + slot % 3)), max(1, min(62, ay + dy + slot // 3 % 3)))


def pick(fx, fy, items):
    """最近 tile（items: list of (x,y)）。"""
    best = None
    bd = None
    for (x, y) in items:
        d = d2(x, y, fx, fy)
        if bd is None or d < bd:
            bd, best = d, (x, y)
    return best


log(f'=== m6adrv v4 start poll={POLL} timeout={TIMEOUT} ===')
phase = 0
polls = 0
while time.time() - _t0 < TIMEOUT and not _dead:
    r = result()
    if r:
        log('RESULT ' + r)
        break
    st = aistate()
    if st is None:
        if _dead:
            break
        continue
    polls += 1
    tick = st['tick']
    units = [u for u in st.get('units', []) if u['p'] == 0]
    enemies = [u for u in st.get('units', []) if u['p'] == 1]
    ebs = [b for b in st.get('buildingRecs', []) if b['p'] == 1]

    if units and ANCHOR is None:
        ANCHOR = (sum(u['tile'][0] for u in units) // len(units),
                  sum(u['tile'][1] for u in units) // len(units))
        log(f'anchor={ANCHOR} t={tick} my={len(units)} enemy={len(enemies)} eb={len(ebs)}')
    if ANCHOR is None:
        continue
    ax, ay = ANCHOR
    if not units:
        log(f't={tick} 我方全灭(等通用判负)')
        time.sleep(POLL)
        continue
    if not ebs:
        log(f't={tick} 敌建筑清零(等脚本 20t 计时→WIN)')
        time.sleep(POLL)
        continue

    cx = sum(u['tile'][0] for u in units) // len(units)
    cy = sum(u['tile'][1] for u in units) // len(units)

    # ---- 塔目标（=v8：距锚点最近） ----
    towers = [(b['tile'][0], b['tile'][1]) for b in ebs if b['type'] == 12]
    softs = [(b['tile'][0], b['tile'][1]) for b in ebs if b['type'] != 12]
    tower = pick(ax, ay, towers)
    soft = pick(ax, ay, softs)
    towers_near = 0
    if tower:
        towers_near = sum(1 for (bx, by) in towers
                          if (bx, by) != tower and d2(bx, by, tower[0], tower[1]) <= CLUSTER_D2)

    # ---- 反应式威胁：质心 12 格内敌兵；焦点吸附活敌 + 投石机优先 ----
    cands = []
    for e in enemies:
        ex, ey = e['tile']
        d = d2(ex, ey, cx, cy)
        if d <= THREAT_D2:
            cands.append((d, (ex, ey), e['type']))
    threat = None
    if focus is not None:
        glued = [(d, tile) for (d, tile, _t) in cands
                 if d2(tile[0], tile[1], focus[0], focus[1]) <= FOCUS_GLUE_D2]
        if glued:
            glued.sort(key=lambda p: p[0])
            threat = glued[0][1]        # 焦点前移到最近活敌
    if threat is None and cands:
        best = None
        for (d, tile, ttype) in cands:
            prio = d - (60 if ttype == 8 else 0)
            if best is None or prio < best[0]:
                best = (prio, tile)
        threat = best[1]
    focus = threat
    src = threat if threat is not None else tower

    # ---- phase0 集结：2/3 到位（d²≤16）即转攻城 ----
    if phase == 0:
        arrived = sum(1 for u in units if d2(u['tile'][0], u['tile'][1], ax, ay) <= 16)
        if arrived * 3 >= len(units) * 2:
            phase = 2
            log(f't={tick} fa phase2 SIEGE(direct) arrived={arrived}/{len(units)}')
        else:
            send_cmds([f'retask {u["slot"]} {retreat_tile(u["slot"])[0]} {retreat_tile(u["slot"])[1]}'
                       for u in units if u['action'] != 1])
            time.sleep(POLL)
            continue

    melee_joins = threat is None and tower is not None and towers_near == 0
    cmds = []
    summary_b = []
    for u in units:
        s = u['slot']
        px, py = u['tile']
        t = u['type']
        hp = u['hp']
        act = u['action']
        tgt_tile = [px, py] == list(u['target'])   # 闲置在自身格（引擎 arrival 改写后=闲置）
        siege = t in SIEGE_TYPES
        # 轮换状态机（=razeHealing）
        h = healing.get(s, False)
        if not h and hp < (RETREAT_SIEGE if siege else RETREAT_OTHER):
            h = True
        elif h and hp >= HEAL_FULL:
            h = False
        healing[s] = h

        tgt = None
        sticky_keep = False
        if h:
            # healTile：脱险(≥8格)原地站；否则威胁源 7 格外（粘性）
            if src is None:
                tgt = retreat_tile(s)
            elif d2(px, py, src[0], src[1]) >= SAFE_D2:
                tgt = (px, py)
            else:
                prev = sticky.get(s)
                if prev and prev[0] == src and d2(prev[1][0], prev[1][1], src[0], src[1]) >= 49:
                    tgt = prev[1]
                    sticky_keep = True
                else:
                    tgt = stance(src[0], src[1], px, py, 7)
                    sticky[s] = (src, tgt)
        elif threat is not None:
            if siege:
                # 攻城器 3 格外站桩（v8）；闲置远程只索敌建筑、对单位仅相邻开打
                # （引擎认知 boot4 定案）——站桩=保攻城资产，杀伤靠近战群。
                if tgt_tile and d2(px, py, threat[0], threat[1]) <= 9:
                    continue                    # 已在安全位：不发令
                prev = sticky.get(s)
                if prev and prev[0] == threat and d2(prev[1][0], prev[1][1], threat[0], threat[1]) <= 13:
                    tgt = prev[1]
                    sticky_keep = True
                else:
                    tgt = stance(threat[0], threat[1], px, py, 3)
                    sticky[s] = (threat, tgt)
            else:
                tgt = threat                     # 近战+弓兵贴上（v8：t4 上威胁 tile，相邻才开打）
        elif siege:
            tgt = tower if tower is not None else (soft if soft is not None else retreat_tile(s))
        elif tower is None:
            tgt = soft if soft is not None else retreat_tile(s)
        elif melee_joins:
            tgt = tower
        else:
            tgt = stance(tower[0], tower[1], ax, ay, 8)   # 集群塔戒备点
        if tgt is None:
            continue
        # 下令门（=orderUnit 同目标不复发 / 接战不打断 / healing 强制）
        if list(u['target']) == list(tgt):
            continue
        if act == 1 and not h:
            continue
        # 发令侧卡死检测：同一 tgt 连发 STUCK_POLLS 拍且未动
        rec = issue.get(s)
        if rec and rec[0] == tgt and rec[2] == [px, py]:
            rec[1] += 1
            if rec[1] >= STUCK_POLLS:
                alt_k[s] = alt_k.get(s, 0) + 1
                alt = retreat_alt(s, alt_k[s])
                cmds.append(f'retask {s} {alt[0]} {alt[1]}')
                sticky.pop(s, None)
                issue[s] = [alt, 0, [px, py]]
                summary_b.append(f's{s} STUCK->{alt}')
                continue
        else:
            issue[s] = [tgt, 1, [px, py]]
        cmds.append(f'retask {s} {tgt[0]} {tgt[1]}')
        summary_b.append(f's{s}t{t}@{px},{py}hp{hp}{"H" if h else ""}->{tgt[0]},{tgt[1]}')
    n = len(cmds)
    if n:
        _n_retask += n
        send_cmds(cmds)
    if not NOSUM and (time.time() - _last_summary > 5.0 or threat is not None):
        _last_summary = time.time()
        avg_hp = sum(u['hp'] for u in units) // len(units)
        low = sorted((u['hp'], u['slot'], u['type']) for u in units)[:3]
        log(f't={tick} my={len(units)} avg_hp={avg_hp} low={low} n_e={len(enemies)} '
            f'eb={len(ebs)} tower={tower} near={towers_near} threat={threat} '
            f'heal={sum(healing.get(u["slot"], False) for u in units)} retask={n} {" ".join(summary_b[:8])}')
    time.sleep(POLL)

r = result()
if r:
    log('RESULT ' + r)
else:
    log(f'NO RESULT (dead={_dead} {time.time() - _t0:.0f}s polls={polls} retasks={_n_retask})')
