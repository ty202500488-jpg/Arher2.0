import java.awt.*;
import java.util.Random;

/**
 * MapHazard — environmental hazards.
 * Each hazard type corresponds to specific maps:
 * FIREBALL → Volcano
 * LAVA_POOL → Volcano
 * ICE_PATCH → Ice Cavern
 * SPIKE → future Trap Dungeon
 */
public class MapHazard {

    public enum Type {
        FIREBALL, LAVA_POOL, ICE_PATCH, FALLING_ROCK
    }

    public float x, y, vx, vy;
    public int w, h;
    public Type type;
    public boolean active = true;
    public int lifetime = 0; // ticks alive
    private final Rectangle bounds = new Rectangle();

    // ── Static Color constants (avoid per-frame allocation) ────────
    private static final Color FIRE_GLOW = new Color(255, 120, 0, 80);
    private static final Color FIRE_HOT  = new Color(255, 255, 100, 180);
    private static final Color ICE_FILL  = new Color(180, 230, 255, 140);
    private static final Color ICE_BRD   = new Color(220, 245, 255, 200);
    private static final Color ICE_SHN   = new Color(255, 255, 255, 100);
    private static final Color ROCK_FILL = new Color(90, 80, 70);
    private static final Color ROCK_BRD  = new Color(120, 110, 100);

    public MapHazard(float x, float y, float vx, float vy, int w, int h, Type type) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.w = w;
        this.h = h;
        this.type = type;
    }

    public void update() {
        lifetime++;
        x += vx;
        y += vy;
        if (type == Type.FIREBALL || type == Type.FALLING_ROCK) {
            vy += 0.35f; // gravity
        }
        // Deactivate off-screen
        if (y > GameWindow.HEIGHT + 50 || x < -100 || x > GameWindow.WIDTH + 100)
            active = false;
        // Lava pools persist, but fireballs die on ground hit
        if ((type == Type.FIREBALL || type == Type.FALLING_ROCK)
                && y >= Arena.GROUND_Y - h) {
            y = Arena.GROUND_Y - h;
            vx *= 0.4f;
            vy = 0;
            if (lifetime > 20)
                active = false;
        }
    }

    public Rectangle getBounds() {
        bounds.setBounds((int) x, (int) y, w, h);
        return bounds;
    }

    public void draw(Graphics2D g) {
        long t = System.currentTimeMillis();
        switch (type) {
            case FIREBALL -> drawFireball(g, t);
            case LAVA_POOL -> drawLavaPool(g, t);
            case ICE_PATCH -> drawIcePatch(g);
            case FALLING_ROCK -> drawRock(g, t);
        }
    }

    private void drawFireball(Graphics2D g, long t) {
        float p = (float) (Math.sin(t / 80.0) * 0.5 + 0.5);
        // Outer glow
        g.setColor(FIRE_GLOW);
        g.fillOval((int) x - 6, (int) y - 6, w + 12, h + 12);
        // Core
        g.setColor(new Color(255, 80 + (int) (p * 80), 0));
        g.fillOval((int) x, (int) y, w, h);
        // Inner hot
        g.setColor(FIRE_HOT);
        g.fillOval((int) x + 4, (int) y + 4, w - 8, h - 8);
    }

    private void drawLavaPool(Graphics2D g, long t) {
        float p = (float) (Math.sin(t / 400.0 + x * 0.01) * 0.3 + 0.7);
        g.setColor(new Color(200, 50, 0, (int) (p * 200)));
        g.fillRect((int) x, (int) y, w, h);
        g.setColor(new Color(255, 160, 0, (int) (p * 160)));
        g.fillRect((int) x + 4, (int) y + 2, w - 8, h / 3);
    }

    private void drawIcePatch(Graphics2D g) {
        g.setColor(ICE_FILL);
        g.fillRoundRect((int) x, (int) y, w, h, 8, 8);
        g.setColor(ICE_BRD);
        g.drawRoundRect((int) x, (int) y, w, h, 8, 8);
        // Shine
        g.setColor(ICE_SHN);
        g.fillOval((int) x + 6, (int) y + 3, w / 3, h / 3);
    }

    private void drawRock(Graphics2D g, long t) {
        g.setColor(ROCK_FILL);
        int[] rx = { (int) x, (int) x + w / 2, (int) x + w, (int) x + w * 3 / 4, (int) x + w / 4 };
        int[] ry = { (int) y + h, (int) y, (int) y + h / 2, (int) y + h, (int) y + h };
        g.fillPolygon(rx, ry, 5);
        g.setColor(ROCK_BRD);
        g.drawPolygon(rx, ry, 5);
    }

    // ── Static factory methods ─────────────────────────────────────

    /** Spawn a fireball from the top of the screen. */
    public static MapHazard spawnFireball(Random rng) {
        float sx = 100 + rng.nextFloat() * (GameWindow.WIDTH - 200);
        float vx = (rng.nextFloat() - 0.5f) * 2f;
        return new MapHazard(sx, -30, vx, 2f + rng.nextFloat() * 2f, 24, 24, Type.FIREBALL);
    }

    /** Spawn a falling rock. */
    public static MapHazard spawnRock(Random rng) {
        float sx = 100 + rng.nextFloat() * (GameWindow.WIDTH - 200);
        return new MapHazard(sx, -20, (rng.nextFloat() - 0.5f) * 1.5f, 1.5f, 30, 20, Type.FALLING_ROCK);
    }

    /** Create a static lava pool at given position. */
    public static MapHazard makeLavaPool(int x, int y, int w) {
        MapHazard h = new MapHazard(x, y, 0, 0, w, 18, Type.LAVA_POOL);
        return h;
    }

    /** Create a static ice patch at given position. */
    public static MapHazard makeIcePatch(int x, int y, int w) {
        return new MapHazard(x, y, 0, 0, w, 10, Type.ICE_PATCH);
    }
}
