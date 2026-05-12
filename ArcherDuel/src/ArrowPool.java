import java.util.ArrayList;
import java.util.List;

public class ArrowPool {
    private final List<Arrow> pool;
    private static final int INITIAL_SIZE = 50;

    public ArrowPool() {
        pool = new ArrayList<>(INITIAL_SIZE);
        for (int i = 0; i < INITIAL_SIZE; i++) {
            pool.add(new Arrow());
        }
    }

    public Arrow obtain(float x, float y, float dx, float dy, int owner, float ch) {
        for (Arrow a : pool) {
            if (!a.active) {
                a.reset(x, y, dx, dy, owner, ch);
                return a;
            }
        }
        // Expand if needed
        Arrow a = new Arrow();
        a.reset(x, y, dx, dy, owner, ch);
        pool.add(a);
        return a;
    }

    public List<Arrow> getPool() {
        return pool;
    }
}
