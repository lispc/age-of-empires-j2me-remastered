package aoe;

import AgeOfEmpires.AoeMidlet;
import com.ulysseo.mad.a;

/**
 * dev 测试驱动（headless）：启动游戏 → 直进指定关卡 → 等待/导出画面 → 退出。
 * 用法：
 * <pre>
 * java -Daoe.headless=1 -Daoe.dev=tutorial:1 [-Daoe.tickms=40] \
 *     -cp build/classes/java/main:build/resources/main aoe.DevHarness [输出.png] [额外等待秒]
 * </pre>
 * 依赖 -Daoe.headless=1（无窗口）与 -Daoe.dev=...（自动导航），建议加 -Daoe.tickms 加速。
 */
public final class DevHarness {
    private DevHarness() {
    }

    public static void main(String[] args) throws Exception {
        AoeMidlet midlet = new AoeMidlet();
        midlet.startApp();
        AgeOfEmpires.c game = midlet.game();
        long deadline = System.currentTimeMillis() + 60000;
        boolean inMission = false;
        int stable = 0;
        while (System.currentTimeMillis() < deadline) {
            if (game.screenState == 6) {
                inMission = true;
            }
            if (inMission && game.screenState == 2) {
                game.onKeyPress(-6);        // 推任务内的教程对话框（F1 = 左软键）
                stable = 0;
            } else if (inMission && game.screenState == 6 && ++stable >= 8) {
                break;                  // 主视图稳定 ~2.4s：对话框已推完
            }
            Thread.sleep(300);
        }
        int extra = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        Thread.sleep(extra * 1000L);
        if (args.length > 0) {
            a.var_com_ulysseo_mad_b_a.dumpFramebuffer(args[0]);
            System.out.println("[harness] dumped " + args[0]);
        }
        System.out.println("[harness] done aA=" + game.screenState + " units="
            + game.playerUnitHeaders[0][2]);
        System.exit(0);
    }
}
