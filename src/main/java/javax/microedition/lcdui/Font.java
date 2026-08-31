package javax.microedition.lcdui;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * lcdui.Font 的桌面实现：包装 java.awt.Font，提供 MIDP 风格的度量接口。
 * 字号为目测近似值（240x320 逻辑像素下），不合适再调。
 */
public final class Font {
    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;

    private static final Graphics2D METRICS_G = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

    final java.awt.Font awtFont;
    final FontMetrics metrics;
    private final int face;
    private final int style;
    private final int size;

    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
        int points = size == SIZE_SMALL ? 9 : (size == SIZE_LARGE ? 14 : 11);
        int awtStyle = ((style & STYLE_BOLD) != 0 ? java.awt.Font.BOLD : 0)
                | ((style & STYLE_ITALIC) != 0 ? java.awt.Font.ITALIC : 0);
        String name = face == FACE_MONOSPACE ? java.awt.Font.MONOSPACED : java.awt.Font.DIALOG;
        this.awtFont = new java.awt.Font(name, awtStyle, points);
        this.metrics = METRICS_G.getFontMetrics(this.awtFont);
    }

    public static Font getDefaultFont() {
        return getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
    }

    public static Font getFont(int face, int style, int size) {
        return new Font(face, style, size);
    }

    public static Font getFont(int specifier) {
        return getDefaultFont();
    }

    public int getHeight() {
        return metrics.getHeight();
    }

    public int getBaselinePosition() {
        return metrics.getAscent();
    }

    public int stringWidth(String str) {
        return metrics.stringWidth(str);
    }

    public int substringWidth(String str, int offset, int len) {
        return metrics.stringWidth(str.substring(offset, offset + len));
    }

    public int charWidth(char ch) {
        return metrics.charWidth(ch);
    }

    public int charsWidth(char[] ch, int offset, int length) {
        return metrics.charsWidth(ch, offset, length);
    }

    public int getFace() {
        return face;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public boolean isPlain() {
        return style == STYLE_PLAIN;
    }

    public boolean isBold() {
        return (style & STYLE_BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & STYLE_ITALIC) != 0;
    }

    public boolean isUnderlined() {
        return (style & STYLE_UNDERLINED) != 0;
    }
}
