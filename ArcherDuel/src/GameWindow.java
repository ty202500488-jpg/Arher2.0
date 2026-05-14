import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * The main application window (JFrame).
 * Configures the window properties, sets the application icon, 
 * and hosts the primary GamePanel where the game is drawn.
 */
public class GameWindow extends JFrame {

    // ── Window Dimensions ────────────────────────────────────────────
    public static final int WIDTH = 1400; // Total window width
    public static final int HEIGHT = 800; // Total window height

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

        // Custom exit handler to show a confirmation dialog
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                gamePanel.confirmExit();
            }
        });

        pack(); // Resize window to fit the preferred size of GamePanel
        setLocationRelativeTo(null); // Center the window on the screen
    }
}
