#!/usr/bin/env python3
"""r16 boot3 热修：驱动 v7.1-p 无 BS 建造支路（双 Mill 假设被引擎证伪：
build FAIL type5 已建成过(不可重复)），城堡门 mills+bss>=2 不可达。
本脚本经 FIFO 放一座 BS(type6, 25W/20S)——放格即返回，其余全部交给
在跑的 drv3（城堡速攻支路自动接手：age==1 且 mills+bss>=2 且 20/20/20 →
研究舞步 → 城堡 → techFlags[14] → Univ → WIN）。
红线自查：不碰仓库/存档/rms；只发 build 指令；放格成功或资源长期不足即退。"""
import json
import os
import subprocess
import sys
import time

W = '/tmp/aoe-camp/m4p'
FIFO = W + '/fifo'
AISTATE = W + '/fifo.aistate.json'
BS_CANDS = [(43, 55), (45, 57), (40, 58)]   # 驱动 BS_CANDS 同款（全偶和）
TC = (43, 57)


def fifo(c, wait=0.5):
    subprocess.run(['sh', '-c', f"echo '{c}' > {FIFO}"], check=True, timeout=5)
    time.sleep(wait)


def aistate():
    for _ in range(6):
        try:
            fifo('aistate', 0.35)
            return json.load(open(AISTATE))
        except Exception:
            time.sleep(0.5)
    raise RuntimeError('aistate 无响应')


def build_fb(cands, btype, tag):
    for (x, y) in cands:
        if (x + y) % 2 != 0:
            continue
        fifo(f'build {x} {y} {btype}', 0.6)
        # 放格判定：buildingRecs 出现 uc=True 的该类型
        a = aistate()
        for b in a['buildingRecs']:
            if b['p'] == 0 and b['type'] == btype:
                print(f'BS OK ({x},{y}) t{btype} [{tag}] res='
                      f"{a['players'][0]['res']}", flush=True)
                return (x, y)
        print(f'  build FAIL ({x},{y}) t{btype}', flush=True)
    return None


def main():
    deadline = time.time() + 1500
    while time.time() < deadline:
        try:
            a = aistate()
        except Exception:
            # 宿主 java 退出后 fifo 无读者、echo 必超时——静默续等同
            # reseat-miners（r57：赛后自毁 traceback 是日志噪声不是故障）
            time.sleep(3)
            continue
        p0 = a['players'][0]
        Wq, Gq, Sq = p0['res']
        brecs = [b for b in a['buildingRecs'] if b['p'] == 0]
        bs = [b for b in brecs if b['type'] == 6]
        mills = [b for b in brecs if b['type'] == 5 and not b['uc']]
        if bs:
            print(f'BS 已存在（uc={bs[0]["uc"]}），退出', flush=True)
            return
        if p0['age'] >= 1 and mills and Wq >= 25 and Sq >= 20:
            print(f"放 BS res={p0['res']} age={p0['age']} mills={len(mills)}",
                  flush=True)
            if build_fb(BS_CANDS, 6, 'BS-hotfix'):
                # 等完工（进展 uc 清除），最长 15 分钟，只为日志完整
                for _ in range(180):
                    time.sleep(5)
                    a2 = aistate()
                    bs2 = [b for b in a2['buildingRecs']
                           if b['p'] == 0 and b['type'] == 6]
                    if bs2 and not bs2[0]['uc']:
                        print('== BS 完工：城堡门 mills+bss>=2 达成 ==',
                              flush=True)
                        return
                    if a2.get('aA') == 2:
                        fifo('key -6', 0.5)
                return
        time.sleep(2)
    print('BS 热修超时退出（资源未凑齐或门未开）', flush=True)


if __name__ == '__main__':
    main()
