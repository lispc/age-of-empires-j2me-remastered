#!/usr/bin/env python3
"""m4q 回放侧波时刻标定：replay-verify.py + aistate 只读轮询。
轮询命令在录制协议中就是无模拟副作用（trace 排除项），用于给回放标定
p1 军事单位出生沿（波时刻）与 p0 民兵数，对比录制侧 drv 波时刻找首分歧。"""
import os
import subprocess
import sys
import time

DIR = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m4q/lossrec'
BASE = int(sys.argv[2]) if len(sys.argv) > 2 else 641
EXPECT = sys.argv[3] if len(sys.argv) > 3 else 'LOSS ticks=5359'
WORK = '/tmp/aoe-camp/m4q/replay-work'
REPO = ('/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered')

import shutil
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
while time.time() - t0 < 900:
    if proc.poll() is not None:
        print('java 已退出', flush=True)
        break
    try:
        with open(WORK + '/replay.log', 'r', errors='replace') as f:
            lines = f.readlines()
    except FileNotFoundError:
        continue
    if not armed and any('[devBoot] done' in ln for ln in lines):
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
        fw.write('aistate\n')
        fw.flush()
        try:
            import json
            a = json.load(open(WORK + '/fifo.aistate.json'))
            p0 = a['players'][0]
            p1 = a['players'][1] if len(a['players']) > 1 else {}
            mils = [u for u in a['units'] if u['p'] == 0 and u['type'] == 2]
            e_mils = [u for u in a['units'] if u['p'] == 1 and u['type'] >= 2]
            e_vils = [u for u in a['units'] if u['p'] == 1 and u['type'] <= 1]
            print(f"PROBE ar={a['tick']} p0 v={p0.get('v')} m={len(mils)}"
                                  f" | p1 mil={len(e_mils)} vil={len(e_vils)}"
                                  f" res0={p0.get('res')}", flush=True)
        except Exception as e:
            print('probe skip', e, flush=True)
    if sent and any('replaytrace done' in ln for ln in lines):
        for ln in lines:
            if 'replaytrace done' in ln or '[result]' in ln:
                print(ln.strip(), flush=True)
        break
    if any('[result]' in ln for ln in lines):
        for ln in lines:
            if '[result]' in ln:
                print('GAME', ln.strip(), flush=True)
        break
    time.sleep(0.01)

fw.close()
time.sleep(2)
proc.terminate()
result = ''
with open(WORK + '/replay.log', 'r', errors='replace') as f:
    for ln in f:
        if '[result]' in ln:
            result = ln.strip()
print(f'终局: {result} | 预期: {EXPECT}', flush=True)
