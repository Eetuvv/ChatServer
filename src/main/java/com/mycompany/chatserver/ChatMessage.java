package com.mycompany.chatserver;

import java.time.LocalDateTime;

public class ChatMessage {

    public LocalDateTime sent;
    public String userName;
    public String message;
    public String channel;
    public String tag;
    
    public ChatMessage(String channel, LocalDateTime sent, String nick, String msg, String tag) {
        this.channel = channel;
        this.sent = sent;
        this.userName = nick;
        this.message = msg;
        this.tag = tag;
    }
}
