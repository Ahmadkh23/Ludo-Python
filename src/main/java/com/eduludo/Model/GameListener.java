package com.eduludo.Model;

/** Observer pattern: receives every {@link GameEvent} fired by a {@link Game}. */
@FunctionalInterface
public interface GameListener {
    void onGameEvent(Game game, GameEvent event);
}

