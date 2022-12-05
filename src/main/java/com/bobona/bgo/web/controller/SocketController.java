package com.bobona.bgo.web.controller;

import com.bobona.bgo.game.Move;
import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.GamePlayer;
import com.bobona.bgo.model.User;
import com.bobona.bgo.service.GameService;
import com.bobona.bgo.service.UserService;
import com.bobona.bgo.web.data.*;
import com.bobona.bgo.web.exception.InvalidOperationException;
import com.bobona.bgo.web.socket.ActiveSessionRepository;
import com.bobona.bgo.web.socket.GameSession;
import com.bobona.bgo.web.socket.SocketMessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class SocketController {

    @Autowired
    private GameService gameService;
    @Autowired
    private UserService userService;
    @Autowired
    private ActiveSessionRepository sessionRepository;
    @Autowired
    private SocketMessagingService messager;

    @EventListener
    private void sessionConnected(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        java.lang.String destination = headers.getDestination();
        PathMatcher parser = new AntPathMatcher();
        if (destination.equals(SocketMessagingService.HOME_SUBSCRIPTION)) {
            sessionRepository.setSessionData(headers.getSessionId(), -1L, headers.getUser().getName());
        } else if (parser.match(SocketMessagingService.GAME_SUBSCRIPTION_PATTERN, destination)) {
            Long gameId = Long.parseLong(parser.extractUriTemplateVariables(SocketMessagingService.GAME_SUBSCRIPTION_PATTERN, destination).get("gameId"));
            sessionRepository.setSessionData(headers.getSessionId(), gameId, headers.getUser().getName());
            // send fetch_players response
            messager.sendToGame(gameId, fetchPlayersResponse(gameService.getGame(gameId)));
            messager.sendStatusMessage(gameId, headers.getUser().getName() + " connected");
        }
    }

    @EventListener
    private void sessionDisconnected(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (sessionRepository.hasSession(headers.getSessionId())) {
            GameSession session = sessionRepository.getSession(headers.getSessionId());
            if (session.isGameSession()) {
                Game game = gameService.getGame(session.getGameId());
                if (game != null && (!game.isGameStarted() || game.isGameEnded())) {
                    if (sessionRepository.getConnectedPlayers(session.getGameId()).size() == 1) { // only the person about to disconnect is connected
                        gameService.deleteGame(game);
                        SocketHomeResponse response = new SocketHomeResponse(SocketHomeRequest.RequestType.GAME_REMOVED);
                        response.game = new BasicGameData(game.getId());
                        messager.sendToAll(response);
                    }
                } else {
                    messager.sendToGame(game.getId(), fetchPlayersResponse(game));
                    messager.sendStatusMessage(session.getGameId(), headers.getUser().getName() + " disconnected");
                }
            }
            sessionRepository.removeSession(headers.getSessionId());
        }
    }

    @MessageMapping(SocketMessagingService.HOME_SUBSCRIPTION)
    public void processHomeRequest(SocketHomeRequest request, Principal principal) {
        SocketHomeResponse response = new SocketHomeResponse();
        response.type = request.getType();
        User user = userService.findUserByUsername(principal.getName());
        if (request.getType().equals(SocketHomeRequest.RequestType.GET_GAMES)) {
            List<BasicGameData> unsorted = new ArrayList<>();
            for (Game game : gameService.getGames()) {
                BasicGameData gameData = gameService.getBasicGameData(game);
                gameData.joined = gameService.getGamePlayer(user, game, true) != null;
                unsorted.add(gameData);
            }
            unsorted.sort(Comparator.comparing(x -> x.gameId));
            response.allGames = unsorted;
            messager.sendToUser(user.getId(), response);
        } else if (request.getType().equals(SocketHomeRequest.RequestType.CHAT_MESSAGE)) {
            response.message = new SocketChatMessage(principal.getName(), request.getChatMessage());
            messager.sendToAll(response);
        }
    }
    
    @MessageMapping(SocketMessagingService.GAME_SUBSCRIPTION_PATTERN)
    public void processGameRequest(@DestinationVariable Long gameId, SocketGameRequest request, Principal principal, SimpMessageHeaderAccessor simpHeaderAccessor) {
        SocketGameResponse response = new SocketGameResponse();
        User user = userService.findUserByUsername(principal.getName());
        Game game = gameService.getGame(gameId);
        GamePlayer gamePlayer = gameService.getGamePlayer(user, game, false);
        response.responseType = request.getType();
        if (request.getType().equals(SocketGameRequest.RequestType.UPDATE)) {
            setResponsesGameData(response, game, gamePlayer);
            messager.sendToPlayer(gameId, user.getId(), response);
            setResponsesRoleChangeData(response, gamePlayer);
            messager.sendToPlayer(gameId, user.getId(), response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.START)) {
            if (gamePlayer.getRole() != GamePlayer.Role.CREATOR) {
                throw new InvalidOperationException("You are not allowed to do this");
            }
            gameService.startGame(game);
            setResponsesRoleChangeData(response, gamePlayer);
            messager.sendToGame(gameId, response); // send role changes
            response.responseType = SocketGameRequest.RequestType.REQUEST_UPDATE;
            messager.sendToGame(gameId, response); // send game update
            messager.sendGameStatusUpdate(gameService.getBasicGameData(game));
        } else if (request.getType().equals(SocketGameRequest.RequestType.CONTINUATION_PREVIEW)) {
            if (!game.isGameStarted() || game.isGameEnded()) {
                throw new InvalidOperationException("Game is not in session");
            }
            Move parsedMove = gameService.parseRawMove(request.getMove(), game.getGameType());
            response.moveIsValid = gameService.isMoveValid(game, gamePlayer, parsedMove);
            if (response.moveIsValid != 0) {
                response.continuations = gameService.getMoveContinuations(game, gamePlayer, parsedMove);
                if (response.moveIsValid == 1) { // only preview if move is complete
                    gameService.processMove(game, gamePlayer, parsedMove, false);
                }
            }
            setResponsesGameData(response, game, gamePlayer);
            messager.sendToPlayer(gameId, user.getId(), response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.MOVE)) {
            if (gamePlayer.getRole() == GamePlayer.Role.SPECTATOR) {
                throw new InvalidOperationException("Spectators can't move! Why are you messing with the code!?!");
            }
            Move move = gameService.parseRawMove(request.getMove(), game.getGameType());
            gameService.processMove(game, gamePlayer, move, true);
            response.lastMove = request.getMove();
            response.lastMove.setSquares(move.getSquares()); // to enable process move to change the move output
            messager.sendToGame(gameId, response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.PREVIEW)) {
            gameService.processMove(game, gamePlayer, gameService.parseRawMove(request.getMove(), game.getGameType()), false);
            setResponsesGameData(response, game, gamePlayer);
            messager.sendToPlayer(gameId, user.getId(), response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.VALIDATE)) {
            if (!game.isGameStarted() || game.isGameEnded()) {
                throw new InvalidOperationException("Game is not in session");
            }
            response.moveIsValid = gameService.isMoveValid(game, gamePlayer, gameService.parseRawMove(request.getMove(), game.getGameType()));
            messager.sendToPlayer(gameId, user.getId(), response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.CHAT)) {
            if (gamePlayer.getRole().equals(GamePlayer.Role.SPECTATOR) && !game.canSpectatorsChat()) {
                throw new InvalidOperationException("Spectators can't chat!");
            }
            response.message = new SocketChatMessage(user.getUsername(), request.getChatMessage());
            messager.sendToGame(gameId, response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.FETCH_PLAYERS)) {
            messager.sendToPlayer(gameId, user.getId(), fetchPlayersResponse(game));
        } else if (request.getType().equals(SocketGameRequest.RequestType.LEAVE)) {
            if (gamePlayer.getRole().equals(GamePlayer.Role.SPECTATOR)) {
                throw new InvalidOperationException("You're not in the game dummy");
            }
            Long newAdminId = gameService.removePlayer(game, user);
            if (newAdminId == null) { // no one is left in the game
                // this already happens when the session disconnects. Can I rely on sessions?
//                SocketHomeResponse response1 = new SocketHomeResponse(SocketHomeRequest.RequestType.GAME_REMOVED);
//                response1.game = new BasicGameData(gameId);
//                messager.sendToAll(response1);
            } else {
                messager.sendStatusMessage(gameId, user.getUsername() + " left the game.");
                messager.sendGameStatusUpdate(gameService.getBasicGameData(game));
                SocketGameResponse response1 = new SocketGameResponse();
                response1.responseType = SocketGameRequest.RequestType.ROLE_DATA;
                response1.role = GamePlayer.Role.CREATOR;
                messager.sendToPlayer(gameId, newAdminId, response1);
            }
            try {
                sessionRepository.getSession(simpHeaderAccessor.getSessionId()).getSession().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (request.getType().equals(SocketGameRequest.RequestType.ROLE_DATA)) {
            setResponsesRoleChangeData(response, gamePlayer);
            messager.sendToPlayer(gameId, user.getId(), response);
        } else if (request.getType().equals(SocketGameRequest.RequestType.NEW_GAME)) {
            if (gamePlayer.getRole().equals(GamePlayer.Role.CREATOR) && game.isGameEnded()) {
                gameService.createGame(user, game, game.getGameType(), false);
            }
            setResponsesRoleChangeData(response, gamePlayer);
            messager.sendToGame(gameId, response);
            response = new SocketGameResponse();
            response.responseType = SocketGameRequest.RequestType.REQUEST_UPDATE;
            messager.sendToGame(gameId, response);
        }
    }

    private SocketGameResponse fetchPlayersResponse(Game game) {
        SocketGameResponse response = new SocketGameResponse();
        response.responseType = SocketGameRequest.RequestType.FETCH_PLAYERS;
        response.connectedPlayers = new ArrayList<>();
        response.disconnectedPlayers = new ArrayList<>();
        for (GamePlayer gp : game.getGamePlayers()) {
            BasicPlayerData playerData = new BasicPlayerData(gp.getUser().getUsername(), gp.getRole(), gp.getTeam());
            if (sessionRepository.getConnectedPlayers(game.getId()).contains(playerData.name)) {
                response.connectedPlayers.add(playerData);
            } else {
                response.disconnectedPlayers.add(playerData);
            }
        }
        return response;
    }

    private void setResponsesGameData(SocketGameResponse response, Game game, GamePlayer gamePlayer) {
        response.gameStarted = game.isGameStarted();
        response.scoreboards = game.getScoreboards();
        response.tiles = game.getSquares();
        response.maxPlayers = gameService.getMaxPlayers(game.getGameType());
        response.winner = game.getWinner();
        response.gameType = game.getGameType();
        response.canMove = gameService.canMove(game, gamePlayer);
    }

    private void setResponsesRoleChangeData(SocketGameResponse response, GamePlayer gamePlayer) {
        response.responseType = SocketGameRequest.RequestType.ROLE_DATA;
        response.team = gamePlayer.getTeam();
        response.role = gamePlayer.getRole();
    }
}
