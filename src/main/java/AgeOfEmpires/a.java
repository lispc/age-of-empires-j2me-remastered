/*
 * Decompiled with CFR 0.152.
 */
package AgeOfEmpires;

import com.ulysseo.mad.c;

public final class a {
    byte[] a;

    a(int n) {
        this.a = c.byte_arr_a(n);
    }

    public final String a(int n) {
        if (this.a == null) {
            return null;
        }
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        try {
            while (true) {
                n4 = ((this.a[n3] & 0xFF) << 8) + (this.a[n3 + 1] & 0xFF);
                n3 += 2;
                byte[] byArray = new byte[n4];
                for (int i = 0; i < n4; ++i) {
                    byArray[i] = this.a[n3 + i];
                }
                if (n2 == n) {
                    String string;
                    try {
                        string = new String(byArray, "UTF-8");
                    }
                    catch (Exception exception) {
                        return new String(byArray);
                    }
                    return string;
                }
                n3 += n4;
                ++n2;
            }
        }
        catch (Exception exception) {
            return null;
        }
    }
}

