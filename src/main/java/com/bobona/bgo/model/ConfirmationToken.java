package com.bobona.bgo.model;

import com.bobona.bgo.web.forms.SignupForm;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class ConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String confirmationToken;
    private Instant timestamp;
    private String email;
    private String username;
    private String password;

    public ConfirmationToken() {}

    public ConfirmationToken(SignupForm signupForm) {
        this.timestamp = Instant.now();
        this.confirmationToken = UUID.randomUUID().toString();
        this.email = signupForm.getEmail();
        this.username = signupForm.getUsername();
        this.password = signupForm.getPassword();
    }

    public SignupForm getSignupForm() {
        return new SignupForm(username, email, password);
    }

    public Long getId() {
        return id;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public String getUsername() {
        return username;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
