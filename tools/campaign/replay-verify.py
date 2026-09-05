#!/usr/bin/env python3
"""用法: replay-verify.py <missionDir> [baseTick] [预期result子串]
r16 win 对拍（campaign-replay.sh 的紧竞态版）：
原脚本 grep 轮询 2s + echo 才发 replaytrace——turbo 下 unattended 游戏
几百 tick/s，等发令时早已被波拆掉（r16 实测 LOSS@4095，脚本卡死在
fifo echo 上）。本版：①提前 open fifo 写端（握住管道）；②10ms 轮询
[load] applied（快照落地=traceBase 已钉 581），瞬间写 replaytrace；
③帧首 drain 把 t<当前 的迟到事件按序补发，<53t 窗口内落地=位精确可期。"""
import os
import shutil
import subprocess
import sys
import time

DIR = sys.argv[1] if len(sys.argv) > 1 else '/tmp/aoe-camp/m4q/lossrec'
BASE = int(sys.argv[2]) if len(sys.argv) > 2 else 641
EXPECT = sys.argv[3] if len(sys.argv) > 3 else '[result] LOSS ticks=5359'
REPO = ('/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered')
MS = '10'
WORK = '/tmp/rv-work-' + os.path.basename(DIR.rstrip('/'))

shutil.rmtree(WORK, ignore_errors=True)
os.makedirs(WORK + '/saves')
os.makedirs(WORK + '/rms')
FIFO = WORK + '/fifo'
os.mkfifo(FIFO)

flags = [f'-Daoe.tickms={MS}', '-Daoe.debug=1', '-Daoe.harnessQuiet=1',
         '-Daoe.exitOnResult=1', f'-Daoe.saveDir={WORK}/saves',
         f'-Daoe.rmsDir={WORK}/rms', '-Daoe.mapSeed=8224',
         f'-Daoe.devBoot={DIR}/base.aoesave', f'-Daoe.devMouse={FIFO}',
         '-Daoe.bfsPath=1',
         '-Daoe.headless=1']
cp = (f'{REPO}/build/classes/java/main:{REPO}/build/resources/main')
log = open(WORK + '/replay.log', 'wb')
proc = subprocess.Popen(['/opt/homebrew/opt/openjdk@17/bin/java'] + flags
                        + ['-cp', cp, 'aoe.Main'], stdout=log, stderr=log)
print(f'java pid={proc.pid}', flush=True)

# 提前握住 fifo 写端（reader 在游戏启动后 open；我们的 open 会等到那一刻）
fw = open(FIFO, 'w')
print('fifo 写端已连接', flush=True)

armed = False
sent = False
t0 = time.time()
while time.time() - t0 < 900:
    if proc.poll() is not None:
        print('java 已退出', flush=True)
        break
    try:
        with open(WORK + '/replay.log', 'r', errors='replace') as f:
            lines = f.readlines()
    except FileNotFoundError:
        continue
    if not armed:
        if any('[devBoot] done' in ln for ln in lines):
            armed = True
            print('devBoot done', flush=True)
    if armed and not sent:
        # devBoot done 出现即发（快照落地与 done 同拍；done 轮询粒度 10ms）
        fw.write(f'replaytrace {DIR}/trace.txt {BASE}\n')
        fw.flush()
        sent = True
        print('replaytrace 已发送', flush=True)
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
time.sleep(3)
proc.terminate()
# 终局与事件数对拍
n_fifo = n_input = 0
result = ''
with open(WORK + '/replay.log', 'r', errors='replace') as f:
    for ln in f:
        if ln.startswith('[fifo] ar='):
            n_fifo += 1
        elif ln.startswith('[input] ar='):
            n_input += 1
        elif '[result]' in ln:
            result = ln.strip()
print(f'对拍: applied fifo={n_fifo} input={n_input} | {result}', flush=True)
print(f'预期: {EXPECT}', flush=True)
print('VERDICT:', 'PASS' if result.replace('[result] ', '').strip() in EXPECT else 'FAIL', flush=True)
