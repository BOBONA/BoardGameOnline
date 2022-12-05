package com.bobona.bgo.web.socket;

import com.bobona.bgo.web.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SocketMessagingService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public static final java.lang.String GAME_SUBSCRIPTION_PATTERN = "/socket/game/{gameId}";
    public static final java.lang.String HOME_SUBSCRIPTION = "/socket/main";

    public void sendStatusMessage(Long gameId, java.lang.String message) {
        SocketGameResponse response = new SocketGameResponse();
        response.responseType = SocketGameRequest.RequestType.STATUS_CHAT;
        response.message = new SocketChatMessage(null, message);
        sendToGame(gameId, response);
    }

    public void sendToGame(Long gameId, SocketGameResponse response) {
        messagingTemplate.convertAndSend(java.lang.String.format("/socket/game/%d", gameId), response);
    }

    public void sendToPlayer(Long gameId, Long userId, SocketGameResponse response) {
        messagingTemplate.convertAndSend(java.lang.String.format("/socket/game/%d/%d", gameId, userId), response);
    }

    public void sendToAll(SocketHomeResponse response) {
        messagingTemplate.convertAndSend(HOME_SUBSCRIPTION, response);
    }

    public void sendToUser(Long userId, SocketHomeResponse response) {
        messagingTemplate.convertAndSend("/socket/main/" + userId, response);
    }

    public void sendGameStatusUpdate(BasicGameData gameData) {
        SocketHomeResponse response = new SocketHomeResponse();
        response.type = SocketHomeRequest.RequestType.GAME_UPDATED;
        response.game = gameData;
        messagingTemplate.convertAndSend(HOME_SUBSCRIPTION, response);
    }
}
