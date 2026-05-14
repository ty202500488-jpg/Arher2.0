import java.util.ArrayList;
import java.util.List;

/**
 * Manages the lifecycle of arrow projectiles using an object pool.
 * Reuses inactive arrows to reduce garbage collection overhead.
 */
public class ArrowPool {
    private final List<Arrow> pool;
    private static final int INITIAL_SIZE = 50;

    public ArrowPool() {
        pool = new ArrayList<>(INITIAL_SIZE);
        for (int i = 0; i < INITIAL_SIZE; i++) {
            pool.add(new Arrow());
        }
    }

    /**
     * Retrieves an arrow from the pool, resetting it with the given parameters.
     * If no inactive arrows are available, creates a new one.
     */
    public Arrow obtain(float x, float y, float dx, float dy, int owner) {
        for (Arrow a : pool) {
            if (!a.active) {
                a.reset(x, y, dx, dy, owner);
                return a;
            }
        }
        // Expand if needed
        Arrow a = new Arrow();
        a.reset(x, y, dx, dy, owner);
        pool.add(a);
        return a;
    }

    public List<Arrow> getPool() {
        return pool;
    }
}
