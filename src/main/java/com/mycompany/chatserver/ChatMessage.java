package com.mycompany.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

    long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
    
    void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }
}
