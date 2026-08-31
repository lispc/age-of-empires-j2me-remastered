package javax.microedition.media;

import java.io.InputStream;

/**
 * MMAPI Manager 的桌面实现：只支持游戏用到的 MIDI 播放。
 */
public final class Manager {
    private Manager() {
    }

    public static Player createPlayer(InputStream stream, String type) {
        return new Player(stream);
    }
}
