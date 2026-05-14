import java.awt.Rectangle;

/**
 * The ArenaData class contains the structural definitions for every map.
 * This includes platform positions, movement boundaries, and world dimensions.
 * It is separated from Arena.java to decouple game physics/logic from rendering.
 *
 * Physics Reference (Player.java): JUMP_POW=-13f, GRAV=0.52f, BASE_SPD=4.2f
 * - Max jump height is ~162px (Safe vertical gap <= 130px)
 * - Max horizontal reach is ~210px (Safe horizontal gap <= 160px)
 */
public class ArenaData {

    public static final int W = 1400;     // Total world width
    public static final int H = 800;      // Total world height
    public static final int GY = H - 90; // The Y-coordinate of the ground level (710)

    // ── FOREST — 4 tiers, open ground ─────────────────────────────
    // T1 y=590 T2 y=470 T3 y=350 T4 y=230 Apex y=110 (vert gap=120 each)
    // Horiz: each tier has L, C, R platforms that overlap or gap<=160px.
    static final int[][] L_FOREST = {
            { 0, GY, W, 90 },
            { 50, GY - 120, 220, 20 },
            { W - 270, GY - 120, 220, 20 },
            { 150, GY - 240, 200, 20 },
            { W - 350, GY - 240, 200, 20 },
            { W / 2 - 210, GY - 240, 420, 20 },
            { 60, GY - 360, 260, 20 },
            { W - 320, GY - 360, 260, 20 },
            { W / 2 - 230, GY - 360, 460, 22 },
            { 310, GY - 480, 180, 20 },
            { W - 490, GY - 480, 180, 20 },
            { W / 2 - 110, GY - 600, 220, 20 },
    };

    // ── CASTLE — twin towers + 3-segment chained bridges ──────────
    // Bridges chain: tower-L → seg-L → seg-C → seg-R → tower-R
    // so player can cross the full map at each height.
    // Tower gaps = 130px. Bridge height is midway between tower levels.
    static final int[][] L_CASTLE = {
            { 0, GY, W, 90 },
            { 120, GY - 130, 180, 22 },
            { W - 270, GY - 130, 180, 22 },
            { 425, GY - 195, 560, 18 },
            { 190, GY - 325, 250, 18 },
            { 960, GY - 325, 240, 18 },
            { W / 2 - 200, GY - 355, 400, 22 },
    };

    // ── VOLCANO — stepped rocks + vertical elevator ────────────────
    // Elevator [5] travels y=460..230. When at y=460 it sits at T2
    // height, reachable horizontally from [3]/[4] (gap=110).
    // When at y=350 it's at T3 height, adjacent to [6]/[7] (gap=110).
    static final int[][] L_VOLCANO = {
            { 0, GY, W, 90 },
            { 80, GY - 130, 200, 22 },
            { W - 280, GY - 130, 200, 22 },
            { 300, GY - 250, 200, 20 },
            { W - 500, GY - 250, 200, 20 },
            { W / 2 - 90, GY - 250, 180, 22 },
            { 280, GY - 370, 200, 20 },
            { W - 480, GY - 370, 200, 20 },
            { W / 2 - 200, GY - 490, 400, 22 },
    };

    // ── ICE —
    // wide sliding platforms ──────────────────────────────
    // Wider platforms mean players slide further — all gaps tighter.
    static final int[][] L_ICE = {
            { 0, GY, W, 90 },
            { 20, GY - 120, 300, 22 },
            { W - 320, GY - 120, 300, 22 },
            { W / 2 - 280, GY - 200, 560, 22 },
            { 60, GY - 335, 260, 20 },
            { W - 320, GY - 335, 260, 20 },
            { W / 2 - 190, GY - 405, 400, 20 },
    };

    // ── SKY ISLANDS — floating chain, fall=death ──────────────────
    // Spawn on [0]. Step outward and upward in a clear chain.
    static final int[][] L_SKY = {
            { W / 2 - 180, GY - 60, 360, 22 },
            { W / 2 - 420, GY - 170, 170, 20 },
            { W / 2 + 250, GY - 170, 170, 20 },
            { W / 2 - 310, GY - 280, 130, 20 },
            { W / 2 + 180, GY - 280, 130, 20 },
            { W / 2 - 65, GY - 360, 130, 20 },
            { 30, GY - 270, 150, 20 },
            { W - 180, GY - 270, 150, 20 },
            { W / 2 - 180, GY - 430, 150, 20 },
            { W / 2 + 30, GY - 430, 150, 20 },
            { W / 2 - 80, GY - 540, 160, 20 },
            { W / 2 - 100, GY - 650, 200, 20 },
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
            { 3, W / 2 - 500, W / 2 - 250, 110, 1 },
            { 4, W / 2 + 120, W / 2 + 370, 100, 1 },
            { 5, W / 2 - 150, W / 2 + 20, 90, 1 },
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

    /**
     * Updates the positions of moving platforms based on their velocity and bounds.
     */
    public void tick() {
        for (int i = 0; i < movDefs.length; i++) {
            movPos[i] += movVel[i];
            float lo = movDefs[i][1], hi = movDefs[i][2];
            
            // Reverse direction if boundaries are hit
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
            if (horiz) platforms[pi].x = (int) movPos[i];
            else platforms[pi].y = (int) movPos[i];
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
