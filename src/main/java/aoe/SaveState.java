package aoe;

import AgeOfEmpires.c;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 任务内快照存档（v1）。设计：不重建世界，恢复 = 先让原版管线把同一个任务
 * （同 ac/aF）装载完，再把快照里的状态数组/指针覆写回去（"同任务重载+覆写"）。
 * 字段完整性由 DevFields 存→载→diff 验收。
 *
 * 格式：int magic "AOE1" | int version | 定长顺序字段段（见 capture）。
 * 覆写点只在 EDT 帧首（c.p() 里消费 pending 字节），避免和渲染遍历打架。
 */
public final class SaveState {
    static final int MAGIC = 0x414F4531;    // "AOE1"
    static final int VERSION = 1;

    private SaveState() {
    }

    /** 捕获任务内状态。必须在 aA==6（任务主视图稳定）时调用。 */
    public static byte[] capture(c g) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 16);
        DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        writeString(out, g.devLastNavSpec);     // dev 导航 spec（null=窗口会话），放头部便于 O(1) 读取
        // —— 任务标识 ——
        out.writeInt(g.ac);
        out.writeInt(g.aC);
        out.writeInt(g.aF);
        out.writeBoolean(g.var_boolean_k);
        // —— 设置/进度（.nfo 镜像 + 解锁位）——
        writeBytes(out, g.var_byte_arr_f);
        out.writeInt(g.aj);
        out.writeInt(g.aG);
        // —— 脚本树与引擎指针（任务内目标/对话推进状态）——
        writeBytes(out, g.var_byte_arr_i);
        // 任务数据镜像（res aF）：脚本解释器会就地写"已执行"标记，读档必须一并还原
        writeBytes(out, g.var_byte_arr_a);
        out.writeInt(g.aR);
        out.writeInt(g.ao);
        out.writeInt(g.Z);
        out.writeInt(g.H);
        out.writeInt(g.v);
        out.writeInt(g.ap);
        // —— 地图格（地形+迷雾+占位）——
        writeShorts(out, g.var_short_arr_a);
        // —— 计数/资源/杂项 int 数组 ——
        writeInts(out, g.var_int_arr_a);
        writeInts(out, g.var_int_arr_c);
        writeInts(out, g.var_int_arr_d);
        writeInts(out, g.var_int_arr_e);
        // —— 每玩家单位/建筑槽位 ——
        int players = g.var_int_arr_arr_a.length;
        out.writeInt(players);
        for (int i = 0; i < players; ++i) {
            writeInts(out, g.var_int_arr_arr_a[i]);
            writeShorts(out, g.var_short_arr_arr_a[i]);
            writeInts(out, g.var_int_arr_arr_b[i]);
            writeShorts(out, g.var_short_arr_arr_b[i]);
        }
        // —— 相机/光标/选中 ——
        out.writeInt(g.y);
        out.writeInt(g.N);
        out.writeInt(g.aa);
        out.writeInt(g.aV);
        out.writeInt(g.Q);
        out.writeInt(g.aU);
        out.writeInt(g.A);
        out.writeInt(g.at);
        out.writeInt(g.n);
        out.writeInt(g.var_int_b);
        out.writeInt(g.aE);
        out.writeInt(g.Y);
        out.writeInt(g.var_int_h);
        out.writeInt(g.aJ);
        out.writeInt(g.p);
        out.writeByte(g.var_byte_a);
        out.flush();
        return bos.toByteArray();
    }

    /** 把快照覆写回当前（已装载同一任务的）游戏实例。只该在 EDT 帧首调用。 */
    public static void apply(c g, byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data));
        if (in.readInt() != MAGIC) {
            throw new IOException("bad magic");
        }
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("bad version " + version);
        }
        g.devLastNavSpec = readString(in);
        g.ac = in.readInt();
        g.aC = in.readInt();
        g.aF = in.readInt();
        g.var_boolean_k = in.readBoolean();
        readBytes(in, g.var_byte_arr_f);
        g.aj = in.readInt();
        g.aG = in.readInt();
        readBytes(in, g.var_byte_arr_i);
        readBytes(in, g.var_byte_arr_a);
        g.aR = in.readInt();
        g.ao = in.readInt();
        g.Z = in.readInt();
        g.H = in.readInt();
        g.v = in.readInt();
        g.ap = in.readInt();
        readShorts(in, g.var_short_arr_a);
        readInts(in, g.var_int_arr_a);
        readInts(in, g.var_int_arr_c);
        readInts(in, g.var_int_arr_d);
        readInts(in, g.var_int_arr_e);
        int players = in.readInt();
        if (players != g.var_int_arr_arr_a.length) {
            throw new IOException("player count mismatch " + players);
        }
        for (int i = 0; i < players; ++i) {
            readInts(in, g.var_int_arr_arr_a[i]);
            readShorts(in, g.var_short_arr_arr_a[i]);
            readInts(in, g.var_int_arr_arr_b[i]);
            readShorts(in, g.var_short_arr_arr_b[i]);
        }
        g.y = in.readInt();
        g.N = in.readInt();
        g.aa = in.readInt();
        g.aV = in.readInt();
        g.Q = in.readInt();
        g.aU = in.readInt();
        g.A = in.readInt();
        g.at = in.readInt();
        g.n = in.readInt();
        g.var_int_b = in.readInt();
        g.aE = in.readInt();
        g.Y = in.readInt();
        g.var_int_h = in.readInt();
        g.aJ = in.readInt();
        g.p = in.readInt();
        g.var_byte_a = in.readByte();
        // 强制下一帧全量重画 + 小地图重新盖章（探索可能有变化）
        g.af = 0;
        g.k();
    }

    /** 只解头部（identity + spec），不解析全量字段段。 */
    public static int[] identity(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data));
        expectHeader(in);
        readString(in);     // nav spec
        int ac = in.readInt();
        int aC = in.readInt();
        int aF = in.readInt();
        in.readBoolean();
        byte[] nfo = readLenBytes(in);
        int hi = nfo != null && nfo.length > 32 ? ((nfo[31] & 0xFF) << 8 | nfo[32] & 0xFF) : 0;
        return new int[]{ac, aF, hi, aC};
    }

    /** 读快照里记录的 dev 导航 spec（null = 窗口会话存档）。 */
    public static String navSpec(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data));
        expectHeader(in);
        return readString(in);      // 头部第一个字段
    }

    private static void expectHeader(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("bad magic");
        }
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("bad version " + version);
        }
    }

    private static byte[] readLenBytes(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) {
            return null;
        }
        byte[] a = new byte[len];
        in.readFully(a);
        return a;
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
        } else {
            byte[] b = s.getBytes("UTF-8");
            out.writeInt(b.length);
            out.write(b);
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) {
            return null;
        }
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, "UTF-8");
    }

    // —— 定长数组 IO：长度不符直接抛错（不同任务间错载会在这里暴露）——

    private static void writeBytes(DataOutputStream out, byte[] a) throws IOException {
        out.writeInt(a == null ? -1 : a.length);
        if (a != null) {
            out.write(a);
        }
    }

    private static void readBytes(DataInputStream in, byte[] a) throws IOException {
        int len = in.readInt();
        if (a == null && len == -1) {
            return;
        }
        if (a == null || a.length != len) {
            throw new IOException("byte[] length mismatch (" + len + ")");
        }
        in.readFully(a);
    }

    private static void writeShorts(DataOutputStream out, short[] a) throws IOException {
        out.writeInt(a.length);
        for (short v : a) {
            out.writeShort(v);
        }
    }

    private static void readShorts(DataInputStream in, short[] a) throws IOException {
        if (in.readInt() != a.length) {
            throw new IOException("short[] length mismatch");
        }
        for (int i = 0; i < a.length; ++i) {
            a[i] = in.readShort();
        }
    }

    private static void writeInts(DataOutputStream out, int[] a) throws IOException {
        out.writeInt(a == null ? -1 : a.length);
        if (a != null) {
            for (int v : a) {
                out.writeInt(v);
            }
        }
    }

    private static void readInts(DataInputStream in, int[] a) throws IOException {
        int len = in.readInt();
        if (a == null && len == -1) {
            return;
        }
        if (a == null || a.length != len) {
            throw new IOException("int[] length mismatch (" + len + ")");
        }
        for (int i = 0; i < a.length; ++i) {
            a[i] = in.readInt();
        }
    }
}
