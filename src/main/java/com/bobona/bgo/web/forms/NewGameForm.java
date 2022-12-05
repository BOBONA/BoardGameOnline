package com.bobona.bgo.web.forms;

import com.bobona.bgo.model.Game;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class NewGameForm {

    @NotNull
    @Size(min=4, max=26)
    private String name;
    @NotNull
    private int gameType;
    @NotNull
    private boolean spectatorsEnabled;
    @NotNull
    private boolean secured;
    @Size(min=5, max=15)
    private String passcode;
    @NotNull
    @Range(min=1, max=200)
    private int hoursUntilExpire;

    public NewGameForm() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public boolean isSpectatorsEnabled() {
        return spectatorsEnabled;
    }

    public void setSpectatorsEnabled(boolean spectatorsEnabled) {
        this.spectatorsEnabled = spectatorsEnabled;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public int getHoursUntilExpire() {
        return hoursUntilExpire;
    }

    public void setHoursUntilExpire(int hoursUntilExpire) {
        this.hoursUntilExpire = hoursUntilExpire;
    }

    public Game toGame() {
        Game game = new Game();
        game.setName(name);
        game.setGameType(gameType);
        game.setSpectatorsEnabled(spectatorsEnabled);
        game.setSecured(secured);
        game.setPasscode(passcode);
        game.setHoursUntilExpire(hoursUntilExpire);
        return game;
    }
}
