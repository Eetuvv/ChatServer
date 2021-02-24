package com.mycompany.chatserver;

import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;

public class User {

    String username;
    String password;
    String email;

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
