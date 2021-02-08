package com.mycompany.chatserver;

import com.sun.net.httpserver.HttpContext;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class ChatServer {
    
    public static void main(String[] args) throws Exception {

        try {
            
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            SSLContext sslContext = chatServerSSLContext();

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {

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

            server.setExecutor(null);
            server.start();
            
            ChatDatabase database = ChatDatabase.getInstance();
            database.open("jdbc:sqlite:C:\\Users\\Eetu\\Documents\\NetBeansProjects\\ChatServer\\chatDatabase.db");
            
        } catch (FileNotFoundException e) {
            System.out.println("Error: certificate not found!");

            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static SSLContext chatServerSSLContext() throws Exception {

        char[] passphrase = "123456789".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

}
