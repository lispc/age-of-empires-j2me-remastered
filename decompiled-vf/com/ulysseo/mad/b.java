package com.ulysseo.mad;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

public final class b extends GameCanvas {
   private a a;
   private boolean c = false;
   private boolean b;
   private Timer a;
   private Timer b;
   private TimerTask b;
   private TimerTask a;
   private int a;
   private Image a;
   private Graphics a;
   private boolean a = true;

   b(a var1) {
      super(false);
      if (!this.isDoubleBuffered()) {
         this.a = Image.createImage(this.getWidth(), this.getHeight());
         this.a = this.a.getGraphics();
      }

      this.a = var1;
   }

   public final void paint(Graphics var1) {
      if (this.a) {
         this.a = false;
      } else if (this.isDoubleBuffered()) {
         this.a.p(var1);
      } else {
         this.a.p(this.a);
         var1.drawImage(this.a, 0, 0, 20);
      }
   }

   protected final void keyPressed(int var1) {
      this.a.a(var1);
      this.getGameAction(var1);
   }

   protected final void keyRepeated(int var1) {
   }

   protected final void keyReleased(int var1) {
      this.a.e(var1);
      this.getGameAction(var1);
   }

   protected final void pointerPressed(int var1, int var2) {
   }

   protected final void pointerReleased(int var1, int var2) {
   }

   protected final void pointerDragged(int var1, int var2) {
   }

   protected final void hideNotify() {
      this.a.q();
   }

   protected final void showNotify() {
      this.a.k();
   }

   protected final void sizeChanged(int var1, int var2) {
      this.a.j(var1, var2);
   }

   public final void b() {
      Display.getDisplay(com.ulysseo.mad.a.a).setCurrent(this);
   }

   public final void a(int var1, int var2) {
      if (this.b != null) {
         this.a();
      }

      this.a = var2;
      this.c = true;
      this.b = false;
      this.b = new TimerTask(this) {
         private final b a;

         {
            this.a = var1;
         }

         public final void run() {
            if (com.ulysseo.mad.b.b(this.a)) {
               if (com.ulysseo.mad.b.a(this.a) == 4) {
                  System.currentTimeMillis();
                  this.scheduledExecutionTime();
               }

               com.ulysseo.mad.b.a(this.a).w();
               if (!com.ulysseo.mad.b.a(this.a)) {
                  this.a.repaint();
                  this.a.serviceRepaints();
                  return;
               }
            } else {
               Thread.yield();
            }
         }
      };
      this.a = new Timer();
      if (var2 == 1) {
         this.a.schedule(this.b, 0L, (long)var1);
      } else {
         this.a.scheduleAtFixedRate(this.b, 0L, (long)var1);
      }
   }

   public final void a() {
      if (this.a != null) {
         this.a.cancel();
      }

      if (this.b != null) {
         this.b.cancel();
      }

      if (this.b) {
         if (this.b != null) {
            this.b.cancel();
         }

         if (this.a != null) {
            this.a.cancel();
         }
      }

      this.c = false;
      this.b = false;
      this.b = null;
      this.a = null;
      this.a = null;
      this.b = null;
   }

   static final boolean b(b var0) {
      return var0.c;
   }

   static final int a(b var0) {
      return var0.a;
   }

   static final a a(b var0) {
      return var0.a;
   }

   static final boolean a(b var0) {
      return var0.b;
   }
}
