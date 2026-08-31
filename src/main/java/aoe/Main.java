package aoe;

import AgeOfEmpires.AoeMidlet;
import javax.microedition.midlet.MIDlet;

/**
 * 桌面启动器：替代 J2ME 的 AMS，直接实例化 MIDlet 并调 startApp()。
 * 用法：gradle run，可用 -Daoe.scale=N 调整放大倍数（默认 3）。
 */
public final class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            System.setProperty("aoe.scale", args[0]);
        }
        MIDlet.setAppProperty("MIDlet-Version", "01.02.15");
        AoeMidlet midlet = new AoeMidlet();
        midlet.startApp();
    }
}
