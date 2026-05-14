/**
 * Entry point for the ArcherDuel application.
 * Initializes system properties and launches the game window.
 */
public class Main {
    public static void main(String[] args) {
        // Run in the Event Dispatch Thread (EDT) for Swing thread-safety
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
