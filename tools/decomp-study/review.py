#!/usr/bin/env python3
"""对指定 (cls, idx) 打印两引擎/orig 的 L3 对齐差异概览"""
import sys, json, difflib, importlib.util
spec=importlib.util.spec_from_file_location('cmp','/tmp/decomp-study/scripts/compare.py')
cmp=importlib.util.module_from_spec(spec); spec.loader.exec_module(cmp)
parsed={}
for c in cmp.CLASSES:
    parsed[c]={s:cmp.parse_code(f'/tmp/decomp-study/dumps/{s}/{c}.txt') for s in ['orig','cfr','vf']}
r=json.load(open('/tmp/decomp-study/compare-results.json'))['result']
def show(c, idx):
    print('#'*110)
    mo=parsed[c]['orig'][idx]
    print(f"{c} idx={idx} {mo['sig']}")
    for s in ('cfr','vf'):
        me=parsed[c][s][idx]
        k0=[str(x) for x in cmp.l3_keys(cmp.idiom_norm(mo['instrs']))]
        k1=[str(x) for x in cmp.l3_keys(cmp.idiom_norm(me['instrs']))]
        sm=difflib.SequenceMatcher(None,k0,k1,autojunk=False)
        print(f"--- {s}: ratio={sm.ratio():.3f} orig={len(k0)} eng={len(k1)} bucket={ [x for x in r[c][s] if x['idx']==idx][0]['bucket'] }")
        ops=[o for o in sm.get_opcodes() if o[0]!='equal']
        for tag,i1,i2,j1,j2 in ops[:6]:
            frag0=' '.join(k0[i1:min(i2,i1+12)])
            frag1=' '.join(k1[j1:min(j2,j1+12)])
            print(f"   {tag:8s} orig[{i1}:{i2}]{(' '+frag0)[:150]}")
            print(f"   {'':8s} {s}[{j1}:{j2}]{(' '+frag1)[:150]}")
        if not ops: print('   (identical at L3)')
for arg in sys.argv[1:]:
    c,idx=arg.rsplit(':',1); show(c,int(idx))
