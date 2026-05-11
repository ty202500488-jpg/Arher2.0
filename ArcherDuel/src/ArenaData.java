import java.awt.Rectangle;

/**
 * ArenaData — platform layouts, moving platforms, shrink logic.
 *
 * Physics reference (Player.java):
 *   jump power = -12f, gravity = 0.55f
 *   max jump height   = 12²/(2×0.55) ≈ 130 px  → safe vertical gap  ≤ 120 px
 *   air time          = 2×12/0.55   ≈ 43.6 ticks
 *   max horiz range   = 3.8×43.6    ≈ 165 px  → safe horizontal gap ≤ 140 px
 *
 * All layouts are symmetric around W/2 = 700 for fairness.
 * Ground maps:  P1 spawns x=100, P2 spawns x=W-142 (=1258).
 * Sky Islands:  spawned on center island by GamePanel.reset().
 */
public class ArenaData {

    public static final int W  = 1400;
    public static final int H  = 800;
    public static final int GY = H - 90;   // GROUND_Y = 710

    // ── FOREST — 3-route tiered ruins ─────────────────────────────
    // Tier gaps (vertical): 110-120 px each.  Horiz overlaps ≥ 60 px.
    // [5] centre mid-low is reachable from [3]/[4] (same height, run across)
    // [8] wide centre high: reachable from [6]/[7] (gap=85px ✓)
    static final int[][] L_FOREST = {
        { 0,        GY,       W,   90 }, // [0]  full ground
        { 50,       GY-120,   200, 20 }, // [1]  L low       (y=590)
        { W-250,    GY-120,   200, 20 }, // [2]  R low       (y=590)
        { 260,      GY-225,   170, 20 }, // [3]  L mid-low   (y=485)  gap=95 ✓
        { W-430,    GY-225,   170, 20 }, // [4]  R mid-low
        { W/2-120,  GY-225,   240, 20 }, // [5]  C mid-low   same tier — run from [3]
        { 80,       GY-335,   160, 20 }, // [6]  L mid-high  (y=375)  gap=110 ✓
        { W-240,    GY-335,   160, 20 }, // [7]  R mid-high
        { W/2-190,  GY-420,   380, 22 }, // [8]  wide centre (y=290)  gap=85 from [6][7] ✓
        { 300,      GY-520,   140, 20 }, // [9]  L upper     (y=190)  gap=100 ✓
        { W-440,    GY-520,   140, 20 }, // [10] R upper
        { W/2-100,  GY-610,   200, 20 }, // [11] apex        gap=90 ✓
        { W/2-40,   GY-68,    80,  68 }, // [12] centre ground cover block
    };

    // ── CASTLE — twin tower + bridges, vertical emphasis ──────────
    // Tower stacks at x=10 and x=W-190.  Bridges connect at mid-heights.
    // ALL tower gaps = 130px.  Bridge → high-centre gap = 115px ✓
    static final int[][] L_CASTLE = {
        { 0,        GY,       W,   90 }, // [0]  ground
        // Left tower (x=10..190)
        { 10,       GY-130,   180, 22 }, // [1]  L1   y=580
        { 10,       GY-270,   180, 22 }, // [2]  L2   y=440  gap=140 ✓
        { 10,       GY-400,   180, 22 }, // [3]  L3   y=310  gap=130 ✓
        { 10,       GY-530,   180, 22 }, // [4]  L4   y=180  gap=130 ✓
        // Right tower (x=W-190..W)
        { W-190,    GY-130,   180, 22 }, // [5]  R1
        { W-190,    GY-270,   180, 22 }, // [6]  R2
        { W-190,    GY-400,   180, 22 }, // [7]  R3
        { W-190,    GY-530,   180, 22 }, // [8]  R4
        // Low bridges at y=500 (between L1/R1 and L2/R2)
        { 195,      GY-205,   110, 18 }, // [9]  bridge-low L   x=195..305
        { W/2-55,   GY-205,   110, 18 }, // [10] bridge-low C
        { W-305,    GY-205,   110, 18 }, // [11] bridge-low R
        // Mid bridges at y=370
        { 195,      GY-340,   110, 18 }, // [12] bridge-mid L
        { W/2-55,   GY-340,   110, 18 }, // [13] bridge-mid C   y=370
        { W-305,    GY-340,   110, 18 }, // [14] bridge-mid R
        // High centre — gap from bridge-mid (y=370) = 115px ✓
        { W/2-130,  GY-455,   260, 22 }, // [15] high centre    y=255
    };

    // ── VOLCANO — rock islands over lava, vertical elevator ────────
    static final int[][] L_VOLCANO = {
        { 0,        GY,       W,   90 }, // [0]  lava ground
        { 80,       GY-130,   200, 22 }, // [1]  L low    y=580
        { W-280,    GY-130,   200, 22 }, // [2]  R low
        { 250,      GY-250,   160, 20 }, // [3]  L mid    y=460  gap=120 ✓
        { W-410,    GY-250,   160, 20 }, // [4]  R mid
        { W/2-90,   GY-340,   180, 22 }, // [5]  MOVING elevator (vertical)
        { 90,       GY-440,   160, 20 }, // [6]  L high   y=270
        { W-250,    GY-440,   160, 20 }, // [7]  R high
        { W/2-170,  GY-540,   150, 20 }, // [8]  L upper  y=170
        { W/2+20,   GY-540,   150, 20 }, // [9]  R upper
        { W/2-80,   GY-630,   160, 20 }, // [10] apex
    };

    // ── ICE — wide sliding platforms ──────────────────────────────
    // Wide platforms encourage sliding momentum, symmetric tiers.
    // Side lows widened + shifted inward so centre gap ≤ 130px ✓
    static final int[][] L_ICE = {
        { 0,        GY,       W,   90 }, // [0]  slippery ground
        { 30,       GY-125,   330, 22 }, // [1]  L wide low   right edge=360
        { W-360,    GY-125,   330, 22 }, // [2]  R wide low   left edge=1040
        { W/2-200,  GY-240,   400, 22 }, // [3]  wide centre  x=500..900, gap from [1]=140 ✓
        { 60,       GY-355,   240, 20 }, // [4]  L mid
        { W-300,    GY-355,   240, 20 }, // [5]  R mid
        { W/2-170,  GY-455,   340, 22 }, // [6]  wide upper   gap from [4][5]=100 ✓
        { 150,      GY-562,   200, 20 }, // [7]  L high
        { W-350,    GY-562,   200, 20 }, // [8]  R high
        { W/2-120,  GY-648,   240, 20 }, // [9]  top
    };

    // ── SKY ISLANDS — wide center, close side platforms, 3 moving ─
    // P1/P2 spawn on platform [0] (set in GamePanel.reset).
    // All adjacent platforms within 1-jump reach (gap ≤ 140 px).
    static final int[][] L_SKY = {
        { W/2-160,  GY-60,    320, 22 }, // [0]  wide center start (x=540..860, y=650)
        { W/2-380,  GY-145,   150, 20 }, // [1]  L near  x=320..470, horiz gap=70 ✓
        { W/2+230,  GY-145,   150, 20 }, // [2]  R near  x=930..1080, gap=70 ✓
        { W/2-260,  GY-250,   120, 20 }, // [3]  MOVING L (horiz)
        { W/2+140,  GY-250,   120, 20 }, // [4]  MOVING R (horiz)
        { W/2-60,   GY-340,   120, 20 }, // [5]  MOVING C (horiz)
        { 60,       GY-295,   130, 20 }, // [6]  L far
        { W-190,    GY-295,   130, 20 }, // [7]  R far
        { W/2-185,  GY-430,   130, 20 }, // [8]  inner L upper
        { W/2+55,   GY-430,   130, 20 }, // [9]  inner R upper
        { W/2-70,   GY-515,   140, 20 }, // [10] upper centre
        { 50,       GY-510,   110, 20 }, // [11] L high
        { W-160,    GY-510,   110, 20 }, // [12] R high
        { W/2-100,  GY-610,   200, 20 }, // [13] apex
    };

    // ── NIGHT — classic symmetric ambush arena ─────────────────────
    static final int[][] L_NIGHT = {
        { 0,        GY,       W,   90 }, // [0]  ground
        { 60,       GY-115,   220, 22 }, // [1]  L low
        { W-280,    GY-115,   220, 22 }, // [2]  R low
        { W/2-180,  GY-225,   140, 20 }, // [3]  L centre
        { W/2+40,   GY-225,   140, 20 }, // [4]  R centre
        { W/2-60,   GY-315,   120, 20 }, // [5]  centre narrow
        { 150,      GY-395,   160, 20 }, // [6]  L mid
        { W-310,    GY-395,   160, 20 }, // [7]  R mid
        { W/2-170,  GY-475,   340, 22 }, // [8]  wide upper centre
        { 50,       GY-568,   180, 20 }, // [9]  L high
        { W-230,    GY-568,   180, 20 }, // [10] R high
        { W/2-110,  GY-648,   220, 20 }, // [11] apex
    };

    // ── Moving platform descriptors ────────────────────────────────
    // { platIdx, min, max, speed*100, isHoriz(1) / isVert(0) }
    static final int[][] MOVING_VOLCANO_V = {
        { 5, GY-420, GY-250, 75, 0 }, // vertical elevator
    };
    static final int[][] MOVING_SKY = {
        { 3, 100,       W/2-140, 110, 1 },
        { 4, W/2+140,   W-270,   100, 1 },
        { 5, W/2-200,   W/2+80,   90, 1 },
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
            case FOREST      -> L_FOREST;
            case CASTLE      -> L_CASTLE;
            case VOLCANO     -> L_VOLCANO;
            case ICE         -> L_ICE;
            case SKY_ISLANDS -> L_SKY;
            case NIGHT       -> L_NIGHT;
        };
        platforms = new Rectangle[layout.length];
        for (int i = 0; i < layout.length; i++)
            platforms[i] = new Rectangle(layout[i][0], layout[i][1], layout[i][2], layout[i][3]);

        movDefs = switch (theme) {
            case VOLCANO     -> MOVING_VOLCANO_V;
            case SKY_ISLANDS -> MOVING_SKY;
            default          -> new int[0][];
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
            if (movPos[i] <= lo) { movPos[i] = lo; movVel[i] =  movDefs[i][3] / 100f; }
            if (movPos[i] >= hi) { movPos[i] = hi; movVel[i] = -movDefs[i][3] / 100f; }
            int pi = movDefs[i][0];
            boolean horiz = movDefs[i][4] == 1;
            if (horiz) platforms[pi].x = (int) movPos[i];
            else       platforms[pi].y = (int) movPos[i];
        }
    }

    public boolean isMovingPlatform(int idx) {
        for (int[] d : movDefs)
            if (d[0] == idx) return true;
        return false;
    }

    public void shrink(int level) { shrinkOffset = level * 60; }
    public int getLeftWall()      { return shrinkOffset; }
    public int getRightWall()     { return W - shrinkOffset; }
    public int getShrinkOffset()  { return shrinkOffset; }
}
