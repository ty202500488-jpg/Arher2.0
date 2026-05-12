import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel
        implements KeyListener, ActionListener, MouseListener, MouseMotionListener {

    private static final int TICK_MS = 1000 / 60;

    private Arena arena;
    private Player p1, p2;
    private final SpriteRenderer sprites = new SpriteRenderer();
    private final List<MapHazard> hazards = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private int hazardTimer = 0, powerUpTimer = 0;
    private static final int PU_IV = 600;

    private GameState state = GameState.LOADING, prevState = GameState.PLAYING, subReturnState = GameState.MAIN_MENU;

    // Loading
    private int loadTick = 0;
    private static final int LOAD_DUR = 150;

    // Menu cursors
    private int menuCursor = -1; // 0=PLAY 1=CONTROLS 2=SETTINGS 3=EXIT
    private static final int MENU_COUNT = 4;
    private int mapCursor = 0;
    private MapTheme selMap = MapTheme.FOREST;
    private float mapTrans = 0;
    private int mapTransDir = 0;
    private int resultCursor = -1; // 0=Rematch 1=MapSelect 2=MainMenu
    private int pauseCursor = -1; // 0=Resume 1=Restart 2=Settings 3=MainMenu
    private int exitCursor = 0; // 0=No 1=Yes
    private static final int PAUSE_COUNT = 4, RESULT_COUNT = 3;

    // Settings
    public static boolean bgmOn = true, sfxOn = true;

    // Match
    private int winner = 0, roundsP1 = 0, roundsP2 = 0;
    private static final int WIN_ROUNDS = 2;
    private int countdownTick = 0, matchTick = 0, shrinkLevel = 0, gameTick = 0;
    private int flashAlpha = 0;
    private String winTxt = "";
    private int slowMoTick = 0;
    private float slowMoRatio = 0;
    private static final int SLOW_DUR = 90;
    private int shakeTick = 0, shakeAmt = 0;
    private int p1F = 0, p1H = 0, p1S = 0, p2F = 0, p2H = 0, p2S = 0;

    // Mouse
    private int mouseX = -1, mouseY = -1;

    // BG particles
    private final float[][] bgPts = new float[80][4];
    private float pulse = 0.3f;
    private boolean pulseUp = true;
    private final Random rng = new Random();
    private BufferedImage buffer;
    private final Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        for (float[] p : bgPts) {
            p[0] = rng.nextFloat() * GameWindow.WIDTH;
            p[1] = rng.nextFloat() * GameWindow.HEIGHT;
            p[2] = (rng.nextFloat() - 0.5f) * 0.8f;
            p[3] = (rng.nextFloat() - 0.5f) * 0.5f;
        }
        timer = new Timer(TICK_MS, this);
        timer.start();
        if (bgmOn)
            AudioManager.startBGM();
    }

    // ── Reset ──────────────────────────────────────────────────────
    private void reset() {
        boolean wind = selMap.hasWind || selMap == MapTheme.SKY_ISLANDS;
        Arrow.windX = wind ? (rng.nextFloat() - 0.5f) * 2.8f : 0f;
        Arrow.windY = 0;
        arena = new Arena(selMap);

        // Sky Islands has no ground — spawn players ON the center start platform
        if (selMap == MapTheme.SKY_ISLANDS) {
            // Center island: { W/2-160, GY-60, 320, 22 } → top at GY-60
            int islandTop = ArenaData.GY - 60;
            int islandLeft = ArenaData.W / 2 - 160;
            float spawnY = islandTop - Player.H;
            p1 = new Player(0, islandLeft + 20, spawnY, sprites);
            p2 = new Player(1, islandLeft + 320 - Player.W - 20, spawnY, sprites);
        } else {
            p1 = new Player(0, 100, Arena.GROUND_Y - Player.H, sprites);
            p2 = new Player(1, GameWindow.WIDTH - 100 - Player.W, Arena.GROUND_Y - Player.H, sprites);
        }

        p1.setIceMode(selMap.hasIce);
        p1.setSpeedMult(selMap.speedMultiplier);
        p2.setIceMode(selMap.hasIce);
        p2.setSpeedMult(selMap.speedMultiplier);
        hazards.clear();
        hazardTimer = 0;
        buildHazards();
        particles.clear();
        powerUps.clear();
        winner = 0;
        flashAlpha = 0;
        countdownTick = 0;
        matchTick = 0;
        shrinkLevel = 0;
        slowMoTick = 0;
        powerUpTimer = 0;
        gameTick = 0;
    }

    private void buildHazards() {
        if (selMap == MapTheme.VOLCANO) {
            hazards.add(MapHazard.makeLavaPool(200, Arena.GROUND_Y - 18, 160));
            hazards.add(MapHazard.makeLavaPool(620, Arena.GROUND_Y - 18, 200));
            hazards.add(MapHazard.makeLavaPool(1020, Arena.GROUND_Y - 18, 160));
        }
        if (selMap == MapTheme.ICE)
            for (Rectangle r : arena.getPlatforms())
                hazards.add(MapHazard.makeIcePatch(r.x + r.width / 4, r.y - 8, r.width / 2));
    }

    private void startRound() {
        reset();
        state = GameState.PLAYING;
        countdownTick = 0;
    }

    private void startMatch() {
        roundsP1 = 0;
        roundsP2 = 0;
        startRound();
    }

    // ── Loop ───────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    private void update() {
        updatePulse();
        updateBg();
        switch (state) {
            case LOADING -> {
                loadTick++;
                if (loadTick >= LOAD_DUR)
                    state = GameState.MAIN_MENU;
            }
            case MAIN_MENU, MAP_SELECT, CONTROLS, SETTINGS, PAUSED, OVER, EXIT_CONFIRM -> {
            }
            case PLAYING -> updatePlaying();
            case SLOW_MO -> updateSlowMo();
        }
    }

    private void updatePulse() {
        pulse += (pulseUp ? 0.04f : -0.04f);
        if (pulse >= 1)
            pulseUp = false;
        if (pulse <= 0.3f)
            pulseUp = true;
    }

    private void updateBg() {
        for (float[] p : bgPts) {
            p[0] += p[2];
            p[1] += p[3];
            if (p[0] < 0)
                p[0] = GameWindow.WIDTH;
            if (p[0] > GameWindow.WIDTH)
                p[0] = 0;
            if (p[1] < 0)
                p[1] = GameWindow.HEIGHT;
            if (p[1] > GameWindow.HEIGHT)
                p[1] = 0;
        }
    }

    private void updatePlaying() {
        matchTick++;
        gameTick++;
        if (shakeTick > 0)
            shakeTick--;

        // Advance countdown; players cannot act until it reaches 180 ("FIGHT!")
        if (countdownTick < 180) {
            countdownTick++;
            // During countdown: apply gravity / platform collision so players land
            // correctly, but do NOT process any input-driven logic.
            p1.updateGravityOnly(arena.getPlatforms());
            p2.updateGravityOnly(arena.getPlatforms());
            particles.forEach(Particle::update);
            particles.removeIf(pp -> pp.alpha <= 0);
            if (flashAlpha > 0)
                flashAlpha -= 12;
            return;
        }

        if (matchTick % 1200 == 0 && matchTick > 0) {
            shrinkLevel++;
            arena.shrink(shrinkLevel);
        }
        powerUpTimer++;
        if (powerUpTimer >= PU_IV) {
            powerUpTimer = 0;
            powerUps.add(new PowerUp(200 + rng.nextFloat() * (GameWindow.WIDTH - 400), Arena.GROUND_Y - 130,
                    rng.nextInt(2)));
        }
        hazardTimer++;
        int hiv = selMap == MapTheme.VOLCANO ? 110 : selMap == MapTheme.CASTLE ? 190 : 0;
        if (hiv > 0 && hazardTimer >= hiv) {
            hazardTimer = 0;
            if (selMap == MapTheme.VOLCANO)
                hazards.add(MapHazard.spawnFireball(rng));
            else
                hazards.add(MapHazard.spawnRock(rng));
        }
        arena.tick();
        p1.update(arena.getPlatforms());
        p2.update(arena.getPlatforms());
        if (p1.shakeRequest > 0) {
            shake(3);
            p1.shakeRequest = 0;
        }
        if (p2.shakeRequest > 0) {
            shake(3);
            p2.shakeRequest = 0;
        }
        hazards.forEach(MapHazard::update);
        hazards.removeIf(h -> !h.active);
        checkArrows();
        checkPU();
        checkHazards();
        checkFall();
        particles.forEach(Particle::update);
        particles.removeIf(pp -> pp.alpha <= 0);
        if (flashAlpha > 0)
            flashAlpha -= 12;
    }

    private void updateSlowMo() {
        slowMoTick++;
        slowMoRatio = 1f - (float) slowMoTick / SLOW_DUR;
        if (slowMoTick % 4 == 0) {
            p1.update(arena.getPlatforms());
            p2.update(arena.getPlatforms());
        }
        particles.forEach(Particle::update);
        particles.removeIf(pp -> pp.alpha <= 0);
        if (flashAlpha > 0)
            flashAlpha -= 4;
        if (slowMoTick >= SLOW_DUR)
            state = GameState.OVER;
    }

    // ── Collisions ─────────────────────────────────────────────────
    private void checkArrows() {
        hitCheck(p1, p2);
        hitCheck(p2, p1);
    }

    private void hitCheck(Player sh, Player tg) {
        if (!tg.isAlive() || tg.isInvincible())
            return;
        Rectangle tb = tg.getBounds(), hb = Arrow.getHeadshotBounds(tb);
        for (Arrow a : sh.arrows) {
            if (!a.active)
                continue;
            Rectangle ab = a.getBounds();
            if (ab.intersects(tb)) {
                a.active = false;
                sh.arrowsHit++;
                boolean hs = ab.intersects(hb);
                if (hs) {
                    sh.headshots++;
                    boolean d = tg.takeHit();
                    if (!d)
                        d = tg.takeHit();
                    parts(a.x, a.y, new Color(255, 220, 0), 22);
                    AudioManager.play(AudioManager.Sound.HEADSHOT);
                    shake(5);
                    if (d) {
                        kill(tg);
                        return;
                    }
                } else {
                    boolean d = tg.takeHit();
                    parts(a.x, a.y, new Color(255, 160, 60), 10);
                    if (d) {
                        kill(tg);
                        return;
                    }
                }
            }
        }
    }

    private void checkPU() {
        for (PowerUp pu : powerUps) {
            if (!pu.active)
                continue;
            Rectangle r = new Rectangle((int) pu.x - 15, (int) pu.y - 15, 30, 30);
            if (p1.isAlive() && p1.getBounds().intersects(r))
                applyPU(pu, p1);
            else if (p2.isAlive() && p2.getBounds().intersects(r))
                applyPU(pu, p2);
        }
        powerUps.removeIf(pu -> !pu.active);
    }

    private void applyPU(PowerUp pu, Player p) {
        pu.active = false;
        parts(pu.x, pu.y, pu.type == 0 ? new Color(80, 255, 80) : new Color(255, 220, 0), 12);
        if (pu.type == 0) {
            p.hp = Math.min(p.hp + 1, Player.MAX_HP);
            p.rageMode = (p.hp <= 1);
        }
    }

    private void checkHazards() {
        for (MapHazard h : hazards) {
            if (!h.active || h.type == MapHazard.Type.ICE_PATCH)
                continue;
            Rectangle hb = h.getBounds();
            if (p1.isAlive() && !p1.isInvincible() && p1.getBounds().intersects(hb))
                hitH(p1, h);
            if (p2.isAlive() && !p2.isInvincible() && p2.getBounds().intersects(hb))
                hitH(p2, h);
        }
    }

    private void hitH(Player p, MapHazard h) {
        if (h.type == MapHazard.Type.LAVA_POOL) {
            boolean d = p.takeHit();
            parts(p.getCenterX(), p.getCenterY(), new Color(255, 120, 0), 8);
            if (d)
                kill(p);
        } else {
            h.active = false;
            boolean d = p.takeHit();
            parts(h.x, h.y, new Color(255, 150, 0), 15);
            shake(3);
            if (d)
                kill(p);
        }
    }

    private void checkFall() {
        if (!selMap.fallDeath)
            return;
        if (p1.isAlive() && p1.y > GameWindow.HEIGHT)
            kill(p1);
        if (p2.isAlive() && p2.y > GameWindow.HEIGHT)
            kill(p2);
    }

    private void kill(Player p) {
        p1F = p1.arrowsFired;
        p1H = p1.arrowsHit;
        p1S = p1.headshots;
        p2F = p2.arrowsFired;
        p2H = p2.arrowsHit;
        p2S = p2.headshots;
        winner = (p.playerIndex == 0) ? 2 : 1;
        winTxt = "Player " + winner + " Wins!";
        flashAlpha = 255;
        if (winner == 1)
            roundsP1++;
        else
            roundsP2++;
        parts(p.getCenterX(), p.getCenterY(), p.playerIndex == 0 ? new Color(80, 130, 255) : new Color(255, 80, 80),
                45);
        AudioManager.play(AudioManager.Sound.WIN);
        shake(5);
        slowMoTick = 0;
        slowMoRatio = 1;
        state = GameState.SLOW_MO;
    }

    private void shake(int a) {
        shakeAmt = Math.min(a, 5);
        shakeTick = Math.min(a, 5);
    }

    private void parts(float cx, float cy, Color c, int n) {
        for (int i = 0; i < n; i++) {
            float vx = (rng.nextFloat() - 0.5f) * 9, vy = (rng.nextFloat() - 0.5f) * 9 - 2;
            particles.add(new Particle(cx, cy, vx, vy, 3 + rng.nextInt(6), c));
        }
    }

    // ── Paint ──────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        if (buffer == null || buffer.getWidth() != getWidth())
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        switch (state) {
            case LOADING -> GameRenderer.drawLoading(g, loadTick, LOAD_DUR, bgPts, mouseX, mouseY);
            case MAIN_MENU -> GameRenderer.drawMainMenu(g, menuCursor, pulse, bgPts, mouseX, mouseY);
            case MAP_SELECT -> GameRenderer.drawMapSelect(g, mapCursor, MapTheme.values(), pulse, mapTrans, mapTransDir,
                    mouseX, mouseY);
            case CONTROLS -> GameRenderer.drawControls(g, pulse, mouseX, mouseY);
            case SETTINGS -> GameRenderer.drawSettings(g, bgmOn, sfxOn, pulse, mouseX, mouseY);
            case EXIT_CONFIRM -> GameRenderer.drawExitConfirm(g, exitCursor, mouseX, mouseY);
            default -> drawGame(g);
        }
        g.dispose();
        gRaw.drawImage(buffer, 0, 0, null);
    }

    private void drawGame(Graphics2D g) {
        int ox = 0, oy = 0;
        if (shakeTick > 0) {
            ox = rng.nextInt(Math.max(1, shakeAmt * 2)) - shakeAmt;
            oy = rng.nextInt(Math.max(1, shakeAmt * 2)) - shakeAmt;
        }
        g.translate(ox, oy);
        arena.draw(g);
        if (selMap == MapTheme.NIGHT)
            drawNight(g);
        for (PowerUp pu : powerUps)
            drawPU(g, pu);
        hazards.forEach(h -> h.draw(g));
        p1.draw(g);
        p2.draw(g);
        GameRenderer.drawParticles(g, particles);
        if (flashAlpha > 0) {
            g.setColor(new Color(255, 255, 255, Math.min(flashAlpha, 180)));
            g.fillRect(-ox, -oy, GameWindow.WIDTH, GameWindow.HEIGHT);
        }
        g.translate(-ox, -oy);
        GameRenderer.drawHUD(g, p1, p2, Arrow.windX, gameTick, roundsP1, roundsP2, WIN_ROUNDS);
        if (countdownTick < 180)
            GameRenderer.drawCountdown(g, countdownTick);
        if (state == GameState.SLOW_MO)
            GameRenderer.drawSlowMo(g, slowMoRatio);
        if (state == GameState.OVER)
            GameRenderer.drawResults(g, winner, winTxt, roundsP1, roundsP2, WIN_ROUNDS, p1F, p1H, p1S, p2F, p2H, p2S,
                    resultCursor, mouseX, mouseY);
        if (state == GameState.PAUSED)
            GameRenderer.drawPause(g, pauseCursor, mouseX, mouseY);
    }

    private void drawNight(Graphics2D g) {
        BufferedImage dk = new BufferedImage(GameWindow.WIDTH, GameWindow.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dg = dk.createGraphics();
        dg.setColor(new Color(0, 0, 0, 215));
        dg.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        for (Player pp : new Player[] { p1, p2 }) {
            if (!pp.isAlive())
                continue;
            int cx = (int) pp.getCenterX(), cy = (int) pp.getCenterY(), r = 175;
            RadialGradientPaint rp = new RadialGradientPaint(new Point(cx, cy), r, new float[] { 0f, 0.55f, 1f },
                    new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 130), new Color(0, 0, 0, 215) });
            dg.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
            dg.setPaint(rp);
            dg.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        dg.dispose();
        g.drawImage(dk, 0, 0, null);
    }

    private void drawPU(Graphics2D g, PowerUp pu) {
        if (!pu.active)
            return;
        long t = System.currentTimeMillis();
        float pp = (float) (Math.sin(t / 300.0) * 0.4 + 0.6);
        Color c = pu.type == 0 ? new Color(80, 255, 80) : new Color(255, 220, 0);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (pp * 80)));
        g.fillOval((int) pu.x - 20, (int) pu.y - 20, 40, 40);
        g.setColor(c);
        g.fillOval((int) pu.x - 10, (int) pu.y - 10, 20, 20);
        g.setColor(Color.BLACK);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
        g.drawString(pu.type == 0 ? "+" : "»", (int) pu.x - 5, (int) pu.y + 5);
    }

    // ── Key input ──────────────────────────────────────────────────
    /** Returns true if the countdown is still running (players must not act yet). */
    private boolean isCountdownActive() {
        return (state == GameState.PLAYING || state == GameState.SLOW_MO) && countdownTick < 180;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();
        // Right CTRL → P2 shoot (blocked during countdown)
        if (c == KeyEvent.VK_CONTROL && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            if (p2 != null && !isCountdownActive())
                p2.setFirePressed(true);
            return;
        }
        // Right SHIFT → P2 dash (blocked during countdown)
        if (c == KeyEvent.VK_SHIFT && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            if (p2 != null && !isCountdownActive())
                p2.setDashPressed(true);
            return;
        }

        if (state == GameState.LOADING) {
            state = GameState.MAIN_MENU;
            return;
        }
        if (state == GameState.MAIN_MENU) {
            navMenu(c);
            return;
        }
        if (state == GameState.MAP_SELECT) {
            navMap(c);
            return;
        }
        if (state == GameState.CONTROLS) {
            if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_SPACE || c == KeyEvent.VK_ESCAPE)
                state = subReturnState;
            return;
        }
        if (state == GameState.SETTINGS) {
            navSettings(c);
            return;
        }
        if (state == GameState.OVER) {
            navResult(c);
            return;
        }
        if (c == KeyEvent.VK_ESCAPE) {
            if (state == GameState.PLAYING) {
                prevState = state;
                pauseCursor = -1;
                state = GameState.PAUSED;
            } else if (state == GameState.PAUSED)
                state = prevState;
            return;
        }
        if (state == GameState.PAUSED) {
            navPause(c);
            return;
        }
        if (state == GameState.EXIT_CONFIRM) {
            navExitConfirm(c);
            return;
        }
        if (!isCountdownActive()) {
            if (p1 != null)
                p1.keyPressed(c);
            if (p2 != null)
                p2.keyPressed(c);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == KeyEvent.VK_CONTROL && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            if (p2 != null)
                p2.setFirePressed(false);
            return;
        }
        if (c == KeyEvent.VK_SHIFT && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            if (p2 != null)
                p2.setDashPressed(false);
            return;
        }
        // Always forward key-releases so held-keys don't stay stuck after countdown ends
        if (p1 != null)
            p1.keyReleased(c);
        if (p2 != null)
            p2.keyReleased(c);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void navMenu(int c) {
        if (c == KeyEvent.VK_UP || c == KeyEvent.VK_W)
            menuCursor = (menuCursor - 1 + MENU_COUNT) % MENU_COUNT;
        if (c == KeyEvent.VK_DOWN || c == KeyEvent.VK_S)
            menuCursor = (menuCursor + 1) % MENU_COUNT;
        if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_SPACE)
            selectMenu(menuCursor);
    }

    private void selectMenu(int i) {
        switch (i) {
            case 0 -> {
                subReturnState = state;
                state = GameState.MAP_SELECT;
            }
            case 1 -> {
                subReturnState = state;
                state = GameState.CONTROLS;
            }
            case 2 -> {
                subReturnState = state;
                state = GameState.SETTINGS;
            }
            case 3 -> confirmExit();
        }
    }

    public void confirmExit() {
        subReturnState = state;
        exitCursor = 1; // Default to NO for safety
        state = GameState.EXIT_CONFIRM;
    }

    private void navExitConfirm(int c) {
        if (c == KeyEvent.VK_UP || c == KeyEvent.VK_W || c == KeyEvent.VK_LEFT || c == KeyEvent.VK_A)
            exitCursor = 0;
        if (c == KeyEvent.VK_DOWN || c == KeyEvent.VK_S || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_D)
            exitCursor = 1;
        if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_SPACE)
            selectExit(exitCursor);
        if (c == KeyEvent.VK_ESCAPE)
            state = prevState;
    }

    private void selectExit(int i) {
        if (i == 0)
            System.exit(0);
        else
            state = subReturnState;
    }

    private void navMap(int c) {
        MapTheme[] m = MapTheme.values();
        if (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_A) {
            mapTransDir = -1;
            mapTrans = 1;
            mapCursor = (mapCursor - 1 + m.length) % m.length;
        }
        if (c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_D) {
            mapTransDir = 1;
            mapTrans = 1;
            mapCursor = (mapCursor + 1) % m.length;
        }
        if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_SPACE) {
            selMap = m[mapCursor];
            startMatch();
        }
        if (c == KeyEvent.VK_ESCAPE)
            state = GameState.MAIN_MENU;
    }

    private void navSettings(int c) {
        if (c == KeyEvent.VK_UP) {
        }
        if (c == KeyEvent.VK_DOWN) {
        }
        if (c == KeyEvent.VK_ENTER) {
        }
        if (c == KeyEvent.VK_ESCAPE)
            state = subReturnState;
    }

    private void navPause(int c) {
        if (c == KeyEvent.VK_UP)
            pauseCursor = (pauseCursor - 1 + PAUSE_COUNT) % PAUSE_COUNT;
        if (c == KeyEvent.VK_DOWN)
            pauseCursor = (pauseCursor + 1) % PAUSE_COUNT;
        if (c == KeyEvent.VK_ENTER)
            selectPause(pauseCursor);
        if (c == KeyEvent.VK_ESCAPE)
            state = prevState;
    }

    private void selectPause(int i) {
        switch (i) {
            case 0 -> state = prevState;
            case 1 -> {
                startRound();
                state = GameState.PLAYING;
            }
            case 2 -> {
                subReturnState = state;
                state = GameState.SETTINGS;
            }
            case 3 -> state = GameState.MAIN_MENU;
        }
    }

    private void navResult(int c) {
        if (c == KeyEvent.VK_UP)
            resultCursor = (resultCursor - 1 + RESULT_COUNT) % RESULT_COUNT;
        if (c == KeyEvent.VK_DOWN)
            resultCursor = (resultCursor + 1) % RESULT_COUNT;
        if (c == KeyEvent.VK_ENTER)
            selectResult(resultCursor);
        if (c == KeyEvent.VK_ESCAPE)
            state = GameState.MAIN_MENU;
    }

    private void selectResult(int i) {
        switch (i) {
            case 0 -> startRound();
            case 1 -> state = GameState.MAP_SELECT;
            case 2 -> state = GameState.MAIN_MENU;
        }
    }

    // ── Mouse ──────────────────────────────────────────────────────
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateHoverCursors();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateHoverCursors();
    }

    private void updateHoverCursors() {
        if (state == GameState.MAIN_MENU) {
            menuCursor = GameRenderer.menuHit(mouseX, mouseY);
        } else if (state == GameState.PAUSED) {
            pauseCursor = GameRenderer.pauseHit(mouseX, mouseY);
        } else if (state == GameState.OVER) {
            resultCursor = GameRenderer.resultHit(mouseX, mouseY);
        } else if (state == GameState.EXIT_CONFIRM) {
            exitCursor = GameRenderer.exitHit(mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        handleClick(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseX = -1;
        mouseY = -1;
    }

    private void handleClick(int mx, int my) {
        switch (state) {
            case LOADING -> state = GameState.MAIN_MENU;
            case MAIN_MENU -> {
                int i = GameRenderer.menuHit(mx, my);
                if (i >= 0) {
                    menuCursor = i;
                    selectMenu(i);
                }
            }
            case MAP_SELECT -> {
                int i = GameRenderer.mapHit(mx, my, MapTheme.values().length);
                if (i == 0) {
                    mapTransDir = -1;
                    mapTrans = 1;
                    mapCursor = (mapCursor - 1 + MapTheme.values().length) % MapTheme.values().length;
                } else if (i == 1) {
                    mapTransDir = 1;
                    mapTrans = 1;
                    mapCursor = (mapCursor + 1) % MapTheme.values().length;
                } else if (i == 2) {
                    selMap = MapTheme.values()[mapCursor];
                    startMatch();
                }
            }
            case CONTROLS -> {
                int i = GameRenderer.controlsHit(mx, my);
                if (i == 0)
                    state = subReturnState;
            }
            case SETTINGS -> {
                int i = GameRenderer.settingsHit(mx, my);
                if (i == 0) {
                    bgmOn = !bgmOn;
                    if (bgmOn)
                        AudioManager.startBGM();
                    else
                        AudioManager.stopBGM();
                } else if (i == 1)
                    sfxOn = !sfxOn;
                else if (i == 2)
                    state = subReturnState;
            }
            case PAUSED -> {
                int i = GameRenderer.pauseHit(mx, my);
                if (i >= 0)
                    selectPause(i);
            }
            case OVER -> {
                int i = GameRenderer.resultHit(mx, my);
                if (i >= 0)
                    selectResult(i);
            }
            case EXIT_CONFIRM -> {
                int i = GameRenderer.exitHit(mx, my);
                if (i >= 0) {
                    exitCursor = i;
                    selectExit(i);
                }
            }
        }
    }
}