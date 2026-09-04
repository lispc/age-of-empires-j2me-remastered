package javax.microedition.media;

import java.io.InputStream;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

/**
 * MMAPI Player 的桌面实现：用 javax.sound.midi 播放 MIDI。
 * 任何音频失败都以 RuntimeException 抛出；游戏侧（AgeOfEmpires.b）
 * 本来就 catch Exception 并静默降级为无音乐。
 * headless/mute 模式下连 MIDI 子系统都不初始化：MidiSystem.getSequencer()
 * 会触发 com.sun.media.sound 本地库加载，加载跑在 Timer（模拟）线程上，
 * macOS 音频服务卡壳时整个模拟永久冻结（2026-09-04 实测：watchdog 报
 * NativeLibraries.load 卡死，战役 intro 冻在 ar=25；连续 kill 音频进程
 * 后 coreaudiod 进入病态可复现）。
 */
public final class Player {
    private final Sequence sequence;
    private Sequencer sequencer;
    private int loopCount = 1;

    private static boolean muted() {
        return System.getProperty("aoe.mute") != null || System.getProperty("aoe.headless") != null;
    }

    Player(InputStream stream) {
        if (muted()) {
            this.sequence = null;
            return;
        }
        try {
            this.sequence = MidiSystem.getSequence(stream);
        } catch (Exception e) {
            throw new RuntimeException("cannot load midi", e);
        }
    }

    public void prefetch() {
        if (this.sequence == null) {
            return; // muted/headless：不初始化 MIDI 子系统
        }
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(sequence);
        } catch (Exception e) {
            throw new RuntimeException("cannot init sequencer", e);
        }
    }

    public void setLoopCount(int count) {
        this.loopCount = count;
    }

    public void start() {
        if (sequencer == null) {
            return;
        }
        // dev 模式（headless/静音）不发声
        if (muted()) {
            return;
        }
        sequencer.setLoopCount(loopCount < 0 ? Sequencer.LOOP_CONTINUOUSLY : loopCount - 1);
        sequencer.start();
    }

    public void stop() {
        if (sequencer != null) {
            sequencer.stop();
        }
    }

    public void deallocate() {
    }

    public void close() {
        if (sequencer != null) {
            sequencer.close();
            sequencer = null;
        }
    }
}
