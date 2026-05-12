import java.awt.Rectangle;

/**
 * ArenaData — platform layouts, moving platforms, shrink logic.
 *
 * Physics (Player.java): JUMP_POW=-13f GRAV=0.52f BASE_SPD=4.2f
 * max jump height ~162px => safe vertical gap <= 130px
 * air time ~50 ticks
 * max horiz reach ~210px => safe horizontal gap <= 160px
 *
 * Every platform reachable step-by-step from spawn.
 * All layouts symmetric around W/2 = 700.
 */
public class ArenaData {

    public static final int W = 1400;
    public static final int H = 800;
    public static final int GY = H - 90; // GROUND_Y = 710

    // ── FOREST — 4 tiers, open ground ─────────────────────────────
    // T1 y=590 T2 y=470 T3 y=350 T4 y=230 Apex y=110 (vert gap=120 each)
    // Horiz: each tier has L, C, R platforms that overlap or gap<=160px.
    static final int[][] L_FOREST = {
            { 0, GY, W, 90 }, // [0] ground
            { 50, GY - 120, 220, 20 }, // [1] L-T1 x=50..270
            { W - 270, GY - 120, 220, 20 }, // [2] R-T1 x=1130..1350
            { 150, GY - 240, 200, 20 }, // [3] L-T2 x=150..350 overlap [1] vert=120
            { W - 350, GY - 240, 200, 20 }, // [4] R-T2 x=1050..1250 overlap [2]
            { W / 2 - 210, GY - 240, 420, 20 }, // [5] C-T2 x=490..910 gap from[3]=140 gap from[4]=140
            { 60, GY - 360, 260, 20 }, // [6] L-T3 x=60..320 overlap [3] vert=120
            { W - 320, GY - 360, 260, 20 }, // [7] R-T3 x=1080..1340 overlap [4]
            { W / 2 - 230, GY - 360, 460, 22 }, // [8] C-T3 x=470..930 gap from[6]=150 gap from[7]=150
            { 310, GY - 480, 180, 20 }, // [9] L-T4 x=310..490 overlap [8] gap from[6]=10
            { W - 490, GY - 480, 180, 20 }, // [10] R-T4 x=910..1090 overlap [8] gap from[7]=10
            { W / 2 - 110, GY - 600, 220, 20 }, // [11] Apex x=590..810 gap from[9]=100 gap from[10]=100 vert=120
    };

    // ── CASTLE — twin towers + 3-segment chained bridges ──────────
    // Bridges chain: tower-L → seg-L → seg-C → seg-R → tower-R
    // so player can cross the full map at each height.
    // Tower gaps = 130px. Bridge height is midway between tower levels.
    static final int[][] L_CASTLE = {
            { 0, GY, W, 90 }, // [0] ground
            // Left tower x=10..190
            { 120, GY - 130, 180, 22 }, // [1] L1 y=580

            // Right tower x=1210..1390
            { W - 270, GY - 130, 180, 22 }, // [5] R1

            // Low bridge y=515 (65px below L1, 65px above L2 — both reachable)

            { 425, GY - 195, 560, 18 }, // [10] blo-C x=445..955 gap from[9]=5

            // Mid bridge y=385 (65px below L2, 65px above L3)
            { 190, GY - 325, 250, 18 }, // [12] bmd-L

            { 960, GY - 325, 240, 18 }, // [14] bmd-R
            // High centre y=255 vert from bmd(y=385) = 130
            { W / 2 - 200, GY - 355, 400, 22 }, // [15] hi-ctr x=500..900 gap from[12]=60 gap from[14]=60
    };

    // ── VOLCANO — stepped rocks + vertical elevator ────────────────
    // Elevator [5] travels y=460..230. When at y=460 it sits at T2
    // height, reachable horizontally from [3]/[4] (gap=110).
    // When at y=350 it's at T3 height, adjacent to [6]/[7] (gap=110).
    static final int[][] L_VOLCANO = {
            { 0, GY, W, 90 }, // [0] lava ground
            { 80, GY - 130, 200, 22 }, // [1] L-low x=80..280 y=580
            { W - 280, GY - 130, 200, 22 }, // [2] R-low x=1120..1320
            { 300, GY - 250, 200, 20 }, // [3] L-mid x=300..500 y=460 gap from[1]=20 vert=120
            { W - 500, GY - 250, 200, 20 }, // [4] R-mid x=900..1100 symmetric
            { W / 2 - 90, GY - 250, 180, 22 }, // [5] MOVING x=610..790 gap from[3]right=110 from[4]left=110
            { 280, GY - 370, 200, 20 }, // [6] L-high x=280..480 y=340 overlap[3] vert=120
            { W - 480, GY - 370, 200, 20 }, // [7] R-high x=920..1120 overlap[4]
            { W / 2 - 200, GY - 490, 400, 22 }, // [8] upper x=500..900 y=220 gap from[6]=20 gap from[7]=20 vert=120

    };

    // ── ICE —
    // wide sliding platforms ──────────────────────────────
    // Wider platforms mean players slide further — all gaps tighter.
    static final int[][] L_ICE = {
            { 0, GY, W, 90 }, // [0] ground
            { 20, GY - 120, 300, 22 }, // [1] L-low x=20..320 y=590
            { W - 320, GY - 120, 300, 22 }, // [2] R-low x=1080..1380
            { W / 2 - 280, GY - 200, 560, 22 }, // [3] C-mid x=480..920 y=470 gap from[1]=160 from[2]=160
            { 60, GY - 335, 260, 20 }, // [4] L-high x=60..320 y=355 gap from[3]left=420? overlap[1] vert=115
            { W - 320, GY - 335, 260, 20 }, // [5] R-high x=1080..1340 symmetric
            { W / 2 - 190, GY - 405, 400, 20 }, // [10] apex x=590..810 y=115 gap from[7] vert=120
    };

    // ── SKY ISLANDS — floating chain, fall=death ──────────────────
    // Spawn on [0]. Step outward and upward in a clear chain.
    static final int[][] L_SKY = {
            { W / 2 - 180, GY - 60, 360, 22 }, // [0] spawn-center x=520..880 y=650
            { W / 2 - 420, GY - 170, 170, 20 }, // [1] L-near x=280..450 gap from[0]left=70 vert=110
            { W / 2 + 250, GY - 170, 170, 20 }, // [2] R-near x=950..1120 gap from[0]right=70 vert=110
            { W / 2 - 310, GY - 280, 130, 20 }, // [3] MOVING-L initial x; range covers gap from [1]
            { W / 2 + 180, GY - 280, 130, 20 }, // [4] MOVING-R symmetric
            { W / 2 - 65, GY - 360, 130, 20 }, // [5] MOVING-C x=635..765 reachable from [3]/[4] vert~80
            { 50, GY - 270, 150, 20 }, // [6] L-far x=50..200 gap from[1]left=80 same vert
            { W - 200, GY - 270, 150, 20 }, // [7] R-far x=1200..1350 symmetric
            { W / 2 - 180, GY - 430, 150, 20 }, // [8] L-upper x=520..670 gap from[5]left=70 vert=70
            { W / 2 + 30, GY - 430, 150, 20 }, // [9] R-upper x=730..880 gap from[5]right=70 vert=70
            { W / 2 - 80, GY - 540, 160, 20 }, // [10] top x=620..780 gap from[8]=50 gap from[9]=50 vert=110
            { W / 2 - 100, GY - 650, 200, 20 }, // [11] apex x=600..800 overlap[10] vert=110
    };

    // ── NIGHT — symmetric, torch-lit arena ────────────────────────
    // Every platform chains cleanly. vert gaps=120, horiz gaps<=150.
    static final int[][] L_NIGHT = {
            { 0, GY, W, 90 }, // [0] ground
            { 60, GY - 120, 230, 22 }, // [1] L-low x=60..290 y=590
            { W - 290, GY - 120, 230, 22 }, // [2] R-low x=1110..1340
            { 180, GY - 240, 220, 20 }, // [3] L-mid x=180..400 y=470 overlap[1] vert=120
            { W - 400, GY - 240, 220, 20 }, // [4] R-mid x=1000..1220 overlap[2]
            { W / 2 - 160, GY - 240, 320, 20 }, // [5] C-mid x=540..860 gap from[3]=140 from[4]=140
            { 100, GY - 360, 220, 20 }, // [6] L-high x=100..320 y=350 overlap[3] vert=120
            { W - 320, GY - 360, 220, 20 }, // [7] R-high x=1080..1300 overlap[4]
            { W / 2 - 190, GY - 360, 380, 22 }, // [8] C-high x=510..890 gap from[6]=190 -> link via [5] vert=120
            { 250, GY - 480, 190, 20 }, // [9] L-top x=250..440 y=230 gap from[8]left=70 vert=120
            { W - 440, GY - 480, 190, 20 }, // [10] R-top x=960..1150 gap from[8]right=50 vert=120
            { W / 2 - 110, GY - 600, 220, 20 }, // [11] Apex x=590..810 gap from[9]=150 from[10]=150 vert=120
    };

    // ── Moving platform descriptors ────────────────────────────────
    // { platIdx, min, max, speed*100, isHoriz(1)/isVert(0) }
    // Volcano elevator: travels vertically between y=230 (top of [8]) and y=460
    // (tier of [3][4])
    static final int[][] MOVING_VOLCANO_V = {
            { 5, GY - 490, GY - 250, 80, 0 },
    };
    // Sky Islands: 3 horizontal movers provide dynamic routes between islands
    static final int[][] MOVING_SKY = {
            { 3, W / 2 - 480, W / 2 - 200, 110, 1 },
            { 4, W / 2 + 200, W / 2 + 480, 100, 1 },
            { 5, W / 2 - 180, W / 2 + 50, 90, 1 },
    };

    // ── Instance data ──────────────────────────────────────────────
    public final MapTheme theme;
    public final Rectangle[] platforms;
    private int shrinkOffset = 0;

    private final int[][] movDefs;
    private final float[] movPos;
    private final float[] movVel;

    public ArenaData(MapTheme theme) {
        this.theme = theme;
        int[][] layout = switch (theme) {
            case FOREST -> L_FOREST;
            case CASTLE -> L_CASTLE;
            case VOLCANO -> L_VOLCANO;
            case ICE -> L_ICE;
            case SKY_ISLANDS -> L_SKY;
            case NIGHT -> L_NIGHT;
        };
        platforms = new Rectangle[layout.length];
        for (int i = 0; i < layout.length; i++)
            platforms[i] = new Rectangle(layout[i][0], layout[i][1], layout[i][2], layout[i][3]);

        movDefs = switch (theme) {
            case VOLCANO -> MOVING_VOLCANO_V;
            case SKY_ISLANDS -> MOVING_SKY;
            default -> new int[0][];
        };
        movPos = new float[movDefs.length];
        movVel = new float[movDefs.length];
        for (int i = 0; i < movDefs.length; i++) {
            movPos[i] = movDefs[i][1];
            movVel[i] = movDefs[i][3] / 100f;
        }
    }

    public void tick() {
        for (int i = 0; i < movDefs.length; i++) {
            movPos[i] += movVel[i];
            float lo = movDefs[i][1], hi = movDefs[i][2];
            if (movPos[i] <= lo) {
                movPos[i] = lo;
                movVel[i] = movDefs[i][3] / 100f;
            }
            if (movPos[i] >= hi) {
                movPos[i] = hi;
                movVel[i] = -movDefs[i][3] / 100f;
            }
            int pi = movDefs[i][0];
            boolean horiz = movDefs[i][4] == 1;
            if (horiz)
                platforms[pi].x = (int) movPos[i];
            else
                platforms[pi].y = (int) movPos[i];
        }
    }

    public boolean isMovingPlatform(int idx) {
        for (int[] d : movDefs)
            if (d[0] == idx)
                return true;
        return false;
    }

    public void shrink(int level) {
        shrinkOffset = level * 60;
    }

    public int getLeftWall() {
        return shrinkOffset;
    }

    public int getRightWall() {
        return W - shrinkOffset;
    }

    public int getShrinkOffset() {
        return shrinkOffset;
    }
}
