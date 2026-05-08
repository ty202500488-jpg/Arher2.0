import javax.sound.sampled.*;
import javax.sound.midi.*;

/**
 * AudioManager v2 — Richer procedural sounds + new DASH sound.
 */
public class AudioManager {

    private static final int SAMPLE_RATE = 22050;
    private static Sequencer sequencer;

    public enum Sound {
        SHOOT, HIT, JUMP, WIN, DASH, HEADSHOT
    }

    /** Starts a richer MIDI background music loop. */
    public static void startBGM() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();

            Sequence seq   = new Sequence(Sequence.PPQ, 8);
            Track    track = seq.createTrack();

            // Instrument: Steel Drums (channel 0, program 114) for a mystical feel
            track.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 114, 0), 0));

            int[] melody  = {50, 53, 57, 60, 62, 60, 57, 53, 50, 55, 59, 62, 64, 62, 59, 55};
            int[] harmony = {45, 48, 52, 55, 57, 55, 52, 48, 45, 50, 54, 57, 59, 57, 54, 50};

            for (int i = 0; i < 32; i++) {
                int note = melody[i % melody.length];
                int har  = harmony[i % harmony.length];
                long tick = (long) i * 8;

                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON,  0, note, 65), tick));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0),  tick + 7));

                // Harmony note every other beat
                if (i % 2 == 0) {
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON,  0, har, 45), tick));
                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, har, 0),  tick + 7));
                }
            }

            sequencer.setSequence(seq);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(110);
            sequencer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Play a sound effect on a background thread. */
    public static void play(Sound sound) {
        new Thread(() -> {
            try {
                byte[] data   = generateSoundData(sound);
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(format, data, 0, data.length);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000 + 100);
                clip.close();
            } catch (Exception e) {
                // silently ignore audio failures
            }
        }).start();
    }

    private static byte[] generateSoundData(Sound sound) {
        int durationMs = switch (sound) {
            case SHOOT    -> 140;
            case JUMP     -> 110;
            case HIT      -> 280;
            case WIN      -> 1100;
            case DASH     -> 90;
            case HEADSHOT -> 350;
        };

        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double t   = (double) i / SAMPLE_RATE;
            double val = switch (sound) {
                case SHOOT -> {
                    double freq = 900 - 700 * ((double) i / numSamples);
                    yield Math.sin(2 * Math.PI * freq * t) * 0.7
                            + Math.sin(2 * Math.PI * freq * 2 * t) * 0.3;
                }
                case JUMP -> {
                    double freq = 220 + 500 * ((double) i / numSamples);
                    yield Math.sin(2 * Math.PI * freq * t);
                }
                case HIT -> {
                    // Distorted thud
                    double noise = (Math.random() * 2 - 1) * (1 - (double) i / numSamples);
                    double thud  = Math.sin(2 * Math.PI * 80 * t) * (1 - (double) i / numSamples);
                    yield noise * 0.6 + thud * 0.4;
                }
                case WIN -> {
                    double[] notes = {523, 659, 784, 1047};
                    int idx = Math.min((i / (numSamples / 4)), 3);
                    yield Math.sin(2 * Math.PI * notes[idx] * t);
                }
                case DASH -> {
                    // Whoosh: high-freq noise with pitch drop
                    double freq = 2000 - 1800 * ((double) i / numSamples);
                    double noise = (Math.random() * 2 - 1) * 0.4;
                    yield Math.sin(2 * Math.PI * freq * t) * 0.6 + noise;
                }
                case HEADSHOT -> {
                    // Sharp crack + ring
                    double crack = (Math.random() * 2 - 1) * (1 - (double) i / numSamples);
                    double ring  = Math.sin(2 * Math.PI * 1200 * t) * Math.exp(-t * 8);
                    yield crack * 0.5 + ring * 0.5;
                }
            };

            // Envelope to avoid clicks
            double env = 1.0;
            if (i < 100) env = i / 100.0;
            if (i > numSamples - 100) env = (numSamples - i) / 100.0;

            data[i] = (byte) Math.max(-127, Math.min(127, (int)(val * 120 * env)));
        }
        return data;
    }
}
