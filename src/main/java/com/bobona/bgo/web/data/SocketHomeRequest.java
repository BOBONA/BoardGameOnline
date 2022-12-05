package com.bobona.bgo.web.data;

public class SocketHomeRequest {

    private String type;
    private String chatMessage;

    public SocketHomeRequest(java.lang.String type, String chatMessage) {
        this.type = type;
        this.chatMessage = chatMessage;
    }

    public RequestType getType() {
        return RequestType.valueOf(type);
    }

    public void setType(java.lang.String type) {
        this.type = type;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(String chatMessage) {
        this.chatMessage = chatMessage;
    }

    public enum RequestType {
        GET_GAMES,
        GAME_ADDED,
        GAME_UPDATED,
        GAME_REMOVED,
        GET_USERS,
        USER_CONNECTED,
        USER_DISCONNECTED,
        CHAT_MESSAGE
    }
}
