#!/usr/bin/env python3
"""round-stats — 每轮试玩结束后的效率画像。

用法: python3 tools/round-stats.py <agent目录/transcript.jsonl>
输出: 工具调用分布 / Bash 分类 / FIFO 指令分布 / sleep 累计 / token 汇总。
用途: 发现时间黑洞（等待循环、重复操作），驱动流程改进（每轮战报附一份）。
"""
import json, sys, re
from collections import Counter

path = sys.argv[1]
tools = Counter(); bash_kinds = Counter(); n_model = 0
chars_model = 0; sleeps = 0.0; n_tools = 0
tok_in = tok_out = 0
fifo_cmds = Counter()
errors = 0
with open(path) as f:
    for line in f:
        try: obj = json.loads(line)
        except Exception: continue
        t = obj.get("type")
        u = obj.get("usage") or {}
        tok_in += u.get("input_tokens",0) or 0
        tok_out += u.get("output_tokens",0) or 0
        if t == "tool_call_scheduled":
            n_tools += 1
            p = obj.get("payload", {}); name = p.get("toolName",""); tools[name]+=1
            inp = p.get("input",{}) or {}
            if name == "Bash":
                cmd = (inp.get("command") or "")
                if "fifo" in cmd or "aoectl" in cmd:
                    bash_kinds["fifo操作"]+=1
                    m = re.findall(r'(state|save|load|ctile|key|tapk|rclick|click|drag|dump|fields|until|script|probe|strtbl|dlg|replaytrace|stopat|exit)\b', cmd)
                    for x in m: fifo_cmds[x]+=1
                elif "sleep" in cmd and ("grep" in cmd or "tail" in cmd): bash_kinds["等待+log轮询"]+=1
                elif "sleep" in cmd: bash_kinds["纯sleep等待"]+=1
                elif re.search(r'res\.py|parse\.py|probe\.py|python3', cmd): bash_kinds["python分析"]+=1
                elif "session.log" in cmd or "grep" in cmd or "tail" in cmd: bash_kinds["log挖掘"]+=1
                elif "BUGS" in cmd: bash_kinds["写BUGS"]+=1
                else: bash_kinds["其他"]+=1
                for s in re.findall(r'sleep\s+([\d.]+)', cmd): sleeps += float(s)
            elif name in ("Read","Write","Edit"): bash_kinds[name+"文件"]+=1
            elif "screenshot" in name.lower() or "zoom" in name.lower(): bash_kinds["截图"]+=1
        elif t == "model_complete":
            n_model += 1
            c = obj.get("payload",{}).get("content") or ""
            chars_model += len(c)
        elif t == "tool_call_failed": errors += 1

print(f"工具调用总数: {n_tools}")
print(f"按工具: {dict(tools.most_common(10))}")
print(f"Bash分类: {dict(bash_kinds.most_common(12))}")
print(f"FIFO指令分布: {dict(fifo_cmds.most_common(15))}")
print(f"显式sleep累计: {sleeps:.0f}s")
print(f"model_complete 次数: {n_model}, 思考文本总字符: {chars_model}")
print(f"tokens: in={tok_in/1e6:.1f}M out={tok_out/1e6:.1f}M")
print(f"工具失败次数: {errors}")
