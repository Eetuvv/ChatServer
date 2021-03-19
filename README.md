# ChatServer
Chat Server made in Programming 3 course. Server handles HTTP requests. User can register a new user with username, password and email, login with user, and post messages that are saved to  SQLite database.  
Passwords are encrypted for extra security.


# Startup parameters
To start the server, pass these tree startup parameters to server, <strong>in this order:</strong> 
1. Database file name and path
2. Certificate file name and path
3. Password of the certificate

For example: java -jar target/my-server-jar.jar chat-database.db keystore.jks mypassword123

If using the example certificate "keystore.jks", the password is "123456789". 
