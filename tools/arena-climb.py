#!/usr/bin/env python3
"""arena-climb.py — 自对弈竞技场第 7 轮：旋钮空间坐标下降自动爬山。

基因型 = RuleBasedAi 自消费旋钮的 curated 14 维子集（默认值 = G0 基因）。
适应度 = tools/ailoop.sh 镜像批 candidate 合并胜场（DRAW 计半，满分 20）：
  champion = 当前基因型（EXTRA_D 透传为双侧 base 属性），
  candidate = 当前基因型改一个基因（ailoop -m <gene>=<value> 按侧覆盖）。
  镜像批同种子 candidate 在 p0/p1 各一局，-Daoe.arena=1 自动带。

搜索 = 坐标下降：每轮对每个基因试 ±步长，得分 ≥ 11.5（镜像自对弈平局基准
10.0 + 噪声边际 1.5，第 7 轮实证 >10.0 会采纳噪声点且复测全军覆没）才采纳
最高分的值；一轮无改进或预算耗尽自停，写 summary。

确定性：同种子同相位 pin（ailoop PHASE_STEP 默认 7）下同一评估命令结果确定，
评估结果按 (基因型, 基因, 候选值) 缓存进 state.json，杀掉重启自动续跑
（已评估的组合直接命中缓存不重跑）。

用法：
  python3 tools/arena-climb.py                      # 正式搜索（默认 75 分钟预算）
  python3 tools/arena-climb.py --minutes 90 --workdir /tmp/arena-r7
  python3 tools/arena-climb.py --games 1 --max-evals 2 --workdir /tmp/arena-r7-smoke  # 冒烟
输出：stdout 每次评估一行（重定向到 climb.log）；workdir/state.json 断点状态；
  workdir/summary.txt 收尾摘要。
依赖：仅标准库。JDK17 经 PATH 前缀注入（ailoop 的 /opt/homebrew java 在本机
  不存在，回落 PATH 的 java——必须保证是 17）。
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# (基因名, 默认=G0, 步长, 下界, 上界) —— 默认值与 RuleBasedAi 构造器 propInt 一致
GENES = [
    ("aiK.milVills",  3,  1,  1,   8),   # 军事生产村民数下限门
    ("aiK.meleeW",   15,  5,  0,  40),   # 近战木门
    ("aiK.meleeG1",  25,  5,  5,  50),   # 近战金门（兵营 1 封建前）
    ("aiK.archW",    25,  5,  5,  50),   # 弓兵木门
    ("aiK.towerW",   22,  4,  6,  42),   # 塔门木
    ("aiK.towerG",    6,  3,  0,  21),   # 塔门金
    ("aiK.towerS",   20,  4,  4,  36),   # 塔门石（G1=20，第 10 轮转正）
    ("aiK.towerCap",  5,  1,  1,   8),   # 塔数量上限
    ("aiK.smithW",   25,  5,  5,  50),   # 铁匠铺门木
    ("aiK.smithS",   15,  5,  0,  40),   # 铁匠铺门石
    ("aiK.t8w",      25,  5,  5,  50),   # 投石机门木
    ("aiK.t8g",      25,  5,  0,  50),   # 投石机门金
    ("aiK.bowsawW",  30,  8,  6,  62),   # Bow Saw 研究木门
    ("exm.focusD2",  64, 16, 16, 144),   # 防御集火候选距离门²
]
GENE_NAMES = [g[0] for g in GENES]

EVEN_SCORE = 10.0        # 镜像自对弈平局基准（candidate==champion 时恒 10.0/20）
# 采纳边际（2026-09-07 第 7 轮定稿）：n=20 镜像批噪声带 ±1.5-2 分，>10.0 即
# 采纳会把噪声当改进——首轮实证：meleeW 15→20(10.5) 与 towerG 6→9(11.0) 两个
# 采纳点独立复测（双侧 EXTRA_D 分批，合计 40 局）candidate 仅 21.0/40=52.5%
# <28/40 采纳线，全是噪声。边际提到 +1.5（≥11.5 才采纳）；产出仍须协议复测。
ADOPT_SCORE = 11.5
EVAL_TIMEOUT = 1500      # 单次 ailoop 批的墙钟超时（秒；历史批 2-4 分钟，6 倍余量）
JDK17 = "/tmp/jdk17/bin"


def log(msg):
    print(time.strftime("[%H:%M:%S]") + " " + msg, flush=True)


def state_path(wd):
    return os.path.join(wd, "state.json")


def default_genotype():
    return {name: default for name, default, *_ in GENES}


def load_state(wd):
    p = state_path(wd)
    if os.path.exists(p):
        with open(p) as f:
            return json.load(f)
    return {
        "genotype": default_genotype(),
        "cache": {},        # key -> {score,w,l,d,s,ticks,rundir}
        "history": [],      # 每次评估一条
        "adoptions": [],    # 采纳记录
        "round": 0,
        "evals": 0,
        "started": time.strftime("%Y-%m-%d %H:%M:%S"),
    }


def save_state(wd, st):
    tmp = state_path(wd) + ".tmp"
    with open(tmp, "w") as f:
        json.dump(st, f, ensure_ascii=False, indent=1)
    os.replace(tmp, state_path(wd))


def cache_key(genotype, gene, value, games, seed0):
    """评估的完整决定因素：champion 基因型（EXTRA_D 双侧 base）+ candidate
    按侧覆盖 (gene=value) + 种子集。同 key 的 ailoop 命令逐字节相同 → 结果确定。"""
    material = json.dumps(
        [[genotype[n] for n in GENE_NAMES], gene, value, games, seed0],
        sort_keys=True)
    return hashlib.sha1(material.encode()).hexdigest()[:16]


RE_MERGE = re.compile(
    r"candidate 合并: ([\d.]+)/(\d+)\s*\(胜 (\d+) 负 (\d+) 和 (\d+) 僵持 (\d+)\)")
RE_TICKS = re.compile(r"ticks: 平均 (\d+)")
RE_RUNDIR = re.compile(r"rundir: (\S+)")


def run_eval(genotype, gene, value, games, seed0):
    """跑一次镜像批，返回 dict(score,w,l,d,s,ticks,rundir) 或 None（失败/超时）。"""
    extra = " ".join("-Daoe.%s=%d" % (n, genotype[n]) for n in GENE_NAMES)
    env = dict(os.environ)
    env["EXTRA_D"] = extra
    if os.path.isdir(JDK17):
        env["PATH"] = JDK17 + ":" + env.get("PATH", "")
    cmd = ["tools/ailoop.sh", "-m", "%s=%d" % (gene, value),
           "-n", str(games), "-d", "2", "-k", "-b"]
    t0 = time.time()
    try:
        proc = subprocess.run(cmd, cwd=REPO, env=env, capture_output=True,
                              text=True, timeout=EVAL_TIMEOUT)
        out = proc.stdout + proc.stderr
    except subprocess.TimeoutExpired:
        log("  !! ailoop 超时（%ds）: %s=%d" % (EVAL_TIMEOUT, gene, value))
        return None
    wall = time.time() - t0
    m = RE_MERGE.search(out)
    if not m:
        log("  !! 解析失败（rc=%d）：%s" % (proc.returncode, out[-400:]))
        return None
    ticks = RE_TICKS.search(out)
    rundir = RE_RUNDIR.search(out)
    return {
        "score": float(m.group(1)),
        "n": int(m.group(2)),
        "w": int(m.group(3)), "l": int(m.group(4)),
        "d": int(m.group(5)), "s": int(m.group(6)),
        "ticks": int(ticks.group(1)) if ticks else -1,
        "rundir": rundir.group(1) if rundir else "?",
        "wall": int(wall),
    }


def candidates_for(gene, cur):
    for name, _d, step, lo, hi in GENES:
        if name == gene:
            vals = []
            for v in (cur + step, cur - step):
                if lo <= v <= hi and v != cur:
                    vals.append(v)
            return vals
    return []


def finalize(wd, st, reason):
    gt = st["genotype"]
    lines = [
        "arena-climb 收尾（%s）" % reason,
        "开始: %s  结束: %s" % (st["started"], time.strftime("%Y-%m-%d %H:%M:%S")),
        "评估次数: %d  轮次: %d" % (st["evals"], st["round"]),
        "",
        "最终基因型（= champion 候选，-D 形式）:",
        "  " + " ".join("-Daoe.%s=%s" % (n, gt[n]) for n in GENE_NAMES),
        "",
        "采纳史:",
    ]
    for a in st["adoptions"]:
        lines.append("  R%d %s %s -> %s (score %.1f)" % (
            a["round"], a["gene"], a["old"], a["new"], a["score"]))
    lines.append("")
    lines.append("评估历史（gene old->new score w-l-d-s ticks rundir）:")
    for h in st["history"]:
        lines.append("  R%d %s %s->%s %.1f %d-%d-%d-%d ticks=%s %s%s" % (
            h["round"], h["gene"], h["old"], h["new"], h["score"],
            h.get("w", -1), h.get("l", -1), h.get("d", -1), h.get("s", -1),
            h.get("ticks", "?"), h.get("rundir", "?"),
            " ADOPTED" if h.get("adopted") else ""))
    text = "\n".join(lines) + "\n"
    with open(os.path.join(wd, "summary.txt"), "w") as f:
        f.write(text)
    log("==== 收尾（%s）====" % reason)
    print(text, flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--workdir", default="/tmp/arena-r7")
    ap.add_argument("--minutes", type=float, default=75,
                    help="墙钟预算，到点自动收尾写 summary")
    ap.add_argument("--games", type=int, default=10,
                    help="ailoop -n（种子数；镜像批局数=2×）")
    ap.add_argument("--seed0", type=int, default=1000)
    ap.add_argument("--max-evals", type=int, default=0,
                    help="评估次数上限（0=不限；冒烟用）")
    args = ap.parse_args()

    wd = args.workdir
    os.makedirs(wd, exist_ok=True)

    # 单实例锁（防 nohup 重复启动双跑同一 state）
    lock = os.path.join(wd, "climb.lock")
    try:
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        os.write(fd, str(os.getpid()).encode())
        os.close(fd)
    except FileExistsError:
        with open(lock) as f:
            old = f.read().strip()
        if old and os.path.exists("/proc/%s" % old):
            print("已有实例在跑（pid %s），退出" % old, file=sys.stderr)
            sys.exit(2)
        os.remove(lock)  # 陈旧锁（被杀的进程），接管
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        os.write(fd, str(os.getpid()).encode())
        os.close(fd)

    st = load_state(wd)
    t_start = time.time()
    budget = args.minutes * 60
    log("arena-climb 启动: workdir=%s 预算=%.0fmin games=%d 评估=%d(已做) 基因型=%s" % (
        wd, args.minutes, args.games, st["evals"],
        ",".join("%s=%s" % (n, st["genotype"][n]) for n in GENE_NAMES)))

    stop = False
    try:
        while not stop:
            st["round"] += 1
            improved = False
            for name, _d, _step, _lo, _hi in GENES:
                cur = st["genotype"][name]
                best_val, best_score, best_res = None, ADOPT_SCORE, None
                for value in candidates_for(name, cur):
                    if time.time() - t_start > budget:
                        stop = True
                        break
                    if args.max_evals and st["evals"] >= args.max_evals:
                        stop = True
                        break
                    key = cache_key(st["genotype"], name, value,
                                    args.games, args.seed0)
                    hit = st["cache"].get(key)
                    if hit:
                        res = hit
                        src = "CACHE"
                    else:
                        res = run_eval(st["genotype"], name, value,
                                       args.games, args.seed0)
                        src = "RUN"
                        if res is not None:
                            st["cache"][key] = res
                    st["evals"] += 1
                    if res is None:
                        log("R%d %-14s %s->%s 评估失败，跳过" % (
                            st["round"], name, cur, value))
                        st["history"].append({
                            "round": st["round"], "gene": name, "old": cur,
                            "new": value, "score": -1, "src": src})
                        save_state(wd, st)
                        continue
                    log("R%d %-14s %s->%s score=%.1f/20 (%d-%d-%d%s) ticks=%s %s [%s]%s" % (
                        st["round"], name, cur, value, res["score"],
                        res["w"], res["l"], res["d"],
                        "+%d僵" % res["s"] if res["s"] else "",
                        res["ticks"], res["rundir"], src,
                        " *best" if res["score"] > best_score else ""))
                    st["history"].append({
                        "round": st["round"], "gene": name, "old": cur,
                        "new": value, "score": res["score"], "src": src,
                        "w": res["w"], "l": res["l"], "d": res["d"],
                        "s": res["s"], "ticks": res["ticks"],
                        "rundir": res["rundir"]})
                    if res["score"] > best_score:
                        best_val, best_score, best_res = value, res["score"], res
                    save_state(wd, st)
                if stop:
                    break
                if best_val is not None:
                    st["genotype"][name] = best_val
                    improved = True
                    st["adoptions"].append({
                        "round": st["round"], "gene": name,
                        "old": cur, "new": best_val, "score": best_score})
                    for h in reversed(st["history"]):
                        if h["gene"] == name and h["new"] == best_val \
                                and h["round"] == st["round"]:
                            h["adopted"] = True
                            break
                    log("R%d 采纳 %s: %s -> %s (score %.1f)" % (
                        st["round"], name, cur, best_val, best_score))
                    save_state(wd, st)
            save_state(wd, st)
            if stop:
                finalize(wd, st, "预算/上限耗尽")
                break
            if not improved:
                finalize(wd, st, "一轮无改进（局部最优）")
                break
            log("==== R%d 结束，基因型=%s" % (st["round"], " ".join(
                "%s=%s" % (n, st["genotype"][n]) for n in GENE_NAMES)))
    finally:
        try:
            os.remove(lock)
        except OSError:
            pass


if __name__ == "__main__":
    main()
