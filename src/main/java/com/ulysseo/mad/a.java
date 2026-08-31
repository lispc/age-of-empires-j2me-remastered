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

    public abstract void onKeyPress(int var1);

    public abstract void onKeyRelease(int var1);

    public abstract void onShown();

    public abstract void onHidden();

    public abstract void j(int var1, int var2);

    public abstract void onPaint(Graphics var1);

    public abstract void w();

    /** 桌面移植扩展：鼠标事件。kind: 0=移动/悬停 1=左按下 2=左抬起 3=右按下；
     *  (x,y) 为 240x320 逻辑像素。J2ME 本无鼠标，默认无操作。 */
    public void mouseA(int kind, int x, int y) {
    }

    /** 桌面专属命令键（1=F5 快存，2=F9 快读），不占 J2ME 键码。默认无操作。 */
    public void desktopCommand(int id) {
    }
}

