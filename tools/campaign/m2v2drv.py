#!/usr/bin/env python3
"""m2 经济关驱动 v3 (missionIndex 2, r70)。目标: WIN ~7-9k ticks (现行录制 64077)。

胜利 = 木>100 → 金>100 → 石>100 链式锁存 (res112, 严格 >100, 20t 结算)。
无脚本判负; 判负只剩通用规则 (TC 毁/全灭)。村民死亡合法但断产线。

[r70 彩排 2/4 双尸检 → v3 铁律]
 1. 完工简报弹窗 (aA=2, z=70) 冻结世界 → 每拍查 aA, -6/-7 交替清 (含死亡
    事件弹 aA=8/4)。不弹窗清 = 全员冻结 (彩排1)。
 2. **先仓后采铁律**: 采集 auto-loop 的交付回程由引擎 BFS 自选路径, 不保安全
    —— 金 v2 首趟回程被走南线 (30,12) 进 (29,9) aggro 圈 d2=10 杀死 (彩排4);
    石 v3 同理死于 (31,12) (彩排2)。金/石村民一律: 安全走廊腿 → 矿旁候车格
    (探格) → remote 拍仓 → 仓成才上矿格 (此后交付 1-2 格进仓零暴露)。
    候车格不得是仓格本体 (占格 build FAIL): 金候车 (56,16), 石候车 (33,3)。
 3. 木格枯竭 = 死刑: (11,24)+(10,25) 容量 62 载, 彩排2/4 木超额 100+ 载 →
    引擎 findNearbyResource 自动改派邻近格 → 走进 (16,28) 圈死 (14,26)。
    → 缺口驱动换线: w>=175 木工全部走石协议 (候车安全); 不许木超采。
 4. 楔死检测必须库存门 (同格+库存不涨 20 拍); 重接近格全部 d2enemy>25
    ((13,24)=d2 25 边界致 v0 被追杀, 彩排2)。
 5. 降级链: v2 死 → first_stone 门 w>=100 兜底; 房屋门脱离金仓; 补员按缺口
    (石→金→木) 重投。
前置仓 (nearestDropOff: 木=TC vs hdr[9]; 金/石=TC/hdr[10]/hdr[11] 最近):
  伐木场 type0 15W @(12,22) 放格即写 hdr[9]; 采矿场 type1 15W @(57,17)金 +
  @(34,3)石 (type1 完工才写 hdr[10]/[11]; build 宏 type1 上限 2)。
开销预算: 仓 45 + 房 5 + 补员 10 = 60W; 配额 101 → 木总量 161, W_QUOTA=175。
"""
import json
import os
import subprocess
import sys
import time

W = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m2v2'
DRY = os.environ.get('M2_DRY', '') == '1'
FIFO = os.path.join(W, 'fifo')
if DRY:
    FIFO = '/tmp/aoe-camp/m2v2/DRY-FIFO-MUST-NOT-EXIST'
AIS = FIFO + '.aistate.json'
STJ = FIFO + '.json'
LOG = os.path.join(W, 'play.log')
POLL = float(os.environ.get('M2_POLL', '0.30'))
TIMEOUT = float(os.environ.get('M2_TIMEOUT', '1500'))

# ---- 地理常量 (r70 校验: 可走 + d2enemy>25 + d2tower>16) ----
WOOD_A = (11, 24)
WOOD_B = (10, 25)
GOLD_A = (57, 19)      # 仓成后矿格 1
GOLD_B = (57, 21)      # 仓成后矿格 2 (front (57,22))
GOLD_HOLD = (56, 16)   # 金候车格 (探 (57,17); 非仓格本体)
STONE_A = (34, 4)
STONE_B = (32, 4)
STONE_C = (34, 7)
GOLD_LEGS = [(25, 3), (32, 3), (38, 5), (44, 8), (50, 11), (54, 14), GOLD_HOLD]
STONE_LEGS = [(22, 5), (28, 3), (33, 3)]    # 末腿=石候车格
STONE_HOLDBY = (33, 3)
LUMBER_SPOT = (12, 22)
GOLD_CAMP_SPOT = (57, 17)
STONE_CAMP_SPOT = (34, 3)
HOUSE_SPOT = (18, 12)
HOUSE_TILE = (19, 10)
# 重接近格 (全部 d2enemy>25)
REAPPROACH = {WOOD_A: (12, 24), WOOD_B: (12, 24), GOLD_A: (58, 18),
              GOLD_B: (58, 22), STONE_A: (35, 6), STONE_B: (31, 3),
              STONE_C: (35, 7)}
W_QUOTA = 175
G_QUOTA = 131
S_QUOTA = 101


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


_t0 = time.time()
_dead = False
send_count = 0
flags = {}
legs = {}            # role -> [tiles, idx]
KEYMAP = {}          # slot -> role
PENDING = []
stuck = {}           # role -> [tile, count]
stockwatch = {}      # role -> [tile, stock, count]
wedge = {}           # role -> re-approach tile
defer_switch = {}    # role -> True (回送态暂挂换线)
lastcmd = {}         # role -> tgt
build_watch = {}     # name -> [attempts, polls_since]


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
    global _dead
    if DRY:
        return None
    for _ in range(4):
        if _dead:
            return None
        try:
            subprocess.run(["sh", "-c", f"echo 'aistate' > {FIFO}"],
                           timeout=4.0)
        except Exception:
            _dead = True
            return None
        time.sleep(0.10)
        try:
            with open(AIS) as f:
                return json.load(f)
        except Exception:
            time.sleep(0.12)
    return None


def screen_state():
    """读 aA; !=6 = 弹窗冻结 (完工简报/死亡事件), key -6/-7 交替清。"""
    if DRY:
        return 6
    try:
        subprocess.run(["sh", "-c", f"echo 'state' > {FIFO}"], timeout=4.0)
    except Exception:
        return 6
    time.sleep(0.08)
    try:
        with open(STJ) as f:
            aa = json.load(f).get('aA', 6)
    except Exception:
        return 6
    if aa == 6:
        return 6
    key = -6 if flags.get('popup_n', 0) % 2 == 0 else -7
    flags['popup_n'] = flags.get('popup_n', 0) + 1
    PENDING.append(f'key {key}')
    log(f'  !! 弹窗 aA={aa} → key {key} (#{flags["popup_n"]})')
    return aa


def result():
    try:
        with open(LOG, errors='replace') as f:
            for ln in f:
                if '[result]' in ln:
                    return ln.strip()
    except Exception:
        pass
    return None


def retask(role, slot, tgt, tag):
    if lastcmd.get(role) == tuple(tgt):
        return
    lastcmd[role] = tuple(tgt)
    PENDING.append(f'retask {slot} {tgt[0]} {tgt[1]}')
    log(f'  {role}(s{slot}) retask -> {tgt} ({tag})')


def start_legs(role, slot, tiles, tag):
    legs[role] = [list(tiles), 0]
    lastcmd.pop(role, None)
    retask(role, slot, tiles[0], tag)


def ensure_build(name, tile, btype, placing, built):
    st = build_watch.get(name)
    if placing(tile) or built(tile):
        return
    if st is None or (st[1] >= 20 and st[0] < 3):
        n = 1 if st is None else st[0] + 1
        build_watch[name] = [n, 0]
        PENDING.append(f'build {tile[0]} {tile[1]} {btype}')
        log(f'拍 build type{btype} @{tile} (尝试 {n})')


def main():
    log(f'm2v2drv v3 start DRY={DRY} W={W}')
    inited = False
    polls = 0
    while True:
        if time.time() - _t0 > TIMEOUT:
            log('TIMEOUT 止损退出')
            return 1
        r = result()
        if r:
            log(f'RESULT: {r}')
            return 0
        st = aistate()
        if not st:
            if _dead:
                log('aistate 链路死, 退出')
                return 1
            time.sleep(POLL)
            continue
        polls += 1
        tick = st['tick']
        aa = screen_state()
        if aa != 6:
            fl = list(PENDING)
            PENDING.clear()
            send(fl)
            time.sleep(POLL)
            continue
        p0 = st['players'][0]
        w, g, s = p0['res']
        stock = w + g + s
        mine = [u for u in st['units'] if u['p'] == 0]
        vills = [u for u in mine if u['type'] < 2]
        blds = [b for b in st.get('buildingRecs', []) if b['p'] == 0]
        for b in build_watch.values():
            b[1] += 1
        if polls % 25 == 1:
            log(f't={tick} res={w}/{g}/{s} units={len(mine)} vills={len(vills)} '
                f'q={p0["trainQueue"]}')
        alive_slots = {u['slot'] for u in vills}
        for sl in list(KEYMAP):
            if sl not in alive_slots:
                log(f'  !! {KEYMAP[sl]} (slot{sl}) 消失')
                del KEYMAP[sl]
        roles = {}
        for u in vills:
            role = KEYMAP.get(u['slot'], f's{u["slot"]}')
            u['role'] = role
            roles[role] = u

        def built(tile):
            return any(tuple(b['tile']) == tuple(tile) and not b['uc']
                       for b in blds)

        def placing(tile):
            return any(tuple(b['tile']) == tuple(tile) for b in blds)

        def near(t, r=8):
            return any(d2(tuple(u['tile']), t) <= r for u in vills)

        # ---- 开局分工 (一次): v0/v1 木, v2 金(候车协议) ----
        if not flags.get('init') and roles.get('s0') and roles.get('s1') \
                and roles.get('s2'):
            s0, s1, s2 = roles['s0'], roles['s1'], roles['s2']
            for u, role in ((s0, 'v0'), (s1, 'v1'), (s2, 'v2')):
                KEYMAP[u['slot']] = role
                u['role'] = role
                roles[role] = u
            start_legs('v0', s0['slot'], [WOOD_A], 'init/木A')
            start_legs('v1', s1['slot'], [WOOD_B], 'init/木B')
            start_legs('v2', s2['slot'], GOLD_LEGS, 'init/金协议')
            flags['init'] = True
            flags['gold_n'] = 0
            flags['stone_n'] = 0

        # ---- 建造链 ----
        if flags.get('init'):
            if not flags.get('lumber_ok'):
                if w >= 15 and near(LUMBER_SPOT):
                    ensure_build('lumber', LUMBER_SPOT, 0, placing, built)
                if built(LUMBER_SPOT):
                    flags['lumber_ok'] = True
                    build_watch.pop('lumber', None)
                    log(f't={tick} 伐木场完工 (hdr9)')
            # 房屋门独立 (pop 余量给补员; 不依赖金线)
            if flags.get('lumber_ok') and not flags.get('house_ok') and w >= 25:
                ensure_build('house', HOUSE_SPOT, 11, placing, built)
                if built(HOUSE_SPOT):
                    flags['house_ok'] = True
                    build_watch.pop('house', None)
                    log(f't={tick} 房屋完工 (pop+5)')
            # 金仓: v2 候车 (56,16) 探格后拍; 仓成才许上矿
            if not flags.get('gold_camp_ok') and near(GOLD_CAMP_SPOT):
                if w >= 15:
                    ensure_build('gold_camp', GOLD_CAMP_SPOT, 1, placing, built)
                if built(GOLD_CAMP_SPOT):
                    flags['gold_camp_ok'] = True
                    build_watch.pop('gold_camp', None)
                    log(f't={tick} 金仓完工 (hdr10/11) — 金线上矿')
            # 石仓: 候车者 (33,3) 探格后拍
            if not flags.get('stone_camp_ok') and near(STONE_CAMP_SPOT):
                if w >= 15:
                    ensure_build('stone_camp', STONE_CAMP_SPOT, 1, placing,
                                 built)
                if built(STONE_CAMP_SPOT):
                    flags['stone_camp_ok'] = True
                    build_watch.pop('stone_camp', None)
                    log(f't={tick} 石仓完工 — 石线开闸')
            # 补员: 房成后训 2 个
            if flags.get('house_ok') and flags.get('recruits', 0) < 2 \
                    and w >= 10 and p0['trainQueue'] == 0 \
                    and len(mine) + p0['trainQueue'] < p0['popCap'] - 1:
                PENDING.append(f'train {HOUSE_TILE[0]} {HOUSE_TILE[1]} 1')
                flags['recruits'] = flags.get('recruits', 0) + 1
                log(f't={tick} 排补员 #{flags["recruits"]} (W={w})')

        # ---- 金协议: 候车者见金仓成 → 上矿格 ----
        if flags.get('gold_camp_ok'):
            for role, u in list(roles.items()):
                lg = legs.get(role)
                if not lg:
                    continue
                end = tuple(lg[0][-1])
                if end == GOLD_HOLD and u.get('action', 0) != 3 \
                        and d2(tuple(u['tile']), GOLD_HOLD) <= 2:
                    gn = flags.get('gold_n', 0)
                    tgt = GOLD_A if gn % 2 == 0 else GOLD_B
                    flags['gold_n'] = gn + 1
                    start_legs(role, u['slot'], [tgt], f'{role}上金{tgt}')

        # ---- 石协议: 候车者见石仓成 → 上矿格 ----
        if flags.get('stone_camp_ok'):
            for role, u in list(roles.items()):
                lg = legs.get(role)
                if not lg:
                    continue
                end = tuple(lg[0][-1])
                if end == STONE_HOLDBY and u.get('action', 0) != 3 \
                        and d2(tuple(u['tile']), STONE_HOLDBY) <= 2:
                    sn = flags.get('stone_n', 0)
                    tgt = (STONE_A, STONE_B, STONE_C)[min(sn, 2)]
                    flags['stone_n'] = sn + 1
                    start_legs(role, u['slot'], [tgt], f'{role}上石{tgt}')

        # ---- 枯竭防护轮换 (格容量 31 载; 单人采 34 载必耗尽 → 引擎改派邻近
        #      格进敌圈, 彩排2/4 死法) ----
        if polls % 10 == 0:
            if not flags.get('wood_rot') and w >= 110:
                v0 = roles.get('v0')
                if v0 is not None and legs.get('v0') \
                        and tuple(legs['v0'][0][-1]) == WOOD_A \
                        and v0.get('action', 0) != 3:
                    flags['wood_rot'] = True
                    start_legs('v0', v0['slot'], [WOOD_B], f'木轮换A→B (w={w})')
            if not flags.get('gold_rot') and g >= 78:
                for role, u in roles.items():
                    lg = legs.get(role)
                    if lg and tuple(lg[0][-1]) == GOLD_A \
                            and u.get('action', 0) != 3:
                        flags['gold_rot'] = True
                        start_legs(role, u['slot'], [GOLD_B],
                                   f'金轮换A→B (g={g})')
                        break
            if not flags.get('stone_rot') and s >= 78:
                for role, u in roles.items():
                    lg = legs.get(role)
                    if lg and tuple(lg[0][-1]) == STONE_A \
                            and u.get('action', 0) != 3:
                        flags['stone_rot'] = True
                        start_legs(role, u['slot'], [STONE_B],
                                   f'石轮换A→B (s={s})')
                        break

        # ---- 首石工: 金仓成 OR 木满 OR v2 缺 —— 派 v1 走石协议候车 ----
        first_stone_ready = flags.get('gold_camp_ok') or w >= 100 \
            or ('v2' not in roles and flags.get('init'))
        if first_stone_ready and not flags.get('first_stone'):
            v1 = roles.get('v1')
            if v1 is not None and v1.get('action', 0) != 3 and s < S_QUOTA:
                flags['first_stone'] = True
                start_legs('v1', v1['slot'], STONE_LEGS, '首石工候车')

        # ---- 缺口驱动换线 (回送态暂挂; 石协议自身安全, 候车即可) ----
        if flags.get('first_stone') and polls % 10 == 0:
            for role, u in roles.items():
                lg = legs.get(role)
                if not lg:
                    continue
                end = tuple(lg[0][-1])
                if end == STONE_HOLDBY or end in (STONE_A, STONE_B, STONE_C):
                    continue
                if defer_switch.get(role):
                    if u.get('action', 0) == 3:
                        continue
                    defer_switch.pop(role)
                on_wood = end in (WOOD_A, WOOD_B) and w >= W_QUOTA and s < S_QUOTA
                on_gold = end in (GOLD_A, GOLD_B) and g >= G_QUOTA and s < S_QUOTA
                if on_wood or on_gold:
                    if u.get('action', 0) == 3:
                        defer_switch[role] = True
                        continue
                    log(f'  {role} 换线→石协议 (res {w}/{g}/{s})')
                    start_legs(role, u['slot'], STONE_LEGS, f'{role}换线石')

        # ---- 新村民认领 → 缺口线 ----
        for u in vills:
            sl = u['slot']
            if sl in KEYMAP:
                continue
            role = f'r{sl}'
            KEYMAP[sl] = role
            u['role'] = role
            roles[role] = u
            if flags.get('stone_camp_ok') and s < S_QUOTA:
                sn = flags.get('stone_n', 0)
                flags['stone_n'] = sn + 1
                start_legs(role, sl, [(STONE_A, STONE_B, STONE_C)[min(sn, 2)]],
                           f'{role}/补员上石')
            elif flags.get('gold_camp_ok') and g < G_QUOTA:
                gn = flags.get('gold_n', 0)
                flags['gold_n'] = gn + 1
                start_legs(role, sl, [GOLD_A if gn % 2 == 0 else GOLD_B],
                           f'{role}/补员上金')
            elif g < G_QUOTA:
                start_legs(role, sl, GOLD_LEGS, f'{role}/补员金协议')
            elif w < W_QUOTA:
                start_legs(role, sl, [WOOD_A], f'{role}/补员上木')
            else:
                start_legs(role, sl, STONE_LEGS, f'{role}/补员石协议')
            log(f'  {role} 认领 slot{sl} @ {tuple(u["tile"])}')

        # ---- 腿推进 + 看门狗 + 库存门楔死检测 ----
        for role, u in roles.items():
            lg = legs.get(role)
            if not lg:
                continue
            tile = tuple(u['tile'])
            action = u.get('action', 0)
            if role in wedge:
                rt = wedge[role]
                if d2(tile, rt) <= 1:
                    cur2 = lg[0][lg[1]] if lg[1] < len(lg[0]) else None
                    wedge.pop(role)
                    stockwatch.pop(role, None)
                    stuck.pop(role, None)
                    if cur2 is not None:
                        lastcmd.pop(role, None)
                        retask(role, u['slot'], cur2, f'{role}重接近回')
                continue
            if lg[1] >= len(lg[0]):
                continue
            cur = lg[0][lg[1]]
            last_leg = lg[1] == len(lg[0]) - 1
            if tile == tuple(cur) or (not last_leg and d2(tile, cur) <= 1):
                lg[1] += 1
                stuck.pop(role, None)
                stockwatch.pop(role, None)
                if lg[1] < len(lg[0]):
                    lastcmd.pop(role, None)
                    retask(role, u['slot'], lg[0][lg[1]], f'{role} leg{lg[1]}')
                continue
            sw = stockwatch.get(role)
            if action in (0, 2) and sw and sw[0] == tile and sw[1] == stock:
                sw[2] += 1
            else:
                stockwatch[role] = [tile, stock, 0]
                sw = stockwatch[role]
            if sw[2] >= 20 and action != 3:
                rt = REAPPROACH.get(tuple(cur))
                sw[2] = 0
                if rt is not None and d2(tile, rt) > 1:
                    log(f'  !! {role} 楔死 @{tile} (线 {cur}, stock {stock}) '
                        f'→ 重接近 {rt}')
                    wedge[role] = rt
                    lastcmd.pop(role, None)
                    retask(role, u['slot'], rt, f'{role}楔重接近')
                    continue
            sk = stuck.setdefault(role, [tile, 0])
            if tile != sk[0]:
                stuck[role] = [tile, 0]
                continue
            sk[1] += 1
            if sk[1] >= 10 and action not in (2, 3):
                if d2(tile, cur) <= 2:
                    lg[1] += 1
                    stuck.pop(role, None)
                    if lg[1] < len(lg[0]):
                        lastcmd.pop(role, None)
                        retask(role, u['slot'], lg[0][lg[1]], f'{role}卡重发')
                else:
                    lastcmd.pop(role, None)
                    retask(role, u['slot'], cur, f'{role}看门狗')
                stuck[role] = [tile, 0]

        if w > 100 and g > 100 and s > 100 and not flags.get('winwait'):
            flags['winwait'] = True
            log(f't={tick} 三桶过线 {w}/{g}/{s} — 等 [result]')
        fl = list(PENDING)
        PENDING.clear()
        send(fl)
        time.sleep(POLL)


if __name__ == '__main__':
    sys.exit(main())
