package com.ulysseo.mad;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class c {
   private static int a = 1;
   private static DataInputStream a = null;
   private static byte[] a = null;

   c(int var1) {
      try {
         if (a == 0 && a == null) {
            DataInputStream var2;
            a = new byte[(var2 = new DataInputStream(this.getClass().getResourceAsStream("/res/data.res"))).readInt()];
            var2.read(a);
         }

         if (a == 0) {
            a = new DataInputStream(new ByteArrayInputStream(a));
         } else if (a == 1) {
            a = new DataInputStream(this.getClass().getResourceAsStream("/res/data.res"));
            a.readInt();
         }

         a.skipBytes(var1 * 4);
         a.skipBytes(a.readInt() - (var1 + 2) * 4);
      } catch (Exception var3) {
      }
   }

   public static final void a(int var0) {
      a = var0;
      a = null;
   }

   public static final byte[] a(int var0) {
      try {
         new c(var0);
         Object var2 = null;
         byte[] var1 = new byte[a.readInt()];
         a.read(var1);
         return var1;
      } catch (Exception var3) {
         return null;
      }
   }
}
