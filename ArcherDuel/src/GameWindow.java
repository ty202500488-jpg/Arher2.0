import javax.swing.*;
import java.awt.*;

/**
 * GameWindow
 * Sets up the main JFrame and hosts the GamePanel.
 */
public class GameWindow extends JFrame {

    public static final int WIDTH  = 1400;
    public static final int HEIGHT = 800;

    private GamePanel gamePanel;

    public GameWindow() {
        setTitle("Archer Duel — 1v1 Local Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null); // center on screen
    }
}
