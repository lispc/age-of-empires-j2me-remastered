package AgeOfEmpires;

public final class a {
   byte[] a;

   a(int var1) {
      this.a = com.ulysseo.mad.c.a(var1);
   }

   public final String a(int var1) {
      if (this.a == null) {
         return null;
      } else {
         int var2 = 0;
         int var3 = 0;
         int var4 = 0;

         try {
            while (true) {
               var4 = ((this.a[var3] & 255) << 8) + (this.a[var3 + 1] & 255);
               var3 += 2;
               byte[] var5 = new byte[var4];

               for (int var6 = 0; var6 < var4; var6++) {
                  var5[var6] = this.a[var3 + var6];
               }

               if (var2 == var1) {
                  String var12;
                  try {
                     var12 = new String(var5, "UTF-8");
                  } catch (Exception var8) {
                     return new String(var5);
                  }

                  return var12;
               }

               var3 += var4;
               var2++;
            }
         } catch (Exception var9) {
            return null;
         }
      }
   }
}
