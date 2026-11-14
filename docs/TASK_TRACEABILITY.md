# Task traceability - submitted SDP pages 5-7

Requirement IDs follow the submitted SDP. Each task has an executor and an independent reviewer.

| Task | SRS | Scope | Executor | Reviewer | Implementation | Evidence |
|---|---|---|---|---|---|---|
| T1.1 | FR-1.1, FR-1.2, NFR-03 | Entry point / MVC / menu | Ahmad Khawalid | Kasem Hilf | App.java; Controller/AppController; View/StartPanel | Iteration1Check menu callbacks; native launch on second computer |
| T1.2 | FR-2.1, FR-2.2 | Setup validation and handoff | Kasem Hilf | Mohamad Amara | Model/Game.Builder; View/SetupPanel | GameRulesTest.setupRulesAreEnforced; CombinedValidationTest; Iteration1Check setup validation |
| T1.3 | FR-3.1, FR-3.2, NFR-01 | Board/cell/token model and initial state | Mohamad Amara | Omar Ahmad | Model/Board, BoardFactory, Cell, Player, Piece | BoardFactoryTest and GameRulesTest initial-state scenarios |
| T1.4 | FR-3.1, FR-3.2, NFR-03 | Game screen / player and turn indicators | Omar Ahmad | Ahmad Khawalid | View/GamePanel, BoardCanvas | Iteration1Check screen indicators and refresh; screenshots at both resolutions |
| T1.5 | FR-4.1, FR-4.2 | Dice operation and roll-phase restriction | Ahmad Khawalid | Omar Ahmad | Model/Game.roll | GameRulesTest.actionsAreRejectedInTheWrongPhase; Iteration1Check double-roll and controller-roll checks |
| T1.6 | FR-5.1, FR-5.2 | Legal token selection and movement | Mohamad Amara | Kasem Hilf | Model/Game.choose/options; BoardCanvas | GameRulesTest block/entry/movement cases; Iteration1Check controller piece selection |
| T1.7 | FR-6.1, FR-6.2 | Initial rules and turn transitions | Kasem Hilf | Ahmad Khawalid | Model/Game phase/turn transitions | GameRulesTest entry, extra-turn and third-six scenarios; Iteration1Check turn advancement |
| T1.8 | FR-1.1, FR-2.1, NFR-03 | Menu/setup/board navigation | Omar Ahmad | Mohamad Amara | Controller/AppController; panel action interfaces | Iteration1Check menu/setup callbacks; native menu-to-setup switch |
| T1.9 | FR-5.1, FR-6.1, NFR-10 | Focused automated checks | Ahmad Khawalid | Kasem Hilf | GameRulesTest / Iteration1RegressionTest; Iteration1Check; testing/TestRunner | 134 tests passed; custom runner; no coverage percentage claim |
| T1.10 | FR-3.1, FR-5.1, NFR-03 | Controller-driven view refresh | Omar Ahmad | Mohamad Amara | Controller/GameController; GamePanel.refresh | Iteration1Check controller selection and view update; screenshot state |
| T1.11 | FR-1.2, NFR-09, NFR-10 | Build/run and repository workflow | Kasem Hilf | Ahmad Khawalid | build.sh/build.bat; README | Clean build from the extracted ZIP on computer 2 (first JAR built on computer 1); test-results/second-computer-run.txt |
| T1.12 | FR-3.1, FR-5.1, FR-6.2, NFR-10 | Cross-layer integration and defect record | Mohamad Amara | Omar Ahmad | GameController; Iteration1Check | Cross-layer roll/selection tests passed |
| T1.13 | FR-1.1, FR-2.1, FR-3.1, NFR-10 | Acceptance walkthrough and DoD evidence | Omar Ahmad | Ahmad Khawalid | ACCEPTANCE_CHECKLIST.md; this matrix | Second-computer run done; team walkthrough and sign-off in progress |
