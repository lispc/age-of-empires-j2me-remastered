/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.midlet.MIDlet
 */
package AgeOfEmpires;

import AgeOfEmpires.b;
import AgeOfEmpires.c;
import javax.microedition.midlet.MIDlet;

public final class AoeMidlet
extends MIDlet {
    c var_AgeOfEmpires_c_a = null;
    static boolean var_boolean_a;

    public AoeMidlet() {
        var_boolean_a = false;
        b.var_javax_microedition_midlet_MIDlet_a = this;
    }

    public final void startApp() {
        if (!var_boolean_a) {
            b.var_javax_microedition_midlet_MIDlet_a = this;
            this.var_AgeOfEmpires_c_a = new c(this);
            b.var_boolean_a = false;
            this.var_AgeOfEmpires_c_a.o = -1;
            this.var_AgeOfEmpires_c_a.int_b();
            this.var_AgeOfEmpires_c_a.void_b();
            var_boolean_a = true;
            String dev = System.getProperty("aoe.dev");
            if (dev != null) {
                this.var_AgeOfEmpires_c_a.devStartMission(dev);
            }
            String devBoot = System.getProperty("aoe.devBoot");
            if (devBoot != null) {
                this.var_AgeOfEmpires_c_a.devBootFromSave(devBoot);
            }
            String devMouse = System.getProperty("aoe.devMouse");
            if (devMouse != null) {
                this.var_AgeOfEmpires_c_a.devStartMouseFifo(devMouse);
            }
        }
    }

    /** dev 模式：测试驱动（DevHarness）用。 */
    public c game() {
        return this.var_AgeOfEmpires_c_a;
    }

    public final void destroyApp(boolean bl) {
        c.var_com_ulysseo_mad_b_a.a();
    }

    public final void pauseApp() {
        if (var_boolean_a) {
            b.c();
            this.var_AgeOfEmpires_c_a.mapThumbStampRow = 0;
            this.var_AgeOfEmpires_c_a.s();
            b.var_boolean_a = true;
        }
    }

    public final void a() {
        b.c();
        this.var_AgeOfEmpires_c_a.h();
        this.destroyApp(false);
        this.notifyDestroyed();
    }
}

