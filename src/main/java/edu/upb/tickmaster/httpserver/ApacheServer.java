package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import edu.upb.tickmaster.Handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApacheServer {
    private HttpServer server = null;

    public ApacheServer() {}

    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(1914), 0);

            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                new RootHandler().handle(exchange);
            });

            this.server.createContext("/health", new HealthHandler());
            this.server.createContext("/hola", new EchoPostHandler());
            this.server.createContext("/usuarios", new UsuariosHandler());
            this.server.createContext("/tickets", new TicketHandler());
            this.server.createContext("/login", new AuthHandler());
            this.server.createContext("/events", new EventsHandler());
            this.server.createContext("/events-with-types", new EventWithTypesHandler());

            this.server.createContext("/webhook", new WebhookHmacHandler());

            this.server.setExecutor(Executors.newFixedThreadPool(2));
            this.server.start();

            System.out.println("✅ API1 corriendo en puerto 1914");
            return true;

        } catch (IOException e) {
            System.err.println("❌ Error al iniciar API1 (puerto 1914)");
            e.printStackTrace();
            this.server = null;
            return false;
        }
    }

    public void stop() {
        this.server.stop(0);
        this.server = null;
    }
}