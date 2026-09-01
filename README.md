# Age of Empires II (J2ME) — macOS 桌面移植

从 `age_of_empires_ii_240x320-9174.jar`（In-Fusio 2005，240x320）反编译并移植到
桌面 Swing 的版本。原始代码是混淆过的字节码，本仓库的源码由 CFR 反编译后人工修正，
处于"能编译、能运行、可读性逐步改进"的状态。

> **开发状态与调试设施见 `DEVELOPMENT.md`；玩家操作见 `USER-GUIDE.md`。**

## 构建与运行

```sh
cd aoe-desktop
./run.sh                      # 推荐：自动修正 JAVA_HOME
gradle run                    # 或直接用 gradle（需要 JDK 17，见 gradle.properties）
gradle installDist            # 生成 build/install/aoe-desktop/ 可分发目录
```

调试开关（JVM 参数）：

- `-Daoe.scale=N`：窗口放大倍数（不指定时按屏幕可用区域自适应，取能完整显示的
  最大倍数、上限 3；这台 MacBook 上自动选 2）
- `-Daoe.width=N` / `-Daoe.height=N`：逻辑分辨率（默认 240x320 原版尺寸；
  `run.sh` 默认传 720 宽 = 宽视野。游戏渲染/镜头/UI 全部按该尺寸自适应）
- `-Daoe.debug=1`：打印游戏状态机、被吞掉的异常
- `-Daoe.dumpFrames=/tmp/frame.png`：每 ~5 秒把当前画面导出为 PNG

## 目录结构

- `src/main/java/AgeOfEmpires/` — 游戏本体（反编译源码）
  - `AoeMidlet.java` — 入口 MIDlet（原类名 `AgeOfEmpires`，因与包名冲突改名）
  - `c.java` — 游戏主类（~6900 行，状态机 + 渲染 + 逻辑全在这里）
  - `b.java` — 工具类：sin 表、自定义图片格式→PNG 解码器、位读写器、MIDI 播放
  - `d.java` — 地图生成器（64x64 程序化地形）
  - `a.java` — 字符串表读取器（从 data.res 读 UTF-8 文本）
- `src/main/java/com/ulysseo/mad/` — In-Fusio 的 J2ME 游戏框架
  - `a.java` — 抽象游戏控制器（按键/绘制/定时器回调）
  - `b.java` — GameCanvas：双缓冲 + Timer 主循环驱动
  - `c.java` — data.res 资源包读取器（索引 + 长度前缀 blob）
  - `d.java` — drawRegion 越界保护
  - `e.java` — 主循环 TimerTask（每 80ms 一帧）
- `src/main/java/javax/microedition/` — J2ME API 的 Swing 适配层（薄 shim）
  - `lcdui/` — Graphics/Image/Font/Canvas/GameCanvas/Display 等
  - `rms/` — RecordStore → `~/.aoe-desktop/*.rms` 文件存档
  - `media/` — MIDI 播放（javax.sound.midi）
  - `midlet/` — MIDlet 桩
- `src/main/resources/` — 游戏资源（data.res、MIDI、图标），原样取自 jar
- `decompiled/` — CFR 的原始反编译输出，仅作参考；**以 src/ 为准**
- `tools/cfr.jar` — 反编译器
- `docs/game-mechanics.md` — 游戏机制知识地图（主循环/按键模型/状态机/菜单模板/存档布局/资源索引），
- `docs/symbols.md` — 混淆名→可读名符号字典，
  读 `c.java` 前先看它；单点机制细节以代码现场注释为准

## 渲染模型

游戏以逻辑分辨率（默认 240x320，`run.sh` 传 `-Daoe.width=720` 宽视野）画进一块
**设备分辨率的持久帧缓冲**（逻辑尺寸 × `aoe.scale` × Retina 倍数）。持久缓冲是
游戏的硬性依赖：它按脏矩形局部重绘（比如主菜单背景只在进入时画一次），没有持久
缓冲就会丢内容。绘制在游戏主循环线程完成（80ms/帧，`mad.b` 里的 `java.util.Timer`），
Swing 只负责把缓冲 1:1 贴上窗口。文字按矢量渲染落到物理像素上，始终清晰；图片
素材经 `Image.ASSET_SCALE` 预放大后 1:1 上屏，保持像素风。

## 键位（macOS 桌面 ↔ J2ME 手机）

游戏里的按钮分两类：**画在屏幕内容里的**（菜单项、任务简报的文字）用方向键+回车
操作；**画在屏幕四角的软键按钮**（如左下角的 "Next"、右下角的"返回"）对应真机的
左右软键，桌面上是 F1/ESC：

| 桌面按键 | J2ME 键码 | 游戏中的作用 |
|---|---|---|
| ↑ ↓ ← → 或 **W A S D** | -1 ~ -4 | 移动菜单高亮 / 光标；按住持续移动（先慢后快，同真机自动重复） |
| 回车 / 空格 / **X** | -5 (FIRE/5) | 确认：进菜单、推进对话框、游戏中选中/下令 |
| **F1** | -6（左软键） | 屏幕**左下角**的按钮：任务简报的 "Next"、菜单里的 "Menu" 等 |
| **ESC 或 F2** | -7（右软键） | 屏幕右下角的按钮：返回/取消/跳过 |
| 数字键 1-9 | 49-57 | 数字键操作（教程：2/4/6/8 移动、1/3/7/9 斜向、5 确认） |
| * 和 # | 42 / 35 | 原机星号/井号键 |

WASD/X 与方向键/回车完全等价（同一映射出口）；**Q/E/Z/C** = 斜向移动
（对应数字 1/3/7/9 的左上/右上/左下/右下）。

## 鼠标 / 触摸板（桌面增强）

J2ME 原版没有指针操作，这是移植新增的能力，仅在任务主视图内生效：

| 操作 | 游戏行为 |
|---|---|
| 移动 / 悬停 | 光标格跟随鼠标（本游戏光标恒居中，实际是镜头平移把该格带到中心） |
| 左键单击 | 等价键盘 FIRE（选中光标处单位 / 确认 / 推进对话框） |
| 左键拖动 | 框选：选中框内所有本方单位（对应传统 RTS 的 band select） |
| 右键（触控板双指点按） | 有选中单位 → 全体移动到鼠标格；无选中 → 取消选择 |

实现位于 `c.java` 的"桌面鼠标增强"段（`mouseA` 入口）：屏幕→格子不做投影换算，
而是复用世界渲染遍历做像素拾取；移动指令走游戏自身的 `d(0,tx,ty)` 路径；
多选复刻游戏按类型多选 `h()` 的置位方式，与存档/成就逻辑完全兼容。

键位表本体在 `data.res` 资源 #129 里，是 Nokia 风格键码；上表是桌面适配层
（`Canvas.mapKeyCode`）到这套键码的映射。

## 移植时对反编译代码做的手工修正

CFR 输出不能直接编译，以下为已做的修正（改代码时心中有数）：

1. `AgeOfEmpires` 类改名 `AoeMidlet`：原名与包名冲突，导致 `AgeOfEmpires.b.xxx`
   这类限定引用在源码层面无法表达。
2. `mad/e.java`：CFR 整体分析失败，三个重名方法调用手工还原。
3. `c.java` 5389 行附近：CFR 还原字节序读取的顺序错误（`by` 先用后赋值），已按
   字节码语义修正。
4. 若干 CFR 丢掉的 `int→short/byte` 强转，已补上。
5. `mad/c.java`：资源流 `read()` 改 `readFully()`（jar 内流不保证一次读满，
   真机上凑巧能跑，桌面上会读出全 0 尾部）。
6. `c.java` 的 `p()` 里被吞掉的异常加了 `-Daoe.debug=1` 时才打印的探针。
7. `c.java` 的 `void_e()`（按键松开）：原实现松开立即清零动作变量，桌面快速点按会
   在同一 tick 内被吞掉；但直接改成"不清"又会让持续移动粘滞（`ab != 0` 是"持续
   移动"的条件）。最终方案：`c.java` 保持原版语义，由适配层 `Canvas` 把松开事件
   **延迟到本次 paint 之后**再投递——按下保证被游戏完整消费一帧（不吞点按），
   松开照旧全清（不粘滞）。
8. `c.java` 选关屏上限补丁（`boolean_d` 的 H-case 11/12）：原版按通关进度开放
   战役 7 关（`campaignProgress+1`）和教学 Mission 3 关（`tutorialProgress+1`），现改为固定全解锁（7/3），
   进度变量本身保留（选关屏默认仍停在玩家进度所在关，存档格式不变）。

## 待办（后续阶段）

- **可读化重命名**：`c.java` 里大量 `var_int_arr_a` / `boolean_d(3)` 风格的名字，
  需要对着游戏行为逐步改成语义化命名（这是主要工作量）。
- **资源解包**：`data.res` 格式已完全掌握（`mad/c.java` 索引结构 +
  `b.java` 的自定义图片格式现场拼 PNG），可以写工具解包成 PNG/文本，便于修改游戏内容。
- **音频**：MIDI 播放已接上（javax.sound.midi），但只验证了"不崩"，音质/音量未细调。
- **打包**：`jpackage` 出 .app。
- 鼠标支持（原版是数字键操作，桌面端可考虑加）。
