package com.bobona.bgo.service;

import com.bobona.bgo.dao.UserDao;
import com.bobona.bgo.model.ConfirmationToken;
import com.bobona.bgo.model.User;
import com.bobona.bgo.web.forms.SignupForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private Environment env;
    @Autowired
    private UserDao userDao;
    @Autowired
    private PasswordEncoder bcrypt;
    @Autowired
    private ConfirmationTokenService tokenService;
    @Autowired
    private MailSenderService mailSenderService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userDao.findByUsername(username);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new UsernameNotFoundException(String.format("User with email %s has not been found.", username));
        }
    }

    public User findById(Long id) {
        return userDao.findById(id).orElse(null);
    }

    public User findUserByUsername(String username) {
        Optional<User> user = userDao.findByUsername(username);
        return user.orElse(null);
    }

    public void signUpUser(SignupForm signupForm, boolean verify) {
        ConfirmationToken confirmationToken = new ConfirmationToken(signupForm);
        tokenService.saveConfirmationToken(confirmationToken);
        if (verify) {
            confirmUser(confirmationToken);
        } else {
            sendConfirmationEmail(signupForm.getEmail(), confirmationToken.getUsername(), confirmationToken.getConfirmationToken());
        }
    }

    public void updateLastJoinedGame(User user, Long gameId) {
        user.setLastJoinedGame(gameId);
        userDao.save(user);
    }

    public void resetJoinedGame(User user) {
        user.setLastJoinedGame(-1L);
        userDao.save(user);
    }

    public boolean userHasLastJoinedGame(User user) {
        if (user.getLastJoinedGame() == null) {
            return false;
        } else {
            return user.getLastJoinedGame() != -1L;
        }
    }

    public void confirmUser(ConfirmationToken token) {
        User user = new User(token.getSignupForm());
        String encryptedPassword = bcrypt.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        userDao.save(user);
        tokenService.deleteConfirmationToken(token.getId());
    }

    private void sendConfirmationEmail(String userMail, String username, String token) {
        String domain = env.getProperty("bgo.domain");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userMail);
        message.setSubject("Confirm your Account!");
        message.setFrom("BoardGamesOnline");
        message.setText(
                "Thank you for registering, '" + username + "'. Please click on the link below to activate your account."
                        + "\n" + domain + "/signup/confirm?token="
                + token + "\nThis link will expire in 1 hour."
        );
        mailSenderService.sendMail(message);
    }

    @Scheduled(fixedRate = 1000*1800)
    public void deleteExpiredTokens() {
        List<Long> tokens = new ArrayList<>();
        tokenService.getTokens().forEach(token -> {
            if (token.getTimestamp() == null || Duration.between(Instant.now(), token.getTimestamp()).isNegative()) {
                tokens.add(token.getId());
            }
        });
        tokens.forEach(tokenService::deleteConfirmationToken);
    }
}
