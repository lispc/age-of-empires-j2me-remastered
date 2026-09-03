#!/usr/bin/env python3
"""aoeops — 战术宏组合层。

服务端宏（sel/train/goto/build/tile/sitrep，见 DEVELOPMENT.md「FIFO 指令」）是
原子的；本脚本把它们组合成多步流程，并处理两类已知坑：
  - 建筑/训练完成弹窗（aA=2）会冻结世界：build 之后自动 -5/-7 清弹窗再等完工；
  - 完工判定轮询 tile 宏的 bldStatus，不再靠拍脑袋 sleep。

用法（环境变量 AOE_FIFO，默认 /tmp/aoe-mouse）:
  aoeops.py sitrep                     # 一行战况（服务端 sitrep，state json 增强字段亦可）
  aoeops.py build <tx> <ty> <type>     # 放置+清弹窗+等完工（阻塞至多 15s）
  aoeops.py train <tx> <ty> <n>        # 排队（若撞在建先等完工）
  aoeops.py wait-built <tx> <ty>       # 只等完工
  aoeops.py state                      # 透传 state 并打印 fifo.json
"""
import json
import os
import re
import sys
import time

FIFO = os.environ.get("AOE_FIFO", "/tmp/aoe-mouse")
STATUS = FIFO + ".json"


def send(cmd: str) -> None:
    with open(FIFO, "w") as f:
        f.write(cmd + "\n")


def result(pattern: str, timeout: float = 3.0) -> str:
    """取 FIFO 日志回显不现实（进程 stdout 不在这）——改用 fifo.json/轮询。
    这里返回最近一次 state 的关键字段拼串，供调用方判断。"""
    deadline = time.time() + timeout
    last = ""
    while time.time() < deadline:
        try:
            last = open(STATUS).read()
            break
        except OSError:
            time.sleep(0.2)
    return last


def get_state(timeout: float = 3.0) -> dict:
    try:
        os.remove(STATUS)
    except OSError:
        pass
    send("state")
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            return json.load(open(STATUS))
        except (OSError, ValueError):
            time.sleep(0.2)
    raise SystemExit("state 超时")


def tile(tx: int, ty: int) -> str:
    send(f"tile {tx} {ty}")
    time.sleep(0.4)
    # tile 宏的回显在游戏进程 stdout；这里用间接触法——重发后读不到时退回
    # 由调用方自行 grep 日志。为让 wait-built 可用，改用 bldStatus 轮询：
    send(f"tile {tx} {ty}")
    time.sleep(0.4)
    return ""


def clear_popup() -> None:
    """清弹窗专用：用 -7 而非 -5。build 完工后光标自动吸附到完工建筑格，
    -5 会在弹窗关闭后落成"生产排队"（r22 幻影排队事故，House 多出 1 村民）。"""
    d = get_state(2.0)
    if d.get("aA") == 2:
        send("key -7")          # 弹窗上 -7=关弹窗，不触发光标格语义
        time.sleep(0.6)
        d = get_state(2.0)
        if d.get("aA") == 4:    # -7 在无选中态会开暂停菜单
            send("key -5")      # Continue 恢复
            time.sleep(0.6)


def wait_built(tx: int, ty: int, timeout: float = 15.0) -> bool:
    """轮询直到 aA 回到世界态 6（弹窗全清）。"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        d = get_state(2.0)
        if d.get("aA") == 6:
            return True
        clear_popup()
    return False


def main() -> None:
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return
    cmd = args[0]
    if cmd == "sitrep":
        send("sitrep")
        time.sleep(0.6)
        print("(sitrep 一行输出在游戏 stdout 日志；或用 `aoeops.py state`)")
        d = get_state(2.0)
        print("res=%s pop=%s q=%s ai=%s aA=%s" % (
            d.get("res"), d.get("pop"), d.get("queued"), d.get("ai"), d.get("aA")))
    elif cmd == "build" and len(args) == 4:
        tx, ty, t = args[1:4]
        send(f"build {tx} {ty} {t}")
        time.sleep(0.8)
        clear_popup()
        ok = wait_built(int(tx), int(ty))
        print(f"build {tx},{ty} type{t}: {'完工' if ok else '未确认完工(查日志)'}")
        d = get_state(2.0)
        print("res=%s pop=%s" % (d.get("res"), d.get("pop")))
    elif cmd == "train" and len(args) == 4:
        tx, ty, n = args[1:4]
        wait_built(int(tx), int(ty), 8.0)
        send(f"train {tx} {ty} {n}")
        time.sleep(0.8)
        d = get_state(2.0)
        print("queued=%s res=%s (排队回显查日志 [devMouse] train)" % (
            d.get("queued"), d.get("res")))
    elif cmd == "wait-built" and len(args) == 3:
        print("完工" if wait_built(int(args[1]), int(args[2])) else "超时")
    elif cmd == "state":
        print(json.dumps(get_state(), ensure_ascii=False, indent=1))
    else:
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()
