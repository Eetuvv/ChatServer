package com.mycompany.chatserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {

    private int responseCode = 0;
    private String response = "";

    public ChatHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Handle different requests
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePostRequest(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            try {
                handleGetRequest(exchange);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            handlePutRequest(exchange);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            handleDeleteRequest(exchange);
        } else {
            handleBadRequest();
        }

        if (responseCode < 200 || responseCode > 299) {

            byte[] bytes = response.getBytes("UTF-8");

            exchange.sendResponseHeaders(responseCode, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.flush();
            os.close();
            exchange.close();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // Handle POST request (client sent new chat message)

        response = "";
        responseCode = 200;

        try {

            Headers headers = exchange.getRequestHeaders();
            int contentLength = 0;
            String contentType = "";

            if (headers.containsKey("Content-length")) {
                contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
            } else {
                response = "Content-length not defined";
                responseCode = 411;
            }

            if (headers.containsKey("Content-Type")) {
                contentType = headers.get("Content-Type").get(0);
            } else {
                response = "No Content-Type specified in request";
                responseCode = 400;
            }

            if (contentType.equalsIgnoreCase("application/json")) {

                InputStream stream = exchange.getRequestBody();

                String text = new BufferedReader(new InputStreamReader(stream,
                        StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                stream.close();

                JSONObject chatMessage = new JSONObject(text);

                String dateStr = chatMessage.getString("sent");
                OffsetDateTime odt = OffsetDateTime.parse(dateStr);

                LocalDateTime sent = odt.toLocalDateTime();
                String userName = chatMessage.get("user").toString();
                String message = chatMessage.getString("message");
                String channel = chatMessage.getString("channel");

                if (!text.isEmpty()) {
                    //Add message to database
                    ChatMessage newMessage = new ChatMessage(channel, sent, userName, message, "");
                    ChatDatabase db = ChatDatabase.getInstance();
                    db.insertMessage(newMessage);

                    exchange.sendResponseHeaders(200, -1);
                } else {
                    response = "Text was empty.";
                    responseCode = 400;
                }

            } else if (!contentType.isEmpty() && !contentType.equalsIgnoreCase("application/json")) {
                response = "Content-Type must be application/json";
                responseCode = 411;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("Invalid JSON-file");
            responseCode = 400;
            response = "Invalid JSON-file";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        // Handle GET request (client wants to see messages)

        URI requestURI = exchange.getRequestURI();
        String channel = "main";
        String action = null;

        if (requestURI.getQuery() == null) {
            String mainChannel = "https://localhost:8001/chat?channel=main";

            exchange.getResponseHeaders().add("Location", mainChannel);
            exchange.sendResponseHeaders(302, -1);
        } else {
            String query = requestURI.getQuery();
            if (query != null) {

                String split[] = query.split("=");
                // Split query string, should be "channel" or "listChannels"
                String splitQuery = split[0];
                if (splitQuery.equals("channel")) {
                    channel = split[1];
                    action = "getMessages";
                } else if (splitQuery.equals("listChannels")) {
                    action = "listChannels";
                }

            } else {
                responseCode = 400;
                response = "Bad query";
            }
        }

        ChatDatabase db = ChatDatabase.getInstance();
        Headers headers = exchange.getRequestHeaders();
        String lastModified = null;
        String contentType = null;
        LocalDateTime fromWhichDate = null;
        long messagesSince = -1;
        
        if (headers.containsKey("If-Modified-Since")) {
            lastModified = headers.get("If-Modified-Since").get(0);
            try {
                ZonedDateTime zd = ZonedDateTime.parse(lastModified);
                fromWhichDate = zd.toLocalDateTime();
                messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (DateTimeException e) {
                System.out.println("Invalid date in if-modified-since header");
            }

        } else {
            System.out.println("No last-modified header found");
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            response = "No Content-Type specified in request";
            responseCode = 400;
        }

        // List all different channels available
        if (action.equals("listChannels")) {
            ArrayList<String> channels = db.listChannels();
            String responseChannels = channels.toString();
            byte[] bytes = responseChannels.getBytes("UTF-8");

            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);

            os.flush();
            os.close();

            // Return messages from specified channel
        } else if (action.equals("getMessages")) {

            //Formatter for timestamps
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            ArrayList<ChatMessage> dbMessages = db.getMessages(channel, messagesSince);
            if (dbMessages.isEmpty()) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                //Sort messages by timestamp
                Collections.sort(dbMessages, (ChatMessage lhs, ChatMessage rhs) -> lhs.sent.compareTo(rhs.sent));

                //Create JSONArray to add messages to
                JSONArray responseMessages = new JSONArray();

                LocalDateTime latest = null;

                for (ChatMessage message : dbMessages) {

                    //Keep track of latest message in db
                    if (latest == null || message.sent.isAfter(latest)) {
                        latest = message.sent;
                    }

                    //Format timestamps
                    ZonedDateTime zonedDateTime = message.sent.atZone(ZoneId.of("UTC"));
                    String formattedTimestamp = zonedDateTime.format(formatter);

                    //Create new JSONObject with message details
                    JSONObject json = new JSONObject();

                    json.put("user", message.userName);
                    json.put("message", message.message);
                    json.put("sent", formattedTimestamp);
                    json.put("tag", message.tag);

                    //Add JSONObject to JSONArray
                    responseMessages.put(json);
                }

                if (latest != null) {
                    ZonedDateTime zonedDateTime = latest.atZone(ZoneId.of("UTC"));
                    String latestFormatted = zonedDateTime.format(formatter);

                    //Add last-modified header with value of latest msg timestamp
                    exchange.getResponseHeaders().add("Last-Modified", latestFormatted);
                }

                String JSON = responseMessages.toString();
                byte[] bytes = JSON.getBytes("UTF-8");

                exchange.sendResponseHeaders(200, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);

                os.flush();
                os.close();
            }
        } else {
            response = "Invalid action specified. To get messages, use action getMessages, and specify channel."
                    + " To list channels, use action listChannels.";
            responseCode = 400;
        }
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {

        //Handle PUT-request
        response = "";
        responseCode = 200;

        try {
            //Handle PUT request
            Headers headers = exchange.getRequestHeaders();
            int contentLength = 0;
            String contentType = "";

            if (headers.containsKey("Content-Length")) {
                contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
            } else {
                response = "Content-Length not specified";
                responseCode = 411;
            }

            if (headers.containsKey("Content-Type")) {
                contentType = headers.get("Content-Type").get(0);
            } else {
                response = "No Content-Type specified in request";
                responseCode = 400;
            }

            if (contentType.equalsIgnoreCase("application/json")) {
                InputStream stream = exchange.getRequestBody();

                String text = new BufferedReader(new InputStreamReader(stream,
                        StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                stream.close();

                JSONObject requestBody = new JSONObject(text);

                if (!text.isEmpty()) {

                    String user = requestBody.getString("user");
                    String action = requestBody.getString("action");

                    String role = null;
                    int messageID = 0;
                    String message = null;

                    ChatDatabase db = ChatDatabase.getInstance();

                    //Check if user has admin rights before editing user in db
                    if (action.equals("editUser")) {
                        role = requestBody.getString("role");
                        if (role.equals("admin")) {
                            JSONObject userDetails = requestBody.getJSONObject("userdetails");
                            String currentUsername = userDetails.getString("currentUsername");
                            String updatedUsername = userDetails.getString("updatedUsername");
                            String updatedPassword = userDetails.getString("updatedPassword");
                            String updatedEmail = userDetails.getString("updatedEmail");

                            db.adminEditUser(currentUsername, updatedUsername, updatedPassword, updatedEmail, role);
                            exchange.sendResponseHeaders(200, -1);
                        } else {
                            System.out.println("Only admin is authorized to edit users.");
                            response = "Not authorized: admin rights required to edit user";
                            responseCode = 401;
                        }

                        //Edit message with specified id
                    } else if (action.equals("editmessage")) {
                        messageID = requestBody.getInt("messageid");
                        message = requestBody.getString("message");
                        db.editMessage(messageID, user, message);
                        exchange.sendResponseHeaders(200, -1);
                    }

                } else {
                    response = "Text was empty.";
                    responseCode = 400;
                }

            } else {
                //Return error code if headers don't match JSON-type
                responseCode = 400;
                response = "Content-Type must be application/json";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            response = "JSON file not valid";
            responseCode = 400;
        } catch (SQLException e) {
            System.out.println("Error editing user");
        }
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        // Handle DELETE request

        response = "";
        responseCode = 200;

        try {

            Headers headers = exchange.getRequestHeaders();
            int contentLength = 0;
            String contentType = "";

            if (headers.containsKey("Content-length")) {
                contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
            } else {
                response = "Content-length not defined";
                responseCode = 411;
            }

            if (headers.containsKey("Content-Type")) {
                contentType = headers.get("Content-Type").get(0);
            } else {
                response = "No Content-Type specified in request";
                responseCode = 400;
            }

            if (contentType.equalsIgnoreCase("application/json")) {

                InputStream stream = exchange.getRequestBody();

                String text = new BufferedReader(new InputStreamReader(stream,
                        StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                stream.close();

                JSONObject requestBody = new JSONObject(text);

                if (!text.isEmpty()) {

                    String user = requestBody.getString("user");
                    String action = requestBody.getString("action");

                    ChatDatabase db = ChatDatabase.getInstance();

                    //Check if user has admin rights before deleting user from db
                    if (action.equals("remove")) {
                        JSONObject userDetails = requestBody.getJSONObject("userdetails");
                        String role = requestBody.getJSONObject("userdetails").getString("role");
                        if (role.equals("admin")) {
                            String username = userDetails.getString("username");
                            String password = userDetails.getString("password");
                            String email = userDetails.getString("email");
                            db.adminDeleteUser(user);
                            exchange.sendResponseHeaders(200, -1);
                        } else {
                            System.out.println("Only admin is authorized to remove users.");
                            response = "Not authorized: admin rights required to remove user";
                            responseCode = 401;
                        }
                        //Delete message with specified id
                    } else if (action.equals("deletemessage")) {
                        int messageID = requestBody.getInt("messageid");
                        //String channel = requestBody.getString("channel");
                        db.deleteMessage(messageID, user);
                        exchange.sendResponseHeaders(200, -1);
                    }
                } else {
                    response = "Text was empty.";
                    responseCode = 400;
                }

            } else if (!contentType.isEmpty() && !contentType.equalsIgnoreCase("text/plain")) {
                response = "Username to be deleted must be passed in text format: Content-Type: text/plain";
                responseCode = 411;
            }
        } catch (IOException | NumberFormatException | JSONException e) {
            e.printStackTrace();
            System.out.println("Invalid JSON-file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBadRequest() throws IOException {
        // Handle error if request not GET, POST, DELETE OR PUT
        response = "Not supported";
        responseCode = 400;
    }
}
