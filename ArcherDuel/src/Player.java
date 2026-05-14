import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Player v5
 * P1: A/D=Move W/SPACE=Jump F=Shoot LEFT_SHIFT=Dash
 * P2: Arrows=Move UP=Jump RIGHT_CTRL=Shoot RIGHT_SHIFT=Dash
 * (Right CTRL/SHIFT injected from GamePanel via setFirePressed/setDashPressed)
 */
/**
 * Represents a player in the game. Handles input, physics, animations, and combat.
 */
public class Player {
    // ── Dimensional Constants ─────────────────────────────────────────
    public static final int W = 42, H = 63;      // Player hitbox dimensions
    public static final int MAX_HP = 3;          // Total health points
    public static final int I_FRAMES = 40;       // Duration of invincibility after getting hit

    // ── Physics Constants ─────────────────────────────────────────────
    private static final float BASE_SPD = 4.2f;  // Normal walking speed
    private static final float JUMP_POW = -13f;  // Initial vertical velocity for jumps
    private static final float GRAV     = 0.52f; // Gravity force applied per tick
    private static final float MAX_FALL = 15f;   // Terminal velocity to prevent falling too fast
    
    // ── Ability Constants ─────────────────────────────────────────────
    private static final int DASH_DUR = 12;      // How many ticks a dash lasts
    private static final int DASH_CD  = 50;      // Ticks before dash can be used again
    private static final int SHOOT_CD = 30;      // Delay between firing arrows
    private static final int MAX_CHARGE = 40;    // Ticks required to reach maximum shot power
    private static final float DASH_SPD = 12f;   // Horizontal velocity during a dash

    // ── Basic State ──────────────────────────────────────────────────
    public final int playerIndex;     // 0 for Player 1, 1 for Player 2
    public float x, y;               // Current world position
    private float vy = 0;            // Current vertical velocity
    public boolean onGround = false;  // Whether the player is touching a platform
    public boolean alive = true;      // Whether the player is still in the match
    public boolean rageMode = false;  // Visual effect when health is low
    public int hp = MAX_HP;           // Current health
    public int shakeRequest = 0;      // Screen shake trigger on hit

    // ── Statistics ────────────────────────────────────────────────────
    public int arrowsFired = 0;
    public int arrowsHit = 0;
    public int headshots = 0;

    // ── Movement & Action State ───────────────────────────────────────
    private boolean facingRight;      // Direction for sprite flipping and shooting
    private int shootCD = 0;          // Ticks remaining until next shot
    private int shootAnim = 0;        // Ticks remaining for the "shooting" animation frame
    private int iFrames = 0;          // Ticks remaining for invincibility
    private int dashTimer = 0;        // Ticks remaining in current dash
    private int dashCD = 0;           // Ticks remaining until next dash
    private float dashDX = 0;         // Velocity applied during dash
    private boolean isDashing = false;
    private boolean charging = false;  // Whether the player is currently holding the fire button
    private int chargeTicks = 0;      // How long the shot has been held
    private float speedMult = 1f;     // Speed modifier (for power-ups)
    private boolean iceMode = false;  // Frictionless movement flag
    private float iceVX = 0;          // Current sliding velocity on ice

    // External fire/dash injection (for RIGHT_CTRL, RIGHT_SHIFT)
    private boolean extFirePressed = false, extDashPressed = false;
    private ArrowPool arrowPool;

    private final boolean[] keys = new boolean[65536];
    private final int KEY_L, KEY_R, KEY_UP, KEY_DOWN, KEY_DASH, KEY_FIRE;
    private final boolean useExtFire, useExtDash; // P2 uses external injection

    private int downPressTimer = 0;
    private int dropThroughTimer = 0;

    private final Rectangle bounds = new Rectangle();
    private final Rectangle headshotBounds = new Rectangle();
    private final Rectangle footBox = new Rectangle();

    private final AnimationController anim = new AnimationController();
    private final SpriteRenderer sprites;
    public final List<Arrow> arrows = new ArrayList<>();

    public Player(int idx, float sx, float sy, SpriteRenderer sp, ArrowPool ap) {
        playerIndex = idx;
        x = sx;
        y = sy;
        sprites = sp;
        arrowPool = ap;
        facingRight = (idx == 0);
        if (idx == 0) {
            KEY_L = KeyEvent.VK_A;
            KEY_R = KeyEvent.VK_D;
            KEY_UP = KeyEvent.VK_W;
            KEY_DOWN = KeyEvent.VK_S;
            KEY_DASH = KeyEvent.VK_SHIFT;
            KEY_FIRE = KeyEvent.VK_F;
            useExtFire = false;
            useExtDash = false;
        } else {
            KEY_L = KeyEvent.VK_LEFT;
            KEY_R = KeyEvent.VK_RIGHT;
            KEY_UP = KeyEvent.VK_UP;
            KEY_DOWN = KeyEvent.VK_DOWN;
            KEY_DASH = -1;
            KEY_FIRE = -1; // injected externally
            useExtFire = true;
            useExtDash = true;
        }
    }

    // External injection for RIGHT_CTRL / RIGHT_SHIFT (P2)
    public void setFirePressed(boolean v) {
        extFirePressed = v;
        if (!v && charging)
            releaseShot();
    }

    public void setDashPressed(boolean v) {
        extDashPressed = v;
    }

    public void setSpeedMult(float m) {
        speedMult = m;
    }

    public void setIceMode(boolean b) {
        iceMode = b;
    }

    public void keyPressed(int c) {
        if (c < keys.length) {
            if (!keys[c] && c == KEY_DOWN) {
                if (downPressTimer > 0) {
                    dropThroughTimer = 15;
                    downPressTimer = 0;
                } else {
                    downPressTimer = 15;
                }
            }
            keys[c] = true;
        }
        // P1 alternate jump
        if (playerIndex == 0 && c == KeyEvent.VK_SPACE)
            keys[KEY_UP] = true;
    }

    public void keyReleased(int c) {
        if (c < keys.length)
            keys[c] = false;
        if (playerIndex == 0 && c == KeyEvent.VK_SPACE)
            keys[KEY_UP] = false;
        if (!useExtFire && c == KEY_FIRE && charging)
            releaseShot();
    }

    /**
     * Logic for the pre-game countdown phase.
     * Players can fall and snap to platforms but cannot move or shoot.
     */
    public void updateGravityOnly(Rectangle[] plats) {
        if (iFrames > 0) iFrames--;
        if (!alive) {
            anim.tick();
            return;
        }
        if (downPressTimer > 0) downPressTimer--;
        if (dropThroughTimer > 0) dropThroughTimer--;

        // Apply standard gravity
        vy = Math.min(vy + GRAV, MAX_FALL);
        y += vy;
        
        // Collision check
        onGround = false;
        footBox.setBounds((int) x, (int) y + H - 4, W, 8);
        for (Rectangle r : plats) {
            if (dropThroughTimer > 0 && r.height < 50) continue; // Allow dropping through thin platforms
            if (footBox.intersects(r) && vy >= 0) {
                y = r.y - H;
                vy = 0;
                onGround = true;
                break;
            }
        }
        
        // Screen bounds
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));
        
        anim.setAnimation(SpriteRenderer.ANIM_IDLE);
        anim.tick();
        
        // Arrows from previous round might still be flying
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);
    }

    /**
     * Main update loop for the player.
     * Processes input, movement, combat, and animation states.
     */
    public void update(Rectangle[] plats) {
        if (downPressTimer > 0) downPressTimer--;
        if (dropThroughTimer > 0) dropThroughTimer--;

        if (iFrames > 0) iFrames--;
        
        if (!alive) {
            anim.tick();
            arrows.forEach(Arrow::update);
            arrows.removeIf(a -> !a.active);
            return;
        }

        updateDash();     // Handle dash ability
        updateMove(plats); // Handle walking, jumping, and platform collision
        updateCharge();   // Handle arrow charging logic
        
        if (shootCD > 0) shootCD--;
        if (shootAnim > 0) shootAnim--;
        
        updateAnim();     // Determine which animation to play
        
        // Update projectiles owned by this player
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);
        
        anim.tick();      // Advance animation frame
    }

    private void updateDash() {
        if (dashCD > 0)
            dashCD--;
        if (isDashing) {
            dashTimer--;
            x += dashDX;
            x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));
            if (dashTimer <= 0)
                isDashing = false;
            return;
        }
        boolean dashKey = useExtDash ? extDashPressed : (KEY_DASH != -1 && keys[KEY_DASH]);
        if (dashKey && dashCD == 0) {
            isDashing = true;
            dashTimer = DASH_DUR;
            dashCD = DASH_CD;
            dashDX = facingRight ? DASH_SPD : -DASH_SPD;
            AudioManager.play(AudioManager.Sound.DASH);
        }
    }

    private void updateMove(Rectangle[] plats) {
        if (isDashing)
            return;
        float spd = BASE_SPD * speedMult, mvX = 0;
        if (keys[KEY_L]) {
            mvX -= spd;
            facingRight = false;
        }
        if (keys[KEY_R]) {
            mvX += spd;
            facingRight = true;
        }
        if (iceMode && onGround) {
            iceVX += mvX * 0.18f;
            iceVX *= 0.91f;
            x += iceVX;
        } else {
            iceVX = 0;
            x += mvX;
        }
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));
        if (keys[KEY_UP] && onGround) {
            vy = JUMP_POW;
            onGround = false;
            AudioManager.play(AudioManager.Sound.JUMP);
        }
        vy = Math.min(vy + GRAV, MAX_FALL);
        y += vy;
        onGround = false;
        footBox.setBounds((int) x, (int) y + H - 4, W, 8);
        for (Rectangle r : plats) {
            if (dropThroughTimer > 0 && r.height < 50) continue;
            if (footBox.intersects(r) && vy >= 0) {
                y = r.y - H;
                vy = 0;
                onGround = true;
                break;
            }
        }
    }

    private void updateCharge() {
        if (shootCD > 0)
            return;
        boolean fire = useExtFire ? extFirePressed : (KEY_FIRE != -1 && keys[KEY_FIRE]);
        if (fire) {
            if (!charging)
                charging = true;
            chargeTicks = Math.min(chargeTicks + 1, MAX_CHARGE);
            if (chargeTicks >= MAX_CHARGE)
                releaseShot();
        } else {
            if (!charging)
                chargeTicks = 0;
        }
    }

    /**
     * Finalizes and fires an arrow.
     * Speed and trajectory are calculated based on the charge time.
     */
    private void releaseShot() {
        if (!alive) return;
        
        // Calculate shot power (0.0 to 1.0)
        float ch = Math.min(1.0f, chargeTicks / (float)MAX_CHARGE);
        
        // Interpolate speed between base and max based on charge
        float s = Arrow.BASE_SPEED + (Arrow.MAX_SPEED - Arrow.BASE_SPEED) * ch;
        float dx = facingRight ? s : -s;
        
        // Slight upward arc for all shots to make them feel more natural
        float dy = -1.5f - (ch * 2f);
        
        // Obtain an arrow from the global object pool to save memory
        Arrow a = arrowPool.obtain(x + W/2, y + H/3, dx, dy, playerIndex);
        arrows.add(a);
        
        // Reset charging state
        charging = false;
        chargeTicks = 0;
        shootCD = SHOOT_CD;
        shootAnim = 15; // Show shooting pose for 15 ticks
        arrowsFired++;
        
        AudioManager.play(AudioManager.Sound.SHOOT);
    }

    private void updateAnim() {
        float mx = (keys[KEY_L] ? -1 : 0) + (keys[KEY_R] ? 1 : 0);
        if (!alive)
            anim.setAnimation(SpriteRenderer.ANIM_DEATH);
        else if (isDashing)
            anim.setAnimation(SpriteRenderer.ANIM_DASH);
        else if (charging || shootAnim > 0)
            anim.setAnimation(SpriteRenderer.ANIM_SHOOT);
        else if (Math.abs(mx) > 0.1f)
            anim.setAnimation(SpriteRenderer.ANIM_WALK);
        else
            anim.setAnimation(SpriteRenderer.ANIM_IDLE);
    }

    public boolean isInvincible() {
        return iFrames > 0 || isDashing;
    }

    public boolean takeHit() {
        if (isInvincible())
            return false;
        hp--;
        iFrames = I_FRAMES;
        shakeRequest = 3;
        rageMode = (hp <= 1);
        if (hp <= 0) {
            die();
            return true;
        }
        AudioManager.play(AudioManager.Sound.HIT);
        return false;
    }

    public void die() {
        alive = false;
        hp = 0;
        anim.setAnimation(SpriteRenderer.ANIM_DEATH);
    }

    public Rectangle getBounds() {
        bounds.setBounds((int) x, (int) y, W, H);
        return bounds;
    }

    public Rectangle getHeadshotBounds() {
        int hbH = H / 3;
        headshotBounds.setBounds((int) x, (int) y, W, hbH);
        return headshotBounds;
    }

    public boolean isAlive() {
        return alive;
    }

    public float getCenterX() {
        return x + W / 2f;
    }

    public float getCenterY() {
        return y + H / 2f;
    }

    public float getChargeRatio() {
        return chargeTicks / (float) MAX_CHARGE;
    }

    public void draw(Graphics2D g) {
        if (isDashing) {
            Color gh = (playerIndex == 0) ? new Color(80, 160, 255, 55) : new Color(255, 100, 40, 55);
            g.setColor(gh);
            g.fillRect((int) (x - dashDX), (int) y, W, H);
        }
        if (rageMode && alive) {
            long t = System.currentTimeMillis();
            float p = (float) (Math.sin(t / 120.0) * 0.5 + 0.5);
            Color rc = (playerIndex == 0) ? new Color(0, 140, 255, (int) (p * 80))
                    : new Color(255, 60, 0, (int) (p * 80));
            g.setColor(rc);
            g.fillOval((int) x - 10, (int) y - 10, W + 20, H + 20);
        }
        if (iFrames > 0 && (iFrames / 4) % 2 == 1) {
            for (Arrow a : arrows)
                a.draw(g);
            return;
        }
        BufferedImage fr = sprites.getFrame(playerIndex, anim.getAnimation(), anim.getFrame(), facingRight);
        g.drawImage(fr, (int) x + W / 2 - SpriteRenderer.FRAME_W / 2, (int) y + H - SpriteRenderer.FRAME_H + 4, null);
        if (charging && chargeTicks > 0) {
            float r = getChargeRatio();
            Color cc = r < 0.5f ? new Color(255, 220, 0) : r < 0.85f ? new Color(255, 140, 0) : new Color(255, 50, 50);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect((int) x - 1, (int) y - 14, W + 2, 9);
            g.setColor(cc);
            g.fillRect((int) x, (int) y - 13, (int) (W * r), 7);
        }
        if (shootCD > 0) {
            float cd = shootCD / (float) SHOOT_CD;
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect((int) x, (int) y + H + 4, W, 5);
            g.setColor(new Color(80, 200, 255));
            g.fillRect((int) x, (int) y + H + 4, (int) (W * (1 - cd)), 5);
        }
        for (Arrow a : arrows)
            a.draw(g);
    }
}