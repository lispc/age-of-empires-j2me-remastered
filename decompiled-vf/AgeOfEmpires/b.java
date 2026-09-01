package AgeOfEmpires;

import java.io.InputStream;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;

public final class b {
   static boolean c;
   public static MIDlet a;
   public static boolean a = false;
   public static boolean b = false;
   static int[] b;
   static byte[] a;
   static int a;
   static long[] a;
   static int[] a = new int[]{
      9471,
      16720896,
      28159,
      16739584,
      6993663,
      16758123,
      5415423,
      16753489,
      3248639,
      16748598,
      1605119,
      16743963,
      27135,
      16739072,
      24063,
      16735488,
      19711,
      16727808,
      15615,
      16723968,
      12543,
      16720128,
      8447,
      14491904,
      8414,
      12198144,
      8381,
      9969920,
      8348,
      7676160,
      8307,
      5382400,
      8274,
      5382400,
      -1
   };
   static Player a;

   static final void a() {
      byte[] var0;
      int var1;
      b = new int[var1 = (var0 = com.ulysseo.mad.c.a(130)).length >> 1];

      for (int var2 = 0; var2 < var1; var2++) {
         b[var2] = (var0[var2 << 1] << 8) + (var0[(var2 << 1) + 1] & 255) & 65535;
      }
   }

   static final int c(int var0) {
      int var1;
      return b(var1 = var0 - 512);
   }

   static final int b(int var0) {
      if ((var0 = var0 & 2047) < 1024) {
         return var0 < 512 ? b[var0] : -b[1024 - var0];
      } else {
         return var0 < 1536 ? -b[var0 - 1024] : b[2048 - var0];
      }
   }

   static final int d(int var0) {
      return var0 < 0 ? -var0 : var0;
   }

   public static final Image a(byte[] var0, int var1, int var2) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 1) {
         return null;
      } else {
         if (a == null) {
            b();
         }

         a = 0;
         a = var0;
         int var3 = a(8) + 1;
         int var4 = a(8) + 1;
         int var5 = a(16) + 1;
         int var6 = a(16) + 1;
         int var7;
         int[] var8 = new int[var7 = a(8) + 1];
         int var13 = 0;
         int var14 = -1;

         for (int var9 = 0; var9 < var4; var9++) {
            if (var1 != var9) {
               int var11 = a(4);
               if (a(4) == 1) {
                  b(8);
               }

               a(var7 * (1 + var11));
            } else {
               int var12 = a(4);
               if ((var13 = a(4)) == 1) {
                  var14 = a(8);
               }

               for (int var10 = 0; var10 < var7; var10++) {
                  var8[var10] = a(a(1 + var12 << 3), var12);
               }
            }
         }

         switch (var2) {
            case 1:
               for (int var27 = 0; var27 < var7; var27++) {
                  for (byte var20 = 0; a[var20] >= 0; var20 += 2) {
                     if (var8[var27] == a[var20]) {
                        var8[var27] = a[var20 + 1];
                     }
                  }
               }
               break;
            case 2:
               for (int var26 = 1; var26 < var7; var26++) {
                  int var31;
                  int var34 = ((var31 = var8[var26]) & 0xFF00) >> 8;
                  int var36 = var31 & 0xFF;
                  var31 = (var31 & 0xFF0000) >> 16;
                  int var28 = (var36 + var34 * 6 + var31 * 3) / 16;
                  var8[var26] = var28 << 16 | var28 << 8 | var28;
               }
               break;
            case 3:
               for (int var25 = 1; var25 < var7; var25++) {
                  var8[var25] = 16777215;
               }
               break;
            case 4:
               for (int var24 = 1; var24 < var7; var24++) {
                  int var16;
                  int var17 = ((var16 = var8[var24]) & 0xFF00) >> 8;
                  int var18 = var16 & 0xFF;
                  var16 = (var16 & 0xFF0000) >> 16;
                  int var15 = (var18 + var17 * 6 + var16 * 3) / 10;
                  var8[var24] = var15 << 16;
               }
         }

         int var29;
         byte[] var33 = a(var29 = a(16));
         byte[] var35 = new byte[69 + var13 * (12 + var14 + 1) + var7 * 3 + var29];
         int var37 = 0;
         a(var35, 0, 32, -1991225785);
         a(var35, 32, 32, 218765834);
         a(var35, 64, 32, 13);
         a(var35, 96, 32, 1229472850);
         a(var35, 128, 32, var5);
         a(var35, 160, 32, var6);
         a(var35, 192, 8, var3);
         a(var35, 200, 32, 50331648);
         int var19 = b(var35, 12, 29);
         a(var35, 232, 32, var19);
         a(var35, 264, 32, var7 * 3);
         a(var35, 296, 32, 1347179589);

         for (int var21 = 0; var21 < var7; var21++) {
            a(var35, 328 + var21 * 24, 24, var8[var21]);
         }

         var19 = b(var35, 37, 41 + var7 * 3);
         a(var35, 264 + (8 + var7 * 3 << 3), 32, var19);
         var37 = 264 + (8 + var7 * 3 + 4 << 3);
         if (var13 == 1) {
            a(var35, var37, 32, var14 + 1);
            a(var35, var37 + 32, 32, 1951551059);

            int var22;
            for (var22 = 0; var22 < var14; var22++) {
               a(var35, var37 + 64 + (var22 << 3), 8, 255);
            }

            a(var35, var37 + 64 + (var22 << 3), 8, 0);
            var19 = b(var35, (var37 >> 3) + 4, (var37 >> 3) + 4 + 4 + var14 + 1);
            a(var35, var37 + (8 + var14 + 1 << 3), 32, var19);
            var37 += 8 + var14 + 1 + 4 << 3;
         }

         a(var35, var37, 32, var29);
         a(var35, var37 + 32, 32, 1229209940);

         for (int var23 = 0; var23 < var29; var23++) {
            a(var35, var37 + 64 + (var23 << 3), 8, var33[var23]);
         }

         var19 = b(var35, (var37 >> 3) + 4, (var37 >> 3) + 4 + 4 + var29);
         a(var35, var37 + (8 + var29 << 3), 32, var19);
         var37 += 8 + var29 + 4 << 3;
         a(var35, var37, 32, 0);
         a(var35, var37 + 32, 32, 1229278788);
         var19 = b(var35, (var37 >> 3) + 4, (var37 >> 3) + 4 + 4 + 0);
         a(var35, var37 + 64, 32, var19);
         a = null;
         return Image.createImage(var35, 0, var35.length);
      }
   }

   public static final int a(int var0, int var1) {
      int var2;
      int var3;
      int var4;
      switch (var1) {
         case 0:
            var2 = (var0 >> 5) * 255 / 7;
            var3 = (var0 >> 2 & 7) * 255 / 7;
            var4 = (var0 & 3) * 255 / 3;
            break;
         case 1:
            var2 = (var0 >> 11) * 255 / 31;
            var3 = (var0 >> 5 & 63) * 255 / 63;
            var4 = (var0 & 31) * 255 / 31;
            break;
         default:
            return var0;
      }

      return var2 << 16 | var3 << 8 | var4;
   }

   public static final void b() {
      a = new long[256];

      for (int var2 = 0; var2 < 256; var2++) {
         long var0 = (long)var2;

         for (int var3 = 0; var3 < 8; var3++) {
            if ((var0 & 1L) == 1L) {
               var0 = 3988292384L ^ var0 >> 1;
            } else {
               var0 >>= 1;
            }
         }

         a[var2] = var0;
      }
   }

   public static final int b(byte[] var0, int var1, int var2) {
      long var3 = 4294967295L;

      for (int var5 = var1; var5 < var2; var5++) {
         var3 = a[(int)((var3 ^ (long)var0[var5]) & 255L)] ^ var3 >> 8;
      }

      return (int)(var3 ^ 4294967295L);
   }

   public static final int a(byte[] var0, int var1, int var2) {
      int var3 = 0;
      if ((var1 & 7) == 0 && (var2 & 7) == 0) {
         int var7 = var2 >> 3;

         for (int var8 = 0; var8 < var7; var8++) {
            int var6;
            var3 = (var6 = var3 << 8) + (var0[var1 >> 3] & 255);
            var1 += 8;
         }
      } else {
         for (int var5 = 1; var5 <= var2; var5++) {
            int var4 = var1 >> 3;
            var3 += (var0[var4] >> 7 - (var1 - (var4 << 3)) & 1) << var2 - var5;
            var1++;
         }
      }

      return var3;
   }

   public static final void a(byte[] var0, int var1, int var2, int var3) {
      int var4 = 0;
      byte var5 = 0;
      int var6 = var1;
      int var7 = 1 << var2 - 1;

      for (int var8 = 0; var8 < var2; var8++) {
         var4 = var6 >> 3;
         var5 = (byte)(1 << 7 - (var6 - (var4 << 3)));
         if ((var3 & var7) == var7) {
            var0[var4] |= var5;
         } else {
            var0[var4] &= (byte)(255 ^ var5);
         }

         var3 <<= 1;
         var6++;
      }
   }

   public static final int a(int var0) {
      int var1 = a(a, a, var0);
      b(var0);
      return var1;
   }

   public static final void b(int var0) {
      a += var0;
   }

   public static final void a(int var0) {
      a += var0 << 3;
   }

   public static final byte a() {
      return (byte)a(8);
   }

   public static final byte[] a(int var0) {
      byte[] var1 = new byte[var0];
      if ((a & 7) == 0) {
         System.arraycopy(a, a >> 3, var1, 0, var0);
         a(var0);
      } else {
         for (int var2 = 0; var2 < var0; var2++) {
            a[var2] = a();
         }
      }

      return var1;
   }

   static final String a(int var0, byte var1) {
      String var2 = "/Menu_poly";
      switch (var0) {
         case 132:
            var2 = "/Won";
            break;
         case 133:
            var2 = "/Lost";
            break;
         case 134:
            var2 = "/Music01";
            break;
         case 135:
            var2 = "/Music02";
            break;
         case 136:
            var2 = "/Music03";
            break;
         case 137:
            var2 = "/Music04";
            break;
         case 138:
            var2 = "/Music05";
            break;
         case 139:
            var2 = "/Music06";
            break;
         case 140:
            var2 = "/Music07";
            break;
         case 141:
            var2 = "/Music08";
            break;
         case 142:
            var2 = "/Music09";
            break;
         case 143:
            var2 = "/Music10";
      }

      if (var1 == 1) {
         var2 = var2 + ".mmf";
      }

      if (var1 == 0) {
         var2 = var2 + ".mid";
      }

      return var2;
   }

   public static final boolean a(int var0, boolean var1) {
      if (!c) {
         return true;
      } else {
         try {
            if (a != null) {
               a.stop();
               a.deallocate();
               a.close();
               a = null;
            }

            String var2 = a(var0, (byte)0);
            InputStream var3;
            if ((var3 = a.getClass().getResourceAsStream(var2)) == null) {
               return false;
            } else {
               a = Manager.createPlayer(var3, "audio/midi");
               if (a == null) {
                  return false;
               } else {
                  a.prefetch();
                  if (var1) {
                     a.setLoopCount(-1);
                  }

                  a.start();
                  return true;
               }
            }
         } catch (Exception var4) {
            return false;
         }
      }
   }

   public static final void c() {
      try {
         if (a != null) {
            a.stop();
            a.deallocate();
            a.close();
            a = null;
         }
      } catch (Exception var1) {
      }
   }
}
