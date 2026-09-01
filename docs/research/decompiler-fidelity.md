# 反编译器保真度实测报告(CFR vs Vineflower)——重编译字节码对拍

> 由子代理于 2026-09-01 生成(方法论/脚本可重放,见 tools/decomp-study/);
> 主会话已按其结论修复 AgeOfEmpires.c B() 的 CFR 伪影(见 WORKLOG 同日条目)。

# J2ME 反编译器保真度实测报告(CFR vs Vineflower)

日期:2026-08-31 / 工作区:`/tmp/decomp-study/`(repo 全程只读,未改动任何 repo 文件)

## 0. 一句话结论

- **CFR 0.152**:确认 **3 处语义失真(伪影)** + 1 处"表达式乱序(编译阻断+若强行编译则语义错误)"。
  最严重的是除已知 `G()` 之外新发现的 **`B()` 丢失循环出口 + 极性反转**,以及 **继承静态字段误解析(10 个使用点,若按 CFR 原样运行会抛 ClassCastException)**。
- **Vineflower 1.10.1**:**未发现静默语义伪影**。它的全部问题都是"响亮"的编译阻断或布局差异:
  继承静态字段引用不传播重命名(10 点)、有名内部类被折叠成非法匿名 `TimerTask`(1 点)、
  跨类陈旧引用(3 点)、short 局部类型化(2 点)。
- **G() 锚点按预期判定**:CFR 在循环出口处真实失真;VF 结构正确(其残差为我补 (short) cast 的 i2s,值域 [-63,63],语义无害)。
- **对 repo src 的影响**:repo 已修复 G()(注释记载了 2026-09-01 卡死)与字段误解析;
  但 **repo 的 `AgeOfEmpires/c.java` `B()` 仍原样携带 CFR 伪影 `if (!bl) continue;`**(见 §5.2),建议按 G() 同样方式补 `if (bl) break;`。

## 1. 方法论

```
原 jar (2005, major 46)
  ├─ javap -c -p / -p -s dump  →  dumps/orig*
  ├─ CFR 0.152 --renamedupmembers true        → dec-cfr  → javac --release 8 → build-cfr → dumps/cfr*
  │    └ e.class 因 InnerClasses 属性被整包跑吞掉,单独反编译补 e.java
  └─ Vineflower 1.10.1 --rename-members=true --user-renamer-class=VFMinRenamer
       (自定义 renamer:仅重命名"重复成员",类名与普通成员保持原名) → dec-vf → build-vf → dumps/vf*
       └ e.class 同样单独补
shim:repo 的 src/main/java/javax/microedition/** 原样拷贝;因 Image.java 用了 Java9+ 的
     readAllBytes(),shim 以默认 release 单独编译为 shim-classes,游戏树再以 --release 8 -cp 引用。
```

比较脚本 `scripts/compare.py`(226 个方法,按"类内方法序号+描述符"配对,描述符序列全程一致,0 异常):

| 级别 | 含义 | 归一化 |
|---|---|---|
| L1 exact | 逐指令一致 | 字段/方法引用按「描述符+声明类内序号」规范化(重命名不变量);沿继承链解析声明类 |
| L2 mnem | 助记符序列一致 | `istore_2`≡`istore 9`(槽位族归一),忽略布局/偏移 |
| L3 idiom | 惯用法归一后一致 | 老/新 javac 惯用法:if+goto 反演、goto-直落删除、`dup;Xstore_n`→`Xstore_n;Xload_n`、`iconst_0/1+if_icmp`→ifeq/ifne、`xload,push c,iadd, xstore`→iinc、iinc 只比增量、switch 只比匹配键、invokeinterface≡invokevirtual(shim 中 Player 为 class) |
| L4 MISMATCH | 以上皆否 | 真实不一致,人工裁决 |

辅助:`scripts/analyze.py`(difflib 相似度分层 + 跨引擎差分:单引擎 MISMATCH=最强伪影候选)、
`scripts/review.py`(对齐视图人工审查)。

## 2. 编译修复清单

### CFR(dec-cfr,修复脚本 `scripts/fix-cfr.sh`)

| # | 类别 | 数量 | 说明 |
|---|---|---|---|
| F0 | 工具配置 | - | 必须加 `--renamedupmembers true`(混淆器有同名同参不同返回值方法、同名异型字段,默认输出直接不可编译);`mad/e.java` 需单独反编译补齐 |
| F1 | 全限定名撞 package/class | 108 点 | CFR 写出 `AgeOfEmpires.b.x`,与同名 package/class 冲突;去前缀即可(机械) |
| F2 | **字段误解析(=伪影,见 §5.3)** | 10 点 | 把继承静态 `mad.a.a : Lcom/ulysseo/mad/b;` 误解析为 `c` 自己的实例字段 `a : LAgeOfEmpires/AgeOfEmpires;`(MIDlet),并以 `(com.ulysseo.mad.b)((Object)…)` 强转掩盖;按字节码改回 `var_com_ulysseo_mad_b_a` |
| F3 | 局部合并不可编译 | 1 处 | `Object object` 同时充当 byte[] 与类 a 实例,`object[0]` 非法;拆双局部 |
| F4 | byte/short 精度 | 5 处 | `-x`/赋值报 lossy;补显式 cast(原字节码含 i2b/i2s,忠实) |
| F5 | **表达式乱序(=伪影,见 §5.4)** | 1 处 | `b(II)Z`:CFR 文本先算 `n22=g[by+1]`(用旧 by)再 `by=g[n20++]`;按字节码重排 by→n21→n22 |

统计:131 个编译错误 → 修复 6 类后全绿,输出 10 个类。可直接编译率:**0%(默认输出)/ 需 F0 配置 + F1~F5**。

### Vineflower(dec-vf,修复脚本 `scripts/fix-vf.sh` + `scripts/_vf_*.py`)

| # | 类别 | 数量 | 说明 |
|---|---|---|---|
| VF0 | 工具配置 | - | VF 默认同样不处理重复成员;`--rename-members=true` 会连类名一起改,故实现 `VFMinRenamer`(接口 `IIdentifierRenamer`):仅对"重复成员"返回新名 `a_vfN`,其余与类名保持原名。注意:renamer 不能重命名"实现父类抽象方法"的重名方法(会破坏 override),需按继承链排除 |
| VF1 | 悬挂引用 | 10 点 | VF 对继承静态字段的 `c.a`/裸 `a` 引用**不传播重命名**,留下旧名;按字节码改回 `a_vf0`(等价于 CFR 的 F2,但 VF 的字段绑定意图是**对的**,只是名字没改;CFR 是绑错字段) |
| VF2 | 内部类折叠失真 | 1 处 | 有名内部类 `e` 被折叠成 `new TimerTask(this){…}`——`TimerTask` 无此构造器且捕获变量类型错;按字节码还原 `new e(this)` |
| VF3 | 跨类陈旧引用 | 3 点 | `e.java` 单独反编译时对 `mad.b` 重命名方法(`a_vf0/1/2`)引用未更新;按返回类型真值改回 |
| VF4 | short 精度 | 2 处 | 同 F4 |

统计:131→34→18 个错误,修复后全绿,输出 10 个类。

## 3. 三级统计(226 方法)

| 引擎 | exact | mnem(布局噪声) | idiom(编译器代差噪声) | MISMATCH(需裁决) |
|---|---|---|---|---|
| CFR | **115 (50.9%)** | 5 (2.2%) | 21 (9.3%) | **85 (37.6%)** |
| Vineflower | **102 (45.1%)** | 14 (6.2%) | 5 (2.2%) | **105 (46.5%)** |

逐类数字见运行 `python3 scripts/compare.py` 输出;明细在 `compare-results.json`。

### MISMATCH 的家族归类(人工裁决,190 个方法-实例 / 173 个不同方法)

绝大多数属于以下**良性"反编译源≠原始源(语义等价)+编译器代差"家族**,两引擎同受影响:

- **A. 分支/块重排**(switch-case 顺序、if/else 臂互换、块搬移):如 vf c:37 `k(II)`(88 键块移至尾部,switch 源码完好)、c:139 `d(II)`(247 键块)、c:35、c:54。
- **B. `dup2`/长运算栈形态**:原版共享栈上数组下标(`dup2`),反编译源改为逐次重算,两侧算术序列相同(如 c:94 `a(III)`、c:121;即任务提示的 long 宽度差异)。
- **C. 局部槽位/临时变量**:多 `astore/aload` 对、槽位重排、`astore` vs `pop`(catch 处理器未用异常:pop vs astore)、iinc_w 展开。
- **D. 条件赋值形态**:`x=(cond)?1:0` 现代单店 vs 老分支双店(如 c:5 `b()` 的 putfield/putstatic)。
- **E. 计数器位置**:`iinc` 提前/滞后(c:42、c:139)。
- **F. 分支极性/直落**:ifne↔ifeq、return↔goto(mad/b paint)。

抽样覆盖:小类全部逐一核过;单引擎 MISMATCH 43 条全部过目;双引擎低相似度(<0.85)前 20 过目;每家族抽查 2~3 例读原始字节码确认。**未逐条全检**(190 条中约 100 条中高相似度按家族归类),c:121 `a(II)` 的 VF 侧存在一处 `iconst_0`/`iconst_2` 错位对齐,已列入残余风险,建议后续做 CFG 级等价验证。

## 4. G() 锚点判定(流水线自检,通过)

- **CFR**:c:123 `G()` MISMATCH,首个 L3 差异 pos=45 `('ifgt',)` vs `('ifle',)` —— 循环体回边/出口结构与原版不符,即已知失真(裸 continue 丢出口)。**判定=真实伪影 ✓**
- **VF**:结构对齐(循环/break 部分与原版逐键吻合直到 pos=123),残差为 `istore` vs `i2s` —— VF 把局部变量类型化为 short,由我补的 `(short)` cast 产生 i2s;该值域为 `[(x&63)-y] ⊂ [-63,63]`,i2s 无语义影响。**判定=结构正确 ✓**

## 5. 确认的真实伪影清单

### 5.1 CFR:`G()` 丢失循环出口(已知锚点,独立复核确认)

原版字节码(c:123):`82: iinc 7,-1; 85: iload 7; 87: ifgt 48` —— ifgt 为假落到 90(出口检查 `iload 7; ifgt 98; goto 344`)。
CFR 渲染为 while 体末尾 `if (--n3 > 0) continue;` —— 两分支都回到循环条件,出口边丢失:窗口内没有 1000 态记录时永久自旋(repo 注释记载即 2026-09-01 首战卡死根因)。

### 5.2 CFR:`B()` 丢失循环出口 + 极性反转(新发现)

原版字节码(c:125)循环尾部:
```
173: iconst_1
174: istore_2            ← bl = true
175: iload_2
176: ifeq 34             ← bl==0 → 回 34(循环条件,继续扫)
179: iinc 1,1            ← bl!=0 → 落出内层 while(出口!)
182: goto 2
```
CFR 渲染 `… if (!bl) continue; }` → 重编译产物:
```
160: iload_2
161: ifne 167
164: goto 35             ← !bl → 条件
167: goto 35             ← bl  → 也是条件!(应为退出)
170: iinc 1,1            ← 成为死代码
```
**语义**:原版每次 `B()` 调用(每 tick、每玩家)最多处理一个满足条件的单位(bl 置真即终止扫描);CFR 版会扫完全部单位。**repo 的 `AgeOfEmpires/c.java` `B()`(约 6775-6791 行)仍原样携带此渲染**,建议改为 `if (bl) break;` 并以字节码对拍验证。

### 5.3 CFR:继承静态字段误解析(10 个使用点,4 个方法)

原版 `destroyApp`/`c.h`/`c.int_b`/`c.void_b` 中的 `getstatic AgeOfEmpires/c.a : Lcom/ulysseo/mad/b;`
(字段声明在超类 `com.ulysseo.mad.a`,类型=GameCanvas)被 CFR 解析为 `c` 自己的实例字段
`a : LAgeOfEmpires/AgeOfEmpires;`(类型=MIDlet),并插入 `(com.ulysseo.mad.b)((Object)…)` 强转使其"可编译"。
按 CFR 源语义,MIDlet 实例 → GameCanvas 的强转在运行时必抛 ClassCastException。
修复后(改回 `var_com_ulysseo_mad_b_a`)这 4 个方法在 L1 全部 **exact**,反向验证了修复正确。

### 5.4 CFR:`b(II)Z` 表达式乱序(编译阻断;若强行编译则语义错误)

原字节码顺序:`by=g[n20++]`(537)→ `n21=n3+g[by]`(539-548)→ `n22=n5+g[by+1]`(550-562)。
CFR 文本却把 `n22` 提到 `by` 赋值之前 —— 按源码语义会读到**上一轮的 by**;且 `by` 未赋值先用,现代 javac 直接报
"might not have been initialized"。已按原顺序重排(F5)。

### 5.5 Vineflower:确认伪影数 = **0**

所有 VF 问题均为编译阻断(响亮)或布局/类型差异(语义等价):
悬挂引用 10 点(VF1)、TimerTask 折叠 1 点(VF2)、e.java 陈旧引用 3 点(VF3)、short 类型化 2 点(VF4)。
单引擎 MISMATCH 榜上的 VF 条目(c:37/35/54/76/85/64/62/58/22 等)逐一核过,全部属于 §3 家族 A/C/F。

## 6. 对"我们的 src 派生自 CFR"的影响结论

| CFR 伪影 | repo src 现状 |
|---|---|
| G() 丢出口 | **已修复**(`aimProjectiles` 内 `if (--n3 <= 0) break;`,注释完整记载考证与 2026-09-01 卡死) |
| B() 丢出口+极性反转 | **未修复,仍在**(`final void B()` 内 `if (!bl) continue;`)。行为差异:每 tick 每玩家会处理所有满足条件的单位,原版只处理一个。建议按 G() 的方式补回 `if (bl) break;` |
| 继承静态字段误解析 | **已修复**(全库统一使用 `var_com_ulysseo_mad_b_a`,如 c.java:282/309、AoeMidlet.destroyApp) |
| b(II)Z 表达式乱序 | 编译必然迫使其解决;repo 6523 行把 `by` 提前为独立赋值,n21/n22 求值顺序与原版不同但均无副作用,**语义等价** |
| F3 类局部合并、F4 类精度 cast | 编译阻断类,建库时必然已处理 |

**最终判断:除已知的 G()(已修)外,repo src 还携带一处未修复的 CFR 语义伪影(B() 循环出口丢失),
未发现其他静默伪影。** 建议对 B() 补 `if (bl) break;`,并用本工作区的对拍管线回归。

## 7. 产物索引

| 路径 | 内容 |
|---|---|
| `REPORT.md` | 本报告 |
| `scripts/compare.py` | 三级对拍(L1/L2/L3/L4,重命名不变量规范化) |
| `scripts/analyze.py` / `scripts/review.py` | 相似度分层+跨引擎差分 / 对齐审查 |
| `scripts/fix-cfr.sh` / `scripts/fix-vf.sh`(+`_vf_*.py`) | 两引擎全部修复(可重放) |
| `VFMinRenamer.java` / `renamer-classes/` | VF 最小重命名器(仅重复成员) |
| `dup-table.json` | 原版重复成员表(生成 renamer 用) |
| `dec-cfr/` / `dec-vf/` | 修复后的可编译反编译树 |
| `build-cfr/` / `build-vf/` | 重编译产物(各 10 类) |
| `dumps/{orig,cfr,vf}{,-sig}/` | javap dump |
| `compare-results.json` / `mismatch-analysis.json` | 对拍明细与分层结果 |
| `logs/` | 各轮编译错误记录 |

复现:`export JAVA_HOME=/opt/homebrew/opt/openjdk@17; cd /tmp/decomp-study && python3 scripts/compare.py && python3 scripts/analyze.py`
