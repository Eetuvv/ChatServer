package com.mycompany.chatserver;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        ChatDatabase db = ChatDatabase.getInstance();
        
        try {
            return db.authenticateUser(username, password);
        } catch (SQLException ex) {
            
        }
        return false;
    }

    public boolean addUser(String userName, String password, String email) throws SQLException {

        ChatDatabase db = ChatDatabase.getInstance();
        
        return db.addUser(userName, password, email);
    }
}
