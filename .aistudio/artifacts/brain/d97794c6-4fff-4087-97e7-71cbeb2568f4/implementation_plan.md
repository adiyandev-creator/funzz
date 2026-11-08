# PartyGrid — Local Multiplayer Mini-Games

PartyGrid is a high-polish, offline same-device multiplayer arcade collection for 1 to 4 players. Built with Jetpack Compose and optimized Canvas game rendering, it delivers fast-launching, tactile mini-games with clean Apple/Nintendo-inspired minimalism and dynamic per-game touch controls.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the planning interview and serve as foundational constraints for the build:

- **Confirmed App Identity**: Brand name **PartyGrid**, with clean, understated typography, subtle borders, and a neutral, calm UI shell (#FFFFFF / #151515) where mini-games provide their own vibrant palette.
- **Confirmed Initial Lineup**: Complete launch of **all 8 core mini-games** across Action, Racing, Sports, Puzzles, and Party categories, each with full win/loss conditions, collision physics, local multiplayer, and AI opponents:
  1. **Tic-Tac-Toe** (Puzzles: 1P vs AI or 2P local)
  2. **Reaction Battle** (Party/Action: 1–4P false-start detection & millisecond reaction)
  3. **Paint Arena** (Party/Action: 2–4P real-time territory painter with physics & coverage calculation)
  4. **Spinner Battle** (Action: 2–4P knockback arena physics with ring-out elimination)
  5. **Micro Racing** (Racing: 1–4P top-down track racer with steering, drift, lap counters & checkpoints)
  6. **Memory Match** (Puzzles: 1–4P turn-based card reveal grid with combo scoring)
  7. **Tug of War** (Party/Sports: 2P / 2v2 rapid tap tug-of-war with rope tension dynamics)
  8. **Basketball Shots** (Sports: 1–4P arc trajectory shot mechanics with backboard & rim physics)
- **Confirmed Control Architecture**: **Dynamic corner and split-edge touch pads** tailored specifically to player count (opposite edges for 2P, 3-way layout for 3P, 4 corner pads for 4P) with customizable player colors (Blue, Coral, Emerald, Amber).

---

### 1. Overview & Core Concept

- **What It Does**: Allows 1 to 4 players to gather around a single Android phone or tablet and instantly play competitive and cooperative mini-games without internet, accounts, or setup friction.
- **Target Audience / Persona**: Friends, families, siblings, and party groups looking for immediate, zero-latency pick-up-and-play gaming on a single screen, plus solo players practicing against adaptive AI.
- **Key Value**: Less than 10 seconds from app launch to active gameplay; rock-solid offline reliability; responsive 60 FPS physics and canvas rendering; unified tournament scoring and player tracking.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Quick Play Flow**: Open app → Tap "Quick Play" or select player count (1P, 2P, 3P, 4P) → Select game card → 3-2-1-GO countdown → Fast gameplay → Results podium & Instant Rematch button.
2. **Custom Game Hub**: Browse by Category (Action, Racing, Sports, Puzzles, Party) → Filter by player compatibility → Preview average duration & difficulty → Toggle Favorite.
3. **Tournament Mode**: Select 2–4 players → Set round count (3, 5, or 7 rounds) → Random or custom game playlist → Point tallying across matches → Final Championship Podium.
4. **Player Profile & Stats**: Configure player avatars and colors → View win/loss history, high scores, and game records stored privately on device.

#### Visual Identity & Theme
- **Shell Palette (Minimalist Neutral)**:
  - *Light Mode*: Pure white canvas (`#FFFFFF`), subtle card backgrounds (`#F6F6F6`), borders (`#EAEAEA`), primary text (`#171717`), secondary text (`#777777`), brand accent (`#5578C8`).
  - *Dark Mode*: Deep charcoal (`#151515`), surface (`#202020`), borders (`#303030`), primary text (`#FFFFFF`), secondary text (`#A0A0A0`), brand accent (`#829BE0`).
- **Player Colors (Vibrant & Distinguishable)**:
  - *Player 1 (Blue)*: `#3B82F6` (Active Glow `#60A5FA`)
  - *Player 2 (Coral / Red)*: `#EF4444` (Active Glow `#F87171`)
  - *Player 3 (Emerald / Green)*: `#10B981` (Active Glow `#34D399`)
  - *Player 4 (Amber / Yellow)*: `#F59E0B` (Active Glow `#FBBF24`)
- **Typography & Rhythm**: Clean sans-serif hierarchy, generous whitespace, pill-shaped chips for player counts, and crisp vector iconography.
- **In-Game Aesthetics**: High-contrast, clean 2D vector art drawn directly in Compose Canvas (smooth circles, rounded bumpers, crisp trails, and responsive hit sparks).

---

### 3. Key Product Decisions & Trade-Offs

- **Game Engine & Rendering**:
  - *Chosen Approach*: Android Compose `Canvas` with Kotlin Coroutines game loops (`withFrameNanos` / 60 FPS fixed-timestep physics updates).
  - *Why*: Native Jetpack Compose interoperability, zero external heavy game engine overhead, instant lifecycle handling, tiny APK footprint, and predictable 60 FPS rendering.
  - *Alternatives Considered*: Unity/Godot (too heavy, long build times, large binary size) or raw SurfaceView (breaks Compose declarative overlay ergonomics).
- **Player Input System**:
  - *Chosen Approach*: Multi-touch `pointerInput` with corner zones and touch IDs (`pointerId`) mapped to virtual buttons / joysticks.
  - *Why*: Essential for up to 4 simultaneous fingers on one screen without touch cancellation or crosstalk.
- **Local Persistence**:
  - *Chosen Approach*: Room Database for player profiles, game statistics, favorites, and tournament records, combined with DataStore for sound/haptic settings.
  - *Why*: Robust structured queries for win rates and game history; 100% offline, zero network dependencies.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          MainActivity (Edge-to-Edge)                   │
├────────────────────────────────────────────────────────────────────────┤
│                     Navigation Compose (Type-Safe Routes)              │
│  ┌───────────────┬────────────────┬─────────────────┬───────────────┐  │
│  │  HomeScreen   │  LibraryScreen │ FavoritesScreen │ SettingsScreen│  │
│  └───────┬───────┴────────┬───────┴────────┬────────┴───────┬───────┘  │
│          │                │                │                │          │
│  ┌───────▼────────────────▼────────────────▼────────────────▼───────┐  │
│  │                PlayerSelectionModal / Screen                     │  │
│  │           (1P Solo vs AI, 2P Head-to-Head, 3P, 4P Party)         │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │                                     │
│  ┌───────────────────────────────▼──────────────────────────────────┐  │
│  │                       GameHostScreen                             │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │ GameHeader: Pause / Sound / Timer / Round Score            │  │  │
│  │  ├────────────────────────────────────────────────────────────┤  │  │
│  │  │ MiniGameView (Compose Canvas 60 FPS Engine)                │  │  │
│  │  │ - SpinnerBattle        - MicroRacing        - Basketball   │  │  │
│  │  │ - PaintArena           - ReactionBattle     - TugOfWar     │  │  │
│  │  │ - TicTacToe            - MemoryMatch                       │  │  │
│  │  ├────────────────────────────────────────────────────────────┤  │  │
│  │  │ MultiTouchInputOverlay (Dynamic Corner & Split-Edge Pads)  │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  │                               │                                     │
│  │  ┌────────────────────────────▼───────────────────────────────┐  │  │
│  │  │ GameResultDialog / TournamentPodium (Rematch / Next Game)  │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
├──────────────────────────────────┼─────────────────────────────────────┤
│                                  ▼                                     │
│                     Shared Architecture & Engine                       │
│  ┌─────────────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │   MultiplayerEngine     │  │  Sound & Haptic│  │   Room Database│  │
│  │   - GameLifecycle       │  │  - SoundPool   │  │   - PlayerStats│  │
│  │   - AI Bots (3 levels)  │  │  - ToneGenerator│  │  - Favorites  │  │
│  │   - Collision2D/Physics │  │  - Vibrator    │  │  - GameRecords │  │
│  └─────────────────────────┘  └────────────────┘  └────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Components & Responsibilities
- **`MultiplayerEngine`**: Holds player slot definitions, player colors, team allocations, and score tallies. Provides AI controllers (Easy, Normal, Hard) that generate synthetic input events when in 1-player mode.
- **`MiniGame` Interface**: Standard contract implemented by all 8 games: `initialize(players, bounds)`, `update(deltaSeconds)`, `onPlayerInput(playerId, action)`, `draw(drawScope)`, `isGameOver(): Boolean`, `getResults(): GameResult`.
- **`InputOverlay`**: Configures touch hit-boxes based on screen orientation and player count:
  - 2 Players: Left edge vs Right edge, or Top edge vs Bottom edge.
  - 3 Players: Top-left, Top-right, and full Bottom edge.
  - 4 Players: Four quadrant corner pads with rotation indicators.
- **`SoundManager` & `HapticManager`**: Audio cues for countdown, tap, collision, goal, win, and game over, with toggle controls in Settings.
- **`StatsRepository`**: Tracks total matches played, wins per player, and best scores per game in local Room database.
