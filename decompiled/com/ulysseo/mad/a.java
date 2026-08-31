/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Display
 *  javax.microedition.lcdui.Displayable
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.midlet.MIDlet
 */
package com.ulysseo.mad;

import com.ulysseo.mad.b;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

public abstract class a {
    public static b var_com_ulysseo_mad_b_a;
    public static MIDlet var_javax_microedition_midlet_MIDlet_a;

    public a(MIDlet mIDlet, int n, int n2) {
        var_javax_microedition_midlet_MIDlet_a = mIDlet;
        var_com_ulysseo_mad_b_a = new b(this);
        Display.getDisplay((MIDlet)var_javax_microedition_midlet_MIDlet_a).setCurrent((Displayable)var_com_ulysseo_mad_b_a);
    }

    public abstract void void_a(int var1);

    public abstract void void_e(int var1);

    public abstract void k();

    public abstract void q();

    public abstract void j(int var1, int var2);

    public abstract void p(Graphics var1);

    public abstract void w();
}

