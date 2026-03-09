package edu.upb.tickmaster.Handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.DB.DBConnection;
import edu.upb.tickmaster.Util.AuthStore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class BuyTicketHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();

        try {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Auth required
            AuthStore.Session session = requireSession(exchange);
            if (session == null) return;

            // Solo CLIENT compra
            if (!"CLIENT".equalsIgnoreCase(session.role)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            JsonObject body = readJson(exchange);
            int ticketTypeId = getInt(body, "ticket_type_id", -1);
            if (ticketTypeId <= 0) {
                writeJson(exchange, 400, msg("Missing/invalid ticket_type_id"));
                return;
            }

            // Transacción anti-sobreventa
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // 1) Bloquea la fila del ticket_type
                    int quota;
                    int eventId;

                    String lockSql =
                            "SELECT id, event_id, quota, is_active " +
                                    "FROM ticket_types " +
                                    "WHERE id = ? " +
                                    "FOR UPDATE";

                    try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                        ps.setInt(1, ticketTypeId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                writeJson(exchange, 404, msg("ticket_type not found"));
                                return;
                            }
                            boolean active = rs.getBoolean("is_active");
                            if (!active) {
                                conn.rollback();
                                writeJson(exchange, 400, msg("ticket_type is not active"));
                                return;
                            }
                            eventId = rs.getInt("event_id");
                            quota = rs.getInt("quota");
                        }
                    }

                    // 2) Cuenta vendidos para ese ticket_type (dentro de la misma transacción)
                    int sold;
                    String soldSql = "SELECT COUNT(*) AS c FROM tickets WHERE ticket_type_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(soldSql)) {
                        ps.setInt(1, ticketTypeId);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            sold = rs.getInt("c");
                        }
                    }

                    if (sold >= quota) {
                        conn.rollback();
                        writeJson(exchange, 409, msg("Sold out"));
                        return;
                    }

                    // 3) Inserta ticket comprado
                    String insSql =
                            "INSERT INTO tickets(event_id, user_id, ticket_type_id) " +
                                    "VALUES (?, ?, ?) " +
                                    "RETURNING id, purchase_date";

                    int ticketId;
                    Timestamp purchaseDate;

                    try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                        ps.setInt(1, eventId);
                        ps.setInt(2, session.userId);
                        ps.setInt(3, ticketTypeId);

                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            ticketId = rs.getInt("id");
                            purchaseDate = rs.getTimestamp("purchase_date");
                        }
                    }

                    conn.commit();

                    JsonObject resp = new JsonObject();
                    resp.addProperty("ticket_id", ticketId);
                    resp.addProperty("event_id", eventId);
                    resp.addProperty("user_id", session.userId);
                    resp.addProperty("ticket_type_id", ticketTypeId);
                    resp.addProperty("purchase_date", purchaseDate != null ? purchaseDate.toString() : null);

                    writeJson(exchange, 201, resp);
                    return;

                } catch (Exception e) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                    throw e;
                } finally {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            try { exchange.close(); } catch (Exception ignored) {}
        }
    }

    // ===== Auth helpers =====
    private static AuthStore.Session requireSession(HttpExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            exchange.sendResponseHeaders(401, -1);
            return null;
        }
        String token = auth.substring("Bearer ".length()).trim();
        AuthStore.Session s = AuthStore.getSession(token);
        if (s == null) {
            exchange.sendResponseHeaders(401, -1);
            return null;
        }
        return s;
    }

    // ===== JSON helpers =====
    private static JsonObject readJson(HttpExchange exchange) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        if (sb.toString().isEmpty()) return new JsonObject();
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }

    private static int getInt(JsonObject obj, String key, int def) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsInt();
        } catch (Exception ignored) {}
        return def;
    }

    private static JsonObject msg(String text) {
        JsonObject o = new JsonObject();
        o.addProperty("message", text);
        return o;
    }

    private static void writeJson(HttpExchange exchange, int code, JsonObject obj) throws Exception {
        byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}