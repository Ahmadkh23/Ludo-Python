# EduLudo – Educational Ludo for Software Engineering Students

EduLudo is classic Ludo combined with multiple-choice questions from the course material
(Agile, Scrum, MVC, Design Patterns, QA, software design, version control).
It is written in **Java 17+ with Swing** and needs **no external libraries and no internet**.

| Document | What it covers |
|---|---|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Every requirement of the assignment, where it is implemented, and the test that checks it |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | MVC layers, design patterns, class diagram, turn flow |
| [SECURITY.md](SECURITY.md) | How the question manager and the answers are protected, and why |
| [docs/TEAM_PLAN.md](docs/TEAM_PLAN.md) | Work split for 4 members, sprint plan, GitHub workflow |
| [Visual asset credits](src/main/resources/com/eduludo/assets/ASSET_CREDITS.txt) | CC0 sources for the tabletop texture and board-game icons |

---

## Run the game

**Option 1: Windows, no Java needed.** Run `build.bat exe` once, then copy the whole `dist\EduLudo` folder to any
Windows PC and double-click `EduLudo.exe`. Java is bundled inside the folder.

**Option 2: any PC with Java 17 or newer (Windows, macOS, Linux).**

```bash
java -jar dist/EduLudo.jar
```

On Windows you can also double-click `EduLudo.jar`, or run `run.bat`.

> Check your Java version with `java -version`. It must be 17 or higher.

## Build from source

You only need a JDK 17+ (it includes `javac`, `jar` and `jpackage`). No Maven or Gradle download is required.

| Command | Result |
|---|---|
| `build.bat` (Windows) / `./build.sh` (macOS, Linux, Git Bash) | compiles, runs **all tests**, creates `dist/EduLudo.jar` |
| `build.bat exe` | the above + `dist/EduLudo/EduLudo.exe` with a bundled Java runtime |

The build stops and **no JAR is produced if any test fails**, so a broken version can't be submitted by mistake.

## How to play (short)

1. **New game** → choose 2–4 players, a level (Easy / Medium / Hard), a name and a unique colour for each player.
   A seat can optionally be played by the computer.
2. On your turn press **Roll dice**, then click one of your highlighted pieces.
3. A piece leaves the castle only with a **6**. A 6 gives another roll; the third 6 in a row is cancelled.
4. Land on a single opponent piece on an unprotected square to send it home. Two of your pieces on one square
   form a **Double Block** that nobody can pass.
5. **?** squares ask a course question (+1/+2/+3 for a right answer, −1/−2/−3 for a wrong one).
   **!** squares are surprises. **Arrows** in your colour lead you to the next inner track.
6. The game ends when all players but one have finished. The ranking, the time and the level are saved to the
   **Game history**.

The full rules are in the game under **How to play**.

## Question manager (admin)

The **Question manager** shows the answers, so it is protected by an admin password.
The **first time** it is opened, the admin creates the password (at least 8 characters, letters and digits).
After that it asks for the password. 5 wrong attempts lock the login for 60 seconds. See [SECURITY.md](SECURITY.md).

## Where the data is saved

All data is saved in `<your home folder>/.eduludo/` (always writable, also when the game is installed in Program Files):

| File | Content |
|---|---|
| `questions.dat` | question bank, encrypted with AES-GCM (created from the 42 built-in questions on first start) |
| `history.csv` | finished games in the course CSV format (opens in Excel). Started from the lecturer's CSV on first run. |
| `admin.properties` | salted PBKDF2 hash of the admin password (never the password itself) |
| `error.log` | written only if an unexpected error happens |

To use another folder: `java -Deduludo.data=C:\some\folder -jar EduLudo.jar`.

## Project structure

```
src/main/java/com/eduludo/
  App.java              entry point – wires everything together
  Model/                game rules (Model): Game, Board, BoardFactory, Piece, Player, Surprise, dice/, ai/
  Controller/           AppController, GameController, AdminController (Controller)
  View/                 Swing screens and dialogs (View)
  service/              QuestionService – the only gateway to the question bank
  security/             AuthService, PasswordHasher, CryptoBox, AdminSession
  persistence/          repositories (DAO): encrypted questions, CSV history, JSON
src/main/resources/     the 42 default questions + the lecturer's history CSV
src/test/java/          136 automated tests + a small test runner (no JUnit download needed)
```

## Credits

Written by the team for the course project. No third-party code or libraries are used; all graphics are drawn in code.

---

## הוראות הרצה (עברית)

- **הרצה ללא התקנת Java (Windows):** להריץ פעם אחת `build.bat exe`, להעתיק את כל התיקייה `dist\EduLudo`
  ולהפעיל את `EduLudo.exe`.
- **הרצה עם Java 17 ומעלה:** `java -jar dist/EduLudo.jar` (או לחיצה כפולה על הקובץ).
- **בנייה מהקוד:** `build.bat` – מקמפל, מריץ את כל הבדיקות האוטומטיות ויוצר את קובץ ה-JAR.
  אם בדיקה נכשלת, קובץ ההרצה לא נוצר.
- **אשף ניהול השאלות** מוגן בסיסמת מנהל שנוצרת בכניסה הראשונה. נימוק בחירת האבטחה: [SECURITY.md](SECURITY.md).
