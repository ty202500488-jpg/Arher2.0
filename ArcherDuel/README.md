# 🏹 ArcherDuel — 2D 1v1 Local Multiplayer Archer Game

A retro pixel-art archer duel game built in **pure Java** (Swing only, zero external libraries).

---

## 📋 Requirements

- **Java JDK 11 or higher** (must have `javac` compiler)
  - Windows: https://adoptium.net/
  - Ubuntu/Debian: `sudo apt-get install default-jdk`
  - macOS: `brew install openjdk`

---

## 🚀 How to Run

### Linux / macOS
```bash
chmod +x run.sh
./run.sh
```

### Windows
Double-click `run.bat` or run from command prompt:
```
run.bat
```

### Manual (any OS)
```bash
mkdir -p bin
javac -d bin src/*.java
java -cp bin Main
```

---

## 🎮 Controls

### Player 1 (Blue Archer)
| Action        | Key       |
|---------------|-----------|
| Move Left     | A         |
| Move Right    | D         |
| Jump          | W         |
| Shoot Right   | H         |
| Shoot Left    | F         |
| Shoot Up      | T         |
| Shoot Down    | G         |
| Shoot Up-Left | R         |
| Shoot Up-Right| Y         |
| Shoot Dn-Left | V         |
| Shoot Dn-Right| B         |

### Player 2 (Red Archer)
| Action         | Key         |
|----------------|-------------|
| Move Left      | ← Arrow     |
| Move Right     | → Arrow     |
| Jump           | ↑ Arrow     |
| Shoot Right    | L           |
| Shoot Left     | J           |
| Shoot Up       | I           |
| Shoot Down     | K           |
| Shoot Up-Left  | U           |
| Shoot Up-Right | O           |
| Shoot Dn-Left  | M           |
| Shoot Dn-Right | . (period)  |

### Global
| Action        | Key |
|---------------|-----|
| Restart Match | R   |

---

## 🗺️ Map Layout

```
[P1]                          [P2]
       [Left Ledge]  [Right Ledge]
              [Float Platform]
                   [Block]
===================Ground===========
```

- **Main Ground** — Full width floor
- **Left / Right Ledges** — Raised side platforms for vertical play
- **Center Floating Platform** — Mid-air platform above center
- **Center Obstacle Block** — Solid block on the ground providing cover

---

## ⚔️ Rules

- **One-hit kill** — A single arrow ends the match instantly
- **8-directional shooting** — Fire arrows in any of 8 directions
- **Arrows normalize** — Diagonal arrows travel the same speed as cardinal
- **No respawn** — Match ends immediately; press **R** to restart

---

## 📁 Project Structure

```
ArcherDuel/
├── src/
│   ├── Main.java              # Entry point
│   ├── GameWindow.java        # JFrame setup
│   ├── GamePanel.java         # Game loop, input, collision, HUD
│   ├── Player.java            # Player physics, controls, state
│   ├── Arrow.java             # Projectile movement & rendering
│   ├── Arena.java             # Map definition & background rendering
│   ├── SpriteRenderer.java    # Programmatic pixel-art sprite generation
│   └── AnimationController.java # Animation state machine
├── run.sh                     # Linux/macOS build & run
├── run.bat                    # Windows build & run
└── README.md
```

---

## 🎨 Technical Notes

- **Sprites** are generated programmatically at startup — no image files needed
- **Double-buffering** via offscreen `BufferedImage` prevents flicker
- **Game loop** runs at 60 FPS via `javax.swing.Timer`
- **Pixel art** uses 2×2 pixel blocks for retro look
- **Death particles** spawn on arrow impact with physics simulation
- **Countdown** gives players 3 seconds before the match begins
