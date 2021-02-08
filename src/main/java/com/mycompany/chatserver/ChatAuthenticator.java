package com.mycompany.chatserver;

import java.sql.SQLException;
import java.util.Map;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eetu
 */
public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private Map<String, User> users;

    public ChatAuthenticator() {
        super("chat");
        this.users = new Hashtable<>();
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        ChatDatabase db = ChatDatabase.getInstance();

        try {
            return db.authenticateUser(username, password);
        } catch (SQLException e) {
            Logger.getLogger(ChatAuthenticator.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("tst");
        }
        return false;
    }

    public boolean addUser(String userName, String password, String email) throws SQLException {

        ChatDatabase db = ChatDatabase.getInstance();

        return db.addUser(userName, password, email);
    }
}
