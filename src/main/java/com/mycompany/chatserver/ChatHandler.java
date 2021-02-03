package com.mycompany.chatserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {

    private ArrayList<ChatMessage> messages;
    private String messageBody;

    public ChatHandler() {
        this.messages = new ArrayList<ChatMessage>();
        this.messageBody = "";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handlePostRequest(exchange);

        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            handleGetRequest(exchange);

        } else {
            handleBadRequest(exchange);
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // Handle POST request (client sent new chat message)

        String errorResponse = "";
        int code = 200;

        try {

            Headers headers = exchange.getRequestHeaders();
            int contentLength = 0;
            String contentType = "";

            if (headers.containsKey("Content-Length")) {
                contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
            } else {
                errorResponse = "Content-length not defined";
                code = 411;
            }

            if (headers.containsKey("Content-Type")) {
                contentType = headers.get("Content-Type").get(0);
            } else {
                errorResponse = "No Content-Type specified in request";
                code = 400;
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
                String nickName = chatMessage.get("user").toString();
                String message = chatMessage.getString("message");

                ChatMessage newMessage = new ChatMessage(sent, nickName, message);

                if (!text.isEmpty()) {
                    messages.add(newMessage);
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    errorResponse = "Text was empty.";
                    code = 400;
                }

            } else if (!contentType.isEmpty() && !contentType.equalsIgnoreCase("application/json")) {
                errorResponse = "Content-Type must be application/json";
                code = 411;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            code = 400;
            errorResponse = "Invalid JSON-file";
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (code < 200 || code > 299) {

            byte[] bytes = errorResponse.getBytes("UTF-8");

            exchange.sendResponseHeaders(code, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(errorResponse.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        exchange.close();
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        // Handle GET request (client wants to see all messages)

        try {

            if (messages.isEmpty()) {
                //Send code 204 with no content if messages list is empty
                exchange.sendResponseHeaders(204, -1);
            } else {

                //Sort messages by timestamp
                Collections.sort(messages, (ChatMessage lhs, ChatMessage rhs) -> lhs.sent.compareTo(rhs.sent));

                //Create JSONArray to add messages to
                JSONArray responseMessages = new JSONArray();

                for (ChatMessage message : messages) {

                    //Format timestamps
                    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMdd'T'HH:mm:ss.SSSX");
                    //Create new JSONObject with message details
                    JSONObject json = new JSONObject();
                    json.put("sent", message.sent);
                    json.put("user", message.userName);
                    json.put("message", message.message);

                    //Add JSONObject to JSONArray
                    responseMessages.put(json);
                }

                for (int i = 0; i < responseMessages.length(); i++) {
                    //Get data from JSONArray's JSONObject's and paste it to messageBody
                    JSONObject o = new JSONObject();
                    o = responseMessages.getJSONObject(i);

                    messageBody += o.get("sent") + "<" + o.get("user") + ">" + o.get("message");
                }

                byte[] bytes = messageBody.getBytes("UTF-8");

                exchange.sendResponseHeaders(200, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);

                os.flush();
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        exchange.close();
    }

    private void handleBadRequest(HttpExchange exchange) throws IOException {
        // Handle error if request not GET or POST
        String errorResponse = "Not supported";

        byte[] bytes = errorResponse.getBytes("UTF-8");

        exchange.sendResponseHeaders(400, bytes.length);

        OutputStream os = exchange.getResponseBody();

        os.write(errorResponse.getBytes("UTF-8"));
        os.flush();
        os.close();

        exchange.close();
    }
}
