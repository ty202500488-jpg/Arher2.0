import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Player
 *
 * Represents one archer in the arena.
 * Handles movement, jumping, shooting, animation, and collision with platforms.
 *
 * Player 1 (index 0):
 * Move - W/A/S/D
 * Shoot - Space (Forward), TFGH (Aiming)
 *
 * Player 2 (index 1):
 * Move - Arrow Keys
 * Shoot - NumPad Keys
 */
public class Player {

    // Constants
    public static final int W = 42;
    public static final int H = 63;

    private static final float MOVE_SPEED = 3.5f;
    private static final float JUMP_POWER = -11f;
    private static final float GRAVITY = 0.55f;
    private static final float MAX_FALL = 14f;

    // Shoot cooldown
    private static final int SHOOT_COOLDOWN = 30;

    // Identity
    public final int playerIndex;

    // Position / physics
    public float x, y;
    private float vy = 0;
    private boolean onGround = false;

    // State
    public boolean alive = true;
    private boolean facingRight;
    private int shootCooldown = 0;
    private int shootAnimTimer = 0;

    // Input keys
    private final boolean[] keys = new boolean[65536];

    // Controls
    private final int KEY_LEFT, KEY_RIGHT, KEY_UP, KEY_DOWN;
    private final int KEY_SHOOT_U, KEY_SHOOT_D, KEY_SHOOT_L, KEY_SHOOT_R;
    private final int KEY_SHOOT_UL, KEY_SHOOT_UR, KEY_SHOOT_DL, KEY_SHOOT_DR;

    // Animation
    private final AnimationController anim = new AnimationController();
    private final SpriteRenderer sprites;

    // Arrows
    public final List<Arrow> arrows = new ArrayList<>();

    // Constructor
    public Player(int playerIndex, float startX, float startY, SpriteRenderer sprites) {

        this.playerIndex = playerIndex;
        this.x = startX;
        this.y = startY;
        this.sprites = sprites;

        this.facingRight = (playerIndex == 0);

        if (playerIndex == 0) {

            // Player 1
            KEY_LEFT = KeyEvent.VK_A;
            KEY_RIGHT = KeyEvent.VK_D;
            KEY_UP = KeyEvent.VK_W;
            KEY_DOWN = KeyEvent.VK_S;

            KEY_SHOOT_U = KeyEvent.VK_T;
            KEY_SHOOT_D = KeyEvent.VK_G;
            KEY_SHOOT_L = KeyEvent.VK_F;
            KEY_SHOOT_R = KeyEvent.VK_H;

            KEY_SHOOT_UL = KeyEvent.VK_R;
            KEY_SHOOT_UR = KeyEvent.VK_Y;
            KEY_SHOOT_DL = KeyEvent.VK_V;
            KEY_SHOOT_DR = KeyEvent.VK_B;

        } else {

            // Player 2
            KEY_LEFT = KeyEvent.VK_LEFT;
            KEY_RIGHT = KeyEvent.VK_RIGHT;
            KEY_UP = KeyEvent.VK_UP;
            KEY_DOWN = KeyEvent.VK_DOWN;

            KEY_SHOOT_U = KeyEvent.VK_NUMPAD8;
            KEY_SHOOT_D = KeyEvent.VK_NUMPAD2;
            KEY_SHOOT_L = KeyEvent.VK_NUMPAD4;
            KEY_SHOOT_R = KeyEvent.VK_NUMPAD6;

            KEY_SHOOT_UL = KeyEvent.VK_NUMPAD7;
            KEY_SHOOT_UR = KeyEvent.VK_NUMPAD9;
            KEY_SHOOT_DL = KeyEvent.VK_NUMPAD1;
            KEY_SHOOT_DR = KeyEvent.VK_NUMPAD3;
        }
    }

    // Key handlers
    public void keyPressed(int code) {
        if (code < keys.length) {
            keys[code] = true;
        }
    }

    public void keyReleased(int code) {
        if (code < keys.length) {
            keys[code] = false;
        }
    }

    // Update
    public void update(Rectangle[] platforms) {

        if (!alive) {

            anim.tick();

            arrows.forEach(Arrow::update);
            arrows.removeIf(a -> !a.active);

            return;
        }

        // Horizontal movement
        float moveX = 0;

        if (keys[KEY_LEFT]) {
            moveX -= MOVE_SPEED;
            facingRight = false;
        }

        if (keys[KEY_RIGHT]) {
            moveX += MOVE_SPEED;
            facingRight = true;
        }

        x += moveX;

        // Screen bounds
        x = Math.max(0, Math.min(x, GameWindow.WIDTH - W));

        // Jump
        if (keys[KEY_UP] && onGround) {
            vy = JUMP_POWER;
            onGround = false;
            AudioManager.play(AudioManager.Sound.JUMP);
        }

        // Gravity
        vy = Math.min(vy + GRAVITY, MAX_FALL);
        y += vy;

        // Platform collision
        onGround = false;

        for (Rectangle plat : platforms) {

            Rectangle feet = new Rectangle((int) x, (int) y + H - 4, W, 8);

            if (feet.intersects(plat) && vy >= 0) {

                y = plat.y - H;
                vy = 0;
                onGround = true;

                break;
            }
        }

        // Shooting timers
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        if (shootAnimTimer > 0) {
            shootAnimTimer--;
        }

        tryShoot();

        // Animation
        updateAnimation(moveX);

        // Update arrows
        arrows.forEach(Arrow::update);
        arrows.removeIf(a -> !a.active);

        anim.tick();
    }

    // Shooting
    private void tryShoot() {

        if (shootCooldown > 0) {
            return;
        }

        int dirX = 0;
        int dirY = 0;

        if (playerIndex == 0) {

            // Player 1
            boolean anyShoot = keys[KeyEvent.VK_SPACE] ||
                    keys[KEY_SHOOT_U] ||
                    keys[KEY_SHOOT_D] ||
                    keys[KEY_SHOOT_L] ||
                    keys[KEY_SHOOT_R];

            if (anyShoot) {

                if (keys[KEY_SHOOT_L]) {
                    dirX = -1;
                } else if (keys[KEY_SHOOT_R]) {
                    dirX = 1;
                } else {
                    dirX = facingRight ? 1 : -1;
                }

                dirY = 0;
            }

        } else {

            // Player 2
            boolean shootForward = keys[KeyEvent.VK_NUMPAD1] ||
                    keys[KeyEvent.VK_END];

            if (shootForward) {

                dirX = facingRight ? 1 : -1;
                dirY = 0;

            } else {

                if (keys[KEY_SHOOT_U])
                    dirY--;
                if (keys[KEY_SHOOT_D])
                    dirY++;
                if (keys[KEY_SHOOT_L])
                    dirX--;
                if (keys[KEY_SHOOT_R])
                    dirX++;

                if (dirX == 0 && dirY == 0) {

                    if (keys[KEY_SHOOT_UL]) {
                        dirX = -1;
                        dirY = -1;
                    } else if (keys[KEY_SHOOT_UR]) {
                        dirX = 1;
                        dirY = -1;
                    } else if (keys[KEY_SHOOT_DL]) {
                        dirX = -1;
                        dirY = 1;
                    } else if (keys[KEY_SHOOT_DR]) {
                        dirX = 1;
                        dirY = 1;
                    }
                }
            }
        }

        // No direction
        if (dirX == 0 && dirY == 0) {
            return;
        }

        // Spawn arrow
        float ax = x + W / 2f;
        float ay = y + H / 2f;

        arrows.add(new Arrow(ax, ay, dirX, dirY, playerIndex));
        AudioManager.play(AudioManager.Sound.SHOOT);

        shootCooldown = SHOOT_COOLDOWN;
        shootAnimTimer = 20;

        // Face shooting direction
        if (dirX != 0) {
            facingRight = (dirX > 0);
        }
    }

    // Animation state
    private void updateAnimation(float moveX) {

        if (!alive) {

            anim.setAnimation(SpriteRenderer.ANIM_DEATH);

        } else if (shootAnimTimer > 0) {

            anim.setAnimation(SpriteRenderer.ANIM_SHOOT);

        } else if (Math.abs(moveX) > 0.1f) {

            anim.setAnimation(SpriteRenderer.ANIM_WALK);

        } else {

            anim.setAnimation(SpriteRenderer.ANIM_IDLE);
        }
    }

    // Death
    public void die() {

        alive = false;
        anim.setAnimation(SpriteRenderer.ANIM_DEATH);
    }

    // Collision box
    public Rectangle getBounds() {

        return new Rectangle((int) x, (int) y, W, H);
    }

    // Draw
    public void draw(Graphics2D g) {

        BufferedImage frame = sprites.getFrame(
                playerIndex,
                anim.getAnimation(),
                anim.getFrame(),
                facingRight);

        int drawX = (int) x + W / 2 - SpriteRenderer.FRAME_W / 2;
        int drawY = (int) y + H - SpriteRenderer.FRAME_H + 4;

        g.drawImage(frame, drawX, drawY, null);

        // Draw arrows
        for (Arrow a : arrows) {
            a.draw(g);
        }
    }

    // Getters
    public boolean isAlive() {
        return alive;
    }

    public float getCenterX() {
        return x + W / 2f;
    }

    public float getCenterY() {
        return y + H / 2f;
    }

}