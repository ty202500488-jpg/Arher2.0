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
    private final ParticlePool particlePool = new ParticlePool();
    private final ArrowPool arrowPool = new ArrowPool();
    private int hazardTimer = 0;

    private static final Font FPS_FONT = new Font("Monospaced", Font.BOLD, 12);

    // Night mask constants
    private static final Color NIGHT_BASE_OVERLAY = new Color(0, 0, 0, 215);

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
    private int p1Skin = 0, p2Skin = 1;
    private int p1SkinCursor = 0, p2SkinCursor = 1;
    private boolean p1Ready = false, p2Ready = false;
    private static final int PAUSE_COUNT = 4, RESULT_COUNT = 3;

    // Settings
    public static boolean bgmOn = true, sfxOn = true;

    // Match
    private int winner = 0, roundsP1 = 0, roundsP2 = 0;
    private static final int WIN_ROUNDS = 2;
    private int countdownTick = 0, gameTick = 0;
    private int flashAlpha = 0;

    // Performance monitoring
    private int fps = 0;
    private int frameCount = 0;
    private long lastFpsTime = 0;
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
    private BufferedImage nightMask;
    private BufferedImage lightSprite;
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
        try {
            lightSprite = javax.imageio.ImageIO.read(new java.io.File("assets/fx/light.png"));
        } catch (Exception e) {
            System.err.println("Could not load light sprite");
        }
        if (bgmOn)
            AudioManager.startBGM();
    }

    // ── Reset ──────────────────────────────────────────────────────
    private void reset() {
        boolean wind = selMap.hasWind || selMap == MapTheme.SKY_ISLANDS;
        Arrow.windX = wind ? (rng.nextFloat() - 0.5f) * 2.8f : 0f;
        Arrow.windY = 0;
        arena = new Arena(selMap);

        // Sky Islands: spawn players on the far edge platforms
        if (selMap == MapTheme.SKY_ISLANDS) {
            // Far left island: { 30, GY-270, 150, 20 }
            // Far right island: { W-180, GY-270, 150, 20 }
            int islandTop = ArenaData.GY - 270;
            float spawnY = islandTop - Player.H;

            p1 = new Player(0, 30 + 30, spawnY, sprites, arrowPool, p1Skin);
            p2 = new Player(1, ArenaData.W - 180 + 150 - Player.W - 30, spawnY, sprites, arrowPool, p2Skin);
        } else {
            p1 = new Player(0, 100, Arena.GROUND_Y - Player.H, sprites, arrowPool, p1Skin);
            p2 = new Player(1, GameWindow.WIDTH - 100 - Player.W, Arena.GROUND_Y - Player.H, sprites, arrowPool,
                    p2Skin);
        }

        p1.setIceMode(selMap.hasIce);
        p1.setSpeedMult(selMap.speedMultiplier);
        p2.setIceMode(selMap.hasIce);
        p2.setSpeedMult(selMap.speedMultiplier);
        hazards.clear();
        hazardTimer = 0;
        buildHazards();
        // Reset pools
        for (Particle p : particlePool.getPool())
            p.active = false;
        for (Arrow a : arrowPool.getPool())
            a.active = false;
        winner = 0;
        flashAlpha = 0;
        countdownTick = 0;
        slowMoTick = 0;
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
            case LOADING:
                loadTick++;
                if (loadTick >= LOAD_DUR)
                    state = GameState.MAIN_MENU;
                break;
            case MAP_SELECT:
                updateMapSelect();
                break;
            case PLAYING:
                updatePlaying();
                break;
            case SLOW_MO:
                updateSlowMo();
                break;
            case MAIN_MENU:
            case CONTROLS:
            case SETTINGS:
            case PAUSED:
            case OVER:
            case EXIT_CONFIRM:
            case SKIN_SELECT:
                break;
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
            particlePool.getPool().forEach(Particle::update);
            if (flashAlpha > 0)
                flashAlpha -= 12;
            return;
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
        checkHazards();
        checkFall();
        particlePool.getPool().forEach(Particle::update);
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
        particlePool.getPool().forEach(Particle::update);
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
            particlePool.obtain(cx, cy, vx, vy, 3 + rng.nextInt(6), c);
        }
    }

    // ── Paint ──────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);

        // FPS Tracking
        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = now;
        }

        if (buffer == null || buffer.getWidth() != getWidth())
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        switch (state) {
            case LOADING:
                GameRenderer.drawLoading(g, loadTick, LOAD_DUR, bgPts, mouseX, mouseY);
                break;
            case MAIN_MENU:
                GameRenderer.drawMainMenu(g, menuCursor, pulse, bgPts, mouseX, mouseY);
                break;
            case MAP_SELECT:
                GameRenderer.drawMapSelect(g, mapCursor, MapTheme.values(), pulse, mapTrans, mapTransDir,
                        mouseX, mouseY);
                break;
            case SKIN_SELECT:
                GameRenderer.drawSkinSelect(g, p1SkinCursor, p2SkinCursor, p1Ready, p2Ready, sprites, pulse, mouseX,
                        mouseY);
                break;
            case CONTROLS:
                GameRenderer.drawControls(g, pulse, mouseX, mouseY);
                break;
            case SETTINGS:
                GameRenderer.drawSettings(g, bgmOn, sfxOn, pulse, mouseX, mouseY);
                break;
            case EXIT_CONFIRM:
                GameRenderer.drawExitConfirm(g, exitCursor, mouseX, mouseY);
                break;
            default:
                drawGame(g);
                break;
        }
        // Draw FPS
        g.setFont(FPS_FONT);
        g.setColor(Color.GREEN);
        g.drawString("FPS: " + fps, 10, getHeight() - 10);

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
        hazards.forEach(h -> h.draw(g));
        p1.draw(g);
        p2.draw(g);
        GameRenderer.drawParticles(g, particlePool.getPool());
        if (flashAlpha > 0) {
            g.setColor(new Color(255, 255, 255, Math.min(flashAlpha, 180)));
            g.fillRect(-ox, -oy, GameWindow.WIDTH, GameWindow.HEIGHT);
        }
        g.translate(-ox, -oy);
        GameRenderer.drawHUD(g, p1, p2, Arrow.windX, gameTick);
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
        if (nightMask == null || nightMask.getWidth() != GameWindow.WIDTH) {
            nightMask = new BufferedImage(GameWindow.WIDTH, GameWindow.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D dg = nightMask.createGraphics();
        dg.setComposite(AlphaComposite.Src);
        dg.setColor(NIGHT_BASE_OVERLAY);
        dg.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);

        if (lightSprite != null) {
            dg.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
            for (Player pp : new Player[] { p1, p2 }) {
                if (!pp.isAlive())
                    continue;
                int r = 175;
                int cx = (int) pp.getCenterX() - r, cy = (int) pp.getCenterY() - r;
                dg.drawImage(lightSprite, cx, cy, r * 2, r * 2, null);
            }
        }
        dg.dispose();
        g.drawImage(nightMask, 0, 0, null);
    }

    // ── Key input ──────────────────────────────────────────────────
    /**
     * Returns true if the countdown is still running (players must not act yet).
     */
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
        if (state == GameState.SKIN_SELECT) {
            navSkin(e);
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
        // Always forward key-releases so held-keys don't stay stuck after countdown
        // ends
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
            case 0:
                subReturnState = state;
                state = GameState.MAP_SELECT;
                break;
            case 1:
                subReturnState = state;
                state = GameState.CONTROLS;
                break;
            case 2:
                subReturnState = state;
                state = GameState.SETTINGS;
                break;
            case 3:
                confirmExit();
                break;
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

    private void updateMapSelect() {
        if (mapTrans > 0) {
            mapTrans -= 0.1f;
            if (mapTrans < 0)
                mapTrans = 0;
        }
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
            p1SkinCursor = p1Skin;
            p2SkinCursor = p2Skin;
            p1Ready = false;
            p2Ready = false;
            state = GameState.SKIN_SELECT;
        }
        if (c == KeyEvent.VK_ESCAPE)
            state = GameState.MAIN_MENU;
    }

    private void navSkin(KeyEvent e) {
        int c = e.getKeyCode();
        int count = SpriteRenderer.getSkinCount();

        // Player 1: A/D to move, F or SPACE to toggle ready
        if (c == KeyEvent.VK_A && !p1Ready)
            p1SkinCursor = (p1SkinCursor - 1 + count) % count;
        if (c == KeyEvent.VK_D && !p1Ready)
            p1SkinCursor = (p1SkinCursor + 1) % count;
        if (c == KeyEvent.VK_F || c == KeyEvent.VK_SPACE) {
            p1Ready = !p1Ready;
            if (p1Ready)
                p1Skin = p1SkinCursor;
        }

        // Player 2: Arrows to move, Right CTRL to toggle ready
        if (c == KeyEvent.VK_LEFT && !p2Ready)
            p2SkinCursor = (p2SkinCursor - 1 + count) % count;
        if (c == KeyEvent.VK_RIGHT && !p2Ready)
            p2SkinCursor = (p2SkinCursor + 1) % count;
        if (c == KeyEvent.VK_CONTROL && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            p2Ready = !p2Ready;
            if (p2Ready)
                p2Skin = p2SkinCursor;
        }

        if (p1Ready && p2Ready) {
            startMatch();
        }
        if (c == KeyEvent.VK_ESCAPE)
            state = GameState.MAP_SELECT;
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
            case 0:
                state = prevState;
                break;
            case 1:
                startRound();
                state = GameState.PLAYING;
                break;
            case 2:
                subReturnState = state;
                state = GameState.SETTINGS;
                break;
            case 3:
                state = GameState.MAIN_MENU;
                break;
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
            case 0:
                startRound();
                break;
            case 1:
                state = GameState.MAP_SELECT;
                break;
            case 2:
                state = GameState.MAIN_MENU;
                break;
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
        } else if (state == GameState.SKIN_SELECT) {
            int hit = GameRenderer.skinHit(mouseX, mouseY);
            if (hit >= 0) {
                if (!p1Ready)
                    p1SkinCursor = hit;
                else if (!p2Ready)
                    p2SkinCursor = hit;
            }
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
            case LOADING:
                state = GameState.MAIN_MENU;
                break;
            case MAIN_MENU:
                int mi = GameRenderer.menuHit(mx, my);
                if (mi >= 0) {
                    menuCursor = mi;
                    selectMenu(mi);
                }
                break;
            case MAP_SELECT:
                int mapI = GameRenderer.mapHit(mx, my, MapTheme.values().length);
                if (mapI == 0) {
                    mapTransDir = -1;
                    mapTrans = 1;
                    mapCursor = (mapCursor - 1 + MapTheme.values().length) % MapTheme.values().length;
                } else if (mapI == 1) {
                    mapTransDir = 1;
                    mapTrans = 1;
                    mapCursor = (mapCursor + 1) % MapTheme.values().length;
                } else if (mapI == 2) {
                    selMap = MapTheme.values()[mapCursor];
                    p1SkinCursor = p1Skin;
                    p2SkinCursor = p2Skin;
                    p1Ready = false;
                    p2Ready = false;
                    state = GameState.SKIN_SELECT;
                } else if (mapI >= 10) {
                    int dot = mapI - 10;
                    if (dot != mapCursor) {
                        mapTransDir = (dot > mapCursor) ? 1 : -1;
                        mapTrans = 1;
                        mapCursor = dot;
                    }
                }
                break;
            case SKIN_SELECT:
                int sHit = GameRenderer.skinHit(mx, my);
                if (sHit >= 0) {
                    if (!p1Ready) {
                        p1SkinCursor = sHit;
                        p1Skin = sHit;
                        p1Ready = true;
                    } else if (!p2Ready) {
                        p2SkinCursor = sHit;
                        p2Skin = sHit;
                        p2Ready = true;
                    }
                    if (p1Ready && p2Ready)
                        startMatch();
                }
                break;
            case CONTROLS:
                int ci = GameRenderer.controlsHit(mx, my);
                if (ci == 0)
                    state = subReturnState;
                break;
            case SETTINGS:
                int si = GameRenderer.settingsHit(mx, my);
                if (si == 0) {
                    bgmOn = !bgmOn;
                    if (bgmOn)
                        AudioManager.startBGM();
                    else
                        AudioManager.stopBGM();
                } else if (si == 1)
                    sfxOn = !sfxOn;
                else if (si == 2)
                    state = subReturnState;
                break;
            case PAUSED:
                int pi = GameRenderer.pauseHit(mx, my);
                if (pi >= 0)
                    selectPause(pi);
                break;
            case OVER:
                int ri = GameRenderer.resultHit(mx, my);
                if (ri >= 0)
                    selectResult(ri);
                break;
            case EXIT_CONFIRM:
                int ei = GameRenderer.exitHit(mx, my);
                if (ei >= 0) {
                    exitCursor = ei;
                    selectExit(ei);
                }
                break;
            default:
                break;
        }
    }
}
// IDE reload trigger