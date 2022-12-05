package com.bobona.bgo.web.data;

public class BasicGameData {

    public Long gameId;
    public int gameType;
    public String gameName;
    public String creatorName;
    public boolean started;
    public boolean ended;
    public int players;
    public int maxPlayers;
    public boolean canSpectate;
    public boolean secured;
    public boolean joined = false;

    public BasicGameData(Long gameId) {
        this.gameId = gameId;
    }

    public BasicGameData(Long gameId, int gameType, String gameName, String creatorName, boolean started, boolean ended, int players, int maxPlayers, boolean canSpectate, boolean secured) {
        this.gameId = gameId;
        this.gameType = gameType;
        this.gameName = gameName;
        this.creatorName = creatorName;
        this.started = started;
        this.ended = ended;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.canSpectate = canSpectate;
        this.secured = secured;
    }
}
