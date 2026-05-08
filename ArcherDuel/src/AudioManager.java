import javax.sound.sampled.*;
import javax.sound.midi.*;
import java.io.File;
import java.nio.ByteBuffer;

/**
 * AudioManager - Procedural 8-bit sound generator.
 * Since no external assets are provided, we generate sounds mathematically.
 */
public class AudioManager {

    private static final int SAMPLE_RATE = 22050;
    private static Sequencer sequencer;

    public enum Sound {
        SHOOT, HIT, JUMP, WIN
    }

    /**
     * Starts a simple MIDI background music loop.
     */
    public static void startBGM() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();

            Sequence seq = new Sequence(Sequence.PPQ, 4);
            Track track = seq.createTrack();

            // Simple "Forest" BGM loop
            int[] notes = {50, 53, 57, 60, 50, 53, 57, 62};
            for (int i = 0; i < 32; i++) {
                int note = notes[i % notes.length];
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, note, 60), i * 4));
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0), i * 4 + 3));
            }

            sequencer.setSequence(seq);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays a procedural sound effect or loads from file if available.
     */
    public static void play(Sound sound) {
        new Thread(() -> {
            try {
                // Try loading from file first for SHOOT (if it's a .wav)
                if (sound == Sound.SHOOT) {
                    File file = new File("assets/audio/arrow-shoot.wav");
                    if (file.exists()) {
                        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                        Clip clip = AudioSystem.getClip();
                        clip.open(stream);
                        clip.start();
                        return;
                    }
                }

                // Fallback to procedural generation
                byte[] data = generateSoundData(sound);
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(format, data, 0, data.length);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000 + 100);
                clip.close();
            } catch (Exception e) {
                // If anything fails (like unsupported MP3), just use procedural fallback
                playProcedural(sound);
            }
        }).start();
    }

    private static void playProcedural(Sound sound) {
        try {
            byte[] data = generateSoundData(sound);
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] generateSoundData(Sound sound) {
        int durationMs = 0;
        switch (sound) {
            case SHOOT -> durationMs = 150;
            case JUMP  -> durationMs = 100;
            case HIT   -> durationMs = 300;
            case WIN   -> durationMs = 1000;
        }

        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double value = 0;

            switch (sound) {
                case SHOOT -> {
                    // Pitch drop: 800Hz to 200Hz
                    double freq = 800 - (600 * ((double) i / numSamples));
                    value = Math.sin(2 * Math.PI * freq * t);
                }
                case JUMP -> {
                    // Pitch rise: 200Hz to 600Hz
                    double freq = 200 + (400 * ((double) i / numSamples));
                    value = Math.sin(2 * Math.PI * freq * t);
                }
                case HIT -> {
                    // Noise burst
                    value = (Math.random() * 2 - 1) * (1.0 - (double) i / numSamples);
                }
                case WIN -> {
                    // Simple arpeggio
                    double[] notes = {440, 554, 659, 880};
                    int noteIdx = (i / (numSamples / 4)) % 4;
                    value = Math.sin(2 * Math.PI * notes[noteIdx] * t);
                }
            }

            // Apply simple envelope to avoid clicking
            double envelope = 1.0;
            if (i < 100) envelope = i / 100.0;
            if (i > numSamples - 100) envelope = (numSamples - i) / 100.0;

            data[i] = (byte) (value * 127 * envelope);
        }

        return data;
    }
}
