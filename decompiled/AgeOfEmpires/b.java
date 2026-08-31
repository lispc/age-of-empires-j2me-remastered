/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Image
 *  javax.microedition.media.Manager
 *  javax.microedition.media.Player
 *  javax.microedition.midlet.MIDlet
 */
package AgeOfEmpires;

import com.ulysseo.mad.c;
import java.io.InputStream;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;

public final class b {
    static boolean c;
    public static MIDlet var_javax_microedition_midlet_MIDlet_a;
    public static boolean var_boolean_a;
    public static boolean var_boolean_b;
    static int[] var_int_arr_b;
    static byte[] var_byte_arr_a;
    static int var_int_a;
    static long[] var_long_arr_a;
    static int[] var_int_arr_a;
    static Player var_javax_microedition_media_Player_a;

    static final void void_a() {
        byte[] byArray = com.ulysseo.mad.c.byte_arr_a(130);
        int n = byArray.length >> 1;
        var_int_arr_b = new int[n];
        for (int i = 0; i < n; ++i) {
            AgeOfEmpires.b.var_int_arr_b[i] = (byArray[i << 1] << 8) + (byArray[(i << 1) + 1] & 0xFF) & 0xFFFF;
        }
    }

    static final int c(int n) {
        return AgeOfEmpires.b.int_b(n -= 512);
    }

    static final int int_b(int n) {
        if ((n &= 0x7FF) < 1024) {
            if (n < 512) {
                return var_int_arr_b[n];
            }
            return -var_int_arr_b[1024 - n];
        }
        if (n < 1536) {
            return -var_int_arr_b[n - 1024];
        }
        return var_int_arr_b[2048 - n];
    }

    static final int d(int n) {
        if (n < 0) {
            return -n;
        }
        return n;
    }

    public static final Image javax_microedition_lcdui_Image_a(byte[] byArray, int n, int n2) {
        int n3;
        int n4;
        int n5;
        int n6;
        if (byArray == null) {
            return null;
        }
        if (byArray.length == 1) {
            return null;
        }
        if (var_long_arr_a == null) {
            AgeOfEmpires.b.b();
        }
        var_int_a = 0;
        var_byte_arr_a = byArray;
        int n7 = AgeOfEmpires.b.int_a(8) + 1;
        int n8 = AgeOfEmpires.b.int_a(8) + 1;
        int n9 = AgeOfEmpires.b.int_a(16) + 1;
        int n10 = AgeOfEmpires.b.int_a(16) + 1;
        int n11 = AgeOfEmpires.b.int_a(8) + 1;
        int[] nArray = new int[n11];
        int n12 = 0;
        int n13 = -1;
        for (n6 = 0; n6 < n8; ++n6) {
            if (n != n6) {
                int n14 = AgeOfEmpires.b.int_a(4);
                if (AgeOfEmpires.b.int_a(4) == 1) {
                    AgeOfEmpires.b.void_b(8);
                }
                AgeOfEmpires.b.void_a(n11 * (1 + n14));
                continue;
            }
            int n15 = AgeOfEmpires.b.int_a(4);
            n12 = AgeOfEmpires.b.int_a(4);
            if (n12 == 1) {
                n13 = AgeOfEmpires.b.int_a(8);
            }
            for (n5 = 0; n5 < n11; ++n5) {
                nArray[n5] = AgeOfEmpires.b.a(AgeOfEmpires.b.int_a(1 + n15 << 3), n15);
            }
        }
        switch (n2) {
            case 1: {
                for (n5 = 0; n5 < n11; ++n5) {
                    n6 = 0;
                    while (var_int_arr_a[n6] >= 0) {
                        if (nArray[n5] == var_int_arr_a[n6]) {
                            nArray[n5] = var_int_arr_a[n6 + 1];
                        }
                        n6 += 2;
                    }
                }
                break;
            }
            case 2: {
                int n16;
                int n17;
                for (n5 = 1; n5 < n11; ++n5) {
                    n17 = nArray[n5];
                    n16 = (n17 & 0xFF00) >> 8;
                    n4 = n17 & 0xFF;
                    n17 = (n17 & 0xFF0000) >> 16;
                    n3 = (n4 + n16 * 6 + n17 * 3) / 16;
                    nArray[n5] = n3 << 16 | n3 << 8 | n3;
                }
                break;
            }
            case 3: {
                for (n5 = 1; n5 < n11; ++n5) {
                    nArray[n5] = 0xFFFFFF;
                }
                break;
            }
            case 4: {
                int n16;
                int n17;
                for (n5 = 1; n5 < n11; ++n5) {
                    n17 = nArray[n5];
                    n16 = (n17 & 0xFF00) >> 8;
                    n4 = n17 & 0xFF;
                    n17 = (n17 & 0xFF0000) >> 16;
                    n3 = (n4 + n16 * 6 + n17 * 3) / 10;
                    nArray[n5] = n3 << 16;
                }
                break;
            }
        }
        n3 = AgeOfEmpires.b.int_a(16);
        byte[] byArray2 = AgeOfEmpires.b.byte_arr_a(n3);
        byte[] byArray3 = new byte[69 + n12 * (12 + n13 + 1) + n11 * 3 + n3];
        n4 = 0;
        AgeOfEmpires.b.a(byArray3, 0, 32, -1991225785);
        AgeOfEmpires.b.a(byArray3, 32, 32, 218765834);
        AgeOfEmpires.b.a(byArray3, 64, 32, 13);
        AgeOfEmpires.b.a(byArray3, 96, 32, 1229472850);
        AgeOfEmpires.b.a(byArray3, 128, 32, n9);
        AgeOfEmpires.b.a(byArray3, 160, 32, n10);
        AgeOfEmpires.b.a(byArray3, 192, 8, n7);
        AgeOfEmpires.b.a(byArray3, 200, 32, 0x3000000);
        int n18 = AgeOfEmpires.b.b(byArray3, 12, 29);
        AgeOfEmpires.b.a(byArray3, 232, 32, n18);
        AgeOfEmpires.b.a(byArray3, 264, 32, n11 * 3);
        AgeOfEmpires.b.a(byArray3, 296, 32, 1347179589);
        for (n6 = 0; n6 < n11; ++n6) {
            AgeOfEmpires.b.a(byArray3, 328 + n6 * 24, 24, nArray[n6]);
        }
        n18 = AgeOfEmpires.b.b(byArray3, 37, 41 + n11 * 3);
        AgeOfEmpires.b.a(byArray3, 264 + (8 + n11 * 3 << 3), 32, n18);
        n4 = 264 + (8 + n11 * 3 + 4 << 3);
        if (n12 == 1) {
            AgeOfEmpires.b.a(byArray3, n4, 32, n13 + 1);
            AgeOfEmpires.b.a(byArray3, n4 + 32, 32, 1951551059);
            for (n6 = 0; n6 < n13; ++n6) {
                AgeOfEmpires.b.a(byArray3, n4 + 64 + (n6 << 3), 8, 255);
            }
            AgeOfEmpires.b.a(byArray3, n4 + 64 + (n6 << 3), 8, 0);
            n18 = AgeOfEmpires.b.b(byArray3, (n4 >> 3) + 4, (n4 >> 3) + 4 + 4 + (n13 + 1));
            AgeOfEmpires.b.a(byArray3, n4 + (8 + (n13 + 1) << 3), 32, n18);
            n4 += 8 + (n13 + 1) + 4 << 3;
        }
        AgeOfEmpires.b.a(byArray3, n4, 32, n3);
        AgeOfEmpires.b.a(byArray3, n4 + 32, 32, 1229209940);
        for (n6 = 0; n6 < n3; ++n6) {
            AgeOfEmpires.b.a(byArray3, n4 + 64 + (n6 << 3), 8, byArray2[n6]);
        }
        n18 = AgeOfEmpires.b.b(byArray3, (n4 >> 3) + 4, (n4 >> 3) + 4 + 4 + n3);
        AgeOfEmpires.b.a(byArray3, n4 + (8 + n3 << 3), 32, n18);
        AgeOfEmpires.b.a(byArray3, n4 += 8 + n3 + 4 << 3, 32, 0);
        AgeOfEmpires.b.a(byArray3, n4 + 32, 32, 1229278788);
        n18 = AgeOfEmpires.b.b(byArray3, (n4 >> 3) + 4, (n4 >> 3) + 4 + 4 + 0);
        AgeOfEmpires.b.a(byArray3, n4 + 64, 32, n18);
        var_byte_arr_a = null;
        return Image.createImage((byte[])byArray3, (int)0, (int)byArray3.length);
    }

    public static final int a(int n, int n2) {
        int n3;
        int n4;
        int n5;
        switch (n2) {
            case 0: {
                n5 = (n >> 5) * 255 / 7;
                n4 = (n >> 2 & 7) * 255 / 7;
                n3 = (n & 3) * 255 / 3;
                break;
            }
            case 1: {
                n5 = (n >> 11) * 255 / 31;
                n4 = (n >> 5 & 0x3F) * 255 / 63;
                n3 = (n & 0x1F) * 255 / 31;
                break;
            }
            default: {
                return n;
            }
        }
        return n5 << 16 | n4 << 8 | n3;
    }

    public static final void b() {
        var_long_arr_a = new long[256];
        for (int i = 0; i < 256; ++i) {
            long l = i;
            for (int j = 0; j < 8; ++j) {
                if ((l & 1L) == 1L) {
                    l = 0xEDB88320L ^ l >> 1;
                    continue;
                }
                l >>= 1;
            }
            AgeOfEmpires.b.var_long_arr_a[i] = l;
        }
    }

    public static final int b(byte[] byArray, int n, int n2) {
        long l = 0xFFFFFFFFL;
        for (int i = n; i < n2; ++i) {
            l = var_long_arr_a[(int)((l ^ (long)byArray[i]) & 0xFFL)] ^ l >> 8;
        }
        return (int)(l ^ 0xFFFFFFFFL);
    }

    public static final int int_a(byte[] byArray, int n, int n2) {
        int n3 = 0;
        if ((n & 7) == 0 && (n2 & 7) == 0) {
            int n4 = n2 >> 3;
            for (int i = 0; i < n4; ++i) {
                n3 <<= 8;
                n3 += byArray[n >> 3] & 0xFF;
                n += 8;
            }
        } else {
            for (int i = 1; i <= n2; ++i) {
                int n5 = n >> 3;
                n3 += (byArray[n5] >> 7 - (n - (n5 << 3)) & 1) << n2 - i;
                ++n;
            }
        }
        return n3;
    }

    public static final void a(byte[] byArray, int n, int n2, int n3) {
        int n4 = 0;
        byte by = 0;
        int n5 = n;
        int n6 = 1 << n2 - 1;
        for (int i = 0; i < n2; ++i) {
            n4 = n5 >> 3;
            by = (byte)(1 << 7 - (n5 - (n4 << 3)));
            if ((n3 & n6) == n6) {
                int n7 = n4;
                byArray[n7] = (byte)(byArray[n7] | by);
            } else {
                int n8 = n4;
                byArray[n8] = (byte)(byArray[n8] & (byte)(0xFF ^ by));
            }
            n3 <<= 1;
            ++n5;
        }
    }

    public static final int int_a(int n) {
        int n2 = AgeOfEmpires.b.int_a(var_byte_arr_a, var_int_a, n);
        AgeOfEmpires.b.void_b(n);
        return n2;
    }

    public static final void void_b(int n) {
        var_int_a += n;
    }

    public static final void void_a(int n) {
        var_int_a += n << 3;
    }

    public static final byte byte_a() {
        return (byte)AgeOfEmpires.b.int_a(8);
    }

    public static final byte[] byte_arr_a(int n) {
        byte[] byArray = new byte[n];
        if ((var_int_a & 7) == 0) {
            System.arraycopy(var_byte_arr_a, var_int_a >> 3, byArray, 0, n);
            AgeOfEmpires.b.void_a(n);
        } else {
            for (int i = 0; i < n; ++i) {
                AgeOfEmpires.b.var_byte_arr_a[i] = AgeOfEmpires.b.byte_a();
            }
        }
        return byArray;
    }

    static final String a(int n, byte by) {
        String string = "/Menu_poly";
        switch (n) {
            case 132: {
                string = "/Won";
                break;
            }
            case 133: {
                string = "/Lost";
                break;
            }
            case 134: {
                string = "/Music01";
                break;
            }
            case 135: {
                string = "/Music02";
                break;
            }
            case 136: {
                string = "/Music03";
                break;
            }
            case 137: {
                string = "/Music04";
                break;
            }
            case 138: {
                string = "/Music05";
                break;
            }
            case 139: {
                string = "/Music06";
                break;
            }
            case 140: {
                string = "/Music07";
                break;
            }
            case 141: {
                string = "/Music08";
                break;
            }
            case 142: {
                string = "/Music09";
                break;
            }
            case 143: {
                string = "/Music10";
            }
        }
        if (by == 1) {
            string = string + ".mmf";
        }
        if (by == 0) {
            string = string + ".mid";
        }
        return string;
    }

    public static final boolean a(int n, boolean bl) {
        if (!c) {
            return true;
        }
        try {
            if (var_javax_microedition_media_Player_a != null) {
                var_javax_microedition_media_Player_a.stop();
                var_javax_microedition_media_Player_a.deallocate();
                var_javax_microedition_media_Player_a.close();
                var_javax_microedition_media_Player_a = null;
            }
            String string = AgeOfEmpires.b.a(n, (byte)0);
            InputStream inputStream = var_javax_microedition_midlet_MIDlet_a.getClass().getResourceAsStream(string);
            if (inputStream == null) {
                return false;
            }
            var_javax_microedition_media_Player_a = Manager.createPlayer((InputStream)inputStream, (String)"audio/midi");
            if (var_javax_microedition_media_Player_a == null) {
                return false;
            }
            var_javax_microedition_media_Player_a.prefetch();
            if (bl) {
                var_javax_microedition_media_Player_a.setLoopCount(-1);
            }
            var_javax_microedition_media_Player_a.start();
        }
        catch (Exception exception) {
            return false;
        }
        return true;
    }

    public static final void c() {
        try {
            if (var_javax_microedition_media_Player_a != null) {
                var_javax_microedition_media_Player_a.stop();
                var_javax_microedition_media_Player_a.deallocate();
                var_javax_microedition_media_Player_a.close();
                var_javax_microedition_media_Player_a = null;
            }
            return;
        }
        catch (Exception exception) {
            return;
        }
    }

    static {
        var_boolean_a = false;
        var_boolean_b = false;
        var_int_arr_a = new int[]{9471, 16720896, 28159, 16739584, 6993663, 16758123, 5415423, 16753489, 3248639, 16748598, 1605119, 16743963, 27135, 16739072, 24063, 16735488, 19711, 0xFF3F00, 15615, 0xFF3000, 12543, 16720128, 8447, 14491904, 8414, 12198144, 8381, 9969920, 8348, 7676160, 8307, 5382400, 8274, 5382400, -1};
    }
}

