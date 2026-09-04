/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Command
 *  javax.microedition.lcdui.CommandListener
 *  javax.microedition.lcdui.Displayable
 *  javax.microedition.lcdui.Font
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 *  javax.microedition.rms.RecordStore
 *  javax.microedition.rms.RecordStoreException
 */
package AgeOfEmpires;

/* ============================================================================
 * 符号表（32 轮玩家代理逆向考据沉淀，2026-09-03 wave7 改名后与本文件同步；
 * 证据链见 docs/symbols.md 与 docs/agent-operations.md）。
 *
 * 【状态机】
 *   screenState(aA)   当前屏状态：2=对话框/弹窗（期间 j() 世界循环冻结！）
 *                     4=菜单 6=世界视图 8=建造菜单 12=胜利/结算屏
 *   pendingScreenState(am) 待切换屏状态（startMissionBriefing 链）
 *   tickCount(ar)     全局 tick（aoe.tickms=40ms/tick）
 *   gameMode          0=dev/headless 随机图 16=tutorial 32=战役链（TC 毁胜利判定
 *                     在 32 链模式且 missionIndex≠0 时被跳过——见 onThingDestroyed
 *                     case 9）
 *   startMissionBriefing(as, dialogScriptId, briefingVariant)：
 *                     as=任务id；dialogScriptId=对话框id(62=简报 70=难度页
 *                     98=胜负结算 74=失败简报)；briefingVariant=变体
 *                     (98 结算: 0=胜 1=败)
 *   dialogScriptId=98 由两条独立判定触发：敌 TC 毁→briefingVariant=0（即胜）；
 *   我 TC 毁→briefingVariant=1（即败）。另有 missionScript 脚本胜利路径（opcode 4/5）。
 *
 * 【玩家数据】
 *   playerUnitHeaders[p]  玩家标量区：[0]=age [2]=单位数(pop占用) [3]=pop上限
 *                     [4]=建筑数 [5][6][7]=木/金/石 [49]=在训队列长 [48]=投射物活跃数
 *                     [56..][66..]=队列/计数区
 *   playerUnitSlots[p]    单位表 stride=8（i<[2] 全是活单位）：[+0]tile 打包
 *                     (tx=>>>8, ty=&0xFF) [+3]&0xFF=type [+4]&0x8000=选中位
 *                     [+7]任务字：低nibble 0=闲置 1=行军 2=采集中(高字节0x66=满载
 *                     计时,bit4-5=资源种) 3=回送中
 *   buildingTable[p]  建筑表 stride=4（i<[4]）：[+0]tile 打包(tx=>>8&0x3F,ty=&0x3F)
 *                     [+2]状态(0x40000000=施工中,&0xFF=进度,255=完工) [+3]&0xFF=type
 *                     ⚠️ 数建筑的数组！拿它数单位=bug（rally 曾中招 r31）
 *   selectedTrainProduct  生产菜单选中待训产品（映射见 queueUnitTraining）；
 *                     =0 产物 fifo 显示 type=1（村民第二种），t0 是任务初始赠品
 *
 * 【地图】mapTiles[64×64] short：
 *   0x8000=未探索雾  0x300=资源(低2位 1木2金3石)  0x200=单位占位(低字节=槽位)
 *   0x100=建筑占位(低字节=建筑序号,bit10-11=owner)  雾下：0x83xx=资源(&3 判种)
 *   0x85xx=建筑(低字节=type)——雾中信息其实可读（免侦察地图术）
 *   0x0=虚空/毁灭  Short.MIN_VALUE=未初始化
 * ========================================================================= */

import AgeOfEmpires.AoeMidlet;
import AgeOfEmpires.a;
import AgeOfEmpires.b;
import AgeOfEmpires.d;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public final class c
extends com.ulysseo.mad.a
implements CommandListener {
    public AoeMidlet var_AoeMidlet_a;
    public int screenW;
    public int screenH;
    public int ad;
    public int J;
    public int viewTileCols;
    public int viewTileRows;
    public int bottomBarY;
    public int var_int_f;
    public int mapThumbStampRow;
    public int u;
    public boolean var_boolean_f = false;
    public boolean var_boolean_b = false;
    public int[] nfoHighScores;
    public String[] var_java_lang_String_arr_b;
    public String var_java_lang_String_b;
    public byte var_byte_a = 0;
    public int ah;
    public int ay;
    public Font var_javax_microedition_lcdui_Font_a;
    public int aiGatherMultiplier = 256;
    public short var_short_a;
    public short var_short_b;
    public short var_short_c;
    public int[][] buildingTable;
    // 投射物记录池：[玩家][20]，每玩家 5 条记录 × 4 short，紧凑存放——活跃记录恒占
    // 0 .. playerUnitHeaders[玩家][48]-1（[48]=活跃数；发射追加在 [48]<<2 并 +1，
    // 移除走尾部交换 + [48]-1）。记录字段：[+0] 射手单位 id；[+1] 状态字，
    // 1000=待瞄准（发射/落地后的静止态），否则=已瞄准（值为目标敌单位槽位下标）；
    // [+2] 目标格 tx<<8|ty；[+3] 飞行计时（0-15，16 tick 判命中）。
    // aim/tick 两阶段见 aimProjectiles/tickProjectiles；机制与卡死考证见
    // docs/game-mechanics.md 投射物节。
    public short[][] projectileTable;
    public short[] mapTiles;
    public int[][] playerUnitHeaders;
    // —— 状态机（速查见 GAME_NOTES.md，完整语义未完全考证，改动前对照 p() 的分发）——
    // screenState: 顶层画面状态，p() 按它分发渲染；E() 在 screenState 变化时构建新画面。
    // am: 配套的"当前画面"值（boolean_g(n) 直接设置），am==screenState 表示画面稳定。
    public int screenState;
    public int pendingScreenState;
    public int aH;
    // 本局是否走随机图生成：任务资源字节[0..1] 是地图 RNG 种子，全零 = 无固定地图
    // → randomMap=true，d.a(9,20,mapTiles,this) 用生成器铺地形，t() 再补城镇中心；
    // 非零则该种子喂 rngStateLo/Hi 走脚本固定地图。快照 v2 已随档保存。
    public boolean randomMap;
    public int R;
    // ac: 游戏模式（由菜单脚本动作 65/71/73 设置）：0=教程，16=随机地图，32=战役。
    // aC: 当前选中的任务序号（0 起；战役 0..6，随机地图 0..2）。
    public int gameMode;
    public int missionIndex;
    public int dialogScriptId;
    public int I;
    public int[] var_int_arr_e;
    // 音效开关（存档字节 29：0=开）。
    public boolean var_boolean_d = true;
    // 解锁进度：战役可选上限 = aj+1（通关后 aj = aC+1，上限 6，共 7 关）；
    // 随机地图可选上限 = aG+1（上限 2，共 3 档）。打包存进 .nfo 字节 28 = aG<<4|aj。
    // （移植已改：选关屏上限固定全解锁，见 boolean_d 的 H-case 11/12。）
    public int campaignProgress;
    public int tutorialProgress;
    // ar: tick 计数（每帧 +1，80ms 一帧；调试日志里的 ar 就是它）。
    public int tickCount;
    public boolean var_boolean_h;
    public boolean var_boolean_j;
    public byte[] techFlags;
    public int cameraPxX;
    public int cameraPxY;
    public int camTargetX;
    public int camTargetY;
    public int cursorTileIdx;
    public int t;
    public int cursorTileX;
    public int cursorTileY;
    public int selectedTrainProduct;
    public String[] var_java_lang_String_arr_a;
    public int as;
    public int briefingVariant;
    public int overlayPrevState;
    public int aW;
    public int aT;
    public int av;
    public int var_int_e;
    public int D;
    public int selectionMode;
    public int p;
    public int selectionMark;
    public int selectedType;
    public int selectedSlot;
    public int selectionPlayer;
    public int var_int_i;
    public int aQ;
    public Graphics var_javax_microedition_lcdui_Graphics_a;
    public byte[] var_byte_arr_d;
    public byte[] var_byte_arr_b;
    public byte[] var_byte_arr_h;
    public boolean var_boolean_e = false;
    // .nfo 存档原始字节（314B，无参 m() 读写 RecordStore——与字段 bgmFramesLeft 无关，nfoReadInt(...)/h(...) 存取）：
    // [0..27) = 7 个 4 字节大端战役高分（nfoHighScores）；[28] = aG<<4|aj；
    // [29] = 音效开关；[30] = 另一开关（AgeOfEmpires.b.payCost，含义未考证）。
    public byte[] nfoData;
    // —— 按键输入状态（键位表 loadKeymap(129) 从 data.res #129 加载）——
    // keymap: 键码→动作码映射表，ak = 表长/2。onKeyPress（按下）查表置
    // ae（瞬时值）和 ab=ax（持续值）；游戏逻辑以 "ab != 0" 为"有键按住"
    // （如 o() 开头 if (ab == 0) return），自身从不清 ab，依赖松开事件
    // void_e 全清。桌面适配层把松开延迟到 paint 之后投递，见 void_e 注释。
    public int keymapCount;
    public byte[] keymap;
    public int keyActionEvent;
    // volatile：按键状态由 EDT/dev 线程写、游戏 tick 线程读，ARM 弱内存模型下
    // 无同步的跨线程写入可能长期不可见（dev 自动导航曾因此失灵）。
    public volatile int keyActionPulse;
    public volatile int keyActionHeld;
    public String var_java_lang_String_d;
    public int ag;
    public String var_java_lang_String_a;
    public String var_java_lang_String_c;
    public int G;
    public int x;
    public boolean var_boolean_a;
    public boolean var_boolean_g;
    public int clipLeft;
    public int clipRight;
    public int clipTop;
    public int clipBottom;
    public boolean var_boolean_l;
    public int W;
    public int X;
    public int[] var_int_arr_c;
    public int T;
    public int aD;
    public byte[] costTable;
    public int var_int_g;
    public int var_int_c;
    public int[] var_int_arr_b;
    // 任务脚本字节码（data.res 资源装载；解释器 = tickMissionScript → evalScriptCondition
    // → runScriptActions/skipScriptActions/skipScriptBlock；解释器会在块首字节写"已执行"
    // 标记 = 翻转符号位）。快照存档整体携带。字节 127 = 脚本区结束标记。
    public byte[] missionScript;
    public int[] var_int_arr_a;
    public int var_int_d;
    public int S;
    public int au;
    public int U;
    public int cursorScreenPxX;
    public int cursorScreenPxY;
    public static int rngStateHi = 12;
    public static int rngStateLo;
    d var_AgeOfEmpires_d_a;
    public int missionResId;
    // 菜单/对话框模板字节：a(n, true) 加载 data.res 对话框资源（131=选关）。
    // 树状节点结构，int_c/int_e/int_k/int_i 遍历；节点参数在 +9 起
    // （循环器控件：+9+1 = 选项总数，+9+2 = 当前选中项）。item 激活时执行
    // 一段脚本，动作码见 1225 行附近的 switch（65=教程/71=随机地图/
    // 73=战役/67,72=设置开关…）。
    public byte[] menuTree;
    // H: 子状态/对话框选择器，boolean_d 按 H 加载对应菜单并打补丁：
    // 11=随机地图选关，12=战役选关，1=回主菜单。
    public int menuScreenId;
    public int pendingPanelSwitch;
    public int ap;
    public int menuNode;
    public int menuNodeCount;
    public int menuHighlight;
    public int var_int_a;
    public int K;
    public boolean aiEnabled = true;
    public int aiBuildInterval;
    public int aiBuildTimer = 0;
    public int aiBuildPhase;
    public int aiBuildTarget;
    public int aiAttackThreshold;
    public int aiGuardRadiusSq;
    public int aiTrainTimer;
    public int aiTrainInterval;
    public int aiFreeResTimer;
    public int aiFreeResInterval;
    public Image[] var_javax_microedition_lcdui_Image_arr_a;
    // 自动重复：ab==L 视为"按住未换键"，s 每 tick +1，s>=5 后动作持续生效。
    public int keyRepeatFrames;
    public int keyRepeatLast;
    public int mapViewSavedCamX;
    public int mapViewSavedCamY;
    public int mapViewSavedCursorX;
    public int mapViewSavedCursorY;
    // 距下次 BGM 换曲的帧数：世界模拟块里 --，到 0 调 playNextBgm()（写 曲长ms/80，
    // 选曲走化妆品 RNG nextBgmRandomInt）。菜单 v() 刷 510 拉长间隔；任务装载清 0
    // 立即开曲；boot 初始化 100000（开机先沉默）。换曲时机随 tickms 变速（见
    // docs/game-mechanics.md「主循环与时序」），但不再消耗模拟 RNG。
    public int bgmFramesLeft = 100000;
    public int mediaRequestId;
    public boolean var_boolean_c;
    public byte[] dirTable;
    public short[][] playerUnitSlots;
    public byte[] aiBuildOrder = new byte[]{11, 2, 10, 1, 12, 2, -1, 11, 3, 12, 5, 6, 1, 7, 1, 5, 1, 10, 1, -1, 11, 5, 12, 5, 8, 1, 7, 1, 10, 1, 2, 1, -1, 11, 5, 8, 1, 12, 5, 7, 1, 3, 1, 2, 1, 10, 1, 10, 1, -2};

    c(AoeMidlet ageOfEmpires) {
        super(ageOfEmpires, 134, 0);
        this.var_AoeMidlet_a = ageOfEmpires;
        this.var_java_lang_String_d = "V" + ageOfEmpires.getAppProperty("MIDlet-Version");
        com.ulysseo.mad.c.void_a(0);
        this.bgmFramesLeft = 100000;
    }

    public final void w() {
        if (this.var_boolean_e) {
            this.var_AoeMidlet_a.a();
        }
    }

    public final synchronized void onShown() {
        if (this.tickCount < 20 || !AoeMidlet.var_boolean_a) {
            return;
        }
        this.mapThumbStampRow = 0;
        this.var_boolean_l = true;
        AgeOfEmpires.b.var_boolean_a = true;
    }

    public final synchronized void onHidden() {
        if (this.tickCount < 20 || !AoeMidlet.var_boolean_a) {
            return;
        }
        AgeOfEmpires.b.c();
        AgeOfEmpires.b.var_boolean_a = true;
        this.s();
        this.var_boolean_j = true;
        this.keyActionHeld = 0;
    }

    public final void h() {
        var_com_ulysseo_mad_b_a.a();
    }

    public final int int_b() {
        AgeOfEmpires.b.void_a();
        this.keyActionEvent = 0;
        this.loadKeymap(129);
        this.nfoData = null;
        this.nfoData = new byte[314];
        this.loadNfo();
        this.e();
        this.playerUnitHeaders = new int[2][91];
        this.playerUnitSlots = new short[2][208];
        this.buildingTable = new int[2][88];
        this.projectileTable = new short[2][20];
        this.mapTiles = new short[4096];
        this.var_javax_microedition_lcdui_Font_a = Font.getDefaultFont();
        this.ah = 19;
        this.ay = -2;
        this.var_int_f = 19;
        this.setupScreenMetrics(var_com_ulysseo_mad_b_a.getWidth(), var_com_ulysseo_mad_b_a.getHeight());
        this.menuTree = com.ulysseo.mad.c.byte_arr_a(117);
        this.pendingScreenState = 4;
        this.menuScreenId = 2;
        this.tickCount = 0;
        this.nfoHighScores = new int[7];
        this.var_int_arr_e = new int[7];
        this.var_int_arr_a = new int[4];
        this.loadNfo();
        int n = this.nfoReadInt(28, 1);
        this.tutorialProgress = n >> 4;
        this.campaignProgress = n & 0xF;
        this.var_boolean_d = this.nfoData[29] == 0;
        AgeOfEmpires.b.c = this.nfoData[30] == 0;
        for (int i = 0; i < 7; ++i) {
            this.nfoHighScores[i] = this.nfoReadInt(0 + (i << 2), 4);
        }
        this.R = 0;
        return -1;
    }

    public final void startGameCanvas() {
        var_com_ulysseo_mad_b_a.b();
        var_com_ulysseo_mad_b_a.setFullScreenMode(true);
        while (!var_com_ulysseo_mad_b_a.isShown()) {
        }
        var_com_ulysseo_mad_b_a.setCommandListener(this);
        var_com_ulysseo_mad_b_a.getWidth();
        var_com_ulysseo_mad_b_a.getHeight();
        // dev 模式可用 -Daoe.tickms=N 加速/放慢主循环（默认 80 = 原版 12.5fps）
        var_com_ulysseo_mad_b_a.a(Integer.getInteger("aoe.tickms", 80), 1);
    }

    public static final void a(Object[] objectArray) {
        if (objectArray == null) {
            return;
        }
        for (int i = 0; i < objectArray.length; ++i) {
            objectArray[i] = null;
        }
    }

    public final void v() {
        if (this.tickCount - this.u >= 50) {
            this.u = this.tickCount;
            this.bgmFramesLeft = 510;
        }
        this.ag = 20;
        this.var_java_lang_String_a = null;
        this.var_java_lang_String_c = null;
        a a2 = new a(99);
        this.var_java_lang_String_a = a2.a(5);
        this.var_java_lang_String_c = a2.a(4);
    }

    public final void requestMedia(int n, boolean bl) {
        if (this.mediaRequestId == n) {
            return;
        }
        AgeOfEmpires.b.var_boolean_b = true;
        this.mediaRequestId = n;
        this.var_boolean_c = bl;
    }

    public final void playNextBgm() {
        // 背景音乐换曲：曲长毫秒按"80ms/帧"折成帧数倒计时（写入字段 bgmFramesLeft，在 onPaint 模拟块里每帧 --）。
        // 硬编码了原版帧率——改 aoe.tickms 会按比例 skew 换曲时机（详见 docs/game-mechanics.md「主循环与时序」）。
        // 选曲走独立的"化妆品 RNG"（nextBgmRandomInt），不消耗模拟用全局 nextRandomInt——
        // 这是模拟轨迹"纯任务+输入决定"（确定性回放，tools/replaycheck.sh）的前提：
        // 换曲时机随墙钟漂移，若与模拟共用一条流，战斗掷骰等结果会随听了几首曲子而变。
        int n = nextBgmRandomInt() % 6;
        int[] nArray = new int[]{204000, 123000, 233000, 180000, 188000, 190000, 143000, 197000, 184000, 175000};
        this.bgmFramesLeft = nArray[n] / 80;
        this.requestMedia(n + 3 + 131, false);
    }

    public final void onKeyPress(int n) {
        if (n > 127) {
            n = -(n - 128);
        }
        int n2 = n;
        this.keyActionEvent = 0;
        for (int i = 0; i < this.keymapCount; ++i) {
            if (n2 != this.keymap[i << 1]) continue;
            this.keyActionEvent = this.keymap[(i << 1) + 1];
            break;
        }
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[void_a] key=" + n + " ae=" + this.keyActionEvent + " ak=" + this.keymapCount + " ar=" + this.tickCount);
            // 确定性回放 trace 行（tools/replaycheck.sh）：ar= 发生时的 tick
            System.out.println("[input] ar=" + this.tickCount + " key " + n);
        }
        this.keyActionPulse = this.keyActionHeld = this.keyActionEvent;
    }
    public final void onKeyRelease(int n) {
        // 原版语义：松开即全清。桌面适配层（Canvas）会把松开事件延迟到 paint
        // 之后才投递到这里，保证按下至少被游戏完整消费一帧，快速点按不会被吞。
        this.keyActionEvent = 0;
        this.keyRepeatLast = 0;
        this.keyActionHeld = 0;
        this.keyActionPulse = 0;
    }

    // ===== dev 模式（-Daoe.dev=tutorial:1|campaign:2|random:1，可配 -Daoe.headless=1/-Daoe.tickms=N）=====
    /** 跳过菜单直进指定关卡：守护线程走真实菜单流（注入按键，等稳定再下一步）。
     *  依赖 volatile 的 ax/ab 保证注入可见性。 */
    public void devStartMission(String spec) {
        Thread t = new Thread(() -> {
            try {
                devNavToMission(spec);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "dev-navigate");
        t.setDaemon(true);
        t.start();
    }

    /** dev 快照直启：-Daoe.devBoot=<存档路径>。读档 → 按快照记录的 nav spec 走
     *  菜单导航装载同一任务 → 主视图稳定后覆写快照状态。测试不再每次从主菜单
     *  爬 10-50 秒。窗口会话（无 nav spec）的存档不支持直启，请用 F9。 */
    public void devBootFromSave(String path) {
        Thread t = new Thread(() -> {
            try {
                byte[] data;
                try {
                    data = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
                } catch (Exception e) {
                    System.out.println("[devBoot] cannot read " + path + ": " + e);
                    return;
                }
                String spec;
                try {
                    spec = aoe.SaveState.navSpec(data);
                } catch (Exception e) {
                    System.out.println("[devBoot] bad save " + path + ": " + e);
                    return;
                }
                if (spec == null) {
                    System.out.println("[devBoot] " + path
                        + " has no nav spec (window-mode save); boot-restore unsupported");
                    return;
                }
                System.out.println("[devBoot] " + path + " -> nav " + spec);
                devNavToMission(spec);
                long deadline = System.currentTimeMillis() + 45000;
                int stable = 0;
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(200);
                    if (this.screenState == 6) {
                        if (++stable >= 15) {
                            break;
                        }
                    } else {
                        stable = 0;
                    }
                }
                if (this.screenState != 6 || !this.devLoadFrom(path)) {
                    System.out.println("[devBoot] failed, aA=" + this.screenState);
                    return;
                }
                long until = System.currentTimeMillis() + 3000;
                while (this.devPendingRestore != null && System.currentTimeMillis() < until) {
                    Thread.sleep(50);
                }
                System.out.println("[devBoot] done, aA=" + this.screenState);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "dev-boot");
        t.setDaemon(true);
        t.start();
    }

    /** 菜单导航到指定任务并推掉装载简报（同步，须在 dev 线程调用）。
     *  成功后记录 spec 到 devLastNavSpec：快照存档带上它，boot 直启时原样重放，
     *  不用解读 ac 的运行时语义（持久化 .nfo 会让模式循环器位置跨会话漂移）。 */
    private void devNavToMission(String spec) throws InterruptedException {
        this.devLastNavSpec = spec;
        String[] parts = spec.split(":");
        final int mission = Math.max(1, Integer.parseInt(parts[1]));
        final boolean campaign = parts[0].startsWith("c");
        final boolean random = parts[0].startsWith("r");
        try {
            devWaitStable();
            // 等到真·主菜单再按 Play:开屏闪屏页(屏根 menuNode=187/254/333,定时脚本
            // 自动翻页)同样满足 aA=4 的"稳定",在其上按 -5 会被闪屏当确认吞掉,后续
            // Game Mode 右切全部落空 → random:N 落进教学关(BUG-001,2026-09-01 玩家
            // 会话实伤)。menuNode 即 [fMenu] 日志的 aR,主菜单 = 0。
            long menuDeadline = System.currentTimeMillis() + 20000;
            while (this.menuNode != 0 && System.currentTimeMillis() < menuDeadline) {
                Thread.sleep(200);
            }
            devPress(-5);                       // 主菜单：Play
            if (campaign || random) {
                devPress(-4);                   // Game Mode 循环器右切 1：Tutorial→Campaign
            }
            if (random) {
                devPress(-4);                   // 右切 2：Campaign→Random Map（进 Difficulty 屏）
            }
            // Game Mode → （选关等中间屏）→ 任务装载：菜单链长度随模式/版本
            // 有差异（有的屏高亮项脚本是空操作），连按 FIRE 直到离开菜单态。
            // 注意：mission>1 的右切只能在选关/难度屏按——Game Mode 屏（menuScreenId==1）
            // 的高亮项也是 op=3 循环器，在那里右切会把模式切跑（2026-09-02 实测
            // random:2 被右切成 Tutorial→进了教学关 2）。
            long deadline = System.currentTimeMillis() + 30000;
            while (this.screenState == 4 && System.currentTimeMillis() < deadline) {
                if (mission > 1 && this.menuScreenId != 1 && devHiScriptOp() == 3) {
                    for (int i = 1; i < mission; ++i) {
                        devPress(-4);           // 选关循环器右切（仅限 op=3 的选关项）
                    }
                }
                devPress(-5);
            }
            deadline = System.currentTimeMillis() + 30000;
            while (this.screenState != 6 && System.currentTimeMillis() < deadline) {
                if (this.screenState == 2) {
                    this.onKeyPress(-6);            // F1 推简报/教程对话框（周期重注=自带重试）
                }
                Thread.sleep(600);
            }
            System.out.println("[dev] in mission, aA=" + this.screenState);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 菜单指针快照：screenState / ao / Z / aR + （高亮项是循环器时）循环器位置字节。
     *  几乎任何菜单移动、换屏、循环切换都会改变其中之一，用作注入"被消费"的观测信号。 */
    private long devSig() {
        long sig = ((long) this.screenState & 0xFF) << 48
            | ((long) this.menuNodeCount & 0xFFFF) << 32
            | ((long) this.menuHighlight & 0xFFFF) << 16
            | ((long) this.menuNode & 0xFFFF);
        try {
            if (this.menuTree != null && this.menuHighlight >= 0 && this.menuHighlight < this.menuNodeCount) {
                int node = this.int_c(this.menuHighlight);
                if (node + 11 < this.menuTree.length
                        && (this.menuTree[node + 8] & 0xF) == 2) {
                    sig |= (1L << 63)
                        | ((long) (this.menuTree[node + 11] & 0xFF & 0x7F) << 56);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // 模板正好被游戏线程换掉：退化为不含循环器字节的快照
        }
        return sig;
    }

    /** 等菜单指针静止（devSig 连续 ~3 帧不变）：按键前保证打在稳定画面上，
     *  确认后保证换屏过渡完成，下一步快照不落在过渡中。 */
    private void devWaitSigStable() throws InterruptedException {
        long frame = Long.getLong("aoe.tickms", 80L);
        long last = devSig();
        long lastChange = System.currentTimeMillis();
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(frame);
            long now = devSig();
            if (now != last) {
                last = now;
                lastChange = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastChange >= 3 * frame) {
                return;
            }
        }
    }

    /** 高亮项的脚本操作码；高亮项不是菜单项或模板正在换时返回 -1。 */
    private int devHiScriptOp() {
        try {
            if (this.menuTree == null || this.menuHighlight < 0 || this.menuHighlight >= this.menuNodeCount) {
                return -1;
            }
            int node = this.int_c(this.menuHighlight);
            if ((node + 12 < this.menuTree.length)
                    && (this.menuTree[node + 8] & 0xF) == 2) {
                return this.menuTree[this.int_k(node)] & 0xFF;
            }
        } catch (IndexOutOfBoundsException e) {
            // 模板被游戏线程换掉：按"读不到"处理
        }
        return -1;
    }

    /** 注入按键并等菜单指针变化确认生效；未生效按帧重注——每帧末 ax=0 与激活判断
     *  存在竞态窗口，单次注入可能被无痕吞掉（只按一次的旧实现因此时灵时不灵）。
     *  前后各等一次指针静止，防止重注/下一步打在换屏过渡里造成连跳。 */
    private void devPress(int key) throws InterruptedException {
        long frame = Long.getLong("aoe.tickms", 80L);
        devWaitSigStable();
        long before = devSig();
        long deadline = System.currentTimeMillis() + 8000;
        while (true) {
            this.onKeyPress(key);
            // 面板切换是延迟生效的（v=H 由状态机在后续帧消费），确认窗口必须盖过它，
            // 否则把"生效中"误判为"被吞"而重注，造成一次按键两次消费、流程跳屏。
            long until = System.currentTimeMillis() + 12 * frame;
            while (System.currentTimeMillis() < until && devSig() == before) {
                Thread.sleep(20);
            }
            if (devSig() != before) {
                devWaitSigStable();
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                System.out.println("[dev] press " + key + " unconfirmed (sig=" + before + ")");
                return;
            }
        }
    }

    /** 等画面状态稳定（screenState 连续 ~1.2s 不变），最长 6s——规避状态切换中按键被吞。 */
    private void devWaitStable() throws InterruptedException {
        int last = -999;
        int same = 0;
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
            if (this.screenState == last) {
                if (++same >= 6) {
                    return;
                }
            } else {
                last = this.screenState;
                same = 0;
            }
        }
    }

    // ===== dev 效率工具：autoDismiss / HUD / 快照存档（F5/F9、FIFO、自动 checkpoint）=====
    private static final boolean DEV_AUTO_DISMISS = System.getProperty("aoe.autoDismiss") != null;
    static final boolean DEV_HUD = System.getProperty("aoe.devHud") != null;
    static final boolean DEV_NO_RENDER = System.getProperty("aoe.noRender") != null;
    private static final boolean DEV_AUTO_CHECKPOINT =
        !"-".equals(System.getProperty("aoe.autoCheckpoint", "1"));
    /** 对话框自动推进：任务里进过一次主视图后，screenState==2 的弹窗每 4 帧补一个左软键。 */
    private boolean devMissionSeen;
    private int devDismissFrames;
    /** 任务内连续帧计数：到 25（≈2s）做一次开局自动 checkpoint。 */
    private int devStableFrames;
    /** 本任务是否已写过 auto checkpoint（setupMissionEnv 装载新任务时复位）。 */
    private boolean devCheckpointedThisMission;
    private volatile String devPendingSavePath;
    /** 周期快照：-Daoe.snapshotEvery=N（N>0 生效，缺省关）。任务主视图期间每 N tick
     *  在帧首捕获一份 snap-<tick>.aoesave 到 saveDir，滚动只留最新 8 份
     *  （ailoop 败局尸检用，配合 tools/aoesave.py 只读解析）。 */
    private static final int DEV_SNAPSHOT_EVERY = Integer.getInteger("aoe.snapshotEvery", 0);
    private static final int DEV_SNAPSHOT_KEEP = 8;
    /** 下次该捕快照的 tick 阈值（>= 触发；不用 tick%N==0，turbo 一帧可能跨多 tick）。 */
    private int devNextSnapshotTick;
    /** 待写快照路径（仅 devFrameHousekeeping 同线程读写，无需 volatile）。 */
    private String devPendingSnapshotPath;
    private volatile byte[] devPendingRestore;
    /** 本次会话进入当前任务用的 nav spec（"-Daoe.dev" 语义）；窗口会话为 null。 */
    public String devLastNavSpec;
    private String devToast;
    private long devToastUntil;

    // ===== 视频渲染（-Daoe.videoDir=<dir>，配 -Daoe.videoEvery=N；-Daoe.reveal=1 全亮）=====
    // 每帧渲染完成后（com.ulysseo.mad.e 的 Timer 循环尾）导出一帧 PNG 到 videoDir。
    // reveal 只改 renderWorld 两处的绘制门（地表 switch 掩码 / 精灵 s>0 门），纯
    // paint 层、不碰 mapTiles——模拟与回放确定性不受影响（regress/replaycheck 双验）。
    static final boolean DEV_REVEAL = System.getProperty("aoe.reveal") != null;
    private static final String DEV_VIDEO_DIR = System.getProperty("aoe.videoDir");
    /** 每 N tick 一帧（默认 10 = 0.4 游戏秒；30fps 合成 ≈12 倍原速）。 */
    private static final int DEV_VIDEO_EVERY = Integer.getInteger("aoe.videoEvery", 10);
    /** 进过一次任务主视图(6)才开始录——跳过 boot/导航噪声；终局弹窗继续录。 */
    private boolean devVideoArmed;
    private int devVideoSeq;

    /** 帧尾钩子：videoDir 启用时按 videoEvery 节奏导出当前帧缓冲为 PNG。
     *  PNG 编码的墙钟耗时只拖慢 tick 节奏，不改 tick 计数驱动的模拟。 */
    public void devAfterFrame() {
        if (DEV_VIDEO_DIR == null) {
            return;
        }
        if (!this.devVideoArmed) {
            if (this.screenState != 6) {
                return;
            }
            this.devVideoArmed = true;
        }
        if (this.tickCount % DEV_VIDEO_EVERY != 0) {
            return;
        }
        var_com_ulysseo_mad_b_a.dumpFramebuffer(
            String.format("%s/frame_%08d.png", DEV_VIDEO_DIR, ++this.devVideoSeq));
    }

    // ===== 可选 BFS 寻路（-Daoe.bfsPath=1，默认关，关闭时行为与原版逐字节一致）=====
    // 只替换 boolean_b 里"选落点"一步：DDA 直线步进换成"沿 BFS 路径取下一格"；
    // 落点三重检查、目标格被占处理（抵达钩子，采集/接敌靠它）、7 邻格扇形回退、
    // 占位表更新全部保留。障碍语义：建筑(0x100)与地形资源/虚空(0x300)当墙，
    // 单位占用(0x200)当可穿（动态障碍由运行时扇形回退处理，与原版兼容）。
    // 目标格本身是墙（采集资源/攻击建筑）时，以目标格的可走邻格为种子反向洪泛，
    // 走到目标隔壁即算到位，后续由原抵达钩子接管。路径是纯缓存（可从 slot[2]
    // 重建），不进存档、不影响快照格式。同步 = 推进/沿原路径归队/离队重算；
    // 连续 8 次无进展升级为"单位也当墙"绕开人墙，再不行永久回退原 DDA（兜底）。
    static final boolean BFS_PATH = "1".equals(System.getProperty("aoe.bfsPath"));
    /** BFS 邻格扩张/回溯的固定方向顺序（8 连通，与原版可斜走语义一致；固定顺序保确定性）。 */
    private static final byte[] BFS_DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final byte[] BFS_DY = {0, -1, -1, -1, 0, 1, 1, 1};
    /** 单单位路径容量（格）。最短路径超长即放弃，回退原 DDA+扇形行为。 */
    private static final int BFS_PATH_CAP = 1024;
    /** 每玩家单位上限（playerUnitSlots = short[2][208]，8 short/单位）。 */
    private static final int BFS_MAX_UNITS = 26;
    /** 沿路径连续无进展（pos 不前进）这么多次后升级重算/回退（节流）。 */
    private static final int BFS_REPLAN_AFTER = 8;
    /** 每单位路径格序列（打包 tx<<8|ty），flat 下标 = 玩家*26+单位。 */
    private final short[] bfsPath = new short[2 * BFS_MAX_UNITS * BFS_PATH_CAP];
    /** 路径长度（格数）；0 = 无路径（不可达/已在目标隔壁/超长），回退原 DDA。 */
    private final short[] bfsPathLen = new short[2 * BFS_MAX_UNITS];
    /** 下一个待确认格在路径中的下标。 */
    private final short[] bfsPathPos = new short[2 * BFS_MAX_UNITS];
    /** 路径对应的目标（slot[2] 打包值）；不一致即重算。初值 0 不会误命中（首算即覆盖）。 */
    private final short[] bfsPathTarget = new short[2 * BFS_MAX_UNITS];
    /** 沿路径连续无进展（pos 不前进，含振荡活锁）计数，到 BFS_REPLAN_AFTER 触发升级。 */
    private final short[] bfsPathStuck = new short[2 * BFS_MAX_UNITS];
    /** 重算模式：1 = 把单位占用格也当墙（无进展后升级，绕开静态人墙/袋形死角）。 */
    private final byte[] bfsUnitsBlock = new byte[2 * BFS_MAX_UNITS];
    /** BFS 距离场 scratch（-1 = 未访问）。 */
    private final int[] bfsDist = new int[4096];
    /** BFS 队列 scratch。 */
    private final int[] bfsQueue = new int[4096];

    static String devSaveDir() {
        return System.getProperty("aoe.saveDir",
            System.getProperty("user.home") + "/Library/Application Support/AoeJ2ME/saves");
    }

    /** FIFO save/load 路径解析：裸文件名归位到 saveDir（r21 仓库根冒出裸 s14 事故）。 */
    static String devResolveSavePath(String p) {
        if (p.indexOf('/') >= 0) {
            return p;
        }
        String resolved = devSaveDir() + "/" + p;
        System.out.println("[devMouse] bare name -> " + resolved);
        return resolved;
    }

    private void devToast(String s) {
        this.devToast = s;
        this.devToastUntil = System.currentTimeMillis() + 2000;
    }

    /** 快存。实际写盘在 EDT 帧首（p()），避免与 tick/渲染竞态产生撕裂快照。 */
    public boolean devSaveTo(String path) {
        if (this.screenState != 6) {
            System.out.println("[save] refused, aA=" + this.screenState);
            this.devToast("Cannot save now");
            return false;
        }
        this.devPendingSavePath = path;
        return true;
    }

    /** 快读。同任务才允许直接覆写；boot 恢复走 devBoot（先导航后覆写）。 */
    public boolean devLoadFrom(String path) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
            int[] id = aoe.SaveState.identity(data);
            if (this.screenState == 6 && (id[0] != this.gameMode || id[1] != this.missionResId || id[2] != this.nfoMissionId())) {
                System.out.println("[load] mission mismatch: save ac=" + id[0] + " aF=" + id[1]
                    + " nfo=" + id[2] + " / now ac=" + this.gameMode + " aF=" + this.missionResId + " nfo=" + this.nfoMissionId());
                this.devToast("Save is for another mission");
                return false;
            }
            this.devPendingRestore = data;
            return true;
        } catch (Exception e) {
            System.out.println("[load] " + path + ": " + e);
            this.devToast("Load failed");
            return false;
        }
    }

    /** 当前任务身份三元组（教程任务号在 .nfo 字节 31/32）。 */
    private int nfoMissionId() {
        return this.nfoData != null && this.nfoData.length > 32
            ? ((this.nfoData[31] & 0xFF) << 8 | this.nfoData[32] & 0xFF) : 0;
    }

    /** 桌面命令键（Canvas.keyPressed 转发）：1=F5 快存，2=F9 快读。 */
    @Override
    public void desktopCommand(int id) {
        if (id == 1) {
            this.devSaveTo(devSaveDir() + "/quick.aoesave");
        } else if (id == 2) {
            this.devLoadFrom(devSaveDir() + "/quick.aoesave");
        }
    }

    /** 每帧帧首（EDT）：存档请求串行化 + autoDismiss + 自动 checkpoint。 */
    private void devFrameHousekeeping() {
        if (this.screenState == 6) {
            this.devMissionSeen = true;
        } else if (this.screenState == 4) {
            this.devMissionSeen = false;
        }
        if (DEV_AUTO_DISMISS && this.devMissionSeen && this.screenState == 2
                && ++this.devDismissFrames >= 4) {
            this.devDismissFrames = 0;
            this.onKeyPress(-6);
        }
        if (this.screenState != 6) {
            this.devStableFrames = 0;
        } else if (this.devStableFrames < 25) {
            if (++this.devStableFrames == 25 && DEV_AUTO_CHECKPOINT
                    && !this.devCheckpointedThisMission) {
                // 每任务只存一次(BUG-003 终版):此前按"回主视图重数 25 帧 + 600 tick
                // 节流"实现,一次会话仍会写 18 份;现改为 setupMissionEnv 装载时复位
                // 的标志,弹窗/全图视图往返不再触发。
                this.devCheckpointedThisMission = true;
                this.devSaveTo(devSaveDir() + "/auto.aoesave");
            }
        }
        // 周期快照（screenState==6 期间；弹窗冻结态也是有效尸检材料，照存）。
        // 只排队路径，捕获在同方法下方的帧首写盘块——与 devSaveTo 同一时机。
        if (DEV_SNAPSHOT_EVERY > 0 && this.screenState == 6
                && this.tickCount >= this.devNextSnapshotTick) {
            this.devNextSnapshotTick = this.tickCount + DEV_SNAPSHOT_EVERY;
            this.devPendingSnapshotPath = devSaveDir() + "/snap-" + this.tickCount + ".aoesave";
        }
        if (this.devPendingSavePath != null) {
            String path = this.devPendingSavePath;
            this.devPendingSavePath = null;
            try {
                byte[] data = aoe.SaveState.capture(this);
                java.nio.file.Path p = java.nio.file.Path.of(path);
                if (p.getParent() != null) {
                    java.nio.file.Files.createDirectories(p.getParent());
                }
                java.nio.file.Files.write(p, data);
                // ar= 是捕获时刻的 tick（确定性回放的基准，tools/replaycheck.sh 依赖）——
                // devSaveTo 只是指令排队，真正捕获在帧首，与指令时刻可差十几 tick
                System.out.println("[save] wrote " + path + " (" + data.length + "B) ar=" + this.tickCount);
                this.devToast("Saved");
            } catch (Exception e) {
                System.out.println("[save] " + path + ": " + e);
                this.devToast("Save failed");
            }
        }
        if (this.devPendingSnapshotPath != null) {
            String path = this.devPendingSnapshotPath;
            this.devPendingSnapshotPath = null;
            try {
                byte[] data = aoe.SaveState.capture(this);
                java.nio.file.Path p = java.nio.file.Path.of(path);
                if (p.getParent() != null) {
                    java.nio.file.Files.createDirectories(p.getParent());
                }
                java.nio.file.Files.write(p, data);
                System.out.println("[save] wrote " + path + " (" + data.length + "B) ar=" + this.tickCount);
                devPruneSnapshots(p.getParent());
            } catch (Exception e) {
                // 尸检辅助不许炸主循环：失败打日志继续，下个周期再试
                System.out.println("[save] snapshot " + path + ": " + e);
            }
        }
        if (this.devPendingRestore != null) {
            byte[] data = this.devPendingRestore;
            this.devPendingRestore = null;
            try {
                aoe.SaveState.apply(this, data);
                // BFS 路径是纯缓存但带目标记忆:装载后必须失效,否则"装载前 live 段"
                // 残留的缓存会污染回放确定性(2026-09-02 实测:devBoot 双跑单位错位)
                if (BFS_PATH) {
                    java.util.Arrays.fill(this.bfsPathTarget, (short)-1);
                }
                // 回放锚：trace 的相对 tick 坐标从这里起算（快照 v2 已钉住 tickCount）
                this.devTraceBaseTick = this.tickCount;
                System.out.println("[load] applied (" + data.length + "B) traceBase=" + this.devTraceBaseTick);
                this.devToast("Loaded");
            } catch (Exception e) {
                System.out.println("[load] apply: " + e);
                this.devToast("Load failed");
            }
        }
    }

    /** 快照滚动窗口：dir 下 snap-<tick>.aoesave 只留最新 DEV_SNAPSHOT_KEEP 份
     *  （按文件名里的 tick 排序——名字不定长补零，字典序不可靠）。 */
    private static void devPruneSnapshots(java.nio.file.Path dir) throws java.io.IOException {
        java.util.List<java.nio.file.Path> snaps = new java.util.ArrayList<>();
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().matches("snap-\\d+\\.aoesave"))
                .forEach(snaps::add);
        }
        if (snaps.size() <= DEV_SNAPSHOT_KEEP) {
            return;
        }
        snaps.sort(java.util.Comparator.comparingInt(p -> {
            String n = p.getFileName().toString();
            return Integer.parseInt(n.substring(5, n.length() - ".aoesave".length()));
        }));
        for (int i = 0; i < snaps.size() - DEV_SNAPSHOT_KEEP; ++i) {
            java.nio.file.Files.deleteIfExists(snaps.get(i));
        }
    }

    // ===== dev 鼠标驱动（-Daoe.devMouse=<fifo 路径>）=====
    /** 从 FIFO 逐行读指令直接喂 mouseA/onKeyPress（逻辑坐标 240x320）。宿主终端缺
     *  辅助功能/屏幕录制授权、无法注入真实 CGEvent 时，用它在游戏层驱动和验证
     *  鼠标逻辑；dump 指令同步导出帧缓冲（配合截图验证渲染结果）。
     *  指令：move x y | press x y | release x y | click x y | rclick x y |
     *        drag x1 y1 x2 y2 | key <键码> | tapk <键码> <期望aA> [重试] |
     *        state（写 <fifo>.json 快照）| until <aA> [超时s] | probe x y |
     *        script <文件> | save [路径] | load <路径> | fields <输出> |
     *        replaytrace <trace文件> [baseTick]（确定性回放，见 tools/replaycheck.sh）|
     *        stopat <tick>（确定性停表，对拍取态用）| dump <png路径> | exit
     *  用法：mkfifo /tmp/aoe-mouse 后启动游戏，echo "move 120 160" > /tmp/aoe-mouse */
    public void devStartMouseFifo(String path) {
        this.devFifoPath = path;
        Thread t = new Thread(() -> {
            java.io.File fifo = new java.io.File(path);
            if (!fifo.exists()) {
                System.out.println("[devMouse] fifo missing: " + path);
                return;
            }
            System.out.println("[devMouse] listening on " + path);
            while (true) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(fifo), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        if (!devMouseCmd(line)) {
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[devMouse] io: " + e);
                }
                try {
                    Thread.sleep(100);      // 写端关闭致 EOF：稍候重开等下一条
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "dev-mouse");
        t.setDaemon(true);
        t.start();
    }

    private String devFifoPath;
    // 确定性回放锚：load 成功后的 tickCount；replaytrace 的相对 tick 以此为原点
    // （无 load 时以指令执行瞬间为原点）。见 replaytrace 指令与 tools/replaycheck.sh。
    private long devTraceBaseTick = -1;
    // 上次自动 checkpoint 的 tick(节流用,见 devFrameHousekeeping)
    private boolean devInScript;
    // 写宏队列：dev 线程入队([fifo] ar= 锚=入队 tick)，模拟线程在每帧 sim 段前
    // 统一应用。裸写在帧内微位置 play/replay 不同，1479 条累计 ±tick 尾差足以翻转
    // 终局(m1 录制局实测)；量化到 tick 边界后两侧同路径，回放位精确。
    private final java.util.Queue<String> devOpQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private boolean devDraining;
    // replaytrace 调度表：到期事件在帧首排空段应用（见 onPaint），dev 线程只负责载入
    private final java.util.ArrayList<Long> devTraceTicks = new java.util.ArrayList<>();
    private final java.util.ArrayList<String> devTraceCmds = new java.util.ArrayList<>();
    private int devTracePos;
    /** 写宏集合：入队量化到帧边界；其余(诊断/控制流/键鼠)保持 dev 线程内联即时。 */
    private static final java.util.Set<String> DEV_QUEUED_OPS = java.util.Set.of(
        "sel", "goto", "rally", "retask", "assign", "train", "build", "gather");

    /** 执行一条 FIFO 指令；返回 false 表示 exit。 */
    private boolean devMouseCmd(String line) {
        // 操作录制锚点：每条指令应用瞬间记 tick。会话日志的 [fifo]/[input] 行
        // 就是精确回放所需的全部信息（tools/mktrace.py 转换、replaytrace `fifo` op 重放）。
        String[] p = line.split("\\s+");
        if (!this.devDraining && DEV_QUEUED_OPS.contains(p[0])) {
            // 入队锚用 [fifoQ]（不进 trace）；[fifo] 留给帧首应用锚（mktrace 契约）
            System.out.println("[fifoQ] ar=" + this.tickCount + " " + line);
            this.devOpQueue.add(line);
            return true;
        }
        System.out.println("[fifo] ar=" + this.tickCount + " " + line);
        // 各命令最小 token 数（含命令名）：缺参直接拒并回显，否则 AIOOBE 只留
        // 一行笼统异常（r20 裸 rclick 实锤），agent 无从得知正确用法。
        int need;
        switch (p[0]) {
            case "move": case "press": case "release": case "click": case "rclick":
            case "ctile": case "probe": case "strtbl": case "dlg": case "tapk": case "sel":
            case "tile":
                need = 3; break;
            case "drag":
                need = 5; break;
            case "train": case "build":
                need = 4; break;
            case "gather":
                need = 5; break;
            case "assign":
                need = 4; break;
            case "rally":
                need = 3; break;
            case "retask":
                need = 4; break;
            case "hdr9":
                need = 3; break;
            case "goto":
                need = 3; break;
            case "key": case "until": case "replaytrace": case "script":
            case "fields": case "save": case "load": case "dump": case "stopat":
            case "count": case "slots":
                need = 2; break;
            case "state": case "exit": case "sitrep": case "ping": case "aistate":
                need = 1; break;
            default:
                System.out.println("[devMouse] unknown cmd: " + line);
                return true;
        }
        if (p.length < need) {
            System.out.println("[devMouse] missing args, need " + (need - 1)
                + " (got " + (p.length - 1) + "): " + line);
            return true;
        }
        try {
            switch (p[0]) {
                case "ping":
                    // handler 存活心跳：echo 端用它一秒判读端死活（r21 指令雨打死
                    // handler 后 agent 拿 pong 失败即知该重启进程，而非盲发）。
                    System.out.println("[devMouse] pong ar=" + this.tickCount);
                    break;
                case "move":
                    this.mouseA(0, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                case "press":
                    this.mouseA(1, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                case "release":
                    this.mouseA(2, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                case "click": {
                    int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]);
                    this.mouseA(1, x, y);
                    Thread.sleep(200);      // 留 ≥1 帧让 j() 完成像素拾取
                    this.mouseA(2, x, y);
                    break;
                }
                case "rclick":
                    this.mouseA(3, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                case "sel": {
                    // 宏：按 tile 直选该格的单位/建筑。绕过像素拾取(相机缓动/精灵截胡/
                    // 落点漂移三坑)。tile 编码见 mapTiles: 0x200=单位 0x100=建筑,
                    // 低字节=槽位序号, bit10-11=owner。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    int v = this.mapTiles[tx + (ty << 6)];
                    int cls = v & 0x300, owner = (v & 0xC00) >> 10, ord = v & 0xFF;
                    if (cls == 0x200 || cls == 0x100) {
                        if (this.selectionMark > 0) {
                            // 替换语义：selectUnderCursor 不清旧组，残留的 0x8000 sel 位
                            // 会让 goto 连带旧组一起走（r22 全军撞敌团灭事故根因）。
                            this.clearSelection();
                        }
                        this.selectUnderCursor(owner, cls, ord);
                        this.selectionMode = 6;
                        System.out.println("[devMouse] sel OK p" + owner
                            + (cls == 0x200 ? " unit" : " building")
                            + " type=" + this.selectedType + " at (" + tx + "," + ty + ")");
                    } else {
                        // sel 失败=未选中任何东西；残留旧组会让后续裸 goto 打到旧组上
                        // （r31 全军被拉回北辕事故）——FAIL 路径同样清选中。
                        this.clearSelection();
                        System.out.println("[devMouse] sel FAIL (" + tx + "," + ty
                            + ") class=0x" + Integer.toHexString(cls) + " — 空地/资源/雾");
                    }
                    break;
                }
                case "hdr9": {
                    // 宏：hdr9 <tx> <ty> —— 直写玩家伐木场交存点指针 hdr[9]（-1=无）。
                    // 伪造到袋内空地=载满回送变"走到袋心闲置"，绕开不可达 TC 的回送
                    // orbit（m1 三连 LOSS 根因）。到空地不触发交付钩子，村民停下待重新
                    // 派工；趟数在砍完一载时已扣，隧道不需要真实入账。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    this.playerUnitHeaders[0][9] = (short)(tx << 8 | ty);
                    System.out.println("[devMouse] hdr9 -> (" + tx + "," + ty + ")");
                    break;
                }
                case "retask": {
                    // 宏：retask <slot> <tx> <ty> —— 按槽位直写任务目标（p0 单位）。
                    // 与 orderMove 的选中路径同款三写（slot[2]=目标 slot[1]=现位 word7=0），
                    // 但不经过 sel——orbit 中的单位位置秒变，sel 坐标必落空（m1 录制局实锤）。
                    // 用途：解"回送中卡死"（目标格设为可达空地，word7 清 0 走过去）与
                    // 免 sel 派工。落点=资源格时同 assign 触发采集。
                    int slotIdx = Integer.parseInt(p[1]);
                    int tx = Integer.parseInt(p[2]) & 0x3F, ty = Integer.parseInt(p[3]) & 0x3F;
                    short[] tab = this.playerUnitSlots[0];
                    int off = slotIdx << 3;
                    if (slotIdx < 0 || slotIdx >= this.playerUnitHeaders[0][2]) {
                        System.out.println("[devMouse] retask FAIL slot" + slotIdx
                            + " 越界(n=" + this.playerUnitHeaders[0][2] + ")");
                    } else {
                        int w7 = tab[off + 7] & 0xFFFF;
                        tab[off + 2] = (short)(tx << 8 | ty);
                        tab[off + 1] = tab[off + 0];
                        tab[off + 7] = 0;
                        System.out.println("[devMouse] retask slot" + slotIdx + " ("
                            + (tab[off] >>> 8) + "," + (tab[off] & 0xFF) + ")w" + Integer.toHexString(w7)
                            + " -> (" + tx + "," + ty + ")");
                    }
                    break;
                }
                case "rally": {
                    // 宏：rally <tx> <ty> —— 全体军事单位(type>=2)逐类扩选+下令集结。
                    // 绕开 goto all 的 x0q 空集结群(选中被移动自清后扩选空集, r24 破案)：
                    // 这里每个 type 独立 selectUnits+orderMove，互不依赖选中存活。
                    // r31 修复：门槛/回显改真单位计数 devCountUnits——原 a(0,t,false)
                    // 数的是建筑数组，真有兵的 type 因无同名建筑全被 skip（实测零移动），
                    // 且回显的"单位数"实为建筑数。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    int sent = 0, types = 0;
                    for (int t = 2; t < 16; ++t) {
                        int cnt = this.devCountUnits(0, t);
                        if (cnt <= 0) {
                            continue;
                        }
                        this.selectUnits(0, t);
                        this.orderMove(0, tx, ty);
                        ++types;
                        sent += cnt;
                    }
                    System.out.println("[devMouse] rally OK " + sent + " 单位(" + types
                        + " 个兵种) → (" + tx + "," + ty + ")");
                    break;
                }
                case "gather": {
                    // 宏：gather <村民tx> <村民ty> <资源tx> <资源ty> —— 直绑采集
                    // （sel 村民→orderMove 资源格），省掉 agent 侧 sel 失败/落点漂移
                    // 两步试错（r23 需求）。资源格须已探明（雾格会吞令）。
                    int vtx = Integer.parseInt(p[1]) & 0x3F, vty = Integer.parseInt(p[2]) & 0x3F;
                    int rtx = Integer.parseInt(p[3]) & 0x3F, rty = Integer.parseInt(p[4]) & 0x3F;
                    int vv = this.mapTiles[vtx + (vty << 6)];
                    if ((vv & 0x300) != 0x200) {
                        System.out.println("[devMouse] gather FAIL (" + vtx + "," + vty
                            + ") 无单位(站桩单位请用 ctile 选中)");
                    } else if ((this.mapTiles[rtx + (rty << 6)] & 0x8000) != 0) {
                        System.out.println("[devMouse] gather FAIL (" + rtx + "," + rty
                            + ") 资源格在雾中(先探图)");
                    } else {
                        if (this.selectionMark > 0) {
                            this.clearSelection();
                        }
                        this.selectUnderCursor((vv & 0xC00) >> 10, 0x200, vv & 0xFF);
                        this.selectionMode = 6;
                        this.orderMove(0, rtx, rty);
                        System.out.println("[devMouse] gather OK 村民(" + vtx + "," + vty
                            + ") → 资源(" + rtx + "," + rty + ")");
                    }
                    break;
                }
                case "assign": {
                    // 宏：assign <rtx> <rty> [n] —— 把最多 n 个闲置村民绑到资源格。
                    // 闲置判定=游戏自己的标准：type≤1 且 slot word7 低 nibble==0
                    // （selectUnits 的村民分支同款；word7: 0=闲置 1=行军 2=采集中
                    // [高字节0x66=满载计时,bit4-5=资源种] 3=回送中——r32 读码钉死）。
                    // 逐个 clearSelection+selectUnderCursor+orderMove（命令落点=资源格
                    // 触发采集，r22 读码）。n 省略=全部闲置村民。
                    int rtx = Integer.parseInt(p[1]) & 0x3F, rty = Integer.parseInt(p[2]) & 0x3F;
                    int want = p.length > 3 ? Integer.parseInt(p[3]) : Integer.MAX_VALUE;
                    int rv = this.mapTiles[rtx + (rty << 6)];
                    if ((rv & 0x8000) != 0) {
                        System.out.println("[devMouse] assign FAIL (" + rtx + "," + rty
                            + ") 资源格在雾中(先探图)");
                    } else if ((rv & 0x300) != 0x300 || (rv & 3) == 0) {
                        System.out.println("[devMouse] assign FAIL (" + rtx + "," + rty
                            + ") 非资源格(raw=0x" + Integer.toHexString(rv & 0xFFFF) + ")");
                    } else {
                        int ucnt = this.playerUnitHeaders[0][2];
                        int done = 0;
                        StringBuilder sb = new StringBuilder("[devMouse] assign ");
                        for (int i = 0; i < ucnt && done < want; ++i) {
                            int off = i << 3;
                            short[] tab = this.playerUnitSlots[0];
                            int t = tab[off + 3] & 0xFF;
                            if (t > 1 || (tab[off + 7] & 0xF) != 0) {
                                continue;
                            }
                            this.clearSelection();
                            this.selectUnderCursor(0, 0x200, i);
                            this.selectionMode = 6;
                            this.orderMove(0, rtx, rty);
                            sb.append("slot").append(i).append("@(").append(tab[off] >>> 8)
                                .append(',').append(tab[off] & 0xFF).append(") ");
                            ++done;
                        }
                        System.out.println(sb.append("→ 资源(").append(rtx).append(',').append(rty)
                            .append(") 绑 ").append(done).append(" 个闲置村民 (种=林/金/石码 ")
                            .append(rv & 3).append(')').toString());
                    }
                    break;
                }
                case "goto": {
                    // 宏：对当前选中单位下移动令(可选 all=扩选全体同类再下令)。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    boolean all = p.length > 3 && "all".equals(p[3]);
                    if (this.selectionMark != 512 || this.selectionPlayer != 0) {
                        System.out.println("[devMouse] goto FAIL 无有效选中(先 sel)");
                    } else {
                        if (all && this.selectedType >= 0) {
                            this.selectUnits(0, this.selectedType);
                        }
                        this.orderMove(0, tx, ty);
                        System.out.println("[devMouse] goto OK (" + tx + "," + ty + ")"
                            + (all ? " x" + this.playerUnitHeaders[0][49] + "q" : "")
                            + " type=" + this.selectedType);
                    }
                    break;
                }
                case "train": {
                    // train 宏：光标直置建筑格排队 n 个(生产即排队, §手册4.4), 返回
                    // 实际排队数(pop/canAfford 拒时如实报)。绕过拾取/面板/4s超时全链。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    int want = Integer.parseInt(p[3]);
                    int v = this.mapTiles[tx + (ty << 6)];
                    if ((v & 0x300) != 0x100 || ((v & 0xC00) >> 10) != 0) {
                        System.out.println("[devMouse] train FAIL (" + tx + "," + ty
                            + ") 非己方建筑");
                    } else if (!this.openBuildingMenu(v & 0xFF)) {
                        System.out.println("[devMouse] train FAIL 菜单拒绝(在建/门控/无军事选中?)");
                    } else if (this.var_int_g != 0) {
                        System.out.println("[devMouse] train FAIL type 非生产建筑(g=" + this.var_int_g
                            + " — TC/研究类建筑不产兵)");
                    } else {
                        // selectedTrainProduct→单位 type 恒等映射（r20 实测: 0村民/2民兵/3剑士/4弓兵/5侦察）。
                        // 不走 y()——它依赖菜单态的 var_int_arr_c 槽位表，宏上下文为 null。
                        int q0 = this.playerUnitHeaders[0][49];
                        int got = 0;
                        for (int i = 0; i < want; ++i) {
                            if (this.playerUnitHeaders[0][2] + this.playerUnitHeaders[0][49]
                                < this.playerUnitHeaders[0][3] && this.canAfford(0, 0, this.selectedTrainProduct)) {
                                this.queueUnitTraining(0, this.selectedTrainProduct);
                                ++got;
                            }
                        }
                        System.out.println("[devMouse] train OK 排队 " + got + "/" + want
                            + " aK=" + this.selectedTrainProduct + " (aK=0村民 2/3步兵 4弓兵 5/6骑兵)");
                    }
                    break;
                }
                case "build": {
                    // build 宏：直接放置建筑(付费+施工中), 绕过 选村民→35→槽→臂置→
                    // 方向键→落位 七步链(臂置间歇失灵/parity 死角全规避)。经济约束
                    // (canAfford)与数量上限仍由游戏逻辑执行。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    int type = Integer.parseInt(p[3]) & 0xFF;
                    boolean hasVillager = false;
                    for (int i = 0; i < this.playerUnitHeaders[0][2]; ++i) {
                        int t = this.playerUnitSlots[0][(i << 3) + 3] & 0xFF;
                        if (t == 0 || t == 1) {
                            hasVillager = true;
                            break;
                        }
                    }
                    if (!hasVillager) {
                        System.out.println("[devMouse] build FAIL 无村民(建造主体)");
                    } else if (this.mapTiles[tx + (ty << 6)] < 0) {
                        System.out.println("[devMouse] build FAIL (" + tx + "," + ty + ") 迷雾格未探索");
                    } else if ((this.mapTiles[tx + (ty << 6)] & 0x300) != 0) {
                        System.out.println("[devMouse] build FAIL (" + tx + "," + ty
                            + ") 被单位/建筑/资源占格");
                    } else if (type != 1 && type != 11 && type != 12 && this.techFlags[10 + type] == 0) {
                        System.out.println("[devMouse] build FAIL type" + type + " 已建成过(不可重复)");
                    } else if ((type == 11 && this.countBuildings(0, 11, false) >= 4)
                        || (type == 12 && this.countBuildings(0, 12, false) >= 5)
                        || (type == 1 && this.countBuildings(0, 1, false) >= 2)) {
                        System.out.println("[devMouse] build FAIL type" + type + " 达数量上限");
                    } else if (!this.canAfford(0, 1, type)) {
                        System.out.println("[devMouse] build FAIL 资源不足 type" + type);
                    } else {
                        int off = this.a(0, type, tx, ty, 0x40000000, true);
                        if (off < 0) {
                            System.out.println("[devMouse] build FAIL 格被占/建筑数满(22)");
                        } else {
                            System.out.println("[devMouse] build OK type" + type + " @("
                                + tx + "," + ty + ") 施工中(约1.3s完工)");
                        }
                    }
                    break;
                }
                case "ctile": {
                    // tile 直点：用当前相机把 tile 中心换算回屏幕坐标再走 click 链路。
                    // 普通 click 的屏幕坐标→tile 映射随镜头缓动漂移（鼠标拾取用的是
                    // 拾取那一刻的相机），远距精确点击不可行；ctile 用实时 cameraPx
                    // 计算，无漂移。
                    // 坐标基准（第14轮玩家实测钉死）：FIFO mouse 坐标喂给 j() 的拾取
                    // 管线，是**物理帧缓冲空间**（=逻辑×SCALE，默认 480x640），不是
                    // 240x320 逻辑空间——tile 菱形中心 = (32*(tx-ty)-2*camX-64,
                    // 16*(tx+ty)-2*camY+19)。早版用逻辑基准(16/8,+76)导致系统性
                    // 偏左上、远点常被邻格精灵截胡。
                    int tx = Integer.parseInt(p[1]), ty = Integer.parseInt(p[2]);
                    int sx = 32 * (tx - ty) - 2 * this.cameraPxX - 64;
                    int sy = 16 * (tx + ty) - 2 * this.cameraPxY + 19;
                    this.mouseA(1, sx, sy);
                    Thread.sleep(200);
                    this.mouseA(2, sx, sy);
                    System.out.println("[devMouse] ctile " + tx + "," + ty + " -> screen "
                        + sx + "," + sy + " (cam " + this.cameraPxX + "," + this.cameraPxY + ")");
                    break;
                }
                case "drag": {
                    int x1 = Integer.parseInt(p[1]), y1 = Integer.parseInt(p[2]);
                    int x2 = Integer.parseInt(p[3]), y2 = Integer.parseInt(p[4]);
                    this.mouseA(1, x1, y1);
                    for (int i = 1; i <= 8; ++i) {
                        Thread.sleep(60);
                        this.mouseA(0, x1 + (x2 - x1) * i / 8, y1 + (y2 - y1) * i / 8);
                    }
                    Thread.sleep(200);
                    this.mouseA(2, x2, y2);
                    // 防楔死(第9轮):终点在贴边滚动区时,合成鼠标永远停留在那里,
                    // 边缘滚动每帧钉死光标/probe 全 -2,只能重启。回屏中心解除。
                    if (x2 >= this.screenW - 14 || y2 >= this.screenH - 14
                            || x2 < 14 || y2 < 14) {
                        this.mouseA(0, this.screenW / 2, this.screenH / 2);
                        System.out.println("[devMouse] drag ended in edge-scroll zone, mouse re-centered");
                    }
                    break;
                }
                case "key": {
                    int kc = Integer.parseInt(p[1]);
                    this.onKeyPress(kc);
                    // 注入即松开（延迟队列）：否则 ab 永久保持，o()/e() 的按住重复
                    // 会让方向键每帧漂移一格，FIRE 也会被当成持续按住。
                    var_com_ulysseo_mad_b_a.queueSyntheticKeyRelease(kc);
                    break;
                }
                case "tapk": {
                    // 带验证重试的按键：注入单发键有"帧末 ax=0 vs 激活判断"竞态，
                    // 可能被无痕吞掉——按 screenState 是否到达期望值决定重注（最多 tries 次）。
                    int kc = Integer.parseInt(p[1]);
                    int expect = Integer.parseInt(p[2]);
                    int tries = p.length > 3 ? Integer.parseInt(p[3]) : 5;
                    for (int i = 0; i < tries && this.screenState != expect; ++i) {
                        this.onKeyPress(kc);
                        var_com_ulysseo_mad_b_a.queueSyntheticKeyRelease(kc);
                        Thread.sleep(1000);
                    }
                    System.out.println("[devMouse] tapk " + kc + " expect aA=" + expect
                        + ": " + (this.screenState == expect ? "ok" : "timeout") + " (aA=" + this.screenState + ")");
                    break;
                }
                case "count": {
                    // 宏：count <type> —— 双方真单位计数（devCountUnits 基元）。
                    // "全军就位判定"：连续两次 count 数值不变=集结/到货完成（r30/r31 需求）。
                    int t = Integer.parseInt(p[1]);
                    System.out.println("[devMouse] count t" + t
                        + " 我=" + this.devCountUnits(0, t)
                        + " 敌=" + this.devCountUnits(1, t));
                    break;
                }
                case "slots": {
                    // 诊断：打印 p 玩家单位槽位 {i:type@(tx,ty)w任务字}（只读不进 trace）。
                    // 任务字=word7 hex：低 nibble 0闲置/1行军/2采集(高字节0x66=满载计时)/3回送。
                    int pl = Integer.parseInt(p[1]) & 1;
                    int ucnt = this.playerUnitHeaders[pl][2];
                    StringBuilder sb2 = new StringBuilder("[slots] p" + pl + " n=" + ucnt);
                    for (int i = 0, off = 0; i < ucnt; ++i, off += 8) {
                        sb2.append(' ').append(i).append(":t")
                            .append(this.playerUnitSlots[pl][off + 3] & 0xFF)
                            .append("@(").append(this.playerUnitSlots[pl][off] >>> 8)
                            .append(',').append(this.playerUnitSlots[pl][off] & 0xFF)
                            .append(")w").append(Integer.toHexString(
                                this.playerUnitSlots[pl][off + 7] & 0xFFFF));
                    }
                    System.out.println(sb2);
                    break;
                }
                case "state": {
                    System.out.println("[devMouse] aA=" + this.screenState + "/" + this.pendingScreenState
                        + " cursor=(" + this.cursorTileX + "," + this.cursorTileY + ") cam=(" + this.cameraPxX + "," + this.cameraPxY + ")"
                        + " Q=" + this.cursorTileIdx + " aE=" + this.selectionMark + " Y=" + this.selectionPlayer + " sel=" + this.selectionMode);
                    StringBuilder json = new StringBuilder(512);
                    json.append("{\"aA\":").append(this.screenState).append(",\"am\":").append(this.pendingScreenState)
                        .append(",\"ar\":").append(this.tickCount)
                        .append(",\"cursor\":[").append(this.cursorTileX).append(',').append(this.cursorTileY).append(']')
                        .append(",\"cam\":[").append(this.cameraPxX).append(',').append(this.cameraPxY).append(']')
                        .append(",\"sel\":").append(this.selectionMode).append(",\"aE\":").append(this.selectionMark)
                        .append(",\"res\":[").append(this.playerUnitHeaders[0][5]).append(',')
                        .append(this.playerUnitHeaders[0][6]).append(',')
                        .append(this.playerUnitHeaders[0][7]).append(']')
                        .append(",\"pop\":[").append(this.playerUnitHeaders[0][2]).append(',')
                        .append(this.playerUnitHeaders[0][3]).append(']')
                        .append(",\"queued\":").append(this.playerUnitHeaders[0][49])
                        .append(",\"ai\":").append(this.var_int_i);
                    if (this.mapTiles != null) {
                        // explored = 无 0x8000 迷雾位的格子数（装载时大面积置位、随探索
                        // 经矩形填充助手清除；regress golden 开局值 298）。另一迷雾位
                        // 0x4000 由 l(int,int) 每帧在单位周围置位、渲染走暗化分支——
                        // 疑为"当前可见/已探明"二级标记，语义未完全考证，勿据字面 grep
                        // 下结论：0x8000 的置位走参数间接传入（a(int,int,int,int,int)）。
                        int explored = 0;
                        for (int i = 0; i < this.mapTiles.length; ++i) {
                            if ((this.mapTiles[i] & 0x8000) == 0) {
                                ++explored;
                            }
                        }
                        json.append(",\"explored\":").append(explored)
                            .append(",\"tiles\":").append(this.mapTiles.length);
                    }
                    json.append(",\"units\":[");
                    // 双方单位都列出，带 "p" 阵营列——第9轮教训：只列 P0 时，AI 单位
                    // 位置被误读成"我方单位瞬移/乱走"（死亡后槽位压缩又放大了错觉）
                    int unitTotal = 0;
                    for (int pl = 0; pl < 2 && unitTotal < 32; ++pl) {
                        int ucnt = Math.min(this.playerUnitHeaders[pl][2], 32);
                        for (int i = 0; i < ucnt && unitTotal < 64; ++i) {
                            int off = i * 8;
                            if (unitTotal > 0) {
                                json.append(',');
                            }
                            json.append("{\"p\":").append(pl)
                                .append(",\"tile\":[").append(this.playerUnitSlots[pl][off] >>> 8)
                                .append(',').append(this.playerUnitSlots[pl][off] & 0xFF)
                                .append("],\"type\":").append(this.playerUnitSlots[pl][off + 3] & 0xFF)
                                .append(",\"sel\":").append((this.playerUnitSlots[pl][off + 4] & 0x8000) != 0)
                                .append('}');
                            ++unitTotal;
                        }
                    }
                    json.append("]}\n");
                    if (this.devFifoPath != null) {
                        try {
                            java.nio.file.Files.write(java.nio.file.Path.of(this.devFifoPath + ".json"),
                                json.toString().getBytes("UTF-8"));
                        } catch (Exception e) {
                            System.out.println("[devMouse] json: " + e);
                        }
                    }
                    for (int pl = 0; pl < 2; ++pl) {
                        int ucnt = Math.min(this.playerUnitHeaders[pl][2], 32);
                        for (int i = 0; i < ucnt; ++i) {
                            int off = i * 8;
                            System.out.println("[devMouse] p" + pl + " unit " + i
                                + " tile=(" + (this.playerUnitSlots[pl][off] >>> 8)
                                + "," + (this.playerUnitSlots[pl][off] & 0xFF) + ")"
                                + " type=" + (this.playerUnitSlots[pl][off + 3] & 0xFF)
                                + " sel=" + ((this.playerUnitSlots[pl][off + 4] & 0x8000) != 0));
                        }
                    }
                    break;
                }
                case "aistate": {
                    // 全量结构化状态（玩家 AI / 批量实验用）：写 <fifo>.aistate.json
                    // ——不动 state 指令的 <fifo>.json（那是 golden/回放契约）。
                    // 与 state 的差别：双方单位不设 32 条上限、带 slot/prev/target/hp/
                    // action，另含双方 headers 关键字段、techFlags、全部建筑记录。
                    StringBuilder aj = new StringBuilder(4096);
                    aj.append("{\"tick\":").append(this.tickCount)
                        .append(",\"aA\":").append(this.screenState)
                        .append(",\"gameMode\":").append(this.gameMode);
                    if (this.techFlags != null) {
                        aj.append(",\"techFlags\":[");
                        for (int i = 0; i < this.techFlags.length; ++i) {
                            if (i > 0) {
                                aj.append(',');
                            }
                            aj.append(this.techFlags[i]);
                        }
                        aj.append(']');
                    }
                    aj.append(",\"players\":[");
                    for (int pl = 0; pl < 2; ++pl) {
                        if (pl > 0) {
                            aj.append(',');
                        }
                        // headers: [0]=时代 [2]现役单位 [3]人口上限 [4]建筑记录数
                        // [5..7]木金石 [8]=该方 TC/基地格（双方都在 [8]，AI 防守逻辑
                        // 读 hdr[1][8]、进攻目标读 hdr[0][8] 可为证）[9]=类型0建筑放置
                        // 记录 [49]训练队列长 [55]军队价值 [86..90]生产/阵亡/建成/建筑损失/交存趟数
                        int[] h = this.playerUnitHeaders[pl];
                        aj.append("{\"age\":").append(h[0])
                            .append(",\"units\":").append(h[2])
                            .append(",\"popCap\":").append(h[3])
                            .append(",\"buildings\":").append(h[4])
                            .append(",\"res\":[").append(h[5]).append(',').append(h[6]).append(',').append(h[7]).append(']')
                            .append(",\"tcTile\":").append(h[8])
                            .append(",\"hq9\":").append(h[9])
                            .append(",\"trainQueue\":").append(h[49])
                            .append(",\"armyValue\":").append(h[55])
                            .append(",\"produced\":").append(h[86])
                            .append(",\"lost\":").append(h[87])
                            .append(",\"built\":").append(h[88])
                            .append(",\"buildingsLost\":").append(h[89])
                            .append(",\"deliveries\":").append(h[90])
                            .append('}');
                    }
                    aj.append(']');
                    if (this.mapTiles != null) {
                        int explored = 0;
                        for (int i = 0; i < this.mapTiles.length; ++i) {
                            if ((this.mapTiles[i] & 0x8000) == 0) {
                                ++explored;
                            }
                        }
                        aj.append(",\"explored\":").append(explored);
                    }
                    aj.append(",\"units\":[");
                    boolean firstU = true;
                    for (int pl = 0; pl < 2; ++pl) {
                        short[] s = this.playerUnitSlots[pl];
                        for (int i = 0; i < this.playerUnitHeaders[pl][2]; ++i) {
                            int off = i * 8;
                            if (!firstU) {
                                aj.append(',');
                            }
                            firstU = false;
                            aj.append("{\"p\":").append(pl)
                                .append(",\"slot\":").append(i)
                                .append(",\"tile\":[").append((s[off] & 0xFFFF) >>> 8).append(',').append(s[off] & 0xFF).append(']')
                                .append(",\"prevTile\":[").append((s[off + 1] & 0xFFFF) >>> 8).append(',').append(s[off + 1] & 0xFF).append(']')
                                .append(",\"target\":[").append((s[off + 2] & 0xFFFF) >>> 8).append(',').append(s[off + 2] & 0xFF).append(']')
                                .append(",\"type\":").append(s[off + 3] & 0xFF)
                                .append(",\"hp\":").append(s[off + 4] & 0xFF)
                                .append(",\"action\":").append(s[off + 7] & 0xF)
                                .append(",\"sel\":").append((s[off + 4] & 0x8000) != 0)
                                .append('}');
                        }
                    }
                    aj.append(']');
                    aj.append(",\"buildingRecs\":[");
                    boolean firstB = true;
                    for (int pl = 0; pl < 2; ++pl) {
                        int[] r = this.buildingTable[pl];
                        for (int i = 0; i < this.playerUnitHeaders[pl][4]; ++i) {
                            int base = i * 4;
                            if (!firstB) {
                                aj.append(',');
                            }
                            firstB = false;
                            // 记录布局（放置代码 a(int,int,int,int,int,boolean)）：
                            // [+0]=格 x<<8|y [+1]=类型（箭塔带高字节打包，取 &0xFF）
                            // [+2] 低字节=HP，0x40000000=在建，0x20000000=研究中
                            aj.append("{\"p\":").append(pl)
                                .append(",\"slot\":").append(i)
                                .append(",\"type\":").append(r[base + 1] & 0xFF)
                                .append(",\"tile\":[").append((r[base] >>> 8) & 0xFF).append(',').append(r[base] & 0xFF).append(']')
                                .append(",\"hp\":").append(r[base + 2] & 0xFF)
                                .append(",\"uc\":").append((r[base + 2] & 0x40000000) != 0)
                                .append('}');
                        }
                    }
                    aj.append("]}\n");
                    if (this.devFifoPath != null) {
                        try {
                            java.nio.file.Files.write(java.nio.file.Path.of(this.devFifoPath + ".aistate.json"),
                                aj.toString().getBytes("UTF-8"));
                        } catch (Exception e) {
                            System.out.println("[devMouse] aistate: " + e);
                        }
                    }
                    System.out.println("[devMouse] aistate written, ar=" + this.tickCount);
                    break;
                }
                case "tile": {
                    // 诊断：打印一格 mapTiles 原值与分解（类目/owner/序号/雾/地表）。
                    int tx = Integer.parseInt(p[1]) & 0x3F, ty = Integer.parseInt(p[2]) & 0x3F;
                    int v = this.mapTiles[tx + (ty << 6)];
                    String st = "";
                    if ((v & 0x300) == 0x100 && (v & 0xC00) == 0) {
                        int s = this.buildingTable[0][((v & 0xFF) << 2) + 2];
                        st = " bldStatus=0x" + Integer.toHexString(s)
                            + (((s & 0x40000000) != 0) ? " 在建" + (s & 0xFF) : (s & 0xFF) == 255 ? " 完工" : " ?" + (s & 0xFF));
                    }
                    System.out.println("[devMouse] tile (" + tx + "," + ty + ") raw=0x"
                        + Integer.toHexString(v & 0xFFFF)
                        + " class=0x" + Integer.toHexString(v & 0x300)
                        + " owner=" + ((v & 0xC00) >> 10)
                        + " ord=" + (v & 0xFF)
                        + (v < 0 ? " 雾" : "") + ((v & 0x4000) != 0 ? " 地表" : "") + st);
                    break;
                }
                case "sitrep": {
                    // 一行战况摘要：agent 每次决策只需这一行，替代全量 state 轮询。
                    StringBuilder sr = new StringBuilder(256);
                    sr.append("[devMouse] sitrep ar=").append(this.tickCount)
                        .append(" aA=").append(this.screenState).append("/").append(this.pendingScreenState)
                        .append(" cur=(").append(this.cursorTileX).append(',').append(this.cursorTileY).append(')')
                        .append(" res=").append(this.playerUnitHeaders[0][5]).append('/')
                        .append(this.playerUnitHeaders[0][6]).append('/').append(this.playerUnitHeaders[0][7])
                        .append(" pop=").append(this.playerUnitHeaders[0][2]).append('/').append(this.playerUnitHeaders[0][3])
                        .append(" q=").append(this.playerUnitHeaders[0][49])
                        .append(" ai=").append(this.var_int_i)
                        .append(" sel=").append(this.selectionMark);
                    int[] mine = new int[16], foe = new int[16];
                    int foeN = 0, foeSx = 0, foeSy = 0;
                    for (int pl = 0; pl < 2; ++pl) {
                        int ucnt = this.playerUnitHeaders[pl][2];
                        int[] tab = pl == 0 ? mine : foe;
                        for (int i = 0; i < ucnt; ++i) {
                            int off = i << 3;
                            int t = this.playerUnitSlots[pl][off + 3] & 0xFF;
                            if (t < 16) {
                                ++tab[t];
                            }
                            if (pl == 1) {
                                ++foeN;
                                foeSx += this.playerUnitSlots[pl][off] >>> 8;
                                foeSy += this.playerUnitSlots[pl][off] & 0xFF;
                            }
                        }
                    }
                    sr.append(" 我军:");
                    for (int t = 0; t < 16; ++t) {
                        if (mine[t] > 0) {
                            sr.append('t').append(t).append('x').append(mine[t]).append(' ');
                        }
                    }
                    sr.append("敌军:").append(foeN);
                    for (int t = 0; t < 16; ++t) {
                        if (foe[t] > 0) {
                            sr.append(" t").append(t).append('x').append(foe[t]);
                        }
                    }
                    if (foeN > 0) {
                        sr.append(" 质心=(").append(foeSx / foeN).append(',').append(foeSy / foeN).append(')');
                    }
                    System.out.println(sr);
                    break;
                }
                case "until": {
                    // 阻塞等 screenState 到目标值（把"发命令→sleep→grep 日志"循环挪进游戏进程）
                    int target = Integer.parseInt(p[1]);
                    long timeout = p.length > 2 ? Long.parseLong(p[2]) * 1000L : 30000L;
                    long start = System.currentTimeMillis();
                    while (this.screenState != target && System.currentTimeMillis() - start < timeout) {
                        Thread.sleep(100);
                    }
                    System.out.println("[devMouse] until aA=" + target + ": "
                        + (this.screenState == target ? "ok" : "timeout") + " after "
                        + (System.currentTimeMillis() - start) + "ms");
                    break;
                }
                case "probe": {
                    // 只拾取不动镜头：屏幕→格子标定用
                    long seqBefore = this.mousePickSeq;
                    this.mousePickX = Integer.parseInt(p[1]);
                    this.mousePickY = Integer.parseInt(p[2]);
                    this.mousePickPending = true;
                    long start = System.currentTimeMillis();
                    while (this.mousePickSeq == seqBefore && System.currentTimeMillis() - start < 800) {
                        Thread.sleep(30);
                    }
                    System.out.println("[probe] " + this.mousePickX + "," + this.mousePickY
                        + " -> tile=" + (this.mousePickSeq == seqBefore ? -2 : this.mousePickTile));
                    break;
                }
                case "strtbl": {
                    // 字符串表考古：打印 data.res 字符串表条目。`strtbl <表> <条目>`
                    // 打单条，`strtbl <表> all` 顺序打到末尾（文本考古/BUG-005 类排查用）
                    int tbl = Integer.parseInt(p[1]);
                    a table = new a(tbl);
                    if (p.length > 2 && "all".equals(p[2])) {
                        for (int i = 0; i < 64; ++i) {
                            String s = table.a(i);
                            if (s == null) {
                                break;
                            }
                            System.out.println("[strtbl] " + tbl + "[" + i + "] = " + s);
                        }
                    } else {
                        System.out.println("[strtbl] " + tbl + "[" + p[2] + "] = "
                            + table.a(Integer.parseInt(p[2])));
                    }
                    break;
                }
                case "dlg": {
                    // 强制打开结算/简报对话框（startMissionBriefing 0,dialogScriptId,v）复现渲染问题
                    this.startMissionBriefing(0, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    System.out.println("[devMouse] dlg z=" + p[1] + " v=" + p[2]
                        + " -> aA=" + this.screenState);
                    break;
                }
                case "script": {
                    // 批量执行指令文件：每行一条，#注释，"sleep 毫秒" 等待
                    if (this.devInScript) {
                        System.out.println("[devMouse] nested script ignored");
                        break;
                    }
                    this.devInScript = true;
                    try (java.io.BufferedReader sr = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(p[1]), "UTF-8"))) {
                        String sl;
                        while ((sl = sr.readLine()) != null) {
                            sl = sl.trim();
                            if (sl.isEmpty() || sl.startsWith("#")) {
                                continue;
                            }
                            if (sl.startsWith("sleep ")) {
                                Thread.sleep(Integer.parseInt(sl.substring(6).trim()));
                                continue;
                            }
                            System.out.println("[devMouse] script> " + sl);
                            if (!devMouseCmd(sl)) {
                                this.devInScript = false;
                                return false;
                            }
                        }
                    } finally {
                        this.devInScript = false;
                    }
                    break;
                }
                case "fields":
                    try (java.io.PrintWriter w = new java.io.PrintWriter(p[1], "UTF-8")) {
                        aoe.DevFields.dump(this, AgeOfEmpires.b.class, w);
                    }
                    System.out.println("[devMouse] fields dumped to " + p[1]);
                    break;
                case "save":
                    // 裸文件名（无路径分隔）统一解析到 saveDir——落 CWD 曾把仓库根
                    // 弄出裸 s14（r21 事故源，agent 建议采纳）。
                    this.devSaveTo(p.length > 1 ? devResolveSavePath(p[1]) : devSaveDir() + "/quick.aoesave");
                    break;
                case "load":
                    this.devLoadFrom(p.length > 1 ? devResolveSavePath(p[1]) : devSaveDir() + "/quick.aoesave");
                    Thread.sleep(300);      // 等帧首 EDT 应用
                    break;
                case "dump":
                    var_com_ulysseo_mad_b_a.dumpFramebuffer(p[1]);
                    System.out.println("[devMouse] dumped " + p[1]);
                    break;
                case "replaytrace": {
                    // 确定性回放（调度表式）：载入 trace 到 devTraceTicks/Cmds，由模拟
                    // 循环帧首（onPaint 排空段）统一应用到期事件——与 play 的写宏队列
                    // 同一条帧首路径，±1 tick 竞态从结构上消失（旧版 dev 线程自旋
                    // 等待的 application 时刻落在帧内随机位置，1884 事件可累计出
                    // 284 tick 的终局漂移）。行格式与 mktrace 产出一致：
                    //   t <相对tick> key <键码> | move <x> <y> | fifo <命令...>
                    // 相对 tick 原点：load 存档后的 tickCount（快照钉住），可选第三参
                    // 显式指定。
                    String file = p[1];
                    long base = this.devTraceBaseTick >= 0 ? this.devTraceBaseTick : this.tickCount;
                    if (p.length > 2) {
                        base = Long.parseLong(p[2]);
                    }
                    this.devTraceTicks.clear();
                    this.devTraceCmds.clear();
                    this.devTracePos = 0;
                    int played = 0;
                    try (java.io.BufferedReader tr = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(file), "UTF-8"))) {
                        String tl;
                        while ((tl = tr.readLine()) != null) {
                            tl = tl.trim();
                            if (tl.isEmpty() || tl.startsWith("#")) {
                                continue;
                            }
                            String[] q = tl.split("\\s+");
                            if (!"t".equals(q[0])) {
                                System.out.println("[devMouse] replaytrace bad line: " + tl);
                                continue;
                            }
                            long at = base + Long.parseLong(q[1]);
                            if ("key".equals(q[2])) {
                                this.devTraceTicks.add(at);
                                this.devTraceCmds.add("key " + q[3]);
                            } else if ("move".equals(q[2])) {
                                this.devTraceTicks.add(at);
                                this.devTraceCmds.add("move " + q[3] + " " + q[4]);
                            } else if ("fifo".equals(q[2])) {
                                // 宏/输入指令（sel/goto/train/build/retask/...）。控制流指令
                                // （load/save/replaytrace/script/stopat/exit/until）拒绝——
                                // 会破坏 tick 锚定或嵌套死循环。
                                String rest = String.join(" ", java.util.Arrays.copyOfRange(q, 3, q.length));
                                if ("replaytrace".equals(q[3]) || "script".equals(q[3]) || "load".equals(q[3])
                                        || "save".equals(q[3]) || "stopat".equals(q[3]) || "exit".equals(q[3])
                                        || "until".equals(q[3])) {
                                    System.out.println("[devMouse] replaytrace: 拒绝控制流指令: " + rest);
                                    continue;
                                }
                                this.devTraceTicks.add(at);
                                this.devTraceCmds.add(rest);
                            } else {
                                System.out.println("[devMouse] replaytrace unknown op: " + tl);
                                continue;
                            }
                            ++played;
                        }
                    }
                    System.out.println("[devMouse] replaytrace scheduled: " + played + " events, base=" + base);
                    break;
                }
                case "stopat": {
                    // 确定性停表：等到 tickCount>=目标后取消主循环 Timer。用于对拍
                    // 时把两次运行冻在同一 tick 上再取 state（回放结束后若任由墙钟
                    // 推进，ar 会抖 ±几 tick，tile 级比较会假失败）。1ms 轮询下
                    // tickCount==目标必落在该帧 paint 窗口内，cancel 生效时下一帧
                    // 尚未开始（固定周期下帧间隙 ≈30ms），停点是确定的。
                    long target = Long.parseLong(p[1]);
                    while (this.tickCount < target) {
                        Thread.sleep(1);
                    }
                    this.h();
                    System.out.println("[devMouse] stopped at ar=" + this.tickCount);
                    break;
                }
                case "exit":
                    return false;
                default:
                    System.out.println("[devMouse] unknown: " + line);
            }
        } catch (Exception e) {
            System.out.println("[devMouse] cmd '" + line + "': " + e);
        }
        System.out.flush();     // 重定向到文件时 println 不是行缓冲，保证指令打印立即可见
        return true;
    }
    // ===== dev 鼠标驱动结束 =====
    // ===== dev 模式结束 =====

    // ===== 玩家 AI 接口（移植新增） =====
    // -Daoe.playerAi=<全限定类名>：帧首 hook。onPaint 帧首（Timer/paint 线程，
    // 与模拟同线程）每帧调一次 aoe.ai.PlayerAi.tick(this)，AI 内部自行节流。
    // 反射装载失败或 tick 抛异常：打 [ai] 日志并永久禁用，不影响游戏本身。
    // AI 可读面 = 本类 public 字段（playerUnitHeaders/playerUnitSlots/
    // buildingTable/mapTiles/techFlags/tickCount/screenState/gameMode/相机光标）；
    // 写操作原语 = orderMove/selectUnits/clearSelection/selectUnderCursor/
    // queueUnitTraining/canAfford/payCost/findAiBuildSpot/findNearbyResource/
    // a(player,type,tx,ty,flags,bl)/tryResearch（其中 orderMove/queueUnitTraining/
    // findAiBuildSpot/findNearbyResource 原先是包可见，为此放宽为 public）。
    private static final String PLAYER_AI_CLASS = System.getProperty("aoe.playerAi");
    private aoe.ai.PlayerAi playerAiHook;
    private boolean playerAiDisabled;

    private void tickPlayerAi() {
        if (this.playerAiHook == null) {
            if (this.playerAiDisabled) {
                return;
            }
            try {
                this.playerAiHook = (aoe.ai.PlayerAi) Class.forName(PLAYER_AI_CLASS)
                    .getDeclaredConstructor().newInstance();
                System.out.println("[ai] player AI loaded: " + PLAYER_AI_CLASS);
            } catch (Throwable t) {
                this.playerAiDisabled = true;
                System.out.println("[ai] load failed: " + PLAYER_AI_CLASS + ": " + t);
                return;
            }
        }
        try {
            this.playerAiHook.tick(this);
        } catch (Throwable t) {
            this.playerAiDisabled = true;
            this.playerAiHook = null;
            System.out.println("[ai] tick exception, AI disabled: " + t);
            t.printStackTrace();
        }
    }

    /** 研究/升时代原语：与 y() case 2 完全同语义的核心三步（写建筑槽研究标志
     *  0x20000000|0x10000、清进度字节 0xFFFF00FF、payCost），但显式传建筑槽位
     *  下标（buildingSlot = buildingTable[player] 的记录起点，4 int/记录；
     *  UI 侧对应"选中建筑槽"字段 aD），不依赖当前选中状态，供玩家 AI 调用。
     *  可用性校验镜像 openBuildingMenu（研究菜单门真正生效的地方——研究菜单
     *  var_boolean_g=false，boolean_k case 2 的 techFlags 过滤对研究菜单不生效，
     *  首批实现误把它当菜单门，导致升时代永远被拒，2026-09-02 玩家 AI 实测暴露）：
     *  - techFlags[23+techId] != 0 = 该科技已研究（j() 研究完成时置位）→ 拒绝；
     *  - 升时代（21/22/23）只在 TC(9)、techId == 21+当前时代，且前置建筑达标
     *    （封建：建成兵营≥1；城堡：磨坊+铁匠铺≥2；帝国：城堡≥1）；
     *  - 其余科技按 openBuildingMenu 的建筑类型→可研究科技+时代门槛表。
     *  y() case 2 保持原样不改，保证默认行为逐字节不变。 */
    public final boolean tryResearch(int player, int buildingSlot, int techId) {
        int[] recs = this.buildingTable[player];
        if (buildingSlot < 0 || buildingSlot + 3 >= recs.length) {
            return false;
        }
        if ((recs[buildingSlot + 2] & 0x40000000) != 0) {
            return false;   // 建筑未建成
        }
        if ((recs[buildingSlot + 2] & 0x20000000) != 0) {
            return false;   // 该建筑已有研究在进行
        }
        int btype = recs[buildingSlot + 3] & 0xFF;
        int age = this.playerUnitHeaders[player][0];
        if (techId >= 21 && techId <= 23) {
            // 升时代：openBuildingMenu case 9
            if (btype != 9 || techId != 21 + age || age >= 3) {
                return false;
            }
            if (age == 0 && this.countBuildings(player, 10, true) < 1) {
                return false;
            }
            if (age == 1 && this.countBuildings(player, 5, true) + this.countBuildings(player, 6, true) < 2) {
                return false;
            }
            if (age == 2 && this.countBuildings(player, 3, true) < 1) {
                return false;
            }
        } else {
            if (techId < 0 || 23 + techId >= this.techFlags.length
                    || this.techFlags[23 + techId] != 0) {
                return false;   // 已研究（或非法 id）
            }
            if (!researchOfferedAt(btype, techId, age, this.techFlags)) {
                return false;
            }
        }
        if (!this.canAfford(player, 2, techId)) {
            return false;
        }
        recs[buildingSlot + 2] |= 0x20000000 | 0x10000;
        recs[buildingSlot + 2] &= 0xFFFF00FF;
        this.payCost(player, 2, techId);
        return true;
    }

    /** openBuildingMenu 的"建筑当前提供哪项科技"表的镜像（不含已研究检查——调用方已查）。
     *  塔升级链（13→17→20）额外要求前一级已研究。 */
    private static boolean researchOfferedAt(int btype, int techId, int age, byte[] techFlags) {
        switch (btype) {
            case 4:  // 大学
                return (techId == 14 || techId == 11) && age >= 2
                    || (techId == 10 || techId == 12) && age >= 3;
            case 0:  // 伐木场
                return techId == 3 && age >= 1 || techId == 1 && age >= 2;
            case 1:  // 采矿场
                return (techId == 5 || techId == 9) && age >= 1
                    || (techId == 19 || techId == 18) && age >= 2;
            case 5:  // 磨坊
                return techId == 6;
            case 6:  // 铁匠铺
                return (techId == 4 || techId == 8) && age >= 1
                    || (techId == 7 || techId == 2) && age >= 2
                    || (techId == 0 || techId == 15) && age >= 3;
            case 12: // 哨塔（塔升级链）
                return techId == 13 && age >= 1
                    || techId == 17 && age >= 2 && techFlags[36] != 0
                    || techId == 20 && age >= 3 && techFlags[40] != 0;
            default:
                return false;
        }
    }
    // ===== 玩家 AI 接口结束 =====

    public final void loadKeymap(int n) {
        this.keymap = com.ulysseo.mad.c.byte_arr_a(n);
        this.keymapCount = this.keymap.length >> 1;
    }

    // ===== 桌面鼠标增强（Canvas→mad.b 转发；仅在任务主视图生效）=====
    // 悬停：只记录拾取结果做高亮，不平移镜头（桌面惯例）。
    // 左键单击：光标/镜头直达点击格并触发确认（等价"移过去 + FIRE"）。
    // 左键拖动：框选本方单位（复刻 h() 的多选）。
    // 右键：有选中单位时全体移动到鼠标格（复刻 d(0,tx,ty) 指令路径），无选中时取消选择。
    // 鼠标贴窗口边缘（<=14 逻辑像素）：持续向该方向平移（桌面 RTS 惯例）。
    // 屏幕→格子不做投影换算，而是由 j() 的格子遍历直接做像素拾取（mousePick* / mouseInsideTile）。
    public boolean mouseBandActive;
    public boolean mouseBandDragging;
    public boolean mouseBandCorner1Set;
    public int mouseBandX1;
    public int mouseBandY1;
    public int mouseBandX2;
    public int mouseBandY2;
    private int mouseBandTx1;
    private int mouseBandTy1;
    private int mouseBandTx2;
    private int mouseBandTy2;
    private int mousePickX = -1;
    private int mousePickY = -1;
    private boolean mousePickPending;
    private int mousePickTile = -1;
    private int mousePickSeq;
    private int mouseAppliedSeq;
    private int mouseLastTile = -1;
    private int mouseScreenX = -1;
    private int mouseScreenY = -1;
    private int mouseHighlightTile = -1;
    public int mouseHiX = -1;
    public int mouseHiY = -1;
    private int mouseEdgeTick;
    private boolean mouseRclickPending;
    private int mouseRclickGrace;

    @Override
    public void mouseA(int kind, int mx, int my) {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[mouseA] kind=" + kind + " aA=" + this.screenState);
            // 确定性回放 trace 行（tools/replaycheck.sh）：ar= 发生时的 tick
            System.out.println("[input] ar=" + this.tickCount + " move " + mx + " " + my);
        }
        if (this.screenState != 6 && this.screenState != 1) {
            return;
        }
        switch (kind) {
            case 0: {
                this.mouseScreenX = mx;
                this.mouseScreenY = my;
                this.mousePickX = mx;
                this.mousePickY = my;
                this.mousePickPending = true;
                if (this.mouseBandDragging) {
                    this.mouseBandX2 = mx;
                    this.mouseBandY2 = my;
                    this.mouseBandActive = true;
                }
                break;
            }
            case 1: {
                this.mouseBandDragging = true;
                this.mouseBandActive = false;
                this.mouseBandCorner1Set = false;
                this.mouseBandX1 = this.mouseBandX2 = mx;
                this.mouseBandY1 = this.mouseBandY2 = my;
                this.mousePickX = mx;
                this.mousePickY = my;
                this.mousePickPending = true;
                break;
            }
            case 2: {
                this.mouseBandDragging = false;
                if (this.mouseBandActive && this.mouseBandCorner1Set
                        && (Math.abs(this.mouseBandX2 - this.mouseBandX1) > 8
                        || Math.abs(this.mouseBandY2 - this.mouseBandY1) > 8)) {
                    this.mouseBandSelect();
                } else {
                    int tile = this.mousePickTile >= 0 ? this.mousePickTile : this.mouseLastTile;
                    if (tile >= 0) {
                        // 桌面惯例：点击 = 光标直达该格并就地确认
                        this.cursorTileX = tile & 63;
                        this.cursorTileY = tile >> 6;
                        this.mouseApplyQ();
                    }
                    // 走键盘 FIRE 管道；松开走延迟队列（复用防吞点按机制）
                    this.onKeyPress(-5);
                    var_com_ulysseo_mad_b_a.queueSyntheticKeyRelease(-5);
                }
                this.mouseBandActive = false;
                break;
            }
            case 3: {
                if (this.screenState != 6) {
                    break;      // 全图视图里右键无意义，也别把指令挂起到回主视图后
                }
                // 右键不直接用 mouseLastTile（那是上一帧悬停的拾取结果）：快速甩动后
                // 立刻右键、或程序化注入没有悬停轨迹时，它会指向旧格子或 -1。
                // 刷新拾取点并把指令挂起到 mouseTick 里新拾取生效后再发。
                this.mousePickX = mx;
                this.mousePickY = my;
                this.mousePickPending = true;
                this.mouseRclickPending = true;
                this.mouseRclickGrace = 3;
                break;
            }
            case 4: {
                // 鼠标离开窗口：停用边缘滚动
                this.mouseScreenX = -1;
                this.mouseScreenY = -1;
                break;
            }
        }
    }

    /** 复刻 f() 的光标格索引计算（含虚空格失效），供"点击直达"使用。 */
    private void mouseApplyQ() {
        int idx = this.cursorTileX + (this.cursorTileY << 6) & 0xFFF;
        if ((idx & 0xFFFFF000) != 0 || (this.mapTiles[idx] & 0xFFF) == 768) {
            this.cursorTileIdx = -1;
        } else {
            this.cursorTileIdx = idx;
        }
    }

    /** 每帧（a(Graphics) 开头）：应用 j() 最近一次拾取结果。只看序号不看 pending——
     *  pending 由 j() 拾取成功时清理；拖动中事件比 tick 密，若在这里等 pending
     *  归零会被新事件无限饿死。 */
    private void mouseTick() {
        if (this.mousePickSeq != this.mouseAppliedSeq) {
            this.mouseAppliedSeq = this.mousePickSeq;
            if (this.mousePickTile >= 0) {
                this.mouseLastTile = this.mousePickTile;
                this.mouseHighlightTile = this.mousePickTile;
                if (this.mouseBandDragging) {
                    // 拖动中：第一个拾取结果给框选起点，后续给终点
                    if (!this.mouseBandCorner1Set) {
                        this.mouseBandTx1 = this.mousePickTile & 63;
                        this.mouseBandTy1 = this.mousePickTile >> 6;
                        this.mouseBandTx2 = this.mouseBandTx1;
                        this.mouseBandTy2 = this.mouseBandTy1;
                        this.mouseBandCorner1Set = true;
                        if (System.getProperty("aoe.debug") != null) {
                            System.out.println("[corner1] " + this.mouseBandTx1 + "," + this.mouseBandTy1);
                        }
                    } else {
                        this.mouseBandTx2 = this.mousePickTile & 63;
                        this.mouseBandTy2 = this.mousePickTile >> 6;
                        if (System.getProperty("aoe.debug") != null) {
                            System.out.println("[corner2] " + this.mouseBandTx2 + "," + this.mouseBandTy2);
                        }
                    }
                }
            }
        }
        // 右键待定指令：等本次拾取生效（≤3 帧宽限）；落点在虚空/地图外则丢弃
        // （不清选中、不移动——用旧格子发移动比什么都不做更糟）。
        if (this.mouseRclickPending) {
            if (this.mousePickTile >= 0 && !this.mousePickPending) {
                this.mouseRclickPending = false;
                this.mouseCommand();
            } else if (--this.mouseRclickGrace < 0) {
                this.mouseRclickPending = false;
                this.mousePickPending = false;
            }
        }
        // 边缘滚动：鼠标贴窗口边缘时按方向键同款步进平移（半速），桌面 RTS 惯例
        if (this.screenState == 6 && this.mouseScreenX >= 0) {
            final int edge = 14;
            int tx = this.cursorTileX;
            int ty = this.cursorTileY;
            if (this.mouseScreenX <= edge) {
                --tx;
                ++ty;
            } else if (this.mouseScreenX >= javax.microedition.lcdui.Screen.WIDTH - edge) {
                ++tx;
                --ty;
            }
            if (this.mouseScreenY <= edge) {
                --tx;
                --ty;
            } else if (this.mouseScreenY >= javax.microedition.lcdui.Screen.HEIGHT - edge) {
                ++tx;
                ++ty;
            }
            if ((tx != this.cursorTileX || ty != this.cursorTileY) && (++this.mouseEdgeTick & 1) == 0) {
                this.cursorTileX = Math.max(0, Math.min(63, tx));
                this.cursorTileY = Math.max(0, Math.min(63, ty));
            }
        }
    }

    /** 点 (px,py) 是否落在以 (x,yTop) 为上顶点、64x32 的菱形格内。 */
    private boolean mouseInsideTile(int px, int py, int x, int yTop) {
        int adx = px - x;
        if (adx < 0) {
            adx = -adx;
        }
        int ady = py - yTop - 16;
        if (ady < 0) {
            ady = -ady;
        }
        return adx * 16 + ady * 32 <= 512;
    }

    /** 框选：选中两角格构成的矩形内（格子坐标）的本方单位，收尾与 h()（按类型多选）一致。 */
    private void mouseBandSelect() {
        int tx1 = Math.min(this.mouseBandTx1, this.mouseBandTx2);
        int tx2 = Math.max(this.mouseBandTx1, this.mouseBandTx2);
        int ty1 = Math.min(this.mouseBandTy1, this.mouseBandTy2);
        int ty2 = Math.max(this.mouseBandTy1, this.mouseBandTy2);
        this.clearSelection();
        int count = this.playerUnitHeaders[0][2];
        int firstType = -1;
        int selected = 0;
        int off = 0;
        boolean dbg = System.getProperty("aoe.debug") != null;
        for (int i = 0; i < count; ++i, off += 8) {
            int packed = this.playerUnitSlots[0][off + 0];
            int utx = packed >>> 8;
            int uty = packed & 0xFF;
            if (dbg) {
                System.out.println("[band] unit " + i + " tile=" + utx + "," + uty);
            }
            if (utx >= tx1 && utx <= tx2 && uty >= ty1 && uty <= ty2) {
                this.playerUnitSlots[0][off + 4] |= 0x8000;
                ++selected;
                if (firstType < 0) {
                    firstType = this.playerUnitSlots[0][off + 3] & 0xFF;
                }
            }
        }
        if (dbg) {
            System.out.println("[band] rect (" + tx1 + "," + ty1 + ")-(" + tx2 + "," + ty2
                + ") count=" + count + " selected=" + selected);
        }
        if (selected > 0) {
            this.selectionMark = 512;
            this.selectionPlayer = 0;
            this.selectedSlot = -1;
            this.selectedType = firstType;
            this.var_boolean_h = true;
            this.selectionMode = 6;
        }
    }

    /** 右键：有本方选中单位 → 全体移动到鼠标格（与游戏命令路径一致，含落点标记）；
     *  无选中 → 清除选择（等价游戏自身的取消）。 */
    private void mouseCommand() {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[rcmd] Y=" + this.selectionPlayer + " aE=" + this.selectionMark
                + " lastTile=" + this.mouseLastTile + " sel=" + this.selectionMode);
        }
        if (this.selectionPlayer == 0 && this.selectionMark == 512 && this.mouseLastTile >= 0) {
            int tx = this.mouseLastTile & 63;
            int ty = this.mouseLastTile >> 6;
            this.var_int_d = this.cameraPxX + this.cursorScreenPxX;
            this.S = this.cameraPxY + this.cursorScreenPxY;
            this.au = this.screenH;
            this.U = 8;
            this.orderMove(0, tx, ty);
        } else {
            this.clearSelection();
            this.selectionMode = 0;
        }
    }
    // ===== 桌面鼠标增强结束 =====

    public final void commandAction(Command command, Displayable displayable) {
        this.onKeyPress(command.getCommandType());
    }

    public final void loadNfo() {
        try {
            RecordStore recordStore = RecordStore.openRecordStore((String)".nfo", (boolean)true);
            if (recordStore.getNumRecords() == 0) {
                recordStore.addRecord(this.nfoData, 0, this.nfoData.length);
            } else {
                this.nfoData = recordStore.getRecord(1);
            }
            recordStore.closeRecordStore();
            return;
        }
        catch (RecordStoreException recordStoreException) {
            return;
        }
    }

    public final void saveNfo() {
        try {
            RecordStore recordStore = RecordStore.openRecordStore((String)".nfo", (boolean)true);
            if (recordStore.getNumRecords() == 0) {
                recordStore.addRecord(this.nfoData, 0, this.nfoData.length);
            } else {
                recordStore.setRecord(1, this.nfoData, 0, this.nfoData.length);
            }
            recordStore.closeRecordStore();
            return;
        }
        catch (RecordStoreException recordStoreException) {
            return;
        }
    }

    public final int nfoReadInt(int n, int n2) {
        int n3 = 0;
        while (--n2 >= 0) {
            n3 <<= 8;
            n3 |= this.nfoData[n + n2] & 0xFF;
        }
        return n3;
    }

    public final void nfoWriteInt(int n, int n2, int n3) {
        int n4 = n3;
        while (n2 > 0) {
            this.nfoData[n++] = (byte)(n4 & 0xFF);
            n4 >>= 8;
            --n2;
        }
    }

    /** 主循环体：框架每 80ms 调 paint → 这里（paint-driven），先推进游戏逻辑再渲染，
     *  ar 计帧。屏幕切换按 screenState 分发（见 GAME_NOTES.md 的状态机速查）。 */
    /** dev HUD（-Daoe.devHud=1）：画面顶部两行状态，dump 截图自描述。 */
    private void devDrawHud(Graphics graphics) {
        graphics.setColor(0);
        graphics.fillRect(0, 27, this.screenW, 26);
        graphics.setColor(0xFFFFFF);
        String line1 = "aA=" + this.screenState + "/" + this.pendingScreenState + " ar=" + this.tickCount;
        if (this.menuTree != null) {
            line1 += " node=" + this.menuNode + " Z=" + this.menuHighlight + "/" + this.menuNodeCount + " H=" + this.menuScreenId;
        }
        String pick = this.mouseLastTile < 0 ? "-"
            : (this.mouseLastTile & 63) + "," + (this.mouseLastTile >> 6);
        String line2 = "cur=" + this.cursorTileX + "," + this.cursorTileY + " cam=" + this.cameraPxX + "," + this.cameraPxY
            + " pick=" + pick + " sel=" + this.selectionMode + "/" + (this.selectionMark & 0xFFFF);
        graphics.drawString(line1, 2, 28, 20);
        graphics.drawString(line2, 2, 40, 20);
    }

    /** 存/读档结果提示（居中，2s）。按逻辑屏宽居中，宽屏（aoe.width）下同样适用。 */
    private void devDrawToast(Graphics graphics) {
        if (this.devToast == null || System.currentTimeMillis() >= this.devToastUntil) {
            return;
        }
        graphics.setClip(0, 0, this.screenW, this.screenH);
        graphics.setColor(0);
        graphics.fillRect(this.screenW - 140 >> 1, 46, 140, 16);
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.devToast, this.screenW >> 1, 49, 17);    // TOP|HCENTER
    }

    public final void onPaint(Graphics graphics) {
        ++this.tickCount;
        this.devFrameHousekeeping();
        if (PLAYER_AI_CLASS != null) {
            this.tickPlayerAi();
        }
        if (this.var_boolean_j) {
            if (Runtime.getRuntime().freeMemory() < 50000L) {
                return;
            }
            this.var_boolean_j = false;
        }
        if (AgeOfEmpires.b.var_boolean_a) {
            AgeOfEmpires.b.var_boolean_b = true;
            this.clipLeft = 0;
            this.clipTop = 0;
            this.clipRight = this.screenW;
            this.clipBottom = this.screenH;
            graphics.setClip(0, 0, this.screenW, this.screenH);
            graphics.setColor(0);
            graphics.fillRect(0, 0, this.screenW, this.screenH);
            this.clipTop = 0;
            this.clipBottom = this.screenH;
            AgeOfEmpires.b.var_boolean_a = false;
            this.s();
            this.var_boolean_h = false;
            this.var_boolean_j = true;
            switch (this.screenState) {
                case 0: 
                case 10: 
                case 12: {
                    return;
                }
                case 2: {
                    if (this.overlayPrevState == 1) {
                        this.renderMapView(graphics);
                    } else if (this.overlayPrevState == 4 && this.dialogScriptId != 98) {
                        this.renderMenu(graphics);
                    } else {
                        this.renderWorld(graphics);
                    }
                    this.clipLeft = 0;
                    this.clipTop = 0;
                    this.clipRight = this.screenW;
                    this.clipBottom = this.screenH;
                    return;
                }
                case 1: {
                    this.boolean_j(0);
                    return;
                }
                case 11: {
                    this.beginMissionLoad(0);
                    this.aQ = 0;
                    return;
                }
                case 4: 
                case 9: {
                    this.pendingScreenState = 4;
                    this.screenState = 9;
                    return;
                }
            }
            this.renderWorld(graphics);
            this.c(graphics);
            return;
        }
        try {
            boolean bl = false;
            graphics.setFont(this.var_javax_microedition_lcdui_Font_a);
            graphics.setClip(this.clipLeft, this.clipTop, this.clipRight - this.clipLeft, this.clipBottom - this.clipTop);
            if (this.boolean_a()) {
                this.dispatchRender(graphics);
                graphics.setClip(0, 0, this.screenW, this.screenH);
                if (DEV_HUD) {
                    this.devDrawHud(graphics);
                }
                this.devDrawToast(graphics);
                if (this.var_boolean_f) {
                    this.a(graphics, 21, 4, this.screenH - 6, 14, 0, 7, 6, 0, 0);
                }
                if (this.var_boolean_b) {
                    this.a(graphics, 21, this.screenW - 10, this.screenH - 6, 21, 0, 7, 6, 0, 0);
                }
                // dev 写宏帧首排空：任何屏状态都先应用（弹窗态按键类操作不被饿死）
                if (!this.devOpQueue.isEmpty()) {
                    this.devDraining = true;
                    try {
                        String op;
                        while ((op = this.devOpQueue.poll()) != null) {
                            devMouseCmd(op);
                        }
                    } finally {
                        this.devDraining = false;
                    }
                }
                if (this.devTracePos < this.devTraceTicks.size()) {
                    this.devDraining = true;
                    try {
                        while (this.devTracePos < this.devTraceTicks.size()
                                && this.devTraceTicks.get(this.devTracePos) <= this.tickCount) {
                            devMouseCmd(this.devTraceCmds.get(this.devTracePos));
                            ++this.devTracePos;
                        }
                    } finally {
                        this.devDraining = false;
                    }
                    if (this.devTracePos >= this.devTraceTicks.size()) {
                        System.out.println("[devMouse] replaytrace done: " + this.devTracePos
                            + " events, ar=" + this.tickCount);
                    }
                }
                switch (this.screenState) {
                    case 7: {
                        this.tickMissionScript();
                        break;
                    }
                    case 2: 
                    case 4: 
                    case 5: 
                    case 9: 
                    case 10: 
                    case 11: 
                    case 12: 
                    case 13: 
                    case 14: {
                        break;
                    }
                    default: {
                        if (this.bgmFramesLeft-- <= 0) {
                            this.playNextBgm();
                        }
                        this.g();
                        this.p();
                        this.B();
                        this.aimProjectiles();
                        this.tickProjectiles();
                        this.tickAi();
                        this.j();
                        this.tickMissionScript();
                        if (this.ag <= 0) break;
                        if (this.var_java_lang_String_a != null) {
                            graphics.setColor(0);
                            graphics.drawString(this.var_java_lang_String_a, this.clipLeft + 2, this.clipBottom - (this.ah - this.ay), 20);
                            graphics.setColor(0xFFFFFF);
                            graphics.drawString(this.var_java_lang_String_a, this.clipLeft + 1, this.clipBottom - (this.ah - this.ay + 1), 20);
                        }
                        if (this.var_java_lang_String_c != null) {
                            graphics.setColor(0);
                            graphics.drawString(this.var_java_lang_String_c, this.clipLeft + 2, this.clipBottom - ((this.ah << 1) - this.ay), 20);
                            graphics.setColor(0xFFFFFF);
                            graphics.drawString(this.var_java_lang_String_c, this.clipLeft + 1, this.clipBottom - ((this.ah << 1) - this.ay + 1), 20);
                        }
                        --this.ag;
                        if (this.ag != 0) break;
                        this.var_java_lang_String_a = null;
                        this.var_java_lang_String_c = null;
                    }
                }
                if (this.clipBottom != this.screenH && this.screenState == 6 || this.screenState == 7 && this.W != 0) {
                    int n = 0;
                    int n2 = this.bottomBarY;
                    int n3 = this.screenW - 41;
                    int n4 = this.a(this.var_java_lang_String_b);
                    graphics.setClip(41, n2, n3, this.var_int_f);
                    graphics.setColor(0);
                    graphics.fillRect(41, n2, n3, this.var_int_f);
                    n = n4 > n3 ? 41 - (this.R++ << 1) % (n3 + n4) : 41 - n3;
                    graphics.setColor(0xFFFFFF);
                    graphics.drawString(this.var_java_lang_String_b, n + n3, n2 + this.ay, 20);
                }
                this.beginStateTransition();
            }
            if (AgeOfEmpires.b.var_boolean_b) {
                if (!AgeOfEmpires.b.c) {
                    AgeOfEmpires.b.var_boolean_b = false;
                    return;
                }
                if (this.mediaRequestId < 0) {
                    return;
                }
                if (Runtime.getRuntime().freeMemory() >= 30000L && AgeOfEmpires.b.a(this.mediaRequestId, this.var_boolean_c)) {
                    AgeOfEmpires.b.var_boolean_b = false;
                }
            }
            if (this.var_boolean_h) {
                this.s();
                this.var_boolean_h = false;
                this.var_boolean_j = true;
            }
            return;
        }
        catch (Exception exception) {
            // 画面帧内异常绝不能静默：静默吞掉的表现就是"画面冻在最后一帧/全蓝、
            // 按键全无响应"而日志毫无线索。始终带上状态上下文打印。
            System.out.println("[paint] exception aA=" + this.screenState + "/" + this.pendingScreenState
                + " ar=" + this.tickCount + " sel=" + this.selectionMode);
            exception.printStackTrace();
            return;
        }
    }

    final boolean boolean_a() {
        if (this.pendingScreenState == this.screenState) {
            return true;
        }
        boolean bl = false;
        switch (this.pendingScreenState) {
            case 11: {
                bl = this.beginMissionLoad(this.aH);
                break;
            }
            case 1: {
                bl = this.boolean_j(this.aH);
                break;
            }
            case 2: {
                bl = this.n(this.aH);
                break;
            }
            case 10: {
                bl = this.boolean_a(this.aH);
                break;
            }
            case 5: {
                bl = this.setupMissionEnv(this.aH);
                break;
            }
            case 6: {
                bl = this.boolean_h(this.aH);
                break;
            }
            case 7: {
                bl = this.buildActionMenu(this.aH);
                break;
            }
            case 8: {
                bl = this.boolean_i(this.aH);
                break;
            }
            case 4: {
                bl = this.activateMenuScreen(this.aH);
                break;
            }
            case 9: {
                bl = this.boolean_e(this.aH);
                break;
            }
            case 12: {
                bl = this.boolean_f(this.aH);
            }
        }
        if (bl) {
            this.screenState = this.pendingScreenState;
            this.aH = 0;
            return true;
        }
        this.screenState = 0;
        ++this.aH;
        return false;
    }

    final void dispatchRender(Graphics graphics) {
        if (this.screenState == 0) {
            return;
        }
        switch (this.screenState) {
            case 11: {
                this.renderLoadingScreen(graphics);
                return;
            }
            case 1: {
                this.renderMapView(graphics);
                return;
            }
            case 2: {
                this.renderDialog(graphics);
                return;
            }
            case 10: {
                this.k(graphics);
                return;
            }
            case 5: {
                this.i(graphics);
                return;
            }
            case 6: {
                // -Daoe.noRender=1：跳过任务主视图渲染（批量玩家 AI 实验提速）。
                // 守卫只能放在这里、不能整跳 dispatchRender：菜单引擎（renderMenu）
                // 的按键消费与闪屏定时翻页都嵌在渲染函数里，整跳会冻住菜单导航。
                // 注意：鼠标像素拾取依赖渲染帧，noRender 下 FIFO probe/click/ctile
                // 失效；a(Graphics) 内嵌的 tickCursor/updateCamera/mouseTick 同样
                // 不跑（玩家 AI 走原语，不依赖这些）。
                if (!DEV_NO_RENDER) {
                    this.a(graphics);
                }
                return;
            }
            case 7: {
                this.l(graphics);
                return;
            }
            case 8: {
                this.c(graphics);
                return;
            }
            case 4: {
                this.renderMenu(graphics);
                return;
            }
            case 9: {
                this.o(graphics);
                return;
            }
            case 12: {
                this.b(graphics);
            }
        }
    }

    final void beginStateTransition() {
        if (this.screenState == this.pendingScreenState) {
            return;
        }
        switch (this.screenState) {
            case 11: {
                this.t();
                return;
            }
            case 1: {
                this.d();
                return;
            }
            case 2: {
                this.A();
                return;
            }
            case 10: {
                this.r();
                return;
            }
            case 5: {
                return;
            }
            case 6: {
                return;
            }
            case 7: {
                this.clearActionMenu();
                return;
            }
            case 8: {
                return;
            }
            case 4: {
                this.C();
                return;
            }
            case 9: {
                return;
            }
            case 12: {
                this.u();
            }
        }
    }

    final boolean requestStateSwitch(int n) {
        if (System.getProperty("aoe.debug") != null && n != 4 && n != 6) {
            System.out.println("[trace] g->" + n + " (aA=" + this.screenState + " am=" + this.pendingScreenState + ")");
        }
        if (this.pendingScreenState != this.screenState && n == 8) {
            return false;
        }
        if (this.pendingScreenState != this.screenState && n == 7) {
            return false;
        }
        if ((n == 7 || n == 8) && this.screenState == 1) {
            return false;
        }
        this.pendingScreenState = n;
        return true;
    }

    public final boolean activateMenuScreen(int n) {
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
        this.ag = 0;
        this.loadMenuScreen();
        if (n == 0) {
            if (this.pendingPanelSwitch >= 0) {
                this.menuTree[this.menuNode + 0] = (byte)this.pendingPanelSwitch;
            }
            this.var_boolean_f = true;
            this.var_boolean_b = true;
            switch (this.menuScreenId) {
                case 3: {
                    this.requestMedia(131, true);
                }
                case 2: 
                case 4: {
                    this.var_boolean_f = false;
                    this.var_boolean_b = false;
                    break;
                }
                case 6: {
                    this.var_boolean_f = true;
                    this.var_boolean_b = false;
                    break;
                }
                case 11: {
                    int n2 = this.int_c(4) + 9;
                    // 移植修改：随机地图 3 档全部解锁（原版 = aG + 1，按通关进度开放）
                    this.menuTree[n2 + 1] = 3;
                    this.menuTree[n2 + 2] = (byte)this.tutorialProgress;
                    this.requestMedia(131, true);
                    break;
                }
                case 12: {
                    int n3 = this.int_c(4) + 9;
                    // 移植修改：战役 7 关全部解锁（原版 = aj + 1，按通关进度开放）
                    this.menuTree[n3 + 1] = 7;
                    this.menuTree[n3 + 2] = (byte)this.campaignProgress;
                    this.requestMedia(131, true);
                    break;
                }
                case 7: {
                    int n4 = this.int_c(4) + 9;
                    boolean bl = false;
                    if (this.var_boolean_d) {
                        bl = true;
                    }
                    this.menuTree[n4 + 2] = (byte)(bl ? 1 : 0);
                    n4 = this.int_c(5) + 9;
                    bl = true;
                    if (AgeOfEmpires.b.c) {
                        bl = false;
                    }
                    this.menuTree[n4 + 2] = (byte)(bl ? 1 : 0);
                    break;
                }
                case 8: {
                    int n5 = this.int_c(5) + 9;
                    this.menuTree[n5 + 2] = (byte)this.pendingPanelSwitch;
                    break;
                }
                case 0: 
                case 10: {
                    this.requestMedia(131, true);
                }
            }
            return false;
        }
        return true;
    }

    public final void C() {
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
    }

    public final void renderMenu(Graphics graphics) {
        if (System.getProperty("aoe.debug") != null && this.keyActionPulse != 0) {
            System.out.println("[fHead] ax=" + this.keyActionPulse);
        }
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[fMenu] ao=" + this.menuNodeCount + " Z=" + this.menuHighlight + " aR=" + this.menuNode
                + " tmpl=" + (this.menuTree == null ? "null" : this.menuTree.length));
        }
        int n = this.menuNode + 6;
        int n2 = this.var_int_a + this.menuTree[this.menuNode + 4] >> 1;
        int n3 = this.K + this.menuTree[this.menuNode + 5] >> 1;
        this.var_int_a = n2;
        this.K = n3;
        int n4 = this.menuNode + 6;
        int n5 = 0;
        int n6 = 0;
        for (int i = 0; i < this.menuNodeCount; ++i) {
            byte by;
            if (i == this.menuHighlight) {
                n = n4;
            }
            if (((by = this.menuTree[n4 + 0]) & 0x20) == 0) {
                n4 = this.int_e(n4);
                continue;
            }
            if ((by & 0x40) != 0) {
                byte by2;
                byte by3 = this.menuTree[n4 + 1];
                if ((by3 & 0x7F) != 127) {
                    int n7 = this.int_c(by3 & 0x7F);
                    n5 = this.menuTree[n7 + 4];
                    n6 = this.menuTree[n7 + 5];
                }
                int n8 = this.menuTree[n4 + 6] & 0xFF;
                int n9 = this.menuTree[n4 + 7] & 0xFF;
                byte by4 = this.menuTree[n4 + 2];
                int n10 = this.menuTree[n4 + 3] & 0xFF;
                if ((by3 & 0xFFFFFF80) != 0) {
                    if (by4 == -1) {
                        n6 = -(this.screenH >> 3);
                    } else {
                        n5 += by4;
                        n6 = -4;
                    }
                } else {
                    n5 += n10 * AgeOfEmpires.b.int_b(by4 << 3) >> 16;
                    n6 += n10 * AgeOfEmpires.b.c(by4 << 3) >> 16;
                }
                int n11 = n5 + (this.screenW >> 1);
                int n12 = (this.screenH >> 1) - n6;
                if ((by & 4) == 0) {
                    n11 -= n2;
                    n12 += n3;
                }
                if (((by2 = this.menuTree[n4 + 8]) & 0x10) == 0) {
                    n11 = (by2 & 0x20) == 0 ? (n11 -= n8 >> 1) : (n11 -= n8);
                }
                if ((by2 & 0x40) == 0) {
                    n12 = (by2 & 0xFFFFFF80) == 0 ? (n12 -= n9 >> 1) : (this.screenH >> 2) - (n9 >> 1);
                }
                if (i == this.menuHighlight) {
                    if (n11 < this.clipLeft) {
                        int n13 = this.menuNode + 4;
                        this.menuTree[n13] = (byte)(this.menuTree[n13] - (this.clipLeft - n11));
                    }
                    if (n12 < this.clipTop) {
                        int n14 = this.menuNode + 5;
                        this.menuTree[n14] = (byte)(this.menuTree[n14] + (this.clipTop - n12));
                    }
                    if (n11 + n8 > this.clipRight) {
                        int n15 = this.menuNode + 4;
                        this.menuTree[n15] = (byte)(this.menuTree[n15] + (n11 + n8 - this.clipRight));
                    }
                    if (n12 + n9 > this.clipBottom) {
                        int n16 = this.menuNode + 5;
                        this.menuTree[n16] = (byte)(this.menuTree[n16] - (n12 + n9 - this.clipBottom));
                    }
                }
                if ((by & 0x10) != 0) {
                    if (n11 < 0) {
                        n5 -= n11;
                        n11 = 0;
                    }
                    if (n12 < 0) {
                        n6 += n12;
                        n12 = 0;
                    }
                    if (n11 + n8 > this.screenW) {
                        n5 += n11 - this.screenW;
                        n11 = this.screenW - n8;
                    }
                    if (n12 + n9 > this.screenH) {
                        n6 -= n12 - this.screenH;
                        n12 = this.screenH - n9;
                    }
                }
                this.menuTree[n4 + 4] = (byte)n5;
                this.menuTree[n4 + 5] = (byte)n6;
                switch (by2 & 0xF) {
                    case 7: {
                        graphics.setColor(this.menuTree[n4 + 9] & 0xFF, this.menuTree[n4 + 9 + 1] & 0xFF, this.menuTree[n4 + 9 + 2] & 0xFF);
                        graphics.fillRect(0, 0, this.screenW, this.screenH);
                        break;
                    }
                    case 3: {
                        byte by5 = this.menuTree[n4 + 9];
                        this.drawTileSprite(graphics, 10 + by5, n11, n12, 0);
                        break;
                    }
                    case 6: {
                        byte by5 = 0;
                        this.e(graphics, 0, 0, this.screenW, this.screenH);
                        break;
                    }
                    case 0: {
                        this.d(graphics, n11, n12, n4, i);
                        break;
                    }
                    case 2: {
                        this.c(graphics, n11, n12, n4, i);
                    }
                }
            } else {
                switch (this.menuTree[n4 + 8]) {
                    case 10: {
                        this.clipLeft = this.screenW * this.menuTree[n4 + 9] / 100;
                        this.clipTop = this.screenH * this.menuTree[n4 + 9 + 1] / 100;
                        this.clipRight = this.screenW * this.menuTree[n4 + 9 + 2] / 100;
                        this.clipBottom = this.screenH * this.menuTree[n4 + 9 + 3] / 100;
                        graphics.setClip(this.clipLeft, this.clipTop, this.clipRight - this.clipLeft, this.clipBottom - this.clipTop);
                        break;
                    }
                    case 4: {
                        if (this.aQ <= this.menuTree[n4 + 9]) break;
                        this.runMenuScript(n4, 0);
                        break;
                    }
                    case 5: {
                        if (System.getProperty("aoe.debug") != null) {
                            System.out.println("[menuGate] ax=" + this.keyActionPulse + " ab=" + this.keyActionHeld);
                        }
                        if (this.keyActionPulse != 38 && this.keyActionPulse != 22 && this.keyActionPulse != 6) break;
                        this.runMenuScript(n4, 0);
                    }
                }
            }
            n4 = this.int_e(n4);
        }
        this.void_d(n);
        if (this.keyActionHeld == 22) {
            this.keyActionHeld = 0;
        }
        this.keyActionPulse = 0;
        ++this.aQ;
    }

    public final void void_d(int n) {
        if (this.aQ < 10) {
            return;
        }
        this.menuTree[this.menuNode + 1] = (byte)this.menuHighlight;
        switch (this.keyActionPulse) {
            case 6: 
            case 22: 
            case 38: {
                this.pendingPanelSwitch = this.menuScreenId;
                this.runMenuScript(n, 1);
                return;
            }
            case 47: {
                if (this.menuScreenId >= 2 && this.menuScreenId <= 5) {
                    return;
                }
                if (!this.requestStateSwitch(9)) break;
                this.D();
                this.pendingPanelSwitch = -1;
                if (this.menuScreenId == 0) {
                    this.ap = 0;
                    this.menuTree[this.menuNode + 1] = (byte)(this.menuTree[this.menuNode + 2] - 1);
                    return;
                }
                this.ap = this.menuTree[this.menuNode + 0];
                return;
            }
            case 5: 
            case 21: {
                switch (this.menuTree[n + 8]) {
                    case 2: {
                        byte by = this.menuTree[n + 9 + 2];
                        this.menuTree[n + 9 + 2] = by > 0 ? (byte)(by - 1 & 0xFF) : (byte)(this.menuTree[n + 9 + 1] - 1);
                        this.runMenuScript(n, 0);
                    }
                }
                return;
            }
            case 7: 
            case 23: {
                switch (this.menuTree[n + 8]) {
                    case 2: {
                        byte by = this.menuTree[n + 9 + 2];
                        byte by2 = this.menuTree[n + 9 + 1];
                        this.menuTree[n + 9 + 2] = by < by2 - 1 ? (byte)(by + 1 & 0xFF) : (byte)0;
                        this.runMenuScript(n, 0);
                    }
                }
                return;
            }
            case 3: 
            case 19: {
                int n2 = this.menuHighlight;
                int n3 = 0;
                do {
                    if (--n2 >= 0) continue;
                    return;
                } while ((this.menuTree[n3 = this.int_c(n2)] & 0xFFFFFF80) == 0);
                this.menuHighlight = n2;
                return;
            }
            case 9: 
            case 25: {
                int n4 = this.menuHighlight;
                int n5 = 0;
                do {
                    if (++n4 < this.menuNodeCount) continue;
                    return;
                } while ((this.menuTree[n5 = this.int_c(n4)] & 0xFFFFFF80) == 0);
                this.menuHighlight = n4;
            }
        }
    }

    public final void d(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5 = this.aQ - n4;
        int n6 = this.menuTree[n3 + 9] & 0xFF;
        n2 += this.ay;
        if (this.menuTree[n3 + 0] == 104) {
            graphics.setColor(this.int_a(0xFFFFFF, 14595245, n5, 10));
            graphics.drawString(this.var_java_lang_String_arr_a[n6], n + 1, n2 + 1, 20);
            graphics.setColor(this.int_a(0xFF0000, 14595245, n5, 10));
            graphics.drawString(this.var_java_lang_String_arr_a[n6], n, n2, 20);
            return;
        }
        int n7 = 0;
        int n8 = 0xFFFFFF;
        if ((this.menuTree[n3 + 0] & 0xFFFFFF80) != 0) {
            if (n4 == this.menuHighlight) {
                n7 = 0xFF0000;
                n8 = 0xFFFFFF;
            }
        } else if ((n5 >>= 2) > 4) {
            n5 = 4;
        }
        graphics.setColor(this.int_a(n8, 14595245, n5, 10));
        graphics.drawString(this.var_java_lang_String_arr_a[n6], n + 1, n2 + 1, 20);
        graphics.setColor(this.int_a(n7, 14595245, n5, 10));
        graphics.drawString(this.var_java_lang_String_arr_a[n6], n, n2, 20);
    }

    public final void c(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5 = (this.menuTree[n3 + 9] & 0xFF) + (this.menuTree[n3 + 9 + 2] & 0xFF);
        int n6 = this.menuTree[n3 + 6] & 0xFF;
        int n7 = this.aQ - n4;
        boolean bl = false;
        int n8 = 0xFFFFFF;
        int n9 = 14595245;
        if (n4 == this.menuHighlight) {
            int n10 = (AgeOfEmpires.b.int_b(this.aQ << 7) >> 9) + 128 << 16;
            n10 = this.int_a(n10, n9, n7, 10);
            graphics.setColor(n10);
            graphics.fillRect(n, n2, n6, this.ah + 6);
            graphics.setColor(this.int_a(0xFFFFFF, n9, n7, 10));
            graphics.fillRect(n + 1, n2 + 1, n6 - 2, this.ah + 4);
            graphics.setColor(14595245);
            graphics.fillRect(n + 2, n2 + 2, n6 - 4, this.ah + 2);
            if (this.menuTree[n3 + 9 + 1] > 1) {
                int n11 = AgeOfEmpires.b.int_b(this.tickCount << 8) >> 14;
                if (n7 > 6) {
                    this.a(graphics, 21, n - n11 - 13, n2 + (this.ah >> 1), 28, 0, 7, 6, 0, 0);
                    this.a(graphics, 21, n + n6 + n11 + 6, n2 + (this.ah >> 1), 35, 0, 7, 6, 0, 0);
                }
            }
        } else if ((this.menuTree[n3 + 0] & 0xFFFFFF80) != 0) {
            graphics.setColor(this.int_a(0xD0D0D0, n9, n7, 10));
            graphics.fillRect(n + 1, n2 + 1, n6 - 2, this.ah + 4);
            graphics.setColor(this.int_a(14595245, n9, n7, 10));
            graphics.fillRect(n + 2, n2 + 2, n6 - 4, this.ah + 2);
        } else {
            n9 = 0x808080;
        }
        graphics.setColor(this.int_a(n8, n9, n7, 10));
        graphics.drawString(this.var_java_lang_String_arr_a[n5], (n += n6 - this.a(this.var_java_lang_String_arr_a[n5]) >> 1) + 1, (n2 += this.ay) + 4, 20);
        graphics.setColor(this.int_a(0, n9, n7, 10));
        graphics.drawString(this.var_java_lang_String_arr_a[n5], n, n2 + 3, 20);
    }

    public final int int_c(int n) {
        int n2 = this.menuNode + 6;
        for (int i = 0; i < n; ++i) {
            n2 = this.int_e(n2);
        }
        return n2;
    }

    public final int int_j(int n) {
        int n2 = this.menuTree[n + 2];
        n += 6;
        for (int i = 0; i < n2; ++i) {
            n = this.int_e(n);
        }
        return n;
    }

    public final int int_e(int n) {
        byte by = this.menuTree[n + 8];
        n = this.int_k(n);
        n = this.int_i(n);
        if (by == 2) {
            n = this.int_i(n);
        }
        return n;
    }

    public final int int_i(int n) {
        switch (this.menuTree[n++]) {
            case 2: 
            case 3: 
            case 6: 
            case 7: {
                ++n;
                break;
            }
            case 5: {
                n += 2;
            }
        }
        return n;
    }

    public final int int_k(int n) {
        n += 8;
        switch (this.menuTree[n++] & 0xF) {
            case 0: 
            case 4: {
                ++n;
                break;
            }
            case 3: 
            case 6: 
            case 8: 
            case 9: {
                n += 2;
                break;
            }
            case 2: 
            case 7: {
                n += 3;
                break;
            }
            case 10: {
                n += 4;
            }
        }
        return n;
    }

    public final void loadMenuScreen() {
        int n;
        int n2;
        this.menuNode = 0;
        for (n2 = 0; n2 < this.menuScreenId; ++n2) {
            this.menuNode = this.int_j(this.menuNode);
        }
        n2 = this.menuTree[this.menuNode + 3] & 0xFF;
        this.menuHighlight = this.menuTree[this.menuNode + 1];
        a a2 = new a(this.menuScreenId + 83);
        this.var_java_lang_String_arr_a = new String[n2];
        for (n = 0; n < n2; ++n) {
            this.var_java_lang_String_arr_a[n] = a2.a(n);
        }
        this.menuNodeCount = this.menuTree[this.menuNode + 2] & 0xFF;
        n = this.menuNode + 6;
        boolean bl = false;
        for (int i = 0; i < this.menuNodeCount; ++i) {
            switch (this.menuTree[n + 8] & 0xF) {
                case 3: 
                case 6: 
                case 8: 
                case 9: {
                    Image image = this.javax_microedition_lcdui_Image_a(10 + this.menuTree[n + 9], 0);
                    if (image == null) break;
                    this.menuTree[n + 6] = (byte)(image.getWidth() & 0xFF);
                    this.menuTree[n + 7] = (byte)(image.getHeight() & 0xFF);
                    break;
                }
                case 0: {
                    if (this.menuTree[n + 3] == 10) {
                        this.menuTree[n + 3] = (byte)this.ah;
                    }
                    this.menuTree[n + 6] = (byte)(this.a(this.var_java_lang_String_arr_a[this.menuTree[n + 9]]) + 1);
                    this.menuTree[n + 7] = (byte)this.ah;
                    break;
                }
                case 2: {
                    int n3 = 0;
                    for (int j = 0; j < this.menuTree[n + 9 + 1]; ++j) {
                        int n4 = this.a(this.var_java_lang_String_arr_a[this.menuTree[n + 9] + j]);
                        if (n4 <= n3) continue;
                        n3 = n4;
                    }
                    this.menuTree[n + 6] = (byte)(n3 + 8 & 0xFF);
                    this.menuTree[n + 7] = (byte)(this.ah + 4);
                    this.menuTree[n + 3] = (byte)(this.ah + 8);
                    this.menuTree[this.int_e((int)n) + 3] = (byte)(this.ah + 8);
                }
            }
            n = this.int_e(n);
        }
    }

    public final void D() {
        for (int i = 0; i < this.menuNodeCount; ++i) {
            int n = this.int_c(i);
            if ((this.menuTree[n + 0] & 0x20) == 0 || this.menuTree[n + 8] != 2) continue;
            this.runMenuScript(n, 0);
        }
    }

    public final void runMenuScript(int n, int n2) {
        int n3 = n;
        n3 = this.int_k(n3);
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[k] node=" + n + " mode=" + n2 + " type=" + this.menuTree[n + 8]
                + " op=" + this.menuTree[n3]);
        }
        if (this.menuTree[n + 8] == 2 && n2 > 0) {
            n3 = this.int_i(n3);
        }
        switch (this.menuTree[n3++]) {
            case 11: {
                this.clipTop = 0;
                this.clipBottom = this.screenH;
                this.loadNfo();
                if (this.nfoData[33] == 0) {
                    this.startMissionBriefing(0, 82, 2);
                } else {
                    this.requestStateSwitch(14);
                }
                this.pendingPanelSwitch = 0;
                return;
            }
            case 8: {
                this.clipTop = 0;
                this.clipBottom = this.screenH;
                for (int i = 0; i < 7; ++i) {
                    this.var_int_arr_e[i] = this.nfoHighScores[i];
                }
                this.startMissionBriefing(0, 82, 3);
                this.pendingPanelSwitch = 0;
                return;
            }
            case 7: {
                if (!this.requestStateSwitch(this.menuTree[n3])) break;
                this.D();
                return;
            }
            case 6: {
                this.clipTop = 0;
                this.clipBottom = this.screenH;
                this.startMissionBriefing(0, 82, this.menuTree[n3]);
                this.pendingPanelSwitch = -1;
                return;
            }
            case 2: {
                if (!this.requestStateSwitch(9)) break;
                this.D();
                this.ap = this.menuTree[n3];
                this.pendingPanelSwitch = this.menuScreenId;
                return;
            }
            case 5: {
                byte by = this.menuTree[n3++];
                byte by2 = this.menuTree[n3];
                n3 = this.int_c(by);
                this.menuTree[n3] = (byte)(by2 & 0xFF);
                return;
            }
            case 4: {
                this.var_boolean_e = true;
                return;
            }
            case 3: {
                int n4 = this.menuTree[n + 9 + 2];
                if (this.menuTree[n3] == 74) {
                    return;
                }
                if (this.menuTree[n3] == 67) {
                    this.loadNfo();
                    this.nfoData[30] = 0;
                    if (n4 == 1) {
                        this.nfoData[30] = 1;
                        AgeOfEmpires.b.c();
                        AgeOfEmpires.b.c = false;
                    } else {
                        AgeOfEmpires.b.c = true;
                        AgeOfEmpires.b.var_boolean_b = true;
                    }
                    this.saveNfo();
                    return;
                }
                if (this.menuTree[n3] == 71) {
                    this.gameMode = 16;
                    this.missionIndex = n4;
                    return;
                }
                if (this.menuTree[n3] == 73) {
                    this.gameMode = 32;
                    this.missionIndex = n4;
                    return;
                }
                if (this.menuTree[n3] == 72) {
                    this.loadNfo();
                    this.nfoData[29] = 0;
                    if (n4 == 0) {
                        this.nfoData[29] = 1;
                        this.var_boolean_d = false;
                    } else {
                        this.var_boolean_d = true;
                    }
                    this.saveNfo();
                    return;
                }
                if (this.menuTree[n3] == 65) {
                    this.gameMode = 0;
                    n3 = this.int_k(this.int_c(4));
                    n3 = this.int_i(n3);
                    this.menuTree[n3] = 2;
                    if (n4 == 0) {
                        this.menuTree[n3 + 1] = 11;
                        return;
                    }
                    if (n4 == 1) {
                        this.menuTree[n3 + 1] = 12;
                        return;
                    }
                    if (n4 != 2) break;
                    this.menuTree[n3 + 1] = 10;
                    return;
                }
                if (this.menuTree[n3] != 66) break;
                this.gameMode = 0;
                this.var_byte_a = (byte)(n4 & 0xF);
            }
        }
    }

    public final boolean boolean_e(int n) {
        return true;
    }

    public final void o(Graphics graphics) {
        if (this.pendingPanelSwitch != -1) {
            this.pendingPanelSwitch = this.menuScreenId;
        }
        if (this.ap != this.menuScreenId) {
            this.menuScreenId = this.ap;
            this.aQ = 0;
            this.var_int_a = 0;
            this.K = 0;
            if (this.menuScreenId == 2 || this.menuScreenId == 3 || this.menuScreenId == 4) {
                this.var_boolean_f = false;
                this.var_boolean_b = false;
            } else {
                this.var_boolean_f = true;
                this.var_boolean_b = true;
            }
        }
        this.requestStateSwitch(4);
    }

    public final boolean boolean_f(int n) {
        this.var_boolean_f = true;
        this.var_boolean_b = false;
        this.clipTop = 0;
        this.clipLeft = 0;
        this.clipBottom = this.screenH;
        this.clipRight = this.screenW;
        this.var_java_lang_String_arr_a = new String[6];
        a a2 = new a(97);
        for (int i = 0; i < 6; ++i) {
            this.var_java_lang_String_arr_a[i] = a2.a(i);
        }
        return true;
    }

    public final void u() {
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
    }

    public final void b(Graphics graphics) {
        int n;
        this.e(graphics, 0, 0, this.screenW, this.screenH);
        this.a(graphics, this.var_java_lang_String_arr_a[5], this.screenW - this.a(this.var_java_lang_String_arr_a[5]) >> 1, 9 + this.ay, this.aQ);
        int n2 = this.clipRight - this.clipLeft - 24;
        boolean bl = false;
        int n3 = (this.screenH >> 1) - 35;
        if (n3 < 17) {
            n3 = 17;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[1], 12, n3, this.aQ);
        n3 += this.ah + 3;
        int n4 = this.playerUnitHeaders[0][86] + this.playerUnitHeaders[1][86];
        if (n4 > 0) {
            n = this.playerUnitHeaders[0][86] * n2 / n4;
            graphics.setColor(1065087);
            graphics.fillRect(12, n3, n, 3);
            n = this.playerUnitHeaders[0][2] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.playerUnitHeaders[1][86] * n2 / n4;
            graphics.setColor(8329232);
            graphics.fillRect(12, n3 += 3, n, 3);
            n = this.playerUnitHeaders[1][2] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3, n, 3);
            n3 += 7;
        } else {
            n3 += 10;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[2], 12, n3, this.aQ + 1);
        n3 += this.ah + 3;
        n4 = this.playerUnitHeaders[0][88] + this.playerUnitHeaders[1][88];
        if (n4 > 0) {
            n = this.playerUnitHeaders[0][88] * n2 / n4;
            graphics.setColor(1065087);
            graphics.fillRect(12, n3, n, 3);
            n = this.playerUnitHeaders[0][4] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.playerUnitHeaders[1][88] * n2 / n4;
            graphics.setColor(8329232);
            graphics.fillRect(12, n3 += 3, n, 3);
            n = this.playerUnitHeaders[1][4] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3, n, 3);
            n3 += 7;
        } else {
            n3 += 10;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[0], 12, n3, this.aQ + 1);
        n3 += this.ah + 3;
        n4 = this.playerUnitHeaders[0][90] + this.playerUnitHeaders[1][90];
        if (n4 > 0) {
            n = this.playerUnitHeaders[0][90] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.playerUnitHeaders[1][90] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3 += 3, n, 3);
        }
        if (this.keyActionHeld == 22 || this.keyActionHeld == 6 || this.keyActionHeld == 38) {
            this.requestStateSwitch(4);
        }
        if (this.keyActionHeld == 22) {
            this.keyActionHeld = 0;
        }
        this.keyActionPulse = 0;
        this.keyActionHeld = 0;
        ++this.aQ;
    }

    public final boolean boolean_a(int n) {
        this.var_boolean_f = true;
        this.var_boolean_b = false;
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
        a a2 = new a(65);
        this.var_java_lang_String_arr_a = new String[160];
        int n2 = 0;
        do {
            this.var_java_lang_String_arr_a[n2] = a2.a(n2);
        } while (this.var_java_lang_String_arr_a[n2++] != null);
        this.aQ = 0;
        return true;
    }

    public final void r() {
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
        this.pendingPanelSwitch = -1;
        this.keyActionHeld = 0;
        this.keyActionPulse = 0;
    }

    public final void k(Graphics graphics) {
        int n = this.screenH / 19;
        int n2 = this.aQ / 19 - n;
        int n3 = -(this.aQ % 19);
        int n4 = n + 1 + n2;
        graphics.setColor(0);
        graphics.fillRect(0, 0, this.screenW, this.screenH);
        if (n2 > 0 && this.var_java_lang_String_arr_a[n2] == null) {
            this.requestStateSwitch(4);
            return;
        }
        for (int i = n2; i < n4; ++i) {
            if (i >= 0 && this.var_java_lang_String_arr_a[i] != null) {
                int n5 = this.screenW - this.a(this.var_java_lang_String_arr_a[i]) >> 1;
                int n6 = this.int_a(0x606060, 0, this.screenH - n3 - 19, this.screenH >> 2);
                if (n6 > 0) {
                    graphics.setColor(n6);
                    graphics.drawString(this.var_java_lang_String_arr_a[i], n5, n3 + 1, 20);
                }
                if ((n6 = this.int_a(0xFFFFFF, 0, this.screenH - n3 - 19, this.screenH >> 2)) > 0) {
                    graphics.setColor(n6);
                    graphics.drawString(this.var_java_lang_String_arr_a[i], n5, n3, 20);
                }
            }
            n3 += 19;
        }
        ++this.aQ;
        if (this.keyActionPulse == 38 || this.keyActionPulse == 22 || this.keyActionPulse == 6 || this.keyActionPulse == 47) {
            this.requestStateSwitch(4);
            this.keyActionHeld = 0;
        }
    }

    public final boolean beginMissionLoad(int n) {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[trace] boolean_c aH=" + n + " ac=" + this.gameMode + " aC=" + this.missionIndex);
        }
        Object object;
        this.var_boolean_f = false;
        this.var_boolean_b = false;
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
        this.var_AgeOfEmpires_d_a = new d();
        if (this.gameMode == 0) {
            this.loadNfo();
            this.var_AgeOfEmpires_d_a.o = this.nfoReadInt(31, 2);
            if (this.var_AgeOfEmpires_d_a.o == 0) {
                this.var_AgeOfEmpires_d_a.o = 8224;
            }
            // -Daoe.mapSeed=N：批量实验换图。在随机图装载点覆盖 nfo 种子
            // （拆分字节序与上方原路径完全一致），不设属性时逐字节不变。
            Integer mapSeed = Integer.getInteger("aoe.mapSeed");
            if (mapSeed != null) {
                this.var_AgeOfEmpires_d_a.o = mapSeed.intValue();
            }
            rngStateLo = (byte)(this.var_AgeOfEmpires_d_a.o >>> 8) & 0xFF;
            rngStateHi = (byte)(this.var_AgeOfEmpires_d_a.o & 0xFF);
        }
        if (this.missionResId != 0) {
            byte[] var_byte_arr_t = com.ulysseo.mad.c.byte_arr_a(this.missionResId);
            this.randomMap = false;
            int n2 = var_byte_arr_t[0] & 0xFF;
            int n3 = var_byte_arr_t[1] & 0xFF;
            if ((n2 | n3) != 0) {
                rngStateLo = n2;
                rngStateHi = n3;
            } else {
                this.randomMap = true;
            }
        }
        this.var_AgeOfEmpires_d_a.a(9, 20, this.mapTiles, this.randomMap);
        this.aQ = 0;
        object = new a(99);
        this.var_java_lang_String_a = ((a)object).a(0);
        return true;
    }

    public final void t() {
        if (this.randomMap) {
            this.mapTiles[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1 << 6)] = 0;
            this.mapTiles[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] - 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1 << 6)] = 0;
            this.mapTiles[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] << 6)] = 0;
            this.a(0, 9, this.var_AgeOfEmpires_d_a.var_int_arr_a[0], this.var_AgeOfEmpires_d_a.var_int_arr_a[1], 255, false);
            this.a(0, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1, false);
            this.a(0, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] - 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1, false);
            if (this.var_byte_a < 2) {
                this.a(0, 5, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1], false);
            }
            this.cursorTileX = this.var_AgeOfEmpires_d_a.var_int_arr_a[0];
            this.cursorTileY = this.var_AgeOfEmpires_d_a.var_int_arr_a[1];
            this.cursorTileIdx = this.cursorTileX + (this.cursorTileY << 6);
            this.mapTiles[this.var_AgeOfEmpires_d_a.var_int_arr_a[2] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1 << 6)] = Short.MIN_VALUE;
            this.mapTiles[this.var_AgeOfEmpires_d_a.var_int_arr_a[2] - 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1 << 6)] = Short.MIN_VALUE;
            this.a(1, 9, this.var_AgeOfEmpires_d_a.var_int_arr_a[2], this.var_AgeOfEmpires_d_a.var_int_arr_a[3], 255, false);
            this.a(1, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[2] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1, false);
            this.a(1, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[2] - 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1, false);
        }
        if (this.missionResId != 0) {
            this.spawnMission(this.missionResId);
        }
        this.var_AgeOfEmpires_d_a.d();
        this.var_AgeOfEmpires_d_a = null;
        this.var_java_lang_String_a = null;
        AgeOfEmpires.b.c();
    }

    public final void renderLoadingScreen(Graphics graphics) {
        boolean bl = false;
        int n = (this.screenH >> 1) - 10;
        graphics.setClip(0, 0, this.screenW, this.screenH);
        this.e(graphics, 0, 0, this.screenW, this.screenH);
        graphics.setColor(7039826);
        graphics.drawString(this.var_java_lang_String_a, 14, n + 1, 20);
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.var_java_lang_String_a, 13, n, 20);
        graphics.setColor(7039826);
        graphics.fillRect(11, (n += this.ah + 3) + 2, this.screenW - 21, 10);
        graphics.setColor(0);
        graphics.fillRect(10, n + 1, this.screenW - 21, 10);
        graphics.setColor(0xFFFFFF);
        graphics.fillRect(8, n, this.screenW - 21, 9);
        int n2 = this.screenW - 23;
        int n3 = this.var_AgeOfEmpires_d_a.n * n2 >> 8;
        int n4 = 128;
        int n5 = 7;
        ++n;
        for (int i = 0; i < 4; ++i) {
            graphics.setColor(n4 << 16 | n4 << 8);
            graphics.fillRect(9, n, n3, n5);
            graphics.setColor(n4 << 16);
            graphics.fillRect(9 + n3, n, n2 - n3, n5);
            n5 -= 2;
            n4 += 40;
            ++n;
        }
        if (this.aQ == 0) {
            this.var_AgeOfEmpires_d_a.run();
        }
        if (this.var_AgeOfEmpires_d_a.var_boolean_c) {
            this.requestStateSwitch(6);
        }
        ++this.aQ;
    }

    static final int nextRandomInt() {
        rngStateHi += rngStateLo;
        rngStateHi += (rngStateLo & 0xFF) >> 2;
        rngStateLo ^= rngStateHi;
        rngStateLo += (rngStateHi & 0xFF) >> 1;
        return rngStateHi & 0xFF;
    }

    // —— 化妆品 RNG：只服务 BGM 选曲（playNextBgm），与模拟流隔离 ——
    // 同一条 LCG、独立状态。模拟外消费源若混用 nextRandomInt，模拟轨迹会随
    // "听了几首曲子"发散，确定性回放（tools/replaycheck.sh）即失效。新加的非
    // 模拟随机需求一律走这里，勿动 nextRandomInt。
    private static int bgmRngStateHi = 12;
    private static int bgmRngStateLo = 45;

    static final int nextBgmRandomInt() {
        bgmRngStateHi += bgmRngStateLo;
        bgmRngStateHi += (bgmRngStateLo & 0xFF) >> 2;
        bgmRngStateLo ^= bgmRngStateHi;
        bgmRngStateLo += (bgmRngStateHi & 0xFF) >> 1;
        return bgmRngStateHi & 0xFF;
    }

    public final void void_a(int n, int n2, int n3, int n4) {
        int n5 = n2 + (n3 << 6) & 0xFFF;
        this.mapTiles[n5] = (short)(this.mapTiles[n5] & 0xF000);
        if (n4 > 31) {
            n4 = 31;
        }
        int n6 = n2 + (n3 << 6) & 0xFFF;
        this.mapTiles[n6] = (short)(this.mapTiles[n6] | (short)(0x300 | n & 0xFF | (n4 <<= 2)));
    }

    public final boolean boolean_j(int n) {
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
        this.mapViewSavedCamX = this.cameraPxX;
        this.mapViewSavedCamY = this.cameraPxY;
        this.mapViewSavedCursorX = this.cursorTileX;
        this.mapViewSavedCursorY = this.cursorTileY;
        this.mapThumbStampRow = 0;
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[view] enter map: cursor=(" + this.cursorTileX + "," + this.cursorTileY
                + ") cam=(" + this.cameraPxX + "," + this.cameraPxY + ")");
        }
        int n2 = this.camTargetX + this.ad;
        int n3 = (this.camTargetY << 1) + this.J;
        int n4 = n2 + n3 >> 5;
        int n5 = n3 - n2 >> 5;
        this.cameraPxX = n4 - n5 << 1;
        this.cameraPxY = n4 + n5;
        this.ag = 0;
        this.keyActionHeld = 0;
        return true;
    }

    public final void d() {
        this.cameraPxX = this.mapViewSavedCamX;
        this.cameraPxY = this.mapViewSavedCamY;
        this.cursorTileX = this.mapViewSavedCursorX;
        this.cursorTileY = this.mapViewSavedCursorY;
        this.keyActionPulse = 0;
        this.keyActionHeld = 0;
    }

    /** 全图视图（键 0 开关）：蓝底满幅 + 240x120 整图缩略图（按镜头位置摆放）+
     *  白色四角框标出当前视野范围 + 光标/软键导航（含原版连发节奏）。
     *  mapThumbStampRow：进图时逐行盖章地形（>=10 即可操作，64 盖完后开始逐帧叠单位）。 */
    public final void renderMapView(Graphics graphics) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        graphics.setClip(0, 0, this.screenW, this.screenH);
        graphics.setColor(3438335);
        graphics.fillRect(0, 0, this.screenW, this.screenH);
        int n8 = this.screenW >> 1;
        int n9 = this.screenH >> 1;
        Image image = this.javax_microedition_lcdui_Image_a(20, 240, 120, 3438335);
        if (image == null) {
            return;
        }
        if (this.mapThumbStampRow < 64) {
            for (n7 = 0; n7 <= 64 && this.mapThumbStampRow < 64; ++n7) {
                for (n6 = 0; n6 < 64; ++n6) {
                    this.stampThumbTile(this.var_javax_microedition_lcdui_Graphics_a, n6, this.mapThumbStampRow);
                }
                ++this.mapThumbStampRow;
            }
        } else {
            for (n5 = 0; n5 < 2; ++n5) {
                n7 = 0;
                n6 = this.playerUnitHeaders[n5][2];
                n4 = 0;
                while (n4 < n6) {
                    n3 = this.playerUnitSlots[n5][n7 + 0];
                    n2 = n3 >>> 8;
                    int n10 = this.playerUnitSlots[n5][n7 + 1];
                    int n11 = n10 >>> 8;
                    n = this.playerUnitSlots[n5][n7 + 6] & 0xF0;
                    // 反编译原样的 (n10 &= 0xFF) != (n3 &= 0xFF) 写在 || 短路第二支上：
                    // tx 相等时被跳过、n3/n10 保持 tx<<8|ty 全值，mapTiles[n2 + (n3<<6)]
                    // 会越界（原版即有的暗雷，全图视图偶发掉帧/卡退出）。改为用点处掩码，
                    // 对原已掩码路径数值完全一致。
                    if (((n11 != n2 || (n10 & 0xFF) != (n3 & 0xFF)) && n == 0 || n4 == this.tickCount % n6) && (this.mapTiles[n2 + ((n3 & 0xFF) << 6)] & 0x8000) == 0) {
                        this.stampThumbTile(this.var_javax_microedition_lcdui_Graphics_a, n11, n10 & 0xFF);
                        this.stampThumbTile(this.var_javax_microedition_lcdui_Graphics_a, n2, n3 & 0xFF);
                    }
                    ++n4;
                    n7 += 8;
                }
                n7 = this.playerUnitHeaders[n5][4];
                if (n7 == 0) continue;
                if (n7 > 1) {
                    n7 = this.tickCount % (n7 - 1) << 2;
                }
                n3 = this.buildingTable[n5][n7 + 0];
                n2 = n3 >>> 8;
                this.stampThumbTile(this.var_javax_microedition_lcdui_Graphics_a, n2, n3 &= 0xFF);
            }
        }
        n7 = n8 - this.cameraPxX - 120;
        n6 = n9 - this.cameraPxY;
        graphics.setClip(this.clipLeft, this.clipTop, this.clipRight - this.clipLeft, this.clipBottom - this.clipTop);
        com.ulysseo.mad.d.a(graphics, image, 0, 0, 240, 120, 0, n7, n6, 0);
        graphics.setColor(0xFFFFFF);
        this.aQ = (this.tickCount & 3) >= 2 ? 4 - (this.tickCount & 3) : this.tickCount & 3;
        n2 = (this.viewTileCols >> 1) + this.aQ;
        n3 = (this.viewTileRows >> 2) + this.aQ;
        n = this.viewTileCols / 3 + 1;
        n5 = n8 - n2;
        n4 = n9 - n3;
        n2 = n8 + n2;
        n3 = n9 + n3;
        graphics.drawLine(n2, n3, n2 - n, n3);
        graphics.drawLine(n2, n3, n2, n3 - n);
        graphics.drawLine(n5, n4, n5 + n, n4);
        graphics.drawLine(n5, n4, n5, n4 + n);
        graphics.drawLine(n2, n4, n2 - n, n4);
        graphics.drawLine(n2, n4, n2, n4 + n);
        graphics.drawLine(n5, n3, n5 + n, n3);
        graphics.drawLine(n5, n3, n5, n3 - n);
        if (this.mapThumbStampRow >= 10) {
            int n12 = this.keyActionPulse;
            if (this.keyActionHeld == 0) {
                return;
            }
            if (this.keyActionHeld == this.keyRepeatLast) {
                ++this.keyRepeatFrames;
                if (this.keyRepeatFrames >= 5) {
                    n12 = this.keyActionHeld;
                }
            } else {
                this.keyRepeatFrames = 0;
                this.keyRepeatLast = this.keyActionHeld;
            }
            switch (n12) {
                case 1: 
                case 6: 
                case 22: 
                case 38: {
                    this.mapViewSavedCamX = this.cameraPxX;
                    this.mapViewSavedCamY = this.cameraPxY;
                    this.mapViewSavedCursorX = this.cursorTileX;
                    this.mapViewSavedCursorY = this.cursorTileY;
                }
                case 47: {
                    if (System.getProperty("aoe.debug") != null) {
                        System.out.println("[view] exit map -> 8: cursor=(" + this.cursorTileX + "," + this.cursorTileY
                            + ") cam=(" + this.cameraPxX + "," + this.cameraPxY + ") clipTop=" + this.clipTop
                            + " row=" + this.mapThumbStampRow);
                    }
                    this.clipTop = 0;
                    this.clipBottom = this.screenH;
                    this.var_boolean_h = true;
                    this.pendingScreenState = 8;
                    this.updateCamera();
                    break;
                }
                case 2: {
                    n12 = 255;
                    break;
                }
                case 3: 
                case 19: {
                    n12 = 15;
                    break;
                }
                case 4: {
                    n12 = 31;
                    break;
                }
                case 5: 
                case 21: {
                    n12 = 240;
                    break;
                }
                case 7: 
                case 23: {
                    n12 = 16;
                    break;
                }
                case 8: {
                    n12 = 241;
                    break;
                }
                case 9: 
                case 25: {
                    n12 = 1;
                    break;
                }
                case 10: {
                    n12 = 17;
                    break;
                }
                default: {
                    n12 = 0;
                }
            }
            if ((n12 & 0xF0) == 240) {
                if (this.cameraPxX > -119) {
                    this.cameraPxX -= 4;
                    --this.cursorTileX;
                    ++this.cursorTileY;
                }
            } else if ((n12 & 0xF0) == 16 && this.cameraPxX < 119) {
                this.cameraPxX += 4;
                ++this.cursorTileX;
                --this.cursorTileY;
            }
            if ((n12 & 0xF) == 15) {
                if (this.cameraPxY > 0) {
                    this.cameraPxY -= 2;
                    --this.cursorTileX;
                    --this.cursorTileY;
                    return;
                }
            } else if ((n12 & 0xF) == 1 && this.cameraPxY < 119) {
                this.cameraPxY += 2;
                ++this.cursorTileX;
                ++this.cursorTileY;
            }
        }
    }

    /** 全图缩略图上钉一格：(tx,ty) → 1 像素（iso：x-y+120, (x+y)/2），颜色取自
     *  mapTiles 低 12 位地形/覆盖位；空地形 768 → 海蓝（与底色同）。迷雾格（0x8000
     *  置位使 short 变负）不走地形 switch 而画深色——"黑迷雾"。位语义详见
     *  devMouseCmd state JSON explored 处的注释。 */
    public final void stampThumbTile(Graphics graphics, int n, int n2) {
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = this.mapTiles[n + (n2 << 6) & 0xFFF];
        graphics.setClip(0, 0, 240, 120);
        n += n;
        n2 += n2;
        int n9 = n - n2 + 120;
        int n10 = n2 + n >> 1;
        int n11 = (n8 & 0xC00) >> 10;
        if (n8 >= 0) {
            switch (n8 & 0x300) {
                case 0: {
                    if ((n8 & 0x4000) != 0) {
                        n3 = 2385412;
                        n4 = 2385412;
                        n5 = 2385412;
                        n6 = 2385412;
                        n7 = 2385412;
                        break;
                    }
                    n3 = 47872;
                    n4 = 47872;
                    n5 = 47872;
                    n6 = 47872;
                    n7 = 47872;
                    break;
                }
                case 768: {
                    if ((n8 &= 3) == 0) {
                        n3 = 3438335;
                        n4 = 3438335;
                        n5 = 3438335;
                        n6 = 3438335;
                        n7 = 3438335;
                        break;
                    }
                    if (n8 == 1) {
                        graphics.setColor(0x208000);
                        graphics.fillRect(n9, n10 - 2, 1, 1);
                        graphics.setColor(0x80B000);
                        graphics.fillRect(n9, n10 - 1, 1, 1);
                        graphics.setColor(0x208000);
                        graphics.fillRect(n9 + 1, n10, 1, 1);
                        graphics.setColor(0x208000);
                        graphics.fillRect(n9 - 1, n10, 1, 1);
                        n3 = 0x40A000;
                        n4 = 0x804000;
                        n5 = 0x208000;
                        n6 = 0x804000;
                        n7 = 0x804000;
                        break;
                    }
                    if (n8 == 2) {
                        n3 = 0xF0F000;
                        n4 = 0x808000;
                        n5 = 0xC0C080;
                        n6 = 0xF0F000;
                        n7 = 0x808000;
                        break;
                    }
                    if (n8 != 3) break;
                    n3 = 0x808080;
                    n4 = 0xA0A080;
                    n5 = 0xA0A080;
                    n6 = 0x808080;
                    n7 = 0xA0A0FF;
                    break;
                }
                case 256: {
                    if (n11 == 0) {
                        n3 = 128;
                        n4 = 128;
                        n5 = 128;
                        n6 = 128;
                        n7 = 128;
                        graphics.setColor(128);
                        graphics.fillRect(n9 + 1, n10, 1, 1);
                        graphics.setColor(128);
                        graphics.fillRect(n9 - 1, n10, 1, 1);
                        break;
                    }
                    n3 = 0xF00000;
                    n4 = 0xF00000;
                    n5 = 0xF00000;
                    n6 = 0xF00000;
                    n7 = 0xF00000;
                    break;
                }
                case 512: {
                    if (this.mapThumbStampRow < 64) {
                        return;
                    }
                    n6 = 0x206000;
                    n5 = 0x206000;
                    n7 = 0x206000;
                    if (n11 == 0) {
                        n3 = 0xF0F0F0;
                        n4 = 128;
                        break;
                    }
                    n3 = 0xF0F0F0;
                    n4 = 0xF00000;
                }
            }
        } else if ((n8 & 0xFFF) == 768) {
            n3 = 3438335;
            n4 = 3438335;
            n5 = 3438335;
            n6 = 3438335;
            n7 = 3438335;
        }
        graphics.setColor(n3);
        graphics.fillRect(n9, n10, 1, 1);
        graphics.setColor(n4);
        graphics.fillRect(n9, n10 + 1, 1, 1);
        graphics.setColor(n5);
        graphics.fillRect(n9 - 1, n10 + 1, 1, 1);
        graphics.setColor(n6);
        graphics.fillRect(n9 + 1, n10 + 1, 1, 1);
        graphics.setColor(n7);
        graphics.fillRect(n9, n10 + 2, 1, 1);
    }

    public final boolean boolean_h(int n) {
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.pendingPanelSwitch = -1;
        if (this.clipTop == 0) {
            if (System.getProperty("aoe.debug") != null) {
                System.out.println("[view] world rebuild with clipTop=0 -> var_boolean_l=true");
            }
            this.var_boolean_l = true;
        }
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        return true;
    }

    public final void a(Graphics graphics) {
        this.mouseTick();
        if (this.var_boolean_l) {
            this.requestStateSwitch(8);
        }
        this.tickCursor();
        if (this.keyActionHeld == 22) {
            this.keyActionHeld = 0;
        }
        this.keyActionPulse = 0;
        this.updateCamera();
        this.renderWorld(graphics);
        // 移植增强：桌面拖选橡皮筋 + 悬停格高亮（画在世界之上）
        if (this.mouseBandActive) {
            graphics.setColor(0xFFFFFF);
            graphics.drawRect(Math.min(this.mouseBandX1, this.mouseBandX2),
                Math.min(this.mouseBandY1, this.mouseBandY2),
                Math.abs(this.mouseBandX2 - this.mouseBandX1),
                Math.abs(this.mouseBandY2 - this.mouseBandY1));
        }
        if (this.mouseHiX >= 0) {
            graphics.setColor(0xFFFF00);
            graphics.drawLine(this.mouseHiX, this.mouseHiY, this.mouseHiX + 32, this.mouseHiY + 16);
            graphics.drawLine(this.mouseHiX + 32, this.mouseHiY + 16, this.mouseHiX, this.mouseHiY + 32);
            graphics.drawLine(this.mouseHiX, this.mouseHiY + 32, this.mouseHiX - 32, this.mouseHiY + 16);
            graphics.drawLine(this.mouseHiX - 32, this.mouseHiY + 16, this.mouseHiX, this.mouseHiY);
            this.mouseHiX = -1;
        }
    }

    public final boolean setupMissionEnv(int n) {
        int n2;
        if (BFS_PATH) {
            // 新任务清空 BFS 路径缓存:不同任务的 slot[2] 目标可能撞车,
            // 残留缓存会把上张图的路径接到新任务单位上
            java.util.Arrays.fill(this.bfsPathTarget, (short)-1);
        }
        this.bgmFramesLeft = 0;
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        this.clipTop = 0;
        this.clipBottom = this.screenH;
        if (n == 0) {
            this.var_int_c = 0;
            this.var_int_arr_b = null;
            this.var_int_arr_b = new int[48];
            for (int i = 0; i < 181; ++i) {
                this.var_javax_microedition_lcdui_Image_arr_a[i] = null;
            }
            this.selectionMark = 0;
            this.selectedType = -1;
            this.selectedSlot = 0;
            this.selectionPlayer = 0;
            this.selectionMode = 0;
            this.var_short_a = (short)-1;
            this.var_short_c = (short)-1;
            this.var_short_b = (short)-1;
            this.aiTrainTimer = 0;
            this.aiBuildTimer = 0;
            AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
            this.var_int_arr_a[0] = 0;
            this.var_int_arr_a[1] = 0;
            this.var_int_arr_a[2] = 0;
            this.var_int_arr_a[3] = 0;
            this.costTable = null;
            this.techFlags = null;
            this.missionScript = null;
            this.dirTable = null;
            return false;
        }
        this.costTable = com.ulysseo.mad.c.byte_arr_a(122);
        this.techFlags = com.ulysseo.mad.c.byte_arr_a(127);
        this.dirTable = com.ulysseo.mad.c.byte_arr_a(123);
        this.devCheckpointedThisMission = false;    // 新任务环境装载,允许一次 auto checkpoint
        this.var_int_i = 0;
        for (n2 = 0; n2 < 4096; ++n2) {
            this.mapTiles[n2] = Short.MIN_VALUE;
        }
        for (n2 = 0; n2 < 2; ++n2) {
            byte[] byArray = com.ulysseo.mad.c.byte_arr_a(121);
            for (int i = 0; i < 91; ++i) {
                this.playerUnitHeaders[n2][i] = byArray[i];
            }
            int[] nArray = this.playerUnitHeaders[n2];
            nArray[56] = nArray[56] << 8;
            this.playerUnitHeaders[n2][54] = 0xFFFFFF;
            this.playerUnitHeaders[n2][12] = 16;
        }
        this.aiGatherMultiplier = 256;
        this.aiAttackThreshold = 0;
        this.aiFreeResTimer = 0;
        this.aiTrainTimer = 0;
        // aiEnabled 跨任务泄漏修复：这组 AI 开关/调参原先只在 gameMode==0(随机图)
        // 和战役 m4 分支赋值且从不复位——同进程先玩战役再玩随机图会继承上一场的
        // 全部调参（教学分支甚至继承战役的 aiEnabled=false）。装载新任务时统一
        // 先落默认值（=Easy 档），模式分支照旧覆盖。
        this.aiEnabled = true;
        this.aiBuildInterval = 250;
        this.aiTrainInterval = 20;
        this.aiGuardRadiusSq = 49;
        this.aiFreeResInterval = Integer.MAX_VALUE;
        this.aiBuildTimer = 0;
        this.aiBuildPhase = 0;
        this.aiBuildTarget = -1;
        this.var_int_i = 0;
        if (this.gameMode == 0) {
            this.aiEnabled = true;
            switch (this.var_byte_a) {
                case 0: {
                    this.playerUnitHeaders[0][5] = 200;
                    this.playerUnitHeaders[0][6] = 100;
                    this.playerUnitHeaders[0][7] = 100;
                    this.playerUnitHeaders[1][5] = 50;
                    this.playerUnitHeaders[1][6] = 15;
                    this.playerUnitHeaders[1][7] = 15;
                    this.aiGatherMultiplier = 512;
                    this.aiBuildInterval = 250;
                    // 原版 50:AI 军队价值 2 分钟内到线即 75% 兵力 all-in 玩家基地
                    // (Easy 训练间隔 20 tick≈1.6s/兵),实测新手 2 分钟被推平。
                    // 移植平衡修正:Easy 提高首攻门槛,给玩家发育窗口;中/高难不变。
                    this.aiAttackThreshold = 200;
                    this.aiTrainInterval = 20;
                    this.aiGuardRadiusSq = 49;
                    this.aiFreeResInterval = Integer.MAX_VALUE;
                    break;
                }
                case 1: {
                    this.playerUnitHeaders[0][5] = 200;
                    this.playerUnitHeaders[0][6] = 100;
                    this.playerUnitHeaders[0][7] = 100;
                    this.playerUnitHeaders[1][5] = 50;
                    this.playerUnitHeaders[1][6] = 50;
                    this.playerUnitHeaders[1][7] = 50;
                    this.aiGatherMultiplier = 786;
                    this.aiBuildInterval = 150;
                    this.aiAttackThreshold = 60;
                    this.aiTrainInterval = 20;
                    this.aiGuardRadiusSq = 36;
                    this.aiFreeResInterval = 2500;
                    break;
                }
                case 2: {
                    this.playerUnitHeaders[0][5] = 200;
                    this.playerUnitHeaders[0][6] = 100;
                    this.playerUnitHeaders[0][7] = 100;
                    this.playerUnitHeaders[1][5] = 20;
                    this.playerUnitHeaders[1][6] = 20;
                    this.playerUnitHeaders[1][7] = 20;
                    this.aiGatherMultiplier = 2048;
                    this.aiBuildInterval = 100;
                    this.aiAttackThreshold = 100;
                    this.aiTrainInterval = 1;
                    this.aiGuardRadiusSq = 25;
                    this.aiFreeResInterval = 1000;
                }
            }
        }
        n2 = 128;
        this.missionResId = 0;
        switch (this.gameMode) {
            case 16: {
                if (this.missionIndex == 0) {
                    n2 = 124;
                    this.I = 78;
                    this.missionResId = 118;
                    break;
                }
                if (this.missionIndex == 1) {
                    n2 = 125;
                    this.I = 79;
                    this.missionResId = 119;
                    break;
                }
                if (this.missionIndex != 2) break;
                n2 = 126;
                this.I = 80;
                this.missionResId = 120;
                break;
            }
            case 32: {
                this.aiEnabled = false;
                switch (this.missionIndex) {
                    case 0: {
                        n2 = 110;
                        this.I = 71;
                        this.missionResId = 103;
                        break;
                    }
                    case 1: {
                        n2 = 111;
                        this.I = 72;
                        this.missionResId = 104;
                        break;
                    }
                    case 2: {
                        n2 = 112;
                        this.I = 73;
                        this.missionResId = 105;
                        break;
                    }
                    case 3: {
                        n2 = 113;
                        this.I = 74;
                        this.missionResId = 106;
                        break;
                    }
                    case 4: {
                        n2 = 114;
                        this.I = 75;
                        this.missionResId = 107;
                        this.aiEnabled = true;
                        this.aiBuildInterval = 100;
                        this.aiGatherMultiplier = 256;
                        this.aiBuildInterval = 200;
                        this.aiAttackThreshold = 30;
                        this.aiTrainInterval = 200;
                        this.aiGuardRadiusSq = 49;
                        this.aiFreeResInterval = 1000;
                        break;
                    }
                    case 5: {
                        n2 = 115;
                        this.I = 76;
                        this.missionResId = 108;
                        this.aiAttackThreshold = -1000;
                        break;
                    }
                    case 6: {
                        n2 = 116;
                        this.I = 77;
                        this.missionResId = 109;
                    }
                }
                break;
            }
            default: {
                this.randomMap = true;
                this.var_boolean_l = true;
            }
        }
        if (this.aiEnabled) {
            this.aiBuildPhase = 0;
            this.void_a();
        }
        this.missionScript = com.ulysseo.mad.c.byte_arr_a(n2);
        return true;
    }

    public final void i(Graphics graphics) {
        if (this.gameMode == 16 && (this.missionIndex == 0 || this.missionIndex == 2)) {
            this.spawnMission(this.missionResId);
            this.requestStateSwitch(6);
            return;
        }
        this.requestStateSwitch(11);
    }

    public final boolean buildActionMenu(int n) {
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        if (this.var_boolean_a) {
            if (this.var_boolean_g) {
                this.selectedTrainProduct = 0;
            }
            this.T = 0;
        }
        this.ag = 0;
        a a2 = new a(99);
        this.var_java_lang_String_a = a2.a(6);
        this.var_int_arr_c = new int[this.X];
        this.var_java_lang_String_arr_a = new String[this.X << 1];
        this.W = 0;
        a a3 = new a(this.G);
        for (int i = 0; i < this.X; ++i) {
            if (!this.boolean_k(i) && this.var_boolean_g) continue;
            int n2 = i;
            this.var_int_arr_c[this.W] = i;
            if (this.var_int_g == 1 && n2 == 12) {
                n2 += this.techFlags[36];
                n2 += this.techFlags[40];
                n2 += this.techFlags[43];
                int n3 = this.W;
                this.var_int_arr_c[n3] = this.var_int_arr_c[n3] + this.techFlags[36];
            }
            this.var_java_lang_String_arr_a[this.W << 1] = a3.a(n2 << 1);
            this.var_java_lang_String_arr_a[(this.W << 1) + 1] = a3.a((n2 << 1) + 1);
            ++this.W;
        }
        return true;
    }

    public final void clearActionMenu() {
        this.var_int_arr_c = null;
        this.var_java_lang_String_a = null;
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
    }

    public final void l(Graphics graphics) {
        if (this.W == 0) {
            this.pendingScreenState = 6;
            return;
        }
        this.clipBottom = this.bottomBarY;
        graphics.setClip(0, 0, this.screenW, this.screenH);
        graphics.setColor(0);
        graphics.fillRect(0, this.clipBottom, this.screenW, this.screenH - this.clipBottom);
        if (this.var_boolean_a) {
            int n = 0;
            boolean bl = false;
            int n2 = this.screenW - 111 >> 1;
            int n3 = this.clipBottom - this.clipTop - 111 >> 1;
            if (n3 < this.clipTop) {
                n3 = this.clipTop;
            }
            if (this.var_boolean_g) {
                graphics.setColor(0x408040);
                graphics.fillRect(n2 - 1, n3 - 1, 112, 112);
                this.i();
                this.keyActionPulse = 0;
                this.keyActionHeld = 0;
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        int n4 = j * 37 + n2;
                        int n5 = i * 37 + n3;
                        if (n == this.T) {
                            if ((this.tickCount & 1) == 0) {
                                graphics.setColor(0xFFFFFF);
                            } else {
                                graphics.setColor(0xFF0000);
                            }
                            graphics.fillRect(n4 - 1, n5 - 1, 38, 38);
                        }
                        if (n < this.W) {
                            int n6 = 0;
                            n6 = this.var_int_g == 0 ? (this.playerUnitHeaders[0][3] > this.playerUnitHeaders[0][49] + this.playerUnitHeaders[0][2] ? (this.canAfford(0, this.var_int_g, this.var_int_arr_c[n]) ? 0 : 4) : 4) : (this.canAfford(0, this.var_int_g, this.var_int_arr_c[n]) ? 0 : 4);
                            this.a(graphics, this.x, n4, n5, this.var_int_arr_c[n] * 36, 0, 36, 36, 0, n6);
                            graphics.setColor(0);
                            graphics.fillRect(n4 + 36 - 5, n5 + 36 - 7, 4, 6);
                            int n7 = n + 1;
                            this.b(graphics, n7, n4 + 36 - 8, n5 + 36 - 12, 1);
                        } else {
                            graphics.fillRect(n4, n5, 36, 36);
                        }
                        ++n;
                    }
                }
            } else {
                int n8 = 0;
                boolean bl2 = this.canAfford(0, 2, this.selectedTrainProduct);
                switch (this.keyActionPulse) {
                    case 5: 
                    case 21: {
                        if (this.T <= 0) break;
                        --this.T;
                        break;
                    }
                    case 7: 
                    case 23: {
                        if (this.T >= 2) break;
                        ++this.T;
                        break;
                    }
                    case 6: 
                    case 22: 
                    case 38: {
                        if (this.T == 2) {
                            this.startMissionBriefing(0, 81, this.selectedTrainProduct);
                            break;
                        }
                        if (bl2 && this.T == 0) {
                            this.y();
                        }
                        this.pendingScreenState = 6;
                        break;
                    }
                    case 47: {
                        this.T = 1;
                        this.pendingScreenState = 6;
                    }
                }
                this.keyActionPulse = 0;
                this.keyActionHeld = 0;
                if (bl2) {
                    n8 = 0;
                } else {
                    n8 = 4;
                    if (this.T == 0) {
                        this.T = 1;
                    }
                }
                boolean bl3 = false;
                int n9 = this.a(this.var_java_lang_String_a);
                if (n9 < 68) {
                    n9 = 68;
                }
                n9 += 52;
                int n10 = 17 + this.ah + 11;
                if (n10 < 44) {
                    n10 = 44;
                }
                int n11 = this.screenW - n9 >> 1;
                int n12 = (this.clipBottom - this.clipTop >> 1) + this.clipTop - (n10 >> 1);
                graphics.setColor(0);
                graphics.fillRect(n11 + 1, n12 + 1, n9, n10);
                graphics.setColor(7031296);
                graphics.fillRect(n11, n12, n9, n10);
                graphics.setColor(11899986);
                graphics.fillRect(n11 + 1, n12 + 1, n9 - 2, n10 - 2);
                graphics.setColor(14595245);
                graphics.drawString(this.var_java_lang_String_a, n11 + 36 + 8, n12 + 4 - 2, 20);
                graphics.setColor(0);
                graphics.drawString(this.var_java_lang_String_a, n11 + 36 + 7, n12 + 3 - 2, 20);
                this.a(graphics, this.x, n11 + 3, n12 + (n10 - 36 >> 1), this.selectedTrainProduct * 36, 0, 36, 36, 0, n8);
                n12 = n12 + n10 - 4 - 20;
                if ((this.tickCount & 1) == 0) {
                    graphics.setColor(0xFFFFFF);
                } else {
                    graphics.setColor(0xFF0000);
                }
                int n13 = n9 - 32;
                n13 /= 3;
                n11 += 44;
                if (this.T == 0) {
                    graphics.drawRect(n11 - 2, n12 - 2, 27, 19);
                }
                this.a(graphics, this.x, n11, n12, 864, 0, 24, 17, 0, 0);
                if (this.T == 1) {
                    graphics.drawRect(n11 + n13 - 2, n12 - 2, 27, 19);
                }
                this.a(graphics, this.x, n11 + n13, n12, 864, 17, 24, 17, 0, 0);
                if (this.T == 2) {
                    graphics.drawRect(n11 + (n13 << 1) - 2, n12 - 2, 15, 22);
                }
                this.a(graphics, this.x, n11 + (n13 << 1), n12, 888, 0, 12, 20, 0, 0);
            }
        }
        this.a(graphics, this.var_int_arr_c[this.selectedTrainProduct], this.var_java_lang_String_arr_a[this.selectedTrainProduct << 1], this.var_java_lang_String_arr_a[(this.selectedTrainProduct << 1) + 1]);
        if (!this.var_boolean_a) {
            this.t = this.cursorTileIdx;
            this.pendingScreenState = 6;
        }
    }

    final void i() {
        boolean bl = false;
        boolean bl2 = false;
        int n = 1000;
        switch (this.keyActionPulse) {
            case 19: {
                n = this.T - 3;
                bl = true;
                break;
            }
            case 25: {
                n = this.T + 3;
                bl = true;
                break;
            }
            case 21: {
                n = this.T - 1;
                bl = true;
                break;
            }
            case 23: {
                n = this.T + 1;
                bl = true;
                break;
            }
            case 2: {
                n = 0;
                break;
            }
            case 3: {
                n = 1;
                break;
            }
            case 4: {
                n = 2;
                break;
            }
            case 5: {
                n = 3;
                break;
            }
            case 6: {
                n = 4;
                break;
            }
            case 7: {
                n = 5;
                break;
            }
            case 8: {
                n = 6;
                break;
            }
            case 9: {
                n = 7;
                break;
            }
            case 10: {
                n = 8;
                break;
            }
            case 22: 
            case 38: {
                bl2 = true;
                break;
            }
            case 47: {
                this.requestStateSwitch(6);
                this.t = this.cursorTileIdx;
                return;
            }
        }
        if ((n &= 0xFF) == this.T) {
            bl2 = true;
        } else if (n < this.W) {
            this.T = n;
        }
        if (bl) {
            return;
        }
        if (this.T != this.selectedTrainProduct) {
            this.R = 40;
        }
        this.selectedTrainProduct = this.T;
        if (bl2) {
            this.y();
            this.requestStateSwitch(8);
            this.t = this.cursorTileIdx;
        }
    }

    final void y() {
        switch (this.var_int_g) {
            case 0: {
                if (this.playerUnitHeaders[0][2] + this.playerUnitHeaders[0][49] >= this.playerUnitHeaders[0][3] || !this.canAfford(0, 0, this.var_int_arr_c[this.selectedTrainProduct])) break;
                this.queueUnitTraining(0, this.var_int_arr_c[this.selectedTrainProduct]);
                return;
            }
            case 1: {
                int n = this.var_int_arr_c[this.selectedTrainProduct];
                if (n > 12) {
                    n = 12;
                }
                if (!this.canAfford(0, 1, this.var_int_arr_c[this.selectedTrainProduct])) break;
                this.selectionMode = 1;
                this.p = n;
                this.var_int_d = this.cameraPxX + this.cursorScreenPxX;
                this.S = this.cameraPxY + this.cursorScreenPxY;
                this.au = this.screenH;
                this.U = 8;
                return;
            }
            case 2: {
                if (!this.canAfford(0, 2, this.var_int_arr_c[this.selectedTrainProduct])) break;
                int[] nArray = this.buildingTable[0];
                int n = this.aD + 2;
                nArray[n] = nArray[n] | 0x20000000;
                int[] nArray2 = this.buildingTable[0];
                int n2 = this.aD + 2;
                nArray2[n2] = nArray2[n2] | 0x10000;
                int[] nArray3 = this.buildingTable[0];
                int n3 = this.aD + 2;
                nArray3[n3] = nArray3[n3] & 0xFFFF00FF;
                this.payCost(0, 2, this.var_int_arr_c[this.selectedTrainProduct]);
            }
        }
    }

    final boolean boolean_k(int n) {
        if (!this.var_boolean_a) {
            return true;
        }
        switch (this.var_int_g) {
            case 0: {
                if (this.techFlags[0 + n] == 0) {
                    return false;
                }
                if (n != 9 || this.playerUnitHeaders[0][74] + this.playerUnitHeaders[0][65] <= 0) break;
                return false;
            }
            case 1: {
                if (this.techFlags[10 + n] == 0) {
                    return false;
                }
                switch (n) {
                    case 11: {
                        if (this.countBuildings(0, 11, false) < 4) break;
                        return false;
                    }
                    case 12: {
                        if (this.countBuildings(0, 12, false) < 5) break;
                        return false;
                    }
                    case 1: {
                        if (this.countBuildings(0, 1, false) < 2) break;
                        return false;
                    }
                }
                break;
            }
            case 2: {
                if (this.techFlags[23 + n] != 0) break;
                return false;
            }
        }
        return true;
    }

    public final boolean boolean_i(int n) {
        this.clipLeft = 0;
        this.clipRight = this.screenW;
        return true;
    }

    public final void c(Graphics graphics) {
        this.d(graphics);
        this.pendingScreenState = 6;
    }

    public final void d(Graphics graphics) {
        graphics.setClip(0, 0, this.screenW, this.screenH);
        int n = this.screenW - 240 >> 1;
        graphics.setColor(0);
        if (n > 0) {
            graphics.fillRect(0, 0, this.screenW, 21);
        } else {
            graphics.fillRect(0, 19, this.screenW, 1);
        }
        this.a(graphics, 16, n, 0, 0, 0, 240, 19, 0, 0);
        this.b(graphics, this.playerUnitHeaders[0][5], n + 26, 3, 3);
        this.b(graphics, this.playerUnitHeaders[0][6], n + 84, 3, 3);
        this.b(graphics, this.playerUnitHeaders[0][7], n + 140, 3, 3);
        this.b(graphics, this.playerUnitHeaders[0][2], n + 197, 3, 2);
        this.b(graphics, this.playerUnitHeaders[0][3], n + 220, 3, 2);
        this.clipTop = 21;
        this.var_boolean_l = false;
    }

    public final void b(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5;
        int n6 = 1;
        for (n5 = 0; n5 < n4 - 1; ++n5) {
            n6 *= 10;
        }
        if (n >= n6 * 10) {
            n = 999;
        } else if (n < 0) {
            n = 0;
        }
        while (n4 > 0) {
            n5 = n / n6;
            this.a(graphics, 16, n2, n3, n5 * 8 + 240, 0, 8, 12, 0, 0);
            n2 += 9;
            n -= n5 * n6;
            --n4;
            n6 /= 10;
        }
    }

    public final void a(Graphics graphics, int n, String string, String string2) {
        int n2 = 0;
        if (this.screenW > 128) {
            n2 += 2;
        }
        int n3 = 0;
        int n4 = 17;
        switch (this.var_int_g) {
            case 0: {
                n3 = n + 0;
                n4 = 17;
                break;
            }
            case 1: {
                n3 = n + 10;
                n4 = 18;
                break;
            }
            case 2: {
                n3 = n + 26;
                n4 = 19;
            }
        }
        int n5 = 0;
        if (this.var_boolean_a && (this.costTable[n3 *= 3] > this.playerUnitHeaders[0][5] || this.costTable[n3 + 1] > this.playerUnitHeaders[0][6] || this.costTable[n3 + 2] > this.playerUnitHeaders[0][7])) {
            n5 = 4;
        }
        int n6 = this.screenH - 19 + 2;
        this.a(graphics, n4, n2, this.bottomBarY + (this.screenH - this.bottomBarY - 36 >> 1), n * 36, 0, 36, 36, 0, n5);
        this.a(graphics, 16, 41, n6, 1, 1, 27, 17, 0, 0);
        this.b(graphics, this.costTable[n3++], 67, n6, 2);
        this.a(graphics, 16, 99, n6, 58, 1, 27, 17, 0, 0);
        this.b(graphics, this.costTable[n3++], 125, n6, 2);
        this.a(graphics, 16, 157, n6, 115, 1, 27, 17, 0, 0);
        this.b(graphics, this.costTable[n3], 183, n6, 2);
        this.var_java_lang_String_b = string + " " + string2;
    }

    /** 相机缓动：目标 = 光标格的 iso 坐标减半屏偏移（ad/J），1/3 逐帧趋近。
     *  最后两行维护 cursorTileIdx（光标格的 mapTiles 下标；越界或空地形 768 时置 -1）。 */
    public final void updateCamera() {
        this.camTargetX = (this.cursorTileX - this.cursorTileY << 4) - this.ad;
        this.camTargetY = (this.cursorTileX + this.cursorTileY << 3) - (this.J >> 1) + 8;
        this.cameraPxX = ((this.camTargetX << 1) + this.cameraPxX + 1) / 3;
        this.cameraPxY = ((this.camTargetY << 1) + this.cameraPxY + 1) / 3;
        this.cursorTileIdx = this.cursorTileX + (this.cursorTileY << 6) & 0xFFF;
        if ((this.cursorTileIdx & 0xFFFFF000) != 0) {
            this.cursorTileIdx = -1;
            return;
        }
        if ((this.mapTiles[this.cursorTileIdx] & 0xFFF) == 768) {
            this.cursorTileIdx = -1;
        }
    }

    /** 由逻辑分辨率派生全部屏幕度量——原游戏按多分辨率 J2ME 机型设计，加宽逻辑屏
     * （-Daoe.width，run.sh 默认 720）即经此自动适配：可视格数、镜头居中偏移、底栏。
     * 注意 viewTileCols 以 64px/列、viewTileRows 以 16px/行为步长（iso 菱形 32x16 的
     * 遍历步进），不是"每屏格数"。 */
    public final void setupScreenMetrics(int n, int n2) {
        this.screenW = n;
        this.screenH = n2;
        this.viewTileCols = (this.screenW >> 6) + 3;
        this.viewTileRows = (this.screenH >> 4) + 5;
        // ad/J：把光标格挪到屏幕中心的 iso 空间偏移（updateCamera 用；2:1 菱形投影
        // 的精确推导未考证，只动渲染/镜头时把它们当"半屏修正"对待即可）。
        this.ad = (this.screenW >> 1) + 64 >> 1;
        this.J = this.screenH + 48 >> 1;
        // 底栏（消息/跑马灯条）上缘的 y：高度 = 字体高 + 顶栏高，下限 36。
        this.bottomBarY = this.var_int_f + 19 + 2;
        if (this.bottomBarY < 36) {
            this.bottomBarY = 36;
        }
        this.bottomBarY = this.screenH - this.bottomBarY;
    }

    public final void onScreenSizeChanged(int n, int n2) {
        this.setupScreenMetrics(n, n2);
    }

    public final void e() {
        this.var_byte_arr_d = null;
        this.var_byte_arr_b = null;
        this.var_javax_microedition_lcdui_Image_arr_a = null;
        this.var_byte_arr_h = null;
        this.var_byte_arr_d = com.ulysseo.mad.c.byte_arr_a(102);
        this.var_byte_arr_b = com.ulysseo.mad.c.byte_arr_a(100);
        this.var_javax_microedition_lcdui_Image_arr_a = new Image[181];
        this.var_byte_arr_h = com.ulysseo.mad.c.byte_arr_a(101);
    }

    public final void s() {
        for (int i = 0; i < 181; ++i) {
            this.var_javax_microedition_lcdui_Image_arr_a[i] = null;
        }
        this.var_javax_microedition_lcdui_Graphics_a = null;
    }

    public final Image javax_microedition_lcdui_Image_a(int n, int n2, int n3, int n4) {
        if (this.var_javax_microedition_lcdui_Image_arr_a[n] != null) {
            return this.var_javax_microedition_lcdui_Image_arr_a[n];
        }
        try {
            this.var_javax_microedition_lcdui_Image_arr_a[n] = Image.createImage((int)n2, (int)n3);
        }
        catch (Exception exception) {
            this.var_boolean_h = true;
            return null;
        }
        this.var_javax_microedition_lcdui_Graphics_a = this.var_javax_microedition_lcdui_Image_arr_a[n].getGraphics();
        this.var_javax_microedition_lcdui_Graphics_a.setColor(n4);
        this.var_javax_microedition_lcdui_Graphics_a.fillRect(0, 0, n2, n3);
        return this.var_javax_microedition_lcdui_Image_arr_a[n];
    }

    public final Image javax_microedition_lcdui_Image_a(int n, int n2, int n3) {
        byte[] byArray = com.ulysseo.mad.c.byte_arr_a(n);
        Image image = AgeOfEmpires.b.javax_microedition_lcdui_Image_a(byArray, n2, n3);
        return image;
    }

    public final Image javax_microedition_lcdui_Image_a(int n, int n2) {
        int n3 = n;
        if (n <= 3) {
            if (n2 == 2) {
                n += 173;
            }
        } else if (n >= 17 && n <= 19) {
            if (n2 != 0) {
                n = n - 17 + 177;
            }
        } else if (n >= 22) {
            n += (n2 & 3) * 38;
        }
        if (this.var_javax_microedition_lcdui_Image_arr_a[n] == null) {
            if (Runtime.getRuntime().freeMemory() < 50000L) {
                this.var_boolean_h = true;
                return null;
            }
            try {
                this.var_javax_microedition_lcdui_Image_arr_a[n] = this.javax_microedition_lcdui_Image_a(2 + n3, 0, n2);
            }
            catch (Exception exception) {
                this.var_boolean_h = true;
                return null;
            }
        }
        return this.var_javax_microedition_lcdui_Image_arr_a[n];
    }

    public final void drawTileSprite(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5 = n << 1;
        n2 -= this.var_byte_arr_b[n5++];
        n3 -= this.var_byte_arr_b[n5];
        Image image = this.javax_microedition_lcdui_Image_a(n, n4);
        if (image != null) {
            graphics.drawImage(image, n2, n3, 0);
        }
    }

    public final void a(Graphics graphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9) {
        int n10 = n << 1;
        n2 -= this.var_byte_arr_b[n10++];
        n3 -= this.var_byte_arr_b[n10];
        Image image = this.javax_microedition_lcdui_Image_a(n, n9);
        if (image != null) {
            com.ulysseo.mad.d.a(graphics, image, n4, n5, n6, n7, n8, n2, n3, 0);
        }
    }

    public final int int_a(int n, int n2, int n3, int n4) {
        int n5 = n >> 16 & 0xFF;
        int n6 = n >> 8 & 0xFF;
        int n7 = n & 0xFF;
        int n8 = n2 >> 16 & 0xFF;
        int n9 = n2 >> 8 & 0xFF;
        int n10 = n2 & 0xFF;
        if (n3 < 0) {
            n3 = 0;
        }
        if (n3 >= n4) {
            return n;
        }
        n8 = ((n5 - n8 << 16) / n4 * n3 >> 16) + n8;
        n9 = ((n6 - n9 << 16) / n4 * n3 >> 16) + n9;
        n10 = ((n7 - n10 << 16) / n4 * n3 >> 16) + n10;
        if (n8 < 0) {
            n8 = 0;
        } else if (n8 > 255) {
            n8 = 255;
        }
        if (n9 < 0) {
            n9 = 0;
        } else if (n9 > 255) {
            n9 = 255;
        }
        if (n10 < 0) {
            n10 = 0;
        } else if (n10 > 255) {
            n10 = 255;
        }
        return n8 << 16 | n9 << 8 | n10;
    }

    public final void a(Graphics graphics, String string, int n, int n2, int n3) {
        graphics.setColor(this.int_a(0xFFFFFF, 14595245, n3, 5));
        graphics.drawString(string, n + 1, n2 + 1, 20);
        graphics.setColor(this.int_a(0, 14595245, n3, 5));
        graphics.drawString(string, n, n2, 20);
    }

    /** 游戏内光标移动（按 ab 持续消费方向动作；ab==0 直接返回——"按住"的判定点）。 */
    public final void tickCursor() {
        if (this.cursorTileIdx != this.t) {
            this.clipBottom = this.screenH;
        }
        int n = this.keyActionPulse;
        if (this.keyActionHeld == 0) {
            return;
        }
        if (this.keyActionHeld == this.keyRepeatLast) {
            ++this.keyRepeatFrames;
            if (this.keyRepeatFrames >= 5) {
                n = this.keyActionHeld;
            }
        } else {
            this.keyRepeatFrames = 0;
            this.keyRepeatLast = this.keyActionHeld;
        }
        switch (n) {
            case 2: {
                --this.cursorTileX;
                break;
            }
            case 3: 
            case 19: {
                --this.cursorTileX;
                --this.cursorTileY;
                break;
            }
            case 4: {
                --this.cursorTileY;
                break;
            }
            case 5: 
            case 21: {
                --this.cursorTileX;
                ++this.cursorTileY;
                break;
            }
            case 7: 
            case 23: {
                ++this.cursorTileX;
                --this.cursorTileY;
                break;
            }
            case 9: 
            case 25: {
                ++this.cursorTileX;
                ++this.cursorTileY;
                break;
            }
            case 8: {
                ++this.cursorTileY;
                break;
            }
            case 10: {
                ++this.cursorTileX;
            }
        }
        // 移植修正：钳回地图内。原版无钳制，光标可走出 [0,63]（cursorTileIdx 掩码
        // 后指向错误格），confirmAtCursor 对出界格全部静默失效——行军途中连按
        // 方向键走出边界后，FIRE 就"没反应"了（玩家第4轮实测）。
        if (this.cursorTileX < 0) {
            this.cursorTileX = 0;
        } else if (this.cursorTileX > 63) {
            this.cursorTileX = 63;
        }
        if (this.cursorTileY < 0) {
            this.cursorTileY = 0;
        } else if (this.cursorTileY > 63) {
            this.cursorTileY = 63;
        }
        switch (this.keyActionPulse) {
            case 6: 
            case 22: 
            case 38: {
                this.confirmAtCursor();
                return;
            }
            case 1: {
                this.var_boolean_h = true;
                this.requestStateSwitch(1);
                return;
            }
            case 47: {
                int n2;
                if (this.cursorTileIdx != -1 && (this.mapTiles[this.cursorTileIdx] & 0x300) == 256 && (n2 = (this.mapTiles[this.cursorTileIdx] & 0xC00) >> 10) == 0) {
                    int n3 = (this.mapTiles[this.cursorTileIdx] & 0xFF) << 2;
                    if ((this.buildingTable[0][n3 + 2] & 0x20000000) != 0) {
                        return;
                    }
                    if ((this.buildingTable[0][n3 + 2] & 0xFF0000) != 0) {
                        int[] nArray = this.buildingTable[0];
                        int n4 = n3 + 2;
                        nArray[n4] = nArray[n4] - 65536;
                        int[] nArray2 = this.buildingTable[0];
                        int n5 = n3 + 2;
                        nArray2[n5] = nArray2[n5] & 0xDFFFFFFF;
                        if ((this.buildingTable[0][n3 + 2] & 0xFF0000) == 0) {
                            int[] nArray3 = this.buildingTable[0];
                            int n6 = n3 + 2;
                            nArray3[n6] = nArray3[n6] & 0xFFFF00FF;
                        }
                        int n7 = this.playerUnitHeaders[0][0];
                        int n8 = 1;
                        int n9 = this.buildingTable[0][n3 + 3] & 0xFF;
                        if (n9 == 10) {
                            n8 = n7 == 0 ? 2 : 3;
                        } else if (n9 == 7) {
                            n8 = 4;
                        } else if (n9 == 8) {
                            n8 = n7 >= 2 ? 6 : 5;
                        } else if (n9 == 6) {
                            n8 = 8;
                        } else if (n9 == 2) {
                            n8 = 7;
                        } else if (n9 == 3) {
                            n8 = 9;
                        }
                        int[] nArray4 = this.playerUnitHeaders[0];
                        nArray4[49] = nArray4[49] - 1;
                        if (n8 < 2) {
                            int[] nArray5 = this.playerUnitHeaders[0];
                            nArray5[66] = nArray5[66] - 1;
                            return;
                        }
                        int[] nArray6 = this.playerUnitHeaders[0];
                        int n10 = 66 + n8 - 1;
                        nArray6[n10] = nArray6[n10] - 1;
                        return;
                    }
                    if ((this.buildingTable[0][n3 + 2] & 0x40000000) != 0) {
                        n3 = this.mapTiles[this.cursorTileIdx] & 0xFF;
                        int n11 = this.buildingTable[0][(n3 << 2) + 3] & 0xFF;
                        this.onThingDestroyed(0, n3);
                        this.refundCost(0, 1, n11);
                        return;
                    }
                }
                if (this.selectionMode == 1) {
                    this.clearSelection();
                    return;
                }
                if (this.selectedType != -1) {
                    if (this.selectedType == 256 && this.selectionPlayer == 0 && (n2 = this.buildingTable[0][this.selectedSlot + 2] & 0xFF0000) != 0) {
                        int[] nArray = this.buildingTable[0];
                        int n12 = this.selectedSlot + 2;
                        nArray[n12] = nArray[n12] - 65536;
                        return;
                    }
                    this.clearSelection();
                    return;
                }
                if (this.selectionMode != 0) {
                    this.clearSelection();
                    return;
                }
                if (!this.requestStateSwitch(4)) break;
                this.clipTop = 0;
                this.clipLeft = 0;
                this.clipBottom = this.screenH;
                this.clipRight = this.screenW;
                this.pendingPanelSwitch = this.menuScreenId = 6;
                return;
            }
            case 11: {
                if (!this.requestStateSwitch(7)) break;
                this.var_int_g = 0;
                this.x = 17;
                this.G = 67;
                this.X = 10;
                this.var_boolean_a = true;
                this.var_boolean_g = true;
                return;
            }
            case 12: {
                if (!this.requestStateSwitch(7)) break;
                this.var_int_g = 1;
                this.x = 18;
                this.G = 63;
                this.X = 13;
                this.var_boolean_a = true;
                this.var_boolean_g = true;
            }
        }
    }

    public final void e(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5;
        int n6;
        Image image = this.javax_microedition_lcdui_Image_a(14, 0);
        if (image == null) {
            return;
        }
        int n7 = 42;
        int n8 = 38;
        if (n3 < 84) {
            n7 = n3 >> 1;
        }
        if (n4 < 76) {
            n8 = n4 >> 1;
        }
        graphics.setColor(14595245);
        graphics.fillRect(n + n7, n2 + n8, n3 - n7 - n7, n4 - n8 - n8);
        com.ulysseo.mad.d.a(graphics, image, 0, 0, n7, n8, 0, n, n2, 0);
        com.ulysseo.mad.d.a(graphics, image, 0, 0, n7, n8, 1, n, n2 + n4 - n8 - 1, 0);
        if (n7 == 42) {
            n6 = n + 42;
            n5 = 2;
            while (n6 < n + n3 - n7) {
                com.ulysseo.mad.d.a(graphics, image, 18, 0, 24, n8, n5, n6, n2, 0);
                com.ulysseo.mad.d.a(graphics, image, 18, 0, 24, n8, n5 | 1, n6, n2 + n4 - n8, 0);
                n6 += 24;
                n5 ^= 2;
            }
        }
        if (n8 == 38) {
            n6 = n2 + 38;
            n5 = 1;
            while (n6 < n2 + n4 - n8) {
                com.ulysseo.mad.d.a(graphics, image, 0, 17, n7, 21, n5, n, n6, 0);
                com.ulysseo.mad.d.a(graphics, image, 0, 17, n7, 21, n5 | 2, n + n3 - n7, n6, 0);
                n6 += 21;
                n5 ^= 1;
            }
        }
        com.ulysseo.mad.d.a(graphics, image, 0, 0, n7, n8, 2, n + n3 - n7 - 1, n2, 0);
        com.ulysseo.mad.d.a(graphics, image, 0, 0, n7, n8, 3, n + n3 - n7 - 1, n2 + n4 - n8 - 1, 0);
    }

    public final boolean openBuildingMenu(int n) {
        if ((this.buildingTable[0][(n <<= 2) + 2] & 0x40000000) != 0) {
            return false;
        }
        if ((this.buildingTable[0][n + 2] & 0xFF) != 255 && this.selectedType < 2 && this.selectionPlayer == 0) {
            return false;
        }
        this.var_int_g = -1;
        int n2 = this.playerUnitHeaders[0][0];
        switch (this.buildingTable[0][n + 3] & 0xFF) {
            case 9: {
                this.var_int_g = 2;
                this.selectedTrainProduct = 21 + n2;
                if (n2 == 0) {
                    if (this.countBuildings(0, 10, true) >= 1) break;
                    return false;
                }
                if (n2 == 1) {
                    if (this.countBuildings(0, 5, true) + this.countBuildings(0, 6, true) >= 2) break;
                    return false;
                }
                if (n2 == 2) {
                    if (this.countBuildings(0, 3, true) >= 1) break;
                    return false;
                }
                return false;
            }
            case 4: {
                if (this.techFlags[37] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 14;
                } else if (this.techFlags[34] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 11;
                } else if (this.techFlags[33] == 0 && n2 >= 3) {
                    this.selectedTrainProduct = 10;
                } else if (this.techFlags[35] == 0 && n2 >= 3) {
                    this.selectedTrainProduct = 12;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 0: {
                if (this.techFlags[26] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 3;
                } else if (this.techFlags[24] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 1;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 1: {
                if (this.techFlags[28] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 5;
                } else if (this.techFlags[32] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 9;
                } else if (this.techFlags[42] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 19;
                } else if (this.techFlags[41] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 18;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 5: {
                if (this.techFlags[29] != 0) {
                    return false;
                }
                this.selectedTrainProduct = 6;
                this.var_int_g = 2;
                break;
            }
            case 6: {
                if (this.techFlags[27] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 4;
                } else if (this.techFlags[31] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 8;
                } else if (this.techFlags[30] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 7;
                } else if (this.techFlags[25] == 0 && n2 >= 2) {
                    this.selectedTrainProduct = 2;
                } else if (this.techFlags[23] == 0 && n2 >= 3) {
                    this.selectedTrainProduct = 0;
                } else if (this.techFlags[38] == 0 && n2 >= 3) {
                    this.selectedTrainProduct = 15;
                } else {
                    if (n2 < 3) {
                        return false;
                    }
                    this.selectedTrainProduct = 8;
                    this.var_int_g = 0;
                    break;
                }
                this.var_int_g = 2;
                break;
            }
            case 11: {
                this.selectedTrainProduct = 0;
                this.var_int_g = 0;
                break;
            }
            case 10: {
                this.selectedTrainProduct = n2 == 0 ? 2 : 3;
                this.var_int_g = 0;
                break;
            }
            case 7: {
                this.selectedTrainProduct = 4;
                this.var_int_g = 0;
                break;
            }
            case 8: {
                this.selectedTrainProduct = n2 == 1 ? 5 : 6;
                this.var_int_g = 0;
                break;
            }
            case 3: {
                if (this.playerUnitHeaders[0][74] + this.playerUnitHeaders[0][65] > 0) {
                    return false;
                }
                this.selectedTrainProduct = 9;
                this.var_int_g = 0;
                break;
            }
            case 2: {
                this.selectedTrainProduct = 7;
                this.var_int_g = 0;
                break;
            }
            case 12: {
                if (this.techFlags[43] == 1) {
                    return false;
                }
                this.var_int_g = 2;
                if (this.techFlags[43] == 0 && this.techFlags[40] != 0 && n2 >= 3) {
                    this.selectedTrainProduct = 20;
                    break;
                }
                if (this.techFlags[40] == 0 && this.techFlags[36] != 0 && n2 >= 2) {
                    this.selectedTrainProduct = 17;
                    break;
                }
                if (this.techFlags[36] == 0 && n2 >= 1) {
                    this.selectedTrainProduct = 13;
                    break;
                }
                return false;
            }
            default: {
                return false;
            }
        }
        switch (this.var_int_g) {
            case 2: {
                if ((this.buildingTable[0][n + 2] & 0x20000000) != 0) {
                    return true;
                }
                if (this.requestStateSwitch(7)) {
                    this.x = 19;
                    this.G = 64;
                    this.X = 24;
                    this.var_boolean_a = true;
                    this.var_boolean_g = false;
                    this.aD = n;
                }
                int[] nArray = this.buildingTable[0];
                int n3 = n + 2;
                nArray[n3] = nArray[n3] | Integer.MIN_VALUE;
                int[] nArray2 = this.buildingTable[0];
                int n4 = n + 2;
                nArray2[n4] = nArray2[n4] & 0xFF00FFFF;
                break;
            }
            case 0: {
                if (this.playerUnitHeaders[0][3] <= this.playerUnitHeaders[0][49] + this.playerUnitHeaders[0][2]) {
                    return true;
                }
                if (this.requestStateSwitch(7)) {
                    this.var_boolean_a = false;
                    this.x = 17;
                    this.G = 67;
                    this.X = 10;
                    this.var_boolean_g = false;
                }
                int[] nArray = this.playerUnitHeaders[0];
                nArray[49] = nArray[49] + 1;
                if (this.selectedTrainProduct < 2) {
                    int[] nArray3 = this.playerUnitHeaders[0];
                    nArray3[66] = nArray3[66] + 1;
                } else {
                    int[] nArray4 = this.playerUnitHeaders[0];
                    int n5 = 66 + this.selectedTrainProduct - 1;
                    nArray4[n5] = nArray4[n5] + 1;
                }
                int[] nArray5 = this.buildingTable[0];
                int n6 = n + 2;
                nArray5[n6] = nArray5[n6] + 65536;
                break;
            }
            default: {
                return false;
            }
        }
        return true;
    }

    final void confirmAtCursor() {
        if (((this.cursorTileX | this.cursorTileY) & 0xFFFFFFC0) != 0 || this.cursorTileIdx == -1) {
            return;
        }
        int n = (this.mapTiles[this.cursorTileIdx] & 0xC00) >> 10;
        int n2 = this.mapTiles[this.cursorTileIdx] & 0x300;
        int n3 = this.mapTiles[this.cursorTileIdx] & 0xFF;
        if (n2 == 256 && n == 0 && this.openBuildingMenu(n3)) {
            return;
        }
        switch (this.selectionMode) {
            case 0: {
                if (n2 == 256 && (this.buildingTable[n][(n3 << 2) + 2] & 0x40000000) != 0) {
                    return;
                }
                this.selectUnderCursor(n, n2, n3);
                this.selectionMode = 6;
                return;
            }
            case 6: {
                if (n == 0) {
                    if (this.selectionPlayer == 0 && n2 == 256 && this.selectionMark == 512 && this.selectedType >= 2) {
                        this.clearSelection();
                        this.selectUnderCursor(n, n2, n3);
                        return;
                    }
                    if (n2 == 512) {
                        if (this.selectionMark == 512 && this.selectionPlayer == 0) {
                            int n4 = this.playerUnitSlots[0][(n3 << 3) + 3] & 0xFF;
                            if (this.selectedType != -1 && this.selectedType == n4) {
                                if (this.selectedSlot == -1) {
                                    this.selectUnits(0, -1);
                                    return;
                                }
                                if (this.selectedSlot == n3 << 3) {
                                    this.selectUnits(0, n4);
                                    return;
                                }
                            }
                        }
                        this.clearSelection();
                        this.selectUnderCursor(n, n2, n3);
                        return;
                    }
                }
                if (this.selectionPlayer == 0 && this.selectionMark == 512) {
                    this.var_int_d = this.cameraPxX + this.cursorScreenPxX;
                    this.S = this.cameraPxY + this.cursorScreenPxY;
                    this.au = this.screenH;
                    this.U = 8;
                    this.orderMove(0, this.cursorTileX, this.cursorTileY);
                    if (this.selectedType >= 2 || n2 != 768 || (this.mapTiles[this.cursorTileIdx] & 0xF000) != 0) break;
                    this.clearSelection();
                    this.selectionMode = 0;
                    return;
                }
                this.clearSelection();
                this.selectUnderCursor(n, n2, n3);
                return;
            }
            case 1: {
                if (n2 != 0 || this.mapTiles[this.cursorTileIdx] < 0) {
                    return;
                }
                if (this.canAfford(0, 1, this.p)) {
                    this.a(0, this.p, this.cursorTileX, this.cursorTileY, 0x40000000, true);
                }
                this.clearSelection();
                this.selectionMode = 0;
            }
        }
    }

    public final void selectUnderCursor(int n, int n2, int n3) {
        this.selectionMark = n2;
        this.selectionPlayer = n;
        this.R = 40;
        switch (n2) {
            case 512: {
                short[] sArray = this.playerUnitSlots[n];
                int n4 = (n3 <<= 3) + 4;
                sArray[n4] = (short)(sArray[n4] | 0x8000);
                this.selectedSlot = n3;
                this.selectedType = this.playerUnitSlots[n][n3 + 3] & 0xFF;
                this.var_boolean_h = true;
                if (this.clipBottom != this.screenH || !this.requestStateSwitch(7)) break;
                this.var_int_g = 0;
                this.x = 17;
                this.G = 67;
                this.var_boolean_a = false;
                this.X = 10;
                this.selectedTrainProduct = this.selectedType;
                break;
            }
            case 256: {
                int[] nArray = this.buildingTable[n];
                int n5 = (n3 <<= 2) + 2;
                nArray[n5] = nArray[n5] | Integer.MIN_VALUE;
                this.selectedSlot = n3;
                this.selectedType = this.buildingTable[n][n3 + 3] & 0xFF;
                this.var_boolean_h = true;
                if (!this.requestStateSwitch(7)) break;
                this.var_int_g = 1;
                this.x = 18;
                this.G = 63;
                this.var_boolean_a = false;
                this.X = 13;
                this.selectedTrainProduct = this.selectedType;
                break;
            }
            case 768: {
                int n6 = n3;
                this.mapTiles[n6] = (short)(this.mapTiles[n6] | 0x80);
                break;
            }
            default: {
                return;
            }
        }
        this.selectionMode = 6;
    }

    // selectUnits(n, n2)：把 n 玩家 type==n2 的全部活单位打上 0x8000 选中位并置
    // group-选中态（selectionMark=512 是"组选标记"不是数量！）。n2=-1=全体军事(≥2)，
    // n2<2=村民且只选闲置(word7 低 nibble==0——游戏自己的闲置判定，assign 宏同款)。
    // 不清旧组：调用方负责先 clearSelection（sel 宏的替换语义）。
    public final void selectUnits(int n, int n2) {
        this.selectionMark = 512;
        this.selectedType = n2;
        this.selectedSlot = -1;
        this.selectionPlayer = n;
        this.selectionMode = 6;
        int n3 = 0;
        int n4 = this.playerUnitHeaders[this.selectionPlayer][2];
        if (n2 == -1) {
            for (int i = 0; i < n4; ++i) {
                if ((this.playerUnitSlots[this.selectionPlayer][n3 + 3] & 0xFF) >= 2) {
                    short[] sArray = this.playerUnitSlots[this.selectionPlayer];
                    int n5 = n3 + 4;
                    sArray[n5] = (short)(sArray[n5] | 0x8000);
                }
                n3 += 8;
            }
        } else if (n2 < 2) {
            for (int i = 0; i < n4; ++i) {
                if ((this.playerUnitSlots[this.selectionPlayer][n3 + 3] & 0xFF) < 2 && (this.playerUnitSlots[this.selectionPlayer][n3 + 7] & 0xF) == 0) {
                    short[] sArray = this.playerUnitSlots[this.selectionPlayer];
                    int n6 = n3 + 4;
                    sArray[n6] = (short)(sArray[n6] | 0x8000);
                }
                n3 += 8;
            }
        } else {
            for (int i = 0; i < n4; ++i) {
                if ((this.playerUnitSlots[this.selectionPlayer][n3 + 3] & 0xFF) == this.selectedType) {
                    short[] sArray = this.playerUnitSlots[this.selectionPlayer];
                    int n7 = n3 + 4;
                    sArray[n7] = (short)(sArray[n7] | 0x8000);
                }
                n3 += 8;
            }
        }
    }

    // 真单位计数（r31 rally 事故修复基元）：playerUnitSlots 是唯一可信单位表
    // （stride 8，off+3=type）；a(n,type,false) 数的是建筑数组，勿再当单位用。
    // type<0 = 全军事(type>=2)。
    final int devCountUnits(int pl, int type) {
        int n = 0;
        int ucnt = this.playerUnitHeaders[pl][2];
        for (int i = 0, off = 0; i < ucnt; ++i, off += 8) {
            int t = this.playerUnitSlots[pl][off + 3] & 0xFF;
            if (type >= 0 ? t == type : t >= 2) {
                ++n;
            }
        }
        return n;
    }

    public final void clearSelection() {
        int n;
        boolean bl = false;
        this.selectionMode = 0;
        if (this.selectionMark == -1) {
            return;
        }
        int n2 = 0;
        for (n = 0; n < this.playerUnitHeaders[this.selectionPlayer][2]; ++n) {
            short[] sArray = this.playerUnitSlots[this.selectionPlayer];
            int n3 = n2 + 4;
            sArray[n3] = (short)(sArray[n3] & Short.MAX_VALUE);
            n2 += 8;
        }
        n2 = 0;
        for (n = 0; n < this.playerUnitHeaders[this.selectionPlayer][4]; ++n) {
            int[] nArray = this.buildingTable[this.selectionPlayer];
            int n4 = n2 + 2;
            nArray[n4] = nArray[n4] & Integer.MAX_VALUE;
            n2 += 4;
        }
        this.selectionMark = -1;
        this.selectedType = -1;
    }

    public final void orderMove(int n, int n2, int n3) {
        short s = (short)(n2 << 8 | n3);
        int n4 = 0;
        int n5 = 0;
        while (n5 < this.playerUnitHeaders[n][2]) {
            if ((this.playerUnitSlots[n][n4 + 4] & 0x8000) != 0) {
                this.playerUnitSlots[n][n4 + 2] = s;
                this.playerUnitSlots[n][n4 + 1] = this.playerUnitSlots[n][n4 + 0];
                this.playerUnitSlots[n][n4 + 7] = 0;
                short[] sArray = this.playerUnitSlots[n];
                int n6 = n4 + 3;
                sArray[n6] = (short)(sArray[n6] & 0xFF);
            }
            ++n5;
            n4 += 8;
        }
    }

    public final void payCost(int n, int n2, int n3) {
        int n4 = 0;
        switch (n2) {
            case 1: {
                n4 = 10;
                break;
            }
            case 0: {
                n4 = 0;
                break;
            }
            case 2: {
                n4 = 26;
            }
        }
        n3 += n4;
        n3 *= 3;
        int[] nArray = this.playerUnitHeaders[n];
        nArray[5] = nArray[5] - this.costTable[n3++];
        int[] nArray2 = this.playerUnitHeaders[n];
        nArray2[6] = nArray2[6] - this.costTable[n3++];
        int[] nArray3 = this.playerUnitHeaders[n];
        nArray3[7] = nArray3[7] - this.costTable[n3];
        if (this.playerUnitHeaders[n][5] < 0) {
            this.playerUnitHeaders[n][5] = 0;
        }
        if (this.playerUnitHeaders[n][6] < 0) {
            this.playerUnitHeaders[n][6] = 0;
        }
        if (this.playerUnitHeaders[n][7] < 0) {
            this.playerUnitHeaders[n][7] = 0;
        }
        this.var_boolean_l = true;
    }

    public final void refundCost(int n, int n2, int n3) {
        int n4 = 0;
        switch (n2) {
            case 1: {
                n4 = 10;
                break;
            }
            case 0: {
                n4 = 0;
                break;
            }
            case 2: {
                n4 = 26;
            }
        }
        n3 += n4;
        n3 *= 3;
        int[] nArray = this.playerUnitHeaders[n];
        nArray[5] = nArray[5] + this.costTable[n3++];
        int[] nArray2 = this.playerUnitHeaders[n];
        nArray2[6] = nArray2[6] + this.costTable[n3++];
        int[] nArray3 = this.playerUnitHeaders[n];
        nArray3[7] = nArray3[7] + this.costTable[n3];
        this.var_boolean_l = true;
    }

    public final boolean canAfford(int n, int n2, int n3) {
        int n4 = 0;
        switch (n2) {
            case 1: {
                n4 = 10;
                break;
            }
            case 0: {
                n4 = 0;
                break;
            }
            case 2: {
                n4 = 26;
            }
        }
        n3 += n4;
        n3 *= 3;
        if (this.playerUnitHeaders[n][5] < this.costTable[n3++]) {
            return false;
        }
        if (this.playerUnitHeaders[n][6] < this.costTable[n3++]) {
            return false;
        }
        return this.playerUnitHeaders[n][7] >= this.costTable[n3];
    }

    final void p() {
        int n;
        for (n = 0; n < this.playerUnitHeaders[0][2]; ++n) {
            this.void_d(0, n);
        }
        if (this.playerUnitHeaders[0][4] > 0) {
            n = this.tickCount % this.playerUnitHeaders[0][4];
            int n2 = n << 2;
            int n3 = this.buildingTable[0][n2 + 0];
            int n4 = n3 & 0xFF;
            n3 >>>= 8;
            int n5 = this.buildingTable[0][n2 + 3] & 0xFF;
            if ((this.buildingTable[0][n2 + 2] & 0x40000000) == 0) {
                if (n5 == 12) {
                    this.void_a(n3, n4, 6);
                    return;
                }
                this.void_a(n3, n4, 3);
            }
        }
    }

    final void void_a(int n, int n2, int n3) {
        int n4 = n3;
        int n5 = 3 - (n3 << 1);
        for (int i = 0; n4 >= i; ++i) {
            int n6;
            for (n6 = 0; n6 < i << 1; ++n6) {
                int n7 = n - i + n6 + (n2 + n4 << 6) & 0xFFF;
                this.mapTiles[n7] = (short)(this.mapTiles[n7] & 0xFFF);
                int n8 = n - i + n6 + (n2 + n4 << 6) & 0xFFF;
                this.mapTiles[n8] = (short)(this.mapTiles[n8] | 0x1000);
                int n9 = n - i + n6 + (n2 - n4 << 6) & 0xFFF;
                this.mapTiles[n9] = (short)(this.mapTiles[n9] & 0xFFF);
                int n10 = n - i + n6 + (n2 - n4 << 6) & 0xFFF;
                this.mapTiles[n10] = (short)(this.mapTiles[n10] | 0x1000);
            }
            for (n6 = 0; n6 < n4 << 1; ++n6) {
                int n11 = n - n4 + n6 + (n2 + i << 6) & 0xFFF;
                this.mapTiles[n11] = (short)(this.mapTiles[n11] & 0x1FFF);
                int n12 = n - n4 + n6 + (n2 + i << 6) & 0xFFF;
                this.mapTiles[n12] = (short)(this.mapTiles[n12] | 0x1000);
                int n13 = n - n4 + n6 + (n2 - i << 6) & 0xFFF;
                this.mapTiles[n13] = (short)(this.mapTiles[n13] & 0x1FFF);
                int n14 = n - n4 + n6 + (n2 - i << 6) & 0xFFF;
                this.mapTiles[n14] = (short)(this.mapTiles[n14] | 0x1000);
            }
            if (n5 < 0) {
                n5 += (i << 1) + 3;
                continue;
            }
            n5 += (i - n4 << 1) + 10;
            --n4;
        }
    }

    final void a(int n, int n2, int n3, int n4, int n5) {
        short s = (short)n5;
        while (n4-- > 0) {
            int n6 = n3;
            int n7 = n4 + n2 << 6;
            int n8 = n;
            while (n6-- > 0) {
                int n9 = n8 + n7 & 0xFFF;
                this.mapTiles[n9] = (short)(this.mapTiles[n9] | s);
                ++n8;
            }
        }
    }

    final void e(int n, int n2, int n3, int n4) {
        while (n4-- > 0) {
            int n5 = n3;
            int n6 = n4 + n2 << 6;
            int n7 = n;
            while (n5-- > 0) {
                int n8 = n7 + n6 & 0xFFF;
                this.mapTiles[n8] = (short)(this.mapTiles[n8] & 0xFFF);
                int n9 = n7 + n6 & 0xFFF;
                this.mapTiles[n9] = (short)(this.mapTiles[n9] | 0x1000);
                ++n7;
            }
        }
    }

    public final void l(int n, int n2) {
        int n3 = n2 << 3;
        int n4 = this.playerUnitSlots[n][n3 + 0] >>> 8;
        int n5 = this.playerUnitSlots[n][n3 + 0] & 0xFF;
        int n6 = n4 + (n5 << 6) - 65 & 0xFFF;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if ((this.mapTiles[n6] & 0x1000) == 0) {
                    this.mapTiles[n6] = (short)(this.mapTiles[n6] & 0x3FFF | 0x4000);
                }
                ++n6;
                n6 &= 0xFFF;
            }
            n6 += 61;
            n6 &= 0xFFF;
        }
    }

    final void void_d(int n, int n2) {
        int n3 = n2 << 3;
        int n4 = this.playerUnitSlots[n][n3 + 0];
        int n5 = n4 >>> 8;
        int n6 = n5 + ((n4 &= 0xFF) << 6);
        int n7 = n6 - 65 & 0xFFF;
        this.mapTiles[n7] = (short)(this.mapTiles[n7] & 0x3FFF);
        int n8 = n6 - 64 & 0xFFF;
        this.mapTiles[n8] = (short)(this.mapTiles[n8] & 0x3FFF);
        int n9 = n6 - 63 & 0xFFF;
        this.mapTiles[n9] = (short)(this.mapTiles[n9] & 0x3FFF);
        int n10 = n6 - 1 & 0xFFF;
        this.mapTiles[n10] = (short)(this.mapTiles[n10] & 0x3FFF);
        int n11 = n6 & 0xFFF;
        this.mapTiles[n11] = (short)(this.mapTiles[n11] & 0x3FFF);
        int n12 = n6 + 1 & 0xFFF;
        this.mapTiles[n12] = (short)(this.mapTiles[n12] & 0x3FFF);
        int n13 = n6 + 63 & 0xFFF;
        this.mapTiles[n13] = (short)(this.mapTiles[n13] & 0x3FFF);
        int n14 = n6 + 64 & 0xFFF;
        this.mapTiles[n14] = (short)(this.mapTiles[n14] & 0x3FFF);
        int n15 = n6 + 65 & 0xFFF;
        this.mapTiles[n15] = (short)(this.mapTiles[n15] & 0x3FFF);
    }

    public final void spawnMission(int n) {
        int n2;
        this.a(0, 0, 64, 64, Short.MIN_VALUE);
        byte[] byArray = com.ulysseo.mad.c.byte_arr_a(n);
        int n3 = byArray[0] & 0xFF;
        int n4 = byArray[1] & 0xFF;
        if ((n3 | n4) != 0) {
            rngStateLo = n3;
            rngStateHi = n4;
        } else {
            this.randomMap = true;
        }
        for (n2 = 0; n2 < 4; ++n2) {
            this.var_int_arr_a[n2] = 0;
        }
        this.playerUnitHeaders[0][0] = byArray[2] & 0xFF;
        this.playerUnitHeaders[0][5] = byArray[3] & 0xFF;
        this.playerUnitHeaders[0][6] = byArray[4] & 0xFF;
        this.playerUnitHeaders[0][7] = byArray[5] & 0xFF;
        this.playerUnitHeaders[1][5] = byArray[6] & 0xFF;
        this.playerUnitHeaders[1][6] = byArray[7] & 0xFF;
        this.playerUnitHeaders[1][7] = byArray[8] & 0xFF;
        this.playerUnitHeaders[0][8] = -1;
        this.playerUnitHeaders[1][8] = -1;
        this.var_boolean_l = true;
        this.cursorTileX = byArray[9] & 0xFF;
        this.cursorTileY = byArray[10] & 0xFF;
        this.updateCamera();
        int n5 = (byArray[12] & 0xFF) << 8 | byArray[11] & 0xFF;
        int n6 = 13;
        block6: for (int i = 0; i < n5; ++i) {
            switch (byArray[n6++]) {
                case 1: {
                    n2 = byArray[n6++] & 0xFF;
                    int n7 = byArray[n6++] & 0xFF;
                    int n8 = byArray[n6++] & 0xFF;
                    int n9 = byArray[n6++] & 0xFF;
                    int n10 = byArray[n6++] & 0xFF;
                    this.a(n7, n2, n8, n9, n10, false);
                    continue block6;
                }
                case 2: {
                    n2 = byArray[n6++] & 0xFF;
                    int n7 = byArray[n6++] & 0xFF;
                    int n8 = byArray[n6++] & 0xFF;
                    int n9 = byArray[n6++] & 0xFF;
                    this.a(n7, n2, n8, n9, false);
                    continue block6;
                }
                case 3: {
                    n2 = byArray[n6++] | 0x300;
                    int n8 = byArray[n6++] & 0xFF;
                    int n9 = byArray[n6++] & 0xFF;
                    this.void_a(n2, n8, n9, 31);
                }
            }
        }
        this.a(64, 0, 0, 64, 768);
        this.a(0, 64, 64, 0, 768);
        this.e(64, 0, 0, 64);
        this.e(0, 64, 64, 0);
    }

    final void j() {
        for (int i = 0; i < 2; ++i) {
            int n = 0;
            int n2 = 0;
            while (n2 < this.playerUnitHeaders[i][4]) {
                int n3;
                int n4;
                int n5 = this.buildingTable[i][n + 2];
                if ((n5 & 0x40000000) != 0) {
                    n4 = this.buildingTable[i][n + 3] & 0xFF;
                    if (((n5 += 8) & 0xFFFF) >= 255) {
                        this.buildingTable[i][n + 2] = 255;
                        if (i == 0) {
                            this.ag = 20;
                            a a2 = new a(69);
                            n3 = n4;
                            if (n4 == 12) {
                                n3 += this.techFlags[36];
                                n3 += this.techFlags[40];
                                n3 += this.techFlags[43];
                            }
                            this.var_java_lang_String_c = a2.a(n3);
                            this.var_java_lang_String_a = a2.a(16);
                            if (this.var_boolean_d && this.countBuildings(0, n4, true) == 1) {
                                this.startMissionBriefing(0, 70, n4);
                            }
                        }
                        switch (n4) {
                            case 9: {
                                int[] nArray = this.playerUnitHeaders[i];
                                nArray[3] = nArray[3] + 5;
                                if (i != 0) break;
                                this.var_boolean_l = true;
                                break;
                            }
                            case 11: {
                                if (i == 0) {
                                    this.techFlags[0] = 1;
                                    this.var_boolean_l = true;
                                }
                                int[] nArray = this.playerUnitHeaders[i];
                                nArray[3] = nArray[3] + 5;
                                break;
                            }
                            case 10: {
                                if (i != 0) break;
                                this.techFlags[2] = 1;
                                break;
                            }
                            case 7: {
                                if (i != 0) break;
                                this.techFlags[4] = 1;
                                break;
                            }
                            case 2: {
                                if (i != 0) break;
                                this.techFlags[7] = 1;
                                break;
                            }
                            case 8: {
                                if (i != 0) break;
                                if (this.playerUnitHeaders[0][0] >= 2) {
                                    this.techFlags[6] = 1;
                                    break;
                                }
                                this.techFlags[5] = 1;
                                break;
                            }
                            case 3: {
                                if (i != 0) break;
                                this.techFlags[9] = 1;
                                break;
                            }
                            case 5: {
                                int[] nArray = this.playerUnitHeaders[i];
                                nArray[56] = nArray[56] + (this.playerUnitHeaders[i][56] >> 1);
                                break;
                            }
                            case 12: {
                                int[] nArray = this.playerUnitHeaders[i];
                                nArray[55] = nArray[55] + (this.playerUnitHeaders[i][45] + this.playerUnitHeaders[i][46]);
                                int n6 = this.playerUnitHeaders[i][48] << 2;
                                this.projectileTable[i][n6 + 0] = (short)n;
                                this.projectileTable[i][n6 + 1] = 1000;
                                this.projectileTable[i][n6 + 3] = 0;
                                int[] nArray2 = this.playerUnitHeaders[i];
                                nArray2[48] = nArray2[48] + 1;
                                int[] nArray3 = this.buildingTable[i];
                                int n7 = n + 3;
                                nArray3[n7] = nArray3[n7] & 0xFFFFFF;
                                int[] nArray4 = this.buildingTable[i];
                                int n8 = n + 3;
                                nArray4[n8] = nArray4[n8] | n6 << 24;
                                break;
                            }
                            case 1: {
                                if (this.playerUnitHeaders[i][10] == -1) {
                                    this.playerUnitHeaders[i][10] = this.buildingTable[i][n + 0];
                                    break;
                                }
                                if (this.playerUnitHeaders[i][11] == -1) {
                                    this.playerUnitHeaders[i][11] = this.buildingTable[i][n + 0];
                                }
                                if (i != 0) break;
                                this.techFlags[10 + n4] = 0;
                            }
                        }
                    } else {
                        int[] nArray = this.buildingTable[i];
                        int n9 = n + 2;
                        nArray[n9] = nArray[n9] + 8;
                    }
                } else {
                    n4 = n5 & 0xFF0000;
                    if (n4 != 0) {
                        if ((n5 & 0x20000000) != 0) {
                            boolean bl = false;
                            if ((n5 & 0xFF00) + 2048 >= 65280) {
                                if (i == 0) {
                                    n3 = -1;
                                    switch (this.buildingTable[0][n + 3] & 0xFF) {
                                        case 9: {
                                            int[] nArray = this.playerUnitHeaders[0];
                                            nArray[0] = nArray[0] + 1;
                                            if (this.playerUnitHeaders[0][0] == 1) {
                                                this.techFlags[15] = 1;
                                                this.techFlags[16] = 1;
                                                this.techFlags[17] = 1;
                                                this.techFlags[18] = 1;
                                                this.techFlags[2] = 0;
                                                this.techFlags[3] = 1;
                                                this.convertUnitType(2, 3);
                                            } else if (this.playerUnitHeaders[0][0] == 2) {
                                                this.techFlags[12] = 1;
                                                this.techFlags[14] = 1;
                                                this.techFlags[13] = 1;
                                                this.techFlags[6] = this.techFlags[5];
                                                this.techFlags[5] = 0;
                                                this.convertUnitType(5, 6);
                                            }
                                            this.var_boolean_l = true;
                                            this.startMissionBriefing(0, 62, this.playerUnitHeaders[0][0] - 1);
                                            break;
                                        }
                                        case 0: {
                                            if (this.techFlags[26] == 0) {
                                                n3 = 3;
                                                int[] nArray = this.playerUnitHeaders[0];
                                                nArray[50] = nArray[50] + 5;
                                                break;
                                            }
                                            if (this.techFlags[24] != 0) break;
                                            n3 = 1;
                                            int[] nArray = this.playerUnitHeaders[0];
                                            nArray[50] = nArray[50] + 5;
                                            break;
                                        }
                                        case 1: {
                                            if (this.techFlags[28] == 0) {
                                                n3 = 5;
                                                int[] nArray = this.playerUnitHeaders[0];
                                                nArray[52] = nArray[52] + 3;
                                                break;
                                            }
                                            if (this.techFlags[32] == 0) {
                                                n3 = 9;
                                                int[] nArray = this.playerUnitHeaders[0];
                                                nArray[51] = nArray[51] + 3;
                                                break;
                                            }
                                            if (this.techFlags[42] == 0) {
                                                n3 = 19;
                                                int[] nArray = this.playerUnitHeaders[0];
                                                nArray[52] = nArray[52] + 3;
                                                break;
                                            }
                                            if (this.techFlags[41] != 0) break;
                                            n3 = 18;
                                            int[] nArray = this.playerUnitHeaders[0];
                                            nArray[51] = nArray[51] + 3;
                                            break;
                                        }
                                        case 5: {
                                            n3 = 6;
                                            int[] nArray = this.playerUnitHeaders[0];
                                            nArray[56] = nArray[56] + (this.playerUnitHeaders[0][56] >> 1);
                                            break;
                                        }
                                        case 6: {
                                            if (this.techFlags[27] == 0) {
                                                n3 = 4;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n10 = 13 + j;
                                                    nArray[n10] = nArray[n10] + 1;
                                                }
                                            } else if (this.techFlags[31] == 0) {
                                                n3 = 8;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n11 = 23 + j;
                                                    nArray[n11] = nArray[n11] + 1;
                                                }
                                            } else if (this.techFlags[30] == 0) {
                                                n3 = 7;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n12 = 13 + j;
                                                    nArray[n12] = nArray[n12] + 1;
                                                }
                                            } else if (this.techFlags[25] == 0) {
                                                n3 = 2;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n13 = 23 + j;
                                                    nArray[n13] = nArray[n13] + 1;
                                                }
                                            } else if (this.techFlags[23] == 0) {
                                                n3 = 0;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n14 = 13 + j;
                                                    nArray[n14] = nArray[n14] + 1;
                                                }
                                            } else {
                                                if (this.techFlags[38] != 0) break;
                                                n3 = 15;
                                                this.techFlags[8] = 1;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n15 = 23 + j;
                                                    nArray[n15] = nArray[n15] + 1;
                                                }
                                            }
                                            break;
                                        }
                                        case 4: {
                                            if (this.techFlags[37] == 0) {
                                                n3 = 14;
                                                for (int j = 0; j < 13; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n16 = 33 + j;
                                                    nArray[n16] = nArray[n16] + 1;
                                                }
                                                break;
                                            }
                                            if (this.techFlags[34] == 0) {
                                                n3 = 11;
                                                int[] nArray = this.playerUnitHeaders[0];
                                                nArray[42] = nArray[42] + 1;
                                                int[] nArray5 = this.playerUnitHeaders[0];
                                                nArray5[45] = nArray5[45] + 1;
                                                break;
                                            }
                                            if (this.techFlags[33] == 0) {
                                                n3 = 10;
                                                for (int j = 0; j < 13; ++j) {
                                                    int[] nArray = this.playerUnitHeaders[0];
                                                    int n17 = 33 + j;
                                                    nArray[n17] = nArray[n17] + 1;
                                                }
                                                break;
                                            }
                                            if (this.techFlags[35] != 0) break;
                                            n3 = 12;
                                            int[] nArray = this.playerUnitHeaders[0];
                                            nArray[42] = nArray[42] + 1;
                                            int[] nArray6 = this.playerUnitHeaders[0];
                                            nArray6[45] = nArray6[45] + 1;
                                            break;
                                        }
                                        case 12: {
                                            if (this.techFlags[36] == 0) {
                                                n3 = 13;
                                                this.playerUnitHeaders[0][47] = 0;
                                                this.playerUnitHeaders[0][46] = 2;
                                                this.playerUnitHeaders[0][45] = 15;
                                                this.playerUnitHeaders[0][12] = 25;
                                                this.playerUnitHeaders[1][12] = 25;
                                                break;
                                            }
                                            if (this.techFlags[40] == 0) {
                                                n3 = 17;
                                                this.playerUnitHeaders[0][47] = 3;
                                                this.playerUnitHeaders[0][46] = 3;
                                                this.playerUnitHeaders[0][45] = 20;
                                                this.playerUnitHeaders[0][12] = 36;
                                                this.playerUnitHeaders[1][12] = 36;
                                                break;
                                            }
                                            if (this.techFlags[43] != 0) break;
                                            n3 = 20;
                                            this.playerUnitHeaders[0][47] = 2;
                                            this.playerUnitHeaders[0][46] = 4;
                                            this.playerUnitHeaders[0][45] = 25;
                                            this.playerUnitHeaders[0][12] = 36;
                                            this.playerUnitHeaders[1][12] = 36;
                                        }
                                    }
                                    if (n3 >= 0) {
                                        this.techFlags[23 + n3] = 1;
                                        this.ag = 20;
                                        a a3 = new a(68);
                                        this.var_java_lang_String_c = a3.a(24);
                                        this.var_java_lang_String_a = a3.a(n3);
                                    }
                                    this.requestStateSwitch(8);
                                }
                                int[] nArray = this.buildingTable[i];
                                int n18 = n + 2;
                                nArray[n18] = nArray[n18] & 0xFF;
                            } else {
                                int[] nArray = this.buildingTable[i];
                                int n19 = n + 2;
                                nArray[n19] = nArray[n19] + 2048;
                            }
                        } else {
                            int n20 = this.playerUnitHeaders[i][56];
                            if ((n5 & 0xFF00) + n20 < 65280) {
                                int[] nArray = this.buildingTable[i];
                                int n21 = n + 2;
                                nArray[n21] = nArray[n21] + n20;
                            } else {
                                n3 = 0;
                                switch (this.buildingTable[i][n + 3] & 0xFF) {
                                    case 11: {
                                        n3 = 1;
                                        if ((this.tickCount & 1) == 0) break;
                                        n3 = 0;
                                        break;
                                    }
                                    case 10: {
                                        if (this.playerUnitHeaders[0][0] == 0) {
                                            n3 = 2;
                                            break;
                                        }
                                        n3 = 3;
                                        break;
                                    }
                                    case 7: {
                                        n3 = 4;
                                        break;
                                    }
                                    case 8: {
                                        if (this.playerUnitHeaders[0][0] >= 2) {
                                            n3 = 6;
                                            break;
                                        }
                                        n3 = 5;
                                        break;
                                    }
                                    case 6: {
                                        n3 = 8;
                                        break;
                                    }
                                    case 2: {
                                        n3 = 7;
                                        break;
                                    }
                                    case 3: {
                                        n3 = 9;
                                    }
                                }
                                if (this.canAfford(i, 0, n3)) {
                                    int n22 = this.buildingTable[i][n + 0] >>> 8;
                                    int n23 = this.buildingTable[i][n + 0] & 0xFF;
                                    int n24 = n22 + 1;
                                    int n25 = n23 + 1;
                                    int n26 = 0;
                                    int n27 = 1;
                                    int n28 = 0;
                                    int n29 = 1;
                                    while ((this.mapTiles[n24 + (n25 << 6) & 0xFFF] & 0xFFF) != 0 || ((n24 | n25) & 0xFFFFFFC0) != 0) {
                                        if (++n26 > 8) {
                                            ++n29;
                                            n26 = 0;
                                            n27 = 1;
                                        }
                                        int n30 = (n27 << 1 & 0xF) + 0;
                                        n24 = n22 + this.dirTable[n30] * n29;
                                        n25 = n23 + this.dirTable[n30 + 1] * n29;
                                        n27 = (n27 + 1) % 7;
                                        if (n28++ < 64) continue;
                                        return;
                                    }
                                    n4 -= 65536;
                                    if (i == 0) {
                                        this.ag = 20;
                                        a a4 = new a(66);
                                        this.var_java_lang_String_c = a4.a(n3);
                                        this.var_java_lang_String_a = a4.a(10);
                                    }
                                    if (this.a(i, n3, n24, n25, true)) {
                                        int[] nArray = this.playerUnitHeaders[i];
                                        nArray[49] = nArray[49] - 1;
                                        if (--n3 < 0) {
                                            n3 = 0;
                                        }
                                        int[] nArray7 = this.playerUnitHeaders[i];
                                        int n31 = 66 + n3;
                                        nArray7[n31] = nArray7[n31] - 1;
                                        if (n4 == 0) {
                                            int[] nArray8 = this.buildingTable[i];
                                            int n32 = n + 2;
                                            nArray8[n32] = nArray8[n32] & 0xFF0000FF;
                                        } else {
                                            int[] nArray9 = this.buildingTable[i];
                                            int n33 = n + 2;
                                            nArray9[n33] = nArray9[n33] & 0xFF0000FF;
                                            int[] nArray10 = this.buildingTable[i];
                                            int n34 = n + 2;
                                            nArray10[n34] = nArray10[n34] | n4;
                                        }
                                    } else {
                                        int[] nArray = this.buildingTable[i];
                                        int n35 = n + 2;
                                        nArray[n35] = nArray[n35] & 0xFF0000FF;
                                        int[] nArray11 = this.buildingTable[i];
                                        int n36 = n + 2;
                                        nArray11[n36] = nArray11[n36] & 0xFF00FFFF;
                                    }
                                    if (i == 0) {
                                        this.requestStateSwitch(8);
                                    }
                                }
                            }
                        }
                    }
                }
                ++n2;
                n += 4;
            }
        }
    }

    final void renderWorld(Graphics graphics) {
        int n;
        int n2;
        int n3;
        int n4 = 0xFF0000;
        if (this.selectionMode == 1) {
            n4 = 65280;
        }
        graphics.setColor(0);
        graphics.fillRect(0, 0, this.screenW, this.screenH);
        int n5 = this.viewTileRows;
        int n6 = this.cameraPxX + (this.cameraPxY << 1);
        int n7 = (this.cameraPxY << 1) - this.cameraPxX;
        int n8 = -(n6 & 0x1F) + (n7 & 0x1F);
        int n9 = (n6 & 0x1F) + (n7 & 0x1F) >> 1;
        int n10 = n8 - 32;
        int n11 = 3 - n9;
        int n12 = 64;
        int n13 = n10;
        int n14 = n11;
        int n15 = n6 >>= 5;
        int n16 = n7 >>= 5;
        this.cursorScreenPxX = -64;
        this.cursorScreenPxY = -64;
        while (n5-- > 0) {
            n3 = n10 - n12;
            n2 = this.viewTileCols;
            while (n2-- > 0) {
                if (((n6 | n7) & 0xFFFFFFC0) == 0) {
                    n = n6 + (n7 << 6);
                    // 移植增强：拾取鼠标所指格（渲染遍历即投影）+ 记录高亮格屏幕位置
                    if (n == this.mouseHighlightTile) {
                        this.mouseHiX = n3;
                        this.mouseHiY = n11;
                    }
                    if (this.mousePickPending && this.mouseInsideTile(this.mousePickX, this.mousePickY, n3, n11)) {
                        this.mousePickTile = n;
                        ++this.mousePickSeq;
                        this.mousePickPending = false;
                        if (System.getProperty("aoe.debug") != null) {
                            System.out.println("[pick] tile=" + n + " at " + n3 + "," + n11);
                        }
                    }
                    if (n == this.cursorTileIdx) {
                        this.cursorScreenPxX = n3;
                        this.cursorScreenPxY = n11;
                    }
                    if ((this.mapTiles[n] & 0xFFF) == 768) {
                        this.drawTileSprite(graphics, 0, n3, n11, 0);
                    } else {
                        // reveal：掩掉 0xF000 里的雾位(0x8000)→雾下地形按本体绘制
                        switch (this.mapTiles[n] & (DEV_REVEAL ? 0x7000 : 0xF000)) {
                            case 0: 
                            case 4096: {
                                this.drawTileSprite(graphics, 4, n3, n11, 0);
                                break;
                            }
                            case 16384: {
                                this.drawTileSprite(graphics, 5, n3, n11, 0);
                            }
                        }
                    }
                } else {
                    this.drawTileSprite(graphics, 0, n3, n11, 0);
                }
                n3 += 64;
                ++n6;
                --n7;
            }
            if (n12 == 0) {
                n12 ^= 0x20;
                n6 -= this.viewTileCols;
                n7 += this.viewTileCols + 1;
            } else {
                n12 = 0;
                n6 -= this.viewTileCols - 1;
                n7 += this.viewTileCols;
            }
            n11 += 16;
        }
        if (this.cursorTileIdx >= 0 && (this.selectionMode != 1 || this.mapTiles[this.cursorTileIdx] >= 0)) {
            graphics.setColor(n4);
            graphics.drawLine(this.cursorScreenPxX - 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY - 16);
            graphics.drawLine(this.cursorScreenPxX + 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY - 16);
            ++this.cursorScreenPxX;
            graphics.drawLine(this.cursorScreenPxX - 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY - 16);
            graphics.drawLine(this.cursorScreenPxX + 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY - 16);
            --this.cursorScreenPxX;
        }
        if (this.U > 0) {
            this.m(graphics);
        }
        n10 = n13;
        n11 = n14;
        n6 = n15;
        n7 = n16;
        n12 = 32;
        n5 = this.viewTileRows;
        while (n5-- > 0) {
            n3 = n10 - n12;
            n2 = this.viewTileCols;
            while (n2-- > 0) {
                if (((n6 | n7) & 0xFFFFFFC0) == 0) {
                    n = n6 + (n7 << 6);
                    short s = this.mapTiles[n];
                    int n17 = s & 0x300;
                    // reveal：雾格(short 负值)也走正向精灵绘制（雾编码占位 0x83xx/0x85xx/
                    // 0x82xx 的低 12 位与明格同构，资源/建筑/单元三 case 直接复用）
                    if (s > 0 || DEV_REVEAL) {
                        switch (n17) {
                            case 768: {
                                if ((this.mapTiles[n] & 3) == 0) break;
                                this.a(graphics, n3, n11, n);
                                break;
                            }
                            case 256: {
                                this.b(graphics, n3, n11, n);
                                break;
                            }
                            case 512: {
                                this.c(graphics, n3, n11, n);
                            }
                        }
                    } else {
                        switch (n17) {
                            case 512: {
                                int n18 = (s & 0xC00) >> 10;
                                int n19 = s & 0xFF;
                                if ((this.playerUnitSlots[n18][(n19 << 3) + 7] & 0xFF) != 1) break;
                                this.void_d(n18, n19);
                                this.c(graphics, n3, n11, n);
                            }
                        }
                    }
                    if (n == this.cursorTileIdx && (this.selectionMode != 1 || this.mapTiles[this.cursorTileIdx] >= 0)) {
                        graphics.setColor(n4);
                        graphics.drawLine(this.cursorScreenPxX - 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY + 16);
                        graphics.drawLine(this.cursorScreenPxX + 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY + 16);
                        ++this.cursorScreenPxX;
                        graphics.drawLine(this.cursorScreenPxX - 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY + 16);
                        graphics.drawLine(this.cursorScreenPxX + 32, this.cursorScreenPxY, this.cursorScreenPxX, this.cursorScreenPxY + 16);
                        --this.cursorScreenPxX;
                    }
                }
                n3 += 64;
                ++n6;
                --n7;
            }
            if (n12 == 0) {
                n12 ^= 0x20;
                n6 -= this.viewTileCols;
                n7 += this.viewTileCols + 1;
            } else {
                n12 = 0;
                n6 -= this.viewTileCols - 1;
                n7 += this.viewTileCols;
            }
            n11 += 16;
        }
    }

    final void a(Graphics graphics, int n, int n2, int n3, int n4, int n5) {
        boolean bl = false;
        int n6 = 6;
        switch (n) {
            case 2: {
                n6 = 7;
                break;
            }
            case 3: 
            case 4: {
                n6 = 8;
            }
        }
        switch (n4) {
            case 0: {
                this.a(graphics, n6, n2, n3, 0, 0, 20, 2, n5, 0);
                return;
            }
            case 1: {
                this.a(graphics, n6, n2, n3, 0, 2, 20, 14, n5, 0);
                return;
            }
            case 2: {
                this.a(graphics, n6, n2, n3, 0, 16, 20, 18, n5, 0);
                return;
            }
            case 3: {
                this.a(graphics, n6, n2, n3, 0, 34, 20, 4, n5, 0);
            }
        }
    }

    final void b(Graphics graphics, int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = (n3 - n) * 20;
        int n8 = (n4 - n2) * 20;
        int n9 = (n7 * (n5 &= 0xF) >> 8) + n;
        int n10 = (n8 * n5 - (AgeOfEmpires.b.c((n5 << 6) + 64) >> 4) >> 8) + n2;
        if (--n5 < 0) {
            n5 = 0;
        }
        int n11 = (n7 * n5 >> 8) + n;
        int n12 = (n8 * n5 - (AgeOfEmpires.b.c((n5 << 6) + 64) >> 4) >> 8) + n2;
        if (n9 == n11 && n10 == n12) {
            return;
        }
        switch (n6) {
            case 0: {
                graphics.setColor(0xFFFF00);
                graphics.drawLine(n9, n10, n11, n12);
                graphics.fillRect(--n9, --n10, 3, 3);
                return;
            }
            case 1: {
                graphics.setColor(0xFFFFFF);
                graphics.drawLine(n9, n10, n11, n12);
                return;
            }
        }
        graphics.setColor(0xC0C0C0);
        graphics.drawLine(n9, n10, n11, n12);
        graphics.fillRect(--n9, --n10, 3, 3);
    }

    final void c(Graphics graphics, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        short s = this.mapTiles[n3];
        int n8 = (s & 0xFF) << 3;
        int n9 = (s & 0xC00) >> 10;
        int n10 = this.playerUnitSlots[n9][n8 + 3] & 0xFF;
        int n11 = n9;
        int n12 = this.playerUnitSlots[n9][n8 + 7] & 0xF;
        if (n9 != 0 && (s & 0x4000) != 0 && n12 != 1) {
            return;
        }
        int n13 = 0;
        int n14 = 0;
        int n15 = this.playerUnitSlots[n9][n8 + 7] >>> 8 & 0xFF;
        switch (n12) {
            case 0: 
            case 3: {
                n14 = (this.playerUnitSlots[n9][n8 + 6] & 0xFF00) >> 8;
                break;
            }
            case 4: {
                n14 = -AgeOfEmpires.b.d(AgeOfEmpires.b.int_b((n15 & 7) << 7) >> 14);
                n13 = 1;
                break;
            }
            case 2: {
                n14 = -AgeOfEmpires.b.d(AgeOfEmpires.b.int_b((n15 & 7) << 7) >> 14);
                n13 = 2;
                break;
            }
            case 1: {
                if (n10 == 4 || n10 == 8) {
                    n13 = 3;
                    break;
                }
                n14 = -AgeOfEmpires.b.d(AgeOfEmpires.b.int_b((n15 & 7) << 7) >> 14);
                n13 = 4;
            }
        }
        int n16 = ((this.playerUnitSlots[n9][n8 + 6] & 0xFF) << 1) + 80;
        int n17 = 0;
        if (n14 != 0) {
            n17 = this.tickCount & 1;
        }
        int n18 = n14 * this.dirTable[n16++];
        n14 *= this.dirTable[n16];
        int n19 = 0;
        if ((this.playerUnitSlots[n9][n8 + 4] & 0x4000) != 0) {
            n19 = 2;
        }
        if ((this.playerUnitSlots[n9][n8 + 4] & 0x2000) != 0) {
            n17 += 2;
        }
        if ((this.playerUnitSlots[n9][n8 + 4] & 0x1000) != 0) {
            n11 = 3;
            short[] sArray = this.playerUnitSlots[n9];
            int n20 = n8 + 4;
            sArray[n20] = (short)(sArray[n20] & 0xEFFF);
        }
        switch (n13) {
            case 0: {
                this.a(graphics, n10, n + n18, n2 + n14, n17, n19, n11);
                break;
            }
            case 1: 
            case 2: 
            case 4: {
                if (n17 == 0) {
                    this.a(graphics, n10, n + n18, n2 + n14, n17, n19, n11);
                }
                if ((n7 = n15 + 2 & 7) < 4) {
                    n6 = 0;
                    n5 = 12 * this.dirTable[n16--];
                    n6 = 12 * this.dirTable[n16];
                    this.a(graphics, n13, n - n6 - 8, n2 - n5 - 8, n7, n19);
                }
                if (n17 == 0) break;
                this.a(graphics, n10, n + n18, n2 + n14, n17, n19, n11);
                break;
            }
            case 3: {
                if (n17 == 0) {
                    this.a(graphics, n10, n + n18, n2 + n14, n17, n19, n11);
                }
                n7 = this.playerUnitSlots[n9][n8 + 5] >>> 8;
                n6 = this.playerUnitSlots[n9][n8 + 5] & 0xFF;
                n5 = ((n7 -= this.playerUnitSlots[n9][n8 + 0] >>> 8) - (n6 -= this.playerUnitSlots[n9][n8 + 0] & 0xFF)) * 26;
                n4 = (n7 + n6) * 12;
                if (n10 == 4) {
                    this.b(graphics, n, n2 - 4, n + n5, n2 + n4, n15, 1);
                } else {
                    this.b(graphics, n, n2 - 4, n + n5, n2 + n4, n15, 0);
                }
                if (n17 == 0) break;
                this.a(graphics, n10, n + n18, n2 + n14, n17, n19, n11);
            }
        }
        n7 = this.playerUnitSlots[n9][n8 + 4];
        n += n18;
        n2 += n14 - 5;
        n2 -= (this.var_byte_arr_d[n10] >> 1) + 5;
        n6 = 0;
        switch (this.playerUnitSlots[n9][n8 + 7] & 0xF) {
            case 0: {
                n6 = -1;
                break;
            }
            case 1: {
                n6 = 16;
                break;
            }
            case 4: {
                n6 = 32;
            }
        }
        if (n6 >= 0) {
            this.a(graphics, 9, n - 20, n2 - 9, 0, n6, 16, 16, 0, 0);
            n += 9;
        }
        n5 = n7 & 0xFF;
        n4 = 0xFFFFFF;
        if (n5 < 255 || (n7 & 0x8000) != 0) {
            n -= 9;
            if ((n7 & 0x8000) != 0) {
                graphics.setColor(0);
                graphics.fillRect(n, n2, 18, 4);
                if (this.selectedSlot == -1) {
                    n4 = 0;
                }
                graphics.setColor(n4);
                graphics.fillRect(n - 1, n2 - 1, 18, 4);
                n4 = this.selectedType == -1 ? 8703 : 65280;
            } else {
                graphics.setColor(0);
                graphics.fillRect(n - 1, n2 - 1, 18, 4);
                n4 = 65280;
            }
            int n21 = (n5 >> 4) + 1;
            graphics.setColor(n4);
            graphics.fillRect(n, n2, n21, 2);
            graphics.setColor(0xFF0000);
            graphics.fillRect(n + n21, n2, 16 - n21, 2);
        }
    }

    final void a(Graphics graphics, int n, int n2, int n3, int n4, int n5, int n6) {
        int n7 = n;
        byte by = this.var_byte_arr_d[n];
        n7 <<= 3;
        n7 += 10 + (n4 << 1);
        byte by2 = this.var_byte_arr_d[n7++];
        byte by3 = this.var_byte_arr_d[n7];
        this.a(graphics, 22 + n, n2, n3, by2, 0, by3, by, n5, n6);
    }

    final void b(Graphics graphics, int n, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        short s = this.mapTiles[n3];
        int n8 = (s & 0xFF) << 2;
        int n9 = (s & 0xC00) >> 10;
        int n10 = this.buildingTable[n9][n8 + 2];
        int n11 = this.buildingTable[n9][n8 + 3];
        int n12 = this.playerUnitHeaders[n9][0];
        int n13 = n9;
        if (n11 >= 12) {
            n7 = 0;
            n7 = 0 + this.techFlags[36];
            n7 += this.techFlags[40];
            n6 = this.var_byte_arr_h[((n11 & 0xFF) << 2) + (n7 += this.techFlags[43])] + 33;
        } else {
            n6 = this.var_byte_arr_h[((n11 & 0xFF) << 2) + n12] + 33;
        }
        if ((n10 & 0x40000000) != 0) {
            n6 = 32;
            n7 = n10 & 0xFFFF;
            this.buildingTable[n9][n8 + 2] = n7 >= 255 ? 255 : n10 & 0xFFFFFF00 | n7;
        }
        if ((n10 & 0x10000000) != 0) {
            int[] nArray = this.buildingTable[n9];
            int n14 = n8 + 2;
            nArray[n14] = nArray[n14] & 0xEFFFFFFF;
            n13 = 3;
        }
        if ((n11 & 0xFFFFFF00) != 0 && n6 != 32) {
            n7 = n11 >>> 24;
            n5 = this.projectileTable[n9][n7 + 2];
            n4 = n5 >>> 8;
            n5 &= 0xFF;
            int n15 = ((n4 -= this.buildingTable[n9][n8 + 0] >>> 8) - (n5 -= this.buildingTable[n9][n8 + 0] & 0xFF)) * 26;
            int n16 = (n4 + n5) * 12;
            int n17 = this.playerUnitHeaders[n9][47];
            if (n16 >= -10) {
                this.drawTileSprite(graphics, n6, n, n2, n13);
                this.b(graphics, n, n2 - 32, n + n15, n2 + n16, this.projectileTable[n9][n7 + 3], n17);
            } else {
                this.b(graphics, n, n2 - 32, n + n15, n2 + n16, this.projectileTable[n9][n7 + 3], n17);
                this.drawTileSprite(graphics, n6, n, n2, n13);
            }
        } else {
            if ((this.mapTiles[n3] & 0x4000) != 0) {
                n13 = 2;
            }
            this.drawTileSprite(graphics, n6, n, n2, n13);
        }
        if ((n10 & 0xFFFF0000) != 0 || (n10 & Integer.MIN_VALUE) != 0) {
            n7 = n6 << 1;
            n2 -= this.var_byte_arr_b[n7 + 1];
            graphics.setColor(0);
            graphics.fillRect(n -= 16, n2 -= 5, 34, 4);
            graphics.setColor(0xDCDCDC);
            graphics.fillRect(n - 1, n2 - 1, 34, 4);
            if ((n10 & 0xFF0000) == 0) {
                n5 = (n10 & 0xFF) + 1;
                graphics.setColor(65280);
            } else {
                n5 = ((n10 & 0xFF00) >> 8) + 1;
                if ((n10 & 0x20000000) == 0) {
                    n4 = n10 >> 16 & 0xFF;
                    String string = "" + n4;
                    graphics.drawString(string, n - this.a(string) - 4, n2 - (this.ah >> 1), 20);
                    graphics.setColor(255);
                } else {
                    graphics.setColor(0xFF00FF);
                }
            }
            n4 = n5 >> 3;
            graphics.fillRect(n, n2, n4, 2);
            if ((n10 & 0xFF0000) == 0) {
                graphics.setColor(0xFF0000);
            } else {
                graphics.setColor(0x8080FF);
            }
            graphics.fillRect(n + n4, n2, 32 - n4, 2);
        }
    }

    final void a(Graphics graphics, int n, int n2, int n3) {
        int n4 = 0 + (this.mapTiles[n3] & 3);
        if ((this.mapTiles[n3] & 0x4000) != 0) {
            this.drawTileSprite(graphics, n4, n, n2, 2);
        } else {
            this.drawTileSprite(graphics, n4, n, n2, 0);
        }
        if ((this.mapTiles[n3] & 0x80) != 0) {
            graphics.setColor(0);
            graphics.fillRect(n -= 8, n2 -= 4, 18, 4);
            graphics.setColor(0xDCDCDC);
            graphics.fillRect(n - 1, n2 - 1, 18, 4);
            int n5 = ((this.mapTiles[n3] & 0x7C) >> 2) + 1 >> 1;
            graphics.setColor(65280);
            graphics.fillRect(n, n2, n5, 2);
            graphics.setColor(0xFF0000);
            graphics.fillRect(n + n5, n2, 16 - n5, 2);
        }
    }

    public final boolean a(int n, int n2, int n3, int n4, boolean bl) {
        short s;
        int n5 = (n3 &= 0x3F) + ((n4 &= 0x3F) << 6);
        if ((this.mapTiles[n5] & 0xFFF) != 0) {
            return false;
        }
        if (bl) {
            this.payCost(n, 0, n2);
        }
        this.e(n, 2, n2);
        int n6 = this.playerUnitHeaders[n][2];
        if (n6 >= 26) {
            return false;
        }
        int n7 = n5;
        this.mapTiles[n7] = (short)(this.mapTiles[n7] | (short)(0x200 | n6 | n << 10 & 0xC00));
        this.playerUnitSlots[n][(n6 <<= 3) + 0] = s = (short)(n3 << 8 | n4);
        this.playerUnitSlots[n][n6 + 2] = s;
        this.playerUnitSlots[n][n6 + 1] = s;
        this.playerUnitSlots[n][n6 + 3] = (short)n2;
        this.playerUnitSlots[n][n6 + 4] = 255;
        this.playerUnitSlots[n][n6 + 6] = 0;
        this.playerUnitSlots[n][n6 + 7] = 0;
        if (!(this.selectedType != n2 && this.selectedType != -1 || this.selectionPlayer != 0 || this.selectedSlot != -1 || n != 0 || this.selectedType == -1 && n2 < 2)) {
            short[] sArray = this.playerUnitSlots[n];
            int n8 = n6 + 4;
            sArray[n8] = (short)(sArray[n8] | 0x8000);
        }
        if (n == 0) {
            this.void_d(0, this.playerUnitHeaders[n][2]);
        }
        int[] nArray = this.playerUnitHeaders[n];
        nArray[55] = nArray[55] + (this.playerUnitHeaders[n][23 + n2] + this.playerUnitHeaders[n][13 + n2]);
        int[] nArray2 = this.playerUnitHeaders[n];
        nArray2[2] = nArray2[2] + 1;
        int[] nArray3 = this.playerUnitHeaders[n];
        nArray3[86] = nArray3[86] + 1;
        if (--n2 < 0) {
            n2 = 0;
        }
        int[] nArray4 = this.playerUnitHeaders[n];
        int n9 = 57 + n2;
        nArray4[n9] = nArray4[n9] + 1;
        return true;
    }

    // queueUnitTraining(p, selectedTrainProduct)：向 p 玩家的生产建筑排一个 selectedTrainProduct 产品。selectedTrainProduct→可排建筑
    // （switch 返回值 n3）：0村民→House(11) 2/3民兵剑士→Barracks(10, 按时代默认)
    // 5/6骑兵→Stable(8) 4弓兵→Range(7)。占用 pop 当量（headers[49] 队列长）；
    // 付款在出兵时；宏路径排队前另检 canAfford（train case）。public 化=player-ai
    // 分支的 RuleBasedAi 需要跨包调用。
    public final int queueUnitTraining(int n, int n2) {
        int n3 = 0;
        switch (n2) {
            case 0: {
                n3 = 11;
                break;
            }
            case 2: 
            case 3: {
                n3 = 10;
                break;
            }
            case 5: 
            case 6: {
                n3 = 8;
                break;
            }
            case 4: {
                n3 = 7;
                break;
            }
            case 7: {
                n3 = 2;
                break;
            }
            case 8: {
                n3 = 6;
                break;
            }
            case 9: {
                n3 = 3;
            }
        }
        int n4 = 0;
        for (int i = 0; i < this.playerUnitHeaders[n][4]; ++i) {
            if ((this.buildingTable[n][n4 + 3] & 0xFF) == n3) {
                if ((this.buildingTable[n][n4 + 2] & 0x40000000) != 0) {
                    return -1;
                }
                int[] nArray = this.buildingTable[n];
                int n5 = n4 + 2;
                nArray[n5] = nArray[n5] + 65536;
                int[] nArray2 = this.playerUnitHeaders[n];
                nArray2[49] = nArray2[49] + 1;
                int n6 = n2 - 1;
                if (n6 < 0) {
                    n6 = 0;
                }
                int[] nArray3 = this.playerUnitHeaders[n];
                int n7 = 66 + n6;
                nArray3[n7] = nArray3[n7] + 1;
                return n4;
            }
            n4 += 4;
        }
        return -1;
    }

    final void convertUnitType(int n, int n2) {
        int n3 = n2 - 1;
        int n4 = n - 1;
        if (n3 < 0) {
            n3 = 0;
        }
        if (n4 < 0) {
            n4 = 0;
        }
        for (int i = 0; i < 2; ++i) {
            int n5 = 0;
            int n6 = this.playerUnitHeaders[i][2];
            for (int j = 0; j < n6; ++j) {
                if ((this.playerUnitSlots[i][n5 + 3] & 0xFF) == n) {
                    short[] sArray = this.playerUnitSlots[i];
                    int n7 = n5 + 3;
                    sArray[n7] = (short)(sArray[n7] & 0xFF00);
                    short[] sArray2 = this.playerUnitSlots[i];
                    int n8 = n5 + 3;
                    sArray2[n8] = (short)(sArray2[n8] | n2);
                }
                n5 += 8;
            }
            this.playerUnitHeaders[i][57 + n3] = this.playerUnitHeaders[i][57 + n4];
            this.playerUnitHeaders[i][57 + n4] = 0;
            this.playerUnitHeaders[i][66 + n3] = this.playerUnitHeaders[i][66 + n4];
            this.playerUnitHeaders[i][66 + n4] = 0;
        }
    }

    final void tickMoveTimer(int n, int n2) {
        short s = this.playerUnitSlots[n][n2 + 6];
        if ((s & 0xF00) != 0) {
            int n3 = 0;
            switch (this.playerUnitSlots[n][n2 + 3] & 0xFF) {
                case 0: 
                case 1: {
                    n3 = 512;
                    break;
                }
                case 4: {
                    n3 = 768;
                    break;
                }
                case 2: {
                    n3 = 768;
                    break;
                }
                case 3: {
                    n3 = 1024;
                    break;
                }
                case 7: {
                    n3 = 256;
                    break;
                }
                case 8: {
                    n3 = 256;
                    break;
                }
                case 5: {
                    n3 = 1024;
                    break;
                }
                case 6: {
                    n3 = 1024;
                    break;
                }
                case 9: {
                    n3 = 1536;
                    break;
                }
                default: {
                    return;
                }
            }
            this.playerUnitSlots[n][n2 + 6] = (short)(s - n3 & 0xF00 | s & 0xFF);
            if (s < this.playerUnitSlots[n][n2 + 6]) {
                short[] sArray = this.playerUnitSlots[n];
                int n4 = n2 + 6;
                sArray[n4] = (short)(sArray[n4] & 0xFF);
                return;
            }
        } else if (this.playerUnitSlots[n][n2 + 0] != this.playerUnitSlots[n][n2 + 2]) {
            n2 >>= 3;
            if (n == 0) {
                this.l(0, n2);
                if (this.boolean_b(0, n2)) {
                    this.void_a(0, n2);
                }
                this.void_d(0, n2);
                return;
            }
            if (this.boolean_b(n, n2)) {
                this.void_a(n, n2);
            }
        }
    }

    public final void g(int n, int n2) {
        // 战斗日志(调试基建,第11轮:战斗在日志里完全是黑箱,只能靠 state 前后对比)。
        // g = 单位死亡/移除唯一入口:含阵营/类型/位置/剩余数,一次战场推演可完整回放。
        if (System.getProperty("aoe.debug") != null) {
            short sPos = this.playerUnitSlots[n][n2 << 3];
            System.out.println("[combat] p" + n + " type" + (this.playerUnitSlots[n][(n2 << 3) + 3] & 0xFF)
                + " died at (" + (sPos >>> 8) + "," + (sPos & 0xFF) + ") ar=" + this.tickCount
                + " remaining=" + (this.playerUnitHeaders[n][2] - 1));
        }
        this.l(0, n2);
        int[] nArray = this.playerUnitHeaders[n];
        nArray[2] = nArray[2] - 1;
        int n3 = this.playerUnitHeaders[n][2] << 3;
        int n4 = n2 << 3;
        if (n == 0 && n3 == 0 && this.playerUnitHeaders[0][4] == 0) {
            this.startMissionBriefing(0, 98, 1);
        }
        short s = this.playerUnitSlots[n][n4 + 0];
        int n5 = (s >>> 8 & 0x3F) + ((s & 0x3F) << 6);
        this.mapTiles[n5] = 16384;
        int n6 = this.playerUnitSlots[n][n4 + 3] & 0xFF;
        this.e(n, 0, n6);
        int[] nArray2 = this.playerUnitHeaders[n];
        nArray2[55] = nArray2[55] - (this.playerUnitHeaders[n][23 + n6] + this.playerUnitHeaders[n][13 + n6]);
        if (--n6 < 0) {
            n6 = 0;
        }
        int[] nArray3 = this.playerUnitHeaders[n];
        int n7 = 57 + n6;
        nArray3[n7] = nArray3[n7] - 1;
        if (n4 < n3) {
            s = this.playerUnitSlots[n][n3 + 0];
            int n8 = n5 = (s >>> 8 & 0x3F) + ((s & 0x3F) << 6);
            this.mapTiles[n8] = (short)(this.mapTiles[n8] & 0xFFFFFF00);
            int n9 = n5;
            this.mapTiles[n9] = (short)(this.mapTiles[n9] | n2);
            for (int i = 0; i < 8; ++i) {
                this.playerUnitSlots[n][n4++] = this.playerUnitSlots[n][n3];
                this.playerUnitSlots[n][n3++] = 0;
            }
        }
        int[] nArray4 = this.playerUnitHeaders[n];
        nArray4[87] = nArray4[87] + 1;
        if (n == 0) {
            this.var_boolean_l = true;
        }
    }

    final void g() {
        int n = this.tickCount & 8;
        for (int i = 0; i < 2; ++i) {
            int n2 = 0;
            int n3 = 0;
            while (n3 < this.playerUnitHeaders[i][2]) {
                int n4 = this.playerUnitSlots[i][n2 + 7] & 0xF;
                switch (n4) {
                    case 0: {
                        if (n != 0 && this.playerUnitSlots[i][n2 + 0] == this.playerUnitSlots[i][n2 + 2] && (this.playerUnitSlots[i][n2 + 4] & 0xFF) < 255) {
                            short[] sArray = this.playerUnitSlots[i];
                            int n5 = n2 + 4;
                            sArray[n5] = (short)(sArray[n5] + 1);
                        }
                    }
                    case 3: {
                        this.tickMoveTimer(i, n2);
                        break;
                    }
                    case 2: {
                        int n6;
                        int n7;
                        int n8;
                        int n9;
                        int n10 = this.playerUnitSlots[i][n2 + 7] >>> 8;
                        if (--n10 == 0) {
                            this.playerUnitSlots[i][n2 + 2] = (short)this.int_a(i, (int)this.playerUnitSlots[i][n2 + 0], (int)this.playerUnitSlots[i][n2 + 7]);
                            short[] sArray = this.playerUnitSlots[i];
                            int n11 = n2 + 7;
                            sArray[n11] = (short)(sArray[n11] & 0xF0);
                            short[] sArray2 = this.playerUnitSlots[i];
                            int n12 = n2 + 7;
                            sArray2[n12] = (short)(sArray2[n12] | 3);
                            n9 = this.playerUnitSlots[i][n2 + 5] >>> 8 | (this.playerUnitSlots[i][n2 + 5] & 0xFF) << 6;
                            n8 = (this.mapTiles[n9] & 0x7C) >> 2;
                            if (--n8 == 0 || (this.mapTiles[n9] & 0x300) != 768) {
                                if ((this.mapTiles[n9] & 0x300) == 768) {
                                    int n13 = n9;
                                    this.mapTiles[n13] = (short)(this.mapTiles[n13] & 0xF000);
                                }
                                for (n7 = 0; n7 < 8; ++n7) {
                                    n6 = this.playerUnitSlots[i][n2 + 5] >>> 8;
                                    int n14 = this.playerUnitSlots[i][n2 + 5] & 0xFF;
                                    if ((((n6 += this.dirTable[n7 << 1]) | (n14 += this.dirTable[(n7 << 1) + 1])) & 0xFFFFFFC0) != 0 || (this.mapTiles[n9 = n6 + (n14 << 6)] & 0x300) != 768 || (this.playerUnitSlots[i][n2 + 7] & 0xF0) >> 4 != (this.mapTiles[n9] & 3)) continue;
                                    this.playerUnitSlots[i][n2 + 5] = (short)(n6 << 8 | n14);
                                    n7 = 10;
                                }
                                break;
                            }
                            int n15 = n9;
                            this.mapTiles[n15] = (short)(this.mapTiles[n15] & 0xFF83);
                            int n16 = n9;
                            this.mapTiles[n16] = (short)(this.mapTiles[n16] | n8 << 2);
                            int n17 = n9;
                            this.mapTiles[n17] = (short)(this.mapTiles[n17] | 0x80);
                            break;
                        }
                        short[] sArray = this.playerUnitSlots[i];
                        int n18 = n2 + 7;
                        sArray[n18] = (short)(sArray[n18] & 0xFF);
                        short[] sArray3 = this.playerUnitSlots[i];
                        int n19 = n2 + 7;
                        sArray3[n19] = (short)(sArray3[n19] | (short)(n10 << 8));
                        break;
                    }
                    case 1: {
                        int n10 = this.playerUnitSlots[i][n2 + 7] & 0x7F00;
                        n10 += 256;
                        int n9 = this.playerUnitSlots[i][n2 + 5] >>> 8;
                        int n8 = (this.mapTiles[n9 += (this.playerUnitSlots[i][n2 + 5] & 0xFF) << 6] & 0xC00) >> 10;
                        if (n8 == i || (this.mapTiles[n9] & 0xFFF) == 0) {
                            this.playerUnitSlots[i][n2 + 7] = 0;
                            break;
                        }
                        int n7 = 0;
                        int n6 = this.playerUnitSlots[i][n2 + 3] & 0xFF;
                        if (n6 == 4 || n6 == 8) {
                            if (n10 >= 3840) {
                                n7 = 1;
                            }
                        } else if (n10 >= 2048) {
                            n7 = 1;
                        }
                        if (n7 != 0) {
                            short[] sArray = this.playerUnitSlots[i];
                            int n20 = n2 + 7;
                            sArray[n20] = (short)(sArray[n20] & 0xFF);
                            this.d(i, n2, n9, n8);
                            break;
                        }
                        short[] sArray = this.playerUnitSlots[i];
                        int n21 = n2 + 7;
                        sArray[n21] = (short)(sArray[n21] & 0xFF);
                        short[] sArray4 = this.playerUnitSlots[i];
                        int n22 = n2 + 7;
                        sArray4[n22] = (short)(sArray4[n22] + (n10 & 0x7F00));
                        break;
                    }
                    case 4: {
                        int n10 = this.playerUnitSlots[i][n2 + 7] & 0x7F00;
                        n10 += 256;
                        this.tickConstruction(i, n2);
                        short[] sArray = this.playerUnitSlots[i];
                        int n23 = n2 + 7;
                        sArray[n23] = (short)(sArray[n23] & 0xFF);
                        short[] sArray5 = this.playerUnitSlots[i];
                        int n24 = n2 + 7;
                        sArray5[n24] = (short)(sArray5[n24] | (short)n10);
                    }
                }
                ++n3;
                n2 += 8;
            }
        }
    }

    final void c(int n, int n2, int n3, int n4) {
        int n5 = 0;
        int n6 = n3 + (n4 << 6);
        switch (this.mapTiles[n6] & 0x300) {
            case 768: {
                if ((this.mapTiles[n6] & 3) == 0 || this.playerUnitSlots[n][n2 + 3] > 1) break;
                n5 = 0x6602 | (this.mapTiles[n6] & 3) << 4;
                this.playerUnitSlots[n][n2 + 7] = (short)n5;
                this.playerUnitSlots[n][n2 + 5] = (short)(n3 << 8 | n4);
                short[] sArray = this.playerUnitSlots[n];
                int n7 = n2 + 3;
                sArray[n7] = (short)(sArray[n7] & 0xFF);
                break;
            }
            case 256: {
                int n8 = (this.mapTiles[n6] & 0xC00) >> 10;
                if (n8 == n) {
                    int n9 = this.playerUnitSlots[n][n2 + 3] & 0xFF;
                    if (n9 > 2) break;
                    if ((this.playerUnitSlots[n][n2 + 7] & 0xF) == 3) {
                        int n10 = ((this.playerUnitSlots[n][n2 + 7] & 0xF0) >> 4) - 1;
                        n5 = 0;
                        this.playerUnitSlots[n][n2 + 7] = 0;
                        int n11 = 256;
                        if (n == 1) {
                            n11 = this.aiGatherMultiplier;
                        }
                        int[] nArray = this.playerUnitHeaders[n];
                        int n12 = 5 + n10;
                        nArray[n12] = nArray[n12] + (this.playerUnitHeaders[n][50 + n10] * n11 >> 8);
                        int[] nArray2 = this.playerUnitHeaders[n];
                        nArray2[90] = nArray2[90] + (n11 >> 8);
                        this.var_boolean_l = true;
                        if (n == 0) {
                            this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n][n2 + 5];
                            short[] sArray = this.playerUnitSlots[n];
                            int n13 = n2 + 3;
                            sArray[n13] = (short)(sArray[n13] & 0xFF);
                            break;
                        }
                        int n14 = this.playerUnitSlots[n][n2 + 5];
                        int n15 = n14 >>> 8;
                        int n16 = n15 + ((n14 &= 0xFF) << 6);
                        int n17 = this.int_b(n);
                        if (n17 == (this.mapTiles[n16] & 0x303)) {
                            this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n][n2 + 5];
                            short[] sArray = this.playerUnitSlots[n];
                            int n18 = n2 + 3;
                            sArray[n18] = (short)(sArray[n18] & 0xFF);
                            break;
                        }
                        this.findNearbyResource((int)this.playerUnitSlots[n][n2 + 0], n17);
                        break;
                    }
                    int n19 = (this.mapTiles[n6] & 0xFF) << 2;
                    if ((this.buildingTable[n][n19 + 2] & 0xFF) >= 255) break;
                    n5 = 4;
                    this.playerUnitSlots[n][n2 + 7] = 4;
                    this.playerUnitSlots[n][n2 + 5] = (short)(n3 << 8 | n4);
                    break;
                }
                n5 = 1;
                this.playerUnitSlots[n][n2 + 7] = 1;
                int n20 = (this.mapTiles[n6] & 0xFF) << 2;
                this.playerUnitSlots[n][n2 + 5] = (short)(n3 << 8 | n4);
                int[] nArray = this.buildingTable[n8];
                int n21 = n20 + 2;
                nArray[n21] = nArray[n21] | Integer.MIN_VALUE;
                break;
            }
            case 512: {
                int n22 = (this.mapTiles[n6] & 0xC00) >> 10;
                if (n22 == n) {
                    n5 = 0;
                    this.playerUnitSlots[n][n2 + 7] = 0;
                    break;
                }
                n5 = 1;
                this.playerUnitSlots[n][n2 + 7] = 1;
                int n23 = (this.mapTiles[n6] & 0xFF) << 3;
                this.playerUnitSlots[n][n2 + 5] = (short)(n3 << 8 | n4);
                int n24 = this.playerUnitSlots[n][n2 + 0] >>> 8;
                int n25 = this.playerUnitSlots[n][n2 + 0] & 0xFF;
                this.playerUnitSlots[n22][n23 + 5] = (short)(n24 << 8 | n25);
                short[] sArray = this.playerUnitSlots[n22];
                int n26 = n23 + 7;
                sArray[n26] = (short)(sArray[n26] & 0xFFF0);
                short[] sArray2 = this.playerUnitSlots[n22];
                int n27 = n23 + 7;
                sArray2[n27] = (short)(sArray2[n27] | 1);
                this.playerUnitSlots[n22][n23 + 6] = (short)((this.playerUnitSlots[n][n2 + 6] & 0xFF) + 4 & 7);
                this.b(n22, n23, n24, n25);
            }
        }
        this.playerUnitSlots[n][n2 + 7] = (short)n5;
    }

    final void d(int n, int n2, int n3, int n4) {
        int n5 = this.playerUnitSlots[n][n2 + 3] & 0xFF;
        int n6 = this.mapTiles[n3] & 0x300;
        switch (n6) {
            case 256: {
                int n7 = (this.mapTiles[n3] & 0xFF) << 2;
                int n8 = this.buildingTable[n4][n7 + 2] & 0xFF;
                int n9 = this.buildingTable[n4][n7 + 3] & 0xFF;
                int n10 = this.playerUnitHeaders[n][13 + n5] << 4;
                if ((n8 -= (n10 /= this.playerUnitHeaders[n4][33 + n9])) > 0) {
                    int[] nArray = this.buildingTable[n4];
                    int n11 = n7 + 2;
                    nArray[n11] = nArray[n11] & 0xFFFFFF00;
                    int[] nArray2 = this.buildingTable[n4];
                    int n12 = n7 + 2;
                    nArray2[n12] = nArray2[n12] | (n8 | Integer.MIN_VALUE | 0x10000000);
                } else {
                    this.onThingDestroyed(n4, this.mapTiles[n3] & 0xFF);
                    this.playerUnitSlots[n][n2 + 7] = 0;
                }
                if (n4 != 0) break;
                this.v();
                return;
            }
            case 512: {
                int n13 = (this.mapTiles[n3] & 0xFF) << 3;
                int n14 = this.playerUnitSlots[n4][n13 + 4] & 0xFF;
                int n15 = this.playerUnitSlots[n4][n13 + 3] & 0xFF;
                int n16 = this.playerUnitHeaders[n][13 + n5] << 4;
                if ((n14 -= (n16 /= this.playerUnitHeaders[n4][23 + n15])) > 0) {
                    short[] sArray = this.playerUnitSlots[n4];
                    int n17 = n13 + 4;
                    sArray[n17] = (short)(sArray[n17] & 0xFF00);
                    short[] sArray2 = this.playerUnitSlots[n4];
                    int n18 = n13 + 4;
                    sArray2[n18] = (short)(sArray2[n18] | (n14 | 0x1000));
                    this.playerUnitSlots[n4][n13 + 2] = this.playerUnitSlots[n][n2 + 0];
                    return;
                }
                this.g(n4, this.mapTiles[n3] & 0xFF);
                this.playerUnitSlots[n][n2 + 7] = 0;
            }
        }
    }

    final void tickConstruction(int n, int n2) {
        int n3 = this.playerUnitSlots[n][n2 + 5] >>> 8;
        int n4 = (this.mapTiles[n3 += (this.playerUnitSlots[n][n2 + 5] & 0xFF) << 6] & 0xFF) << 2;
        int n5 = this.buildingTable[n][n4 + 2] & 0xFF;
        if ((AgeOfEmpires.c.nextRandomInt() & 1) == 0) {
            ++n5;
        }
        if (n5 <= 255) {
            int[] nArray = this.buildingTable[n];
            int n6 = n4 + 2;
            nArray[n6] = nArray[n6] & 0xFFFFFF00;
            int[] nArray2 = this.buildingTable[n];
            int n7 = n4 + 2;
            nArray2[n7] = nArray2[n7] | (n5 | Integer.MIN_VALUE);
            return;
        }
        this.playerUnitSlots[n][n2 + 7] = 0;
    }

    public final int a(int n, int n2, int n3, int n4, int n5, boolean bl) {
        int n6;
        int n7;
        int n8 = this.playerUnitHeaders[n][4];
        if (n8 >= 22) {
            return -1;
        }
        int[] nArray = this.playerUnitHeaders[n];
        nArray[88] = nArray[88] + 1;
        this.e(n, 3, n2);
        int n9 = n7 = (n3 &= 0x3F) + ((n4 &= 0x3F) << 6);
        this.mapTiles[n9] = (short)(this.mapTiles[n9] | (short)(0x100 | n << 10 & 0xC00 | n8 & 0xFF));
        n8 <<= 2;
        if (n == 0 && (n5 & 0x40000000) == 0) {
            this.void_a(n3, n4, 3);
        }
        switch (n2) {
            case 0: {
                this.playerUnitHeaders[n][9] = n3 << 8 | n4;
                break;
            }
            case 9: {
                this.playerUnitHeaders[n][8] = n3 << 8 | n4;
                if ((n5 & 0x40000000) != 0) break;
                int[] nArray2 = this.playerUnitHeaders[n];
                nArray2[3] = nArray2[3] + 5;
                break;
            }
            case 11: {
                if ((n5 & 0x40000000) != 0) break;
                int[] nArray3 = this.playerUnitHeaders[n];
                nArray3[3] = nArray3[3] + 5;
                break;
            }
            case 12: {
                if ((n5 & 0x40000000) != 0) break;
                n6 = this.playerUnitHeaders[n][48] << 2;
                this.projectileTable[n][n6 + 0] = (short)n8;
                this.projectileTable[n][n6 + 1] = 1000;
                this.projectileTable[n][n6 + 2] = (short)((n3 << 8) + n4);
                this.projectileTable[n][n6 + 3] = 0;
                n2 |= n6 << 24;
                int[] nArray4 = this.playerUnitHeaders[n];
                nArray4[48] = nArray4[48] + 1;
            }
        }
        this.buildingTable[n][n8 + 0] = (n3 << 8) + n4;
        this.buildingTable[n][n8 + 1] = n2;
        this.buildingTable[n][n8 + 2] = n5;
        this.buildingTable[n][n8 + 3] = n2;
        n2 &= 0xFF;
        int[] nArray5 = this.playerUnitHeaders[n];
        nArray5[4] = nArray5[4] + 1;
        if (bl) {
            n6 = 0;
            if (n2 == 12) {
                n6 = 0 + this.techFlags[36];
                n6 += this.techFlags[40];
                n6 += this.techFlags[43];
            }
            this.payCost(n, 1, n2 + n6);
        }
        if (n2 != 1 && n2 != 11 && n2 != 12 && n == 0) {
            this.techFlags[10 + n2] = 0;
        }
        return n8;
    }

    // a(n,type,bl)：⚠️ 数的是【建筑表】buildingTable（i<headers[n][4]，stride4），
    // bl=true 再排除施工中(0x40000000)。别拿它数单位——rally 宏曾因此零移动且回显
    // 全是建筑数（r31 事故，修复=devCountUnits）。
    final int countBuildings(int n, int n2, boolean bl) {
        int n3 = 0;
        int n4 = 0;
        int n5 = this.playerUnitHeaders[n][4];
        int n6 = 0;
        while (n6 < n5) {
            if ((this.buildingTable[n][n4 + 3] & 0xFF) == n2) {
                if (bl) {
                    if ((this.buildingTable[n][n4 + 2] & 0x40000000) == 0) {
                        ++n3;
                    }
                } else {
                    ++n3;
                }
            }
            ++n6;
            n4 += 4;
        }
        return n3;
    }

    // i(p, slot)：建筑/单位被摧毁处理器（清 mapTiles 占位、techFlags 置位、pop 回收、
    // 投射物/胜利败北判定）。case 9(TC) 即胜负开关：我方 TC 毁→98,1 败；敌 TC 毁→
    // 98,0 胜（gameMode 32 链且 missionIndex≠0 时跳过，走 missionScript 脚本路径）。
    // r30 源码定案 + r31 实战双验证（敌 0 单位+TC 毁同轮触发）。
    public final void onThingDestroyed(int n, int n2) {
        int n3 = this.playerUnitHeaders[n][4] - 1;
        int[] nArray = this.playerUnitHeaders[n];
        nArray[89] = nArray[89] + 1;
        int n4 = n2 << 2;
        int n5 = n3 << 2;
        int n6 = this.buildingTable[n][n4 + 0];
        int n7 = (n6 >>> 8 & 0x3F) + ((n6 & 0x3F) << 6);
        this.mapTiles[n7] = 0;
        int n8 = this.buildingTable[n][n4 + 3] & 0xFF;
        if (n == 0) {
            this.techFlags[10 + n8] = 1;
        }
        this.e(n, 1, n8);
        switch (n8) {
            case 9: {
                if (n == 0) {
                    this.startMissionBriefing(0, 98, 1);
                    break;
                }
                if (this.gameMode == 32 && this.missionIndex != 0) break;
                this.startMissionBriefing(0, 98, 0);
                break;
            }
            case 11: {
                if ((this.buildingTable[n][n4 + 2] & 0x40000000) == 0) {
                    int[] nArray2 = this.playerUnitHeaders[n];
                    nArray2[3] = nArray2[3] - 5;
                }
                this.var_boolean_l = true;
                break;
            }
            case 0: {
                this.playerUnitHeaders[n][9] = -1;
                break;
            }
            case 1: {
                int n9 = this.buildingTable[n][n4 + 0];
                if (n9 == this.playerUnitHeaders[n][10]) {
                    this.playerUnitHeaders[n][10] = -1;
                    break;
                }
                if (n9 != this.playerUnitHeaders[n][11]) break;
                this.playerUnitHeaders[n][11] = -1;
                break;
            }
            case 12: {
                if ((this.buildingTable[n][n4 + 2] & 0x40000000) != 0) break;
                int n10 = this.playerUnitHeaders[n][48];
                int n11 = n10 - 1 << 2;
                int n12 = this.buildingTable[n][n4 + 3] >>> 24 & 0xFF;
                if (n11 != n12) {
                    int n13;
                    for (n13 = 0; n13 < 4; ++n13) {
                        this.projectileTable[n][n12 + n13] = this.projectileTable[n][n11 + n13];
                    }
                    n13 = this.projectileTable[n][n11 + 0];
                    int n14 = this.projectileTable[n][n12 + 2] & 0xFFFF;
                    this.buildingTable[n][n13 + 3] = n12 << 24 | n14 << 8 | 0xC;
                }
                int[] nArray3 = this.playerUnitHeaders[n];
                nArray3[48] = nArray3[48] - 1;
            }
        }
        if (n4 != n5) {
            n6 = this.buildingTable[n][n5];
            ++n5;
            int n15 = n7 = (n6 >>> 8 & 0x3F) + ((n6 & 0x3F) << 6);
            this.mapTiles[n15] = (short)(this.mapTiles[n15] & 0xFFFFFF00);
            int n16 = n7;
            this.mapTiles[n16] = (short)(this.mapTiles[n16] | n2);
            this.buildingTable[n][n4] = n6;
            this.buildingTable[n][++n4] = this.buildingTable[n][n5];
            this.buildingTable[n][n5] = 0;
            this.buildingTable[n][++n4] = this.buildingTable[n][++n5];
            this.buildingTable[n][n5] = 0;
            this.buildingTable[n][++n4] = this.buildingTable[n][++n5];
            this.buildingTable[n][n5] = 0;
        } else {
            this.buildingTable[n][n4++] = 0;
            this.buildingTable[n][n4++] = 0;
            this.buildingTable[n][n4++] = 0;
            this.buildingTable[n][n4] = 0;
        }
        int[] nArray4 = this.playerUnitHeaders[n];
        nArray4[4] = nArray4[4] - 1;
    }

    final boolean boolean_b(int n, int n2) {
        int n3;
        int n4 = this.playerUnitSlots[n][(n2 <<= 3) + 2];
        if (n4 == (n3 = this.playerUnitSlots[n][n2 + 0])) {
            return false;
        }
        int targetPacked = n4; // n4 随后被 >>>= 8 就地改写,BFS 寻路要用打包目标
        short s = this.playerUnitSlots[n][n2 + 1];
        int n5 = n3 & 0xFF;
        short s2 = (short)(this.mapTiles[(n3 >>>= 8) + (n5 << 6)] & 0xFFF);
        int n6 = n3;
        int n7 = n5;
        int n8 = n4 & 0xFF;
        int n9 = (n4 >>>= 8) - n3;
        int n10 = 1;
        if (n9 == 0) {
            n10 = 0;
        } else if (n9 < 0) {
            n10 = -1;
            n9 = -n9;
        }
        int n11 = n8 - n5;
        int n12 = 1;
        if (n11 == 0) {
            n12 = 0;
        } else if (n11 < 0) {
            n12 = -1;
            n11 = -n11;
        }
        int n13 = this.playerUnitSlots[n][n2 + 3] >> 8;
        boolean dda = true;
        if (BFS_PATH) {
            // 可选 BFS 寻路：沿缓存路径取下一格替代 DDA 直线步进；之后的落点检查、
            // 抵达钩子、扇形回退、占位表更新完全复用。bfsNextStep 返回 -1 =
            // 目标不可达或已在目标隔壁 → 落回原 DDA 直线步进（严格不比原版差）。
            int bfsNext = this.bfsNextStep(n, n2 >> 3, n3, n5, targetPacked);
            if (bfsNext >= 0) {
                n3 = bfsNext >>> 8;
                n5 = bfsNext & 0xFF;
                n13 = 0;
                dda = false;
            }
        }
        if (dda) {
        if (n9 > n11) {
            if ((n13 += n11) << 1 >= n9) {
                n5 += n12;
                n13 -= n9;
            }
            n3 += n10;
        } else {
            if ((n13 += n9) << 1 >= n11) {
                n3 += n10;
                n13 -= n11;
            }
            n5 += n12;
        }
        }
        if ((n3 & 0xFF) > 63) {
            n6 = n3;
        }
        if ((n5 & 0xFF) > 63) {
            n7 = n5;
        }
        short[] sArray = this.playerUnitSlots[n];
        int n14 = n2 + 3;
        sArray[n14] = (short)(sArray[n14] & 0xFF);
        short[] sArray2 = this.playerUnitSlots[n];
        int n15 = n2 + 3;
        sArray2[n15] = (short)(sArray2[n15] | (short)n13 << 8);
        int n16 = (n3 << 8) + n5;
        if ((this.mapTiles[n3 + (n5 << 6)] & 0xFFF) != 0 || n16 == s) {
            int n17;
            short[] sArray3 = this.playerUnitSlots[n];
            int n18 = n2 + 3;
            sArray3[n18] = (short)(sArray3[n18] & 0xFF);
            if (n16 == this.playerUnitSlots[n][n2 + 2]) {
                short s3;
                this.playerUnitSlots[n][n2 + 1] = s3 = this.playerUnitSlots[n][n2 + 0];
                this.playerUnitSlots[n][n2 + 0] = (short)n16;
                this.void_a(n, n2 >> 3);
                short[] sArray4 = this.playerUnitSlots[n];
                int n19 = n2 + 6;
                sArray4[n19] = (short)(sArray4[n19] & 0xFF);
                this.playerUnitSlots[n][n2 + 2] = s3;
                this.playerUnitSlots[n][n2 + 0] = s3;
                this.c(n, n2, n3, n5);
                return false;
            }
            int n20 = ((this.playerUnitSlots[n][n2 + 6] & 0xFF) << 3) + 16;
            n3 = n6;
            n5 = n7;
            for (n17 = 0; n17 < 7; ++n17) {
                int n21;
                byte by = this.dirTable[n20++];
                int n22 = n5 + this.dirTable[by + 1];
                if ((((n21 = n3 + this.dirTable[by]) | n22) & 0xFFFFFFC0) != 0 || (this.mapTiles[n21 + (n22 << 6)] & 0xFFF) != 0 || (n21 << 8 | n22) == s) continue;
                n3 = n21;
                n5 = n22;
                short[] sArray5 = this.playerUnitSlots[n];
                int n23 = n2 + 3;
                sArray5[n23] = (short)(sArray5[n23] & 0xFF);
                n17 = 10;
            }
            if (n17 == 7) {
                short[] sArray6 = this.playerUnitSlots[n];
                int n24 = n2 + 6;
                sArray6[n24] = (short)(sArray6[n24] & 0xFF);
                return false;
            }
        }
        int n25 = n6 + (n7 << 6);
        this.mapTiles[n25] = (short)(this.mapTiles[n25] & 0xFFFFF000);
        this.mapTiles[(n3 &= 0x3F) + ((n5 &= 0x3F) << 6)] = (short)(this.mapTiles[n3 + (n5 << 6)] & 0xFFFFF000 | s2);
        this.playerUnitSlots[n][n2 + 1] = this.playerUnitSlots[n][n2 + 0];
        this.playerUnitSlots[n][n2 + 0] = (short)(n3 << 8 | n5);
        return true;
    }

    /**
     * BFS 寻路的"选落点"（仅 BFS_PATH 打开时由 boolean_b 调用）。
     * 返回下一步落点（打包 tx<<8|ty），-1 = 回退原 DDA+扇形行为。
     * 同步策略：到达建议格则推进；被扇形闪避带偏时先沿原路径归队（扇形落点通常
     * 仍在路径上，避免重算风暴），彻底离队才重算。节流/升级：沿路径连续
     * BFS_REPLAN_AFTER 次无进展（含"在几格间振荡但 pos 不前进"的活锁）→ 先升级为
     * "单位也当墙"重算绕开静态人墙；仍无进展 → 本目标永久回退原 DDA+扇形
     * （= 基准行为兜底，严格不比原版差）。
     */
    private int bfsNextStep(int player, int unit, int curTx, int curTy, int target) {
        int idx = player * BFS_MAX_UNITS + unit;
        int cur = curTx << 8 | curTy;
        if (this.bfsPathTarget[idx] != (short)target) {
            this.bfsUnitsBlock[idx] = 0;
            this.bfsComputePath(idx, cur, target, false);
        }
        int len = this.bfsPathLen[idx];
        if (len == 0) {
            return -1; // 不可达 / 已在目标隔壁 / 已判定回退 → 原 DDA（抵达钩子靠它）
        }
        int base = idx * BFS_PATH_CAP;
        int prevPos = this.bfsPathPos[idx];
        int pos = prevPos;
        if (pos < len && (this.bfsPath[base + pos] & 0xFFFF) == cur) {
            ++pos; // 确认到达上次建议的格子
        } else {
            // 先沿原路径归队（路径是最短路，无重复格，至多命中一次）
            int k = -1;
            for (int i = 0; i < len; ++i) {
                if ((this.bfsPath[base + i] & 0xFFFF) == cur) {
                    k = i;
                    break;
                }
            }
            if (k >= 0) {
                pos = k + 1;
            } else {
                // 彻底离队 → 以当前位置重算（保持当前 unitsBlock 档位）
                this.bfsComputePath(idx, cur, target, this.bfsUnitsBlock[idx] != 0);
                len = this.bfsPathLen[idx];
                if (len == 0) {
                    return -1;
                }
                pos = 0;
            }
        }
        this.bfsPathPos[idx] = (short)pos;
        if (pos >= len) {
            this.bfsPathStuck[idx] = 0;
            return -1; // 路径走完（墙目标走到隔壁）→ DDA 收尾，触发抵达钩子
        }
        if (pos > prevPos) {
            this.bfsPathStuck[idx] = 0;
        } else if (++this.bfsPathStuck[idx] >= BFS_REPLAN_AFTER) {
            this.bfsPathStuck[idx] = 0;
            if (this.bfsUnitsBlock[idx] == 0) {
                // 无进展到阈值 → 升级为"单位当墙"重算，绕开静态人墙/袋形死角
                this.bfsUnitsBlock[idx] = 1;
                this.bfsComputePath(idx, cur, target, true);
                if (System.getProperty("aoe.debug") != null) {
                    System.out.println("[bfs] p" + player + " u" + unit + " escalate unitsBlock"
                        + " cur=" + Integer.toHexString(cur) + " target=" + Integer.toHexString(target)
                        + " -> len=" + this.bfsPathLen[idx] + " ar=" + this.tickCount);
                }
                len = this.bfsPathLen[idx];
                if (len == 0) {
                    return -1;
                }
                this.bfsPathPos[idx] = 0;
                pos = 0;
            } else {
                // 单位当墙也走不通 → 本目标永久回退原 DDA+扇形（= 基准行为）
                if (System.getProperty("aoe.debug") != null) {
                    System.out.println("[bfs] p" + player + " u" + unit + " fallback to DDA"
                        + " cur=" + Integer.toHexString(cur) + " target=" + Integer.toHexString(target)
                        + " ar=" + this.tickCount);
                }
                this.bfsPathLen[idx] = 0;
                return -1;
            }
        }
        return this.bfsPath[base + pos] & 0xFFFF;
    }

    /**
     * 以 cur 为起点、target 为目标重算 BFS 路径：从目标（或其可走邻格，目标格本身是墙时）
     * 做多源反向洪泛建距离场，再从 cur 沿距离递减回溯出路径。队列/邻格顺序全固定，
     * 无 RNG、无 HashMap/墙钟，确定性可回放。unitsBlock=true 时单位占用格也当墙
     * （停滞后的升级重算：绕开静态人墙；起点格自身除外）。不可达/已在种子格/路径
     * 超容量时 len=0。
     */
    private void bfsComputePath(int idx, int cur, int target, boolean unitsBlock) {
        this.bfsPathTarget[idx] = (short)target;
        this.bfsPathPos[idx] = 0;
        this.bfsPathStuck[idx] = 0;
        int[] dist = this.bfsDist;
        for (int i = 0; i < 4096; ++i) {
            dist[i] = -1;
        }
        int[] queue = this.bfsQueue;
        int head = 0;
        int tail = 0;
        int ttx = target >>> 8 & 0x3F;
        int tty = target & 0x3F;
        if (this.bfsWalkable(ttx, tty, unitsBlock)) {
            dist[ttx + (tty << 6)] = 0;
            queue[tail++] = ttx + (tty << 6);
        } else {
            // 目标格是墙（采集资源/攻击建筑）：以其全部可走邻格为种子，
            // 走到目标隔壁即算到位，抵达钩子由 boolean_b 原有逻辑接管
            for (int d = 0; d < 8; ++d) {
                int nx = ttx + BFS_DX[d];
                int ny = tty + BFS_DY[d];
                if (!this.bfsWalkable(nx, ny, unitsBlock)) continue;
                dist[nx + (ny << 6)] = 0;
                queue[tail++] = nx + (ny << 6);
            }
        }
        int curTx = cur >>> 8 & 0x3F;
        int curTy = cur & 0x3F;
        int curTile = curTx + (curTy << 6);
        while (head < tail) {
            int t = queue[head++];
            if (t == curTile) break; // 队列按距离非减序弹出，首次弹出 cur 即最短
            int tx = t & 0x3F;
            int ty = t >>> 6;
            int nd = dist[t] + 1;
            for (int d = 0; d < 8; ++d) {
                int nx = tx + BFS_DX[d];
                int ny = ty + BFS_DY[d];
                if (((nx | ny) & 0xFFFFFFC0) != 0) continue;
                int nt = nx + (ny << 6);
                if (dist[nt] != -1) continue;
                // 起点格带单位自己(0x200)，unitsBlock 下也要允许踏入
                if (nt != curTile && !this.bfsWalkable(nx, ny, unitsBlock)) continue;
                dist[nt] = nd;
                queue[tail++] = nt;
            }
        }
        int d0 = dist[curTile];
        if (d0 <= 0 || d0 > BFS_PATH_CAP) {
            this.bfsPathLen[idx] = 0; // 不可达 / 已在目标隔壁 / 超长 → 回退原行为
            return;
        }
        // 沿距离场回溯：每步按固定顺序找 dist 恰好小 1 的邻格（距离场保证必然找到）
        int base = idx * BFS_PATH_CAP;
        for (int step = 0; step < d0; ++step) {
            int want = d0 - step - 1;
            for (int d = 0; d < 8; ++d) {
                int nx = curTx + BFS_DX[d];
                int ny = curTy + BFS_DY[d];
                if (((nx | ny) & 0xFFFFFFC0) != 0) continue;
                if (dist[nx + (ny << 6)] != want) continue;
                curTx = nx;
                curTy = ny;
                break;
            }
            this.bfsPath[base + step] = (short)(curTx << 8 | curTy);
        }
        this.bfsPathLen[idx] = (short)d0;
    }

    /**
     * BFS 障碍判定：建筑(0x100)与地形资源/虚空(0x300)当墙；空格与单位占用(0x200)
     * 可穿；unitsBlock=true（停滞后的升级重算）时单位占用格也当墙。
     */
    private boolean bfsWalkable(int tx, int ty, boolean unitsBlock) {
        if (((tx | ty) & 0xFFFFFFC0) != 0) {
            return false;
        }
        int kind = this.mapTiles[tx + (ty << 6)] & 0x300;
        return kind == 0 || (kind == 0x200 && !unitsBlock);
    }

    final void void_a(int n, int n2) {
        int n3 = n2 << 3;
        int n4 = (this.playerUnitSlots[n][n3 + 0] & 0xFF00) >>> 8;
        int n5 = this.playerUnitSlots[n][n3 + 0] & 0xFF;
        int n6 = (this.playerUnitSlots[n][n3 + 1] & 0xFF00) >>> 8;
        int n7 = this.playerUnitSlots[n][n3 + 1] & 0xFF;
        int n8 = n6 - n4;
        int n9 = n7 - n5;
        int n10 = n8 << 8 & 0xFF00 | n9 & 0xFF;
        int n11 = 0;
        switch (n10) {
            case 256: {
                n11 = 0;
                short[] sArray = this.playerUnitSlots[n];
                int n12 = n3 + 4;
                sArray[n12] = (short)(sArray[n12] | 0x2000);
                short[] sArray2 = this.playerUnitSlots[n];
                int n13 = n3 + 4;
                sArray2[n13] = (short)(sArray2[n13] & 0xBFFF);
                break;
            }
            case 257: {
                n11 = 1;
                short[] sArray = this.playerUnitSlots[n];
                int n14 = n3 + 4;
                sArray[n14] = (short)(sArray[n14] | 0x2000);
                break;
            }
            case 1: {
                n11 = 2;
                short[] sArray = this.playerUnitSlots[n];
                int n15 = n3 + 4;
                sArray[n15] = (short)(sArray[n15] | 0x2000);
                short[] sArray3 = this.playerUnitSlots[n];
                int n16 = n3 + 4;
                sArray3[n16] = (short)(sArray3[n16] | 0x4000);
                break;
            }
            case 65281: {
                n11 = 3;
                short[] sArray = this.playerUnitSlots[n];
                int n17 = n3 + 4;
                sArray[n17] = (short)(sArray[n17] | 0x4000);
                break;
            }
            case 65280: {
                n11 = 4;
                short[] sArray = this.playerUnitSlots[n];
                int n18 = n3 + 4;
                sArray[n18] = (short)(sArray[n18] & 0xDFFF);
                short[] sArray4 = this.playerUnitSlots[n];
                int n19 = n3 + 4;
                sArray4[n19] = (short)(sArray4[n19] | 0x4000);
                break;
            }
            case 65535: {
                n11 = 5;
                short[] sArray = this.playerUnitSlots[n];
                int n20 = n3 + 4;
                sArray[n20] = (short)(sArray[n20] & 0xDFFF);
                break;
            }
            case 255: {
                n11 = 6;
                short[] sArray = this.playerUnitSlots[n];
                int n21 = n3 + 4;
                sArray[n21] = (short)(sArray[n21] & 0xDFFF);
                short[] sArray5 = this.playerUnitSlots[n];
                int n22 = n3 + 4;
                sArray5[n22] = (short)(sArray5[n22] & 0xBFFF);
                break;
            }
            case 511: {
                n11 = 7;
                short[] sArray = this.playerUnitSlots[n];
                int n23 = n3 + 4;
                sArray[n23] = (short)(sArray[n23] & 0xBFFF);
            }
        }
        this.playerUnitSlots[n][n3 + 6] = (short)(n11 | 0xF00);
    }

    final void b(int n, int n2, int n3, int n4) {
        int n5 = (this.playerUnitSlots[n][n2 + 0] & 0xFF00) >>> 8;
        int n6 = this.playerUnitSlots[n][n2 + 0] & 0xFF;
        n5 = (n3 - n5) * 181;
        n6 = (n4 - n6) * 181;
        int n7 = n5 - n6;
        int n8 = n6 + n5;
        if (n7 > 0) {
            short[] sArray = this.playerUnitSlots[n];
            int n9 = n2 + 4;
            sArray[n9] = (short)(sArray[n9] | 0x4000);
        } else if (n7 < 0) {
            short[] sArray = this.playerUnitSlots[n];
            int n10 = n2 + 4;
            sArray[n10] = (short)(sArray[n10] & 0xBFFF);
        }
        if (n8 < 0) {
            short[] sArray = this.playerUnitSlots[n];
            int n11 = n2 + 4;
            sArray[n11] = (short)(sArray[n11] | 0x2000);
            return;
        }
        if (n8 > 0) {
            short[] sArray = this.playerUnitSlots[n];
            int n12 = n2 + 4;
            sArray[n12] = (short)(sArray[n12] & 0xDFFF);
        }
    }

    final void aimProjectiles() {
        for (int i = 0; i < 2; ++i) {
            int n = this.playerUnitHeaders[i][48];
            if (n == 0) continue;
            int n2 = this.tickCount % n << 2;
            int n3 = n;
            int n4 = n3 << 2;
            // 找一条"待瞄准"记录(+1==1000,发射/落地后回到该态)。原版字节码(方法 G)
            // 自带正确边界:--n3>0 才回循环头(ifgt),≤0 落到循环出口——与"找到 1000"
            // 同一入口。CFR 把该出口错渲染成 while 体末尾的裸 continue(= 回头再判
            // 条件)而丢失,窗口内全是飞行中(已瞄准)记录时即永久自旋:模拟+渲染整体
            // 跑在 Timer 线程 serviceRepaints 里,表现为画面冻结、EDT 键鼠仍收、无
            // 异常栈(2026-09-01 战役首战卡死,字节码考证=移植伪影,原版无此 bug)。
            // 此处按原版语义补回:最多扫 n 条,找不到就本 tick 跳过该玩家。
            while (this.projectileTable[i][n2 + 1] != 1000) {
                if ((n2 += 4) >= n4) {
                    n2 = 0;
                }
                if (--n3 <= 0) {
                    if (System.getProperty("aoe.debug") != null) {
                        System.out.println("[proj] aim scan exhausted p=" + i
                            + " active=" + n + " tick=" + this.tickCount);
                    }
                    break;
                }
            }
            if (n3 <= 0) continue;
            int n5 = this.playerUnitHeaders[i][12];
            short s = this.projectileTable[i][n2 + 0];
            int n6 = this.buildingTable[i][s + 0];
            int n7 = n6 >>> 8 & 0x3F;
            n6 &= 0x3F;
            int n8 = i ^ 1;
            int n9 = 0;
            int n10 = this.playerUnitHeaders[n8][2];
            int n11 = 0;
            while (n11 < n10) {
                int n12 = this.playerUnitSlots[n8][n9 + 0];
                int n13 = (n12 >>> 8 & 0x3F) - n7;
                if (n13 * n13 + (n12 = (n12 & 0x3F) - n6) * n12 <= n5) {
                    this.projectileTable[i][n2 + 3] = 0;
                    this.projectileTable[i][n2 + 1] = (short)n9;
                    this.projectileTable[i][n2 + 2] = this.playerUnitSlots[n8][n9 + 0];
                    int[] nArray = this.buildingTable[i];
                    int n14 = s + 3;
                    nArray[n14] = nArray[n14] & 0xFF0000FF;
                    int[] nArray2 = this.buildingTable[i];
                    int n15 = s + 3;
                    nArray2[n15] = nArray2[n15] | (this.projectileTable[i][n2 + 2] & 0xFFFF) << 8;
                    n11 = 1000;
                }
                ++n11;
                n9 += 8;
            }
        }
    }

    final void tickProjectiles() {
        for (int i = 0; i < 2; ++i) {
            int n = 0;
            int n2 = this.playerUnitHeaders[i][48];
            int n3 = 0;
            while (n3 < n2) {
                short s = this.projectileTable[i][n + 1];
                if (s != 1000) {
                    int n4 = this.projectileTable[i][n + 3] + 1;
                    this.projectileTable[i][n + 3] = (short)(n4 & 0xF);
                    if (n4 >= 16) {
                        int n5 = this.projectileTable[i][n + 2];
                        int n6 = n5 >>> 8 & 0x3F;
                        int n7 = n6 + ((n5 &= 0x3F) << 6);
                        int n8 = (this.mapTiles[n7] & 0xC00) >> 10;
                        if (n8 == i || (this.mapTiles[n7] & 0xFFF) == 0) {
                            this.projectileTable[i][n + 1] = 1000;
                            int[] nArray = this.buildingTable[i];
                            int n9 = this.projectileTable[i][n + 0] + 3;
                            nArray[n9] = nArray[n9] & 0xFF0000FF;
                        } else if ((this.mapTiles[n7] & 0x300) == 512) {
                            int n10 = this.playerUnitSlots[n8][s + 4] & 0xFF;
                            int n11 = (this.playerUnitHeaders[i][46] << 4) / this.playerUnitHeaders[n8][23 + (this.playerUnitSlots[n8][s + 3] & 0xFF)];
                            if ((n10 -= n11) <= 0) {
                                this.g(n8, this.mapTiles[n7] & 0xFF);
                                this.projectileTable[i][n + 1] = 1000;
                                int[] nArray = this.buildingTable[i];
                                int n12 = this.projectileTable[i][n + 0] + 3;
                                nArray[n12] = nArray[n12] & 0xFF0000FF;
                            } else {
                                short[] sArray = this.playerUnitSlots[n8];
                                int n13 = s + 4;
                                sArray[n13] = (short)(sArray[n13] & 0xFF00);
                                short[] sArray2 = this.playerUnitSlots[n8];
                                int n14 = s + 4;
                                sArray2[n14] = (short)(sArray2[n14] | (n10 | 0x1000));
                                if (n8 == 0) {
                                    if (this.playerUnitSlots[n8][s + 2] == this.playerUnitSlots[n8][s + 0] && (this.playerUnitSlots[n8][s + 7] & 0xFF) != 1) {
                                        this.playerUnitSlots[n8][s + 2] = (short)this.buildingTable[i][this.projectileTable[i][n + 0] + 0];
                                    }
                                } else if ((this.playerUnitSlots[n8][s + 7] & 0xFF) != 1) {
                                    this.playerUnitSlots[n8][s + 2] = (short)this.buildingTable[i][this.projectileTable[i][n + 0] + 0];
                                }
                            }
                        } else {
                            this.projectileTable[i][n + 1] = 1000;
                            int[] nArray = this.buildingTable[i];
                            int n15 = this.projectileTable[i][n + 0] + 3;
                            nArray[n15] = nArray[n15] & 0xFF0000FF;
                        }
                    }
                }
                ++n3;
                n += 4;
            }
        }
    }

    final void B() {
        for (int i = 0; i < 2; ++i) {
            boolean bl = false;
            int n = this.tickCount;
            int n2 = this.playerUnitHeaders[i][2];
            if (n2 == 0) continue;
            int n3 = 0;
            while (n3 < n2) {
                int n4;
                ++n3;
                if ((this.playerUnitSlots[i][(n4 = n++ % n2 << 3) + 7] & 0xF) != 1 && (this.playerUnitSlots[i][n4 + 6] & 0xFF00) == 0 && (this.playerUnitSlots[i][n4 + 3] & 0xFF) >= 2 && (i != 0 || this.playerUnitSlots[i][n4 + 0] == this.playerUnitSlots[i][n4 + 2])) {
                    if (!this.boolean_a(i, n4)) {
                        this.void_b(i, n4);
                    }
                    bl = true;
                }
                // 原版字节码(方法 B,175: iload_2; 176: ifeq 34; 179: iinc 1,1):
                // bl 置位即 break——每玩家每次调用只处理一个单位。CFR 把该出口
                // 渲染成 `if (!bl) continue;`(两分支都继续循环),行为变成"每 tick
                // 全部处理"。2026-09-01 重编译字节码对拍发现,按原版语义修复。
                if (bl) {
                    break;
                }
            }
        }
    }

    final boolean boolean_a(int n, int n2) {
        int n3 = this.playerUnitSlots[n][n2 + 3] & 0xFF;
        int n4 = this.playerUnitSlots[n][n2 + 0];
        int n5 = n4 >>> 8;
        n4 &= 0xFF;
        int n6 = 0;
        switch (n3) {
            case 4: {
                n6 = 16;
                break;
            }
            case 2: {
                n6 = 9;
                break;
            }
            case 3: {
                n6 = 9;
                break;
            }
            case 5: {
                n6 = 16;
                break;
            }
            case 6: {
                n6 = 9;
                break;
            }
            case 8: {
                n6 = 16;
                break;
            }
            case 7: {
                n6 = 9;
                break;
            }
            case 9: {
                n6 = 16;
            }
        }
        int n7 = n ^ 1;
        int n8 = 0;
        int n9 = this.playerUnitHeaders[n7][2];
        int n10 = 0;
        while (n10 < n9) {
            int n11 = this.playerUnitSlots[n7][n8 + 0];
            int n12 = (n11 >>> 8) - n5;
            int n13 = n12 * n12 + (n11 = (n11 & 0xFF) - n4) * n11;
            if (n13 <= 1) {
                short[] sArray = this.playerUnitSlots[n];
                int n14 = n2 + 7;
                sArray[n14] = (short)(sArray[n14] & 0xFFF0);
                short[] sArray2 = this.playerUnitSlots[n];
                int n15 = n2 + 7;
                sArray2[n15] = (short)(sArray2[n15] | 1);
                this.playerUnitSlots[n][n2 + 5] = this.playerUnitSlots[n7][n8 + 0];
                this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n][n2 + 0];
                this.b(n, n2, this.playerUnitSlots[n7][n8 + 0] >>> 8, this.playerUnitSlots[n7][n8 + 0] & 0xFF);
                if ((this.playerUnitSlots[n7][n8 + 7] & 0xF) == 1) {
                    return true;
                }
                short[] sArray3 = this.playerUnitSlots[n7];
                int n16 = n8 + 7;
                sArray3[n16] = (short)(sArray3[n16] & 0xFFF0);
                short[] sArray4 = this.playerUnitSlots[n7];
                int n17 = n8 + 7;
                sArray4[n17] = (short)(sArray4[n17] | 1);
                this.playerUnitSlots[n7][n8 + 5] = this.playerUnitSlots[n][n2 + 0];
                this.playerUnitSlots[n7][n8 + 2] = this.playerUnitSlots[n7][n8 + 0];
                this.b(n7, n8, n5, n4);
                return true;
            }
            if (n13 <= n6) {
                switch (n3) {
                    case 4: 
                    case 8: {
                        short[] sArray = this.playerUnitSlots[n];
                        int n18 = n2 + 7;
                        sArray[n18] = (short)(sArray[n18] & 0xFFF0);
                        short[] sArray5 = this.playerUnitSlots[n];
                        int n19 = n2 + 7;
                        sArray5[n19] = (short)(sArray5[n19] | 1);
                        this.playerUnitSlots[n][n2 + 5] = this.playerUnitSlots[n7][n8 + 0];
                        this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n][n2 + 0];
                        this.b(n, n2, this.playerUnitSlots[n7][n8 + 0] >>> 8, this.playerUnitSlots[n7][n8 + 0] & 0xFF);
                        if ((this.playerUnitSlots[n7][n8 + 7] & 0xF) == 1) {
                            return true;
                        }
                        this.playerUnitSlots[n7][n8 + 2] = this.playerUnitSlots[n][n2 + 0];
                        this.b(n7, n8, n5, n4);
                        return true;
                    }
                }
                this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n7][n8 + 0];
                return true;
            }
            ++n10;
            n8 += 8;
        }
        return false;
    }

    final void void_b(int n, int n2) {
        int n3 = this.playerUnitSlots[n][n2 + 0];
        int n4 = n3 >>> 8 & 0x3F;
        n3 &= 0x3F;
        int n5 = this.playerUnitSlots[n][n2 + 3] & 0xFF;
        int n6 = 0;
        switch (n5) {
            case 4: {
                n6 = 16;
                break;
            }
            case 2: {
                n6 = 9;
                break;
            }
            case 3: {
                n6 = 9;
                break;
            }
            case 5: {
                n6 = 16;
                break;
            }
            case 6: {
                n6 = 9;
                break;
            }
            case 8: {
                n6 = 16;
                break;
            }
            case 7: {
                n6 = 9;
                break;
            }
            case 9: {
                n6 = 16;
            }
        }
        int n7 = Integer.MAX_VALUE;
        int n8 = -1;
        int n9 = n ^ 1;
        int n10 = 0;
        int n11 = this.playerUnitHeaders[n9][4];
        int n12 = 0;
        while (n12 < n11) {
            int n13;
            int n14 = this.buildingTable[n9][n10 + 0];
            int n15 = n14 >>> 8 & 0x3F;
            n14 &= 0x3F;
            if ((n13 = (n15 -= n4) * n15 + (n14 -= n3) * n14) > 0 && n13 <= n6) {
                if ((this.buildingTable[n9][n10 + 3] & 0xFF) == 12) {
                    n8 = n10;
                    break;
                }
                if (n13 < n7) {
                    n7 = n13;
                    n8 = n10;
                }
            }
            ++n12;
            n10 += 4;
        }
        if (n8 >= 0) {
            switch (n5) {
                case 4: 
                case 8: {
                    short[] sArray = this.playerUnitSlots[n];
                    int n16 = n2 + 7;
                    sArray[n16] = (short)(sArray[n16] & 0xFFF0);
                    short[] sArray2 = this.playerUnitSlots[n];
                    int n17 = n2 + 7;
                    sArray2[n17] = (short)(sArray2[n17] | 1);
                    this.playerUnitSlots[n][n2 + 5] = (short)this.buildingTable[n9][n8 + 0];
                    this.b(n, n2, this.buildingTable[n9][n8 + 0] >>> 8, this.buildingTable[n9][n8 + 0] & 0xFF);
                    this.playerUnitSlots[n][n2 + 2] = this.playerUnitSlots[n][n2 + 0];
                    return;
                }
            }
            this.playerUnitSlots[n][n2 + 2] = (short)this.buildingTable[n9][n8 + 0];
        }
    }

    final void tickAi() {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6 = 0;
        int n7 = this.playerUnitHeaders[0][2];
        if (n7 != 0) {
            n5 = this.playerUnitHeaders[1][54];
            n4 = this.playerUnitHeaders[1][55];
            n3 = this.playerUnitHeaders[0][55];
            n2 = (n5 & 0x7F000000) >> 24;
            n5 &= 0xFFFFFF;
            if (n2 >= n7) {
                this.var_int_i = 0;
                if (n3 < n4 + (n4 >> 2) && n4 >= this.aiAttackThreshold) {
                    this.var_int_i = 2;
                } else if (n5 <= this.aiGuardRadiusSq) {
                    this.var_int_i = 1;
                }
                this.playerUnitHeaders[1][54] = 0xFFFFFF;
            } else {
                n = this.playerUnitHeaders[1][8];
                if (n >= 0) {
                    int n8;
                    int n9 = n >>> 8;
                    n &= 0xFF;
                    int n10 = this.playerUnitSlots[0][(++n2 << 3) + 0];
                    int n11 = n10 >>> 8;
                    if ((n8 = (n9 -= n11) * n9 + (n -= (n10 &= 0xFF)) * n) < n5) {
                        n5 = n8;
                        this.playerUnitHeaders[1][53] = this.playerUnitSlots[0][(n2 << 3) + 0];
                    }
                    this.playerUnitHeaders[1][54] = n5 & 0xFFFFFF | n2 << 24;
                }
            }
        }
        if (this.aiEnabled) {
            if (this.aiFreeResTimer++ >= this.aiFreeResInterval) {
                int[] nArray = this.playerUnitHeaders[1];
                nArray[5] = nArray[5] + this.playerUnitHeaders[1][57];
                int[] nArray2 = this.playerUnitHeaders[1];
                nArray2[6] = nArray2[6] + this.playerUnitHeaders[1][57];
                int[] nArray3 = this.playerUnitHeaders[1];
                nArray3[7] = nArray3[7] + this.playerUnitHeaders[1][57];
                this.aiFreeResTimer = 0;
            }
            if (this.aiBuildTimer++ >= this.aiBuildInterval) {
                this.void_a();
                if (this.tryPlaceAiBuilding()) {
                    this.aiBuildTimer = 0;
                }
            }
            if (this.aiTrainTimer++ >= this.aiTrainInterval && this.tryTrainAiUnit(this.tickCount % 10)) {
                this.aiTrainTimer = 0;
            }
        }
        n6 = 0;
        n7 = this.playerUnitHeaders[1][2];
        if (n7 == 0) {
            return;
        }
        do {
            if ((this.playerUnitSlots[1][n6 + 3] & 0xFF) < 2 && (this.playerUnitSlots[1][n6 + 7] & 0xFF) == 0 && (this.playerUnitSlots[1][n6 + 2] == this.playerUnitSlots[1][n6 + 0] || this.playerUnitSlots[1][n6 + 1] == this.playerUnitSlots[1][n6 + 0])) {
                this.playerUnitSlots[1][n6 + 2] = this.findNearbyResource((int)this.playerUnitSlots[1][n6 + 0], this.int_b(1));
            }
            n6 += 8;
        } while (--n7 > 0);
        if (!this.aiEnabled) {
            return;
        }
        n5 = n7;
        n4 = 0;
        n7 = this.playerUnitHeaders[1][2];
        n3 = -1;
        n2 = -1;
        switch (this.var_int_i) {
            case 2: {
                if (this.playerUnitHeaders[0][8] == -1) break;
                n3 = (short)(this.playerUnitHeaders[0][8] + (this.tickCount & 1) + ((this.tickCount & 3) - 2 << 8));
                n4 = n7 - (n5 >>= 2);
                break;
            }
            case 1: {
                if (this.playerUnitHeaders[1][8] == -1) break;
                n3 = (short)(this.playerUnitHeaders[0][8] + (this.tickCount & 1) + ((this.tickCount & 3) - 2 << 8));
                n2 = (short)this.playerUnitHeaders[1][53];
                n4 = n7 - (n5 >>= 3);
            }
        }
        if (this.var_int_i != 0) {
            n6 = 0;
            if (n3 >= 0) {
                n = 0;
                while (n < n4) {
                    if ((this.playerUnitSlots[1][n6 + 3] & 0xFF) >= 2 && (this.playerUnitSlots[1][n6 + 7] & 0xF) != 1) {
                        this.playerUnitSlots[1][n6 + 2] = (short)n3;
                    }
                    ++n;
                    n6 += 8;
                }
            }
            if (n2 >= 0) {
                n = n4;
                while (n < n5) {
                    if ((this.playerUnitSlots[1][n6 + 3] & 0xFF) >= 2 && (this.playerUnitSlots[1][n6 + 7] & 0xF) != 1) {
                        this.playerUnitSlots[1][n6 + 2] = (short)n2;
                    }
                    ++n;
                    n6 += 8;
                }
            }
            this.var_int_i = 0;
        }
    }

    final boolean tryPlaceAiBuilding() {
        if (this.aiBuildTarget == -1) {
            return true;
        }
        if (!this.canAfford(1, 1, this.aiBuildTarget)) {
            return false;
        }
        int n = this.playerUnitHeaders[1][8];
        if (n == -1) {
            return true;
        }
        int n2 = this.findAiBuildSpot(n);
        int n3 = n2 >>> 8;
        this.a(1, this.aiBuildTarget, n3, n2 &= 0xFF, 0x40000000, false);
        return true;
    }

    final void void_a() {
        int n = this.aiBuildPhase;
        while (this.aiBuildOrder[n] >= 0) {
            if (this.countBuildings(1, (int)this.aiBuildOrder[n], false) < this.aiBuildOrder[n + 1]) {
                this.aiBuildTarget = this.aiBuildOrder[n];
                return;
            }
            n += 2;
        }
        if (this.aiBuildOrder[n] == -1) {
            this.aiBuildPhase = n + 1;
        }
        this.aiBuildTarget = -1;
    }

    public final int findAiBuildSpot(int n) {
        int n2 = n;
        int n3 = 0;
        int n4 = -2;
        int n5 = 2;
        int n6 = n >>> 8;
        n &= 0xFF;
        do {
            for (int i = n4; i <= n5; i += 2) {
                for (int j = n4; j <= n5; j += 2) {
                    int n7;
                    int n8 = n6 + i;
                    int n9 = n + j;
                    if ((n8 | n9) < 0 || n8 >= 64 || n9 >= 64 || (this.mapTiles[n7 = n8 + (n9 << 6)] & 0x300) != 0 || (this.mapTiles[n7] & 0xFFF) != 0) continue;
                    return n8 << 8 | n9;
                }
            }
            --n4;
            ++n5;
        } while (n3++ < 10);
        return n2;
    }

    public final short findNearbyResource(int n, int n2) {
        int n3;
        n2 &= 3;
        int n4 = n;
        int n5 = 0;
        boolean bl = false;
        boolean bl2 = false;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 2;
        int n10 = n >>> 8 & 0x3F;
        int n11 = n & 0x3F;
        short s = 0;
        switch (n2) {
            case 1: {
                s = this.var_short_a;
                break;
            }
            case 2: {
                s = this.var_short_c;
                break;
            }
            case 3: {
                s = this.var_short_b;
            }
        }
        if (s >= 0 && (this.mapTiles[n3 = (n10 = s >>> 8 & 0x3F) + ((n11 = s & 0x3F) << 6)] & Short.MAX_VALUE) != 0) {
            return s;
        }
        do {
            if (((n10 | n11) & 0xFFFFFFC0) == 0 && (this.mapTiles[n3 = n10 | n11 << 6] & 0x300) == 768 && (this.mapTiles[n3] & 3) == n2) {
                s = (short)(n10 << 8 | n11);
                switch (n2) {
                    case 1: {
                        this.var_short_a = s;
                        break;
                    }
                    case 2: {
                        this.var_short_c = s;
                        break;
                    }
                    case 3: {
                        this.var_short_b = s;
                    }
                }
                return s;
            }
            switch (n6) {
                case 0: {
                    ++n10;
                    break;
                }
                case 1: {
                    ++n11;
                    break;
                }
                case 2: {
                    --n10;
                    break;
                }
                case 3: {
                    --n11;
                }
            }
            if (++n7 != n9) continue;
            n7 = 0;
            n6 = n6 + 1 & 3;
            if (++n8 != 2) continue;
            n8 = 0;
            ++n9;
        } while (n5++ < 65536);
        return (short)n4;
    }

    final int int_b(int n) {
        int n2 = this.playerUnitHeaders[n][5];
        int n3 = this.playerUnitHeaders[n][7];
        int n4 = this.playerUnitHeaders[n][6];
        int n5 = n2 + n3 + n4;
        if ((n2 << 16) / n5 < 21845) {
            return 769;
        }
        if ((n3 << 16) / n5 < 21845) {
            return 771;
        }
        return 770;
    }

    final boolean tryTrainAiUnit(int n) {
        int n2;
        int n3 = this.playerUnitHeaders[1][2] + this.playerUnitHeaders[1][49];
        if (n3 >= this.playerUnitHeaders[1][3]) {
            return false;
        }
        if (n3 >= 26) {
            return false;
        }
        if (!this.canAfford(1, 0, n)) {
            return false;
        }
        int n4 = this.playerUnitHeaders[0][0];
        if (n4 != 0 && n == 2) {
            n = 3;
        }
        if (n4 == 0 && n == 3) {
            n = 2;
        }
        if (n4 < 2 && n == 6) {
            n = 5;
        }
        if (n4 >= 2 && n == 5) {
            n = 6;
        }
        if (n == 8 && this.countBuildings(1, 3, true) == 0) {
            return false;
        }
        int n5 = n - 1;
        if (n5 < 0) {
            n5 = 0;
        }
        if ((n2 = this.playerUnitHeaders[1][57 + n5] + this.playerUnitHeaders[1][66 + n5]) >= this.playerUnitHeaders[1][75 + n5]) {
            return false;
        }
        this.queueUnitTraining(1, n);
        return true;
    }

    final int int_a(int n, int n2, int n3) {
        int n4 = this.playerUnitHeaders[n][8];
        if (n4 == -1) {
            return n2;
        }
        switch (n3 & 0xF0) {
            case 16: {
                int n5;
                int n6;
                int n7;
                int n8;
                int n9;
                int n10;
                int n11;
                int n12;
                int n13 = this.playerUnitHeaders[n][9];
                if (n13 > 0 && (n12 = (n11 = (n13 >>> 8) - (n10 = n2 >>> 8)) * n11 + (n9 = (n13 & 0xFF) - (n8 = n2 & 0xFF)) * n9) <= (n7 = (n6 = (n4 >>> 8) - n10) * n6 + (n5 = (n4 & 0xFF) - n8) * n5)) {
                    return n13;
                }
                return n4;
            }
            case 32: 
            case 48: {
                int n14 = n4;
                int n15 = n2 >>> 8;
                int n16 = n2 & 0xFF;
                int n17 = (n4 >>> 8) - n15;
                int n18 = (n4 & 0xFF) - n16;
                int n19 = n17 * n17 + n18 * n18;
                for (int i = 0; i < 2; ++i) {
                    int n20;
                    int n21;
                    int n22;
                    int n23 = this.playerUnitHeaders[n][10 + i];
                    if (n23 <= 0 || (n22 = (n21 = (n23 >>> 8) - n15) * n21 + (n20 = (n23 & 0xFF) - n16) * n20) > n19) continue;
                    n19 = n22;
                    n14 = n23;
                }
                return n14;
            }
        }
        return n2;
    }

    final void m(Graphics graphics) {
        int n = this.var_int_d - this.cameraPxX;
        int n2 = this.S - this.cameraPxY;
        int n3 = 0xFFFFFF;
        int n4 = 0x7F007F;
        int n5 = this.au;
        for (int i = 0; i < 2; ++i) {
            int n6 = n + n5;
            int n7 = n2;
            int n8 = n;
            int n9 = n2 + (n5 >> 1);
            int n10 = n - n5;
            int n11 = n2;
            int n12 = n;
            int n13 = n2 - (n5 >> 1);
            graphics.setColor(n3 -= n4);
            graphics.drawLine(n6, n7, n8, n9);
            graphics.drawLine(n8, n9, n10, n11);
            graphics.drawLine(n10, n11, n12, n13);
            graphics.drawLine(n12, n13, n6, n7);
            n5 = n5 + 31 >> 1;
        }
        this.au = this.au + 32 >> 1;
        --this.U;
    }

    public final void tickMissionScript() {
        int n;
        int n2 = 0;
        while (this.missionScript[n2] != 127) {
            n = n2;
            if (this.missionScript[n2] < 0) {
                n2 = this.skipScriptBlock(n2);
                continue;
            }
            if ((n2 = this.evalScriptCondition(n2)) >= 0) {
                n2 = this.runScriptActions(n2, n);
                continue;
            }
            n2 = this.skipScriptActions(-n2);
        }
        n = 0;
        while (n < 4) {
            int n3 = n++;
            this.var_int_arr_a[n3] = this.var_int_arr_a[n3] + 1;
        }
        this.var_int_c = 0;
    }

    public final int evalScriptCondition(int n) {
        switch (this.missionScript[n++]) {
            case 1: {
                int n2;
                int n3 = this.missionScript[n++] & 0xFF;
                int n4 = this.missionScript[n++] & 0xFF;
                int n5 = this.missionScript[n++] & 0xFF;
                int n6 = this.missionScript[n++] & 0xFF;
                int n7 = this.missionScript[n++] & 0xFF;
                int n8 = this.missionScript[n++] & 0xFF;
                int n9 = this.missionScript[n++] & 0xFF;
                if (n3 == 1) {
                    n = -n;
                }
                if ((this.playerUnitSlots[n4][(n2 = n5 << 3) + 6] & 0xFF00) != 0) {
                    return -n;
                }
                int n10 = this.playerUnitSlots[n4][n2 + 0];
                int n11 = n10 >>> 8;
                n10 &= 0xFF;
                if (n11 < n6) {
                    return -n;
                }
                if (n10 < n7) {
                    return -n;
                }
                if (n11 >= n6 + n8) {
                    return -n;
                }
                if (n10 >= n7 + n9) {
                    return -n;
                }
                return n;
            }
            case 2: {
                byte by = this.missionScript[n++];
                if ((this.missionScript[n++] & 0xFF) * 10 > this.var_int_arr_a[by]) break;
                return n;
            }
            case 6: {
                int n12 = this.missionScript[n++] & 0xFF;
                int n13 = this.missionScript[n++] & 0xFF;
                if (this.techFlags[n12] != n13) break;
                return n;
            }
            case 7: {
                int n14 = this.missionScript[n++] & 0xFF;
                int n15 = this.missionScript[n++] & 0xFF;
                int n16 = this.missionScript[n++] & 0xFF;
                int n17 = this.missionScript[n++] & 0xFF;
                if (!(n16 == 0 ? this.playerUnitHeaders[n14][n15] == n17 : (n16 == 1 ? this.playerUnitHeaders[n14][n15] > n17 : n16 == 2 && this.playerUnitHeaders[n14][n15] < n17))) break;
                return n;
            }
            case 5: {
                int n18 = this.missionScript[n++] & 0xFF;
                int n19 = this.missionScript[n++] & 0xFF;
                byte by = this.missionScript[n++];
                int n20 = this.int_b(n18, n19);
                if (n20 < 0) {
                    return -n;
                }
                if (by == -1 || n20 == by) {
                    return n;
                }
                if (by == 0 && n20 < 2) {
                    return n;
                }
                return -n;
            }
            case 3: {
                if (this.missionScript[n++] != this.screenState) break;
                return n;
            }
            case 4: {
                int n21 = this.missionScript[n++] & 0xFF;
                int n22 = this.missionScript[n++] & 0xFF;
                int n23 = (this.missionScript[n++] & 0xFF) << 8;
                byte by = this.missionScript[n++];
                byte by2 = this.missionScript[n++];
                if (n21 == 1) {
                    n = -n;
                }
                if (this.selectionPlayer != n22) {
                    return -n;
                }
                if (this.selectedSlot != by2) {
                    return -n;
                }
                if (this.selectionMark != n23) {
                    return -n;
                }
                if (by != -1 && this.selectedType != by) {
                    return -n;
                }
                return n;
            }
        }
        return -n;
    }

    public final int runScriptActions(int n, int n2) {
        block13: while (true) {
            switch (this.missionScript[n++]) {
                case 8: {
                    int n3;
                    int n4 = this.missionScript[n++];
                    int n5 = this.missionScript[n++] & 0xFF;
                    int n6 = this.missionScript[n++];
                    if (n5 >= 5 && n5 <= 7) {
                        this.var_boolean_l = true;
                    }
                    if (n4 == -1) {
                        for (n3 = 0; n3 < 2; ++n3) {
                            this.playerUnitHeaders[n3][n5] = n6;
                        }
                        continue block13;
                    }
                    this.playerUnitHeaders[n4][n5] = n6;
                    continue block13;
                }
                case 9: {
                    int n4 = this.missionScript[n++] & 0xFF;
                    int n5 = this.missionScript[n++] & 0xFF;
                    this.techFlags[n4] = (byte)n5;
                    break;
                }
                case 6: {
                    int n4 = this.missionScript[n++] & 0xFF;
                    int n5 = this.missionScript[n++] & 0xFF;
                    int n6 = this.missionScript[n++] & 0xFF;
                    int n3 = this.missionScript[n++] & 0xFF;
                    this.a(n4, n5, n6, n3, false);
                    break;
                }
                case 7: {
                    int n7;
                    int n4 = this.missionScript[n] & 0xFF;
                    int n5 = this.missionScript[++n];
                    int n6 = this.missionScript[++n] & 0xFF;
                    int n3 = this.missionScript[++n] & 0xFF;
                    ++n;
                    int n8 = 0;
                    if (n5 == -1) {
                        n7 = 0;
                        while (n7 < this.playerUnitHeaders[n4][2]) {
                            if ((this.playerUnitSlots[n4][n8 + 3] & 0xFF) < 2) {
                                this.playerUnitSlots[n4][n8 + 2] = (short)((n6 << 8 | n3) & 0xFFFF);
                            }
                            ++n7;
                            n8 += 8;
                        }
                        continue block13;
                    }
                    if (n5 < 0) {
                        n5 = -n5;
                        n7 = 0;
                        while (n7 < this.playerUnitHeaders[n4][2]) {
                            if ((this.playerUnitSlots[n4][n8 + 3] & 0xFF) == n5) {
                                this.playerUnitSlots[n4][n8 + 2] = (short)((n6 << 8 | n3) & 0xFFFF);
                            }
                            ++n7;
                            n8 += 8;
                        }
                        continue block13;
                    }
                    this.tickMoveTimer(n4, n << 3);
                    continue block13;
                }
                case 0: {
                    int n4 = this.missionScript[n++] & 0xFF;
                    int n5 = this.missionScript[n++] & 0xFF;
                    this.startMissionBriefing(n4, this.I, n5);
                    break;
                }
                case 1: {
                    int n4 = this.missionScript[n++] & 0xFF;
                    int n5 = this.scriptBlockOffset(n4);
                    if (this.missionScript[n5] >= 0) break;
                    this.missionScript[n5] = (byte)(-this.missionScript[n5]);
                    break;
                }
                case 4: {
                    ++n;
                    this.startMissionBriefing(0, 98, 0);
                    break;
                }
                case 5: {
                    ++n;
                    this.startMissionBriefing(0, 98, 1);
                    break;
                }
                case 2: {
                    int n4 = this.missionScript[n++];
                    int n5 = n2;
                    if (n4 >= 0) {
                        n5 = this.scriptBlockOffset(n4);
                    }
                    if (this.missionScript[n5] <= 0) break;
                    this.missionScript[n5] = (byte)(-this.missionScript[n5]);
                    break;
                }
                case 3: {
                    int n4 = this.missionScript[n++];
                    this.var_int_arr_a[n4] = 0;
                    break;
                }
                case 126: {
                    return n;
                }
            }
        }
    }

    public final int scriptBlockOffset(int n) {
        int n2 = 0;
        while (n > 0) {
            n2 = this.skipScriptBlock(n2);
            --n;
        }
        return n2;
    }

    public final int skipScriptBlock(int n) {
        byte by;
        if ((by = this.missionScript[n++]) < 0) {
            by = (byte)(-by);
        }
        switch (by) {
            case 1: {
                n += 7;
                break;
            }
            case 3: {
                ++n;
                break;
            }
            case 2: {
                n += 2;
                break;
            }
            case 4: {
                n += 5;
                break;
            }
            case 7: {
                n += 4;
                break;
            }
            case 5: 
            case 6: {
                n += 3;
            }
        }
        return this.skipScriptActions(n);
    }

    public final int skipScriptActions(int n) {
        while (true) {
            switch (this.missionScript[n++]) {
                case 6: 
                case 7: {
                    n += 4;
                    break;
                }
                case 8: {
                    n += 3;
                    break;
                }
                case 0: 
                case 9: {
                    n += 2;
                    break;
                }
                case 1: 
                case 2: 
                case 3: 
                case 4: 
                case 5: {
                    ++n;
                    break;
                }
                case 126: {
                    return n;
                }
            }
        }
    }

    final void e(int n, int n2, int n3) {
        if (this.var_int_c >= 15) {
            return;
        }
        int n4 = this.var_int_c * 3;
        this.var_int_arr_b[n4++] = n;
        this.var_int_arr_b[n4++] = n2;
        this.var_int_arr_b[n4] = n3;
        ++this.var_int_c;
    }

    final int int_b(int n, int n2) {
        int n3 = 0;
        for (int i = 0; i < this.var_int_c; ++i) {
            if (n >= 0 && this.var_int_arr_b[n3++] != n || this.var_int_arr_b[n3++] != n2) continue;
            return this.var_int_arr_b[n3];
        }
        return -1;
    }

    public final boolean n(int n) {
        if (this.screenW <= 128) {
            this.clipLeft = 4;
            this.clipRight = this.screenW - 4;
        } else {
            this.clipLeft = 8;
            this.clipRight = this.screenW - 16;
        }
        if (n == 0) {
            // 任务脚本/结算对话框可能在小地图视图（aA=1）里弹出。若如实记 1，
            // 关闭对话框会把用户扔回全蓝小地图（教程/战役对话框连锁时=反复弹回
            // 蓝屏"退不出去"；原版真机节奏下几乎不触发）。统一记世界视图链 8，
            // 小地图随时可再按 0 进。
            this.overlayPrevState = this.screenState == 1 ? 8 : this.screenState;
            if (System.getProperty("aoe.debug") != null) {
                System.out.println("[view] dialog open z=" + this.dialogScriptId + " overlayPrevState=" + this.overlayPrevState);
            }
        }
        this.void_c(this.briefingVariant);
        this.aQ = 0;
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        return true;
    }

    final void void_c(int n) {
        a a2 = new a(this.dialogScriptId);
        String string = a2.a(n);
        int n2 = string.length() - 1;
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[dlg-parse] z=" + this.dialogScriptId + " v=" + n + " strLen=" + string.length()
                + " str=" + string);
        }
        this.var_java_lang_String_arr_a = new String[32];
        int n3 = 3;
        if (this.as == 1) {
            n3 = 1;
        }
        a a3 = new a(99);
        this.var_java_lang_String_arr_a[0] = a3.a(n3);
        this.aW = 1;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        this.aT = 0;
        this.av = 0;
        while (n4 < n2) {
            int n7;
            int n8 = n4;
            while (n4 < n2) {
                char c2 = string.charAt(n4);
                if (c2 == '@' || c2 == ' ') {
                    ++n6;
                    break;
                }
                ++n4;
            }
            if (string.charAt(n4++) == '@') {
                n7 = 0;
                int n9 = 0;
                switch (string.charAt(n4++)) {
                    case 'A': {
                        n9 = 10;
                        break;
                    }
                    case 'V': {
                        n9 = -1000;
                        break;
                    }
                    case '1': {
                        n9 = -1;
                        break;
                    }
                    case '2': {
                        n9 = -2;
                        break;
                    }
                    case '3': {
                        n9 = -3;
                        break;
                    }
                    case '4': {
                        n9 = -4;
                        break;
                    }
                    case '5': {
                        n9 = -5;
                        break;
                    }
                    case '6': {
                        n9 = -6;
                        break;
                    }
                    case '7': {
                        n9 = -7;
                        break;
                    }
                    case 'B': {
                        n9 = -8;
                        break;
                    }
                    case 'C': {
                        n9 = -9;
                        break;
                    }
                    case 'D': {
                        n9 = -10;
                        break;
                    }
                    case 'E': {
                        n9 = -11;
                        break;
                    }
                    case 'F': {
                        n9 = -12;
                        break;
                    }
                    case 'G': {
                        n9 = -13;
                    }
                }
                if (n9 < 0) {
                    this.var_java_lang_String_arr_a[this.aW] = n9 == -1000 ? this.var_java_lang_String_d : ((n9 = -n9) > 7 ? "" + this.var_java_lang_String_arr_b[n9 - 8] : "" + this.var_int_arr_e[n9 - 1]);
                    n7 = this.a(this.var_java_lang_String_arr_a[this.aW++]);
                    this.aT += this.ah + this.ay + 2;
                } else if (n9 == 0) {
                    this.var_java_lang_String_arr_a[this.aW] = string.substring(n5, n4 - 2);
                    n7 = this.a(this.var_java_lang_String_arr_a[this.aW++]);
                    this.aT += this.ah + this.ay + 2;
                } else {
                    this.var_java_lang_String_arr_a[this.aW++] = null;
                    n7 = 64;
                    this.aT += 32 + this.ay + 2 + (this.ah << 1);
                }
                n5 = n4;
                n6 = 0;
                if (this.av >= n7) continue;
                this.av = n7;
                continue;
            }
            if (this.var_javax_microedition_lcdui_Font_a.substringWidth(string, n5, n4 - n5) >= this.clipRight - this.clipLeft) {
                if (n6 < 1) {
                    n5 = n4;
                    n6 = 0;
                    continue;
                }
                this.var_java_lang_String_arr_a[this.aW] = string.substring(n5, n8);
                n7 = this.a(this.var_java_lang_String_arr_a[this.aW++]);
                this.aT += this.ah + this.ay + 2;
                n4 = n5 = n8;
                n6 = 0;
                if (this.av < n7) {
                    this.av = n7;
                }
                if (n4 < n2) continue;
                break;
            }
            if (n4 < n2) continue;
            this.var_java_lang_String_arr_a[this.aW] = string.substring(n5, n2 + 1);
            n7 = this.a(this.var_java_lang_String_arr_a[this.aW++]);
            this.aT += this.ah + this.ay + 2;
            if (this.av >= n7) break;
            this.av = n7;
            break;
        }
        this.var_int_e = 0;
        this.D = this.aT - (this.clipBottom - this.clipTop) + 16 + (this.ah << 1);
        this.keyActionHeld = 0;
        this.keyActionPulse = 0;
    }

    public final void A() {
        this.keyActionHeld = 0;
        this.keyActionPulse = 0;
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        this.clipLeft = 0;
        this.clipRight = this.screenW;
    }

    public final void renderDialog(Graphics graphics) {
        int n;
        int n2 = (this.screenW - this.av >> 1) - 8;
        int n3 = (this.clipBottom - this.clipTop - this.aT >> 1) + this.clipTop - 8;
        int n4 = this.av + 16;
        int n5 = this.aT + 24;
        int n6 = n3 + n5 - 8;
        if (n2 < 0) {
            n2 = 0;
        }
        if (n4 > this.screenW) {
            n4 = this.screenW;
        }
        if (n3 < this.clipTop) {
            n3 = this.clipTop;
        }
        if (n5 > this.clipBottom - this.clipTop) {
            n5 = this.clipBottom - this.clipTop;
        }
        boolean bl = false;
        boolean bl2 = false;
        graphics.setClip(0, 0, this.screenW, this.screenH);
        this.e(graphics, n2, n3, n4, n5);
        int n7 = n2;
        graphics.setClip(n2 += 2, n3 += 6, n4 -= 4, n5 -= 12);
        boolean bl3 = false;
        boolean bl4 = false;
        n3 += 2;
        n3 -= this.var_int_e;
        for (int i = 1; i < this.aW; ++i) {
            if (this.var_java_lang_String_arr_a[i] == null) {
                n = this.screenW >> 1;
                int n8 = n - 32;
                int n9 = n8 + 64;
                int n10 = (n3 += this.ah) + 16;
                int n11 = n3 + 32;
                graphics.setColor(0xFF0000);
                graphics.drawLine(n8, n10, n, n3);
                graphics.drawLine(n9, n10, n, n3);
                graphics.drawLine(n8, n10, n, n11);
                graphics.drawLine(n9, n10, n, n11);
                graphics.drawLine(++n8, n10, n, n3);
                graphics.drawLine(n9, n10, n, n3);
                graphics.drawLine(n8, n10, n, n11);
                graphics.drawLine(n9, n10, n, n11);
                n3 = n11;
                continue;
            }
            n2 = this.screenW - this.a(this.var_java_lang_String_arr_a[i]) >> 1;
            this.a(graphics, this.var_java_lang_String_arr_a[i], n2, n3, this.aQ - i);
            n3 += this.ah + this.ay + 2;
        }
        graphics.setClip(0, 0, this.screenW, this.screenH);
        n4 = this.a(this.var_java_lang_String_arr_a[0]) + 12;
        n2 = n7 - (n4 >> 1);
        n5 = this.ah + 12;
        n3 = n6;
        if (n2 < 0) {
            n2 = 0;
        }
        if (n3 + n5 > this.clipBottom) {
            n3 = this.clipBottom - n5;
        }
        this.e(graphics, n2, n3, n4, n5);
        n = (AgeOfEmpires.b.int_b((this.aQ << 6) + 512) >> 10) + 128;
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.var_java_lang_String_arr_a[0], (n2 += 6) + 1, (n3 += 6 + this.ay) + 1, 20);
        graphics.setColor((n << 16 | n << 4) & 0xFFFF00);
        graphics.drawString(this.var_java_lang_String_arr_a[0], n2, n3, 20);
        if (this.aQ++ > 10) {
            switch (this.keyActionHeld) {
                case 6: 
                case 22: 
                case 38: 
                case 47: {
                    this.pendingScreenState = this.overlayPrevState;
                    this.keyActionHeld = 0;
                    this.keyActionPulse = 0;
                    break;
                }
                case 9: 
                case 25: {
                    this.var_int_e += 4;
                    break;
                }
                case 3: 
                case 19: {
                    this.var_int_e -= 4;
                }
            }
        }
        this.a(graphics, this.screenW - 6 >> 1, n3);
    }

    public final void a(Graphics graphics, int n, int n2) {
        if (this.var_int_e > this.D) {
            this.var_int_e = this.D;
        }
        if (this.var_int_e < 0) {
            this.var_int_e = 0;
        }
        if ((this.tickCount & 1) == 0) {
            return;
        }
        if (this.D > 0) {
            if (this.var_int_e > 0) {
                n2 = this.clipTop + 2;
                this.a(graphics, 21, n, n2, 0, 0, 7, 6, 0, 0);
            }
            if (this.var_int_e < this.D) {
                n2 = this.clipBottom - 8;
                this.a(graphics, 21, n, n2, 7, 0, 7, 6, 0, 0);
            }
        }
    }

    /** 任务结束结算（g(0, 98, n3)，n3==0 为胜利）：推进解锁计数 aj/aG、
     *  写每关高分（nfoHighScores）并持久化 .nfo 字节 28。 */
    // startMissionBriefing(as, dialogScriptId, V)：切到简报/对话框屏。dialogScriptId=对话框脚本 id
    // （62=任务简报 70=难度/链页 74=失败简报 98=胜负结算屏）；V=变体/正文索引
    // （98 结算:0=Victorious 1=Defeated）。已在对话框态时来新简报会重跑解析
    // （链式弹窗 BUG-005 修复点）。
    final void startMissionBriefing(int n, int n2, int n3) {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[trace] g(" + n + "," + n2 + "," + n3 + ") ac=" + this.gameMode);
        }
        if (this.requestStateSwitch(2)) {
            this.as = n;
            this.dialogScriptId = n2;
            this.briefingVariant = n3;
            if (this.screenState == 2 && this.pendingScreenState == 2) {
                // 链式弹窗修复(BUG-005 根因,2026-09-01 第5轮玩家定位):已在对话框态
                // 时来新简报,pending==current 不触发 boolean_a() 的 n(aH) 重入,
                // dialogScriptId/V 换了但正文仍是上一条对话框的解析结果(实测:dlg 70 9 空条目
                // 开着时再发 70 12,画面维持空白)。重跑 n() 的解析段;不动
                // overlayPrevState,关闭链仍应回世界视图。
                this.void_c(this.briefingVariant);
                this.aQ = 0;
                this.var_boolean_f = true;
                this.var_boolean_b = true;
            }
            if (n2 == 98) {
                // -Daoe.exitOnResult=1：批量实验终局信号。无条件打印（不受
                // aoe.debug 门控，同 [paint] 的做法）并立即退出——与批量脚本的
                // 契约，格式固定勿改。放在 nfo 写回/screenState=12 之前：批跑
                // 不需要结算屏，也顺带跳过随机图种子写回 nfo。
                if (System.getProperty("aoe.exitOnResult") != null) {
                    System.out.println("[result] " + (n3 == 0 ? "WIN" : "LOSS") + " ticks=" + this.tickCount);
                    System.out.flush();
                    System.exit(0);
                }
                this.clipTop = 0;
                this.clipLeft = 0;
                this.clipBottom = this.screenH;
                this.clipRight = this.screenW;
                this.requestMedia(132 + n3, false);
                this.loadNfo();
                switch (this.gameMode) {
                    case 0: {
                        this.nfoWriteInt(31, 2, rngStateHi << 8 | rngStateLo);
                        this.screenState = 12;
                        this.menuScreenId = 1;
                        break;
                    }
                    case 16: {
                        if (n3 == 0 && this.tutorialProgress == this.missionIndex) {
                            if (this.tutorialProgress < 2) {
                                ++this.tutorialProgress;
                                this.menuScreenId = 11;
                            } else {
                                this.briefingVariant = 2;
                                this.menuScreenId = 1;
                            }
                        }
                        this.screenState = 4;
                        break;
                    }
                    case 32: {
                        if (this.campaignProgress < 6) {
                            if (n3 == 0 && this.campaignProgress < this.missionIndex + 1) {
                                this.campaignProgress = this.missionIndex + 1;
                            }
                            this.menuScreenId = 12;
                        } else {
                            this.menuScreenId = 1;
                        }
                        int n4 = this.playerUnitHeaders[0][90] * 3;
                        n4 += this.playerUnitHeaders[1][87] * 124;
                        n4 += this.playerUnitHeaders[0][86] * 421;
                        n4 -= this.playerUnitHeaders[0][87] * 9;
                        if (this.nfoHighScores[this.missionIndex] < (n4 -= this.playerUnitHeaders[1][86] * 12)) {
                            this.nfoHighScores[this.missionIndex] = n4;
                            this.nfoWriteInt(0 + (this.missionIndex << 2), 4, n4);
                            this.saveNfo();
                        }
                        this.screenState = 12;
                    }
                }
                if (n3 == 0) {
                    this.nfoWriteInt(28, 1, this.tutorialProgress << 4 | this.campaignProgress);
                    this.saveNfo();
                }
            }
        }
    }

    final int a(String string) {
        if (string == null) {
            return 0;
        }
        return this.var_javax_microedition_lcdui_Font_a.stringWidth(string);
    }

    static {
        rngStateLo = 45;
    }
}

