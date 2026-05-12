import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * GameWindow
 * Sets up the main JFrame and hosts the GamePanel.
 */
public class GameWindow extends JFrame {

    public static final int WIDTH = 1400;
    public static final int HEIGHT = 800;

    private GamePanel gamePanel;

    public GameWindow() {
        setTitle("Archer Duel — 1v1 Local Multiplayer");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // Set frame icon
        try {
            File iconFile = new File("ArcherDuel/assets/logo/download.png");
            if (iconFile.exists()) {
                Image icon = ImageIO.read(iconFile);
                setIconImage(icon);
            } else {
                System.err.println("Could not load icon from: " + iconFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        gamePanel = new GamePanel();
        add(gamePanel);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                gamePanel.confirmExit();
            }
        });

        pack();
        setLocationRelativeTo(null); // center on screen
    }
}
