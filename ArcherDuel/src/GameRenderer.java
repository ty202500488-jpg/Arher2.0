import java.awt.*;
import java.util.List;

public class GameRenderer {
    static final int W=GameWindow.WIDTH,H=GameWindow.HEIGHT;
    static final Color GOLD=new Color(200,160,40),DARK=new Color(12,10,22),STONE=new Color(50,45,65);
    static final Font F72=new Font("Monospaced",Font.BOLD,72),F36=new Font("Monospaced",Font.BOLD,36),F20=new Font("Monospaced",Font.BOLD,20),F14=new Font("Monospaced",Font.PLAIN,14),F11=new Font("Monospaced",Font.PLAIN,11);

    // ── Button layout helpers ──────────────────────────────────────
    static Rectangle btn(int cx,int y,int bw,int bh){return new Rectangle(cx-bw/2,y,bw,bh);}
    static Rectangle menuBtn(int i){return btn(W/2,H/2-20+i*58,260,44);}
    static Rectangle pauseBtn(int i){return btn(W/2,H/2+10+i*54,260,44);}
    static Rectangle resultBtn(int i){return btn(W/2,H/2+90+i*52,240,42);}
    static Rectangle settingsBtn(int i){return btn(W/2,H/2-40+i*64,280,44);}
    static Rectangle ctrlBtn(int i){return btn(W/2,H-110+i*54,260,44);}

    // ── Hit tests ─────────────────────────────────────────────────
    static int menuHit(int x,int y){for(int i=0;i<4;i++)if(menuBtn(i).contains(x,y))return i;return -1;}
    static int pauseHit(int x,int y){for(int i=0;i<4;i++)if(pauseBtn(i).contains(x,y))return i;return -1;}
    static int resultHit(int x,int y){for(int i=0;i<3;i++)if(resultBtn(i).contains(x,y))return i;return -1;}
    static int settingsHit(int x,int y){for(int i=0;i<3;i++)if(settingsBtn(i).contains(x,y))return i;return -1;}
    static int controlsHit(int x,int y){for(int i=0;i<2;i++)if(ctrlBtn(i).contains(x,y))return i;return -1;}
    static int mapHit(int x,int y,int n){
        if(new Rectangle(W/2-380,H/2-50,80,44).contains(x,y))return 0;
        if(new Rectangle(W/2+300,H/2-50,80,44).contains(x,y))return 1;
        if(btn(W/2,H/2+200,220,44).contains(x,y))return 2;
        return -1;
    }

    // ── Draw utilities ─────────────────────────────────────────────
    static void txt(Graphics2D g,String s,int cx,int y,Font f,Color c){
        g.setFont(f);FontMetrics m=g.getFontMetrics();int x=cx-m.stringWidth(s)/2;
        g.setColor(new Color(0,0,0,120));g.drawString(s,x+3,y+3);g.setColor(c);g.drawString(s,x,y);
    }
    static void panel(Graphics2D g,int x,int y,int w,int h){
        g.setColor(new Color(20,16,35,210));g.fillRoundRect(x,y,w,h,14,14);
        g.setColor(new Color(180,140,40,90));g.drawRoundRect(x,y,w,h,14,14);
    }
    static void drawBtn(Graphics2D g,Rectangle r,String s,boolean sel,boolean hover){
        Color bg=hover?new Color(50,40,75):sel?new Color(40,32,60):new Color(25,20,40);
        Color border=hover||sel?GOLD:new Color(80,70,100);
        Color fc=hover||sel?Color.WHITE:new Color(180,160,120);
        if(hover||sel){g.setColor(new Color(200,160,40,60));g.fillRoundRect(r.x-3,r.y-3,r.width+6,r.height+6,12,12);}
        g.setColor(bg);g.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
        g.setColor(border);g.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
        g.setFont(hover||sel?F20:new Font("Monospaced",Font.BOLD,16));
        FontMetrics fm=g.getFontMetrics();g.setColor(fc);
        g.drawString(s,r.x+r.width/2-fm.stringWidth(s)/2,r.y+r.height/2+fm.getAscent()/2-3);
    }
    static void bg(Graphics2D g,Color a,Color b){g.setPaint(new GradientPaint(0,0,a,0,H,b));g.fillRect(0,0,W,H);}
    static void bgPts(Graphics2D g,float[][]pts,int al){
        g.setColor(new Color(255,255,255,al));for(float[]p:pts)g.fillOval((int)p[0],(int)p[1],2,2);
    }
    static void drawParticles(Graphics2D g,List<Particle>ps){
        for(var p:ps){int a=Math.max(0,Math.min(255,p.alpha));g.setColor(new Color(p.r,p.grn,p.b,a));g.fillRect((int)p.x,(int)p.y,p.size,p.size);}
    }

    // ── LOADING ───────────────────────────────────────────────────
    static void drawLoading(Graphics2D g,int tick,int max,float[][]pts,int mx,int my){
        bg(g,DARK,new Color(20,14,35));bgPts(g,pts,20);
        // Archer silhouettes
        long t=System.currentTimeMillis();
        drawArcher(g,180,(int)(H/2+80+Math.sin(t/700.0)*8),true,new Color(60,120,255,90));
        drawArcher(g,W-220,(int)(H/2+80+Math.sin(t/700.0+1)*8),false,new Color(255,60,60,90));
        txt(g,"ARCHER DUEL",W/2,H/2-70,F72,Color.CYAN);
        // Bar
        int bx=W/2-220,by=H/2+20,bw=440,bh=16;
        g.setColor(new Color(30,25,50));g.fillRoundRect(bx,by,bw,bh,8,8);
        g.setColor(new Color(100,80,30,80));g.drawRoundRect(bx,by,bw,bh,8,8);
        float r=Math.min(1f,(float)tick/max);
        g.setPaint(new GradientPaint(bx,0,new Color(60,100,255),bx+bw,0,Color.CYAN));
        g.fillRoundRect(bx,by,(int)(bw*r),bh,8,8);g.setPaint(null);
        // Arrow tip
        int tip=bx+(int)(bw*r);
        g.setColor(new Color(255,255,200));g.fillPolygon(new int[]{tip,tip+12,tip},new int[]{by,by+bh/2,by+bh},3);
        txt(g,"Loading...",W/2,H/2+55,F14,new Color(160,160,160));
        String[]tips={"Headshots deal double damage!","Hold F or RCTRL to charge shot","Dash makes you invincible briefly","Sky Islands: fall off = instant death","Watch wind direction before shooting"};
        txt(g,"Tip: "+tips[(tick/30)%tips.length],W/2,H-45,F11,new Color(120,120,120));
        txt(g,"Press any key or click to skip",W/2,H-22,F11,new Color(70,70,70));
    }

    // ── MAIN MENU ─────────────────────────────────────────────────
    static void drawMainMenu(Graphics2D g,int cur,float pulse,float[][]pts,int mx,int my){
        bg(g,new Color(8,18,12),new Color(18,36,20));bgPts(g,pts,16);
        // Trees
        g.setColor(new Color(8,16,10));
        int[]tx={0,110,240,380,530,670,810,960,1110,1270};int[]tw={80,65,75,60,85,70,75,60,80,75};
        for(int i=0;i<tx.length;i++){int h2=tw[i]*7;g.fillRect(tx[i]+tw[i]/2-7,H-h2,14,h2);g.fillOval(tx[i],H-h2-40,tw[i]+50,h2/2+40);g.fillOval(tx[i]+8,H-h2-90,tw[i]+35,h2/2+50);}
        // Fog
        g.setPaint(new GradientPaint(0,H-120,new Color(15,30,15,0),0,H,new Color(8,18,12,200)));g.fillRect(0,H-120,W,120);g.setPaint(null);
        // Archers
        long t=System.currentTimeMillis();
        drawArcher(g,150,(int)(H-240+Math.sin(t/900.0)*7),true,new Color(80,140,255,130));
        drawArcher(g,W-190,(int)(H-240+Math.sin(t/900.0+1.3)*7),false,new Color(255,80,80,130));
        txt(g,"ARCHER  DUEL",W/2,H/2-160,F72,Color.CYAN);
        txt(g,"Medieval 1v1 Arena",W/2,H/2-110,F11,new Color(120,180,120));
        String[]items={"PLAY","CONTROLS","SETTINGS","EXIT"};
        for(int i=0;i<items.length;i++){Rectangle r=menuBtn(i);boolean hov=r.contains(mx,my);drawBtn(g,r,items[i],i==cur,hov);}
        txt(g,"↑↓ Navigate     ENTER / Click = Select",W/2,H-22,F11,new Color(80,80,80));
    }

    // ── MAP SELECT ────────────────────────────────────────────────
    static void drawMapSelect(Graphics2D g,int cur,MapTheme[]maps,float pulse,float trans,int dir,int mx,int my){
        MapTheme m=maps[cur];Color bgc=mapCol(m);
        bg(g,bgc.darker().darker(),bgc.darker());
        txt(g,"SELECT  MAP",W/2,55,F36,Color.CYAN);
        // Card
        int cx=W/2,py=80,pw=640,ph=370;
        panel(g,cx-pw/2,py,pw,ph);
        g.setColor(bgc);g.fillRoundRect(cx-pw/2+8,py+8,pw-16,ph-70,10,10);
        drawMapIcon(g,m,cx,py+8+(ph-70)/2);
        txt(g,m.displayName,cx,py+ph-42,F20,Color.WHITE);
        txt(g,m.description,cx,py+ph-18,F11,new Color(180,180,180));
        // Nav arrows
        Rectangle lBtn=new Rectangle(cx-380,py+ph/2-22,80,44);
        Rectangle rBtn=new Rectangle(cx+300,py+ph/2-22,80,44);
        drawBtn(g,lBtn,"◄",false,lBtn.contains(mx,my));
        drawBtn(g,rBtn,"►",false,rBtn.contains(mx,my));
        // Map dots
        for(int i=0;i<maps.length;i++){g.setColor(i==cur?Color.WHITE:new Color(70,70,70));g.fillOval(cx-maps.length*11+i*22,py+ph+16,14,14);}
        // Hazard badges
        int bx=cx-160,by2=py+ph+40;
        if(m.hasWind){drawBadge(g,"WIND",bx,by2,new Color(60,140,220));bx+=76;}
        if(m.hasIce){drawBadge(g,"ICE",bx,by2,new Color(100,190,230));bx+=60;}
        if(m.hasLava){drawBadge(g,"LAVA",bx,by2,new Color(200,70,20));bx+=66;}
        if(m.fallDeath){drawBadge(g,"FALL DEATH",bx,by2,new Color(180,40,40));}
        // Start btn
        Rectangle sb=btn(cx,py+ph+75,220,44);drawBtn(g,sb,"START  [ENTER]",true,sb.contains(mx,my));
        txt(g,"◄ ► Change Map    ESC = Back",cx,H-22,F11,new Color(80,80,80));
    }

    // ── CONTROLS ──────────────────────────────────────────────────
    static void drawControls(Graphics2D g,float pulse,int mx,int my){
        bg(g,DARK,new Color(18,14,32));
        txt(g,"CONTROLS",W/2,58,F36,Color.CYAN);
        int col1=W/2-290,col2=W/2+30,cw=260,py=80;
        panel(g,col1-10,py,cw+20,240);panel(g,col2-10,py,cw+20,240);
        g.setColor(new Color(80,140,255));g.setFont(F20);g.drawString("PLAYER  1",col1,py+28);
        g.setColor(new Color(255,80,80));g.drawString("PLAYER  2",col2,py+28);
        g.setFont(F14);g.setColor(new Color(180,180,180));
        String[]p1c={"A / D  —  Move","W / SPACE  —  Jump","F  —  Shoot  (Hold=Charge)","LEFT SHIFT  —  Dash"};
        String[]p2c={"← / →  —  Move","↑  —  Jump","RIGHT CTRL  —  Shoot","RIGHT SHIFT  —  Dash"};
        for(int i=0;i<p1c.length;i++){g.drawString(p1c[i],col1,py+58+i*38);g.drawString(p2c[i],col2,py+58+i*38);}
        // Tips panel
        panel(g,W/2-320,340,640,80);
        txt(g,"TIPS",W/2,364,F14,GOLD);
        txt(g,"Headshot = 2 damage    Dash = Invincible    Hold shoot = Charge",W/2,392,F11,new Color(170,170,170));
        // Buttons
        String[]bl={"BACK","START MATCH"};
        for(int i=0;i<2;i++){Rectangle r=ctrlBtn(i);drawBtn(g,r,bl[i],i==1,r.contains(mx,my));}
        txt(g,"ESC = Back    ENTER = Start Match",W/2,H-22,F11,new Color(80,80,80));
    }

    // ── SETTINGS ──────────────────────────────────────────────────
    static void drawSettings(Graphics2D g,boolean bgm,boolean sfx,float pulse,int mx,int my){
        bg(g,DARK,new Color(18,14,32));
        txt(g,"SETTINGS",W/2,58,F36,Color.CYAN);
        String[]sl={"MUSIC: "+(bgm?"ON":"OFF"),"SFX: "+(sfx?"ON":"OFF"),"BACK"};
        Color[]sc={bgm?new Color(80,200,80):new Color(180,80,80),sfx?new Color(80,200,80):new Color(180,80,80),new Color(160,140,100)};
        for(int i=0;i<3;i++){Rectangle r=settingsBtn(i);boolean hov=r.contains(mx,my);drawBtn(g,r,sl[i],false,hov);}
        txt(g,"Click or ENTER to toggle    ESC = Back",W/2,H-22,F11,new Color(80,80,80));
    }

    // ── HUD ───────────────────────────────────────────────────────
    static void drawHUD(Graphics2D g,Player p1,Player p2,float wx,int tick,int r1,int r2,int rw){
        drawHP(g,10,10,p1,new Color(60,120,255),true,r1,rw);
        drawHP(g,W-310,10,p2,new Color(255,70,70),false,r2,rw);
        drawTimer(g,tick);if(Math.abs(wx)>0.2f)drawWind(g,wx);
    }
    static void drawHP(Graphics2D g,int x,int y,Player p,Color c,boolean left,int rnd,int rw){
        panel(g,x,y,300,52);
        g.setColor(p.isAlive()?c:new Color(70,70,70));g.setFont(F14);
        g.drawString(left?"P1  WASD+F":"P2  ARROWS+RCTRL",x+8,y+17);
        for(int i=0;i<Player.MAX_HP;i++){boolean f=i<p.hp;g.setColor(f?new Color(220,50,50):new Color(50,25,25));int hx=x+8+i*22,hy=y+23;g.fillOval(hx,hy,9,9);g.fillOval(hx+5,hy,9,9);g.fillPolygon(new int[]{hx,hx+7,hx+14},new int[]{hy+6,hy+16,hy+6},3);}
        g.setColor(new Color(100,100,100));g.setFont(F11);g.drawString("R:",x+80,y+46);
        for(int i=0;i<rw;i++){g.setColor(i<rnd?c:new Color(40,40,40));g.fillOval(x+98+i*18,y+36,12,12);}
        if(p.rageMode&&p.isAlive()){long t=System.currentTimeMillis();float pp=(float)(Math.sin(t/150.0)*0.5+0.5);g.setColor(new Color(255,80,0,(int)(pp*200)));g.setFont(F11);g.drawString("RAGE!",x+200,y+46);}
    }
    static void drawTimer(Graphics2D g,int tick){
        int s=tick/60,m=s/60;s%=60;String t=String.format("%d:%02d",m,s);
        panel(g,W/2-52,6,104,30);txt(g,t,W/2,20,F14,new Color(200,200,200));
    }
    static void drawWind(Graphics2D g,float wx){
        String d=wx>1?">>> WIND":wx>0.3f?"> WIND":wx<-1?"WIND <<<":"WIND <";
        panel(g,W/2-65,40,130,22);g.setFont(F11);g.setColor(new Color(140,200,255));
        FontMetrics fm=g.getFontMetrics();g.drawString(d,W/2-fm.stringWidth(d)/2,56);
    }

    // ── OVERLAYS ─────────────────────────────────────────────────
    static void drawCountdown(Graphics2D g,int tick){
        if(tick>=180)return;int s=3-tick/60;String t=s>0?String.valueOf(s):"FIGHT!";
        txt(g,t,W/2,H/2,s>0?F72:F36,s>0?Color.WHITE:new Color(80,255,80));
    }
    static void drawSlowMo(Graphics2D g,float r){
        g.setColor(new Color(0,0,0,(int)(r*40)));g.fillRect(0,0,W,H);
        txt(g,"★  FINAL HIT  ★",W/2,H/2-160,F20,new Color(255,220,0,(int)(r*220)));
    }

    // ── PAUSE ─────────────────────────────────────────────────────
    static void drawPause(Graphics2D g,int cur,int mx,int my){
        g.setColor(new Color(0,0,0,170));g.fillRect(0,0,W,H);
        txt(g,"PAUSED",W/2,H/2-80,F72,Color.WHITE);
        String[]pl={"RESUME","RESTART","SETTINGS","MAIN MENU"};
        for(int i=0;i<4;i++){Rectangle r=pauseBtn(i);drawBtn(g,r,pl[i],i==cur,r.contains(mx,my));}
        txt(g,"↑↓ Navigate    ENTER = Select    ESC = Resume",W/2,H-22,F11,new Color(80,80,80));
    }

    // ── RESULTS ───────────────────────────────────────────────────
    static void drawResults(Graphics2D g,int win,String wt,int r1,int r2,int rw,int f1,int h1,int s1,int f2,int h2,int s2,int cur,int mx,int my){
        g.setColor(new Color(0,0,0,185));g.fillRect(0,0,W,H);
        Color wc=win==1?new Color(80,150,255):new Color(255,80,80);
        txt(g,wt,W/2,H/2-150,F72,wc);
        txt(g,"P1 "+r1+" — "+r2+" P2",W/2,H/2-95,F36,Color.WHITE);
        // Stats
        panel(g,W/2-340,H/2-70,680,140);
        txt(g,"MATCH STATS",W/2,H/2-44,F14,GOLD);
        g.setFont(F14);
        String[]lb={"Arrows Fired","Arrows Hit","Accuracy","Headshots"};
        int[]v1={f1,h1,f1>0?(int)(h1*100f/f1):0,s1};int[]v2={f2,h2,f2>0?(int)(h2*100f/f2):0,s2};
        for(int i=0;i<lb.length;i++){int sy=H/2-20+i*26;
            g.setColor(new Color(80,140,255));String sv1=i==2?v1[i]+"%":String.valueOf(v1[i]);g.drawString(sv1,W/2-280,sy);
            g.setColor(new Color(160,160,160));FontMetrics fm=g.getFontMetrics();g.drawString(lb[i],W/2-fm.stringWidth(lb[i])/2,sy);
            g.setColor(new Color(255,80,80));String sv2=i==2?v2[i]+"%":String.valueOf(v2[i]);g.drawString(sv2,W/2+240,sy);}
        String[]rl={"REMATCH","MAP SELECT","MAIN MENU"};
        for(int i=0;i<3;i++){Rectangle r=resultBtn(i);drawBtn(g,r,rl[i],i==cur,r.contains(mx,my));}
        txt(g,"↑↓ Navigate    ENTER = Select    ESC = Main Menu",W/2,H-22,F11,new Color(80,80,80));
    }

    // ── MAP HELPERS ───────────────────────────────────────────────
    static Color mapCol(MapTheme m){return switch(m){
        case FOREST->new Color(18,55,22);case CASTLE->new Color(35,30,55);
        case VOLCANO->new Color(80,25,10);case ICE->new Color(60,110,160);
        case SKY_ISLANDS->new Color(80,120,200);case NIGHT->new Color(10,10,30);};}

    static void drawMapIcon(Graphics2D g,MapTheme m,int cx,int cy){
        switch(m){
            case FOREST->{g.setColor(new Color(20,70,20));g.fillRect(cx-300,cy+70,600,12);g.setColor(new Color(15,50,15));for(int x=-240;x<240;x+=55){g.fillRect(cx+x-8,cy-50,16,120);g.fillOval(cx+x-28,cy-90,60,80);}}
            case CASTLE->{g.setColor(new Color(55,50,75));g.fillRect(cx-110,cy+20,80,100);g.fillRect(cx+30,cy+20,80,100);g.fillRect(cx-30,cy+55,60,12);for(int bx=-110;bx<=30;bx+=80)for(int x=0;x<80;x+=18)g.fillRect(cx+bx+x,cy+5,12,18);}
            case VOLCANO->{g.setColor(new Color(60,18,10));g.fillPolygon(new int[]{cx-260,cx,cx+260},new int[]{cy+90,cy-80,cy+90},3);g.setColor(new Color(255,80,0,180));g.fillOval(cx-18,cy-100,36,28);}
            case ICE->{g.setColor(new Color(180,220,255,160));g.fillRect(cx-260,cy+65,520,12);g.fillRect(cx-170,cy+15,340,12);g.fillRect(cx-90,cy-25,180,12);g.setColor(new Color(255,255,255,80));for(int x=-240;x<240;x+=45)g.fillPolygon(new int[]{cx+x,cx+x+11,cx+x+22},new int[]{0,cy-30,0},3);}
            case SKY_ISLANDS->{g.setColor(new Color(60,160,50));int[]iy={cy+55,cy+15,cy-20,cy+35,cy-8};int[]ix={cx-210,cx-70,cx+70,cx+180,cx-140};for(int i=0;i<5;i++)g.fillRoundRect(ix[i],iy[i],75,12,8,8);}
            case NIGHT->{g.setColor(new Color(18,18,38));g.fillRect(cx-260,cy-75,520,165);g.setColor(new Color(28,28,52));g.fillRect(cx-240,cy+25,520,12);g.fillRect(cx-150,cy-5,300,12);g.setColor(new Color(240,240,210));g.fillOval(cx+200,cy-75,30,30);g.setColor(new Color(10,10,28));g.fillOval(cx+212,cy-83,30,30);}
        }
    }

    static void drawBadge(Graphics2D g,String s,int x,int y,Color c){
        int bw=g.getFontMetrics(F11).stringWidth(s)+12;
        g.setColor(c);g.fillRoundRect(x,y,bw,16,4,4);g.setColor(Color.WHITE);g.setFont(new Font("Monospaced",Font.BOLD,9));g.drawString(s,x+6,y+11);
    }

    static void drawArcher(Graphics2D g,int ax,int ay,boolean fr,Color c){
        g.setColor(c);g.fillRect(ax+10,ay-38,18,26);g.fillOval(ax+9,ay-58,20,20);
        g.fillRect(ax+7,ay-12,10,16);g.fillRect(ax+19,ay-12,10,16);
        g.setStroke(new BasicStroke(3));
        if(fr)g.drawArc(ax+25,ay-46,16,32,-90,180);else g.drawArc(ax-3,ay-46,16,32,-90,180);
        g.setStroke(new BasicStroke(1));
    }
}
