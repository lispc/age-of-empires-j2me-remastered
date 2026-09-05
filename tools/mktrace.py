#!/usr/bin/env python3
"""会话日志 → tick 锚定回放 trace（replaytrace 用）。

输入: 游戏会话 stdout（-Daoe.debug=1），其中
  [input] ar=<T> key <K>      —— 键事件（onKeyPress 打点）
  [fifo]  ar=<T> <命令行>     —— FIFO 指令应用瞬间（devMouseCmd 入口打点）
输出: replaytrace trace 文件，行格式 `t <相对tick> key|move|fifo ...`，
相对 tick 原点 = base（通常为 base 存档的 ar，见 [save] wrote ... ar=N）。

控制流/只读指令不进 trace（replaytrace 侧也拒绝它们）：
  replaytrace script save load stopat exit until（控制流/嵌套）
  state ping fields dump aistate sitrep strtbl dlg probe count tile（只读诊断）
  tapk（重试次数依赖墙钟读回，天然不确定——录制对局禁用）

用法: mktrace.py <session.log> <baseTick> <out.trace> [--until RESULT|tag]
  --until RESULT: 截到 [result] 行为止（不含之后）
"""
import argparse
import re
import sys

EXCLUDE = {
    "replaytrace", "script", "save", "load", "stopat", "exit", "until",
    "state", "ping", "fields", "dump", "aistate", "sitrep", "strtbl",
    "dlg", "probe", "count", "tile", "tapk", "slots", "taskinfo",
}
RE_FIFO = re.compile(r"^\[fifo\] ar=(\d+) (.+)$")
RE_INPUT = re.compile(r"^\[input\] ar=(\d+) key (-?\d+)$")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("log")
    ap.add_argument("base", type=int)
    ap.add_argument("out")
    ap.add_argument("--until", default=None,
                    help="截断标记子串（如 '[result]'），该行本身及其后不进 trace")
    a = ap.parse_args()

    events = []  # (absTick, order, line)
    order = 0
    seen_key = set()  # (absTick, cmd)：同一次按键走 [fifo]+[input] 两路各打一行，
    #                   不去重则 trace 里每个 key 成双、回放多按一次（r56 定案）
    with open(a.log, encoding="utf-8", errors="replace") as f:
        for ln in f:
            ln = ln.rstrip("\n")
            if a.until and a.until in ln:
                break
            m = RE_INPUT.match(ln)
            if m:
                t, cmd = int(m.group(1)), f"key {m.group(2)}"
            else:
                m = RE_FIFO.match(ln)
                if not m:
                    continue
                t, rest = int(m.group(1)), m.group(2).strip()
                if rest.split(" ", 1)[0] in EXCLUDE:
                    continue
                cmd = rest
            if t < a.base:
                continue
            if cmd.startswith("key "):
                k = (t, cmd)
                if k in seen_key:
                    continue
                seen_key.add(k)
            events.append((t, order, cmd))
            order += 1

    with open(a.out, "w", encoding="utf-8") as w:
        w.write(f"# base={a.base} events={len(events)}\n")
        last = -1
        for t, _, cmd in events:
            rel = t - a.base
            if rel < last:
                print(f"warn: tick 倒退 {last}->{rel}（load?）行丢弃: {cmd}",
                      file=sys.stderr)
                continue
            last = rel
            # key/move 是 replaytrace 原生 op；其余命令走 fifo op 重放
            op = cmd if cmd.startswith(("key ", "move ")) else f"fifo {cmd}"
            w.write(f"t {rel} {op}\n")
    print(f"mktrace: {len(events)} events -> {a.out} (base={a.base})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
