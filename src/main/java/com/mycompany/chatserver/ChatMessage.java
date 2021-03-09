package com.mycompany.chatserver;

import java.time.LocalDateTime;

public class ChatMessage {

    LocalDateTime sent;
    String userName;
    String message;
    String channel;
    String tag;

    public ChatMessage(String channel, LocalDateTime sent, String nick, String msg, String tag) {
        this.channel = channel;
        this.sent = sent;
        this.userName = nick;
        this.message = msg;
        this.tag = tag;
    }
}
