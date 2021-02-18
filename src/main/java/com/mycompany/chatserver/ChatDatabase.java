package com.mycompany.chatserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;

/**
 *
 * @author Eetu
 */
public class ChatDatabase {

    private static ChatDatabase singleton = null;
    private String databaseName = "";
    private SecureRandom secureRandom;

    private ChatDatabase() {
        secureRandom = new SecureRandom();
    }

    public static synchronized ChatDatabase getInstance() {
        //Function is synchronized to prevent creating multiple instances of the same object
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    public void open(String dbName) throws SQLException {
        //Remove first 12 characters from db name (jdbc:sqlite:)
        File f = new File(dbName.substring(12));
        //Check if a file exists
        Boolean exists = f.exists() && !f.isDirectory();

        databaseName = dbName;
        Connection db = DriverManager.getConnection(databaseName);

        if (exists == false) {
            initializeDatabase();
        } else {
            System.out.println("Database found.");
        }
    }

    private boolean initializeDatabase() throws SQLException {
        //Create a new database if one does not yet exist
        try (Connection db = DriverManager.getConnection(databaseName)) {

            Statement s = db.createStatement();

            s.execute("CREATE TABLE Users(id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, email TEXT, salt TEXT)");
            s.execute("CREATE TABLE Messages(id INTEGER PRIMARY KEY, message TEXT, timestamp INTEGER, username REFERENCES Users)");

            System.out.println("Database created.");

            s.close();
            return true;
        } catch (SQLException e) {
            System.out.println("Database already exists.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addUser(String username, String password, String email) throws SQLException {

        Statement s;

        //Create salt for password
        byte[] bytes = new byte[13];
        secureRandom.nextBytes(bytes);

        //Conver salt bytes to a string
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        
        //Hash password with salt
        String hashedPassword = Crypt.crypt(password, salt);

        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get count of users with the same username in database, should be 0
            PreparedStatement p = db.prepareStatement("SELECT COUNT(Users.username) AS COUNT FROM Users WHERE Users.username = ?");
            p.setString(1, username);

            ResultSet r = p.executeQuery();

            //Add user to database if username is available
            try {
                if (r.getInt("COUNT") == 0) {
                    s.execute("INSERT INTO Users(username, password, email, salt) VALUES ('" + username + "', '" + hashedPassword + "','" + email + "','" + salt +"')");
                    System.out.println("Added user " + username + " to database.");
                    return true;

                } else {
                    System.out.println("Username already exists.");
                }
            } catch (SQLException e) {
                System.out.println("Error when adding user credentials to database.");
                e.printStackTrace();
            }
            s.close();

        } catch (SQLException e) {
            System.out.println("Database not found");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean authenticateUser(String username, String password) throws SQLException {
        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get user info matching given username and password
            PreparedStatement p = db.prepareStatement("SELECT Users.username, Users.password FROM Users WHERE username = ?");
                    //+ "AND password = ?");
            p.setString(1, username);
            //????????????????
           // p.setString(2, password);

            ResultSet r = p.executeQuery();

            if (r.next()) {
                String hashedPassword = r.getString("password");
                //Check if username and password match
                //Check if hashed password in database matches new hashed password with salt
                if (r.getString("username").equals(username) && hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
                    System.out.println("Authentication successful.");
                    return true;
                } else {
                    System.out.println("Wrong username or password");
                    return false;
                }

            } else {
                System.out.println("Invalid user credentials");
            }
            s.close();

        } catch (SQLException e) {
            System.out.println("Database not found");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertMessage(String msg, LocalDateTime timestamp, String user) {
        Statement s;

        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();

            String msgBody = "INSERT INTO Messages(message, timestamp, username) VALUES ('" + msg + "','" + timestamp + "','" + user + "')";

            try {
                s.executeUpdate(msgBody);
            } catch (SQLException e) {
                System.out.println("Error inserting message into database.");
                e.printStackTrace();
            }

            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList getMessages() {
        //Return all messages from database in a ArrayList

        ArrayList<ChatMessage> messages = new ArrayList<>();
        String msg;
        String timestamp;
        String user;

        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();

            //Select all messages
            PreparedStatement p = db.prepareStatement("SELECT * FROM Messages");

            ResultSet r = p.executeQuery();

            while (r.next()) {

                msg = r.getString("message");
                timestamp = r.getString("timestamp");
                user = r.getString("username");

                //Convert string to LocalDateTime
                LocalDateTime time = LocalDateTime.parse(timestamp);

                ChatMessage message = new ChatMessage(time, user, msg);

                messages.add(message);
            }
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
}
