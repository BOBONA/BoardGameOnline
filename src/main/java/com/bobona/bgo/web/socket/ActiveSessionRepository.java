package com.bobona.bgo.web.socket;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ActiveSessionRepository {

    private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();

    public void addSession(String sessionId, GameSession session) {
        gameSessions.put(sessionId, session);
    }

    public void removeSession(String sessionId) {
        gameSessions.remove(sessionId);
    }

    public boolean hasSession(String sessionId) {
        return gameSessions.containsKey(sessionId);
    }

    public GameSession getSession(String sessionId) {
        return gameSessions.get(sessionId);
    }

    public void setSessionData(String sessionId, long gameId, String username) {
        GameSession session = gameSessions.get(sessionId);
        // delete duplicate sessions (since who knows what might happen)
        for (Map.Entry<String, GameSession> pair : new HashMap<>(gameSessions).entrySet()) {
            GameSession oSession = pair.getValue();
            if (oSession.getGameId() != null && gameId == oSession.getGameId() &&
                    username.equals(oSession.getUsername())) {
                gameSessions.remove(pair.getKey());
            }
        }
        // set data
        session.setGameId(gameId);
        session.setUsername(username);
    }

    public List<String> getConnectedPlayers(Long gameId) {
        return gameSessions.values().stream()
                .filter(session -> gameId.equals(session.getGameId()))
                .map(GameSession::getUsername)
                .collect(Collectors.toList());
    }

    public int sessionCount() {
        return gameSessions.size();
    }
}
