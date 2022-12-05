package com.bobona.bgo.config;

import com.bobona.bgo.service.GameService;
import com.bobona.bgo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class SocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Autowired
    private GameService gameService;
    @Autowired
    private UserService userService;

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpSubscribeDestMatchers("/socket/main").authenticated()
                .simpDestMatchers("/socket/game/{gameId}").access(
                        "isAuthenticated() and @gameService.canUserAccessGame(" +
                                "@userService.findUserByUsername(authentication.getName()), " +
                                "@gameService.getGame(#gameId))")
                .simpSubscribeDestMatchers("/socket/game/{gameId}/{userId}").access(
                        "isAuthenticated() and @gameService.canUserAccessGame(" +
                                "@userService.findUserByUsername(authentication.getName()), " +
                                "@gameService.getGame(#gameId))" +
                                "and (!#userId.equals('') or @userService.findUserByUsername(authentication.getName()).getId() == #userid)")
                .anyMessage().authenticated();
    }
}
