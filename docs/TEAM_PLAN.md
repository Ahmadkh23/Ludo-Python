# Team plan (4 members)

The course checks that **every member commits to GitHub every week**. Each member owns one area of the code,
reviews another member's pull requests, and writes the tests for their own area.

## Ownership

| Member | Area | Main files | Tests they own |
|---|---|---|---|
| **A – Rules & model** | game engine, dice, blocks, captures, 6-rule, surprises, computer player | `Model/Game.java`, `Model/Surprise.java`, `Model/dice/*`, `Model/ai/*` | `GameRulesTest`, `ComputerPlayerTest` |
| **B – Board & game screen** | board geometry for the 3 levels, drawing, animation, dice view, game screen | `Model/BoardFactory.java`, `Model/Board.java`, `View/BoardCanvas.java`, `View/DiceView.java`, `View/GamePanel.java` | `BoardFactoryTest` |
| **C – Questions & security** | question bank (content!), admin wizard, login, encryption | `questions-default.json`, `service/*`, `security/*`, `View/AdminPanel.java`, `View/QuestionEditorDialog.java`, `View/LoginDialog.java`, `Controller/AdminController.java`, `SECURITY.md` | `SecurityTest`, `QuestionServiceTest` |
| **D – Flow, data & delivery** | start/setup/history screens, game controller, history storage, build scripts, release, docs | `Controller/AppController.java`, `Controller/GameController.java`, `View/StartPanel.java`, `View/SetupPanel.java`, `View/HistoryPanel.java`, `persistence/*`, `build.bat`, `README.md` | `PersistenceTest` |

## Sprints

| Sprint | Goal | Done when |
|---|---|---|
| 1 | Easy level playable: board, normal die, enter/move/capture/blocks/6-rule, runnable JAR | tests green, `dist/EduLudo.jar` runs on two PCs |
| 2 | Questions: bank of 30+, question squares, admin wizard with login | admin can add/edit/delete; players can't open it |
| 3 | Medium & Hard: rings, arrows, Boost/UltraBoost, surprises | square counts match the PDF; all surprise effects tested |
| 4 | History (course schema), timer, game over, polish, `.exe`, documentation | history saved with ranking/time/level |
| 5 | Bonus: computer players, animations, statistics, extra questions | demo for the competition |

The current code already covers sprints 1–5. In each sprint, the owners **review, improve and extend their own
area** (e.g. more questions, better computer strategy, the official history schema, the exact board images),
so that every member has real weekly commits.

## GitHub workflow

1. One repository; `main` always builds (`build.bat` passes).
2. Every task is a GitHub **Issue**, on a **Project board** (To do / In progress / Review / Done), labelled with the sprint.
3. Work on a branch per task: `feature/<issue-number>-short-name`.
4. Open a **Pull Request**; another member reviews it; merge only when `build.bat` passes.
5. Commit messages say *what* and *why*, e.g. `Add double-block check when entering from the castle (#12)`.
6. At the end of each sprint: tag a release (`v0.1`, `v0.2`, …) and attach `EduLudo.jar` and the zipped `EduLudo` folder (exe).
7. Before submitting, **test the runnable file on at least two different computers** (the course requires it).

## Before every submission – checklist

- [ ] `build.bat` → "Tests run: N, passed: N, failed: 0"
- [ ] `java -jar dist/EduLudo.jar` starts on PC 1 and PC 2
- [ ] `build.bat exe` → `dist/EduLudo/EduLudo.exe` starts on a PC **without** Java
- [ ] Play one short game on each level; the result appears in Game history
- [ ] Question manager can't be opened without the password
- [ ] Every member has commits this week
- [ ] README / REQUIREMENTS updated
