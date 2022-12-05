package com.bobona.bgo.web.socket;

import org.springframework.web.socket.WebSocketSession;

public class GameSession {

    private Long gameId;
    private String username;
    private final WebSocketSession session;

    public GameSession(WebSocketSession session) {
        this.session = session;
    }

    public boolean isGameSession() {
        return gameId != null && gameId != -1;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getUsername() {
        return username;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
