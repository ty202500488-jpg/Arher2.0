/**
 * AnimationController
 *
 * Tracks the current animation state (row + frame) for a player sprite.
 * Handles frame advance timing and animation transitions.
 */
public class AnimationController {

    private int  currentAnim  = SpriteRenderer.ANIM_IDLE;
    private int  currentFrame = 0;
    private int  tickCounter  = 0;
    private int  ticksPerFrame;

    // How many game ticks each frame holds before advancing
    private static final int[] TICKS_PER_FRAME = {
        8,   // Idle — slow breathing
        5,   // Walk — moderate pace
        6,   // Shoot — slightly slow for clarity
        8    // Death — slow dramatic fall
    };

    // Whether the current animation should loop
    private static final boolean[] LOOPS = {true, true, false, false};

    private boolean finished = false; // true after non-looping anim ends

    public AnimationController() {
        ticksPerFrame = TICKS_PER_FRAME[currentAnim];
    }

    // ── public API ────────────────────────────────────────────────

    /** Set the active animation. Resets to frame 0 if changed. */
    public void setAnimation(int animRow) {
        if (animRow == currentAnim) return;
        currentAnim  = animRow;
        currentFrame = 0;
        tickCounter  = 0;
        ticksPerFrame = TICKS_PER_FRAME[animRow];
        finished     = false;
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
                    currentFrame = maxFrames - 1; // hold last frame
                    finished = true;
                }
            }
        }
    }

    public int  getAnimation() { return currentAnim;  }
    public int  getFrame()     { return currentFrame; }
    public boolean isFinished(){ return finished;     }
}
