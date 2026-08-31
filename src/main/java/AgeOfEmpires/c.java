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
    public int aO;
    public int var_int_j;
    public int ad;
    public int J;
    public int aI;
    public int aB;
    public int aL;
    public int var_int_f;
    public int af;
    public int u;
    public boolean var_boolean_f = false;
    public boolean var_boolean_b = false;
    public int[] var_int_arr_d;
    public String[] var_java_lang_String_arr_b;
    public String var_java_lang_String_b;
    public byte var_byte_a = 0;
    public int ah;
    public int ay;
    public Font var_javax_microedition_lcdui_Font_a;
    public int aM = 256;
    public short var_short_a;
    public short var_short_b;
    public short var_short_c;
    public int[][] var_int_arr_arr_b;
    public short[][] var_short_arr_arr_b;
    public short[] var_short_arr_a;
    public int[][] var_int_arr_arr_a;
    // —— 状态机（速查见 GAME_NOTES.md，完整语义未完全考证，改动前对照 p() 的分发）——
    // aA: 顶层画面状态，p() 按它分发渲染；E() 在 aA 变化时构建新画面。
    // am: 配套的"当前画面"值（boolean_g(n) 直接设置），am==aA 表示画面稳定。
    public int aA;
    public int am;
    public int aH;
    public boolean var_boolean_k;
    public int R;
    // ac: 游戏模式（由菜单脚本动作 65/71/73 设置）：0=教程，16=随机地图，32=战役。
    // aC: 当前选中的任务序号（0 起；战役 0..6，随机地图 0..2）。
    public int ac;
    public int aC;
    public int z;
    public int I;
    public int[] var_int_arr_e;
    // 音效开关（存档字节 29：0=开）。
    public boolean var_boolean_d = true;
    // 解锁进度：战役可选上限 = aj+1（通关后 aj = aC+1，上限 6，共 7 关）；
    // 随机地图可选上限 = aG+1（上限 2，共 3 档）。打包存进 .nfo 字节 28 = aG<<4|aj。
    // （移植已改：选关屏上限固定全解锁，见 boolean_d 的 H-case 11/12。）
    public int aj;
    public int aG;
    // ar: tick 计数（每帧 +1，80ms 一帧；调试日志里的 ar 就是它）。
    public int ar;
    public boolean var_boolean_h;
    public boolean var_boolean_j;
    public byte[] var_byte_arr_c;
    public int y;
    public int N;
    public int az;
    public int al;
    public int Q;
    public int t;
    public int aa;
    public int aV;
    public int aK;
    public String[] var_java_lang_String_arr_a;
    public int as;
    public int V;
    public int aN;
    public int aW;
    public int aT;
    public int av;
    public int var_int_e;
    public int D;
    public int var_int_b;
    public int p;
    public int aE;
    public int var_int_h;
    public int aJ;
    public int Y;
    public int var_int_i;
    public int aQ;
    public Graphics var_javax_microedition_lcdui_Graphics_a;
    public byte[] var_byte_arr_d;
    public byte[] var_byte_arr_b;
    public byte[] var_byte_arr_h;
    public boolean var_boolean_e = false;
    // .nfo 存档原始字节（314B，m() 读写 RecordStore，int_a(...)/h(...) 存取）：
    // [0..27) = 7 个 4 字节大端战役高分（var_int_arr_d）；[28] = aG<<4|aj；
    // [29] = 音效开关；[30] = 另一开关（AgeOfEmpires.b.c，含义未考证）。
    public byte[] var_byte_arr_f;
    // —— 按键输入状态（键位表 void_b(129) 从 data.res #129 加载）——
    // var_byte_arr_k: 键码→动作码映射表，ak = 表长/2。void_a（按下）查表置
    // ae（瞬时值）和 ab=ax（持续值）；游戏逻辑以 "ab != 0" 为"有键按住"
    // （如 o() 开头 if (ab == 0) return），自身从不清 ab，依赖松开事件
    // void_e 全清。桌面适配层把松开延迟到 paint 之后投递，见 void_e 注释。
    public int ak;
    public byte[] var_byte_arr_k;
    public int ae;
    // volatile：按键状态由 EDT/dev 线程写、游戏 tick 线程读，ARM 弱内存模型下
    // 无同步的跨线程写入可能长期不可见（dev 自动导航曾因此失灵）。
    public volatile int ax;
    public volatile int ab;
    public String var_java_lang_String_d;
    public int ag;
    public String var_java_lang_String_a;
    public String var_java_lang_String_c;
    public int G;
    public int x;
    public boolean var_boolean_a;
    public boolean var_boolean_g;
    public int F;
    public int B;
    public int O;
    public int P;
    public boolean var_boolean_l;
    public int W;
    public int X;
    public int[] var_int_arr_c;
    public int T;
    public int aD;
    public byte[] var_byte_arr_e;
    public int var_int_g;
    public int var_int_c;
    public int[] var_int_arr_b;
    public byte[] var_byte_arr_a;
    public int[] var_int_arr_a;
    public int var_int_d;
    public int S;
    public int au;
    public int U;
    public int aP;
    public int aS;
    public static int r = 12;
    public static int var_int_k;
    d var_AgeOfEmpires_d_a;
    public int aF;
    // 菜单/对话框模板字节：a(n, true) 加载 data.res 对话框资源（131=选关）。
    // 树状节点结构，int_c/int_e/int_k/int_i 遍历；节点参数在 +9 起
    // （循环器控件：+9+1 = 选项总数，+9+2 = 当前选中项）。item 激活时执行
    // 一段脚本，动作码见 1225 行附近的 switch（65=教程/71=随机地图/
    // 73=战役/67,72=设置开关…）。
    public byte[] var_byte_arr_i;
    // H: 子状态/对话框选择器，boolean_d 按 H 加载对应菜单并打补丁：
    // 11=随机地图选关，12=战役选关，1=回主菜单。
    public int H;
    public int v;
    public int ap;
    public int aR;
    public int ao;
    public int Z;
    public int var_int_a;
    public int K;
    public boolean var_boolean_i = true;
    public int an;
    public int E = 0;
    public int ai;
    public int M;
    public int q;
    public int var_int_l;
    public int aq;
    public int C;
    public int w;
    public int aw;
    public Image[] var_javax_microedition_lcdui_Image_arr_a;
    // 自动重复：ab==L 视为"按住未换键"，s 每 tick +1，s>=5 后动作持续生效。
    public int s;
    public int L;
    public int aU;
    public int A;
    public int at;
    public int n;
    public int m = 100000;
    public int o;
    public boolean var_boolean_c;
    public byte[] var_byte_arr_g;
    public short[][] var_short_arr_arr_a;
    public byte[] var_byte_arr_j = new byte[]{11, 2, 10, 1, 12, 2, -1, 11, 3, 12, 5, 6, 1, 7, 1, 5, 1, 10, 1, -1, 11, 5, 12, 5, 8, 1, 7, 1, 10, 1, 2, 1, -1, 11, 5, 8, 1, 12, 5, 7, 1, 3, 1, 2, 1, 10, 1, 10, 1, -2};

    c(AoeMidlet ageOfEmpires) {
        super(ageOfEmpires, 134, 0);
        this.var_AoeMidlet_a = ageOfEmpires;
        this.var_java_lang_String_d = "V" + ageOfEmpires.getAppProperty("MIDlet-Version");
        com.ulysseo.mad.c.void_a(0);
        this.m = 100000;
    }

    public final void w() {
        if (this.var_boolean_e) {
            this.var_AoeMidlet_a.a();
        }
    }

    public final synchronized void k() {
        if (this.ar < 20 || !AoeMidlet.var_boolean_a) {
            return;
        }
        this.af = 0;
        this.var_boolean_l = true;
        AgeOfEmpires.b.var_boolean_a = true;
    }

    public final synchronized void q() {
        if (this.ar < 20 || !AoeMidlet.var_boolean_a) {
            return;
        }
        AgeOfEmpires.b.c();
        AgeOfEmpires.b.var_boolean_a = true;
        this.s();
        this.var_boolean_j = true;
        this.ab = 0;
    }

    public final void h() {
        var_com_ulysseo_mad_b_a.a();
    }

    public final int int_b() {
        AgeOfEmpires.b.void_a();
        this.ae = 0;
        this.void_b(129);
        this.var_byte_arr_f = null;
        this.var_byte_arr_f = new byte[314];
        this.m();
        this.e();
        this.var_int_arr_arr_a = new int[2][91];
        this.var_short_arr_arr_a = new short[2][208];
        this.var_int_arr_arr_b = new int[2][88];
        this.var_short_arr_arr_b = new short[2][20];
        this.var_short_arr_a = new short[4096];
        this.var_javax_microedition_lcdui_Font_a = Font.getDefaultFont();
        this.ah = 19;
        this.ay = -2;
        this.var_int_f = 19;
        this.m(var_com_ulysseo_mad_b_a.getWidth(), var_com_ulysseo_mad_b_a.getHeight());
        this.var_byte_arr_i = com.ulysseo.mad.c.byte_arr_a(117);
        this.am = 4;
        this.H = 2;
        this.ar = 0;
        this.var_int_arr_d = new int[7];
        this.var_int_arr_e = new int[7];
        this.var_int_arr_a = new int[4];
        this.m();
        int n = this.int_a(28, 1);
        this.aG = n >> 4;
        this.aj = n & 0xF;
        this.var_boolean_d = this.var_byte_arr_f[29] == 0;
        AgeOfEmpires.b.c = this.var_byte_arr_f[30] == 0;
        for (int i = 0; i < 7; ++i) {
            this.var_int_arr_d[i] = this.int_a(0 + (i << 2), 4);
        }
        this.R = 0;
        return -1;
    }

    public final void void_b() {
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
        if (this.ar - this.u >= 50) {
            this.u = this.ar;
            this.m = 510;
        }
        this.ag = 20;
        this.var_java_lang_String_a = null;
        this.var_java_lang_String_c = null;
        a a2 = new a(99);
        this.var_java_lang_String_a = a2.a(5);
        this.var_java_lang_String_c = a2.a(4);
    }

    public final void a(int n, boolean bl) {
        if (this.o == n) {
            return;
        }
        AgeOfEmpires.b.var_boolean_b = true;
        this.o = n;
        this.var_boolean_c = bl;
    }

    public final void c() {
        int n = AgeOfEmpires.c.int_a() % 6;
        int[] nArray = new int[]{204000, 123000, 233000, 180000, 188000, 190000, 143000, 197000, 184000, 175000};
        this.m = nArray[n] / 80;
        this.a(n + 3 + 131, false);
    }

    public final void void_a(int n) {
        if (n > 127) {
            n = -(n - 128);
        }
        int n2 = n;
        this.ae = 0;
        for (int i = 0; i < this.ak; ++i) {
            if (n2 != this.var_byte_arr_k[i << 1]) continue;
            this.ae = this.var_byte_arr_k[(i << 1) + 1];
            break;
        }
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[void_a] key=" + n + " ae=" + this.ae + " ak=" + this.ak);
        }
        this.ax = this.ab = this.ae;
    }
    public final void void_e(int n) {
        // 原版语义：松开即全清。桌面适配层（Canvas）会把松开事件延迟到 paint
        // 之后才投递到这里，保证按下至少被游戏完整消费一帧，快速点按不会被吞。
        this.ae = 0;
        this.L = 0;
        this.ab = 0;
        this.ax = 0;
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
                    if (this.aA == 6) {
                        if (++stable >= 15) {
                            break;
                        }
                    } else {
                        stable = 0;
                    }
                }
                if (this.aA != 6 || !this.devLoadFrom(path)) {
                    System.out.println("[devBoot] failed, aA=" + this.aA);
                    return;
                }
                long until = System.currentTimeMillis() + 3000;
                while (this.devPendingRestore != null && System.currentTimeMillis() < until) {
                    Thread.sleep(50);
                }
                System.out.println("[devBoot] done, aA=" + this.aA);
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
            devPress(-5);                       // 主菜单：Play
            if (campaign || random) {
                devPress(-4);                   // 循环器右切（Tutorial→Campaign→Random）
            }
            // Game Mode → （选关等中间屏）→ 任务装载：菜单链长度随模式/版本
            // 有差异（有的屏高亮项脚本是空操作），连按 FIRE 直到离开菜单态。
            long deadline = System.currentTimeMillis() + 30000;
            while (this.aA == 4 && System.currentTimeMillis() < deadline) {
                if (mission > 1 && devHiScriptOp() == 3) {
                    for (int i = 1; i < mission; ++i) {
                        devPress(-4);           // 选关循环器右切（仅限 op=3 的选关项）
                    }
                }
                devPress(-5);
            }
            deadline = System.currentTimeMillis() + 30000;
            while (this.aA != 6 && System.currentTimeMillis() < deadline) {
                if (this.aA == 2) {
                    this.void_a(-6);            // F1 推简报/教程对话框（周期重注=自带重试）
                }
                Thread.sleep(600);
            }
            System.out.println("[dev] in mission, aA=" + this.aA);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 菜单指针快照：aA / ao / Z / aR + （高亮项是循环器时）循环器位置字节。
     *  几乎任何菜单移动、换屏、循环切换都会改变其中之一，用作注入"被消费"的观测信号。 */
    private long devSig() {
        long sig = ((long) this.aA & 0xFF) << 48
            | ((long) this.ao & 0xFFFF) << 32
            | ((long) this.Z & 0xFFFF) << 16
            | ((long) this.aR & 0xFFFF);
        try {
            if (this.var_byte_arr_i != null && this.Z >= 0 && this.Z < this.ao) {
                int node = this.int_c(this.Z);
                if (node + 11 < this.var_byte_arr_i.length
                        && (this.var_byte_arr_i[node + 8] & 0xF) == 2) {
                    sig |= (1L << 63)
                        | ((long) (this.var_byte_arr_i[node + 11] & 0xFF & 0x7F) << 56);
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
            if (this.var_byte_arr_i == null || this.Z < 0 || this.Z >= this.ao) {
                return -1;
            }
            int node = this.int_c(this.Z);
            if ((node + 12 < this.var_byte_arr_i.length)
                    && (this.var_byte_arr_i[node + 8] & 0xF) == 2) {
                return this.var_byte_arr_i[this.int_k(node)] & 0xFF;
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
            this.void_a(key);
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

    /** 等画面状态稳定（aA 连续 ~1.2s 不变），最长 6s——规避状态切换中按键被吞。 */
    private void devWaitStable() throws InterruptedException {
        int last = -999;
        int same = 0;
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
            if (this.aA == last) {
                if (++same >= 6) {
                    return;
                }
            } else {
                last = this.aA;
                same = 0;
            }
        }
    }

    // ===== dev 效率工具：autoDismiss / HUD / 快照存档（F5/F9、FIFO、自动 checkpoint）=====
    private static final boolean DEV_AUTO_DISMISS = System.getProperty("aoe.autoDismiss") != null;
    static final boolean DEV_HUD = System.getProperty("aoe.devHud") != null;
    private static final boolean DEV_AUTO_CHECKPOINT =
        !"-".equals(System.getProperty("aoe.autoCheckpoint", "1"));
    /** 对话框自动推进：任务里进过一次主视图后，aA==2 的弹窗每 4 帧补一个左软键。 */
    private boolean devMissionSeen;
    private int devDismissFrames;
    /** 任务内连续帧计数：到 25（≈2s）做一次开局自动 checkpoint。 */
    private int devStableFrames;
    private volatile String devPendingSavePath;
    private volatile byte[] devPendingRestore;
    /** 本次会话进入当前任务用的 nav spec（"-Daoe.dev" 语义）；窗口会话为 null。 */
    public String devLastNavSpec;
    private String devToast;
    private long devToastUntil;

    static String devSaveDir() {
        return System.getProperty("aoe.saveDir",
            System.getProperty("user.home") + "/Library/Application Support/AoeJ2ME/saves");
    }

    private void devToast(String s) {
        this.devToast = s;
        this.devToastUntil = System.currentTimeMillis() + 2000;
    }

    /** 快存。实际写盘在 EDT 帧首（p()），避免与 tick/渲染竞态产生撕裂快照。 */
    public boolean devSaveTo(String path) {
        if (this.aA != 6) {
            System.out.println("[save] refused, aA=" + this.aA);
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
            if (this.aA == 6 && (id[0] != this.ac || id[1] != this.aF || id[2] != this.nfoMissionId())) {
                System.out.println("[load] mission mismatch: save ac=" + id[0] + " aF=" + id[1]
                    + " nfo=" + id[2] + " / now ac=" + this.ac + " aF=" + this.aF + " nfo=" + this.nfoMissionId());
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
        return this.var_byte_arr_f != null && this.var_byte_arr_f.length > 32
            ? ((this.var_byte_arr_f[31] & 0xFF) << 8 | this.var_byte_arr_f[32] & 0xFF) : 0;
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
        if (this.aA == 6) {
            this.devMissionSeen = true;
        } else if (this.aA == 4) {
            this.devMissionSeen = false;
        }
        if (DEV_AUTO_DISMISS && this.devMissionSeen && this.aA == 2
                && ++this.devDismissFrames >= 4) {
            this.devDismissFrames = 0;
            this.void_a(-6);
        }
        if (this.aA != 6) {
            this.devStableFrames = 0;
        } else if (this.devStableFrames < 25) {
            if (++this.devStableFrames == 25 && DEV_AUTO_CHECKPOINT) {
                this.devSaveTo(devSaveDir() + "/auto.aoesave");
            }
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
                System.out.println("[save] wrote " + path + " (" + data.length + "B)");
                this.devToast("Saved");
            } catch (Exception e) {
                System.out.println("[save] " + path + ": " + e);
                this.devToast("Save failed");
            }
        }
        if (this.devPendingRestore != null) {
            byte[] data = this.devPendingRestore;
            this.devPendingRestore = null;
            try {
                aoe.SaveState.apply(this, data);
                System.out.println("[load] applied (" + data.length + "B)");
                this.devToast("Loaded");
            } catch (Exception e) {
                System.out.println("[load] apply: " + e);
                this.devToast("Load failed");
            }
        }
    }

    // ===== dev 鼠标驱动（-Daoe.devMouse=<fifo 路径>）=====
    /** 从 FIFO 逐行读指令直接喂 mouseA/void_a（逻辑坐标 240x320）。宿主终端缺
     *  辅助功能/屏幕录制授权、无法注入真实 CGEvent 时，用它在游戏层驱动和验证
     *  鼠标逻辑；dump 指令同步导出帧缓冲（配合截图验证渲染结果）。
     *  指令：move x y | press x y | release x y | click x y | rclick x y |
     *        drag x1 y1 x2 y2 | key <J2ME键码> | dump <png路径> | exit
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
    private boolean devInScript;

    /** 执行一条 FIFO 指令；返回 false 表示 exit。 */
    private boolean devMouseCmd(String line) {
        String[] p = line.split("\\s+");
        try {
            switch (p[0]) {
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
                    break;
                }
                case "key": {
                    int kc = Integer.parseInt(p[1]);
                    this.void_a(kc);
                    // 注入即松开（延迟队列）：否则 ab 永久保持，o()/e() 的按住重复
                    // 会让方向键每帧漂移一格，FIRE 也会被当成持续按住。
                    var_com_ulysseo_mad_b_a.queueSyntheticKeyRelease(kc);
                    break;
                }
                case "state": {
                    System.out.println("[devMouse] aA=" + this.aA + "/" + this.am
                        + " cursor=(" + this.aa + "," + this.aV + ") cam=(" + this.y + "," + this.N + ")"
                        + " Q=" + this.Q + " aE=" + this.aE + " Y=" + this.Y + " sel=" + this.var_int_b);
                    StringBuilder json = new StringBuilder(512);
                    json.append("{\"aA\":").append(this.aA).append(",\"am\":").append(this.am)
                        .append(",\"ar\":").append(this.ar)
                        .append(",\"cursor\":[").append(this.aa).append(',').append(this.aV).append(']')
                        .append(",\"cam\":[").append(this.y).append(',').append(this.N).append(']')
                        .append(",\"sel\":").append(this.var_int_b).append(",\"aE\":").append(this.aE);
                    if (this.var_short_arr_a != null) {
                        int explored = 0;
                        for (int i = 0; i < this.var_short_arr_a.length; ++i) {
                            if ((this.var_short_arr_a[i] & 0x8000) == 0) {
                                ++explored;
                            }
                        }
                        json.append(",\"explored\":").append(explored)
                            .append(",\"tiles\":").append(this.var_short_arr_a.length);
                    }
                    json.append(",\"units\":[");
                    int cnt = this.var_int_arr_arr_a[0][2];
                    for (int i = 0; i < cnt && i < 16; ++i) {
                        int off = i * 8;
                        if (i > 0) {
                            json.append(',');
                        }
                        json.append("{\"tile\":[").append(this.var_short_arr_arr_a[0][off] >>> 8)
                            .append(',').append(this.var_short_arr_arr_a[0][off] & 0xFF)
                            .append("],\"type\":").append(this.var_short_arr_arr_a[0][off + 3] & 0xFF)
                            .append(",\"sel\":").append((this.var_short_arr_arr_a[0][off + 4] & 0x8000) != 0)
                            .append('}');
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
                    for (int i = 0; i < cnt && i < 16; ++i) {
                        int off = i * 8;
                        System.out.println("[devMouse] unit " + i
                            + " tile=(" + (this.var_short_arr_arr_a[0][off] >>> 8)
                            + "," + (this.var_short_arr_arr_a[0][off] & 0xFF) + ")"
                            + " type=" + (this.var_short_arr_arr_a[0][off + 3] & 0xFF)
                            + " sel=" + ((this.var_short_arr_arr_a[0][off + 4] & 0x8000) != 0));
                    }
                    break;
                }
                case "until": {
                    // 阻塞等 aA 到目标值（把"发命令→sleep→grep 日志"循环挪进游戏进程）
                    int target = Integer.parseInt(p[1]);
                    long timeout = p.length > 2 ? Long.parseLong(p[2]) * 1000L : 30000L;
                    long start = System.currentTimeMillis();
                    while (this.aA != target && System.currentTimeMillis() - start < timeout) {
                        Thread.sleep(100);
                    }
                    System.out.println("[devMouse] until aA=" + target + ": "
                        + (this.aA == target ? "ok" : "timeout") + " after "
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
                    this.devSaveTo(p.length > 1 ? p[1] : devSaveDir() + "/quick.aoesave");
                    break;
                case "load":
                    this.devLoadFrom(p.length > 1 ? p[1] : devSaveDir() + "/quick.aoesave");
                    Thread.sleep(300);      // 等帧首 EDT 应用
                    break;
                case "dump":
                    var_com_ulysseo_mad_b_a.dumpFramebuffer(p[1]);
                    System.out.println("[devMouse] dumped " + p[1]);
                    break;
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

    public final void void_b(int n) {
        this.var_byte_arr_k = com.ulysseo.mad.c.byte_arr_a(n);
        this.ak = this.var_byte_arr_k.length >> 1;
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
            System.out.println("[mouseA] kind=" + kind + " aA=" + this.aA);
        }
        if (this.aA != 6 && this.aA != 1) {
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
                        this.aa = tile & 63;
                        this.aV = tile >> 6;
                        this.mouseApplyQ();
                    }
                    // 走键盘 FIRE 管道；松开走延迟队列（复用防吞点按机制）
                    this.void_a(-5);
                    var_com_ulysseo_mad_b_a.queueSyntheticKeyRelease(-5);
                }
                this.mouseBandActive = false;
                break;
            }
            case 3: {
                if (this.aA != 6) {
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
        int idx = this.aa + (this.aV << 6) & 0xFFF;
        if ((idx & 0xFFFFF000) != 0 || (this.var_short_arr_a[idx] & 0xFFF) == 768) {
            this.Q = -1;
        } else {
            this.Q = idx;
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
        if (this.aA == 6 && this.mouseScreenX >= 0) {
            final int edge = 14;
            int tx = this.aa;
            int ty = this.aV;
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
            if ((tx != this.aa || ty != this.aV) && (++this.mouseEdgeTick & 1) == 0) {
                this.aa = Math.max(0, Math.min(63, tx));
                this.aV = Math.max(0, Math.min(63, ty));
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
        this.H();
        int count = this.var_int_arr_arr_a[0][2];
        int firstType = -1;
        int selected = 0;
        int off = 0;
        boolean dbg = System.getProperty("aoe.debug") != null;
        for (int i = 0; i < count; ++i, off += 8) {
            int packed = this.var_short_arr_arr_a[0][off + 0];
            int utx = packed >>> 8;
            int uty = packed & 0xFF;
            if (dbg) {
                System.out.println("[band] unit " + i + " tile=" + utx + "," + uty);
            }
            if (utx >= tx1 && utx <= tx2 && uty >= ty1 && uty <= ty2) {
                this.var_short_arr_arr_a[0][off + 4] |= 0x8000;
                ++selected;
                if (firstType < 0) {
                    firstType = this.var_short_arr_arr_a[0][off + 3] & 0xFF;
                }
            }
        }
        if (dbg) {
            System.out.println("[band] rect (" + tx1 + "," + ty1 + ")-(" + tx2 + "," + ty2
                + ") count=" + count + " selected=" + selected);
        }
        if (selected > 0) {
            this.aE = 512;
            this.Y = 0;
            this.aJ = -1;
            this.var_int_h = firstType;
            this.var_boolean_h = true;
            this.var_int_b = 6;
        }
    }

    /** 右键：有本方选中单位 → 全体移动到鼠标格（与游戏命令路径一致，含落点标记）；
     *  无选中 → 清除选择（等价游戏自身的取消）。 */
    private void mouseCommand() {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[rcmd] Y=" + this.Y + " aE=" + this.aE
                + " lastTile=" + this.mouseLastTile + " sel=" + this.var_int_b);
        }
        if (this.Y == 0 && this.aE == 512 && this.mouseLastTile >= 0) {
            int tx = this.mouseLastTile & 63;
            int ty = this.mouseLastTile >> 6;
            this.var_int_d = this.y + this.aP;
            this.S = this.N + this.aS;
            this.au = this.var_int_j;
            this.U = 8;
            this.d(0, tx, ty);
        } else {
            this.H();
            this.var_int_b = 0;
        }
    }
    // ===== 桌面鼠标增强结束 =====

    public final void commandAction(Command command, Displayable displayable) {
        this.void_a(command.getCommandType());
    }

    public final void m() {
        try {
            RecordStore recordStore = RecordStore.openRecordStore((String)".nfo", (boolean)true);
            if (recordStore.getNumRecords() == 0) {
                recordStore.addRecord(this.var_byte_arr_f, 0, this.var_byte_arr_f.length);
            } else {
                this.var_byte_arr_f = recordStore.getRecord(1);
            }
            recordStore.closeRecordStore();
            return;
        }
        catch (RecordStoreException recordStoreException) {
            return;
        }
    }

    public final void I() {
        try {
            RecordStore recordStore = RecordStore.openRecordStore((String)".nfo", (boolean)true);
            if (recordStore.getNumRecords() == 0) {
                recordStore.addRecord(this.var_byte_arr_f, 0, this.var_byte_arr_f.length);
            } else {
                recordStore.setRecord(1, this.var_byte_arr_f, 0, this.var_byte_arr_f.length);
            }
            recordStore.closeRecordStore();
            return;
        }
        catch (RecordStoreException recordStoreException) {
            return;
        }
    }

    public final int int_a(int n, int n2) {
        int n3 = 0;
        while (--n2 >= 0) {
            n3 <<= 8;
            n3 |= this.var_byte_arr_f[n + n2] & 0xFF;
        }
        return n3;
    }

    public final void h(int n, int n2, int n3) {
        int n4 = n3;
        while (n2 > 0) {
            this.var_byte_arr_f[n++] = (byte)(n4 & 0xFF);
            n4 >>= 8;
            --n2;
        }
    }

    /** 主循环体：框架每 80ms 调 paint → 这里（paint-driven），先推进游戏逻辑再渲染，
     *  ar 计帧。屏幕切换按 aA 分发（见 GAME_NOTES.md 的状态机速查）。 */
    /** dev HUD（-Daoe.devHud=1）：画面顶部两行状态，dump 截图自描述。 */
    private void devDrawHud(Graphics graphics) {
        graphics.setColor(0);
        graphics.fillRect(0, 27, this.aO, 26);
        graphics.setColor(0xFFFFFF);
        String line1 = "aA=" + this.aA + "/" + this.am + " ar=" + this.ar;
        if (this.var_byte_arr_i != null) {
            line1 += " node=" + this.aR + " Z=" + this.Z + "/" + this.ao + " H=" + this.H;
        }
        String pick = this.mouseLastTile < 0 ? "-"
            : (this.mouseLastTile & 63) + "," + (this.mouseLastTile >> 6);
        String line2 = "cur=" + this.aa + "," + this.aV + " cam=" + this.y + "," + this.N
            + " pick=" + pick + " sel=" + this.var_int_b + "/" + (this.aE & 0xFFFF);
        graphics.drawString(line1, 2, 28, 20);
        graphics.drawString(line2, 2, 40, 20);
    }

    /** 存/读档结果提示（居中，2s）。 */
    private void devDrawToast(Graphics graphics) {
        if (this.devToast == null || System.currentTimeMillis() >= this.devToastUntil) {
            return;
        }
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        graphics.setColor(0);
        graphics.fillRect(50, 46, 140, 16);
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.devToast, 120, 49, 17);    // TOP|HCENTER
    }

    public final void p(Graphics graphics) {
        ++this.ar;
        this.devFrameHousekeeping();
        if (this.var_boolean_j) {
            if (Runtime.getRuntime().freeMemory() < 50000L) {
                return;
            }
            this.var_boolean_j = false;
        }
        if (AgeOfEmpires.b.var_boolean_a) {
            AgeOfEmpires.b.var_boolean_b = true;
            this.F = 0;
            this.O = 0;
            this.B = this.aO;
            this.P = this.var_int_j;
            graphics.setClip(0, 0, this.aO, this.var_int_j);
            graphics.setColor(0);
            graphics.fillRect(0, 0, this.aO, this.var_int_j);
            this.O = 0;
            this.P = this.var_int_j;
            AgeOfEmpires.b.var_boolean_a = false;
            this.s();
            this.var_boolean_h = false;
            this.var_boolean_j = true;
            switch (this.aA) {
                case 0: 
                case 10: 
                case 12: {
                    return;
                }
                case 2: {
                    if (this.aN == 1) {
                        this.e(graphics);
                    } else if (this.aN == 4 && this.z != 98) {
                        this.f(graphics);
                    } else {
                        this.j(graphics);
                    }
                    this.F = 0;
                    this.O = 0;
                    this.B = this.aO;
                    this.P = this.var_int_j;
                    return;
                }
                case 1: {
                    this.boolean_j(0);
                    return;
                }
                case 11: {
                    this.boolean_c(0);
                    this.aQ = 0;
                    return;
                }
                case 4: 
                case 9: {
                    this.am = 4;
                    this.aA = 9;
                    return;
                }
            }
            this.j(graphics);
            this.c(graphics);
            return;
        }
        try {
            boolean bl = false;
            graphics.setFont(this.var_javax_microedition_lcdui_Font_a);
            graphics.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
            if (this.boolean_a()) {
                this.n(graphics);
                graphics.setClip(0, 0, this.aO, this.var_int_j);
                if (DEV_HUD) {
                    this.devDrawHud(graphics);
                }
                this.devDrawToast(graphics);
                if (this.var_boolean_f) {
                    this.a(graphics, 21, 4, this.var_int_j - 6, 14, 0, 7, 6, 0, 0);
                }
                if (this.var_boolean_b) {
                    this.a(graphics, 21, this.aO - 10, this.var_int_j - 6, 21, 0, 7, 6, 0, 0);
                }
                switch (this.aA) {
                    case 7: {
                        this.F();
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
                        if (this.m-- <= 0) {
                            this.c();
                        }
                        this.g();
                        this.p();
                        this.B();
                        this.G();
                        this.J();
                        this.z();
                        this.j();
                        this.F();
                        if (this.ag <= 0) break;
                        if (this.var_java_lang_String_a != null) {
                            graphics.setColor(0);
                            graphics.drawString(this.var_java_lang_String_a, this.F + 2, this.P - (this.ah - this.ay), 20);
                            graphics.setColor(0xFFFFFF);
                            graphics.drawString(this.var_java_lang_String_a, this.F + 1, this.P - (this.ah - this.ay + 1), 20);
                        }
                        if (this.var_java_lang_String_c != null) {
                            graphics.setColor(0);
                            graphics.drawString(this.var_java_lang_String_c, this.F + 2, this.P - ((this.ah << 1) - this.ay), 20);
                            graphics.setColor(0xFFFFFF);
                            graphics.drawString(this.var_java_lang_String_c, this.F + 1, this.P - ((this.ah << 1) - this.ay + 1), 20);
                        }
                        --this.ag;
                        if (this.ag != 0) break;
                        this.var_java_lang_String_a = null;
                        this.var_java_lang_String_c = null;
                    }
                }
                if (this.P != this.var_int_j && this.aA == 6 || this.aA == 7 && this.W != 0) {
                    int n = 0;
                    int n2 = this.aL;
                    int n3 = this.aO - 41;
                    int n4 = this.a(this.var_java_lang_String_b);
                    graphics.setClip(41, n2, n3, this.var_int_f);
                    graphics.setColor(0);
                    graphics.fillRect(41, n2, n3, this.var_int_f);
                    n = n4 > n3 ? 41 - (this.R++ << 1) % (n3 + n4) : 41 - n3;
                    graphics.setColor(0xFFFFFF);
                    graphics.drawString(this.var_java_lang_String_b, n + n3, n2 + this.ay, 20);
                }
                this.E();
            }
            if (AgeOfEmpires.b.var_boolean_b) {
                if (!AgeOfEmpires.b.c) {
                    AgeOfEmpires.b.var_boolean_b = false;
                    return;
                }
                if (this.o < 0) {
                    return;
                }
                if (Runtime.getRuntime().freeMemory() >= 30000L && AgeOfEmpires.b.a(this.o, this.var_boolean_c)) {
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
            if (System.getProperty("aoe.debug") != null) {
                exception.printStackTrace();
            }
            return;
        }
    }

    final boolean boolean_a() {
        if (this.am == this.aA) {
            return true;
        }
        boolean bl = false;
        switch (this.am) {
            case 11: {
                bl = this.boolean_c(this.aH);
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
                bl = this.o(this.aH);
                break;
            }
            case 6: {
                bl = this.boolean_h(this.aH);
                break;
            }
            case 7: {
                bl = this.m(this.aH);
                break;
            }
            case 8: {
                bl = this.boolean_i(this.aH);
                break;
            }
            case 4: {
                bl = this.boolean_d(this.aH);
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
            this.aA = this.am;
            this.aH = 0;
            return true;
        }
        this.aA = 0;
        ++this.aH;
        return false;
    }

    final void n(Graphics graphics) {
        if (this.aA == 0) {
            return;
        }
        switch (this.aA) {
            case 11: {
                this.h(graphics);
                return;
            }
            case 1: {
                this.e(graphics);
                return;
            }
            case 2: {
                this.g(graphics);
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
                this.a(graphics);
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
                this.f(graphics);
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

    final void E() {
        if (this.aA == this.am) {
            return;
        }
        switch (this.aA) {
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
                this.n();
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

    final boolean boolean_g(int n) {
        if (System.getProperty("aoe.debug") != null && n != 4 && n != 6) {
            System.out.println("[trace] g->" + n + " (aA=" + this.aA + " am=" + this.am + ")");
        }
        if (this.am != this.aA && n == 8) {
            return false;
        }
        if (this.am != this.aA && n == 7) {
            return false;
        }
        if ((n == 7 || n == 8) && this.aA == 1) {
            return false;
        }
        this.am = n;
        return true;
    }

    public final boolean boolean_d(int n) {
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
        this.ag = 0;
        this.l();
        if (n == 0) {
            if (this.v >= 0) {
                this.var_byte_arr_i[this.aR + 0] = (byte)this.v;
            }
            this.var_boolean_f = true;
            this.var_boolean_b = true;
            switch (this.H) {
                case 3: {
                    this.a(131, true);
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
                    this.var_byte_arr_i[n2 + 1] = 3;
                    this.var_byte_arr_i[n2 + 2] = (byte)this.aG;
                    this.a(131, true);
                    break;
                }
                case 12: {
                    int n3 = this.int_c(4) + 9;
                    // 移植修改：战役 7 关全部解锁（原版 = aj + 1，按通关进度开放）
                    this.var_byte_arr_i[n3 + 1] = 7;
                    this.var_byte_arr_i[n3 + 2] = (byte)this.aj;
                    this.a(131, true);
                    break;
                }
                case 7: {
                    int n4 = this.int_c(4) + 9;
                    boolean bl = false;
                    if (this.var_boolean_d) {
                        bl = true;
                    }
                    this.var_byte_arr_i[n4 + 2] = (byte)(bl ? 1 : 0);
                    n4 = this.int_c(5) + 9;
                    bl = true;
                    if (AgeOfEmpires.b.c) {
                        bl = false;
                    }
                    this.var_byte_arr_i[n4 + 2] = (byte)(bl ? 1 : 0);
                    break;
                }
                case 8: {
                    int n5 = this.int_c(5) + 9;
                    this.var_byte_arr_i[n5 + 2] = (byte)this.v;
                    break;
                }
                case 0: 
                case 10: {
                    this.a(131, true);
                }
            }
            return false;
        }
        return true;
    }

    public final void C() {
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
    }

    public final void f(Graphics graphics) {
        if (System.getProperty("aoe.debug") != null && this.ax != 0) {
            System.out.println("[fHead] ax=" + this.ax);
        }
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[fMenu] ao=" + this.ao + " Z=" + this.Z + " aR=" + this.aR
                + " tmpl=" + (this.var_byte_arr_i == null ? "null" : this.var_byte_arr_i.length));
        }
        int n = this.aR + 6;
        int n2 = this.var_int_a + this.var_byte_arr_i[this.aR + 4] >> 1;
        int n3 = this.K + this.var_byte_arr_i[this.aR + 5] >> 1;
        this.var_int_a = n2;
        this.K = n3;
        int n4 = this.aR + 6;
        int n5 = 0;
        int n6 = 0;
        for (int i = 0; i < this.ao; ++i) {
            byte by;
            if (i == this.Z) {
                n = n4;
            }
            if (((by = this.var_byte_arr_i[n4 + 0]) & 0x20) == 0) {
                n4 = this.int_e(n4);
                continue;
            }
            if ((by & 0x40) != 0) {
                byte by2;
                byte by3 = this.var_byte_arr_i[n4 + 1];
                if ((by3 & 0x7F) != 127) {
                    int n7 = this.int_c(by3 & 0x7F);
                    n5 = this.var_byte_arr_i[n7 + 4];
                    n6 = this.var_byte_arr_i[n7 + 5];
                }
                int n8 = this.var_byte_arr_i[n4 + 6] & 0xFF;
                int n9 = this.var_byte_arr_i[n4 + 7] & 0xFF;
                byte by4 = this.var_byte_arr_i[n4 + 2];
                int n10 = this.var_byte_arr_i[n4 + 3] & 0xFF;
                if ((by3 & 0xFFFFFF80) != 0) {
                    if (by4 == -1) {
                        n6 = -(this.var_int_j >> 3);
                    } else {
                        n5 += by4;
                        n6 = -4;
                    }
                } else {
                    n5 += n10 * AgeOfEmpires.b.int_b(by4 << 3) >> 16;
                    n6 += n10 * AgeOfEmpires.b.c(by4 << 3) >> 16;
                }
                int n11 = n5 + (this.aO >> 1);
                int n12 = (this.var_int_j >> 1) - n6;
                if ((by & 4) == 0) {
                    n11 -= n2;
                    n12 += n3;
                }
                if (((by2 = this.var_byte_arr_i[n4 + 8]) & 0x10) == 0) {
                    n11 = (by2 & 0x20) == 0 ? (n11 -= n8 >> 1) : (n11 -= n8);
                }
                if ((by2 & 0x40) == 0) {
                    n12 = (by2 & 0xFFFFFF80) == 0 ? (n12 -= n9 >> 1) : (this.var_int_j >> 2) - (n9 >> 1);
                }
                if (i == this.Z) {
                    if (n11 < this.F) {
                        int n13 = this.aR + 4;
                        this.var_byte_arr_i[n13] = (byte)(this.var_byte_arr_i[n13] - (this.F - n11));
                    }
                    if (n12 < this.O) {
                        int n14 = this.aR + 5;
                        this.var_byte_arr_i[n14] = (byte)(this.var_byte_arr_i[n14] + (this.O - n12));
                    }
                    if (n11 + n8 > this.B) {
                        int n15 = this.aR + 4;
                        this.var_byte_arr_i[n15] = (byte)(this.var_byte_arr_i[n15] + (n11 + n8 - this.B));
                    }
                    if (n12 + n9 > this.P) {
                        int n16 = this.aR + 5;
                        this.var_byte_arr_i[n16] = (byte)(this.var_byte_arr_i[n16] - (n12 + n9 - this.P));
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
                    if (n11 + n8 > this.aO) {
                        n5 += n11 - this.aO;
                        n11 = this.aO - n8;
                    }
                    if (n12 + n9 > this.var_int_j) {
                        n6 -= n12 - this.var_int_j;
                        n12 = this.var_int_j - n9;
                    }
                }
                this.var_byte_arr_i[n4 + 4] = (byte)n5;
                this.var_byte_arr_i[n4 + 5] = (byte)n6;
                switch (by2 & 0xF) {
                    case 7: {
                        graphics.setColor(this.var_byte_arr_i[n4 + 9] & 0xFF, this.var_byte_arr_i[n4 + 9 + 1] & 0xFF, this.var_byte_arr_i[n4 + 9 + 2] & 0xFF);
                        graphics.fillRect(0, 0, this.aO, this.var_int_j);
                        break;
                    }
                    case 3: {
                        byte by5 = this.var_byte_arr_i[n4 + 9];
                        this.a(graphics, 10 + by5, n11, n12, 0);
                        break;
                    }
                    case 6: {
                        byte by5 = 0;
                        this.e(graphics, 0, 0, this.aO, this.var_int_j);
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
                switch (this.var_byte_arr_i[n4 + 8]) {
                    case 10: {
                        this.F = this.aO * this.var_byte_arr_i[n4 + 9] / 100;
                        this.O = this.var_int_j * this.var_byte_arr_i[n4 + 9 + 1] / 100;
                        this.B = this.aO * this.var_byte_arr_i[n4 + 9 + 2] / 100;
                        this.P = this.var_int_j * this.var_byte_arr_i[n4 + 9 + 3] / 100;
                        graphics.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
                        break;
                    }
                    case 4: {
                        if (this.aQ <= this.var_byte_arr_i[n4 + 9]) break;
                        this.k(n4, 0);
                        break;
                    }
                    case 5: {
                        if (System.getProperty("aoe.debug") != null) {
                            System.out.println("[menuGate] ax=" + this.ax + " ab=" + this.ab);
                        }
                        if (this.ax != 38 && this.ax != 22 && this.ax != 6) break;
                        this.k(n4, 0);
                    }
                }
            }
            n4 = this.int_e(n4);
        }
        this.void_d(n);
        if (this.ab == 22) {
            this.ab = 0;
        }
        this.ax = 0;
        ++this.aQ;
    }

    public final void void_d(int n) {
        if (this.aQ < 10) {
            return;
        }
        this.var_byte_arr_i[this.aR + 1] = (byte)this.Z;
        switch (this.ax) {
            case 6: 
            case 22: 
            case 38: {
                this.v = this.H;
                this.k(n, 1);
                return;
            }
            case 47: {
                if (this.H >= 2 && this.H <= 5) {
                    return;
                }
                if (!this.boolean_g(9)) break;
                this.D();
                this.v = -1;
                if (this.H == 0) {
                    this.ap = 0;
                    this.var_byte_arr_i[this.aR + 1] = (byte)(this.var_byte_arr_i[this.aR + 2] - 1);
                    return;
                }
                this.ap = this.var_byte_arr_i[this.aR + 0];
                return;
            }
            case 5: 
            case 21: {
                switch (this.var_byte_arr_i[n + 8]) {
                    case 2: {
                        byte by = this.var_byte_arr_i[n + 9 + 2];
                        this.var_byte_arr_i[n + 9 + 2] = by > 0 ? (byte)(by - 1 & 0xFF) : (byte)(this.var_byte_arr_i[n + 9 + 1] - 1);
                        this.k(n, 0);
                    }
                }
                return;
            }
            case 7: 
            case 23: {
                switch (this.var_byte_arr_i[n + 8]) {
                    case 2: {
                        byte by = this.var_byte_arr_i[n + 9 + 2];
                        byte by2 = this.var_byte_arr_i[n + 9 + 1];
                        this.var_byte_arr_i[n + 9 + 2] = by < by2 - 1 ? (byte)(by + 1 & 0xFF) : (byte)0;
                        this.k(n, 0);
                    }
                }
                return;
            }
            case 3: 
            case 19: {
                int n2 = this.Z;
                int n3 = 0;
                do {
                    if (--n2 >= 0) continue;
                    return;
                } while ((this.var_byte_arr_i[n3 = this.int_c(n2)] & 0xFFFFFF80) == 0);
                this.Z = n2;
                return;
            }
            case 9: 
            case 25: {
                int n4 = this.Z;
                int n5 = 0;
                do {
                    if (++n4 < this.ao) continue;
                    return;
                } while ((this.var_byte_arr_i[n5 = this.int_c(n4)] & 0xFFFFFF80) == 0);
                this.Z = n4;
            }
        }
    }

    public final void d(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5 = this.aQ - n4;
        int n6 = this.var_byte_arr_i[n3 + 9] & 0xFF;
        n2 += this.ay;
        if (this.var_byte_arr_i[n3 + 0] == 104) {
            graphics.setColor(this.int_a(0xFFFFFF, 14595245, n5, 10));
            graphics.drawString(this.var_java_lang_String_arr_a[n6], n + 1, n2 + 1, 20);
            graphics.setColor(this.int_a(0xFF0000, 14595245, n5, 10));
            graphics.drawString(this.var_java_lang_String_arr_a[n6], n, n2, 20);
            return;
        }
        int n7 = 0;
        int n8 = 0xFFFFFF;
        if ((this.var_byte_arr_i[n3 + 0] & 0xFFFFFF80) != 0) {
            if (n4 == this.Z) {
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
        int n5 = (this.var_byte_arr_i[n3 + 9] & 0xFF) + (this.var_byte_arr_i[n3 + 9 + 2] & 0xFF);
        int n6 = this.var_byte_arr_i[n3 + 6] & 0xFF;
        int n7 = this.aQ - n4;
        boolean bl = false;
        int n8 = 0xFFFFFF;
        int n9 = 14595245;
        if (n4 == this.Z) {
            int n10 = (AgeOfEmpires.b.int_b(this.aQ << 7) >> 9) + 128 << 16;
            n10 = this.int_a(n10, n9, n7, 10);
            graphics.setColor(n10);
            graphics.fillRect(n, n2, n6, this.ah + 6);
            graphics.setColor(this.int_a(0xFFFFFF, n9, n7, 10));
            graphics.fillRect(n + 1, n2 + 1, n6 - 2, this.ah + 4);
            graphics.setColor(14595245);
            graphics.fillRect(n + 2, n2 + 2, n6 - 4, this.ah + 2);
            if (this.var_byte_arr_i[n3 + 9 + 1] > 1) {
                int n11 = AgeOfEmpires.b.int_b(this.ar << 8) >> 14;
                if (n7 > 6) {
                    this.a(graphics, 21, n - n11 - 13, n2 + (this.ah >> 1), 28, 0, 7, 6, 0, 0);
                    this.a(graphics, 21, n + n6 + n11 + 6, n2 + (this.ah >> 1), 35, 0, 7, 6, 0, 0);
                }
            }
        } else if ((this.var_byte_arr_i[n3 + 0] & 0xFFFFFF80) != 0) {
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
        int n2 = this.aR + 6;
        for (int i = 0; i < n; ++i) {
            n2 = this.int_e(n2);
        }
        return n2;
    }

    public final int int_j(int n) {
        int n2 = this.var_byte_arr_i[n + 2];
        n += 6;
        for (int i = 0; i < n2; ++i) {
            n = this.int_e(n);
        }
        return n;
    }

    public final int int_e(int n) {
        byte by = this.var_byte_arr_i[n + 8];
        n = this.int_k(n);
        n = this.int_i(n);
        if (by == 2) {
            n = this.int_i(n);
        }
        return n;
    }

    public final int int_i(int n) {
        switch (this.var_byte_arr_i[n++]) {
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
        switch (this.var_byte_arr_i[n++] & 0xF) {
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

    public final void l() {
        int n;
        int n2;
        this.aR = 0;
        for (n2 = 0; n2 < this.H; ++n2) {
            this.aR = this.int_j(this.aR);
        }
        n2 = this.var_byte_arr_i[this.aR + 3] & 0xFF;
        this.Z = this.var_byte_arr_i[this.aR + 1];
        a a2 = new a(this.H + 83);
        this.var_java_lang_String_arr_a = new String[n2];
        for (n = 0; n < n2; ++n) {
            this.var_java_lang_String_arr_a[n] = a2.a(n);
        }
        this.ao = this.var_byte_arr_i[this.aR + 2] & 0xFF;
        n = this.aR + 6;
        boolean bl = false;
        for (int i = 0; i < this.ao; ++i) {
            switch (this.var_byte_arr_i[n + 8] & 0xF) {
                case 3: 
                case 6: 
                case 8: 
                case 9: {
                    Image image = this.javax_microedition_lcdui_Image_a(10 + this.var_byte_arr_i[n + 9], 0);
                    if (image == null) break;
                    this.var_byte_arr_i[n + 6] = (byte)(image.getWidth() & 0xFF);
                    this.var_byte_arr_i[n + 7] = (byte)(image.getHeight() & 0xFF);
                    break;
                }
                case 0: {
                    if (this.var_byte_arr_i[n + 3] == 10) {
                        this.var_byte_arr_i[n + 3] = (byte)this.ah;
                    }
                    this.var_byte_arr_i[n + 6] = (byte)(this.a(this.var_java_lang_String_arr_a[this.var_byte_arr_i[n + 9]]) + 1);
                    this.var_byte_arr_i[n + 7] = (byte)this.ah;
                    break;
                }
                case 2: {
                    int n3 = 0;
                    for (int j = 0; j < this.var_byte_arr_i[n + 9 + 1]; ++j) {
                        int n4 = this.a(this.var_java_lang_String_arr_a[this.var_byte_arr_i[n + 9] + j]);
                        if (n4 <= n3) continue;
                        n3 = n4;
                    }
                    this.var_byte_arr_i[n + 6] = (byte)(n3 + 8 & 0xFF);
                    this.var_byte_arr_i[n + 7] = (byte)(this.ah + 4);
                    this.var_byte_arr_i[n + 3] = (byte)(this.ah + 8);
                    this.var_byte_arr_i[this.int_e((int)n) + 3] = (byte)(this.ah + 8);
                }
            }
            n = this.int_e(n);
        }
    }

    public final void D() {
        for (int i = 0; i < this.ao; ++i) {
            int n = this.int_c(i);
            if ((this.var_byte_arr_i[n + 0] & 0x20) == 0 || this.var_byte_arr_i[n + 8] != 2) continue;
            this.k(n, 0);
        }
    }

    public final void k(int n, int n2) {
        int n3 = n;
        n3 = this.int_k(n3);
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[k] node=" + n + " mode=" + n2 + " type=" + this.var_byte_arr_i[n + 8]
                + " op=" + this.var_byte_arr_i[n3]);
        }
        if (this.var_byte_arr_i[n + 8] == 2 && n2 > 0) {
            n3 = this.int_i(n3);
        }
        switch (this.var_byte_arr_i[n3++]) {
            case 11: {
                this.O = 0;
                this.P = this.var_int_j;
                this.m();
                if (this.var_byte_arr_f[33] == 0) {
                    this.g(0, 82, 2);
                } else {
                    this.boolean_g(14);
                }
                this.v = 0;
                return;
            }
            case 8: {
                this.O = 0;
                this.P = this.var_int_j;
                for (int i = 0; i < 7; ++i) {
                    this.var_int_arr_e[i] = this.var_int_arr_d[i];
                }
                this.g(0, 82, 3);
                this.v = 0;
                return;
            }
            case 7: {
                if (!this.boolean_g(this.var_byte_arr_i[n3])) break;
                this.D();
                return;
            }
            case 6: {
                this.O = 0;
                this.P = this.var_int_j;
                this.g(0, 82, this.var_byte_arr_i[n3]);
                this.v = -1;
                return;
            }
            case 2: {
                if (!this.boolean_g(9)) break;
                this.D();
                this.ap = this.var_byte_arr_i[n3];
                this.v = this.H;
                return;
            }
            case 5: {
                byte by = this.var_byte_arr_i[n3++];
                byte by2 = this.var_byte_arr_i[n3];
                n3 = this.int_c(by);
                this.var_byte_arr_i[n3] = (byte)(by2 & 0xFF);
                return;
            }
            case 4: {
                this.var_boolean_e = true;
                return;
            }
            case 3: {
                int n4 = this.var_byte_arr_i[n + 9 + 2];
                if (this.var_byte_arr_i[n3] == 74) {
                    return;
                }
                if (this.var_byte_arr_i[n3] == 67) {
                    this.m();
                    this.var_byte_arr_f[30] = 0;
                    if (n4 == 1) {
                        this.var_byte_arr_f[30] = 1;
                        AgeOfEmpires.b.c();
                        AgeOfEmpires.b.c = false;
                    } else {
                        AgeOfEmpires.b.c = true;
                        AgeOfEmpires.b.var_boolean_b = true;
                    }
                    this.I();
                    return;
                }
                if (this.var_byte_arr_i[n3] == 71) {
                    this.ac = 16;
                    this.aC = n4;
                    return;
                }
                if (this.var_byte_arr_i[n3] == 73) {
                    this.ac = 32;
                    this.aC = n4;
                    return;
                }
                if (this.var_byte_arr_i[n3] == 72) {
                    this.m();
                    this.var_byte_arr_f[29] = 0;
                    if (n4 == 0) {
                        this.var_byte_arr_f[29] = 1;
                        this.var_boolean_d = false;
                    } else {
                        this.var_boolean_d = true;
                    }
                    this.I();
                    return;
                }
                if (this.var_byte_arr_i[n3] == 65) {
                    this.ac = 0;
                    n3 = this.int_k(this.int_c(4));
                    n3 = this.int_i(n3);
                    this.var_byte_arr_i[n3] = 2;
                    if (n4 == 0) {
                        this.var_byte_arr_i[n3 + 1] = 11;
                        return;
                    }
                    if (n4 == 1) {
                        this.var_byte_arr_i[n3 + 1] = 12;
                        return;
                    }
                    if (n4 != 2) break;
                    this.var_byte_arr_i[n3 + 1] = 10;
                    return;
                }
                if (this.var_byte_arr_i[n3] != 66) break;
                this.ac = 0;
                this.var_byte_a = (byte)(n4 & 0xF);
            }
        }
    }

    public final boolean boolean_e(int n) {
        return true;
    }

    public final void o(Graphics graphics) {
        if (this.v != -1) {
            this.v = this.H;
        }
        if (this.ap != this.H) {
            this.H = this.ap;
            this.aQ = 0;
            this.var_int_a = 0;
            this.K = 0;
            if (this.H == 2 || this.H == 3 || this.H == 4) {
                this.var_boolean_f = false;
                this.var_boolean_b = false;
            } else {
                this.var_boolean_f = true;
                this.var_boolean_b = true;
            }
        }
        this.boolean_g(4);
    }

    public final boolean boolean_f(int n) {
        this.var_boolean_f = true;
        this.var_boolean_b = false;
        this.O = 0;
        this.F = 0;
        this.P = this.var_int_j;
        this.B = this.aO;
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
        this.e(graphics, 0, 0, this.aO, this.var_int_j);
        this.a(graphics, this.var_java_lang_String_arr_a[5], this.aO - this.a(this.var_java_lang_String_arr_a[5]) >> 1, 9 + this.ay, this.aQ);
        int n2 = this.B - this.F - 24;
        boolean bl = false;
        int n3 = (this.var_int_j >> 1) - 35;
        if (n3 < 17) {
            n3 = 17;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[1], 12, n3, this.aQ);
        n3 += this.ah + 3;
        int n4 = this.var_int_arr_arr_a[0][86] + this.var_int_arr_arr_a[1][86];
        if (n4 > 0) {
            n = this.var_int_arr_arr_a[0][86] * n2 / n4;
            graphics.setColor(1065087);
            graphics.fillRect(12, n3, n, 3);
            n = this.var_int_arr_arr_a[0][2] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.var_int_arr_arr_a[1][86] * n2 / n4;
            graphics.setColor(8329232);
            graphics.fillRect(12, n3 += 3, n, 3);
            n = this.var_int_arr_arr_a[1][2] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3, n, 3);
            n3 += 7;
        } else {
            n3 += 10;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[2], 12, n3, this.aQ + 1);
        n3 += this.ah + 3;
        n4 = this.var_int_arr_arr_a[0][88] + this.var_int_arr_arr_a[1][88];
        if (n4 > 0) {
            n = this.var_int_arr_arr_a[0][88] * n2 / n4;
            graphics.setColor(1065087);
            graphics.fillRect(12, n3, n, 3);
            n = this.var_int_arr_arr_a[0][4] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.var_int_arr_arr_a[1][88] * n2 / n4;
            graphics.setColor(8329232);
            graphics.fillRect(12, n3 += 3, n, 3);
            n = this.var_int_arr_arr_a[1][4] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3, n, 3);
            n3 += 7;
        } else {
            n3 += 10;
        }
        this.a(graphics, this.var_java_lang_String_arr_a[0], 12, n3, this.aQ + 1);
        n3 += this.ah + 3;
        n4 = this.var_int_arr_arr_a[0][90] + this.var_int_arr_arr_a[1][90];
        if (n4 > 0) {
            n = this.var_int_arr_arr_a[0][90] * n2 / n4;
            graphics.setColor(2130175);
            graphics.fillRect(12, n3, n, 3);
            n = this.var_int_arr_arr_a[1][90] * n2 / n4;
            graphics.setColor(16724000);
            graphics.fillRect(12, n3 += 3, n, 3);
        }
        if (this.ab == 22 || this.ab == 6 || this.ab == 38) {
            this.boolean_g(4);
        }
        if (this.ab == 22) {
            this.ab = 0;
        }
        this.ax = 0;
        this.ab = 0;
        ++this.aQ;
    }

    public final boolean boolean_a(int n) {
        this.var_boolean_f = true;
        this.var_boolean_b = false;
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
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
        this.v = -1;
        this.ab = 0;
        this.ax = 0;
    }

    public final void k(Graphics graphics) {
        int n = this.var_int_j / 19;
        int n2 = this.aQ / 19 - n;
        int n3 = -(this.aQ % 19);
        int n4 = n + 1 + n2;
        graphics.setColor(0);
        graphics.fillRect(0, 0, this.aO, this.var_int_j);
        if (n2 > 0 && this.var_java_lang_String_arr_a[n2] == null) {
            this.boolean_g(4);
            return;
        }
        for (int i = n2; i < n4; ++i) {
            if (i >= 0 && this.var_java_lang_String_arr_a[i] != null) {
                int n5 = this.aO - this.a(this.var_java_lang_String_arr_a[i]) >> 1;
                int n6 = this.int_a(0x606060, 0, this.var_int_j - n3 - 19, this.var_int_j >> 2);
                if (n6 > 0) {
                    graphics.setColor(n6);
                    graphics.drawString(this.var_java_lang_String_arr_a[i], n5, n3 + 1, 20);
                }
                if ((n6 = this.int_a(0xFFFFFF, 0, this.var_int_j - n3 - 19, this.var_int_j >> 2)) > 0) {
                    graphics.setColor(n6);
                    graphics.drawString(this.var_java_lang_String_arr_a[i], n5, n3, 20);
                }
            }
            n3 += 19;
        }
        ++this.aQ;
        if (this.ax == 38 || this.ax == 22 || this.ax == 6 || this.ax == 47) {
            this.boolean_g(4);
            this.ab = 0;
        }
    }

    public final boolean boolean_c(int n) {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[trace] boolean_c aH=" + n + " ac=" + this.ac + " aC=" + this.aC);
        }
        Object object;
        this.var_boolean_f = false;
        this.var_boolean_b = false;
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
        this.var_AgeOfEmpires_d_a = new d();
        if (this.ac == 0) {
            this.m();
            this.var_AgeOfEmpires_d_a.o = this.int_a(31, 2);
            if (this.var_AgeOfEmpires_d_a.o == 0) {
                this.var_AgeOfEmpires_d_a.o = 8224;
            }
            var_int_k = (byte)(this.var_AgeOfEmpires_d_a.o >>> 8) & 0xFF;
            r = (byte)(this.var_AgeOfEmpires_d_a.o & 0xFF);
        }
        if (this.aF != 0) {
            byte[] var_byte_arr_t = com.ulysseo.mad.c.byte_arr_a(this.aF);
            this.var_boolean_k = false;
            int n2 = var_byte_arr_t[0] & 0xFF;
            int n3 = var_byte_arr_t[1] & 0xFF;
            if ((n2 | n3) != 0) {
                var_int_k = n2;
                r = n3;
            } else {
                this.var_boolean_k = true;
            }
        }
        this.var_AgeOfEmpires_d_a.a(9, 20, this.var_short_arr_a, this.var_boolean_k);
        this.aQ = 0;
        object = new a(99);
        this.var_java_lang_String_a = ((a)object).a(0);
        return true;
    }

    public final void t() {
        if (this.var_boolean_k) {
            this.var_short_arr_a[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1 << 6)] = 0;
            this.var_short_arr_a[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] - 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1 << 6)] = 0;
            this.var_short_arr_a[this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[1] << 6)] = 0;
            this.a(0, 9, this.var_AgeOfEmpires_d_a.var_int_arr_a[0], this.var_AgeOfEmpires_d_a.var_int_arr_a[1], 255, false);
            this.a(0, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1, false);
            this.a(0, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] - 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1] + 1, false);
            if (this.var_byte_a < 2) {
                this.a(0, 5, this.var_AgeOfEmpires_d_a.var_int_arr_a[0] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[1], false);
            }
            this.aa = this.var_AgeOfEmpires_d_a.var_int_arr_a[0];
            this.aV = this.var_AgeOfEmpires_d_a.var_int_arr_a[1];
            this.Q = this.aa + (this.aV << 6);
            this.var_short_arr_a[this.var_AgeOfEmpires_d_a.var_int_arr_a[2] + 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1 << 6)] = Short.MIN_VALUE;
            this.var_short_arr_a[this.var_AgeOfEmpires_d_a.var_int_arr_a[2] - 1 + (this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1 << 6)] = Short.MIN_VALUE;
            this.a(1, 9, this.var_AgeOfEmpires_d_a.var_int_arr_a[2], this.var_AgeOfEmpires_d_a.var_int_arr_a[3], 255, false);
            this.a(1, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[2] + 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1, false);
            this.a(1, 0, this.var_AgeOfEmpires_d_a.var_int_arr_a[2] - 1, this.var_AgeOfEmpires_d_a.var_int_arr_a[3] + 1, false);
        }
        if (this.aF != 0) {
            this.void_f(this.aF);
        }
        this.var_AgeOfEmpires_d_a.d();
        this.var_AgeOfEmpires_d_a = null;
        this.var_java_lang_String_a = null;
        AgeOfEmpires.b.c();
    }

    public final void h(Graphics graphics) {
        boolean bl = false;
        int n = (this.var_int_j >> 1) - 10;
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        this.e(graphics, 0, 0, this.aO, this.var_int_j);
        graphics.setColor(7039826);
        graphics.drawString(this.var_java_lang_String_a, 14, n + 1, 20);
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.var_java_lang_String_a, 13, n, 20);
        graphics.setColor(7039826);
        graphics.fillRect(11, (n += this.ah + 3) + 2, this.aO - 21, 10);
        graphics.setColor(0);
        graphics.fillRect(10, n + 1, this.aO - 21, 10);
        graphics.setColor(0xFFFFFF);
        graphics.fillRect(8, n, this.aO - 21, 9);
        int n2 = this.aO - 23;
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
            this.boolean_g(6);
        }
        ++this.aQ;
    }

    static final int int_a() {
        r += var_int_k;
        r += (var_int_k & 0xFF) >> 2;
        var_int_k ^= r;
        var_int_k += (r & 0xFF) >> 1;
        return r & 0xFF;
    }

    public final void void_a(int n, int n2, int n3, int n4) {
        int n5 = n2 + (n3 << 6) & 0xFFF;
        this.var_short_arr_a[n5] = (short)(this.var_short_arr_a[n5] & 0xF000);
        if (n4 > 31) {
            n4 = 31;
        }
        int n6 = n2 + (n3 << 6) & 0xFFF;
        this.var_short_arr_a[n6] = (short)(this.var_short_arr_a[n6] | (short)(0x300 | n & 0xFF | (n4 <<= 2)));
    }

    public final boolean boolean_j(int n) {
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
        this.aU = this.y;
        this.A = this.N;
        this.at = this.aa;
        this.n = this.aV;
        this.af = 0;
        int n2 = this.az + this.ad;
        int n3 = (this.al << 1) + this.J;
        int n4 = n2 + n3 >> 5;
        int n5 = n3 - n2 >> 5;
        this.y = n4 - n5 << 1;
        this.N = n4 + n5;
        this.ag = 0;
        this.ab = 0;
        return true;
    }

    public final void d() {
        this.y = this.aU;
        this.N = this.A;
        this.aa = this.at;
        this.aV = this.n;
        this.ax = 0;
        this.ab = 0;
    }

    public final void e(Graphics graphics) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6;
        int n7;
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        graphics.setColor(3438335);
        graphics.fillRect(0, 0, this.aO, this.var_int_j);
        int n8 = this.aO >> 1;
        int n9 = this.var_int_j >> 1;
        Image image = this.javax_microedition_lcdui_Image_a(20, 240, 120, 3438335);
        if (image == null) {
            return;
        }
        if (this.af < 64) {
            for (n7 = 0; n7 <= 64 && this.af < 64; ++n7) {
                for (n6 = 0; n6 < 64; ++n6) {
                    this.b(this.var_javax_microedition_lcdui_Graphics_a, n6, this.af);
                }
                ++this.af;
            }
        } else {
            for (n5 = 0; n5 < 2; ++n5) {
                n7 = 0;
                n6 = this.var_int_arr_arr_a[n5][2];
                n4 = 0;
                while (n4 < n6) {
                    n3 = this.var_short_arr_arr_a[n5][n7 + 0];
                    n2 = n3 >>> 8;
                    int n10 = this.var_short_arr_arr_a[n5][n7 + 1];
                    int n11 = n10 >>> 8;
                    n = this.var_short_arr_arr_a[n5][n7 + 6] & 0xF0;
                    if (((n11 != n2 || (n10 &= 0xFF) != (n3 &= 0xFF)) && n == 0 || n4 == this.ar % n6) && (this.var_short_arr_a[n2 + (n3 << 6)] & 0x8000) == 0) {
                        this.b(this.var_javax_microedition_lcdui_Graphics_a, n11, n10);
                        this.b(this.var_javax_microedition_lcdui_Graphics_a, n2, n3);
                    }
                    ++n4;
                    n7 += 8;
                }
                n7 = this.var_int_arr_arr_a[n5][4];
                if (n7 == 0) continue;
                if (n7 > 1) {
                    n7 = this.ar % (n7 - 1) << 2;
                }
                n3 = this.var_int_arr_arr_b[n5][n7 + 0];
                n2 = n3 >>> 8;
                this.b(this.var_javax_microedition_lcdui_Graphics_a, n2, n3 &= 0xFF);
            }
        }
        n7 = n8 - this.y - 120;
        n6 = n9 - this.N;
        graphics.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
        com.ulysseo.mad.d.a(graphics, image, 0, 0, 240, 120, 0, n7, n6, 0);
        graphics.setColor(0xFFFFFF);
        this.aQ = (this.ar & 3) >= 2 ? 4 - (this.ar & 3) : this.ar & 3;
        n2 = (this.aI >> 1) + this.aQ;
        n3 = (this.aB >> 2) + this.aQ;
        n = this.aI / 3 + 1;
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
        if (this.af >= 10) {
            int n12 = this.ax;
            if (this.ab == 0) {
                return;
            }
            if (this.ab == this.L) {
                ++this.s;
                if (this.s >= 5) {
                    n12 = this.ab;
                }
            } else {
                this.s = 0;
                this.L = this.ab;
            }
            switch (n12) {
                case 1: 
                case 6: 
                case 22: 
                case 38: {
                    this.aU = this.y;
                    this.A = this.N;
                    this.at = this.aa;
                    this.n = this.aV;
                }
                case 47: {
                    this.O = 0;
                    this.P = this.var_int_j;
                    this.var_boolean_h = true;
                    this.am = 8;
                    this.f();
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
                if (this.y > -119) {
                    this.y -= 4;
                    --this.aa;
                    ++this.aV;
                }
            } else if ((n12 & 0xF0) == 16 && this.y < 119) {
                this.y += 4;
                ++this.aa;
                --this.aV;
            }
            if ((n12 & 0xF) == 15) {
                if (this.N > 0) {
                    this.N -= 2;
                    --this.aa;
                    --this.aV;
                    return;
                }
            } else if ((n12 & 0xF) == 1 && this.N < 119) {
                this.N += 2;
                ++this.aa;
                ++this.aV;
            }
        }
    }

    public final void b(Graphics graphics, int n, int n2) {
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = this.var_short_arr_a[n + (n2 << 6) & 0xFFF];
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
                    if (this.af < 64) {
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
        this.F = 0;
        this.B = this.aO;
        this.v = -1;
        if (this.O == 0) {
            this.var_boolean_l = true;
        }
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        return true;
    }

    public final void a(Graphics graphics) {
        this.mouseTick();
        if (this.var_boolean_l) {
            this.boolean_g(8);
        }
        this.o();
        if (this.ab == 22) {
            this.ab = 0;
        }
        this.ax = 0;
        this.f();
        this.j(graphics);
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

    public final boolean o(int n) {
        int n2;
        this.m = 0;
        this.F = 0;
        this.B = this.aO;
        this.O = 0;
        this.P = this.var_int_j;
        if (n == 0) {
            this.var_int_c = 0;
            this.var_int_arr_b = null;
            this.var_int_arr_b = new int[48];
            for (int i = 0; i < 181; ++i) {
                this.var_javax_microedition_lcdui_Image_arr_a[i] = null;
            }
            this.aE = 0;
            this.var_int_h = -1;
            this.aJ = 0;
            this.Y = 0;
            this.var_int_b = 0;
            this.var_short_a = (short)-1;
            this.var_short_c = (short)-1;
            this.var_short_b = (short)-1;
            this.aq = 0;
            this.E = 0;
            AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
            this.var_int_arr_a[0] = 0;
            this.var_int_arr_a[1] = 0;
            this.var_int_arr_a[2] = 0;
            this.var_int_arr_a[3] = 0;
            this.var_byte_arr_e = null;
            this.var_byte_arr_c = null;
            this.var_byte_arr_a = null;
            this.var_byte_arr_g = null;
            return false;
        }
        this.var_byte_arr_e = com.ulysseo.mad.c.byte_arr_a(122);
        this.var_byte_arr_c = com.ulysseo.mad.c.byte_arr_a(127);
        this.var_byte_arr_g = com.ulysseo.mad.c.byte_arr_a(123);
        this.var_int_i = 0;
        for (n2 = 0; n2 < 4096; ++n2) {
            this.var_short_arr_a[n2] = Short.MIN_VALUE;
        }
        for (n2 = 0; n2 < 2; ++n2) {
            byte[] byArray = com.ulysseo.mad.c.byte_arr_a(121);
            for (int i = 0; i < 91; ++i) {
                this.var_int_arr_arr_a[n2][i] = byArray[i];
            }
            int[] nArray = this.var_int_arr_arr_a[n2];
            nArray[56] = nArray[56] << 8;
            this.var_int_arr_arr_a[n2][54] = 0xFFFFFF;
            this.var_int_arr_arr_a[n2][12] = 16;
        }
        this.aM = 256;
        this.q = 0;
        this.w = 0;
        this.aq = 0;
        this.var_int_i = 0;
        if (this.ac == 0) {
            this.var_boolean_i = true;
            switch (this.var_byte_a) {
                case 0: {
                    this.var_int_arr_arr_a[0][5] = 200;
                    this.var_int_arr_arr_a[0][6] = 100;
                    this.var_int_arr_arr_a[0][7] = 100;
                    this.var_int_arr_arr_a[1][5] = 50;
                    this.var_int_arr_arr_a[1][6] = 15;
                    this.var_int_arr_arr_a[1][7] = 15;
                    this.aM = 512;
                    this.an = 250;
                    this.q = 50;
                    this.C = 20;
                    this.var_int_l = 49;
                    this.aw = Integer.MAX_VALUE;
                    break;
                }
                case 1: {
                    this.var_int_arr_arr_a[0][5] = 200;
                    this.var_int_arr_arr_a[0][6] = 100;
                    this.var_int_arr_arr_a[0][7] = 100;
                    this.var_int_arr_arr_a[1][5] = 50;
                    this.var_int_arr_arr_a[1][6] = 50;
                    this.var_int_arr_arr_a[1][7] = 50;
                    this.aM = 786;
                    this.an = 150;
                    this.q = 60;
                    this.C = 20;
                    this.var_int_l = 36;
                    this.aw = 2500;
                    break;
                }
                case 2: {
                    this.var_int_arr_arr_a[0][5] = 200;
                    this.var_int_arr_arr_a[0][6] = 100;
                    this.var_int_arr_arr_a[0][7] = 100;
                    this.var_int_arr_arr_a[1][5] = 20;
                    this.var_int_arr_arr_a[1][6] = 20;
                    this.var_int_arr_arr_a[1][7] = 20;
                    this.aM = 2048;
                    this.an = 100;
                    this.q = 100;
                    this.C = 1;
                    this.var_int_l = 25;
                    this.aw = 1000;
                }
            }
        }
        n2 = 128;
        this.aF = 0;
        switch (this.ac) {
            case 16: {
                if (this.aC == 0) {
                    n2 = 124;
                    this.I = 78;
                    this.aF = 118;
                    break;
                }
                if (this.aC == 1) {
                    n2 = 125;
                    this.I = 79;
                    this.aF = 119;
                    break;
                }
                if (this.aC != 2) break;
                n2 = 126;
                this.I = 80;
                this.aF = 120;
                break;
            }
            case 32: {
                this.var_boolean_i = false;
                switch (this.aC) {
                    case 0: {
                        n2 = 110;
                        this.I = 71;
                        this.aF = 103;
                        break;
                    }
                    case 1: {
                        n2 = 111;
                        this.I = 72;
                        this.aF = 104;
                        break;
                    }
                    case 2: {
                        n2 = 112;
                        this.I = 73;
                        this.aF = 105;
                        break;
                    }
                    case 3: {
                        n2 = 113;
                        this.I = 74;
                        this.aF = 106;
                        break;
                    }
                    case 4: {
                        n2 = 114;
                        this.I = 75;
                        this.aF = 107;
                        this.var_boolean_i = true;
                        this.an = 100;
                        this.aM = 256;
                        this.an = 200;
                        this.q = 30;
                        this.C = 200;
                        this.var_int_l = 49;
                        this.aw = 1000;
                        break;
                    }
                    case 5: {
                        n2 = 115;
                        this.I = 76;
                        this.aF = 108;
                        this.q = -1000;
                        break;
                    }
                    case 6: {
                        n2 = 116;
                        this.I = 77;
                        this.aF = 109;
                    }
                }
                break;
            }
            default: {
                this.var_boolean_k = true;
                this.var_boolean_l = true;
            }
        }
        if (this.var_boolean_i) {
            this.ai = 0;
            this.void_a();
        }
        this.var_byte_arr_a = com.ulysseo.mad.c.byte_arr_a(n2);
        return true;
    }

    public final void i(Graphics graphics) {
        if (this.ac == 16 && (this.aC == 0 || this.aC == 2)) {
            this.void_f(this.aF);
            this.boolean_g(6);
            return;
        }
        this.boolean_g(11);
    }

    public final boolean m(int n) {
        this.F = 0;
        this.B = this.aO;
        if (this.var_boolean_a) {
            if (this.var_boolean_g) {
                this.aK = 0;
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
                n2 += this.var_byte_arr_c[36];
                n2 += this.var_byte_arr_c[40];
                n2 += this.var_byte_arr_c[43];
                int n3 = this.W;
                this.var_int_arr_c[n3] = this.var_int_arr_c[n3] + this.var_byte_arr_c[36];
            }
            this.var_java_lang_String_arr_a[this.W << 1] = a3.a(n2 << 1);
            this.var_java_lang_String_arr_a[(this.W << 1) + 1] = a3.a((n2 << 1) + 1);
            ++this.W;
        }
        return true;
    }

    public final void n() {
        this.var_int_arr_c = null;
        this.var_java_lang_String_a = null;
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
    }

    public final void l(Graphics graphics) {
        if (this.W == 0) {
            this.am = 6;
            return;
        }
        this.P = this.aL;
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        graphics.setColor(0);
        graphics.fillRect(0, this.P, this.aO, this.var_int_j - this.P);
        if (this.var_boolean_a) {
            int n = 0;
            boolean bl = false;
            int n2 = this.aO - 111 >> 1;
            int n3 = this.P - this.O - 111 >> 1;
            if (n3 < this.O) {
                n3 = this.O;
            }
            if (this.var_boolean_g) {
                graphics.setColor(0x408040);
                graphics.fillRect(n2 - 1, n3 - 1, 112, 112);
                this.i();
                this.ax = 0;
                this.ab = 0;
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        int n4 = j * 37 + n2;
                        int n5 = i * 37 + n3;
                        if (n == this.T) {
                            if ((this.ar & 1) == 0) {
                                graphics.setColor(0xFFFFFF);
                            } else {
                                graphics.setColor(0xFF0000);
                            }
                            graphics.fillRect(n4 - 1, n5 - 1, 38, 38);
                        }
                        if (n < this.W) {
                            int n6 = 0;
                            n6 = this.var_int_g == 0 ? (this.var_int_arr_arr_a[0][3] > this.var_int_arr_arr_a[0][49] + this.var_int_arr_arr_a[0][2] ? (this.boolean_a(0, this.var_int_g, this.var_int_arr_c[n]) ? 0 : 4) : 4) : (this.boolean_a(0, this.var_int_g, this.var_int_arr_c[n]) ? 0 : 4);
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
                boolean bl2 = this.boolean_a(0, 2, this.aK);
                switch (this.ax) {
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
                            this.g(0, 81, this.aK);
                            break;
                        }
                        if (bl2 && this.T == 0) {
                            this.y();
                        }
                        this.am = 6;
                        break;
                    }
                    case 47: {
                        this.T = 1;
                        this.am = 6;
                    }
                }
                this.ax = 0;
                this.ab = 0;
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
                int n11 = this.aO - n9 >> 1;
                int n12 = (this.P - this.O >> 1) + this.O - (n10 >> 1);
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
                this.a(graphics, this.x, n11 + 3, n12 + (n10 - 36 >> 1), this.aK * 36, 0, 36, 36, 0, n8);
                n12 = n12 + n10 - 4 - 20;
                if ((this.ar & 1) == 0) {
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
        this.a(graphics, this.var_int_arr_c[this.aK], this.var_java_lang_String_arr_a[this.aK << 1], this.var_java_lang_String_arr_a[(this.aK << 1) + 1]);
        if (!this.var_boolean_a) {
            this.t = this.Q;
            this.am = 6;
        }
    }

    final void i() {
        boolean bl = false;
        boolean bl2 = false;
        int n = 1000;
        switch (this.ax) {
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
                this.boolean_g(6);
                this.t = this.Q;
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
        if (this.T != this.aK) {
            this.R = 40;
        }
        this.aK = this.T;
        if (bl2) {
            this.y();
            this.boolean_g(8);
            this.t = this.Q;
        }
    }

    final void y() {
        switch (this.var_int_g) {
            case 0: {
                if (this.var_int_arr_arr_a[0][2] + this.var_int_arr_arr_a[0][49] >= this.var_int_arr_arr_a[0][3] || !this.boolean_a(0, 0, this.var_int_arr_c[this.aK])) break;
                this.int_c(0, this.var_int_arr_c[this.aK]);
                return;
            }
            case 1: {
                int n = this.var_int_arr_c[this.aK];
                if (n > 12) {
                    n = 12;
                }
                if (!this.boolean_a(0, 1, this.var_int_arr_c[this.aK])) break;
                this.var_int_b = 1;
                this.p = n;
                this.var_int_d = this.y + this.aP;
                this.S = this.N + this.aS;
                this.au = this.var_int_j;
                this.U = 8;
                return;
            }
            case 2: {
                if (!this.boolean_a(0, 2, this.var_int_arr_c[this.aK])) break;
                int[] nArray = this.var_int_arr_arr_b[0];
                int n = this.aD + 2;
                nArray[n] = nArray[n] | 0x20000000;
                int[] nArray2 = this.var_int_arr_arr_b[0];
                int n2 = this.aD + 2;
                nArray2[n2] = nArray2[n2] | 0x10000;
                int[] nArray3 = this.var_int_arr_arr_b[0];
                int n3 = this.aD + 2;
                nArray3[n3] = nArray3[n3] & 0xFFFF00FF;
                this.c(0, 2, this.var_int_arr_c[this.aK]);
            }
        }
    }

    final boolean boolean_k(int n) {
        if (!this.var_boolean_a) {
            return true;
        }
        switch (this.var_int_g) {
            case 0: {
                if (this.var_byte_arr_c[0 + n] == 0) {
                    return false;
                }
                if (n != 9 || this.var_int_arr_arr_a[0][74] + this.var_int_arr_arr_a[0][65] <= 0) break;
                return false;
            }
            case 1: {
                if (this.var_byte_arr_c[10 + n] == 0) {
                    return false;
                }
                switch (n) {
                    case 11: {
                        if (this.a(0, 11, false) < 4) break;
                        return false;
                    }
                    case 12: {
                        if (this.a(0, 12, false) < 5) break;
                        return false;
                    }
                    case 1: {
                        if (this.a(0, 1, false) < 2) break;
                        return false;
                    }
                }
                break;
            }
            case 2: {
                if (this.var_byte_arr_c[23 + n] != 0) break;
                return false;
            }
        }
        return true;
    }

    public final boolean boolean_i(int n) {
        this.F = 0;
        this.B = this.aO;
        return true;
    }

    public final void c(Graphics graphics) {
        this.d(graphics);
        this.am = 6;
    }

    public final void d(Graphics graphics) {
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        int n = this.aO - 240 >> 1;
        graphics.setColor(0);
        if (n > 0) {
            graphics.fillRect(0, 0, this.aO, 21);
        } else {
            graphics.fillRect(0, 19, this.aO, 1);
        }
        this.a(graphics, 16, n, 0, 0, 0, 240, 19, 0, 0);
        this.b(graphics, this.var_int_arr_arr_a[0][5], n + 26, 3, 3);
        this.b(graphics, this.var_int_arr_arr_a[0][6], n + 84, 3, 3);
        this.b(graphics, this.var_int_arr_arr_a[0][7], n + 140, 3, 3);
        this.b(graphics, this.var_int_arr_arr_a[0][2], n + 197, 3, 2);
        this.b(graphics, this.var_int_arr_arr_a[0][3], n + 220, 3, 2);
        this.O = 21;
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
        if (this.aO > 128) {
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
        if (this.var_boolean_a && (this.var_byte_arr_e[n3 *= 3] > this.var_int_arr_arr_a[0][5] || this.var_byte_arr_e[n3 + 1] > this.var_int_arr_arr_a[0][6] || this.var_byte_arr_e[n3 + 2] > this.var_int_arr_arr_a[0][7])) {
            n5 = 4;
        }
        int n6 = this.var_int_j - 19 + 2;
        this.a(graphics, n4, n2, this.aL + (this.var_int_j - this.aL - 36 >> 1), n * 36, 0, 36, 36, 0, n5);
        this.a(graphics, 16, 41, n6, 1, 1, 27, 17, 0, 0);
        this.b(graphics, this.var_byte_arr_e[n3++], 67, n6, 2);
        this.a(graphics, 16, 99, n6, 58, 1, 27, 17, 0, 0);
        this.b(graphics, this.var_byte_arr_e[n3++], 125, n6, 2);
        this.a(graphics, 16, 157, n6, 115, 1, 27, 17, 0, 0);
        this.b(graphics, this.var_byte_arr_e[n3], 183, n6, 2);
        this.var_java_lang_String_b = string + " " + string2;
    }

    public final void f() {
        this.az = (this.aa - this.aV << 4) - this.ad;
        this.al = (this.aa + this.aV << 3) - (this.J >> 1) + 8;
        this.y = ((this.az << 1) + this.y + 1) / 3;
        this.N = ((this.al << 1) + this.N + 1) / 3;
        this.Q = this.aa + (this.aV << 6) & 0xFFF;
        if ((this.Q & 0xFFFFF000) != 0) {
            this.Q = -1;
            return;
        }
        if ((this.var_short_arr_a[this.Q] & 0xFFF) == 768) {
            this.Q = -1;
        }
    }

    public final void m(int n, int n2) {
        this.aO = n;
        this.var_int_j = n2;
        this.aI = (this.aO >> 6) + 3;
        this.aB = (this.var_int_j >> 4) + 5;
        this.ad = (this.aO >> 1) + 64 >> 1;
        this.J = this.var_int_j + 48 >> 1;
        this.aL = this.var_int_f + 19 + 2;
        if (this.aL < 36) {
            this.aL = 36;
        }
        this.aL = this.var_int_j - this.aL;
    }

    public final void j(int n, int n2) {
        this.m(n, n2);
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

    public final void a(Graphics graphics, int n, int n2, int n3, int n4) {
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
    public final void o() {
        if (this.Q != this.t) {
            this.P = this.var_int_j;
        }
        int n = this.ax;
        if (this.ab == 0) {
            return;
        }
        if (this.ab == this.L) {
            ++this.s;
            if (this.s >= 5) {
                n = this.ab;
            }
        } else {
            this.s = 0;
            this.L = this.ab;
        }
        switch (n) {
            case 2: {
                --this.aa;
                break;
            }
            case 3: 
            case 19: {
                --this.aa;
                --this.aV;
                break;
            }
            case 4: {
                --this.aV;
                break;
            }
            case 5: 
            case 21: {
                --this.aa;
                ++this.aV;
                break;
            }
            case 7: 
            case 23: {
                ++this.aa;
                --this.aV;
                break;
            }
            case 9: 
            case 25: {
                ++this.aa;
                ++this.aV;
                break;
            }
            case 8: {
                ++this.aV;
                break;
            }
            case 10: {
                ++this.aa;
            }
        }
        switch (this.ax) {
            case 6: 
            case 22: 
            case 38: {
                this.x();
                return;
            }
            case 1: {
                this.var_boolean_h = true;
                this.boolean_g(1);
                return;
            }
            case 47: {
                int n2;
                if (this.Q != -1 && (this.var_short_arr_a[this.Q] & 0x300) == 256 && (n2 = (this.var_short_arr_a[this.Q] & 0xC00) >> 10) == 0) {
                    int n3 = (this.var_short_arr_a[this.Q] & 0xFF) << 2;
                    if ((this.var_int_arr_arr_b[0][n3 + 2] & 0x20000000) != 0) {
                        return;
                    }
                    if ((this.var_int_arr_arr_b[0][n3 + 2] & 0xFF0000) != 0) {
                        int[] nArray = this.var_int_arr_arr_b[0];
                        int n4 = n3 + 2;
                        nArray[n4] = nArray[n4] - 65536;
                        int[] nArray2 = this.var_int_arr_arr_b[0];
                        int n5 = n3 + 2;
                        nArray2[n5] = nArray2[n5] & 0xDFFFFFFF;
                        if ((this.var_int_arr_arr_b[0][n3 + 2] & 0xFF0000) == 0) {
                            int[] nArray3 = this.var_int_arr_arr_b[0];
                            int n6 = n3 + 2;
                            nArray3[n6] = nArray3[n6] & 0xFFFF00FF;
                        }
                        int n7 = this.var_int_arr_arr_a[0][0];
                        int n8 = 1;
                        int n9 = this.var_int_arr_arr_b[0][n3 + 3] & 0xFF;
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
                        int[] nArray4 = this.var_int_arr_arr_a[0];
                        nArray4[49] = nArray4[49] - 1;
                        if (n8 < 2) {
                            int[] nArray5 = this.var_int_arr_arr_a[0];
                            nArray5[66] = nArray5[66] - 1;
                            return;
                        }
                        int[] nArray6 = this.var_int_arr_arr_a[0];
                        int n10 = 66 + n8 - 1;
                        nArray6[n10] = nArray6[n10] - 1;
                        return;
                    }
                    if ((this.var_int_arr_arr_b[0][n3 + 2] & 0x40000000) != 0) {
                        n3 = this.var_short_arr_a[this.Q] & 0xFF;
                        int n11 = this.var_int_arr_arr_b[0][(n3 << 2) + 3] & 0xFF;
                        this.i(0, n3);
                        this.b(0, 1, n11);
                        return;
                    }
                }
                if (this.var_int_b == 1) {
                    this.H();
                    return;
                }
                if (this.var_int_h != -1) {
                    if (this.var_int_h == 256 && this.Y == 0 && (n2 = this.var_int_arr_arr_b[0][this.aJ + 2] & 0xFF0000) != 0) {
                        int[] nArray = this.var_int_arr_arr_b[0];
                        int n12 = this.aJ + 2;
                        nArray[n12] = nArray[n12] - 65536;
                        return;
                    }
                    this.H();
                    return;
                }
                if (this.var_int_b != 0) {
                    this.H();
                    return;
                }
                if (!this.boolean_g(4)) break;
                this.O = 0;
                this.F = 0;
                this.P = this.var_int_j;
                this.B = this.aO;
                this.v = this.H = 6;
                return;
            }
            case 11: {
                if (!this.boolean_g(7)) break;
                this.var_int_g = 0;
                this.x = 17;
                this.G = 67;
                this.X = 10;
                this.var_boolean_a = true;
                this.var_boolean_g = true;
                return;
            }
            case 12: {
                if (!this.boolean_g(7)) break;
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

    public final boolean l(int n) {
        if ((this.var_int_arr_arr_b[0][(n <<= 2) + 2] & 0x40000000) != 0) {
            return false;
        }
        if ((this.var_int_arr_arr_b[0][n + 2] & 0xFF) != 255 && this.var_int_h < 2 && this.Y == 0) {
            return false;
        }
        this.var_int_g = -1;
        int n2 = this.var_int_arr_arr_a[0][0];
        switch (this.var_int_arr_arr_b[0][n + 3] & 0xFF) {
            case 9: {
                this.var_int_g = 2;
                this.aK = 21 + n2;
                if (n2 == 0) {
                    if (this.a(0, 10, true) >= 1) break;
                    return false;
                }
                if (n2 == 1) {
                    if (this.a(0, 5, true) + this.a(0, 6, true) >= 2) break;
                    return false;
                }
                if (n2 == 2) {
                    if (this.a(0, 3, true) >= 1) break;
                    return false;
                }
                return false;
            }
            case 4: {
                if (this.var_byte_arr_c[37] == 0 && n2 >= 2) {
                    this.aK = 14;
                } else if (this.var_byte_arr_c[34] == 0 && n2 >= 2) {
                    this.aK = 11;
                } else if (this.var_byte_arr_c[33] == 0 && n2 >= 3) {
                    this.aK = 10;
                } else if (this.var_byte_arr_c[35] == 0 && n2 >= 3) {
                    this.aK = 12;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 0: {
                if (this.var_byte_arr_c[26] == 0 && n2 >= 1) {
                    this.aK = 3;
                } else if (this.var_byte_arr_c[24] == 0 && n2 >= 2) {
                    this.aK = 1;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 1: {
                if (this.var_byte_arr_c[28] == 0 && n2 >= 1) {
                    this.aK = 5;
                } else if (this.var_byte_arr_c[32] == 0 && n2 >= 1) {
                    this.aK = 9;
                } else if (this.var_byte_arr_c[42] == 0 && n2 >= 2) {
                    this.aK = 19;
                } else if (this.var_byte_arr_c[41] == 0 && n2 >= 2) {
                    this.aK = 18;
                } else {
                    return false;
                }
                this.var_int_g = 2;
                break;
            }
            case 5: {
                if (this.var_byte_arr_c[29] != 0) {
                    return false;
                }
                this.aK = 6;
                this.var_int_g = 2;
                break;
            }
            case 6: {
                if (this.var_byte_arr_c[27] == 0 && n2 >= 1) {
                    this.aK = 4;
                } else if (this.var_byte_arr_c[31] == 0 && n2 >= 1) {
                    this.aK = 8;
                } else if (this.var_byte_arr_c[30] == 0 && n2 >= 2) {
                    this.aK = 7;
                } else if (this.var_byte_arr_c[25] == 0 && n2 >= 2) {
                    this.aK = 2;
                } else if (this.var_byte_arr_c[23] == 0 && n2 >= 3) {
                    this.aK = 0;
                } else if (this.var_byte_arr_c[38] == 0 && n2 >= 3) {
                    this.aK = 15;
                } else {
                    if (n2 < 3) {
                        return false;
                    }
                    this.aK = 8;
                    this.var_int_g = 0;
                    break;
                }
                this.var_int_g = 2;
                break;
            }
            case 11: {
                this.aK = 0;
                this.var_int_g = 0;
                break;
            }
            case 10: {
                this.aK = n2 == 0 ? 2 : 3;
                this.var_int_g = 0;
                break;
            }
            case 7: {
                this.aK = 4;
                this.var_int_g = 0;
                break;
            }
            case 8: {
                this.aK = n2 == 1 ? 5 : 6;
                this.var_int_g = 0;
                break;
            }
            case 3: {
                if (this.var_int_arr_arr_a[0][74] + this.var_int_arr_arr_a[0][65] > 0) {
                    return false;
                }
                this.aK = 9;
                this.var_int_g = 0;
                break;
            }
            case 2: {
                this.aK = 7;
                this.var_int_g = 0;
                break;
            }
            case 12: {
                if (this.var_byte_arr_c[43] == 1) {
                    return false;
                }
                this.var_int_g = 2;
                if (this.var_byte_arr_c[43] == 0 && this.var_byte_arr_c[40] != 0 && n2 >= 3) {
                    this.aK = 20;
                    break;
                }
                if (this.var_byte_arr_c[40] == 0 && this.var_byte_arr_c[36] != 0 && n2 >= 2) {
                    this.aK = 17;
                    break;
                }
                if (this.var_byte_arr_c[36] == 0 && n2 >= 1) {
                    this.aK = 13;
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
                if ((this.var_int_arr_arr_b[0][n + 2] & 0x20000000) != 0) {
                    return true;
                }
                if (this.boolean_g(7)) {
                    this.x = 19;
                    this.G = 64;
                    this.X = 24;
                    this.var_boolean_a = true;
                    this.var_boolean_g = false;
                    this.aD = n;
                }
                int[] nArray = this.var_int_arr_arr_b[0];
                int n3 = n + 2;
                nArray[n3] = nArray[n3] | Integer.MIN_VALUE;
                int[] nArray2 = this.var_int_arr_arr_b[0];
                int n4 = n + 2;
                nArray2[n4] = nArray2[n4] & 0xFF00FFFF;
                break;
            }
            case 0: {
                if (this.var_int_arr_arr_a[0][3] <= this.var_int_arr_arr_a[0][49] + this.var_int_arr_arr_a[0][2]) {
                    return true;
                }
                if (this.boolean_g(7)) {
                    this.var_boolean_a = false;
                    this.x = 17;
                    this.G = 67;
                    this.X = 10;
                    this.var_boolean_g = false;
                }
                int[] nArray = this.var_int_arr_arr_a[0];
                nArray[49] = nArray[49] + 1;
                if (this.aK < 2) {
                    int[] nArray3 = this.var_int_arr_arr_a[0];
                    nArray3[66] = nArray3[66] + 1;
                } else {
                    int[] nArray4 = this.var_int_arr_arr_a[0];
                    int n5 = 66 + this.aK - 1;
                    nArray4[n5] = nArray4[n5] + 1;
                }
                int[] nArray5 = this.var_int_arr_arr_b[0];
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

    final void x() {
        if (((this.aa | this.aV) & 0xFFFFFFC0) != 0 || this.Q == -1) {
            return;
        }
        int n = (this.var_short_arr_a[this.Q] & 0xC00) >> 10;
        int n2 = this.var_short_arr_a[this.Q] & 0x300;
        int n3 = this.var_short_arr_a[this.Q] & 0xFF;
        if (n2 == 256 && n == 0 && this.l(n3)) {
            return;
        }
        switch (this.var_int_b) {
            case 0: {
                if (n2 == 256 && (this.var_int_arr_arr_b[n][(n3 << 2) + 2] & 0x40000000) != 0) {
                    return;
                }
                this.f(n, n2, n3);
                this.var_int_b = 6;
                return;
            }
            case 6: {
                if (n == 0) {
                    if (this.Y == 0 && n2 == 256 && this.aE == 512 && this.var_int_h >= 2) {
                        this.H();
                        this.f(n, n2, n3);
                        return;
                    }
                    if (n2 == 512) {
                        if (this.aE == 512 && this.Y == 0) {
                            int n4 = this.var_short_arr_arr_a[0][(n3 << 3) + 3] & 0xFF;
                            if (this.var_int_h != -1 && this.var_int_h == n4) {
                                if (this.aJ == -1) {
                                    this.h(0, -1);
                                    return;
                                }
                                if (this.aJ == n3 << 3) {
                                    this.h(0, n4);
                                    return;
                                }
                            }
                        }
                        this.H();
                        this.f(n, n2, n3);
                        return;
                    }
                }
                if (this.Y == 0 && this.aE == 512) {
                    this.var_int_d = this.y + this.aP;
                    this.S = this.N + this.aS;
                    this.au = this.var_int_j;
                    this.U = 8;
                    this.d(0, this.aa, this.aV);
                    if (this.var_int_h >= 2 || n2 != 768 || (this.var_short_arr_a[this.Q] & 0xF000) != 0) break;
                    this.H();
                    this.var_int_b = 0;
                    return;
                }
                this.H();
                this.f(n, n2, n3);
                return;
            }
            case 1: {
                if (n2 != 0 || this.var_short_arr_a[this.Q] < 0) {
                    return;
                }
                if (this.boolean_a(0, 1, this.p)) {
                    this.a(0, this.p, this.aa, this.aV, 0x40000000, true);
                }
                this.H();
                this.var_int_b = 0;
            }
        }
    }

    public final void f(int n, int n2, int n3) {
        this.aE = n2;
        this.Y = n;
        this.R = 40;
        switch (n2) {
            case 512: {
                short[] sArray = this.var_short_arr_arr_a[n];
                int n4 = (n3 <<= 3) + 4;
                sArray[n4] = (short)(sArray[n4] | 0x8000);
                this.aJ = n3;
                this.var_int_h = this.var_short_arr_arr_a[n][n3 + 3] & 0xFF;
                this.var_boolean_h = true;
                if (this.P != this.var_int_j || !this.boolean_g(7)) break;
                this.var_int_g = 0;
                this.x = 17;
                this.G = 67;
                this.var_boolean_a = false;
                this.X = 10;
                this.aK = this.var_int_h;
                break;
            }
            case 256: {
                int[] nArray = this.var_int_arr_arr_b[n];
                int n5 = (n3 <<= 2) + 2;
                nArray[n5] = nArray[n5] | Integer.MIN_VALUE;
                this.aJ = n3;
                this.var_int_h = this.var_int_arr_arr_b[n][n3 + 3] & 0xFF;
                this.var_boolean_h = true;
                if (!this.boolean_g(7)) break;
                this.var_int_g = 1;
                this.x = 18;
                this.G = 63;
                this.var_boolean_a = false;
                this.X = 13;
                this.aK = this.var_int_h;
                break;
            }
            case 768: {
                int n6 = n3;
                this.var_short_arr_a[n6] = (short)(this.var_short_arr_a[n6] | 0x80);
                break;
            }
            default: {
                return;
            }
        }
        this.var_int_b = 6;
    }

    public final void h(int n, int n2) {
        this.aE = 512;
        this.var_int_h = n2;
        this.aJ = -1;
        this.Y = n;
        this.var_int_b = 6;
        int n3 = 0;
        int n4 = this.var_int_arr_arr_a[this.Y][2];
        if (n2 == -1) {
            for (int i = 0; i < n4; ++i) {
                if ((this.var_short_arr_arr_a[this.Y][n3 + 3] & 0xFF) >= 2) {
                    short[] sArray = this.var_short_arr_arr_a[this.Y];
                    int n5 = n3 + 4;
                    sArray[n5] = (short)(sArray[n5] | 0x8000);
                }
                n3 += 8;
            }
        } else if (n2 < 2) {
            for (int i = 0; i < n4; ++i) {
                if ((this.var_short_arr_arr_a[this.Y][n3 + 3] & 0xFF) < 2 && (this.var_short_arr_arr_a[this.Y][n3 + 7] & 0xF) == 0) {
                    short[] sArray = this.var_short_arr_arr_a[this.Y];
                    int n6 = n3 + 4;
                    sArray[n6] = (short)(sArray[n6] | 0x8000);
                }
                n3 += 8;
            }
        } else {
            for (int i = 0; i < n4; ++i) {
                if ((this.var_short_arr_arr_a[this.Y][n3 + 3] & 0xFF) == this.var_int_h) {
                    short[] sArray = this.var_short_arr_arr_a[this.Y];
                    int n7 = n3 + 4;
                    sArray[n7] = (short)(sArray[n7] | 0x8000);
                }
                n3 += 8;
            }
        }
    }

    public final void H() {
        int n;
        boolean bl = false;
        this.var_int_b = 0;
        if (this.aE == -1) {
            return;
        }
        int n2 = 0;
        for (n = 0; n < this.var_int_arr_arr_a[this.Y][2]; ++n) {
            short[] sArray = this.var_short_arr_arr_a[this.Y];
            int n3 = n2 + 4;
            sArray[n3] = (short)(sArray[n3] & Short.MAX_VALUE);
            n2 += 8;
        }
        n2 = 0;
        for (n = 0; n < this.var_int_arr_arr_a[this.Y][4]; ++n) {
            int[] nArray = this.var_int_arr_arr_b[this.Y];
            int n4 = n2 + 2;
            nArray[n4] = nArray[n4] & Integer.MAX_VALUE;
            n2 += 4;
        }
        this.aE = -1;
        this.var_int_h = -1;
    }

    final void d(int n, int n2, int n3) {
        short s = (short)(n2 << 8 | n3);
        int n4 = 0;
        int n5 = 0;
        while (n5 < this.var_int_arr_arr_a[n][2]) {
            if ((this.var_short_arr_arr_a[n][n4 + 4] & 0x8000) != 0) {
                this.var_short_arr_arr_a[n][n4 + 2] = s;
                this.var_short_arr_arr_a[n][n4 + 1] = this.var_short_arr_arr_a[n][n4 + 0];
                this.var_short_arr_arr_a[n][n4 + 7] = 0;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n6 = n4 + 3;
                sArray[n6] = (short)(sArray[n6] & 0xFF);
            }
            ++n5;
            n4 += 8;
        }
    }

    public final void c(int n, int n2, int n3) {
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
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[5] = nArray[5] - this.var_byte_arr_e[n3++];
        int[] nArray2 = this.var_int_arr_arr_a[n];
        nArray2[6] = nArray2[6] - this.var_byte_arr_e[n3++];
        int[] nArray3 = this.var_int_arr_arr_a[n];
        nArray3[7] = nArray3[7] - this.var_byte_arr_e[n3];
        if (this.var_int_arr_arr_a[n][5] < 0) {
            this.var_int_arr_arr_a[n][5] = 0;
        }
        if (this.var_int_arr_arr_a[n][6] < 0) {
            this.var_int_arr_arr_a[n][6] = 0;
        }
        if (this.var_int_arr_arr_a[n][7] < 0) {
            this.var_int_arr_arr_a[n][7] = 0;
        }
        this.var_boolean_l = true;
    }

    public final void b(int n, int n2, int n3) {
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
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[5] = nArray[5] + this.var_byte_arr_e[n3++];
        int[] nArray2 = this.var_int_arr_arr_a[n];
        nArray2[6] = nArray2[6] + this.var_byte_arr_e[n3++];
        int[] nArray3 = this.var_int_arr_arr_a[n];
        nArray3[7] = nArray3[7] + this.var_byte_arr_e[n3];
        this.var_boolean_l = true;
    }

    public final boolean boolean_a(int n, int n2, int n3) {
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
        if (this.var_int_arr_arr_a[n][5] < this.var_byte_arr_e[n3++]) {
            return false;
        }
        if (this.var_int_arr_arr_a[n][6] < this.var_byte_arr_e[n3++]) {
            return false;
        }
        return this.var_int_arr_arr_a[n][7] >= this.var_byte_arr_e[n3];
    }

    final void p() {
        int n;
        for (n = 0; n < this.var_int_arr_arr_a[0][2]; ++n) {
            this.void_d(0, n);
        }
        if (this.var_int_arr_arr_a[0][4] > 0) {
            n = this.ar % this.var_int_arr_arr_a[0][4];
            int n2 = n << 2;
            int n3 = this.var_int_arr_arr_b[0][n2 + 0];
            int n4 = n3 & 0xFF;
            n3 >>>= 8;
            int n5 = this.var_int_arr_arr_b[0][n2 + 3] & 0xFF;
            if ((this.var_int_arr_arr_b[0][n2 + 2] & 0x40000000) == 0) {
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
                this.var_short_arr_a[n7] = (short)(this.var_short_arr_a[n7] & 0xFFF);
                int n8 = n - i + n6 + (n2 + n4 << 6) & 0xFFF;
                this.var_short_arr_a[n8] = (short)(this.var_short_arr_a[n8] | 0x1000);
                int n9 = n - i + n6 + (n2 - n4 << 6) & 0xFFF;
                this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] & 0xFFF);
                int n10 = n - i + n6 + (n2 - n4 << 6) & 0xFFF;
                this.var_short_arr_a[n10] = (short)(this.var_short_arr_a[n10] | 0x1000);
            }
            for (n6 = 0; n6 < n4 << 1; ++n6) {
                int n11 = n - n4 + n6 + (n2 + i << 6) & 0xFFF;
                this.var_short_arr_a[n11] = (short)(this.var_short_arr_a[n11] & 0x1FFF);
                int n12 = n - n4 + n6 + (n2 + i << 6) & 0xFFF;
                this.var_short_arr_a[n12] = (short)(this.var_short_arr_a[n12] | 0x1000);
                int n13 = n - n4 + n6 + (n2 - i << 6) & 0xFFF;
                this.var_short_arr_a[n13] = (short)(this.var_short_arr_a[n13] & 0x1FFF);
                int n14 = n - n4 + n6 + (n2 - i << 6) & 0xFFF;
                this.var_short_arr_a[n14] = (short)(this.var_short_arr_a[n14] | 0x1000);
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
                this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] | s);
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
                this.var_short_arr_a[n8] = (short)(this.var_short_arr_a[n8] & 0xFFF);
                int n9 = n7 + n6 & 0xFFF;
                this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] | 0x1000);
                ++n7;
            }
        }
    }

    public final void l(int n, int n2) {
        int n3 = n2 << 3;
        int n4 = this.var_short_arr_arr_a[n][n3 + 0] >>> 8;
        int n5 = this.var_short_arr_arr_a[n][n3 + 0] & 0xFF;
        int n6 = n4 + (n5 << 6) - 65 & 0xFFF;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if ((this.var_short_arr_a[n6] & 0x1000) == 0) {
                    this.var_short_arr_a[n6] = (short)(this.var_short_arr_a[n6] & 0x3FFF | 0x4000);
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
        int n4 = this.var_short_arr_arr_a[n][n3 + 0];
        int n5 = n4 >>> 8;
        int n6 = n5 + ((n4 &= 0xFF) << 6);
        int n7 = n6 - 65 & 0xFFF;
        this.var_short_arr_a[n7] = (short)(this.var_short_arr_a[n7] & 0x3FFF);
        int n8 = n6 - 64 & 0xFFF;
        this.var_short_arr_a[n8] = (short)(this.var_short_arr_a[n8] & 0x3FFF);
        int n9 = n6 - 63 & 0xFFF;
        this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] & 0x3FFF);
        int n10 = n6 - 1 & 0xFFF;
        this.var_short_arr_a[n10] = (short)(this.var_short_arr_a[n10] & 0x3FFF);
        int n11 = n6 & 0xFFF;
        this.var_short_arr_a[n11] = (short)(this.var_short_arr_a[n11] & 0x3FFF);
        int n12 = n6 + 1 & 0xFFF;
        this.var_short_arr_a[n12] = (short)(this.var_short_arr_a[n12] & 0x3FFF);
        int n13 = n6 + 63 & 0xFFF;
        this.var_short_arr_a[n13] = (short)(this.var_short_arr_a[n13] & 0x3FFF);
        int n14 = n6 + 64 & 0xFFF;
        this.var_short_arr_a[n14] = (short)(this.var_short_arr_a[n14] & 0x3FFF);
        int n15 = n6 + 65 & 0xFFF;
        this.var_short_arr_a[n15] = (short)(this.var_short_arr_a[n15] & 0x3FFF);
    }

    public final void void_f(int n) {
        int n2;
        this.a(0, 0, 64, 64, Short.MIN_VALUE);
        byte[] byArray = com.ulysseo.mad.c.byte_arr_a(n);
        int n3 = byArray[0] & 0xFF;
        int n4 = byArray[1] & 0xFF;
        if ((n3 | n4) != 0) {
            var_int_k = n3;
            r = n4;
        } else {
            this.var_boolean_k = true;
        }
        for (n2 = 0; n2 < 4; ++n2) {
            this.var_int_arr_a[n2] = 0;
        }
        this.var_int_arr_arr_a[0][0] = byArray[2] & 0xFF;
        this.var_int_arr_arr_a[0][5] = byArray[3] & 0xFF;
        this.var_int_arr_arr_a[0][6] = byArray[4] & 0xFF;
        this.var_int_arr_arr_a[0][7] = byArray[5] & 0xFF;
        this.var_int_arr_arr_a[1][5] = byArray[6] & 0xFF;
        this.var_int_arr_arr_a[1][6] = byArray[7] & 0xFF;
        this.var_int_arr_arr_a[1][7] = byArray[8] & 0xFF;
        this.var_int_arr_arr_a[0][8] = -1;
        this.var_int_arr_arr_a[1][8] = -1;
        this.var_boolean_l = true;
        this.aa = byArray[9] & 0xFF;
        this.aV = byArray[10] & 0xFF;
        this.f();
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
            while (n2 < this.var_int_arr_arr_a[i][4]) {
                int n3;
                int n4;
                int n5 = this.var_int_arr_arr_b[i][n + 2];
                if ((n5 & 0x40000000) != 0) {
                    n4 = this.var_int_arr_arr_b[i][n + 3] & 0xFF;
                    if (((n5 += 8) & 0xFFFF) >= 255) {
                        this.var_int_arr_arr_b[i][n + 2] = 255;
                        if (i == 0) {
                            this.ag = 20;
                            a a2 = new a(69);
                            n3 = n4;
                            if (n4 == 12) {
                                n3 += this.var_byte_arr_c[36];
                                n3 += this.var_byte_arr_c[40];
                                n3 += this.var_byte_arr_c[43];
                            }
                            this.var_java_lang_String_c = a2.a(n3);
                            this.var_java_lang_String_a = a2.a(16);
                            if (this.var_boolean_d && this.a(0, n4, true) == 1) {
                                this.g(0, 70, n4);
                            }
                        }
                        switch (n4) {
                            case 9: {
                                int[] nArray = this.var_int_arr_arr_a[i];
                                nArray[3] = nArray[3] + 5;
                                if (i != 0) break;
                                this.var_boolean_l = true;
                                break;
                            }
                            case 11: {
                                if (i == 0) {
                                    this.var_byte_arr_c[0] = 1;
                                    this.var_boolean_l = true;
                                }
                                int[] nArray = this.var_int_arr_arr_a[i];
                                nArray[3] = nArray[3] + 5;
                                break;
                            }
                            case 10: {
                                if (i != 0) break;
                                this.var_byte_arr_c[2] = 1;
                                break;
                            }
                            case 7: {
                                if (i != 0) break;
                                this.var_byte_arr_c[4] = 1;
                                break;
                            }
                            case 2: {
                                if (i != 0) break;
                                this.var_byte_arr_c[7] = 1;
                                break;
                            }
                            case 8: {
                                if (i != 0) break;
                                if (this.var_int_arr_arr_a[0][0] >= 2) {
                                    this.var_byte_arr_c[6] = 1;
                                    break;
                                }
                                this.var_byte_arr_c[5] = 1;
                                break;
                            }
                            case 3: {
                                if (i != 0) break;
                                this.var_byte_arr_c[9] = 1;
                                break;
                            }
                            case 5: {
                                int[] nArray = this.var_int_arr_arr_a[i];
                                nArray[56] = nArray[56] + (this.var_int_arr_arr_a[i][56] >> 1);
                                break;
                            }
                            case 12: {
                                int[] nArray = this.var_int_arr_arr_a[i];
                                nArray[55] = nArray[55] + (this.var_int_arr_arr_a[i][45] + this.var_int_arr_arr_a[i][46]);
                                int n6 = this.var_int_arr_arr_a[i][48] << 2;
                                this.var_short_arr_arr_b[i][n6 + 0] = (short)n;
                                this.var_short_arr_arr_b[i][n6 + 1] = 1000;
                                this.var_short_arr_arr_b[i][n6 + 3] = 0;
                                int[] nArray2 = this.var_int_arr_arr_a[i];
                                nArray2[48] = nArray2[48] + 1;
                                int[] nArray3 = this.var_int_arr_arr_b[i];
                                int n7 = n + 3;
                                nArray3[n7] = nArray3[n7] & 0xFFFFFF;
                                int[] nArray4 = this.var_int_arr_arr_b[i];
                                int n8 = n + 3;
                                nArray4[n8] = nArray4[n8] | n6 << 24;
                                break;
                            }
                            case 1: {
                                if (this.var_int_arr_arr_a[i][10] == -1) {
                                    this.var_int_arr_arr_a[i][10] = this.var_int_arr_arr_b[i][n + 0];
                                    break;
                                }
                                if (this.var_int_arr_arr_a[i][11] == -1) {
                                    this.var_int_arr_arr_a[i][11] = this.var_int_arr_arr_b[i][n + 0];
                                }
                                if (i != 0) break;
                                this.var_byte_arr_c[10 + n4] = 0;
                            }
                        }
                    } else {
                        int[] nArray = this.var_int_arr_arr_b[i];
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
                                    switch (this.var_int_arr_arr_b[0][n + 3] & 0xFF) {
                                        case 9: {
                                            int[] nArray = this.var_int_arr_arr_a[0];
                                            nArray[0] = nArray[0] + 1;
                                            if (this.var_int_arr_arr_a[0][0] == 1) {
                                                this.var_byte_arr_c[15] = 1;
                                                this.var_byte_arr_c[16] = 1;
                                                this.var_byte_arr_c[17] = 1;
                                                this.var_byte_arr_c[18] = 1;
                                                this.var_byte_arr_c[2] = 0;
                                                this.var_byte_arr_c[3] = 1;
                                                this.e(2, 3);
                                            } else if (this.var_int_arr_arr_a[0][0] == 2) {
                                                this.var_byte_arr_c[12] = 1;
                                                this.var_byte_arr_c[14] = 1;
                                                this.var_byte_arr_c[13] = 1;
                                                this.var_byte_arr_c[6] = this.var_byte_arr_c[5];
                                                this.var_byte_arr_c[5] = 0;
                                                this.e(5, 6);
                                            }
                                            this.var_boolean_l = true;
                                            this.g(0, 62, this.var_int_arr_arr_a[0][0] - 1);
                                            break;
                                        }
                                        case 0: {
                                            if (this.var_byte_arr_c[26] == 0) {
                                                n3 = 3;
                                                int[] nArray = this.var_int_arr_arr_a[0];
                                                nArray[50] = nArray[50] + 5;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[24] != 0) break;
                                            n3 = 1;
                                            int[] nArray = this.var_int_arr_arr_a[0];
                                            nArray[50] = nArray[50] + 5;
                                            break;
                                        }
                                        case 1: {
                                            if (this.var_byte_arr_c[28] == 0) {
                                                n3 = 5;
                                                int[] nArray = this.var_int_arr_arr_a[0];
                                                nArray[52] = nArray[52] + 3;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[32] == 0) {
                                                n3 = 9;
                                                int[] nArray = this.var_int_arr_arr_a[0];
                                                nArray[51] = nArray[51] + 3;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[42] == 0) {
                                                n3 = 19;
                                                int[] nArray = this.var_int_arr_arr_a[0];
                                                nArray[52] = nArray[52] + 3;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[41] != 0) break;
                                            n3 = 18;
                                            int[] nArray = this.var_int_arr_arr_a[0];
                                            nArray[51] = nArray[51] + 3;
                                            break;
                                        }
                                        case 5: {
                                            n3 = 6;
                                            int[] nArray = this.var_int_arr_arr_a[0];
                                            nArray[56] = nArray[56] + (this.var_int_arr_arr_a[0][56] >> 1);
                                            break;
                                        }
                                        case 6: {
                                            if (this.var_byte_arr_c[27] == 0) {
                                                n3 = 4;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n10 = 13 + j;
                                                    nArray[n10] = nArray[n10] + 1;
                                                }
                                            } else if (this.var_byte_arr_c[31] == 0) {
                                                n3 = 8;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n11 = 23 + j;
                                                    nArray[n11] = nArray[n11] + 1;
                                                }
                                            } else if (this.var_byte_arr_c[30] == 0) {
                                                n3 = 7;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n12 = 13 + j;
                                                    nArray[n12] = nArray[n12] + 1;
                                                }
                                            } else if (this.var_byte_arr_c[25] == 0) {
                                                n3 = 2;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n13 = 23 + j;
                                                    nArray[n13] = nArray[n13] + 1;
                                                }
                                            } else if (this.var_byte_arr_c[23] == 0) {
                                                n3 = 0;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n14 = 13 + j;
                                                    nArray[n14] = nArray[n14] + 1;
                                                }
                                            } else {
                                                if (this.var_byte_arr_c[38] != 0) break;
                                                n3 = 15;
                                                this.var_byte_arr_c[8] = 1;
                                                for (int j = 0; j < 9; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n15 = 23 + j;
                                                    nArray[n15] = nArray[n15] + 1;
                                                }
                                            }
                                            break;
                                        }
                                        case 4: {
                                            if (this.var_byte_arr_c[37] == 0) {
                                                n3 = 14;
                                                for (int j = 0; j < 13; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n16 = 33 + j;
                                                    nArray[n16] = nArray[n16] + 1;
                                                }
                                                break;
                                            }
                                            if (this.var_byte_arr_c[34] == 0) {
                                                n3 = 11;
                                                int[] nArray = this.var_int_arr_arr_a[0];
                                                nArray[42] = nArray[42] + 1;
                                                int[] nArray5 = this.var_int_arr_arr_a[0];
                                                nArray5[45] = nArray5[45] + 1;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[33] == 0) {
                                                n3 = 10;
                                                for (int j = 0; j < 13; ++j) {
                                                    int[] nArray = this.var_int_arr_arr_a[0];
                                                    int n17 = 33 + j;
                                                    nArray[n17] = nArray[n17] + 1;
                                                }
                                                break;
                                            }
                                            if (this.var_byte_arr_c[35] != 0) break;
                                            n3 = 12;
                                            int[] nArray = this.var_int_arr_arr_a[0];
                                            nArray[42] = nArray[42] + 1;
                                            int[] nArray6 = this.var_int_arr_arr_a[0];
                                            nArray6[45] = nArray6[45] + 1;
                                            break;
                                        }
                                        case 12: {
                                            if (this.var_byte_arr_c[36] == 0) {
                                                n3 = 13;
                                                this.var_int_arr_arr_a[0][47] = 0;
                                                this.var_int_arr_arr_a[0][46] = 2;
                                                this.var_int_arr_arr_a[0][45] = 15;
                                                this.var_int_arr_arr_a[0][12] = 25;
                                                this.var_int_arr_arr_a[1][12] = 25;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[40] == 0) {
                                                n3 = 17;
                                                this.var_int_arr_arr_a[0][47] = 3;
                                                this.var_int_arr_arr_a[0][46] = 3;
                                                this.var_int_arr_arr_a[0][45] = 20;
                                                this.var_int_arr_arr_a[0][12] = 36;
                                                this.var_int_arr_arr_a[1][12] = 36;
                                                break;
                                            }
                                            if (this.var_byte_arr_c[43] != 0) break;
                                            n3 = 20;
                                            this.var_int_arr_arr_a[0][47] = 2;
                                            this.var_int_arr_arr_a[0][46] = 4;
                                            this.var_int_arr_arr_a[0][45] = 25;
                                            this.var_int_arr_arr_a[0][12] = 36;
                                            this.var_int_arr_arr_a[1][12] = 36;
                                        }
                                    }
                                    if (n3 >= 0) {
                                        this.var_byte_arr_c[23 + n3] = 1;
                                        this.ag = 20;
                                        a a3 = new a(68);
                                        this.var_java_lang_String_c = a3.a(24);
                                        this.var_java_lang_String_a = a3.a(n3);
                                    }
                                    this.boolean_g(8);
                                }
                                int[] nArray = this.var_int_arr_arr_b[i];
                                int n18 = n + 2;
                                nArray[n18] = nArray[n18] & 0xFF;
                            } else {
                                int[] nArray = this.var_int_arr_arr_b[i];
                                int n19 = n + 2;
                                nArray[n19] = nArray[n19] + 2048;
                            }
                        } else {
                            int n20 = this.var_int_arr_arr_a[i][56];
                            if ((n5 & 0xFF00) + n20 < 65280) {
                                int[] nArray = this.var_int_arr_arr_b[i];
                                int n21 = n + 2;
                                nArray[n21] = nArray[n21] + n20;
                            } else {
                                n3 = 0;
                                switch (this.var_int_arr_arr_b[i][n + 3] & 0xFF) {
                                    case 11: {
                                        n3 = 1;
                                        if ((this.ar & 1) == 0) break;
                                        n3 = 0;
                                        break;
                                    }
                                    case 10: {
                                        if (this.var_int_arr_arr_a[0][0] == 0) {
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
                                        if (this.var_int_arr_arr_a[0][0] >= 2) {
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
                                if (this.boolean_a(i, 0, n3)) {
                                    int n22 = this.var_int_arr_arr_b[i][n + 0] >>> 8;
                                    int n23 = this.var_int_arr_arr_b[i][n + 0] & 0xFF;
                                    int n24 = n22 + 1;
                                    int n25 = n23 + 1;
                                    int n26 = 0;
                                    int n27 = 1;
                                    int n28 = 0;
                                    int n29 = 1;
                                    while ((this.var_short_arr_a[n24 + (n25 << 6) & 0xFFF] & 0xFFF) != 0 || ((n24 | n25) & 0xFFFFFFC0) != 0) {
                                        if (++n26 > 8) {
                                            ++n29;
                                            n26 = 0;
                                            n27 = 1;
                                        }
                                        int n30 = (n27 << 1 & 0xF) + 0;
                                        n24 = n22 + this.var_byte_arr_g[n30] * n29;
                                        n25 = n23 + this.var_byte_arr_g[n30 + 1] * n29;
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
                                        int[] nArray = this.var_int_arr_arr_a[i];
                                        nArray[49] = nArray[49] - 1;
                                        if (--n3 < 0) {
                                            n3 = 0;
                                        }
                                        int[] nArray7 = this.var_int_arr_arr_a[i];
                                        int n31 = 66 + n3;
                                        nArray7[n31] = nArray7[n31] - 1;
                                        if (n4 == 0) {
                                            int[] nArray8 = this.var_int_arr_arr_b[i];
                                            int n32 = n + 2;
                                            nArray8[n32] = nArray8[n32] & 0xFF0000FF;
                                        } else {
                                            int[] nArray9 = this.var_int_arr_arr_b[i];
                                            int n33 = n + 2;
                                            nArray9[n33] = nArray9[n33] & 0xFF0000FF;
                                            int[] nArray10 = this.var_int_arr_arr_b[i];
                                            int n34 = n + 2;
                                            nArray10[n34] = nArray10[n34] | n4;
                                        }
                                    } else {
                                        int[] nArray = this.var_int_arr_arr_b[i];
                                        int n35 = n + 2;
                                        nArray[n35] = nArray[n35] & 0xFF0000FF;
                                        int[] nArray11 = this.var_int_arr_arr_b[i];
                                        int n36 = n + 2;
                                        nArray11[n36] = nArray11[n36] & 0xFF00FFFF;
                                    }
                                    if (i == 0) {
                                        this.boolean_g(8);
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

    final void j(Graphics graphics) {
        int n;
        int n2;
        int n3;
        int n4 = 0xFF0000;
        if (this.var_int_b == 1) {
            n4 = 65280;
        }
        graphics.setColor(0);
        graphics.fillRect(0, 0, this.aO, this.var_int_j);
        int n5 = this.aB;
        int n6 = this.y + (this.N << 1);
        int n7 = (this.N << 1) - this.y;
        int n8 = -(n6 & 0x1F) + (n7 & 0x1F);
        int n9 = (n6 & 0x1F) + (n7 & 0x1F) >> 1;
        int n10 = n8 - 32;
        int n11 = 3 - n9;
        int n12 = 64;
        int n13 = n10;
        int n14 = n11;
        int n15 = n6 >>= 5;
        int n16 = n7 >>= 5;
        this.aP = -64;
        this.aS = -64;
        while (n5-- > 0) {
            n3 = n10 - n12;
            n2 = this.aI;
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
                    if (n == this.Q) {
                        this.aP = n3;
                        this.aS = n11;
                    }
                    if ((this.var_short_arr_a[n] & 0xFFF) == 768) {
                        this.a(graphics, 0, n3, n11, 0);
                    } else {
                        switch (this.var_short_arr_a[n] & 0xF000) {
                            case 0: 
                            case 4096: {
                                this.a(graphics, 4, n3, n11, 0);
                                break;
                            }
                            case 16384: {
                                this.a(graphics, 5, n3, n11, 0);
                            }
                        }
                    }
                } else {
                    this.a(graphics, 0, n3, n11, 0);
                }
                n3 += 64;
                ++n6;
                --n7;
            }
            if (n12 == 0) {
                n12 ^= 0x20;
                n6 -= this.aI;
                n7 += this.aI + 1;
            } else {
                n12 = 0;
                n6 -= this.aI - 1;
                n7 += this.aI;
            }
            n11 += 16;
        }
        if (this.Q >= 0 && (this.var_int_b != 1 || this.var_short_arr_a[this.Q] >= 0)) {
            graphics.setColor(n4);
            graphics.drawLine(this.aP - 32, this.aS, this.aP, this.aS - 16);
            graphics.drawLine(this.aP + 32, this.aS, this.aP, this.aS - 16);
            ++this.aP;
            graphics.drawLine(this.aP - 32, this.aS, this.aP, this.aS - 16);
            graphics.drawLine(this.aP + 32, this.aS, this.aP, this.aS - 16);
            --this.aP;
        }
        if (this.U > 0) {
            this.m(graphics);
        }
        n10 = n13;
        n11 = n14;
        n6 = n15;
        n7 = n16;
        n12 = 32;
        n5 = this.aB;
        while (n5-- > 0) {
            n3 = n10 - n12;
            n2 = this.aI;
            while (n2-- > 0) {
                if (((n6 | n7) & 0xFFFFFFC0) == 0) {
                    n = n6 + (n7 << 6);
                    short s = this.var_short_arr_a[n];
                    int n17 = s & 0x300;
                    if (s > 0) {
                        switch (n17) {
                            case 768: {
                                if ((this.var_short_arr_a[n] & 3) == 0) break;
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
                                if ((this.var_short_arr_arr_a[n18][(n19 << 3) + 7] & 0xFF) != 1) break;
                                this.void_d(n18, n19);
                                this.c(graphics, n3, n11, n);
                            }
                        }
                    }
                    if (n == this.Q && (this.var_int_b != 1 || this.var_short_arr_a[this.Q] >= 0)) {
                        graphics.setColor(n4);
                        graphics.drawLine(this.aP - 32, this.aS, this.aP, this.aS + 16);
                        graphics.drawLine(this.aP + 32, this.aS, this.aP, this.aS + 16);
                        ++this.aP;
                        graphics.drawLine(this.aP - 32, this.aS, this.aP, this.aS + 16);
                        graphics.drawLine(this.aP + 32, this.aS, this.aP, this.aS + 16);
                        --this.aP;
                    }
                }
                n3 += 64;
                ++n6;
                --n7;
            }
            if (n12 == 0) {
                n12 ^= 0x20;
                n6 -= this.aI;
                n7 += this.aI + 1;
            } else {
                n12 = 0;
                n6 -= this.aI - 1;
                n7 += this.aI;
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
        short s = this.var_short_arr_a[n3];
        int n8 = (s & 0xFF) << 3;
        int n9 = (s & 0xC00) >> 10;
        int n10 = this.var_short_arr_arr_a[n9][n8 + 3] & 0xFF;
        int n11 = n9;
        int n12 = this.var_short_arr_arr_a[n9][n8 + 7] & 0xF;
        if (n9 != 0 && (s & 0x4000) != 0 && n12 != 1) {
            return;
        }
        int n13 = 0;
        int n14 = 0;
        int n15 = this.var_short_arr_arr_a[n9][n8 + 7] >>> 8 & 0xFF;
        switch (n12) {
            case 0: 
            case 3: {
                n14 = (this.var_short_arr_arr_a[n9][n8 + 6] & 0xFF00) >> 8;
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
        int n16 = ((this.var_short_arr_arr_a[n9][n8 + 6] & 0xFF) << 1) + 80;
        int n17 = 0;
        if (n14 != 0) {
            n17 = this.ar & 1;
        }
        int n18 = n14 * this.var_byte_arr_g[n16++];
        n14 *= this.var_byte_arr_g[n16];
        int n19 = 0;
        if ((this.var_short_arr_arr_a[n9][n8 + 4] & 0x4000) != 0) {
            n19 = 2;
        }
        if ((this.var_short_arr_arr_a[n9][n8 + 4] & 0x2000) != 0) {
            n17 += 2;
        }
        if ((this.var_short_arr_arr_a[n9][n8 + 4] & 0x1000) != 0) {
            n11 = 3;
            short[] sArray = this.var_short_arr_arr_a[n9];
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
                    n5 = 12 * this.var_byte_arr_g[n16--];
                    n6 = 12 * this.var_byte_arr_g[n16];
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
                n7 = this.var_short_arr_arr_a[n9][n8 + 5] >>> 8;
                n6 = this.var_short_arr_arr_a[n9][n8 + 5] & 0xFF;
                n5 = ((n7 -= this.var_short_arr_arr_a[n9][n8 + 0] >>> 8) - (n6 -= this.var_short_arr_arr_a[n9][n8 + 0] & 0xFF)) * 26;
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
        n7 = this.var_short_arr_arr_a[n9][n8 + 4];
        n += n18;
        n2 += n14 - 5;
        n2 -= (this.var_byte_arr_d[n10] >> 1) + 5;
        n6 = 0;
        switch (this.var_short_arr_arr_a[n9][n8 + 7] & 0xF) {
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
                if (this.aJ == -1) {
                    n4 = 0;
                }
                graphics.setColor(n4);
                graphics.fillRect(n - 1, n2 - 1, 18, 4);
                n4 = this.var_int_h == -1 ? 8703 : 65280;
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
        short s = this.var_short_arr_a[n3];
        int n8 = (s & 0xFF) << 2;
        int n9 = (s & 0xC00) >> 10;
        int n10 = this.var_int_arr_arr_b[n9][n8 + 2];
        int n11 = this.var_int_arr_arr_b[n9][n8 + 3];
        int n12 = this.var_int_arr_arr_a[n9][0];
        int n13 = n9;
        if (n11 >= 12) {
            n7 = 0;
            n7 = 0 + this.var_byte_arr_c[36];
            n7 += this.var_byte_arr_c[40];
            n6 = this.var_byte_arr_h[((n11 & 0xFF) << 2) + (n7 += this.var_byte_arr_c[43])] + 33;
        } else {
            n6 = this.var_byte_arr_h[((n11 & 0xFF) << 2) + n12] + 33;
        }
        if ((n10 & 0x40000000) != 0) {
            n6 = 32;
            n7 = n10 & 0xFFFF;
            this.var_int_arr_arr_b[n9][n8 + 2] = n7 >= 255 ? 255 : n10 & 0xFFFFFF00 | n7;
        }
        if ((n10 & 0x10000000) != 0) {
            int[] nArray = this.var_int_arr_arr_b[n9];
            int n14 = n8 + 2;
            nArray[n14] = nArray[n14] & 0xEFFFFFFF;
            n13 = 3;
        }
        if ((n11 & 0xFFFFFF00) != 0 && n6 != 32) {
            n7 = n11 >>> 24;
            n5 = this.var_short_arr_arr_b[n9][n7 + 2];
            n4 = n5 >>> 8;
            n5 &= 0xFF;
            int n15 = ((n4 -= this.var_int_arr_arr_b[n9][n8 + 0] >>> 8) - (n5 -= this.var_int_arr_arr_b[n9][n8 + 0] & 0xFF)) * 26;
            int n16 = (n4 + n5) * 12;
            int n17 = this.var_int_arr_arr_a[n9][47];
            if (n16 >= -10) {
                this.a(graphics, n6, n, n2, n13);
                this.b(graphics, n, n2 - 32, n + n15, n2 + n16, this.var_short_arr_arr_b[n9][n7 + 3], n17);
            } else {
                this.b(graphics, n, n2 - 32, n + n15, n2 + n16, this.var_short_arr_arr_b[n9][n7 + 3], n17);
                this.a(graphics, n6, n, n2, n13);
            }
        } else {
            if ((this.var_short_arr_a[n3] & 0x4000) != 0) {
                n13 = 2;
            }
            this.a(graphics, n6, n, n2, n13);
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
        int n4 = 0 + (this.var_short_arr_a[n3] & 3);
        if ((this.var_short_arr_a[n3] & 0x4000) != 0) {
            this.a(graphics, n4, n, n2, 2);
        } else {
            this.a(graphics, n4, n, n2, 0);
        }
        if ((this.var_short_arr_a[n3] & 0x80) != 0) {
            graphics.setColor(0);
            graphics.fillRect(n -= 8, n2 -= 4, 18, 4);
            graphics.setColor(0xDCDCDC);
            graphics.fillRect(n - 1, n2 - 1, 18, 4);
            int n5 = ((this.var_short_arr_a[n3] & 0x7C) >> 2) + 1 >> 1;
            graphics.setColor(65280);
            graphics.fillRect(n, n2, n5, 2);
            graphics.setColor(0xFF0000);
            graphics.fillRect(n + n5, n2, 16 - n5, 2);
        }
    }

    public final boolean a(int n, int n2, int n3, int n4, boolean bl) {
        short s;
        int n5 = (n3 &= 0x3F) + ((n4 &= 0x3F) << 6);
        if ((this.var_short_arr_a[n5] & 0xFFF) != 0) {
            return false;
        }
        if (bl) {
            this.c(n, 0, n2);
        }
        this.e(n, 2, n2);
        int n6 = this.var_int_arr_arr_a[n][2];
        if (n6 >= 26) {
            return false;
        }
        int n7 = n5;
        this.var_short_arr_a[n7] = (short)(this.var_short_arr_a[n7] | (short)(0x200 | n6 | n << 10 & 0xC00));
        this.var_short_arr_arr_a[n][(n6 <<= 3) + 0] = s = (short)(n3 << 8 | n4);
        this.var_short_arr_arr_a[n][n6 + 2] = s;
        this.var_short_arr_arr_a[n][n6 + 1] = s;
        this.var_short_arr_arr_a[n][n6 + 3] = (short)n2;
        this.var_short_arr_arr_a[n][n6 + 4] = 255;
        this.var_short_arr_arr_a[n][n6 + 6] = 0;
        this.var_short_arr_arr_a[n][n6 + 7] = 0;
        if (!(this.var_int_h != n2 && this.var_int_h != -1 || this.Y != 0 || this.aJ != -1 || n != 0 || this.var_int_h == -1 && n2 < 2)) {
            short[] sArray = this.var_short_arr_arr_a[n];
            int n8 = n6 + 4;
            sArray[n8] = (short)(sArray[n8] | 0x8000);
        }
        if (n == 0) {
            this.void_d(0, this.var_int_arr_arr_a[n][2]);
        }
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[55] = nArray[55] + (this.var_int_arr_arr_a[n][23 + n2] + this.var_int_arr_arr_a[n][13 + n2]);
        int[] nArray2 = this.var_int_arr_arr_a[n];
        nArray2[2] = nArray2[2] + 1;
        int[] nArray3 = this.var_int_arr_arr_a[n];
        nArray3[86] = nArray3[86] + 1;
        if (--n2 < 0) {
            n2 = 0;
        }
        int[] nArray4 = this.var_int_arr_arr_a[n];
        int n9 = 57 + n2;
        nArray4[n9] = nArray4[n9] + 1;
        return true;
    }

    final int int_c(int n, int n2) {
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
        for (int i = 0; i < this.var_int_arr_arr_a[n][4]; ++i) {
            if ((this.var_int_arr_arr_b[n][n4 + 3] & 0xFF) == n3) {
                if ((this.var_int_arr_arr_b[n][n4 + 2] & 0x40000000) != 0) {
                    return -1;
                }
                int[] nArray = this.var_int_arr_arr_b[n];
                int n5 = n4 + 2;
                nArray[n5] = nArray[n5] + 65536;
                int[] nArray2 = this.var_int_arr_arr_a[n];
                nArray2[49] = nArray2[49] + 1;
                int n6 = n2 - 1;
                if (n6 < 0) {
                    n6 = 0;
                }
                int[] nArray3 = this.var_int_arr_arr_a[n];
                int n7 = 66 + n6;
                nArray3[n7] = nArray3[n7] + 1;
                return n4;
            }
            n4 += 4;
        }
        return -1;
    }

    final void e(int n, int n2) {
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
            int n6 = this.var_int_arr_arr_a[i][2];
            for (int j = 0; j < n6; ++j) {
                if ((this.var_short_arr_arr_a[i][n5 + 3] & 0xFF) == n) {
                    short[] sArray = this.var_short_arr_arr_a[i];
                    int n7 = n5 + 3;
                    sArray[n7] = (short)(sArray[n7] & 0xFF00);
                    short[] sArray2 = this.var_short_arr_arr_a[i];
                    int n8 = n5 + 3;
                    sArray2[n8] = (short)(sArray2[n8] | n2);
                }
                n5 += 8;
            }
            this.var_int_arr_arr_a[i][57 + n3] = this.var_int_arr_arr_a[i][57 + n4];
            this.var_int_arr_arr_a[i][57 + n4] = 0;
            this.var_int_arr_arr_a[i][66 + n3] = this.var_int_arr_arr_a[i][66 + n4];
            this.var_int_arr_arr_a[i][66 + n4] = 0;
        }
    }

    final void f(int n, int n2) {
        short s = this.var_short_arr_arr_a[n][n2 + 6];
        if ((s & 0xF00) != 0) {
            int n3 = 0;
            switch (this.var_short_arr_arr_a[n][n2 + 3] & 0xFF) {
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
            this.var_short_arr_arr_a[n][n2 + 6] = (short)(s - n3 & 0xF00 | s & 0xFF);
            if (s < this.var_short_arr_arr_a[n][n2 + 6]) {
                short[] sArray = this.var_short_arr_arr_a[n];
                int n4 = n2 + 6;
                sArray[n4] = (short)(sArray[n4] & 0xFF);
                return;
            }
        } else if (this.var_short_arr_arr_a[n][n2 + 0] != this.var_short_arr_arr_a[n][n2 + 2]) {
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
        this.l(0, n2);
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[2] = nArray[2] - 1;
        int n3 = this.var_int_arr_arr_a[n][2] << 3;
        int n4 = n2 << 3;
        if (n == 0 && n3 == 0 && this.var_int_arr_arr_a[0][4] == 0) {
            this.g(0, 98, 1);
        }
        short s = this.var_short_arr_arr_a[n][n4 + 0];
        int n5 = (s >>> 8 & 0x3F) + ((s & 0x3F) << 6);
        this.var_short_arr_a[n5] = 16384;
        int n6 = this.var_short_arr_arr_a[n][n4 + 3] & 0xFF;
        this.e(n, 0, n6);
        int[] nArray2 = this.var_int_arr_arr_a[n];
        nArray2[55] = nArray2[55] - (this.var_int_arr_arr_a[n][23 + n6] + this.var_int_arr_arr_a[n][13 + n6]);
        if (--n6 < 0) {
            n6 = 0;
        }
        int[] nArray3 = this.var_int_arr_arr_a[n];
        int n7 = 57 + n6;
        nArray3[n7] = nArray3[n7] - 1;
        if (n4 < n3) {
            s = this.var_short_arr_arr_a[n][n3 + 0];
            int n8 = n5 = (s >>> 8 & 0x3F) + ((s & 0x3F) << 6);
            this.var_short_arr_a[n8] = (short)(this.var_short_arr_a[n8] & 0xFFFFFF00);
            int n9 = n5;
            this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] | n2);
            for (int i = 0; i < 8; ++i) {
                this.var_short_arr_arr_a[n][n4++] = this.var_short_arr_arr_a[n][n3];
                this.var_short_arr_arr_a[n][n3++] = 0;
            }
        }
        int[] nArray4 = this.var_int_arr_arr_a[n];
        nArray4[87] = nArray4[87] + 1;
        if (n == 0) {
            this.var_boolean_l = true;
        }
    }

    final void g() {
        int n = this.ar & 8;
        for (int i = 0; i < 2; ++i) {
            int n2 = 0;
            int n3 = 0;
            while (n3 < this.var_int_arr_arr_a[i][2]) {
                int n4 = this.var_short_arr_arr_a[i][n2 + 7] & 0xF;
                switch (n4) {
                    case 0: {
                        if (n != 0 && this.var_short_arr_arr_a[i][n2 + 0] == this.var_short_arr_arr_a[i][n2 + 2] && (this.var_short_arr_arr_a[i][n2 + 4] & 0xFF) < 255) {
                            short[] sArray = this.var_short_arr_arr_a[i];
                            int n5 = n2 + 4;
                            sArray[n5] = (short)(sArray[n5] + 1);
                        }
                    }
                    case 3: {
                        this.f(i, n2);
                        break;
                    }
                    case 2: {
                        int n6;
                        int n7;
                        int n8;
                        int n9;
                        int n10 = this.var_short_arr_arr_a[i][n2 + 7] >>> 8;
                        if (--n10 == 0) {
                            this.var_short_arr_arr_a[i][n2 + 2] = (short)this.int_a(i, (int)this.var_short_arr_arr_a[i][n2 + 0], (int)this.var_short_arr_arr_a[i][n2 + 7]);
                            short[] sArray = this.var_short_arr_arr_a[i];
                            int n11 = n2 + 7;
                            sArray[n11] = (short)(sArray[n11] & 0xF0);
                            short[] sArray2 = this.var_short_arr_arr_a[i];
                            int n12 = n2 + 7;
                            sArray2[n12] = (short)(sArray2[n12] | 3);
                            n9 = this.var_short_arr_arr_a[i][n2 + 5] >>> 8 | (this.var_short_arr_arr_a[i][n2 + 5] & 0xFF) << 6;
                            n8 = (this.var_short_arr_a[n9] & 0x7C) >> 2;
                            if (--n8 == 0 || (this.var_short_arr_a[n9] & 0x300) != 768) {
                                if ((this.var_short_arr_a[n9] & 0x300) == 768) {
                                    int n13 = n9;
                                    this.var_short_arr_a[n13] = (short)(this.var_short_arr_a[n13] & 0xF000);
                                }
                                for (n7 = 0; n7 < 8; ++n7) {
                                    n6 = this.var_short_arr_arr_a[i][n2 + 5] >>> 8;
                                    int n14 = this.var_short_arr_arr_a[i][n2 + 5] & 0xFF;
                                    if ((((n6 += this.var_byte_arr_g[n7 << 1]) | (n14 += this.var_byte_arr_g[(n7 << 1) + 1])) & 0xFFFFFFC0) != 0 || (this.var_short_arr_a[n9 = n6 + (n14 << 6)] & 0x300) != 768 || (this.var_short_arr_arr_a[i][n2 + 7] & 0xF0) >> 4 != (this.var_short_arr_a[n9] & 3)) continue;
                                    this.var_short_arr_arr_a[i][n2 + 5] = (short)(n6 << 8 | n14);
                                    n7 = 10;
                                }
                                break;
                            }
                            int n15 = n9;
                            this.var_short_arr_a[n15] = (short)(this.var_short_arr_a[n15] & 0xFF83);
                            int n16 = n9;
                            this.var_short_arr_a[n16] = (short)(this.var_short_arr_a[n16] | n8 << 2);
                            int n17 = n9;
                            this.var_short_arr_a[n17] = (short)(this.var_short_arr_a[n17] | 0x80);
                            break;
                        }
                        short[] sArray = this.var_short_arr_arr_a[i];
                        int n18 = n2 + 7;
                        sArray[n18] = (short)(sArray[n18] & 0xFF);
                        short[] sArray3 = this.var_short_arr_arr_a[i];
                        int n19 = n2 + 7;
                        sArray3[n19] = (short)(sArray3[n19] | (short)(n10 << 8));
                        break;
                    }
                    case 1: {
                        int n10 = this.var_short_arr_arr_a[i][n2 + 7] & 0x7F00;
                        n10 += 256;
                        int n9 = this.var_short_arr_arr_a[i][n2 + 5] >>> 8;
                        int n8 = (this.var_short_arr_a[n9 += (this.var_short_arr_arr_a[i][n2 + 5] & 0xFF) << 6] & 0xC00) >> 10;
                        if (n8 == i || (this.var_short_arr_a[n9] & 0xFFF) == 0) {
                            this.var_short_arr_arr_a[i][n2 + 7] = 0;
                            break;
                        }
                        int n7 = 0;
                        int n6 = this.var_short_arr_arr_a[i][n2 + 3] & 0xFF;
                        if (n6 == 4 || n6 == 8) {
                            if (n10 >= 3840) {
                                n7 = 1;
                            }
                        } else if (n10 >= 2048) {
                            n7 = 1;
                        }
                        if (n7 != 0) {
                            short[] sArray = this.var_short_arr_arr_a[i];
                            int n20 = n2 + 7;
                            sArray[n20] = (short)(sArray[n20] & 0xFF);
                            this.d(i, n2, n9, n8);
                            break;
                        }
                        short[] sArray = this.var_short_arr_arr_a[i];
                        int n21 = n2 + 7;
                        sArray[n21] = (short)(sArray[n21] & 0xFF);
                        short[] sArray4 = this.var_short_arr_arr_a[i];
                        int n22 = n2 + 7;
                        sArray4[n22] = (short)(sArray4[n22] + (n10 & 0x7F00));
                        break;
                    }
                    case 4: {
                        int n10 = this.var_short_arr_arr_a[i][n2 + 7] & 0x7F00;
                        n10 += 256;
                        this.void_c(i, n2);
                        short[] sArray = this.var_short_arr_arr_a[i];
                        int n23 = n2 + 7;
                        sArray[n23] = (short)(sArray[n23] & 0xFF);
                        short[] sArray5 = this.var_short_arr_arr_a[i];
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
        switch (this.var_short_arr_a[n6] & 0x300) {
            case 768: {
                if ((this.var_short_arr_a[n6] & 3) == 0 || this.var_short_arr_arr_a[n][n2 + 3] > 1) break;
                n5 = 0x6602 | (this.var_short_arr_a[n6] & 3) << 4;
                this.var_short_arr_arr_a[n][n2 + 7] = (short)n5;
                this.var_short_arr_arr_a[n][n2 + 5] = (short)(n3 << 8 | n4);
                short[] sArray = this.var_short_arr_arr_a[n];
                int n7 = n2 + 3;
                sArray[n7] = (short)(sArray[n7] & 0xFF);
                break;
            }
            case 256: {
                int n8 = (this.var_short_arr_a[n6] & 0xC00) >> 10;
                if (n8 == n) {
                    int n9 = this.var_short_arr_arr_a[n][n2 + 3] & 0xFF;
                    if (n9 > 2) break;
                    if ((this.var_short_arr_arr_a[n][n2 + 7] & 0xF) == 3) {
                        int n10 = ((this.var_short_arr_arr_a[n][n2 + 7] & 0xF0) >> 4) - 1;
                        n5 = 0;
                        this.var_short_arr_arr_a[n][n2 + 7] = 0;
                        int n11 = 256;
                        if (n == 1) {
                            n11 = this.aM;
                        }
                        int[] nArray = this.var_int_arr_arr_a[n];
                        int n12 = 5 + n10;
                        nArray[n12] = nArray[n12] + (this.var_int_arr_arr_a[n][50 + n10] * n11 >> 8);
                        int[] nArray2 = this.var_int_arr_arr_a[n];
                        nArray2[90] = nArray2[90] + (n11 >> 8);
                        this.var_boolean_l = true;
                        if (n == 0) {
                            this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n][n2 + 5];
                            short[] sArray = this.var_short_arr_arr_a[n];
                            int n13 = n2 + 3;
                            sArray[n13] = (short)(sArray[n13] & 0xFF);
                            break;
                        }
                        int n14 = this.var_short_arr_arr_a[n][n2 + 5];
                        int n15 = n14 >>> 8;
                        int n16 = n15 + ((n14 &= 0xFF) << 6);
                        int n17 = this.int_b(n);
                        if (n17 == (this.var_short_arr_a[n16] & 0x303)) {
                            this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n][n2 + 5];
                            short[] sArray = this.var_short_arr_arr_a[n];
                            int n18 = n2 + 3;
                            sArray[n18] = (short)(sArray[n18] & 0xFF);
                            break;
                        }
                        this.short_a((int)this.var_short_arr_arr_a[n][n2 + 0], n17);
                        break;
                    }
                    int n19 = (this.var_short_arr_a[n6] & 0xFF) << 2;
                    if ((this.var_int_arr_arr_b[n][n19 + 2] & 0xFF) >= 255) break;
                    n5 = 4;
                    this.var_short_arr_arr_a[n][n2 + 7] = 4;
                    this.var_short_arr_arr_a[n][n2 + 5] = (short)(n3 << 8 | n4);
                    break;
                }
                n5 = 1;
                this.var_short_arr_arr_a[n][n2 + 7] = 1;
                int n20 = (this.var_short_arr_a[n6] & 0xFF) << 2;
                this.var_short_arr_arr_a[n][n2 + 5] = (short)(n3 << 8 | n4);
                int[] nArray = this.var_int_arr_arr_b[n8];
                int n21 = n20 + 2;
                nArray[n21] = nArray[n21] | Integer.MIN_VALUE;
                break;
            }
            case 512: {
                int n22 = (this.var_short_arr_a[n6] & 0xC00) >> 10;
                if (n22 == n) {
                    n5 = 0;
                    this.var_short_arr_arr_a[n][n2 + 7] = 0;
                    break;
                }
                n5 = 1;
                this.var_short_arr_arr_a[n][n2 + 7] = 1;
                int n23 = (this.var_short_arr_a[n6] & 0xFF) << 3;
                this.var_short_arr_arr_a[n][n2 + 5] = (short)(n3 << 8 | n4);
                int n24 = this.var_short_arr_arr_a[n][n2 + 0] >>> 8;
                int n25 = this.var_short_arr_arr_a[n][n2 + 0] & 0xFF;
                this.var_short_arr_arr_a[n22][n23 + 5] = (short)(n24 << 8 | n25);
                short[] sArray = this.var_short_arr_arr_a[n22];
                int n26 = n23 + 7;
                sArray[n26] = (short)(sArray[n26] & 0xFFF0);
                short[] sArray2 = this.var_short_arr_arr_a[n22];
                int n27 = n23 + 7;
                sArray2[n27] = (short)(sArray2[n27] | 1);
                this.var_short_arr_arr_a[n22][n23 + 6] = (short)((this.var_short_arr_arr_a[n][n2 + 6] & 0xFF) + 4 & 7);
                this.b(n22, n23, n24, n25);
            }
        }
        this.var_short_arr_arr_a[n][n2 + 7] = (short)n5;
    }

    final void d(int n, int n2, int n3, int n4) {
        int n5 = this.var_short_arr_arr_a[n][n2 + 3] & 0xFF;
        int n6 = this.var_short_arr_a[n3] & 0x300;
        switch (n6) {
            case 256: {
                int n7 = (this.var_short_arr_a[n3] & 0xFF) << 2;
                int n8 = this.var_int_arr_arr_b[n4][n7 + 2] & 0xFF;
                int n9 = this.var_int_arr_arr_b[n4][n7 + 3] & 0xFF;
                int n10 = this.var_int_arr_arr_a[n][13 + n5] << 4;
                if ((n8 -= (n10 /= this.var_int_arr_arr_a[n4][33 + n9])) > 0) {
                    int[] nArray = this.var_int_arr_arr_b[n4];
                    int n11 = n7 + 2;
                    nArray[n11] = nArray[n11] & 0xFFFFFF00;
                    int[] nArray2 = this.var_int_arr_arr_b[n4];
                    int n12 = n7 + 2;
                    nArray2[n12] = nArray2[n12] | (n8 | Integer.MIN_VALUE | 0x10000000);
                } else {
                    this.i(n4, this.var_short_arr_a[n3] & 0xFF);
                    this.var_short_arr_arr_a[n][n2 + 7] = 0;
                }
                if (n4 != 0) break;
                this.v();
                return;
            }
            case 512: {
                int n13 = (this.var_short_arr_a[n3] & 0xFF) << 3;
                int n14 = this.var_short_arr_arr_a[n4][n13 + 4] & 0xFF;
                int n15 = this.var_short_arr_arr_a[n4][n13 + 3] & 0xFF;
                int n16 = this.var_int_arr_arr_a[n][13 + n5] << 4;
                if ((n14 -= (n16 /= this.var_int_arr_arr_a[n4][23 + n15])) > 0) {
                    short[] sArray = this.var_short_arr_arr_a[n4];
                    int n17 = n13 + 4;
                    sArray[n17] = (short)(sArray[n17] & 0xFF00);
                    short[] sArray2 = this.var_short_arr_arr_a[n4];
                    int n18 = n13 + 4;
                    sArray2[n18] = (short)(sArray2[n18] | (n14 | 0x1000));
                    this.var_short_arr_arr_a[n4][n13 + 2] = this.var_short_arr_arr_a[n][n2 + 0];
                    return;
                }
                this.g(n4, this.var_short_arr_a[n3] & 0xFF);
                this.var_short_arr_arr_a[n][n2 + 7] = 0;
            }
        }
    }

    final void void_c(int n, int n2) {
        int n3 = this.var_short_arr_arr_a[n][n2 + 5] >>> 8;
        int n4 = (this.var_short_arr_a[n3 += (this.var_short_arr_arr_a[n][n2 + 5] & 0xFF) << 6] & 0xFF) << 2;
        int n5 = this.var_int_arr_arr_b[n][n4 + 2] & 0xFF;
        if ((AgeOfEmpires.c.int_a() & 1) == 0) {
            ++n5;
        }
        if (n5 <= 255) {
            int[] nArray = this.var_int_arr_arr_b[n];
            int n6 = n4 + 2;
            nArray[n6] = nArray[n6] & 0xFFFFFF00;
            int[] nArray2 = this.var_int_arr_arr_b[n];
            int n7 = n4 + 2;
            nArray2[n7] = nArray2[n7] | (n5 | Integer.MIN_VALUE);
            return;
        }
        this.var_short_arr_arr_a[n][n2 + 7] = 0;
    }

    public final int a(int n, int n2, int n3, int n4, int n5, boolean bl) {
        int n6;
        int n7;
        int n8 = this.var_int_arr_arr_a[n][4];
        if (n8 >= 22) {
            return -1;
        }
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[88] = nArray[88] + 1;
        this.e(n, 3, n2);
        int n9 = n7 = (n3 &= 0x3F) + ((n4 &= 0x3F) << 6);
        this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] | (short)(0x100 | n << 10 & 0xC00 | n8 & 0xFF));
        n8 <<= 2;
        if (n == 0 && (n5 & 0x40000000) == 0) {
            this.void_a(n3, n4, 3);
        }
        switch (n2) {
            case 0: {
                this.var_int_arr_arr_a[n][9] = n3 << 8 | n4;
                break;
            }
            case 9: {
                this.var_int_arr_arr_a[n][8] = n3 << 8 | n4;
                if ((n5 & 0x40000000) != 0) break;
                int[] nArray2 = this.var_int_arr_arr_a[n];
                nArray2[3] = nArray2[3] + 5;
                break;
            }
            case 11: {
                if ((n5 & 0x40000000) != 0) break;
                int[] nArray3 = this.var_int_arr_arr_a[n];
                nArray3[3] = nArray3[3] + 5;
                break;
            }
            case 12: {
                if ((n5 & 0x40000000) != 0) break;
                n6 = this.var_int_arr_arr_a[n][48] << 2;
                this.var_short_arr_arr_b[n][n6 + 0] = (short)n8;
                this.var_short_arr_arr_b[n][n6 + 1] = 1000;
                this.var_short_arr_arr_b[n][n6 + 2] = (short)((n3 << 8) + n4);
                this.var_short_arr_arr_b[n][n6 + 3] = 0;
                n2 |= n6 << 24;
                int[] nArray4 = this.var_int_arr_arr_a[n];
                nArray4[48] = nArray4[48] + 1;
            }
        }
        this.var_int_arr_arr_b[n][n8 + 0] = (n3 << 8) + n4;
        this.var_int_arr_arr_b[n][n8 + 1] = n2;
        this.var_int_arr_arr_b[n][n8 + 2] = n5;
        this.var_int_arr_arr_b[n][n8 + 3] = n2;
        n2 &= 0xFF;
        int[] nArray5 = this.var_int_arr_arr_a[n];
        nArray5[4] = nArray5[4] + 1;
        if (bl) {
            n6 = 0;
            if (n2 == 12) {
                n6 = 0 + this.var_byte_arr_c[36];
                n6 += this.var_byte_arr_c[40];
                n6 += this.var_byte_arr_c[43];
            }
            this.c(n, 1, n2 + n6);
        }
        if (n2 != 1 && n2 != 11 && n2 != 12 && n == 0) {
            this.var_byte_arr_c[10 + n2] = 0;
        }
        return n8;
    }

    final int a(int n, int n2, boolean bl) {
        int n3 = 0;
        int n4 = 0;
        int n5 = this.var_int_arr_arr_a[n][4];
        int n6 = 0;
        while (n6 < n5) {
            if ((this.var_int_arr_arr_b[n][n4 + 3] & 0xFF) == n2) {
                if (bl) {
                    if ((this.var_int_arr_arr_b[n][n4 + 2] & 0x40000000) == 0) {
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

    public final void i(int n, int n2) {
        int n3 = this.var_int_arr_arr_a[n][4] - 1;
        int[] nArray = this.var_int_arr_arr_a[n];
        nArray[89] = nArray[89] + 1;
        int n4 = n2 << 2;
        int n5 = n3 << 2;
        int n6 = this.var_int_arr_arr_b[n][n4 + 0];
        int n7 = (n6 >>> 8 & 0x3F) + ((n6 & 0x3F) << 6);
        this.var_short_arr_a[n7] = 0;
        int n8 = this.var_int_arr_arr_b[n][n4 + 3] & 0xFF;
        if (n == 0) {
            this.var_byte_arr_c[10 + n8] = 1;
        }
        this.e(n, 1, n8);
        switch (n8) {
            case 9: {
                if (n == 0) {
                    this.g(0, 98, 1);
                    break;
                }
                if (this.ac == 32 && this.aC != 0) break;
                this.g(0, 98, 0);
                break;
            }
            case 11: {
                if ((this.var_int_arr_arr_b[n][n4 + 2] & 0x40000000) == 0) {
                    int[] nArray2 = this.var_int_arr_arr_a[n];
                    nArray2[3] = nArray2[3] - 5;
                }
                this.var_boolean_l = true;
                break;
            }
            case 0: {
                this.var_int_arr_arr_a[n][9] = -1;
                break;
            }
            case 1: {
                int n9 = this.var_int_arr_arr_b[n][n4 + 0];
                if (n9 == this.var_int_arr_arr_a[n][10]) {
                    this.var_int_arr_arr_a[n][10] = -1;
                    break;
                }
                if (n9 != this.var_int_arr_arr_a[n][11]) break;
                this.var_int_arr_arr_a[n][11] = -1;
                break;
            }
            case 12: {
                if ((this.var_int_arr_arr_b[n][n4 + 2] & 0x40000000) != 0) break;
                int n10 = this.var_int_arr_arr_a[n][48];
                int n11 = n10 - 1 << 2;
                int n12 = this.var_int_arr_arr_b[n][n4 + 3] >>> 24 & 0xFF;
                if (n11 != n12) {
                    int n13;
                    for (n13 = 0; n13 < 4; ++n13) {
                        this.var_short_arr_arr_b[n][n12 + n13] = this.var_short_arr_arr_b[n][n11 + n13];
                    }
                    n13 = this.var_short_arr_arr_b[n][n11 + 0];
                    int n14 = this.var_short_arr_arr_b[n][n12 + 2] & 0xFFFF;
                    this.var_int_arr_arr_b[n][n13 + 3] = n12 << 24 | n14 << 8 | 0xC;
                }
                int[] nArray3 = this.var_int_arr_arr_a[n];
                nArray3[48] = nArray3[48] - 1;
            }
        }
        if (n4 != n5) {
            n6 = this.var_int_arr_arr_b[n][n5];
            ++n5;
            int n15 = n7 = (n6 >>> 8 & 0x3F) + ((n6 & 0x3F) << 6);
            this.var_short_arr_a[n15] = (short)(this.var_short_arr_a[n15] & 0xFFFFFF00);
            int n16 = n7;
            this.var_short_arr_a[n16] = (short)(this.var_short_arr_a[n16] | n2);
            this.var_int_arr_arr_b[n][n4] = n6;
            this.var_int_arr_arr_b[n][++n4] = this.var_int_arr_arr_b[n][n5];
            this.var_int_arr_arr_b[n][n5] = 0;
            this.var_int_arr_arr_b[n][++n4] = this.var_int_arr_arr_b[n][++n5];
            this.var_int_arr_arr_b[n][n5] = 0;
            this.var_int_arr_arr_b[n][++n4] = this.var_int_arr_arr_b[n][++n5];
            this.var_int_arr_arr_b[n][n5] = 0;
        } else {
            this.var_int_arr_arr_b[n][n4++] = 0;
            this.var_int_arr_arr_b[n][n4++] = 0;
            this.var_int_arr_arr_b[n][n4++] = 0;
            this.var_int_arr_arr_b[n][n4] = 0;
        }
        int[] nArray4 = this.var_int_arr_arr_a[n];
        nArray4[4] = nArray4[4] - 1;
    }

    final boolean boolean_b(int n, int n2) {
        int n3;
        int n4 = this.var_short_arr_arr_a[n][(n2 <<= 3) + 2];
        if (n4 == (n3 = this.var_short_arr_arr_a[n][n2 + 0])) {
            return false;
        }
        short s = this.var_short_arr_arr_a[n][n2 + 1];
        int n5 = n3 & 0xFF;
        short s2 = (short)(this.var_short_arr_a[(n3 >>>= 8) + (n5 << 6)] & 0xFFF);
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
        int n13 = this.var_short_arr_arr_a[n][n2 + 3] >> 8;
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
        if ((n3 & 0xFF) > 63) {
            n6 = n3;
        }
        if ((n5 & 0xFF) > 63) {
            n7 = n5;
        }
        short[] sArray = this.var_short_arr_arr_a[n];
        int n14 = n2 + 3;
        sArray[n14] = (short)(sArray[n14] & 0xFF);
        short[] sArray2 = this.var_short_arr_arr_a[n];
        int n15 = n2 + 3;
        sArray2[n15] = (short)(sArray2[n15] | (short)n13 << 8);
        int n16 = (n3 << 8) + n5;
        if ((this.var_short_arr_a[n3 + (n5 << 6)] & 0xFFF) != 0 || n16 == s) {
            int n17;
            short[] sArray3 = this.var_short_arr_arr_a[n];
            int n18 = n2 + 3;
            sArray3[n18] = (short)(sArray3[n18] & 0xFF);
            if (n16 == this.var_short_arr_arr_a[n][n2 + 2]) {
                short s3;
                this.var_short_arr_arr_a[n][n2 + 1] = s3 = this.var_short_arr_arr_a[n][n2 + 0];
                this.var_short_arr_arr_a[n][n2 + 0] = (short)n16;
                this.void_a(n, n2 >> 3);
                short[] sArray4 = this.var_short_arr_arr_a[n];
                int n19 = n2 + 6;
                sArray4[n19] = (short)(sArray4[n19] & 0xFF);
                this.var_short_arr_arr_a[n][n2 + 2] = s3;
                this.var_short_arr_arr_a[n][n2 + 0] = s3;
                this.c(n, n2, n3, n5);
                return false;
            }
            int n20 = ((this.var_short_arr_arr_a[n][n2 + 6] & 0xFF) << 3) + 16;
            n3 = n6;
            n5 = n7;
            for (n17 = 0; n17 < 7; ++n17) {
                int n21;
                byte by = this.var_byte_arr_g[n20++];
                int n22 = n5 + this.var_byte_arr_g[by + 1];
                if ((((n21 = n3 + this.var_byte_arr_g[by]) | n22) & 0xFFFFFFC0) != 0 || (this.var_short_arr_a[n21 + (n22 << 6)] & 0xFFF) != 0 || (n21 << 8 | n22) == s) continue;
                n3 = n21;
                n5 = n22;
                short[] sArray5 = this.var_short_arr_arr_a[n];
                int n23 = n2 + 3;
                sArray5[n23] = (short)(sArray5[n23] & 0xFF);
                n17 = 10;
            }
            if (n17 == 7) {
                short[] sArray6 = this.var_short_arr_arr_a[n];
                int n24 = n2 + 6;
                sArray6[n24] = (short)(sArray6[n24] & 0xFF);
                return false;
            }
        }
        int n25 = n6 + (n7 << 6);
        this.var_short_arr_a[n25] = (short)(this.var_short_arr_a[n25] & 0xFFFFF000);
        this.var_short_arr_a[(n3 &= 0x3F) + ((n5 &= 0x3F) << 6)] = (short)(this.var_short_arr_a[n3 + (n5 << 6)] & 0xFFFFF000 | s2);
        this.var_short_arr_arr_a[n][n2 + 1] = this.var_short_arr_arr_a[n][n2 + 0];
        this.var_short_arr_arr_a[n][n2 + 0] = (short)(n3 << 8 | n5);
        return true;
    }

    final void void_a(int n, int n2) {
        int n3 = n2 << 3;
        int n4 = (this.var_short_arr_arr_a[n][n3 + 0] & 0xFF00) >>> 8;
        int n5 = this.var_short_arr_arr_a[n][n3 + 0] & 0xFF;
        int n6 = (this.var_short_arr_arr_a[n][n3 + 1] & 0xFF00) >>> 8;
        int n7 = this.var_short_arr_arr_a[n][n3 + 1] & 0xFF;
        int n8 = n6 - n4;
        int n9 = n7 - n5;
        int n10 = n8 << 8 & 0xFF00 | n9 & 0xFF;
        int n11 = 0;
        switch (n10) {
            case 256: {
                n11 = 0;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n12 = n3 + 4;
                sArray[n12] = (short)(sArray[n12] | 0x2000);
                short[] sArray2 = this.var_short_arr_arr_a[n];
                int n13 = n3 + 4;
                sArray2[n13] = (short)(sArray2[n13] & 0xBFFF);
                break;
            }
            case 257: {
                n11 = 1;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n14 = n3 + 4;
                sArray[n14] = (short)(sArray[n14] | 0x2000);
                break;
            }
            case 1: {
                n11 = 2;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n15 = n3 + 4;
                sArray[n15] = (short)(sArray[n15] | 0x2000);
                short[] sArray3 = this.var_short_arr_arr_a[n];
                int n16 = n3 + 4;
                sArray3[n16] = (short)(sArray3[n16] | 0x4000);
                break;
            }
            case 65281: {
                n11 = 3;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n17 = n3 + 4;
                sArray[n17] = (short)(sArray[n17] | 0x4000);
                break;
            }
            case 65280: {
                n11 = 4;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n18 = n3 + 4;
                sArray[n18] = (short)(sArray[n18] & 0xDFFF);
                short[] sArray4 = this.var_short_arr_arr_a[n];
                int n19 = n3 + 4;
                sArray4[n19] = (short)(sArray4[n19] | 0x4000);
                break;
            }
            case 65535: {
                n11 = 5;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n20 = n3 + 4;
                sArray[n20] = (short)(sArray[n20] & 0xDFFF);
                break;
            }
            case 255: {
                n11 = 6;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n21 = n3 + 4;
                sArray[n21] = (short)(sArray[n21] & 0xDFFF);
                short[] sArray5 = this.var_short_arr_arr_a[n];
                int n22 = n3 + 4;
                sArray5[n22] = (short)(sArray5[n22] & 0xBFFF);
                break;
            }
            case 511: {
                n11 = 7;
                short[] sArray = this.var_short_arr_arr_a[n];
                int n23 = n3 + 4;
                sArray[n23] = (short)(sArray[n23] & 0xBFFF);
            }
        }
        this.var_short_arr_arr_a[n][n3 + 6] = (short)(n11 | 0xF00);
    }

    final void b(int n, int n2, int n3, int n4) {
        int n5 = (this.var_short_arr_arr_a[n][n2 + 0] & 0xFF00) >>> 8;
        int n6 = this.var_short_arr_arr_a[n][n2 + 0] & 0xFF;
        n5 = (n3 - n5) * 181;
        n6 = (n4 - n6) * 181;
        int n7 = n5 - n6;
        int n8 = n6 + n5;
        if (n7 > 0) {
            short[] sArray = this.var_short_arr_arr_a[n];
            int n9 = n2 + 4;
            sArray[n9] = (short)(sArray[n9] | 0x4000);
        } else if (n7 < 0) {
            short[] sArray = this.var_short_arr_arr_a[n];
            int n10 = n2 + 4;
            sArray[n10] = (short)(sArray[n10] & 0xBFFF);
        }
        if (n8 < 0) {
            short[] sArray = this.var_short_arr_arr_a[n];
            int n11 = n2 + 4;
            sArray[n11] = (short)(sArray[n11] | 0x2000);
            return;
        }
        if (n8 > 0) {
            short[] sArray = this.var_short_arr_arr_a[n];
            int n12 = n2 + 4;
            sArray[n12] = (short)(sArray[n12] & 0xDFFF);
        }
    }

    final void G() {
        for (int i = 0; i < 2; ++i) {
            int n = this.var_int_arr_arr_a[i][48];
            if (n == 0) continue;
            int n2 = this.ar % n << 2;
            int n3 = n;
            int n4 = n3 << 2;
            while (this.var_short_arr_arr_b[i][n2 + 1] != 1000) {
                if ((n2 += 4) >= n4) {
                    n2 = 0;
                }
                if (--n3 > 0) continue;
            }
            if (n3 <= 0) continue;
            int n5 = this.var_int_arr_arr_a[i][12];
            short s = this.var_short_arr_arr_b[i][n2 + 0];
            int n6 = this.var_int_arr_arr_b[i][s + 0];
            int n7 = n6 >>> 8 & 0x3F;
            n6 &= 0x3F;
            int n8 = i ^ 1;
            int n9 = 0;
            int n10 = this.var_int_arr_arr_a[n8][2];
            int n11 = 0;
            while (n11 < n10) {
                int n12 = this.var_short_arr_arr_a[n8][n9 + 0];
                int n13 = (n12 >>> 8 & 0x3F) - n7;
                if (n13 * n13 + (n12 = (n12 & 0x3F) - n6) * n12 <= n5) {
                    this.var_short_arr_arr_b[i][n2 + 3] = 0;
                    this.var_short_arr_arr_b[i][n2 + 1] = (short)n9;
                    this.var_short_arr_arr_b[i][n2 + 2] = this.var_short_arr_arr_a[n8][n9 + 0];
                    int[] nArray = this.var_int_arr_arr_b[i];
                    int n14 = s + 3;
                    nArray[n14] = nArray[n14] & 0xFF0000FF;
                    int[] nArray2 = this.var_int_arr_arr_b[i];
                    int n15 = s + 3;
                    nArray2[n15] = nArray2[n15] | (this.var_short_arr_arr_b[i][n2 + 2] & 0xFFFF) << 8;
                    n11 = 1000;
                }
                ++n11;
                n9 += 8;
            }
        }
    }

    final void J() {
        for (int i = 0; i < 2; ++i) {
            int n = 0;
            int n2 = this.var_int_arr_arr_a[i][48];
            int n3 = 0;
            while (n3 < n2) {
                short s = this.var_short_arr_arr_b[i][n + 1];
                if (s != 1000) {
                    int n4 = this.var_short_arr_arr_b[i][n + 3] + 1;
                    this.var_short_arr_arr_b[i][n + 3] = (short)(n4 & 0xF);
                    if (n4 >= 16) {
                        int n5 = this.var_short_arr_arr_b[i][n + 2];
                        int n6 = n5 >>> 8 & 0x3F;
                        int n7 = n6 + ((n5 &= 0x3F) << 6);
                        int n8 = (this.var_short_arr_a[n7] & 0xC00) >> 10;
                        if (n8 == i || (this.var_short_arr_a[n7] & 0xFFF) == 0) {
                            this.var_short_arr_arr_b[i][n + 1] = 1000;
                            int[] nArray = this.var_int_arr_arr_b[i];
                            int n9 = this.var_short_arr_arr_b[i][n + 0] + 3;
                            nArray[n9] = nArray[n9] & 0xFF0000FF;
                        } else if ((this.var_short_arr_a[n7] & 0x300) == 512) {
                            int n10 = this.var_short_arr_arr_a[n8][s + 4] & 0xFF;
                            int n11 = (this.var_int_arr_arr_a[i][46] << 4) / this.var_int_arr_arr_a[n8][23 + (this.var_short_arr_arr_a[n8][s + 3] & 0xFF)];
                            if ((n10 -= n11) <= 0) {
                                this.g(n8, this.var_short_arr_a[n7] & 0xFF);
                                this.var_short_arr_arr_b[i][n + 1] = 1000;
                                int[] nArray = this.var_int_arr_arr_b[i];
                                int n12 = this.var_short_arr_arr_b[i][n + 0] + 3;
                                nArray[n12] = nArray[n12] & 0xFF0000FF;
                            } else {
                                short[] sArray = this.var_short_arr_arr_a[n8];
                                int n13 = s + 4;
                                sArray[n13] = (short)(sArray[n13] & 0xFF00);
                                short[] sArray2 = this.var_short_arr_arr_a[n8];
                                int n14 = s + 4;
                                sArray2[n14] = (short)(sArray2[n14] | (n10 | 0x1000));
                                if (n8 == 0) {
                                    if (this.var_short_arr_arr_a[n8][s + 2] == this.var_short_arr_arr_a[n8][s + 0] && (this.var_short_arr_arr_a[n8][s + 7] & 0xFF) != 1) {
                                        this.var_short_arr_arr_a[n8][s + 2] = (short)this.var_int_arr_arr_b[i][this.var_short_arr_arr_b[i][n + 0] + 0];
                                    }
                                } else if ((this.var_short_arr_arr_a[n8][s + 7] & 0xFF) != 1) {
                                    this.var_short_arr_arr_a[n8][s + 2] = (short)this.var_int_arr_arr_b[i][this.var_short_arr_arr_b[i][n + 0] + 0];
                                }
                            }
                        } else {
                            this.var_short_arr_arr_b[i][n + 1] = 1000;
                            int[] nArray = this.var_int_arr_arr_b[i];
                            int n15 = this.var_short_arr_arr_b[i][n + 0] + 3;
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
            int n = this.ar;
            int n2 = this.var_int_arr_arr_a[i][2];
            if (n2 == 0) continue;
            int n3 = 0;
            while (n3 < n2) {
                int n4;
                ++n3;
                if ((this.var_short_arr_arr_a[i][(n4 = n++ % n2 << 3) + 7] & 0xF) != 1 && (this.var_short_arr_arr_a[i][n4 + 6] & 0xFF00) == 0 && (this.var_short_arr_arr_a[i][n4 + 3] & 0xFF) >= 2 && (i != 0 || this.var_short_arr_arr_a[i][n4 + 0] == this.var_short_arr_arr_a[i][n4 + 2])) {
                    if (!this.boolean_a(i, n4)) {
                        this.void_b(i, n4);
                    }
                    bl = true;
                }
                if (!bl) continue;
            }
        }
    }

    final boolean boolean_a(int n, int n2) {
        int n3 = this.var_short_arr_arr_a[n][n2 + 3] & 0xFF;
        int n4 = this.var_short_arr_arr_a[n][n2 + 0];
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
        int n9 = this.var_int_arr_arr_a[n7][2];
        int n10 = 0;
        while (n10 < n9) {
            int n11 = this.var_short_arr_arr_a[n7][n8 + 0];
            int n12 = (n11 >>> 8) - n5;
            int n13 = n12 * n12 + (n11 = (n11 & 0xFF) - n4) * n11;
            if (n13 <= 1) {
                short[] sArray = this.var_short_arr_arr_a[n];
                int n14 = n2 + 7;
                sArray[n14] = (short)(sArray[n14] & 0xFFF0);
                short[] sArray2 = this.var_short_arr_arr_a[n];
                int n15 = n2 + 7;
                sArray2[n15] = (short)(sArray2[n15] | 1);
                this.var_short_arr_arr_a[n][n2 + 5] = this.var_short_arr_arr_a[n7][n8 + 0];
                this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n][n2 + 0];
                this.b(n, n2, this.var_short_arr_arr_a[n7][n8 + 0] >>> 8, this.var_short_arr_arr_a[n7][n8 + 0] & 0xFF);
                if ((this.var_short_arr_arr_a[n7][n8 + 7] & 0xF) == 1) {
                    return true;
                }
                short[] sArray3 = this.var_short_arr_arr_a[n7];
                int n16 = n8 + 7;
                sArray3[n16] = (short)(sArray3[n16] & 0xFFF0);
                short[] sArray4 = this.var_short_arr_arr_a[n7];
                int n17 = n8 + 7;
                sArray4[n17] = (short)(sArray4[n17] | 1);
                this.var_short_arr_arr_a[n7][n8 + 5] = this.var_short_arr_arr_a[n][n2 + 0];
                this.var_short_arr_arr_a[n7][n8 + 2] = this.var_short_arr_arr_a[n7][n8 + 0];
                this.b(n7, n8, n5, n4);
                return true;
            }
            if (n13 <= n6) {
                switch (n3) {
                    case 4: 
                    case 8: {
                        short[] sArray = this.var_short_arr_arr_a[n];
                        int n18 = n2 + 7;
                        sArray[n18] = (short)(sArray[n18] & 0xFFF0);
                        short[] sArray5 = this.var_short_arr_arr_a[n];
                        int n19 = n2 + 7;
                        sArray5[n19] = (short)(sArray5[n19] | 1);
                        this.var_short_arr_arr_a[n][n2 + 5] = this.var_short_arr_arr_a[n7][n8 + 0];
                        this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n][n2 + 0];
                        this.b(n, n2, this.var_short_arr_arr_a[n7][n8 + 0] >>> 8, this.var_short_arr_arr_a[n7][n8 + 0] & 0xFF);
                        if ((this.var_short_arr_arr_a[n7][n8 + 7] & 0xF) == 1) {
                            return true;
                        }
                        this.var_short_arr_arr_a[n7][n8 + 2] = this.var_short_arr_arr_a[n][n2 + 0];
                        this.b(n7, n8, n5, n4);
                        return true;
                    }
                }
                this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n7][n8 + 0];
                return true;
            }
            ++n10;
            n8 += 8;
        }
        return false;
    }

    final void void_b(int n, int n2) {
        int n3 = this.var_short_arr_arr_a[n][n2 + 0];
        int n4 = n3 >>> 8 & 0x3F;
        n3 &= 0x3F;
        int n5 = this.var_short_arr_arr_a[n][n2 + 3] & 0xFF;
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
        int n11 = this.var_int_arr_arr_a[n9][4];
        int n12 = 0;
        while (n12 < n11) {
            int n13;
            int n14 = this.var_int_arr_arr_b[n9][n10 + 0];
            int n15 = n14 >>> 8 & 0x3F;
            n14 &= 0x3F;
            if ((n13 = (n15 -= n4) * n15 + (n14 -= n3) * n14) > 0 && n13 <= n6) {
                if ((this.var_int_arr_arr_b[n9][n10 + 3] & 0xFF) == 12) {
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
                    short[] sArray = this.var_short_arr_arr_a[n];
                    int n16 = n2 + 7;
                    sArray[n16] = (short)(sArray[n16] & 0xFFF0);
                    short[] sArray2 = this.var_short_arr_arr_a[n];
                    int n17 = n2 + 7;
                    sArray2[n17] = (short)(sArray2[n17] | 1);
                    this.var_short_arr_arr_a[n][n2 + 5] = (short)this.var_int_arr_arr_b[n9][n8 + 0];
                    this.b(n, n2, this.var_int_arr_arr_b[n9][n8 + 0] >>> 8, this.var_int_arr_arr_b[n9][n8 + 0] & 0xFF);
                    this.var_short_arr_arr_a[n][n2 + 2] = this.var_short_arr_arr_a[n][n2 + 0];
                    return;
                }
            }
            this.var_short_arr_arr_a[n][n2 + 2] = (short)this.var_int_arr_arr_b[n9][n8 + 0];
        }
    }

    final void z() {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        int n6 = 0;
        int n7 = this.var_int_arr_arr_a[0][2];
        if (n7 != 0) {
            n5 = this.var_int_arr_arr_a[1][54];
            n4 = this.var_int_arr_arr_a[1][55];
            n3 = this.var_int_arr_arr_a[0][55];
            n2 = (n5 & 0x7F000000) >> 24;
            n5 &= 0xFFFFFF;
            if (n2 >= n7) {
                this.var_int_i = 0;
                if (n3 < n4 + (n4 >> 2) && n4 >= this.q) {
                    this.var_int_i = 2;
                } else if (n5 <= this.var_int_l) {
                    this.var_int_i = 1;
                }
                this.var_int_arr_arr_a[1][54] = 0xFFFFFF;
            } else {
                n = this.var_int_arr_arr_a[1][8];
                if (n >= 0) {
                    int n8;
                    int n9 = n >>> 8;
                    n &= 0xFF;
                    int n10 = this.var_short_arr_arr_a[0][(++n2 << 3) + 0];
                    int n11 = n10 >>> 8;
                    if ((n8 = (n9 -= n11) * n9 + (n -= (n10 &= 0xFF)) * n) < n5) {
                        n5 = n8;
                        this.var_int_arr_arr_a[1][53] = this.var_short_arr_arr_a[0][(n2 << 3) + 0];
                    }
                    this.var_int_arr_arr_a[1][54] = n5 & 0xFFFFFF | n2 << 24;
                }
            }
        }
        if (this.var_boolean_i) {
            if (this.w++ >= this.aw) {
                int[] nArray = this.var_int_arr_arr_a[1];
                nArray[5] = nArray[5] + this.var_int_arr_arr_a[1][57];
                int[] nArray2 = this.var_int_arr_arr_a[1];
                nArray2[6] = nArray2[6] + this.var_int_arr_arr_a[1][57];
                int[] nArray3 = this.var_int_arr_arr_a[1];
                nArray3[7] = nArray3[7] + this.var_int_arr_arr_a[1][57];
                this.w = 0;
            }
            if (this.E++ >= this.an) {
                this.void_a();
                if (this.boolean_b()) {
                    this.E = 0;
                }
            }
            if (this.aq++ >= this.C && this.boolean_b(this.ar % 10)) {
                this.aq = 0;
            }
        }
        n6 = 0;
        n7 = this.var_int_arr_arr_a[1][2];
        if (n7 == 0) {
            return;
        }
        do {
            if ((this.var_short_arr_arr_a[1][n6 + 3] & 0xFF) < 2 && (this.var_short_arr_arr_a[1][n6 + 7] & 0xFF) == 0 && (this.var_short_arr_arr_a[1][n6 + 2] == this.var_short_arr_arr_a[1][n6 + 0] || this.var_short_arr_arr_a[1][n6 + 1] == this.var_short_arr_arr_a[1][n6 + 0])) {
                this.var_short_arr_arr_a[1][n6 + 2] = this.short_a((int)this.var_short_arr_arr_a[1][n6 + 0], this.int_b(1));
            }
            n6 += 8;
        } while (--n7 > 0);
        if (!this.var_boolean_i) {
            return;
        }
        n5 = n7;
        n4 = 0;
        n7 = this.var_int_arr_arr_a[1][2];
        n3 = -1;
        n2 = -1;
        switch (this.var_int_i) {
            case 2: {
                if (this.var_int_arr_arr_a[0][8] == -1) break;
                n3 = (short)(this.var_int_arr_arr_a[0][8] + (this.ar & 1) + ((this.ar & 3) - 2 << 8));
                n4 = n7 - (n5 >>= 2);
                break;
            }
            case 1: {
                if (this.var_int_arr_arr_a[1][8] == -1) break;
                n3 = (short)(this.var_int_arr_arr_a[0][8] + (this.ar & 1) + ((this.ar & 3) - 2 << 8));
                n2 = (short)this.var_int_arr_arr_a[1][53];
                n4 = n7 - (n5 >>= 3);
            }
        }
        if (this.var_int_i != 0) {
            n6 = 0;
            if (n3 >= 0) {
                n = 0;
                while (n < n4) {
                    if ((this.var_short_arr_arr_a[1][n6 + 3] & 0xFF) >= 2 && (this.var_short_arr_arr_a[1][n6 + 7] & 0xF) != 1) {
                        this.var_short_arr_arr_a[1][n6 + 2] = (short)n3;
                    }
                    ++n;
                    n6 += 8;
                }
            }
            if (n2 >= 0) {
                n = n4;
                while (n < n5) {
                    if ((this.var_short_arr_arr_a[1][n6 + 3] & 0xFF) >= 2 && (this.var_short_arr_arr_a[1][n6 + 7] & 0xF) != 1) {
                        this.var_short_arr_arr_a[1][n6 + 2] = (short)n2;
                    }
                    ++n;
                    n6 += 8;
                }
            }
            this.var_int_i = 0;
        }
    }

    final boolean boolean_b() {
        if (this.M == -1) {
            return true;
        }
        if (!this.boolean_a(1, 1, this.M)) {
            return false;
        }
        int n = this.var_int_arr_arr_a[1][8];
        if (n == -1) {
            return true;
        }
        int n2 = this.int_h(n);
        int n3 = n2 >>> 8;
        this.a(1, this.M, n3, n2 &= 0xFF, 0x40000000, false);
        return true;
    }

    final void void_a() {
        int n = this.ai;
        while (this.var_byte_arr_j[n] >= 0) {
            if (this.a(1, (int)this.var_byte_arr_j[n], false) < this.var_byte_arr_j[n + 1]) {
                this.M = this.var_byte_arr_j[n];
                return;
            }
            n += 2;
        }
        if (this.var_byte_arr_j[n] == -1) {
            this.ai = n + 1;
        }
        this.M = -1;
    }

    final int int_h(int n) {
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
                    if ((n8 | n9) < 0 || n8 >= 64 || n9 >= 64 || (this.var_short_arr_a[n7 = n8 + (n9 << 6)] & 0x300) != 0 || (this.var_short_arr_a[n7] & 0xFFF) != 0) continue;
                    return n8 << 8 | n9;
                }
            }
            --n4;
            ++n5;
        } while (n3++ < 10);
        return n2;
    }

    final short short_a(int n, int n2) {
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
        if (s >= 0 && (this.var_short_arr_a[n3 = (n10 = s >>> 8 & 0x3F) + ((n11 = s & 0x3F) << 6)] & Short.MAX_VALUE) != 0) {
            return s;
        }
        do {
            if (((n10 | n11) & 0xFFFFFFC0) == 0 && (this.var_short_arr_a[n3 = n10 | n11 << 6] & 0x300) == 768 && (this.var_short_arr_a[n3] & 3) == n2) {
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
        int n2 = this.var_int_arr_arr_a[n][5];
        int n3 = this.var_int_arr_arr_a[n][7];
        int n4 = this.var_int_arr_arr_a[n][6];
        int n5 = n2 + n3 + n4;
        if ((n2 << 16) / n5 < 21845) {
            return 769;
        }
        if ((n3 << 16) / n5 < 21845) {
            return 771;
        }
        return 770;
    }

    final boolean boolean_b(int n) {
        int n2;
        int n3 = this.var_int_arr_arr_a[1][2] + this.var_int_arr_arr_a[1][49];
        if (n3 >= this.var_int_arr_arr_a[1][3]) {
            return false;
        }
        if (n3 >= 26) {
            return false;
        }
        if (!this.boolean_a(1, 0, n)) {
            return false;
        }
        int n4 = this.var_int_arr_arr_a[0][0];
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
        if (n == 8 && this.a(1, 3, true) == 0) {
            return false;
        }
        int n5 = n - 1;
        if (n5 < 0) {
            n5 = 0;
        }
        if ((n2 = this.var_int_arr_arr_a[1][57 + n5] + this.var_int_arr_arr_a[1][66 + n5]) >= this.var_int_arr_arr_a[1][75 + n5]) {
            return false;
        }
        this.int_c(1, n);
        return true;
    }

    final int int_a(int n, int n2, int n3) {
        int n4 = this.var_int_arr_arr_a[n][8];
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
                int n13 = this.var_int_arr_arr_a[n][9];
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
                    int n23 = this.var_int_arr_arr_a[n][10 + i];
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
        int n = this.var_int_d - this.y;
        int n2 = this.S - this.N;
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

    public final void F() {
        int n;
        int n2 = 0;
        while (this.var_byte_arr_a[n2] != 127) {
            n = n2;
            if (this.var_byte_arr_a[n2] < 0) {
                n2 = this.int_g(n2);
                continue;
            }
            if ((n2 = this.int_d(n2)) >= 0) {
                n2 = this.int_d(n2, n);
                continue;
            }
            n2 = this.int_a(-n2);
        }
        n = 0;
        while (n < 4) {
            int n3 = n++;
            this.var_int_arr_a[n3] = this.var_int_arr_a[n3] + 1;
        }
        this.var_int_c = 0;
    }

    public final int int_d(int n) {
        switch (this.var_byte_arr_a[n++]) {
            case 1: {
                int n2;
                int n3 = this.var_byte_arr_a[n++] & 0xFF;
                int n4 = this.var_byte_arr_a[n++] & 0xFF;
                int n5 = this.var_byte_arr_a[n++] & 0xFF;
                int n6 = this.var_byte_arr_a[n++] & 0xFF;
                int n7 = this.var_byte_arr_a[n++] & 0xFF;
                int n8 = this.var_byte_arr_a[n++] & 0xFF;
                int n9 = this.var_byte_arr_a[n++] & 0xFF;
                if (n3 == 1) {
                    n = -n;
                }
                if ((this.var_short_arr_arr_a[n4][(n2 = n5 << 3) + 6] & 0xFF00) != 0) {
                    return -n;
                }
                int n10 = this.var_short_arr_arr_a[n4][n2 + 0];
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
                byte by = this.var_byte_arr_a[n++];
                if ((this.var_byte_arr_a[n++] & 0xFF) * 10 > this.var_int_arr_a[by]) break;
                return n;
            }
            case 6: {
                int n12 = this.var_byte_arr_a[n++] & 0xFF;
                int n13 = this.var_byte_arr_a[n++] & 0xFF;
                if (this.var_byte_arr_c[n12] != n13) break;
                return n;
            }
            case 7: {
                int n14 = this.var_byte_arr_a[n++] & 0xFF;
                int n15 = this.var_byte_arr_a[n++] & 0xFF;
                int n16 = this.var_byte_arr_a[n++] & 0xFF;
                int n17 = this.var_byte_arr_a[n++] & 0xFF;
                if (!(n16 == 0 ? this.var_int_arr_arr_a[n14][n15] == n17 : (n16 == 1 ? this.var_int_arr_arr_a[n14][n15] > n17 : n16 == 2 && this.var_int_arr_arr_a[n14][n15] < n17))) break;
                return n;
            }
            case 5: {
                int n18 = this.var_byte_arr_a[n++] & 0xFF;
                int n19 = this.var_byte_arr_a[n++] & 0xFF;
                byte by = this.var_byte_arr_a[n++];
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
                if (this.var_byte_arr_a[n++] != this.aA) break;
                return n;
            }
            case 4: {
                int n21 = this.var_byte_arr_a[n++] & 0xFF;
                int n22 = this.var_byte_arr_a[n++] & 0xFF;
                int n23 = (this.var_byte_arr_a[n++] & 0xFF) << 8;
                byte by = this.var_byte_arr_a[n++];
                byte by2 = this.var_byte_arr_a[n++];
                if (n21 == 1) {
                    n = -n;
                }
                if (this.Y != n22) {
                    return -n;
                }
                if (this.aJ != by2) {
                    return -n;
                }
                if (this.aE != n23) {
                    return -n;
                }
                if (by != -1 && this.var_int_h != by) {
                    return -n;
                }
                return n;
            }
        }
        return -n;
    }

    public final int int_d(int n, int n2) {
        block13: while (true) {
            switch (this.var_byte_arr_a[n++]) {
                case 8: {
                    int n3;
                    int n4 = this.var_byte_arr_a[n++];
                    int n5 = this.var_byte_arr_a[n++] & 0xFF;
                    int n6 = this.var_byte_arr_a[n++];
                    if (n5 >= 5 && n5 <= 7) {
                        this.var_boolean_l = true;
                    }
                    if (n4 == -1) {
                        for (n3 = 0; n3 < 2; ++n3) {
                            this.var_int_arr_arr_a[n3][n5] = n6;
                        }
                        continue block13;
                    }
                    this.var_int_arr_arr_a[n4][n5] = n6;
                    continue block13;
                }
                case 9: {
                    int n4 = this.var_byte_arr_a[n++] & 0xFF;
                    int n5 = this.var_byte_arr_a[n++] & 0xFF;
                    this.var_byte_arr_c[n4] = (byte)n5;
                    break;
                }
                case 6: {
                    int n4 = this.var_byte_arr_a[n++] & 0xFF;
                    int n5 = this.var_byte_arr_a[n++] & 0xFF;
                    int n6 = this.var_byte_arr_a[n++] & 0xFF;
                    int n3 = this.var_byte_arr_a[n++] & 0xFF;
                    this.a(n4, n5, n6, n3, false);
                    break;
                }
                case 7: {
                    int n7;
                    int n4 = this.var_byte_arr_a[n] & 0xFF;
                    int n5 = this.var_byte_arr_a[++n];
                    int n6 = this.var_byte_arr_a[++n] & 0xFF;
                    int n3 = this.var_byte_arr_a[++n] & 0xFF;
                    ++n;
                    int n8 = 0;
                    if (n5 == -1) {
                        n7 = 0;
                        while (n7 < this.var_int_arr_arr_a[n4][2]) {
                            if ((this.var_short_arr_arr_a[n4][n8 + 3] & 0xFF) < 2) {
                                this.var_short_arr_arr_a[n4][n8 + 2] = (short)((n6 << 8 | n3) & 0xFFFF);
                            }
                            ++n7;
                            n8 += 8;
                        }
                        continue block13;
                    }
                    if (n5 < 0) {
                        n5 = -n5;
                        n7 = 0;
                        while (n7 < this.var_int_arr_arr_a[n4][2]) {
                            if ((this.var_short_arr_arr_a[n4][n8 + 3] & 0xFF) == n5) {
                                this.var_short_arr_arr_a[n4][n8 + 2] = (short)((n6 << 8 | n3) & 0xFFFF);
                            }
                            ++n7;
                            n8 += 8;
                        }
                        continue block13;
                    }
                    this.f(n4, n << 3);
                    continue block13;
                }
                case 0: {
                    int n4 = this.var_byte_arr_a[n++] & 0xFF;
                    int n5 = this.var_byte_arr_a[n++] & 0xFF;
                    this.g(n4, this.I, n5);
                    break;
                }
                case 1: {
                    int n4 = this.var_byte_arr_a[n++] & 0xFF;
                    int n5 = this.int_f(n4);
                    if (this.var_byte_arr_a[n5] >= 0) break;
                    this.var_byte_arr_a[n5] = (byte)(-this.var_byte_arr_a[n5]);
                    break;
                }
                case 4: {
                    ++n;
                    this.g(0, 98, 0);
                    break;
                }
                case 5: {
                    ++n;
                    this.g(0, 98, 1);
                    break;
                }
                case 2: {
                    int n4 = this.var_byte_arr_a[n++];
                    int n5 = n2;
                    if (n4 >= 0) {
                        n5 = this.int_f(n4);
                    }
                    if (this.var_byte_arr_a[n5] <= 0) break;
                    this.var_byte_arr_a[n5] = (byte)(-this.var_byte_arr_a[n5]);
                    break;
                }
                case 3: {
                    int n4 = this.var_byte_arr_a[n++];
                    this.var_int_arr_a[n4] = 0;
                    break;
                }
                case 126: {
                    return n;
                }
            }
        }
    }

    public final int int_f(int n) {
        int n2 = 0;
        while (n > 0) {
            n2 = this.int_g(n2);
            --n;
        }
        return n2;
    }

    public final int int_g(int n) {
        byte by;
        if ((by = this.var_byte_arr_a[n++]) < 0) {
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
        return this.int_a(n);
    }

    public final int int_a(int n) {
        while (true) {
            switch (this.var_byte_arr_a[n++]) {
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
        if (this.aO <= 128) {
            this.F = 4;
            this.B = this.aO - 4;
        } else {
            this.F = 8;
            this.B = this.aO - 16;
        }
        if (n == 0) {
            this.aN = this.aA;
        }
        this.void_c(this.V);
        this.aQ = 0;
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        return true;
    }

    final void void_c(int n) {
        a a2 = new a(this.z);
        String string = a2.a(n);
        int n2 = string.length() - 1;
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
            if (this.var_javax_microedition_lcdui_Font_a.substringWidth(string, n5, n4 - n5) >= this.B - this.F) {
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
        this.D = this.aT - (this.P - this.O) + 16 + (this.ah << 1);
        this.ab = 0;
        this.ax = 0;
    }

    public final void A() {
        this.ab = 0;
        this.ax = 0;
        AgeOfEmpires.c.a(this.var_java_lang_String_arr_a);
        this.var_boolean_f = true;
        this.var_boolean_b = true;
        this.F = 0;
        this.B = this.aO;
    }

    public final void g(Graphics graphics) {
        int n;
        int n2 = (this.aO - this.av >> 1) - 8;
        int n3 = (this.P - this.O - this.aT >> 1) + this.O - 8;
        int n4 = this.av + 16;
        int n5 = this.aT + 24;
        int n6 = n3 + n5 - 8;
        if (n2 < 0) {
            n2 = 0;
        }
        if (n4 > this.aO) {
            n4 = this.aO;
        }
        if (n3 < this.O) {
            n3 = this.O;
        }
        if (n5 > this.P - this.O) {
            n5 = this.P - this.O;
        }
        boolean bl = false;
        boolean bl2 = false;
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        this.e(graphics, n2, n3, n4, n5);
        int n7 = n2;
        graphics.setClip(n2 += 2, n3 += 6, n4 -= 4, n5 -= 12);
        boolean bl3 = false;
        boolean bl4 = false;
        n3 += 2;
        n3 -= this.var_int_e;
        for (int i = 1; i < this.aW; ++i) {
            if (this.var_java_lang_String_arr_a[i] == null) {
                n = this.aO >> 1;
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
            n2 = this.aO - this.a(this.var_java_lang_String_arr_a[i]) >> 1;
            this.a(graphics, this.var_java_lang_String_arr_a[i], n2, n3, this.aQ - i);
            n3 += this.ah + this.ay + 2;
        }
        graphics.setClip(0, 0, this.aO, this.var_int_j);
        n4 = this.a(this.var_java_lang_String_arr_a[0]) + 12;
        n2 = n7 - (n4 >> 1);
        n5 = this.ah + 12;
        n3 = n6;
        if (n2 < 0) {
            n2 = 0;
        }
        if (n3 + n5 > this.P) {
            n3 = this.P - n5;
        }
        this.e(graphics, n2, n3, n4, n5);
        n = (AgeOfEmpires.b.int_b((this.aQ << 6) + 512) >> 10) + 128;
        graphics.setColor(0xFFFFFF);
        graphics.drawString(this.var_java_lang_String_arr_a[0], (n2 += 6) + 1, (n3 += 6 + this.ay) + 1, 20);
        graphics.setColor((n << 16 | n << 4) & 0xFFFF00);
        graphics.drawString(this.var_java_lang_String_arr_a[0], n2, n3, 20);
        if (this.aQ++ > 10) {
            switch (this.ab) {
                case 6: 
                case 22: 
                case 38: 
                case 47: {
                    this.am = this.aN;
                    this.ab = 0;
                    this.ax = 0;
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
        this.a(graphics, this.aO - 6 >> 1, n3);
    }

    public final void a(Graphics graphics, int n, int n2) {
        if (this.var_int_e > this.D) {
            this.var_int_e = this.D;
        }
        if (this.var_int_e < 0) {
            this.var_int_e = 0;
        }
        if ((this.ar & 1) == 0) {
            return;
        }
        if (this.D > 0) {
            if (this.var_int_e > 0) {
                n2 = this.O + 2;
                this.a(graphics, 21, n, n2, 0, 0, 7, 6, 0, 0);
            }
            if (this.var_int_e < this.D) {
                n2 = this.P - 8;
                this.a(graphics, 21, n, n2, 7, 0, 7, 6, 0, 0);
            }
        }
    }

    /** 任务结束结算（g(0, 98, n3)，n3==0 为胜利）：推进解锁计数 aj/aG、
     *  写每关高分（var_int_arr_d）并持久化 .nfo 字节 28。 */
    final void g(int n, int n2, int n3) {
        if (System.getProperty("aoe.debug") != null) {
            System.out.println("[trace] g(" + n + "," + n2 + "," + n3 + ") ac=" + this.ac);
        }
        if (this.boolean_g(2)) {
            this.as = n;
            this.z = n2;
            this.V = n3;
            if (n2 == 98) {
                this.O = 0;
                this.F = 0;
                this.P = this.var_int_j;
                this.B = this.aO;
                this.a(132 + n3, false);
                this.m();
                switch (this.ac) {
                    case 0: {
                        this.h(31, 2, r << 8 | var_int_k);
                        this.aA = 12;
                        this.H = 1;
                        break;
                    }
                    case 16: {
                        if (n3 == 0 && this.aG == this.aC) {
                            if (this.aG < 2) {
                                ++this.aG;
                                this.H = 11;
                            } else {
                                this.V = 2;
                                this.H = 1;
                            }
                        }
                        this.aA = 4;
                        break;
                    }
                    case 32: {
                        if (this.aj < 6) {
                            if (n3 == 0 && this.aj < this.aC + 1) {
                                this.aj = this.aC + 1;
                            }
                            this.H = 12;
                        } else {
                            this.H = 1;
                        }
                        int n4 = this.var_int_arr_arr_a[0][90] * 3;
                        n4 += this.var_int_arr_arr_a[1][87] * 124;
                        n4 += this.var_int_arr_arr_a[0][86] * 421;
                        n4 -= this.var_int_arr_arr_a[0][87] * 9;
                        if (this.var_int_arr_d[this.aC] < (n4 -= this.var_int_arr_arr_a[1][86] * 12)) {
                            this.var_int_arr_d[this.aC] = n4;
                            this.h(0 + (this.aC << 2), 4, n4);
                            this.I();
                        }
                        this.aA = 12;
                    }
                }
                if (n3 == 0) {
                    this.h(28, 1, this.aG << 4 | this.aj);
                    this.I();
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
        var_int_k = 45;
    }
}

