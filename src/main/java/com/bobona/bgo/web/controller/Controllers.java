package com.bobona.bgo.web.controller;

import com.bobona.bgo.service.UserService;
import com.bobona.bgo.web.socket.ActiveSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class Controllers {

    @Autowired
    private UserService userService;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private ActiveSessionRepository sessionRepository;

    @ModelAttribute("username")
    public String username(Principal principal) {
        if (principal == null) {
            return "Guest";
        } else {
            return principal.getName();
        }
    }

    @ModelAttribute("loggedin")
    public boolean loggedin(Principal principal) {
        return principal != null;
    }

    @ModelAttribute("hasLastJoined")
    public boolean hasLastJoined(Principal principal) {
        if (principal == null) {
            return false;
        } else {
            return userService.userHasLastJoinedGame(userService.findUserByUsername(principal.getName()));
        }
    }

    @ModelAttribute("playersOnline")
    public int playersOnline() {
        return sessionRepository.sessionCount();
    }
}
