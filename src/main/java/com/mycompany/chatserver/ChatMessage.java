
package com.mycompany.chatserver;

import java.time.LocalDateTime;

/**
 *
 * @author Eetu
 */
public class ChatMessage {

    private LocalDateTime sent;
    private String nickName;
    private String message;
    
    public ChatMessage(LocalDateTime sent, String nick, String msg) {
        this.sent = sent;
        this.nickName = nick;
        this.message = msg;
    }

}
