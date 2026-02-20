package edu.upb.tickmaster.Handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.Model.Ticket;
import edu.upb.tickmaster.Service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TicketHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(TicketHandler.class);

    private final TicketService service = new TicketService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) {

        long start = System.currentTimeMillis();

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "";
        int status = 500;

        // ✅ requestId (compatible Java 8)
        String rid = exchange.getRequestHeaders().getFirst("X-Request-Id");
        if (isNullOrBlank(rid)) {
            rid = "rid-" + System.nanoTime();
        }

        try {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                status = 200;
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                log.info("[{}] Preflight OPTIONS {} {}", rid, method, path);
                return;
            }

            log.info("[{}] Incoming request {} {}", rid, method, path);

            // =========================
            // GET /tickets
            // =========================
            if ("GET".equalsIgnoreCase(method)) {

                log.info("[{}] GET -> service.listarTickets()", rid);

                List<Ticket> tickets = service.listarTickets();
                String response = gson.toJson(tickets);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                status = 200;
                exchange.sendResponseHeaders(200, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                exchange.close();

                log.info("[{}] GET <- ok bytes={}", rid, bytes.length);
                return;
            }

            // =========================
            // POST /tickets
            // =========================
            if ("POST".equalsIgnoreCase(method)) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
                );

                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);

                String raw = body.toString();
                log.info("[{}] POST bodySize={} bodyPreview={}", rid, raw.length(), preview(raw, 250));

                Ticket ticket = gson.fromJson(raw, Ticket.class);

                // Logs útiles para debug
                if (ticket == null) {
                    throw new Exception("Body inválido: no se pudo parsear Ticket");
                }

                log.info("[{}] POST parsed titulo='{}' usuarioId={} empleadoId={} estadoId={}",
                        rid,
                        safe(ticket.getTitulo()),
                        ticket.getUsuarioId(),
                        ticket.getEmpleadoId(),
                        ticket.getEstadoId()
                );

                log.info("[{}] POST -> service.crearTicket()", rid);

                // ✅ Aquí es donde debería insertar en DB
                Ticket creado = service.crearTicket(ticket);

                log.info("[{}] POST <- service.crearTicket() OK id={}",
                        rid, (creado != null ? creado.getId() : -1));

                // 🔥 demo: retrasar respuesta DESPUÉS de insertar
                Thread.sleep(1000);

                String response = gson.toJson(creado);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                status = 201;
                exchange.sendResponseHeaders(201, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                exchange.close();

                log.info("[{}] POST <- responded 201 bytes={}", rid, bytes.length);
                return;
            }

            // =========================
            // Método no soportado
            // =========================
            String response = "{\"error\":\"Método no permitido\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            status = 405;
            exchange.sendResponseHeaders(405, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
            exchange.close();

        } catch (Exception e) {

            log.error("[{}] TicketHandler failed {} {} - {}", rid, method, path, e.toString(), e);

            try {
                status = 500;
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            } catch (Exception ignored) {}

        } finally {
            log.info("[{}] Completed {} {} status={} timeMs={}",
                    rid, method, path, status, (System.currentTimeMillis() - start));
        }
    }

    // ==== helpers (Java 8 OK) ====
    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String preview(String s, int max) {
        if (s == null) return "null";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }
}
