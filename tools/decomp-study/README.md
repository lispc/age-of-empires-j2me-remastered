# 反编译器保真度对拍工具(decompile → recompile → javap 归一化 diff)

用法与完整方法论见 `docs/research/decompiler-fidelity.md`。要点:
- 原 jar 反编译(CFR/Vineflower)→ `javac --release 8` + repo shim 编译回 class
  → `javap -c -p` 与原 class 逐方法对比(L1 exact / L2 助记符 / L3 惯用法 / L4 不一致)。
- `compare.py` 四级对拍;`analyze.py` 相似度分层+跨引擎差分;`review.py` 人工对齐视图。
- `fix-cfr.sh`/`fix-vf.sh`/`VFMinRenamer.java` 是编译通过所需的修复与最小重名重命名器。
- 原始工作区(含 dumps/build 树):/tmp/decomp-study/(临时)。
