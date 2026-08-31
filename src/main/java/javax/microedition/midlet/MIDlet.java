package javax.microedition.midlet;

import java.util.HashMap;
import java.util.Map;

/**
 * MIDlet 的桌面桩实现。应用属性由启动器（aoe.Main）注入，
 * notifyDestroyed 直接退出进程。
 */
public abstract class MIDlet {
    private static final Map<String, String> APP_PROPERTIES = new HashMap<>();

    public static void setAppProperty(String key, String value) {
        APP_PROPERTIES.put(key, value);
    }

    public final String getAppProperty(String key) {
        return APP_PROPERTIES.get(key);
    }

    public final void notifyDestroyed() {
        System.exit(0);
    }

    public final void notifyPaused() {
    }

    public final void resumeRequest() {
    }

    public final boolean platformRequest(String url) {
        return false;
    }

    public final int checkPermission(String permission) {
        return 1;
    }

    protected abstract void startApp();

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional);
}
