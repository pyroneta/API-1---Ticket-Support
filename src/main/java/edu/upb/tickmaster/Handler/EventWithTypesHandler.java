package edu.upb.tickmaster.Handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.DB.DBConnection;
import edu.upb.tickmaster.Util.AuthStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventWithTypesHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(EventWithTypesHandler.class);

    private static final DateTimeFormatter ISO_NO_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "";

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

            if (!"POST".equalsIgnoreCase(method) || !"/events-with-types".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            // ===== Auth: solo ADMIN =====
            AuthStore.Session session = requireSession(exchange);
            if (session == null) return;

            if (!"ADMIN".equalsIgnoreCase(session.role)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            // ===== Body JSON =====
            JsonObject body = readJson(exchange);

            JsonObject eventObj = body.has("event") ? body.getAsJsonObject("event") : null;
            JsonArray typesArr = body.has("ticket_types") ? body.getAsJsonArray("ticket_types") : null;

            if (eventObj == null || typesArr == null) {
                writeJson(exchange, 400, msg("Body must contain: { event: {...}, ticket_types: [...] }"));
                return;
            }

            // Event fields
            String name = getStr(eventObj, "name");
            String description = getStr(eventObj, "description");
            String startsAtStr = getStr(eventObj, "starts_at"); // "YYYY-MM-DDTHH:MM"
            int capacity = getInt(eventObj, "capacity", -1);

            if (name.isEmpty() || startsAtStr.isEmpty() || capacity <= 0) {
                writeJson(exchange, 400, msg("Invalid event fields: name, starts_at, capacity>0"));
                return;
            }

            Timestamp startsAt = parseTimestampFromDatetimeLocal(startsAtStr);

            // Validate ticket types
            if (typesArr.size() <= 0) {
                writeJson(exchange, 400, msg("You must provide at least 1 ticket_type"));
                return;
            }

            for (int i = 0; i < typesArr.size(); i++) {
                JsonObject t = typesArr.get(i).getAsJsonObject();
                String tName = getStr(t, "name");
                double price = getDouble(t, "price", -1);
                int quota = getInt(t, "quota", -1);

                if (tName.isEmpty() || price < 0 || quota <= 0) {
                    writeJson(exchange, 400, msg("Invalid ticket_types[" + i + "]: name, price>=0, quota>0"));
                    return;
                }
            }

            // ===== Transaction: insert event + insert ticket_types =====
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    int eventId;
                    Timestamp createdAtEvent;

                    String insEvent =
                            "INSERT INTO events(name, description, starts_at, capacity, created_by) " +
                                    "VALUES (?, ?, ?, ?, ?) " +
                                    "RETURNING id, created_at";

                    try (PreparedStatement ps = conn.prepareStatement(insEvent)) {
                        ps.setString(1, name);
                        ps.setString(2, description.isEmpty() ? null : description);
                        ps.setTimestamp(3, startsAt);
                        ps.setInt(4, capacity);
                        ps.setInt(5, session.userId);

                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            eventId = rs.getInt("id");
                            createdAtEvent = rs.getTimestamp("created_at");
                        }
                    }

                    // Insert ticket types
                    JsonArray insertedTypes = new JsonArray();
                    String insType =
                            "INSERT INTO ticket_types(event_id, name, price, quota, is_active) " +
                                    "VALUES (?, ?, ?, ?, true) " +
                                    "RETURNING id, created_at";

                    for (int i = 0; i < typesArr.size(); i++) {
                        JsonObject t = typesArr.get(i).getAsJsonObject();
                        String tName = getStr(t, "name");
                        double price = getDouble(t, "price", 0);
                        int quota = getInt(t, "quota", 1);

                        try (PreparedStatement ps = conn.prepareStatement(insType)) {
                            ps.setInt(1, eventId);
                            ps.setString(2, tName);
                            ps.setDouble(3, price);
                            ps.setInt(4, quota);

                            try (ResultSet rs = ps.executeQuery()) {
                                rs.next();

                                JsonObject out = new JsonObject();
                                out.addProperty("id", rs.getInt("id"));
                                out.addProperty("event_id", eventId);
                                out.addProperty("name", tName);
                                out.addProperty("price", price);
                                out.addProperty("quota", quota);
                                out.addProperty("is_active", true);
                                out.addProperty("created_at", rs.getString("created_at"));
                                insertedTypes.add(out);
                            }
                        }
                    }

                    conn.commit();

                    JsonObject resp = new JsonObject();

                    JsonObject ev = new JsonObject();
                    ev.addProperty("id", eventId);
                    ev.addProperty("name", name);
                    ev.addProperty("description", description);
                    ev.addProperty("starts_at", startsAtStr);
                    ev.addProperty("capacity", capacity);
                    ev.addProperty("created_by", session.userId);
                    ev.addProperty("created_at", createdAtEvent != null ? createdAtEvent.toString() : null);

                    resp.add("event", ev);
                    resp.add("ticket_types", insertedTypes);

                    writeJson(exchange, 201, resp);
                    return;

                } catch (Exception e) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                    throw e;
                } finally {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                }
            }

        } catch (IllegalArgumentException bad) {
            try { writeJson(exchange, 400, msg("starts_at must be like 2026-03-10T20:00")); } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("EventWithTypesHandler error", e);
            try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            try { exchange.close(); } catch (Exception ignored) {}
        }
    }

    // ===== timestamp parse =====
    private static Timestamp parseTimestampFromDatetimeLocal(String startsAtStr) {
        // expects "YYYY-MM-DDTHH:MM"
        LocalDateTime ldt = LocalDateTime.parse(startsAtStr, ISO_NO_SECONDS);
        return Timestamp.valueOf(ldt);
    }

    // ===== Auth =====
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

    private static String getStr(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString().trim()
                : "";
    }

    private static int getInt(JsonObject obj, String key, int def) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsInt();
        } catch (Exception ignored) {}
        return def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsDouble();
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