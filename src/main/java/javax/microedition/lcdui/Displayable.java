package javax.microedition.lcdui;

import javax.swing.JComponent;

/**
 * lcdui.Displayable 的桌面实现。每个 Displayable 持有一个 Swing 组件，
 * 由 Display 放进窗口显示。
 */
public abstract class Displayable {
    JComponent panel;
    volatile boolean shown;
    private String title;
    CommandListener commandListener;

    public int getWidth() {
        return Screen.WIDTH;
    }

    public int getHeight() {
        return Screen.HEIGHT;
    }

    public boolean isShown() {
        return shown;
    }

    public void setTitle(String title) {
        this.title = title;
        Display.updateTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void setCommandListener(CommandListener l) {
        this.commandListener = l;
    }

    public void addCommand(Command cmd) {
        // 桌面端没有软键菜单，命令通过键盘直达游戏逻辑
    }

    public void removeCommand(Command cmd) {
    }

    /** Display.setCurrent 把本组件显示出来后回调。 */
    void notifyShown() {
    }

    /** 窗口最小化/恢复时回调。 */
    void notifyHidden() {
    }
}
