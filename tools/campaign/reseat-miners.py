#!/usr/bin/env python3
"""r16 boot3 热修 2：采集 FSM 停摆复位。
症状：金/石矿工站在资源格上 action=0（r47t 单元快照实锤 slot4/6/9）——
历史 retask 风暴把「走回家→再进格」循环打断后，驱动「老 slot 不重发」
纪律使其永驻 idle；G 收入=0（G 钉 5 达 26000t）。
引擎语义（r47 手册）：采集钩子=多步行走踏入资源格；同格/贴格 retask=no-op。
复位=把 idle@矿区的村民 retask 到同簇**另一资源格、距现位 manh>=2**——
多格行走踏入即重燃 FSM。只动 act==0 且 tgt==tile 的矿区村民；
每槽 90s 冷却；不碰 TC 6 格内（扫堂/keeper 不归我管）。"""
import json
import subprocess
import time

W = '/tmp/aoe-camp/m4p'
FIFO = W + '/fifo'
AISTATE = W + '/fifo.aistate.json'

GOLD = [(35, 36), (36, 36), (37, 36), (35, 35), (36, 35), (34, 36),
        (32, 35), (33, 35), (34, 35), (33, 36), (32, 36), (37, 35), (38, 35),
        (32, 34), (33, 34), (34, 34), (35, 34), (36, 34), (30, 33), (31, 33),
        (34, 37), (35, 37), (36, 37)]
STONE = [(39, 36), (40, 36), (41, 36), (37, 37), (38, 37), (39, 37), (40, 37),
         (41, 37), (37, 38), (38, 38), (39, 38), (40, 38), (41, 38), (37, 39),
         (38, 39), (39, 39), (40, 39), (41, 39), (37, 40), (38, 40), (39, 40),
         (40, 40), (41, 40)]
CLUSTER = set(GOLD) | set(STONE)
TC = (43, 57)


def fifo(c, wait=0.4):
    subprocess.run(['sh', '-c', f"echo '{c}' > {FIFO}"], check=True, timeout=5)
    time.sleep(wait)


def aistate():
    for _ in range(5):
        try:
            fifo('aistate', 0.3)
            return json.load(open(AISTATE))
        except Exception:
            time.sleep(0.4)
    raise RuntimeError('aistate 无响应')


def manh(a, b):
    return abs(a[0] - b[0]) + abs(a[1] - b[1])


def main():
    cooldown = {}
    deadline = time.time() + 3600
    while time.time() < deadline:
        try:
            a = aistate()
        except Exception:
            continue
        now = time.time()
        for u in a['units']:
            if u['p'] != 0 or u['type'] > 1:
                continue
            if u['action'] != 0:
                continue
            tile = tuple(u['tile'])
            tgt = tuple(u['target'] or tile)
            if tgt != tile:
                continue
            if manh(tile, TC) < 7:
                continue
            near = [t for t in CLUSTER if manh(tile, t) <= 3]
            if not near:
                continue
            sl = u['slot']
            if now - cooldown.get(sl, 0) < 8:
                continue
            # 同簇资源格里挑距现位 >=2 的（多格行走才触发钩子）
            cands = [t for t in near if manh(tile, t) >= 2]
            if not cands:
                cands = [t for t in near if manh(tile, t) == 1]
            if not cands:
                continue
            dst = min(cands, key=lambda t: manh(tile, t))
            fifo(f"retask {sl} {dst[0]} {dst[1]}", 0.4)
            cooldown[sl] = now
            print(f"复位 slot{sl} {tile} → {dst}（采集 FSM 停摆重触发）",
                  flush=True)
        time.sleep(3)
    print('reseat 退出', flush=True)


if __name__ == '__main__':
    main()
