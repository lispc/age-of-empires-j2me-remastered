package javax.microedition.lcdui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * lcdui.Image 的桌面实现：内部是 BufferedImage。
 * 从字节流解码的内容图（精灵、UI 素材）按 ASSET_SCALE 倍预放大存储——
 * 现在是朴素最近邻放大，以后用超分辨率工具离线放大后同尺寸替换即可获益。
 * getWidth()/getHeight() 等对外接口始终以 240x320 逻辑像素计。
 * 游戏运行时用 createImage(w,h) 创建的可变图保持 1x。
 */
public final class Image {
    /** 内容素材的预放大倍数。可用 -Daoe.assetScale=N 调整（默认  2）。 */
    public static final int ASSET_SCALE = Integer.parseInt(System.getProperty("aoe.assetScale", "2"));

    public final BufferedImage image;
    /** 位图像素相对逻辑像素的倍数（素材=ASSET_SCALE，可变图=1）。 */
    final int scale;

    private Image(BufferedImage image, int scale) {
        this.image = image;
        this.scale = scale;
    }

    private static BufferedImage upscale(BufferedImage src, int factor) {
        if (factor == 1) {
            return src;
        }
        int w = src.getWidth() * factor;
        int h = src.getHeight() * factor;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = dst.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    public static Image createImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("bad size " + width + "x" + height);
        }
        return new Image(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), 1);
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        try {
            BufferedImage raw = ImageIO.read(new ByteArrayInputStream(imageData, imageOffset, imageLength));
            if (raw == null) {
                throw new IllegalArgumentException("undecodable image data");
            }
            BufferedImage argb = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(raw, 0, 0, null);
            return new Image(upscale(argb, ASSET_SCALE), ASSET_SCALE);
        } catch (IOException e) {
            throw new IllegalArgumentException("undecodable image data: " + e);
        }
    }

    public static Image createImage(InputStream stream) {
        try {
            byte[] data = stream.readAllBytes();
            return createImage(data, 0, data.length);
        } catch (IOException e) {
            throw new IllegalArgumentException("undecodable image stream: " + e);
        }
    }

    public static Image createImage(String name) {
        InputStream in = Image.class.getResourceAsStream(name);
        if (in == null) {
            throw new IllegalArgumentException("resource not found: " + name);
        }
        return createImage(in);
    }

    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        BufferedImage img = new BufferedImage(width, height, processAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, width, height, rgb, 0, width);
        return new Image(img, 1);
    }

    public Graphics getGraphics() {
        return new Graphics(image.createGraphics(), this);
    }

    /** 逻辑宽度（游戏布局用）。 */
    public int getWidth() {
        return image.getWidth() / scale;
    }

    /** 逻辑高度。 */
    public int getHeight() {
        return image.getHeight() / scale;
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        // 逻辑坐标 → 位图像素：隔 scale 取样
        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                rgbData[offset + row * scanlength + col] =
                        image.getRGB((x + col) * scale, (y + row) * scale);
            }
        }
    }
}
