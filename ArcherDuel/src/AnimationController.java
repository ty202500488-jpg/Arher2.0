/**
 * AnimationController v2
 * Supports the new DASH animation row.
 */
public class AnimationController {

    private int currentAnim  = SpriteRenderer.ANIM_IDLE;
    private int currentFrame = 0;
    private int tickCounter  = 0;
    private int ticksPerFrame;

    // Ticks per frame for each animation row
    private static final int[] TICKS_PER_FRAME = {
        8,   // 0 IDLE  — slow breathing
        5,   // 1 WALK  — moderate pace
        5,   // 2 SHOOT — slightly slow for clarity
        7,   // 3 DEATH — slow dramatic
        3    // 4 DASH  — very fast
    };

    private static final boolean[] LOOPS = {true, true, false, false, true};

    private boolean finished = false;

    public AnimationController() {
        ticksPerFrame = TICKS_PER_FRAME[currentAnim];
    }

    /** Set the active animation. Resets to frame 0 if changed. */
    public void setAnimation(int animRow) {
        if (animRow == currentAnim) return;
        currentAnim   = animRow;
        currentFrame  = 0;
        tickCounter   = 0;
        ticksPerFrame = TICKS_PER_FRAME[animRow];
        finished      = false;
    }

    /** Advance one game tick. */
    public void tick() {
        if (finished) return;
        tickCounter++;
        if (tickCounter >= ticksPerFrame) {
            tickCounter = 0;
            currentFrame++;
            int maxFrames = SpriteRenderer.FRAME_COUNTS[currentAnim];
            if (currentFrame >= maxFrames) {
                if (LOOPS[currentAnim]) {
                    currentFrame = 0;
                } else {
                    currentFrame = maxFrames - 1;
                    finished = true;
                }
            }
        }
    }

    public int     getAnimation() { return currentAnim;  }
    public int     getFrame()     { return currentFrame; }
    public boolean isFinished()   { return finished;     }
}
