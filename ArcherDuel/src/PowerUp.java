/**
 * Represents a power-up item in the arena (e.g., Heal or Speed).
 */
public class PowerUp {
    public float x, y;
    public int type; // 0 = Heal, 1 = Rage/Speed
    public boolean active = true;

    public PowerUp(float x, float y, int t) {
        this.x = x;
        this.y = y;
        this.type = t;
    }
}
