package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * lcdui.Display 的桌面实现：管理唯一的 JFrame，setCurrent 切换窗口内容。
 */
public final class Display {
    private static Display instance;

    private JFrame frame;
    private Displayable current;

    private Display() {
    }

    public static synchronized Display getDisplay(MIDlet midlet) {
        if (instance == null) {
            instance = new Display();
        }
        return instance;
    }

    public void setCurrent(Displayable d) {
        // headless（dev 模式）：不创建/显示窗口，直接完成切换语义
        if (System.getProperty("aoe.headless") != null) {
            if (current != null && current != d) {
                current.shown = false;
            }
            current = d;
            if (d != null) {
                d.shown = true;
                d.notifyShown();
            }
            return;
        }
        runOnEdt(() -> {
            ensureFrame();
            if (current != null && current != d) {
                current.shown = false;
            }
            current = d;
            if (d != null) {
                d.shown = true;
                frame.setContentPane(d.panel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                if (!frame.isVisible()) {
                    frame.setVisible(true);
                }
                d.panel.requestFocusInWindow();
                d.notifyShown();
            }
        });
    }

    public Displayable getCurrent() {
        return current;
    }

    static void updateTitle(String title) {
        Display d = instance;
        if (d != null && d.frame != null && title != null) {
            runOnEdt(() -> d.frame.setTitle(title));
        }
    }

    private void ensureFrame() {
        if (frame != null) {
            return;
        }
        frame = new JFrame("Age of Empires II");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                if (current != null) {
                    current.notifyHidden();
                }
            }

            @Override
            public void windowDeiconified(java.awt.event.WindowEvent e) {
                if (current != null) {
                    current.notifyShown();
                }
            }
        });
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
