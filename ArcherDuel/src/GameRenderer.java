import java.awt.*;
import java.util.List;

public class GameRenderer {
    static final int W = GameWindow.WIDTH, H = GameWindow.HEIGHT;
    static final Color GOLD = new Color(230, 185, 55),
            AMBER = new Color(255, 195, 70),
            DARK = new Color(20, 16, 38),
            STONE = new Color(55, 50, 72),
            TITLE_COL = new Color(255, 210, 85);
    static final Font F72 = new Font("Monospaced", Font.BOLD, 72), F36 = new Font("Monospaced", Font.BOLD, 36),
            F20 = new Font("Monospaced", Font.BOLD, 20), F16 = new Font("Monospaced", Font.BOLD, 16),
            F14 = new Font("Monospaced", Font.PLAIN, 14),
            F11 = new Font("Monospaced", Font.PLAIN, 11),
            F9 = new Font("Monospaced", Font.BOLD, 9);

    static final Color SHADOW = new Color(0, 0, 0, 140),
            PANEL_BG = new Color(20, 16, 35, 210),
            PANEL_BRD = new Color(180, 140, 40, 90),
            BTN_BG_H = new Color(70, 52, 18),
            BTN_BG_S = new Color(55, 42, 14),
            BTN_BG_N = new Color(32, 26, 10),
            BTN_BRD_N = new Color(120, 95, 40),
            BTN_FC_H = new Color(255, 235, 160),
            BTN_FC_N = new Color(200, 175, 110),
            BTN_GLOW = new Color(230, 185, 55, 70),
            STAR_W = new Color(255, 255, 255),
            STAR_G = new Color(255, 230, 160),
            STAR_B = new Color(200, 220, 255),
            TEXT_SUB = new Color(210, 175, 100),
            DIVIDER = new Color(200, 155, 50, 120),
            BAR_BG = new Color(40, 30, 12),
            BAR_BRD = new Color(150, 110, 40, 120),
            BAR_SHN = new Color(255, 240, 180, 55),
            BAR_TIP = new Color(255, 245, 160),
            HINT_COL = new Color(120, 110, 80),
            HUD_BLUE = new Color(60, 120, 255),
            HUD_RED = new Color(255, 70, 70),
            HP_FULL = new Color(220, 50, 50),
            HP_EMPTY = new Color(50, 25, 25),
            HP_GRAY = new Color(70, 70, 70),
            HP_RND_BG = new Color(40, 40, 40),
            HP_RAGE = new Color(255, 80, 0),
            HP_LABEL = new Color(100, 100, 100),
            FIGHT_COL = new Color(80, 255, 80),
            FINAL_GLO = new Color(255, 220, 0),
            LBL_LOAD = new Color(210, 185, 130),
            LBL_TIP = new Color(185, 160, 100),
            SUB_FOR = new Color(175, 215, 140),
            CTRL_TEXT = new Color(180, 180, 180),
            CTRL_P1 = new Color(80, 140, 255),
            CTRL_P2 = new Color(255, 80, 80);

    static final BasicStroke STROKE2 = new BasicStroke(2f),
            STROKE1_2 = new BasicStroke(1.2f),
            STROKE1 = new BasicStroke(1f),
            STROKE3 = new BasicStroke(3f);

    private static final Rectangle REUSABLE_RECT = new Rectangle();

    // ── Button layout helpers ──────────────────────────────────────
    static Rectangle btn(int cx, int y, int bw, int bh) {
        return new Rectangle(cx - bw / 2, y, bw, bh);
    }

    static Rectangle menuBtn(int i) {
        return btn(W / 2, H / 2 - 20 + i * 58, 260, 44);
    }

    static Rectangle pauseBtn(int i) {
        return btn(W / 2, H / 2 + 10 + i * 54, 260, 44);
    }

    static Rectangle resultBtn(int i) {
        return btn(W / 2, H / 2 + 90 + i * 52, 240, 42);
    }

    static Rectangle settingsBtn(int i) {
        return btn(W / 2, H / 2 - 40 + i * 64, 280, 44);
    }

    static Rectangle ctrlBtn(int i) {
        return btn(W / 2, H - 110 + i * 54, 260, 44);
    }

    // ── Hit tests ─────────────────────────────────────────────────
    static int menuHit(int x, int y) {
        for (int i = 0; i < 4; i++)
            if (menuBtn(i).contains(x, y))
                return i;
        return -1;
    }

    static int pauseHit(int x, int y) {
        for (int i = 0; i < 4; i++)
            if (pauseBtn(i).contains(x, y))
                return i;
        return -1;
    }

    static int resultHit(int x, int y) {
        for (int i = 0; i < 3; i++)
            if (resultBtn(i).contains(x, y))
                return i;
        return -1;
    }

    static int settingsHit(int x, int y) {
        for (int i = 0; i < 3; i++)
            if (settingsBtn(i).contains(x, y))
                return i;
        return -1;
    }

    static int controlsHit(int x, int y) {
        if (ctrlBtn(0).contains(x, y))
            return 0;
        return -1;
    }

    static int exitHit(int x, int y) {
        int bw = 180, bh = 44, gap = 30;
        int totalW = bw * 2 + gap;
        int startX = W / 2 - totalW / 2;
        for (int i = 0; i < 2; i++) {
            REUSABLE_RECT.setBounds(startX + i * (bw + gap), H / 2 + 30, bw, bh);
            if (REUSABLE_RECT.contains(x, y))
                return i;
        }
        return -1;
    }

    static int mapHit(int x, int y, int n) {
        int cx = W / 2, py = 80, ph = 370;
        // Arrows
        REUSABLE_RECT.setBounds(cx - 380, py + ph / 2 - 22, 80, 44);
        if (REUSABLE_RECT.contains(x, y))
            return 0; // Left
        REUSABLE_RECT.setBounds(cx + 300, py + ph / 2 - 22, 80, 44);
        if (REUSABLE_RECT.contains(x, y))
            return 1; // Right
        // Start button
        REUSABLE_RECT.setBounds(cx - 110, py + ph + 75, 220, 44);
        if (REUSABLE_RECT.contains(x, y))
            return 2; // Start Match
        // Dots
        for (int i = 0; i < n; i++) {
            REUSABLE_RECT.setBounds(cx - n * 11 + i * 22, py + ph + 16, 14, 14);
            if (REUSABLE_RECT.contains(x, y))
                return 10 + i; // 10+i = Dot index
        }
        return -1;
    }

    // ── Draw utilities ─────────────────────────────────────────────
    static void txt(Graphics2D g, String s, int cx, int y, Font f, Color c) {
        g.setFont(f);
        FontMetrics m = g.getFontMetrics();
        int x = cx - m.stringWidth(s) / 2;
        g.setColor(SHADOW);
        g.drawString(s, x + 2, y + 2);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    /** Spaced title: draws each char with extra kerning for large display fonts. */
    static void txtSpaced(Graphics2D g, String s, int cx, int y, Font f, Color c, int kern) {
        g.setFont(f);
        FontMetrics m = g.getFontMetrics();
        int totalW = m.stringWidth(s) + kern * (s.length() - 1);
        int x = cx - totalW / 2;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            g.setColor(SHADOW);
            g.drawString(ch, x + 2, y + 2);
            g.setColor(c);
            g.drawString(ch, x, y);
            x += m.stringWidth(ch) + kern;
        }
    }

    static void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(PANEL_BG);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(PANEL_BRD);
        g.drawRoundRect(x, y, w, h, 14, 14);
    }

    static void drawBtn(Graphics2D g, Rectangle r, String s, boolean sel, boolean hover) {
        Color bg = hover ? BTN_BG_H : sel ? BTN_BG_S : BTN_BG_N;
        Color border = hover || sel ? AMBER : BTN_BRD_N;
        Color fc = hover || sel ? BTN_FC_H : BTN_FC_N;
        if (hover || sel) {
            g.setColor(BTN_GLOW);
            g.fillRoundRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, 12, 12);
        }
        g.setColor(bg);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(border);
        g.setStroke(hover || sel ? STROKE2 : STROKE1_2);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setStroke(STROKE1);
        g.setFont(hover || sel ? F20 : F16);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(fc);
        g.drawString(s, r.x + r.width / 2 - fm.stringWidth(s) / 2, r.y + r.height / 2 + fm.getAscent() / 2 - 3);
    }

    static void bg(Graphics2D g, Color a, Color b) {
        g.setPaint(new GradientPaint(0, 0, a, 0, H, b));
        g.fillRect(0, 0, W, H);
    }

    static void bgPts(Graphics2D g, float[][] pts, int al) {
        for (float[] p : pts) {
            int idx = (int) (p[0] + p[1]) % 3;
            Color base = idx == 0 ? STAR_G : idx == 1 ? STAR_W : STAR_B;
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), al));
            int sz = (int) (p[0] * 31 + p[1]) % 3 == 0 ? 3 : 2;
            g.fillOval((int) p[0], (int) p[1], sz, sz);
        }
    }

    static void drawParticles(Graphics2D g, List<Particle> ps) {
        for (var p : ps) {
            if (!p.active)
                continue;
            int a = Math.max(0, Math.min(255, p.alpha));
            g.setColor(new Color(p.r, p.grn, p.b, a));
            g.fillRect((int) p.x, (int) p.y, p.size, p.size);
        }
    }

    // ── LOADING ───────────────────────────────────────────────────
    static void drawLoading(Graphics2D g, int tick, int max, float[][] pts, int mx, int my) {
        // Brighter, warmer background
        bg(g, new Color(28, 20, 50), new Color(55, 30, 15));
        bgPts(g, pts, 60);
        long t = System.currentTimeMillis();

        // ── Layout anchors ──
        int centerX = W / 2;
        int titleY = H / 2 - 110; // main title baseline
        int subY = titleY + 36; // subtitle just below title
        int divY = subY + 22; // thin separator line
        int barY = divY + 28; // progress bar top
        int barW = 460;
        int barH = 18;
        int barX = centerX - barW / 2;
        int loadLblY = barY + barH + 26; // "Loading..." label
        int tipY = H - 48;
        int skipY = H - 22;

        // Ambient glow behind title
        float glow = (float) (Math.sin(t / 600.0) * 0.5 + 0.5);
        g.setColor(new Color(200, 140, 40, (int) (20 + glow * 40)));
        g.fillOval(centerX - 260, titleY - 80, 520, 140);

        // ── Title ──
        txtSpaced(g, "ARCHER DUEL", centerX, titleY, F72, TITLE_COL, 6);

        // ── Subtitle ──
        txt(g, "1v1  Arena", centerX, subY, F14, TEXT_SUB);

        // ── Thin gold divider ──
        g.setColor(DIVIDER);
        g.fillRect(centerX - 160, divY, 320, 1);

        // ── Progress bar ──
        g.setColor(BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, 9, 9);
        g.setColor(BAR_BRD);
        g.drawRoundRect(barX, barY, barW, barH, 9, 9);
        float r = Math.min(1f, (float) tick / max);
        g.setPaint(new GradientPaint(barX, 0, new Color(160, 90, 20), barX + barW, 0, AMBER));
        g.fillRoundRect(barX, barY, (int) (barW * r), barH, 9, 9);
        g.setPaint(null);
        // Sheen
        g.setColor(BAR_SHN);
        g.fillRoundRect(barX, barY, (int) (barW * r), barH / 2, 9, 9);
        // Arrow-tip at leading edge
        int tip = barX + (int) (barW * r);
        g.setColor(BAR_TIP);
        g.fillPolygon(new int[] { tip, tip + 14, tip }, new int[] { barY, barY + barH / 2, barY + barH }, 3);

        // ── "Loading..." label ──
        txt(g, "Loading...", centerX, loadLblY, F14, LBL_LOAD);

        // ── Tip ──
        String[] tips = { "Headshots deal double damage!", "Hold F or RCTRL to charge shot",
                "Dash makes you invincible briefly", "Sky Islands: fall off = instant death",
                "Watch wind direction before shooting" };
        txt(g, "Tip:  " + tips[(tick / 30) % tips.length], centerX, tipY, F11, LBL_TIP);

        // ── Skip hint ──
        txt(g, "Press any key or click to skip", centerX, skipY, F11, HINT_COL);
    }

    // ── MAIN MENU ─────────────────────────────────────────────────
    static void drawMainMenu(Graphics2D g, int cur, float pulse, float[][] pts, int mx, int my) {
        bg(g, new Color(10, 20, 14), new Color(22, 40, 22));
        bgPts(g, pts, 50);

        // ── Trees (decorative background) ──
        g.setColor(new Color(10, 22, 12));
        int[] tx = { 0, 110, 240, 380, 530, 670, 810, 960, 1110, 1270 };
        int[] tw = { 80, 65, 75, 60, 85, 70, 75, 60, 80, 75 };
        for (int i = 0; i < tx.length; i++) {
            int h2 = tw[i] * 7;
            g.fillRect(tx[i] + tw[i] / 2 - 7, H - h2, 14, h2);
            g.fillOval(tx[i], H - h2 - 40, tw[i] + 50, h2 / 2 + 40);
            g.fillOval(tx[i] + 8, H - h2 - 90, tw[i] + 35, h2 / 2 + 50);
        }
        // Fog at bottom
        g.setPaint(new GradientPaint(0, H - 120, new Color(18, 35, 18, 0), 0, H, new Color(10, 22, 14, 210)));
        g.fillRect(0, H - 120, W, 120);
        g.setPaint(null);

        // ── Layout anchors ──
        int centerX = W / 2;
        int titleY = H / 2 - 155; // main title baseline
        int subY = titleY + 36; // subtitle
        int divY = subY + 20; // thin gold divider

        // ── Ambient glow behind title ──
        long t = System.currentTimeMillis();
        float glow = (float) (Math.sin(t / 700.0) * 0.5 + 0.5);
        g.setColor(new Color(180, 130, 30, (int) (15 + glow * 30)));
        g.fillOval(centerX - 280, titleY - 80, 560, 140);

        // ── Title ──
        txtSpaced(g, "ARCHER DUEL", centerX, titleY, F72, TITLE_COL, 6);

        // ── Subtitle ──
        txt(g, "1v1  Arena", centerX, subY, F14, SUB_FOR);

        // ── Thin gold divider ──
        g.setColor(DIVIDER);
        g.fillRect(centerX - 140, divY, 280, 1);

        // ── Menu buttons (PLAY / CONTROLS / SETTINGS / EXIT) ──
        String[] items = { "PLAY", "CONTROLS", "SETTINGS", "EXIT" };
        for (int i = 0; i < items.length; i++) {
            Rectangle r = menuBtn(i);
            boolean hov = r.contains(mx, my);
            drawBtn(g, r, items[i], i == cur, hov);
        }

        // ── Hint at very bottom ──
        txt(g, "↑↓  Navigate     ENTER / Click = Select", centerX, H - 22, F11, HINT_COL);
    }

    // ── MAP SELECT ────────────────────────────────────────────────
    static void drawMapSelect(Graphics2D g, int cur, MapTheme[] maps, float pulse, float trans, int dir, int mx,
            int my) {
        MapTheme m = maps[cur];
        Color bgc = mapCol(m);
        bg(g, bgc.darker().darker(), bgc.darker());
        txt(g, "SELECT  MAP", W / 2, 55, F36, TITLE_COL);
        // Card
        int cx = W / 2, py = 80, pw = 640, ph = 370;
        panel(g, cx - pw / 2, py, pw, ph);
        g.setColor(bgc);
        g.fillRoundRect(cx - pw / 2 + 8, py + 8, pw - 16, ph - 70, 10, 10);
        drawMapIcon(g, m, cx, py + 8 + (ph - 70) / 2);
        txt(g, m.displayName, cx, py + ph - 32, F20, Color.WHITE);
        // Nav arrows
        REUSABLE_RECT.setBounds(cx - 380, py + ph / 2 - 22, 80, 44);
        // Actually, drawBtn takes a Rectangle. I should probably just pass the bounds.
        // But drawBtn uses r.x, r.y etc.
        // Let's use a local Rectangle for buttons if they are used for drawing too, or
        // just drawBtn with bounds.
        // Since drawBtn is called with a Rectangle, I'll keep it as is for now or use a
        // local one.
        // Actually, Rectangle is small, but if I can avoid it...
        // Let's use a simple Rectangle for now to avoid complexity in this refactor.
        Rectangle lb = new Rectangle(cx - 380, py + ph / 2 - 22, 80, 44);
        Rectangle rb = new Rectangle(cx + 300, py + ph / 2 - 22, 80, 44);
        drawBtn(g, lb, "◄", false, lb.contains(mx, my));
        drawBtn(g, rb, "►", false, rb.contains(mx, my));
        // Map dots
        for (int i = 0; i < maps.length; i++) {
            g.setColor(i == cur ? Color.WHITE : HP_RND_BG);
            g.fillOval(cx - maps.length * 11 + i * 22, py + ph + 16, 14, 14);
        }
        // Start btn
        Rectangle sb = btn(cx, py + ph + 75, 220, 44);
        drawBtn(g, sb, "START  MATCH", false, sb.contains(mx, my));
        txt(g, "◄ ►  Change Map     ESC = Back", cx, H - 22, F11, HINT_COL);
    }

    // ── CONTROLS ──────────────────────────────────────────────────
    static void drawControls(Graphics2D g, float pulse, int mx, int my) {
        bg(g, DARK, new Color(18, 14, 32));
        txt(g, "CONTROLS", W / 2, 58, F36, TITLE_COL);
        int col1 = W / 2 - 290, col2 = W / 2 + 30, cw = 260, py = 80;
        panel(g, col1 - 10, py, cw + 20, 240);
        panel(g, col2 - 10, py, cw + 20, 240);
        g.setColor(CTRL_P1);
        g.setFont(F20);
        g.drawString("PLAYER 1", col1, py + 28);
        g.setColor(CTRL_P2);
        g.drawString("PLAYER 2", col2, py + 28);
        g.setFont(F14);
        g.setColor(CTRL_TEXT);
        String[] p1c = { "A / D  —  Move", "W / SPACE  —  Jump", "F  —  Shoot  (Hold=Charge)", "LEFT SHIFT  —  Dash" };
        String[] p2c = { "← / →  —  Move", "↑  —  Jump", "RIGHT CTRL  —  Shoot", "RIGHT SHIFT  —  Dash" };
        for (int i = 0; i < p1c.length; i++) {
            g.drawString(p1c[i], col1, py + 58 + i * 38);
            g.drawString(p2c[i], col2, py + 58 + i * 38);
        }
        // Buttons
        Rectangle r = ctrlBtn(0);
        drawBtn(g, r, "BACK", true, r.contains(mx, my));
        txt(g, "ESC / ENTER = Back", W / 2, H - 22, F11, HINT_COL);
    }

    // ── SETTINGS ──────────────────────────────────────────────────
    static void drawSettings(Graphics2D g, boolean bgm, boolean sfx, float pulse, int mx, int my) {
        bg(g, DARK, new Color(18, 14, 32));
        txt(g, "SETTINGS", W / 2, 58, F36, TITLE_COL);
        String[] sl = { "MUSIC: " + (bgm ? "ON" : "OFF"), "SFX: " + (sfx ? "ON" : "OFF"), "BACK" };
        for (int i = 0; i < 3; i++) {
            Rectangle r = settingsBtn(i);
            boolean hov = r.contains(mx, my);
            drawBtn(g, r, sl[i], false, hov);
        }
        txt(g, "Click or ENTER to toggle    ESC = Back", W / 2, H - 22, F11, HINT_COL);
    }

    // ── HUD ───────────────────────────────────────────────────────
    static void drawHUD(Graphics2D g, Player p1, Player p2, float wx, int tick) {
        drawHP(g, 10, 10, p1, HUD_BLUE, true);
        drawHP(g, W - 310, 10, p2, HUD_RED, false);
    }

    static void drawHP(Graphics2D g, int x, int y, Player p, Color c, boolean left) {
        panel(g, x, y, 300, 52);
        g.setColor(p.isAlive() ? c : HP_GRAY);
        g.setFont(F14);
        g.drawString(left ? "P1  WASD+F" : "P2  ARROWS+RCTRL", x + 8, y + 17);
        for (int i = 0; i < Player.MAX_HP; i++) {
            boolean f = i < p.hp;
            g.setColor(f ? HP_FULL : HP_EMPTY);
            int hx = x + 8 + i * 22, hy = y + 23;
            g.fillOval(hx, hy, 9, 9);
            g.fillOval(hx + 5, hy, 9, 9);
            g.fillPolygon(new int[] { hx, hx + 7, hx + 14 }, new int[] { hy + 6, hy + 16, hy + 6 }, 3);
        }

    }


    // ── OVERLAYS ─────────────────────────────────────────────────
    static void drawCountdown(Graphics2D g, int tick) {
        if (tick >= 180)
            return;
        int s = 3 - tick / 60;
        String t = s > 0 ? String.valueOf(s) : "FIGHT!";
        txt(g, t, W / 2, H / 2, s > 0 ? F72 : F36, s > 0 ? Color.WHITE : FIGHT_COL);
    }

    static void drawSlowMo(Graphics2D g, float r) {
        g.setColor(new Color(0, 0, 0, (int) (r * 40)));
        g.fillRect(0, 0, W, H);
        txt(g, "★  FINAL HIT  ★", W / 2, H / 2 - 160, F20,
                new Color(FINAL_GLO.getRed(), FINAL_GLO.getGreen(), FINAL_GLO.getBlue(), (int) (r * 220)));
    }

    // ── PAUSE ─────────────────────────────────────────────────────
    static void drawPause(Graphics2D g, int cur, int mx, int my) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, W, H);
        txt(g, "PAUSED", W / 2, H / 2 - 80, F72, Color.WHITE);
        String[] pl = { "RESUME", "RESTART", "SETTINGS", "MAIN MENU" };
        for (int i = 0; i < 4; i++) {
            Rectangle r = pauseBtn(i);
            drawBtn(g, r, pl[i], i == cur, r.contains(mx, my));
        }
        txt(g, "↑↓ Navigate    ENTER = Select    ESC = Resume", W / 2, H - 22, F11, PANEL_BRD);
    }

    // ── RESULTS ───────────────────────────────────────────────────
    static void drawResults(Graphics2D g, int win, String wt, int r1, int r2, int rw, int f1, int h1, int s1, int f2,
            int h2, int s2, int cur, int mx, int my) {
        // Dark overlay
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, W, H);

        // Layout anchors
        int cx = W / 2;
        int winY = H / 2 - 140; // winner text
        int scoreY = winY + 52; // score line
        int divY = scoreY + 20; // thin divider
        int hintY = H - 22;

        // Winner colour
        Color wc = win == 1 ? STAR_B : HUD_RED;

        // Winner text (large, spaced)
        txtSpaced(g, wt.toUpperCase(), cx, winY, F72, wc, 4);

        // Score "P1 2 — 1 P2"
        txt(g, "P1   " + r1 + "  —  " + r2 + "   P2", cx, scoreY, F36, Color.WHITE);

        // Thin divider
        g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), 120));
        g.fillRect(cx - 180, divY, 360, 1);

        // Three action buttons
        String[] rl = { "REMATCH", "MAP SELECT", "MAIN MENU" };
        for (int i = 0; i < 3; i++) {
            Rectangle r = resultBtn(i);
            drawBtn(g, r, rl[i], i == cur, r.contains(mx, my));
        }

        txt(g, "↑↓  Navigate     ENTER = Select     ESC = Main Menu", cx, hintY, F11, HINT_COL);
    }

    // ── EXIT CONFIRM ─────────────────────────────────────────────
    static void drawExitConfirm(Graphics2D g, int cur, int mx, int my) {
        // Darken background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, W, H);

        panel(g, W / 2 - 200, H / 2 - 120, 400, 240);
        txt(g, "QUIT GAME?", W / 2, H / 2 - 60, F36, Color.WHITE);
        txt(g, "Are you sure you want to leave?", W / 2, H / 2 - 25, F11, CTRL_TEXT);

        String[] labels = { "YES, QUIT", "NO, STAY" };
        int bw = 180, bh = 44, gap = 30;
        int totalW = bw * 2 + gap;
        int startX = W / 2 - totalW / 2;
        for (int i = 0; i < 2; i++) {
            REUSABLE_RECT.setBounds(startX + i * (bw + gap), H / 2 + 30, bw, bh);
            drawBtn(g, REUSABLE_RECT, labels[i], i == cur, REUSABLE_RECT.contains(mx, my));
        }
    }

    // ── MAP HELPERS ───────────────────────────────────────────────
    static Color mapCol(MapTheme m) {
        return switch (m) {
            case FOREST -> new Color(18, 55, 22);
            case CASTLE -> new Color(35, 30, 55);
            case VOLCANO -> new Color(80, 25, 10);
            case ICE -> new Color(60, 110, 160);
            case SKY_ISLANDS -> new Color(80, 120, 200);
        };
    }

    static void drawMapIcon(Graphics2D g, MapTheme m, int cx, int cy) {
        switch (m) {
            case FOREST -> {
                g.setColor(new Color(20, 70, 20));
                g.fillRect(cx - 300, cy + 70, 600, 12);
                g.setColor(new Color(15, 50, 15));
                for (int x = -240; x < 240; x += 55) {
                    g.fillRect(cx + x - 8, cy - 50, 16, 120);
                    g.fillOval(cx + x - 28, cy - 90, 60, 80);
                }
            }
            case CASTLE -> {
                g.setColor(new Color(55, 50, 75));
                g.fillRect(cx - 110, cy + 20, 80, 100);
                g.fillRect(cx + 30, cy + 20, 80, 100);
                g.fillRect(cx - 30, cy + 55, 60, 12);
                for (int bx = -110; bx <= 30; bx += 80)
                    for (int x = 0; x < 80; x += 18)
                        g.fillRect(cx + bx + x, cy + 5, 12, 18);
            }
            case VOLCANO -> {
                g.setColor(new Color(60, 18, 10));
                g.fillPolygon(new int[] { cx - 260, cx, cx + 260 }, new int[] { cy + 90, cy - 80, cy + 90 }, 3);
                g.setColor(new Color(255, 80, 0, 180));
                g.fillOval(cx - 18, cy - 100, 36, 28);
            }
            case ICE -> {
                g.setColor(new Color(180, 220, 255, 160));
                g.fillRect(cx - 260, cy + 65, 520, 12);
                g.fillRect(cx - 170, cy + 15, 340, 12);
                g.fillRect(cx - 90, cy - 25, 180, 12);

            }
            case SKY_ISLANDS -> {
                g.setColor(new Color(60, 160, 50));
                int[] iy = { cy + 55, cy + 15, cy - 20, cy + 35, cy - 8 };
                int[] ix = { cx - 210, cx - 70, cx + 70, cx + 180, cx - 140 };
                for (int i = 0; i < 5; i++)
                    g.fillRoundRect(ix[i], iy[i], 75, 12, 8, 8);
            }
        }
    }

    static void drawBadge(Graphics2D g, String s, int x, int y, Color c) {
        int bw = g.getFontMetrics(F11).stringWidth(s) + 12;
        g.setColor(c);
        g.fillRoundRect(x, y, bw, 16, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(F9);
        g.drawString(s, x + 6, y + 11);
    }

    static void drawArcher(Graphics2D g, int ax, int ay, boolean fr, Color c) {
        g.setColor(c);
        g.fillRect(ax + 10, ay - 38, 18, 26);
        g.fillOval(ax + 9, ay - 58, 20, 20);
        g.fillRect(ax + 7, ay - 12, 10, 16);
        g.fillRect(ax + 19, ay - 12, 10, 16);
        g.setStroke(STROKE3);
        if (fr)
            g.drawArc(ax + 25, ay - 46, 16, 32, -90, 180);
        else
            g.drawArc(ax - 3, ay - 46, 16, 32, -90, 180);
        g.setStroke(STROKE1);
    }
}
