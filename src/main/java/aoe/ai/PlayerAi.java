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
 * （playerUnitHeaders/playerUnitSlots/var_int_arr_arr_b/mapTiles/techFlags/
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
}
