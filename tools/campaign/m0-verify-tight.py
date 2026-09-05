#!/usr/bin/env python3
"""m0 修正版紧竞态验证 (fork replay-verify.py):
等到 [result] 才收尾; verdict 要求 result 非空且匹配。"""
import os
import subprocess
import sys
import time

DIR = '/tmp/aoe-camp/m0/deliver'
BASE = 431
EXPECT = '[result] WIN ticks=8576'
REPO = '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered'
WORK = '/tmp/rv-tight-m0'

subprocess.run(['rm', '-rf', WORK])
os.makedirs(WORK + '/saves')
os.makedirs(WORK + '/rms')
FIFO = WORK + '/fifo'
os.mkfifo(FIFO)

flags = [f'-Daoe.tickms=10', '-Daoe.debug=1', '-Daoe.harnessQuiet=1',
         '-Daoe.exitOnResult=1', f'-Daoe.saveDir={WORK}/saves',
         f'-Daoe.rmsDir={WORK}/rms', '-Daoe.mapSeed=8224',
         f'-Daoe.devBoot={DIR}/base.aoesave', f'-Daoe.devMouse={FIFO}',
         '-Daoe.bfsPath=1', '-Daoe.headless=1']
cp = f'{REPO}/build/classes/java/main:{REPO}/build/resources/main'
log = open(WORK + '/replay.log', 'wb')
proc = subprocess.Popen(['/opt/homebrew/opt/openjdk@17/bin/java'] + flags
                        + ['-cp', cp, 'aoe.Main'], stdout=log, stderr=log)
fw = open(FIFO, 'w')
armed = sent = False
t0 = time.time()
res_line = None
while time.time() - t0 < 600:
    if proc.poll() is not None:
        break
    try:
        with open(WORK + '/replay.log', errors='replace') as f:
            lines = f.readlines()
    except FileNotFoundError:
        time.sleep(0.01)
        continue
    if not armed and any('[devBoot] done' in ln for ln in lines):
        armed = True
        print('devBoot done', flush=True)
    if armed and not sent:
        fw.write(f'replaytrace {DIR}/trace.txt {BASE}\n')
        fw.flush()
        sent = True
        print('replaytrace 已发送 (devBoot done 同拍)', flush=True)
    if sent and any('[result]' in ln for ln in lines):
        for ln in lines:
            if '[result]' in ln:
                res_line = ln.strip()
        break
    time.sleep(0.01)
fw.close()
# 等 java 自然退出 (exitOnResult), 最多 30s
for _ in range(300):
    if proc.poll() is not None:
        break
    time.sleep(0.1)
proc.terminate()

n_fifo = n_input = 0
with open(WORK + '/replay.log', errors='replace') as f:
    for ln in f:
        if ln.startswith('[fifo] ar='):
            n_fifo += 1
        elif ln.startswith('[input] ar='):
            n_input += 1
print(f'回放侧 [result]: {res_line}')
print(f'预期          : {EXPECT}')
print(f'事件数 fifo={n_fifo} input={n_input}')
ok = (res_line is not None and res_line == EXPECT)
print('VERDICT:', 'PASS' if ok else 'FAIL')
# 事件流 diff (应用 ar 级)
import re
def stream(path, flt):
    out = []
    for ln in open(path, errors='replace'):
        if ln.startswith('[fifo] ar='):
            body = ln[len('[fifo] '):].strip()
            if not re.match(r'(state|aistate|mapdump|save|slots|ping|until'
                            r'|replaytrace|key)', body):
                out.append(body)
    return out
a = stream(f'{DIR}/session.log', None)
b = stream(WORK + '/replay.log', None)
if a == b:
    print(f'事件流对拍: 一致 ({len(a)} 行, 含应用 ar)')
else:
    nd = sum(1 for x, y in zip(a, b) if x != y) + abs(len(a) - len(b))
    print(f'事件流对拍: 不一致 (diff≈{nd}, play={len(a)} replay={len(b)})')
    for x, y in zip(a, b):
        if x != y:
            print('  play :', x)
            print('  replay:', y)
            break
sys.exit(0 if ok else 1)
