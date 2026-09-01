/*
 * Decompiled with CFR 0.152.
 *
 * 地图生成器（"随机地图"模式）：c.setupMissionEnv 装载任务时，若任务资源
 * 字节[0..1]（地图 RNG 种子）为零 → c.randomMap=true → c 以 d.a(9, 20, mapTiles,
 * randomMap) 进入本类（9/20 = 正/负两张种源表的容量，推断——表按 4 int/项分配）。
 * 全图先铺 768（虚空）；boolean_a() 是相位驱动（Runnable，q: 0=g() 布源 →
 * 1=e() 影响扩散 → 2 收尾）。e() 双计数器 k/g 扫 64×64 全格，对种源表 b[]（正
 * 影响 +m/距离²）与 a[]（负影响 -l/距离²）求和，过阈值 f 的格按 p（目标地形）
 * 写 mapTiles 低 12 位（768=虚空特殊处理，强度 n4>>8 钳 31 后 <<2 入 0x300 位段）。
 * c.t() 随后给出生点补城镇中心与村民。种子来源见 c.setupMissionEnv（资源字节
 * 或全局 RNG，二者已随确定性回放改造收敛，见 docs/game-mechanics.md「确定性模型」）。
 */
package AgeOfEmpires;

import AgeOfEmpires.b;
import AgeOfEmpires.c;

public final class d
implements Runnable {
    public boolean var_boolean_c = false;
    int s;
    int q = 0;
    int[] var_int_arr_b;
    int[] var_int_arr_a;
    short[] var_short_arr_a;
    int i;
    int var_int_b;
    int var_int_a;
    int j;
    int h;
    int k;
    int g;
    int var_int_c;
    int e;
    int m;
    int l;
    int f;
    int p;
    boolean var_boolean_a;
    boolean var_boolean_b;
    int o;
    public int r = 0;
    public int n = 0;
    public int d = 270;

    public final void run() {
        while (!this.boolean_a()) {
        }
        this.var_boolean_c = true;
    }

    public final void a(int n, int n2, short[] sArray, boolean bl) {
        if (this.o == 0) {
            this.o = AgeOfEmpires.c.rngStateHi << 8 | AgeOfEmpires.c.rngStateLo;
        }
        this.var_boolean_b = bl;
        this.var_short_arr_a = sArray;
        this.s = n;
        this.var_int_arr_b = new int[this.s << 2];
        this.var_int_arr_a = new int[4];
        for (int i = 0; i < 4096; ++i) {
            this.var_short_arr_a[i] = 768;
        }
        this.i = AgeOfEmpires.c.nextRandomInt() & 0x3F;
        this.var_int_b = AgeOfEmpires.c.nextRandomInt() & 0x3F;
        this.i = 32;
        this.var_int_b = 32;
        this.var_int_a = this.i;
        this.j = this.var_int_b;
        this.h = 0;
        this.q = 0;
    }

    public final void d() {
        this.var_int_arr_b = null;
        this.var_int_arr_a = null;
    }

    public final boolean boolean_a() {
        ++this.r;
        this.n = (this.r << 8) / this.d;
        switch (this.q) {
            case 0: {
                this.g();
                break;
            }
            case 1: {
                this.e();
                break;
            }
            case 2: {
                if (this.var_boolean_b) {
                    this.f();
                    break;
                }
                ++this.q;
                break;
            }
            case 3: {
                this.c();
                break;
            }
            case 4: {
                this.e();
                break;
            }
            case 5: {
                this.b();
                break;
            }
            case 6: {
                this.e();
                break;
            }
            case 7: {
                this.void_a();
                break;
            }
            case 8: {
                this.e();
                break;
            }
            default: {
                for (int i = 0; i < 4096; ++i) {
                    if ((this.var_short_arr_a[i] & 0xFFF) == 768) continue;
                    int n = i;
                    this.var_short_arr_a[n] = (short)(this.var_short_arr_a[n] | 0x8000);
                }
                return true;
            }
        }
        return false;
    }

    public final void g() {
        int n = AgeOfEmpires.c.nextRandomInt() << 3;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        while (n4 < 9) {
            n2 = (8 * AgeOfEmpires.b.int_b(n) >> 16) + this.i;
            n3 = (8 * AgeOfEmpires.b.c(n) >> 16) + this.var_int_b;
            int n5 = n2 & 0x3F;
            int n6 = n3 & 0x3F;
            if (n5 == n2 && n6 == n3) break;
            ++n4;
            n += 256;
        }
        if (n4 > 8) {
            this.i = this.var_int_a;
            this.var_int_b = this.j;
        } else {
            this.i = n2;
            this.var_int_b = n3;
        }
        this.var_int_arr_b[this.h++] = this.i;
        this.var_int_arr_b[this.h++] = this.var_int_b;
        if (this.h >= this.s << 1) {
            this.k = 0;
            this.g = 0;
            this.var_int_c = this.s;
            this.e = 0;
            this.m = 65536;
            this.l = 0;
            this.f = 512;
            this.p = 0;
            this.var_boolean_a = true;
            ++this.q;
        }
    }

    public final void f() {
        int n;
        int n2;
        AgeOfEmpires.c.nextRandomInt();
        int n3 = 0;
        do {
            n2 = AgeOfEmpires.c.nextRandomInt() & 0x3F;
            n = AgeOfEmpires.c.nextRandomInt() & 0x3F;
        } while (n2 >= 64 || n >= 64 || this.var_short_arr_a[n2 + (n << 6)] != 0);
        this.var_int_arr_a[0] = n2;
        this.var_int_arr_a[1] = n;
        boolean bl = false;
        do {
            n3 = AgeOfEmpires.c.nextRandomInt() << 3;
            int n4 = AgeOfEmpires.c.nextRandomInt() % 15;
            n2 = (AgeOfEmpires.b.int_b(n3) * (20 + n4) >> 16) + this.var_int_arr_a[0];
            n = (AgeOfEmpires.b.c(n3) * (20 + n4) >> 16) + this.var_int_arr_a[1];
        } while (n2 < 0 || n < 0 || n2 >= 64 || n >= 64 || this.var_short_arr_a[n2 + (n << 6) & 0xFFF] != 0);
        this.var_int_arr_a[2] = n2;
        this.var_int_arr_a[3] = n;
        ++this.q;
    }

    public final void e() {
        for (int i = 0; i < 64; ++i) {
            int n;
            int n2;
            int n3;
            ++this.k;
            if (this.k == 64) {
                this.k = 0;
                ++this.g;
                if (this.g >= 64) {
                    ++this.q;
                    return;
                }
            }
            int n4 = 0;
            int n5 = 0;
            int n6 = 0;
            for (n3 = 0; n3 < this.var_int_c; ++n3) {
                if ((n6 = (n2 = this.k - this.var_int_arr_b[n5++]) * n2 + (n = this.g - this.var_int_arr_b[n5++]) * n) == 0) {
                    n4 += this.m;
                    continue;
                }
                n4 += this.m / n6;
            }
            n5 = 0;
            for (n3 = 0; n3 < this.e; ++n3) {
                if ((n6 = (n2 = this.k - this.var_int_arr_a[n5++]) * n2 + (n = this.g - this.var_int_arr_a[n5++]) * n) == 0) {
                    n4 -= this.l;
                    continue;
                }
                n4 -= this.l / n6;
            }
            if (n4 < this.f) continue;
            n3 = this.k + (this.g << 6) & 0xFFF;
            if (this.p == 0) {
                int n7 = n3;
                this.var_short_arr_a[n7] = (short)(this.var_short_arr_a[n7] & 0xF000);
                continue;
            }
            if (!this.var_boolean_a && (this.var_short_arr_a[n3] & 0xFFF) != 0 || (this.var_short_arr_a[n3] & 0xFFF) == 768) continue;
            if (this.p == 768) {
                this.var_short_arr_a[n3] = 768;
                continue;
            }
            int n8 = n3;
            this.var_short_arr_a[n8] = (short)(this.var_short_arr_a[n8] & 0xF000);
            if ((n4 >>= 8) > 31) {
                n4 = 31;
            }
            int n9 = n3;
            this.var_short_arr_a[n9] = (short)(this.var_short_arr_a[n9] | (short)(0x300 | this.p & 0xFF | (n4 <<= 2)));
        }
    }

    public final void a(int n) {
        for (int i = 0; i < n; ++i) {
            int n2;
            int n3;
            while (this.var_short_arr_a[(n3 = AgeOfEmpires.c.nextRandomInt() & 0x3F) + ((n2 = AgeOfEmpires.c.nextRandomInt() & 0x3F) << 6) & 0xFFF] != 0) {
            }
            this.var_int_arr_b[i << 1] = n3;
            this.var_int_arr_b[(i << 1) + 1] = n2;
        }
    }

    public final void c() {
        this.k = 0;
        this.g = 0;
        this.var_int_c = this.s;
        this.e = 2;
        this.m = 65536;
        this.l = 196608;
        this.f = 4096;
        this.p = 1;
        this.var_boolean_a = false;
        ++this.q;
        this.a(this.var_int_c);
    }

    public final void b() {
        this.k = 0;
        this.g = 0;
        this.var_int_c = this.s >> 1;
        this.e = 2;
        this.m = 65536;
        this.l = 262144;
        this.f = 8192;
        this.p = 2;
        this.var_boolean_a = false;
        ++this.q;
        this.a(this.var_int_c);
    }

    public final void void_a() {
        this.k = 0;
        this.g = 0;
        this.var_int_c = this.s >> 1;
        this.e = 2;
        this.m = 65536;
        this.l = 262144;
        this.f = 8192;
        this.p = 3;
        this.var_boolean_a = false;
        ++this.q;
        this.a(this.var_int_c);
    }
}

