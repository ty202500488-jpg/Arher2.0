import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * SpriteRenderer v3
 * Adds: DASH animation row, rage-tinted sprites, better proportions.
 */
public class SpriteRenderer {

    public static final int FRAME_W = 72;
    public static final int FRAME_H = 72;

    // Animation row indices
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_WALK = 1;
    public static final int ANIM_SHOOT = 2;
    public static final int ANIM_DEATH = 3;
    public static final int ANIM_DASH = 4;

    // Frame counts per animation row
    public static final int[] FRAME_COUNTS = { 4, 6, 6, 6, 4 };

    private static final Color[][] PALETTES = {
            // Player 1 — Frost Archer (Blue/Cyan/Silver)
            {
                    new Color(30, 40, 90), // 0: Armor Main
                    new Color(100, 200, 255), // 1: Highlight
                    new Color(240, 210, 180), // 2: Skin
                    new Color(180, 240, 255), // 3: Bow
                    new Color(255, 255, 255), // 4: String/Glow
                    new Color(0, 150, 255), // 5: Cape
                    new Color(80, 120, 255, 120) // 6: Particle Aura
            },
            // Player 2 — Ember Archer (Red/Gold/Charcoal)
            {
                    new Color(60, 20, 20), // 0: Armor Main
                    new Color(255, 180, 50), // 1: Highlight
                    new Color(230, 190, 160), // 2: Skin
                    new Color(150, 40, 20), // 3: Bow
                    new Color(255, 255, 100), // 4: String/Glow
                    new Color(200, 50, 30), // 5: Cape
                    new Color(255, 100, 0, 130) // 6: Particle Aura
            }
    };

    private final BufferedImage[] sheets = new BufferedImage[2];
    private final Random rand = new Random();

    public SpriteRenderer() {
        for (int p = 0; p < 2; p++) {
            sheets[p] = buildSheet(p);
        }
    }

    public BufferedImage getFrame(int player, int animRow, int frame, boolean facingRight) {
        int col = frame % FRAME_COUNTS[animRow];
        BufferedImage src = sheets[player].getSubimage(
                col * FRAME_W, animRow * FRAME_H, FRAME_W, FRAME_H);
        return facingRight ? src : flipH(src);
    }

    private BufferedImage buildSheet(int player) {
        int numRows = FRAME_COUNTS.length; // 5 rows now
        int maxFrames = 0;
        for (int c : FRAME_COUNTS)
            maxFrames = Math.max(maxFrames, c);

        BufferedImage sheet = new BufferedImage(
                maxFrames * FRAME_W, numRows * FRAME_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Color[] pal = PALETTES[player];

        for (int row = 0; row < numRows; row++) {
            for (int f = 0; f < FRAME_COUNTS[row]; f++) {
                int ox = f * FRAME_W;
                int oy = row * FRAME_H;

                Graphics2D fg = (Graphics2D) g.create(ox, oy, FRAME_W, FRAME_H);
                fg.scale(1.5, 1.5);

                switch (row) {
                    case ANIM_IDLE -> drawIdle(fg, 0, 0, f, pal);
                    case ANIM_WALK -> drawWalk(fg, 0, 0, f, pal);
                    case ANIM_SHOOT -> drawShoot(fg, 0, 0, f, pal);
                    case ANIM_DEATH -> drawDeath(fg, 0, 0, f, pal);
                    case ANIM_DASH -> drawDash(fg, 0, 0, f, pal);
                }
                fg.dispose();
            }
        }
        g.dispose();
        return sheet;
    }

    // ── ANIMATION HANDLERS ──────────────────────────────────────

    private void drawIdle(Graphics2D g, int ox, int oy, int frame, Color[] pal) {
        int bob = (frame % 4 == 1 || frame % 4 == 2) ? 1 : 0;
        drawShadow(g, ox, oy);
        drawGlow(g, ox, oy, frame, pal[6]);
        drawArcher(g, ox, oy + bob, pal, 0, false, 0, false);
    }

    private void drawWalk(Graphics2D g, int ox, int oy, int frame, Color[] pal) {
        int bob = (frame % 2 == 0) ? 0 : 1;
        drawShadow(g, ox, oy);
        drawArcher(g, ox, oy + bob, pal, frame, true, 0, false);
    }

    private void drawShoot(Graphics2D g, int ox, int oy, int frame, Color[] pal) {
        drawShadow(g, ox, oy);
        if (frame == 3) {
            g.setColor(pal[4]);
            g.fillOval(ox + 28, oy + 18, 14, 14);
        }
        drawArcher(g, ox, oy, pal, 0, false, frame, false);
    }

    private void drawDeath(Graphics2D g, int ox, int oy, int frame, Color[] pal) {
        float alpha = Math.max(0f, 1f - frame * 0.15f);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.translate(ox + 24, oy + 40);
        g2.rotate(Math.toRadians(frame * 15));
        drawArcher(g2, -24, -40, pal, 0, false, 0, false);
        g2.dispose();
    }

    private void drawDash(Graphics2D g, int ox, int oy, int frame, Color[] pal) {
        drawShadow(g, ox, oy);
        // Slight horizontal stretch to suggest speed
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(ox + 12, oy + 20);
        g2.shear(-0.25 + frame * 0.05, 0);
        drawArcher(g2, -12, -20, pal, frame, true, 0, true);
        g2.dispose();
    }

    // ── CORE ARCHER DRAWING ────────────────────────────────────

    private void drawArcher(Graphics2D g, int ox, int oy, Color[] pal,
            int walkF, boolean isWalk, int shootF, boolean isDash) {
        Color main = pal[0], light = pal[1], skin = pal[2],
                bow = pal[3], string = pal[4], cape = pal[5];
        int bx = 14, by = 12;

        int legX = 0, legY = 0;
        if (isWalk || isDash) {
            legX = (walkF % 6 < 3) ? 2 : -2;
            legY = (walkF % 2 == 0) ? -1 : 0;
        }

        // Cape
        int capeW = 6 + (isWalk ? (walkF % 3) : 0) + (isDash ? 4 : 0);
        drawRect(g, ox + bx - 2, oy + by + 6, capeW, 12, cape, null);

        // Body
        drawRect(g, ox + bx, oy + by + 6, 10, 12, main, Color.BLACK);
        drawRect(g, ox + bx + 2, oy + by + 7, 6, 4, light, null);
        drawRect(g, ox + bx, oy + by + 16, 10, 3, Color.BLACK, null);

        // Legs
        drawRect(g, ox + bx + 1 + legX, oy + by + 18 + legY, 4, 6, main, Color.BLACK);
        drawRect(g, ox + bx + 5 - legX, oy + by + 18 - legY, 4, 6, main, Color.BLACK);

        // Head / Helmet
        drawRect(g, ox + bx - 1, oy + by - 6, 12, 12, main, Color.BLACK);
        drawRect(g, ox + bx + 1, oy + by - 2, 8, 6, skin, null);
        g.setColor(Color.BLACK);
        g.fillRect(ox + bx + 6, oy + by + 1, 2, 2);

        // Bow
        int drawBack = (shootF > 0) ? Math.min(shootF * 2, 6) : 0;
        g.setColor(bow);
        g.setStroke(new BasicStroke(2));
        g.drawArc(ox + bx + 8 - drawBack, oy + by - 4, 15, 28, -90, 180);

        // String
        g.setColor(string);
        g.drawLine(ox + bx + 15 - drawBack, oy + by - 2,
                ox + bx + 10 - drawBack * 2, oy + by + 10);
        g.drawLine(ox + bx + 10 - drawBack * 2, oy + by + 10,
                ox + bx + 15 - drawBack, oy + by + 22);

        // Arrow on string when shooting
        if (shootF > 0 && shootF < 4) {
            g.setColor(Color.WHITE);
            g.fillRect(ox + bx + 5 - drawBack, oy + by + 9, 12, 2);
        }
        g.setStroke(new BasicStroke(1));
    }

    // ── HELPERS ────────────────────────────────────────────────

    private void drawShadow(Graphics2D g, int ox, int oy) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(ox + 10, oy + 38, 28, 8);
    }

    private void drawGlow(Graphics2D g, int ox, int oy, int frame, Color color) {
        for (int i = 0; i < 5; i++) {
            int px = rand.nextInt(20) + 14;
            int py = rand.nextInt(20) + 14;
            int size = rand.nextInt(4) + 2;
            g.setColor(color);
            g.fillOval(ox + px, oy + py - (frame * 2 % 10), size, size);
        }
    }

    private void drawRect(Graphics2D g, int x, int y, int w, int h, Color fill, Color border) {
        if (fill != null) {
            g.setColor(fill);
            g.fillRect(x, y, w, h);
        }
        if (border != null) {
            g.setColor(border);
            g.drawRect(x, y, w, h);
        }
    }

    private BufferedImage flipH(BufferedImage src) {
        BufferedImage flipped = new BufferedImage(FRAME_W, FRAME_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(src, FRAME_W, 0, -FRAME_W, FRAME_H, null);
        g.dispose();
        return flipped;
    }
}