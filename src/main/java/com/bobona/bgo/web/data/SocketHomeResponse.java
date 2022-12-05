package com.bobona.bgo.web.data;

import java.util.List;

public class SocketHomeResponse {

    public SocketHomeRequest.RequestType type;
    public SocketChatMessage message;
    public String changedUser;
    public List<BasicGameData> allGames;
    public BasicGameData game;

    public SocketHomeResponse() {}

    public SocketHomeResponse(SocketHomeRequest.RequestType type) {
        this.type = type;
    }
}
