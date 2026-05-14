import java.awt.*;

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
        LAVA_POOL, ICE_PATCH
    }

    public float x, y, vx, vy;
    public int w, h;
    public Type type;
    public boolean active = true;
    public int lifetime = 0; // ticks alive
    private final Rectangle bounds = new Rectangle();

    // ── Static Color constants (avoid per-frame allocation) ────────
    private static final Color ICE_FILL  = new Color(180, 230, 255, 140);
    private static final Color ICE_BRD   = new Color(220, 245, 255, 200);
    private static final Color ICE_SHN   = new Color(255, 255, 255, 100);

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
        // Lava pools persist
        if (type == Type.LAVA_POOL) {
            // Lava pools don't fall or die on ground hit in this implementation
        }
    }

    public Rectangle getBounds() {
        bounds.setBounds((int) x, (int) y, w, h);
        return bounds;
    }

    public void draw(Graphics2D g) {
        long t = System.currentTimeMillis();
        switch (type) {
            case LAVA_POOL -> drawLavaPool(g, t);
            case ICE_PATCH -> drawIcePatch(g);
        }
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

    // ── Static factory methods ─────────────────────────────────────

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
