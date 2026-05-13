import java.awt.*;
import java.awt.image.BufferedImage;
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
    private BufferedImage bgCache;

    // ── Cached Color constants (avoid per-frame allocation ~60x/sec) ─────
    // FOREST
    private static final Color FC_BG1      = new Color(10, 24, 14);
    private static final Color FC_BG2      = new Color(20, 44, 24);
    private static final Color FC_TREE_FAR = new Color(8, 16, 10, 170);
    private static final Color FC_TREE_D   = new Color(10, 22, 13);
    private static final Color FC_TREE_M   = new Color(14, 32, 16);
    private static final Color FC_PLAT_S   = new Color(58, 53, 46);
    private static final Color FC_PLAT_M   = new Color(38, 115, 32);
    // CASTLE
    private static final Color CC_BG1      = new Color(15, 12, 28);
    private static final Color CC_BG2      = new Color(38, 32, 58);
    private static final Color CC_STAR     = new Color(255, 255, 255, 160);
    private static final Color CC_MOON1    = new Color(232, 232, 202);
    private static final Color CC_MOON2    = new Color(15, 12, 28);
    private static final Color CC_MOON_G   = new Color(255, 255, 200, 14);
    private static final Color CC_SIL      = new Color(18, 14, 33);
    private static final Color CC_MIST0    = new Color(28, 18, 48, 0);
    private static final Color CC_MIST1    = new Color(18, 12, 38, 180);
    private static final Color CC_PLAT_S   = new Color(75, 70, 90);
    private static final Color CC_PLAT_M   = new Color(50, 45, 70);
    private static final Color CC_AMB_G    = new Color(180, 160, 255, 32);
    private static final Color CC_AMB_C    = new Color(200, 190, 255, 185);
    private static final Color CC_STONE    = new Color(42, 38, 62);
    private static final Color CC_STONELT  = new Color(55, 50, 78);
    private static final Color CC_CREN     = new Color(38, 34, 56);
    private static final Color CC_WIN1     = new Color(255, 190, 60, 80);
    private static final Color CC_WIN2     = new Color(255, 190, 60, 50);
    private static final Color CC_GATE_BG  = new Color(8, 6, 16);
    private static final Color CC_GATE_G   = new Color(255, 160, 40, 22);
    private static final Color CC_SEAM     = new Color(30, 26, 46, 120);
    // VOLCANO
    private static final Color VC_BG1      = new Color(38, 8, 4);
    private static final Color VC_BG2      = new Color(85, 20, 5);
    private static final Color VC_GLOW     = new Color(255, 65, 0, 22);
    private static final Color VC_MTN1     = new Color(48, 10, 5);
    private static final Color VC_MTN2     = new Color(58, 14, 7);
    private static final Color VC_LAVA0    = new Color(255, 85, 0, 210);
    private static final Color VC_LAVA1    = new Color(185, 28, 0, 130);
    private static final Color VC_HAZ      = new Color(255, 50, 0, 40);
    private static final Color VC_PLAT_S   = new Color(92, 46, 36);
    private static final Color VC_PLAT_M   = new Color(162, 72, 26);
    private static final Color VC_AMB_G    = new Color(255, 140, 0, 42);
    private static final Color VC_AMB_C    = new Color(255, 200, 50, 205);
    // ICE
    private static final Color IC_BG1      = new Color(145, 198, 228);
    private static final Color IC_BG2      = new Color(58, 108, 158);
    private static final Color IC_SNOW     = new Color(255, 255, 255, 65);
    private static final Color IC_STLCT    = new Color(185, 225, 255, 190);
    private static final Color IC_TINT     = new Color(175, 215, 245, 28);
    private static final Color IC_SHIM0    = new Color(205, 238, 255, 0);
    private static final Color IC_SHIM1    = new Color(225, 248, 255, 130);
    private static final Color IC_PLAT_S   = new Color(158, 212, 242);
    private static final Color IC_PLAT_M   = new Color(202, 237, 255);
    private static final Color IC_TOP      = new Color(255, 255, 255, 60);
    private static final Color IC_AMB_G    = new Color(200, 230, 255, 32);
    private static final Color IC_AMB_C    = new Color(240, 250, 255, 205);
    // SKY
    private static final Color SK_BG1      = new Color(90, 130, 220);
    private static final Color SK_BG2      = new Color(230, 170, 120);
    private static final Color SK_CLOUD    = new Color(255, 255, 255, 190);
    private static final Color SK_DIRT_MV  = new Color(160, 100, 50);
    private static final Color SK_DIRT_ST  = new Color(120, 80, 45);
    private static final Color SK_GRASS_MV = new Color(100, 200, 60);
    private static final Color SK_GRASS_ST = new Color(60, 160, 50);
    private static final Color SK_SHADOW   = new Color(0, 0, 0, 40);
    private static final Color SK_DEATH0   = new Color(0, 0, 0, 0);
    private static final Color SK_DEATH1   = new Color(0, 0, 0, 140);
    private static final Color SK_AMB_G    = new Color(255, 240, 180, 30);
    private static final Color SK_AMB_C    = new Color(255, 255, 220, 200);
    private static final Font  SK_FONT     = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14);
    // NIGHT
    private static final Color NC_BG1      = new Color(4, 4, 16);
    private static final Color NC_BG2      = new Color(10, 10, 26);
    private static final Color NC_STAR     = new Color(255, 255, 255, 200);
    private static final Color NC_MOON1    = new Color(242, 242, 212);
    private static final Color NC_MOON2    = new Color(4, 4, 16);
    private static final Color NC_MOON_G   = new Color(255, 255, 200, 12);
    private static final Color NC_RUIN     = new Color(10, 10, 22);
    private static final Color NC_MIST0    = new Color(18, 18, 42, 0);
    private static final Color NC_MIST1    = new Color(12, 12, 32, 200);
    private static final Color NC_PLAT_S   = new Color(28, 28, 48);
    private static final Color NC_PLAT_M   = new Color(38, 58, 34);
    private static final Color NC_AMB_G    = new Color(255, 200, 80, 32);
    private static final Color NC_AMB_C    = new Color(255, 230, 120, 205);
    // Shared / decorations
    private static final Color BAR_BODY    = new Color(120, 70, 30);
    private static final Color BAR_RIM     = new Color(160, 100, 50);
    private static final Color BAR_BAND    = new Color(80, 80, 80);
    private static final Color PLT_HILITE  = new Color(255, 255, 255, 90);
    private static final Color PLT_SHADOW  = new Color(0, 0, 0, 50);
    private static final Color VINE_GRN    = new Color(22, 70, 22);
    private static final Color VINE_LEAF   = new Color(30, 90, 25, 160);
    private static final BasicStroke STROKE2 = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE1 = new BasicStroke(1);


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
    }

    // ── FOREST ────────────────────────────────────────────────────
    private void drawForest(Graphics2D g) {
        if (bgCache == null) {
            bgCache = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bgCache.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            grad(g2, FC_BG1, FC_BG2);
            // Far tree layer
            g2.setColor(FC_TREE_FAR);
            for (int x = 0; x < WIDTH; x += 90) { 
                int h2 = 420 + (x % 120); 
                g2.fillOval(x - 20, GROUND_Y - h2, 130, h2); 
            }
            // Mid tree layer
            drawTrees(g2, FC_TREE_D, FC_TREE_M);

            // Static Platforms
            drawAllPlatforms(g2, FC_PLAT_S, FC_PLAT_M, true);

            // Barrels
            drawBarrel(g2, 420, GROUND_Y); 
            drawBarrel(g2, 960, GROUND_Y);
            drawBarrel(g2, WIDTH/2 - 50, GROUND_Y); 
            drawBarrel(g2, WIDTH/2 + 30, GROUND_Y);
            
            g2.dispose();
        }
        g.drawImage(bgCache, 0, 0, null);
    }

    // ── CASTLE ────────────────────────────────────────────────────
    private void drawCastle(Graphics2D g) {
        grad(g, CC_BG1, CC_BG2);
        // Stars
        long ts = System.currentTimeMillis();
        g.setColor(CC_STAR);
        for (int[] p : pts) { float tw = (float)(Math.sin(ts/800.0+p[2])*0.5+0.5); if (tw>0.3f) g.fillOval(p[0], p[1]%260, 2, 2); }
        // Moon
        g.setColor(CC_MOON1); g.fillOval(W()-165, 22, 92, 92);
        g.setColor(CC_MOON2);    g.fillOval(W()-135, 12, 92, 92);
        g.setColor(CC_MOON_G); g.fillOval(W()-190, 5, 132, 132);
        // Far castle silhouettes
        g.setColor(CC_SIL);
        for (int[] t : new int[][]{{0,195,80},{280,158,100},{580,138,92},{880,165,100},{1080,148,80},{1280,188,90}}) {
            g.fillRect(t[0], t[1], t[2], HEIGHT-t[1]);
            for (int bx=t[0]; bx<t[0]+t[2]; bx+=20) g.fillRect(bx, t[1]-15, 12, 18);
        }
        // Castle BG drawing
        drawCastleBG(g);
        // Purple mist
        g.setPaint(new GradientPaint(0, GROUND_Y-70, CC_MIST0, 0, GROUND_Y, CC_MIST1));
        g.fillRect(0, GROUND_Y-70, W(), 70); g.setPaint(null);
        drawAllPlatforms(g, CC_PLAT_S, CC_PLAT_M, false);
        drawAmb(g, CC_AMB_G, CC_AMB_C);
    }

    // ── VOLCANO ───────────────────────────────────────────────────
    private void drawVolcano(Graphics2D g) {
        grad(g, VC_BG1, VC_BG2);
        // Fire sky glow
        g.setColor(VC_GLOW); g.fillRect(0, 0, W(), HEIGHT/2);
        // Mountain silhouettes
        g.setColor(VC_MTN1);
        int[] vx={0,320,480,600,710,860,W(),W(),0}, vy={HEIGHT,245,HEIGHT-75,195,HEIGHT-55,235,HEIGHT,HEIGHT,HEIGHT};
        g.fillPolygon(vx, vy, 9);
        g.setColor(VC_MTN2);
        int[] vx2={0,200,380,W()/2,720,900,1100,W(),W(),0}, vy2={HEIGHT,270,HEIGHT-60,210,HEIGHT-50,250,HEIGHT-45,HEIGHT,HEIGHT,HEIGHT};
        g.fillPolygon(vx2, vy2, 10);
        // Lava ground
        g.setPaint(new GradientPaint(0, GROUND_Y, VC_LAVA0, 0, HEIGHT, VC_LAVA1));
        g.fillRect(0, GROUND_Y, W(), HEIGHT-GROUND_Y); g.setPaint(null);
        // Lava surface shimmer
        long tl=System.currentTimeMillis(); float lp=(float)(Math.sin(tl/400.0)*0.5+0.5);
        g.setColor(new Color(255,180,0,(int)(lp*80))); g.fillRect(0, GROUND_Y, W(), 6);
        // Hazard floor markings
        g.setColor(VC_HAZ);
        g.fillRect(195, GROUND_Y-12, 165, 12);
        g.fillRect(615, GROUND_Y-12, 205, 12);
        g.fillRect(1015, GROUND_Y-12, 165, 12);
        drawAllPlatforms(g, VC_PLAT_S, VC_PLAT_M, false);
        drawAmb(g, VC_AMB_G, VC_AMB_C);
    }

    // ── ICE ───────────────────────────────────────────────────────
    private void drawIce(Graphics2D g) {
        grad(g, IC_BG1, IC_BG2);
        // Snowfall
        long t = System.currentTimeMillis();
        g.setColor(IC_SNOW);
        for (int[] p : pts) {
            int px = (int)(p[0] + Math.sin(t/900.0+p[2])*18) % W();
            int py = (int)((p[1] + (t/18 + p[2]*4)) % GROUND_Y);
            if (py > 0) { int sz=1+(p[2]%3); g.fillOval(px, py, sz+1, sz+1); }
        }
        // Ice stalactites
        g.setColor(IC_STLCT);
        for (int x = 35; x < W(); x += 62) {
            int ih = 22 + (x%5)*15;
            g.fillPolygon(new int[]{x,x+12,x+24}, new int[]{0,ih,0}, 3);
        }
        // Ice wall tint
        g.setColor(IC_TINT); g.fillRect(0, 0, W(), HEIGHT);
        // Floor shimmer
        g.setPaint(new GradientPaint(0, GROUND_Y-28, IC_SHIM0, 0, GROUND_Y, IC_SHIM1));
        g.fillRect(0, GROUND_Y-28, W(), 28); g.setPaint(null);
        drawAllPlatforms(g, IC_PLAT_S, IC_PLAT_M, false);
        // Ice shimmer strips on platform tops
        g.setColor(IC_TOP);
        for (Rectangle r : data.platforms) g.fillRect(r.x+2, r.y, r.width-4, 3);
        drawAmb(g, IC_AMB_G, IC_AMB_C);
    }

    // ── SKY ISLANDS ───────────────────────────────────────────────
    private void drawSky(Graphics2D g) {
        // Dawn gradient
        grad(g, SK_BG1, SK_BG2);
        // Clouds (drifting)
        long t = System.currentTimeMillis();
        g.setColor(SK_CLOUD);
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
            Color dirt = moving ? SK_DIRT_MV : SK_DIRT_ST;
            g.setColor(dirt);
            g.fillRoundRect(r.x, r.y + 5, r.width, 22, 10, 10);
            // Grass
            Color grass = moving ? SK_GRASS_MV : SK_GRASS_ST;
            g.setColor(grass);
            g.fillRoundRect(r.x, r.y, r.width, 12, 10, 10);
            // Tufts
            g.setColor(grass.brighter());
            for (int gx = r.x + 6; gx < r.x + r.width - 4; gx += 9)
                g.drawLine(gx, r.y, gx, r.y - 4);
            // Shadow under
            g.setColor(SK_SHADOW);
            g.fillOval(r.x + 8, r.y + 28, r.width - 16, 10);
            // Moving indicator: pulsing arrow
            if (moving) {
                long tt = System.currentTimeMillis();
                float p = (float) (Math.sin(tt / 300.0) * 0.5 + 0.5);
                g.setColor(new Color(255, 255, 100, (int) (p * 180)));
                g.setFont(SK_FONT);
                g.drawString("◄ ►", r.x + r.width / 2 - 14, r.y - 5);
            }
        }
        // Death zone indicator
        g.setPaint(new GradientPaint(0, HEIGHT - 50, SK_DEATH0, 0, HEIGHT, SK_DEATH1));
        g.fillRect(0, HEIGHT - 50, W(), 50);
        drawAmb(g, SK_AMB_G, SK_AMB_C);
        return;
    }

    // ── NIGHT ─────────────────────────────────────────────────────
    private void drawNight(Graphics2D g) {
        grad(g, NC_BG1, NC_BG2);
        // Stars
        long t = System.currentTimeMillis();
        g.setColor(NC_STAR);
        for (int[] p : pts) { float tw=(float)(Math.sin(t/800.0+p[2])*0.5+0.5); if (tw>0.35f) g.fillOval(p[0], p[1]%280, 2, 2); }
        // Crescent moon
        g.setColor(NC_MOON1); g.fillOval(W()-140, 25, 80, 80);
        g.setColor(NC_MOON2);       g.fillOval(W()-115, 15, 80, 80);
        g.setColor(NC_MOON_G);  g.fillOval(W()-162,5,122,122);
        // Distant ruins silhouette
        g.setColor(NC_RUIN);
        for (int[] b : new int[][]{{50,350,60},{200,320,40},{500,340,70},{750,310,55},{1000,345,65},{1200,330,50}}) {
            g.fillRect(b[0], b[1], b[2], HEIGHT-b[1]);
            for (int wx=b[0]; wx<b[0]+b[2]; wx+=14) g.fillRect(wx, b[1]-10, 8, 12);
        }
        // Ground mist
        g.setPaint(new GradientPaint(0, GROUND_Y-38, NC_MIST0, 0, GROUND_Y, NC_MIST1));
        g.fillRect(0, GROUND_Y-38, W(), 38); g.setPaint(null);
        drawAllPlatforms(g, NC_PLAT_S, NC_PLAT_M, false);
        drawTorches(g);
        drawAmb(g, NC_AMB_G, NC_AMB_C);
    }

    // ── CASTLE BACKGROUND ────────────────────────────────────────────
    private void drawCastleBG(Graphics2D g) {
        int cx = W() / 2, baseY = GROUND_Y;
        // ── Left tower (x=cx-340..cx-220) ──
        int lx = cx - 340, tw = 120, th = 480;
        g.setColor(CC_STONE); g.fillRect(lx, baseY-th, tw, th);
        g.setColor(CC_STONELT); g.fillRect(lx, baseY-th, tw, 6); // top highlight
        for (int bx=lx; bx<lx+tw; bx+=20) { g.setColor(CC_CREN); g.fillRect(bx, baseY-th-20, 14, 22); } // battlements
        // Windows L tower
        g.setColor(CC_WIN1); g.fillRect(lx+30, baseY-th+60, 20, 30);
        g.setColor(CC_WIN1); g.fillRect(lx+70, baseY-th+60, 20, 30);
        g.setColor(CC_WIN2); g.fillRect(lx+30, baseY-th+160, 20, 30);
        g.setColor(CC_WIN2); g.fillRect(lx+70, baseY-th+160, 20, 30);
        // ── Right tower (x=cx+220..cx+340) ──
        int rx = cx + 220;
        g.setColor(CC_STONE); g.fillRect(rx, baseY-th, tw, th);
        g.setColor(CC_STONELT); g.fillRect(rx, baseY-th, tw, 6);
        for (int bx=rx; bx<rx+tw; bx+=20) { g.setColor(CC_CREN); g.fillRect(bx, baseY-th-20, 14, 22); }
        g.setColor(CC_WIN1); g.fillRect(rx+30, baseY-th+60, 20, 30);
        g.setColor(CC_WIN1); g.fillRect(rx+70, baseY-th+60, 20, 30);
        g.setColor(CC_WIN2); g.fillRect(rx+30, baseY-th+160, 20, 30);
        g.setColor(CC_WIN2); g.fillRect(rx+70, baseY-th+160, 20, 30);
        // ── Centre wall ──
        int ww = rx - (lx + tw); // wall width between towers
        int wy = baseY - 340;
        g.setColor(CC_STONE); g.fillRect(lx+tw, wy, ww, 340);
        g.setColor(CC_STONELT); g.fillRect(lx+tw, wy, ww, 5);
        for (int bx=lx+tw; bx<rx; bx+=20) { g.setColor(CC_CREN); g.fillRect(bx, wy-18, 14, 20); }
        // Arched gate
        g.setColor(CC_GATE_BG);
        g.fillRect(cx-28, wy+200, 56, 140);
        g.fillArc(cx-28, wy+175, 56, 50, 0, 180);
        // Gate glow
        g.setColor(CC_GATE_G); g.fillRect(cx-28, wy+200, 56, 140);
        // Block seam lines on wall
        g.setColor(CC_SEAM);
        for (int gy2=wy; gy2<baseY; gy2+=24) g.drawLine(lx+tw, gy2, rx, gy2);
        for (int bx2=lx+tw; bx2<rx; bx2+=36) g.drawLine(bx2, wy, bx2, baseY);
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
        if (r.height < 5) return;
        Color base = moving ? s.brighter() : s;
        // Body
        g.setColor(base);
        g.fillRect(r.x, r.y, r.width, r.height);
        // Block seams
        g.setColor(s.darker());
        for (int bx = r.x; bx < r.x+r.width; bx += 32) g.drawLine(bx, r.y, bx, r.y+r.height);
        g.drawRect(r.x, r.y, r.width-1, r.height-1);
        // Moss layer
        g.setColor(m);
        g.fillRect(r.x, r.y, r.width, 7);
        // ── TOP EDGE HIGHLIGHT (readability) ──
        if (data.theme != MapTheme.FOREST) {
            g.setColor(PLT_HILITE);
            g.fillRect(r.x + 1, r.y, r.width - 2, 2);
        }
        // Moss tufts
        g.setColor(m.brighter());
        for (int x = r.x+4; x < r.x+r.width-4; x += 7) g.drawLine(x, r.y, x, r.y-3-(x%3));
        // Underside shadow for depth
        if (data.theme != MapTheme.FOREST) {
            g.setColor(PLT_SHADOW);
            g.fillRect(r.x, r.y+r.height, r.width, 4);
        }
        // Vines
        if (vines) {
            g.setColor(VINE_GRN);
            g.setStroke(STROKE2);
            for (int vx = r.x+18; vx < r.x+r.width-10; vx += 28) {
                int vl = 12 + (vx*7)%22;
                g.drawLine(vx, r.y+r.height, vx, r.y+r.height+vl);
                g.setColor(VINE_LEAF);
                g.fillOval(vx-3, r.y+r.height+vl-2, 8, 5);
                g.setColor(VINE_GRN);
            }
            g.setStroke(STROKE1);
        }
        // Moving platform highlight
        if (moving && data.theme != MapTheme.FOREST) {
            long t = System.currentTimeMillis();
            float p = (float)(Math.sin(t/250.0)*0.5+0.5);
            g.setColor(new Color(255, 220, 80, (int)(p*130)));
            g.fillRect(r.x, r.y, r.width, 4);
        }
    }

    // ── DECORATIONS ───────────────────────────────────────────────
    private void drawBarrel(Graphics2D g, int bx, int by) {
        g.setColor(BAR_BODY);
        g.fillRoundRect(bx, by - 28, 22, 28, 6, 6);
        g.setColor(BAR_RIM);
        g.drawRoundRect(bx, by - 28, 22, 28, 6, 6);
        g.setColor(BAR_BAND);
        g.fillRect(bx - 1, by - 24, 24, 4);
        g.fillRect(bx - 1, by - 10, 24, 4);
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
        if (data.theme == MapTheme.FOREST) return;
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


    private int W() {
        return WIDTH;
    }
}