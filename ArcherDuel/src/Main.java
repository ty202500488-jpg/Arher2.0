/**
 * ArcherDuel - 2D 1v1 Local Multiplayer Archer Game
 * Pure Java (Swing) — No external libraries required
 *
 * Main entry point: launches the game window.
 */
public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
