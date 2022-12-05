package com.bobona.bgo.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;
    @ManyToOne
    private Game game;
    @OneToOne
    private User user;
    private int team = -1;
    private Role role;

    public GamePlayer() {}

    public GamePlayer(Game game, User user, Role role) {
        this.game = game;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public User getUser() {
        return user;
    }

    public int getTeam() {
        return team;
    }

    public Role getRole() {
        return role;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public enum Role {

        CREATOR,
        PLAYER,
        SPECTATOR
    }
}
