package com.bobona.bgo.service;

import com.bobona.bgo.dao.GameDao;
import com.bobona.bgo.dao.GamePlayerDao;
import com.bobona.bgo.game.*;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;
import com.bobona.bgo.model.User;
import com.bobona.bgo.web.data.BasicGameData;
import com.bobona.bgo.web.data.BasicPlayerData;
import com.bobona.bgo.web.data.RawMove;
import com.bobona.bgo.web.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GameService {

    @Autowired
    private GameDao gameDao;
    @Autowired
    private GamePlayerDao gamePlayerDao;
    private final List<GameProcessor> processors;

    @Autowired
    public GameService() {
        processors = new ArrayList<>();
        processors.add(new TicTacToeProcessor());
        processors.add(new CheckersProcessor());
        processors.add(new Connect4Processor());
    }

    public Game createGame(User user, Game game, int gameType, boolean addUser) {
        game.getSquares().clear();
        game.getScoreboards().clear();
        processors.get(gameType).initializeGame(game);
        game.setCreator(user);
        game.setGameStarted(false);
        game.setTimestamp(Instant.now());
        game.setWinner(-1);
        gameDao.save(game);
        if (addUser) {
            addPlayer(game, user);
        }
        return game;
    }

    public void startGame(Game game) {
        GameProcessor processor = getGameProcessor(game);
        if (game.isGameStarted() || game.getGamePlayers().size() != getMaxPlayers(game.getGameType())) {
            throw new InvalidOperationException("This game has already started!");
        }
        game.setGameStarted(true);
        game.setTimestamp(Instant.now());
        // randomly add players to teams
        List<Integer> teamIds = IntStream.rangeClosed(1, processor.getTeamSizes().size())
                .boxed().collect(Collectors.toList());
        for (GamePlayer player : game.getGamePlayers()) {
            player.setTeam(-1);
            gamePlayerDao.save(player);
        }
        for (GamePlayer player : game.getGamePlayers()) {
            int teamIndex = ThreadLocalRandom.current().nextInt(0, teamIds.size());
            player.setTeam(teamIds.get(teamIndex));
            gamePlayerDao.save(player);
            // remove the team id from the list if the team is full
            if (gameDao.getTeam(game, teamIds.get(teamIndex)).size() == processor.getTeamSizes().get(teamIndex)) {
                teamIds.remove(teamIndex);
            }
        }
        game.setScoreboards(processor.getInitialScoreboardValues());
        gameDao.save(game);
    }

    public boolean canMove(Game game, GamePlayer gamePlayer) {
        if (gamePlayer == null || gamePlayer.getRole() == GamePlayer.Role.SPECTATOR) {
            return false;
        } else {
            return game.isGameStarted() &&
                    !game.isGameEnded() &&
                    getGameProcessor(game).canPlayerMove(gamePlayer);
        }
    }

    public int isMoveValid(Game game, GamePlayer gamePlayer, Move move) {
        return getGameProcessor(game).isMoveValid(game, gamePlayer, move);
    }

    // if modify game is false, the game object won't be updated in the database
    public void processMove(Game game, GamePlayer gamePlayer, Move move, boolean modifyGame) {
        GameProcessor processor = getGameProcessor(game);
        if (canMove(game, gamePlayer) && isMoveValid(game, gamePlayer, move) == 1) {
            processor.processMove(game, gamePlayer, move);
            if (modifyGame) {
                game.setWinner(processor.hasGameEnded(new Game(game)));
                gameDao.save(game);
            }
        } else {
            throw new InvalidOperationException("Invalid move!");
        }
    }

    public List<Integer> getMoveContinuations(Game game, GamePlayer gamePlayer, Move move) {
        return getGameProcessor(game).getPossibleMoveContinuations(game, gamePlayer, move);
    }

    public void addPlayer(Game game, User user) {
        if (game.getGamePlayers().size() >= getMaxPlayers(game.getGameType())) {
            throw new InvalidOperationException("Game is full");
        }
        if (getGamePlayer(user, game, true) != null) {
            throw new InvalidOperationException("You're already in this game!");
        }
        GamePlayer gamePlayer = new GamePlayer(game, user, game.getGamePlayers().size() == 0 ? GamePlayer.Role.CREATOR : GamePlayer.Role.PLAYER);
        game.getGamePlayers().add(gamePlayer);
        gamePlayerDao.save(gamePlayer);
        if (game.getGamePlayers().size() == getMaxPlayers(game.getGameType()) && game.shouldStartWhenFilled()) {
            startGame(game);
        }
    }

    // returns new admins id or null if the game was deleted when no players left
    public Long removePlayer(Game game, User user) {
        if (game.isGameStarted()) {
            throw new InvalidOperationException("Players cannot leave a game in progress");
        }
        GamePlayer gamePlayer = getGamePlayer(user, game, true);
        int index = -1;
        for (GamePlayer player : game.getGamePlayers()) {
            if (player.getId().equals(gamePlayer.getId())) {
                index = game.getGamePlayers().indexOf(player);
            }
        }
        if (game.getGamePlayers().size() == 0) {
            deleteGame(game);
            return null;
        } else {
            game.getGamePlayers().remove(index);
            Long newAdminId = null;
            if (gamePlayer.getRole().equals(GamePlayer.Role.CREATOR) && game.getGamePlayers().size() != 0) {
                int newAdminIndex = ThreadLocalRandom.current().nextInt(0, game.getGamePlayers().size());
                GamePlayer gamePlayer1 = game.getGamePlayers().get(newAdminIndex);
                gamePlayer1.setRole(GamePlayer.Role.CREATOR);
                gamePlayerDao.save(gamePlayer1);
                newAdminId = gamePlayer1.getUser().getId();
            }
            gameDao.save(game);
            gamePlayerDao.delete(gamePlayer);
            return newAdminId;
        }
    }

    public List<BasicPlayerData> getGamePlayerData(Game game) {
        game = getGame(game.getId()); // lazy intialization stuff, not sure how to easily fix this without this awkward line
        List<BasicPlayerData> playerData = new ArrayList<>();
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            playerData.add(new BasicPlayerData(gamePlayer.getUser().getUsername(), gamePlayer.getRole(), gamePlayer.getTeam()));
        }
        return playerData;
    }

    public List<BasicGameData> getBasicGameData(Set<Game> games) {
        return games.stream().map(this::getBasicGameData).collect(Collectors.toList());
    }

    public BasicGameData getBasicGameData(Game game) {
        return new BasicGameData(
                game.getId(),
                game.getGameType(),
                game.getName(),
                game.getCreator() == null ? "ME" : game.getCreator().getUsername(),
                game.isGameStarted(),
                game.isGameEnded(),
                game.getGamePlayers().size(),
                getMaxPlayers(game.getGameType()),
                game.isSpectatorsEnabled(),
                game.isSecured()
        );
    }

    // this function takes the raw string inputs obtained from the client and parses them into objects
    public Move parseRawMove(RawMove rawMove, int gameType) {
        GameProcessor processor = processors.get(gameType);
        Move move = new Move(rawMove);
        if (rawMove.getInputs() != null) {
            // i might be using classes wrong (especially when parsing lists). I don't actually need this functionality yet but i should test it
            move.setInputs(parseInputs(rawMove.getInputs(), processor.getInputTypes(), 0, rawMove.getInputs().size()));
        }
        return move;
    }

    private List<Object> parseInputs(List<String> inputs, List<Class<?>> types, Integer pointer, int length) {
        List<Object> parsedInputs = new ArrayList<>();
        int parsedTypeCount = 0;
        for (Class<?> type : types) {
            if (type.equals(String.class)) {
                parsedInputs.add(inputs.get(pointer));
            } else if (type.equals(Integer.class)) {
                parsedInputs.add(Integer.parseInt(inputs.get(pointer)));
            } else if (type.equals(List.class)) {
                // 4, a, b, c, d
                Class<?> listType = type.getTypeParameters()[0].getClass();
                int listLength = Integer.parseInt(inputs.get(pointer));
                pointer++;
                parseInputs(inputs, Collections.nCopies(listLength, listType), pointer, listLength);
                pointer--; // to cancel out the pointer++ at the end of the for loop here
            }
            parsedTypeCount++;
            if (parsedTypeCount > length) {
                break;
            }
            pointer++;
        }
        return parsedInputs;
    }

    public List<String> getGameNames() {
        List<String> gameNames = new ArrayList<>();
        for (GameProcessor processor : processors) {
            gameNames.add(processor.getGameName());
        }
        return gameNames;
    }

    private GameProcessor getGameProcessor(Game game) {
        return processors.get(game.getGameType());
    }

    public boolean gameExists(Long id) {
        return gameDao.existsById(id);
    }

    public Game getGame(Long id) {
        Optional<Game> game = gameDao.findById(id);
        return game.orElse(null);
    }

    public Set<Game> getGames() {
        return gameDao.findAll();
    }

    public Game getGame(String id) {
        return getGame(Long.parseLong(id));
    }

    public GamePlayer getGamePlayer(User user, Game game, boolean nullReturn) {
        Optional<GamePlayer> gamePlayer = gamePlayerDao.findByUserAndGame(user, game);
        if (nullReturn) {
            return gamePlayer.orElse(null);
        } else {
            GamePlayer alternate = new GamePlayer(game, user, GamePlayer.Role.SPECTATOR);
            alternate.setTeam(-1);
            return gamePlayer.orElse(alternate);
        }
    }

    public GamePlayer.Role getUserRole(User user, Game game) {
        Optional<GamePlayer> gamePlayer = gamePlayerDao.findByUserAndGame(user, game);
        if (gamePlayer.isPresent()) {
            return gamePlayer.get().getRole();
        } else {
            return GamePlayer.Role.SPECTATOR;
        }
    }

    // user can access the game if they are playing or if spectators are enabled
    public boolean canUserAccessGame(User user, Game game) {
        return !GamePlayer.Role.SPECTATOR.equals(getUserRole(user, game)) || (game != null && game.isSpectatorsEnabled());
    }

    // users can chat if they are part of the game or if spectator chat is enabled
    public boolean canUserChat(User user, Game game) {
        return !GamePlayer.Role.SPECTATOR.equals(getUserRole(user, game)) || game.canSpectatorsChat();
    }

    public boolean canUserPlay(User user, Game game) {
        return getUserRole(user, game) != GamePlayer.Role.SPECTATOR;
    }

    public int getMaxPlayers(int gameType) {
        GameProcessor processor = processors.get(gameType);
        return processor.getTeamSizes().stream().reduce(Integer::sum).orElse(0);
    }

    public void deleteGame(Game game) {
        List<GamePlayer> gamePlayers = new ArrayList<>(game.getGamePlayers());
        game.getGamePlayers().clear();
        gameDao.save(game);
        for (GamePlayer gamePlayer : gamePlayers) {
            gamePlayerDao.delete(gamePlayer);
        }
        gameDao.deleteById(game.getId());
    }
}
