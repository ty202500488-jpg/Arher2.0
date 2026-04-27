import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GamePanel - Updated with Start Menu / Loading Screen
 */
public class GamePanel extends JPanel implements KeyListener, ActionListener {

    // --- constants ---
    private static final int TARGET_FPS = 60;
    private static final int TICK_MS    = 1000 / TARGET_FPS;

    // --- game objects ---
    private Arena          arena;
    private Player         player1, player2;
    private SpriteRenderer sprites;

    // --- game state ---
    // Added START_MENU state
    private enum GameState { START_MENU, COUNTDOWN, PLAYING, OVER }
    private GameState state = GameState.START_MENU;

    private int    winner        = -1;
    private int    countdownTick = 0;
    private int    flashAlpha    = 0;
    private String winnerText    = "";
    
    // UI pulse animation for the loading screen
    private float  pulse        = 0; 
    private boolean pulseUp     = true;

    private final List<Particle> particles = new ArrayList<>();
    private final Timer timer;
    private BufferedImage buffer;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        sprites = new SpriteRenderer();
        
        // Initial setup but don't start countdown yet
        resetEntities(); 

        timer = new Timer(TICK_MS, this);
        timer.start();
    }

    private void resetEntities() {
        arena   = new Arena();
        player1 = new Player(0, 80,  Arena.GROUND_Y - Player.H, sprites);
        player2 = new Player(1, GameWindow.WIDTH - 80 - Player.W,
                                Arena.GROUND_Y - Player.H, sprites);
        particles.clear();
        winner       = -1;
        flashAlpha   = 0;
        countdownTick = 0;
    }

    private void initGame() {
        resetEntities();
        state = GameState.COUNTDOWN;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    private void update() {
        switch (state) {
            case START_MENU -> updateMenu();
            case COUNTDOWN  -> {
                countdownTick++;
                if (countdownTick >= 180) state = GameState.PLAYING;
            }
            case PLAYING    -> updatePlaying();
            case OVER       -> updateOver();
        }
    }

    private void updateMenu() {
        // Animate the "Press Enter" pulse
        if (pulseUp) {
            pulse += 0.05f;
            if (pulse >= 1.0f) pulseUp = false;
        } else {
            pulse -= 0.05f;
            if (pulse <= 0.3f) pulseUp = true;
        }
    }

    private void updatePlaying() {
        Rectangle[] platforms = arena.getPlatforms();
        player1.update(platforms);
        player2.update(platforms);
        checkArrowCollisions();
        updateParticles();
        if (flashAlpha > 0) flashAlpha -= 12;
    }

    private void updateOver() {
        Rectangle[] platforms = arena.getPlatforms();
        player1.update(platforms);
        player2.update(platforms);
        updateParticles();
        if (flashAlpha > 0) flashAlpha -= 8;
    }

    private void checkArrowCollisions() {
        checkHit(player1, player2);
        checkHit(player2, player1);
    }

    private void checkHit(Player shooter, Player target) {
        if (!target.isAlive()) return;
        Rectangle targetBounds = target.getBounds();
        for (Arrow arrow : shooter.arrows) {
            if (!arrow.active) continue;
            if (arrow.getBounds().intersects(targetBounds)) {
                arrow.active = false;
                killPlayer(target);
                return;
            }
        }
    }

    private void killPlayer(Player p) {
        p.die();
        winner     = (p.playerIndex == 0) ? 2 : 1;
        winnerText = "Player " + winner + " Wins!";
        state      = GameState.OVER;
        flashAlpha = 255;
        spawnDeathParticles(p.getCenterX(), p.getCenterY(), p.playerIndex);
    }

    private void spawnDeathParticles(float cx, float cy, int playerIdx) {
        Color base = (playerIdx == 0) ? new Color(80, 130, 255) : new Color(255, 80, 80);
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 30; i++) {
            float vx = (rng.nextFloat() - 0.5f) * 8f;
            float vy = (rng.nextFloat() - 0.5f) * 8f - 2f;
            int size = 3 + rng.nextInt(6);
            particles.add(new Particle(cx, cy, vx, vy, size, base));
        }
    }

    private void updateParticles() {
        particles.forEach(Particle::update);
        particles.removeIf(p -> p.alpha <= 0);
    }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g = buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Render based on state
        if (state == GameState.START_MENU) {
            drawStartMenu(g);
        } else {
            arena.draw(g);
            player1.draw(g);
            player2.draw(g);
            drawParticles(g);
            drawHUD(g);

            if (state == GameState.COUNTDOWN) drawCountdown(g);
            if (state == GameState.OVER)      drawWinScreen(g);

            if (flashAlpha > 0) {
                g.setColor(new Color(255, 255, 255, Math.min(flashAlpha, 180)));
                g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
            }
        }

        g.dispose();
        gRaw.drawImage(buffer, 0, 0, null);
    }

    private void drawStartMenu(Graphics2D g) {
        // Background
        g.setColor(new Color(10, 10, 25));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        
        // Title
        drawDropShadowText(g, "ARROW BRAWL", GameWindow.WIDTH/2, GameWindow.HEIGHT/2 - 80, 
                           new Font("Monospaced", Font.BOLD, 80), Color.CYAN, 8);

        // Subtitle / Instructions
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.setColor(Color.GRAY);
        String p1Keys = "P1: WASD to Move, T to Shoot";
        String p2Keys = "P2: ARROWS to Move, I to Shoot";
        g.drawString(p1Keys, GameWindow.WIDTH/2 - g.getFontMetrics().stringWidth(p1Keys)/2, GameWindow.HEIGHT/2 + 80);
        g.drawString(p2Keys, GameWindow.WIDTH/2 - g.getFontMetrics().stringWidth(p2Keys)/2, GameWindow.HEIGHT/2 + 110);

        // Pulse "Press Enter"
        int alpha = (int)(pulse * 255);
        drawDropShadowText(g, "PRESS [ENTER] TO PLAY", GameWindow.WIDTH/2, GameWindow.HEIGHT/2 + 20, 
                           new Font("Monospaced", Font.BOLD, 32), new Color(255, 255, 255, alpha), 4);
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            g.setColor(new Color(p.r, p.grn, p.b, Math.max(0, Math.min(255, p.alpha))));
            g.fillRect((int) p.x, (int) p.y, p.size, p.size);
        }
    }

    private void drawHUD(Graphics2D g) {
        drawPlayerBanner(g, 10, 10, "P1 — WASD+TFGH/RYVB", new Color(60, 120, 255), player1.isAlive());
        drawPlayerBanner(g, GameWindow.WIDTH - 310, 10, "P2 — ARROWS+IJKL/UOMN", new Color(255, 70, 70), player2.isAlive());
    }

    private void drawPlayerBanner(Graphics2D g, int x, int y, String label, Color col, boolean alive) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(x, y, 300, 28, 8, 8);
        g.setColor(alive ? col : new Color(80, 80, 80));
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.drawString(label, x + 8, y + 19);
        g.fillOval(x + 284, y + 8, 10, 10);
    }

    private void drawCountdown(Graphics2D g) {
        int secondsLeft = 3 - (countdownTick / 60);
        String text = secondsLeft > 0 ? String.valueOf(secondsLeft) : "FIGHT!";
        drawDropShadowText(g, text, GameWindow.WIDTH / 2, GameWindow.HEIGHT / 2 - 20,
                           new Font("Monospaced", Font.BOLD, 96), Color.WHITE, 5);
    }

    private void drawWinScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        Color winCol = (winner == 1) ? new Color(80, 150, 255) : new Color(255, 80, 80);
        drawDropShadowText(g, winnerText, GameWindow.WIDTH / 2, GameWindow.HEIGHT / 2 - 40,
                           new Font("Monospaced", Font.BOLD, 72), winCol, 6);
        drawDropShadowText(g, "Press  R  to  Play  Again", GameWindow.WIDTH / 2, GameWindow.HEIGHT / 2 + 60,
                           new Font("Monospaced", Font.PLAIN, 26), new Color(200, 200, 200), 3);
    }

    private void drawDropShadowText(Graphics2D g, String text, int cx, int cy, Font font, Color col, int shadowOffset) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(text) / 2;
        int ty = cy + fm.getAscent() / 2;
        g.setColor(new Color(0, 0, 0, Math.min(col.getAlpha(), 180)));
        g.drawString(text, tx + shadowOffset, ty + shadowOffset);
        g.setColor(col);
        g.drawString(text, tx, ty);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Start game from Menu
        if (state == GameState.START_MENU && code == KeyEvent.VK_ENTER) {
            initGame();
            return;
        }

        // Restart from Win Screen
        if (state == GameState.OVER && code == KeyEvent.VK_R) {
            initGame();
            return;
        }

        player1.keyPressed(code);
        player2.keyPressed(code);
    }

    @Override public void keyReleased(KeyEvent e) {
        player1.keyReleased(e.getKeyCode());
        player2.keyReleased(e.getKeyCode());
    }
    @Override public void keyTyped(KeyEvent e) {}

    private static class Particle {
        float x, y, vx, vy;
        int size, r, grn, b, alpha;
        Particle(float x, float y, float vx, float vy, int size, Color col) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size;
            this.r = col.getRed(); this.grn = col.getGreen(); this.b = col.getBlue();
            this.alpha = 255;
        }
        void update() {
            x += vx; y += vy; vy += 0.25f; vx *= 0.95f; alpha -= 6;
        }
    }
}