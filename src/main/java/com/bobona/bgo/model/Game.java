package com.bobona.bgo.model;

import com.bobona.bgo.game.utils.Position;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;
    @OneToMany
    private final List<GamePlayer> gamePlayers;
    @OneToOne
    private User creator;
    @OneToMany
    private List<ChatMessage> messages;
    @LazyCollection(LazyCollectionOption.FALSE)
    @ElementCollection
    private List<Integer> tiles;
    @LazyCollection(LazyCollectionOption.FALSE)
    @ElementCollection
    private List<Double> scoreboards;
    private Instant timestamp;
    private String name;
    private int gameType;
    private boolean gameStarted = false;
    private int winner = -1; // -1 hasn't ended, 0 draw, >0 team id
    private boolean spectatorsEnabled;
    private boolean enableChat;
    private boolean spectatorsCanChat = false;
    private boolean startWhenFilled;
    private boolean secured;
    private String passcode;
    @Column(name = "hoursUntilExpire", nullable = false, columnDefinition = "int default 1")
    private int hoursUntilExpire;

    public Game() {
        timestamp = Instant.now();
        gamePlayers = new ArrayList<>();
        messages = new ArrayList<>();
        tiles = new ArrayList<>();
        scoreboards = new ArrayList<>();
    }

    public Game(Game game) {
        gamePlayers = game.getGamePlayers();
        tiles = new ArrayList<>(game.getSquares());
        scoreboards = new ArrayList<>(game.getScoreboards());
    }

    public Long getId() {
        return id;
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

    public List<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean shouldStartWhenFilled() {
        return startWhenFilled;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public String getName() {
        return name;
    }

    public int getWinner() {
        return winner;
    }

    public boolean isSecured() {
        return secured;
    }

    public String getPasscode() {
        return passcode;
    }

    public List<Integer> getSquares() {
        return tiles;
    }

    public boolean enableChat() {
        return enableChat;
    }

    public boolean canSpectatorsChat() {
        return spectatorsCanChat;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public void setSpectatorsEnabled(boolean spectatorsEnabled) {
        this.spectatorsEnabled = spectatorsEnabled;
    }

    public void setEnableChat(boolean enableChat) {
        this.enableChat = enableChat;
    }

    public void setSpectatorsCanChat(boolean spectatorsCanChat) {
        this.spectatorsCanChat = spectatorsCanChat;
    }

    public void setScoreboards(List<Double> scoreboards) {
        this.scoreboards = scoreboards;
    }

    public List<Double> getScoreboards() {
        return scoreboards;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartWhenFilled(boolean startWhenFilled) {
        this.startWhenFilled = startWhenFilled;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isGameEnded() {
        return winner != -1;
    }

    public void setTiles(List<Integer> tiles) {
        this.tiles = tiles;
    }

    public Integer getSquare(Position pos, int boardWidth) {
        return getSquares().get(pos.getX() + boardWidth * pos.getY());
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public int getHoursUntilExpire() {
        return hoursUntilExpire;
    }

    public void setHoursUntilExpire(int hoursUntilExpire) {
        this.hoursUntilExpire = hoursUntilExpire;
    }

    public Duration getTimeLeft() {
        return Duration.between(Instant.now(), getTimestamp().plusSeconds(getHoursUntilExpire() * 3600L));
    }
}
