import java.awt.*;
import java.util.Random;

/**
 * Forest Arena - Optimized for 1400x800
 * Theme: Deep woods with grassy platforms, vines, and sunbeams.
 */
public class Arena {

    // Centralized dimensions (Assuming GameWindow constants are 1400 and 800)
    public static final int WIDTH = 1400;
    public static final int HEIGHT = 800;
    public static final int GROUND_Y = HEIGHT - 80;
    
    private static final int[][] PLAT_DEFS = {
        // { x, y, width, height }
        { 0, GROUND_Y, WIDTH, 80 },                             // 0: Main Forest Floor
        { WIDTH/2 - 250, GROUND_Y - 180, 500, 25 },             // 1: High Center Branch (Centered)
        { 100, GROUND_Y - 120, 250, 22 },                       // 2: Lower Left Mossy Ledge
        { WIDTH - 350, GROUND_Y - 120, 250, 22 },               // 3: Lower Right Mossy Ledge
        { WIDTH/2 - 50, GROUND_Y - 55, 100, 55 },               // 4: TREE STUMP (Centered & Solid)
        
        { 350, GROUND_Y - 280, 180, 18 },                       // 5: Upper Left Hanging Vine
        { WIDTH - 530, GROUND_Y - 280, 180, 18 },               // 6: Upper Right Hanging Vine
        { 50, GROUND_Y - 380, 200, 22 },                        // 7: High Left Corner Ledge
        { WIDTH - 250, GROUND_Y - 380, 200, 22 }                // 8: High Right Corner Ledge
    };

    private final Rectangle[] platforms;
    private final int[][] fireflies;

    public Arena() {
        platforms = new Rectangle[PLAT_DEFS.length];
        for (int i = 0; i < PLAT_DEFS.length; i++) {
            platforms[i] = new Rectangle(PLAT_DEFS[i][0], PLAT_DEFS[i][1], PLAT_DEFS[i][2], PLAT_DEFS[i][3]);
        }

        Random rng = new Random();
        fireflies = new int[60][3]; // Scaled up for 1400 width
        for (int[] f : fireflies) {
            f[0] = rng.nextInt(WIDTH);
            f[1] = rng.nextInt(GROUND_Y);
            f[2] = rng.nextInt(360);
        }
    }

    public Rectangle[] getPlatforms() { return platforms; }

    /**
     * Logic for Arrow/Projectile classes:
     * Check if the platform at index 4 (Stump) or 0 (Ground) is hit.
     */
    public boolean isRectangleSolid(Rectangle r) {
        // Platform 0 is Ground, Platform 4 is the Stump. 
        // Arrows should disappear/stick here instead of passing through.
        return r.equals(platforms[0]) || r.equals(platforms[4]);
    }

    public void draw(Graphics2D g) {
        drawForestBackground(g);
        drawPlatforms(g);
    }

    private void drawForestBackground(Graphics2D g) {
        // 1. Deep Forest Gradient
        GradientPaint forestSky = new GradientPaint(
            0, 0, new Color(10, 25, 12), 
            0, GROUND_Y, new Color(25, 45, 20)
        );
        g.setPaint(forestSky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Distant Trees (Scaled for 1400 width)
        g.setColor(new Color(15, 30, 18));
        for (int i = 0; i < WIDTH; i += 120) {
            int treeWidth = 40 + (i % 50);
            g.fillRect(i + 20, 100 + (i % 80), treeWidth, GROUND_Y);
            g.fillOval(i - 40, 40 + (i % 80), treeWidth + 100, 130);
        }

        // 3. Sunbeams (Centered & Spread)
        g.setColor(new Color(255, 255, 200, 10));
        int[] beamX = {200, WIDTH/2, WIDTH - 200};
        for (int bx : beamX) {
            g.fillPolygon(
                new int[]{bx - 50, bx + 150, bx + 50, bx - 150}, 
                new int[]{0, 0, GROUND_Y, GROUND_Y}, 4
            );
        }

        // 4. Fireflies
        for (int[] f : fireflies) {
            float alpha = (float)(Math.sin(System.currentTimeMillis() / 600.0 + f[2]) * 0.5 + 0.5);
            g.setColor(new Color(210, 255, 100, (int)(alpha * 200)));
            g.fillOval(f[0], f[1], 4, 4);
        }
    }

    private void drawPlatforms(Graphics2D g) {
        // Ground
        drawNatureBlock(g, platforms[0], true);

        // All other platforms
        for (int i = 1; i < platforms.length; i++) {
            if (i == 4) { 
                drawStump(g, platforms[i]);
            } else {
                drawNatureBlock(g, platforms[i], false);
            }
        }
    }

    private void drawStump(Graphics2D g, Rectangle stump) {
        // Stump Body
        g.setColor(new Color(55, 35, 20)); 
        g.fillRect(stump.x, stump.y, stump.width, stump.height);
        
        // Wood Grain Texture
        g.setColor(new Color(75, 50, 30));
        for(int ix = stump.x + 10; ix < stump.x + stump.width; ix += 20) {
            g.drawLine(ix, stump.y, ix, stump.y + stump.height);
        }

        // Top Surface (Oval to give 3D effect)
        g.setColor(new Color(90, 65, 40)); 
        g.fillOval(stump.x, stump.y - 8, stump.width, 16);
        
        // Inner Rings
        g.setColor(new Color(40, 25, 10));
        g.drawOval(stump.x + 5, stump.y - 5, stump.width - 10, 10);
        g.drawOval(stump.x + 15, stump.y - 2, stump.width - 30, 4);
    }

    private void drawNatureBlock(Graphics2D g, Rectangle r, boolean isGround) {
        // Base Earth
        g.setColor(new Color(40, 25, 15)); 
        g.fillRect(r.x, r.y, r.width, r.height);

        // Grass Top
        g.setColor(new Color(35, 100, 30));
        g.fillRect(r.x, r.y, r.width, 6);

        // Grass Tufts (More dense for large screen)
        g.setColor(new Color(60, 150, 45));
        for (int gx = r.x; gx < r.x + r.width; gx += 8) {
            g.drawLine(gx, r.y, gx + (gx % 5) - 2, r.y - 6);
        }

        // Hanging Vines
        if (!isGround && r.height < 30) {
            g.setColor(new Color(20, 60, 20));
            for (int vx = r.x + 20; vx < r.x + r.width; vx += 50) {
                int vineLen = 20 + (vx % 25);
                g.setStroke(new BasicStroke(2));
                g.drawLine(vx, r.y + 5, vx + (int)(Math.sin(vx)*5), r.y + vineLen);
                g.setStroke(new BasicStroke(1));
            }
        }

        // Border
        g.setColor(new Color(10, 5, 0));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }
}