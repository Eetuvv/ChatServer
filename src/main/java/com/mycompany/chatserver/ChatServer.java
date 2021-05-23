package com.mycompany.chatserver;

import com.sun.net.httpserver.HttpContext;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class ChatServer {

    public static void main(String[] args) throws Exception {
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            // Pass arguments given on launch to sslContext (keystore, keystore pass)
            SSLContext sslContext = chatServerSSLContext(args[1], args[2]);

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {

                @Override
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatContext = server.createContext("/chat", new ChatHandler());
            chatContext.setAuthenticator(auth);

            server.createContext("/registration", new RegistrationHandler(auth));

            // Enable multithread support
            server.setExecutor(Executors.newCachedThreadPool());

            server.start();

            try {
                System.out.println("Starting server..\n");
                if (args.length != 3) {
                    System.out.println("Invalid startup parameters");
                    System.out.println("Usage java -jar jar-file.jar dbname.db cert.jks c3rt-p4ssw0rd");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            ChatDatabase database = ChatDatabase.getInstance();
            //Open db from first argument (should be path to db-file)
            database.open("jdbc:sqlite:" + args[0]);
            //database.open("jdbc:sqlite:C:\\Users\\Eetu\\Documents\\NetBeansProjects\\ChatServer\\chatDatabase.db");

            boolean running = true;
            Scanner reader = new Scanner(System.in);

            while (running) {
                System.out.println("To quit, type /quit");
                String command = String.valueOf(reader.nextLine());

                // If user types command /quit, server will shut down in 3 seconds
                if (command.equals("/quit")) {
                    running = false;
                    System.out.println("--------Shutting down server--------");
                    server.stop(3);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: certificate not found!");
        } catch (KeyStoreException e) {
            System.out.println("Error: Keystore " + args[1] + " not found");
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("Invalid startup parameters");
            System.out.println("Usage java -jar jar-file.jar dbname.db cert.jks c3rt-p4ssw0rd");
        }
    }

    private static SSLContext chatServerSSLContext(String keystore, String pass) throws Exception {

        char[] passphrase = pass.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystore), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }
}
