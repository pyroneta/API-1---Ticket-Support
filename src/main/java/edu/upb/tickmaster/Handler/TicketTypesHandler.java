package edu.upb.tickmaster.Handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.DB.DBConnection;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TicketTypesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI() != null ? exchange.getRequestURI().getQuery() : null;

        try {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Integer eventId = parseQueryInt(query, "eventId");
            if (eventId == null) {
                writeJson(exchange, 400, msg("Missing query param: eventId"));
                return;
            }

            JsonArray arr = new JsonArray();

            try (Connection conn = DBConnection.getConnection()) {
                String sql =
                        "SELECT id, event_id, name, price, quota, is_active, created_at " +
                                "FROM ticket_types " +
                                "WHERE event_id = ? AND is_active = true " +
                                "ORDER BY id";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, eventId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JsonObject o = new JsonObject();
                            o.addProperty("id", rs.getInt("id"));
                            o.addProperty("event_id", rs.getInt("event_id"));
                            o.addProperty("name", rs.getString("name"));
                            o.addProperty("price", rs.getBigDecimal("price").toString()); // si usas numeric
                            o.addProperty("quota", rs.getInt("quota"));
                            o.addProperty("is_active", rs.getBoolean("is_active"));
                            o.addProperty("created_at", rs.getString("created_at"));
                            arr.add(o);
                        }
                    }
                }
            }

            writeJson(exchange, 200, arr);

        } catch (Exception e) {
            try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            try { exchange.close(); } catch (Exception ignored) {}
        }
    }

    private static Integer parseQueryInt(String query, String key) {
        if (query == null) return null;
        // super simple parser: eventId=1&x=y
        String[] parts = query.split("&");
        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(key)) {
                try { return Integer.parseInt(kv[1]); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static JsonObject msg(String text) {
        JsonObject o = new JsonObject();
        o.addProperty("message", text);
        return o;
    }

    private static void writeJson(HttpExchange exchange, int code, JsonArray arr) throws Exception {
        byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void writeJson(HttpExchange exchange, int code, JsonObject obj) throws Exception {
        byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}