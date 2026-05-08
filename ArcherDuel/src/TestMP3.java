import javax.sound.sampled.*;
import java.io.File;
public class TestMP3 {
    public static void main(String[] args) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File("../assets/audio/arrow-shoot.mp3"));
            System.out.println("Format: " + stream.getFormat());
        } catch (Exception e) {
            System.out.println("Unsupported format");
            e.printStackTrace();
        }
    }
}
