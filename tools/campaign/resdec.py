#!/usr/bin/env python3
"""res 112 (m2 战役脚本) 解码器。
解释器语义 = src/main/java/AgeOfEmpires/c.java tickMissionScript/evalScriptCondition/
runScriptActions/skipScriptBlock/skipScriptActions（2026-09-04 直读）。
块结构 = [条件opcode][条件args][动作...][126]；块首字节<0 = 已执行标记。
条件 opcode:
  1=slot在矩形(+闲置): args invert,player,slot,x,y,w,h (7)
  2=计时: args blkIdx,val (2)  真值: frames[blk] >= val*10
  3=screenState==v: args v (1)
  4=选中检查: args 5
  5=数单位: args a,b,match (3)  int_b(a,b) 带 match==0 → type<2 (村民)
  6=techFlags[i]==v: args i,v (2)
  7=headers比较: args player,idx,op(0==/1>/2<),val (4)
动作 opcode:
  0=briefing(player,I,variant) 2 args | 1=enable blk 1 | 2=disable blk 1
  3=清计时 blk 1 | 4=WIN 1 | 5=LOSS 1 | 6=a(...,false) 4 | 7=move单位 4
  8=set headers[p][i]=v 3 | 9=set techFlags[i]=v 2 | 126=end
"""
import struct, sys

PATH = '/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered/src/main/resources/res/data.res'

def entry(n):
    d = open(PATH, 'rb').read()
    off = struct.unpack_from('>i', d, 4 + n * 4)[0]  # mode1: 绝对文件偏移
    ln = struct.unpack_from('>i', d, off)[0]
    return d[off + 4: off + 4 + ln]

COND_LEN = {1: 7, 2: 2, 3: 1, 4: 5, 5: 3, 6: 2, 7: 4}
ACT_LEN = {0: 2, 1: 1, 2: 1, 3: 1, 4: 1, 5: 1, 6: 4, 7: 4, 8: 3, 9: 2}

def dec(bs):
    def sgn(b):  # Java byte 语义：>=128 → 负（已执行标记 = 取负写入）
        return b - 256 if b > 127 else b
    i = 0
    blocks = []
    while i < len(bs) and bs[i] != 127:
        start = i
        op = sgn(bs[i])
        neg = op < 0
        opc = -op if neg else op
        i += 1
        args = list(bs[i:i + COND_LEN.get(opc, 0)])
        i += COND_LEN.get(opc, 0)
        acts = []
        while i < len(bs):
            a = bs[i]; i += 1
            if a == 126:
                break
            n = ACT_LEN.get(a)
            if n is None:
                acts.append((a, [999])); break
            acts.append((a, list(bs[i:i+n]))); i += n
        blocks.append(dict(idx=len(blocks), off=start, opc=opc, exec=neg,
                           args=args, acts=acts))
    return blocks

def hh(bs):
    return ' '.join('%02x' % (b & 0xFF) for b in bs)

def show(res, label):
    bs = entry(res)
    print(f'== res {res} ({label}) len={len(bs)} hex={hh(bs[:64])}...')
    for b in dec(bs):
        ex = '已执行' if b['exec'] else '待命'
        s = f"blk{b['idx']} @{b['off']} 条件op{b['opc']} args={[x & 0xFF for x in b['args']]} [{ex}]"
        for a, av in b['acts']:
            anames = {0: 'briefing', 1: 'enable', 2: 'disable', 3: '清计时',
                      4: 'WIN(98,0)', 5: 'LOSS(98,1)', 6: 'a()', 7: 'move',
                      8: 'set hdr', 9: 'set tech'}
            s += f"\n    → op{a} {anames.get(a, '?')} {[x & 0xFF for x in av]}"
        print(s)

if __name__ == '__main__':
    show(111, 'm1 校准基准')
    print()
    show(112, 'm2 目标')
