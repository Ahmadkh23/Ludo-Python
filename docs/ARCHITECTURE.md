# Architecture

## 1. MVC layers

```mermaid
flowchart LR
    subgraph View["View (com.eduludo.View)"]
        SP[StartPanel] --- SetP[SetupPanel] --- GP[GamePanel + BoardCanvas + DiceView]
        HP[HistoryPanel] --- AP[AdminPanel] --- DLG[QuestionDialog, LoginDialog, ...]
    end
    subgraph Controller["Controller (com.eduludo.Controller)"]
        AC[AppController] --> GC[GameController]
        AC --> ADC[AdminController]
    end
    subgraph Model["Model (com.eduludo.Model)"]
        G[Game] --> B[Board]
        G --> PL[Player / Piece]
        G --> D[DiceStrategy]
        G --> S[Surprise]
        BF[BoardFactory] --> B
        AI[ComputerPlayer]
    end
    subgraph Services["Service / Security / Persistence"]
        QS[QuestionService] --> QR[(EncryptedQuestionRepository)]
        QS --> AUTH[AuthService]
        HR[(CsvHistoryRepository)]
    end
    View -- "user actions (Actions interfaces)" --> Controller
    Controller -- "calls" --> Model
    Model -- "GameEvent (Observer)" --> Controller
    Controller -- "updates" --> View
    Controller --> Services
    G -. "QuestionProvider (draw only)" .-> QS
```

| Layer | Responsibility | Depends on |
|---|---|---|
| **Model** (`Model`) | Board geometry, all game rules, turn state machine, computer decisions. No Swing code. | nothing (pure Java) |
| **View** (`View`) | Drawing and input. Each screen exposes an `Actions` interface for its user actions. Reads the model to draw. | model (read only) |
| **Controller** (`Controller`) | Receives user actions, calls the model, plays the computer seats, switches screens, saves history. | model, view, services |
| **Service / security / persistence** | Question access rules, admin login, encryption, files. | model records |

The model is fully tested without a UI (136 automated tests and 9 Swing interaction checks). The views never change the model directly.

## 2. Design patterns

| Pattern | Where | Why |
|---|---|---|
| **Model–View–Controller** | the packages above | separates rules, drawing and flow; the rules can be tested without a window |
| **Factory** | `Model/BoardFactory` | builds the EASY / MEDIUM / HARD board (geometry, fixed squares, random squares) in one place |
| **Builder** | `Game.Builder` | creates a game step by step and validates the setup (2–4 players, unique colours and names) before the object exists |
| **Strategy** | `Model/dice/DiceStrategy` → `NormalDice`, `BoostDice`, `UltraBoostDice` | the engine rolls "a die" without knowing which; tests plug in a scripted die |
| **Strategy** | `Model/Surprise` (each constant has its own `apply`) | each surprise effect is a separate algorithm with its own probability |
| **State** | `Model/Phase` + `Game.requirePhase` | each action is only legal in one phase (roll → choose → answer…); illegal calls throw |
| **Observer** | `GameListener` / `GameEvent` | the model announces what happened; the controller updates the log, dice and board |
| **Repository / DAO** | `QuestionRepository`, `HistoryRepository` | storage is hidden behind interfaces; the course's history schema can be plugged in by adding one class |
| **Protection Proxy / Facade** | `service/QuestionService` | the only gateway to the bank; checks the admin session before any sensitive operation |
| **Composition Root (manual Dependency Injection)** | `App.java` | all objects are created and connected in one place, so every class receives its dependencies and can be tested alone |

*Singleton was deliberately not used*: global instances make testing harder. One instance of each service is created
in `App` and passed to whoever needs it.

## 3. Board model

All three boards are built from one red layout that is rotated 90° for every other colour, so the board is fair.

| Level | Grid | Shared tracks | Path length per colour | Arrows |
|---|---|---|---|---|
| Easy | 15×15 | 52-square loop | 51 + 6 home = 57 | 4 (loop → home path) |
| Medium | 17×17 | 64-square ring + 52 loop | 58 + 1 + 51 + 6 = 116 | 8 (ring → loop, loop → home) |
| Hard | 19×19 | 72 ring + 64 ring + 52 loop | 72 + 58 + 1 + 51 + 6 = 188 | 12 |

A piece's position is just an index into its colour's path (`-1` = castle, last index = finish).
Arrows are therefore automatic: after a colour's arrow square, its path continues on the next inner track.

## 4. Turn flow

```mermaid
stateDiagram-v2
    [*] --> WAITING_FOR_ROLL
    WAITING_FOR_ROLL --> CHOOSING_MOVE: roll() and a legal move exists
    WAITING_FOR_ROLL --> WAITING_FOR_ROLL: no move / third 6 → next player
    CHOOSING_MOVE --> ANSWERING_QUESTION: landed on ?
    CHOOSING_MOVE --> CHOOSING_OPPONENT: surprise "sabotage"
    CHOOSING_MOVE --> WAITING_FOR_ROLL: move done (same player after a 6 or a bonus roll, else next player)
    ANSWERING_QUESTION --> WAITING_FOR_ROLL: answer(i)
    CHOOSING_OPPONENT --> WAITING_FOR_ROLL: chooseOpponentPiece(p)
    CHOOSING_MOVE --> GAME_OVER: all players but one finished
    ANSWERING_QUESTION --> GAME_OVER
    WAITING_FOR_ROLL --> [*]
    GAME_OVER --> [*]
```

## 5. Main classes

```mermaid
classDiagram
    class Game {
      +roll() DiceRoll
      +choose(MoveOption)
      +answer(int) boolean
      +chooseOpponentPiece(Piece) int
      +options() List~MoveOption~
      +phase() Phase
      +toRecord() GameRecord
    }
    class Board { +path(PlayerColor) List~Cell~ +square(Cell) Square }
    class Square { +type() SquareType +isProtected() boolean }
    class Player { +pieces() List~Piece~ +hasFinished() boolean }
    class Piece { +position() int +isInCastle() +isFinished() }
    class DiceStrategy { <<interface>> +roll(Random) int }
    class QuestionProvider { <<interface>> +draw(QuestionLevel) Question }
    class QuestionService { +draw() +listAll(AdminSession) +add() +update() +delete() }
    class AuthService { +createAdmin() +login() AdminSession +requireValid() }
    Game --> Board
    Game --> "2..4" Player
    Player --> "4" Piece
    Board --> "*" Square
    Game --> DiceStrategy
    Game --> QuestionProvider
    QuestionService ..|> QuestionProvider
    QuestionService --> AuthService
    QuestionService --> QuestionRepository
    DiceStrategy <|.. NormalDice
    DiceStrategy <|.. BoostDice
    DiceStrategy <|.. UltraBoostDice
```

## 6. Error handling

- Illegal game actions throw `IllegalStateException` / `IllegalArgumentException` (tested).
- A damaged or tampered question file is detected on start; the user may restore the defaults.
- A damaged history line is skipped instead of losing the whole history.
- Any unexpected error is written to `error.log` and shown in a message instead of a silent crash.
