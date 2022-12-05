package com.bobona.bgo.web.data;

import java.sql.Timestamp;

public class SocketChatMessage {

    private final java.lang.String username;
    private final java.lang.String text;
    private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public SocketChatMessage(java.lang.String username, java.lang.String text) {
        this.username = username;
        this.text = text;
    }

    public java.lang.String getUsername() {
        return username;
    }

    public java.lang.String getText() {
        return text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
