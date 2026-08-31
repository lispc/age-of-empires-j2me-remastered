/*
 * Decompiled with CFR 0.152.
 */
package com.ulysseo.mad;

import com.ulysseo.mad.b;
import java.util.TimerTask;

/*
 * Exception performing whole class analysis ignored.
 */
final class e
extends TimerTask {
    private final b a;

    e(b b2) {
        this.a = b2;
    }

    public final void run() {
        if (b.b(this.a)) {
            if (b.a(this.a) == 4) {
                System.currentTimeMillis();
                this.scheduledExecutionTime();
            }
            b.a(this.a).w();
            if (!b.a(this.a)) {
                this.a.repaint();
                this.a.serviceRepaints();
                return;
            }
        } else {
            Thread.yield();
        }
    }
}
