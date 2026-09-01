package com.ulysseo.mad;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

public abstract class a {
   public static b a;
   public static MIDlet a;

   public a(MIDlet var1, int var2, int var3) {
      a = var1;
      a = new b(this);
      Display.getDisplay(a).setCurrent(a);
   }

   public abstract void a(int var1);

   public abstract void e(int var1);

   public abstract void k();

   public abstract void q();

   public abstract void j(int var1, int var2);

   public abstract void p(Graphics var1);

   public abstract void w();
}
