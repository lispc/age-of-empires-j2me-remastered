#!/usr/bin/env python3
"""m5a 回放验证（replay-probe.py 模式,防 campaign-replay.sh turbo 快死竞态）：
预握 fifo 写端 + 10ms 轮询 [devBoot] done → 立刻送 replaytrace。
tickms=10 非 turbo（3156 tick ≈ 32s,无快死竞态）。aistate 1s 轮询标定波时刻。"""
import json
import os
import shutil
import subprocess
import sys
import time

DIR = '/tmp/aoe-camp/m5a'
BASE = 397
WORK = '/tmp/aoe-camp/m5a/replay-work'
REPO = '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered'

shutil.rmtree(WORK, ignore_errors=True)
os.makedirs(WORK + '/saves')
os.makedirs(WORK + '/rms')
FIFO = WORK + '/fifo'
os.mkfifo(FIFO)

flags = ['-Daoe.tickms=10', '-Daoe.debug=1', '-Daoe.harnessQuiet=1',
         '-Daoe.exitOnResult=1', f'-Daoe.saveDir={WORK}/saves',
         f'-Daoe.rmsDir={WORK}/rms', '-Daoe.mapSeed=8224',
         f'-Daoe.devBoot={DIR}/base.aoesave', f'-Daoe.devMouse={FIFO}',
         '-Daoe.bfsPath=1', '-Daoe.headless=1']
cp = f'{REPO}/build/classes/java/main:{REPO}/build/resources/main'
log = open(WORK + '/replay.log', 'wb')
proc = subprocess.Popen(['/opt/homebrew/opt/openjdk@17/bin/java'] + flags
                        + ['-cp', cp, 'aoe.Main'], stdout=log, stderr=log)
print(f'java pid={proc.pid}', flush=True)
fw = open(FIFO, 'w')
print('fifo 写端已连接', flush=True)

armed = False
sent = False
t0 = time.time()
last_poll = 0.0
waves = []       # (tick, n_e, comp) 波沿
prev_e = 0
result_line = None
while time.time() - t0 < 300:
    if proc.poll() is not None:
        print('java 已退出', flush=True)
        break
    try:
        with open(WORK + '/replay.log', 'r', errors='replace') as f:
            content = f.read()
    except FileNotFoundError:
        continue
    if not armed and '[devBoot] done' in content:
        armed = True
        print('devBoot done', flush=True)
    if armed and not sent:
        fw.write(f'replaytrace {DIR}/trace.txt {BASE}\n')
        fw.flush()
        sent = True
        print('replaytrace 已发送', flush=True)
        time.sleep(0.2)
        continue
    if sent and time.time() - last_poll > 1.0:
        last_poll = time.time()
        for ln in content.splitlines():
            if '[result]' in ln:
                result_line = ln.strip()
        try:
            fw.write('aistate\n')
            fw.flush()
            time.sleep(0.1)
            with open(FIFO + '.aistate.json') as f:
                st = json.load(f)
            ne = [u for u in st['units'] if u['p'] == 1]
            if len(ne) != prev_e:
                comp = {}
                for e in ne:
                    comp[e['type']] = comp.get(e['type'], 0) + 1
                waves.append((st['tick'], len(ne), comp))
                prev_e = len(ne)
        except Exception:
            pass
    if result_line:
        break
time.sleep(2)
print('RESULT:', result_line, flush=True)
print('波沿(tick,n_e,comp):', waves, flush=True)
try:
    fw.write('exit\n')
    fw.flush()
except Exception:
    pass
time.sleep(1)
try:
    proc.kill()
except Exception:
    pass
# 对拍终局
want = open(DIR + '/session.log', errors='replace').read()
want = [ln for ln in want.splitlines() if ln.startswith('[result]')]
print('play 终局:', want, flush=True)
print('对拍:', '一致 ✓' if want and want[0] == result_line else '不一致 ✗', flush=True)
