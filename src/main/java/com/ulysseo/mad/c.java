/*
 * Decompiled with CFR 0.152.
 */
package com.ulysseo.mad;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class c {
    private static int var_int_a;
    private static DataInputStream var_java_io_DataInputStream_a;
    private static byte[] var_byte_arr_a;

    c(int n) {
        try {
            if (var_int_a == 0 && var_byte_arr_a == null) {
                DataInputStream dataInputStream = new DataInputStream(this.getClass().getResourceAsStream("/res/data.res"));
                var_byte_arr_a = new byte[dataInputStream.readInt()];
                // 移植修正：jar 内资源流不保证一次读满，必须用 readFully
                dataInputStream.readFully(var_byte_arr_a);
            }
            if (var_int_a == 0) {
                var_java_io_DataInputStream_a = new DataInputStream(new ByteArrayInputStream(var_byte_arr_a));
            } else if (var_int_a == 1) {
                var_java_io_DataInputStream_a = new DataInputStream(this.getClass().getResourceAsStream("/res/data.res"));
                var_java_io_DataInputStream_a.readInt();
            }
            var_java_io_DataInputStream_a.skipBytes(n * 4);
            var_java_io_DataInputStream_a.skipBytes(var_java_io_DataInputStream_a.readInt() - (n + 2) * 4);
            return;
        }
        catch (Exception exception) {
            return;
        }
    }

    public static final void void_a(int n) {
        var_int_a = n;
        var_java_io_DataInputStream_a = null;
    }

    public static final byte[] byte_arr_a(int n) {
        try {
            new c(n);
            Object var2_1 = null;
            byte[] byArray = new byte[var_java_io_DataInputStream_a.readInt()];
            var_java_io_DataInputStream_a.readFully(byArray);
            return byArray;
        }
        catch (Exception exception) {
            return null;
        }
    }

    static {
        var_int_a = 1;
        var_java_io_DataInputStream_a = null;
        var_byte_arr_a = null;
    }
}

