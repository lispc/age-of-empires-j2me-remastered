#!/usr/bin/env python3
"""探针局: 塔火 vs 建筑甲冲车 DPS 标定 (r47 惯例, 不烧主线预算)。

前置: M6_START_DRV=0 ./m6h-boot.sh 1 (裸局, base 已存)。
本脚本: 3 冲车 retask 到塔 (22,50) 格 auto-chew, 每 2s 采
  buildingRecs hp / 冲车 hp / tick → probe-ram.jsonl + 控制台摘要。
终点: 塔平 / 冲车全灭 / 360s。
"""
import json
import subprocess
import sys
import time

W = '/tmp/aoe-camp/m6h'
FIFO = W + '/fifo'
TOWER = (22, 50)
CAP = 360.0


def send(cmd):
    subprocess.run(['sh', '-c', f"echo '{cmd}' > {FIFO}"], timeout=6)


def aistate():
    try:
        send('aistate')
        time.sleep(0.25)
        with open(FIFO + '.aistate.json') as f:
            return json.load(f)
    except Exception:
        return None


def main():
    st = aistate()
    if not st:
        print('NO AISTATE — 裸局没起来?')
        return 1
    rams = [u['slot'] for u in st['units'] if u['p'] == 0 and u['type'] == 7]
    print(f'tick={st["tick"]} rams={rams}')
    if not rams:
        return 1
    for s in rams:
        send(f'retask {s} {TOWER[0]} {TOWER[1]}')
    print(f'retask {rams} -> {TOWER}, 采样中...')
    t0 = time.time()
    logf = open(W + '/probe-ram.jsonl', 'w')
    last_sum = ''
    while time.time() - t0 < CAP:
        st = aistate()
        if not st:
            break
        tw = [b for b in st.get('buildingRecs', [])
              if tuple(b['tile']) == TOWER]
        rhp = {u['slot']: u['hp'] for u in st['units']
               if u['p'] == 0 and u['type'] == 7}
        rtile = {u['slot']: tuple(u['tile']) for u in st['units']
                 if u['p'] == 0 and u['type'] == 7}
        rec = {'t': round(time.time() - t0, 1), 'tick': st['tick'],
               'tower_hp': tw[0]['hp'] if tw else 0,
               'ram_hp': rhp, 'ram_tile': {str(k): v for k, v in
                                           rtile.items()}}
        logf.write(json.dumps(rec) + '\n')
        logf.flush()
        s = f't={rec["t"]:5.1f} tick={st["tick"]} tower_hp=' \
            f'{rec["tower_hp"]} rams={rhp}'
        if s != last_sum:
            print(s, flush=True)
            last_sum = s
        if not tw or rec['tower_hp'] <= 0:
            print('TOWER DOWN')
            break
        if not rhp or all(v <= 0 for v in rhp.values()):
            print('RAMS ALL DEAD')
            break
        # 阵亡冲车重发不设: 观察 natural chew 行为
        time.sleep(2.0)
    logf.close()
    # 摘要
    rows = [json.loads(l) for l in open(W + '/probe-ram.jsonl')]
    if len(rows) >= 2:
        a, b = rows[0], rows[-1]
        dt = b['tick'] - a['tick']
        dhp = a['tower_hp'] - b['tower_hp']
        ram0 = sum(a['ram_hp'].values())
        ram1 = sum(b['ram_hp'].values())
        print(f'== 摘要 ==\n嚼时 {dt} tick, 塔 -{dhp} hp → '
              f'冲车合计 DPS vs 塔 = {dhp / dt:.3f} hp/t')
        print(f'冲车合计 hp {ram0}→{ram1} (-{ram0 - ram1}) → '
              f'塔对冲车 DPS ≈ {(ram0 - ram1) / dt:.3f} hp/t'
              f' (含无塔期误差)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
