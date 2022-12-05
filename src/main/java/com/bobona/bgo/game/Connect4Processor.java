package com.bobona.bgo.game;

import com.bobona.bgo.game.utils.Position;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Connect4Processor extends GameProcessor {

    @Override
    public void initializeGame(Game game) {
        game.setGameType(2);
        for (int i = 0; i < 42; i++) {
            game.getSquares().add(0);
        }
    }

    @Override
    public List<Double> getInitialScoreboardValues() {
        return Collections.singletonList(1D); // 1 = red, 2 = yellow
    }

    @Override
    public int isMoveValid(Game game, GamePlayer gamePlayer, Move move) {
        int xPos = getPosition(move.getSquares().get(0)).getX();
        if (game.getSquares().get(xPos) == 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public List<Integer> getTeamSizes() {
        return Arrays.asList(1, 1);
    }

    @Override
    public String getGameName() {
        return "Connect 4";
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return new ArrayList<>();
    }

    @Override
    public void processMove(Game game, GamePlayer gamePlayer, Move move) {
        int xPos = getPosition(move.getSquares().get(0)).getX();
        int yPos;
        for (yPos = -1; yPos < 5; yPos++) {
            if (game.getSquares().get(xPos + 7 * (yPos + 1)) != 0) {
                break;
            }
        }
        if (yPos != -1) {
            game.getSquares().set(xPos + 7 * yPos, gamePlayer.getTeam());
            move.setSquares(Collections.singletonList(xPos + 7 * yPos));
        }
        game.getScoreboards().set(0, 3 - game.getScoreboards().get(0));
    }

    @Override
    public int hasGameEnded(Game game) {
        for (int i = 0; i < game.getSquares().size(); i++) {
            int team = game.getSquares().get(i);
            if (team == 0) {
                continue;
            }
            List<Position> directions = Arrays.asList(new Position(-1, 1), new Position(0, 1), new Position(1, 1), new Position(1, 0));
            for (Position direction : directions) {
                Position pointer = getPosition(i);
                boolean flipped = false;
                int adjacentCount = 1;
                do {
                    if (inBounds(pointer.add(direction)) && game.getSquare(pointer.add(direction), 7) == team) {
                        pointer = pointer.add(direction);
                        adjacentCount++;
                    } else if (!flipped) {
                        direction = direction.multiply(-1);
                        pointer = getPosition(i);
                        flipped = true;
                    }

                } while (inBounds(pointer.add(direction)) && game.getSquare(pointer.add(direction), 7) == team);
                if (adjacentCount == 4) {
                    return team;
                }
            }
        }
        if (getPossibleMoveContinuations(game, null, null).size() == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public boolean canPlayerMove(GamePlayer gamePlayer) {
        return gamePlayer.getTeam() == gamePlayer.getGame().getScoreboards().get(0);
    }

    @Override
    public List<Integer> getPossibleMoveContinuations(Game game, GamePlayer gamePlayer, Move move) {
        List<Integer> continuations = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (game.getSquares().get(i) == 0) {
                continuations.add(i);
            }
        }
        return continuations;
    }

    @Override
    public Position getDimensions() {
        return new Position(7, 6);
    }
}
