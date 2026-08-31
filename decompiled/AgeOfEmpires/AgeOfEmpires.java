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

public final class AgeOfEmpires
extends MIDlet {
    c var_AgeOfEmpires_c_a = null;
    static boolean var_boolean_a;

    public AgeOfEmpires() {
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
        }
    }

    public final void destroyApp(boolean bl) {
        ((com.ulysseo.mad.b)((Object)c.var_AgeOfEmpires_AgeOfEmpires_a)).a();
    }

    public final void pauseApp() {
        if (var_boolean_a) {
            b.c();
            this.var_AgeOfEmpires_c_a.af = 0;
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

