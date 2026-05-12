import java.awt.*;

/**
 * Arrow v2 — Physics-based projectile.
 * Features: gravity drop, wind drift, charge-based speed, arrow trail, headshot
 * zone.
 */
public class Arrow {

    // ── constants ─────────────────────────────────────────────────
    public static final int SIZE = 22; // collision bounding box
    public static final float BASE_SPEED = 11f;
    public static final float MAX_SPEED  = 17f;
    public static final float GRAVITY    = 0.22f; // arrow gravity drop per tick
    public static final float WIND_FACTOR = 0.04f; // scale of wind applied per tick

    // ── state ─────────────────────────────────────────────────────
    public float x, y;
    public float dx, dy;
    public int ownerIndex;
    public boolean active = false;
    public boolean isHeadshot = false; 

    private final Rectangle bounds = new Rectangle();
    private float charge;
    
    public Arrow() {}

    public void reset(float x, float y, float dx, float dy, int owner, float ch) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.ownerIndex = owner;
        this.charge = ch;
        this.active = true;
        this.isHeadshot = false;
        this.trailHead = 0;
        this.trailSize = 0;
    }

    // Trail — circular buffer, avoids ArrayList prepend overhead
    private static final int TRAIL_LEN = 12;
    private final float[] trailX = new float[TRAIL_LEN];
    private final float[] trailY = new float[TRAIL_LEN];
    private int trailHead = 0;  // index of newest entry
    private int trailSize = 0;  // how many entries filled so far

    // Pre-cached trail colors per owner (index 0=P1, 1=P2), keyed by alpha step
    private static final Color[] TRAIL_P1 = new Color[TRAIL_LEN];
    private static final Color[] TRAIL_P2 = new Color[TRAIL_LEN];
    static {
        for (int i = 0; i < TRAIL_LEN; i++) {
            float a = 1f - (float) i / TRAIL_LEN;
            TRAIL_P1[i] = new Color(80, 180, 255, (int)(a * 120));
            TRAIL_P2[i] = new Color(255, 130, 40, (int)(a * 120));
        }
    }

    // Wind reference (set per-match from GamePanel)
    public static float windX = 0f;
    public static float windY = 0f;

    // ── update ────────────────────────────────────────────
    public void update() {
        // Store trail point in circular buffer (newest at head)
        trailHead = (trailHead == 0) ? TRAIL_LEN - 1 : trailHead - 1;
        trailX[trailHead] = x;
        trailY[trailHead] = y;
        if (trailSize < TRAIL_LEN) trailSize++;

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
        bounds.setBounds((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        return bounds;
    }

    /** Headshot zone — upper portion of target bounds */
    public static Rectangle getHeadshotBounds(Rectangle playerBounds) {
        int headH = playerBounds.height / 3;
        return new Rectangle(playerBounds.x, playerBounds.y, playerBounds.width, headH);
    }

    // ── rendering ────────────────────────────────────────────
    public void draw(Graphics2D g) {
        if (!active)
            return;

        // Draw trail from circular buffer
        Color[] trailColors = (ownerIndex == 0) ? TRAIL_P1 : TRAIL_P2;
        for (int i = 0; i < trailSize; i++) {
            int idx = (trailHead + i) % TRAIL_LEN;
            float alpha = 1f - (float) i / TRAIL_LEN;
            int sz = Math.max(2, (int) (6 * alpha));
            g.setColor(trailColors[i]);
            g.fillOval((int) trailX[idx] - sz / 2, (int) trailY[idx] - sz / 2, sz, sz);
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
