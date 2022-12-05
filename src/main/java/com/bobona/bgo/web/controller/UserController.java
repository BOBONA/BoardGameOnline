package com.bobona.bgo.web.controller;

import com.bobona.bgo.model.ConfirmationToken;
import com.bobona.bgo.service.ConfirmationTokenService;
import com.bobona.bgo.service.UserService;
import com.bobona.bgo.web.forms.SignupForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ConfirmationTokenService tokenService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        return "redirect:/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm signupForm, BindingResult bindingResult) {
        if (userService.findUserByUsername(signupForm.getUsername()) != null) {
            bindingResult.rejectValue("username", "taken.signupForm.username", "Username has been taken!");
        }
        if (bindingResult.hasErrors()) {
            return "signup";
        }
        // userService.signUpUser(signupForm, false);
        // return "redirect:/login?confirm";
        userService.signUpUser(signupForm, true);
        return "redirect:/login";
    }

    @GetMapping("/signup/confirm")
    public String confirmMail(@RequestParam("token") String token) {
        Optional<ConfirmationToken> optionalConfirmationToken = tokenService.findConfirmationTokenWithToken(token);
        optionalConfirmationToken.ifPresent(userService::confirmUser);
        return "redirect:/";
    }

    @GetMapping("/help")
    public String help() {
        return "help";
    }

    @ResponseBody
    @GetMapping("/userExists/{username}")
    public String userExists(@PathVariable String username) {
        return Boolean.toString(userService.findUserByUsername(username) != null);
    }
}
