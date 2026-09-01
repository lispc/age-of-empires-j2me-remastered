package AgeOfEmpires;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

final class c extends com.ulysseo.mad.a implements CommandListener {
   public AgeOfEmpires a;
   public int aO;
   public int j;
   public int ad;
   public int J;
   public int aI;
   public int aB;
   public int aL;
   public int f;
   public int af;
   public int u;
   public boolean f = false;
   public boolean b = false;
   public int[] d;
   public String[] b;
   public String b;
   public byte a = 0;
   public int ah;
   public int ay;
   public Font a;
   public int aM = 256;
   public short a;
   public short b;
   public short c;
   public int[][] b;
   public short[][] b;
   public short[] a;
   public int[][] a;
   public int aA;
   public int am;
   public int aH;
   public boolean k;
   public int R;
   public int ac;
   public int aC;
   public int z;
   public int I;
   public int[] e;
   public boolean d = true;
   public int aj;
   public int aG;
   public int ar;
   public boolean h;
   public boolean j;
   public byte[] c;
   public int y;
   public int N;
   public int az;
   public int al;
   public int Q;
   public int t;
   public int aa;
   public int aV;
   public int aK;
   public String[] a;
   public int as;
   public int V;
   public int aN;
   public int aW;
   public int aT;
   public int av;
   public int e;
   public int D;
   public int b;
   public int p;
   public int aE;
   public int h;
   public int aJ;
   public int Y;
   public int i;
   public int aQ;
   public Graphics a;
   public byte[] d;
   public byte[] b;
   public byte[] h;
   public boolean e = false;
   public byte[] f;
   public int ak;
   public byte[] k;
   public int ae;
   public int ax;
   public int ab;
   public String d;
   public int ag;
   public String a;
   public String c;
   public int G;
   public int x;
   public boolean a;
   public boolean g;
   public int F;
   public int B;
   public int O;
   public int P;
   public boolean l;
   public int W;
   public int X;
   public int[] c;
   public int T;
   public int aD;
   public byte[] e;
   public int g;
   public int c;
   public int[] b;
   public byte[] a;
   public int[] a;
   public int d;
   public int S;
   public int au;
   public int U;
   public int aP;
   public int aS;
   public static int r = 12;
   public static int k = 45;
   d a;
   public int aF;
   public byte[] i;
   public int H;
   public int v;
   public int ap;
   public int aR;
   public int ao;
   public int Z;
   public int a;
   public int K;
   public boolean i = true;
   public int an;
   public int E = 0;
   public int ai;
   public int M;
   public int q;
   public int l;
   public int aq;
   public int C;
   public int w;
   public int aw;
   public Image[] a;
   public int s;
   public int L;
   public int aU;
   public int A;
   public int at;
   public int n;
   public int m = 100000;
   public int o;
   public boolean c;
   public byte[] g;
   public short[][] a;
   public byte[] j = new byte[]{
      11,
      2,
      10,
      1,
      12,
      2,
      -1,
      11,
      3,
      12,
      5,
      6,
      1,
      7,
      1,
      5,
      1,
      10,
      1,
      -1,
      11,
      5,
      12,
      5,
      8,
      1,
      7,
      1,
      10,
      1,
      2,
      1,
      -1,
      11,
      5,
      8,
      1,
      12,
      5,
      7,
      1,
      3,
      1,
      2,
      1,
      10,
      1,
      10,
      1,
      -2
   };

   c(AgeOfEmpires var1) {
      super(var1, 134, 0);
      this.a = var1;
      this.d = "V" + var1.getAppProperty("MIDlet-Version");
      com.ulysseo.mad.c.a(0);
      this.m = 100000;
   }

   public final void w() {
      if (this.e) {
         this.a.a();
      }
   }

   public final synchronized void k() {
      if (this.ar >= 20 && AgeOfEmpires.a) {
         this.af = 0;
         this.l = true;
         AgeOfEmpires.b.a = true;
      }
   }

   public final synchronized void q() {
      if (this.ar >= 20 && AgeOfEmpires.a) {
         AgeOfEmpires.b.c();
         AgeOfEmpires.b.a = true;
         this.s();
         this.j = true;
         this.ab = 0;
      }
   }

   public final void h() {
      a.a();
   }

   public final int b() {
      AgeOfEmpires.b.a();
      this.ae = 0;
      this.b(129);
      this.f = null;
      this.f = new byte[314];
      this.m();
      this.e();
      this.a = new int[2][91];
      this.a = new short[2][208];
      this.b = new int[2][88];
      this.b = new short[2][20];
      this.a = new short[4096];
      this.a = Font.getDefaultFont();
      this.ah = 19;
      this.ay = -2;
      this.f = 19;
      this.m(a.getWidth(), a.getHeight());
      this.i = com.ulysseo.mad.c.a(117);
      this.am = 4;
      this.H = 2;
      this.ar = 0;
      this.d = new int[7];
      this.e = new int[7];
      this.a = new int[4];
      this.m();
      int var1 = this.a(28, 1);
      this.aG = var1 >> 4;
      this.aj = var1 & 15;
      if (this.f[29] == 0) {
         this.d = true;
      } else {
         this.d = false;
      }

      if (this.f[30] == 0) {
         AgeOfEmpires.b.c = true;
      } else {
         AgeOfEmpires.b.c = false;
      }

      for (int var2 = 0; var2 < 7; var2++) {
         this.d[var2] = this.a(0 + (var2 << 2), 4);
      }

      this.R = 0;
      return -1;
   }

   public final void b() {
      a.b();
      a.setFullScreenMode(true);

      while (!a.isShown()) {
      }

      a.setCommandListener(this);
      a.getWidth();
      a.getHeight();
      a.a(80, 1);
   }

   public static final void a(Object[] var0) {
      if (var0 != null) {
         for (int var1 = 0; var1 < var0.length; var1++) {
            var0[var1] = null;
         }
      }
   }

   public final void v() {
      if (this.ar - this.u >= 50) {
         this.u = this.ar;
         this.m = 510;
      }

      this.ag = 20;
      this.a = null;
      this.c = null;
      a var1 = new a(99);
      this.a = var1.a(5);
      this.c = var1.a(4);
   }

   public final void a(int var1, boolean var2) {
      if (this.o != var1) {
         AgeOfEmpires.b.b = true;
         this.o = var1;
         this.c = var2;
      }
   }

   public final void c() {
      int var1 = a() % 6;
      int[] var2 = new int[]{204000, 123000, 233000, 180000, 188000, 190000, 143000, 197000, 184000, 175000};
      this.m = var2[var1] / 80;
      this.a(var1 + 3 + 131, false);
   }

   public final void a(int var1) {
      if (var1 > 127) {
         var1 = -(var1 - 128);
      }

      int var2 = var1;
      this.ae = 0;

      for (int var3 = 0; var3 < this.ak; var3++) {
         if (var2 == this.k[var3 << 1]) {
            this.ae = this.k[(var3 << 1) + 1];
            break;
         }
      }

      this.ab = this.ae;
      this.ax = this.ab;
   }

   public final void e(int var1) {
      this.ae = 0;
      this.L = 0;
      this.ab = 0;
      this.ax = 0;
   }

   public final void b(int var1) {
      this.k = com.ulysseo.mad.c.a(var1);
      this.ak = this.k.length >> 1;
   }

   public final void commandAction(Command var1, Displayable var2) {
      this.a(var1.getCommandType());
   }

   public final void m() {
      try {
         RecordStore var1;
         if ((var1 = RecordStore.openRecordStore(".nfo", true)).getNumRecords() == 0) {
            var1.addRecord(this.f, 0, this.f.length);
         } else {
            this.f = var1.getRecord(1);
         }

         var1.closeRecordStore();
      } catch (RecordStoreException var2) {
      }
   }

   public final void I() {
      try {
         RecordStore var1;
         if ((var1 = RecordStore.openRecordStore(".nfo", true)).getNumRecords() == 0) {
            var1.addRecord(this.f, 0, this.f.length);
         } else {
            var1.setRecord(1, this.f, 0, this.f.length);
         }

         var1.closeRecordStore();
      } catch (RecordStoreException var2) {
      }
   }

   public final int a(int var1, int var2) {
      int var3 = 0;

      while (--var2 >= 0) {
         int var4;
         var3 = (var4 = var3 << 8) | this.f[var1 + var2] & 255;
      }

      return var3;
   }

   public final void h(int var1, int var2, int var3) {
      int var4 = var3;

      while (var2 > 0) {
         this.f[var1++] = (byte)(var4 & 0xFF);
         var4 >>= 8;
         var2--;
      }
   }

   public final void p(Graphics var1) {
      this.ar++;
      if (this.j) {
         if (Runtime.getRuntime().freeMemory() < 50000L) {
            return;
         }

         this.j = false;
      }

      if (AgeOfEmpires.b.a) {
         AgeOfEmpires.b.b = true;
         this.F = 0;
         this.O = 0;
         this.B = this.aO;
         this.P = this.j;
         var1.setClip(0, 0, this.aO, this.j);
         var1.setColor(0);
         var1.fillRect(0, 0, this.aO, this.j);
         this.O = 0;
         this.P = this.j;
         AgeOfEmpires.b.a = false;
         this.s();
         this.h = false;
         this.j = true;
         switch (this.aA) {
            case 0:
            case 10:
            case 12:
               return;
            case 1:
               this.j(0);
               return;
            case 2:
               if (this.aN == 1) {
                  this.e(var1);
               } else if (this.aN == 4 && this.z != 98) {
                  this.f(var1);
               } else {
                  this.j(var1);
               }

               this.F = 0;
               this.O = 0;
               this.B = this.aO;
               this.P = this.j;
               return;
            case 3:
            case 5:
            case 6:
            case 7:
            case 8:
            default:
               this.j(var1);
               this.c(var1);
               return;
            case 4:
            case 9:
               this.am = 4;
               this.aA = 9;
               return;
            case 11:
               this.c(0);
               this.aQ = 0;
         }
      } else {
         try {
            boolean var4 = false;
            var1.setFont(this.a);
            var1.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
            if (this.a()) {
               this.n(var1);
               var1.setClip(0, 0, this.aO, this.j);
               if (this.f) {
                  this.a(var1, 21, 4, this.j - 6, 14, 0, 7, 6, 0, 0);
               }

               if (this.b) {
                  this.a(var1, 21, this.aO - 10, this.j - 6, 21, 0, 7, 6, 0, 0);
               }

               switch (this.aA) {
                  case 2:
                  case 4:
                  case 5:
                  case 9:
                  case 10:
                  case 11:
                  case 12:
                  case 13:
                  case 14:
                     break;
                  case 3:
                  case 6:
                  case 8:
                  default:
                     if (this.m-- <= 0) {
                        this.c();
                     }

                     this.g();
                     this.p();
                     this.B();
                     this.G();
                     this.J();
                     this.z();
                     this.j();
                     this.F();
                     if (this.ag > 0) {
                        if (this.a != null) {
                           var1.setColor(0);
                           var1.drawString(this.a, this.F + 2, this.P - (this.ah - this.ay), 20);
                           var1.setColor(16777215);
                           var1.drawString(this.a, this.F + 1, this.P - (this.ah - this.ay + 1), 20);
                        }

                        if (this.c != null) {
                           var1.setColor(0);
                           var1.drawString(this.c, this.F + 2, this.P - ((this.ah << 1) - this.ay), 20);
                           var1.setColor(16777215);
                           var1.drawString(this.c, this.F + 1, this.P - ((this.ah << 1) - this.ay + 1), 20);
                        }

                        this.ag--;
                        if (this.ag == 0) {
                           this.a = null;
                           this.c = null;
                        }
                     }
                     break;
                  case 7:
                     this.F();
               }

               if (this.P != this.j && this.aA == 6 || this.aA == 7 && this.W != 0) {
                  int var6 = 0;
                  int var7 = this.aL;
                  int var8 = this.aO - 41;
                  int var9 = this.a(this.b);
                  var1.setClip(41, var7, var8, this.f);
                  var1.setColor(0);
                  var1.fillRect(41, var7, var8, this.f);
                  if (var9 > var8) {
                     var6 = 41 - (this.R++ << 1) % (var8 + var9);
                  } else {
                     var6 = 41 - var8;
                  }

                  var1.setColor(16777215);
                  var1.drawString(this.b, var6 + var8, var7 + this.ay, 20);
               }

               this.E();
            }

            if (AgeOfEmpires.b.b) {
               if (!AgeOfEmpires.b.c) {
                  AgeOfEmpires.b.b = false;
                  return;
               }

               if (this.o < 0) {
                  return;
               }

               if (Runtime.getRuntime().freeMemory() >= 30000L && AgeOfEmpires.b.a(this.o, this.c)) {
                  AgeOfEmpires.b.b = false;
               }
            }

            if (this.h) {
               this.s();
               this.h = false;
               this.j = true;
            }
         } catch (Exception var10) {
         }
      }
   }

   final boolean a() {
      if (this.am == this.aA) {
         return true;
      } else {
         boolean var1 = false;
         switch (this.am) {
            case 1:
               var1 = this.j(this.aH);
               break;
            case 2:
               var1 = this.n(this.aH);
            case 3:
            default:
               break;
            case 4:
               var1 = this.d(this.aH);
               break;
            case 5:
               var1 = this.o(this.aH);
               break;
            case 6:
               var1 = this.h(this.aH);
               break;
            case 7:
               var1 = this.m(this.aH);
               break;
            case 8:
               var1 = this.i(this.aH);
               break;
            case 9:
               var1 = this.e(this.aH);
               break;
            case 10:
               var1 = this.a(this.aH);
               break;
            case 11:
               var1 = this.c(this.aH);
               break;
            case 12:
               var1 = this.f(this.aH);
         }

         if (var1) {
            this.aA = this.am;
            this.aH = 0;
            return true;
         } else {
            this.aA = 0;
            this.aH++;
            return false;
         }
      }
   }

   final void n(Graphics var1) {
      if (this.aA != 0) {
         switch (this.aA) {
            case 1:
               this.e(var1);
               return;
            case 2:
               this.g(var1);
               return;
            case 4:
               this.f(var1);
               return;
            case 5:
               this.i(var1);
               return;
            case 6:
               this.a(var1);
               return;
            case 7:
               this.l(var1);
               return;
            case 8:
               this.c(var1);
               return;
            case 9:
               this.o(var1);
               return;
            case 10:
               this.k(var1);
               return;
            case 11:
               this.h(var1);
               return;
            case 12:
               this.b(var1);
            case 3:
         }
      }
   }

   final void E() {
      if (this.aA != this.am) {
         switch (this.aA) {
            case 1:
               this.d();
               return;
            case 2:
               this.A();
               return;
            case 4:
               this.C();
               return;
            case 5:
               return;
            case 6:
               return;
            case 7:
               this.n();
               return;
            case 8:
               return;
            case 9:
               return;
            case 10:
               this.r();
               return;
            case 11:
               this.t();
               return;
            case 12:
               this.u();
            case 3:
         }
      }
   }

   final boolean g(int var1) {
      if (this.am != this.aA && var1 == 8) {
         return false;
      } else if (this.am != this.aA && var1 == 7) {
         return false;
      } else if ((var1 == 7 || var1 == 8) && this.aA == 1) {
         return false;
      } else {
         this.am = var1;
         return true;
      }
   }

   public final boolean d(int var1) {
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
      this.ag = 0;
      this.l();
      if (var1 == 0) {
         if (this.v >= 0) {
            this.i[this.aR + 0] = (byte)this.v;
         }

         this.f = true;
         this.b = true;
         switch (this.H) {
            case 0:
            case 10:
               this.a(131, true);
            case 1:
            case 5:
            case 9:
            default:
               break;
            case 3:
               this.a(131, true);
            case 2:
            case 4:
               this.f = false;
               this.b = false;
               break;
            case 6:
               this.f = true;
               this.b = false;
               break;
            case 7:
               int var6 = this.c(4) + 9;
               byte var3 = 0;
               if (this.d) {
                  var3 = 1;
               }

               this.i[var6 + 2] = (byte)var3;
               var6 = this.c(5) + 9;
               var3 = 1;
               if (AgeOfEmpires.b.c) {
                  var3 = 0;
               }

               this.i[var6 + 2] = (byte)var3;
               break;
            case 8:
               int var5 = this.c(5) + 9;
               this.i[var5 + 2] = (byte)this.v;
               break;
            case 11:
               int var4 = this.c(4) + 9;
               this.i[var4 + 1] = (byte)(this.aG + 1);
               this.i[var4 + 2] = (byte)this.aG;
               this.a(131, true);
               break;
            case 12:
               int var2 = this.c(4) + 9;
               this.i[var2 + 1] = (byte)(this.aj + 1);
               this.i[var2 + 2] = (byte)this.aj;
               this.a(131, true);
         }

         return false;
      } else {
         return true;
      }
   }

   public final void C() {
      a(this.a);
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
   }

   public final void f(Graphics var1) {
      int var2 = this.aR + 6;
      int var3 = this.a + this.i[this.aR + 4] >> 1;
      int var4 = this.K + this.i[this.aR + 5] >> 1;
      this.a = var3;
      this.K = var4;
      int var5 = this.aR + 6;
      int var6 = 0;
      int var7 = 0;

      for (int var18 = 0; var18 < this.ao; var18++) {
         if (var18 == this.Z) {
            var2 = var5;
         }

         byte var10;
         if (((var10 = this.i[var5 + 0]) & 32) == 0) {
            var5 = this.e(var5);
         } else {
            if ((var10 & 64) != 0) {
               byte var16;
               if (((var16 = this.i[var5 + 1]) & 127) != 127) {
                  int var17 = this.c(var16 & 127);
                  var6 = this.i[var17 + 4];
                  var7 = this.i[var17 + 5];
               }

               int var12 = this.i[var5 + 6] & 255;
               int var13 = this.i[var5 + 7] & 255;
               byte var15 = this.i[var5 + 2];
               int var14 = this.i[var5 + 3] & 255;
               if ((var16 & -128) != 0) {
                  if (var15 == -1) {
                     var7 = -(this.j >> 3);
                  } else {
                     var6 += var15;
                     var7 = -4;
                  }
               } else {
                  var6 += var14 * AgeOfEmpires.b.b(var15 << 3) >> 16;
                  var7 += var14 * AgeOfEmpires.b.c(var15 << 3) >> 16;
               }

               int var8 = var6 + (this.aO >> 1);
               int var9 = (this.j >> 1) - var7;
               if ((var10 & 4) == 0) {
                  var8 -= var3;
                  var9 += var4;
               }

               int var11;
               if (((var11 = this.i[var5 + 8]) & 16) == 0) {
                  if ((var11 & 32) == 0) {
                     var8 -= var12 >> 1;
                  } else {
                     var8 -= var12;
                  }
               }

               if ((var11 & 64) == 0) {
                  if ((var11 & -128) == 0) {
                     var9 -= var13 >> 1;
                  } else {
                     var9 = (this.j >> 2) - (var13 >> 1);
                  }
               }

               if (var18 == this.Z) {
                  if (var8 < this.F) {
                     this.i[this.aR + 4] = (byte)(this.i[this.aR + 4] - (this.F - var8));
                  }

                  if (var9 < this.O) {
                     this.i[this.aR + 5] = (byte)(this.i[this.aR + 5] + (this.O - var9));
                  }

                  if (var8 + var12 > this.B) {
                     this.i[this.aR + 4] = (byte)(this.i[this.aR + 4] + (var8 + var12 - this.B));
                  }

                  if (var9 + var13 > this.P) {
                     this.i[this.aR + 5] = (byte)(this.i[this.aR + 5] - (var9 + var13 - this.P));
                  }
               }

               if ((var10 & 16) != 0) {
                  if (var8 < 0) {
                     var6 -= var8;
                     var8 = 0;
                  }

                  if (var9 < 0) {
                     var7 += var9;
                     var9 = 0;
                  }

                  if (var8 + var12 > this.aO) {
                     var6 += var8 - this.aO;
                     var8 = this.aO - var12;
                  }

                  if (var9 + var13 > this.j) {
                     var7 -= var9 - this.j;
                     var9 = this.j - var13;
                  }
               }

               this.i[var5 + 4] = (byte)var6;
               this.i[var5 + 5] = (byte)var7;
               switch (var11 & 15) {
                  case 0:
                     this.d(var1, var8, var9, var5, var18);
                  case 1:
                  case 4:
                  case 5:
                  default:
                     break;
                  case 2:
                     this.c(var1, var8, var9, var5, var18);
                     break;
                  case 3:
                     byte var20 = this.i[var5 + 9];
                     this.a(var1, 10 + var20, var8, var9, 0);
                     break;
                  case 6:
                     boolean var19 = false;
                     this.e(var1, 0, 0, this.aO, this.j);
                     break;
                  case 7:
                     var1.setColor(this.i[var5 + 9] & 255, this.i[var5 + 9 + 1] & 255, this.i[var5 + 9 + 2] & 255);
                     var1.fillRect(0, 0, this.aO, this.j);
               }
            } else {
               switch (this.i[var5 + 8]) {
                  case 4:
                     if (this.aQ > this.i[var5 + 9]) {
                        this.k(var5, 0);
                     }
                     break;
                  case 5:
                     if (this.ax == 38 || this.ax == 22 || this.ax == 6) {
                        this.k(var5, 0);
                     }
                     break;
                  case 10:
                     this.F = this.aO * this.i[var5 + 9] / 100;
                     this.O = this.j * this.i[var5 + 9 + 1] / 100;
                     this.B = this.aO * this.i[var5 + 9 + 2] / 100;
                     this.P = this.j * this.i[var5 + 9 + 3] / 100;
                     var1.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
               }
            }

            var5 = this.e(var5);
         }
      }

      this.d(var2);
      if (this.ab == 22) {
         this.ab = 0;
      }

      this.ax = 0;
      this.aQ++;
   }

   public final void d(int var1) {
      if (this.aQ >= 10) {
         this.i[this.aR + 1] = (byte)this.Z;
         switch (this.ax) {
            case 3:
            case 19:
               int var6 = this.Z;
               int var9 = 0;

               while (--var6 >= 0) {
                  var9 = this.c(var6);
                  if ((this.i[var9] & -128) != 0) {
                     this.Z = var6;
                     return;
                  }
               }

               return;
            case 4:
            case 8:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 20:
            case 24:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            default:
               break;
            case 5:
            case 21:
               switch (this.i[var1 + 8]) {
                  case 2:
                     byte var5;
                     if ((var5 = this.i[var1 + 9 + 2]) > 0) {
                        this.i[var1 + 9 + 2] = (byte)(var5 - 1 & 0xFF);
                     } else {
                        this.i[var1 + 9 + 2] = (byte)(this.i[var1 + 9 + 1] - 1);
                     }

                     this.k(var1, 0);
                  default:
                     return;
               }
            case 6:
            case 22:
            case 38:
               this.v = this.H;
               this.k(var1, 1);
               return;
            case 7:
            case 23:
               switch (this.i[var1 + 8]) {
                  case 2:
                     byte var4 = this.i[var1 + 9 + 2];
                     byte var8 = this.i[var1 + 9 + 1];
                     if (var4 < var8 - 1) {
                        this.i[var1 + 9 + 2] = (byte)(var4 + 1 & 0xFF);
                     } else {
                        this.i[var1 + 9 + 2] = 0;
                     }

                     this.k(var1, 0);
                  default:
                     return;
               }
            case 9:
            case 25:
               int var2 = this.Z;
               int var3 = 0;

               while (++var2 < this.ao) {
                  var3 = this.c(var2);
                  if ((this.i[var3] & -128) != 0) {
                     this.Z = var2;
                     return;
                  }
               }

               return;
            case 47:
               if (this.H >= 2 && this.H <= 5) {
                  return;
               }

               if (this.g(9)) {
                  this.D();
                  this.v = -1;
                  if (this.H == 0) {
                     this.ap = 0;
                     this.i[this.aR + 1] = (byte)(this.i[this.aR + 2] - 1);
                     return;
                  }

                  this.ap = this.i[this.aR + 0];
                  return;
               }
         }
      }
   }

   public final void d(Graphics var1, int var2, int var3, int var4, int var5) {
      int var6 = this.aQ - var5;
      int var7 = this.i[var4 + 9] & 255;
      var3 += this.ay;
      if (this.i[var4 + 0] == 104) {
         var1.setColor(this.a(16777215, 14595245, var6, 10));
         var1.drawString(this.a[var7], var2 + 1, var3 + 1, 20);
         var1.setColor(this.a(16711680, 14595245, var6, 10));
         var1.drawString(this.a[var7], var2, var3, 20);
      } else {
         int var8 = 0;
         int var9 = 16777215;
         if ((this.i[var4 + 0] & -128) != 0) {
            if (var5 == this.Z) {
               var8 = 16711680;
               var9 = 16777215;
            }
         } else if ((var6 >>= 2) > 4) {
            var6 = 4;
         }

         var1.setColor(this.a(var9, 14595245, var6, 10));
         var1.drawString(this.a[var7], var2 + 1, var3 + 1, 20);
         var1.setColor(this.a(var8, 14595245, var6, 10));
         var1.drawString(this.a[var7], var2, var3, 20);
      }
   }

   public final void c(Graphics var1, int var2, int var3, int var4, int var5) {
      int var6 = (this.i[var4 + 9] & 255) + (this.i[var4 + 9 + 2] & 255);
      int var7 = this.i[var4 + 6] & 255;
      int var8 = this.aQ - var5;
      boolean var9 = false;
      int var10 = 16777215;
      int var11 = 14595245;
      if (var5 == this.Z) {
         int var12 = (AgeOfEmpires.b.b(this.aQ << 7) >> 9) + 128 << 16;
         var12 = this.a(var12, var11, var8, 10);
         var1.setColor(var12);
         var1.fillRect(var2, var3, var7, this.ah + 6);
         var1.setColor(this.a(16777215, var11, var8, 10));
         var1.fillRect(var2 + 1, var3 + 1, var7 - 2, this.ah + 4);
         var1.setColor(14595245);
         var1.fillRect(var2 + 2, var3 + 2, var7 - 4, this.ah + 2);
         if (this.i[var4 + 9 + 1] > 1) {
            int var13 = AgeOfEmpires.b.b(this.ar << 8) >> 14;
            if (var8 > 6) {
               this.a(var1, 21, var2 - var13 - 13, var3 + (this.ah >> 1), 28, 0, 7, 6, 0, 0);
               this.a(var1, 21, var2 + var7 + var13 + 6, var3 + (this.ah >> 1), 35, 0, 7, 6, 0, 0);
            }
         }
      } else if ((this.i[var4 + 0] & -128) != 0) {
         var1.setColor(this.a(13684944, var11, var8, 10));
         var1.fillRect(var2 + 1, var3 + 1, var7 - 2, this.ah + 4);
         var1.setColor(this.a(14595245, var11, var8, 10));
         var1.fillRect(var2 + 2, var3 + 2, var7 - 4, this.ah + 2);
      } else {
         var11 = 8421504;
      }

      var2 += var7 - this.a(this.a[var6]) >> 1;
      var3 += this.ay;
      var1.setColor(this.a(var10, var11, var8, 10));
      var1.drawString(this.a[var6], var2 + 1, var3 + 4, 20);
      var1.setColor(this.a(0, var11, var8, 10));
      var1.drawString(this.a[var6], var2, var3 + 3, 20);
   }

   public final int c(int var1) {
      int var2 = this.aR + 6;

      for (int var3 = 0; var3 < var1; var3++) {
         var2 = this.e(var2);
      }

      return var2;
   }

   public final int j(int var1) {
      byte var2 = this.i[var1 + 2];
      var1 += 6;

      for (int var3 = 0; var3 < var2; var3++) {
         var1 = this.e(var1);
      }

      return var1;
   }

   public final int e(int var1) {
      byte var2 = this.i[var1 + 8];
      var1 = this.k(var1);
      var1 = this.i(var1);
      if (var2 == 2) {
         var1 = this.i(var1);
      }

      return var1;
   }

   public final int i(int var1) {
      switch (this.i[var1++]) {
         case 2:
         case 3:
         case 6:
         case 7:
            var1++;
         case 4:
         default:
            break;
         case 5:
            var1 += 2;
      }

      return var1;
   }

   public final int k(int var1) {
      var1 += 8;
      switch (this.i[var1++] & 15) {
         case 0:
         case 4:
            var1++;
         case 1:
         case 5:
         default:
            break;
         case 2:
         case 7:
            var1 += 3;
            break;
         case 3:
         case 6:
         case 8:
         case 9:
            var1 += 2;
            break;
         case 10:
            var1 += 4;
      }

      return var1;
   }

   public final void l() {
      this.aR = 0;

      for (int var1 = 0; var1 < this.H; var1++) {
         this.aR = this.j(this.aR);
      }

      int var9 = this.i[this.aR + 3] & 255;
      this.Z = this.i[this.aR + 1];
      a var2 = new a(this.H + 83);
      this.a = new String[var9];

      for (int var3 = 0; var3 < var9; var3++) {
         this.a[var3] = var2.a(var3);
      }

      this.ao = this.i[this.aR + 2] & 255;
      int var10 = this.aR + 6;
      boolean var4 = false;

      for (int var5 = 0; var5 < this.ao; var5++) {
         switch (this.i[var10 + 8] & 15) {
            case 0:
               if (this.i[var10 + 3] == 10) {
                  this.i[var10 + 3] = (byte)this.ah;
               }

               this.i[var10 + 6] = (byte)(this.a(this.a[this.i[var10 + 9]]) + 1);
               this.i[var10 + 7] = (byte)this.ah;
            case 1:
            case 4:
            case 5:
            case 7:
            default:
               break;
            case 2:
               int var11 = 0;
               int var7 = 0;

               for (; var7 < this.i[var10 + 9 + 1]; var7++) {
                  int var8;
                  if ((var8 = this.a(this.a[this.i[var10 + 9] + var7])) > var11) {
                     var11 = var8;
                  }
               }

               this.i[var10 + 6] = (byte)(var11 + 8 & 0xFF);
               this.i[var10 + 7] = (byte)(this.ah + 4);
               this.i[var10 + 3] = (byte)(this.ah + 8);
               this.i[this.e(var10) + 3] = (byte)(this.ah + 8);
               break;
            case 3:
            case 6:
            case 8:
            case 9:
               Image var6;
               if ((var6 = this.a(10 + this.i[var10 + 9], 0)) != null) {
                  this.i[var10 + 6] = (byte)(var6.getWidth() & 0xFF);
                  this.i[var10 + 7] = (byte)(var6.getHeight() & 0xFF);
               }
         }

         var10 = this.e(var10);
      }
   }

   public final void D() {
      for (int var1 = 0; var1 < this.ao; var1++) {
         int var2 = this.c(var1);
         if ((this.i[var2 + 0] & 32) != 0 && this.i[var2 + 8] == 2) {
            this.k(var2, 0);
         }
      }
   }

   public final void k(int var1, int var2) {
      int var3 = this.k(var1);
      if (this.i[var1 + 8] == 2 && var2 > 0) {
         var3 = this.i(var3);
      }

      switch (this.i[var3++]) {
         case 2:
            if (this.g(9)) {
               this.D();
               this.ap = this.i[var3];
               this.v = this.H;
               return;
            }
            break;
         case 3:
            byte var12 = this.i[var1 + 9 + 2];
            if (this.i[var3] == 74) {
               return;
            }

            if (this.i[var3] == 67) {
               this.m();
               this.f[30] = 0;
               if (var12 == 1) {
                  this.f[30] = 1;
                  AgeOfEmpires.b.c();
                  AgeOfEmpires.b.c = false;
               } else {
                  AgeOfEmpires.b.c = true;
                  AgeOfEmpires.b.b = true;
               }

               this.I();
               return;
            }

            if (this.i[var3] == 71) {
               this.ac = 16;
               this.aC = var12;
               return;
            }

            if (this.i[var3] == 73) {
               this.ac = 32;
               this.aC = var12;
               return;
            }

            if (this.i[var3] == 72) {
               this.m();
               this.f[29] = 0;
               if (var12 == 0) {
                  this.f[29] = 1;
                  this.d = false;
               } else {
                  this.d = true;
               }

               this.I();
               return;
            }

            if (this.i[var3] == 65) {
               this.ac = 0;
               var3 = this.k(this.c(4));
               var3 = this.i(var3);
               this.i[var3] = 2;
               if (var12 == 0) {
                  this.i[var3 + 1] = 11;
                  return;
               }

               if (var12 == 1) {
                  this.i[var3 + 1] = 12;
                  return;
               }

               if (var12 == 2) {
                  this.i[var3 + 1] = 10;
                  return;
               }
            } else if (this.i[var3] == 66) {
               this.ac = 0;
               this.a = (byte)(var12 & 15);
            }
            break;
         case 4:
            this.e = true;
            return;
         case 5:
            byte var11 = this.i[var3++];
            byte var5 = this.i[var3];
            var3 = this.c(var11);
            this.i[var3] = (byte)(var5 & 255);
            return;
         case 6:
            this.O = 0;
            this.P = this.j;
            this.g(0, 82, this.i[var3]);
            this.v = -1;
            return;
         case 7:
            if (this.g(this.i[var3])) {
               this.D();
               return;
            }
            break;
         case 8:
            this.O = 0;
            this.P = this.j;

            for (int var4 = 0; var4 < 7; var4++) {
               this.e[var4] = this.d[var4];
            }

            this.g(0, 82, 3);
            this.v = 0;
            return;
         case 9:
         case 10:
         default:
            break;
         case 11:
            this.O = 0;
            this.P = this.j;
            this.m();
            if (this.f[33] == 0) {
               this.g(0, 82, 2);
            } else {
               this.g(14);
            }

            this.v = 0;
            return;
      }
   }

   public final boolean e(int var1) {
      return true;
   }

   public final void o(Graphics var1) {
      if (this.v != -1) {
         this.v = this.H;
      }

      if (this.ap != this.H) {
         this.H = this.ap;
         this.aQ = 0;
         this.a = 0;
         this.K = 0;
         if (this.H != 2 && this.H != 3 && this.H != 4) {
            this.f = true;
            this.b = true;
         } else {
            this.f = false;
            this.b = false;
         }
      }

      this.g(4);
   }

   public final boolean f(int var1) {
      this.f = true;
      this.b = false;
      this.O = 0;
      this.F = 0;
      this.P = this.j;
      this.B = this.aO;
      this.a = new String[6];
      a var2 = new a(97);

      for (int var3 = 0; var3 < 6; var3++) {
         this.a[var3] = var2.a(var3);
      }

      return true;
   }

   public final void u() {
      a(this.a);
   }

   public final void b(Graphics var1) {
      this.e(var1, 0, 0, this.aO, this.j);
      this.a(var1, this.a[5], this.aO - this.a(this.a[5]) >> 1, 9 + this.ay, this.aQ);
      int var4 = this.B - this.F - 24;
      boolean var5 = false;
      int var6;
      if ((var6 = (this.j >> 1) - 35) < 17) {
         var6 = 17;
      }

      this.a(var1, this.a[1], 12, var6, this.aQ);
      var6 += this.ah + 3;
      int var3;
      if ((var3 = this.a[0][86] + this.a[1][86]) > 0) {
         int var2 = this.a[0][86] * var4 / var3;
         var1.setColor(1065087);
         var1.fillRect(12, var6, var2, 3);
         var2 = this.a[0][2] * var4 / var3;
         var1.setColor(2130175);
         var1.fillRect(12, var6, var2, 3);
         var6 += 3;
         var2 = this.a[1][86] * var4 / var3;
         var1.setColor(8329232);
         var1.fillRect(12, var6, var2, 3);
         var2 = this.a[1][2] * var4 / var3;
         var1.setColor(16724000);
         var1.fillRect(12, var6, var2, 3);
         var6 += 7;
      } else {
         var6 += 10;
      }

      this.a(var1, this.a[2], 12, var6, this.aQ + 1);
      var6 += this.ah + 3;
      if ((var3 = this.a[0][88] + this.a[1][88]) > 0) {
         int var10 = this.a[0][88] * var4 / var3;
         var1.setColor(1065087);
         var1.fillRect(12, var6, var10, 3);
         var10 = this.a[0][4] * var4 / var3;
         var1.setColor(2130175);
         var1.fillRect(12, var6, var10, 3);
         var6 += 3;
         var10 = this.a[1][88] * var4 / var3;
         var1.setColor(8329232);
         var1.fillRect(12, var6, var10, 3);
         var10 = this.a[1][4] * var4 / var3;
         var1.setColor(16724000);
         var1.fillRect(12, var6, var10, 3);
         var6 += 7;
      } else {
         var6 += 10;
      }

      this.a(var1, this.a[0], 12, var6, this.aQ + 1);
      var6 += this.ah + 3;
      if ((var3 = this.a[0][90] + this.a[1][90]) > 0) {
         int var14 = this.a[0][90] * var4 / var3;
         var1.setColor(2130175);
         var1.fillRect(12, var6, var14, 3);
         var6 += 3;
         var14 = this.a[1][90] * var4 / var3;
         var1.setColor(16724000);
         var1.fillRect(12, var6, var14, 3);
      }

      if (this.ab == 22 || this.ab == 6 || this.ab == 38) {
         this.g(4);
      }

      if (this.ab == 22) {
         this.ab = 0;
      }

      this.ax = 0;
      this.ab = 0;
      this.aQ++;
   }

   public final boolean a(int var1) {
      this.f = true;
      this.b = false;
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
      a var2 = new a(65);
      this.a = new String[160];
      int var3 = 0;

      do {
         this.a[var3] = var2.a(var3);
      } while (this.a[var3++] != null);

      this.aQ = 0;
      return true;
   }

   public final void r() {
      a(this.a);
      this.v = -1;
      this.ab = 0;
      this.ax = 0;
   }

   public final void k(Graphics var1) {
      int var2 = this.j / 19;
      int var3 = this.aQ / 19 - var2;
      int var4 = -(this.aQ % 19);
      int var5 = var2 + 1 + var3;
      var1.setColor(0);
      var1.fillRect(0, 0, this.aO, this.j);
      if (var3 > 0 && this.a[var3] == null) {
         this.g(4);
      } else {
         for (int var6 = var3; var6 < var5; var6++) {
            if (var6 >= 0 && this.a[var6] != null) {
               int var7 = this.aO - this.a(this.a[var6]) >> 1;
               int var8;
               if ((var8 = this.a(6316128, 0, this.j - var4 - 19, this.j >> 2)) > 0) {
                  var1.setColor(var8);
                  var1.drawString(this.a[var6], var7, var4 + 1, 20);
               }

               if ((var8 = this.a(16777215, 0, this.j - var4 - 19, this.j >> 2)) > 0) {
                  var1.setColor(var8);
                  var1.drawString(this.a[var6], var7, var4, 20);
               }
            }

            var4 += 19;
         }

         this.aQ++;
         if (this.ax == 38 || this.ax == 22 || this.ax == 6 || this.ax == 47) {
            this.g(4);
            this.ab = 0;
         }
      }
   }

   public final boolean c(int var1) {
      this.f = false;
      this.b = false;
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
      this.a = new d();
      if (this.ac == 0) {
         this.m();
         this.a.o = this.a(31, 2);
         if (this.a.o == 0) {
            this.a.o = 8224;
         }

         k = (byte)(this.a.o >>> 8) & 255;
         r = (byte)(this.a.o & 0xFF);
      }

      if (this.aF != 0) {
         byte[] var2 = com.ulysseo.mad.c.a(this.aF);
         this.k = false;
         int var3 = var2[0] & 255;
         int var4 = var2[1] & 255;
         if ((var3 | var4) != 0) {
            k = var3;
            r = var4;
         } else {
            this.k = true;
         }
      }

      this.a.a(9, 20, this.a, this.k);
      this.aQ = 0;
      a var5 = new a(99);
      this.a = var5.a(0);
      return true;
   }

   public final void t() {
      if (this.k) {
         this.a[this.a.a[0] + 1 + (this.a.a[1] + 1 << 6)] = 0;
         this.a[this.a.a[0] - 1 + (this.a.a[1] + 1 << 6)] = 0;
         this.a[this.a.a[0] + 1 + (this.a.a[1] << 6)] = 0;
         this.a(0, 9, this.a.a[0], this.a.a[1], 255, false);
         this.a(0, 0, this.a.a[0] + 1, this.a.a[1] + 1, false);
         this.a(0, 0, this.a.a[0] - 1, this.a.a[1] + 1, false);
         if (this.a < 2) {
            this.a(0, 5, this.a.a[0] + 1, this.a.a[1], false);
         }

         this.aa = this.a.a[0];
         this.aV = this.a.a[1];
         this.Q = this.aa + (this.aV << 6);
         this.a[this.a.a[2] + 1 + (this.a.a[3] + 1 << 6)] = -32768;
         this.a[this.a.a[2] - 1 + (this.a.a[3] + 1 << 6)] = -32768;
         this.a(1, 9, this.a.a[2], this.a.a[3], 255, false);
         this.a(1, 0, this.a.a[2] + 1, this.a.a[3] + 1, false);
         this.a(1, 0, this.a.a[2] - 1, this.a.a[3] + 1, false);
      }

      if (this.aF != 0) {
         this.f(this.aF);
      }

      this.a.d();
      this.a = null;
      this.a = null;
      AgeOfEmpires.b.c();
   }

   public final void h(Graphics var1) {
      boolean var2 = false;
      int var3 = (this.j >> 1) - 10;
      var1.setClip(0, 0, this.aO, this.j);
      this.e(var1, 0, 0, this.aO, this.j);
      var1.setColor(7039826);
      var1.drawString(this.a, 14, var3 + 1, 20);
      var1.setColor(16777215);
      var1.drawString(this.a, 13, var3, 20);
      var3 += this.ah + 3;
      var1.setColor(7039826);
      var1.fillRect(11, var3 + 2, this.aO - 21, 10);
      var1.setColor(0);
      var1.fillRect(10, var3 + 1, this.aO - 21, 10);
      var1.setColor(16777215);
      var1.fillRect(8, var3, this.aO - 21, 9);
      int var4 = this.aO - 23;
      int var5 = this.a.n * var4 >> 8;
      short var6 = 128;
      byte var7 = 7;
      var3++;

      for (int var8 = 0; var8 < 4; var8++) {
         var1.setColor(var6 << 16 | var6 << 8);
         var1.fillRect(9, var3, var5, var7);
         var1.setColor(var6 << 16);
         var1.fillRect(9 + var5, var3, var4 - var5, var7);
         var7 -= 2;
         var6 += 40;
         var3++;
      }

      if (this.aQ == 0) {
         this.a.run();
      }

      if (this.a.c) {
         this.g(6);
      }

      this.aQ++;
   }

   static final int a() {
      r = r + k;
      r = r + ((k & 0xFF) >> 2);
      k = k ^ r;
      k = k + ((r & 0xFF) >> 1);
      return r & 0xFF;
   }

   public final void a(int var1, int var2, int var3, int var4) {
      this.a[var2 + (var3 << 6) & 4095] = (short)(this.a[var2 + (var3 << 6) & 4095] & '\uf000');
      if (var4 > 31) {
         var4 = 31;
      }

      var4 <<= 2;
      this.a[var2 + (var3 << 6) & 4095] = (short)(this.a[var2 + (var3 << 6) & 4095] | (short)(768 | var1 & 0xFF | var4));
   }

   public final boolean j(int var1) {
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
      this.aU = this.y;
      this.A = this.N;
      this.at = this.aa;
      this.n = this.aV;
      this.af = 0;
      int var2 = this.az + this.ad;
      int var3 = (this.al << 1) + this.J;
      int var4 = var2 + var3 >> 5;
      int var5 = var3 - var2 >> 5;
      this.y = var4 - var5 << 1;
      this.N = var4 + var5;
      this.ag = 0;
      this.ab = 0;
      return true;
   }

   public final void d() {
      this.y = this.aU;
      this.N = this.A;
      this.aa = this.at;
      this.aV = this.n;
      this.ax = 0;
      this.ab = 0;
   }

   public final void e(Graphics var1) {
      var1.setClip(0, 0, this.aO, this.j);
      var1.setColor(3438335);
      var1.fillRect(0, 0, this.aO, this.j);
      int var2 = this.aO >> 1;
      int var3 = this.j >> 1;
      Image var8;
      if ((var8 = this.a(20, 240, 120, 3438335)) != null) {
         if (this.af < 64) {
            for (int var9 = 0; var9 <= 64 && this.af < 64; var9++) {
               for (int var10 = 0; var10 < 64; var10++) {
                  this.b(this.a, var10, this.af);
               }

               this.af++;
            }
         } else {
            for (int var12 = 0; var12 < 2; var12++) {
               int var24 = 0;
               int var27 = this.a[var12][2];

               for (int var13 = 0; var13 < var27; var24 += 8) {
                  short var5;
                  int var4 = (var5 = this.a[var12][var24 + 0]) >>> 8;
                  var5 &= 255;
                  short var7;
                  int var6 = (var7 = this.a[var12][var24 + 1]) >>> 8;
                  var7 &= 255;
                  int var11 = this.a[var12][var24 + 6] & 240;
                  if (((var6 != var4 || var7 != var5) && var11 == 0 || var13 == this.ar % var27) && (this.a[var4 + (var5 << 6)] & '耀') == 0) {
                     this.b(this.a, var6, var7);
                     this.b(this.a, var4, var5);
                  }

                  var13++;
               }

               if ((var24 = this.a[var12][4]) != 0) {
                  if (var24 > 1) {
                     var24 = this.ar % (var24 - 1) << 2;
                  }

                  int var19;
                  int var15 = (var19 = this.b[var12][var24 + 0]) >>> 8;
                  var19 &= 255;
                  this.b(this.a, var15, var19);
               }
            }
         }

         int var26 = var2 - this.y - 120;
         int var28 = var3 - this.N;
         var1.setClip(this.F, this.O, this.B - this.F, this.P - this.O);
         com.ulysseo.mad.d.a(var1, var8, 0, 0, 240, 120, 0, var26, var28, 0);
         var1.setColor(16777215);
         if ((this.ar & 3) >= 2) {
            this.aQ = 4 - (this.ar & 3);
         } else {
            this.aQ = this.ar & 3;
         }

         int var16 = (this.aI >> 1) + this.aQ;
         int var21 = (this.aB >> 2) + this.aQ;
         int var29 = this.aI / 3 + 1;
         int var30 = var2 - var16;
         int var31 = var3 - var21;
         var16 = var2 + var16;
         var21 = var3 + var21;
         var1.drawLine(var16, var21, var16 - var29, var21);
         var1.drawLine(var16, var21, var16, var21 - var29);
         var1.drawLine(var30, var31, var30 + var29, var31);
         var1.drawLine(var30, var31, var30, var31 + var29);
         var1.drawLine(var16, var31, var16 - var29, var31);
         var1.drawLine(var16, var31, var16, var31 + var29);
         var1.drawLine(var30, var21, var30 + var29, var21);
         var1.drawLine(var30, var21, var30, var21 - var29);
         if (this.af >= 10) {
            int var14 = this.ax;
            if (this.ab == 0) {
               return;
            }

            if (this.ab == this.L) {
               this.s++;
               if (this.s >= 5) {
                  var14 = this.ab;
               }
            } else {
               this.s = 0;
               this.L = this.ab;
            }

            switch (var14) {
               case 1:
               case 6:
               case 22:
               case 38:
                  this.aU = this.y;
                  this.A = this.N;
                  this.at = this.aa;
                  this.n = this.aV;
               case 47:
                  this.O = 0;
                  this.P = this.j;
                  this.h = true;
                  this.am = 8;
                  this.f();
                  break;
               case 2:
                  var14 = 255;
                  break;
               case 3:
               case 19:
                  var14 = 15;
                  break;
               case 4:
                  var14 = 31;
                  break;
               case 5:
               case 21:
                  var14 = 240;
                  break;
               case 7:
               case 23:
                  var14 = 16;
                  break;
               case 8:
                  var14 = 241;
                  break;
               case 9:
               case 25:
                  var14 = 1;
                  break;
               case 10:
                  var14 = 17;
                  break;
               case 11:
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 20:
               case 24:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
               case 32:
               case 33:
               case 34:
               case 35:
               case 36:
               case 37:
               case 39:
               case 40:
               case 41:
               case 42:
               case 43:
               case 44:
               case 45:
               case 46:
               default:
                  var14 = 0;
            }

            if ((var14 & 240) == 240) {
               if (this.y > -119) {
                  this.y -= 4;
                  this.aa--;
                  this.aV++;
               }
            } else if ((var14 & 240) == 16 && this.y < 119) {
               this.y += 4;
               this.aa++;
               this.aV--;
            }

            if ((var14 & 15) == 15) {
               if (this.N > 0) {
                  this.N -= 2;
                  this.aa--;
                  this.aV--;
                  return;
               }
            } else if ((var14 & 15) == 1 && this.N < 119) {
               this.N += 2;
               this.aa++;
               this.aV++;
            }
         }
      }
   }

   public final void b(Graphics var1, int var2, int var3) {
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      int var9 = this.a[var2 + (var3 << 6) & 4095];
      var1.setClip(0, 0, 240, 120);
      var2 += var2;
      var3 += var3;
      int var10 = var2 - var3 + 120;
      int var11 = var3 + var2 >> 1;
      int var12 = (var9 & 3072) >> 10;
      if (var9 >= 0) {
         switch (var9 & 768) {
            case 0:
               if ((var9 & 16384) != 0) {
                  var4 = 2385412;
                  var5 = 2385412;
                  var6 = 2385412;
                  var7 = 2385412;
                  var8 = 2385412;
               } else {
                  var4 = 47872;
                  var5 = 47872;
                  var6 = 47872;
                  var7 = 47872;
                  var8 = 47872;
               }
               break;
            case 256:
               if (var12 == 0) {
                  var4 = 128;
                  var5 = 128;
                  var6 = 128;
                  var7 = 128;
                  var8 = 128;
                  var1.setColor(128);
                  var1.fillRect(var10 + 1, var11, 1, 1);
                  var1.setColor(128);
                  var1.fillRect(var10 - 1, var11, 1, 1);
               } else {
                  var4 = 15728640;
                  var5 = 15728640;
                  var6 = 15728640;
                  var7 = 15728640;
                  var8 = 15728640;
               }
               break;
            case 512:
               if (this.af < 64) {
                  return;
               }

               var7 = 2121728;
               var6 = 2121728;
               var8 = 2121728;
               if (var12 == 0) {
                  var4 = 15790320;
                  var5 = 128;
               } else {
                  var4 = 15790320;
                  var5 = 15728640;
               }
               break;
            case 768:
               if ((var9 = var9 & 3) == 0) {
                  var4 = 3438335;
                  var5 = 3438335;
                  var6 = 3438335;
                  var7 = 3438335;
                  var8 = 3438335;
               } else if (var9 == 1) {
                  var1.setColor(2129920);
                  var1.fillRect(var10, var11 - 2, 1, 1);
                  var1.setColor(8433664);
                  var1.fillRect(var10, var11 - 1, 1, 1);
                  var1.setColor(2129920);
                  var1.fillRect(var10 + 1, var11, 1, 1);
                  var1.setColor(2129920);
                  var1.fillRect(var10 - 1, var11, 1, 1);
                  var4 = 4235264;
                  var5 = 8404992;
                  var6 = 2129920;
                  var7 = 8404992;
                  var8 = 8404992;
               } else if (var9 == 2) {
                  var4 = 15790080;
                  var5 = 8421376;
                  var6 = 12632192;
                  var7 = 15790080;
                  var8 = 8421376;
               } else if (var9 == 3) {
                  var4 = 8421504;
                  var5 = 10526848;
                  var6 = 10526848;
                  var7 = 8421504;
                  var8 = 10526975;
               }
         }
      } else if ((var9 & 4095) == 768) {
         var4 = 3438335;
         var5 = 3438335;
         var6 = 3438335;
         var7 = 3438335;
         var8 = 3438335;
      }

      var1.setColor(var4);
      var1.fillRect(var10, var11, 1, 1);
      var1.setColor(var5);
      var1.fillRect(var10, var11 + 1, 1, 1);
      var1.setColor(var6);
      var1.fillRect(var10 - 1, var11 + 1, 1, 1);
      var1.setColor(var7);
      var1.fillRect(var10 + 1, var11 + 1, 1, 1);
      var1.setColor(var8);
      var1.fillRect(var10, var11 + 2, 1, 1);
   }

   public final boolean h(int var1) {
      this.F = 0;
      this.B = this.aO;
      this.v = -1;
      if (this.O == 0) {
         this.l = true;
      }

      this.f = true;
      this.b = true;
      return true;
   }

   public final void a(Graphics var1) {
      if (this.l) {
         this.g(8);
      }

      this.o();
      if (this.ab == 22) {
         this.ab = 0;
      }

      this.ax = 0;
      this.f();
      this.j(var1);
   }

   public final boolean o(int var1) {
      this.m = 0;
      this.F = 0;
      this.B = this.aO;
      this.O = 0;
      this.P = this.j;
      if (var1 == 0) {
         this.c = 0;
         this.b = null;
         this.b = new int[48];

         for (int var7 = 0; var7 < 181; var7++) {
            this.a[var7] = null;
         }

         this.aE = 0;
         this.h = -1;
         this.aJ = 0;
         this.Y = 0;
         this.b = 0;
         this.a = -1;
         this.c = -1;
         this.b = -1;
         this.aq = 0;
         this.E = 0;
         a(this.a);
         this.a[0] = 0;
         this.a[1] = 0;
         this.a[2] = 0;
         this.a[3] = 0;
         this.e = null;
         this.c = null;
         this.a = null;
         this.g = null;
         return false;
      } else {
         this.e = com.ulysseo.mad.c.a(122);
         this.c = com.ulysseo.mad.c.a(127);
         this.g = com.ulysseo.mad.c.a(123);
         this.i = 0;

         for (int var2 = 0; var2 < 4096; var2++) {
            this.a[var2] = -32768;
         }

         for (int var5 = 0; var5 < 2; var5++) {
            byte[] var3 = com.ulysseo.mad.c.a(121);

            for (int var4 = 0; var4 < 91; var4++) {
               this.a[var5][var4] = var3[var4];
            }

            this.a[var5][56] = this.a[var5][56] << 8;
            this.a[var5][54] = 16777215;
            this.a[var5][12] = 16;
         }

         this.aM = 256;
         this.q = 0;
         this.w = 0;
         this.aq = 0;
         this.i = 0;
         if (this.ac == 0) {
            this.i = true;
            switch (this.a) {
               case 0:
                  this.a[0][5] = 200;
                  this.a[0][6] = 100;
                  this.a[0][7] = 100;
                  this.a[1][5] = 50;
                  this.a[1][6] = 15;
                  this.a[1][7] = 15;
                  this.aM = 512;
                  this.an = 250;
                  this.q = 50;
                  this.C = 20;
                  this.l = 49;
                  this.aw = Integer.MAX_VALUE;
                  break;
               case 1:
                  this.a[0][5] = 200;
                  this.a[0][6] = 100;
                  this.a[0][7] = 100;
                  this.a[1][5] = 50;
                  this.a[1][6] = 50;
                  this.a[1][7] = 50;
                  this.aM = 786;
                  this.an = 150;
                  this.q = 60;
                  this.C = 20;
                  this.l = 36;
                  this.aw = 2500;
                  break;
               case 2:
                  this.a[0][5] = 200;
                  this.a[0][6] = 100;
                  this.a[0][7] = 100;
                  this.a[1][5] = 20;
                  this.a[1][6] = 20;
                  this.a[1][7] = 20;
                  this.aM = 2048;
                  this.an = 100;
                  this.q = 100;
                  this.C = 1;
                  this.l = 25;
                  this.aw = 1000;
            }
         }

         short var6;
         var6 = 128;
         this.aF = 0;
         label50:
         switch (this.ac) {
            case 16:
               if (this.aC == 0) {
                  var6 = 124;
                  this.I = 78;
                  this.aF = 118;
               } else if (this.aC == 1) {
                  var6 = 125;
                  this.I = 79;
                  this.aF = 119;
               } else if (this.aC == 2) {
                  var6 = 126;
                  this.I = 80;
                  this.aF = 120;
               }
               break;
            case 32:
               this.i = false;
               switch (this.aC) {
                  case 0:
                     var6 = 110;
                     this.I = 71;
                     this.aF = 103;
                     break label50;
                  case 1:
                     var6 = 111;
                     this.I = 72;
                     this.aF = 104;
                     break label50;
                  case 2:
                     var6 = 112;
                     this.I = 73;
                     this.aF = 105;
                     break label50;
                  case 3:
                     var6 = 113;
                     this.I = 74;
                     this.aF = 106;
                     break label50;
                  case 4:
                     var6 = 114;
                     this.I = 75;
                     this.aF = 107;
                     this.i = true;
                     this.an = 100;
                     this.aM = 256;
                     this.an = 200;
                     this.q = 30;
                     this.C = 200;
                     this.l = 49;
                     this.aw = 1000;
                     break label50;
                  case 5:
                     var6 = 115;
                     this.I = 76;
                     this.aF = 108;
                     this.q = -1000;
                     break label50;
                  case 6:
                     var6 = 116;
                     this.I = 77;
                     this.aF = 109;
                  default:
                     break label50;
               }
            default:
               this.k = true;
               this.l = true;
         }

         if (this.i) {
            this.ai = 0;
            this.a();
         }

         this.a = com.ulysseo.mad.c.a(var6);
         return true;
      }
   }

   public final void i(Graphics var1) {
      if (this.ac != 16 || this.aC != 0 && this.aC != 2) {
         this.g(11);
      } else {
         this.f(this.aF);
         this.g(6);
      }
   }

   public final boolean m(int var1) {
      this.F = 0;
      this.B = this.aO;
      if (this.a) {
         if (this.g) {
            this.aK = 0;
         }

         this.T = 0;
      }

      this.ag = 0;
      a var2 = new a(99);
      this.a = var2.a(6);
      this.c = new int[this.X];
      this.a = new String[this.X << 1];
      this.W = 0;
      a var3 = new a(this.G);

      for (int var4 = 0; var4 < this.X; var4++) {
         if (this.k(var4) || !this.g) {
            int var5 = var4;
            this.c[this.W] = var4;
            if (this.g == 1 && var4 == 12) {
               var5 = (var5 = var4 + this.c[36]) + this.c[40] + this.c[43];
               this.c[this.W] = this.c[this.W] + this.c[36];
            }

            this.a[this.W << 1] = var3.a(var5 << 1);
            this.a[(this.W << 1) + 1] = var3.a((var5 << 1) + 1);
            this.W++;
         }
      }

      return true;
   }

   public final void n() {
      this.c = null;
      this.a = null;
      a(this.a);
   }

   public final void l(Graphics var1) {
      if (this.W == 0) {
         this.am = 6;
      } else {
         this.P = this.aL;
         var1.setClip(0, 0, this.aO, this.j);
         var1.setColor(0);
         var1.fillRect(0, this.P, this.aO, this.j - this.P);
         if (this.a) {
            int var2 = 0;
            boolean var8 = false;
            int var3 = this.aO - 111 >> 1;
            int var4;
            if ((var4 = this.P - this.O - 111 >> 1) < this.O) {
               var4 = this.O;
            }

            if (this.g) {
               var1.setColor(4227136);
               var1.fillRect(var3 - 1, var4 - 1, 112, 112);
               this.i();
               this.ax = 0;
               this.ab = 0;

               for (int var9 = 0; var9 < 3; var9++) {
                  for (int var10 = 0; var10 < 3; var10++) {
                     int var5 = var10 * 37 + var3;
                     int var6 = var9 * 37 + var4;
                     if (var2 == this.T) {
                        if ((this.ar & 1) == 0) {
                           var1.setColor(16777215);
                        } else {
                           var1.setColor(16711680);
                        }

                        var1.fillRect(var5 - 1, var6 - 1, 38, 38);
                     }

                     if (var2 < this.W) {
                        byte var11 = 0;
                        if (this.g == 0) {
                           if (this.a[0][3] > this.a[0][49] + this.a[0][2]) {
                              if (this.a(0, this.g, this.c[var2])) {
                                 var11 = 0;
                              } else {
                                 var11 = 4;
                              }
                           } else {
                              var11 = 4;
                           }
                        } else if (this.a(0, this.g, this.c[var2])) {
                           var11 = 0;
                        } else {
                           var11 = 4;
                        }

                        this.a(var1, this.x, var5, var6, this.c[var2] * 36, 0, 36, 36, 0, var11);
                        var1.setColor(0);
                        var1.fillRect(var5 + 36 - 5, var6 + 36 - 7, 4, 6);
                        int var7 = var2 + 1;
                        this.b(var1, var7, var5 + 36 - 8, var6 + 36 - 12, 1);
                     } else {
                        var1.fillRect(var5, var6, 36, 36);
                     }

                     var2++;
                  }
               }
            } else {
               byte var19 = 0;
               boolean var21 = this.a(0, 2, this.aK);
               switch (this.ax) {
                  case 5:
                  case 21:
                     if (this.T > 0) {
                        this.T--;
                     }
                     break;
                  case 6:
                  case 22:
                  case 38:
                     if (this.T == 2) {
                        this.g(0, 81, this.aK);
                     } else {
                        if (var21 && this.T == 0) {
                           this.y();
                        }

                        this.am = 6;
                     }
                     break;
                  case 7:
                  case 23:
                     if (this.T < 2) {
                        this.T++;
                     }
                     break;
                  case 47:
                     this.T = 1;
                     this.am = 6;
               }

               this.ax = 0;
               this.ab = 0;
               if (var21) {
                  var19 = 0;
               } else {
                  var19 = 4;
                  if (this.T == 0) {
                     this.T = 1;
                  }
               }

               boolean var23 = false;
               int var12;
               if ((var12 = this.a(this.a)) < 68) {
                  var12 = 68;
               }

               var12 += 52;
               int var13;
               if ((var13 = 17 + this.ah + 11) < 44) {
                  var13 = 44;
               }

               int var15 = this.aO - var12 >> 1;
               int var17 = (this.P - this.O >> 1) + this.O - (var13 >> 1);
               var1.setColor(0);
               var1.fillRect(var15 + 1, var17 + 1, var12, var13);
               var1.setColor(7031296);
               var1.fillRect(var15, var17, var12, var13);
               var1.setColor(11899986);
               var1.fillRect(var15 + 1, var17 + 1, var12 - 2, var13 - 2);
               var1.setColor(14595245);
               var1.drawString(this.a, var15 + 36 + 8, var17 + 4 - 2, 20);
               var1.setColor(0);
               var1.drawString(this.a, var15 + 36 + 7, var17 + 3 - 2, 20);
               this.a(var1, this.x, var15 + 3, var17 + (var13 - 36 >> 1), this.aK * 36, 0, 36, 36, 0, var19);
               var17 = var17 + var13 - 4 - 20;
               if ((this.ar & 1) == 0) {
                  var1.setColor(16777215);
               } else {
                  var1.setColor(16711680);
               }

               int var14;
               var14 = (var14 = var12 - 32) / 3;
               var15 += 44;
               if (this.T == 0) {
                  var1.drawRect(var15 - 2, var17 - 2, 27, 19);
               }

               this.a(var1, this.x, var15, var17, 864, 0, 24, 17, 0, 0);
               if (this.T == 1) {
                  var1.drawRect(var15 + var14 - 2, var17 - 2, 27, 19);
               }

               this.a(var1, this.x, var15 + var14, var17, 864, 17, 24, 17, 0, 0);
               if (this.T == 2) {
                  var1.drawRect(var15 + (var14 << 1) - 2, var17 - 2, 15, 22);
               }

               this.a(var1, this.x, var15 + (var14 << 1), var17, 888, 0, 12, 20, 0, 0);
            }
         }

         this.a(var1, this.c[this.aK], this.a[this.aK << 1], this.a[(this.aK << 1) + 1]);
         if (!this.a) {
            this.t = this.Q;
            this.am = 6;
         }
      }
   }

   final void i() {
      boolean var1 = false;
      boolean var2 = false;
      int var3 = 1000;
      switch (this.ax) {
         case 2:
            var3 = 0;
            break;
         case 3:
            var3 = 1;
            break;
         case 4:
            var3 = 2;
            break;
         case 5:
            var3 = 3;
            break;
         case 6:
            var3 = 4;
            break;
         case 7:
            var3 = 5;
            break;
         case 8:
            var3 = 6;
            break;
         case 9:
            var3 = 7;
            break;
         case 10:
            var3 = 8;
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 24:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         default:
            break;
         case 19:
            var3 = this.T - 3;
            var1 = true;
            break;
         case 21:
            var3 = this.T - 1;
            var1 = true;
            break;
         case 22:
         case 38:
            var2 = true;
            break;
         case 23:
            var3 = this.T + 1;
            var1 = true;
            break;
         case 25:
            var3 = this.T + 3;
            var1 = true;
            break;
         case 47:
            this.g(6);
            this.t = this.Q;
            return;
      }

      if ((var3 = var3 & 0xFF) == this.T) {
         var2 = true;
      } else if (var3 < this.W) {
         this.T = var3;
      }

      if (!var1) {
         if (this.T != this.aK) {
            this.R = 40;
         }

         this.aK = this.T;
         if (var2) {
            this.y();
            this.g(8);
            this.t = this.Q;
         }
      }
   }

   final void y() {
      switch (this.g) {
         case 0:
            if (this.a[0][2] + this.a[0][49] < this.a[0][3] && this.a(0, 0, this.c[this.aK])) {
               this.c(0, this.c[this.aK]);
               return;
            }
            break;
         case 1:
            int var1;
            if ((var1 = this.c[this.aK]) > 12) {
               var1 = 12;
            }

            if (this.a(0, 1, this.c[this.aK])) {
               this.b = 1;
               this.p = var1;
               this.d = this.y + this.aP;
               this.S = this.N + this.aS;
               this.au = this.j;
               this.U = 8;
               return;
            }
            break;
         case 2:
            if (this.a(0, 2, this.c[this.aK])) {
               this.b[0][this.aD + 2] = this.b[0][this.aD + 2] | 536870912;
               this.b[0][this.aD + 2] = this.b[0][this.aD + 2] | 65536;
               this.b[0][this.aD + 2] = this.b[0][this.aD + 2] & -65281;
               this.c(0, 2, this.c[this.aK]);
            }
      }
   }

   final boolean k(int var1) {
      if (!this.a) {
         return true;
      } else {
         switch (this.g) {
            case 0:
               if (this.c[0 + var1] == 0) {
                  return false;
               }

               if (var1 == 9 && this.a[0][74] + this.a[0][65] > 0) {
                  return false;
               }
               break;
            case 1:
               if (this.c[10 + var1] == 0) {
                  return false;
               }

               switch (var1) {
                  case 1:
                     if (this.a(0, 1, false) >= 2) {
                        return false;
                     }

                     return true;
                  case 11:
                     if (this.a(0, 11, false) >= 4) {
                        return false;
                     }

                     return true;
                  case 12:
                     if (this.a(0, 12, false) >= 5) {
                        return false;
                     }

                     return true;
                  default:
                     return true;
               }
            case 2:
               if (this.c[23 + var1] == 0) {
                  return false;
               }
         }

         return true;
      }
   }

   public final boolean i(int var1) {
      this.F = 0;
      this.B = this.aO;
      return true;
   }

   public final void c(Graphics var1) {
      this.d(var1);
      this.am = 6;
   }

   public final void d(Graphics var1) {
      var1.setClip(0, 0, this.aO, this.j);
      int var2 = this.aO - 240 >> 1;
      var1.setColor(0);
      if (var2 > 0) {
         var1.fillRect(0, 0, this.aO, 21);
      } else {
         var1.fillRect(0, 19, this.aO, 1);
      }

      this.a(var1, 16, var2, 0, 0, 0, 240, 19, 0, 0);
      this.b(var1, this.a[0][5], var2 + 26, 3, 3);
      this.b(var1, this.a[0][6], var2 + 84, 3, 3);
      this.b(var1, this.a[0][7], var2 + 140, 3, 3);
      this.b(var1, this.a[0][2], var2 + 197, 3, 2);
      this.b(var1, this.a[0][3], var2 + 220, 3, 2);
      this.O = 21;
      this.l = false;
   }

   public final void b(Graphics var1, int var2, int var3, int var4, int var5) {
      byte var6 = 1;

      for (int var7 = 0; var7 < var5 - 1; var7++) {
         var6 *= 10;
      }

      if (var2 >= var6 * 10) {
         var2 = 999;
      } else if (var2 < 0) {
         var2 = 0;
      }

      while (var5 > 0) {
         int var8 = var2 / var6;
         this.a(var1, 16, var3, var4, var8 * 8 + 240, 0, 8, 12, 0, 0);
         var3 += 9;
         var2 -= var8 * var6;
         var5--;
         var6 /= 10;
      }
   }

   public final void a(Graphics var1, int var2, String var3, String var4) {
      byte var5 = 0;
      if (this.aO > 128) {
         var5 += 2;
      }

      int var6 = 0;
      byte var7 = 17;
      switch (this.g) {
         case 0:
            var6 = var2 + 0;
            var7 = 17;
            break;
         case 1:
            var6 = var2 + 10;
            var7 = 18;
            break;
         case 2:
            var6 = var2 + 26;
            var7 = 19;
      }

      var6 *= 3;
      byte var8 = 0;
      if (this.a && (this.e[var6] > this.a[0][5] || this.e[var6 + 1] > this.a[0][6] || this.e[var6 + 2] > this.a[0][7])) {
         var8 = 4;
      }

      int var9 = this.j - 19 + 2;
      this.a(var1, var7, var5, this.aL + (this.j - this.aL - 36 >> 1), var2 * 36, 0, 36, 36, 0, var8);
      this.a(var1, 16, 41, var9, 1, 1, 27, 17, 0, 0);
      this.b(var1, this.e[var6++], 67, var9, 2);
      this.a(var1, 16, 99, var9, 58, 1, 27, 17, 0, 0);
      this.b(var1, this.e[var6++], 125, var9, 2);
      this.a(var1, 16, 157, var9, 115, 1, 27, 17, 0, 0);
      this.b(var1, this.e[var6], 183, var9, 2);
      this.b = var3 + " " + var4;
   }

   public final void f() {
      this.az = (this.aa - this.aV << 4) - this.ad;
      this.al = (this.aa + this.aV << 3) - (this.J >> 1) + 8;
      this.y = ((this.az << 1) + this.y + 1) / 3;
      this.N = ((this.al << 1) + this.N + 1) / 3;
      this.Q = this.aa + (this.aV << 6) & 4095;
      if ((this.Q & -4096) != 0) {
         this.Q = -1;
      } else {
         if ((this.a[this.Q] & 4095) == 768) {
            this.Q = -1;
         }
      }
   }

   public final void m(int var1, int var2) {
      this.aO = var1;
      this.j = var2;
      this.aI = (this.aO >> 6) + 3;
      this.aB = (this.j >> 4) + 5;
      this.ad = (this.aO >> 1) + 64 >> 1;
      this.J = this.j + 48 >> 1;
      this.aL = this.f + 19 + 2;
      if (this.aL < 36) {
         this.aL = 36;
      }

      this.aL = this.j - this.aL;
   }

   public final void j(int var1, int var2) {
      this.m(var1, var2);
   }

   public final void e() {
      this.d = null;
      this.b = null;
      this.a = null;
      this.h = null;
      this.d = com.ulysseo.mad.c.a(102);
      this.b = com.ulysseo.mad.c.a(100);
      this.a = new Image[181];
      this.h = com.ulysseo.mad.c.a(101);
   }

   public final void s() {
      for (int var1 = 0; var1 < 181; var1++) {
         this.a[var1] = null;
      }

      this.a = null;
   }

   public final Image a(int var1, int var2, int var3, int var4) {
      if (this.a[var1] != null) {
         return this.a[var1];
      } else {
         try {
            this.a[var1] = Image.createImage(var2, var3);
         } catch (Exception var7) {
            this.h = true;
            return null;
         }

         this.a = this.a[var1].getGraphics();
         this.a.setColor(var4);
         this.a.fillRect(0, 0, var2, var3);
         return this.a[var1];
      }
   }

   public final Image a(int var1, int var2, int var3) {
      return AgeOfEmpires.b.a(com.ulysseo.mad.c.a(var1), var2, var3);
   }

   public final Image a(int var1, int var2) {
      int var3 = var1;
      if (var1 <= 3) {
         if (var2 == 2) {
            var1 += 173;
         }
      } else if (var1 >= 17 && var1 <= 19) {
         if (var2 != 0) {
            var1 = var1 - 17 + 177;
         }
      } else if (var1 >= 22) {
         var1 += (var2 & 3) * 38;
      }

      if (this.a[var1] != null) {
         return this.a[var1];
      } else if (Runtime.getRuntime().freeMemory() < 50000L) {
         this.h = true;
         return null;
      } else {
         try {
            this.a[var1] = this.a(2 + var3, 0, var2);
            return this.a[var1];
         } catch (Exception var5) {
            this.h = true;
            return null;
         }
      }
   }

   public final void a(Graphics var1, int var2, int var3, int var4, int var5) {
      int var6 = var2 << 1;
      var3 -= this.b[var6++];
      var4 -= this.b[var6];
      Image var7;
      if ((var7 = this.a(var2, var5)) != null) {
         var1.drawImage(var7, var3, var4, 0);
      }
   }

   public final void a(Graphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      int var11 = var2 << 1;
      var3 -= this.b[var11++];
      var4 -= this.b[var11];
      Image var12;
      if ((var12 = this.a(var2, var10)) != null) {
         com.ulysseo.mad.d.a(var1, var12, var5, var6, var7, var8, var9, var3, var4, 0);
      }
   }

   public final int a(int var1, int var2, int var3, int var4) {
      int var5 = var1 >> 16 & 0xFF;
      int var6 = var1 >> 8 & 0xFF;
      int var7 = var1 & 0xFF;
      int var8 = var2 >> 16 & 0xFF;
      int var9 = var2 >> 8 & 0xFF;
      int var10 = var2 & 0xFF;
      if (var3 < 0) {
         var3 = 0;
      }

      if (var3 >= var4) {
         return var1;
      } else {
         var8 = ((var5 - var8 << 16) / var4 * var3 >> 16) + var8;
         var9 = ((var6 - var9 << 16) / var4 * var3 >> 16) + var9;
         var10 = ((var7 - var10 << 16) / var4 * var3 >> 16) + var10;
         if (var8 < 0) {
            var8 = 0;
         } else if (var8 > 255) {
            var8 = 255;
         }

         if (var9 < 0) {
            var9 = 0;
         } else if (var9 > 255) {
            var9 = 255;
         }

         if (var10 < 0) {
            var10 = 0;
         } else if (var10 > 255) {
            var10 = 255;
         }

         return var8 << 16 | var9 << 8 | var10;
      }
   }

   public final void a(Graphics var1, String var2, int var3, int var4, int var5) {
      var1.setColor(this.a(16777215, 14595245, var5, 5));
      var1.drawString(var2, var3 + 1, var4 + 1, 20);
      var1.setColor(this.a(0, 14595245, var5, 5));
      var1.drawString(var2, var3, var4, 20);
   }

   public final void o() {
      if (this.Q != this.t) {
         this.P = this.j;
      }

      int var1 = this.ax;
      if (this.ab != 0) {
         if (this.ab == this.L) {
            this.s++;
            if (this.s >= 5) {
               var1 = this.ab;
            }
         } else {
            this.s = 0;
            this.L = this.ab;
         }

         switch (var1) {
            case 2:
               this.aa--;
               break;
            case 3:
            case 19:
               this.aa--;
               this.aV--;
               break;
            case 4:
               this.aV--;
               break;
            case 5:
            case 21:
               this.aa--;
               this.aV++;
            case 6:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 20:
            case 22:
            case 24:
            default:
               break;
            case 7:
            case 23:
               this.aa++;
               this.aV--;
               break;
            case 8:
               this.aV++;
               break;
            case 9:
            case 25:
               this.aa++;
               this.aV++;
               break;
            case 10:
               this.aa++;
         }

         switch (this.ax) {
            case 1:
               this.h = true;
               this.g(1);
               return;
            case 6:
            case 22:
            case 38:
               this.x();
               return;
            case 11:
               if (this.g(7)) {
                  this.g = 0;
                  this.x = 17;
                  this.G = 67;
                  this.X = 10;
                  this.a = true;
                  this.g = true;
                  return;
               }
               break;
            case 12:
               if (this.g(7)) {
                  this.g = 1;
                  this.x = 18;
                  this.G = 63;
                  this.X = 13;
                  this.a = true;
                  this.g = true;
               }
               break;
            case 47:
               if (this.Q != -1 && (this.a[this.Q] & 768) == 256 && (this.a[this.Q] & 3072) >> 10 == 0) {
                  int var3 = (this.a[this.Q] & 255) << 2;
                  if ((this.b[0][var3 + 2] & 536870912) != 0) {
                     return;
                  }

                  if ((this.b[0][var3 + 2] & 0xFF0000) != 0) {
                     this.b[0][var3 + 2] = this.b[0][var3 + 2] - 65536;
                     this.b[0][var3 + 2] = this.b[0][var3 + 2] & -536870913;
                     if ((this.b[0][var3 + 2] & 0xFF0000) == 0) {
                        this.b[0][var3 + 2] = this.b[0][var3 + 2] & -65281;
                     }

                     int var8 = this.a[0][0];
                     byte var5 = 1;
                     int var6;
                     if ((var6 = this.b[0][var3 + 3] & 0xFF) == 10) {
                        if (var8 == 0) {
                           var5 = 2;
                        } else {
                           var5 = 3;
                        }
                     } else if (var6 == 7) {
                        var5 = 4;
                     } else if (var6 == 8) {
                        if (var8 >= 2) {
                           var5 = 6;
                        } else {
                           var5 = 5;
                        }
                     } else if (var6 == 6) {
                        var5 = 8;
                     } else if (var6 == 2) {
                        var5 = 7;
                     } else if (var6 == 3) {
                        var5 = 9;
                     }

                     this.a[0][49]--;
                     if (var5 < 2) {
                        this.a[0][66]--;
                        return;
                     }

                     this.a[0][66 + var5 - 1]--;
                     return;
                  }

                  if ((this.b[0][var3 + 2] & 1073741824) != 0) {
                     var3 = this.a[this.Q] & 255;
                     int var4 = this.b[0][(var3 << 2) + 3] & 0xFF;
                     this.i(0, var3);
                     this.b(0, 1, var4);
                     return;
                  }
               }

               if (this.b == 1) {
                  this.H();
                  return;
               }

               if (this.h != -1) {
                  if (this.h == 256 && this.Y == 0 && (this.b[0][this.aJ + 2] & 0xFF0000) != 0) {
                     this.b[0][this.aJ + 2] = this.b[0][this.aJ + 2] - 65536;
                     return;
                  }

                  this.H();
                  return;
               }

               if (this.b != 0) {
                  this.H();
                  return;
               }

               if (this.g(4)) {
                  this.O = 0;
                  this.F = 0;
                  this.P = this.j;
                  this.B = this.aO;
                  this.H = 6;
                  this.v = this.H;
                  return;
               }
         }
      }
   }

   public final void e(Graphics var1, int var2, int var3, int var4, int var5) {
      Image var6;
      if ((var6 = this.a(14, 0)) != null) {
         int var7 = 42;
         int var8 = 38;
         if (var4 < 84) {
            var7 = var4 >> 1;
         }

         if (var5 < 76) {
            var8 = var5 >> 1;
         }

         var1.setColor(14595245);
         var1.fillRect(var2 + var7, var3 + var8, var4 - var7 - var7, var5 - var8 - var8);
         com.ulysseo.mad.d.a(var1, var6, 0, 0, var7, var8, 0, var2, var3, 0);
         com.ulysseo.mad.d.a(var1, var6, 0, 0, var7, var8, 1, var2, var3 + var5 - var8 - 1, 0);
         if (var7 == 42) {
            int var9 = var2 + 42;

            for (byte var10 = 2; var9 < var2 + var4 - var7; var10 ^= 2) {
               com.ulysseo.mad.d.a(var1, var6, 18, 0, 24, var8, var10, var9, var3, 0);
               com.ulysseo.mad.d.a(var1, var6, 18, 0, 24, var8, var10 | 1, var9, var3 + var5 - var8, 0);
               var9 += 24;
            }
         }

         if (var8 == 38) {
            int var11 = var3 + 38;

            for (byte var12 = 1; var11 < var3 + var5 - var8; var12 ^= 1) {
               com.ulysseo.mad.d.a(var1, var6, 0, 17, var7, 21, var12, var2, var11, 0);
               com.ulysseo.mad.d.a(var1, var6, 0, 17, var7, 21, var12 | 2, var2 + var4 - var7, var11, 0);
               var11 += 21;
            }
         }

         com.ulysseo.mad.d.a(var1, var6, 0, 0, var7, var8, 2, var2 + var4 - var7 - 1, var3, 0);
         com.ulysseo.mad.d.a(var1, var6, 0, 0, var7, var8, 3, var2 + var4 - var7 - 1, var3 + var5 - var8 - 1, 0);
      }
   }

   public final boolean l(int var1) {
      var1 <<= 2;
      if ((this.b[0][var1 + 2] & 1073741824) != 0) {
         return false;
      } else if ((this.b[0][var1 + 2] & 0xFF) != 255 && this.h < 2 && this.Y == 0) {
         return false;
      } else {
         this.g = -1;
         int var2 = this.a[0][0];
         switch (this.b[0][var1 + 3] & 0xFF) {
            case 0:
               if (this.c[26] == 0 && var2 >= 1) {
                  this.aK = 3;
               } else {
                  if (this.c[24] != 0 || var2 < 2) {
                     return false;
                  }

                  this.aK = 1;
               }

               this.g = 2;
               break;
            case 1:
               if (this.c[28] == 0 && var2 >= 1) {
                  this.aK = 5;
               } else if (this.c[32] == 0 && var2 >= 1) {
                  this.aK = 9;
               } else if (this.c[42] == 0 && var2 >= 2) {
                  this.aK = 19;
               } else {
                  if (this.c[41] != 0 || var2 < 2) {
                     return false;
                  }

                  this.aK = 18;
               }

               this.g = 2;
               break;
            case 2:
               this.aK = 7;
               this.g = 0;
               break;
            case 3:
               if (this.a[0][74] + this.a[0][65] > 0) {
                  return false;
               }

               this.aK = 9;
               this.g = 0;
               break;
            case 4:
               if (this.c[37] == 0 && var2 >= 2) {
                  this.aK = 14;
               } else if (this.c[34] == 0 && var2 >= 2) {
                  this.aK = 11;
               } else if (this.c[33] == 0 && var2 >= 3) {
                  this.aK = 10;
               } else {
                  if (this.c[35] != 0 || var2 < 3) {
                     return false;
                  }

                  this.aK = 12;
               }

               this.g = 2;
               break;
            case 5:
               if (this.c[29] != 0) {
                  return false;
               }

               this.aK = 6;
               this.g = 2;
               break;
            case 6:
               if (this.c[27] == 0 && var2 >= 1) {
                  this.aK = 4;
               } else if (this.c[31] == 0 && var2 >= 1) {
                  this.aK = 8;
               } else if (this.c[30] == 0 && var2 >= 2) {
                  this.aK = 7;
               } else if (this.c[25] == 0 && var2 >= 2) {
                  this.aK = 2;
               } else if (this.c[23] == 0 && var2 >= 3) {
                  this.aK = 0;
               } else {
                  if (this.c[38] != 0 || var2 < 3) {
                     if (var2 < 3) {
                        return false;
                     }

                     this.aK = 8;
                     this.g = 0;
                     break;
                  }

                  this.aK = 15;
               }

               this.g = 2;
               break;
            case 7:
               this.aK = 4;
               this.g = 0;
               break;
            case 8:
               if (var2 == 1) {
                  this.aK = 5;
               } else {
                  this.aK = 6;
               }

               this.g = 0;
               break;
            case 9:
               this.g = 2;
               this.aK = 21 + var2;
               if (var2 == 0) {
                  if (this.a(0, 10, true) < 1) {
                     return false;
                  }
               } else if (var2 == 1) {
                  if (this.a(0, 5, true) + this.a(0, 6, true) < 2) {
                     return false;
                  }
               } else {
                  if (var2 != 2) {
                     return false;
                  }

                  if (this.a(0, 3, true) < 1) {
                     return false;
                  }
               }
               break;
            case 10:
               if (var2 == 0) {
                  this.aK = 2;
               } else {
                  this.aK = 3;
               }

               this.g = 0;
               break;
            case 11:
               this.aK = 0;
               this.g = 0;
               break;
            case 12:
               if (this.c[43] == 1) {
                  return false;
               }

               this.g = 2;
               if (this.c[43] == 0 && this.c[40] != 0 && var2 >= 3) {
                  this.aK = 20;
               } else {
                  if (this.c[40] == 0 && this.c[36] != 0 && var2 >= 2) {
                     this.aK = 17;
                     break;
                  }

                  if (this.c[36] != 0 || var2 < 1) {
                     return false;
                  }

                  this.aK = 13;
               }
               break;
            default:
               return false;
         }

         switch (this.g) {
            case 0:
               if (this.a[0][3] <= this.a[0][49] + this.a[0][2]) {
                  return true;
               }

               if (this.g(7)) {
                  this.a = false;
                  this.x = 17;
                  this.G = 67;
                  this.X = 10;
                  this.g = false;
               }

               this.a[0][49]++;
               if (this.aK < 2) {
                  this.a[0][66]++;
               } else {
                  this.a[0][66 + this.aK - 1]++;
               }

               this.b[0][var1 + 2] = this.b[0][var1 + 2] + 65536;
               break;
            case 2:
               if ((this.b[0][var1 + 2] & 536870912) != 0) {
                  return true;
               }

               if (this.g(7)) {
                  this.x = 19;
                  this.G = 64;
                  this.X = 24;
                  this.a = true;
                  this.g = false;
                  this.aD = var1;
               }

               this.b[0][var1 + 2] = this.b[0][var1 + 2] | -2147483648;
               this.b[0][var1 + 2] = this.b[0][var1 + 2] & -16711681;
               break;
            default:
               return false;
         }

         return true;
      }
   }

   final void x() {
      if (((this.aa | this.aV) & -64) == 0 && this.Q != -1) {
         int var1 = (this.a[this.Q] & 3072) >> 10;
         int var2 = this.a[this.Q] & 768;
         int var3 = this.a[this.Q] & 255;
         if (var2 != 256 || var1 != 0 || !this.l(var3)) {
            switch (this.b) {
               case 0:
                  if (var2 == 256 && (this.b[var1][(var3 << 2) + 2] & 1073741824) != 0) {
                     return;
                  }

                  this.f(var1, var2, var3);
                  this.b = 6;
                  return;
               case 1:
                  if (var2 != 0 || this.a[this.Q] < 0) {
                     return;
                  }

                  if (this.a(0, 1, this.p)) {
                     this.a(0, this.p, this.aa, this.aV, 1073741824, true);
                  }

                  this.H();
                  this.b = 0;
                  break;
               case 6:
                  if (var1 == 0) {
                     if (this.Y == 0 && var2 == 256 && this.aE == 512 && this.h >= 2) {
                        this.H();
                        this.f(var1, var2, var3);
                        return;
                     }

                     if (var2 == 512) {
                        if (this.aE == 512 && this.Y == 0) {
                           int var4 = this.a[0][(var3 << 3) + 3] & 255;
                           if (this.h != -1 && this.h == var4) {
                              if (this.aJ == -1) {
                                 this.h(0, -1);
                                 return;
                              }

                              if (this.aJ == var3 << 3) {
                                 this.h(0, var4);
                                 return;
                              }
                           }
                        }

                        this.H();
                        this.f(var1, var2, var3);
                        return;
                     }
                  }

                  if (this.Y != 0 || this.aE != 512) {
                     this.H();
                     this.f(var1, var2, var3);
                     return;
                  }

                  this.d = this.y + this.aP;
                  this.S = this.N + this.aS;
                  this.au = this.j;
                  this.U = 8;
                  this.d(0, this.aa, this.aV);
                  if (this.h < 2 && var2 == 768 && (this.a[this.Q] & '\uf000') == 0) {
                     this.H();
                     this.b = 0;
                     return;
                  }
            }
         }
      }
   }

   public final void f(int var1, int var2, int var3) {
      this.aE = var2;
      this.Y = var1;
      this.R = 40;
      switch (var2) {
         case 256:
            var3 <<= 2;
            this.b[var1][var3 + 2] = this.b[var1][var3 + 2] | -2147483648;
            this.aJ = var3;
            this.h = this.b[var1][var3 + 3] & 0xFF;
            this.h = true;
            if (this.g(7)) {
               this.g = 1;
               this.x = 18;
               this.G = 63;
               this.a = false;
               this.X = 13;
               this.aK = this.h;
            }
            break;
         case 512:
            var3 <<= 3;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | '耀');
            this.aJ = var3;
            this.h = this.a[var1][var3 + 3] & 255;
            this.h = true;
            if (this.P == this.j && this.g(7)) {
               this.g = 0;
               this.x = 17;
               this.G = 67;
               this.a = false;
               this.X = 10;
               this.aK = this.h;
            }
            break;
         case 768:
            this.a[var3] = (short)(this.a[var3] | 128);
            break;
         default:
            return;
      }

      this.b = 6;
   }

   public final void h(int var1, int var2) {
      this.aE = 512;
      this.h = var2;
      this.aJ = -1;
      this.Y = var1;
      this.b = 6;
      byte var3 = 0;
      int var4 = this.a[this.Y][2];
      if (var2 == -1) {
         for (int var5 = 0; var5 < var4; var5++) {
            if ((this.a[this.Y][var3 + 3] & 255) >= 2) {
               this.a[this.Y][var3 + 4] = (short)(this.a[this.Y][var3 + 4] | '耀');
            }

            var3 += 8;
         }
      } else if (var2 < 2) {
         for (int var6 = 0; var6 < var4; var6++) {
            if ((this.a[this.Y][var3 + 3] & 255) < 2 && (this.a[this.Y][var3 + 7] & 15) == 0) {
               this.a[this.Y][var3 + 4] = (short)(this.a[this.Y][var3 + 4] | '耀');
            }

            var3 += 8;
         }
      } else {
         for (int var7 = 0; var7 < var4; var7++) {
            if ((this.a[this.Y][var3 + 3] & 255) == this.h) {
               this.a[this.Y][var3 + 4] = (short)(this.a[this.Y][var3 + 4] | '耀');
            }

            var3 += 8;
         }
      }
   }

   public final void H() {
      boolean var1 = false;
      this.b = 0;
      if (this.aE != -1) {
         byte var3 = 0;

         for (int var2 = 0; var2 < this.a[this.Y][2]; var2++) {
            this.a[this.Y][var3 + 4] = (short)(this.a[this.Y][var3 + 4] & 32767);
            var3 += 8;
         }

         var3 = 0;

         for (int var4 = 0; var4 < this.a[this.Y][4]; var4++) {
            this.b[this.Y][var3 + 2] = this.b[this.Y][var3 + 2] & 2147483647;
            var3 += 4;
         }

         this.aE = -1;
         this.h = -1;
      }
   }

   final void d(int var1, int var2, int var3) {
      short var4 = (short)(var2 << 8 | var3);
      byte var5 = 0;

      for (int var6 = 0; var6 < this.a[var1][2]; var5 += 8) {
         if ((this.a[var1][var5 + 4] & '耀') != 0) {
            this.a[var1][var5 + 2] = var4;
            this.a[var1][var5 + 1] = this.a[var1][var5 + 0];
            this.a[var1][var5 + 7] = 0;
            this.a[var1][var5 + 3] = (short)(this.a[var1][var5 + 3] & 255);
         }

         var6++;
      }
   }

   public final void c(int var1, int var2, int var3) {
      byte var4 = 0;
      switch (var2) {
         case 0:
            var4 = 0;
            break;
         case 1:
            var4 = 10;
            break;
         case 2:
            var4 = 26;
      }

      int var5;
      var3 = (var5 = var3 + var4) * 3;
      this.a[var1][5] = this.a[var1][5] - this.e[var3++];
      this.a[var1][6] = this.a[var1][6] - this.e[var3++];
      this.a[var1][7] = this.a[var1][7] - this.e[var3];
      if (this.a[var1][5] < 0) {
         this.a[var1][5] = 0;
      }

      if (this.a[var1][6] < 0) {
         this.a[var1][6] = 0;
      }

      if (this.a[var1][7] < 0) {
         this.a[var1][7] = 0;
      }

      this.l = true;
   }

   public final void b(int var1, int var2, int var3) {
      byte var4 = 0;
      switch (var2) {
         case 0:
            var4 = 0;
            break;
         case 1:
            var4 = 10;
            break;
         case 2:
            var4 = 26;
      }

      int var5;
      var3 = (var5 = var3 + var4) * 3;
      this.a[var1][5] = this.a[var1][5] + this.e[var3++];
      this.a[var1][6] = this.a[var1][6] + this.e[var3++];
      this.a[var1][7] = this.a[var1][7] + this.e[var3];
      this.l = true;
   }

   public final boolean a(int var1, int var2, int var3) {
      byte var4 = 0;
      switch (var2) {
         case 0:
            var4 = 0;
            break;
         case 1:
            var4 = 10;
            break;
         case 2:
            var4 = 26;
      }

      int var5;
      var3 = (var5 = var3 + var4) * 3;
      if (this.a[var1][5] < this.e[var3++]) {
         return false;
      } else {
         return this.a[var1][6] < this.e[var3++] ? false : this.a[var1][7] >= this.e[var3];
      }
   }

   final void p() {
      for (int var1 = 0; var1 < this.a[0][2]; var1++) {
         this.d(0, var1);
      }

      if (this.a[0][4] > 0) {
         int var6;
         int var2 = (var6 = this.ar % this.a[0][4]) << 2;
         int var3;
         int var4 = (var3 = this.b[0][var2 + 0]) & 0xFF;
         var3 >>>= 8;
         int var5 = this.b[0][var2 + 3] & 0xFF;
         if ((this.b[0][var2 + 2] & 1073741824) == 0) {
            if (var5 == 12) {
               this.a(var3, var4, 6);
               return;
            }

            this.a(var3, var4, 3);
         }
      }
   }

   final void a(int var1, int var2, int var3) {
      int var4 = 0;
      int var5 = var3;

      for (int var6 = 3 - (var3 << 1); var5 >= var4; var4++) {
         for (int var7 = 0; var7 < var4 << 1; var7++) {
            this.a[var1 - var4 + var7 + (var2 + var5 << 6) & 4095] = (short)(this.a[var1 - var4 + var7 + (var2 + var5 << 6) & 4095] & 4095);
            this.a[var1 - var4 + var7 + (var2 + var5 << 6) & 4095] = (short)(this.a[var1 - var4 + var7 + (var2 + var5 << 6) & 4095] | 4096);
            this.a[var1 - var4 + var7 + (var2 - var5 << 6) & 4095] = (short)(this.a[var1 - var4 + var7 + (var2 - var5 << 6) & 4095] & 4095);
            this.a[var1 - var4 + var7 + (var2 - var5 << 6) & 4095] = (short)(this.a[var1 - var4 + var7 + (var2 - var5 << 6) & 4095] | 4096);
         }

         for (int var8 = 0; var8 < var5 << 1; var8++) {
            this.a[var1 - var5 + var8 + (var2 + var4 << 6) & 4095] = (short)(this.a[var1 - var5 + var8 + (var2 + var4 << 6) & 4095] & 8191);
            this.a[var1 - var5 + var8 + (var2 + var4 << 6) & 4095] = (short)(this.a[var1 - var5 + var8 + (var2 + var4 << 6) & 4095] | 4096);
            this.a[var1 - var5 + var8 + (var2 - var4 << 6) & 4095] = (short)(this.a[var1 - var5 + var8 + (var2 - var4 << 6) & 4095] & 8191);
            this.a[var1 - var5 + var8 + (var2 - var4 << 6) & 4095] = (short)(this.a[var1 - var5 + var8 + (var2 - var4 << 6) & 4095] | 4096);
         }

         if (var6 < 0) {
            var6 += (var4 << 1) + 3;
         } else {
            var6 += (var4 - var5 << 1) + 10;
            var5--;
         }
      }
   }

   final void a(int var1, int var2, int var3, int var4, int var5) {
      short var6 = (short)var5;

      while (var4-- > 0) {
         int var7 = var3;
         int var8 = var4 + var2 << 6;

         for (int var9 = var1; var7-- > 0; var9++) {
            this.a[var9 + var8 & 4095] = (short)(this.a[var9 + var8 & 4095] | var6);
         }
      }
   }

   final void e(int var1, int var2, int var3, int var4) {
      while (var4-- > 0) {
         int var5 = var3;
         int var6 = var4 + var2 << 6;

         for (int var7 = var1; var5-- > 0; var7++) {
            this.a[var7 + var6 & 4095] = (short)(this.a[var7 + var6 & 4095] & 4095);
            this.a[var7 + var6 & 4095] = (short)(this.a[var7 + var6 & 4095] | 4096);
         }
      }
   }

   public final void l(int var1, int var2) {
      int var3 = var2 << 3;
      int var4 = this.a[var1][var3 + 0] >>> 8;
      int var5 = this.a[var1][var3 + 0] & 255;
      int var6 = var4 + (var5 << 6) - 65 & 4095;

      for (int var7 = 0; var7 < 3; var7++) {
         for (int var8 = 0; var8 < 3; var8++) {
            if ((this.a[var6] & 4096) == 0) {
               this.a[var6] = (short)(this.a[var6] & 16383 | 16384);
            }

            var6 = ++var6 & 4095;
         }

         var6 += 61;
         var6 &= 4095;
      }
   }

   final void d(int var1, int var2) {
      int var3 = var2 << 3;
      short var4;
      int var5 = (var4 = this.a[var1][var3 + 0]) >>> 8;
      var4 &= 255;
      int var6 = var5 + (var4 << 6);
      this.a[var6 - 65 & 4095] = (short)(this.a[var6 - 65 & 4095] & 16383);
      this.a[var6 - 64 & 4095] = (short)(this.a[var6 - 64 & 4095] & 16383);
      this.a[var6 - 63 & 4095] = (short)(this.a[var6 - 63 & 4095] & 16383);
      this.a[var6 - 1 & 4095] = (short)(this.a[var6 - 1 & 4095] & 16383);
      this.a[var6 & 4095] = (short)(this.a[var6 & 4095] & 16383);
      this.a[var6 + 1 & 4095] = (short)(this.a[var6 + 1 & 4095] & 16383);
      this.a[var6 + 63 & 4095] = (short)(this.a[var6 + 63 & 4095] & 16383);
      this.a[var6 + 64 & 4095] = (short)(this.a[var6 + 64 & 4095] & 16383);
      this.a[var6 + 65 & 4095] = (short)(this.a[var6 + 65 & 4095] & 16383);
   }

   public final void f(int var1) {
      this.a(0, 0, 64, 64, -32768);
      byte[] var2;
      int var3 = (var2 = com.ulysseo.mad.c.a(var1))[0] & 255;
      int var4 = var2[1] & 255;
      if ((var3 | var4) != 0) {
         k = var3;
         r = var4;
      } else {
         this.k = true;
      }

      for (int var5 = 0; var5 < 4; var5++) {
         this.a[var5] = 0;
      }

      this.a[0][0] = var2[2] & 255;
      this.a[0][5] = var2[3] & 255;
      this.a[0][6] = var2[4] & 255;
      this.a[0][7] = var2[5] & 255;
      this.a[1][5] = var2[6] & 255;
      this.a[1][6] = var2[7] & 255;
      this.a[1][7] = var2[8] & 255;
      this.a[0][8] = -1;
      this.a[1][8] = -1;
      this.l = true;
      this.aa = var2[9] & 255;
      this.aV = var2[10] & 255;
      this.f();
      int var10 = (var2[12] & 255) << 8 | var2[11] & 255;
      int var11 = 13;

      for (int var12 = 0; var12 < var10; var12++) {
         switch (var2[var11++]) {
            case 1:
               int var15 = var2[var11++] & 255;
               int var16 = var2[var11++] & 255;
               int var18 = var2[var11++] & 255;
               int var20 = var2[var11++] & 255;
               int var9 = var2[var11++] & 255;
               this.a(var16, var15, var18, var20, var9, false);
               break;
            case 2:
               int var14 = var2[var11++] & 255;
               int var6 = var2[var11++] & 255;
               int var17 = var2[var11++] & 255;
               int var19 = var2[var11++] & 255;
               this.a(var6, var14, var17, var19, false);
               break;
            case 3:
               int var13 = var2[var11++] | 768;
               int var7 = var2[var11++] & 255;
               int var8 = var2[var11++] & 255;
               this.a(var13, var7, var8, 31);
         }
      }

      this.a(64, 0, 0, 64, 768);
      this.a(0, 64, 64, 0, 768);
      this.e(64, 0, 0, 64);
      this.e(0, 64, 64, 0);
   }

   final void j() {
      for (int var1 = 0; var1 < 2; var1++) {
         byte var2 = 0;

         for (int var3 = 0; var3 < this.a[var1][4]; var2 += 4) {
            int var4;
            if (((var4 = this.b[var1][var2 + 2]) & 1073741824) != 0) {
               var4 += 8;
               int var19 = this.b[var1][var2 + 3] & 0xFF;
               if ((var4 & 65535) >= 255) {
                  this.b[var1][var2 + 2] = 255;
                  if (var1 == 0) {
                     this.ag = 20;
                     a var21 = new a(69);
                     int var25 = var19;
                     if (var19 == 12) {
                        var25 = (var25 = var19 + this.c[36]) + this.c[40] + this.c[43];
                     }

                     this.c = var21.a(var25);
                     this.a = var21.a(16);
                     if (this.d && this.a(0, var19, true) == 1) {
                        this.g(0, 70, var19);
                     }
                  }

                  switch (var19) {
                     case 1:
                        if (this.a[var1][10] == -1) {
                           this.a[var1][10] = this.b[var1][var2 + 0];
                        } else {
                           if (this.a[var1][11] == -1) {
                              this.a[var1][11] = this.b[var1][var2 + 0];
                           }

                           if (var1 == 0) {
                              this.c[10 + var19] = 0;
                           }
                        }
                        break;
                     case 2:
                        if (var1 == 0) {
                           this.c[7] = 1;
                        }
                        break;
                     case 3:
                        if (var1 == 0) {
                           this.c[9] = 1;
                        }
                     case 4:
                     case 6:
                     default:
                        break;
                     case 5:
                        this.a[var1][56] = this.a[var1][56] + (this.a[var1][56] >> 1);
                        break;
                     case 7:
                        if (var1 == 0) {
                           this.c[4] = 1;
                        }
                        break;
                     case 8:
                        if (var1 == 0) {
                           if (this.a[0][0] >= 2) {
                              this.c[6] = 1;
                           } else {
                              this.c[5] = 1;
                           }
                        }
                        break;
                     case 9:
                        this.a[var1][3] = this.a[var1][3] + 5;
                        if (var1 == 0) {
                           this.l = true;
                        }
                        break;
                     case 10:
                        if (var1 == 0) {
                           this.c[2] = 1;
                        }
                        break;
                     case 11:
                        if (var1 == 0) {
                           this.c[0] = 1;
                           this.l = true;
                        }

                        this.a[var1][3] = this.a[var1][3] + 5;
                        break;
                     case 12:
                        this.a[var1][55] = this.a[var1][55] + this.a[var1][45] + this.a[var1][46];
                        int var22 = this.a[var1][48] << 2;
                        this.b[var1][var22 + 0] = (short)var2;
                        this.b[var1][var22 + 1] = 1000;
                        this.b[var1][var22 + 3] = 0;
                        this.a[var1][48]++;
                        this.b[var1][var2 + 3] = this.b[var1][var2 + 3] & 16777215;
                        this.b[var1][var2 + 3] = this.b[var1][var2 + 3] | var22 << 24;
                  }
               } else {
                  this.b[var1][var2 + 2] = this.b[var1][var2 + 2] + 8;
               }
            } else {
               int var5;
               if ((var5 = var4 & 0xFF0000) != 0) {
                  if ((var4 & 536870912) != 0) {
                     boolean var20 = false;
                     if ((var4 & 0xFF00) + 2048 < 65280) {
                        this.b[var1][var2 + 2] = this.b[var1][var2 + 2] + 2048;
                     } else {
                        if (var1 == 0) {
                           byte var24 = -1;
                           switch (this.b[0][var2 + 3] & 0xFF) {
                              case 0:
                                 if (this.c[26] == 0) {
                                    var24 = 3;
                                    this.a[0][50] = this.a[0][50] + 5;
                                 } else if (this.c[24] == 0) {
                                    var24 = 1;
                                    this.a[0][50] = this.a[0][50] + 5;
                                 }
                                 break;
                              case 1:
                                 if (this.c[28] == 0) {
                                    var24 = 5;
                                    this.a[0][52] = this.a[0][52] + 3;
                                 } else if (this.c[32] == 0) {
                                    var24 = 9;
                                    this.a[0][51] = this.a[0][51] + 3;
                                 } else if (this.c[42] == 0) {
                                    var24 = 19;
                                    this.a[0][52] = this.a[0][52] + 3;
                                 } else if (this.c[41] == 0) {
                                    var24 = 18;
                                    this.a[0][51] = this.a[0][51] + 3;
                                 }
                              case 2:
                              case 3:
                              case 7:
                              case 8:
                              case 10:
                              case 11:
                              default:
                                 break;
                              case 4:
                                 if (this.c[37] == 0) {
                                    var24 = 14;

                                    for (int var34 = 0; var34 < 13; var34++) {
                                       this.a[0][33 + var34]++;
                                    }
                                 } else if (this.c[34] == 0) {
                                    var24 = 11;
                                    this.a[0][42]++;
                                    this.a[0][45]++;
                                 } else if (this.c[33] == 0) {
                                    var24 = 10;

                                    for (int var33 = 0; var33 < 13; var33++) {
                                       this.a[0][33 + var33]++;
                                    }
                                 } else if (this.c[35] == 0) {
                                    var24 = 12;
                                    this.a[0][42]++;
                                    this.a[0][45]++;
                                 }
                                 break;
                              case 5:
                                 var24 = 6;
                                 this.a[0][56] = this.a[0][56] + (this.a[0][56] >> 1);
                                 break;
                              case 6:
                                 if (this.c[27] == 0) {
                                    var24 = 4;

                                    for (int var32 = 0; var32 < 9; var32++) {
                                       this.a[0][13 + var32]++;
                                    }
                                 } else if (this.c[31] == 0) {
                                    var24 = 8;

                                    for (int var31 = 0; var31 < 9; var31++) {
                                       this.a[0][23 + var31]++;
                                    }
                                 } else if (this.c[30] == 0) {
                                    var24 = 7;

                                    for (int var30 = 0; var30 < 9; var30++) {
                                       this.a[0][13 + var30]++;
                                    }
                                 } else if (this.c[25] == 0) {
                                    var24 = 2;

                                    for (int var29 = 0; var29 < 9; var29++) {
                                       this.a[0][23 + var29]++;
                                    }
                                 } else if (this.c[23] == 0) {
                                    var24 = 0;

                                    for (int var28 = 0; var28 < 9; var28++) {
                                       this.a[0][13 + var28]++;
                                    }
                                 } else {
                                    if (this.c[38] != 0) {
                                       break;
                                    }

                                    var24 = 15;
                                    this.c[8] = 1;

                                    for (int var27 = 0; var27 < 9; var27++) {
                                       this.a[0][23 + var27]++;
                                    }
                                 }
                                 break;
                              case 9:
                                 this.a[0][0]++;
                                 if (this.a[0][0] == 1) {
                                    this.c[15] = 1;
                                    this.c[16] = 1;
                                    this.c[17] = 1;
                                    this.c[18] = 1;
                                    this.c[2] = 0;
                                    this.c[3] = 1;
                                    this.e(2, 3);
                                 } else if (this.a[0][0] == 2) {
                                    this.c[12] = 1;
                                    this.c[14] = 1;
                                    this.c[13] = 1;
                                    this.c[6] = this.c[5];
                                    this.c[5] = 0;
                                    this.e(5, 6);
                                 }

                                 this.l = true;
                                 this.g(0, 62, this.a[0][0] - 1);
                                 break;
                              case 12:
                                 if (this.c[36] == 0) {
                                    var24 = 13;
                                    this.a[0][47] = 0;
                                    this.a[0][46] = 2;
                                    this.a[0][45] = 15;
                                    this.a[0][12] = 25;
                                    this.a[1][12] = 25;
                                 } else if (this.c[40] == 0) {
                                    var24 = 17;
                                    this.a[0][47] = 3;
                                    this.a[0][46] = 3;
                                    this.a[0][45] = 20;
                                    this.a[0][12] = 36;
                                    this.a[1][12] = 36;
                                 } else if (this.c[43] == 0) {
                                    var24 = 20;
                                    this.a[0][47] = 2;
                                    this.a[0][46] = 4;
                                    this.a[0][45] = 25;
                                    this.a[0][12] = 36;
                                    this.a[1][12] = 36;
                                 }
                           }

                           if (var24 >= 0) {
                              this.c[23 + var24] = 1;
                              this.ag = 20;
                              a var35 = new a(68);
                              this.c = var35.a(24);
                              this.a = var35.a(var24);
                           }

                           this.g(8);
                        }

                        this.b[var1][var2 + 2] = this.b[var1][var2 + 2] & 0xFF;
                     }
                  } else {
                     int var6 = this.a[var1][56];
                     if ((var4 & 0xFF00) + var6 < 65280) {
                        this.b[var1][var2 + 2] = this.b[var1][var2 + 2] + var6;
                     } else {
                        int var7 = 0;
                        switch (this.b[var1][var2 + 3] & 0xFF) {
                           case 2:
                              var7 = 7;
                              break;
                           case 3:
                              var7 = 9;
                           case 4:
                           case 5:
                           case 9:
                           default:
                              break;
                           case 6:
                              var7 = 8;
                              break;
                           case 7:
                              var7 = 4;
                              break;
                           case 8:
                              if (this.a[0][0] >= 2) {
                                 var7 = 6;
                              } else {
                                 var7 = 5;
                              }
                              break;
                           case 10:
                              if (this.a[0][0] == 0) {
                                 var7 = 2;
                              } else {
                                 var7 = 3;
                              }
                              break;
                           case 11:
                              var7 = 1;
                              if ((this.ar & 1) != 0) {
                                 var7 = 0;
                              }
                        }

                        if (this.a(var1, 0, var7)) {
                           int var8 = this.b[var1][var2 + 0] >>> 8;
                           int var9 = this.b[var1][var2 + 0] & 0xFF;
                           int var10 = var8 + 1;
                           int var11 = var9 + 1;
                           int var12 = 0;
                           int var13 = 1;
                           int var14 = 0;
                           int var15 = 1;

                           while ((this.a[var10 + (var11 << 6) & 4095] & 4095) != 0 || ((var10 | var11) & -64) != 0) {
                              if (++var12 > 8) {
                                 var15++;
                                 var12 = 0;
                                 var13 = 1;
                              }

                              int var16 = (var13 << 1 & 15) + 0;
                              var10 = var8 + this.g[var16] * var15;
                              var11 = var9 + this.g[var16 + 1] * var15;
                              var13 = (var13 + 1) % 7;
                              if (var14++ >= 64) {
                                 return;
                              }
                           }

                           var5 -= 65536;
                           if (var1 == 0) {
                              this.ag = 20;
                              a var36 = new a(66);
                              this.c = var36.a(var7);
                              this.a = var36.a(10);
                           }

                           if (this.a(var1, var7, var10, var11, true)) {
                              this.a[var1][49]--;
                              if (--var7 < 0) {
                                 var7 = 0;
                              }

                              this.a[var1][66 + var7]--;
                              if (var5 == 0) {
                                 this.b[var1][var2 + 2] = this.b[var1][var2 + 2] & -16776961;
                              } else {
                                 this.b[var1][var2 + 2] = this.b[var1][var2 + 2] & -16776961;
                                 this.b[var1][var2 + 2] = this.b[var1][var2 + 2] | var5;
                              }
                           } else {
                              this.b[var1][var2 + 2] = this.b[var1][var2 + 2] & -16776961;
                              this.b[var1][var2 + 2] = this.b[var1][var2 + 2] & -16711681;
                           }

                           if (var1 == 0) {
                              this.g(8);
                           }
                        }
                     }
                  }
               }
            }

            var3++;
         }
      }
   }

   final void j(Graphics var1) {
      int var2 = 16711680;
      if (this.b == 1) {
         var2 = 65280;
      }

      var1.setColor(0);
      var1.fillRect(0, 0, this.aO, this.j);
      int var3 = this.aB;
      int var4 = this.y + (this.N << 1);
      int var5 = (this.N << 1) - this.y;
      int var6 = -(var4 & 31) + (var5 & 31);
      int var7 = (var4 & 31) + (var5 & 31) >> 1;
      int var8 = var6 - 32;
      int var9 = 3 - var7;
      var4 >>= 5;
      var5 >>= 5;
      byte var10 = 64;
      int var12 = var9;
      int var13 = var4;
      int var14 = var5;
      this.aP = -64;

      for (this.aS = -64; var3-- > 0; var9 += 16) {
         int var15 = var8 - var10;

         for (int var16 = this.aI; var16-- > 0; var5--) {
            if (((var4 | var5) & -64) == 0) {
               int var17;
               if ((var17 = var4 + (var5 << 6)) == this.Q) {
                  this.aP = var15;
                  this.aS = var9;
               }

               if ((this.a[var17] & 4095) == 768) {
                  this.a(var1, 0, var15, var9, 0);
               } else {
                  switch (this.a[var17] & 61440) {
                     case 0:
                     case 4096:
                        this.a(var1, 4, var15, var9, 0);
                        break;
                     case 16384:
                        this.a(var1, 5, var15, var9, 0);
                  }
               }
            } else {
               this.a(var1, 0, var15, var9, 0);
            }

            var15 += 64;
            var4++;
         }

         if (var10 == 0) {
            var10 ^= 32;
            var4 -= this.aI;
            var5 += this.aI + 1;
         } else {
            var10 = 0;
            var4 -= this.aI - 1;
            var5 += this.aI;
         }
      }

      if (this.Q >= 0 && (this.b != 1 || this.a[this.Q] >= 0)) {
         var1.setColor(var2);
         var1.drawLine(this.aP - 32, this.aS, this.aP, this.aS - 16);
         var1.drawLine(this.aP + 32, this.aS, this.aP, this.aS - 16);
         this.aP++;
         var1.drawLine(this.aP - 32, this.aS, this.aP, this.aS - 16);
         var1.drawLine(this.aP + 32, this.aS, this.aP, this.aS - 16);
         this.aP--;
      }

      if (this.U > 0) {
         this.m(var1);
      }

      var8 = var8;
      var9 = var12;
      var4 = var13;
      var5 = var14;
      var10 = 32;

      for (int var22 = this.aB; var22-- > 0; var9 += 16) {
         int var30 = var8 - var10;

         for (int var31 = this.aI; var31-- > 0; var5--) {
            if (((var4 | var5) & -64) == 0) {
               int var32 = var4 + (var5 << 6);
               short var18;
               int var19 = (var18 = this.a[var32]) & 768;
               if (var18 > 0) {
                  switch (var19) {
                     case 256:
                        this.b(var1, var30, var9, var32);
                        break;
                     case 512:
                        this.c(var1, var30, var9, var32);
                        break;
                     case 768:
                        if ((this.a[var32] & 3) != 0) {
                           this.a(var1, var30, var9, var32);
                        }
                  }
               } else {
                  switch (var19) {
                     case 512:
                        int var20 = (var18 & 3072) >> 10;
                        int var21 = var18 & 255;
                        if ((this.a[var20][(var21 << 3) + 7] & 255) == 1) {
                           this.d(var20, var21);
                           this.c(var1, var30, var9, var32);
                        }
                  }
               }

               if (var32 == this.Q && (this.b != 1 || this.a[this.Q] >= 0)) {
                  var1.setColor(var2);
                  var1.drawLine(this.aP - 32, this.aS, this.aP, this.aS + 16);
                  var1.drawLine(this.aP + 32, this.aS, this.aP, this.aS + 16);
                  this.aP++;
                  var1.drawLine(this.aP - 32, this.aS, this.aP, this.aS + 16);
                  var1.drawLine(this.aP + 32, this.aS, this.aP, this.aS + 16);
                  this.aP--;
               }
            }

            var30 += 64;
            var4++;
         }

         if (var10 == 0) {
            var10 ^= 32;
            var4 -= this.aI;
            var5 += this.aI + 1;
         } else {
            var10 = 0;
            var4 -= this.aI - 1;
            var5 += this.aI;
         }
      }
   }

   final void a(Graphics var1, int var2, int var3, int var4, int var5, int var6) {
      boolean var7 = false;
      byte var8 = 6;
      switch (var2) {
         case 2:
            var8 = 7;
            break;
         case 3:
         case 4:
            var8 = 8;
      }

      switch (var5) {
         case 0:
            this.a(var1, var8, var3, var4, 0, 0, 20, 2, var6, 0);
            return;
         case 1:
            this.a(var1, var8, var3, var4, 0, 2, 20, 14, var6, 0);
            return;
         case 2:
            this.a(var1, var8, var3, var4, 0, 16, 20, 18, var6, 0);
            return;
         case 3:
            this.a(var1, var8, var3, var4, 0, 34, 20, 4, var6, 0);
      }
   }

   final void b(Graphics var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      var6 &= 15;
      int var8 = (var4 - var2) * 20;
      int var9 = (var5 - var3) * 20;
      int var10 = (var8 * var6 >> 8) + var2;
      int var11 = (var9 * var6 - (AgeOfEmpires.b.c((var6 << 6) + 64) >> 4) >> 8) + var3;
      if (--var6 < 0) {
         var6 = 0;
      }

      int var12 = (var8 * var6 >> 8) + var2;
      int var13 = (var9 * var6 - (AgeOfEmpires.b.c((var6 << 6) + 64) >> 4) >> 8) + var3;
      if (var10 != var12 || var11 != var13) {
         switch (var7) {
            case 0:
               var1.setColor(16776960);
               var1.drawLine(var10, var11, var12, var13);
               var1.fillRect(--var10, --var11, 3, 3);
               return;
            case 1:
               var1.setColor(16777215);
               var1.drawLine(var10, var11, var12, var13);
               return;
            default:
               var1.setColor(12632256);
               var1.drawLine(var10, var11, var12, var13);
               var1.fillRect(--var10, --var11, 3, 3);
         }
      }
   }

   final void c(Graphics var1, int var2, int var3, int var4) {
      short var5;
      int var6 = ((var5 = this.a[var4]) & 255) << 3;
      int var7 = (var5 & 3072) >> 10;
      int var8 = this.a[var7][var6 + 3] & 255;
      int var9 = var7;
      int var10 = this.a[var7][var6 + 7] & 15;
      if (var7 == 0 || (var5 & 16384) == 0 || var10 == 1) {
         byte var11 = 0;
         int var12 = 0;
         int var13 = this.a[var7][var6 + 7] >>> 8 & 0xFF;
         switch (var10) {
            case 0:
            case 3:
               var12 = (this.a[var7][var6 + 6] & '\uff00') >> 8;
               break;
            case 1:
               if (var8 != 4 && var8 != 8) {
                  var12 = -AgeOfEmpires.b.d(AgeOfEmpires.b.b((var13 & 7) << 7) >> 14);
                  var11 = 4;
               } else {
                  var11 = 3;
               }
               break;
            case 2:
               var12 = -AgeOfEmpires.b.d(AgeOfEmpires.b.b((var13 & 7) << 7) >> 14);
               var11 = 2;
               break;
            case 4:
               var12 = -AgeOfEmpires.b.d(AgeOfEmpires.b.b((var13 & 7) << 7) >> 14);
               var11 = 1;
         }

         int var14 = ((this.a[var7][var6 + 6] & 255) << 1) + 80;
         int var15 = 0;
         if (var12 != 0) {
            var15 = this.ar & 1;
         }

         int var16 = var12 * this.g[var14++];
         var12 *= this.g[var14];
         byte var17 = 0;
         if ((this.a[var7][var6 + 4] & 16384) != 0) {
            var17 = 2;
         }

         if ((this.a[var7][var6 + 4] & 8192) != 0) {
            var15 += 2;
         }

         if ((this.a[var7][var6 + 4] & 4096) != 0) {
            var9 = 3;
            this.a[var7][var6 + 4] = (short)(this.a[var7][var6 + 4] & '\uefff');
         }

         switch (var11) {
            case 0:
               this.a(var1, var8, var2 + var16, var3 + var12, var15, var17, var9);
               break;
            case 1:
            case 2:
            case 4:
               if (var15 == 0) {
                  this.a(var1, var8, var2 + var16, var3 + var12, var15, var17, var9);
               }

               int var31;
               if ((var31 = var13 + 2 & 7) < 4) {
                  int var34 = 0;
                  int var37 = 12 * this.g[var14--];
                  var34 = 12 * this.g[var14];
                  this.a(var1, var11, var2 - var34 - 8, var3 - var37 - 8, var31, var17);
               }

               if (var15 != 0) {
                  this.a(var1, var8, var2 + var16, var3 + var12, var15, var17, var9);
               }
               break;
            case 3:
               if (var15 == 0) {
                  this.a(var1, var8, var2 + var16, var3 + var12, var15, var17, var9);
               }

               int var18 = this.a[var7][var6 + 5] >>> 8;
               int var19 = this.a[var7][var6 + 5] & 255;
               var18 -= this.a[var7][var6 + 0] >>> 8;
               var19 -= this.a[var7][var6 + 0] & 255;
               int var20 = (var18 - var19) * 26;
               int var21 = (var18 + var19) * 12;
               if (var8 == 4) {
                  this.b(var1, var2, var3 - 4, var2 + var20, var3 + var21, var13, 1);
               } else {
                  this.b(var1, var2, var3 - 4, var2 + var20, var3 + var21, var13, 0);
               }

               if (var15 != 0) {
                  this.a(var1, var8, var2 + var16, var3 + var12, var15, var17, var9);
               }
         }

         short var32 = this.a[var7][var6 + 4];
         var2 += var16;
         int var25;
         var3 = (var25 = var3 + (var12 - 5)) - ((this.d[var8] >> 1) + 5);
         byte var36 = 0;
         switch (this.a[var7][var6 + 7] & 15) {
            case 0:
               var36 = -1;
               break;
            case 1:
               var36 = 16;
            case 2:
            case 3:
            default:
               break;
            case 4:
               var36 = 32;
         }

         if (var36 >= 0) {
            this.a(var1, 9, var2 - 20, var3 - 9, 0, var36, 16, 16, 0, 0);
            var2 += 9;
         }

         int var38 = var32 & 255;
         int var39 = 16777215;
         if (var38 < 255 || (var32 & '耀') != 0) {
            var2 -= 9;
            char var40;
            if ((var32 & '耀') != 0) {
               var1.setColor(0);
               var1.fillRect(var2, var3, 18, 4);
               if (this.aJ == -1) {
                  var39 = 0;
               }

               var1.setColor(var39);
               var1.fillRect(var2 - 1, var3 - 1, 18, 4);
               if (this.h == -1) {
                  var40 = 8703;
               } else {
                  var40 = '\uff00';
               }
            } else {
               var1.setColor(0);
               var1.fillRect(var2 - 1, var3 - 1, 18, 4);
               var40 = '\uff00';
            }

            int var22 = (var38 >> 4) + 1;
            var1.setColor(var40);
            var1.fillRect(var2, var3, var22, 2);
            var1.setColor(16711680);
            var1.fillRect(var2 + var22, var3, 16 - var22, 2);
         }
      }
   }

   final void a(Graphics var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      byte var9 = this.d[var2];
      int var8;
      var8 = (var8 = var2 << 3) + 10 + (var5 << 1);
      byte var10 = this.d[var8++];
      byte var11 = this.d[var8];
      this.a(var1, 22 + var2, var3, var4, var10, 0, var11, var9, var6, var7);
   }

   final void b(Graphics var1, int var2, int var3, int var4) {
      short var5;
      int var6 = ((var5 = this.a[var4]) & 255) << 2;
      int var7 = (var5 & 3072) >> 10;
      int var8 = this.b[var7][var6 + 2];
      int var9 = this.b[var7][var6 + 3];
      int var10 = this.a[var7][0];
      int var11 = var7;
      int var12;
      if (var9 >= 12) {
         int var13 = 0;
         var13 = (var13 = 0 + this.c[36]) + this.c[40] + this.c[43];
         var12 = this.h[((var9 & 0xFF) << 2) + var13] + 33;
      } else {
         var12 = this.h[((var9 & 0xFF) << 2) + var10] + 33;
      }

      if ((var8 & 1073741824) != 0) {
         var12 = 32;
         int var24;
         if ((var24 = var8 & 65535) >= 255) {
            this.b[var7][var6 + 2] = 255;
         } else {
            this.b[var7][var6 + 2] = var8 & -256 | var24;
         }
      }

      if ((var8 & 268435456) != 0) {
         this.b[var7][var6 + 2] = this.b[var7][var6 + 2] & -268435457;
         var11 = 3;
      }

      if ((var9 & -256) != 0 && var12 != 32) {
         int var25 = var9 >>> 24;
         short var14;
         int var15 = (var14 = this.b[var7][var25 + 2]) >>> 8;
         var14 &= 255;
         var15 -= this.b[var7][var6 + 0] >>> 8;
         var14 -= this.b[var7][var6 + 0] & 0xFF;
         int var16 = (var15 - var14) * 26;
         int var17 = (var15 + var14) * 12;
         int var18 = this.a[var7][47];
         if (var17 >= -10) {
            this.a(var1, var12, var2, var3, var11);
            this.b(var1, var2, var3 - 32, var2 + var16, var3 + var17, this.b[var7][var25 + 3], var18);
         } else {
            this.b(var1, var2, var3 - 32, var2 + var16, var3 + var17, this.b[var7][var25 + 3], var18);
            this.a(var1, var12, var2, var3, var11);
         }
      } else {
         if ((this.a[var4] & 16384) != 0) {
            var11 = 2;
         }

         this.a(var1, var12, var2, var3, var11);
      }

      if ((var8 & -65536) != 0 || (var8 & -2147483648) != 0) {
         int var26 = var12 << 1;
         var3 -= this.b[var26 + 1];
         var2 -= 16;
         var3 -= 5;
         var1.setColor(0);
         var1.fillRect(var2, var3, 34, 4);
         var1.setColor(14474460);
         var1.fillRect(var2 - 1, var3 - 1, 34, 4);
         int var29;
         if ((var8 & 0xFF0000) == 0) {
            var29 = (var8 & 0xFF) + 1;
            var1.setColor(65280);
         } else {
            var29 = ((var8 & 0xFF00) >> 8) + 1;
            if ((var8 & 536870912) == 0) {
               int var31 = var8 >> 16 & 0xFF;
               String var33 = "" + var31;
               var1.drawString(var33, var2 - this.a(var33) - 4, var3 - (this.ah >> 1), 20);
               var1.setColor(255);
            } else {
               var1.setColor(16711935);
            }
         }

         int var32 = var29 >> 3;
         var1.fillRect(var2, var3, var32, 2);
         if ((var8 & 0xFF0000) == 0) {
            var1.setColor(16711680);
         } else {
            var1.setColor(8421631);
         }

         var1.fillRect(var2 + var32, var3, 32 - var32, 2);
      }
   }

   final void a(Graphics var1, int var2, int var3, int var4) {
      int var5 = 0 + (this.a[var4] & 3);
      if ((this.a[var4] & 16384) != 0) {
         this.a(var1, var5, var2, var3, 2);
      } else {
         this.a(var1, var5, var2, var3, 0);
      }

      if ((this.a[var4] & 128) != 0) {
         var2 -= 8;
         var3 -= 4;
         var1.setColor(0);
         var1.fillRect(var2, var3, 18, 4);
         var1.setColor(14474460);
         var1.fillRect(var2 - 1, var3 - 1, 18, 4);
         int var6 = ((this.a[var4] & 124) >> 2) + 1 >> 1;
         var1.setColor(65280);
         var1.fillRect(var2, var3, var6, 2);
         var1.setColor(16711680);
         var1.fillRect(var2 + var6, var3, 16 - var6, 2);
      }
   }

   public final boolean a(int var1, int var2, int var3, int var4, boolean var5) {
      var3 &= 63;
      var4 &= 63;
      int var6 = var3 + (var4 << 6);
      if ((this.a[var6] & 4095) != 0) {
         return false;
      } else {
         if (var5) {
            this.c(var1, 0, var2);
         }

         this.e(var1, 2, var2);
         int var7;
         if ((var7 = this.a[var1][2]) >= 26) {
            return false;
         } else {
            this.a[var6] = (short)(this.a[var6] | (short)(512 | var7 | var1 << 10 & 3072));
            var7 <<= 3;
            short var8 = (short)(var3 << 8 | var4);
            this.a[var1][var7 + 0] = var8;
            this.a[var1][var7 + 2] = var8;
            this.a[var1][var7 + 1] = var8;
            this.a[var1][var7 + 3] = (short)var2;
            this.a[var1][var7 + 4] = 255;
            this.a[var1][var7 + 6] = 0;
            this.a[var1][var7 + 7] = 0;
            if ((this.h == var2 || this.h == -1) && this.Y == 0 && this.aJ == -1 && var1 == 0 && (this.h != -1 || var2 >= 2)) {
               this.a[var1][var7 + 4] = (short)(this.a[var1][var7 + 4] | '耀');
            }

            if (var1 == 0) {
               this.d(0, this.a[var1][2]);
            }

            this.a[var1][55] = this.a[var1][55] + this.a[var1][23 + var2] + this.a[var1][13 + var2];
            this.a[var1][2]++;
            this.a[var1][86]++;
            if (--var2 < 0) {
               var2 = 0;
            }

            this.a[var1][57 + var2]++;
            return true;
         }
      }
   }

   final int c(int var1, int var2) {
      byte var3 = 0;
      switch (var2) {
         case 0:
            var3 = 11;
         case 1:
         default:
            break;
         case 2:
         case 3:
            var3 = 10;
            break;
         case 4:
            var3 = 7;
            break;
         case 5:
         case 6:
            var3 = 8;
            break;
         case 7:
            var3 = 2;
            break;
         case 8:
            var3 = 6;
            break;
         case 9:
            var3 = 3;
      }

      byte var4 = 0;

      for (int var5 = 0; var5 < this.a[var1][4]; var5++) {
         if ((this.b[var1][var4 + 3] & 0xFF) == var3) {
            if ((this.b[var1][var4 + 2] & 1073741824) != 0) {
               return -1;
            }

            this.b[var1][var4 + 2] = this.b[var1][var4 + 2] + 65536;
            this.a[var1][49]++;
            int var6;
            if ((var6 = var2 - 1) < 0) {
               var6 = 0;
            }

            this.a[var1][66 + var6]++;
            return var4;
         }

         var4 += 4;
      }

      return -1;
   }

   final void e(int var1, int var2) {
      int var3 = var2 - 1;
      int var4 = var1 - 1;
      if (var3 < 0) {
         var3 = 0;
      }

      if (var4 < 0) {
         var4 = 0;
      }

      for (int var5 = 0; var5 < 2; var5++) {
         byte var6 = 0;
         int var7 = this.a[var5][2];

         for (int var8 = 0; var8 < var7; var8++) {
            if ((this.a[var5][var6 + 3] & 255) == var1) {
               this.a[var5][var6 + 3] = (short)(this.a[var5][var6 + 3] & '\uff00');
               this.a[var5][var6 + 3] = (short)(this.a[var5][var6 + 3] | var2);
            }

            var6 += 8;
         }

         this.a[var5][57 + var3] = this.a[var5][57 + var4];
         this.a[var5][57 + var4] = 0;
         this.a[var5][66 + var3] = this.a[var5][66 + var4];
         this.a[var5][66 + var4] = 0;
      }
   }

   final void f(int var1, int var2) {
      short var3;
      if (((var3 = this.a[var1][var2 + 6]) & 3840) != 0) {
         short var4 = 0;
         switch (this.a[var1][var2 + 3] & 0xFF) {
            case 0:
            case 1:
               var4 = 512;
               break;
            case 2:
               var4 = 768;
               break;
            case 3:
               var4 = 1024;
               break;
            case 4:
               var4 = 768;
               break;
            case 5:
               var4 = 1024;
               break;
            case 6:
               var4 = 1024;
               break;
            case 7:
               var4 = 256;
               break;
            case 8:
               var4 = 256;
               break;
            case 9:
               var4 = 1536;
               break;
            default:
               return;
         }

         this.a[var1][var2 + 6] = (short)(var3 - var4 & 3840 | var3 & 255);
         if (var3 < this.a[var1][var2 + 6]) {
            this.a[var1][var2 + 6] = (short)(this.a[var1][var2 + 6] & 255);
            return;
         }
      } else if (this.a[var1][var2 + 0] != this.a[var1][var2 + 2]) {
         var2 >>= 3;
         if (var1 == 0) {
            this.l(0, var2);
            if (this.b(0, var2)) {
               this.a(0, var2);
            }

            this.d(0, var2);
            return;
         }

         if (this.b(var1, var2)) {
            this.a(var1, var2);
         }
      }
   }

   public final void g(int var1, int var2) {
      this.l(0, var2);
      this.a[var1][2]--;
      int var3 = this.a[var1][2] << 3;
      int var4 = var2 << 3;
      if (var1 == 0 && var3 == 0 && this.a[0][4] == 0) {
         this.g(0, 98, 1);
      }

      short var5;
      int var6 = ((var5 = this.a[var1][var4 + 0]) >>> 8 & 63) + ((var5 & 63) << 6);
      this.a[var6] = 16384;
      int var7 = this.a[var1][var4 + 3] & 255;
      this.e(var1, 0, var7);
      this.a[var1][55] = this.a[var1][55] - (this.a[var1][23 + var7] + this.a[var1][13 + var7]);
      if (--var7 < 0) {
         var7 = 0;
      }

      this.a[var1][57 + var7]--;
      if (var4 < var3) {
         var6 = ((var5 = this.a[var1][var3 + 0]) >>> 8 & 63) + ((var5 & 63) << 6);
         this.a[var6] = (short)(this.a[var6] & -256);
         this.a[var6] = (short)(this.a[var6] | var2);

         for (int var8 = 0; var8 < 8; var8++) {
            this.a[var1][var4++] = this.a[var1][var3];
            this.a[var1][var3++] = 0;
         }
      }

      this.a[var1][87]++;
      if (var1 == 0) {
         this.l = true;
      }
   }

   final void g() {
      int var2 = this.ar & 8;

      for (int var3 = 0; var3 < 2; var3++) {
         byte var1 = 0;

         for (int var4 = 0; var4 < this.a[var3][2]; var1 += 8) {
            switch (this.a[var3][var1 + 7] & 15) {
               case 0:
                  if (var2 != 0 && this.a[var3][var1 + 0] == this.a[var3][var1 + 2] && (this.a[var3][var1 + 4] & 255) < 255) {
                     this.a[var3][var1 + 4]++;
                  }
               case 3:
                  this.f(var3, var1);
                  break;
               case 1:
                  int var14 = (this.a[var3][var1 + 7] & 32512) + 256;
                  int var16 = (this.a[var3][var1 + 5] >>> 8) + ((this.a[var3][var1 + 5] & 255) << 6);
                  int var18;
                  if ((var18 = (this.a[var16] & 3072) >> 10) != var3 && (this.a[var16] & 4095) != 0) {
                     boolean var19 = false;
                     int var21;
                     if ((var21 = this.a[var3][var1 + 3] & 255) != 4 && var21 != 8) {
                        if (var14 >= 2048) {
                           var19 = true;
                        }
                     } else if (var14 >= 3840) {
                        var19 = true;
                     }

                     if (var19) {
                        this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] & 255);
                        this.d(var3, var1, var16, var18);
                     } else {
                        this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] & 255);
                        this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] + (var14 & 32512));
                     }
                  } else {
                     this.a[var3][var1 + 7] = 0;
                  }
                  break;
               case 2:
                  int var12 = this.a[var3][var1 + 7] >>> 8;
                  if (--var12 == 0) {
                     this.a[var3][var1 + 2] = (short)this.a(var3, this.a[var3][var1 + 0], this.a[var3][var1 + 7]);
                     this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] & 240);
                     this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] | 3);
                     int var7 = this.a[var3][var1 + 5] >>> 8 | (this.a[var3][var1 + 5] & 255) << 6;
                     int var8 = (this.a[var7] & 124) >> 2;
                     if (--var8 != 0 && (this.a[var7] & 768) == 768) {
                        this.a[var7] = (short)(this.a[var7] & 'ﾃ');
                        this.a[var7] = (short)(this.a[var7] | var8 << 2);
                        this.a[var7] = (short)(this.a[var7] | 128);
                        break;
                     }

                     if ((this.a[var7] & 768) == 768) {
                        this.a[var7] = (short)(this.a[var7] & '\uf000');
                     }

                     for (int var9 = 0; var9 < 8; var9++) {
                        int var10 = this.a[var3][var1 + 5] >>> 8;
                        int var11 = this.a[var3][var1 + 5] & 255;
                        var10 += this.g[var9 << 1];
                        var11 += this.g[(var9 << 1) + 1];
                        if (((var10 | var11) & -64) == 0) {
                           var7 = var10 + (var11 << 6);
                           if ((this.a[var7] & 768) == 768 && (this.a[var3][var1 + 7] & 240) >> 4 == (this.a[var7] & 3)) {
                              this.a[var3][var1 + 5] = (short)(var10 << 8 | var11);
                              var9 = 10;
                           }
                        }
                     }
                     break;
                  }

                  this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] & 255);
                  this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] | (short)(var12 << 8));
                  break;
               case 4:
                  int var6 = (this.a[var3][var1 + 7] & 32512) + 256;
                  this.c(var3, var1);
                  this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] & 255);
                  this.a[var3][var1 + 7] = (short)(this.a[var3][var1 + 7] | (short)var6);
            }

            var4++;
         }
      }
   }

   final void c(int var1, int var2, int var3, int var4) {
      int var5 = 0;
      int var6 = var3 + (var4 << 6);
      switch (this.a[var6] & 768) {
         case 256:
            int var15;
            if ((var15 = (this.a[var6] & 3072) >> 10) == var1) {
               if ((this.a[var1][var2 + 3] & 255) <= 2) {
                  if ((this.a[var1][var2 + 7] & 15) == 3) {
                     int var17 = ((this.a[var1][var2 + 7] & 240) >> 4) - 1;
                     var5 = 0;
                     this.a[var1][var2 + 7] = 0;
                     int var19 = 256;
                     if (var1 == 1) {
                        var19 = this.aM;
                     }

                     this.a[var1][5 + var17] = this.a[var1][5 + var17] + (this.a[var1][50 + var17] * var19 >> 8);
                     this.a[var1][90] = this.a[var1][90] + (var19 >> 8);
                     this.l = true;
                     if (var1 == 0) {
                        this.a[var1][var2 + 2] = this.a[var1][var2 + 5];
                        this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
                     } else {
                        short var11;
                        int var12 = (var11 = this.a[var1][var2 + 5]) >>> 8;
                        var11 &= 255;
                        int var13 = var12 + (var11 << 6);
                        int var14;
                        if ((var14 = this.b(var1)) == (this.a[var13] & 771)) {
                           this.a[var1][var2 + 2] = this.a[var1][var2 + 5];
                           this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
                        } else {
                           this.a(this.a[var1][var2 + 0], var14);
                        }
                     }
                  } else {
                     int var18 = (this.a[var6] & 255) << 2;
                     if ((this.b[var1][var18 + 2] & 0xFF) < 255) {
                        var5 = 4;
                        this.a[var1][var2 + 7] = 4;
                        this.a[var1][var2 + 5] = (short)(var3 << 8 | var4);
                     }
                  }
               }
            } else {
               var5 = 1;
               this.a[var1][var2 + 7] = 1;
               int var16 = (this.a[var6] & 255) << 2;
               this.a[var1][var2 + 5] = (short)(var3 << 8 | var4);
               this.b[var15][var16 + 2] = this.b[var15][var16 + 2] | -2147483648;
            }
            break;
         case 512:
            int var7;
            if ((var7 = (this.a[var6] & 3072) >> 10) == var1) {
               var5 = 0;
               this.a[var1][var2 + 7] = 0;
            } else {
               var5 = 1;
               this.a[var1][var2 + 7] = 1;
               int var8 = (this.a[var6] & 255) << 3;
               this.a[var1][var2 + 5] = (short)(var3 << 8 | var4);
               int var9 = this.a[var1][var2 + 0] >>> 8;
               int var10 = this.a[var1][var2 + 0] & 255;
               this.a[var7][var8 + 5] = (short)(var9 << 8 | var10);
               this.a[var7][var8 + 7] = (short)(this.a[var7][var8 + 7] & '\ufff0');
               this.a[var7][var8 + 7] = (short)(this.a[var7][var8 + 7] | 1);
               this.a[var7][var8 + 6] = (short)((this.a[var1][var2 + 6] & 255) + 4 & 7);
               this.b(var7, var8, var9, var10);
            }
            break;
         case 768:
            if ((this.a[var6] & 3) != 0 && this.a[var1][var2 + 3] <= 1) {
               var5 = 26114 | (this.a[var6] & 3) << 4;
               this.a[var1][var2 + 7] = (short)var5;
               this.a[var1][var2 + 5] = (short)(var3 << 8 | var4);
               this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
            }
      }

      this.a[var1][var2 + 7] = (short)var5;
   }

   final void d(int var1, int var2, int var3, int var4) {
      int var7 = this.a[var1][var2 + 3] & 255;
      switch (this.a[var3] & 768) {
         case 256:
            int var14 = (this.a[var3] & 255) << 2;
            int var12 = this.b[var4][var14 + 2] & 0xFF;
            int var15 = this.b[var4][var14 + 3] & 0xFF;
            int var16 = (this.a[var1][13 + var7] << 4) / this.a[var4][33 + var15];
            if ((var12 = var12 - var16) > 0) {
               this.b[var4][var14 + 2] = this.b[var4][var14 + 2] & -256;
               this.b[var4][var14 + 2] = this.b[var4][var14 + 2] | var12 | -2147483648 | 268435456;
            } else {
               this.i(var4, this.a[var3] & 255);
               this.a[var1][var2 + 7] = 0;
            }

            if (var4 == 0) {
               this.v();
               return;
            }
            break;
         case 512:
            int var6 = (this.a[var3] & 255) << 3;
            int var5 = this.a[var4][var6 + 4] & 255;
            int var9 = this.a[var4][var6 + 3] & 255;
            int var10 = (this.a[var1][13 + var7] << 4) / this.a[var4][23 + var9];
            if ((var5 = var5 - var10) > 0) {
               this.a[var4][var6 + 4] = (short)(this.a[var4][var6 + 4] & '\uff00');
               this.a[var4][var6 + 4] = (short)(this.a[var4][var6 + 4] | var5 | 4096);
               this.a[var4][var6 + 2] = this.a[var1][var2 + 0];
               return;
            }

            this.g(var4, this.a[var3] & 255);
            this.a[var1][var2 + 7] = 0;
      }
   }

   final void c(int var1, int var2) {
      int var3 = (this.a[var1][var2 + 5] >>> 8) + ((this.a[var1][var2 + 5] & 255) << 6);
      int var4 = (this.a[var3] & 255) << 2;
      int var5 = this.b[var1][var4 + 2] & 0xFF;
      if ((a() & 1) == 0) {
         var5++;
      }

      if (var5 <= 255) {
         this.b[var1][var4 + 2] = this.b[var1][var4 + 2] & -256;
         this.b[var1][var4 + 2] = this.b[var1][var4 + 2] | var5 | -2147483648;
      } else {
         this.a[var1][var2 + 7] = 0;
      }
   }

   public final int a(int var1, int var2, int var3, int var4, int var5, boolean var6) {
      int var7;
      if ((var7 = this.a[var1][4]) >= 22) {
         return -1;
      } else {
         this.a[var1][88]++;
         this.e(var1, 3, var2);
         var3 &= 63;
         var4 &= 63;
         int var8 = var3 + (var4 << 6);
         this.a[var8] = (short)(this.a[var8] | (short)(256 | var1 << 10 & 3072 | var7 & 0xFF));
         var7 <<= 2;
         if (var1 == 0 && (var5 & 1073741824) == 0) {
            this.a(var3, var4, 3);
         }

         switch (var2) {
            case 0:
               this.a[var1][9] = var3 << 8 | var4;
               break;
            case 9:
               this.a[var1][8] = var3 << 8 | var4;
               if ((var5 & 1073741824) == 0) {
                  this.a[var1][3] = this.a[var1][3] + 5;
               }
               break;
            case 11:
               if ((var5 & 1073741824) == 0) {
                  this.a[var1][3] = this.a[var1][3] + 5;
               }
               break;
            case 12:
               if ((var5 & 1073741824) == 0) {
                  int var9 = this.a[var1][48] << 2;
                  this.b[var1][var9 + 0] = (short)var7;
                  this.b[var1][var9 + 1] = 1000;
                  this.b[var1][var9 + 2] = (short)((var3 << 8) + var4);
                  this.b[var1][var9 + 3] = 0;
                  var2 |= var9 << 24;
                  this.a[var1][48]++;
               }
         }

         this.b[var1][var7 + 0] = (var3 << 8) + var4;
         this.b[var1][var7 + 1] = var2;
         this.b[var1][var7 + 2] = var5;
         this.b[var1][var7 + 3] = var2;
         var2 &= 255;
         this.a[var1][4]++;
         if (var6) {
            int var14 = 0;
            if (var2 == 12) {
               var14 = (var14 = 0 + this.c[36]) + this.c[40] + this.c[43];
            }

            this.c(var1, 1, var2 + var14);
         }

         if (var2 != 1 && var2 != 11 && var2 != 12 && var1 == 0) {
            this.c[10 + var2] = 0;
         }

         return var7;
      }
   }

   final int a(int var1, int var2, boolean var3) {
      int var4 = 0;
      byte var5 = 0;
      int var6 = this.a[var1][4];

      for (int var7 = 0; var7 < var6; var5 += 4) {
         if ((this.b[var1][var5 + 3] & 0xFF) == var2) {
            if (var3) {
               if ((this.b[var1][var5 + 2] & 1073741824) == 0) {
                  var4++;
               }
            } else {
               var4++;
            }
         }

         var7++;
      }

      return var4;
   }

   public final void i(int var1, int var2) {
      int var3 = this.a[var1][4] - 1;
      this.a[var1][89]++;
      int var4 = var2 << 2;
      int var5 = var3 << 2;
      int var6;
      int var7 = ((var6 = this.b[var1][var4 + 0]) >>> 8 & 63) + ((var6 & 63) << 6);
      this.a[var7] = 0;
      int var8 = this.b[var1][var4 + 3] & 0xFF;
      if (var1 == 0) {
         this.c[10 + var8] = 1;
      }

      this.e(var1, 1, var8);
      switch (var8) {
         case 0:
            this.a[var1][9] = -1;
            break;
         case 1:
            int var25;
            if ((var25 = this.b[var1][var4 + 0]) == this.a[var1][10]) {
               this.a[var1][10] = -1;
            } else if (var25 == this.a[var1][11]) {
               this.a[var1][11] = -1;
            }
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 10:
         default:
            break;
         case 9:
            if (var1 == 0) {
               this.g(0, 98, 1);
            } else if (this.ac != 32 || this.aC == 0) {
               this.g(0, 98, 0);
            }
            break;
         case 11:
            if ((this.b[var1][var4 + 2] & 1073741824) == 0) {
               this.a[var1][3] = this.a[var1][3] - 5;
            }

            this.l = true;
            break;
         case 12:
            if ((this.b[var1][var4 + 2] & 1073741824) == 0) {
               int var9 = this.a[var1][48];
               int var10 = this.b[var1][var4 + 3] >>> 24 & 0xFF;
               int var11;
               if ((var11 = var9 - 1 << 2) != var10) {
                  for (int var12 = 0; var12 < 4; var12++) {
                     this.b[var1][var10 + var12] = this.b[var1][var11 + var12];
                  }

                  short var26 = this.b[var1][var11 + 0];
                  int var13 = this.b[var1][var10 + 2] & '\uffff';
                  this.b[var1][var26 + 3] = var10 << 24 | var13 << 8 | 12;
               }

               this.a[var1][48]--;
            }
      }

      if (var4 != var5) {
         var6 = this.b[var1][var5];
         var5++;
         var7 = (var6 >>> 8 & 63) + ((var6 & 63) << 6);
         this.a[var7] = (short)(this.a[var7] & -256);
         this.a[var7] = (short)(this.a[var7] | var2);
         this.b[var1][var4] = var6;
         var4++;
         this.b[var1][var4] = this.b[var1][var5];
         this.b[var1][var5] = 0;
         var4++;
         this.b[var1][var4] = this.b[var1][++var5];
         this.b[var1][var5] = 0;
         var4++;
         this.b[var1][var4] = this.b[var1][++var5];
         this.b[var1][var5] = 0;
      } else {
         this.b[var1][var4++] = 0;
         this.b[var1][var4++] = 0;
         this.b[var1][var4++] = 0;
         this.b[var1][var4] = 0;
      }

      this.a[var1][4]--;
   }

   final boolean b(int var1, int var2) {
      var2 <<= 3;
      int var3 = this.a[var1][var2 + 0];
      short var4;
      if ((var4 = this.a[var1][var2 + 2]) == var3) {
         return false;
      } else {
         short var5 = this.a[var1][var2 + 1];
         int var6 = var3 & 0xFF;
         var3 >>>= 8;
         short var7 = (short)(this.a[var3 + (var6 << 6)] & 4095);
         int var8 = var3;
         int var9 = var6;
         int var10 = var4 & 255;
         int var25;
         int var11 = (var25 = var4 >>> 8) - var3;
         byte var12 = 1;
         if (var11 == 0) {
            var12 = 0;
         } else if (var11 < 0) {
            var12 = -1;
            var11 = -var11;
         }

         int var13 = var10 - var6;
         byte var14 = 1;
         if (var13 == 0) {
            var14 = 0;
         } else if (var13 < 0) {
            var14 = -1;
            var13 = -var13;
         }

         int var15 = this.a[var1][var2 + 3] >> 8;
         if (var11 > var13) {
            if ((var15 = var15 + var13) << 1 >= var11) {
               var6 += var14;
               var15 -= var11;
            }

            var3 += var12;
         } else {
            if ((var15 = var15 + var11) << 1 >= var13) {
               var3 += var12;
               var15 -= var13;
            }

            var6 += var14;
         }

         if ((var3 & 0xFF) > 63) {
            var8 = var3;
         }

         if ((var6 & 0xFF) > 63) {
            var9 = var6;
         }

         this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
         this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] | (short)var15 << 8);
         int var16 = (var3 << 8) + var6;
         if ((this.a[var3 + (var6 << 6)] & 4095) != 0 || var16 == var5) {
            this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
            if (var16 == this.a[var1][var2 + 2]) {
               short var28 = this.a[var1][var2 + 0];
               this.a[var1][var2 + 1] = var28;
               this.a[var1][var2 + 0] = (short)var16;
               this.a(var1, var2 >> 3);
               this.a[var1][var2 + 6] = (short)(this.a[var1][var2 + 6] & 255);
               this.a[var1][var2 + 2] = var28;
               this.a[var1][var2 + 0] = var28;
               this.c(var1, var2, var3, var6);
               return false;
            }

            int var18 = ((this.a[var1][var2 + 6] & 255) << 3) + 16;
            var3 = var8;
            var6 = var9;

            int var17;
            for (var17 = 0; var17 < 7; var17++) {
               byte var19 = this.g[var18++];
               int var20 = var3 + this.g[var19];
               int var21 = var6 + this.g[var19 + 1];
               if (((var20 | var21) & -64) == 0 && (this.a[var20 + (var21 << 6)] & 4095) == 0 && (var20 << 8 | var21) != var5) {
                  var3 = var20;
                  var6 = var21;
                  this.a[var1][var2 + 3] = (short)(this.a[var1][var2 + 3] & 255);
                  var17 = 10;
               }
            }

            if (var17 == 7) {
               this.a[var1][var2 + 6] = (short)(this.a[var1][var2 + 6] & 255);
               return false;
            }
         }

         var3 &= 63;
         var6 &= 63;
         this.a[var8 + (var9 << 6)] = (short)(this.a[var8 + (var9 << 6)] & -4096);
         this.a[var3 + (var6 << 6)] = (short)(this.a[var3 + (var6 << 6)] & -4096 | var7);
         this.a[var1][var2 + 1] = this.a[var1][var2 + 0];
         this.a[var1][var2 + 0] = (short)(var3 << 8 | var6);
         return true;
      }
   }

   final void a(int var1, int var2) {
      int var3 = var2 << 3;
      int var4 = (this.a[var1][var3 + 0] & '\uff00') >>> 8;
      int var5 = this.a[var1][var3 + 0] & 255;
      int var6 = (this.a[var1][var3 + 1] & '\uff00') >>> 8;
      int var7 = this.a[var1][var3 + 1] & 255;
      int var8 = var6 - var4;
      int var9 = var7 - var5;
      int var10 = var8 << 8 & 0xFF00 | var9 & 0xFF;
      byte var11 = 0;
      switch (var10) {
         case 1:
            var11 = 2;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 8192);
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 16384);
            break;
         case 255:
            var11 = 6;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '\udfff');
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '뿿');
            break;
         case 256:
            var11 = 0;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 8192);
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '뿿');
            break;
         case 257:
            var11 = 1;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 8192);
            break;
         case 511:
            var11 = 7;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '뿿');
            break;
         case 65280:
            var11 = 4;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '\udfff');
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 16384);
            break;
         case 65281:
            var11 = 3;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] | 16384);
            break;
         case 65535:
            var11 = 5;
            this.a[var1][var3 + 4] = (short)(this.a[var1][var3 + 4] & '\udfff');
      }

      this.a[var1][var3 + 6] = (short)(var11 | 3840);
   }

   final void b(int var1, int var2, int var3, int var4) {
      int var5 = (this.a[var1][var2 + 0] & '\uff00') >>> 8;
      int var6 = this.a[var1][var2 + 0] & 255;
      var5 = (var3 - var5) * 181;
      var6 = (var4 - var6) * 181;
      int var7 = var5 - var6;
      int var8 = var6 + var5;
      if (var7 > 0) {
         this.a[var1][var2 + 4] = (short)(this.a[var1][var2 + 4] | 16384);
      } else if (var7 < 0) {
         this.a[var1][var2 + 4] = (short)(this.a[var1][var2 + 4] & '뿿');
      }

      if (var8 < 0) {
         this.a[var1][var2 + 4] = (short)(this.a[var1][var2 + 4] | 8192);
      } else {
         if (var8 > 0) {
            this.a[var1][var2 + 4] = (short)(this.a[var1][var2 + 4] & '\udfff');
         }
      }
   }

   final void G() {
      for (int var4 = 0; var4 < 2; var4++) {
         int var5;
         if ((var5 = this.a[var4][48]) != 0) {
            int var6 = this.ar % var5 << 2;
            int var7 = var5;
            int var8 = var5 << 2;

            while (this.b[var4][var6 + 1] != 1000) {
               var6 += 4;
               if (var6 >= var8) {
                  var6 = 0;
               }

               if (--var7 <= 0) {
                  break;
               }
            }

            if (var7 > 0) {
               int var9 = this.a[var4][12];
               short var10 = this.b[var4][var6 + 0];
               int var11;
               int var12 = (var11 = this.b[var4][var10 + 0]) >>> 8 & 63;
               var11 &= 63;
               int var2 = var4 ^ 1;
               byte var3 = 0;
               int var1 = this.a[var2][2];

               for (int var13 = 0; var13 < var1; var3 += 8) {
                  short var14;
                  int var15 = ((var14 = this.a[var2][var3 + 0]) >>> 8 & 63) - var12;
                  var14 = (var14 & 63) - var11;
                  if (var15 * var15 + var14 * var14 <= var9) {
                     this.b[var4][var6 + 3] = 0;
                     this.b[var4][var6 + 1] = (short)var3;
                     this.b[var4][var6 + 2] = this.a[var2][var3 + 0];
                     this.b[var4][var10 + 3] = this.b[var4][var10 + 3] & -16776961;
                     this.b[var4][var10 + 3] = this.b[var4][var10 + 3] | (this.b[var4][var6 + 2] & '\uffff') << 8;
                     var13 = 1000;
                  }

                  var13++;
               }
            }
         }
      }
   }

   final void J() {
      for (int var1 = 0; var1 < 2; var1++) {
         byte var2 = 0;
         int var3 = this.a[var1][48];

         for (int var4 = 0; var4 < var3; var2 += 4) {
            short var5;
            if ((var5 = this.b[var1][var2 + 1]) != 1000) {
               int var6 = this.b[var1][var2 + 3] + 1;
               this.b[var1][var2 + 3] = (short)(var6 & 15);
               if (var6 >= 16) {
                  short var7;
                  int var8 = (var7 = this.b[var1][var2 + 2]) >>> 8 & 63;
                  var7 &= 63;
                  int var9 = var8 + (var7 << 6);
                  int var10;
                  if ((var10 = (this.a[var9] & 3072) >> 10) == var1 || (this.a[var9] & 4095) == 0) {
                     this.b[var1][var2 + 1] = 1000;
                     this.b[var1][this.b[var1][var2 + 0] + 3] = this.b[var1][this.b[var1][var2 + 0] + 3] & -16776961;
                  } else if ((this.a[var9] & 768) == 512) {
                     int var11 = this.a[var10][var5 + 4] & 255;
                     int var12 = (this.a[var1][46] << 4) / this.a[var10][23 + (this.a[var10][var5 + 3] & 255)];
                     if ((var11 = var11 - var12) <= 0) {
                        this.g(var10, this.a[var9] & 255);
                        this.b[var1][var2 + 1] = 1000;
                        this.b[var1][this.b[var1][var2 + 0] + 3] = this.b[var1][this.b[var1][var2 + 0] + 3] & -16776961;
                     } else {
                        this.a[var10][var5 + 4] = (short)(this.a[var10][var5 + 4] & '\uff00');
                        this.a[var10][var5 + 4] = (short)(this.a[var10][var5 + 4] | var11 | 4096);
                        if (var10 == 0) {
                           if (this.a[var10][var5 + 2] == this.a[var10][var5 + 0] && (this.a[var10][var5 + 7] & 255) != 1) {
                              this.a[var10][var5 + 2] = (short)this.b[var1][this.b[var1][var2 + 0] + 0];
                           }
                        } else if ((this.a[var10][var5 + 7] & 255) != 1) {
                           this.a[var10][var5 + 2] = (short)this.b[var1][this.b[var1][var2 + 0] + 0];
                        }
                     }
                  } else {
                     this.b[var1][var2 + 1] = 1000;
                     this.b[var1][this.b[var1][var2 + 0] + 3] = this.b[var1][this.b[var1][var2 + 0] + 3] & -16776961;
                  }
               }
            }

            var4++;
         }
      }
   }

   final void B() {
      for (int var1 = 0; var1 < 2; var1++) {
         boolean var2 = false;
         int var3 = this.ar;
         int var4;
         if ((var4 = this.a[var1][2]) != 0) {
            int var5 = 0;

            while (var5 < var4) {
               var5++;
               int var6 = var3++ % var4 << 3;
               if ((this.a[var1][var6 + 7] & 15) != 1
                  && (this.a[var1][var6 + 6] & '\uff00') == 0
                  && (this.a[var1][var6 + 3] & 255) >= 2
                  && (var1 != 0 || this.a[var1][var6 + 0] == this.a[var1][var6 + 2])) {
                  if (!this.a(var1, var6)) {
                     this.b(var1, var6);
                  }

                  var2 = true;
               }

               if (var2) {
                  break;
               }
            }
         }
      }
   }

   final boolean a(int var1, int var2) {
      int var3 = this.a[var1][var2 + 3] & 255;
      short var4;
      int var5 = (var4 = this.a[var1][var2 + 0]) >>> 8;
      var4 &= 255;
      byte var6 = 0;
      switch (var3) {
         case 2:
            var6 = 9;
            break;
         case 3:
            var6 = 9;
            break;
         case 4:
            var6 = 16;
            break;
         case 5:
            var6 = 16;
            break;
         case 6:
            var6 = 9;
            break;
         case 7:
            var6 = 9;
            break;
         case 8:
            var6 = 16;
            break;
         case 9:
            var6 = 16;
      }

      int var7 = var1 ^ 1;
      byte var8 = 0;
      int var9 = this.a[var7][2];

      for (int var10 = 0; var10 < var9; var8 += 8) {
         short var11;
         int var12 = ((var11 = this.a[var7][var8 + 0]) >>> 8) - var5;
         var11 = (var11 & 0xFF) - var4;
         int var13;
         if ((var13 = var12 * var12 + var11 * var11) <= 1) {
            this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] & '\ufff0');
            this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] | 1);
            this.a[var1][var2 + 5] = this.a[var7][var8 + 0];
            this.a[var1][var2 + 2] = this.a[var1][var2 + 0];
            this.b(var1, var2, this.a[var7][var8 + 0] >>> 8, this.a[var7][var8 + 0] & 255);
            if ((this.a[var7][var8 + 7] & 15) == 1) {
               return true;
            }

            this.a[var7][var8 + 7] = (short)(this.a[var7][var8 + 7] & '\ufff0');
            this.a[var7][var8 + 7] = (short)(this.a[var7][var8 + 7] | 1);
            this.a[var7][var8 + 5] = this.a[var1][var2 + 0];
            this.a[var7][var8 + 2] = this.a[var7][var8 + 0];
            this.b(var7, var8, var5, var4);
            return true;
         }

         if (var13 <= var6) {
            switch (var3) {
               case 4:
               case 8:
                  this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] & '\ufff0');
                  this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] | 1);
                  this.a[var1][var2 + 5] = this.a[var7][var8 + 0];
                  this.a[var1][var2 + 2] = this.a[var1][var2 + 0];
                  this.b(var1, var2, this.a[var7][var8 + 0] >>> 8, this.a[var7][var8 + 0] & 255);
                  if ((this.a[var7][var8 + 7] & 15) == 1) {
                     return true;
                  }

                  this.a[var7][var8 + 2] = this.a[var1][var2 + 0];
                  this.b(var7, var8, var5, var4);
                  return true;
               default:
                  this.a[var1][var2 + 2] = this.a[var7][var8 + 0];
                  return true;
            }
         }

         var10++;
      }

      return false;
   }

   final void b(int var1, int var2) {
      short var3;
      int var4 = (var3 = this.a[var1][var2 + 0]) >>> 8 & 63;
      var3 &= 63;
      int var5 = this.a[var1][var2 + 3] & 255;
      byte var6 = 0;
      switch (var5) {
         case 2:
            var6 = 9;
            break;
         case 3:
            var6 = 9;
            break;
         case 4:
            var6 = 16;
            break;
         case 5:
            var6 = 16;
            break;
         case 6:
            var6 = 9;
            break;
         case 7:
            var6 = 9;
            break;
         case 8:
            var6 = 16;
            break;
         case 9:
            var6 = 16;
      }

      int var7 = Integer.MAX_VALUE;
      byte var8 = -1;
      int var9 = var1 ^ 1;
      byte var10 = 0;
      int var11 = this.a[var9][4];

      for (int var12 = 0; var12 < var11; var10 += 4) {
         int var13;
         int var14 = (var13 = this.b[var9][var10 + 0]) >>> 8 & 63;
         var13 &= 63;
         var14 -= var4;
         var13 -= var3;
         int var15;
         if ((var15 = var14 * var14 + var13 * var13) > 0 && var15 <= var6) {
            if ((this.b[var9][var10 + 3] & 0xFF) == 12) {
               var8 = var10;
               break;
            }

            if (var15 < var7) {
               var7 = var15;
               var8 = var10;
            }
         }

         var12++;
      }

      if (var8 >= 0) {
         switch (var5) {
            case 4:
            case 8:
               this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] & '\ufff0');
               this.a[var1][var2 + 7] = (short)(this.a[var1][var2 + 7] | 1);
               this.a[var1][var2 + 5] = (short)this.b[var9][var8 + 0];
               this.b(var1, var2, this.b[var9][var8 + 0] >>> 8, this.b[var9][var8 + 0] & 0xFF);
               this.a[var1][var2 + 2] = this.a[var1][var2 + 0];
               return;
            default:
               this.a[var1][var2 + 2] = (short)this.b[var9][var8 + 0];
         }
      }
   }

   final void z() {
      byte var1 = 0;
      int var2;
      if ((var2 = this.a[0][2]) != 0) {
         int var3 = this.a[1][54];
         int var4 = this.a[1][55];
         int var5 = this.a[0][55];
         int var6 = (var3 & 2130706432) >> 24;
         var3 &= 16777215;
         if (var6 >= var2) {
            this.i = 0;
            if (var5 < var4 + (var4 >> 2) && var4 >= this.q) {
               this.i = 2;
            } else if (var3 <= this.l) {
               this.i = 1;
            }

            this.a[1][54] = 16777215;
         } else {
            int var7;
            if ((var7 = this.a[1][8]) >= 0) {
               int var8 = var7 >>> 8;
               var7 &= 255;
               short var9;
               int var10 = (var9 = this.a[0][(++var6 << 3) + 0]) >>> 8;
               var9 &= 255;
               var8 -= var10;
               var7 -= var9;
               int var11;
               if ((var11 = var8 * var8 + var7 * var7) < var3) {
                  var3 = var11;
                  this.a[1][53] = this.a[0][(var6 << 3) + 0];
               }

               this.a[1][54] = var3 & 16777215 | var6 << 24;
            }
         }
      }

      if (this.i) {
         if (this.w++ >= this.aw) {
            this.a[1][5] = this.a[1][5] + this.a[1][57];
            this.a[1][6] = this.a[1][6] + this.a[1][57];
            this.a[1][7] = this.a[1][7] + this.a[1][57];
            this.w = 0;
         }

         if (this.E++ >= this.an) {
            this.a();
            if (this.b()) {
               this.E = 0;
            }
         }

         if (this.aq++ >= this.C && this.b(this.ar % 10)) {
            this.aq = 0;
         }
      }

      var1 = 0;
      if ((var2 = this.a[1][2]) != 0) {
         do {
            if ((this.a[1][var1 + 3] & 255) < 2
               && (this.a[1][var1 + 7] & 255) == 0
               && (this.a[1][var1 + 2] == this.a[1][var1 + 0] || this.a[1][var1 + 1] == this.a[1][var1 + 0])) {
               this.a[1][var1 + 2] = this.a(this.a[1][var1 + 0], this.b(1));
            }

            var1 += 8;
         } while (--var2 > 0);

         if (this.i) {
            int var17 = var2;
            int var18 = 0;
            var2 = this.a[1][2];
            short var19 = -1;
            short var21 = -1;
            switch (this.i) {
               case 1:
                  if (this.a[1][8] != -1) {
                     var19 = (short)(this.a[0][8] + (this.ar & 1) + ((this.ar & 3) - 2 << 8));
                     var21 = (short)this.a[1][53];
                     var17 = var2 >> 3;
                     var18 = var2 - var17;
                  }
                  break;
               case 2:
                  if (this.a[0][8] != -1) {
                     var19 = (short)(this.a[0][8] + (this.ar & 1) + ((this.ar & 3) - 2 << 8));
                     var17 = var2 >> 2;
                     var18 = var2 - var17;
                  }
            }

            if (this.i != 0) {
               var1 = 0;
               if (var19 >= 0) {
                  for (int var24 = 0; var24 < var18; var1 += 8) {
                     if ((this.a[1][var1 + 3] & 255) >= 2 && (this.a[1][var1 + 7] & 15) != 1) {
                        this.a[1][var1 + 2] = var19;
                     }

                     var24++;
                  }
               }

               if (var21 >= 0) {
                  for (int var25 = var18; var25 < var17; var1 += 8) {
                     if ((this.a[1][var1 + 3] & 255) >= 2 && (this.a[1][var1 + 7] & 15) != 1) {
                        this.a[1][var1 + 2] = var21;
                     }

                     var25++;
                  }
               }

               this.i = 0;
            }
         }
      }
   }

   final boolean b() {
      if (this.M == -1) {
         return true;
      } else if (!this.a(1, 1, this.M)) {
         return false;
      } else {
         int var1;
         if ((var1 = this.a[1][8]) == -1) {
            return true;
         } else {
            int var2;
            int var3 = (var2 = this.h(var1)) >>> 8;
            var2 &= 255;
            this.a(1, this.M, var3, var2, 1073741824, false);
            return true;
         }
      }
   }

   final void a() {
      int var1;
      for (var1 = this.ai; this.j[var1] >= 0; var1 += 2) {
         if (this.a(1, this.j[var1], false) < this.j[var1 + 1]) {
            this.M = this.j[var1];
            return;
         }
      }

      if (this.j[var1] == -1) {
         this.ai = var1 + 1;
      }

      this.M = -1;
   }

   final int h(int var1) {
      int var3 = 0;
      int var4 = -2;
      int var5 = 2;
      int var6 = var1 >>> 8;
      var1 &= 255;

      do {
         for (int var7 = var4; var7 <= var5; var7 += 2) {
            for (int var8 = var4; var8 <= var5; var8 += 2) {
               int var9 = var6 + var7;
               int var10 = var1 + var8;
               if ((var9 | var10) >= 0 && var9 < 64 && var10 < 64) {
                  int var11 = var9 + (var10 << 6);
                  if ((this.a[var11] & 768) == 0 && (this.a[var11] & 4095) == 0) {
                     return var9 << 8 | var10;
                  }
               }
            }
         }

         var4--;
         var5++;
      } while (var3++ < 10);

      return var1;
   }

   final short a(int var1, int var2) {
      var2 &= 3;
      int var4 = 0;
      boolean var5 = false;
      boolean var6 = false;
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;
      int var10 = 2;
      int var12 = var1 >>> 8 & 63;
      int var13 = var1 & 63;
      short var14 = 0;
      switch (var2) {
         case 1:
            var14 = this.a;
            break;
         case 2:
            var14 = this.c;
            break;
         case 3:
            var14 = this.b;
      }

      if (var14 >= 0) {
         var12 = var14 >>> 8 & 63;
         var13 = var14 & 63;
         int var11 = var12 + (var13 << 6);
         if ((this.a[var11] & 32767) != 0) {
            return var14;
         }
      }

      do {
         if (((var12 | var13) & -64) == 0) {
            int var16 = var12 | var13 << 6;
            if ((this.a[var16] & 768) == 768 && (this.a[var16] & 3) == var2) {
               var14 = (short)(var12 << 8 | var13);
               switch (var2) {
                  case 1:
                     this.a = var14;
                     break;
                  case 2:
                     this.c = var14;
                     break;
                  case 3:
                     this.b = var14;
               }

               return var14;
            }
         }

         switch (var7) {
            case 0:
               var12++;
               break;
            case 1:
               var13++;
               break;
            case 2:
               var12--;
               break;
            case 3:
               var13--;
         }

         if (++var8 == var10) {
            var8 = 0;
            var9++;
            var7 = var7 + 1 & 3;
            if (var9 == 2) {
               var9 = 0;
               var10++;
            }
         }
      } while (var4++ < 65536);

      return (short)var1;
   }

   final int b(int var1) {
      int var2 = this.a[var1][5];
      int var3 = this.a[var1][7];
      int var4 = this.a[var1][6];
      int var5 = var2 + var3 + var4;
      if ((var2 << 16) / var5 < 21845) {
         return 769;
      } else {
         return (var3 << 16) / var5 < 21845 ? 771 : 770;
      }
   }

   final boolean b(int var1) {
      int var2;
      if ((var2 = this.a[1][2] + this.a[1][49]) >= this.a[1][3]) {
         return false;
      } else if (var2 >= 26) {
         return false;
      } else if (!this.a(1, 0, var1)) {
         return false;
      } else {
         int var3;
         if ((var3 = this.a[0][0]) != 0 && var1 == 2) {
            var1 = 3;
         }

         if (var3 == 0 && var1 == 3) {
            var1 = 2;
         }

         if (var3 < 2 && var1 == 6) {
            var1 = 5;
         }

         if (var3 >= 2 && var1 == 5) {
            var1 = 6;
         }

         if (var1 == 8 && this.a(1, 3, true) == 0) {
            return false;
         } else {
            int var4;
            if ((var4 = var1 - 1) < 0) {
               var4 = 0;
            }

            if (this.a[1][57 + var4] + this.a[1][66 + var4] >= this.a[1][75 + var4]) {
               return false;
            } else {
               this.c(1, var1);
               return true;
            }
         }
      }
   }

   final int a(int var1, int var2, int var3) {
      int var4;
      if ((var4 = this.a[var1][8]) == -1) {
         return var2;
      } else {
         switch (var3 & 240) {
            case 16:
               int var16;
               if ((var16 = this.a[var1][9]) > 0) {
                  int var17 = var2 >>> 8;
                  int var18 = var2 & 0xFF;
                  int var19 = (var4 >>> 8) - var17;
                  int var20 = (var4 & 0xFF) - var18;
                  int var21 = var19 * var19 + var20 * var20;
                  int var22 = (var16 >>> 8) - var17;
                  int var23 = (var16 & 0xFF) - var18;
                  if (var22 * var22 + var23 * var23 <= var21) {
                     return var16;
                  }
               }

               return var4;
            case 32:
            case 48:
               int var5 = var4;
               int var6 = var2 >>> 8;
               int var7 = var2 & 0xFF;
               int var8 = (var4 >>> 8) - var6;
               int var9 = (var4 & 0xFF) - var7;
               int var10 = var8 * var8 + var9 * var9;

               for (int var15 = 0; var15 < 2; var15++) {
                  int var11;
                  if ((var11 = this.a[var1][10 + var15]) > 0) {
                     int var12 = (var11 >>> 8) - var6;
                     int var13 = (var11 & 0xFF) - var7;
                     int var14;
                     if ((var14 = var12 * var12 + var13 * var13) <= var10) {
                        var10 = var14;
                        var5 = var11;
                     }
                  }
               }

               return var5;
            default:
               return var2;
         }
      }
   }

   final void m(Graphics var1) {
      int var2 = this.d - this.y;
      int var3 = this.S - this.N;
      int var12 = 16777215;
      int var13 = 8323199;
      int var14 = this.au;

      for (int var15 = 0; var15 < 2; var15++) {
         int var4 = var2 + var14;
         int var7 = var3 + (var14 >> 1);
         int var8 = var2 - var14;
         int var11 = var3 - (var14 >> 1);
         var12 -= var13;
         var1.setColor(var12);
         var1.drawLine(var4, var3, var2, var7);
         var1.drawLine(var2, var7, var8, var3);
         var1.drawLine(var8, var3, var2, var11);
         var1.drawLine(var2, var11, var4, var3);
         var14 = var14 + 31 >> 1;
      }

      this.au = this.au + 32 >> 1;
      this.U--;
   }

   public final void F() {
      int var1 = 0;

      while (this.a[var1] != 127) {
         if (this.a[var1] < 0) {
            var1 = this.g(var1);
         } else if ((var1 = this.d(var1)) >= 0) {
            var1 = this.d(var1, var1);
         } else {
            var1 = this.a(-var1);
         }
      }

      for (int var4 = 0; var4 < 4; var4++) {
         this.a[var4]++;
      }

      this.c = 0;
   }

   public final int d(int var1) {
      switch (this.a[var1++]) {
         case 1:
            int var37 = this.a[var1++] & 255;
            int var41 = this.a[var1++] & 255;
            int var44 = this.a[var1++] & 255;
            int var47 = this.a[var1++] & 255;
            int var48 = this.a[var1++] & 255;
            int var7 = this.a[var1++] & 255;
            int var8 = this.a[var1++] & 255;
            if (var37 == 1) {
               var1 = -var1;
            }

            int var9 = var44 << 3;
            if ((this.a[var41][var9 + 6] & '\uff00') != 0) {
               return -var1;
            }

            short var10;
            int var11 = (var10 = this.a[var41][var9 + 0]) >>> 8;
            var10 &= 255;
            if (var11 < var47) {
               return -var1;
            }

            if (var10 < var48) {
               return -var1;
            }

            if (var11 >= var47 + var7) {
               return -var1;
            }

            if (var10 >= var48 + var8) {
               return -var1;
            }

            return var1;
         case 2:
            byte var36 = this.a[var1++];
            if ((this.a[var1++] & 255) * 10 <= this.a[var36]) {
               return var1;
            }
            break;
         case 3:
            if (this.a[var1++] == this.aA) {
               return var1;
            }
            break;
         case 4:
            int var35 = this.a[var1++] & 255;
            int var40 = this.a[var1++] & 255;
            int var43 = (this.a[var1++] & 255) << 8;
            byte var46 = this.a[var1++];
            byte var6 = this.a[var1++];
            if (var35 == 1) {
               var1 = -var1;
            }

            if (this.Y != var40) {
               return -var1;
            }

            if (this.aJ != var6) {
               return -var1;
            }

            if (this.aE != var43) {
               return -var1;
            }

            if (var46 != -1 && this.h != var46) {
               return -var1;
            }

            return var1;
         case 5:
            int var34 = this.a[var1++] & 255;
            int var39 = this.a[var1++] & 255;
            byte var42 = this.a[var1++];
            int var45;
            if ((var45 = this.b(var34, var39)) < 0) {
               return -var1;
            }

            if (var42 != -1 && var45 != var42) {
               if (var42 == 0 && var45 < 2) {
                  return var1;
               }

               return -var1;
            }

            return var1;
         case 6:
            int var33 = this.a[var1++] & 255;
            int var38 = this.a[var1++] & 255;
            if (this.c[var33] == var38) {
               return var1;
            }
            break;
         case 7:
            int var2 = this.a[var1++] & 255;
            int var3 = this.a[var1++] & 255;
            int var4 = this.a[var1++] & 255;
            int var5 = this.a[var1++] & 255;
            if (var4 == 0) {
               if (this.a[var2][var3] == var5) {
                  return var1;
               }
            } else if (var4 == 1) {
               if (this.a[var2][var3] > var5) {
                  return var1;
               }
            } else if (var4 == 2 && this.a[var2][var3] < var5) {
               return var1;
            }
      }

      return -var1;
   }

   public final int d(int var1, int var2) {
      while (true) {
         switch (this.a[var1++]) {
            case 0:
               int var25 = this.a[var1++] & 255;
               int var32 = this.a[var1++] & 255;
               this.g(var25, this.I, var32);
               break;
            case 1:
               int var24 = this.a[var1++] & 255;
               int var31 = this.f(var24);
               if (this.a[var31] < 0) {
                  this.a[var31] = (byte)(-this.a[var31]);
               }
               break;
            case 2:
               byte var23 = this.a[var1++];
               int var30 = var2;
               if (var23 >= 0) {
                  var30 = this.f(var23);
               }

               if (this.a[var30] > 0) {
                  this.a[var30] = (byte)(-this.a[var30]);
               }
               break;
            case 3:
               byte var22 = this.a[var1++];
               this.a[var22] = 0;
               break;
            case 4:
               var1++;
               this.g(0, 98, 0);
               break;
            case 5:
               var1++;
               this.g(0, 98, 1);
               break;
            case 6:
               int var21 = this.a[var1++] & 255;
               int var29 = this.a[var1++] & 255;
               int var34 = this.a[var1++] & 255;
               int var36 = this.a[var1++] & 255;
               this.a(var21, var29, var34, var36, false);
               break;
            case 7:
               int var20 = this.a[var1] & 255;
               int var27 = this.a[++var1];
               int var33 = this.a[++var1] & 255;
               int var35 = this.a[++var1] & 255;
               var1++;
               byte var7 = 0;
               if (var27 == -1) {
                  for (int var37 = 0; var37 < this.a[var20][2]; var7 += 8) {
                     if ((this.a[var20][var7 + 3] & 255) < 2) {
                        this.a[var20][var7 + 2] = (short)((var33 << 8 | var35) & 65535);
                     }

                     var37++;
                  }
               } else {
                  if (var27 < 0) {
                     var27 = -var27;

                     for (int var8 = 0; var8 < this.a[var20][2]; var7 += 8) {
                        if ((this.a[var20][var7 + 3] & 255) == var27) {
                           this.a[var20][var7 + 2] = (short)((var33 << 8 | var35) & 65535);
                        }

                        var8++;
                     }
                     break;
                  }

                  this.f(var20, var1 << 3);
               }
               break;
            case 8:
               byte var19 = this.a[var1++];
               int var26 = this.a[var1++] & 255;
               byte var5 = this.a[var1++];
               if (var26 >= 5 && var26 <= 7) {
                  this.l = true;
               }

               if (var19 == -1) {
                  for (int var6 = 0; var6 < 2; var6++) {
                     this.a[var6][var26] = var5;
                  }
                  break;
               }

               this.a[var19][var26] = var5;
               break;
            case 9:
               int var3 = this.a[var1++] & 255;
               int var4 = this.a[var1++] & 255;
               this.c[var3] = (byte)var4;
               break;
            case 126:
               return var1;
         }
      }
   }

   public final int f(int var1) {
      int var2 = 0;

      while (var1 > 0) {
         var2 = this.g(var2);
         var1--;
      }

      return var2;
   }

   public final int g(int var1) {
      int var2;
      if ((var2 = this.a[var1++]) < 0) {
         var2 = -var2;
      }

      switch (var2) {
         case 1:
            var1 += 7;
            break;
         case 2:
            var1 += 2;
            break;
         case 3:
            var1++;
            break;
         case 4:
            var1 += 5;
            break;
         case 5:
         case 6:
            var1 += 3;
            break;
         case 7:
            var1 += 4;
      }

      return this.a(var1);
   }

   public final int a(int var1) {
      while (true) {
         switch (this.a[var1++]) {
            case 0:
            case 9:
               var1 += 2;
               break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
               var1++;
               break;
            case 6:
            case 7:
               var1 += 4;
               break;
            case 8:
               var1 += 3;
               break;
            case 126:
               return var1;
         }
      }
   }

   final void e(int var1, int var2, int var3) {
      if (this.c < 15) {
         int var4 = this.c * 3;
         this.b[var4++] = var1;
         this.b[var4++] = var2;
         this.b[var4] = var3;
         this.c++;
      }
   }

   final int b(int var1, int var2) {
      int var3 = 0;

      for (int var4 = 0; var4 < this.c; var4++) {
         if ((var1 < 0 || this.b[var3++] == var1) && this.b[var3++] == var2) {
            return this.b[var3];
         }
      }

      return -1;
   }

   public final boolean n(int var1) {
      if (this.aO <= 128) {
         this.F = 4;
         this.B = this.aO - 4;
      } else {
         this.F = 8;
         this.B = this.aO - 16;
      }

      if (var1 == 0) {
         this.aN = this.aA;
      }

      this.c(this.V);
      this.aQ = 0;
      this.f = true;
      this.b = true;
      return true;
   }

   final void c(int var1) {
      String var3;
      int var4 = (var3 = new a(this.z).a(var1)).length() - 1;
      this.a = new String[32];
      byte var5 = 3;
      if (this.as == 1) {
         var5 = 1;
      }

      a var6 = new a(99);
      this.a[0] = var6.a(var5);
      this.aW = 1;
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;
      this.aT = 0;
      this.av = 0;

      while (var7 < var4) {
         int var11;
         for (var11 = var7; var7 < var4; var7++) {
            char var10;
            if ((var10 = var3.charAt(var7)) == '@' || var10 == ' ') {
               var9++;
               break;
            }
         }

         if (var3.charAt(var7++) == '@') {
            int var15 = 0;
            int var13 = 0;
            switch (var3.charAt(var7++)) {
               case '1':
                  var13 = -1;
                  break;
               case '2':
                  var13 = -2;
                  break;
               case '3':
                  var13 = -3;
                  break;
               case '4':
                  var13 = -4;
                  break;
               case '5':
                  var13 = -5;
                  break;
               case '6':
                  var13 = -6;
                  break;
               case '7':
                  var13 = -7;
               case '8':
               case '9':
               case ':':
               case ';':
               case '<':
               case '=':
               case '>':
               case '?':
               case '@':
               case 'H':
               case 'I':
               case 'J':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'S':
               case 'T':
               case 'U':
               default:
                  break;
               case 'A':
                  var13 = 10;
                  break;
               case 'B':
                  var13 = -8;
                  break;
               case 'C':
                  var13 = -9;
                  break;
               case 'D':
                  var13 = -10;
                  break;
               case 'E':
                  var13 = -11;
                  break;
               case 'F':
                  var13 = -12;
                  break;
               case 'G':
                  var13 = -13;
                  break;
               case 'V':
                  var13 = -1000;
            }

            if (var13 < 0) {
               if (var13 == -1000) {
                  this.a[this.aW] = this.d;
               } else if ((var13 = -var13) > 7) {
                  this.a[this.aW] = "" + this.b[var13 - 8];
               } else {
                  this.a[this.aW] = "" + this.e[var13 - 1];
               }

               var15 = this.a(this.a[this.aW++]);
               this.aT = this.aT + this.ah + this.ay + 2;
            } else if (var13 == 0) {
               this.a[this.aW] = var3.substring(var8, var7 - 2);
               var15 = this.a(this.a[this.aW++]);
               this.aT = this.aT + this.ah + this.ay + 2;
            } else {
               this.a[this.aW++] = null;
               var15 = 64;
               this.aT = this.aT + 32 + this.ay + 2 + (this.ah << 1);
            }

            var8 = var7;
            var9 = 0;
            if (this.av < var15) {
               this.av = var15;
            }
         } else if (this.a.substringWidth(var3, var8, var7 - var8) >= this.B - this.F) {
            if (var9 < 1) {
               var8 = var7;
               var9 = 0;
            } else {
               this.a[this.aW] = var3.substring(var8, var11);
               int var14 = this.a(this.a[this.aW++]);
               this.aT = this.aT + this.ah + this.ay + 2;
               var8 = var11;
               var7 = var11;
               var9 = 0;
               if (this.av < var14) {
                  this.av = var14;
               }

               if (var11 >= var4) {
                  break;
               }
            }
         } else if (var7 >= var4) {
            this.a[this.aW] = var3.substring(var8, var4 + 1);
            int var12 = this.a(this.a[this.aW++]);
            this.aT = this.aT + this.ah + this.ay + 2;
            if (this.av < var12) {
               this.av = var12;
            }
            break;
         }
      }

      this.e = 0;
      this.D = this.aT - (this.P - this.O) + 16 + (this.ah << 1);
      this.ab = 0;
      this.ax = 0;
   }

   public final void A() {
      this.ab = 0;
      this.ax = 0;
      a(this.a);
      this.f = true;
      this.b = true;
      this.F = 0;
      this.B = this.aO;
   }

   public final void g(Graphics var1) {
      int var2 = (this.aO - this.av >> 1) - 8;
      int var3 = (this.P - this.O - this.aT >> 1) + this.O - 8;
      int var4 = this.av + 16;
      int var5 = this.aT + 24;
      int var6 = var3 + var5 - 8;
      if (var2 < 0) {
         var2 = 0;
      }

      if (var4 > this.aO) {
         var4 = this.aO;
      }

      if (var3 < this.O) {
         var3 = this.O;
      }

      if (var5 > this.P - this.O) {
         var5 = this.P - this.O;
      }

      boolean var7 = false;
      boolean var8 = false;
      var1.setClip(0, 0, this.aO, this.j);
      this.e(var1, var2, var3, var4, var5);
      var2 += 2;
      var3 += 6;
      var4 -= 4;
      var5 -= 12;
      var1.setClip(var2, var3, var4, var5);
      boolean var10 = false;
      boolean var11 = false;
      var3 += 2;
      var3 -= this.e;

      for (int var12 = 1; var12 < this.aW; var12++) {
         if (this.a[var12] == null) {
            var3 += this.ah;
            int var13;
            int var14;
            int var15 = (var14 = (var13 = this.aO >> 1) - 32) + 64;
            int var16 = var3 + 16;
            int var17 = var3 + 32;
            var1.setColor(16711680);
            var1.drawLine(var14, var16, var13, var3);
            var1.drawLine(var15, var16, var13, var3);
            var1.drawLine(var14, var16, var13, var17);
            var1.drawLine(var15, var16, var13, var17);
            var1.drawLine(++var14, var16, var13, var3);
            var1.drawLine(var15, var16, var13, var3);
            var1.drawLine(var14, var16, var13, var17);
            var1.drawLine(var15, var16, var13, var17);
            var3 = var17;
         } else {
            var2 = this.aO - this.a(this.a[var12]) >> 1;
            this.a(var1, this.a[var12], var2, var3, this.aQ - var12);
            var3 += this.ah + this.ay + 2;
         }
      }

      var1.setClip(0, 0, this.aO, this.j);
      var4 = this.a(this.a[0]) + 12;
      var2 -= var4 >> 1;
      var5 = this.ah + 12;
      var3 = var6;
      if (var2 < 0) {
         var2 = 0;
      }

      if (var6 + var5 > this.P) {
         var3 = this.P - var5;
      }

      this.e(var1, var2, var3, var4, var5);
      var2 += 6;
      var3 += 6 + this.ay;
      int var32 = (AgeOfEmpires.b.b((this.aQ << 6) + 512) >> 10) + 128;
      var1.setColor(16777215);
      var1.drawString(this.a[0], var2 + 1, var3 + 1, 20);
      var1.setColor((var32 << 16 | var32 << 4) & 16776960);
      var1.drawString(this.a[0], var2, var3, 20);
      if (this.aQ++ > 10) {
         switch (this.ab) {
            case 3:
            case 19:
               this.e -= 4;
               break;
            case 6:
            case 22:
            case 38:
            case 47:
               this.am = this.aN;
               this.ab = 0;
               this.ax = 0;
               break;
            case 9:
            case 25:
               this.e += 4;
         }
      }

      this.a(var1, this.aO - 6 >> 1, var3);
   }

   public final void a(Graphics var1, int var2, int var3) {
      if (this.e > this.D) {
         this.e = this.D;
      }

      if (this.e < 0) {
         this.e = 0;
      }

      if ((this.ar & 1) != 0) {
         if (this.D > 0) {
            if (this.e > 0) {
               var3 = this.O + 2;
               this.a(var1, 21, var2, var3, 0, 0, 7, 6, 0, 0);
            }

            if (this.e < this.D) {
               var3 = this.P - 8;
               this.a(var1, 21, var2, var3, 7, 0, 7, 6, 0, 0);
            }
         }
      }
   }

   final void g(int var1, int var2, int var3) {
      if (this.g(2)) {
         this.as = var1;
         this.z = var2;
         this.V = var3;
         if (var2 == 98) {
            this.O = 0;
            this.F = 0;
            this.P = this.j;
            this.B = this.aO;
            this.a(132 + var3, false);
            this.m();
            switch (this.ac) {
               case 0:
                  this.h(31, 2, r << 8 | k);
                  this.aA = 12;
                  this.H = 1;
                  break;
               case 16:
                  if (var3 == 0 && this.aG == this.aC) {
                     if (this.aG < 2) {
                        this.aG++;
                        this.H = 11;
                     } else {
                        this.V = 2;
                        this.H = 1;
                     }
                  }

                  this.aA = 4;
                  break;
               case 32:
                  if (this.aj < 6) {
                     if (var3 == 0 && this.aj < this.aC + 1) {
                        this.aj = this.aC + 1;
                     }

                     this.H = 12;
                  } else {
                     this.H = 1;
                  }

                  int var4 = this.a[0][90] * 3 + this.a[1][87] * 124 + this.a[0][86] * 421 - this.a[0][87] * 9 - this.a[1][86] * 12;
                  if (this.d[this.aC] < var4) {
                     this.d[this.aC] = var4;
                     this.h(0 + (this.aC << 2), 4, var4);
                     this.I();
                  }

                  this.aA = 12;
            }

            if (var3 == 0) {
               this.h(28, 1, this.aG << 4 | this.aj);
               this.I();
            }
         }
      }
   }

   final int a(String var1) {
      return var1 == null ? 0 : this.a.stringWidth(var1);
   }
}
