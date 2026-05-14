import java.awt.*;

/**
 * Arrow v2 — Physics-based projectile.
 * Features: gravity drop, wind drift, charge-based speed, arrow trail, headshot
 * zone.
 */
public class Arrow {

    // ── constants ─────────────────────────────────────────────────
    /**
     * Physics and Collision Constants
     */
    public static final int SIZE = 22;           // The size of the arrow's collision bounding box
    public static final float BASE_SPEED = 11f;  // Initial velocity when fired
    public static final float MAX_SPEED  = 17f;  // Cap for arrow speed (e.g., from power-ups or wind)
    public static final float GRAVITY    = 0.22f; // Downward acceleration applied every tick
    public static final float WIND_FACTOR = 0.04f; // How much horizontal wind affects the arrow trajectory

    // ── Arrow State ──────────────────────────────────────────────────
    public float x, y;          // Current position in world space
    public float dx, dy;        // Horizontal and vertical velocity components
    public int ownerIndex;      // ID of the player who fired this arrow (0 or 1)
    public boolean active = false; // Whether the arrow is currently flying in the arena
    public boolean isHeadshot = false; // Flag set if the arrow hit a player's head area

    /**
     * Bounding box used for collision detection with players and platforms.
     */
    private final Rectangle bounds = new Rectangle();
    
    public Arrow() {}

    /**
     * Re-initializes an arrow from the pool.
     * @param x Starting X coordinate (usually the player's position)
     * @param y Starting Y coordinate
     * @param dx Initial horizontal velocity
     * @param dy Initial vertical velocity
     * @param owner The index of the player firing the arrow
     */
    public void reset(float x, float y, float dx, float dy, int owner) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.ownerIndex = owner;
        this.active = true;
        this.isHeadshot = false;
        this.trailHead = 0;
        this.trailSize = 0;
    }

    /**
     * Trail System - Uses a circular buffer to avoid memory allocation overhead during flight.
     * This stores the last N positions of the arrow for rendering a motion trail.
     */
    private static final int TRAIL_LEN = 12;
    private final float[] trailX = new float[TRAIL_LEN];
    private final float[] trailY = new float[TRAIL_LEN];
    private int trailHead = 0;  // Index of the most recent position
    private int trailSize = 0;  // Number of valid points currently in the buffer

    /**
     * Pre-cached trail colors to optimize rendering performance.
     */
    private static final Color[] TRAIL_P1 = new Color[TRAIL_LEN];
    private static final Color[] TRAIL_P2 = new Color[TRAIL_LEN];
    static {
        for (int i = 0; i < TRAIL_LEN; i++) {
            float a = 1f - (float) i / TRAIL_LEN;
            TRAIL_P1[i] = new Color(80, 180, 255, (int)(a * 120)); // Blue trail for P1
            TRAIL_P2[i] = new Color(255, 130, 40, (int)(a * 120)); // Orange/Red trail for P2
        }
    }

    /**
     * External wind forces affecting the arrow, synced from GamePanel.
     */
    public static float windX = 0f;
    public static float windY = 0f;

    /**
     * Updates the arrow's position based on its velocity and external forces.
     */
    public void update() {
        // 1. Record current position in trail buffer before moving
        trailHead = (trailHead == 0) ? TRAIL_LEN - 1 : trailHead - 1;
        trailX[trailHead] = x;
        trailY[trailHead] = y;
        if (trailSize < TRAIL_LEN) trailSize++;

        // 2. Apply Gravity
        dy += GRAVITY;

        // 3. Apply Wind drift (scaled by factor to avoid extreme sensitivity)
        dx += windX * WIND_FACTOR;
        dy += windY * WIND_FACTOR;

        // 4. Update Position
        x += dx;
        y += dy;

        // 5. Cleanup - Deactivate if arrow flies too far off-screen
        if (x < -SIZE || x > GameWindow.WIDTH + SIZE
                || y < -SIZE || y > GameWindow.HEIGHT + SIZE) {
            active = false;
        }
    }

    /**
     * Calculates and returns the collision bounds for the arrow.
     * Centers the rectangle on the arrow's current position.
     */
    public Rectangle getBounds() {
        bounds.setBounds((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        return bounds;
    }

    /** 
     * Headshot zone — Defines the upper portion of a player's hitbox.
     * Being hit here might trigger special logic (if implemented).
     */
    public static Rectangle getHeadshotBounds(Rectangle playerBounds) {
        int headH = playerBounds.height / 3;
        return new Rectangle(playerBounds.x, playerBounds.y, playerBounds.width, headH);
    }

    /**
     * Renders the arrow and its motion trail.
     */
    public void draw(Graphics2D g) {
        if (!active)
            return;

        // 1. Draw trail from circular buffer
        Color[] trailColors = (ownerIndex == 0) ? TRAIL_P1 : TRAIL_P2;
        for (int i = 0; i < trailSize; i++) {
            int idx = (trailHead + i) % TRAIL_LEN;
            float alpha = 1f - (float) i / TRAIL_LEN;
            int sz = Math.max(2, (int) (6 * alpha)); // Trail segments get smaller as they get older
            g.setColor(trailColors[i]);
            g.fillOval((int) trailX[idx] - sz / 2, (int) trailY[idx] - sz / 2, sz, sz);
        }

        // 2. Calculate rotation angle based on velocity vector
        double angle = Math.atan2(dy, dx);

        // 3. Create a temporary graphics context for rotation to avoid affecting other drawing
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate((int) x, (int) y); // Move origin to arrow center
        g2.rotate(angle);              // Rotate to face travel direction
        g2.scale(1.4, 1.4);            // Slight scaling for better visibility

        drawPixelArrow(g2);
        g2.dispose(); // Cleanup temp graphics
    }

    /**
     * Draws the actual arrow sprite using geometric shapes.
     */
    private void drawPixelArrow(Graphics2D g) {
        Color shaft = new Color(255, 230, 80); // Gold/Yellow shaft
        Color tip = new Color(210, 210, 230);   // Silver tip
        Color flight = (ownerIndex == 0) ? new Color(80, 160, 255) : new Color(255, 100, 40);

        // Draw Shaft
        g.setColor(shaft);
        g.setStroke(new BasicStroke(2));
        g.drawLine(-8, 0, 5, 0);

        // Draw Tip (Triangle)
        g.setColor(tip);
        int[] hx = { 5, 10, 5 };
        int[] hy = { -3, 0, 3 };
        g.fillPolygon(hx, hy, 3);

        // Draw Fletching (Feathers)
        g.setColor(flight);
        g.fillRect(-9, -3, 4, 2);
        g.fillRect(-9, 1, 4, 2);
        g.setStroke(new BasicStroke(1));
    }
}
