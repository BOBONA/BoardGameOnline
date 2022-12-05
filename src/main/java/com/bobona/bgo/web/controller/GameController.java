package com.bobona.bgo.web.controller;

import com.bobona.bgo.model.Game;
import com.bobona.bgo.model.User;
import com.bobona.bgo.service.GameService;
import com.bobona.bgo.service.UserService;
import com.bobona.bgo.web.data.SocketHomeRequest;
import com.bobona.bgo.web.data.SocketHomeResponse;
import com.bobona.bgo.web.forms.NewGameForm;
import com.bobona.bgo.web.socket.SocketMessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GameController {

    @Autowired
    private GameService gameService;
    @Autowired
    private UserService userService;
    @Autowired
    private BCryptPasswordEncoder bcrypt;
    @Autowired
    private SocketMessagingService messager;

    @GetMapping("/")
    public String main(Model model, Principal principal) {
        model.addAttribute("newGameForm", new NewGameForm());
        model.addAttribute("userId", userService.findUserByUsername(principal.getName()).getId());
        model.addAttribute("join", new Game());
        return "main";
    }

    @GetMapping("/reconnect")
    public String reconnect(Principal principal) {
        User user = userService.findUserByUsername(principal.getName());
        Long gameId = user.getLastJoinedGame();
        if (gameService.gameExists(gameId)) {
            return "redirect:/game/" + gameId;
        } else {
            userService.resetJoinedGame(user);
            return "redirect:/games";
        }
    }

    @GetMapping("/games")
    public String games(Model model, Principal principal) {
        model.addAttribute("newGameForm", new NewGameForm());
        model.addAttribute("userId", userService.findUserByUsername(principal.getName()).getId());
        model.addAttribute("gameTypes", gameService.getGameNames());
        model.addAttribute("join", new Game());
        return "games";
    }

    @GetMapping("/game/{id}")
    public String game(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findUserByUsername(principal.getName());
        Game game = gameService.getGame(id);
        userService.updateLastJoinedGame(user, game.getId());
        model.addAttribute("gameType", gameService.getGameNames().get(game.getGameType()));
        model.addAttribute("userId", user.getId());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("gameId", id);
        model.addAttribute("name", game.getName());
        model.addAttribute("canSpectatorsChat", game.canSpectatorsChat());
        model.addAttribute("timeLeft", game.getTimeLeft());
        return "game";
    }

    @GetMapping("/game/new")
    public String newGame(Model model) {
        model.addAttribute("game", new Game());
        model.addAttribute("gameTypes", gameService.getGameNames());
        return "newGame";
    }

    @PostMapping("/game/new")
    public String newGame(@Valid @ModelAttribute("newGameForm") NewGameForm newGameForm, BindingResult bindingResult, RedirectAttributes redirAttr, Principal principal) {
        if (bindingResult.hasErrors()) {
            redirAttr.addFlashAttribute("newGamePrompt", true);
            return "redirect:/";
        }
        User user = userService.findUserByUsername(principal.getName());
        Game game = newGameForm.toGame();
        if (game.getPasscode() != null) {
            game.setPasscode(bcrypt.encode(game.getPasscode()));
            game.setSecured(true);
        } else {
            game.setSecured(false);
        }
        Game createdGame = gameService.createGame(user, game, game.getGameType(), true);
        SocketHomeResponse response = new SocketHomeResponse(SocketHomeRequest.RequestType.GAME_ADDED);
        response.game = gameService.getBasicGameData(createdGame);
        messager.sendToAll(response);
        return "redirect:/game/" + createdGame.getId();
    }

    @PostMapping("/join")
    public String joinGame(@ModelAttribute Game join, RedirectAttributes redirAttr, Principal principal) {
        User user = userService.findUserByUsername(principal.getName());
        Game game = gameService.getGame(join.getId());
        String passcode = join.getPasscode();
        redirAttr.addFlashAttribute("joinForm", true);
        redirAttr.addFlashAttribute("joinGameId", join.getId());
        if (game != null && ((!passcode.equals("") && bcrypt.matches(passcode, game.getPasscode()) || !game.isSecured()))) {
            if (game.getGamePlayers().size() >= gameService.getMaxPlayers(game.getGameType())) {
                if (game.isGameStarted()) {
                    if (game.isSpectatorsEnabled()) {
                        redirAttr.addFlashAttribute("spectatePrompt", true);
                    } else {
                        redirAttr.addFlashAttribute("joinError", "Game has already started");
                    }
                } else {
                    redirAttr.addFlashAttribute("joinError", "Game is full!");
                }
                return "redirect:/games";
            } else if (gameService.getGamePlayer(user, game, true) != null) {
                redirAttr.addFlashAttribute("joinError", "You're already in this game!");
                return "redirect:/games";
            } else {
                gameService.addPlayer(game, user);
                messager.sendStatusMessage(game.getId(), user.getUsername() + " joined the game!");
                messager.sendGameStatusUpdate(gameService.getBasicGameData(game));
                return "redirect:/game/" + game.getId();
            }
        } else {
            redirAttr.addFlashAttribute("joinError", "Invalid id or passcode");
            return "redirect:/games";
        }
    }

    @Scheduled(fixedRate = 1000*3600)
    public void deleteExpiredGames() {
        List<Long> gameIds = new ArrayList<>();
        gameService.getGames()
                .stream()
                .filter(game -> game.getTimeLeft().isNegative())
                .map(Game::getId)
                .forEach(gameIds::add);
        gameIds
                .stream()
                .map(gameService::getGame)
                .forEach(gameService::deleteGame);
        // wow im so functional
    }
}
