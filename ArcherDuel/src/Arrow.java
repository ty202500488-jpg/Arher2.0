import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Arrow v2 — Physics-based projectile.
 * Features: gravity drop, wind drift, charge-based speed, arrow trail, headshot
 * zone.
 */
public class Arrow {

    // ── constants ─────────────────────────────────────────────────
    public static final int SIZE = 22; // collision bounding box
    public static final float BASE_SPEED = 9f;
    public static final float MAX_SPEED = 22f;
    public static final float GRAVITY = 0.28f; // arrow gravity drop per tick
    public static final float WIND_FACTOR = 0.04f; // scale of wind applied per tick

    // ── state ─────────────────────────────────────────────────────
    public float x, y;
    public float dx, dy;
    public int ownerIndex;
    public boolean active = true;
    public boolean isHeadshot = false; // set when collision checked externally

    // charge 0..1 affects speed
    private final float charge;

    // Trail
    private final List<float[]> trail = new ArrayList<>();
    private static final int TRAIL_LEN = 12;

    // Wind reference (set per-match from GamePanel)
    public static float windX = 0f;
    public static float windY = 0f;

    // ── constructor ───────────────────────────────────────────────
    public Arrow(float startX, float startY, float dirX, float dirY, int ownerIndex, float charge) {
        this.x = startX;
        this.y = startY;
        this.ownerIndex = ownerIndex;
        this.charge = Math.max(0.3f, Math.min(charge, 1f));

        float speed = BASE_SPEED + (MAX_SPEED - BASE_SPEED) * this.charge;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0)
            len = 1;
        this.dx = (dirX / len) * speed;
        this.dy = (dirY / len) * speed;
    }

    // Legacy constructor for compat
    public Arrow(float startX, float startY, int dirX, int dirY, int ownerIndex) {
        this(startX, startY, (float) dirX, (float) dirY, ownerIndex, 0.6f);
    }

    // ── update ────────────────────────────────────────────────────
    public void update() {
        // Store trail point
        trail.add(0, new float[] { x, y });
        if (trail.size() > TRAIL_LEN)
            trail.remove(trail.size() - 1);

        // Gravity
        dy += GRAVITY;

        // Wind drift
        dx += windX * WIND_FACTOR;
        dy += windY * WIND_FACTOR;

        x += dx;
        y += dy;

        // Deactivate off-screen
        if (x < -SIZE || x > GameWindow.WIDTH + SIZE
                || y < -SIZE || y > GameWindow.HEIGHT + SIZE) {
            active = false;
        }
    }

    // ── collision rectangle ───────────────────────────────────────
    public Rectangle getBounds() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    /** Headshot zone — upper portion of target bounds */
    public static Rectangle getHeadshotBounds(Rectangle playerBounds) {
        int headH = playerBounds.height / 3;
        return new Rectangle(playerBounds.x, playerBounds.y, playerBounds.width, headH);
    }

    // ── rendering ─────────────────────────────────────────────────
    public void draw(Graphics2D g) {
        if (!active)
            return;

        // Draw trail
        for (int i = 0; i < trail.size(); i++) {
            float[] pt = trail.get(i);
            float alpha = 1f - (float) i / TRAIL_LEN;
            Color tc = (ownerIndex == 0)
                    ? new Color(80, 180, 255, (int) (alpha * 120))
                    : new Color(255, 130, 40, (int) (alpha * 120));
            int sz = Math.max(2, (int) (6 * alpha));
            g.setColor(tc);
            g.fillOval((int) pt[0] - sz / 2, (int) pt[1] - sz / 2, sz, sz);
        }

        // Draw arrow rotated along velocity
        double angle = Math.atan2(dy, dx);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate((int) x, (int) y);
        g2.rotate(angle);
        g2.scale(1.4, 1.4);

        drawPixelArrow(g2);
        g2.dispose();
    }

    private void drawPixelArrow(Graphics2D g) {
        Color shaft = new Color(255, 230, 80);
        Color tip = new Color(210, 210, 230);
        Color flight = (ownerIndex == 0) ? new Color(80, 160, 255) : new Color(255, 100, 40);

        // Shaft
        g.setColor(shaft);
        g.setStroke(new BasicStroke(2));
        g.drawLine(-8, 0, 5, 0);

        // Tip
        g.setColor(tip);
        int[] hx = { 5, 10, 5 };
        int[] hy = { -3, 0, 3 };
        g.fillPolygon(hx, hy, 3);

        // Fletching
        g.setColor(flight);
        g.fillRect(-9, -3, 4, 2);
        g.fillRect(-9, 1, 4, 2);
        g.setStroke(new BasicStroke(1));
    }
}
