# Wordscapes - Android Word Puzzle Game

A production-ready Wordscapes-style word puzzle game built for Android using Kotlin, Jetpack Compose, MVVM architecture, and Canvas gesture rendering.

---

## 🏗️ 1. Architectural Choices

The application follows **Clean Architecture** principles and **MVVM with Unidirectional Data Flow (UDF)**:

```text
[ Presentation Layer ]
  HomeScreen / LevelSelectScreen / GameScreen / PauseScreen
          │ (Observes StateFlow)
          ▼
   [ ViewModels ] (HomeViewModel, LevelSelectViewModel, GameViewModel)
          │ (Executes logic)
          ▼
  [ Domain / Use Cases ] (GameEngine, ValidateWordUseCase, CalculateScoreUseCase)
          │ (Retrieves data)
          ▼
   [ Repositories ] (LevelRepositoryImpl, UserPreferencesRepositoryImpl)
          │
          ▼
  [ Data Sources ] (LevelDataSource / assets/levels.json & DataStore Preferences)
```

### Key Highlights:
- **UI State (`GameUiState`)**: Immutably models screen state (`grid`, `letters`, `selectedIndices`, `score`, `hintsRemaining`, `showOneRemaining`, `isCompleted`, `isLevelCompleting`, `isGestureActive`).
- **State Preservation**: Uses `SavedStateHandle` in `GameViewModel` so active level progress, found words, bonus words, scores, hint counts, and shuffled letter orders survive configuration changes (rotation) and process death.
- **Local Persistence**: `UserPreferencesRepository` uses **DataStore Preferences** to persist highest unlocked levels, completed level sets, and total score across app sessions.
- **One-Time Event Streams**: `Channel<GameNavigationEvent>` handles one-time navigation events (e.g. `AutoNavigateToNextLevel`) without re-triggering navigation during recompositions or rotation.
- **Lifecycle-Aware Screen Insets & Keep-Screen-On**: Edge-to-edge window drawing with `statusBarsPadding()`/`navigationBarsPadding()` and `KeepScreenOn` using `FLAG_KEEP_SCREEN_ON` during gameplay.

---

## 🖐️ 2. Custom Swipe Gesture Logic

The circular letter wheel and drag line rendering are built using Compose **Canvas** and **Pointer Input**:

### Gesture Detection & Geometry
1. **Letter Node Placement**:
   - Letters are positioned along an outer placement circle centered at $(cx, cy)$:
     $$\text{angle} = -\frac{\pi}{2} + i \cdot \left(\frac{2\pi}{N}\right)$$
     $$\text{nodeCenter}_i = \left(cx + R \cdot \cos(\text{angle}), \; cy + R \cdot \sin(\text{angle})\right)$$
2. **Touch Collision**:
   - `detectDragGestures` monitors drag touch coordinates $(touchX, touchY)$.
   - Calculates distance to each letter center:
     $$\text{distance} = \sqrt{(touchX - cx_i)^2 + (touchY - cy_i)^2}$$
   - Hit detection radius is set to $1.4\times$ the visible letter circle radius for responsive, forgiving touch interactions.
3. **Dynamic Line Drawing**:
   - Renders a continuous path line connecting the centers of selected letters in sequence.
   - Renders a dynamic line segment tracking from the last selected letter node to the user's active finger location while dragging.
4. **Gesture Safety**:
   - `LetterWheel` reports active gesture lifecycle via `onGestureActive(Boolean)`.
   - Power-ups like **Show One** are safely disabled while a swipe gesture is active.

---

## 📊 3. Level Data Structure

Level definitions are stored in `app/src/main/assets/levels.json` with 15 fully playable crossword levels containing valid horizontal and vertical word intersections.

### JSON Dataset Schema:
```json
{
  "levels": [
    {
      "id": 1,
      "letters": ["C", "A", "T"],
      "words": [
        { "word": "CAT", "row": 0, "column": 0, "direction": "HORIZONTAL" },
        { "word": "ACT", "row": 0, "column": 1, "direction": "VERTICAL" }
      ],
      "bonusWords": ["AT"]
    }
  ]
}
```

### Data Fields:
- `id`: Unique integer level identifier (1 to 15).
- `letters`: Array of available letters for the circular letter wheel (3–5 letters).
- `words`: Array of required crossword words.
  - `word`: Word string (e.g. `"CAT"`).
  - `row` & `column`: Starting grid matrix coordinates.
  - `direction`: `"HORIZONTAL"` (left to right) or `"VERTICAL"` (top to bottom).
- `bonusWords`: Array of valid extra words that grant bonus score (`+20` points per letter).

---

## ⚡ 4. Gameplay Mechanics & Power-Ups

- **Limited Hint System (`💡 HINT: 3`)**:
  - Deducts **100 points** from total score and **1 hint** from remaining hints.
  - Identifies an unsolved crossword word and permanently reveals one of its cells with a spring scale-and-highlight animation.
  - Disabled when score $< 100$ or hints $= 0$.
- **Show One Power-Up (`🔄 Show One × 3`)**:
  - Automatically swaps the wheel display positions of two letters with a smooth spring animation.
  - Does not alter crossword words, level data, or player score.
  - Usage limit of 3 per level; disabled during active swipe gestures.
- **Automatic Next-Level Progression**:
  - As soon as the final crossword word is completed, plays grid animations, displays `🎉 LEVEL COMPLETE!`, pauses briefly (750 ms), and automatically transitions to the next level.
  - Uses backstack replacement (`popUpTo(Routes.GAMEPLAY) { inclusive = true }`) so pressing Back from Level 5 returns directly to Level Select without forcing the user to step back through every completed level.

---

## 🧪 5. Testing

Unit tests are located in `app/src/test/java/com/dayanand/wordscapes/`:
- `ValidateWordUseCaseTest`: Validates crossword words, bonus words, duplicate words, and invalid words.
- `CalculateScoreUseCaseTest`: Validates scoring logic for crossword words, bonus words, and level completion bonuses.
- `GameEngineTest`: Validates grid matrix generation, cell revealing, hint application, and level completion checks.
- `GameViewModelTest`: Validates state management, 100-point hint deductions, Show One letter swapping, gesture locks, `SavedStateHandle` restoration, and automatic next-level navigation events.

To run all unit tests:
```bash
./gradlew test
```

---

## ⚙️ 6. Build Instructions

1. Open Android Studio and choose **Open Existing Project**.
2. Select the `Wordscapes` directory.
3. Sync Gradle (`Sync Project with Gradle Files`).
4. Build: `./gradlew assembleDebug`.
5. Run on an Android device or emulator running Android 7.0 (API 24) or higher.
