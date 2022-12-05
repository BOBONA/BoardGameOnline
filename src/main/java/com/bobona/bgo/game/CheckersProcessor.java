package com.bobona.bgo.game;

import com.bobona.bgo.game.utils.Position;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;

import java.util.*;

public class CheckersProcessor extends GameProcessor {

    private final Map<Move, List<Integer>> cachedMoveContinuations;

    public CheckersProcessor() {
        cachedMoveContinuations = new HashMap<>();
    }

    // Scoreboard values: Current team (1 or 2), white pieces taken, black pieces taken
    @Override
    public List<Double> getInitialScoreboardValues() {
        return Arrays.asList(1D, 0D, 0D);
    }

    // 0 = blank, 1 = white, 2 = black, 3 = white king, 4 = black king
    // evens tiles white, odds are black
    // white pieces go in positive direction, black pieces in negative
    @Override
    public void initializeGame(Game game) {
        game.setGameType(1);
        game.getSquares().clear();
        game.setTiles(Arrays.asList(
                0, 1, 0, 1, 0, 1, 0, 1,
                1, 0, 1, 0, 1, 0, 1, 0,
                0, 1, 0, 1, 0, 1, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                2, 0, 2, 0, 2, 0, 2, 0,
                0, 2, 0, 2, 0, 2, 0, 2,
                2, 0, 2, 0, 2, 0, 2, 0
        ));
    }

    @Override
    public int isMoveValid(Game game, GamePlayer gamePlayer, Move move) {
        List<Integer> inputs = move.getSquares();
        // Fails if there isn't a piece on the tile that the player controls
        if (game.getSquares().get(inputs.get(0)) != gamePlayer.getTeam() && game.getSquares().get(inputs.get(0)) - 2 != gamePlayer.getTeam()) {
            return 0;
        }
        if (inputs.size() < 2) {
            return 2;
        }
        for (int i = 0; i < inputs.size() - 1; i++) {
            Move subMove = new Move(move.getTimestamp(), inputs.subList(0, i + 1), move.getInputs());
            if (!getPossibleMoveContinuations(new Game(game), gamePlayer, subMove).contains(inputs.get(i + 1))
                    || (inputs.size() > 2 && subMove.getInputs().size() > 1 && !isCaptureMove(subMove.getSquares().get(0), subMove.getSquares().get(1)))) {
                return 0;
            }
        }
        return 1;
    }

    @Override
    public List<Integer> getTeamSizes() {
        return Arrays.asList(1, 1);
    }

    @Override
    public String getGameName() {
        return "Checkers";
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return new ArrayList<>();
    }

    @Override
    public void processMove(Game game, GamePlayer gamePlayer, Move move) {
        Integer pieceType = game.getSquares().get(move.getSquares().get(0));
        for (int i = 1; i < move.getSquares().size(); i++) {
            int first = move.getSquares().get(i - 1);
            int second = move.getSquares().get(i);
            // move piece
            game.getSquares().set(first, 0);
            game.getSquares().set(second, pieceType);
            // remove captured piece if the move captured any (using the difference between the two tiles part of the move)
            if (isCaptureMove(first, second)) {
                int captured = (first + second) / 2;
                int pieceTeam = getPieceTeam(game.getSquares().get(captured));
                game.getScoreboards().set(pieceTeam, game.getScoreboards().get(pieceTeam) + 1);
                game.getSquares().set(captured, 0);
            }
            // promote piece if necessary
            Position pos2 = getPosition(second);
            if ((pieceType == 1 && pos2.getY() == 7) || (pieceType == 2 && pos2.getY() == 0)) {
                game.getSquares().set(second, pieceType + 2);
            }
        }
        // switch current team
        game.getScoreboards().set(0, 3 - game.getScoreboards().get(0));
    }

    @Override
    public int hasGameEnded(Game game) {
        int currentTeam = game.getScoreboards().get(0).intValue();
        // check if a team has no pieces left
        boolean blackRemains = false;
        boolean whiteRemains = false;
        for (long square : game.getSquares()) {
            if (getPieceTeam((int) square) == 1) {
                whiteRemains = true;
            } else if (getPieceTeam((int) square) == 2) {
                blackRemains = true;
            }
        }
        if (whiteRemains && !blackRemains) {
            return 1;
        } else if (blackRemains && !whiteRemains) {
            return 2;
        } else if (!whiteRemains) {
            return 0; // btw this should never happen
        }
        // check if the current player cannot move (since this is less likely and for neatness, I used a second loop)
        boolean canMove = false;
        GamePlayer player = new GamePlayer(game, null, null);
        player.setTeam(currentTeam);
        for (int i = 0; i < game.getSquares().size(); i++) {
            long square = game.getSquares().get(i);
            if (getPieceTeam((int) square) == currentTeam) {
                Move move = new Move(new Date(), Collections.singletonList(i), new ArrayList<>());
                if (getPossibleMoveContinuations(game, player, move).size() > 0) {
                    canMove = true;
                    break;
                }
            }
        }
        if (canMove) {
            return -1;
        } else {
            return 2 - currentTeam;
        }
    }

    @Override
    public boolean canPlayerMove(GamePlayer gamePlayer) {
        return gamePlayer.getTeam() == gamePlayer.getGame().getScoreboards().get(0);
    }

    @Override
    public List<Integer> getPossibleMoveContinuations(Game game, GamePlayer gamePlayer, Move move) {
//        if (cachedMoveContinuations.containsKey(move)) {
//            return cachedMoveContinuations.get(move);
//        }
        List<Integer> continuations = new ArrayList<>();
        Game gameCopy = new Game(game);
        processMove(gameCopy, gamePlayer, move);
        // return nothing if the move wasn't a capture move
        if (move.getSquares().size() >= 2 &&
                !isCaptureMove(
                        move.getSquares().get(move.getSquares().size() - 2),
                        move.getSquares().get(move.getSquares().size() - 1))) {
            return continuations;
        }
        int value = move.getSquares().get(move.getSquares().size() - 1);
        Position pos = getPosition(value);
        Integer pieceType = gameCopy.getSquares().get(value);
        // create an array of every direction that the piece can go in
        List<Position> directions = new ArrayList<>();
        if (pieceType != 2) {
            directions.addAll(Arrays.asList(new Position(-1, 1), new Position(1, 1)));
        }
        if (pieceType != 1) {
            directions.addAll(Arrays.asList(new Position(1, -1), new Position(-1, -1)));
        }
        for (Position direction : directions) {
            Position basic = pos.add(direction);
            if (!inBounds(basic)) {
                continue;
            }
            Position capture = pos.add(direction.multiply(2));
            // allow the move if nothing is on the tile that it is moving to and it is the only part of the move
            // (you can't move twice or move after capturing)
            if (gameCopy.getSquare(basic, 8) == 0 && move.getSquares().size() < 2) {
                continuations.add(basic.unwrap(8));
                // allow a capture move if there is an enemy piece in the way
            } else if (
                    inBounds(capture) &&
                            gameCopy.getSquare(capture, 8) == 0 &&
                            3 - getPieceTeam(pieceType) == getPieceTeam(gameCopy.getSquare(basic, 8))) { // needs to be enemy piece
                continuations.add(capture.unwrap(8));
            }
        }
        cachedMoveContinuations.put(move, continuations);
        return continuations;
    }

    @Override
    public Position getDimensions() {
        return new Position(8, 8);
    }

    private int getPieceTeam(Integer type) {
        if (type == 1 || type == 3) {
            return 1;
        } else if (type == 2 || type == 4) {
            return 2;
        } else {
            return 0;
        }
    }

    private boolean isCaptureMove(int a, int b) {
        Position posA = getPosition(a);
        Position posB = getPosition(b);
        Position difference = posA.multiply(-1).add(posB);
        return difference.getX() % 2 == 0;
    }
}
