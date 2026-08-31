/*
 * Decompiled with CFR 0.152.
 * (手工修正：b.a(this.a) 的三处调用原本是三个不同的重名方法)
 */
package com.ulysseo.mad;

import com.ulysseo.mad.b;
import java.util.TimerTask;

final class e
extends TimerTask {
    private final b a;
    private int dbg;

    e(b b2) {
        this.a = b2;
    }

    public final void run() {
        if (b.b(this.a)) {
            if (b.int_a(this.a) == 4) {
                System.currentTimeMillis();
                this.scheduledExecutionTime();
            }
            b.com_ulysseo_mad_a_a(this.a).w();
            if (System.getProperty("aoe.debug") != null && ++this.dbg % 25 == 0) {
                AgeOfEmpires.c game = (AgeOfEmpires.c)(Object)b.com_ulysseo_mad_a_a(this.a);
                System.out.println("[dbg] ar=" + game.tickCount + " am=" + game.pendingScreenState + " aA=" + game.screenState + " aH=" + game.aH
                        + " l=" + game.var_boolean_l + " j=" + game.var_boolean_j
                        + " fullRedraw=" + AgeOfEmpires.b.var_boolean_a);
            }
            if (!b.boolean_a(this.a)) {
                this.a.repaint();
                this.a.serviceRepaints();
                return;
            }
        } else {
            Thread.yield();
        }
    }
}
