package aoe.ai;

import AgeOfEmpires.c;

/**
 * 玩家 AI 公共工具件（2026-09-05 抽自 RuleBasedAi/CampaignAi 的逐字节重复段）。
 *
 * 抽公共件的血泪动机：tryResearch 锁槽偏移 bug——RuleBasedAi 传原始偏移 o=i<<2、
 * CampaignAi 传记录序号 i,两边各自手写建筑扫描所以没人对账,GM/DBA/WT 静默
 * 失效了几个版本（v30 尸检实锤）。**约定：凡进 c.buildingTable 的索引一律是
 * 原始偏移 o=i<<2(每条记录 4 int),本类的 findBuilding 返回的即是 o。**
 *
 * 只收"两边逐字节相同"或"已被 bug 咬过"的件;单边特有逻辑（侦察/配额/阶段机）
 * 留在各自文件，别为了抽象而抽象。
 */
public final class AiKit {
    private AiKit() {
    }

    // ===== 单位下令（槽位直写,复刻 orderMove 三写;接敌中默认不打断）=====

    /** 单单位下令：接敌（任务字 1）且非 force 不打断；目标相同不写。 */
    public static void orderUnit(c game, int i, int tgt, boolean force) {
        short[] slots = game.playerUnitSlots[0];
        int o = i << 3;
        if (!force && (slots[o + 7] & 0xF) == 1) {
            return;
        }
        if ((slots[o + 2] & 0xFFFF) == tgt) {
            return;
        }
        slots[o + 1] = slots[o + 0];
        slots[o + 2] = (short) tgt;
        slots[o + 7] = 0;
        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
    }

    /** 全体军事压向目标（includeEngaged=false 时不打断接敌中的）。 */
    public static void orderMilitary(c game, int target, boolean includeEngaged) {
        short[] slots = game.playerUnitSlots[0];
        int units = game.playerUnitHeaders[0][2];
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            if ((slots[o + 3] & 0xFF) < 2) {
                continue;
            }
            if (!includeEngaged && (slots[o + 7] & 0xF) == 1) {
                continue;
            }
            if ((slots[o + 2] & 0xFFFF) != target) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) target;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            }
        }
    }

    // ===== 建筑表访问（原始偏移 o=i<<2 约定的唯一入口）=====

    /** 建筑记录 o 处的打包坐标 x<<8|y。 */
    public static int buildingTile(int[] recs, int o) {
        return ((recs[o] >> 8) & 0x3F) << 8 | (recs[o] & 0x3F);
    }

    /** 找类型 type 的建筑,返回原始偏移 o(直接喂 tryResearch 等),无则 -1。
     *  requireDone=true 时跳过在建（0x40000000）与未完工（进度字节≠255）。 */
    public static int findBuilding(int[] recs, int bcount, int type, boolean requireDone) {
        for (int i = 0; i < bcount; ++i) {
            int o = i << 2;
            if ((recs[o + 3] & 0xFF) != type) {
                continue;
            }
            if (requireDone && ((recs[o + 2] & 0x40000000) != 0 || (recs[o + 2] & 0xFF) != 255)) {
                continue;
            }
            return o;
        }
        return -1;
    }

    // ===== 几何（两 AI 原逐字节相同）=====

    /** from→to 方向 dist 格处的点（Chebyshev 归一,钳图界内）。 */
    public static int stanceTile(int fromPacked, int toPacked, int dist) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int dx = (toPacked >>> 8) - fx, dy = (toPacked & 0xFF) - fy;
        int m = Math.max(Math.abs(dx), Math.abs(dy));
        if (m == 0) {
            return fromPacked;
        }
        int ax = Math.max(1, Math.min(62, fx + dx * dist / m));
        int ay = Math.max(1, Math.min(62, fy + dy * dist / m));
        return ax << 8 | ay;
    }

    /** 两 TC 的 Chebyshev 距离;敌 TC 未知（<0）返回 MAX。 */
    public static int corridorLen(int myTc, int enemyTc) {
        if (enemyTc < 0) {
            return Integer.MAX_VALUE;
        }
        int dx = (enemyTc >>> 8) - (myTc >>> 8), dy = (enemyTc & 0xFF) - (myTc & 0xFF);
        return Math.max(Math.abs(dx), Math.abs(dy));
    }
}
