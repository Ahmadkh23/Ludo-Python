package com.eduludo.Model;

import com.eduludo.Model.dice.DiceStrategy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/** Test double: a die that returns pre-programmed values in order. */
final class ScriptedDice implements DiceStrategy {

    private final Deque<Integer> values = new ArrayDeque<>();
    private final int faces;
    private final String name;

    ScriptedDice(int faces, String name) {
        this.faces = faces;
        this.name = name;
    }

    ScriptedDice then(int... next) {
        for (int v : next) {
            values.addLast(v);
        }
        return this;
    }

    @Override
    public int roll(Random rng) {
        if (values.isEmpty()) {
            throw new IllegalStateException("ScriptedDice " + name + " ran out of values");
        }
        return values.removeFirst();
    }

    @Override
    public int faces() {
        return faces;
    }

    @Override
    public String name() {
        return name;
    }
}

