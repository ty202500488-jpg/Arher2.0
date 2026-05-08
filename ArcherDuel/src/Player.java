import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Player v3 — Simplified, intuitive controls.
 *
 * PLAYER 1:
 *   Move  — A / D
 *   Jump  — W
 *   Dash  — Left Shift
 *   Shoot — SPACE  (tap = quick shot, hold = charge, release = fire)
 *   Aim   — hold arrow direction while pressing SPACE:
 *           T=Up  G=Down  F=Left  H=Right  (diagonals R/Y/V/B)
 *
 * PLAYER 2:
 *   Move  — LEFT / RIGHT arrow keys
 *   Jump  — UP arrow key
 *   Dash  — RIGHT CTRL
 *   Shoot — NUMPAD 0 or ENTER  (tap/hold/release same as P1)
 *   Aim   — NumPad 8/2/4/6  + diagonals 7/9/1/3
 */
public class Player {

    public static final int W = 42;
    public static final int H = 63;

    private static final float BASE_SPEED  = 3.8f;
    private static final float JUMP_POWER  = -12f;
    private static final float GRAVITY     = 0.55f;
    private static final float MAX_FALL    = 15f;

    // Dash
    private static final int   DASH_DUR  = 12;
    private static final float DASH_SPD  = 14f;
    private static final int   DASH_CD   = 45;

    // Shoot
    private static final int SHOOT_CD       = 28;
    private static final int MAX_CHARGE     = 45;

    // Health
    public static final int MAX_HP = 3;
    public static final int I_FRAMES = 40;

    // Identity
    public final int playerIndex;

    // Position
    public float   x, y;
    private float  vy       = 0;
    public boolean onGround = false;

    // State
    public boolean alive = true;
    public int     hp    = MAX_HP;
    private boolean facingRight;

    // Cooldowns
    private int shootCD    = 0;
    private int shootAnim  = 0;
    private int iFrames    = 0;

    // Dash
    private int   dashTimer = 0;
    private int   dashCD    = 0;
    private float dashDX    = 0;
    private boolean isDashing = false;

    // Charge shot
    private boolean charging  = false;
    private int     chargeTicks = 0;
    private int     aimX = 0, aimY = 0;

    // Map physics modifiers
    private float speedMult    = 1f;   // ice = 0.85
    private boolean iceMode    = false; // slippery floor
    private float   iceVX      = 0;    // current horizontal momentum on ice

    // Controls
    private final boolean[] keys = new boolean[65536];
    private final int KEY_LEFT, KEY_RIGHT, KEY_UP;
    private final int KEY_DASH, KEY_FIRE;
    private final int KEY_AIM_U, KEY_AIM_D, KEY_AIM_L, KEY_AIM_R;
    private final int KEY_AIM_UL, KEY_AIM_UR, KEY_AIM_DL, KEY_AIM_DR;

    // Animation & rendering
    private final AnimationController anim = new AnimationController();
    private final SpriteRenderer sprites;

    // Arrows
    public final List<Arrow> arrows = new ArrayList<>();

    // Public flags
    public boolean rageMode    = false;
    public int     shakeRequest = 0;

    // ── Constructor ───────────────────────────────────────────────

    public Player(int playerIndex, float startX, float startY, SpriteRenderer sprites) {
        this.playerIndex = playerIndex;
        this.x = startX; this.y = startY;
        this.sprites = sprites;
        this.facingRight = (playerIndex == 0);

        if (playerIndex == 0) {
            KEY_LEFT  = KeyEvent.VK_A;
            KEY_RIGHT = KeyEvent.VK_D;
            KEY_UP    = KeyEvent.VK_W;
            KEY_DASH  = KeyEvent.VK_SHIFT;
            KEY_FIRE  = KeyEvent.VK_SPACE;

            KEY_AIM_U  = KeyEvent.VK_T;
            KEY_AIM_D  = KeyEvent.VK_G;
            KEY_AIM_L  = KeyEvent.VK_F;
            KEY_AIM_R  = KeyEvent.VK_H;
            KEY_AIM_UL = KeyEvent.VK_R;
            KEY_AIM_UR = KeyEvent.VK_Y;
            KEY_AIM_DL = KeyEvent.VK_V;
            KEY_AIM_DR = KeyEvent.VK_B;
        } else {
            KEY_LEFT  = KeyEvent.VK_LEFT;
            KEY_RIGHT = KeyEvent.VK_RIGHT;
            KEY_UP    = KeyEvent.VK_UP;
            KEY_DASH  = KeyEvent.VK_CONTROL;
            KEY_FIRE  = KeyEvent.VK_NUMPAD0;   // also VK_ENTER mapped in keyPressed

            KEY_AIM_U  = KeyEvent.VK_NUMPAD8;
            KEY_AIM_D  = KeyEvent.VK_NUMPAD2;
            KEY_AIM_L  = KeyEvent.VK_NUMPAD4;
            KEY_AIM_R  = KeyEvent.VK_NUMPAD6;
            KEY_AIM_UL = KeyEvent.VK_NUMPAD7;
            KEY_AIM_UR = KeyEvent.VK_NUMPAD9;
            KEY_AIM_DL = KeyEvent.VK_NUMPAD1;
            KEY_AIM_DR = KeyEvent.VK_NUMPAD3;
        }
    }

    // ── Map physics setters ────────────────────────────────────────

    public void setSpeedMult(float m) { speedMult = m; }
    public void setIceMode(boolean b) { iceMode   = b; }

    // ── Key handlers ──────────────────────────────────────────────

    public void keyPressed(int c) {
        if (c < keys.length) keys[c] = true;
        // P2 also accepts ENTER as fire
        if (playerIndex == 1 && c == KeyEvent.VK_ENTER) keys[KEY_FIRE] = true;
    }

    public void keyReleased(int c) {
        if (c < keys.length) keys[c] = false;
        if (playerIndex == 1 && c == KeyEvent.VK_ENTER) keys[KEY_FIRE] = false;
        // Release fire → shoot
        if (c == KEY_FIRE && charging) releaseShot();
        if (playerIndex == 1 && c == KeyEvent.VK_ENTER && charging) releaseShot();
    }

    // ── Update ────────────────────────────────────────────────────

    public void update(Rectangle[] platforms) {
        if (iFrames > 0) iFrames--;
        if (!alive) {
            anim.tick();
            arrows.forEach(Arrow::update);
            arrows.removeIf(a -> !a.active);
            return;
        }
        updateDash();
        updateMovement(platforms);
        updateCharge();
        if (shootCD  > 0) shootCD--;
        if (shootAnim > 0) shootAnim--;
        updateAnimation();
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);
        anim.tick();
    }

    // ── Dash ──────────────────────────────────────────────────────

    private void updateDash() {
        if (dashCD > 0) dashCD--;
        if (isDashing) {
            dashTimer--;
            x += dashDX;
            x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));
            if (dashTimer <= 0) isDashing = false;
            return;
        }
        if (keys[KEY_DASH] && dashCD == 0) {
            isDashing = true; dashTimer = DASH_DUR; dashCD = DASH_CD;
            dashDX = facingRight ? DASH_SPD : -DASH_SPD;
            AudioManager.play(AudioManager.Sound.DASH);
        }
    }

    // ── Movement & gravity ────────────────────────────────────────

    private void updateMovement(Rectangle[] plats) {
        if (isDashing) return;

        float speed = BASE_SPEED * speedMult;
        float moveX = 0;
        if (keys[KEY_LEFT])  { moveX -= speed; facingRight = false; }
        if (keys[KEY_RIGHT]) { moveX += speed; facingRight = true;  }

        if (iceMode && onGround) {
            // Momentum-based ice sliding
            iceVX += moveX * 0.18f;
            iceVX *= 0.91f;   // friction
            x += iceVX;
        } else {
            iceVX = 0;
            x += moveX;
        }
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));

        if (keys[KEY_UP] && onGround) {
            vy = JUMP_POWER; onGround = false;
            AudioManager.play(AudioManager.Sound.JUMP);
        }

        vy = Math.min(vy + GRAVITY, MAX_FALL);
        y += vy;

        onGround = false;
        for (Rectangle r : plats) {
            Rectangle feet = new Rectangle((int)x, (int)y + H - 4, W, 8);
            if (feet.intersects(r) && vy >= 0) {
                y = r.y - H; vy = 0; onGround = true; break;
            }
        }
    }

    // ── Charge shot ───────────────────────────────────────────────

    private void updateCharge() {
        if (shootCD > 0) return;

        // Determine aim direction
        int dx = 0, dy = 0;
        if      (keys[KEY_AIM_UL]) { dx=-1; dy=-1; }
        else if (keys[KEY_AIM_UR]) { dx= 1; dy=-1; }
        else if (keys[KEY_AIM_DL]) { dx=-1; dy= 1; }
        else if (keys[KEY_AIM_DR]) { dx= 1; dy= 1; }
        else {
            if (keys[KEY_AIM_L]) dx=-1;
            if (keys[KEY_AIM_R]) dx= 1;
            if (keys[KEY_AIM_U]) dy=-1;
            if (keys[KEY_AIM_D]) dy= 1;
        }
        // Default forward if only fire held
        if (dx==0 && dy==0 && keys[KEY_FIRE]) dx = facingRight?1:-1;

        boolean aimHeld = (dx!=0||dy!=0) || keys[KEY_FIRE];

        if (aimHeld) {
            if (dx!=0) { facingRight=(dx>0); aimX=dx; aimY=dy; }
            else if (aimX==0) { aimX=facingRight?1:-1; aimY=dy; }
            if (!charging) charging = true;
            chargeTicks = Math.min(chargeTicks+1, MAX_CHARGE);
            if (chargeTicks >= MAX_CHARGE) releaseShot();
        } else {
            if (!charging) chargeTicks = 0;
        }
    }

    private void releaseShot() {
        if (!charging && chargeTicks==0) return;
        if (aimX==0 && aimY==0) aimX = facingRight?1:-1;
        float charge = Math.max(0.25f, (float)chargeTicks/MAX_CHARGE);
        arrows.add(new Arrow(x+W/2f, y+H/2f, aimX, aimY, playerIndex, charge));
        AudioManager.play(AudioManager.Sound.SHOOT);
        shootCD=SHOOT_CD; shootAnim=18; charging=false; chargeTicks=0; aimX=aimY=0;
    }

    // ── Hit/health ────────────────────────────────────────────────

    public boolean isInvincible() { return iFrames>0||isDashing; }

    /** Returns true if player died from this hit. */
    public boolean takeHit() {
        if (isInvincible()) return false;
        hp--; iFrames=I_FRAMES; shakeRequest=10;
        rageMode=(hp<=1);
        if (hp<=0) { die(); return true; }
        AudioManager.play(AudioManager.Sound.HIT);
        return false;
    }

    public void die() { alive=false; hp=0; anim.setAnimation(SpriteRenderer.ANIM_DEATH); }

    // ── Animation ─────────────────────────────────────────────────

    private void updateAnimation() {
        float moveX = (keys[KEY_LEFT]?-1:0)+(keys[KEY_RIGHT]?1:0);
        if      (!alive)                 anim.setAnimation(SpriteRenderer.ANIM_DEATH);
        else if (isDashing)              anim.setAnimation(SpriteRenderer.ANIM_DASH);
        else if (charging||shootAnim>0)  anim.setAnimation(SpriteRenderer.ANIM_SHOOT);
        else if (Math.abs(moveX)>0.1f)   anim.setAnimation(SpriteRenderer.ANIM_WALK);
        else                             anim.setAnimation(SpriteRenderer.ANIM_IDLE);
    }

    // ── Getters ───────────────────────────────────────────────────

    public Rectangle getBounds()   { return new Rectangle((int)x,(int)y,W,H); }
    public boolean   isAlive()     { return alive; }
    public float     getCenterX()  { return x+W/2f; }
    public float     getCenterY()  { return y+H/2f; }
    public float     getChargeRatio() { return chargeTicks/(float)MAX_CHARGE; }

    // ── Draw ──────────────────────────────────────────────────────

    public void draw(Graphics2D g) {
        // Dash afterimage
        if (isDashing) {
            Color ghost=(playerIndex==0)?new Color(80,160,255,55):new Color(255,100,40,55);
            g.setColor(ghost);
            g.fillRect((int)(x-dashDX),(int)y,W,H);
        }
        // Rage glow
        if (rageMode && alive) {
            long t=System.currentTimeMillis();
            float p=(float)(Math.sin(t/120.0)*0.5+0.5);
            Color rc=(playerIndex==0)?new Color(0,140,255,(int)(p*80)):new Color(255,60,0,(int)(p*80));
            g.setColor(rc);
            g.fillOval((int)x-10,(int)y-10,W+20,H+20);
        }
        // Invincibility blink
        if (iFrames>0&&(iFrames/4)%2==1) {
            for (Arrow a:arrows) a.draw(g);
            return;
        }
        BufferedImage frame=sprites.getFrame(playerIndex,anim.getAnimation(),anim.getFrame(),facingRight);
        g.drawImage(frame,(int)x+W/2-SpriteRenderer.FRAME_W/2,(int)y+H-SpriteRenderer.FRAME_H+4,null);
        // Charge bar
        if (charging && chargeTicks>0) {
            float r=getChargeRatio();
            Color cc=r<0.5f?new Color(255,220,0):r<0.85f?new Color(255,140,0):new Color(255,50,50);
            g.setColor(new Color(0,0,0,150)); g.fillRect((int)x-1,(int)y-14,W+2,9);
            g.setColor(cc);                   g.fillRect((int)x,(int)y-13,(int)(W*r),7);
        }
        for (Arrow a:arrows) a.draw(g);
    }
}