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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventsHandler implements HttpHandler {

    private static final DateTimeFormatter ISO_NO_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "";

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

            if (!"/events".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            JsonArray arr = new JsonArray();

            try (Connection conn = DBConnection.getConnection()) {
                String sql =
                        "SELECT id, name, description, starts_at, capacity, created_by, created_at " +
                                "FROM events " +
                                "ORDER BY starts_at ASC";

                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        JsonObject ev = new JsonObject();
                        ev.addProperty("id", rs.getInt("id"));
                        ev.addProperty("name", rs.getString("name"));

                        String desc = rs.getString("description");
                        if (desc != null) ev.addProperty("description", desc);

                        Timestamp startsAt = rs.getTimestamp("starts_at");
                        ev.addProperty("starts_at", toIsoNoSeconds(startsAt));

                        ev.addProperty("capacity", rs.getInt("capacity"));
                        ev.addProperty("created_by", rs.getInt("created_by"));

                        Timestamp createdAt = rs.getTimestamp("created_at");
                        ev.addProperty("created_at", createdAt != null ? createdAt.toString() : null);

                        arr.add(ev);
                    }
                }
            }

            byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

        } catch (Exception e) {
            try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            try { exchange.close(); } catch (Exception ignored) {}
        }
    }

    private static String toIsoNoSeconds(Timestamp ts) {
        if (ts == null) return null;
        LocalDateTime ldt = ts.toLocalDateTime();
        return ldt.format(ISO_NO_SECONDS);
    }
}