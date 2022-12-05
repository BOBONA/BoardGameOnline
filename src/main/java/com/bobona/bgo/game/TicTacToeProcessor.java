package com.bobona.bgo.game;

import com.bobona.bgo.game.utils.Position;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TicTacToeProcessor extends GameProcessor {

    // Scoreboard values: Current team (1 or 2), last timer record, timestamp of record
    // Values 1 and 2 are an example of how timers should be displayed from client perspective:
    // Take the last timer record and use the timestamp of record to update without the database actually changing
    @Override
    public void initializeGame(Game game) {
        game.setGameType(0);
        game.getSquares().clear();
        for (int i = 0; i < 9; i++) {
            game.getSquares().add(0);
        }
    }

    @Override
    public List<Double> getInitialScoreboardValues() {
        return Arrays.asList(1d, 0d, (double) System.currentTimeMillis());
    }

    @Override
    public int isMoveValid(Game game, GamePlayer gamePlayer, Move move) {
        if (game.getSquares().get(move.getSquares().get(0)) == 0) { // valid if the tile is empty
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean canPlayerMove(GamePlayer gamePlayer) {
        return gamePlayer.getTeam() == gamePlayer.getGame().getScoreboards().get(0);
    }

    @Override
    public List<Integer> getPossibleMoveContinuations(Game game, GamePlayer gamePlayer, Move move) {
        return Collections.emptyList();
    }

    @Override
    public Position getDimensions() {
        return new Position(3, 3);
    }

    @Override
    public List<Integer> getTeamSizes() {
        return Arrays.asList(1, 1);
    }

    @Override
    public String getGameName() {
        return "Tic Tac Toe";
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return new ArrayList<>();
    }

    @Override
    public void processMove(Game game, GamePlayer gamePlayer, Move move) {
        // set the tile to the current team
        double currentTeam = game.getScoreboards().get(0);
        game.getSquares().set(move.getSquares().get(0), (int) currentTeam);
        // change currentTeam from 1 to 2 or from 2 to 1
        game.getScoreboards().set(0, 3 - currentTeam);
        // update timer scoreboard data
        game.getScoreboards().set(1, game.getScoreboards().get(1) + System.currentTimeMillis() - game.getScoreboards().get(2));
        game.getScoreboards().set(2, (double) System.currentTimeMillis());
    }

    private final int[][] winPatterns = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    @Override
    public int hasGameEnded(Game game) {
        // check if the board matches any win pattern
        for (int[] winPattern : winPatterns) {
            long testValue = game.getSquares().get(winPattern[0]);
            if (testValue != 0 &&
                    testValue == game.getSquares().get(winPattern[1]) &&
                    testValue == game.getSquares().get(winPattern[2])) {
                return (int) testValue;
            }
        }
        // check if the board has any remaining spaces
        for (long tile : game.getSquares()) {
            if (tile == 0) {
                return -1; // game has not ended
            }
        }
        return 0; // game is a draw
    }
}
