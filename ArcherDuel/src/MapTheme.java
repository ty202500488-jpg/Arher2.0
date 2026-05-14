/**
 * MapTheme — defines each playable map's identity.
 * Each map has unique platforms, hazards, physics modifiers, and visuals.
 */
public enum MapTheme {
    FOREST("Forest Ruins", "Shifting wind + tree cover", 1.0f, true, false, false, false),
    CASTLE("Castle Walls", "Vertical combat + narrow ledges", 1.0f, false, false, false, false),
    VOLCANO("Volcano", "Rising lava + falling fireballs", 1.0f, false, false, true, false),
    ICE("Ice Cavern", "Slippery floors + cracking ice", 1.0f, false, true, false, false),
    SKY_ISLANDS("Sky Islands", "Strong wind + fall = death", 1.0f, true, false, false, true);

    public final String displayName;
    public final String description;
    public final float speedMultiplier; // ice slows you
    public final boolean hasWind;
    public final boolean hasIce;
    public final boolean hasLava;
    public final boolean fallDeath; // sky islands: fall off bottom = die

    MapTheme(String name, String desc, float speed,
            boolean wind, boolean ice, boolean lava, boolean fall) {
        this.displayName = name;
        this.description = desc;
        this.speedMultiplier = speed;
        this.hasWind = wind;
        this.hasIce = ice;
        this.hasLava = lava;
        this.fallDeath = fall;
    }
}
