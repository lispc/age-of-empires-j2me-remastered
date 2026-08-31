package javax.microedition.lcdui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * lcdui.Graphics 的桌面实现：包装 Graphics2D。
 * 每个实例有一个"基准变换"：画到设备分辨率帧缓冲时 = scale(SCALE × 设备像素倍数)，
 * 画到图片时 = scale(图片素材倍数)。游戏所有坐标都是 240x320 逻辑像素；平移/裁剪/绘制
 * 都在逻辑坐标系里进行（与 MIDP 语义一致：setClip 替换且受 translate 影响，
 * clipRect 求交）。文字经基准变换按矢量渲染，始终清晰；图片按最近邻插值。
 */
public final class Graphics {
    // 锚点
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;

    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    // drawRegion 的变换常量（与 javax.microedition.lcdui.game.Sprite 一致）
    public static final int TRANS_NONE = 0;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_MIRROR_ROT270 = 4;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR_ROT90 = 7;

    final Graphics2D g;
    static final boolean DEBUG_DRAW = System.getProperty("aoe.debugDraw") != null;
    private final AffineTransform baseTransform;
    private final int logicalWidth;
    private final int logicalHeight;
    private Font font = Font.getDefaultFont();
    private int color;

    /** 画到帧缓冲的 Graphics：基准变换 = scale(baseScale)，逻辑区域 logicalWidth×logicalHeight。 */
    Graphics(Graphics2D g, double baseScale, int logicalWidth, int logicalHeight) {
        this.g = g;
        AffineTransform t = g.getTransform();
        t.scale(baseScale, baseScale);
        this.baseTransform = t;
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        resetState();
    }

    /** 画到图片的 Graphics：基准变换 = scale(图片素材倍数)。 */
    Graphics(Graphics2D g, Image target) {
        this(g, target.scale, target.getWidth(), target.getHeight());
    }

    /** 恢复到 MIDP 规定的初始状态（恒等平移、全幅裁剪、黑字默认字体）。 */
    void resetState() {
        g.setTransform(baseTransform);
        g.setClip(0, 0, logicalWidth, logicalHeight);
        setFont(Font.getDefaultFont());
        setColor(0);
    }

    public void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        g.setColor(new Color(color));
    }

    public void setColor(int red, int green, int blue) {
        setColor((red << 16) | (green << 8) | blue);
    }

    public int getColor() {
        return color;
    }

    public int getRedComponent() {
        return (color >> 16) & 0xFF;
    }

    public int getGreenComponent() {
        return (color >> 8) & 0xFF;
    }

    public int getBlueComponent() {
        return color & 0xFF;
    }

    public void setGrayScale(int value) {
        setColor(value, value, value);
    }

    public void fillRect(int x, int y, int width, int height) {
        if (DEBUG_DRAW) System.out.printf("[fillRect] %d,%d %dx%d color=%06x%n", x, y, width, height, color);
        g.fillRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height) {
        g.drawRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        if (DEBUG_DRAW) System.out.printf("[drawLine] %d,%d-%d,%d%n", x1, y1, x2, y2);
        g.drawLine(x1, y1, x2, y2);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }

    public void translate(int x, int y) {
        if (DEBUG_DRAW) System.out.printf("[translate] %d,%d%n", x, y);
        g.translate(x, y);
    }

    public int getTranslateX() {
        double tx = g.getTransform().getTranslateX() - baseTransform.getTranslateX();
        return (int) (tx / baseTransform.getScaleX());
    }

    public int getTranslateY() {
        double ty = g.getTransform().getTranslateY() - baseTransform.getTranslateY();
        return (int) (ty / baseTransform.getScaleY());
    }

    public void setClip(int x, int y, int width, int height) {
        if (DEBUG_DRAW) System.out.printf("[setClip] %d,%d %dx%d%n", x, y, width, height);
        g.setClip(x, y, width, height);
    }

    public void clipRect(int x, int y, int width, int height) {
        g.clipRect(x, y, width, height);
    }

    private Rectangle clipBounds() {
        Rectangle r = g.getClipBounds();
        return r == null ? new Rectangle(0, 0, 0, 0) : r;
    }

    public int getClipX() {
        return clipBounds().x;
    }

    public int getClipY() {
        return clipBounds().y;
    }

    public int getClipWidth() {
        return clipBounds().width;
    }

    public int getClipHeight() {
        return clipBounds().height;
    }

    public void setFont(Font font) {
        this.font = font;
        g.setFont(font.awtFont);
    }

    public Font getFont() {
        return font;
    }

    public void setStrokeStyle(int style) {
        // 桌面端不支持点线；游戏没有使用，忽略
    }

    public int getStrokeStyle() {
        return SOLID;
    }

    public void drawString(String str, int x, int y, int anchor) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (DEBUG_DRAW) System.out.printf("[drawString] %d,%d a=%d \"%s\"%n", x, y, anchor, str);
        int w = font.stringWidth(str);
        int h = font.getHeight();
        int ascent = font.getBaselinePosition();
        if ((anchor & HCENTER) != 0) {
            x -= w / 2;
        } else if ((anchor & RIGHT) != 0) {
            x -= w;
        }
        int baseline;
        if ((anchor & BASELINE) != 0) {
            baseline = y;
        } else if ((anchor & BOTTOM) != 0) {
            baseline = y + ascent - h;
        } else if ((anchor & VCENTER) != 0) {
            baseline = y + ascent - h / 2;
        } else { // TOP（缺省）
            baseline = y + ascent;
        }
        g.drawString(str, x, baseline);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        drawString(str.substring(offset, offset + len), x, y, anchor);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        drawString(String.valueOf(character), x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        drawString(new String(data, offset, length), x, y, anchor);
    }

    private int anchorX(int x, int width, int anchor) {
        if ((anchor & HCENTER) != 0) {
            return x - width / 2;
        }
        if ((anchor & RIGHT) != 0) {
            return x - width;
        }
        return x; // LEFT（缺省）
    }

    private int anchorY(int y, int height, int anchor) {
        if ((anchor & VCENTER) != 0) {
            return y - height / 2;
        }
        if ((anchor & BOTTOM) != 0) {
            return y - height;
        }
        return y; // TOP（缺省）
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null) {
            throw new NullPointerException();
        }
        if (DEBUG_DRAW) System.out.printf("[drawImage] %d,%d anchor=%d lw=%d lh=%d scale=%d%n",
                x, y, anchor, img.getWidth(), img.getHeight(), img.scale);
        // 素材可能是预放大的位图：按逻辑尺寸绘制，源整图采样
        int lw = img.getWidth();
        int lh = img.getHeight();
        if (DEBUG_DRAW) {
            try {
                g.drawImage(img.image,
                        anchorX(x, lw, anchor), anchorY(y, lh, anchor),
                        anchorX(x, lw, anchor) + lw, anchorY(y, lh, anchor) + lh,
                        0, 0, img.image.getWidth(), img.image.getHeight(), null);
                System.out.println("[drawImage] ok, backing=" + img.image.getWidth() + "x" + img.image.getHeight());
            } catch (Throwable t) {
                System.out.println("[drawImage] THREW " + t);
            }
            return;
        }
        g.drawImage(img.image,
                anchorX(x, lw, anchor), anchorY(y, lh, anchor),
                anchorX(x, lw, anchor) + lw, anchorY(y, lh, anchor) + lh,
                0, 0, img.image.getWidth(), img.image.getHeight(), null);
    }

    public void drawRegion(Image src, int xSrc, int ySrc, int width, int height,
                           int transform, int xDst, int yDst, int anchor) {
        if (src == null) {
            throw new NullPointerException();
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        if (DEBUG_DRAW) System.out.printf("[drawRegion] src=%d,%d %dx%d tr=%d dst=%d,%d anchor=%d scale=%d%n",
                xSrc, ySrc, width, height, transform, xDst, yDst, anchor, src.scale);
        int s = src.scale;
        boolean swap = transform == TRANS_ROT90 || transform == TRANS_ROT270
                || transform == TRANS_MIRROR_ROT90 || transform == TRANS_MIRROR_ROT270;
        int dw = (swap ? height : width) * s;
        int dh = (swap ? width : height) * s;
        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        for (int sy = 0; sy < height * s; ++sy) {
            for (int sx = 0; sx < width * s; ++sx) {
                int dx;
                int dy;
                switch (transform) {
                    case TRANS_MIRROR:
                        dx = width * s - 1 - sx; dy = sy;
                        break;
                    case TRANS_MIRROR_ROT180:
                        dx = sx; dy = height * s - 1 - sy;
                        break;
                    case TRANS_ROT180:
                        dx = width * s - 1 - sx; dy = height * s - 1 - sy;
                        break;
                    case TRANS_ROT90:
                        dx = height * s - 1 - sy; dy = sx;
                        break;
                    case TRANS_ROT270:
                        dx = sy; dy = width * s - 1 - sx;
                        break;
                    case TRANS_MIRROR_ROT90:
                        dx = sy; dy = sx;
                        break;
                    case TRANS_MIRROR_ROT270:
                        dx = height * s - 1 - sy; dy = width * s - 1 - sx;
                        break;
                    default: // TRANS_NONE
                        dx = sx; dy = sy;
                        break;
                }
                out.setRGB(dx, dy, src.image.getRGB(xSrc * s + sx, ySrc * s + sy));
            }
        }
        int ldw = swap ? height : width;
        int ldh = swap ? width : height;
        g.drawImage(out,
                anchorX(xDst, ldw, anchor), anchorY(yDst, ldh, anchor),
                anchorX(xDst, ldw, anchor) + ldw, anchorY(yDst, ldh, anchor) + ldh,
                0, 0, dw, dh, null);
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y,
                        int width, int height, boolean processAlpha) {
        if (DEBUG_DRAW) System.out.printf("[drawRGB] %d,%d %dx%d%n", x, y, width, height);
        BufferedImage img = new BufferedImage(width, height,
                processAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, width, height, rgbData, offset, scanlength);
        g.drawImage(img, x, y, null);
    }

    public void copyArea(int xSrc, int ySrc, int width, int height, int xDst, int yDst, int anchor) {
        int ax = anchorX(xDst, width, anchor);
        int ay = anchorY(yDst, height, anchor);
        g.copyArea(xSrc, ySrc, width, height, ax - xSrc, ay - ySrc);
    }
}
