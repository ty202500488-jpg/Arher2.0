import java.awt.Color;

/**
 * Represents a visual particle effect (blood, sparks, etc.)
 */
public class Particle {
    public float x, y, vx, vy;
    public int size, r, grn, b, alpha;

    public Particle(float x, float y, float vx, float vy, int sz, Color c) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = sz;
        this.r = c.getRed();
        this.grn = c.getGreen();
        this.b = c.getBlue();
        this.alpha = 255;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.28f;
        vx *= 0.93f;
        alpha -= 5;
    }
}
