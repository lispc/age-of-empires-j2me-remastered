package aoe.ai;

import AgeOfEmpires.c;

/**
 * 玩家 AI 接口（移植新增，-Daoe.playerAi=<全限定类名> 装载）。
 *
 * 装载：c 帧首反射 Class.forName + newInstance；装载失败/tick 抛异常打 [ai]
 * 日志并永久禁用，不影响游戏本身。
 *
 * 调用时机：onPaint 帧首（Timer/paint 线程，与模拟同线程），每帧一次，
 * AI 内部自行节流。tick 里可直接读 game 的公开字段
 * （playerUnitHeaders/playerUnitSlots/buildingTable/mapTiles/techFlags/
 * tickCount/screenState/gameMode/相机/光标），写操作走公开原语：
 * orderMove/selectUnits/clearSelection/selectUnderCursor/queueUnitTraining/
 * canAfford/payCost/findAiBuildSpot/findNearbyResource/a(放建筑)/tryResearch。
 *
 * 确定性纪律：tick 属于模拟路径——只许按 tickCount 节流，禁止墙钟/线程序
 * 依赖；**绝不许碰游戏的 nextRandomInt**（那是模拟 RNG，AI 消费它会让
 * 回放/对拍发散）。确需随机时用 AI 自己的 new Random(固定种子)
 * （可用 -Daoe.playerAiSeed=N 传入）；能不用就不用。
 */
public interface PlayerAi {
    void tick(c game);

    /** arena 遥测（第 1 轮 side 偏差诊断，c.java arenaTelemetry 调用）：
     *  返回该 AI 私有迷雾的已探索格数（排海：海格 (tile&0xFFF)==768 引擎
     *  从不置雾，不计入，与 c.java 侧 s0 口径一致）；无私有迷雾（side 0
     *  走引擎雾层，由 c.java 直接统计）返回 -1。默认 -1，非 arena 路径不调用。 */
    default int arenaExploredCount(c game) {
        return -1;
    }
}
