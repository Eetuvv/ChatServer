package com.mycompany.chatserver;

import java.sql.SQLException;

public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        ChatDatabase db = ChatDatabase.getInstance();
        
        try {
            return db.authenticateUser(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addUser(String userName, String password, String email) throws SQLException {

        ChatDatabase db = ChatDatabase.getInstance();
        
        return db.addUser(userName, password, email);
    }
}
