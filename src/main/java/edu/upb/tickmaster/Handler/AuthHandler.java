package edu.upb.tickmaster.Handler;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI() != null ? exchange.getRequestURI().getPath() : "";

        try {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(method) || !"/login".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            JsonObject body = readJson(exchange);
            String username = getStr(body, "username");
            String password = getStr(body, "password");

            if (username.isEmpty() || password.isEmpty()) {
                writeJson(exchange, 400, msg("Missing username or password"));
                return;
            }

            // ⚠️ Asume password en texto plano en DB (como lo tienes ahora).
            // Luego lo cambias a hash si quieres.
            try (Connection conn = DBConnection.getConnection()) {

                String sql =
                        "SELECT u.id, r.name AS role " +
                                "FROM users u " +
                                "JOIN roles r ON r.id = u.user_role " +
                                "WHERE u.username = ? AND u.password = ?";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, password);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            writeJson(exchange, 401, msg("Invalid credentials"));
                            return;
                        }

                        int userId = rs.getInt("id");
                        String role = rs.getString("role"); // ADMIN / CLIENTE

                        String token = AuthStore.createToken(userId, role);

                        JsonObject resp = new JsonObject();
                        resp.addProperty("token", token);
                        resp.addProperty("userId", userId);
                        resp.addProperty("role", role);

                        writeJson(exchange, 200, resp);
                        return;
                    }
                }
            }

        } catch (Exception e) {
            log.error("AuthHandler error", e);
            try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            try { exchange.close(); } catch (Exception ignored) {}
        }
    }

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