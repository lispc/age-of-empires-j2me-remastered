package com.ulysseo.mad;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public final class d {
   public static final void a(Graphics var0, Image var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      if (var2 < 0) {
         var4 += -var2;
         var2 = 0;
      }

      if (var3 < 0) {
         var5 += -var3;
         var3 = 0;
      }

      if (var2 + var4 > var1.getWidth()) {
         var4 = var1.getWidth() - var2;
      }

      if (var3 + var5 > var1.getHeight()) {
         var5 = var1.getHeight() - var3;
      }

      var0.drawRegion(var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }
}
