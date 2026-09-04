#!/usr/bin/env python3
"""m4j 制胜驱动 v6.5-j（第 10 轮）= v6.4-i + 木银行（波期木入账，残墙单点总攻）：
  读码考证（BUGS-m4j 附录 A，c.java 行号）：
  - hdr9 宏只写 p0 hdr[9]（木交存指针）；nearestDropOff(8742) 木类(0x10)只在
    hdr[9] 与 hdr[8](TC) 间取近，金/石走 hdr[10]/[11] —— 无金/石副作用。
  - 但入账发生在 onUnitArrived(7367) case 256：到站格必须是**己方建筑格**
    （owner==n 且 word7 低 nibble==3）——hdr9 指空地=不入账（m1 故意如此）。
    ⇒ 「hdr9 伪造空地交存点」在 m4 不成立（会满载停在野地）。
  - 完工 switch a() 7544-7548：case 0（伐木场）自动写 hdr[9]=tile；House(11)
    只加 pop 不写 hdr[9] ⇒ 「H#1 木仓房」也不成立。真木帽（type0）是唯一
    引擎原生解：放格即 hdr[9]+建筑格生效 → 满载回送进木帽格入账 →
    slot[2]=slot[5] 自动回树续采（全程零驱动干预，改派吞货无从发生）。
  - t0 造价 15W/黑暗可建（build 门=techFlags[10]!=0；m4 只锁 [14]Univ/
    [15]Mill/[16]BS —— 探针 boot 实证）。
  本轮驱动改动（其余 v6.4-i 逻辑一行不动）：
  W1 开局 slot1 先探 (29,62)（3×3 照亮 (30,62) 且不坐建造格；d2_line≈166
     离线），到位才算（探雾定律）；slot0 扫堂照旧。
  W2 W>=15 且已探明 → build t0 @(30,62)（先于 Barracks：15W<20W 顺位自然）。
  W3 木帽在位（bank_any=放格即算，hdr[9] 放格即生效）时：西木岗（WOOD_SAFE）
     豁免宵禁/几何避险（m=0 与 m>=1 两支都豁）——树→帽 4-6 manh 全程离线，
     FSM 原生回送，波期照常入账。保底：敌贴身 5 格（d2e<25）仍避险。
  W4 西侧避险格换 (29,62)（PARK_WEST 被木帽占格后不可驻）。
  W5 mocksim 配套：t0 完工 milestone + M4D_WAVE0=<tick>/M4D_WAVEGAP=<t>
     最早抽签压力注入 + 头部「已知分叉点清单」。

v6.4-i（第 9 轮）= v6.4-h + BUGS-m4h 交棒残局 0-3：
  0 boot.sh pkill 旧驱动/僵尸 java + 驱动 ar 倒退守卫（幽灵驱动跨局发令）；
    僵尸局自判（v=0 且 pop=0 持续 150s → VOID 弃局，不再烧满 2400s）。
  1a 民兵风筝：敌众我寡 rally 退 TC 东南 KITE(46,60) 拉扯（敌贴 5 格退 KITE2），
     不做对冲硬拼（boot3 实锤 m 硬拼全灭=经济随崩）；TC 白打可承受。
  1b mocksim 删 TC 防御虚构分支（TC 无攻击力，r45 定案）。
  2a 分工按槽锚定：jobs 只清死槽 + 对新增 slot 追加岗位，老 slot 保原 job
     （军事死亡→槽位压缩→枚举序重建错位，满载金矿工被改派木工，boot3 实锤）。
  2b 矿仓 CANDS 优先 (42,40)/(40,42)（半程交金+离波线更远），探雾民兵改贴 (41,41)。
  3 宵禁出坑补丁：敌 ≤2 且全被钉在 TC 10 格内也放行复工（孤敌围城不冻结经济）。

v6.4-h（第 8 轮）= v6.4-e + 残局 N1-N4（r42 定稿，证据 BUGS-m4e/f）。

本轮新增（只做微调，架构不动）：
  N1 木位离廊：首次敌波出生（或 p1mil 存在）即把走廊木位 (32,52)/(33,51) 全切
     WOOD_SAFE——旧逻辑等矿仓落成才切，boot3 村民死在切换前的走廊上。
  N2 House 紧急阀：v<=2 且 ar>1400 时 House 无视 pop_room/camp 门（W>=5 即建）
     ——boot3 v=1 死锁（House 门与 camp 门互锁，全程 H=0=村民断产）。
  N3 B_CANDS 重排：[(45,59),(46,58),(42,58)]——fallback 离决战位远点；
     HOUSE_CANDS 同理把兵营菱形能照到的格提前。
  N4 雾格兜底：探索模型=每 tick 全体单位 3×3 + 完工建筑轮询曼哈顿菱形 r3
     （c.java:5876 p() / 5978 revealFogAroundUnit / 5904 void_a；build 判定
     c.java:1479 mapTiles<0）。TC(43,57) 菱形 r3 照不到 (45,59)(46,58)（d=4）
     ——boot FAIL 非"异常"，是开局从无单位扫过该处（"boot1/2 同格成功"实为
     mocksim 无雾模型的 dry 假阳性）。兜底=开局 slot0 先扫堂 (44,58)（3×3 覆盖
     (45,59)），扫到前重建/闲置重派不动它；民兵探雾改"到位才算"（旧"下令即算"
     让 boot2 矿仓连吃 3 个雾 FAIL）。mocksim4f 已补同款雾模型防 dry/boot 分叉。
历史：v6.4-e（矿仓定律/偶和格断言/静默窗/per-vil 威胁门/微调 a-d），
  v6.3 及更早见 BUGS-m4d/e。DRY 模式（M4D_DRY=1）注入 SIM（mocksim4f.py）。

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
import shutil
import subprocess
import sys
import time

DRY = os.environ.get('M4D_DRY') == '1'
WORK = os.environ.get('M4D_WORK', '/tmp/aoe-camp/m4j')
FIFO = WORK + '/fifo'
LOG = WORK + '/play.log'
AISTATE = WORK + '/fifo.aistate.json'
STATEJ = WORK + '/fifo.json'

TC = (43, 57)
FRONT = (44, 58)            # TC 庭院决战位（偶和 102）
KEEPER_SPOT = (44, 58)
SWEEP = (44, 58)            # 开局扫堂位：3×3 覆盖 (45,59) 兵营位（N4）
RAID = (15, 40)             # 敌矿工 blob 东缘（base 区外）
SAFE = [(52, 62), (52, 63), (50, 63)]  # 波时避险点 TC 东南（boot2：旧点位距线
# <8 格太贴；52,62 起 d2_line>=85 且离 TC 战团 >9）
KITE = (46, 60)             # 1a 风筝位：TC 东南、波 approach 线外（敌众我寡退此拉扯）
KITE2 = (44, 63)            # 风筝二段（敌贴 KITE 5 格内换位；离 SAFE/PARK 均 >8 格）

WOOD_NEAR = (32, 52)
WOOD_NEAR2 = (33, 51)
WOOD_SAFE = [(28, 58), (27, 59), (28, 57), (29, 56)]   # 前两位避开波走廊 10 格圈（d2e<100 误避险）
WOOD_SIDE = WOOD_SAFE[:3]   # 西侧避险（boot2：走廊木工逃 WOOD_SAFE，不再横穿波线）
# r44 boot3 新增：宵禁公园格（西侧）。旧"逃向"WOOD_SIDE=自家树格=无效自逃（村民
# 波期继续采集→满载上路撞波）。公园格要求：非资源格 + d2_line>81 + 离树 ≤6 格。
PARK_WEST = (30, 62)
# v6.5-j 木银行：真伐木场(type0)放格即写 hdr[9]（c.java:7546）+ 自身成建筑格
# （onUnitArrived case256 入账）——PARK_WEST 本格升级为木帽格。
WOOD_BANK = (30, 62)        # t0 建造格（偶和 92 ✓；d2_line≈164 离线；距西树 4-6 manh）
BANK_SCOUT = (29, 62)       # 探明站位：3×3 照亮 (30,62) 且不坐建造格（d2_line≈166）
WEST_FLEE = (29, 62)        # 木帽占 PARK_WEST 后的西侧避险格（探明即验证可驻）
WEST_WOOD = frozenset(WOOD_SAFE)   # 木帽豁免宵禁的西木岗位集合
WOOD_ALL = set(WOOD_SAFE) | {WOOD_NEAR, WOOD_NEAR2}
STONE = [(39, 40), (41, 40), (41, 38), (38, 40), (40, 40)]
GOLD = [(35, 36), (36, 36), (37, 36), (35, 35), (36, 35), (34, 36)]

# 分工计划（按村民槽位序；金工从槽 2-3 就位——boot1 教训：民兵死光后金=0=永远
# 造不出兵。矿仓只是缩短趟程的经济优化。北岗部署只走静默窗——boot2 教训。）
# boot1 尸检（真实收入 ~2-4x 慢于 sim）：3木3金2石1木，金工提前到 3 人
PLAN = [WOOD_SAFE[0], WOOD_SAFE[1], GOLD[0], GOLD[1], GOLD[2],
        STONE[0], STONE[1], WOOD_SAFE[2]]

# 建筑候选——全部偶和格（硬性，见头部说明 2）
# N3/N4：首选 (45,59) 需开局扫堂（SWEEP 3×3 覆盖）；(46,58) 扫堂照不到=免费
# FAIL 一跳；(42,58) TC 菱形 r3 恒可建但贴决战位，仅作末位兜底。
B_CANDS = [(45, 59), (46, 58), (42, 58)]                 # Barracks 20木10石
# House 首选改兵营菱形照得到的 (44,60)/(46,60)（B 在 (45,59) 时 d=2 已探明）
HOUSE_CANDS = [(44, 60), (46, 60), (42, 60), (40, 60), (42, 62), (44, 62), (40, 62)]
CAMP_CANDS = [(42, 40), (40, 42), (36, 40), (36, 38), (38, 42)]   # Mining Camp 15木
# 2b（r45 残局）：(42,40)/(40,42) 优先——半程交金（4000-4400 静默窗不够跑全程）
# 且离波线更远（d2_line: (42,40)=150、(40,42)=89 均 >81；(36,40)=69 在暴露带）。
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
# 东翼绕行走廊（m4f boot1/dry 定案）：波线=敌 base(8,27)→TC(43,57) 的直行线，
# 各纬度 x≈26-35（y=52→x≈35, y=40→x≈27）——BFS 直连恰好穿线（boot1 尸位于
# (34-37,47-53) 全在线上）。矿工出门先到东翼锚点再切矿，第二段腿全程距线 >9.8 格
# → 出门不再依赖静默窗，波爆发期照走。（锚点选 (49,48)：d2_line=116>81，
# dry4 教训：(47,50) 距线 63<81 会被波出生反复吓跑。）
NORTH_WAY = (49, 48)

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
        _copy_playlog()          # r42/m4f 教训：所有退出路径都必须留 play.log 副本
        sys.exit(2)
    time.sleep(wait)


def tail(n=200):
    if DRY:
        return []    # DRY 拒读 play.log（上轮教训：陈旧日志假信号）
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


WAVE_LINE = ((8, 27), (43, 57))   # 敌波走廊线：base→TC（boot1 四尸位点验证）


def d2_line(p, a=WAVE_LINE[0], b=WAVE_LINE[1]):
    """点 p 到波线段 a→b 的距离平方（暴露判据：距线 <9 格=暴露）"""
    vx, vy = b[0] - a[0], b[1] - a[1]
    wx, wy = p[0] - a[0], p[1] - a[1]
    t = (wx * vx + wy * vy) / float(vx * vx + vy * vy)
    t = max(0.0, min(1.0, t))
    dx = p[0] - (a[0] + t * vx)
    dy = p[1] - (a[1] + t * vy)
    return dx * dx + dy * dy


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


def _copy_playlog():
    bootn = os.environ.get('M4F_BOOTN')
    if not DRY and bootn and os.path.exists(LOG):
        try:
            shutil.copy(LOG, f'{WORK}/play-boot{bootn}.log')
            print(f'play.log 副本 → play-boot{bootn}.log', flush=True)
        except Exception as e:
            print(f'!! play.log 副本失败: {e}', flush=True)


def main():
    # r44 教训自检：显式横幅（dry 忘带 M4D_DRY=1 时走 FIFO 还读陈旧 play.log 报假局）
    print(f'MODE={"DRY(mocksim)" if DRY else "FIFO(live)"} WORK={WORK}', flush=True)
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
    # 开局（N4 扫堂）：slot0 先去 (44,58)（TC 菱形内已探明，3×3 覆盖 (45,59)
    # 兵营位），到位后由闲置重派自然转岗；slot1 直接近木。扫到前所有重派绕开 slot0。
    sweep_slot = vslots[0] if vslots else None
    sweeping = bool(vslots)    # False=无村民可扫（或已扫完）
    cmd(f"retask {vslots[0]} {SWEEP[0]} {SWEEP[1]}", 0.3)
    bank_slot = vslots[1] if len(vslots) > 1 else None
    if bank_slot is not None:
        # W1：slot1 先探木帽格（到位才算——探雾定律），到位后闲置重派自然转岗
        cmd(f"retask {bank_slot} {BANK_SCOUT[0]} {BANK_SCOUT[1]}", 0.3)
    print(f'retask 扫堂 slot{sweep_slot} → {SWEEP}（N4）；slot1 → {BANK_SCOUT} '
          f'探木帽位（W1：先探后建 t0@{WOOD_BANK}）', flush=True)

    last_nv = len(vslots)
    idle_poll = {}
    idle_n = {}
    prev_p1m = 0
    last_b = False          # Barracks 完成沿（触发槽1 石矿出岗重派）
    last_camp = False       # 矿仓出现沿（触发槽0/1 分工切换重派）
    scouted = False         # 矿区已探雾（民兵到位或矿仓已建成）
    scout_sent = False      # 探雾民兵已派出（到位判定之前为 True）
    scout_slot = None
    bank_scouted = False    # W1：木帽格已探明（slot1 到位才算）
    bank_cd = 0.0
    bank_dead_logged = False
    sweeping = True         # N4 扫堂期：slot0 未贴住 (45,59) 前重派绕开它
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
    last_econ_ar = 0
    prev_tc_hp = 255
    tc_alarm = False
    last_wave_birth = -10**9
    last_enemy_seen = 0      # 敌军最后一次存在的 ar（raid 静默窗 = ar-此值）
    last_wave_dead = -10**9  # 波被歼时刻（微调 c：出岗门槛放宽用）
    prev_had_p1m = False
    prev_ar = None          # 残局 0：ar 倒退守卫（幽灵驱动检测）
    zomb0 = None            # 僵尸局计时起点（v=0 且 pop=0）

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
        if prev_ar is not None and ar < prev_ar:
            print(f'!! ar 倒退 {prev_ar}→{ar}：检测到幽灵驱动跨局/异局，立即退出',
                  flush=True)
            _copy_playlog()
            sys.exit(4)
        prev_ar = ar
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
        if not vils and popu == 0:
            # 僵尸局自判（r45：boot2 白烧 25 分钟）——全员死光且 TC 立=永不判负
            if zomb0 is None:
                zomb0 = now
            elif now - zomb0 > (400 if DRY else 150):
                print(f'VOID: 僵尸局弃局（v=0 且 pop=0 已 {now - zomb0:.0f}s，'
                      f'ar={ar}）', flush=True)
                break
        else:
            zomb0 = None

        gate_armed = p1army >= 30 and myarmy < p1army * 1.25

        camp_up = any(b['type'] == 1 and not b['uc'] for b in a['buildingRecs']
                      if b['p'] == 0)
        camp_any = camp_up or any(b['type'] == 1 and b['uc']
                                  for b in a['buildingRecs'] if b['p'] == 0)
        barracks_done = any(b['type'] == 10 and not b['uc']
                            for b in a['buildingRecs'] if b['p'] == 0)
        # W2/W3：木帽「放格即算在位」——a() 放格瞬间 hdr[9]=tile 且 mapTiles
        # 成己方建筑格（c.java:7539/7546），入账链路即刻生效，不必等完工。
        bank_any = any(b['type'] == 0 for b in a['buildingRecs'] if b['p'] == 0)
        west_flee = WEST_FLEE if bank_any else PARK_WEST

        # W1 探明判定（探雾定律：到位才算；单点 Chebyshev≤1）
        if bank_slot is not None and not bank_scouted:
            bu = next((u for u in vils if u['slot'] == bank_slot), None)
            if bu is None:
                if not bank_dead_logged:
                    print(f'ar={ar} !! 探帽村民 slot{bank_slot} 消失，'
                          f'木银行本轮放弃（其余 v6.4-i 逻辑照旧）', flush=True)
                    bank_dead_logged = True
                bank_slot = None
            elif max(abs(bu['tile'][0] - BANK_SCOUT[0]),
                     abs(bu['tile'][1] - BANK_SCOUT[1])) <= 1:
                bank_scouted = True
                print(f'ar={ar} 木帽位已探明（slot{bank_slot} 贴住 {BANK_SCOUT}）',
                      flush=True)

        # 槽 0/1 动态（boot3 教训：矿仓是"到岗后不再穿走廊"的承重结构，木料
        # 优先给它——矿仓起before两村民分开）：双木工抢到矿仓款（W≥15），矿仓
        # 一交款/落成，槽1 才出岗石矿、槽0 转安全木。
        # N1：首波出生（或敌军在场上）后，走廊木位 (32,52)/(33,51) 永久退役——
        # 全切 WOOD_SAFE（旧逻辑等矿仓落成才切，boot3 村民死在切换前）。
        # N1 硬化（boot2）：WOOD_NEAR/WOOD_NEAR2 彻底退役——波 2115 早到（boot 间
        # 方差 ~300t）把走廊木工双双打死在逃亡路上。WOOD_SAFE 到 House(44,60) 交存
        # 18 manh，与走廊位到 TC 相当——零收入代价换零暴露。
        seq = list(PLAN)
        if not camp_any:
            seq[0] = WOOD_SAFE[0]
            seq[1] = WOOD_SAFE[1]
        elif not barracks_done or p0['age'] < 1:
            # 微调 b 补丁：封建前无石需求（Barracks 10S 开局自带），槽1 留木
            seq[1] = WOOD_SAFE[1]

        # 民兵探雾（N4 兜底）："下令即算已探"是 boot2 矿仓三连雾 FAIL 的根因——
        # revealFogAroundUnit 只清民兵当前格 3×3，没到位=没探到。改**到位才算**：
        # 民兵 Chebyshev≤1 贴住 (41,41)（3×3 同时罩住 2b 新首选 (42,40)/(40,42)）
        # 才放行矿仓 build。dry1 实锤：旧"路过 (40,42) 邻格即算探明"会让 scouted
        # 提前触发 → 探雾豁免失效 → stray-recall 当轮把民兵拉回 FRONT → 候选格
        # 从未探明 → 矿仓 FAIL 死循环 → G=0 → m 恒 2 → all-in 永动 → 僵局。
        # m4f boot1 教训：stray-recall（离 FRONT>9 即 rally 回庭院）把探雾民兵
        # 反复拉回，永远到不了 (37,41)，严格判定全程不触发 → 矿仓 C=0 全局 →
        # G≈3 → 民兵断产 → LOSS。修复：①探雾民兵豁免 stray-recall；
        # ②静默窗里发现没人贴住矿区 → 重置 scout_sent 重派。
        if not scout_sent and len(mymil) >= 1 and not p1mil and ar >= 1500 \
                and not camp_any:
            far = max(mymil, key=lambda u: dist(u['tile'], TC))
            cmd(f"retask {far['slot']} 41 41", 0.22)
            scout_slot = far['slot']
            scout_sent = True
            print(f"ar={ar} 民兵探雾 slot{scout_slot} → (41,41)", flush=True)
        if scout_sent and not scouted:
            near_mine = any(max(abs(u['tile'][0] - 41), abs(u['tile'][1] - 41)) <= 3
                            for u in mymil)
            if not mymil or (not near_mine and not p1mil
                             and ar - last_enemy_seen > 150):
                scout_sent = False    # 探雾民兵阵亡/被抽走：重派（波后 150t 即重推）
        if not scouted and mymil:
            if any(max(abs(u['tile'][0] - 41), abs(u['tile'][1] - 41)) <= 1
                   for u in mymil):
                scouted = True
                print(f"ar={ar} 矿区已探明（民兵贴住矿仓位）", flush=True)

        # 北岗部署窗（东翼绕行后出门本身安全；唯一残留风险=TC 迎宾段：
        # 刚出TC 时若波正在抵达（波尾 x40-43,y54-57），距 3-5 格会 aggro——
        # 放行条件=无敌军 或 波已全部抵达 TC 附近（在打庭院=不在走廊上）。
        # 静默 450t 保留为第三条或支（兜底）。）
        def north_ok(u, j):
            if j[1] > MINE_YMAX:
                return True
            if u['tile'][1] <= MINE_YMAX:
                return True
            if ar < 1400 and last_wave_birth < 0:
                return True
            return (not p1mil
                    or all(dist(m['tile'], TC) < 200 for m in p1mil)
                    or ar - last_enemy_seen > 450)

        # 东翼分程：北向岗位且人还在 TC 一带（y>50）→ 先发锚点；到锚点后
        # 闲置重派自然推进到最终岗位（从 (47,50) 直连矿位全程离波线 ≥10 格）
        def stage_target(u, j):
            if j[1] <= MINE_YMAX and u['tile'][1] > 50:
                return NORTH_WAY
            return j

        # boot2 尸检：金工 3 人同时走北撞上 wave2 团灭于 (35,50-53)——
        # 改错峰制：同一时刻只允许 1 人在北向路上（到岗 y<=42 后下一个才出发）
        def north_walkers():
            # 并发上限判据（<2 放行）：只数"在途"者——卡在锚点待推进的不算，
            # 否则两人互堵锚点死锁（dry4 实锤）
            n = 0
            for v_ in vils:
                if v_['slot'] in fleeing:
                    continue    # 避险者不在路上（dry12 尸检：意图占位困死金工）
                j_ = jobs.get(v_['slot'])
                if j_ and j_[1] <= MINE_YMAX and v_['tile'][1] > MINE_YMAX \
                        and tuple(v_['tile']) != NORTH_WAY:
                    n += 1
            return n < 2         # 东翼双车道：放行 2 个并发（m4f：错峰 1 太堵）

        def can_go_north(u, j):
            if j[1] > MINE_YMAX or u['tile'][1] <= MINE_YMAX:
                return True
            return north_ok(u, j) and north_walkers()

        # ---- N4 扫堂判定：slot0 贴住 (45,59)（Chebyshev≤1 = 3×3 覆盖兵营位）
        # 即扫完；扫堂村民消失（死亡）也放行（fallback (42,58) 恒可建）----
        if sweeping and vils:
            su = next((u for u in vils if u['slot'] == sweep_slot), None)
            if su is None:
                sweeping = False
            elif max(abs(su['tile'][0] - B_CANDS[0][0]),
                     abs(su['tile'][1] - B_CANDS[0][1])) <= 1:
                sweeping = False
                print(f"ar={ar} 扫堂完成：({B_CANDS[0][0]},{B_CANDS[0][1]}) 已探明",
                      flush=True)

        # ---- 分工维护（残局 2a 按槽锚定）：只清死槽 + 对新增 slot 追加岗位，
        # 老 slot 永不改派不重发——满载（回送态）retask 不可靠且改派吞货，且军事
        # 死亡压缩枚举序会让整表错位（boot3 实锤：满载金矿工被改派木工，金蒸发）----
        if len(vils) != last_nv or not jobs or barracks_done != last_b \
                or camp_any != last_camp \
                or any(u['slot'] not in jobs for u in vils):
            live = {u['slot'] for u in vils}
            for sl in [s for s in jobs if s not in live]:
                del jobs[sl]       # 死亡槽位清理（在岗者 job 原样保留）
            have = {}
            for j in jobs.values():
                k = tuple(j)
                have[k] = have.get(k, 0) + 1
            want = {}
            for j in seq:
                want[tuple(j)] = want.get(tuple(j), 0) + 1
            fresh = [u for u in vils if u['slot'] not in jobs]
            for u in fresh:
                # 按 seq 序补最大缺口角色（与列表枚举序无关，死亡压缩不再错位）
                pick = next((j for j in seq if have.get(tuple(j), 0)
                             < want[tuple(j)]), seq[len(jobs) % len(seq)])
                jobs[u['slot']] = pick
                have[tuple(pick)] = have.get(tuple(pick), 0) + 1
            last_b = barracks_done
            last_camp = camp_any
            last_nv = len(vils)
            if fresh:
                print(f'ar={ar} 分工追加(按槽锚定): '
                      f'{ {u["slot"]: tuple(jobs[u["slot"]]) for u in fresh} }',
                      flush=True)
            for u in vils:
                if u['slot'] in fleeing or (u['slot'] == sweep_slot and sweeping):
                    continue
                if u['slot'] == bank_slot and not bank_scouted:
                    continue       # W1：探帽途中不夺令（扫堂同款保护）
                if u['slot'] not in fresh:
                    continue       # 老 slot 不重发（防吞货/防错位）
                j = jobs.get(u['slot'])
                if j and can_go_north(u, j):
                    jt = stage_target(u, j)
                    cmd(f"retask {u['slot']} {jt[0]} {jt[1]}", 0.22)

        # ---- per-vil 威胁规则（boot1 教训：全局复工门被 TC 营敌卡死=经济冻结）----
        # 入坑：敌距自身 <10 格或身处路中段且处于波后 450t 窗 → 避险 SAFE；
        # 出坑：敌距**岗位**全部 >12 格且部署窗允许 → 复工。
        for u in vils:
            sl = u['slot']
            j = jobs.get(sl)
            if j is None:
                continue
            if sl in fleeing:
                # 出坑分岗（r44 boot3 dry5 教训）：木工=敌清+静默 300t（波整个走廊
                # 行进期都不许送货）；矿工=旧几何判据（矿区离波线 >9 格，波不涉北岗，
                # dry5 宵禁曾把矿工反复拽停 → G 全程 0 → m=2 裸奔）。
                if j[1] <= MINE_YMAX:
                    clear = all(dist(m['tile'], j) > 144 for m in p1mil) if p1mil \
                        else True
                elif mymil:
                    # 残局 3：孤敌围城不冻结经济——敌 ≤2 且全被钉在 TC 10 格内也放行
                    #（注意 all() 对空 p1mil 恒真，必须 bool() 短路保 300t 静默）
                    clear = ((not p1mil) and ar - last_enemy_seen > 300) \
                        or (bool(p1mil) and len(p1mil) <= 2
                            and all(dist(m['tile'], TC) < 100 for m in p1mil))
                else:
                    # m=0 早期紧急：几何出坑（与 m=0 入坑对称，boot1 尸检）
                    clear = all(dist(m['tile'], j) > 49 for m in p1mil) \
                        if p1mil else True
                if clear and can_go_north(u, j):
                    fleeing.discard(sl)
                    jt = stage_target(u, j)
                    cmd(f"retask {sl} {jt[0]} {jt[1]}", 0.22)
                    idle_n[f'{sl}'] = 0
                continue
            if p1mil or ar - last_wave_birth < 450:
                if j[1] > MINE_YMAX:
                    # W3 木银行豁免：西木岗+木帽在位 → 树→帽 4-6 manh 全程离线
                    # （d2_line≥95>81），FSM 原生回送入账零驱动干预——宵禁/几何
                    # 避险在这里全是负资产（波期收入停摆=残墙本体）。保底：敌真
                    # 贴身 5 格（aggro 圈内）才避险，其余波期照常采。
                    if bank_any and j in WEST_WOOD:
                        if p1mil and min(dist(m['tile'], u['tile'])
                                         for m in p1mil) < 25:
                            side = (west_flee,) if u['tile'][0] < 38 else SAFE
                            s = side[sl % len(side)]
                            fleeing.add(sl)
                            cmd(f"retask {sl} {s[0]} {s[1]}", 0.22)
                            idle_n[f'{sl}'] = 0
                    elif mymil:
                        # 南岗宵禁（r44 boot1/2 尸检定案，有兵时维持）：波存活期或
                        # 波后 450t，木工全员离岗。旧几何判据两个洞：①树上采集者
                        # 不在暴露带→继续采集→满载上路撞波（满载回送态 retask 失灵
                        # 救不回，r35+本轮读码：word7=3 时 slot[2] 由 FSM 接管）；
                        # ②西侧"逃向"WOOD_SIDE=自家树格=no-op 自逃。宵禁把"波期
                        # 满载"概率归零，代价=波期木收入停摆。
                        side = (west_flee,) if u['tile'][0] < 38 else SAFE
                        s = side[sl % len(side)]
                        fleeing.add(sl)
                        cmd(f"retask {sl} {s[0]} {s[1]}", 0.22)
                        idle_n[f'{sl}'] = 0
                    else:
                        # m=0 早期紧急豁免（boot1 尸检新增）：最早抽签（波 1@1364
                        # 本轮实测）+连波 → 宵禁把 W 冻在 0 → 民兵永不出世 → TC 被
                        # 磨平。TC 无攻击力前提下 m=0 时宵禁收益为负——改用几何判据：
                        # 敌真近身（7 格）或身处波线上（波存活全程——boot2 尸检：
                        # 450t 窗太短，满载村民在 440t 时死于线尾 (39,54)）才避险；
                        # 树位离线 >9 格照常砍木，让民兵能在波抵达前出膛。
                        danger = False
                        if p1mil:
                            d2e = min(dist(m['tile'], u['tile']) for m in p1mil)
                            danger = d2e < 49 or d2_line(u['tile']) < 81
                        if danger:
                            side = (west_flee,) if u['tile'][0] < 38 else SAFE
                            s = side[sl % len(side)]
                            fleeing.add(sl)
                            cmd(f"retask {sl} {s[0]} {s[1]}", 0.22)
                            idle_n[f'{sl}'] = 0
                else:
                    # 北岗矿工：几何判据（波不涉矿区，勿拽停金/石收入）。
                    # d2e 阈 49（7 格）：敌 aggro 圈 4-5 格、围城敌不追击——
                    # 旧值 100（10 格）让走廊收尾段（必经 TC 10 格内）反复避险
                    # （dry2：矿工在 (38-42,50-54) 振荡永远过不去）。
                    danger = False
                    if p1mil:
                        d2e = min(dist(m['tile'], u['tile']) for m in p1mil)
                        danger = d2e < 49 or (d2_line(u['tile']) < 81
                                              and ar - last_wave_birth < 450)
                    if danger:
                        side = (west_flee,) if u['tile'][0] < 38 else SAFE
                        s = side[sl % len(side)]
                        fleeing.add(sl)
                        cmd(f"retask {sl} {s[0]} {s[1]}", 0.22)
                        idle_n[f'{sl}'] = 0

        # ---- 闲置卡死重派（仅非避险村民；扫堂期绕开 slot0、探帽期绕开 slot1
        # ——到位后本块自然转岗）----
        for u in vils:
            sl = u['slot']
            if sl in fleeing or (sl == sweep_slot and sweeping) \
                    or (sl == bank_slot and not bank_scouted):
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
            # 锚点/中继点到达即推进（省 2-poll 确认——staged 行军的锁 dwell 是
            # dry2/3 矿工链卡死的根因之一）
            if (idle_n[key] >= 2 or pos == tuple(NORTH_WAY)) and pos != tuple(j):
                if can_go_north(u, j):
                    jt = stage_target(u, j)
                    cmd(f"retask {sl} {jt[0]} {jt[1]}", 0.22)
                idle_n[key] = 0

        # ---- 金饥荒动态转金（仅中盘金断供时；开局金恒 10 不许触发——dry4 教训：
        # ar510 误抽木工挖金 → Barracks 拖 1300t → 无兵防波）----
        if G < 5 and ar > 3000 and now > rb_cd and jobs:
            wj = [sl for sl, j in jobs.items() if tuple(j) in WOOD_ALL]
            gj = [sl for sl, j in jobs.items() if tuple(j) in GOLD]
            if len(gj) < 2 and len(wj) >= 3:
                sl = wj[-1]
                use = {}
                for g in GOLD:
                    use[g] = use.get(g, 0)
                for g in gj:
                    use[tuple(jobs[g])] = use.get(tuple(jobs[g]), 0) + 1
                gfree = min(GOLD, key=lambda g: use[g])
                jobs[sl] = gfree
                gt = stage_target(u, gfree)
                cmd(f"retask {sl} {gt[0]} {gt[1]}", 0.22)
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
            # TC 遇袭：靠近 TC 的村民进避险（远处岗位照常）；军事回防。
            # 北岗矿工（金管道）豁免——他们有自己的几何判据（d2e/d2_line），
            # 且 TC 警报环（144）罩住全部 SAFE 点和北向锚点 (49,48)：不豁免时
            # 围城期矿工陷入 避险→出坑→再避险 死循环（dry2 实锤，G 全程 0）。
            for u in vils:
                j = jobs.get(u['slot'])
                if u['slot'] not in fleeing and dist(u['tile'], TC) < 144 \
                        and (j is None or j[1] > MINE_YMAX):
                    fleeing.add(u['slot'])
                    s = SAFE[u['slot'] % len(SAFE)]
                    cmd(f"retask {u['slot']} {s[0]} {s[1]}", 0.22)
                    idle_n[f"{u['slot']}"] = 0
            if mymil and (not p1mil or len(mymil) >= len(p1mil)) \
                    and now - last_rally['t'] > 6:
                # 敌众我寡时不从警报拉 FRONT（=顶进敌团硬拼）——风筝态由 threat 块定位
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
            if len(mymil) >= len(p1mil):
                if in_fight:
                    mode = 'fight'
                else:
                    if last_rally['md'] != 'front' or now - last_rally['t'] > 7:
                        cmd(f'rally {FRONT[0]} {FRONT[1]}', 0.3)
                        last_rally = {'t': now, 'md': 'front'}
                        print(f'ar={ar} FRONT 拦截 (我m={len(mymil)} 敌m={len(p1mil)} '
                              f'army {myarmy}/{p1army})', flush=True)
                    mode = 'front'
            else:
                # 1a 风筝（r45 残局）：敌众我寡绝不硬拼（boot3 实锤 m 对冲全灭
                # → 经济随崩）。退 TC 东南 KITE(46,60)——波 approach 线外，敌扑
                # TC 打不着；敌贴 5 格内再退 KITE2。TC 白打可承受（boot2 实测
                # 12 围 1470t 未拆动）。等 m 追平（练兵/拣落单）由上支反打。
                mode = 'kite'
                kt = KITE2 if any(dist(m['tile'], KITE) < 25 for m in p1mil) \
                    else KITE
                if last_rally.get('md') != 'kite' \
                        or tuple(last_rally.get('pt') or ()) != kt \
                        or now - last_rally['t'] > 8:
                    cmd(f'rally {kt[0]} {kt[1]}', 0.3)
                    last_rally = {'t': now, 'md': 'kite', 'pt': kt}
                    print(f'ar={ar} KITE 风筝 (我m={len(mymil)} 敌m={len(p1mil)} '
                          f'→{kt})', flush=True)
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
            # 驻守庭院（非 threat 态也把离院的散兵拉回，raider/探雾民兵除外）
            stray = [u for u in mymil
                     if u['slot'] not in raiders
                     and not (not scouted and u['slot'] == scout_slot)
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

        # ---- 建筑（boot2 教训：矿仓=经济 Keystone——旧序 House(5W)+vil(5W) 把 W
        # 振荡在 0-10，矿仓永欠款 → G=0 → m=0 崩盘。新序：W>=15 先矿仓，House 靠后。
        # 唯 v<=2 紧急阀（N2）仍最优先）----
        # N2：v≤2 紧急阀（r42 boot3 v=1 死锁：House 门与 camp 门互锁 → 全程 H=0
        # → 村民断产）。m>=1 限定=过了开局爆兵期才算"产能崩"（v=2 是开局常态）
        house_emg = len(vils) <= 2 and ar > 1400 and len(barracks) >= 1 \
            and (len(mymil) >= 1 or (W >= 10 and (len(mymil) >= 1 or G >= 5))) \
            and (not scouted or len(camps) > 0 or len(houses) == 0) \
            and (len(houses) == 0 or pop_room <= 0)
        # 末两行=储蓄门豁免（r44 boot1 尸检）：
        # a) 矿仓落成前 H#1 之后不得再抢矿仓木款（dry：H#2/H#3 各吃 5W=矿仓晚 2 趟）；
        # b) 有房且有人口时 house_emg 不得与村民训练抢 5W（boot1：建筑块先于生产块，
        #    v=1 时 H#2-4 连吃 15W，村民 5428 才训出=同一「落地即花」墙的变体）。
        # houses==0 豁免保 N2 防死锁语义：首房永远放行。
        if house_emg and not ucon(11) and len(houses) < 4 and W >= 5 \
                and now > hb_cd:
            print(f'ar={ar} 建 House (EMG v={len(vils)})', flush=True)
            build_fb(HOUSE_CANDS, 11, 'H')
            hb_cd = now + 3
            continue
        if bank_slot is not None and bank_scouted and not bank_any \
                and not ucon(0) and W >= 15 and now > bank_cd:
            # W2 木银行（先于 Barracks：15W<20W 自然顺位）——放格即 hdr[9]+
            # 建筑格生效，满载回送改道 (30,62) 入账后 FSM 自动回树（slot[2]=
            # slot[5]），波期木收入与改派吞货同根拔除。
            print(f'ar={ar} 建伐木场 t0 @{WOOD_BANK}（木银行）res={[W, G, S]}',
                  flush=True)
            ok_wb = build_fb([WOOD_BANK], 0, 'WB')
            bank_cd = now + 12
            if ok_wb:
                print(f'ar={ar} *** 木银行 {WOOD_BANK} 放格：hdr[9] 已改道，'
                      f'西木波期照常入账 ***', flush=True)
                continue
        if not barracks and not ucon(10) and W >= 20 and S >= 10 and now > b_cd:
            print(f'ar={ar} 建 Barracks res={[W, G, S]}', flush=True)
            build_fb(B_CANDS, 10, 'B')
            b_cd = now + 12
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
        if not ucon(11) and len(houses) < 4 and pop_room <= 2 and W >= 5 \
                and (camp_any or W >= 20) and now > hb_cd:
            # House 让位于矿仓木款（矿仓 W>=15 永远先拿）
            print(f'ar={ar} 建 House (room={pop_room})', flush=True)
            build_fb(HOUSE_CANDS, 11, 'H')
            hb_cd = now + 3
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
        if len(houses) and nvil_ < VIL_TARGET and nmil >= 1 \
                and W >= 5 and pop_room >= 1 and now > vil_cd \
                and (nmil >= 3 or W >= 10 or nvil_ <= 3) \
                and (len(camps) > 0 or W >= 20 or not scouted):
            # r44 残局主修复：矿仓储蓄门——scouted 后、矿仓建成前禁止村民小额
            # 花木（否则每趟 5W 落地即花，15W 矿仓门槛永不可达=boot3 死因）
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

        if ar - last_econ_ar >= 250:     # 锚点采样线（sim 校准方法学 §r42：boot 实测优先）
            print(f'ar={ar} ECON res={[W, G, S]} v={len(vils)} m={nmil} '
                  f'p1m={len(p1mil)} fleeing={sorted(fleeing)} '
                  f'jobs={ {k: tuple(v) for k, v in sorted(jobs.items())} }',
                  flush=True)
            last_econ_ar = ar
        st = (f"ar={ar} res={[W, G, S]} pop={popu}/{cap} v={len(vils)} m={nmil} "
              f"p1m={len(p1mil)} p1v={len(p1any) - len(p1mil)} "
              f"army {myarmy}/{p1army} gate={'ARMED' if gate_armed else 'off'} "
              f"age={p0['age']} B={bool(barracks)} C={len(camps)} H={len(houses)} "
              f"WB={int(bank_any)} M={bool(mills)} BS={bool(bss)} T={len(towers)} mode={mode}")
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
    # r42 教训：boot.sh 覆盖 play.log → boot1/2 证据丢失。驱动退出时留副本。
    _copy_playlog()
    print(f'driver end it={it}', flush=True)


if __name__ == '__main__':
    main()
