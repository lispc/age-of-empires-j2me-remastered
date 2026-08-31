package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * lcdui.game.GameCanvas 的桌面实现。
 * 本移植里 GameCanvas 与 Canvas 共用同一块离屏缓冲，getGraphics() 直接返回它。
 */
public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 2;
    public static final int LEFT_PRESSED = 4;
    public static final int RIGHT_PRESSED = 32;
    public static final int DOWN_PRESSED = 64;
    public static final int FIRE_PRESSED = 256;
    public static final int GAME_A_PRESSED = 512;
    public static final int GAME_B_PRESSED = 1024;
    public static final int GAME_C_PRESSED = 2048;
    public static final int GAME_D_PRESSED = 4096;

    protected GameCanvas(boolean suppressKeyEvents) {
        super();
    }

    /**
     * 桌面端不支持：Canvas 的画面由 paint() 同步产出，没有可长驻取画的离屏缓冲。
     * 本游戏不会走到这里（mad.b 只在非双缓冲设备上才调 getGraphics）。
     */
    public Graphics getGraphics() {
        throw new UnsupportedOperationException("getGraphics on canvas is not supported on desktop");
    }

    public void flushGraphics() {
        repaint();
    }

    public void flushGraphics(int x, int y, int width, int height) {
        repaint();
    }

    public int getKeyStates() {
        return 0;
    }
}
