package AgeOfEmpires;

import javax.microedition.midlet.MIDlet;

public final class AgeOfEmpires extends MIDlet {
   c a = null;
   static boolean a;

   public AgeOfEmpires() {
      a = false;
      b.a = this;
   }

   public final void startApp() {
      if (!a) {
         b.a = this;
         this.a = new c(this);
         b.a = false;
         this.a.o = -1;
         this.a.b();
         this.a.b();
         a = true;
      }
   }

   public final void destroyApp(boolean var1) {
      c.a.a();
   }

   public final void pauseApp() {
      if (a) {
         b.c();
         this.a.af = 0;
         this.a.s();
         b.a = true;
      }
   }

   public final void a() {
      b.c();
      this.a.h();
      this.destroyApp(false);
      this.notifyDestroyed();
   }
}
