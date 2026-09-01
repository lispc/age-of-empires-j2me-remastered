package AgeOfEmpires;

public final class d implements Runnable {
   public boolean c = false;
   int s;
   int q = 0;
   int[] b;
   int[] a;
   short[] a;
   int i;
   int b;
   int a;
   int j;
   int h;
   int k;
   int g;
   int c;
   int e;
   int m;
   int l;
   int f;
   int p;
   boolean a;
   boolean b;
   int o;
   public int r = 0;
   public int n = 0;
   public int d = 270;

   public final void run() {
      while (!this.a()) {
      }

      this.c = true;
   }

   public final void a(int var1, int var2, short[] var3, boolean var4) {
      if (this.o == 0) {
         this.o = AgeOfEmpires.c.r << 8 | AgeOfEmpires.c.k;
      }

      this.b = var4;
      this.a = var3;
      this.s = var1;
      this.b = new int[this.s << 2];
      this.a = new int[4];

      for (int var5 = 0; var5 < 4096; var5++) {
         this.a[var5] = 768;
      }

      this.i = AgeOfEmpires.c.a() & 63;
      this.b = AgeOfEmpires.c.a() & 63;
      this.i = 32;
      this.b = 32;
      this.a = this.i;
      this.j = this.b;
      this.h = 0;
      this.q = 0;
   }

   public final void d() {
      this.b = null;
      this.a = null;
   }

   public final boolean a() {
      this.r++;
      this.n = (this.r << 8) / this.d;
      switch (this.q) {
         case 0:
            this.g();
            break;
         case 1:
            this.e();
            break;
         case 2:
            if (this.b) {
               this.f();
            } else {
               this.q++;
            }
            break;
         case 3:
            this.c();
            break;
         case 4:
            this.e();
            break;
         case 5:
            this.b();
            break;
         case 6:
            this.e();
            break;
         case 7:
            this.a();
            break;
         case 8:
            this.e();
            break;
         default:
            for (int var1 = 0; var1 < 4096; var1++) {
               if ((this.a[var1] & 4095) != 768) {
                  this.a[var1] = (short)(this.a[var1] | '耀');
               }
            }

            return true;
      }

      return false;
   }

   public final void g() {
      int var4 = AgeOfEmpires.c.a() << 3;
      int var6 = 0;
      int var7 = 0;

      int var1;
      for (var1 = 0; var1 < 9; var4 += 256) {
         var6 = (8 * AgeOfEmpires.b.b(var4) >> 16) + this.i;
         var7 = (8 * AgeOfEmpires.b.c(var4) >> 16) + this.b;
         int var2 = var6 & 63;
         int var3 = var7 & 63;
         if (var2 == var6 && var3 == var7) {
            break;
         }

         var1++;
      }

      if (var1 > 8) {
         this.i = this.a;
         this.b = this.j;
      } else {
         this.i = var6;
         this.b = var7;
      }

      this.b[this.h++] = this.i;
      this.b[this.h++] = this.b;
      if (this.h >= this.s << 1) {
         this.k = 0;
         this.g = 0;
         this.c = this.s;
         this.e = 0;
         this.m = 65536;
         this.l = 0;
         this.f = 512;
         this.p = 0;
         this.a = true;
         this.q++;
      }
   }

   public final void f() {
      AgeOfEmpires.c.a();
      int var3 = 0;

      int var1;
      int var2;
      do {
         var1 = AgeOfEmpires.c.a() & 63;
         var2 = AgeOfEmpires.c.a() & 63;
      } while (var1 >= 64 || var2 >= 64 || this.a[var1 + (var2 << 6)] != 0);

      this.a[0] = var1;
      this.a[1] = var2;
      boolean var4 = false;

      do {
         var3 = AgeOfEmpires.c.a() << 3;
         int var5 = AgeOfEmpires.c.a() % 15;
         var1 = (AgeOfEmpires.b.b(var3) * (20 + var5) >> 16) + this.a[0];
         var2 = (AgeOfEmpires.b.c(var3) * (20 + var5) >> 16) + this.a[1];
      } while (var1 < 0 || var2 < 0 || var1 >= 64 || var2 >= 64 || this.a[var1 + (var2 << 6) & 4095] != 0);

      this.a[2] = var1;
      this.a[3] = var2;
      this.q++;
   }

   public final void e() {
      for (int var8 = 0; var8 < 64; var8++) {
         this.k++;
         if (this.k == 64) {
            this.k = 0;
            this.g++;
            if (this.g >= 64) {
               this.q++;
               return;
            }
         }

         int var5 = 0;
         int var6 = 0;
         int var7 = 0;

         for (int var9 = 0; var9 < this.c; var9++) {
            int var3 = this.k - this.b[var6++];
            int var4 = this.g - this.b[var6++];
            if ((var7 = var3 * var3 + var4 * var4) == 0) {
               var5 += this.m;
            } else {
               var5 += this.m / var7;
            }
         }

         var6 = 0;

         for (int var19 = 0; var19 < this.e; var19++) {
            int var10 = this.k - this.a[var6++];
            int var11 = this.g - this.a[var6++];
            if ((var7 = var10 * var10 + var11 * var11) == 0) {
               var5 -= this.l;
            } else {
               var5 -= this.l / var7;
            }
         }

         if (var5 >= this.f) {
            int var20 = this.k + (this.g << 6) & 4095;
            if (this.p == 0) {
               this.a[var20] = (short)(this.a[var20] & '\uf000');
            } else if ((this.a || (this.a[var20] & 4095) == 0) && (this.a[var20] & 4095) != 768) {
               if (this.p == 768) {
                  this.a[var20] = 768;
               } else {
                  this.a[var20] = (short)(this.a[var20] & '\uf000');
                  if ((var5 = var5 >> 8) > 31) {
                     var5 = 31;
                  }

                  var5 <<= 2;
                  this.a[var20] = (short)(this.a[var20] | (short)(768 | this.p & 0xFF | var5));
               }
            }
         }
      }
   }

   public final void a(int var1) {
      for (int var4 = 0; var4 < var1; var4++) {
         int var2;
         int var3;
         do {
            var2 = AgeOfEmpires.c.a() & 63;
            var3 = AgeOfEmpires.c.a() & 63;
         } while (this.a[var2 + (var3 << 6) & 4095] != 0);

         this.b[var4 << 1] = var2;
         this.b[(var4 << 1) + 1] = var3;
      }
   }

   public final void c() {
      this.k = 0;
      this.g = 0;
      this.c = this.s;
      this.e = 2;
      this.m = 65536;
      this.l = 196608;
      this.f = 4096;
      this.p = 1;
      this.a = false;
      this.q++;
      this.a(this.c);
   }

   public final void b() {
      this.k = 0;
      this.g = 0;
      this.c = this.s >> 1;
      this.e = 2;
      this.m = 65536;
      this.l = 262144;
      this.f = 8192;
      this.p = 2;
      this.a = false;
      this.q++;
      this.a(this.c);
   }

   public final void a() {
      this.k = 0;
      this.g = 0;
      this.c = this.s >> 1;
      this.e = 2;
      this.m = 65536;
      this.l = 262144;
      this.f = 8192;
      this.p = 3;
      this.a = false;
      this.q++;
      this.a(this.c);
   }
}
