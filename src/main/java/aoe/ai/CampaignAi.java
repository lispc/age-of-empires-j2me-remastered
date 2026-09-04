package aoe.ai;

import AgeOfEmpires.c;

/**
 * 战役 AI（-Daoe.playerAi=aoe.ai.CampaignAi）。按 missionIndex 分派 handler。
 * 与 RuleBasedAi（随机图）分立：战役目标各异（拆堡/护送/配额/守城），胜负走
 * missionScript 脚本路径而非通用 TC 规则（missionIndex==0 除外——它的脚本
 * 只有开局简报，胜负走 onThingDestroyed 通用规则：拆敌 TC 胜、全灭负）。
 * 各关胜负条件目录：docs/research/campaign-mission-scripts.md
 * （tools/scriptdis.py 反汇编产物）。
 *
 * 读面：与 RuleBasedAi 相同 + 直读敌 buildingTable（战役期初允许全图读，
 * 诚实化是后话——战役考核的是任务解法不是侦察）。写面：逐单位 slot 直写
 * （同 RuleBasedAi v34 的 DEFEND 原语），攻击态（任务字低 nibble==1）单位
 * 不打断（清 slot[7] = 抹装填）。
 *
 * 确定性：只按 tickCount 节流，无墙钟、无 RNG。
 */
public final class CampaignAi implements PlayerAi {

    private static final int DECIDE_EVERY = 8;
    private static final int LOG_EVERY = 500;

    private int nextDecide;
    private int lastLog;
    private int lastMissionLogged = -1;
    private int fleeCount;              // #4 逃命遥测（500t 摘要行消费后清零）

    // ===== #0 拆堡关状态 =====
    private int m0Target = -1;          // 当前攻击目标（打包 tx<<8|ty）；-1=未定位
    private int m0Reissue;              // 上次群体重投 tick（目标不变的周期性续投）
    private int razeAnchor = -1;        // 出发质心（拆建筑关的撤退回血点）
    private final boolean[] razeHealing = new boolean[26]; // 残血撤退中标记

    @Override
    public void tick(c game) {
        int t = game.tickCount;
        if (t < this.nextDecide) {
            return;
        }
        this.nextDecide = t + DECIDE_EVERY;
        int ss = game.screenState;
        if (ss == 2) {
            // 弹窗冻结世界（简报/事件对话），headless 自关窗。战役简服用 -7 实测可关。
            game.onKeyPress(-7);
            return;
        }
        if (ss != 6 || game.gameMode != 32) {
            return;
        }
        switch (game.missionIndex) {
            case 0:
            case 3:
                this.tickRaze(game);
                break;
            case 6:
                this.tickFinalAssault(game);
                break;
            case 5:
                this.tickProtectCastle(game);
                break;
            case 2:
                this.tickGatherQuota(game);
                break;
            case 1:
                this.tickEscort(game);
                break;
            case 4:
                this.tickCastleRace(game);
                break;
            default:
                if (this.lastMissionLogged != game.missionIndex) {
                    this.lastMissionLogged = game.missionIndex;
                    System.out.println("[cai] mission " + game.missionIndex + ": no handler, idling");
                }
        }
        if (t - this.lastLog >= LOG_EVERY) {
            this.lastLog = t;
            int units = game.playerUnitHeaders[0][2];
            int ebld = game.playerUnitHeaders[1][4];
            System.out.println("[cai] t=" + t + " m" + game.missionIndex
                + " units=" + units + " ebld=" + ebld + " flee=" + this.fleeCount + " tgt="
                + (this.m0Target >= 0 ? (this.m0Target >>> 8) + "," + (this.m0Target & 0xFF) : "?"));
            this.fleeCount = 0;
        }
    }

    /** 拆建筑关（#0/#3/#6 通用）：#0 胜负走通用规则（拆敌 TC 即胜）；#3/#6
     *  脚本胜利 = p1 建筑数==0（res113/res116）。
     *  迭代史：v1 全体 all-in 敌 TC（#3/#6 全灭）；v2 守军最少优先+残血撤退
     *  （接敌单位不撤，缠斗到死）；v3 接敌也撤退+塔区惩罚（#3 5/5 攻克）。
     *  v4（#6 总攻关：敌塔 Keep 级 攻4/甲25/索敌 6 格，投石机索敌只有 4 格——
     *  塔比投石机手长，近战砍塔=自杀 255/2 需千 tick，塔杀剑士 135t）：
     *  兵种分工——攻城组（t7 冲车/t8 投石机/t9 征服者）专职点塔，一座一座拔，
     *  HP<120 即撤（塔 dps 高，90 阈值来不及走）；近战组只拆塔区外建筑，
     *  塔区外拆完后在锚点站桩回血等攻城组，绝不进塔火。我方无攻城单位存活时
     *  近战才被迫啃塔（#3 没有攻城兵，塔总得有人拆）。 */
    private void tickRaze(c game) {
        short[] slots = game.playerUnitSlots[0];
        int units = game.playerUnitHeaders[0][2];
        if (units == 0) {
            return;
        }
        // 出发质心（首帧记录）= 撤退回血点
        if (this.razeAnchor < 0) {
            int sx = 0, sy = 0;
            for (int i = 0; i < units; ++i) {
                int pos = slots[i << 3] & 0xFFFF;
                sx += pos >>> 8;
                sy += pos & 0xFF;
            }
            this.razeAnchor = (sx / units) << 8 | (sy / units);
        }
        int ax = this.razeAnchor >>> 8, ay = this.razeAnchor & 0xFF;
        // 敌单位位置表（守军计数用）
        short[] es = game.playerUnitSlots[1];
        int eu = game.playerUnitHeaders[1][2];
        int[] eb = game.buildingTable[1];
        int ebCount = game.playerUnitHeaders[1][4];
        // 攻城组是否存活（t7/t8/t9）——没有则近战被迫啃塔（#3 场景）
        boolean siegeAlive = false;
        for (int i = 0; i < units; ++i) {
            int type = slots[(i << 3) + 3] & 0xFF;
            if (type == 7 || type == 8 || type == 9) {
                siegeAlive = true;
                break;
            }
        }
        // 两类目标：towerTarget = 离锚点最近的敌塔；softTarget = 塔区外、守军
        // 最少的普通建筑。塔区 = 本身是塔或在任一敌塔 7 格内（v3 尸检：塔旁矿场
        // 收光我方 6 兵）。
        int towerTarget = -1, towerD2 = Integer.MAX_VALUE;
        for (int i = 0; i < ebCount; ++i) {
            int o = i << 2;
            if ((eb[o + 3] & 0xFF) != 12) {
                continue;
            }
            int bx = (eb[o] >> 8) & 0x3F, by = eb[o] & 0x3F;
            int d2 = (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
            if (d2 < towerD2) {
                towerD2 = d2;
                towerTarget = bx << 8 | by;
            }
        }
        int softTarget = -1;
        long bestScore = Long.MAX_VALUE;
        for (int i = 0; i < ebCount; ++i) {
            int o = i << 2;
            if ((eb[o + 3] & 0xFF) == 12) {
                continue;
            }
            int bx = (eb[o] >> 8) & 0x3F, by = eb[o] & 0x3F;
            boolean towerZone = false;
            for (int j = 0; j < ebCount; ++j) {
                int q = j << 2;
                if ((eb[q + 3] & 0xFF) != 12) {
                    continue;
                }
                int dx = ((eb[q] >> 8) & 0x3F) - bx, dy = (eb[q] & 0x3F) - by;
                if (dx * dx + dy * dy <= 49) {
                    towerZone = true;
                    break;
                }
            }
            if (towerZone) {
                continue;
            }
            int defenders = 0;
            for (int j = 0; j < eu; ++j) {
                int ep = es[j << 3] & 0xFFFF;
                int dx = (ep >>> 8) - bx, dy = (ep & 0xFF) - by;
                if (dx * dx + dy * dy <= 64) {
                    ++defenders;
                }
            }
            long score = defenders * 1000000L
                + (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
            if (score < bestScore) {
                bestScore = score;
                softTarget = bx << 8 | by;
            }
        }
        if (towerTarget < 0 && softTarget < 0) {
            return; // 敌建筑清零——胜局判定在路上
        }
        this.m0Target = softTarget >= 0 ? softTarget : towerTarget;
        boolean reissue = game.tickCount - this.m0Reissue >= 150;
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            int type = slots[o + 3] & 0xFF;
            boolean siege = type == 7 || type == 8 || type == 9;
            int hp = slots[o + 4] & 0xFF;
            boolean healing = this.razeHealing[i];
            int retreatAt = siege ? 120 : 90;
            if (!healing && hp < retreatAt) {
                this.razeHealing[i] = true;
                healing = true;
            } else if (healing && hp >= 220) {
                this.razeHealing[i] = false;
                healing = false;
            }
            // 接敌单位不打断——除非触发了撤退（缠斗到死是 #3 全灭根因：守军
            // 不追击，脱战就能活）
            if (!healing && (slots[o + 7] & 0xF) == 1) {
                continue;
            }
            int tgt;
            if (healing) {
                // 撤退点按槽位散开——同点撤退会互相占位，到不了靶心格就不回血
                // （站桩回血要 pos==tgt；#6 尸检：5 台攻城器全卡在锚点外 1-2 格
                // HP 5-111 永不愈合）。
                tgt = this.retreatTile(i);
            } else if (siege) {
                tgt = towerTarget >= 0 ? towerTarget
                    : (softTarget >= 0 ? softTarget : this.razeAnchor);
            } else {
                tgt = softTarget >= 0 ? softTarget
                    : (towerTarget >= 0 && !siegeAlive ? towerTarget : this.razeAnchor);
            }
            boolean stale = (slots[o + 2] & 0xFFFF) != tgt;
            boolean idle = (slots[o + 7] & 0xF) == 0
                && (slots[o + 0] & 0xFFFF) == (slots[o + 2] & 0xFFFF);
            if (stale || (reissue && idle)) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) tgt;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            }
        }
        if (reissue) {
            this.m0Reissue = game.tickCount;
        }
    }

    /** #5 守城关（res115："Protect the Castle"。胜 = p1 单位数==0（脚本波次
     *  全歼）；负 = 我方城堡（建筑 type 3）被毁）。
     *  波次表（刷出即冲我方城堡 (37,47)）：4 剑士 → +500t 5 弓兵 → +500t 3 骑兵
     *  → +500t 4 冲车 → +700t 3 投石机。我方 5 剑士 + 5 骑兵守 1 城堡。
     *  策略：锚定城堡，优先点杀冲车/投石机（对城宝具），其余就近拦截；
     *  视野内无敌则直读敌槽位全场追猎残敌（胜利要全歼）。 */
    private void tickProtectCastle(c game) {
        // 锚点 = 我方城堡
        int castle = -1;
        int[] mb = game.buildingTable[0];
        int mbCount = game.playerUnitHeaders[0][4];
        for (int i = 0; i < mbCount; ++i) {
            int o = i << 2;
            if ((mb[o + 3] & 0xFF) == 3) {
                castle = ((mb[o] >> 8) & 0x3F) << 8 | (mb[o] & 0x3F);
                break;
            }
        }
        if (castle < 0) {
            return; // 城堡没了——败局判定在路上
        }
        int cx = castle >>> 8, cy = castle & 0xFF;
        // 选目标：冲车/投石机优先（任何距离），其次离城堡最近的敌兵。
        short[] eslots = game.playerUnitSlots[1];
        int eunits = game.playerUnitHeaders[1][2];
        int target = -1, targetD2 = Integer.MAX_VALUE;
        for (int i = 0; i < eunits; ++i) {
            int o = i << 3;
            int pos = eslots[o] & 0xFFFF;
            int dx = (pos >>> 8) - cx, dy = (pos & 0xFF) - cy;
            int d2 = dx * dx + dy * dy;
            int type = eslots[o + 3] & 0xFF;
            if (type == 7 || type == 8) {
                d2 = -1; // 攻城武器绝对优先
            }
            if (target < 0 || d2 < targetD2) {
                target = pos;
                targetD2 = d2;
            }
        }
        if (target < 0) {
            return; // 全歼达成——胜局判定在路上
        }
        this.m0Target = target;
        short[] slots = game.playerUnitSlots[0];
        int units = game.playerUnitHeaders[0][2];
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            if ((slots[o + 7] & 0xF) == 1) {
                continue; // 接敌/攻击中，不打断
            }
            if ((slots[o + 2] & 0xFFFF) != target) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) target;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            }
        }
    }

    /** #2 经济配额关（res112："collect 100 wood, 100 stone and 100 gold"——
     *  脚本判定是 hdr[0][5..7] 各 **严格 >100**）。我方 3 村民 + 4 剑士 + 2 侦察，
     *  敌 8 兵静止散布（aiEnabled=false，不主动进攻，但守在木/金矿点旁会
     *  自动接敌——主线 m2 笔记：先清 (17,38)(18,39) 弓手再伐木）。
     *  策略：军事逐个清剿"蹲在所需资源 8 格内"的敌兵（离我 TC 最近者优先）；
     *  村民只派无蹲守的资源格，按配额缺口最大者分派。采集→交存→返矿引擎
     *  全自动（§10 定论），AI 只处理闲置。 */
    private void tickGatherQuota(c game) {
        int[] hdr0 = game.playerUnitHeaders[0];
        // 我方 TC（锚点/逃命点）
        int tc = -1;
        int[] mb = game.buildingTable[0];
        for (int i = 0; i < hdr0[4]; ++i) {
            if ((mb[(i << 2) + 3] & 0xFF) == 9) {
                tc = ((mb[i << 2] >> 8) & 0x3F) << 8 | (mb[i << 2] & 0x3F);
                break;
            }
        }
        if (tc < 0) {
            return;
        }
        int tx = tc >>> 8, ty = tc & 0xFF;
        int[] need = new int[4]; // 1木 2金 3石
        for (int k = 1; k <= 3; ++k) {
            need[k] = Math.max(0, 101 - hdr0[4 + k]); // hdr[5]=木 [6]=金 [7]=石
        }
        if (need[1] + need[2] + need[3] == 0) {
            return; // 配额达成——胜局判定在路上（脚本是严格 >100）
        }
        // 敌单位表 + 蹲守检测
        short[] es = game.playerUnitSlots[1];
        int eu = game.playerUnitHeaders[1][2];
        // 军事清剿：找"蹲资源敌兵"（距任一尚需资源格 ≤8）里离我 TC 最近者
        int clearTarget = -1, clearD2 = Integer.MAX_VALUE;
        for (int i = 0; i < eu; ++i) {
            int ep = es[i << 3] & 0xFFFF;
            int ex = ep >>> 8, ey = ep & 0xFF;
            if (!this.nearWantedResource(game, ex, ey, need)) {
                continue;
            }
            int d2 = (ex - tx) * (ex - tx) + (ey - ty) * (ey - ty);
            if (d2 < clearD2) {
                clearD2 = d2;
                clearTarget = ep;
            }
        }
        // 军事下令（不接敌打断）
        short[] slots = game.playerUnitSlots[0];
        int units = hdr0[2];
        if (clearTarget >= 0) {
            this.m0Target = clearTarget;
            for (int i = 0; i < units; ++i) {
                int o = i << 3;
                if ((slots[o + 3] & 0xFF) < 2 || (slots[o + 7] & 0xF) == 1) {
                    continue;
                }
                if ((slots[o + 2] & 0xFFFF) != clearTarget) {
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) clearTarget;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                }
            }
        }
        // 村民分派：闲置（任务字 0 且已到目标格）→ 缺口最大种类的最近安全格
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            if ((slots[o + 3] & 0xFF) >= 2) {
                continue; // 军事
            }
            boolean idle = (slots[o + 7] & 0xF) == 0
                && (slots[o + 0] & 0xFFFF) == (slots[o + 2] & 0xFFFF);
            if (!idle) {
                continue; // 采集循环引擎全自动
            }
            int pos = slots[o] & 0xFFFF;
            // 缺口最大优先，找不到安全格退次缺
            for (int kTry = 0; kTry < 3; ++kTry) {
                int kind = this.maxNeedKind(need);
                if (kind == 0) {
                    break;
                }
                int tile = this.nearestSafeResource(game, pos, kind, es, eu);
                if (tile >= 0) {
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) tile;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    System.out.println("[cai] assign villager " + i + " kind=" + kind
                        + " -> " + (tile >>> 8) + "," + (tile & 0xFF) + " t=" + game.tickCount);
                    break;
                }
                need[kind] = 0; // 该种类无安全格，本决策退而求其次
            }
        }
    }

    /** 缺口最大的资源种类（1木2金3石；0=全满）。调用处在找不到安全格时
     *  把 need[kind] 清零以退到次缺种类。 */
    private int maxNeedKind(int[] need) {
        int best = 0;
        for (int k = 1; k <= 3; ++k) {
            if (need[k] > 0 && (best == 0 || need[k] > need[best])) {
                best = k;
            }
        }
        return best;
    }

    /** (ex,ey) 是否蹲在任一尚需资源格 8 格内。 */
    private boolean nearWantedResource(c game, int ex, int ey, int[] need) {
        for (int yy = Math.max(0, ey - 8); yy <= Math.min(63, ey + 8); ++yy) {
            for (int xx = Math.max(0, ex - 8); xx <= Math.min(63, ex + 8); ++xx) {
                int t = game.mapTiles[xx + (yy << 6)] & 0xFFF;
                if ((t & 0x300) == 0x300 && need[t & 3] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 离 pos 最近、8 格内无敌兵的资源格（kind 1木2金3石）。全图读（战役许可）。 */
    private int nearestSafeResource(c game, int pos, int kind, short[] es, int eu) {
        return this.nearestSafeResourceOffCorridor(game, pos, kind, es, eu, -1, -1);
    }

    /** 走廊回避版：skip 敌TC→我TC 走廊两侧 5 格内的资源格——敌 all-in 沿走廊
     *  直扑我 TC，走廊上的矿=前线（#4 v4 尸检：石矿 (37,40) 正在走廊上，
     *  4000t 起战场就钉在矿上，经济被拉锯战饿死）。etc=-1 时不回避。 */
    private int nearestSafeResourceOffCorridor(c game, int pos, int kind, short[] es, int eu,
            int etc, int mtc) {
        int px = pos >>> 8, py = pos & 0xFF;
        int best = -1, bestD2 = Integer.MAX_VALUE;
        for (int y = 0; y < 64; ++y) {
            for (int x = 0; x < 64; ++x) {
                int t = game.mapTiles[x + (y << 6)] & 0xFFF;
                if ((t & 0x300) != 0x300 || (t & 3) != kind) {
                    continue;
                }
                if (etc >= 0 && this.distToSegment(x, y, etc >>> 8, etc & 0xFF, mtc >>> 8, mtc & 0xFF) <= 25) {
                    continue; // 走廊 5 格内（d2≤25）不采
                }
                boolean camped = false;
                for (int j = 0; j < eu; ++j) {
                    int ep = es[j << 3] & 0xFFFF;
                    int dx = (ep >>> 8) - x, dy = (ep & 0xFF) - y;
                    if (dx * dx + dy * dy <= 64) {
                        camped = true;
                        break;
                    }
                }
                if (camped) {
                    continue;
                }
                int d2 = (x - px) * (x - px) + (y - py) * (y - py);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = x << 8 | y;
                }
            }
        }
        if (best < 0 && etc >= 0) {
            return this.nearestSafeResourceOffCorridor(game, pos, kind, es, eu, -1, -1); // 全在走廊上=不回避
        }
        return best;
    }

    /** 点 (px,py) 到线段 (x1,y1)-(x2,y2) 的垂直距离²（近似：投影截断）。 */
    private int distToSegment(int px, int py, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1, dy = y2 - y1;
        int len2 = dx * dx + dy * dy;
        if (len2 == 0) {
            return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        }
        int t = ((px - x1) * dx + (py - y1) * dy);
        t = Math.max(0, Math.min(len2, t));
        int cx = x1 + t * dx / len2, cy = y1 + t * dy / len2;
        return (px - cx) * (px - cx) + (py - cy) * (py - cy);
    }

    /** 塔位锚点（移植 RuleBasedAi.corridorAnchor/stanceTile）：敌 TC 已知 → 走廊
     *  阶梯（钳走廊 60% 内，避免压进敌警戒圈/白送在建塔）；未知 → 东侧 dist 格。 */
    private static int corridorAnchor(int myTc, int enemyTc, int dist) {
        if (enemyTc >= 0) {
            return stanceTile(myTc, enemyTc, Math.min(dist, corridorLen(myTc, enemyTc) * 3 / 5));
        }
        int x = Math.max(1, Math.min(62, (myTc >>> 8) + dist));
        int y = Math.max(1, Math.min(62, myTc & 0xFF));
        return x << 8 | y;
    }

    /** from→to 方向 dist 格处的点（Chebyshev 归一，钳图界内）。 */
    private static int stanceTile(int fromPacked, int toPacked, int dist) {
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

    private static int corridorLen(int myTc, int enemyTc) {
        if (enemyTc < 0) {
            return Integer.MAX_VALUE;
        }
        int dx = (enemyTc >>> 8) - (myTc >>> 8), dy = (enemyTc & 0xFF) - (myTc & 0xFF);
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    // ===== #1 护送关状态 =====
    private int escortPhase;            // 0=清西敌 1=砍隧道 2=护送
    private int escortAnchor = -1;      // 村民口袋质心（首帧记录）
    private boolean escortHdr9;         // 伪交存点已写
    private int escortStall;            // 砍树无进展计数
    private int escortLastFront = -1;   // 上次的前排 x 合计

    /** #1 护送关（res111：胜 = p0 单位#0 静止位于 x[50,57)×y[57,64) 持续 20t；
     *  负 = 任一村民（type<2）死亡。军事死亡合法——res111 解码，m1run2 已验证）。
     *  移植 m1run2.py 配方（宏时代已通关的战术）：
     *  0) 军事清掉口袋西侧固定敌（村民乱漂撞敌=判负，先拔钉子）；
     *  1) 砍隧道：树墙 = 东侧大片木格；每行"前排"= 最西且西邻可走的木格。
     *     只砍 y≥57 的安全行——敌塔 (48,50)(50,50)(49,52) 塔火半径 4 覆盖
     *     北行（y≤56），南行安全。3 村民 1 人 1 行，砍穿 x≥50 为止；
     *  2) 伪交存点 hdr[9]=(前排-3, 58)：根治载满回送 orbit（m1 时代 r35 教训）；
     *  3) 砍穿后全体村民 retask (51,60) 进堡区。
     *  纪律：村民全程不得进 y<57（塔区）；卡死 3 决策（24t）无进展才重发
     *  （频繁重发触发 BFS 离队重算，m1run 的 gather_hammer 教训）。 */
    private void tickEscort(c game) {
        short[] slots = game.playerUnitSlots[0];
        int units = game.playerUnitHeaders[0][2];
        if (units == 0) {
            return;
        }
        // 村民/军事分桶
        int[] vill = new int[26];
        int nv = 0;
        for (int i = 0; i < units; ++i) {
            if ((slots[(i << 3) + 3] & 0xFF) < 2) {
                vill[nv++] = i;
            }
        }
        if (nv == 0) {
            return; // 村民死光=判负在路上
        }
        if (this.escortAnchor < 0) {
            int sx = 0, sy = 0;
            for (int i = 0; i < nv; ++i) {
                int pos = slots[vill[i] << 3] & 0xFFFF;
                sx += pos >>> 8;
                sy += pos & 0xFF;
            }
            this.escortAnchor = (sx / nv) << 8 | (sy / nv);
        }
        int ax = this.escortAnchor >>> 8, ay = this.escortAnchor & 0xFF;

        if (this.escortPhase == 0) {
            // 清口袋 12 格内的敌兵（固定敌在 (15,47)/(16,54) 一带）
            short[] es = game.playerUnitSlots[1];
            int eu = game.playerUnitHeaders[1][2];
            int tgt = -1, best = Integer.MAX_VALUE;
            for (int i = 0; i < eu; ++i) {
                int ep = es[i << 3] & 0xFFFF;
                int d2 = ((ep >>> 8) - ax) * ((ep >>> 8) - ax) + ((ep & 0xFF) - ay) * ((ep & 0xFF) - ay);
                if (d2 <= 144 && d2 < best) {
                    best = d2;
                    tgt = ep;
                }
            }
            if (tgt < 0) {
                this.escortPhase = 1;
                System.out.println("[cai] escort phase1 CHOP t=" + game.tickCount);
                return;
            }
            this.m0Target = tgt;
            this.orderMilitary(game, tgt, false);
            // 村民原地不动（钉在口袋里最安全）
            return;
        }

        if (this.escortPhase == 1) {
            // 扫树墙前排：每行 y 最西的、西邻可走的木格（只收 y≥57 安全行）。
            // 可走 = (t & 0xFFF) == 0（引擎 stepUnitMove 的判据：低 12 位全 0，
            // 虚空/废墟 0x0 与雾 0x8000 都可走）。单位占位（0x2xx）**不算**墙——
            // v2 把它当前排实锤翻车：军事单位停在西侧走廊，被误判成"墙的前排"
            // (28,58)，村民对着自己人脚下砍了 8M tick。
            int[] frontX = new int[64];
            java.util.Arrays.fill(frontX, -1);
            boolean breached = true;
            for (int y = 57; y < 63; ++y) {
                for (int x = 20; x < 55; ++x) {
                    int t = game.mapTiles[x + (y << 6)] & 0xFFF;
                    if ((t & 0x300) == 0x300 && (t & 3) == 1) {
                        if (x < 50) {
                            breached = false; // x<50 还有树=隧道没通
                        }
                        if (frontX[y] < 0 && (game.mapTiles[(x - 1) + (y << 6)] & 0xFFF) == 0) {
                            frontX[y] = x;
                        }
                    }
                }
            }
            if (breached) {
                // 隧道区里站着的单位会盖住格下的树（单位占位盖掉资源显示）。
                // 精确豁免：正在采集/回送（任务字 2/3）且 slot[5]（在采资源格）
                // 还在 x<50 隧道区内的村民。路过的/袋心闲置的不算。
                for (int vi = 0; vi < nv; ++vi) {
                    int o = vill[vi] << 3;
                    int nibble = slots[o + 7] & 0xF;
                    if (nibble != 2 && nibble != 3) {
                        continue;
                    }
                    int rt = slots[o + 5] & 0xFFFF;
                    int rx = rt >>> 8, ry = rt & 0xFF;
                    if (rx >= 20 && rx < 50 && ry >= 57 && ry < 63) {
                        breached = false;
                        break;
                    }
                }
            }
            if (breached) {
                this.escortPhase = 2; // 安全行 x<50 已没有树=墙穿了
                System.out.println("[cai] escort phase2 ESCORT t=" + game.tickCount);
                return;
            }
            // 伪交存点：前排 -3（只写一次；hdr[9]=伐木交存指针，伪造到袋内空地
            // = 载满回送变"走到袋心闲置"，绕开不可达 TC 的回送 orbit，m1 宏线
            // 三连 LOSS 的根因修复。本关不需要真实入账）
            int frontSum = 0, rows = 0, minFront = 99;
            for (int y = 57; y < 63; ++y) {
                if (frontX[y] >= 0) {
                    frontSum += frontX[y];
                    ++rows;
                    minFront = Math.min(minFront, frontX[y]);
                }
            }
            // rows==0 = 前排全被站在树上的村民遮住（单位占位盖住木格），不是
            // 墙穿——穿墙判定只看上面的 breached。hiddenByChopper 同理挡住误判：
            // 隧道区(x<50, y57..62)里站着我方单位 = 格子下可能还压着树。
            if (!this.escortHdr9) {
                game.playerUnitHeaders[0][9] = (Math.max(20, minFront - 3) << 8) | 58;
                this.escortHdr9 = true;
                System.out.println("[cai] escort hdr9=" + Math.max(20, minFront - 3) + ",58 t=" + game.tickCount);
            }
            // 军事撤出作业走廊：清完西敌后在 (24,54) 蹲守（塔火半径外），
            // 别站在隧道口挡村民（v2 尸检：骑兵停在 (28,58) 把前排堵死）。
            this.orderMilitary(game, (24 << 8) | 54, false);
            // 围栏（m1run2 fence 的移植）：村民出 x[20,52)×y[57,64) 立刻拉回
            // 袋心——v3 尸检：村民砍完树被引擎"邻格同类续采"链条带进西北林区
            // 漫游，在 (19,45) 撞上北部敌兵，村民死亡=判负（5 局同点同刻）。
            // y=56 也不行：敌塔 (49,52) 索敌半径²=16 恰好覆盖 (49,56)。
            int fenceHome = (Math.max(20, minFront - 3) << 8) | 58;
            for (int vi = 0; vi < nv; ++vi) {
                int o = vill[vi] << 3;
                int pos = slots[o] & 0xFFFF;
                int px = pos >>> 8, py = pos & 0xFF;
                if (px < 20 || px >= 52 || py < 57 || py >= 64) {
                    if ((slots[o + 2] & 0xFFFF) != fenceHome) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) fenceHome;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                        System.out.println("[cai] escort fence pull v" + vi
                            + " from " + px + "," + py + " t=" + game.tickCount);
                    }
                }
            }
            // 卡死检测：前排 50 决策（400t）无进展才允许重发——采集一载计时
            // 102t（tickUnits case 2 高字节 0x66 倒数），v1 用 3 决策（24t）每
            // 24t 清零一次装载计时，永远砍不倒一棵树（8M tick 僵局尸检实锤）。
            if (frontSum == this.escortLastFront) {
                ++this.escortStall;
            } else {
                this.escortStall = 0;
                this.escortLastFront = frontSum;
            }
            boolean reissue = this.escortStall >= 50;
            // 每村民分配一行（槽序轮转行号，确定性）
            int[] rowList = new int[6];
            int nr = 0;
            for (int y = 57; y < 63; ++y) {
                if (frontX[y] >= 0 && frontX[y] < 50) {
                    rowList[nr++] = y;
                }
            }
            for (int vi = 0; vi < nv && nr > 0; ++vi) {
                int o = vill[vi] << 3;
                int nibble = slots[o + 7] & 0xF;
                if (nibble == 2 || nibble == 3) {
                    continue; // 采集/回送中绝不打断（102t 装载周期内写 slot[7]=0 = 清零）
                }
                int y = rowList[vi % nr];
                int tile = frontX[y] << 8 | y;
                boolean idle = nibble == 0
                    && (slots[o + 0] & 0xFFFF) == (slots[o + 2] & 0xFFFF);
                // 闲置（含回送到伪交存点后的落地闲置）→ 派往当前前排；
                // 卡死步行者（目标过期）也重派。同格 retask 是 no-op，靠换行重踏入。
                if (idle || (slots[o + 2] & 0xFFFF) != tile) {
                    if (!idle && !reissue) {
                        continue; // 行军中且未到重发阈值：让它走
                    }
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) tile;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                }
            }
            if (reissue) {
                this.escortStall = 0;
            }
            this.m0Target = (minFront << 8) | 58;
            return;
        }

        // phase 2: 护送全体进堡区 (51,60)
        this.m0Target = 51 << 8 | 60;
        for (int vi = 0; vi < nv; ++vi) {
            int o = vill[vi] << 3;
            int pos = slots[o] & 0xFFFF;
            int px = pos >>> 8, py = pos & 0xFF;
            if (px >= 50 && px < 57 && py >= 57) {
                continue; // 已进堡区
            }
            int tile = 51 << 8 | 60;
            if ((slots[o + 7] & 0xF) == 1) {
                continue;
            }
            boolean idle = (slots[o + 7] & 0xF) == 0
                && (slots[o + 0] & 0xFFFF) == (slots[o + 2] & 0xFFFF);
            if ((slots[o + 2] & 0xFFFF) != tile || idle) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) tile;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            }
        }
    }

    /** 撤退点：锚点周围按槽位散开的 3×3（避免同点撤退互相占位——站桩回血
     *  要求 pos==tgt，到不了靶心格就永远不回血）。 */
    private int retreatTile(int i) {
        int ax = this.razeAnchor >>> 8, ay = this.razeAnchor & 0xFF;
        int x = Math.max(1, Math.min(62, ax + (i % 3) - 1));
        int y = Math.max(1, Math.min(62, ay + (i / 3) % 3 - 1));
        return x << 8 | y;
    }

    // ===== #4 科技冲刺关 =====

    /** #4（res114：胜 = 升城堡时代（tf[14]=1）→ 放置大学（tf[14]=0，c.java:7427
     *  放置清可建标记）→ 50t → 胜。败 = 通用规则（TC 毁/全灭）。
     *  唯一敌方开 AI 的关（Easy 档：采集 ×1、攻击阈值 30、训练间隔 200），
     *  敌 TC(5,26) 真运营，我方 TC(43,57)+2 村民 10/10/10 白手起家。
     *  打法 = 压缩版经济链：房屋→兵营→（封建）→塔防→磨坊→铁匠铺→（城堡）→
     *  大学放下即胜。石是瓶颈（链上共需 ~100 石），村民按缺口最大种类分派。
     *  原语全部照抄 RuleBasedAi  proven 用法（findAiBuildSpot/a/tryResearch/
     *  queueUnitTraining；放下自动成型 ~32t，不需村民施工）。
     *  v12 实验记录（委托 RuleBasedAi）：0/5 且死得更快（~6400）——RB 在全雾
     *  战役图里探图停滞（aiFog=0 后经济也起不来：2 村民开局比随机图穷太多），
     *  且 RB 不知道大学胜利链。结论：委托路线否决，回手写 handler。
     *  RB 本体的兑现方式改为"抄教训"：塔先立、逃命不失业、配额阻尼。 */
    private void tickCastleRace(c game) {
        int[] hdr = game.playerUnitHeaders[0];
        int[] recs = game.buildingTable[0];
        int bc = hdr[4];
        short[] slots = game.playerUnitSlots[0];
        int units = hdr[2];
        // 我方 TC
        int tc = -1, tcSlot = -1;
        int houseN = 0, barracksDone = 0, millDone = 0, smithDone = 0, towerN = 0, uc = 0;
        int lumberN = 0, miningN = 0;
        int lumberSlot = -1, miningSlot = -1, towerSlot = -1;
        for (int i = 0; i < bc; ++i) {
            int o = i << 2;
            int type = recs[o + 3] & 0xFF;
            boolean done = (recs[o + 2] & 0xFF) == 255 && (recs[o + 2] & 0x40000000) == 0;
            if ((recs[o + 2] & 0x40000000) != 0) {
                ++uc;
            }
            if (type == 9) {
                tc = ((recs[o] >> 8) & 0x3F) << 8 | (recs[o] & 0x3F);
                tcSlot = i;
            } else if (type == 11) {
                ++houseN;
            } else if (type == 10 && done) {
                ++barracksDone;
            } else if (type == 5 && done) {
                ++millDone;
            } else if (type == 6 && done) {
                ++smithDone;
            } else if (type == 12 && done) {
                ++towerN;
                if (towerSlot < 0) {
                    towerSlot = i;
                }
            } else if (type == 0) {
                ++lumberN;
                if (done && lumberSlot < 0) {
                    lumberSlot = i;
                }
            } else if (type == 1) {
                ++miningN;
                if (done && miningSlot < 0) {
                    miningSlot = i;
                }
            }
        }
        if (tc < 0) {
            return; // TC 没了=败局在路上
        }
        int tx = tc >>> 8, ty = tc & 0xFF;
        int age = hdr[0];
        // 敌 TC（走廊回避的线段另一端）
        int etc = -1;
        int[] er = game.buildingTable[1];
        for (int i = 0; i < game.playerUnitHeaders[1][4]; ++i) {
            if ((er[(i << 2) + 3] & 0xFF) == 9) {
                etc = ((er[i << 2] >> 8) & 0x3F) << 8 | (er[i << 2] & 0x3F);
                break;
            }
        }
        // —— 防御：敌兵进 TC 12 格 → 全军压上；村民逃命 ——
        short[] es = game.playerUnitSlots[1];
        int eu = game.playerUnitHeaders[1][2];
        int invader = -1, invD2 = Integer.MAX_VALUE;
        for (int i = 0; i < eu; ++i) {
            int ep = es[i << 3] & 0xFFFF;
            int d2 = ((ep >>> 8) - tx) * ((ep >>> 8) - tx) + ((ep & 0xFF) - ty) * ((ep & 0xFF) - ty);
            if (d2 <= 144 && d2 < invD2) {
                invD2 = d2;
                invader = ep;
            }
        }
        if (invader >= 0) {
            this.m0Target = invader;
            this.orderMilitary(game, invader, false);
        }
        // 村民逃命：敌兵贴身 9 格（TC 保卫战里的入侵者，或路过采集点的散兵）→
        // 撤到 TC 背敌侧。v1 只在 TC 被围时逃，远征矿工被路过敌军白砍（game1
        // 尸检：(36,48) 采石村民被割）。
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            if ((slots[o + 3] & 0xFF) >= 2) {
                continue;
            }
            int pos = slots[o] & 0xFFFF;
            int px = pos >>> 8, py = pos & 0xFF;
            int nearE = -1, nearD2 = 81;
            for (int j = 0; j < eu; ++j) {
                int ep = es[j << 3] & 0xFFFF;
                int d2 = ((ep >>> 8) - px) * ((ep >>> 8) - px) + ((ep & 0xFF) - py) * ((ep & 0xFF) - py);
                if (d2 < nearD2) {
                    nearD2 = d2;
                    nearE = ep;
                }
            }
            if (nearE < 0) {
                continue;
            }
            int exx = nearE >>> 8, eyy = nearE & 0xFF;
            int fx = tx + Integer.signum(tx - exx) * 3;
            int fy = ty + Integer.signum(ty - eyy) * 3;
            fx = Math.max(1, Math.min(62, fx));
            fy = Math.max(1, Math.min(62, fy));
            int flee = fx << 8 | fy;
            if ((slots[o + 2] & 0xFFFF) != flee) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) flee;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                ++this.fleeCount; // 逃命遥测：进 500t 摘要行（收入崩盘的早期信号）
            }
        }
        if (invader >= 0) {
            // TC 战中只补塔（战中补塔=255HP 仇恨海绵，敌军索敌优先打塔，
            // RuleBasedAi 同款纪律），科研/训练/再平衡停摆。
            if (uc == 0 && towerN < 2 && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {
                int anchor = corridorAnchor(tc, etc, towerN == 0 ? 4 : 6);
                int spot = game.findAiBuildSpot(anchor);
                int bx = spot >>> 8, by = spot & 0xFF;
                if (bx < 64 && by < 64 && (game.mapTiles[bx + (by << 6)] & 0xFFF) == 0) {
                    game.a(0, 12, bx, by, 0x40000000, true);
                    System.out.println("[cai] build type=12 (threat) at " + bx + "," + by
                        + " t=" + game.tickCount);
                }
            }
            return; // TC 战中不搞科研/训练/再平衡
        }
        // —— 科技：封建（兵营前置）→ 城堡（磨坊+铁匠） ——
        if (age == 0 && barracksDone > 0 && game.canAfford(0, 2, 21)) {
            if (game.tryResearch(0, tcSlot, 21)) {
                System.out.println("[cai] research FEUDAL t=" + game.tickCount);
            }
        }
        if (age == 1 && millDone > 0 && smithDone > 0 && game.canAfford(0, 2, 22)) {
            if (game.tryResearch(0, tcSlot, 22)) {
                System.out.println("[cai] research CASTLE t=" + game.tickCount);
            }
        }
        // 采集科技 + WatchTower（v7 移植 RuleBasedAi）：竞速链优先（上面两条），
        // 富余再投产能科技——GM +3金/载、DBA +5木/载、WT 塔甲 10→15 都是长跑正收益。
        if (age >= 1) {
            if (miningSlot >= 0 && game.canAfford(0, 2, 5) && game.tryResearch(0, miningSlot, 5)) {
                System.out.println("[cai] research GoldMining t=" + game.tickCount);
            }
            if (lumberSlot >= 0 && game.canAfford(0, 2, 3) && game.tryResearch(0, lumberSlot, 3)) {
                System.out.println("[cai] research DoubleBitAxe t=" + game.tickCount);
            }
            if (towerSlot >= 0 && game.canAfford(0, 2, 13) && game.tryResearch(0, towerSlot, 13)) {
                System.out.println("[cai] research WatchTower t=" + game.tickCount);
            }
            if (miningSlot >= 0 && towerN < 2 && game.canAfford(0, 2, 9)
                    && game.tryResearch(0, miningSlot, 9)) {
                System.out.println("[cai] research StoneMining t=" + game.tickCount);
            }
        }
        // —— 建造链（一次一座，放下自动成型）——
        // v2 教训：塔必须在敌首波（~3500t）前立起来——塔不吃人口、攻 32/甲，
        // 是唯一来得及的防御。
        // v7 移植 RuleBasedAi 全套经济：伐木/采矿场贴资源（交存点砍半跑路），
        // 塔改走廊锚点（朝敌 TC 阶梯，钳走廊 60% 内）。
        // v8-v16 迭代史（塔先于兵营/三塔/走廊回避采集/全免门出兵 全部 0/5，
        // 全部回滚）：敌有免费资源滴（1000）+ 连续 raid，2 村民开局的收入
        // 经不起任何"等"——v7 的均衡链（房→伐木→兵营→塔1→采矿→塔2→磨坊→
        // 铁匠铺→大学）是实测最优（2/5），任何单边倾斜都让某条资源线断供。
        // 关卡天花板待破：胜需 ~11k tick 跑完竞速，敌 ~8k 起的大波需要第三塔
        // 或机动兵力，但两者的花费都会拖垮竞速——见 WORKLOG 2026-09-04。
        boolean popPressure = hdr[2] + hdr[49] >= hdr[3] - 1;
        int need = -1, anchor = tc;
        if (houseN == 0 && hdr[5] >= 15) {
            need = 11;                                   // 房屋：人口 + 产村民
        } else if (lumberN == 0 && uc == 0 && hdr[5] >= 20
                && this.nearestSafeResource(game, tc, 1, es, eu) >= 0) {
            need = 0;                                    // 伐木场：贴着最近木
            anchor = this.nearestSafeResource(game, tc, 1, es, eu);
        } else if (barracksDone == 0 && uc == 0 && hdr[5] >= 20 && hdr[7] >= 10) {
            need = 10;                                   // 兵营：封建前置 + 剑士
        } else if (barracksDone > 0 && towerN == 0 && uc == 0 && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {
            need = 12;                                   // 走廊塔 1：抢在敌首波前
            anchor = corridorAnchor(tc, etc, 4);
        } else if (miningN == 0 && uc == 0 && hdr[5] >= 20
                && this.nearestSafeResource(game, tc, 2, es, eu) >= 0) {
            need = 1;                                    // 采矿场：贴着最近金
            anchor = this.nearestSafeResource(game, tc, 2, es, eu);
        } else if (barracksDone > 0 && towerN == 1 && uc == 0 && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {
            need = 12;                                   // 走廊塔 2：双塔再攀科技
            anchor = corridorAnchor(tc, etc, 6);
        } else if (age >= 1 && millDone == 0 && uc == 0 && hdr[5] >= 15 && hdr[7] >= 10) {
            need = 5;                                    // 磨坊：城堡前置 1/2
        } else if (age >= 1 && smithDone == 0 && uc == 0 && hdr[5] >= 25 && hdr[7] >= 20) {
            need = 6;                                    // 铁匠铺：城堡前置 2/2
        } else if (age >= 2 && game.techFlags[14] != 0 && uc == 0 && hdr[5] >= 25 && hdr[7] >= 25) {
            need = 4;                                    // 大学：放下即点亮胜利链
        } else if (popPressure && houseN < 3 && uc == 0 && hdr[5] >= 15) {
            need = 11;
        }
        if (need >= 0 && anchor >= 0) {
            int spot = game.findAiBuildSpot(anchor);
            int bx = spot >>> 8, by = spot & 0xFF;
            if (bx < 64 && by < 64 && (game.mapTiles[bx + (by << 6)] & 0xFFF) == 0) {
                int rc = game.a(0, need, bx, by, 0x40000000, true);
                System.out.println("[cai] build type=" + need + " at " + bx + "," + by
                    + " rc=" + rc + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                    + " t=" + game.tickCount);
            }
        }
        // —— 训练：村民补到 4（房屋产），剑士若干守家 ——
        boolean popRoom = hdr[2] + hdr[49] < hdr[3] && hdr[2] + hdr[49] < 26;
        int vills = 0, milCount = 0;
        int houseSlot = -1, barracksSlot = -1;
        for (int i = 0; i < units; ++i) {
            if ((slots[(i << 3) + 3] & 0xFF) < 2) {
                ++vills;
            } else {
                ++milCount;
            }
        }
        for (int i = 0; i < bc; ++i) {
            int o = i << 2;
            int type = recs[o + 3] & 0xFF;
            boolean done = (recs[o + 2] & 0xFF) == 255 && (recs[o + 2] & 0x40000000) == 0;
            if (type == 11 && done && houseSlot < 0) {
                houseSlot = i;
            } else if (type == 10 && done && barracksSlot < 0) {
                barracksSlot = i;
            }
        }
        if (popRoom && vills + hdr[66] < 4 && hdr[57] + hdr[66] < hdr[75]
                && houseSlot >= 0 && game.canAfford(0, 0, 0)) {
            game.queueUnitTraining(0, 0);
        }
        // 守家兵种：军费让位科技链——城堡时代前只用便宜长枪（5/5 vs 剑士
        // 5/10，省的金全进封建/城堡 40G），帽 2；城后再补剑士到 4。防御主力
        // 是双走廊塔（不吃人口），兵只是补刀。W12/G12 附加门防军费挤塔料
        // （v13 全免门开局即训，10G 正好挤死塔 2 金门——0/5 实锤回滚）。
        int meleeType = age >= 2 ? 3 : 2;
        int meleeCap = age >= 2 ? 4 : 2;
        if (popRoom && barracksSlot >= 0 && milCount < meleeCap
                && hdr[5] >= 12 && hdr[6] >= 12 && game.canAfford(0, 0, meleeType)) {
            game.queueUnitTraining(0, meleeType);
        }
        // —— 村民采集：分阶段配额（v1 用"总量缺口"全堆木，石启动太晚被一波带走）。
        //    兵营前 3木1石（攒兵营料）；兵营后 1木1金2石（石是链上瓶颈 ≈100）。
        //    v9/v14 三段制实验（按塔进度分段）实测不优回滚（见建造链注释迭代史）。
        //    配额阻尼收敛（+1 缓冲，一次决策最多换 1 人，RuleBasedAi 震荡环教训）。
        int[] quota = barracksDone > 0 ? new int[]{0, 1, 1, 2} : new int[]{0, 3, 0, 1};
        int[] have = new int[4];
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            if ((slots[o + 3] & 0xFF) >= 2) {
                continue;
            }
            int nibble = slots[o + 7] & 0xF;
            int kind = 0;
            if (nibble == 2 || nibble == 3) {
                kind = (slots[o + 7] & 0xF0) >> 4;
            } else {
                int tgt = slots[o + 2] & 0xFFFF;
                int tt = game.mapTiles[(tgt >>> 8) + ((tgt & 0xFF) << 6)] & 0xFFF;
                if ((tt & 0x300) == 0x300) {
                    kind = tt & 3;
                }
            }
            if (kind >= 1 && kind <= 3) {
                ++have[kind];
            }
        }
        int swapped = 0;
        for (int i = 0; i < units && swapped < 1; ++i) {
            int o = i << 3;
            if ((slots[o + 3] & 0xFF) >= 2) {
                continue;
            }
            boolean idle = (slots[o + 7] & 0xF) == 0
                && (slots[o + 0] & 0xFFFF) == (slots[o + 2] & 0xFFFF);
            if (!idle) {
                continue; // 采集循环引擎全自动
            }
            int pos = slots[o] & 0xFFFF;
            // 缺口最大的种类
            int kind = 0, gap = 0;
            for (int k = 1; k <= 3; ++k) {
                if (quota[k] - have[k] > gap) {
                    gap = quota[k] - have[k];
                    kind = k;
                }
            }
            if (kind == 0) {
                break; // 配额满
            }
            int tile = this.nearestSafeResourceOffCorridor(game, pos, kind, es, eu, -1, -1);
            if (tile < 0) {
                break;
            }
            slots[o + 1] = slots[o + 0];
            slots[o + 2] = (short) tile;
            slots[o + 7] = 0;
            slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            ++have[kind];
            ++swapped;
            System.out.println("[cai] assign villager " + i + " kind=" + kind
                + " -> " + (tile >>> 8) + "," + (tile & 0xFF) + " t=" + game.tickCount);
        }
        // 主动再平衡（阻尼：一次决策最多换 1 人，RuleBasedAi 震荡环教训）：
        // 在岗村民不换工种会把链卡死——v1 兵营完工后配额切 1木1金2石，但 3 个
        // 木工已在岗无人换金，封建 15 金永远凑不齐（game1 尸检：5751t 0 科研）。
        if (swapped == 0) {
            int over = 0, under = 0;
            for (int k = 1; k <= 3; ++k) {
                if (have[k] > quota[k]) {
                    over = k;
                }
                if (have[k] < quota[k]) {
                    under = k;
                }
            }
            if (over != 0 && under != 0) {
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    if ((slots[o + 3] & 0xFF) >= 2) {
                        continue;
                    }
                    int nibble = slots[o + 7] & 0xF;
                    int kind = 0;
                    if (nibble == 2 || nibble == 3) {
                        kind = (slots[o + 7] & 0xF0) >> 4;
                    } else {
                        int tgt = slots[o + 2] & 0xFFFF;
                        int tt = game.mapTiles[(tgt >>> 8) + ((tgt & 0xFF) << 6)] & 0xFFF;
                        if ((tt & 0x300) == 0x300) {
                            kind = tt & 3;
                        }
                    }
                    if (kind != over) {
                        continue;
                    }
                    int pos = slots[o] & 0xFFFF;
                    int tile = this.nearestSafeResourceOffCorridor(game, pos, under, es, eu, -1, -1);
                    if (tile < 0) {
                        break;
                    }
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) tile;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    System.out.println("[cai] rebalance villager " + i + " kind=" + over
                        + "->" + under + " t=" + game.tickCount);
                    break;
                }
            }
        }
    }

    /** #6 总攻关（res116：胜 = 拆光敌 13 建筑；敌 Keep 塔环 + 19 守军）。
     *  v4 教训：全体 all-in 被塔收 / 攻城组 solo 塔被守军收 / 单位分批到达=添油。
     *  v5 阶段机（集结→野战→攻城）：野战赢则攻城输在塔火力刀锋局。
     *  v6 攻城细化：轮换 165/250、塔周守军清场、孤立塔全员进场——但批测 0/3，
     *  尸检定位真根因=野战兵力方差（4 局野战存活 10/2/2/7，攻城器赔光即判死）。
     *  v7 废除野战：守军不追击（aiEnabled=false，只经 resolveAttack 报复回咬），
     *  中场守军永远不会加入塔区战斗——集结完直接拔塔，塔周 8 格守军由 defTarget
     *  就地集火清算。机制依据（字节码实读）：
     *  - 塔瞄准=槽序扫描（aimProjectiles 取射程内最低 slot），伤害 64/甲 per 17t，
     *    索敌 6 格（我方攻城器程 4，进场必挨打）；
     *  - 站桩回血 +1/2t（tickUnits case0：闲+pos==tgt+(tickCount&8)!=0，HP<255）；
     *  - 单位战斗态不追人（tickUnits case1 只对 slot[5] 格原地开火）；
     *  - 被打会报复（resolveAttack 把受害者 tgt 改写成攻击者位置）。
     *  攻城阶段：孤立塔（10 格内无第二塔）全员进场（近战 slot 低=天然肉盾+可观
     *  拆塔 DPS）；集群塔近战 8 格外戒备只留攻城组轮换（HP<165 撤/250 归队）；
     *  塔全灭后近战进场拆软建筑。 */
    private int faPhase;                // 0=集结 2=攻城/拆家（v7 废除野战阶段 1）
    private int faRally = -1;           // 集结点（=出生质心）
    private int lastFa2Log;             // fa2 快照节流

    private void tickFinalAssault(c game) {
        short[] slots = game.playerUnitSlots[0];
        int units = game.playerUnitHeaders[0][2];
        if (units == 0) {
            return;
        }
        if (this.razeAnchor < 0) {
            int sx = 0, sy = 0;
            for (int i = 0; i < units; ++i) {
                int pos = slots[i << 3] & 0xFFFF;
                sx += pos >>> 8;
                sy += pos & 0xFF;
            }
            this.razeAnchor = (sx / units) << 8 | (sy / units);
            this.faRally = this.razeAnchor;
        }
        int ax = this.razeAnchor >>> 8, ay = this.razeAnchor & 0xFF;
        short[] es = game.playerUnitSlots[1];
        int eu = game.playerUnitHeaders[1][2];
        int[] eb = game.buildingTable[1];
        int ebCount = game.playerUnitHeaders[1][4];
        if (ebCount == 0) {
            return; // 胜局在路上
        }

        if (this.faPhase == 0) {
            // 集结：全员到 rally 3×3 散点；2/3 到位（d2≤16）即转攻城
            int arrived = 0;
            for (int i = 0; i < units; ++i) {
                int pos = slots[i << 3] & 0xFFFF;
                int dx = (pos >>> 8) - (this.faRally >>> 8), dy = (pos & 0xFF) - (this.faRally & 0xFF);
                if (dx * dx + dy * dy <= 16) {
                    ++arrived;
                }
            }
            if (arrived * 3 >= units * 2) {
                this.faPhase = 2;
                System.out.println("[cai] fa phase2 SIEGE(direct) t=" + game.tickCount);
            } else {
                for (int i = 0; i < units; ++i) {
                    this.orderUnit(game, i, this.retreatTile(i), false);
                }
                return;
            }
        }

        // v7：野战阶段整体废除。守军不追击（aiEnabled=false，只有被打才经
        // resolveAttack 报复性回咬），中场守军根本不会加入塔区战斗——v6 系列
        // 的野战是用我方攻城器的命去换一堆不会动的靶子（实测 4 局野战存活
        // 10/2/2/7，全看遭遇阵型）。
        // v8 修复 v7 暴露的两个结构性缺陷：
        //  1) 行军路线撞中场投石机堆：行进间被咬住后全员决斗（orderUnit 不打断
        //     接敌单位），添油式减员。→ 反应式威胁：质心 12 格内最近敌兵 =
        //     全军焦点（非接敌单位；接敌的保持决斗保装填），从包边缘一口口吃；
        //  2) 回家回血=永久添油（敌 mangonel 集火 18.7/t，回家往返 700t+）。
        //     → 就地轮换：退到威胁源 7 格外站桩回血（healTile），已脱险原地站。

        // 我方质心
        int cx = 0, cy = 0;
        for (int i = 0; i < units; ++i) {
            int pos = slots[i << 3] & 0xFFFF;
            cx += pos >>> 8;
            cy += pos & 0xFF;
        }
        cx /= units;
        cy /= units;

        int towerTarget = -1, towerD2 = Integer.MAX_VALUE;
        int softTarget = -1, softD2 = Integer.MAX_VALUE;
        for (int i = 0; i < ebCount; ++i) {
            int o = i << 2;
            int bx = (eb[o] >> 8) & 0x3F, by = eb[o] & 0x3F;
            int d2 = (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
            if ((eb[o + 3] & 0xFF) == 12) {
                if (d2 < towerD2) {
                    towerD2 = d2;
                    towerTarget = bx << 8 | by;
                }
            } else if (d2 < softD2) {
                softD2 = d2;
                softTarget = bx << 8 | by;
            }
        }
        int towersNear = 0; // 目标塔 10 格内的第二塔（集群判定；塔程 6 + 余量）
        if (towerTarget >= 0) {
            int ttx = towerTarget >>> 8, tty = towerTarget & 0xFF;
            for (int i = 0; i < ebCount; ++i) {
                int o = i << 2;
                if ((eb[o + 3] & 0xFF) != 12) {
                    continue;
                }
                int bx = (eb[o] >> 8) & 0x3F, by = eb[o] & 0x3F;
                if (bx == ttx && by == tty) {
                    continue;
                }
                int d2 = (bx - ttx) * (bx - ttx) + (by - tty) * (by - tty);
                if (d2 <= 100) {
                    ++towersNear;
                }
            }
        }
        // 反应式威胁：质心 12 格内最近敌兵（塔周守军/行军拦截都在此列）
        int threat = -1, threatD2 = Integer.MAX_VALUE;
        for (int i = 0; i < eu; ++i) {
            int ep = es[i << 3] & 0xFFFF;
            int d2 = ((ep >>> 8) - cx) * ((ep >>> 8) - cx) + ((ep & 0xFF) - cy) * ((ep & 0xFF) - cy);
            if (d2 <= 144 && d2 < threatD2) {
                threatD2 = d2;
                threat = ep;
            }
        }
        this.m0Target = threat >= 0 ? threat : (towerTarget >= 0 ? towerTarget : softTarget);
        boolean meleeJoins = threat < 0 && towerTarget >= 0 && towersNear == 0;
        int threatSrc = threat >= 0 ? threat : towerTarget;
        int cPacked = cx << 8 | cy;
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            int type = slots[o + 3] & 0xFF;
            boolean siege = type == 7 || type == 8 || type == 9;
            int hp = slots[o + 4] & 0xFF;
            boolean healing = this.razeHealing[i];
            int retreatAt = siege ? 165 : 120;
            if (!healing && hp < retreatAt) {
                this.razeHealing[i] = true;
                healing = true;
            } else if (healing && hp >= 250) {
                this.razeHealing[i] = false;
                healing = false;
            }
            int tgt;
            if (healing) {
                tgt = this.healTile(game, threatSrc, slots[o] & 0xFFFF, i);
            } else if (threat >= 0) {
                // 焦点集火：近战贴上（贴身互锁），攻城器 3 格外站桩齐射（程 4 内）
                tgt = siege ? stanceTile(threat, cPacked, 3) : threat;
            } else if (siege) {
                tgt = towerTarget >= 0 ? towerTarget
                    : (softTarget >= 0 ? softTarget : this.retreatTile(i));
            } else if (towerTarget < 0) {
                tgt = softTarget >= 0 ? softTarget : this.retreatTile(i); // 塔清完了，进场拆
            } else if (meleeJoins) {
                tgt = towerTarget; // 孤立塔：全员进场，近战 slot 低吃塔火
            } else {
                // 集群塔戒备点：塔朝我锚点方向 8 格（塔索敌 6 格之外）
                int bx = towerTarget >>> 8, by = towerTarget & 0xFF;
                int dx = ax - bx, dy = ay - by;
                int m = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
                int gx = Math.max(1, Math.min(62, bx + dx * 8 / m));
                int gy = Math.max(1, Math.min(62, by + dy * 8 / m));
                tgt = gx << 8 | gy;
            }
            this.orderUnit(game, i, tgt, healing);
        }
        // 攻城尸检打点（临时）：每 ~512t 全军槽位快照——塔火力/轮换/卡死定位用。
        if (System.getProperty("aoe.debug") != null && game.tickCount - this.lastFa2Log >= 512) {
            this.lastFa2Log = game.tickCount;
            StringBuilder sb = new StringBuilder("[cai] fa2 t=" + game.tickCount
                + " tower=" + (towerTarget >= 0 ? (towerTarget >>> 8) + "," + (towerTarget & 0xFF) : "-")
                + " near=" + towersNear
                + " threat=" + (threat >= 0 ? (threat >>> 8) + "," + (threat & 0xFF) : "-"));
            for (int i = 0; i < units; ++i) {
                int o = i << 3;
                sb.append(' ').append(i).append(":t").append(slots[o + 3] & 0xFF)
                    .append('@').append(slots[o] >>> 8).append(',').append(slots[o] & 0xFF)
                    .append('>').append((slots[o + 2] & 0xFFFF) >>> 8).append(',')
                    .append((slots[o + 2] & 0xFFFF) & 0xFF)
                    .append(" c>").append((slots[o + 5] & 0xFFFF) >>> 8).append(',')
                    .append((slots[o + 5] & 0xFFFF) & 0xFF)
                    .append(" hp").append(slots[o + 4] & 0xFF)
                    .append(" w").append(Integer.toHexString(slots[o + 7] & 0xFFFF))
                    .append(this.razeHealing[i] ? " HEAL" : "");
            }
            sb.append(" |E|");
            for (int i = 0; i < eu; ++i) {
                int o = i << 3;
                sb.append(' ').append(i).append(":t").append(es[o + 3] & 0xFF)
                    .append('@').append((es[o] & 0xFFFF) >>> 8).append(',').append(es[o] & 0xFF)
                    .append(" hp").append(es[o + 4] & 0xFF)
                    .append(" w").append(Integer.toHexString(es[o + 7] & 0xFFFF));
            }
            System.out.println(sb);
        }
    }

    /** 治疗点（v8 就地轮换）：威胁源 7 格外站桩回血；单位已在 8 格外=原地站；
     *  落点不可走 → 回锚点散点（retreatTile）。回家回血=添油（敌投石机集火
     *  18.7/t 下 700t 往返=编制永远不齐）。 */
    private int healTile(c game, int src, int pos, int i) {
        if (src >= 0) {
            int dx = (pos >>> 8) - (src >>> 8), dy = (pos & 0xFF) - (src & 0xFF);
            if (dx * dx + dy * dy >= 64) {
                return pos; // 已脱险：原地站桩（pos==tgt 才回血）
            }
            int h = stanceTile(src, pos, 7);
            if ((game.mapTiles[(h >>> 8) + ((h & 0xFF) << 6)] & 0xFFF) == 0) {
                return h;
            }
        }
        return this.retreatTile(i);
    }

    /** 单单位下令：接敌（任务字 1）且非撤退不打断；目标相同不写。 */    private void orderUnit(c game, int i, int tgt, boolean force) {
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

    /** 全体军事压向目标（不接敌打断）。 */
    private void orderMilitary(c game, int target, boolean includeEngaged) {        short[] slots = game.playerUnitSlots[0];
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
}
