# Requirements traceability

Every requirement from the assignment (Ludo Project.pdf), where it is implemented, and the automated test that checks it.
Tests are in `src/test/java` and run on every build (`build.bat`); the JAR is not produced if a test fails.

## Board and squares

| # | Requirement | Implementation | Test |
|---|---|---|---|
| B1 | Easy: 52-square loop + 6 home squares per colour | `BoardFactory.createLayout` | `BoardFactoryTest.pathLengthsMatchTheSpecification`, `trackSizesAre52Then64Then72` |
| B2 | Medium: + 64-square ring; Hard: + 64 ring + 72 ring | `BoardFactory.perimeter`, `Difficulty.outerRings` | `trackSizesAre52Then64Then72` |
| B3 | Clockwise movement from the start square (#), around the shared track, into the home path to the centre | colour path = list of cells (`Board.path`) | `everyPathIsContinuousUniqueAndInsideTheGrid` |
| B4 | Castle: each player's 4 pieces start there | `Piece.IN_CASTLE`, `Board.castleCenter` | `aPieceLeavesTheCastleOnlyWithASix` |
| B5 | Safe square (star) protects every piece on it | `SquareType.SAFE` (protected) | `piecesOnASafeSquareCannotBeCaptured` |
| B6 | Start square (#) is protected, fixed per level; on Medium/Hard in the corner next to the castle | `SquareType.START`, `BoardFactory` | `startSquaresAreOwnedAndInTheCornerOnMediumAndHard`, `piecesOnAStartSquareCannotBeCaptured` |
| B7 | Arrow square in a player's colour leads that colour to the next inner track; regular (not protected) otherwise | path continues inward after the arrow; `SquareType.ARROW` | `eachColourHasItsOwnArrowsOnItsPathAndItsOwnFinish`, `protectionRulesFollowTheSpecification` |
| B8 | Home squares: 6 from the finish, protected, move only with a roll ≤ the distance | `SquareType.HOME`, exact-roll check in `Game.computeOptions` | `theFinishNeedsAnExactRoll`, `tooBigARollMustBeUsedByAnotherPiece` |
| B9 | Question squares: green easy / yellow medium / red hard, random question of that level, not protected | `SquareType.QUESTION`, `Game.land`, `QuestionService.draw` | `aCorrectAnswerMovesForwardByTheQuestionLevel`, `drawReturnsTheRequestedLevelWithoutRepeatsUntilAllAreUsed` |
| B10 | Surprise squares (protected) with 10/10/10/30/30/10 % effects | `Surprise` | `surpriseWeightsMatchTheSpecification` + one test per effect |
| B11 | Counts per level: Easy 4 safe, 4 arrows, 4 starts. Medium 8/8, 6 questions (2 per level), 2 surprises. Hard 12/12, 9 questions (3 per level), 3 surprises | `Difficulty`, `BoardFactory.placeRandomSquares` | `fixedSquareCountsPerLevel` |
| B12 | Safe, question and surprise positions random every game; start and arrow positions fixed | `BoardFactory.create(difficulty, rng)` | `randomSquaresChangeBetweenGamesButNeverReplaceFixedSquares` |

## Pieces, players, dice

| # | Requirement | Implementation | Test |
|---|---|---|---|
| P1 | 4 pieces per player, 2–4 players, unique colour and name | `Player`, `Game.Builder.build` | `setupRulesAreEnforced` |
| P2 | Capture: landing on a single opponent piece on an unprotected square sends it to its castle | `Game.captureAt` | `landingOnASingleOpponentPieceCapturesIt` |
| P3 | Double Block: two pieces of one colour block landing and passing, on any square type; broken when one moves | `Game.isBlockedFor`, `isPathClear` | `aDoubleBlockCannotBePassed`, `aDoubleBlockCannotBeLandedOn`, `aDoubleBlockOnTheStartSquareStopsEntering`, `aDoubleBlockIsBrokenWhenOnePieceMoves`, `ownPiecesFormABlockAndCanShareASquare` |
| P4 | Normal die 1–6, equal probability | `NormalDice` | used by every rules test |
| P5 | Boost die 1–3: steps = normal × (N−1) | `BoostDice`, `Game.roll` | `boostMultipliesTheNormalDieByNMinusOne`, `boostOfOneGivesZeroStepsAndNoMove` |
| P6 | UltraBoost die 1–6: steps = normal × (N−1) | `UltraBoostDice` | `hardLevelUsesUltraBoostWithMultipliersUpToFive` |
| P7 | Leaving the castle uses only the normal die (6), the multiplier is ignored even if ×0; or use the multiplied value on a piece on the board | `Game.computeOptions` | `aSixWithBoostZeroStillEntersAPiece`, `aSixWithBoostOffersEnterOrTheMultipliedMove`, `aSixCanMoveAPieceAlreadyOnTheBoard` |

## Questions

| # | Requirement | Implementation | Test |
|---|---|---|---|
| Q1 | Multiple choice, 4 answers | `Question` record validation | `invalidQuestionsAreRejected` |
| Q2 | At least 30 questions over 3 levels, from the course material | `questions-default.json` (42: 13 easy / 15 medium / 14 hard; Agile, Scrum, MVC, Design Patterns, QA, SW design, Git, plus the six architecture questions of `questions_python.xlsx`, IDs 37-42) | `theDefaultQuestionBankIsValidAndLargeEnough` |
| Q3 | Right answer +1/+2/+3, wrong answer −1/−2/−3 by level | `Game.answer`, `QuestionLevel.steps` | `aCorrectAnswerMovesForwardByTheQuestionLevel`, `aWrongAnswerMovesBackByTheQuestionLevel`, `aWrongAnswerNeverMovesBehindTheStartSquare`, `aCorrectAnswerStopsAtTheFinish`, `aCorrectAnswerStopsBeforeAnOpponentBlock` |

## Game rules

| # | Requirement | Implementation | Test |
|---|---|---|---|
| R1 | Enter only with a 6; no active pieces and a 1–5 → turn passes immediately | `Game.roll` / `computeOptions` | `aPieceLeavesTheCastleOnlyWithASix` |
| R2 | Free choice of the piece to move among legal moves | `Game.options`, `choose` | `aSixCanMoveAPieceAlreadyOnTheBoard`, `actionsAreRejectedInTheWrongPhase` |
| R3 | A 6 gives another roll; the third 6 in a row is cancelled and the turn passes | `Game.roll`, `finishAction` | `enteringPutsThePieceOnTheStartSquareAndASixGivesAnotherRoll`, `theThirdSixInARowIsCancelledAndTheTurnPasses` |
| R4 | Exact roll to finish; otherwise move another piece; if none, the turn passes | `computeOptions` | `theFinishNeedsAnExactRoll`, `tooBigARollMustBeUsedByAnotherPiece` |
| R5 | Game ends when all players but one have finished; the finishing order is recorded | `Game.checkGameOver`, `finishOrder` | `theGameEndsWhenAllPlayersButOneHaveFinished`, `computerOnlyGamesAlwaysFinishOnEveryLevel` |

## System flow (screens)

| # | Requirement | Implementation |
|---|---|---|
| F1 | Start screen: start a game, game history, restricted question manager | `View/StartPanel` |
| F2.1 | Choose the number of players and the level | `View/SetupPanel` |
| F2.2 | Name and unique colour per player | `View/SetupPanel` (+ `Game.Builder` validation) |
| F2.3 | Board screen shows the time, the player whose turn it is, the dice (with function), the board | `View/GamePanel`, `BoardCanvas`, `DiceView` |
| F2.4 | Play from start to finish with questions, surprises and safe squares | `Controller/GameController` |
| F2.5 | At the end the game is saved with the winners order, time and level | `GameController.finish` → `HistoryRepository.save` (`historyIsSavedAndReadBackNewestFirst`) |
| F3 | Game history screen | `View/HistoryPanel` (with level filter and average time) |
| F4 | Question manager: view, edit, add, delete | `View/AdminPanel`, `QuestionEditorDialog`, `Controller/AdminController` (`adminCanAddEditAndDelete`) |
| F4.1 | Security: protected from players, choice justified and implemented in code | [SECURITY.md](../SECURITY.md), `security/*`, `QuestionService` (`SecurityTest`, `adminOperationsNeedAValidSession`) |

## Data and non-functional requirements

| # | Requirement | How it is met |
|---|---|---|
| D1 | Organised classes and data structures for history, profiles and questions | `GameRecord`, `Player`, `Question`, repositories – see [ARCHITECTURE.md](ARCHITECTURE.md) |
| D2 | Security of sensitive data (questions and answers) at the data level | AES-GCM encrypted bank + session check in `QuestionService` |
| D3 | History in a fixed schema published by the course | Implemented. `CsvHistoryRepository` writes the lecturer's eight columns exactly, and the lecturer's CSV is bundled in the JAR and copied to `history.csv` on first run (`seedFromCourseTemplate`). A file that does not match the course header is reported and left untouched. See `data/lecturer-history-template.csv`. |
| N1 | Java | Java 17+ (built with `--release 17`) |
| N2 | Runnable file in every submission, works on more than one computer | `dist/EduLudo.jar` (any OS with Java 17+) and `dist/EduLudo/EduLudo.exe` (Java bundled); no external libraries |
| N3 | MVC architecture | `Model` / `View` / `Controller` packages, as named in the assignment. The View declares `Actions` interfaces that the controllers implement, so the View does not depend on the Controller. |
| N4 | Design patterns | Factory, Builder, Strategy (×2), State, Observer, Repository, Protection Proxy – see ARCHITECTURE.md |
| N5 | Clear documentation | Javadoc on every class, README, ARCHITECTURE, SECURITY, this file |
| N6 | Edge cases tested | 136 automated tests plus 9 Swing interaction checks (blocks, exact finish, third six, ×0 multiplier, question limits, tampered files, lockout, CSV written by Excel…) |
| N7 | GitHub, all members weekly | see [TEAM_PLAN.md](TEAM_PLAN.md) |

## Bonus features (beyond the requirements)

- Computer players with a move-scoring strategy (`Model/ai/ComputerPlayer`)
- Animated piece movement and dice; highlighted legal pieces; tooltips on every square
- Colour-coded game log; rules screen in the game; "Play again" with the same setup
- History filter by level and average game time; question search and filter in the admin wizard
- Admin password change, login lockout, session expiry, tamper detection with restore
- Windows `.exe` with bundled Java; crash log instead of silent failure

## Interpretations of unclear points (decided by the team)

| Point in the PDF | Our interpretation |
|---|---|
| Surprise "choose an opponent, roll, move one piece by half the roll" | The player chooses an opponent's piece; a normal die is rolled; that piece moves **back** ⌊roll/2⌋ squares (a surprise that helps the lander). |
| Movement after a question or a surprise ("+3", "back 3") | Following the course clarification (ANSWERS.xlsx): the piece **does** capture and **does** trigger the square it lands on, so effects can chain. It stops before an opponent Double Block, never goes behind the start square or past the finish. A piece pushed back by the Sabotage surprise is the one exception: it can still capture, but its new square is not triggered, because it is not its owner's turn. |
| Several single opponent pieces of different colours on the landing square | All of them are captured (each is "a single piece of another player"). |
| Turn order | Clockwise seating: Red, Green, Yellow, Blue (only the colours in the game). |
| Exact board drawings (start and arrow positions) | Taken from the description: Easy start = classic start square; Medium/Hard start = outer corner next to the castle; arrows = the end of each lap. If the official images differ, only `BoardFactory` changes. |
