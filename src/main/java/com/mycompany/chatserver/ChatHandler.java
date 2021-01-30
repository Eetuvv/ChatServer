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
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ChatHandler implements HttpHandler {

    private ArrayList<String> messages;
    private String messageBody;

    public ChatHandler() {
        this.messages = new ArrayList<>();
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

        if (contentType.equalsIgnoreCase("text/plain")) {

            InputStream stream = exchange.getRequestBody();

            String text = new BufferedReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            stream.close();

            if (!text.isEmpty()) {
                messages.add(text + "\n");
                exchange.sendResponseHeaders(200, -1);
            } else {
                errorResponse = "Text was empty.";
                code = 400;
            }

        } else if (!contentType.isEmpty() && !contentType.equalsIgnoreCase("text/plain")) {
            errorResponse = "Content-Type must be text/plain";
            code = 411;
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
        for (int i = 0; i < messages.size(); i++) {
            messageBody += messages.get(i) + "\n";
        }

        byte[] bytes = messageBody.getBytes("UTF-8");

        exchange.sendResponseHeaders(200, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);

        os.flush();
        os.close();

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
