#!/usr/bin/env python3
"""战役驱动库 v2（BFS 时代）：按 work 目录参数化的全套机器。
import sys; sys.path.insert(0, 'tools/campaign'); import lib
d = lib.Camp('/tmp/aoe-camp/m2')

机器语义（考据详见 docs/agent-operations.md §11）：
- gather_hammer/chop_rows 用"位置 3 轮无进展才重发"守卫——频繁重发 retask 会触发
  BFS 离队重算，路径缓存永远走不完。
- chop_rows 的 fence=(xmin,ymin,ymax[,xmax[,ygate]])：村民出界立即拉回 home
  （载满回送 orbit 会漂向敌区）；(xmax,ygate) 组合成塔区禁入门（m1 敌塔 (49,52)
  射程 4 覆盖出口区 y≤56 段，村民被塔射杀即判负）。"""
import json
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))

import aoesave


class Camp:
    def __init__(self, work):
        self.work = work
        self.fifo = work + "/fifo"
        self.json = work + "/fifo.json"
        self.log = work + "/play.log"

    # ---- 基础 ----
    def cmd(self, c):
        subprocess.run(["sh", "-c", f"echo '{c}' > {self.fifo}"], check=True)

    def cmdq(self, *cmds, gap=0.08):
        for c in cmds:
            self.cmd(c)
            time.sleep(gap)

    def result(self):
        with open(self.log, errors="replace") as f:
            for ln in f:
                if "[result]" in ln:
                    return ln.strip()
        return None

    def state(self, tries=8):
        for _ in range(tries):
            try:
                self.cmd("state")
                time.sleep(0.6)
                with open(self.json) as f:
                    return json.load(f)
            except Exception:
                time.sleep(0.5)
        raise RuntimeError("state 无响应")

    def res(self):
        return self.state().get("res")

    def aA(self):
        return self.state()["aA"]

    # ---- 槽位 ----
    def slots(self, p=0):
        self.cmd(f"slots {p}")
        time.sleep(0.3)
        for ln in reversed(self.lines()):
            m = re.match(rf'\[slots\] p{p} n=(\d+) (.*)', ln.strip())
            if m:
                out = {}
                for tok in m.group(2).split():
                    mm = re.match(r'(\d+):t(\d+)@\((\d+),(\d+)\)w(\w+)', tok)
                    out[int(mm.group(1))] = dict(i=int(mm.group(1)), t=int(mm.group(2)),
                                                 x=int(mm.group(3)), y=int(mm.group(4)),
                                                 w=int(mm.group(5), 16))
                return out
        return {}

    def lines(self, n=None):
        with open(self.log, errors="replace") as f:
            ls = f.readlines()
        return ls[-n:] if n else ls

    def units(self, p=0, t=None):
        return [u for u in self.slots(p).values() if t is None or u['t'] == t]

    def villagers(self):
        return sorted([u for u in self.slots(0).values() if u['t'] == 0 and (u['x'] or u['y'])],
                      key=lambda u: u['i'])

    def military(self):
        return [u for u in self.slots(0).values() if u['t'] >= 2 and (u['x'] or u['y'])]

    # ---- 地图 ----
    def save_probe(self, name="t"):
        self.cmd(f"save {self.work}/{name}.aoesave")
        time.sleep(0.9)
        return aoesave.load(f"{self.work}/{name}.aoesave")

    def trips(self, sv, tx, ty):
        r = sv.tile(tx, ty)['raw']
        return ((r >> 2) & 0x1F) if (r & 0x300) == 0x300 else -1

    # ---- 采集（BFS 时代：位置 3 轮无进展才重发 retask——频繁重发会触发离队重算，
    # BFS 路径缓存永远走不完）----
    def gather_hammer(self, secs, jobs, verbose=True):
        """jobs: {slot: (rx,ry,ax,ay)} r=资源格 a=2+格外的 approach 空地格"""
        t_end = time.time() + secs
        n = 0
        lastpos = {}
        stuck = {}
        while time.time() < t_end:
            n += 1
            if n % 8 == 0 and (self.result() or self.aA() == 12):
                print('RESULT:', self.result() or 'aA=12'); return
            usd = self.slots(0)
            line = []
            for slot, (rx, ry, ax, ay) in jobs.items():
                u = usd.get(slot)
                if u is None or u['t'] != 0:
                    line.append(f's{slot}:X'); continue
                w = u['w'] & 0xF
                if w == 2:
                    lastpos[slot] = (u['x'], u['y']); stuck[slot] = 0
                    line.append(f's{slot}:采'); continue
                pos = (u['x'], u['y'])
                if lastpos.get(slot) == pos:
                    stuck[slot] = stuck.get(slot, 0) + 1
                else:
                    stuck[slot] = 0
                lastpos[slot] = pos
                if stuck[slot] < 3:
                    line.append(f's{slot}:走'); continue
                near_r = (u['x'] - rx) ** 2 + (u['y'] - ry) ** 2 <= 2
                tgt = (ax, ay) if near_r else (rx, ry)
                self.cmd(f"retask {slot} {tgt[0]} {tgt[1]}")
                time.sleep(0.05)
                stuck[slot] = 0
                line.append(f's{slot}:({u["x"]},{u["y"]})w{w}->r')
            if verbose and n % 5 == 0:
                print(f'{int(t_end - time.time())}s ' + ' '.join(line),
                      flush=True)
            time.sleep(1.2)

    # ---- 砍隧道（多行并行推进到目标 x）----
    def chop_rows(self, rows, x_target, secs, approach_off=2, verbose=True,
                  fence=(27, 52, 63), home=(31, 58)):
        """rows: {y: front_x}；锤到所有行 front>=x_target 或终局。
        fence=(xmin,ymin,ymax)：村民出界立即拉回 home（回送 orbit 会漂向敌区，
        m1 三连 LOSS 根因）；p0 数量掉即中止（护送关死村民=判负）。"""
        t_end = time.time() + secs
        n = 0
        probe_q = list(rows)
        lastpos = {}
        stuck = {}
        while time.time() < t_end:
            n += 1
            if n % 10 == 0 and (self.result() or self.aA() == 12):
                print('RESULT:', self.result() or 'aA=12'); return rows
            usd = self.slots(0)
            vs = [u for u in usd.values() if u['t'] == 0 and (u['x'] or u['y'])]
            if len(vs) < 3 or self.result():
                print('VILL LOST/END!', self.result(), [(u['i'], u['t']) for u in usd.values()])
                return rows
            for u in vs:
                xmin, ymin, ymax = fence[0], fence[1], fence[2]
                xmax = fence[3] if len(fence) > 3 else 63
                ygate = fence[4] if len(fence) > 4 else -1
                bad = (u['x'] < xmin or u['x'] > xmax
                       or u['y'] < ymin or u['y'] > ymax
                       or (ygate > 0 and u['x'] > xmax - 3 and u['y'] < ygate))
                if bad:
                    self.cmd(f"retask {u['i']} {home[0]} {home[1]}")
                    stuck[u['i']] = 0
                    lastpos[u['i']] = (u['x'], u['y'])
                    continue
                r = list(rows)[u['i'] % len(rows)]
                w = u['w'] & 0xF
                fx = rows[r]
                pos = (u['x'], u['y'])
                if lastpos.get(u['i']) == pos:
                    stuck[u['i']] = stuck.get(u['i'], 0) + 1
                else:
                    stuck[u['i']] = 0
                lastpos[u['i']] = pos
                if w != 2 and stuck.get(u['i'], 0) >= 3:
                    near = (u['x'] - fx) ** 2 + (u['y'] - r) ** 2 <= 2
                    tgt = (fx, r) if not near else (max(xmin, fx - approach_off), r)
                    self.cmd(f"retask {u['i']} {tgt[0]} {tgt[1]}")
                    time.sleep(0.05)
                    stuck[u['i']] = 0
                    stuck[u['i']] = 0
            r = probe_q.pop(0); probe_q.append(r)
            sv = self.save_probe()
            if self.trips(sv, rows[r], r) < 0:
                print(f'{int(t_end - time.time())}s row {r}: tree {rows[r]} DOWN', flush=True)
                rows[r] += 1
                if all(v >= x_target for v in rows.values()):
                    print('BREACH'); break
            if verbose and n % 8 == 0:
                print(f'{int(t_end - time.time())}s fronts={rows}', flush=True)
            time.sleep(1.0)
        return rows

    # ---- 军事 ----
    def rally_wait(self, tx, ty, settle=25):
        self.cmd(f"rally {tx} {ty}")
        time.sleep(settle)

    def rally_seq(self, targets, settle=25):
        """逐个集结清场；每站后回 p1 计数"""
        for tx, ty in targets:
            self.rally_wait(tx, ty, settle)
            us1 = self.slots(1)
            print(f'rally ({tx},{ty}) -> 敌余 {len(us1)}', flush=True)
