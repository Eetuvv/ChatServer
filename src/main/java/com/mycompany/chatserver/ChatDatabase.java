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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;

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
        //DriverManager.getConnection(databaseName);

        if (exists == false) {
            initializeDatabase();
        } else {
            System.out.println("Connected to database.");
        }
    }

    private boolean initializeDatabase() throws SQLException {
        //Create a new database if one does not yet exist
        try (Connection db = DriverManager.getConnection(databaseName)) {

            Statement s = db.createStatement();

            s.execute("CREATE TABLE IF NOT EXISTS Admins(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT, adminname TEXT UNIQUE, password TEXT, salt TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS Users(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT, username TEXT UNIQUE, password TEXT, email TEXT, salt TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS Messages(id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT, timestamp INTEGER, username REFERENCES Users)");

            System.out.println("Database created.");
            System.out.println("Connected to database.");

            s.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error creating new database.");
        }
        return false;
    }

    private String getHashedPasswordWithSalt(String password) {
        //Create salt for password
        byte[] bytes = new byte[13];
        secureRandom.nextBytes(bytes);

        //Conver salt bytes to a string
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;

        //Hash password with salt
        String hashedPassword = Crypt.crypt(password, salt);

        return hashedPassword + " " + salt;
    }

    public boolean addAdmin(String name, String password) {

        // Administrator has admin role with administrator rights
        String role = "admin";
        // Hash password with salt
        String split[] = getHashedPasswordWithSalt(password).split(" ");
        String hashedPassword = split[0];
        String salt = split[1];

        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get count of users with the same username in database, should be 0
            PreparedStatement p = db.prepareStatement("SELECT COUNT(Admins.adminname) AS COUNT FROM Admins WHERE Admins.adminname = ?");
            p.setString(1, name);

            ResultSet r = p.executeQuery();

            //Add user to database if admin name is available
            try {
                if (r.getInt("COUNT") == 0) {
                    s.execute("INSERT INTO Admins(role, adminname, password, salt) VALUES ('" + role + "', '" + name + "', '" + hashedPassword + "','" + salt + "')");
                    System.out.println("Added administrator " + name + " to database.");
                    return true;
                } else {
                    System.out.println("Administrator already exists.");
                }
            } catch (SQLException e) {
                System.out.println("Error when adding user credentials to database.");
            }
            s.close();

        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
        }
        return false;
    }

    public boolean addUser(String username, String password, String email) throws SQLException {

        // User has role user with user rights
        String role = "user";

        // Hash password with salt
        String split[] = getHashedPasswordWithSalt(password).split(" ");
        String hashedPassword = split[0];
        String salt = split[1];

        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get count of users with the same username in database, should be 0
            PreparedStatement p = db.prepareStatement("SELECT COUNT(Users.username) AS COUNT FROM Users WHERE Users.username = ?");
            p.setString(1, username);

            ResultSet r = p.executeQuery();

            //Add user to database if username is available
            try {
                if (r.getInt("COUNT") == 0) {
                    s.execute("INSERT INTO Users(role, username, password, email, salt) VALUES ('" + role + "', '" + username + "', '" + hashedPassword + "','" + email + "','" + salt + "')");
                    System.out.println("Added user " + username + " to database.");
                    return true;

                } else {
                    System.out.println("Username already exists.");
                }
            } catch (SQLException e) {
                System.out.println("Error when adding user credentials to database.");
            }
            s.close();

        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
        }
        return false;
    }

    public boolean authenticateUser(String username, String password) throws SQLException {
        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get user info matching given username and password
            PreparedStatement p = db.prepareStatement("SELECT Users.username, Users.password FROM Users WHERE username = ?");

            p.setString(1, username);

            ResultSet r = p.executeQuery();

            if (r.next()) {
                String hashedPassword = r.getString("password");
                //Check if username and password match
                //Check if hashed password in database matches new hashed password with salt
                if (r.getString("username").equals(username) && hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
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
            System.out.println("Could not connect to database");
        }
        return false;
    }

    public boolean authenticateAdmin(String name, String password) throws SQLException {
        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {

            s = db.createStatement();

            //Get user info matching given username and password
            PreparedStatement p = db.prepareStatement("SELECT Admins.adminname, Admins.password FROM Admins WHERE adminname = ?");

            p.setString(1, name);

            ResultSet r = p.executeQuery();

            if (r.next()) {
                String hashedPassword = r.getString("password");
                //Check if username and password match
                //Check if hashed password in database matches new hashed password with salt
                if (r.getString("adminname").equals(name) && hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
                    return true;
                } else {
                    System.out.println("Wrong username or password");
                    return false;
                }
            } else {
                System.out.println("Invalid admin credentials");
            }
            s.close();

        } catch (SQLException e) {
            System.out.println("Could not connect to database");
        }
        return false;
    }

    public void deleteUser(String username) {
        Statement s;

        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();

            PreparedStatement p = db.prepareStatement("DELETE FROM Users WHERE username = ?");

            p.setString(1, username);

            int result = p.executeUpdate();

            if (result != 0) {
                System.out.println("User " + username + " deleted.");
            } else {
                System.out.println("Can't delete user: username not found");
            }
            s.close();
        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
        }
    }
    
    public boolean editUser(String currentUsername, String username, String password, String email) throws SQLException {
        
        // Admin can edit user's info, by giving the current username and updated info
        // Hash new password with salt
        String split[] = getHashedPasswordWithSalt(password).split(" ");
        String hashedPassword = split[0];
        
        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();
            
            PreparedStatement p = db.prepareStatement("UPDATE Users SET username = ? , password = ?, email = ? WHERE username = ?");
            
            p.setString(1, username);
            p.setString(2, hashedPassword);
            p.setString(3, email);
            p.setString(4, currentUsername);
            
            if (p.executeUpdate() != 0) {
                System.out.println("User " + currentUsername + " updated");
                return true;
            } else {
                System.out.println("Error updating user data: could not find user");
                return false;
            }
        }
    }

    public void insertMessage(String msg, LocalDateTime timestamp, String user) {
        Statement s;

        long time = timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();;

        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();

            String msgBody = "INSERT INTO Messages(message, timestamp, username) VALUES ('" + msg + "','" + time + "','" + user + "')";

            try {
                s.executeUpdate(msgBody);
            } catch (SQLException e) {
                System.out.println("Error inserting message into database.");
            }

            s.close();
        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
        }
    }

    public ArrayList<ChatMessage> getMessages(long messagesSince) {
        //Return all messages from database in a ArrayList
        ArrayList<ChatMessage> messages = new ArrayList<>();
        String msg;
        Long timestamp;
        String user;

        Statement s;
        try (Connection db = DriverManager.getConnection(databaseName)) {
            s = db.createStatement();

            String query = "";
            PreparedStatement p;

            if (messagesSince == -1) {
                //Get 100 newest messages from db if no last-modified header is found
                query = "SELECT * FROM Messages ORDER BY timestamp DESC LIMIT 100";
                p = db.prepareStatement(query);
            } else {
                query = "SELECT Messages.message, Messages.timestamp, Messages.username "
                        + "FROM Messages WHERE Messages.timestamp > ? ORDER BY timestamp";
                p = db.prepareStatement(query);
                p.setLong(1, messagesSince);
            }

            ResultSet r = p.executeQuery();

            while (r.next()) {

                msg = r.getString("message");
                timestamp = r.getLong("timestamp");
                user = r.getString("username");

                //Convert long to LocalDateTime
                LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);

                //Create ChatMessage object from variables, and add it to arraylist
                ChatMessage message = new ChatMessage(time, user, msg);
                messages.add(message);
            }
            s.close();
        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
        }
        return messages;
    }
}