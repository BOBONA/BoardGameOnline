package com.bobona.bgo.config;

import com.bobona.bgo.web.socket.ActiveSessionRepository;
import com.bobona.bgo.web.socket.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

public class CustomSubProtocolWebSocketHandler extends SubProtocolWebSocketHandler {

    @Autowired
    private ActiveSessionRepository sessionRepository;

    public CustomSubProtocolWebSocketHandler(MessageChannel clientInboundChannel, SubscribableChannel clientOutboundChannel) {
        super(clientInboundChannel, clientOutboundChannel);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionRepository.addSession(session.getId(), new GameSession(session));
        super.afterConnectionEstablished(session);
    }
}
