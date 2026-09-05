package aoe;

import AgeOfEmpires.c;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 任务内快照存档（v2）。设计：不重建世界，恢复 = 先让原版管线把同一个任务
 * （同 ac/aF）装载完，再把快照里的状态数组/指针覆写回去（"同任务重载+覆写"）。
 * 字段完整性由 DevFields 存→载→diff 验收。
 *
 * 格式：int magic "AOE1" | int version | 定长顺序字段段（见 capture）。
 * v2（2026-09-01）：末尾新增 tickCount——确定性回放（tools/replaycheck.sh）的锚：
 * 模拟里有 tick 奇偶/取模逻辑（回血 tickCount&8、投射物旋转起点、BGM 倒计时），
 * 只恢复世界状态不恢复 tick 的话，同一份输入也会走出不同轨迹。
 * v3（2026-09-01）：末尾新增 techFlags——科技/建造解锁位（建筑建成与研究完成
 * 会就地置位，如 House 建成 techFlags[0]=1 解锁村民训练）。不持久化的话，
 * 读档后所有已解锁的生产/研究槽回退到资源模板（玩家第6轮实report：读档局
 * '*' 生产菜单空，无法出兵）。
 * v4（2026-09-05）：末尾新增 AI 大脑字段（aiEnabled/三计时器/建造相位/stance/
 * 调参组）+ 采集目标缓存（var_short_a/b/c，findNearbyResource 的记忆格，
 * tickAi 村民重派/onUnitArrived 直接消费）+ 任务脚本事件日志（var_int_c/
 * var_int_arr_b 已用前缀，脚本条件 5"某槽位单位类型"的查询源）。
 * apply 前的"同任务重载"会跑 setupMissionEnv 把 AI 字段全部重置为
 * 初值（计时器归零、stance=0、建造相位回 0），而世界数组又原样覆写回来——
 * 结果 AI 的经济/军事时钟被静默清零，出兵波时刻漂移（m4 首战提前 650t、
 * 战役回放不复现终局；tickAi 是唯一 aiEnabled 消费方，故 m1-m3 回放不受影响）。
 * var_short 缓存与事件日志同理：活进程里已积累、重载后归零/清空，恢复不完整
 * 时 AI 村民去采不同的格、脚本条件 5 失忆——A/B 对拍实测引发 ~200t 级发散。
 * 覆写点只在 EDT 帧首（payCost.p() 里消费 pending 字段），避免和渲染遍历打架。
 */
public final class SaveState {
    static final int MAGIC = 0x414F4531;    // "AOE1"
    static final int VERSION = 4;

    private SaveState() {
    }

    /** 捕获任务内状态。必须在 screenState==6（任务主视图稳定）时调用。 */
    public static byte[] capture(c g) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 16);
        DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
        writeString(out, g.devLastNavSpec);     // dev 导航 spec（null=窗口会话），放头部便于 O(1) 读取
        // —— 任务标识 ——
        out.writeInt(g.gameMode);
        out.writeInt(g.missionIndex);
        out.writeInt(g.missionResId);
        out.writeBoolean(g.randomMap);
        // —— 设置/进度（.nfo 镜像 + 解锁位）——
        writeBytes(out, g.nfoData);
        out.writeInt(g.campaignProgress);
        out.writeInt(g.tutorialProgress);
        // —— 脚本树与引擎指针（任务内目标/对话推进状态）——
        writeBytes(out, g.menuTree);
        // 任务数据镜像（res aF）：脚本解释器会就地写"已执行"标记，读档必须一并还原
        writeBytes(out, g.missionScript);
        out.writeInt(g.menuNode);
        out.writeInt(g.menuNodeCount);
        out.writeInt(g.menuHighlight);
        out.writeInt(g.menuScreenId);
        out.writeInt(g.pendingPanelSwitch);
        out.writeInt(g.ap);
        // —— 地图格（地形+迷雾+占位）——
        writeShorts(out, g.mapTiles);
        // —— 计数/资源/杂项 int 数组 ——
        writeInts(out, g.scriptFrameCounters);
        writeInts(out, g.actionMenuItemIds);
        writeInts(out, g.nfoHighScores);
        writeInts(out, g.var_int_arr_e);
        // —— 每玩家单位/建筑槽位 ——
        int players = g.playerUnitHeaders.length;
        out.writeInt(players);
        for (int i = 0; i < players; ++i) {
            writeInts(out, g.playerUnitHeaders[i]);
            writeShorts(out, g.playerUnitSlots[i]);
            writeInts(out, g.buildingTable[i]);
            writeShorts(out, g.projectileTable[i]);
        }
        // —— 相机/光标/选中 ——
        out.writeInt(g.cameraPxX);
        out.writeInt(g.cameraPxY);
        out.writeInt(g.cursorTileX);
        out.writeInt(g.cursorTileY);
        out.writeInt(g.cursorTileIdx);
        out.writeInt(g.mapViewSavedCamX);
        out.writeInt(g.mapViewSavedCamY);
        out.writeInt(g.mapViewSavedCursorX);
        out.writeInt(g.mapViewSavedCursorY);
        out.writeInt(g.selectionMode);
        out.writeInt(g.selectionMark);
        out.writeInt(g.selectionPlayer);
        out.writeInt(g.selectedType);
        out.writeInt(g.selectedSlot);
        out.writeInt(g.p);
        out.writeByte(g.randomMapDifficulty);
        // v2：tickCount + 全局 RNG 静态（确定性回放锚，见类注释）。RNG 不钉住的话，
        // 读档后的建造掷骰等模拟消费随"读档前听了多少菜单音乐/走了多少 tick"发散。
        out.writeInt(g.tickCount);
        out.writeInt(c.rngStateHi);
        out.writeInt(c.rngStateLo);
        // v3：科技/建造解锁位（建筑建成/研究完成就地置位，见类注释）
        writeBytes(out, g.techFlags);
        // v4：AI 大脑字段（tickAi 每帧消费；不恢复则读档后 AI 经济/军事时钟被
        // setupMissionEnv 的初值静默清零，出兵/建造/训练时刻全漂，见类注释）
        out.writeBoolean(g.aiEnabled);
        out.writeInt(g.aiStance);
        out.writeInt(g.aiTrainTimer);
        out.writeInt(g.aiBuildTimer);
        out.writeInt(g.aiFreeResTimer);
        out.writeInt(g.aiBuildPhase);
        out.writeInt(g.aiBuildTarget);
        out.writeInt(g.aiBuildInterval);
        out.writeInt(g.aiTrainInterval);
        out.writeInt(g.aiAttackThreshold);
        out.writeInt(g.aiGuardRadiusSq);
        out.writeInt(g.aiFreeResInterval);
        out.writeInt(g.aiGatherMultiplier);
        // v4 续：采集目标缓存 + 任务脚本事件日志已用前缀
        out.writeShort(g.var_short_a);
        out.writeShort(g.var_short_b);
        out.writeShort(g.var_short_c);
        int eventCount = g.var_int_arr_b != null ? g.var_int_c : -1;
        out.writeInt(eventCount);
        for (int i = 0; i < eventCount * 3; ++i) {
            out.writeInt(g.var_int_arr_b[i]);
        }
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
        if (version < 2 || version > VERSION) {
            throw new IOException("bad version " + version);
        }
        g.devLastNavSpec = readString(in);
        g.gameMode = in.readInt();
        g.missionIndex = in.readInt();
        g.missionResId = in.readInt();
        g.randomMap = in.readBoolean();
        readBytes(in, g.nfoData);
        g.campaignProgress = in.readInt();
        g.tutorialProgress = in.readInt();
        readBytes(in, g.menuTree);
        readBytes(in, g.missionScript);
        g.menuNode = in.readInt();
        g.menuNodeCount = in.readInt();
        g.menuHighlight = in.readInt();
        g.menuScreenId = in.readInt();
        g.pendingPanelSwitch = in.readInt();
        g.ap = in.readInt();
        readShorts(in, g.mapTiles);
        readInts(in, g.scriptFrameCounters);
        readInts(in, g.actionMenuItemIds);
        readInts(in, g.nfoHighScores);
        readInts(in, g.var_int_arr_e);
        int players = in.readInt();
        if (players != g.playerUnitHeaders.length) {
            throw new IOException("player count mismatch " + players);
        }
        for (int i = 0; i < players; ++i) {
            readInts(in, g.playerUnitHeaders[i]);
            readShorts(in, g.playerUnitSlots[i]);
            readInts(in, g.buildingTable[i]);
            readShorts(in, g.projectileTable[i]);
        }
        g.cameraPxX = in.readInt();
        g.cameraPxY = in.readInt();
        g.cursorTileX = in.readInt();
        g.cursorTileY = in.readInt();
        g.cursorTileIdx = in.readInt();
        g.mapViewSavedCamX = in.readInt();
        g.mapViewSavedCamY = in.readInt();
        g.mapViewSavedCursorX = in.readInt();
        g.mapViewSavedCursorY = in.readInt();
        g.selectionMode = in.readInt();
        g.selectionMark = in.readInt();
        g.selectionPlayer = in.readInt();
        g.selectedType = in.readInt();
        g.selectedSlot = in.readInt();
        g.p = in.readInt();
        g.randomMapDifficulty = in.readByte();
        // v2：tickCount + 全局 RNG 静态（确定性回放锚，见类注释）
        g.tickCount = in.readInt();
        c.rngStateHi = in.readInt();
        c.rngStateLo = in.readInt();
        if (version >= 3) {
            // v3：科技/建造解锁位。v2 旧档没有此段——保持装载模板不动（旧档
            // 读回来与当年行为一致，只是那时还没有发现这个回退问题）。
            readBytes(in, g.techFlags);
        }
        if (version >= 4) {
            // v4：AI 大脑字段。旧档（v2/v3）没有此段——保持 setupMissionEnv 的
            // 初值（与旧版行为一致）。
            g.aiEnabled = in.readBoolean();
            g.aiStance = in.readInt();
            g.aiTrainTimer = in.readInt();
            g.aiBuildTimer = in.readInt();
            g.aiFreeResTimer = in.readInt();
            g.aiBuildPhase = in.readInt();
            g.aiBuildTarget = in.readInt();
            g.aiBuildInterval = in.readInt();
            g.aiTrainInterval = in.readInt();
            g.aiAttackThreshold = in.readInt();
            g.aiGuardRadiusSq = in.readInt();
            g.aiFreeResInterval = in.readInt();
            g.aiGatherMultiplier = in.readInt();
            g.var_short_a = in.readShort();
            g.var_short_b = in.readShort();
            g.var_short_c = in.readShort();
            int evtCount = in.readInt();
            if (evtCount < 0) {
                // 任务环境尚未装配（理论不可达：apply 前必跑同任务重载）——保持现状
            } else {
                if (g.var_int_arr_b == null || g.var_int_arr_b.length < evtCount * 3) {
                    throw new IOException("event log capacity mismatch (" + evtCount + ")");
                }
                for (int i = 0; i < evtCount * 3; ++i) {
                    g.var_int_arr_b[i] = in.readInt();
                }
                g.var_int_c = evtCount;
            }
        }
        // 强制下一帧全量重画 + 小地图重新盖章（探索可能有变化）
        g.mapThumbStampRow = 0;
        g.onShown();
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
        if (version < 2 || version > VERSION) {
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
