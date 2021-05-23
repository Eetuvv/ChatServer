# ChatServer
REST-API type http-server made with Java's <a href="https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html">HttpServer-class</a>  
Client available <a href="https://github.com/Eetuvv/chat_application_with_back-end">here</a>
<br>
<br>
Server handles HTTP requests. User can: <ul>
    <li>Register with a username, password and email</li>
    <li>Post messages to specific channels</li>
    <li>Request a list of chat channels</li>
    <li>Create new chat channels</li>
    <li>Edit and delete messages</li>
    <li>Edit user details</li>
    </ul>
    Passwords are hashed with <a href="https://en.wikipedia.org/wiki/Salt_(cryptography)">salt</a> for extra security.
<br>
<br>

# Startup parameters
To start the server, pass these tree startup parameters to server, <strong>in this order:</strong> 
1. Database file name and path
2. Certificate file name and path
3. Password of the certificate

For example: java -jar target/my-server-jar.jar chat-database.db keystore.jks mypassword123

If using the example certificate "keystore.jks", the password is "123456789". 

# REST API
Server uses the following API. Requests have to be in JSON-format.
<br><br>
__HTTP body's for specific requests have to follow these formats__
<br>
## <strong>__/registration__</strong>  
#### Register new user
<pre>
{
    “user” : “nickname”,
    “action” : “edit”, //or remove
    “userdetails” :
    {
        “username” : “username”,
        “password” : “password”,
        “role” : “user”, //or administrator
        “email” : user.email@for-contacting.com”
     }
}
</pre>
## <strong>__/chat__</strong>  

### POST-request
Post a message
<pre>
{
    “user” : “nickname”,
    “channel” : “channel”,
    “message” : “contents of the message”,
    “sent” : “2021-04-13T07:57:47.123Z” // Timestamp for message
}
</pre>
### GET-request
#### Get requests to /chat are made using queries  
If no query is specified, server will redirect to: "https://localhost:8001/chat?channel=main"
##### Get messages from channel
<pre>
Query for choosing channel: <strong>?channel=channelname</strong>
Example: "https://localhost:8001/chat?channel=channelname"  
</pre>
##### List available channels
<pre>
Query for listing all available channels: <strong>?listChannels</strong>  
Example: "https://localhost:8001/chat?listChannels"
</pre>
### PUT-request 
Edit message
<pre>
{
    “user” : “username”,
    “action” : “editMessage”
    “messageid” : “293”, // message id to delete
    “channel” : “channel”
    “message” : “new contents of the message”,
    “sent” : “2020-12-21T07:57:47.123Z”
}
</pre>
Edit user info
<pre>
{
    “user” : “username”, // Current username of the user
    “action” : “editUser",
    "userdetails": // Updated user details
    {
      “updatedUsername” : “updatedUsername”, // New user details || or old details if not updating everything
      “updatedEmail” : “updatedEmail”,
      "updatedNickname" : "updatedNickname",
      "role" : "role"
    }
}
</pre>
Edit user's password 
<pre>
{
    “user” : “username”,
    “action” : “editPassword”
    “updatedPassword” : “newPassword”
}
</pre>
### DELETE-request
Delete message
<pre>
{
    “user” : “nickname”,
    “action” : “deletemessage”
    “messageid” : “293”
    “channel” : “channel”,
    “sent” : “2020-12-21T07:57:47.123Z”
}
</pre>

Delete user (with admin rights)
<pre>
{
    “user” : “username”, // user to remove
    “action” : “remove”
    "userdetails": // details of the user requesting the action
    {
        "username" : "username",
        "password" : "password",
        "role": "admin",
        "email" : "email@admin.com"
    }
}
</pre>

