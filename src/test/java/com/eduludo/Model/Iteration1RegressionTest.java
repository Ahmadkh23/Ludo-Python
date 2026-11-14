package com.eduludo.Model;

import com.eduludo.testing.Test;
import java.util.List;
import java.util.Random;
import static com.eduludo.testing.Assert.*;

/** Regression checks derived from the course brief and lecturer answers. */
public class Iteration1RegressionTest {
    private Game game(Board board, ScriptedDice normal, ScriptedDice boost, Random random) {
        return Game.builder().board(board).normalDie(normal).multiplierDie(boost).random(random)
                .questions(level -> new Question(999, level, "Architecture", "Test?", List.of("A","B","C","D"),0))
                .addPlayer("Alice",PlayerColor.RED,false).addPlayer("Bob",PlayerColor.GREEN,false).build();
    }
    @Test void homePathUsesNormalDieEvenWhenBoostIsZero() {
        Board b=BoardFactory.createLayout(Difficulty.MEDIUM);
        Game g=game(b,new ScriptedDice(6,"Normal").then(2),new ScriptedDice(3,"Boost").then(1),new Random(1));
        Piece p=g.players().get(0).piece(1);p.setPosition(b.finishIndex()-3);
        g.roll();assertEquals(1,g.options().size(),"home move remains possible with zero multiplier");
        assertEquals(b.finishIndex()-1,g.options().get(0).target(),"normal die moves two");
    }
    @Test void homePathIgnoresLargeMultiplier() {
        Board b=BoardFactory.createLayout(Difficulty.HARD);
        Game g=game(b,new ScriptedDice(6,"Normal").then(3),new ScriptedDice(6,"UltraBoost").then(6),new Random(1));
        Piece p=g.players().get(0).piece(1);p.setPosition(b.finishIndex()-3);
        g.roll();assertEquals(b.finishIndex(),g.options().get(0).target(),"normal die reaches home exactly");
        g.choose(g.options().get(0));assertTrue(p.isFinished(),"piece finishes");
    }
    @Test void answeringQuestionCanCaptureAndTriggerAnotherQuestion() {
        Board b=BoardFactory.createLayout(Difficulty.EASY);
        b.square(b.path(PlayerColor.RED).get(10)).makeQuestion(QuestionLevel.EASY);
        b.square(b.path(PlayerColor.RED).get(11)).makeQuestion(QuestionLevel.MEDIUM);
        Game g=game(b,new ScriptedDice(6,"Normal").then(1),null,new Random(1));
        Piece p=g.players().get(0).piece(1);p.setPosition(9);
        Piece victim=g.players().get(1).piece(1);
        victim.setPosition(b.path(PlayerColor.GREEN).indexOf(b.path(PlayerColor.RED).get(11)));
        g.roll();g.chooseMoveFor(p);g.answer(0);
        assertTrue(victim.isInCastle(),"capture on question-driven arrival");
        assertEquals(Phase.ANSWERING_QUESTION,g.phase(),"destination question is triggered");
        assertEquals(QuestionLevel.MEDIUM,g.pendingQuestion().level(),"new question level");
        assertEquals(g.players().get(0),g.currentPlayer(),"chain has not ended the turn");
        g.answer(0);assertEquals(g.players().get(1),g.currentPlayer(),"turn advances once after chain");
    }
    @Test void surpriseBackThreeCanTriggerAQuestion() {
        Board b=BoardFactory.createLayout(Difficulty.EASY);
        b.square(b.path(PlayerColor.RED).get(20)).makeSurprise();
        b.square(b.path(PlayerColor.RED).get(17)).makeQuestion(QuestionLevel.EASY);
        Random random=new Random(1) { private static final long serialVersionUID=1L;
            @Override public int nextInt(int bound){return Math.min(45,bound-1);} };
        Game g=game(b,new ScriptedDice(6,"Normal").then(4),null,random);
        Piece p=g.players().get(0).piece(1);p.setPosition(16);
        g.roll();g.chooseMoveFor(p);
        assertEquals(17,p.position(),"back three");assertEquals(Phase.ANSWERING_QUESTION,g.phase(),"question after surprise");
    }
    @Test void wrongAnswerCapturesOnBackwardLanding() {
        Board b=BoardFactory.createLayout(Difficulty.EASY);
        b.square(b.path(PlayerColor.RED).get(10)).makeQuestion(QuestionLevel.EASY);
        Game g=game(b,new ScriptedDice(6,"Normal").then(2),null,new Random(1));
        Piece p=g.players().get(0).piece(1);p.setPosition(8);
        Piece victim=g.players().get(1).piece(1);
        victim.setPosition(b.path(PlayerColor.GREEN).indexOf(b.path(PlayerColor.RED).get(9)));
        g.roll();g.chooseMoveFor(p);g.answer(1);
        assertEquals(9,p.position(),"one step backward");assertTrue(victim.isInCastle(),"backward capture");
    }
}
