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

    private static final long HANG_MS_FIRST_ATTEMPT = 2000;

    @Override
    public void handle(HttpExchange exchange) {

        long start = System.currentTimeMillis();

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "";
        int status = 500;

        String rid = exchange.getRequestHeaders().getFirst("X-Request-Id");
        if (isNullOrBlank(rid)) {
            rid = "rid-" + System.nanoTime();
        }

        try {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id, X-Retry-Attempt");

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

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                exchange.close();

                log.info("[{}] GET <- ok bytes={}", rid, bytes.length);
                return;
            }

            // =========================
            // POST /tickets
            // =========================
            if ("POST".equalsIgnoreCase(method)) {

                // Lee body
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
                );

                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);

                String raw = body.toString();
                log.info("[{}] POST bodySize={} bodyPreview={}", rid, raw.length(), preview(raw, 250));

                // Lee intento (viene del proxy)
                String retryAttemptStr = exchange.getRequestHeaders().getFirst("X-Retry-Attempt");
                int retryAttempt = parseIntSafe(retryAttemptStr, 2); // default 2 para que cree normal si no viene

                log.info("[{}] POST X-Retry-Attempt={}", rid, retryAttempt);

                // 🔥 Intención de tu prueba:
                // - intento 1: forzar read timeout del proxy (colgarse) y NO guardar en DB
                // - intento 2: guardar y responder 201
                if (retryAttempt == 1) {
                    log.warn("[{}] POST attempt=1 -> Simulating upstream HANG {}ms (no DB insert, no response)",
                            rid, HANG_MS_FIRST_ATTEMPT);

                    try { Thread.sleep(HANG_MS_FIRST_ATTEMPT); } catch (InterruptedException ignored) {}

                    // No respondemos nada para que el proxy haga READ TIMEOUT.
                    // Cerramos el exchange silenciosamente.
                    status = 0; // no se envió status real
                    try { exchange.close(); } catch (Exception ignored) {}
                    return;
                }

                // intento 2 (o directo) => procesar normal
                Ticket ticket = gson.fromJson(raw, Ticket.class);

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
                Ticket creado = service.crearTicket(ticket);

                log.info("[{}] POST <- service.crearTicket() OK id={}",
                        rid, (creado != null ? creado.getId() : -1));

                String response = gson.toJson(creado);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                status = 201;
                exchange.sendResponseHeaders(201, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                exchange.close();

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

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            exchange.close();

        } catch (Exception e) {

            log.error("[{}] TicketHandler failed {} {} - {}", rid, method, path, e.toString(), e);

            try {
                status = 500;
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            } catch (Exception ignored) {}

        } finally {
            long ms = System.currentTimeMillis() - start;
            log.info("[{}] Completed {} {} status={} timeMs={}", rid, method, path, status, ms);
        }
    }

    // ==== helpers ====
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

    private static int parseIntSafe(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}