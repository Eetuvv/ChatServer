package com.mycompany.chatserver;

import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

/**
 *
 * @author Eetu
 */
public class ChatAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    //private Map<String, String> users;
    private Map<String, User> users;

    public ChatAuthenticator() {
        super("chat");
        this.users = new Hashtable<>();

        users.put("dummy", new User("dummy", "passwd", "dummy@dummy.com"));
    }

    @Override
    public boolean checkCredentials(String username, String password) {

        //TODO check email
        if (this.users.containsKey(username)) {

            if (this.users.get(username).password.equals(password)) {
                return true;
            }
        }
        return false;
    }

    public boolean addUser(String userName, String password, String email) {

        User newUser = new User(userName, password, email);
        
        if (!this.users.containsKey(userName)) {
            this.users.put(userName, newUser);
            return true;
        }
        return false;
    }
}
