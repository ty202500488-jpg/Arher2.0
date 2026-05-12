import java.awt.Color;

/**
 * Represents a visual particle effect (blood, sparks, etc.)
 */
public class Particle {
    public float x, y, vx, vy;
    public int size, r, grn, b, alpha;
    public boolean active;

    public Particle() {
        this.active = false;
    }

    public void reset(float x, float y, float vx, float vy, int sz, Color c) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = sz;
        this.r = c.getRed();
        this.grn = c.getGreen();
        this.b = c.getBlue();
        this.alpha = 255;
        this.active = true;
    }

    public void update() {
        if (!active) return;
        x += vx;
        y += vy;
        vy += 0.28f;
        vx *= 0.93f;
        alpha -= 5;
        if (alpha <= 0) active = false;
    }
}
