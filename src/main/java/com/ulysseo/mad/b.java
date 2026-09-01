/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Display
 *  javax.microedition.lcdui.Displayable
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 *  javax.microedition.lcdui.game.GameCanvas
 *  javax.microedition.midlet.MIDlet
 */
package com.ulysseo.mad;

import com.ulysseo.mad.a;
import com.ulysseo.mad.e;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;

public final class b
extends GameCanvas {
    private a var_com_ulysseo_mad_a_a;
    private boolean c = false;
    private boolean var_boolean_b;
    private Timer var_java_util_Timer_a;
    private Timer var_java_util_Timer_b;
    private TimerTask var_java_util_TimerTask_b;
    private TimerTask var_java_util_TimerTask_a;
    private int var_int_a;
    private Image var_javax_microedition_lcdui_Image_a;
    private Graphics var_javax_microedition_lcdui_Graphics_a;
    private boolean var_boolean_a = true;

    b(a a2) {
        super(false);
        if (!this.isDoubleBuffered()) {
            this.var_javax_microedition_lcdui_Image_a = Image.createImage((int)this.getWidth(), (int)this.getHeight());
            this.var_javax_microedition_lcdui_Graphics_a = this.var_javax_microedition_lcdui_Image_a.getGraphics();
        }
        this.var_com_ulysseo_mad_a_a = a2;
    }

    public final void paint(Graphics graphics) {
        if (this.var_boolean_a) {
            this.var_boolean_a = false;
            return;
        }
        if (this.isDoubleBuffered()) {
            this.var_com_ulysseo_mad_a_a.onPaint(graphics);
            return;
        }
        this.var_com_ulysseo_mad_a_a.onPaint(this.var_javax_microedition_lcdui_Graphics_a);
        graphics.drawImage(this.var_javax_microedition_lcdui_Image_a, 0, 0, 20);
    }

    protected final void keyPressed(int n) {
        this.var_com_ulysseo_mad_a_a.onKeyPress(n);
        this.getGameAction(n);
    }

    protected final void keyRepeated(int n) {
    }

    protected final void keyReleased(int n) {
        this.var_com_ulysseo_mad_a_a.onKeyRelease(n);
        this.getGameAction(n);
    }

    @Override
    protected final void mouseA(int n, int n2, int n3) {
        this.var_com_ulysseo_mad_a_a.mouseA(n, n2, n3);
    }

    @Override
    public void desktopCommand(int n) {
        this.var_com_ulysseo_mad_a_a.desktopCommand(n);
    }

    protected final void pointerPressed(int n, int n2) {
    }

    protected final void pointerReleased(int n, int n2) {
    }

    protected final void pointerDragged(int n, int n2) {
    }

    protected final void hideNotify() {
        this.var_com_ulysseo_mad_a_a.onHidden();
    }

    protected final void showNotify() {
        this.var_com_ulysseo_mad_a_a.onShown();
    }

    protected final void sizeChanged(int n, int n2) {
        this.var_com_ulysseo_mad_a_a.onScreenSizeChanged(n, n2);
    }

    public final void b() {
        Display.getDisplay((MIDlet)com.ulysseo.mad.a.var_javax_microedition_midlet_MIDlet_a).setCurrent((Displayable)this);
    }

    public final void a(int n, int n2) {
        if (this.var_java_util_TimerTask_b != null) {
            this.a();
        }
        this.var_int_a = n2;
        this.c = true;
        this.var_boolean_b = false;
        this.var_java_util_TimerTask_b = new e(this);
        this.var_java_util_Timer_a = new Timer();
        if (n2 == 1) {
            this.var_java_util_Timer_a.schedule(this.var_java_util_TimerTask_b, 0L, (long)n);
        } else {
            this.var_java_util_Timer_a.scheduleAtFixedRate(this.var_java_util_TimerTask_b, 0L, (long)n);
        }
        startPaintWatchdog();
    }

    /**
     * 卡死看门狗：模拟+渲染整体跑在 Timer 线程的 serviceRepaints 里，单帧预算只有
     * 一个 tick（80ms）。这里每 2s 采样该线程，若它连续 ~6s 停在同一游戏任务帧
     * （而非调度器空闲等待），按"疑似卡死"把完整线程栈打进日志——任何一次真卡死
     * 都能事后从 run.sh 日志直接看到死循环位置（aoe.debug=1 由 build.gradle 常开）。
     */
    private static void startPaintWatchdog() {
        Thread wd = new Thread(() -> {
            String lastSig = null;
            StackTraceElement[] lastStack = null;
            int repeats = 0;
            boolean reported = false;
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    return;
                }
                String sig = null;
                StackTraceElement[] stack = null;
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    if (!t.getName().startsWith("Timer-")) continue;
                    StackTraceElement[] st = t.getStackTrace();
                    // 只把"正在执行游戏任务"判为占用：栈里出现游戏代码帧才算
                    // （空闲时栈是 Object.wait ← TimerThread.mainLoop ← run，无游戏帧）。
                    boolean inGame = false;
                    for (StackTraceElement f : st) {
                        String cn = f.getClassName();
                        if (cn.startsWith("AgeOfEmpires.") || cn.startsWith("com.ulysseo.")
                                || cn.startsWith("aoe.") || cn.startsWith("javax.microedition.")) {
                            inGame = true;
                            break;
                        }
                    }
                    if (inGame) {
                        sig = st[0] + "|" + st[Math.min(1, st.length - 1)] + "|" + st[Math.min(2, st.length - 1)];
                        stack = st;
                    }
                    break;
                }
                if (sig != null && sig.equals(lastSig)) {
                    if (++repeats >= 3 && !reported) {
                        reported = true;
                        System.out.println("[watchdog] Timer 线程疑似卡死：同一任务帧已持续 " + (repeats * 2) + "s+");
                        for (StackTraceElement f : lastStack) {
                            System.out.println("[watchdog]   at " + f);
                        }
                    }
                } else {
                    lastSig = sig;
                    lastStack = stack;
                    repeats = 0;
                    reported = false;
                }
            }
        }, "paint-watchdog");
        wd.setDaemon(true);
        wd.start();
    }

    public final void a() {
        if (this.var_java_util_Timer_a != null) {
            this.var_java_util_Timer_a.cancel();
        }
        if (this.var_java_util_TimerTask_b != null) {
            this.var_java_util_TimerTask_b.cancel();
        }
        if (this.var_boolean_b) {
            if (this.var_java_util_Timer_b != null) {
                this.var_java_util_Timer_b.cancel();
            }
            if (this.var_java_util_TimerTask_a != null) {
                this.var_java_util_TimerTask_a.cancel();
            }
        }
        this.c = false;
        this.var_boolean_b = false;
        this.var_java_util_Timer_b = null;
        this.var_java_util_TimerTask_a = null;
        this.var_java_util_Timer_a = null;
        this.var_java_util_Timer_b = null;
    }

    static final boolean b(b b2) {
        return b2.c;
    }

    static final int int_a(b b2) {
        return b2.var_int_a;
    }

    static final a com_ulysseo_mad_a_a(b b2) {
        return b2.var_com_ulysseo_mad_a_a;
    }

    static final boolean boolean_a(b b2) {
        return b2.var_boolean_b;
    }
}

