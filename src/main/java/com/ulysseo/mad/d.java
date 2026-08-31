/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 */
package com.ulysseo.mad;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public final class d {
    public static final void a(Graphics graphics, Image image, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8) {
        if (n < 0) {
            n3 += -n;
            n = 0;
        }
        if (n2 < 0) {
            n4 += -n2;
            n2 = 0;
        }
        if (n + n3 > image.getWidth()) {
            n3 = image.getWidth() - n;
        }
        if (n2 + n4 > image.getHeight()) {
            n4 = image.getHeight() - n2;
        }
        graphics.drawRegion(image, n, n2, n3, n4, n5, n6, n7, n8);
    }
}

