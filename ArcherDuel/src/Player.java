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
public class Player {
    public static final int W = 42, H = 63, MAX_HP = 3, I_FRAMES = 40;
    private static final float BASE_SPD = 4.2f, JUMP_POW = -13f, GRAV = 0.52f, MAX_FALL = 15f;
    private static final int DASH_DUR = 12, DASH_CD = 50, SHOOT_CD = 30, MAX_CHARGE = 40;
    private static final float DASH_SPD = 12f;

    public final int playerIndex;
    public float x, y;
    private float vy = 0;
    public boolean onGround = false, alive = true, rageMode = false;
    public int hp = MAX_HP, shakeRequest = 0;
    public int arrowsFired = 0, arrowsHit = 0, headshots = 0;

    private boolean facingRight;
    private int shootCD = 0, shootAnim = 0, iFrames = 0;
    private int dashTimer = 0, dashCD = 0;
    private float dashDX = 0;
    private boolean isDashing = false;
    private boolean charging = false;
    private int chargeTicks = 0;
    private float speedMult = 1f;
    private boolean iceMode = false;
    private float iceVX = 0;

    // External fire/dash injection (for RIGHT_CTRL, RIGHT_SHIFT)
    private boolean extFirePressed = false, extDashPressed = false;

    private final boolean[] keys = new boolean[65536];
    private final int KEY_L, KEY_R, KEY_UP, KEY_DASH, KEY_FIRE;
    private final boolean useExtFire, useExtDash; // P2 uses external injection

    private final AnimationController anim = new AnimationController();
    private final SpriteRenderer sprites;
    public final List<Arrow> arrows = new ArrayList<>();

    public Player(int idx, float sx, float sy, SpriteRenderer sp) {
        playerIndex = idx;
        x = sx;
        y = sy;
        sprites = sp;
        facingRight = (idx == 0);
        if (idx == 0) {
            KEY_L = KeyEvent.VK_A;
            KEY_R = KeyEvent.VK_D;
            KEY_UP = KeyEvent.VK_W;
            KEY_DASH = KeyEvent.VK_SHIFT;
            KEY_FIRE = KeyEvent.VK_F;
            useExtFire = false;
            useExtDash = false;
        } else {
            KEY_L = KeyEvent.VK_LEFT;
            KEY_R = KeyEvent.VK_RIGHT;
            KEY_UP = KeyEvent.VK_UP;
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
        if (c < keys.length)
            keys[c] = true;
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
     * Countdown-phase update: applies gravity and platform snapping only.
     * No movement, jump, dash, or shooting input is read.
     */
    public void updateGravityOnly(Rectangle[] plats) {
        if (iFrames > 0)
            iFrames--;
        if (!alive) {
            anim.tick();
            return;
        }
        // Gravity
        vy = Math.min(vy + GRAV, MAX_FALL);
        y += vy;
        onGround = false;
        for (Rectangle r : plats) {
            Rectangle f = new Rectangle((int) x, (int) y + H - 4, W, 8);
            if (f.intersects(r) && vy >= 0) {
                y = r.y - H;
                vy = 0;
                onGround = true;
                break;
            }
        }
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));
        anim.setAnimation(SpriteRenderer.ANIM_IDLE);
        anim.tick();
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);
    }

    public void update(Rectangle[] plats) {
        if (iFrames > 0)
            iFrames--;
        if (!alive) {
            anim.tick();
            arrows.forEach(Arrow::update);
            arrows.removeIf(a -> !a.active);
            return;
        }
        updateDash();
        updateMove(plats);
        updateCharge();
        if (shootCD > 0)
            shootCD--;
        if (shootAnim > 0)
            shootAnim--;
        updateAnim();
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);
        anim.tick();
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
        for (Rectangle r : plats) {
            Rectangle f = new Rectangle((int) x, (int) y + H - 4, W, 8);
            if (f.intersects(r) && vy >= 0) {
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

    private void releaseShot() {
        if (!charging && chargeTicks == 0)
            return;
        float ch = Math.max(0.3f, (float) chargeTicks / MAX_CHARGE);
        arrows.add(new Arrow(x + W / 2f, y + H / 2f, facingRight ? 1 : -1, 0, playerIndex, ch));
        AudioManager.play(AudioManager.Sound.SHOOT);
        shootCD = SHOOT_CD;
        shootAnim = 18;
        charging = false;
        chargeTicks = 0;
        arrowsFired++;
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
        return new Rectangle((int) x, (int) y, W, H);
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