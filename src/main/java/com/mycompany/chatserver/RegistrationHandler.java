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
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    private ChatAuthenticator authenticator;

    public RegistrationHandler(ChatAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String errorResponse = "";
        int code = 200;

        try {

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                //Handle POST request
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";

                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.valueOf(headers.get("Content-Length").get(0));
                } else {
                    errorResponse = "Content-Length not specified";
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

                    JSONObject registrationMsg = new JSONObject(text);

                    String username = registrationMsg.get("username").toString();
                    String password = registrationMsg.getString("password");
                    String email = registrationMsg.getString("email");

                    if (text.isEmpty()) {
                        code = 401;
                        errorResponse = "Error: text was empty.";
                    } else {
                        
                        if (this.authenticator.addUser(username, password, email)) {
                            exchange.sendResponseHeaders(200, -1);
                        } else {
                            code = 403;
                            errorResponse = "Username is already registered";
                        }
                    }
                } else {
                    //Return error code if headers don't match JSON-type
                    code = 400;
                    errorResponse = "Content-Type must be application/json";
                }
            }
        } catch (JSONException e) {
            errorResponse = "JSON file not valid";
            code = 400;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (code < 200 || code > 299) {
            //If response code is not in the 200s's range (Not OK)
            //Send error code and error message
            OutputStream os = exchange.getResponseBody();

            byte[] bytes = errorResponse.getBytes("UTF-8");

            exchange.sendResponseHeaders(code, bytes.length);

            os.write(bytes);
            os.flush();
            os.close();
        }
        exchange.close();
    }
}
