import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GamePanel v3
 * New: map select screen, 6 themed maps, environmental hazards,
 *      map-specific physics, best-of-3 rounds, headshots, slow-mo,
 *      shrinking arena, power-ups, screen shake, pause menu.
 */
public class GamePanel extends JPanel implements KeyListener, ActionListener {

    private static final int TARGET_FPS = 60;
    private static final int TICK_MS    = 1000 / TARGET_FPS;

    // ── Game objects ──────────────────────────────────────────────
    private Arena  arena;
    private Player player1, player2;
    private final SpriteRenderer sprites = new SpriteRenderer();

    // ── Hazards ───────────────────────────────────────────────────
    private final List<MapHazard> hazards = new ArrayList<>();
    private int hazardTimer = 0;

    // ── Particles ─────────────────────────────────────────────────
    public static class Particle {
        public float x,y,vx,vy; public int size,r,grn,b,alpha;
        Particle(float x,float y,float vx,float vy,int size,Color c){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.size=size;
            r=c.getRed();grn=c.getGreen();b=c.getBlue();alpha=255;
        }
        void update(){x+=vx;y+=vy;vy+=0.28f;vx*=0.93f;alpha-=6;}
    }
    private final List<Particle> particles = new ArrayList<>();

    // ── Power-ups ─────────────────────────────────────────────────
    private static class PowerUp {
        float x,y; int type; boolean active=true;
        PowerUp(float x,float y,int type){this.x=x;this.y=y;this.type=type;}
    }
    private final List<PowerUp> powerUps = new ArrayList<>();
    private int powerUpTimer = 0;
    private static final int POWERUP_INTERVAL = 600;

    // ── Game state ────────────────────────────────────────────────
    enum GameState { MAP_SELECT, COUNTDOWN, PLAYING, SLOW_MO, OVER, PAUSED }
    private GameState state = GameState.MAP_SELECT;
    private GameState prevState = GameState.PLAYING;

    private MapTheme selectedMap = MapTheme.FOREST;
    private int      mapCursor   = 0;

    private int winner=0, roundsP1=0, roundsP2=0;
    private static final int ROUNDS_TO_WIN = 2; // first to 2 rounds

    private int    countdownTick = 0;
    private int    flashAlpha    = 0;
    private String winnerText    = "";
    private float  pulse=0f;
    private boolean pulseUp=true;

    // Slow motion
    private int   slowMoTick=0;
    private float slowMoRatio=0f;
    private static final int SLOW_MO_DUR=90;

    // Screen shake
    private int shakeTick=0, shakeAmt=0;

    // Match
    private int matchTick=0, shrinkLevel=0;

    // Buffer
    private BufferedImage buffer;
    private final Timer timer;
    private final Random rng = new Random();

    // ── Constructor ───────────────────────────────────────────────

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(TICK_MS, this);
        timer.start();
        AudioManager.startBGM();
    }

    // ── Entity reset ──────────────────────────────────────────────

    private void resetEntities() {
        // Set wind
        boolean windMap = selectedMap.hasWind || selectedMap == MapTheme.SKY_ISLANDS;
        Arrow.windX = windMap ? (rng.nextFloat()-0.5f)*2.8f : 0f;
        Arrow.windY = 0f;
        if (selectedMap==MapTheme.SKY_ISLANDS) Arrow.windX = (rng.nextFloat()-0.5f)*4f;

        arena = new Arena(selectedMap);
        player1 = new Player(0, 100, Arena.GROUND_Y-Player.H, sprites);
        player2 = new Player(1, GameWindow.WIDTH-100-Player.W, Arena.GROUND_Y-Player.H, sprites);

        // Apply map-specific physics
        applyMapPhysics();

        // Build static hazards
        hazards.clear(); hazardTimer=0;
        buildStaticHazards();

        particles.clear(); powerUps.clear();
        winner=0; flashAlpha=0; countdownTick=0;
        matchTick=0; shrinkLevel=0; slowMoTick=0; powerUpTimer=0;
    }

    private void applyMapPhysics() {
        boolean ice = selectedMap.hasIce;
        float spd   = selectedMap.speedMultiplier;
        player1.setIceMode(ice); player1.setSpeedMult(spd);
        player2.setIceMode(ice); player2.setSpeedMult(spd);
    }

    private void buildStaticHazards() {
        switch (selectedMap) {
            case VOLCANO -> {
                // Lava pools on the ground
                hazards.add(MapHazard.makeLavaPool(200, Arena.GROUND_Y-18, 180));
                hazards.add(MapHazard.makeLavaPool(600, Arena.GROUND_Y-18, 220));
                hazards.add(MapHazard.makeLavaPool(1000,Arena.GROUND_Y-18, 180));
            }
            case ICE -> {
                // Ice patches on platforms
                Rectangle[] plats = arena.getPlatforms();
                for (int i=1;i<plats.length;i++) {
                    Rectangle r=plats[i];
                    hazards.add(MapHazard.makeIcePatch(r.x+r.width/4, r.y-8, r.width/2));
                }
            }
            default -> {}
        }
    }

    private void startRound() {
        resetEntities();
        state = GameState.COUNTDOWN;
    }

    private void startNewMatch() {
        roundsP1=0; roundsP2=0;
        startRound();
    }

    // ── Game loop ─────────────────────────────────────────────────

    @Override public void actionPerformed(ActionEvent e){ update(); repaint(); }

    private void update() {
        switch(state) {
            case MAP_SELECT -> updateMenuPulse();
            case COUNTDOWN  -> { countdownTick++; if(countdownTick>=180) state=GameState.PLAYING; }
            case PLAYING    -> updatePlaying();
            case SLOW_MO    -> updateSlowMo();
            case OVER       -> updateOver();
            case PAUSED     -> {}
        }
    }

    private void updateMenuPulse() {
        pulse+=(pulseUp?0.05f:-0.05f);
        if(pulse>=1f)pulseUp=false; if(pulse<=0.3f)pulseUp=true;
    }

    private void updatePlaying() {
        matchTick++;
        updateShrink();
        if(shakeTick>0) shakeTick--;
        updatePowerUpSpawn();
        updateHazardSpawn();
        arena.tick(); // animate moving platforms

        Rectangle[] plats = arena.getPlatforms();
        player1.update(plats); player2.update(plats);

        if(player1.shakeRequest>0){triggerShake(3);player1.shakeRequest=0;}
        if(player2.shakeRequest>0){triggerShake(3);player2.shakeRequest=0;}

        hazards.forEach(MapHazard::update);
        hazards.removeIf(h->!h.active);

        checkArrows(); checkPowerUps(); checkHazardHits();
        checkFallDeath();

        particles.forEach(Particle::update);
        particles.removeIf(p->p.alpha<=0);
        if(flashAlpha>0) flashAlpha-=12;
    }

    private void updateSlowMo() {
        slowMoTick++;
        slowMoRatio = 1f-(float)slowMoTick/SLOW_MO_DUR;
        if(slowMoTick%4==0){
            player1.update(arena.getPlatforms());
            player2.update(arena.getPlatforms());
        }
        particles.forEach(Particle::update);
        particles.removeIf(p->p.alpha<=0);
        if(flashAlpha>0) flashAlpha-=4;
        if(slowMoTick>=SLOW_MO_DUR) state=GameState.OVER;
    }

    private void updateOver() {
        player1.update(arena.getPlatforms()); player2.update(arena.getPlatforms());
        particles.forEach(Particle::update); particles.removeIf(p->p.alpha<=0);
        if(flashAlpha>0) flashAlpha-=8;
    }

    // ── Shrink ────────────────────────────────────────────────────

    private void updateShrink() {
        if(matchTick%1200==0&&matchTick>0) { shrinkLevel++; arena.shrink(shrinkLevel); }
    }

    // ── Hazard spawning ───────────────────────────────────────────

    private void updateHazardSpawn() {
        hazardTimer++;
        int interval = switch(selectedMap) {
            case VOLCANO -> 120;
            case CASTLE  -> 200;
            default      -> 0; // no dynamic hazards
        };
        if(interval>0 && hazardTimer>=interval) {
            hazardTimer=0;
            switch(selectedMap){
                case VOLCANO -> { hazards.add(MapHazard.spawnFireball(rng)); }
                case CASTLE  -> { hazards.add(MapHazard.spawnRock(rng)); }
                default -> {}
            }
        }
    }

    // ── Hazard vs player ──────────────────────────────────────────

    private void checkHazardHits() {
        for(MapHazard h : hazards) {
            if(!h.active) continue;
            if(h.type==MapHazard.Type.ICE_PATCH) continue; // handled via physics
            Rectangle hb = h.getBounds();
            if(player1.isAlive()&&!player1.isInvincible()&&player1.getBounds().intersects(hb)) {
                onHazardHit(player1, h);
            }
            if(player2.isAlive()&&!player2.isInvincible()&&player2.getBounds().intersects(hb)) {
                onHazardHit(player2, h);
            }
        }
    }

    private void onHazardHit(Player p, MapHazard h) {
        if(h.type==MapHazard.Type.LAVA_POOL) {
            // Lava: continuous damage (only every 60 ticks effectively via iFrames)
            boolean died = p.takeHit();
            spawnParticles(p.getCenterX(),p.getCenterY(),new Color(255,120,0),8);
            if(died) onPlayerKilled(p);
        } else if(h.type==MapHazard.Type.FIREBALL||h.type==MapHazard.Type.FALLING_ROCK) {
            h.active=false;
            boolean died = p.takeHit();
            spawnParticles(h.x,h.y,new Color(255,150,0),15);
            triggerShake(3);
            if(died) onPlayerKilled(p);
        }
    }

    // ── Fall death (Sky Islands) ───────────────────────────────────

    private void checkFallDeath() {
        if(!selectedMap.fallDeath) return;
        if(player1.isAlive()&&player1.y>GameWindow.HEIGHT) onPlayerKilled(player1);
        if(player2.isAlive()&&player2.y>GameWindow.HEIGHT) onPlayerKilled(player2);
    }

    // ── Power-up spawning ─────────────────────────────────────────

    private void updatePowerUpSpawn() {
        powerUpTimer++;
        if(powerUpTimer>=POWERUP_INTERVAL){
            powerUpTimer=0;
            float px=200+rng.nextFloat()*(GameWindow.WIDTH-400);
            powerUps.add(new PowerUp(px, Arena.GROUND_Y-120, rng.nextInt(2)));
        }
    }

    private void checkPowerUps() {
        for(PowerUp pu:powerUps){
            if(!pu.active) continue;
            Rectangle r=new Rectangle((int)pu.x-15,(int)pu.y-15,30,30);
            if(player1.isAlive()&&player1.getBounds().intersects(r)) applyPU(pu,player1);
            else if(player2.isAlive()&&player2.getBounds().intersects(r)) applyPU(pu,player2);
        }
        powerUps.removeIf(pu->!pu.active);
    }

    private void applyPU(PowerUp pu, Player p) {
        pu.active=false;
        spawnParticles(pu.x,pu.y,pu.type==0?new Color(80,255,80):new Color(255,220,0),15);
        if(pu.type==0) { p.hp=Math.min(p.hp+1,Player.MAX_HP); p.rageMode=(p.hp<=1); }
        // type 1 = speed handled simply by rageMode movement for now
    }

    // ── Arrow collisions ──────────────────────────────────────────

    private void checkArrows() {
        checkHit(player1, player2);
        checkHit(player2, player1);
    }

    private void checkHit(Player shooter, Player target) {
        if(!target.isAlive()||target.isInvincible()) return;
        Rectangle tb=target.getBounds();
        Rectangle hb=Arrow.getHeadshotBounds(tb);
        for(Arrow a:shooter.arrows){
            if(!a.active) continue;
            Rectangle ab=a.getBounds();
            if(ab.intersects(tb)){
                a.active=false;
                boolean hs=ab.intersects(hb);
                if(hs){
                    boolean died=target.takeHit();
                    if(!died) died=target.takeHit();
                    spawnParticles(a.x,a.y,new Color(255,220,0),20);
                    AudioManager.play(AudioManager.Sound.HEADSHOT);
                    triggerShake(5);
                    if(died){onPlayerKilled(target);return;}
                } else {
                    boolean died=target.takeHit();
                    spawnParticles(a.x,a.y,new Color(255,160,60),10);
                    if(died){onPlayerKilled(target);return;}
                }
            }
        }
    }

    private void onPlayerKilled(Player p) {
        winner=(p.playerIndex==0)?2:1;
        winnerText="Player "+winner+" Wins!";
        flashAlpha=255;
        if(winner==1) roundsP1++; else roundsP2++;
        spawnParticles(p.getCenterX(),p.getCenterY(),
                p.playerIndex==0?new Color(80,130,255):new Color(255,80,80), 40);
        AudioManager.play(AudioManager.Sound.WIN);
        triggerShake(5);
        slowMoTick=0; slowMoRatio=1f;
        state=GameState.SLOW_MO;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void triggerShake(int a){ shakeAmt=Math.min(a,6); shakeTick=Math.min(a,6); }

    private void spawnParticles(float cx, float cy, Color col, int n) {
        for(int i=0;i<n;i++){
            float vx=(rng.nextFloat()-0.5f)*9f;
            float vy=(rng.nextFloat()-0.5f)*9f-2f;
            particles.add(new Particle(cx,cy,vx,vy,3+rng.nextInt(6),col));
        }
    }

    // ── Rendering ─────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        if(buffer==null||buffer.getWidth()!=getWidth())
            buffer=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if(state==GameState.MAP_SELECT){
            GameRenderer.drawMapSelect(g, mapCursor, MapTheme.values(), pulse);
        } else {
            int ox=0,oy=0;
            if(shakeTick>0){ ox=rng.nextInt(shakeAmt*2)-shakeAmt; oy=rng.nextInt(shakeAmt*2)-shakeAmt; }
            g.translate(ox,oy);

            // Night map: darkness overlay drawn first
            if(selectedMap==MapTheme.NIGHT) drawNightVision(g);
            else arena.draw(g);

            drawPowerUps(g);
            for(MapHazard h:hazards) h.draw(g);
            player1.draw(g); player2.draw(g);
            GameRenderer.drawParticles(g,particles);

            if(flashAlpha>0){
                g.setColor(new Color(255,255,255,Math.min(flashAlpha,180)));
                g.fillRect(-ox,-oy,GameWindow.WIDTH,GameWindow.HEIGHT);
            }
            g.translate(-ox,-oy);

            // Night vignette on top
            if(selectedMap==MapTheme.NIGHT) drawNightVignette(g);

            GameRenderer.drawHUD(g,player1,player2,Arrow.windX,roundsP1,roundsP2,ROUNDS_TO_WIN);
            drawMapName(g);

            if(state==GameState.COUNTDOWN) GameRenderer.drawCountdown(g,countdownTick);
            if(state==GameState.SLOW_MO)   GameRenderer.drawSlowMotionOverlay(g,slowMoRatio);
            if(state==GameState.OVER)       GameRenderer.drawWinScreen(g,winnerText,winner,roundsP1,roundsP2,ROUNDS_TO_WIN);
            if(state==GameState.PAUSED)     GameRenderer.drawPauseScreen(g);
        }
        g.dispose();
        gRaw.drawImage(buffer,0,0,null);
    }

    private void drawNightVision(Graphics2D g) {
        // Draw arena then cover in darkness
        arena.draw(g);
    }

    private void drawNightVignette(Graphics2D g) {
        // Cover most of screen in darkness; reveal small radius around each player
        BufferedImage dark=new BufferedImage(GameWindow.WIDTH,GameWindow.HEIGHT,BufferedImage.TYPE_INT_ARGB);
        Graphics2D dg=dark.createGraphics();
        dg.setColor(new Color(0,0,0,210));
        dg.fillRect(0,0,GameWindow.WIDTH,GameWindow.HEIGHT);
        // Torch/vision radius per player
        int[] radii={180, 180};
        Player[] players={player1,player2};
        for(int i=0;i<2;i++){
            Player p=players[i];
            if(!p.isAlive()) continue;
            int cx=(int)p.getCenterX(), cy=(int)p.getCenterY(), r=radii[i];
            RadialGradientPaint paint=new RadialGradientPaint(
                new Point(cx,cy), r,
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(0,0,0,0),new Color(0,0,0,120),new Color(0,0,0,210)}
            );
            dg.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
            dg.setPaint(paint);
            dg.fillOval(cx-r, cy-r, r*2, r*2);
        }
        dg.dispose();
        g.drawImage(dark,0,0,null);
    }

    private void drawMapName(Graphics2D g) {
        if(state!=GameState.PLAYING&&state!=GameState.SLOW_MO) return;
        if(matchTick>180) return; // only show first 3 sec
        float a=Math.min(1f,(180-matchTick)/60f);
        g.setColor(new Color(255,255,255,(int)(a*200)));
        g.setFont(new Font("Monospaced",Font.BOLD,20));
        String name=selectedMap.displayName+" — "+selectedMap.description;
        FontMetrics fm=g.getFontMetrics();
        g.drawString(name,GameWindow.WIDTH/2-fm.stringWidth(name)/2,GameWindow.HEIGHT-30);
    }

    private void drawPowerUps(Graphics2D g) {
        long t=System.currentTimeMillis();
        for(PowerUp pu:powerUps){
            if(!pu.active) continue;
            float p=(float)(Math.sin(t/300.0)*0.4+0.6);
            Color col=pu.type==0?new Color(80,255,80):new Color(255,220,0);
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),(int)(p*80)));
            g.fillOval((int)pu.x-20,(int)pu.y-20,40,40);
            g.setColor(col);
            g.fillOval((int)pu.x-10,(int)pu.y-10,20,20);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced",Font.BOLD,14));
            g.drawString(pu.type==0?"+":" »",(int)pu.x-7,(int)pu.y+5);
        }
    }

    // ── Key handling ──────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int c=e.getKeyCode();

        if(state==GameState.MAP_SELECT){
            MapTheme[] maps=MapTheme.values();
            if(c==KeyEvent.VK_LEFT||c==KeyEvent.VK_A)  mapCursor=(mapCursor-1+maps.length)%maps.length;
            if(c==KeyEvent.VK_RIGHT||c==KeyEvent.VK_D) mapCursor=(mapCursor+1)%maps.length;
            if(c==KeyEvent.VK_ENTER||c==KeyEvent.VK_SPACE){
                selectedMap=maps[mapCursor];
                startNewMatch();
            }
            return;
        }

        if(state==GameState.OVER){
            if(c==KeyEvent.VK_R)      { startRound(); return; }
            if(c==KeyEvent.VK_ESCAPE) { state=GameState.MAP_SELECT; return; }
        }

        if(c==KeyEvent.VK_P){
            if(state==GameState.PLAYING){ prevState=state; state=GameState.PAUSED; return; }
            if(state==GameState.PAUSED) { state=prevState; return; }
        }
        if(state==GameState.PAUSED&&c==KeyEvent.VK_ESCAPE){ state=GameState.MAP_SELECT; return; }

        player1.keyPressed(c);
        player2.keyPressed(c);
    }

    @Override
    public void keyReleased(KeyEvent e){
        player1.keyReleased(e.getKeyCode());
        player2.keyReleased(e.getKeyCode());
    }

    @Override public void keyTyped(KeyEvent e){}
}