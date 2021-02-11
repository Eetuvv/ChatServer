package com.mycompany.chatserver;

import java.time.LocalDateTime;

/**
 *
 * @author Eetu
 */
public class ChatMessage {

    LocalDateTime sent;
    String userName;
    String message;

    public ChatMessage(LocalDateTime sent, String nick, String msg) {
        this.sent = sent;
        this.userName = nick;
        this.message = msg;
    }
}
