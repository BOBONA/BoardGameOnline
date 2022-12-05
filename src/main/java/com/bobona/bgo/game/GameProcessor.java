package com.bobona.bgo.game;

import com.bobona.bgo.game.utils.Position;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;

import java.util.List;

public abstract class GameProcessor {

    public abstract void initializeGame(Game game);
    public abstract List<Double> getInitialScoreboardValues();
    public abstract int isMoveValid(Game game, GamePlayer gamePlayer, Move move); // returns 0 if the move is invalid, 1 if it is valid, and 2 if it is incomplete
    public abstract List<Integer> getTeamSizes();
    public abstract String getGameName();
    public abstract List<Class<?>> getInputTypes();
    public abstract void processMove(Game game, GamePlayer gamePlayer, Move move); // assumes that isMoveValid == true and canPlayerMove == true
    public abstract int hasGameEnded(Game game); // returns -1 if the game hasn't ended or the team id if it has (0 means a tie)
    public abstract boolean canPlayerMove(GamePlayer gamePlayer);
    public abstract List<Integer> getPossibleMoveContinuations(Game game, GamePlayer gamePlayer, Move move); // same assumption as processMove
    public abstract Position getDimensions();

    public boolean inBounds(Position position) {
        return position.getX() >= 0 && position.getX() <= getDimensions().getX() - 1 &&
                position.getY() >= 0 && position.getY() <= getDimensions().getY() - 1;
    }

    public Position getPosition(int index) {
        return new Position(index % getDimensions().getX(), index / getDimensions().getX());
    }
}
