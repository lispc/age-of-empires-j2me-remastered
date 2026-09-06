#!/usr/bin/env python3
"""drvkit — 战役 FIFO 驱动公共件（r70 收官提炼，m0/m1/m2/m3/m6 八代驱动的
最大公约数）。

设计决定：
- 历史驱动（m0drv/m1drv/m2v2drv/m3v2drv/m6hdrv）是胜局的冻结证据，**不回改**
  挂接本库；本库供下一代驱动 import 使用，模式与各代完全一致（逐块对照
  m1drv/m6hdrv 提炼，无新逻辑）。
- 编码进去的四条铁律（来源见 docs/agent-operations.md §11）：
  1. DRY 闸：M6_DRY 类环境变量强制假 fifo+只落日志——import/自检/干跑
     永远碰不到活局（r62 事故：exec_module 连活局发 14 条 retask）。
  2. retask 风暴禁律（r69）：逐拍重发=单位永在行军态永不接战。沉淀守卫
     =idle 且已贴目标→跳过；敌变位才重发；批量指令一次 send 同帧应用。
  3. 死亡判定唯一真源=[combat] 行（r64）：aistate 有死亡幽灵（slot 消失≠死、
     死者滞留回显）——差分+幽灵过滤是标配；同型 d²≤4 不标死（歧义守卫）。
  4. 尸检辅助 send 日志：每条指令落 [drv-send]（r66 建议），否则尸检无法
     还原「单位被派到哪」。

自检：`python3 drvkit.py --selftest`（强制 DRY，无 fifo 无游戏，跑断言）。
"""
import json
import os
import re
import subprocess
import time

_COMBAT_M = re.compile(
    r'\[combat\] p(\d+) type(\d+) died at \((\d+),(\d+)\) ar=(\d+)')


def d2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2


class Driver:
    """FIFO 驱动骨架。env: <P>_DRY=1 开 DRY 闸；<P>_DRY_SEQ=dry 快照目录
    （dry-seq-*.json 逐拍回放 aistate）。参数 prefix 如 'M6'。"""

    def __init__(self, work, prefix='M6', poll=0.35, timeout=1500.0,
                 log_name='play.log'):
        self.w = work
        self.dry = os.environ.get(prefix + '_DRY', '') == '1'
        self.dry_seq = os.environ.get(prefix + '_DRY_SEQ', '')
        self.fifo = os.path.join(work, 'fifo')
        self.ais = self.fifo + '.aistate.json'
        self.log_path = os.path.join(work, log_name)
        self.poll = poll
        self.timeout = timeout
        self.dead = False          # fifo 无读者（游戏已退）
        self._dry_i = 0
        self._combat_seen = set()  # (type,(x,y),ar) 去重
        self.ghosts = set()        # (type,(x,y)) 死亡幽灵
        self.log('=== drvkit start '
                 f'poll={poll} mode={"DRY" if self.dry else "LIVE"} ===')

    # ---- 基础 ----
    def log(self, m):
        print(f'[{time.strftime("%H:%M:%S")}] {m}', flush=True)

    def send(self, cmds):
        """批量指令一次写（同帧应用，防 retask 风暴）。DRY 时只落日志。"""
        if not cmds:
            return
        if self.dry:
            for c in cmds:
                self.log(f'[dry-send] {c}')
            return
        for c in cmds:
            self.log(f'[drv-send] {c}')
        script = '; '.join(f"echo '{c}' > {self.fifo}" for c in cmds)
        try:
            subprocess.run(['sh', '-c', script], timeout=6.0)
        except subprocess.TimeoutExpired:
            self.dead = True
            self.log('FIFO 无读者(进程退场?)')

    def aistate(self):
        """发 aistate 并读回 JSON；DRY 时按 DRY_SEQ 逐拍回放。失败返回 None。"""
        if self.dry:
            if not self.dry_seq:
                return None
            try:
                files = sorted(f for f in os.listdir(self.dry_seq)
                               if f.startswith('dry-seq-'))
            except OSError:
                return None
            if not files:
                return None
            f = files[min(self._dry_i, len(files) - 1)]
            self._dry_i += 1
            try:
                with open(os.path.join(self.dry_seq, f)) as fh:
                    return json.load(fh)
            except Exception:
                return None
        for _ in range(4):
            if self.dead:
                return None
            try:
                subprocess.run(['sh', '-c', f"echo 'aistate' > {self.fifo}"],
                               timeout=4.0)
            except subprocess.TimeoutExpired:
                self.dead = True
                return None
            time.sleep(0.10)
            try:
                with open(self.ais) as f:
                    return json.load(f)
            except Exception:
                time.sleep(0.15)
        return None

    def result(self):
        """play.log 里的 [result] 行（无则 None）。"""
        try:
            with open(self.log_path, errors='replace') as f:
                for ln in f:
                    if '[result]' in ln:
                        return ln.strip()
        except Exception:
            pass
        return None

    def mapdump(self, fname, box=None):
        """地图 ASCII dump（引擎 devMouse，建筑层转置已修；资源真值仍以
        aoesave res 对账——'S' 图例吞 kind0 浆果）。"""
        cmd = f'mapdump {fname}'
        if box:
            cmd += ' ' + ' '.join(str(v) for v in box)
        self.send([cmd])

    # ---- retask 风暴禁律（r69） ----
    # 单位记录约定: u['slot']=slot 序号(int), u['arr']=playerUnitSlots 的
    # 8 元切片（[0]=pos 打包 x<<8|y, [2]=目标打包, [7] 低 nibble=任务字）。
    @staticmethod
    def idle(u):
        """任务字 0 且 pos==tgt=闲置。"""
        return (u['arr'][7] & 0xF) == 0 and \
            (u['arr'][0] & 0xFFFF) == (u['arr'][2] & 0xFFFF)

    @staticmethod
    def should_retask(u, tgt):
        """沉淀守卫（m6 assign+m3 沉淀守卫合成）：风暴=重复发**同一目标**——
        slot[2] 已是 tgt → 跳过（'enroute' 行军中 / 'settled' 已到位 idle）；
        目标不同 → 'ok' 发一次性改派（busy 中换新目标合法）。敌变位=
        tgt 变化 → 自然只发一次。返回 (bool, reason)。"""
        packed = (tgt[0] << 8) | tgt[1]
        if (u['arr'][2] & 0xFFFF) == packed:
            # idle+目标到位=settled;行军/攻击中= enroute——两者都不重发
            return False, ('settled' if Driver.idle(u) else 'enroute')
        return True, 'ok'

    @staticmethod
    def focus_cmds(units, quarry_tile):
        """集火令（近战/帖弓通用）：只对目标≠猎物当前格者发（沉淀守卫内置，
        敌变位=只发一次）；批量返回由 send() 同帧应用。
        quarry_tile 传敌当前格（aistate 逐拍跟随，无需预判）。"""
        cmds = []
        for u in units:
            do, _ = Driver.should_retask(u, quarry_tile)
            if do:
                cmds.append(f'retask {u["slot"]} {quarry_tile[0]} {quarry_tile[1]}')
        return cmds

    # ---- 死亡差分（[combat] 唯一真源, r64） ----
    def combat_lines(self):
        out = []
        try:
            with open(self.log_path, errors='replace') as f:
                for ln in f:
                    if '[combat]' not in ln:
                        continue
                    m = _COMBAT_M.search(ln)
                    if m:
                        out.append((int(m.group(1)), int(m.group(2)),
                                    (int(m.group(3)), int(m.group(4))),
                                    int(m.group(5))))
        except Exception:
            pass
        return out

    def update_ghosts(self, foes, player=1, same_type_d2=4):
        """扫 [combat] p<player> 死亡 → 标记幽灵（同型且距死点 ≤same_type_d2
        的候选一起排除，防歧义误标）。p0 村民死亡返回 True（护送类判负链）。"""
        pool = list(foes)
        vill_dead = False
        for p, ty, dxy, ar in self.combat_lines():
            key = (p, ty, dxy, ar)
            if key in self._combat_seen:
                continue
            self._combat_seen.add(key)
            if p != player:
                if p == 0 and ty < 2:
                    vill_dead = True
                continue
            best, bd = None, same_type_d2 + 1
            for g in pool:
                if g['type'] != ty:
                    continue
                dd = d2(tuple(g['tile']), dxy)
                if dd < bd:
                    best, bd = g, dd
            if best is not None:
                pool.remove(best)
                self.ghosts.add((ty, tuple(best['tile'])))
                self.log(f'☠[combat] p{player} type{ty}@{dxy} ar={ar} 记亡')
        return vill_dead

    def filter_ghosts(self, foes, ghost_d2=2):
        """aistate 幽灵过滤：位置在已记亡者 ghost_d2 内的同型单位剔除。"""
        out = []
        for u in foes:
            t = tuple(u['tile'])
            if any(gty == u['type'] and d2(t, gxy) <= ghost_d2
                   for gty, gxy in self.ghosts):
                continue
            out.append(u)
        return out


def _selftest():
    """强制 DRY 自检：无 fifo 无游戏。"""
    os.environ['DK_DRY'] = '1'
    work = '/tmp/drvkit-selftest'
    os.makedirs(work, exist_ok=True)
    d = Driver(work, prefix='DK', poll=0.05)
    assert d.dry, 'DRY 闸未生效'
    # 1) send 只落日志不触 fifo
    d.send(['retask 0 10 10', 'retask 1 12 10'])
    assert not os.path.exists(d.fifo), 'DRY 下不得创建 fifo'
    # 2) should_retask 沉淀守卫（风暴=重复同目标）
    def u(slot, pos, tgt, task=1):
        return {'slot': slot, 'tile': [pos >> 8, pos & 0xFF],
                'arr': [pos, 0, tgt, 0, 0, 0, 0, task]}
    assert Driver.should_retask(u(0, 3 << 8 | 3, 7 << 8 | 7),
                                (9, 9)) == (True, 'ok')
    assert Driver.should_retask(u(1, 5 << 8 | 5, 9 << 8 | 9),
                                (9, 9)) == (False, 'enroute')
    assert Driver.should_retask(u(2, 5 << 8 | 5, 5 << 8 | 5, task=0),
                                (5, 5)) == (False, 'settled')
    # 3) focus_cmds 同帧批量（目标≠猎物格者才发）
    cmds = Driver.focus_cmds([u(2, 1 << 8 | 1, 7 << 8 | 7),
                              u(3, 2 << 8 | 2, 5 << 8 | 5)], (7, 7))
    assert cmds == ['retask 3 7 7'], cmds
    # 4) 死亡差分+幽灵过滤
    foes = [{'type': 4, 'slot': 5, 'tile': [6, 5], 'arr': [0] * 8},
            {'type': 4, 'slot': 6, 'tile': [5, 6], 'arr': [0] * 8}]
    with open(d.log_path, 'w') as f:
        f.write('[combat] p1 type4 died at (6,5) ar=100 remaining=3\n')
        f.write('[combat] p0 type0 died at (1,1) ar=101 remaining=2\n')
        f.write('[combat] p1 type4 died at (6,5) ar=100 remaining=3\n')  # 重复
    vill_dead = d.update_ghosts(foes)
    assert vill_dead, 'p0 村民死亡必须上报'
    assert len(d.ghosts) == 1, f'重复死亡+歧义应只记 1: {d.ghosts}'
    alive = d.filter_ghosts(foes)
    assert len(alive) <= 1, f'幽灵过滤失败: {alive}'
    print('DRVKIT SELFTEST ALL PASS')
    return 0


if __name__ == '__main__':
    import sys
    if '--selftest' in sys.argv:
        sys.exit(_selftest())
    print(__doc__)
    sys.exit(1)
