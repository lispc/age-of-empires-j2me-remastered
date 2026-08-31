package javax.microedition.lcdui;

/**
 * 桌面移植层内部常量：J2ME 逻辑屏幕尺寸与桌面缩放倍数。
 * 原游戏是 240x320 竖屏（Nokia 240x320 机型）。
 */
public final class Screen {
    public static final int WIDTH = 240;
    public static final int HEIGHT = 320;
    public static final int SCALE = computeScale();

    private Screen() {
    }

    /** 缩放倍数：显式 -Daoe.scale=N 优先；否则按屏幕可用区域自适应，
     *  取能完整放下窗口（含标题栏）的最大倍数，上限 3。 */
    private static int computeScale() {
        String prop = System.getProperty("aoe.scale");
        if (prop != null) {
            return Integer.parseInt(prop);
        }
        try {
            java.awt.Rectangle usable = java.awt.GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getMaximumWindowBounds();
            // 减去窗口标题栏（约 30pt 逻辑像素）
            int fit = Math.min(usable.width / WIDTH, (usable.height - 30) / HEIGHT);
            return Math.max(1, Math.min(3, fit));
        } catch (Throwable t) {
            return 3;
        }
    }
}
