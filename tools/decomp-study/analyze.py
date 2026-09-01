#!/usr/bin/env python3
"""对 MISMATCH 方法做相似度分层 + 跨引擎差分"""
import json, difflib, importlib.util
spec=importlib.util.spec_from_file_location('cmp','/tmp/decomp-study/scripts/compare.py')
cmp=importlib.util.module_from_spec(spec); spec.loader.exec_module(cmp)

parsed={}
for c in cmp.CLASSES:
    parsed[c]={s:cmp.parse_code(f'/tmp/decomp-study/dumps/{s}/{c}.txt') for s in ['orig','cfr','vf']}

r=json.load(open('/tmp/decomp-study/compare-results.json'))['result']
rows=[]
for c in cmp.CLASSES:
    for s in cmp.SIDES:
        for d in r[c][s]:
            if d['bucket']!='MISMATCH': continue
            idx=d['idx']
            mo=parsed[c]['orig'][idx]; me=parsed[c][s][idx]
            k0=cmp.l3_keys(cmp.idiom_norm(mo['instrs']))
            k1=cmp.l3_keys(cmp.idiom_norm(me['instrs']))
            sm=difflib.SequenceMatcher(None,[str(x) for x in k0],[str(x) for x in k1])
            # 另一引擎在同一方法的桶
            other='vf' if s=='cfr' else 'cfr'
            od=[x for x in r[c][other] if x['idx']==idx][0]
            rows.append({'cls':c,'engine':s,'idx':idx,'sig':d['sig'],'ratio':round(sm.ratio(),3),
                         'other_bucket':od['bucket'],
                         'n0':len(k0),'n1':len(k1)})
rows.sort(key=lambda x:(x['other_bucket']=='exact', x['ratio']))
import collections
print('=== 单引擎 MISMATCH(另一引擎 exact/mnem/idiom)= 最强伪影候选 ===')
single=[x for x in rows if x['other_bucket']!='MISMATCH']
for x in single:
    print(f"  {x['engine']:4s} {x['cls']}:{x['idx']} ratio={x['ratio']} other={x['other_bucket']} {x['sig'][:60]}")
print(f"\n=== 双引擎都 MISMATCH,按相似度升序(低=结构差异大) ===")
both=sorted([x for x in rows if x['other_bucket']=='MISMATCH'],key=lambda x:x['ratio'])
buckets=collections.Counter()
for x in both:
    b='<0.7' if x['ratio']<0.7 else ('<0.85' if x['ratio']<0.85 else ('<0.95' if x['ratio']<0.95 else '>=0.95'))
    buckets[b]+=1
print(' 相似度分布:',dict(buckets))
for x in both:
    if x['ratio']<0.85:
        print(f"  {x['engine']:4s} {x['cls']}:{x['idx']} ratio={x['ratio']} len {x['n0']}v{x['n1']} {x['sig'][:60]}")
json.dump(rows,open('/tmp/decomp-study/mismatch-analysis.json','w'),indent=1)
print(f"\ntotal mismatch rows: {len(rows)} (single-engine: {len(single)})")
