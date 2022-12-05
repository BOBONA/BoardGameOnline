package com.bobona.bgo.web.data;

import com.bobona.bgo.model.GamePlayer;

public class BasicPlayerData {

    public String name;
    public GamePlayer.Role role;
    public long teamId;

    public BasicPlayerData(String username, GamePlayer.Role role, long teamId) {
        this.name = username;
        this.role = role;
        this.teamId = teamId;
    }
}
