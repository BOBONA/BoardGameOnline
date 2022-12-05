package com.bobona.bgo.model;

import com.bobona.bgo.web.forms.SignupForm;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

@Entity(name = "BgoUser")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;
    private Instant joined;
    @Column(unique = true)
    private String username;
    private String email;
    private String password;
    private Long lastJoinedGame; // -1 means it doesn't exist
    private final Role role = Role.USER;

    public User() {}

    public User(SignupForm signupForm) {
        this.username = signupForm.getUsername();
        this.email = signupForm.getEmail();
        this.password = signupForm.getPassword();
        joined = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getJoined() {
        return joined;
    }

    public void setJoined(Instant joined) {
        this.joined = joined;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role.name());
        return Collections.singletonList(authority);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getLastJoinedGame() {
        return lastJoinedGame;
    }

    public void setLastJoinedGame(Long lastJoinedGame) {
        this.lastJoinedGame = lastJoinedGame;
    }

    enum Role {

        USER,
        ADMIN
    }
}
