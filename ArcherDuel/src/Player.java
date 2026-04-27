import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

/**
 * Player
 *
 * Represents one archer in the arena.
 * Handles movement, jumping, shooting, animation, and collision with platforms.
 *
 * Player 1 (index 0):
 *   Move  — W/A/S/D
 *   Shoot — T/G/F/H (cardinal) + R/Y/V/B (diagonals)
 *
 * Player 2 (index 1):
 *   Move  — Arrow Keys (UP/LEFT/DOWN/RIGHT)
 *   Shoot — I/K/J/L (cardinal) + U/O/M/. (diagonals)
 */
public class Player {

    // ── constants ────────────────────────────────────────────────
    public static final int W = 28;           // collision box width
    public static final int H = 42;           // collision box height

    private static final float MOVE_SPEED   = 3.5f;
    private static final float JUMP_POWER   = -11f;
    private static final float GRAVITY      = 0.55f;
    private static final float MAX_FALL     = 14f;

    // Shoot cooldown (ticks between shots)
    private static final int SHOOT_COOLDOWN = 18;

    // ── identity ──────────────────────────────────────────────────
    public final int playerIndex; // 0 or 1

    // ── position / physics ────────────────────────────────────────
    public float x, y;
    private float vy = 0;        // vertical velocity
    private boolean onGround = false;

    // ── state ─────────────────────────────────────────────────────
    public  boolean alive       = true;
    private boolean facingRight;            // sprite direction
    private int     shootCooldown = 0;
    private int     shootAnimTimer = 0;     // counts down after shooting

    // ── input keys held ──────────────────────────────────────────
    private final boolean[] keys = new boolean[65536];

    // Key codes per player
    // P1: move WASD; shoot T/G/F/H/R/Y/V/B
    // P2: move Arrows; shoot I/K/J/L/U/O/M/.
    private final int KEY_LEFT, KEY_RIGHT, KEY_UP, KEY_DOWN;
    private final int KEY_SHOOT_U, KEY_SHOOT_D, KEY_SHOOT_L, KEY_SHOOT_R;
    private final int KEY_SHOOT_UL, KEY_SHOOT_UR, KEY_SHOOT_DL, KEY_SHOOT_DR;

    // ── animation ─────────────────────────────────────────────────
    private final AnimationController anim = new AnimationController();
    private final SpriteRenderer      sprites;

    // ── arrows fired by this player ───────────────────────────────
    public final List<Arrow> arrows = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────
    public Player(int playerIndex, float startX, float startY, SpriteRenderer sprites) {
        this.playerIndex = playerIndex;
        this.x           = startX;
        this.y           = startY;
        this.sprites     = sprites;
        this.facingRight = (playerIndex == 0); // P1 faces right, P2 faces left initially

        if (playerIndex == 0) {
            KEY_LEFT     = KeyEvent.VK_A;
            KEY_RIGHT    = KeyEvent.VK_D;
            KEY_UP       = KeyEvent.VK_W;
            KEY_DOWN     = KeyEvent.VK_S;
            KEY_SHOOT_U  = KeyEvent.VK_T;
            KEY_SHOOT_D  = KeyEvent.VK_G;
            KEY_SHOOT_L  = KeyEvent.VK_F;
            KEY_SHOOT_R  = KeyEvent.VK_H;
            KEY_SHOOT_UL = KeyEvent.VK_R;
            KEY_SHOOT_UR = KeyEvent.VK_Y;
            KEY_SHOOT_DL = KeyEvent.VK_V;
            KEY_SHOOT_DR = KeyEvent.VK_B;
        } else {
            KEY_LEFT     = KeyEvent.VK_LEFT;
            KEY_RIGHT    = KeyEvent.VK_RIGHT;
            KEY_UP       = KeyEvent.VK_UP;
            KEY_DOWN     = KeyEvent.VK_DOWN;
            KEY_SHOOT_U  = KeyEvent.VK_I;
            KEY_SHOOT_D  = KeyEvent.VK_K;
            KEY_SHOOT_L  = KeyEvent.VK_J;
            KEY_SHOOT_R  = KeyEvent.VK_L;
            KEY_SHOOT_UL = KeyEvent.VK_U;
            KEY_SHOOT_UR = KeyEvent.VK_O;
            KEY_SHOOT_DL = KeyEvent.VK_M;
            KEY_SHOOT_DR = KeyEvent.VK_PERIOD;
        }
    }

    // ── key event handlers ────────────────────────────────────────
    public void keyPressed (int code) { if (code < keys.length) keys[code] = true;  }
    public void keyReleased(int code) { if (code < keys.length) keys[code] = false; }

    // ── update ────────────────────────────────────────────────────
    public void update(Rectangle[] platforms) {
        if (!alive) {
            // Continue death animation
            anim.tick();
            // Update own arrows
            arrows.forEach(Arrow::update);
            arrows.removeIf(a -> !a.active);
            return;
        }

        // ── Horizontal movement ───────────────────────────────
        float moveX = 0;
        if (keys[KEY_LEFT])  { moveX -= MOVE_SPEED; facingRight = false; }
        if (keys[KEY_RIGHT]) { moveX += MOVE_SPEED; facingRight = true;  }

        x += moveX;

        // Keep within screen bounds
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));

        // ── Vertical (jump + gravity) ─────────────────────────
        if (keys[KEY_UP] && onGround) {
            vy = JUMP_POWER;
            onGround = false;
        }

        vy = Math.min(vy + GRAVITY, MAX_FALL);
        y += vy;

        // ── Platform collisions ───────────────────────────────
        onGround = false;
        for (Rectangle plat : platforms) {
            // Only collide from above (falling onto platform)
            Rectangle feet = new Rectangle((int) x, (int) y + H - 4, W, 8);
            if (feet.intersects(plat) && vy >= 0) {
                y = plat.y - H;
                vy = 0;
                onGround = true;
                break;
            }
        }

        // ── Shooting ──────────────────────────────────────────
        if (shootCooldown > 0) shootCooldown--;
        if (shootAnimTimer > 0) shootAnimTimer--;

        tryShoot();

        // ── Animation state machine ───────────────────────────
        updateAnimation(moveX);

        // Update own arrows
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);

        anim.tick();
    }

    /** Attempt to fire in whatever direction is currently held. */
    private void tryShoot() {
        if (shootCooldown > 0) return;

        int dirX = 0, dirY = 0;

        if      (keys[KEY_SHOOT_U])  { dirX =  0; dirY = -1; }
        else if (keys[KEY_SHOOT_D])  { dirX =  0; dirY =  1; }
        else if (keys[KEY_SHOOT_L])  { dirX = -1; dirY =  0; }
        else if (keys[KEY_SHOOT_R])  { dirX =  1; dirY =  0; }
        else if (keys[KEY_SHOOT_UL]) { dirX = -1; dirY = -1; }
        else if (keys[KEY_SHOOT_UR]) { dirX =  1; dirY = -1; }
        else if (keys[KEY_SHOOT_DL]) { dirX = -1; dirY =  1; }
        else if (keys[KEY_SHOOT_DR]) { dirX =  1; dirY =  1; }

        if (dirX == 0 && dirY == 0) return;

        // Spawn arrow from chest center
        float ax = x + W / 2f;
        float ay = y + H / 2f;
        arrows.add(new Arrow(ax, ay, dirX, dirY, playerIndex));

        shootCooldown  = SHOOT_COOLDOWN;
        shootAnimTimer = 20;

        // Face the direction of shooting (horizontal component)
        if (dirX != 0) facingRight = (dirX > 0);
    }

    /** Choose animation row based on current state. */
    private void updateAnimation(float moveX) {
        if (!alive) {
            anim.setAnimation(SpriteRenderer.ANIM_DEATH);
            return;
        }
        if (shootAnimTimer > 0) {
            anim.setAnimation(SpriteRenderer.ANIM_SHOOT);
        } else if (Math.abs(moveX) > 0.1f) {
            anim.setAnimation(SpriteRenderer.ANIM_WALK);
        } else {
            anim.setAnimation(SpriteRenderer.ANIM_IDLE);
        }
    }

    /** Called when this player is struck by an arrow. */
    public void die() {
        alive = false;
        anim.setAnimation(SpriteRenderer.ANIM_DEATH);
    }

    // ── collision ─────────────────────────────────────────────────
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, W, H);
    }

    // ── rendering ────────────────────────────────────────────────
    public void draw(Graphics2D g) {
        BufferedImage frame = sprites.getFrame(
            playerIndex,
            anim.getAnimation(),
            anim.getFrame(),
            facingRight
        );

        // Draw sprite centered on player bounds
        int drawX = (int) x + W / 2 - SpriteRenderer.FRAME_W / 2;
        int drawY = (int) y + H - SpriteRenderer.FRAME_H + 4;
        g.drawImage(frame, drawX, drawY, null);

        // Draw arrows
        for (Arrow a : arrows) a.draw(g);

        // Debug: uncomment to see collision box
        // g.setColor(new Color(255,0,0,80));
        // g.fillRect((int)x,(int)y,W,H);
    }

    // ── getters ───────────────────────────────────────────────────
    public boolean isAlive()      { return alive;        }
    public float   getCenterX()   { return x + W / 2f;  }
    public float   getCenterY()   { return y + H / 2f;  }
}
