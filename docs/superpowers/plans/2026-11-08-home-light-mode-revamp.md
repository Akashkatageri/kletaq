# Home Screen Light Mode Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Home tab in Light Mode to match the reference screenshot (`media_1794075166837_4906a129.png`) with rich Teal primary buttons (`#0B6E67`), embed the 3D book vector (`ic_3d_book.xml` from `media_1794075199890_09788053.png`) in the Current Quest card, show missed streak days as frozen in the weekly tracker, and clean up leftover uncommitted sections in `HomeScreen.kt`.

---

## File Structure & Responsibilities

- **`app/src/main/java/com/kletaq/app/core/theme/Theme.kt`**
  - Defines light theme tokens: `TealPrimary = Color(0xFF0B6E67)`, `TealMintBackground = Color(0xFFE6F5F2)`, `AppBackgroundLight = Color(0xFFF4F6FB)`, `StreakOrange = Color(0xFFC2410C)`, `FrozenIce = Color(0xFF38BDF8)`, `FrozenIceBg = Color(0xFFE0F2FE)`.
  - Sets `primary = TealPrimary` in `LightColorScheme`.

- **`app/src/main/java/com/kletaq/app/features/home/components/HeaderSection.kt`**
  - Houses the top unified Hero Profile & Level Card:
    - Teal Squircle Avatar with user initial & Level badge overlay
    - User name & academic program / semester subtitle
    - Circular Search & Notifications buttons
    - XP Progress Bar (`⚡ 952 XP`, amber gradient progress bar, `48 XP to Level 4`)
    - 3-Capsule Stats Row (🔥 Streak, 🛡️ Shields, 🎁 Tasks)

- **`app/src/main/java/com/kletaq/app/features/home/components/WeeklyStreakCard.kt`** *(New)*
  - Displays the 7-day streak tracker (M, T, W, T, F, S, S):
    - Flame `🔥` on completed days
    - Frozen snowflake `❄️` / shield `🛡️` with ice-blue styling on missed/frozen days
    - Dashed ring on current pending day
    - "Pending" / "Active" pill status

- **`app/src/main/java/com/kletaq/app/features/home/components/ContinueLearningCard.kt`**
  - Overhauls the Current Quest card:
    - Teal outline border `BorderStroke(1.5.dp, Color(0xFF0B6E67))`
    - Mint `+ CURRENT QUEST` tag and purple `X% Done` tag
    - 3D Book SVG (`R.drawable.ic_3d_book`) on the right
    - Slim teal progress bar
    - Reward & count chips: `+40 XP reward`, `notes`, `flashcards`
    - Rich Teal action button `Resume Lesson ➔` / `Start Lesson ➔`
    - Stopwatch icon button (`Icons.Outlined.Timer`)

- **`app/src/main/java/com/kletaq/app/features/home/components/ReviewsCard.kt`**
  - Restyled spaced repetition card with purple `SPACED REPETITION` header, `X Due` badge, mystery question mark boxes (`?`), and purple `Start Spaced Review` button (`#5E5CE6`).

- **`app/src/main/java/com/kletaq/app/features/home/components/ClassTimetableCard.kt`**
  - Updated with dashed border styling and rich teal `Upload` / `View` button (`#0B6E67`).

- **`app/src/main/java/com/kletaq/app/features/home/components/BadgesSection.kt`** *(New)*
  - Horizontal badges progression row: "Badges" header (`4 of 18`), badge circles (First focus, 7 day streak, Note taker, 30 day streak).

- **`app/src/main/java/com/kletaq/app/features/home/HomeScreen.kt`**
  - Removes leftover `classic_switch_banner`, `DailyTasksSection`, and broken imports.
  - Assembles the 7 cards in sequential order in `LazyColumn`.

---

## Tasks

### Task 1: Update Theme Tokens & Color Palette
- [x] Update `app/src/main/java/com/kletaq/app/core/theme/Theme.kt`:
  - Add `TealPrimary = Color(0xFF0B6E67)`
  - Add `TealPrimaryDark = Color(0xFF14B8A6)`
  - Add `TealMintBackground = Color(0xFFE6F5F2)`
  - Add `StreakOrangeAccent = Color(0xFFC2410C)`
  - Add `FrozenIceColor = Color(0xFF0284C7)`
  - Add `FrozenIceBg = Color(0xFFE0F2FE)`
  - Update `AppBackgroundLight = Color(0xFFF4F6FB)`
  - Update `BorderLight = Color(0xFFE5E7EB)`
  - Set `primary = TealPrimary` in `LightColorScheme`

### Task 2: Build Unified Hero Profile & Level Card in `HeaderSection.kt`
- [x] In `app/src/main/java/com/kletaq/app/features/home/components/HeaderSection.kt`:
  - Refactor to render a cohesive rounded card container (`CardSurfaceLight`, 20.dp radius, subtle outline).
  - Top row:
    - Teal squircle avatar with white initial (`A`) and level badge (`LevelBadge` pill at corner).
    - User name (bold `TextPrimary`) and academic subtitle (e.g. `Level 3 · Scholar · ISE / Sem 3`).
    - Search (`Icons.Default.Search`) and Notifications (`Icons.Outlined.Notifications`) buttons in soft circular backgrounds.
  - Middle: XP Progress row:
    - `⚡ $currentXp XP` (amber), gradient progress bar to next level, `$xpRemaining XP to Level $nextLevel`.
  - Bottom row: 3 Stat Capsules:
    - Streak Capsule (flame `🔥`, `$streakDays d`, "Streak pending" / "Streak active", orange tint).
    - Shields Capsule (shield `🛡️`, `$shieldsCount`, "Shields", soft blue tint).
    - Tasks Capsule (gift/box `🎁`, `$completedTasks/$totalTasks`, "Tasks", soft tan tint).

### Task 3: Create `WeeklyStreakCard.kt` with Frozen Day Support
- [x] Create `app/src/main/java/com/kletaq/app/features/home/components/WeeklyStreakCard.kt`:
  - Render rounded white card (`RoundedCornerShape(20.dp)`).
  - Header: `$streakDays day streak` with subtitle "Finish one focus run today to keep it alive".
  - Right badge: "Pending" (`#FFF0E6` bg, `#C2410C` text) or "Active" (`#ECFDF5` bg, `#059669` text).
  - 7-day row representing days of the current week (M, T, W, T, F, S, S):
    - Active/Completed day: Solid circle with flame `🔥`.
    - Missed & Protected (Frozen) day: Ice-blue circle (`#E0F2FE` bg, `#38BDF8` border) with `❄️` snowflake or `🛡️` shield icon.
    - Today (pending): Dashed orange ring with day letter in orange.
    - Future day: Muted soft grey circle (`#F3F4F6`) with day letter.

### Task 4: Revamp Current Quest Card in `ContinueLearningCard.kt`
- [x] In `app/src/main/java/com/kletaq/app/features/home/components/ContinueLearningCard.kt`:
  - Card container with `RoundedCornerShape(22.dp)` and `BorderStroke(1.5.dp, TealPrimary)`.
  - Header row:
    - Left: `+ CURRENT QUEST` mint pill (`TealMintBackground` bg, `TealPrimary` text, bold 11.sp).
    - Right: `$progressPercentage% Done` purple pill (`#EDE9FE` bg, `#6D28D9` text).
  - Subject title: Secondary text in 12.sp.
  - Main topic row:
    - Left column: Bold topic title (`TextPrimary`, 17.sp, maxLines = 2).
    - Right column: Vector illustration `painterResource(R.drawable.ic_3d_book)` sized `72.dp x 72.dp`.
  - Progress bar: Slim teal progress bar with rounded ends and indicator.
  - Metadata chips row:
    - `+40 XP reward` pill (amber `#FEF3C7` bg, `#B45309` text).
    - `notes` count pill (`#F3F4F6` bg, `#4B5563` text).
    - `flashcards` count pill (`#F3F4F6` bg, `#4B5563` text).
  - Actions row:
    - Primary Button: `Resume Lesson ➔` / `Start Lesson ➔` with `TealPrimary` (`#0B6E67`) background, white text, bold, `RoundedCornerShape(14.dp)`.
    - Secondary Button: Stopwatch/Timer button (`Icons.Outlined.Timer`, soft circular/squircle background) to launch focus timer.

### Task 5: Restyle Spaced Repetition Card in `ReviewsCard.kt`
- [x] In `app/src/main/java/com/kletaq/app/features/home/components/ReviewsCard.kt`:
  - Header: Purple circular icon, `SPACED REPETITION` bold text (`#5E5CE6`), and `$dueCount Due` orange pill badge.
  - Subtitle: "Next review topic:".
  - Topic title: Bold prominent text.
  - Mystery review cards row: 4 dashed rounded boxes (`#EEEDFC` bg, dashed border `#C7D2FE`, `?` in purple).
  - Action button: Solid purple `Start Spaced Review ($dueCount topics) ➔` (`Color(0xFF5E5CE6)`).

### Task 6: Update `ClassTimetableCard.kt` and Create `BadgesSection.kt`
- [x] In `app/src/main/java/com/kletaq/app/features/home/components/ClassTimetableCard.kt`:
  - Apply dashed border styling and ensure the `Upload` / `View` button uses `TealPrimary` (`#0B6E67`).
- [x] Create `app/src/main/java/com/kletaq/app/features/home/components/BadgesSection.kt`:
  - Title row: "Badges" (bold, 17.sp) and "4 of 18" (secondary, 12.sp).
  - Horizontal row of badge circles:
    - ⚡ "First focus" (Teal circle `#0B6E67` with white icon)
    - 🔥 "7 day streak" (Orange circle `#C2410C` with white flame)
    - 📄 "Note taker" (Purple circle `#5E5CE6` with white note icon)
    - 🔒 "30 day streak" (Dashed grey ring with lock icon)

### Task 7: Wire Everything in `HomeScreen.kt` & Clean Leftover Code
- [x] In `app/src/main/java/com/kletaq/app/features/home/HomeScreen.kt`:
  - Remove leftover `classic_switch_banner`, `DailyTasksSection`, and missing imports.
  - Wire the 7 cards in sequence:
    1. `HeaderSection` (Unified Hero Profile & Level Card)
    2. `WeeklyStreakCard` (with frozen streak day support)
    3. `ContinueLearningCard` (Current Quest card with 3D book SVG and teal button)
    4. `ReviewsCard` (Spaced Repetition card)
    5. `MemoryPalaceHomeCard` (Memory Palace card)
    6. `ClassTimetableCard` (Class Timetable card)
    7. `BadgesSection` (Badges progression row)

### Task 8: Verification
- [x] Verify that all files compile cleanly and no broken references remain (`compileBenchmarkKotlin` SUCCESSFUL).
- [x] Verify that Light Mode renders `#F4F6FB` background with pure white cards and rich teal buttons.
