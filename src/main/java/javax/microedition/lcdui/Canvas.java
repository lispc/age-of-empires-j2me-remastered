package javax.microedition.lcdui;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * lcdui.Canvas 的桌面实现。
 * 渲染模型：游戏画进一块设备分辨率的持久帧缓冲（240x320 × aoe.scale × 设备像素倍数），
 * serviceRepaints 在游戏线程里加锁画完，再异步让 Swing 把缓冲 1:1 贴上窗口。
 * 持久缓冲是游戏逻辑的硬性依赖：它按脏矩形局部重绘（比如主菜单背景只在进入时画一次），
 * 任何"每帧从零重画"的管线都会丢内容。缓冲建在设备分辨率上是为了画质：
 * Graphics 的基准变换 = SCALE × 设备倍数，文字矢量渲染落在物理像素上保持清晰；
 * 图片素材经 Image.ASSET_SCALE 预放大后同样 1:1 上屏，保持像素风。
 */
public abstract class Canvas extends Displayable {
    // 标准键码（Nokia 风格，游戏内部按这套约定判断）
    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    public static final int UP = -1;
    public static final int DOWN = -2;
    public static final int LEFT = -3;
    public static final int RIGHT = -4;
    public static final int FIRE = -5;
    public static final int SOFT_LEFT = -6;
    public static final int SOFT_RIGHT = -7;

    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    /** 帧缓冲锁：游戏线程绘制与 EDT 贴图互斥。 */
    private final Object bufferLock = new Object();
    private java.awt.image.BufferedImage framebuffer;
    /** framebuffer 建立时的设备像素倍数（Retina=2），用于检测显示配置变化。 */
    private double framebufferDeviceScale;
    /** 最近一次 paintComponent 观察到的设备倍数；首帧前由 probeDeviceScale 兜底。 */
    private volatile double observedDeviceScale = 1;
    private long lastDumpMs;
    /**
     * 待投递的松开事件（J2ME 键码）。桌面上快速点按会在一个 tick（80ms）内完成
     * 按下+松开，若松开立即送达游戏，按下动作还没被游戏 tick 消费就被清零，
     * 点按会被吞掉；因此松开先排队，等 paint（即游戏消费完这一帧）之后再投递。
     * EDT 写、游戏线程读，需持锁。
     */
    private final java.util.ArrayList<Integer> pendingKeyReleases = new java.util.ArrayList<>();

    protected Canvas() {
        this.panel = new CanvasPanel(this);
    }

    protected abstract void paint(Graphics g);

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }

    protected void sizeChanged(int w, int h) {
    }

    @Override
    void notifyShown() {
        showNotify();
    }

    @Override
    void notifyHidden() {
        hideNotify();
    }

    /**
     * MIDP 语义里 repaint() 只是预约一帧，真正的同步绘制由 serviceRepaints 完成。
     * 游戏主循环总是成对调用两者，这里把 repaint 做成空操作，避免一帧画两遍。
     */
    public void repaint() {
    }

    public void repaint(int x, int y, int width, int height) {
    }

    /**
     * MIDP 语义：强制同步完成一次 paint。游戏主循环（Timer 线程）每帧都调它：
     * 在游戏线程直接画进帧缓冲（加锁），再异步请求 Swing 贴图——
     * 不再把绘制踢回 EDT，EDT 只做一次 1:1 位块搬运。
     */
    public void serviceRepaints() {
        synchronized (bufferLock) {
            ensureFramebuffer(probeDeviceScale());
            java.awt.Graphics2D g2 = framebuffer.createGraphics();
            try {
                paint(new Graphics(g2, Screen.SCALE * framebufferDeviceScale,
                        Screen.WIDTH, Screen.HEIGHT));
            } finally {
                g2.dispose();
            }
            dumpFrame();
        }
        flushPendingKeyReleases();
        panel.repaint();
    }

    /** 在游戏消费完一帧（paint 返回）后，把排队的松开事件按序送达游戏。 */
    private void flushPendingKeyReleases() {
        Integer[] codes;
        synchronized (pendingKeyReleases) {
            if (pendingKeyReleases.isEmpty()) {
                return;
            }
            codes = pendingKeyReleases.toArray(new Integer[0]);
            pendingKeyReleases.clear();
        }
        for (int code : codes) {
            keyReleased(code);
        }
    }

    /**
     * 桌面鼠标钩子：J2ME 本无鼠标，为移植增强新增。kind: 0=移动/悬停 1=左按下
     * 2=左抬起 3=右按下；(x,y) 已换算为 240x320 逻辑像素。默认无操作。
     */
    protected void mouseA(int kind, int x, int y) {
    }

    /** 桌面鼠标增强：合成按键的延迟松开（复用 pendingKeyReleases 防吞点按机制）。 */
    public void queueSyntheticKeyRelease(int keyCode) {
        synchronized (pendingKeyReleases) {
            pendingKeyReleases.add(keyCode);
        }
    }

    /** dev 模式：把当前帧缓冲导出为 PNG（同步，headless 测试用）。 */
    public void dumpFramebuffer(String path) {
        synchronized (bufferLock) {
            if (framebuffer == null) {
                return;
            }
            try {
                javax.imageio.ImageIO.write(framebuffer, "png", new java.io.File(path));
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** 取得（必要时按当前设备倍数重建）设备分辨率的帧缓冲。重建后通知游戏全量重绘。 */
    private void ensureFramebuffer(double deviceScale) {
        if (framebuffer != null && framebufferDeviceScale == deviceScale) {
            return;
        }
        int pw = Math.round(Screen.WIDTH * Screen.SCALE * (float) deviceScale);
        int ph = Math.round(Screen.HEIGHT * Screen.SCALE * (float) deviceScale);
        framebuffer = new java.awt.image.BufferedImage(pw, ph,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        framebufferDeviceScale = deviceScale;
        // 新缓冲是空的：showNotify → 游戏 k() 置位全量重绘标志，下一帧补全画面
        notifyShown();
    }

    /** 窗口未显示前先借 GraphicsConfiguration 探测设备倍数，避免首帧建成 1x 随后又重建。 */
    private double probeDeviceScale() {
        if (observedDeviceScale > 0) {
            return observedDeviceScale;
        }
        java.awt.GraphicsConfiguration gc = panel.getGraphicsConfiguration();
        if (gc != null) {
            double s = gc.getDefaultTransform().getScaleX();
            if (s > 0) {
                return s;
            }
        }
        return 1.0;
    }

    /** 调试：-Daoe.dumpFrames=/path.png 时每 ~5 秒把当前帧缓冲导出为 PNG。 */
    private void dumpFrame() {
        String path = System.getProperty("aoe.dumpFrames");
        if (path == null || System.currentTimeMillis() - lastDumpMs < 5000) {
            return;
        }
        lastDumpMs = System.currentTimeMillis();
        try {
            javax.imageio.ImageIO.write(framebuffer, "png", new java.io.File(path));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public boolean hasPointerEvents() {
        return false;
    }

    public boolean hasPointerMotionEvents() {
        return false;
    }

    public void setFullScreenMode(boolean mode) {
        // 桌面端忽略：窗口固定为逻辑分辨率的整数倍
    }

    public String getKeyName(int keyCode) {
        return "key " + keyCode;
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case UP:
                return 1;
            case LEFT:
                return 2;
            case RIGHT:
                return 5;
            case DOWN:
                return 6;
            case FIRE:
                return 8;
            default:
                return 0;
        }
    }

    /** AWT 键事件 → J2ME 键码（Nokia 风格负值系统键 + ITU-T 数字键）。
     *  WASD 是方向键的桌面别名，X 是确认键（对应数字 5），方便单手操作。 */
    static int mapKeyCode(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return RIGHT;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_X:
                return FIRE;
            case KeyEvent.VK_Q:
                return KEY_NUM1; // 斜向：左上（对应数字 1）
            case KeyEvent.VK_E:
                return KEY_NUM3; // 斜向：右上（对应数字 3）
            case KeyEvent.VK_Z:
                return KEY_NUM7; // 斜向：左下（对应数字 7）
            case KeyEvent.VK_C:
                return KEY_NUM9; // 斜向：右下（对应数字 9）
            case KeyEvent.VK_F1:
                return SOFT_LEFT;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_F2:
                return SOFT_RIGHT;
            case KeyEvent.VK_MULTIPLY:
                return KEY_STAR;
            default:
                break;
        }
        char ch = e.getKeyChar();
        if (ch >= '0' && ch <= '9' || ch == '*' || ch == '#') {
            return ch;
        }
        if (ch >= 32 && ch < 127) {
            return ch;
        }
        return 0;
    }

    /** Swing 呈现组件：固定整数倍尺寸，把帧缓冲 1:1 贴窗，键盘/鼠标事件转成游戏输入。 */
    private static final class CanvasPanel extends JComponent implements KeyListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {
        private final Canvas canvas;

        CanvasPanel(Canvas canvas) {
            this.canvas = canvas;
            setPreferredSize(new Dimension(Screen.WIDTH * Screen.SCALE, Screen.HEIGHT * Screen.SCALE));
            setFocusable(true);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                // Retina 的 2x 设备变换是 g2 自带的：帧缓冲建在物理像素上，
                // 按用户空间 (0,0,240*S,320*S) 贴图恰好 1:1 上屏
                canvas.observedDeviceScale = g2.getTransform().getScaleX();
                canvas.ensureFramebuffer(canvas.observedDeviceScale);
                synchronized (canvas.bufferLock) {
                    if (canvas.framebuffer != null) {
                        g2.drawImage(canvas.framebuffer, 0, 0,
                                Screen.WIDTH * Screen.SCALE, Screen.HEIGHT * Screen.SCALE, null);
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = mapKeyCode(e);
            if (code != 0) {
                // 同一键的新按下使还未投递的松开失效（快速连点场景）
                synchronized (canvas.pendingKeyReleases) {
                    canvas.pendingKeyReleases.remove(Integer.valueOf(code));
                }
                canvas.keyPressed(code);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = mapKeyCode(e);
            if (code != 0) {
                // 不立即投递：排队，等本次 paint 之后 flushPendingKeyReleases 再送达
                synchronized (canvas.pendingKeyReleases) {
                    canvas.pendingKeyReleases.add(code);
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        // —— 鼠标/触摸板：换算为逻辑像素后走 mouseA 钩子 ——

        private void dispatchMouse(java.awt.event.MouseEvent e, int kind) {
            int lx = e.getX() / Screen.SCALE;
            int ly = e.getY() / Screen.SCALE;
            if (System.getProperty("aoe.debug") != null) {
                System.out.println("[mouse] kind=" + kind + " " + lx + "," + ly);
            }
            canvas.mouseA(kind, lx, ly);
        }

        @Override
        public void mouseMoved(java.awt.event.MouseEvent e) {
            dispatchMouse(e, 0);
        }

        @Override
        public void mouseDragged(java.awt.event.MouseEvent e) {
            dispatchMouse(e, 0);
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                dispatchMouse(e, 1);
            } else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                dispatchMouse(e, 3);
            }
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                dispatchMouse(e, 2);
            }
        }

        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
            dispatchMouse(e, 4);
        }
    }
}
