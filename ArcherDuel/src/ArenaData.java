import java.awt.Rectangle;

/**
 * ArenaData — platform layouts, moving platforms, shrink logic.
 * Arena.java handles all rendering; this class owns the game-logic data.
 */
public class ArenaData {

    public static final int W  = 1400;
    public static final int H  = 800;
    public static final int GY = H - 90; // GROUND_Y

    // ── Static layouts ────────────────────────────────────────────
    // Sky/Volcano include extra entries that become moving platforms.
    // Format: {x, y, width, height}

    static final int[][] L_FOREST = {
        {0,GY,W,90}, {0,GY-110,260,24}, {W-260,GY-110,260,24},
        {320,GY-200,200,22}, {W-520,GY-200,200,22},
        {160,GY-290,180,20}, {W-340,GY-290,180,20},
        {W/2-280,GY-370,560,22}, {380,GY-460,160,20},
        {W-540,GY-460,160,20}, {60,GY-550,180,20},
        {W-240,GY-550,180,20}, {W/2-120,GY-620,240,20},
        {W/2-50,GY-68,100,68}
    };

    // Castle: two towers + bridges — vertical play
    static final int[][] L_CASTLE = {
        {0,GY,W,90},
        // Left tower stack
        {10,GY-130,180,22},{10,GY-270,180,22},{10,GY-410,180,22},{10,GY-550,180,22},
        // Right tower stack
        {W-190,GY-130,180,22},{W-190,GY-270,180,22},{W-190,GY-410,180,22},{W-190,GY-550,22,22},
        // Narrow bridges between towers
        {190,GY-200,120,18},{W/2-60,GY-200,120,18},{W-310,GY-200,120,18},
        {250,GY-380,110,18},{W/2-55,GY-380,110,18},{W-360,GY-380,110,18},
        // High center
        {W/2-140,GY-500,280,22}
    };

    // Volcano: few platforms over lava — moving elevator in center
    static final int[][] L_VOLCANO = {
        {0,GY,W,90},
        {70,GY-140,220,22},{W-290,GY-140,220,22},
        {250,GY-280,180,20},{W-430,GY-280,180,20},
        {W/2-90,GY-350,180,22}, // moving (index 5)
        {80,GY-460,180,20},{W-260,GY-460,180,20},
        {W/2-200,GY-560,160,20},{W/2+40,GY-560,160,20},
        {W/2-80,GY-650,160,20}
    };

    // Ice: wide flat platforms — long slides
    static final int[][] L_ICE = {
        {0,GY,W,90},
        {40,GY-130,320,22},{W-360,GY-130,320,22},
        {W/2-220,GY-240,440,22},
        {80,GY-360,260,20},{W-340,GY-360,260,20},
        {W/2-180,GY-460,360,22},
        {160,GY-570,220,20},{W-380,GY-570,220,20},
        {W/2-130,GY-650,260,20}
    };

    // Sky Islands: small platforms, 3 moving horizontally
    static final int[][] L_SKY = {
        {W/2-150,GY-60,300,22},   // 0 center start
        {60,GY-140,160,20},        // 1 left low
        {W-220,GY-140,160,20},     // 2 right low
        {W/2-260,GY-250,120,20},   // 3 moving (horiz)
        {W/2+140,GY-250,120,20},   // 4 moving (horiz)
        {W/2-60,GY-340,120,20},    // 5 moving (horiz)
        {80,GY-400,140,20},        // 6 left mid
        {W-220,GY-400,140,20},     // 7 right mid
        {W/2-200,GY-490,140,20},   // 8
        {W/2+60,GY-490,140,20},    // 9
        {W/2-70,GY-590,140,20},    // 10
        {30,GY-600,130,20},        // 11
        {W-160,GY-600,130,20},     // 12
        {W/2-100,GY-670,200,20}    // 13 top
    };

    // Night: moderate layout — good for ambushes
    static final int[][] L_NIGHT = {
        {0,GY,W,90},
        {50,GY-120,240,22},{W-290,GY-120,240,22},
        {W/2-200,GY-230,160,20},{W/2+40,GY-230,160,20},
        {W/2-60,GY-320,120,20},
        {140,GY-380,180,20},{W-320,GY-380,180,20},
        {W/2-180,GY-470,360,22},
        {40,GY-570,200,20},{W-240,GY-570,200,20},
        {W/2-120,GY-640,240,20}
    };

    // ── Moving platform descriptors ────────────────────────────────
    // {platIdx, min, max, speed*100, isHoriz(1=yes 0=no)}
    static final int[][] MOVING_VOLCANO = {{5, W/2-90, W/2-90, 0, 0}}; // vert: y moves
    // Actually volcano elevator moves vertically: min/max are Y values
    // {platIdx, minY, maxY, speed*100, 0=vertical}
    static final int[][] MOVING_VOLCANO_V = {{5, GY-420, GY-240, 80, 0}};
    static final int[][] MOVING_SKY      = {
        {3, 100,           W/2-140,   120, 1},
        {4, W/2+140,       W-240,     100, 1},
        {5, W/2-200,       W/2+80,    90,  1}
    };

    // ── Instance data ──────────────────────────────────────────────
    public final MapTheme   theme;
    public final Rectangle[] platforms;
    private int shrinkOffset = 0;

    // Moving platform runtime state
    private final int[][]  movDefs;   // descriptors
    private final float[]  movPos;    // current position
    private final float[]  movVel;    // current velocity

    // ── Constructor ───────────────────────────────────────────────

    public ArenaData(MapTheme theme) {
        this.theme = theme;
        int[][] layout = switch(theme) {
            case FOREST      -> L_FOREST;
            case CASTLE      -> L_CASTLE;
            case VOLCANO     -> L_VOLCANO;
            case ICE         -> L_ICE;
            case SKY_ISLANDS -> L_SKY;
            case NIGHT       -> L_NIGHT;
        };
        platforms = new Rectangle[layout.length];
        for (int i=0;i<layout.length;i++)
            platforms[i] = new Rectangle(layout[i][0],layout[i][1],layout[i][2],layout[i][3]);

        // Setup moving platforms
        movDefs = switch(theme) {
            case VOLCANO     -> MOVING_VOLCANO_V;
            case SKY_ISLANDS -> MOVING_SKY;
            default          -> new int[0][];
        };
        movPos = new float[movDefs.length];
        movVel = new float[movDefs.length];
        for (int i=0;i<movDefs.length;i++) {
            movPos[i] = movDefs[i][1]; // start at min
            movVel[i] = movDefs[i][3] / 100f;
        }
    }

    /** Call once per game tick to animate moving platforms. */
    public void tick() {
        for (int i=0;i<movDefs.length;i++) {
            movPos[i] += movVel[i];
            float lo = movDefs[i][1], hi = movDefs[i][2];
            if (movPos[i] <= lo) { movPos[i]=lo; movVel[i]= movDefs[i][3]/100f; }
            if (movPos[i] >= hi) { movPos[i]=hi; movVel[i]=-movDefs[i][3]/100f; }
            int pi = movDefs[i][0];
            boolean horiz = movDefs[i][4]==1;
            if (horiz) platforms[pi].x = (int)movPos[i];
            else       platforms[pi].y = (int)movPos[i];
        }
    }

    public boolean isMovingPlatform(int idx) {
        for (int[] d : movDefs) if (d[0]==idx) return true;
        return false;
    }

    public void  shrink(int level) { shrinkOffset = level*60; }
    public int   getLeftWall()     { return shrinkOffset; }
    public int   getRightWall()    { return ArenaData.W - shrinkOffset; }
    public int   getShrinkOffset() { return shrinkOffset; }
}
