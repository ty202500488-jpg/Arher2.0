import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class ParticlePool {
    private final List<Particle> pool;
    private static final int INITIAL_SIZE = 300;

    public ParticlePool() {
        pool = new ArrayList<>(INITIAL_SIZE);
        for (int i = 0; i < INITIAL_SIZE; i++) {
            pool.add(new Particle());
        }
    }

    public Particle obtain(float x, float y, float vx, float vy, int sz, Color c) {
        for (Particle p : pool) {
            if (!p.active) {
                p.reset(x, y, vx, vy, sz, c);
                return p;
            }
        }
        // Expand if needed
        Particle p = new Particle();
        p.reset(x, y, vx, vy, sz, c);
        pool.add(p);
        return p;
    }

    public List<Particle> getPool() {
        return pool;
    }
}
