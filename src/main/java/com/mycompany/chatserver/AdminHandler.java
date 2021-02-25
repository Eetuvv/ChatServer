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
import java.time.LocalDateTime;
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

public class AdminHandler implements HttpHandler {

    private AdminAuthenticator authenticator;

    int responseCode = 0;
    String response = "";

    public AdminHandler(AdminAuthenticator auth) {
        this.authenticator = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePostRequest(exchange);

        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGetRequest(exchange);

        } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            handleDeleteRequest(exchange);

        } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            handlePutRequest(exchange);

        } else {
            handleBadRequest(exchange);
        }

        if (responseCode < 200 || responseCode > 299) {

            byte[] bytes = response.getBytes("UTF-8");

            exchange.sendResponseHeaders(responseCode, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.flush();
            os.close();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {

        // Handle POST request
        response = "";
        responseCode = 200;

        try {
            //Handle POST request
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

                JSONObject registrationMsg = new JSONObject(text);

                String username = registrationMsg.get("adminname").toString();
                String password = registrationMsg.getString("password");

                if (this.authenticator.addAdmin(username, password)) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    responseCode = 403;
                    response = "Admin already exists";
                }

            } else {
                //Return error code if headers don't match JSON-type
                responseCode = 400;
                response = "Content-Type must be application/json";
            }
        } catch (JSONException e) {
            response = "JSON file not valid";
            responseCode = 400;
        } catch (SQLException e) {
            System.out.println("Error registering admin");
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        // Handle GET request (client wants to see messages)

        URI requestURI = exchange.getRequestURI();
        String channel = "main";

        // If no channel is specified in query, automatically redirect to main channel
        if (requestURI.getQuery() == null) {
            String mainChannel = "https://localhost:8001/chat?channel=main";

            exchange.getResponseHeaders().add("Location", mainChannel);
            exchange.sendResponseHeaders(302, -1);
        } else {
            String query = requestURI.getQuery();

            if (query != null) {
                String split[] = query.split("=");
                if (split[0].equals("channel")) {
                    channel = split[1];
                }
            }

            try {
                ChatDatabase db = ChatDatabase.getInstance();

                Headers headers = exchange.getRequestHeaders();

                String lastModified = null;
                LocalDateTime fromWhichDate = null;
                long messagesSince = -1;

                if (headers.containsKey("If-Modified-Since")) {

                    lastModified = headers.get("If-Modified-Since").get(0);

                    ZonedDateTime zd = ZonedDateTime.parse(lastModified);
                    fromWhichDate = zd.toLocalDateTime();

                    messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
                } else {
                    System.out.println("No last-modified header found");
                }

                //Formatter for timestamps
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

                // TODO add channel
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
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            exchange.close();
        }
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        // Handle DELETE request (admin wants to delete user)

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

                JSONObject user = new JSONObject(text);
                
                String username = user.getString("username");

                if (!text.isEmpty()) {
                    //Delete user from database
                    ChatDatabase db = ChatDatabase.getInstance();
                    db.deleteUser(username);
                    exchange.sendResponseHeaders(200, -1);
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
        }
        exchange.close();
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {

        //Handle PUT-request (admin wants to edit user)
        response = "";
        int code = 200;

        try {
            //Handle PUT request
            Headers headers = exchange.getRequestHeaders();
            int contentLength = 0;
            String contentType = "";

            if (headers.containsKey("Content-Length")) {
                contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
            } else {
                response = "Content-Length not specified";
                code = 411;
            }

            if (headers.containsKey("Content-Type")) {
                contentType = headers.get("Content-Type").get(0);
            } else {
                response = "No Content-Type specified in request";
                code = 400;
            }

            if (contentType.equalsIgnoreCase("application/json")) {
                InputStream stream = exchange.getRequestBody();

                String text = new BufferedReader(new InputStreamReader(stream,
                        StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                stream.close();

                JSONObject userInfoMsg = new JSONObject(text);

                String currentUsername = userInfoMsg.get("currentUsername").toString();
                String username = userInfoMsg.get("username").toString();
                String password = userInfoMsg.getString("password");
                String email = userInfoMsg.getString("email");

                ChatDatabase db = ChatDatabase.getInstance();

                if (db.editUser(currentUsername, username, password, email)) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    System.out.println("Could not edit user");
                    code = 403;
                    response = "Could not edit user";
                }
            } else {
                //Return error code if headers don't match JSON-type
                code = 400;
                response = "Content-Type must be application/json";
            }
        } catch (JSONException e) {
            response = "JSON file not valid";
            code = 400;
        } catch (SQLException e) {
            System.out.println("Error editing user");
        }

        exchange.close();
    }

    private void handleBadRequest(HttpExchange exchange) throws IOException {
        // Handle error if request not any of the above

        response = "Not supported";
        responseCode = 400;

        byte[] bytes = response.getBytes("UTF-8");

        exchange.sendResponseHeaders(responseCode, bytes.length);

        OutputStream os = exchange.getResponseBody();

        os.write(response.getBytes("UTF-8"));
        os.flush();
        os.close();

        exchange.close();
    }
}
