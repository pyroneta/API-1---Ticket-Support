package edu.upb.tickmaster.Handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.httpserver.ContentType;
import edu.upb.tickmaster.httpserver.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EchoPostHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(EchoPostHandler.class);

    @Override
    public void handle(HttpExchange he) throws IOException {

        long start = System.currentTimeMillis();
        String method = he.getRequestMethod();
        String path = he.getRequestURI() != null ? he.getRequestURI().getPath() : "";
        int status = 500;

        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            log.info("Incoming request {} {}", method, path);

            // =========================
            // POST
            // =========================
            if (method.equalsIgnoreCase("POST")) {

                // Leer body (aunque no lo uses)
                StringBuilder body = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = br.readLine()) != null) body.append(line);
                }

                log.debug("Echo POST bodySize={}", body.length());

                JsonObject object = new JsonObject();
                object.addProperty("nombre", "Ricardo");
                object.addProperty("Apellido", "Laredo");

                String response = object.toString();
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                status = Integer.parseInt(Status._200.name().substring(1, 4));
                he.sendResponseHeaders(status, bytes.length);

                try (OutputStream os = he.getResponseBody()) {
                    os.write(bytes);
                }

                return;
            }

            // =========================
            // GET
            // =========================
            if (method.equalsIgnoreCase("GET")) {

                String response = "{\"status\": \"NOK\",\"message\": \"No se logro imprimir la factura\"}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                status = Integer.parseInt(Status._200.name().substring(1, 4));
                he.sendResponseHeaders(status, bytes.length);

                try (OutputStream os = he.getResponseBody()) {
                    os.write(bytes);
                }

                return;
            }

            // =========================
            // OPTIONS
            // =========================
            if (method.equalsIgnoreCase("OPTIONS")) {

                String response = "{\"status\": \"OK\",\"message\": \"Factura impresa correctamente\"}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                status = Integer.parseInt(Status._200.name().substring(1, 4));
                he.sendResponseHeaders(status, bytes.length);

                try (OutputStream os = he.getResponseBody()) {
                    os.write(bytes);
                }

                return;
            }

            // =========================
            // Método no soportado
            // =========================
            String response = "{\"status\": \"NOK\",\"message\": \"Metodo no soportado\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            status = Integer.parseInt(Status._404.name().substring(1, 4));
            he.sendResponseHeaders(status, bytes.length);

            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }

        } catch (Exception e) {
            log.error("Echo handler failed {} {} - {}", method, path, e.toString(), e);
            try {
                he.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        } finally {
            log.info("Completed {} {} status={} timeMs={}",
                    method,
                    path,
                    status,
                    (System.currentTimeMillis() - start));

            try { he.close(); } catch (Exception ignored) {}
        }
    }
}
