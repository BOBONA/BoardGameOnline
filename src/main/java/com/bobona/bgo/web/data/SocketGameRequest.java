package com.bobona.bgo.web.data;

public class SocketGameRequest {

    private final String type;
    private final RawMove move;
    private final String chatMessage;

    public SocketGameRequest(String type, RawMove move, String chatMessage) {
        this.type = type;
        this.move = move;
        this.chatMessage = chatMessage;
    }

    public RawMove getMove() {
        return move;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public RequestType getType() {
        return RequestType.valueOf(type);
    }

    public enum RequestType {

        UPDATE,
        START,
        MOVE,
        PREVIEW,
        VALIDATE,
        CONTINUATION_PREVIEW,
        CHAT,
        STATUS_CHAT,
        FETCH_PLAYERS,
        LEAVE,
        ROLE_DATA,
        NEW_GAME,
        REQUEST_UPDATE
    }
}