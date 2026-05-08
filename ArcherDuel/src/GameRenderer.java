import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * GameRenderer v2 — all static draw helpers for GamePanel.
 * Added: drawMapSelect()
 */
public class GameRenderer {

    // ── Generic utility ───────────────────────────────────────────

    static void drawDropShadow(Graphics2D g, String text, int cx, int cy,
                                Font font, Color col, int shadow) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(text) / 2;
        int ty = cy + fm.getAscent() / 2;
        g.setColor(new Color(0,0,0, Math.min(col.getAlpha(), 180)));
        g.drawString(text, tx+shadow, ty+shadow);
        g.setColor(col);
        g.drawString(text, tx, ty);
    }

    static void drawParticles(Graphics2D g, List<GamePanel.Particle> particles) {
        for (GamePanel.Particle p : particles) {
            int a = Math.max(0, Math.min(255, p.alpha));
            g.setColor(new Color(p.r, p.grn, p.b, a));
            g.fillRect((int)p.x, (int)p.y, p.size, p.size);
        }
    }

    // ── MAP SELECT SCREEN ─────────────────────────────────────────

    static void drawMapSelect(Graphics2D g, int cursor, MapTheme[] maps, float pulse) {
        int W = GameWindow.WIDTH, H = GameWindow.HEIGHT;

        // Background gradient — shifts colour based on hovered map
        Color bgA = mapBgColor(maps[cursor]).darker().darker();
        Color bgB = mapBgColor(maps[cursor]).darker();
        g.setPaint(new GradientPaint(0,0,bgA,0,H,bgB));
        g.fillRect(0,0,W,H);

        // Title
        drawDropShadow(g, "ARROW BRAWL", W/2, 80,
                new Font("Monospaced",Font.BOLD,70), Color.CYAN, 7);
        drawDropShadow(g, "SELECT YOUR MAP", W/2, 145,
                new Font("Monospaced",Font.BOLD,24), new Color(180,220,255), 3);

        // Map cards
        int cardW=180, cardH=120, gap=20;
        int total = maps.length;
        int rowW = total*(cardW+gap)-gap;
        int startX = W/2 - rowW/2;
        int cardY  = H/2 - cardH/2 - 30;

        for (int i=0; i<total; i++) {
            MapTheme m = maps[i];
            int cx = startX + i*(cardW+gap);
            boolean sel = (i==cursor);

            // Card shadow
            g.setColor(new Color(0,0,0,100));
            g.fillRoundRect(cx+4, cardY+4, cardW, cardH, 14, 14);

            // Card body
            Color cardCol = mapBgColor(m);
            if (sel) {
                // Pulse highlight border
                float p2 = (float)(Math.sin(System.currentTimeMillis()/200.0)*0.5+0.5);
                g.setColor(cardCol.brighter());
                g.fillRoundRect(cx-3, cardY-3, cardW+6, cardH+6, 16,16);
                g.setColor(new Color(255,255,255,(int)(p2*180)));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(cx-3, cardY-3, cardW+6, cardH+6, 16,16);
                g.setStroke(new BasicStroke(1));
            }
            g.setColor(sel ? cardCol : cardCol.darker());
            g.fillRoundRect(cx, cardY, cardW, cardH, 14, 14);

            // Map icon
            drawMapIcon(g, m, cx+cardW/2, cardY+40, sel);

            // Map name
            g.setFont(new Font("Monospaced", Font.BOLD, sel?13:11));
            g.setColor(sel ? Color.WHITE : new Color(180,180,180));
            FontMetrics fm = g.getFontMetrics();
            String name = m.displayName;
            g.drawString(name, cx+cardW/2-fm.stringWidth(name)/2, cardY+cardH-24);

            // Tag badges
            drawMapBadges(g, m, cx+4, cardY+cardH-16);
        }

        // Description box for selected map
        MapTheme sel = maps[cursor];
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(W/2-320, cardY+cardH+16, 640, 70, 12,12);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.CYAN);
        drawDropShadow(g, sel.displayName, W/2, cardY+cardH+40,
                new Font("Monospaced",Font.BOLD,16), mapBgColor(sel).brighter(), 2);
        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(200,200,200));
        FontMetrics fm2 = g.getFontMetrics();
        String desc = sel.description;
        g.drawString(desc, W/2-fm2.stringWidth(desc)/2, cardY+cardH+64);

        // Controls hint
        drawDropShadow(g, "◄ ► Navigate     ENTER / SPACE = Play", W/2, H-55,
                new Font("Monospaced",Font.PLAIN,16), new Color(160,160,160), 2);

        // Controls legend
        g.setColor(new Color(120,120,120));
        g.setFont(new Font("Monospaced",Font.PLAIN,13));
        String[] lines = {
            "P1: A/D=Move  W=Jump  SHIFT=Dash  SPACE=Shoot",
            "P2: ←/→=Move  ↑=Jump  CTRL=Dash  NUM0/ENTER=Shoot"
        };
        for (int i=0; i<lines.length; i++) {
            FontMetrics lm = g.getFontMetrics();
            g.drawString(lines[i], W/2-lm.stringWidth(lines[i])/2, H-30+i*16);
        }
    }

    private static Color mapBgColor(MapTheme m) {
        return switch(m) {
            case FOREST      -> new Color(18,55,22);
            case CASTLE      -> new Color(35,30,55);
            case VOLCANO     -> new Color(80,25,10);
            case ICE         -> new Color(60,110,160);
            case SKY_ISLANDS -> new Color(80,120,200);
            case NIGHT       -> new Color(10,10,30);
        };
    }

    private static void drawMapIcon(Graphics2D g, MapTheme m, int cx, int cy, boolean big) {
        int sz = big ? 28 : 22;
        switch(m) {
            case FOREST -> {
                g.setColor(new Color(30,120,30));
                int[] tx={cx-sz/2,cx,cx+sz/2}; int[] ty={cy+sz/2,cy-sz/2,cy+sz/2};
                g.fillPolygon(tx,ty,3);
                g.setColor(new Color(120,70,30));
                g.fillRect(cx-4,cy+sz/2,8,sz/3);
            }
            case CASTLE -> {
                g.setColor(new Color(100,90,120));
                g.fillRect(cx-sz/2,cy-sz/4,sz,sz*3/4);
                g.fillRect(cx-sz/2-4,cy-sz/2,sz/3,sz/3);
                g.fillRect(cx+sz/6,cy-sz/2,sz/3,sz/3);
                g.setColor(new Color(30,25,45));
                g.fillRect(cx-5,cy,10,sz/2);
            }
            case VOLCANO -> {
                g.setColor(new Color(90,50,30));
                int[] vx={cx-sz,cx,cx+sz}; int[] vy={cy+sz/2,cy-sz/2,cy+sz/2};
                g.fillPolygon(vx,vy,3);
                g.setColor(new Color(255,100,0));
                g.fillOval(cx-8,cy-sz/2-8,16,14);
            }
            case ICE -> {
                g.setColor(new Color(180,220,255));
                g.fillOval(cx-sz/2,cy-sz/2,sz,sz);
                g.setColor(new Color(255,255,255,160));
                g.fillOval(cx-sz/4,cy-sz/2+4,sz/3,sz/3);
            }
            case SKY_ISLANDS -> {
                g.setColor(new Color(255,255,220));
                g.fillOval(cx-sz/2,cy-sz/3,sz,sz/2);
                g.setColor(new Color(80,160,220));
                g.fillOval(cx-sz/4,cy-sz/2,sz/2,sz/4);
            }
            case NIGHT -> {
                g.setColor(new Color(200,200,150));
                g.fillOval(cx-sz/2,cy-sz/2,sz,sz);
                g.setColor(new Color(10,10,30));
                g.fillOval(cx-sz/2+sz/4,cy-sz/2,sz,sz);
            }
        }
    }

    private static void drawMapBadges(Graphics2D g, MapTheme m, int bx, int by) {
        int bw=32, bh=11, gap=2;
        if(m.hasWind)  drawBadge(g,"WIND", bx,by, new Color(60,130,200)); bx+=bw+gap;
        if(m.hasIce)   drawBadge(g,"ICE",  bx,by, new Color(100,180,220)); bx+=bw+gap;
        if(m.hasLava)  drawBadge(g,"LAVA", bx,by, new Color(200,70,20)); bx+=bw+gap;
        if(m.fallDeath)drawBadge(g,"FALL", bx,by, new Color(180,40,40));
    }

    private static void drawBadge(Graphics2D g, String label, int x, int y, Color col) {
        g.setColor(col);
        g.fillRoundRect(x,y,34,11,4,4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced",Font.BOLD,8));
        g.drawString(label,x+2,y+9);
    }

    // ── HUD ───────────────────────────────────────────────────────

    static void drawHUD(Graphics2D g, Player p1, Player p2, float windX,
                         int roundsP1, int roundsP2, int roundsToWin) {
        drawPlayerHUD(g,10,10,"P1  WASD+SPACE",new Color(60,120,255),p1,roundsP1,roundsToWin);
        drawPlayerHUD(g,GameWindow.WIDTH-330,10,"P2  ARROWS+NUM0",new Color(255,70,70),p2,roundsP2,roundsToWin);
        drawWindBar(g,windX);
    }

    private static void drawPlayerHUD(Graphics2D g, int x, int y, String label,
                                       Color col, Player p, int rounds, int toWin) {
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(x,y,320,56,10,10);
        g.setColor(p.isAlive()?col:new Color(80,80,80));
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        g.drawString(label,x+8,y+16);

        // HP hearts
        for(int i=0;i<Player.MAX_HP;i++){
            boolean filled = i<p.hp;
            g.setColor(filled?new Color(220,50,50):new Color(60,30,30));
            int hx=x+8+i*22, hy=y+22;
            g.fillOval(hx,hy,9,9); g.fillOval(hx+5,hy,9,9);
            int[] px={hx,hx+7,hx+14}; int[] py={hy+6,hy+16,hy+6};
            g.fillPolygon(px,py,3);
        }

        // Round pips
        g.setFont(new Font("Monospaced",Font.BOLD,11));
        g.setColor(new Color(180,180,180));
        g.drawString("RND:",x+8,y+50);
        for(int i=0;i<toWin;i++){
            g.setColor(i<rounds?col:new Color(50,50,50));
            g.fillOval(x+50+i*16,y+40,12,12);
        }

        // Rage indicator
        if(p.rageMode&&p.isAlive()){
            long t=System.currentTimeMillis();
            float pulse=(float)(Math.sin(t/150.0)*0.5+0.5);
            g.setColor(new Color(255,80,0,(int)(pulse*200)));
            g.setFont(new Font("Monospaced",Font.BOLD,11));
            g.drawString("!! RAGE !!",x+200,y+50);
        }
    }

    private static void drawWindBar(Graphics2D g, float windX) {
        int cx=GameWindow.WIDTH/2;
        g.setColor(new Color(0,0,0,140));
        g.fillRoundRect(cx-75,8,150,28,8,8);
        String dir;
        if(windX>1f)       dir=">>> WIND >>>";
        else if(windX>0.3f)dir=">  WIND  >";
        else if(windX<-1f) dir="<<< WIND <<<";
        else if(windX<-0.3f)dir="<  WIND  <";
        else               dir="   CALM   ";
        g.setFont(new Font("Monospaced",Font.BOLD,12));
        g.setColor(new Color(160,220,255));
        FontMetrics fm=g.getFontMetrics();
        g.drawString(dir,cx-fm.stringWidth(dir)/2,28);
    }

    // ── COUNTDOWN / WIN / PAUSE / SLOWMO ─────────────────────────

    static void drawCountdown(Graphics2D g, int tick) {
        int s=3-(tick/60);
        String t=s>0?String.valueOf(s):"FIGHT!";
        drawDropShadow(g,t,GameWindow.WIDTH/2,GameWindow.HEIGHT/2-20,
                new Font("Monospaced",Font.BOLD,96),Color.WHITE,5);
    }

    static void drawWinScreen(Graphics2D g, String winnerText, int winner,
                               int rP1, int rP2, int toWin) {
        g.setColor(new Color(0,0,0,170));
        g.fillRect(0,0,GameWindow.WIDTH,GameWindow.HEIGHT);
        Color wc=(winner==1)?new Color(80,150,255):new Color(255,80,80);
        drawDropShadow(g,winnerText,GameWindow.WIDTH/2,GameWindow.HEIGHT/2-60,
                new Font("Monospaced",Font.BOLD,72),wc,6);
        drawDropShadow(g,"P1 "+rP1+" — "+rP2+" P2",GameWindow.WIDTH/2,GameWindow.HEIGHT/2+10,
                new Font("Monospaced",Font.BOLD,36),Color.WHITE,3);
        boolean over=(rP1>=toWin||rP2>=toWin);
        String sub=over?"MATCH OVER!   R=Rematch   ESC=Maps"
                       :"R=Next Round   ESC=Map Select";
        drawDropShadow(g,sub,GameWindow.WIDTH/2,GameWindow.HEIGHT/2+70,
                new Font("Monospaced",Font.PLAIN,20),new Color(200,200,200),2);
    }

    static void drawSlowMotionOverlay(Graphics2D g, float ratio) {
        g.setColor(new Color(0,0,0,(int)(ratio*50)));
        g.fillRect(0,0,GameWindow.WIDTH,GameWindow.HEIGHT);
        g.setFont(new Font("Monospaced",Font.BOLD,30));
        g.setColor(new Color(255,220,0,(int)(ratio*220)));
        String txt="★  FINAL HIT  ★";
        FontMetrics fm=g.getFontMetrics();
        g.drawString(txt,GameWindow.WIDTH/2-fm.stringWidth(txt)/2,GameWindow.HEIGHT/2-160);
    }

    static void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,GameWindow.WIDTH,GameWindow.HEIGHT);
        drawDropShadow(g,"PAUSED",GameWindow.WIDTH/2,GameWindow.HEIGHT/2-30,
                new Font("Monospaced",Font.BOLD,72),Color.WHITE,5);
        drawDropShadow(g,"P = Resume     ESC = Map Select",GameWindow.WIDTH/2,GameWindow.HEIGHT/2+50,
                new Font("Monospaced",Font.PLAIN,22),new Color(200,200,200),2);
    }
}
