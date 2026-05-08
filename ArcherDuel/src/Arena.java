import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * Arena - Mystical Forest Ruins
 * Theme: Ancient stone ruins in a deep enchanted forest.
 * Multiple floating stone platforms, moss, vines, a central arch, and glowing
 * fireflies.
 */
public class Arena {

    public static final int WIDTH = 1400;
    public static final int HEIGHT = 800;
    public static final int GROUND_Y = HEIGHT - 90;

    // Platform layout mirroring the reference image:
    // { x, y, width, height }
    private static final int[][] PLAT_DEFS = {
            // 0: Full ground floor
            { 0, GROUND_Y, WIDTH, 90 },

            // 1: Left wall ledge (Lowered from 170 to 100)
            { 0, GROUND_Y - 100, 270, 28 },

            // 2: Right wall ledge (Lowered from 170 to 100)
            { WIDTH - 270, GROUND_Y - 100, 270, 28 },

            // 3: Center-left lower platform (Lowered from 220 to 190)
            { 310, GROUND_Y - 190, 220, 25 },

            // 4: Center-right lower platform (Lowered from 220 to 190)
            { WIDTH - 530, GROUND_Y - 190, 220, 25 },

            // 5: Upper-left mid platform (Lowered from 360 to 280)
            { 160, GROUND_Y - 280, 200, 22 },

            // 6: Upper-right mid platform (Lowered from 360 to 280)
            { WIDTH - 360, GROUND_Y - 280, 200, 22 },

            // 7: Center floating log platform (Lowered from 300 to 370) - Wait, let's keep
            // it sequential
            { WIDTH / 2 - 300, GROUND_Y - 370, 600, 22 },

            // 8: Center-left high platform
            { 350, GROUND_Y - 460, 170, 20 },

            // 9: Center-right high platform
            { WIDTH - 520, GROUND_Y - 460, 170, 20 },

            // 10: Top-left corner high ledge
            { 30, GROUND_Y - 550, 190, 22 },

            // 11: Top-right corner high ledge
            { WIDTH - 220, GROUND_Y - 550, 190, 22 },

            // 12: Top-center peak platform
            { WIDTH / 2 - 130, GROUND_Y - 640, 260, 22 },

            // 13: Center low step
            { WIDTH / 2 - 55, GROUND_Y - 70, 110, 75 },
    };

    private final Rectangle[] platforms;
    private final int[][] fireflies;
    private final int[][] mushrooms; // {x, y, colorIdx}
    private final Random rng = new Random(42);

    public Arena() {
        platforms = new Rectangle[PLAT_DEFS.length];
        for (int i = 0; i < PLAT_DEFS.length; i++) {
            platforms[i] = new Rectangle(
                    PLAT_DEFS[i][0], PLAT_DEFS[i][1],
                    PLAT_DEFS[i][2], PLAT_DEFS[i][3]);
        }

        // Fireflies scattered across the arena
        fireflies = new int[80][3];
        for (int[] f : fireflies) {
            f[0] = rng.nextInt(WIDTH);
            f[1] = rng.nextInt(GROUND_Y - 50);
            f[2] = rng.nextInt(360); // phase offset
        }

        // Mushrooms on platforms and ground
        mushrooms = new int[28][3];
        int mi = 0;
        // Ground mushrooms
        int[] mxs = { 90, 180, 320, 550, 680, 820, 950, 1100, 1260, 1360 };
        for (int mx : mxs) {
            if (mi < 28) {
                mushrooms[mi][0] = mx;
                mushrooms[mi][1] = GROUND_Y - 2;
                mushrooms[mi][2] = rng.nextInt(3);
                mi++;
            }
        }
        // Platform mushrooms
        for (int p = 1; p < 13 && mi < 28; p++) {
            Rectangle r = platforms[p];
            mushrooms[mi][0] = r.x + 15 + rng.nextInt(Math.max(1, r.width - 30));
            mushrooms[mi][1] = r.y;
            mushrooms[mi][2] = rng.nextInt(3);
            mi++;
        }
    }

    public Rectangle[] getPlatforms() {
        return platforms;
    }

    public void draw(Graphics2D g) {
        drawBackground(g);
        drawLightRays(g);
        drawRuinsArch(g);
        drawPlatformColumns(g);
        drawPlatforms(g);
        drawTotemStump(g);
        drawPool(g);
        drawMushrooms(g);
        drawFireflies(g);
    }

    // ── BACKGROUND ────────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g) {
        // Deep enchanted forest gradient
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(8, 20, 12),
                0, HEIGHT, new Color(18, 40, 22));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Back-layer trees (very dark, silhouette)
        g.setColor(new Color(10, 22, 13));
        int[] treeX = { 0, 80, 200, 340, 480, 600, 720, 850, 990, 1120, 1240, 1360 };
        int[] treeW = { 60, 90, 70, 80, 65, 100, 75, 85, 70, 80, 90, 60 };
        int[] treeH = { 500, 620, 550, 600, 520, 650, 580, 610, 540, 590, 620, 510 };
        for (int i = 0; i < treeX.length; i++) {
            int tx = treeX[i], tw = treeW[i], th = treeH[i];
            // Trunk
            g.fillRect(tx + tw / 2 - 10, GROUND_Y - th + 200, 20, th - 200);
            // Canopy layers
            g.fillOval(tx, GROUND_Y - th, tw + 60, th / 2);
            g.fillOval(tx + 10, GROUND_Y - th - 60, tw + 40, th / 2);
            g.fillOval(tx + 20, GROUND_Y - th - 100, tw + 20, th / 3);
        }

        // Mid-layer trees (slightly lighter)
        g.setColor(new Color(14, 32, 16));
        int[] mt = { 100, 280, 450, 650, 820, 1000, 1180, 1320 };
        for (int tx : mt) {
            g.fillRect(tx, GROUND_Y - 350, 18, 360);
            g.fillOval(tx - 50, GROUND_Y - 420, 120, 180);
            g.fillOval(tx - 30, GROUND_Y - 480, 80, 140);
        }

        // Stone wall pillars on far left and right edges (like the image)
        drawStoneEdgeWall(g, 0, true);
        drawStoneEdgeWall(g, WIDTH - 45, false);
    }

    private void drawStoneEdgeWall(Graphics2D g, int wx, boolean isLeft) {
        // Stone column / wall at screen edge
        g.setColor(new Color(48, 45, 40));
        g.fillRect(wx, 0, 45, HEIGHT);
        // Stone texture lines
        g.setColor(new Color(35, 33, 30));
        for (int sy = 30; sy < HEIGHT; sy += 60) {
            g.drawLine(wx, sy, wx + 45, sy);
        }
        g.setColor(new Color(60, 57, 52));
        for (int sy = 60; sy < HEIGHT; sy += 60) {
            g.drawLine(wx + 5, sy - 30, wx + 40, sy - 30);
        }
        // Moss patches
        g.setColor(new Color(30, 80, 30, 120));
        for (int sy = 80; sy < HEIGHT - 100; sy += 150) {
            g.fillRect(wx, sy, 45, 25);
        }
    }

    // ── LIGHT RAYS ────────────────────────────────────────────────────────────

    private void drawLightRays(Graphics2D g) {
        // Volumetric light beams from top
        int[] beamCX = { WIDTH / 2 + 60, WIDTH / 2 - 200, 400, WIDTH - 400 };
        int[] beamW = { 220, 140, 100, 110 };
        int[] alphas = { 14, 10, 8, 9 };

        for (int i = 0; i < beamCX.length; i++) {
            int cx = beamCX[i], hw = beamW[i] / 2;
            g.setColor(new Color(180, 255, 160, alphas[i]));
            int[] bx = { cx - 15, cx + 15, cx + hw, cx - hw };
            int[] by = { 0, 0, GROUND_Y, GROUND_Y };
            g.fillPolygon(bx, by, 4);
            // Second lighter inner ray
            g.setColor(new Color(200, 255, 180, alphas[i] / 2));
            int[] bx2 = { cx - 6, cx + 6, cx + hw / 3, cx - hw / 3 };
            g.fillPolygon(bx2, by, 4);
        }
    }

    // ── RUINS ARCH ───────────────────────────────────────────────────────────

    private void drawRuinsArch(Graphics2D g) {
        int cx = WIDTH / 2 + 20;
        int archBottom = GROUND_Y;
        int archH = 280;
        int archW = 200;
        int pillarW = 38;
        int pillarH = archH;

        // Left pillar
        drawStonePillar(g, cx - archW / 2 - pillarW / 2, archBottom - pillarH, pillarW, pillarH);
        // Right pillar
        drawStonePillar(g, cx + archW / 2 - pillarW / 2, archBottom - pillarH, pillarW, pillarH);

        // Arch curve (stone)
        g.setColor(new Color(52, 48, 42));
        g.setStroke(new BasicStroke(22, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - archW / 2, archBottom - pillarH - 50, archW, 120, 0, 180);
        g.setColor(new Color(68, 63, 55));
        g.setStroke(new BasicStroke(10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - archW / 2 + 10, archBottom - pillarH - 40, archW - 20, 100, 0, 180);
        g.setStroke(new BasicStroke(1));

        // Moss on arch
        g.setColor(new Color(30, 90, 30, 130));
        for (int ax = cx - archW / 2; ax <= cx + archW / 2; ax += 20) {
            double ang = Math.PI * (ax - (cx - archW / 2)) / archW;
            int ay = archBottom - pillarH - (int) (60 * Math.sin(ang)) - 55;
            g.fillOval(ax - 6, ay - 4, 14, 8);
        }

        // Keystone block at top of arch
        int kx = cx - 15, ky = archBottom - pillarH - 100;
        g.setColor(new Color(65, 60, 52));
        g.fillRect(kx, ky, 30, 22);
        g.setColor(new Color(40, 37, 33));
        g.drawRect(kx, ky, 30, 22);
    }

    private void drawStonePillar(Graphics2D g, int px, int py, int pw, int ph) {
        // Body
        g.setColor(new Color(55, 52, 46));
        g.fillRect(px, py, pw, ph);
        // Shading
        g.setColor(new Color(42, 40, 35));
        g.fillRect(px, py, 5, ph);
        g.setColor(new Color(72, 68, 60));
        g.fillRect(px + pw - 5, py, 5, ph);
        // Stone block lines
        g.setColor(new Color(38, 36, 31));
        for (int sy = py + 30; sy < py + ph; sy += 45) {
            g.drawLine(px, sy, px + pw, sy);
        }
        // Moss
        g.setColor(new Color(28, 80, 28, 110));
        for (int sy = py + 10; sy < py + ph; sy += 90) {
            g.fillRect(px, sy, pw, 12);
        }
        // Cap
        g.setColor(new Color(68, 63, 55));
        g.fillRect(px - 4, py, pw + 8, 12);
        g.setColor(new Color(35, 33, 29));
        g.drawRect(px - 4, py, pw + 8, 12);
    }

    // ── PLATFORM COLUMNS (SUPPORTS) ───────────────────────────────────────────

    private void drawPlatformColumns(Graphics2D g) {
        // Draw stone column supports under select platforms
        // Platforms 3, 4, 7 have visible columns
        int[][] colDefs = {
                // { platIdx, colW, offX } - one or two columns per platform
                { 3, 18, 20 },
                { 3, 18, platforms[3].width - 38 },
                { 4, 18, 20 },
                { 4, 18, platforms[4].width - 38 },
                { 7, 22, 30 },
                { 7, 22, platforms[7].width - 52 },
        };
        for (int[] cd : colDefs) {
            Rectangle plat = platforms[cd[0]];
            int cx2 = plat.x + cd[2];
            int cy2 = plat.y + plat.height;
            int cw = cd[1];
            int ch = GROUND_Y - cy2;
            if (ch > 0) {
                drawStonePillar(g, cx2, cy2, cw, ch);
            }
        }
    }

    // ── PLATFORMS ────────────────────────────────────────────────────────────

    private void drawPlatforms(Graphics2D g) {
        // Ground (index 0)
        drawGroundFloor(g, platforms[0]);

        // Stump (index 13) - draw as solid totem, handled separately
        // All other floating platforms
        for (int i = 1; i < platforms.length; i++) {
            if (i == 13)
                continue; // stump drawn separately
            drawStoneMossPlatform(g, platforms[i], i == 1 || i == 2);
        }
    }

    private void drawGroundFloor(Graphics2D g, Rectangle r) {
        // Dirt/soil base
        g.setColor(new Color(38, 28, 18));
        g.fillRect(r.x, r.y, r.width, r.height);

        // Stone tiles on ground
        g.setColor(new Color(52, 48, 42));
        for (int tx = r.x; tx < r.x + r.width; tx += 80) {
            g.fillRect(tx + 2, r.y, 76, 18);
        }
        g.setColor(new Color(38, 35, 30));
        for (int tx = r.x + 40; tx < r.x + r.width; tx += 80) {
            g.fillRect(tx + 2, r.y + 20, 76, 18);
        }

        // Grass top strip
        g.setColor(new Color(35, 100, 30));
        g.fillRect(r.x, r.y, r.width, 8);

        // Dense grass tufts
        g.setColor(new Color(55, 140, 40));
        for (int gx = r.x + 2; gx < r.x + r.width; gx += 6) {
            int h = 5 + (gx % 4);
            g.drawLine(gx, r.y, gx - 1 + (gx % 3), r.y - h);
        }

        // Ground-level ferns and plants
        g.setColor(new Color(30, 90, 25));
        int[] fernX = { 150, 380, 560, 740, 920, 1100, 1280 };
        for (int fx : fernX) {
            drawFern(g, fx, r.y);
        }
    }

    private void drawStoneMossPlatform(Graphics2D g, Rectangle r, boolean isWallLedge) {
        if (r.height < 5)
            return;

        // Stone base
        g.setColor(new Color(55, 52, 46));
        g.fillRect(r.x, r.y, r.width, r.height);

        // Stone block pattern
        g.setColor(new Color(42, 40, 35));
        for (int bx = r.x; bx < r.x + r.width; bx += 35) {
            g.drawLine(bx, r.y, bx, r.y + r.height);
        }
        g.setColor(new Color(68, 64, 56));
        g.fillRect(r.x, r.y, r.width, 5); // top highlight

        // Moss top
        g.setColor(new Color(35, 110, 30));
        g.fillRect(r.x, r.y, r.width, 6);

        // Grass tufts on top
        g.setColor(new Color(55, 150, 40));
        for (int gx = r.x + 4; gx < r.x + r.width - 4; gx += 7) {
            int h = 4 + (gx % 4);
            g.drawLine(gx, r.y, gx + (gx % 3) - 1, r.y - h);
        }

        // Hanging vines below platform
        g.setColor(new Color(22, 70, 22));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int vx = r.x + 18; vx < r.x + r.width - 10; vx += 30) {
            int vineLen = 15 + ((vx * 7) % 30);
            // Slightly curved vine
            int cx2 = vx + (int) (Math.sin(vx * 0.1) * 5);
            g.drawLine(vx, r.y + r.height, cx2, r.y + r.height + vineLen);
            // Leaf at end
            g.setColor(new Color(30, 90, 25, 180));
            g.fillOval(cx2 - 4, r.y + r.height + vineLen - 2, 9, 5);
            g.setColor(new Color(22, 70, 22));
        }
        g.setStroke(new BasicStroke(1));

        // Side edge detail
        g.setColor(new Color(35, 33, 29));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    // ── TOTEM STUMP ──────────────────────────────────────────────────────────

    private void drawTotemStump(Graphics2D g) {
        int sx = WIDTH / 2 - 55;
        int sy = GROUND_Y - 70;
        int sw = 110;
        int sh = 70;

        // Stump body - dark wood
        g.setColor(new Color(50, 32, 18));
        g.fillRect(sx, sy, sw, sh);

        // Wood grain
        g.setColor(new Color(65, 45, 25));
        for (int wx = sx + 12; wx < sx + sw; wx += 18) {
            g.drawLine(wx, sy, wx + 3, sy + sh);
        }

        // Top surface oval (3D effect)
        g.setColor(new Color(80, 55, 32));
        g.fillOval(sx, sy - 10, sw, 20);

        // Tree ring markings
        g.setColor(new Color(35, 22, 12));
        g.drawOval(sx + 8, sy - 7, sw - 16, 14);
        g.drawOval(sx + 20, sy - 4, sw - 40, 8);
        g.drawOval(sx + 32, sy - 1, sw - 64, 3);

        // Totem face carvings
        g.setColor(new Color(25, 15, 8));
        // Eyes
        g.fillOval(sx + 28, sy + 15, 12, 10);
        g.fillOval(sx + sw - 40, sy + 15, 12, 10);
        // Glow in eyes
        g.setColor(new Color(100, 255, 80, 160));
        g.fillOval(sx + 31, sy + 17, 6, 6);
        g.fillOval(sx + sw - 37, sy + 17, 6, 6);
        // Mouth carving
        g.setColor(new Color(20, 10, 5));
        g.fillRect(sx + 30, sy + 34, sw - 60, 5);
        for (int tx = sx + 32; tx < sx + sw - 30; tx += 8) {
            g.fillRect(tx, sy + 38, 5, 8);
        }

        // Moss on top edge
        g.setColor(new Color(30, 100, 25, 150));
        g.fillRect(sx + 5, sy - 5, sw - 10, 6);

        // Small mushrooms at base
        drawMushroom(g, sx - 8, GROUND_Y - 2, 0);
        drawMushroom(g, sx + sw + 3, GROUND_Y - 2, 1);
    }

    // ── REFLECTIVE POOL ───────────────────────────────────────────────────────

    private void drawPool(Graphics2D g) {
        int px = WIDTH / 2 + 90;
        int py = GROUND_Y - 8;
        int pw = 130;
        int ph = 10;

        // Dark water
        g.setColor(new Color(15, 40, 55, 200));
        g.fillOval(px, py, pw, ph);

        // Water shimmer / reflection
        g.setColor(new Color(40, 120, 160, 80));
        g.fillOval(px + 10, py + 2, pw - 20, ph - 4);

        // Glint
        long t = System.currentTimeMillis();
        float shimmer = (float) (Math.sin(t / 500.0) * 0.5 + 0.5);
        g.setColor(new Color(180, 230, 255, (int) (shimmer * 100 + 30)));
        g.fillOval(px + pw / 2 - 20, py + 1, 40, 4);
    }

    // ── MUSHROOMS ─────────────────────────────────────────────────────────────

    private void drawMushrooms(Graphics2D g) {
        for (int[] m : mushrooms) {
            drawMushroom(g, m[0], m[1], m[2]);
        }
    }

    private void drawMushroom(Graphics2D g, int mx, int my, int colorIdx) {
        Color[] capColors = {
                new Color(180, 40, 30), // red
                new Color(90, 60, 160), // purple
                new Color(40, 130, 60), // green
        };
        Color cap = capColors[colorIdx % 3];
        Color stem = new Color(220, 200, 170);

        // Stem
        g.setColor(stem);
        g.fillRect(mx - 3, my - 10, 6, 10);

        // Cap
        g.setColor(cap);
        g.fillOval(mx - 8, my - 18, 16, 12);

        // White spots
        g.setColor(new Color(255, 255, 255, 180));
        g.fillOval(mx - 3, my - 17, 4, 3);
        g.fillOval(mx + 2, my - 13, 3, 2);
    }

    // ── FERN ──────────────────────────────────────────────────────────────────

    private void drawFern(Graphics2D g, int fx, int fy) {
        g.setColor(new Color(30, 90, 25, 180));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Central stem
        g.drawLine(fx, fy, fx, fy - 18);
        // Left fronds
        for (int i = 1; i <= 3; i++) {
            int base = fy - i * 5;
            g.drawLine(fx, base, fx - 10 - i * 2, base - 6);
        }
        // Right fronds
        for (int i = 1; i <= 3; i++) {
            int base = fy - i * 5;
            g.drawLine(fx, base, fx + 10 + i * 2, base - 6);
        }
        g.setStroke(new BasicStroke(1));
    }

    // ── FIREFLIES ────────────────────────────────────────────────────────────

    private void drawFireflies(Graphics2D g) {
        long t = System.currentTimeMillis();
        for (int[] f : fireflies) {
            double phase = Math.sin(t / 700.0 + f[2] * 0.05);
            float alpha = (float) (phase * 0.5 + 0.5);
            if (alpha < 0.1f)
                continue;

            // Animated drift
            int dx = (int) (Math.sin(t / 1200.0 + f[2]) * 6);
            int dy = (int) (Math.cos(t / 900.0 + f[2]) * 4);
            int fx = f[0] + dx;
            int fy = f[1] + dy;

            // Glow halo
            g.setColor(new Color(160, 255, 80, (int) (alpha * 40)));
            g.fillOval(fx - 4, fy - 4, 10, 10);
            // Core dot
            g.setColor(new Color(210, 255, 120, (int) (alpha * 220)));
            g.fillOval(fx, fy, 4, 4);
        }
    }
}