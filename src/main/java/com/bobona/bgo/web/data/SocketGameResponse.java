package com.bobona.bgo.web.data;

import com.bobona.bgo.model.GamePlayer;

import java.util.List;

// stores every possible type of data that could be in a response... make sure you know what will be sent back to a given request type
public class SocketGameResponse {

    public SocketGameRequest.RequestType responseType;
    public List<Integer> tiles;
    public List<Integer> continuations;
    public List<Double> scoreboards;
    public boolean gameStarted;
    public boolean canMove;
    public int maxPlayers;
    public List<BasicPlayerData> connectedPlayers;
    public List<BasicPlayerData> disconnectedPlayers;
    public int winner;
    public int gameType;
    public int moveIsValid; // 0: invalid, 1: valid, 2: incomplete
    public SocketChatMessage message;
    public RawMove lastMove;
    public GamePlayer.Role role;
    public int team;
}