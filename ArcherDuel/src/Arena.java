import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * Arena v4 — delegates data/physics to ArenaData, owns all rendering.
 * Each map has a fully unique visual theme.
 */
public class Arena {

    // Keep these for backwards compat with GamePanel/GameWindow references
    public static final int WIDTH = ArenaData.W;
    public static final int HEIGHT = ArenaData.H;
    public static final int GROUND_Y = ArenaData.GY;

    private final ArenaData data;
    private final Random rng = new Random(42);

    // Ambient particle data (fireflies / snowflakes / embers / stars)
    private final int[][] pts = new int[60][3];

    public Arena(MapTheme theme) {
        data = new ArenaData(theme);
        for (int[] p : pts) {
            p[0] = rng.nextInt(WIDTH);
            p[1] = rng.nextInt(GROUND_Y - 50);
            p[2] = rng.nextInt(360);
        }
    }

    public Arena() {
        this(MapTheme.FOREST);
    }

    // ── Delegation ────────────────────────────────────────────────
    public Rectangle[] getPlatforms() {
        return data.platforms;
    }

    public MapTheme getTheme() {
        return data.theme;
    }

    public void tick() {
        data.tick();
    }

    public void shrink(int l) {
        data.shrink(l);
    }

    public int getLeftWall() {
        return data.getLeftWall();
    }

    public int getRightWall() {
        return data.getRightWall();
    }

    // ── Draw dispatcher ───────────────────────────────────────────
    public void draw(Graphics2D g) {
        switch (data.theme) {
            case FOREST -> drawForest(g);
            case CASTLE -> drawCastle(g);
            case VOLCANO -> drawVolcano(g);
            case ICE -> drawIce(g);
            case SKY_ISLANDS -> drawSky(g);
            case NIGHT -> drawNight(g);
        }
        drawMovingPlatformGlow(g);
        drawShrinkWalls(g);
    }

    // ── FOREST ────────────────────────────────────────────────────
    private void drawForest(Graphics2D g) {
        grad(g, new Color(8, 20, 12), new Color(18, 40, 22));
        drawTrees(g, new Color(10, 22, 13), new Color(14, 32, 16));
        drawRays(g);
        drawAllPlatforms(g, new Color(55, 52, 46), new Color(35, 110, 30), true);
        // Cover barrels
        drawBarrel(g, 450, GROUND_Y);
        drawBarrel(g, 940, GROUND_Y);
        drawBarrel(g, W() / 2 - 60, GROUND_Y);
        drawBarrel(g, W() / 2 + 40, GROUND_Y);
        drawAmb(g, new Color(160, 255, 80, 40), new Color(200, 255, 120, 200));
    }

    // ── CASTLE ────────────────────────────────────────────────────
    private void drawCastle(Graphics2D g) {
        grad(g, new Color(18, 15, 30), new Color(40, 35, 60));
        // Moon
        g.setColor(new Color(230, 230, 200));
        g.fillOval(W() - 160, 25, 90, 90);
        g.setColor(new Color(18, 15, 30));
        g.fillOval(W() - 130, 15, 90, 90);
        // Castle BG silhouette
        g.setColor(new Color(22, 18, 38));
        for (int[] t : new int[][] { { 0, 200, 80 }, { 300, 160, 100 }, { 600, 140, 90 }, { 900, 170, 100 },
                { 1100, 150, 80 }, { 1300, 190, 90 } }) {
            g.fillRect(t[0], t[1], t[2], HEIGHT - t[1]);
            for (int bx = t[0]; bx < t[0] + t[2]; bx += 20)
                g.fillRect(bx, t[1] - 15, 12, 18);
        }
        drawAllPlatforms(g, new Color(75, 70, 90), new Color(50, 45, 70), false);
        // Flag banners on tower tops
        drawBanner(g, 10 + 90, ArenaData.GY - 550, 0);
        drawBanner(g, W() - 100, ArenaData.GY - 550, 1);
        drawAmb(g, new Color(180, 160, 255, 30), new Color(200, 190, 255, 180));
    }

    // ── VOLCANO ───────────────────────────────────────────────────
    private void drawVolcano(Graphics2D g) {
        grad(g, new Color(35, 8, 5), new Color(80, 20, 5));
        // Volcano BG
        g.setColor(new Color(55, 15, 8));
        int[] vx = { 0, 350, 500, 600, 700, 850, W(), W(), 0 };
        int[] vy = { HEIGHT, 250, HEIGHT - 80, 200, HEIGHT - 60, 240, HEIGHT, HEIGHT, HEIGHT };
        g.fillPolygon(vx, vy, 9);
        // Lava sky glow
        g.setColor(new Color(255, 80, 0, 18));
        g.fillRect(0, 0, W(), HEIGHT / 2);
        // Ground lava
        g.setPaint(new GradientPaint(0, GROUND_Y, new Color(255, 80, 0, 200), 0, HEIGHT, new Color(180, 30, 0, 120)));
        g.fillRect(0, GROUND_Y, W(), HEIGHT - GROUND_Y);
        // Highlight: moving platform gets fiery edge (drawn separately in
        // drawMovingPlatformGlow)
        drawAllPlatforms(g, new Color(90, 45, 35), new Color(160, 70, 25), false);
        drawAmb(g, new Color(255, 140, 0, 40), new Color(255, 200, 50, 200));
    }

    // ── ICE ───────────────────────────────────────────────────────
    private void drawIce(Graphics2D g) {
        grad(g, new Color(150, 200, 230), new Color(60, 110, 160));
        // Snowfall
        long t = System.currentTimeMillis();
        g.setColor(new Color(255, 255, 255, 60));
        for (int[] p : pts) {
            int px = (int) (p[0] + Math.sin(t / 900.0 + p[2]) * 18) % W();
            int py = (int) ((p[1] + (t / 18 + p[2] * 4)) % GROUND_Y);
            if (py > 0)
                g.fillOval(px, py, 4, 4);
        }
        // Ice stalactites
        g.setColor(new Color(180, 220, 255, 180));
        for (int x = 40; x < W(); x += 65) {
            int ih = 25 + (x % 5) * 14;
            int[] sx = { x, x + 14, x + 28 };
            int[] sy = { 0, ih, 0 };
            g.fillPolygon(sx, sy, 3);
        }
        drawAllPlatforms(g, new Color(160, 210, 240), new Color(200, 235, 255), false);
        // Ice shimmer on all platforms
        g.setColor(new Color(220, 245, 255, 50));
        for (Rectangle r : data.platforms)
            g.fillRect(r.x, r.y, r.width, 5);
        drawAmb(g, new Color(200, 230, 255, 30), new Color(240, 250, 255, 200));
    }

    // ── SKY ISLANDS ───────────────────────────────────────────────
    private void drawSky(Graphics2D g) {
        // Dawn gradient
        grad(g, new Color(90, 130, 220), new Color(230, 170, 120));
        // Clouds (drifting)
        long t = System.currentTimeMillis();
        g.setColor(new Color(255, 255, 255, 190));
        int[] cxs = { 60, 300, 580, 850, 1110, 1320 };
        int[] cys = { 70, 150, 55, 130, 80, 155 };
        for (int i = 0; i < cxs.length; i++) {
            int cx = (int) (cxs[i] + (t / 50 + i * 120) % 250) - 100;
            drawCloud(g, cx % W(), cys[i]);
        }
        // Sky island platforms
        for (int i = 0; i < data.platforms.length; i++) {
            Rectangle r = data.platforms[i];
            boolean moving = data.isMovingPlatform(i);
            // Dirt body
            Color dirt = moving ? new Color(160, 100, 50) : new Color(120, 80, 45);
            g.setColor(dirt);
            g.fillRoundRect(r.x, r.y + 5, r.width, 22, 10, 10);
            // Grass
            Color grass = moving ? new Color(100, 200, 60) : new Color(60, 160, 50);
            g.setColor(grass);
            g.fillRoundRect(r.x, r.y, r.width, 12, 10, 10);
            // Tufts
            g.setColor(grass.brighter());
            for (int gx = r.x + 6; gx < r.x + r.width - 4; gx += 9)
                g.drawLine(gx, r.y, gx, r.y - 4);
            // Shadow under
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(r.x + 8, r.y + 28, r.width - 16, 10);
            // Moving indicator: pulsing arrow
            if (moving) {
                long tt = System.currentTimeMillis();
                float p = (float) (Math.sin(tt / 300.0) * 0.5 + 0.5);
                g.setColor(new Color(255, 255, 100, (int) (p * 180)));
                g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
                g.drawString("◄ ►", r.x + r.width / 2 - 14, r.y - 5);
            }
        }
        // Death zone indicator
        g.setPaint(new GradientPaint(0, HEIGHT - 50, new Color(0, 0, 0, 0), 0, HEIGHT, new Color(0, 0, 0, 140)));
        g.fillRect(0, HEIGHT - 50, W(), 50);
        drawAmb(g, new Color(255, 240, 180, 30), new Color(255, 255, 220, 200));
        return;
    }

    // ── NIGHT ─────────────────────────────────────────────────────
    private void drawNight(Graphics2D g) {
        g.setColor(new Color(5, 5, 18));
        g.fillRect(0, 0, W(), HEIGHT);
        // Stars
        long t = System.currentTimeMillis();
        g.setColor(new Color(255, 255, 255, 200));
        for (int[] p : pts) {
            float tw = (float) (Math.sin(t / 800.0 + p[2]) * 0.5 + 0.5);
            if (tw > 0.4f)
                g.fillOval(p[0], p[1] % 280, 2, 2);
        }
        // Crescent moon
        g.setColor(new Color(240, 240, 210));
        g.fillOval(W() - 140, 25, 80, 80);
        g.setColor(new Color(5, 5, 18));
        g.fillOval(W() - 115, 15, 80, 80);
        drawAllPlatforms(g, new Color(30, 30, 48), new Color(40, 60, 36), false);
        drawTorches(g);
        drawAmb(g, new Color(255, 200, 80, 30), new Color(255, 230, 120, 200));
    }

    // ── SHARED PLATFORM DRAWING ───────────────────────────────────
    private void drawAllPlatforms(Graphics2D g, Color stone, Color moss, boolean vines) {
        if (data.theme == MapTheme.SKY_ISLANDS)
            return;
        for (int i = 0; i < data.platforms.length; i++) {
            Rectangle r = data.platforms[i];
            if (i == 0)
                drawGround(g, r, stone, moss);
            else
                drawPlatform(g, r, stone, moss, vines, data.isMovingPlatform(i));
        }
    }

    private void drawGround(Graphics2D g, Rectangle r, Color s, Color m) {
        g.setColor(s.darker());
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(m);
        g.fillRect(r.x, r.y, r.width, 8);
        g.setColor(m.brighter());
        for (int x = r.x + 2; x < r.x + r.width; x += 6)
            g.drawLine(x, r.y, x, r.y - 3 - (x % 3));
    }

    private void drawPlatform(Graphics2D g, Rectangle r, Color s, Color m, boolean vines, boolean moving) {
        if (r.height < 5)
            return;
        Color base = moving ? s.brighter() : s;
        g.setColor(base);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(base.brighter());
        g.fillRect(r.x, r.y, r.width, 4);
        // Block lines
        g.setColor(s.darker());
        for (int bx = r.x; bx < r.x + r.width; bx += 32)
            g.drawLine(bx, r.y, bx, r.y + r.height);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        // Moss
        g.setColor(m);
        g.fillRect(r.x, r.y, r.width, 6);
        g.setColor(m.brighter());
        for (int x = r.x + 4; x < r.x + r.width - 4; x += 7)
            g.drawLine(x, r.y, x, r.y - 3 - (x % 3));
        // Vines
        if (vines) {
            g.setColor(new Color(22, 70, 22));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int vx = r.x + 18; vx < r.x + r.width - 10; vx += 28) {
                int vl = 12 + (vx * 7) % 22;
                g.drawLine(vx, r.y + r.height, vx, r.y + r.height + vl);
                g.setColor(new Color(30, 90, 25, 160));
                g.fillOval(vx - 3, r.y + r.height + vl - 2, 8, 5);
                g.setColor(new Color(22, 70, 22));
            }
            g.setStroke(new BasicStroke(1));
        }
        // Moving platform highlight
        if (moving) {
            long t = System.currentTimeMillis();
            float p = (float) (Math.sin(t / 250.0) * 0.5 + 0.5);
            g.setColor(new Color(255, 220, 80, (int) (p * 120)));
            g.fillRect(r.x, r.y, r.width, 4);
        }
    }

    // ── DECORATIONS ───────────────────────────────────────────────
    private void drawBarrel(Graphics2D g, int bx, int by) {
        g.setColor(new Color(120, 70, 30));
        g.fillRoundRect(bx, by - 28, 22, 28, 6, 6);
        g.setColor(new Color(160, 100, 50));
        g.drawRoundRect(bx, by - 28, 22, 28, 6, 6);
        g.setColor(new Color(80, 80, 80));
        g.fillRect(bx - 1, by - 24, 24, 4);
        g.fillRect(bx - 1, by - 10, 24, 4);
    }

    private void drawBanner(Graphics2D g, int bx, int by, int col) {
        g.setColor(new Color(100, 90, 120));
        g.fillRect(bx, by, 5, 60);
        Color fc = col == 0 ? new Color(200, 60, 60) : new Color(60, 100, 200);
        g.setColor(fc);
        int[] px = { bx + 5, bx + 40, bx + 5 };
        int[] py = { by, by + 15, by + 30 };
        g.fillPolygon(px, py, 3);
    }

    private void drawTorches(Graphics2D g) {
        long t = System.currentTimeMillis();
        int[] ti = { 1, 2, 8, 9 };
        for (int pi : ti) {
            if (pi >= data.platforms.length)
                continue;
            Rectangle r = data.platforms[pi];
            for (int tx : new int[] { r.x + 12, r.x + r.width - 16 }) {
                float p = (float) (Math.sin(t / 180.0 + tx) * 0.3 + 0.7);
                g.setColor(new Color(255, 160, 0, (int) (p * 55)));
                g.fillOval(tx - 35, r.y - 75, 70, 70);
                g.setColor(new Color(160, 80, 30));
                g.fillRect(tx - 3, r.y - 18, 6, 18);
                g.setColor(new Color(255, 180, 0, (int) (p * 220)));
                g.fillOval(tx - 5, r.y - 24, 10, 12);
            }
        }
    }

    private void drawMovingPlatformGlow(Graphics2D g) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < data.platforms.length; i++) {
            if (!data.isMovingPlatform(i))
                continue;
            Rectangle r = data.platforms[i];
            float p = (float) (Math.sin(t / 200.0) * 0.5 + 0.5);
            g.setColor(new Color(255, 220, 60, (int) (p * 80)));
            g.fillRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        }
    }

    // ── BACKGROUND HELPERS ────────────────────────────────────────
    private void grad(Graphics2D g, Color a, Color b) {
        g.setPaint(new GradientPaint(0, 0, a, 0, HEIGHT, b));
        g.fillRect(0, 0, W(), HEIGHT);
    }

    private void drawTrees(Graphics2D g, Color d, Color m) {
        g.setColor(d);
        int[] tx = { 0, 80, 200, 340, 480, 600, 720, 850, 990, 1120, 1240, 1360 };
        int[] tw = { 60, 90, 70, 80, 65, 100, 75, 85, 70, 80, 90, 60 };
        int[] th = { 500, 620, 550, 600, 520, 650, 580, 610, 540, 590, 620, 510 };
        for (int i = 0; i < tx.length; i++) {
            g.fillRect(tx[i] + tw[i] / 2 - 10, GROUND_Y - th[i] + 200, 20, th[i] - 200);
            g.fillOval(tx[i], GROUND_Y - th[i], tw[i] + 60, th[i] / 2);
            g.fillOval(tx[i] + 10, GROUND_Y - th[i] - 60, tw[i] + 40, th[i] / 2);
        }
        g.setColor(m);
        for (int x : new int[] { 100, 280, 450, 650, 820, 1000, 1180, 1320 }) {
            g.fillRect(x, GROUND_Y - 350, 18, 360);
            g.fillOval(x - 50, GROUND_Y - 420, 120, 180);
        }
    }

    private void drawRays(Graphics2D g) {
        int[] cxs = { W() / 2 + 60, W() / 2 - 200, 400, W() - 400 };
        int[] bw = { 220, 140, 100, 110 };
        int[] al = { 14, 10, 8, 9 };
        for (int i = 0; i < cxs.length; i++) {
            int hw = bw[i] / 2;
            g.setColor(new Color(180, 255, 160, al[i]));
            int[] bx = { cxs[i] - 15, cxs[i] + 15, cxs[i] + hw, cxs[i] - hw };
            int[] by = { 0, 0, GROUND_Y, GROUND_Y };
            g.fillPolygon(bx, by, 4);
        }
    }

    private void drawCloud(Graphics2D g, int cx, int cy) {
        g.fillOval(cx, cy + 10, 60, 40);
        g.fillOval(cx + 20, cy, 80, 50);
        g.fillOval(cx + 70, cy + 10, 60, 40);
    }

    private void drawAmb(Graphics2D g, Color glow, Color core) {
        long t = System.currentTimeMillis();
        for (int[] p : pts) {
            double ph = Math.sin(t / 700.0 + p[2] * 0.05);
            float al = (float) (ph * 0.5 + 0.5);
            if (al < 0.12f)
                continue;
            int dx = (int) (Math.sin(t / 1200.0 + p[2]) * 6);
            int dy = (int) (Math.cos(t / 900.0 + p[2]) * 4);
            int px = p[0] + dx, py = p[1] + dy;
            if (py <= 0 || py >= GROUND_Y)
                continue;
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(),
                    (int) (al * glow.getAlpha() / 255f * 40)));
            g.fillOval(px - 4, py - 4, 10, 10);
            g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(),
                    (int) (al * core.getAlpha() / 255f * 220)));
            g.fillOval(px, py, 4, 4);
        }
    }

    // ── SHRINK WALLS ──────────────────────────────────────────────
    private void drawShrinkWalls(Graphics2D g) {
        int so = data.getShrinkOffset();
        if (so <= 0)
            return;
        long t = System.currentTimeMillis();
        float p = (float) (Math.sin(t / 200.0) * 0.3 + 0.7);
        g.setColor(new Color(255, 40, 20, (int) (p * 150)));
        g.fillRect(0, 0, so, HEIGHT);
        g.fillRect(W() - so, 0, so, HEIGHT);
        g.setColor(new Color(255, 100, 0, (int) (p * 220)));
        g.setStroke(new BasicStroke(3));
        g.drawLine(so, 0, so, HEIGHT);
        g.drawLine(W() - so, 0, W() - so, HEIGHT);
        g.setStroke(new BasicStroke(1));
    }

    private int W() {
        return WIDTH;
    }
}