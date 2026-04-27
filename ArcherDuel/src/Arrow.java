import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Arrow
 * Represents a fired projectile from either player.
 * Moves in one of 8 directions at constant speed.
 * Drawn as a pixel-art style arrow with rotation.
 */
public class Arrow {

    // ── constants ────────────────────────────────────────────────
    public static final int   SIZE  = 18;   // bounding box for collision
    public static final float SPEED = 11f;  // pixels per tick

    // ── state ────────────────────────────────────────────────────
    public float x, y;
    public float dx, dy;         // velocity components (normalized × SPEED)
    public int   ownerIndex;     // 0 = player1, 1 = player2
    public boolean active = true;

    // visual angle for drawing
    private double angle;

    // ── constructor ──────────────────────────────────────────────
    public Arrow(float startX, float startY, int dirX, int dirY, int ownerIndex) {
        this.x          = startX;
        this.y          = startY;
        this.ownerIndex = ownerIndex;

        // Normalise diagonal movement so all directions travel same speed
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        this.dx = (dirX / len) * SPEED;
        this.dy = (dirY / len) * SPEED;

        // Pre-compute draw angle from velocity vector
        this.angle = Math.atan2(dy, dx);
    }

    // ── update ───────────────────────────────────────────────────
    public void update() {
        x += dx;
        y += dy;

        // Deactivate if arrow leaves screen bounds
        if (x < -SIZE || x > GameWindow.WIDTH + SIZE
                || y < -SIZE || y > GameWindow.HEIGHT + SIZE) {
            active = false;
        }
    }

    // ── collision rectangle ───────────────────────────────────────
    public Rectangle getBounds() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    // ── rendering ────────────────────────────────────────────────
    public void draw(Graphics2D g) {
        if (!active) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Translate/rotate to arrow position
        g2.translate((int) x, (int) y);
        g2.rotate(angle);

        drawPixelArrow(g2);

        g2.dispose();
    }

    /**
     * Draws a pixel-art style arrow centered at origin, pointing right (+X).
     * The rotation applied above will orient it correctly.
     */
    private void drawPixelArrow(Graphics2D g) {
        // Arrow shaft color (yellow-white)
        Color shaft  = new Color(255, 230, 80);
        Color tip    = new Color(200, 200, 220);
        Color flight = ownerIndex == 0 ? new Color(80, 140, 255) : new Color(255, 80, 80);

        int[] shaftX = {-8, 4};
        int[] shaftY = {0, 0};

        // Shaft
        g.setColor(shaft);
        g.setStroke(new BasicStroke(2));
        g.drawLine(-8, 0, 5, 0);

        // Tip (arrowhead triangle)
        g.setColor(tip);
        int[] hx = {5, 9, 5};
        int[] hy = {-3, 0, 3};
        g.fillPolygon(hx, hy, 3);

        // Fletching (colored feathers at tail)
        g.setColor(flight);
        g.fillRect(-9, -3, 3, 2);
        g.fillRect(-9,  1, 3, 2);
    }
}
