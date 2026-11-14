# Acceptance checklist - Iteration 1, group python

## Automated verification
- [x] Clean build from the extracted ZIP; 134/134 automated tests pass.
- [x] 9/9 Swing interaction checks pass (`sh verify-ui.sh`).
- [x] Submitted JAR matches the source build.

## Second computer (required by Exercise 4)
- [x] The first `dist/EduLudo.jar` (built on computer 1, JDK 25) run on a second computer with a fresh data folder.
- [x] Main menu opens; "Start a new game" opens the setup screen.
- [x] Data folder is created and written (questions.dat).
- [x] Later rebuilds (after review fixes) made and re-verified on the second computer: clean build, tests, UI checks, native launch.

Tester / date / OS / Java / result: second machine, 08/11/2026, macOS 26.6.2 arm64, Java 21.0.7, **PASS**. Details: `test-results/second-computer-run.txt`.

## Team walkthrough (before submission)
- [ ] Run the final `dist/EduLudo.jar` on another team computer (ideally Windows, `run.bat`).
- [ ] Launch the JAR, configure players, reject blank/duplicate names and duplicate colours.
- [ ] Start an Easy game, roll, choose a highlighted piece, check the board and current player.
- [ ] Check menu/setup/back/quit navigation.
- [ ] Finish a match, open history and inspect the saved CSV.
- [ ] Reviewers listed in docs/TASK_TRACEABILITY.md confirm their tasks.
- [ ] Product Owner accepts the increment; Scrum Master updates T1.13 in the Scrum report.
