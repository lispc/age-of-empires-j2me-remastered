package javax.microedition.media;

import java.io.InputStream;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

/**
 * MMAPI Player 的桌面实现：用 javax.sound.midi 播放 MIDI。
 * 任何音频失败都以 RuntimeException 抛出；游戏侧（AgeOfEmpires.b）
 * 本来就 catch Exception 并静默降级为无音乐。
 */
public final class Player {
    private final Sequence sequence;
    private Sequencer sequencer;
    private int loopCount = 1;

    Player(InputStream stream) {
        try {
            this.sequence = MidiSystem.getSequence(stream);
        } catch (Exception e) {
            throw new RuntimeException("cannot load midi", e);
        }
    }

    public void prefetch() {
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
        if (System.getProperty("aoe.mute") != null || System.getProperty("aoe.headless") != null) {
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
