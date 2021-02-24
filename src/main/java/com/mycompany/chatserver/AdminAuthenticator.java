package com.mycompany.chatserver;

import java.sql.SQLException;

public class AdminAuthenticator extends com.sun.net.httpserver.BasicAuthenticator{

    public AdminAuthenticator() {
        super("admin");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        
        ChatDatabase db = ChatDatabase.getInstance();
        
        try {
            return db.authenticateAdmin(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean addAdmin(String name, String password) throws SQLException {

        ChatDatabase db = ChatDatabase.getInstance();

        return db.addAdmin(name, password);
    }
}
