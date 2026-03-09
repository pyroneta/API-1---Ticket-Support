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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuariosHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(UsuariosHandler.class);

    // Cambia esto si tu frontend corre en otro host/puerto
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Override
    public void handle(HttpExchange he) {
        long start = System.currentTimeMillis();
        String method = he.getRequestMethod();
        String path = he.getRequestURI() != null ? he.getRequestURI().getPath() : "";
        String query = he.getRequestURI() != null ? he.getRequestURI().getQuery() : null;

        int status = 500;

        try {
            applyCors(he);

            // Preflight CORS
            if ("OPTIONS".equalsIgnoreCase(method)) {
                status = 204;
                he.sendResponseHeaders(204, -1);
                return;
            }

            he.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            log.info("Incoming request {} {}{}", method, path, (query != null ? "?" + query : ""));

            // ============================
            // POST /api/login
            // ============================
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
                JsonObject body = readJsonBody(he);

                String username = safeGet(body, "username");
                String password = safeGet(body, "password");

                if (username.isEmpty() || password.isEmpty()) {
                    writeJson(he, 400, jsonMsg("Missing username or password"));
                    status = 400;
                    return;
                }

                // ⚠️ Por ahora: password en texto plano (igual que tu tabla actual)
                // Recomendación: luego cambia a hash (BCrypt).
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
                                writeJson(he, 401, jsonMsg("Invalid credentials"));
                                status = 401;
                                return;
                            }

                            int userId = rs.getInt("id");
                            String role = rs.getString("role"); // ADMIN / CLIENTE

                            String token = AuthStore.createToken(userId, role);

                            JsonObject resp = new JsonObject();
                            resp.addProperty("token", token);
                            resp.addProperty("userId", userId);
                            resp.addProperty("role", role);

                            writeJson(he, 200, resp);
                            status = 200;
                            return;
                        }
                    }
                }
            }

            // ============================
            // GET /api/users  o /api/users?id=1
            // (opcional; normalmente solo admin debería ver esto)
            // ============================
            if ("GET".equalsIgnoreCase(method) && path.endsWith("/users")) {

                // (Opcional) exigir token y que sea ADMIN:
                // if (!requireAdmin(he)) return;

                try (Connection conn = DBConnection.getConnection()) {

                    // /api/users?id=5
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.split("=")[1]);

                        String sql =
                                "SELECT u.id, u.username, u.name, r.name AS role, u.created_at " +
                                        "FROM users u JOIN roles r ON r.id = u.user_role " +
                                        "WHERE u.id = ?";

                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, id);

                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) {
                                    he.sendResponseHeaders(404, -1);
                                    status = 404;
                                    return;
                                }

                                JsonObject obj = new JsonObject();
                                obj.addProperty("id", rs.getInt("id"));
                                obj.addProperty("username", rs.getString("username"));
                                obj.addProperty("name", rs.getString("name"));
                                obj.addProperty("role", rs.getString("role"));
                                obj.addProperty("created_at", rs.getString("created_at"));

                                writeJson(he, 200, obj);
                                status = 200;
                                return;
                            }
                        }

                    } else {
                        String sql =
                                "SELECT u.id, u.username, u.name, r.name AS role, u.created_at " +
                                        "FROM users u JOIN roles r ON r.id = u.user_role " +
                                        "ORDER BY u.id";

                        JsonArray arr = new JsonArray();

                        try (PreparedStatement ps = conn.prepareStatement(sql);
                             ResultSet rs = ps.executeQuery()) {

                            while (rs.next()) {
                                JsonObject obj = new JsonObject();
                                obj.addProperty("id", rs.getInt("id"));
                                obj.addProperty("username", rs.getString("username"));
                                obj.addProperty("name", rs.getString("name"));
                                obj.addProperty("role", rs.getString("role"));
                                obj.addProperty("created_at", rs.getString("created_at"));
                                arr.add(obj);
                            }
                        }

                        writeJson(he, 200, arr);
                        status = 200;
                        return;
                    }
                }
            }

            // ============================
            // POST /api/users  (crear usuario)
            // ============================
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/users")) {

                // (Opcional) exigir admin:
                // if (!requireAdmin(he)) return;

                JsonObject body = readJsonBody(he);

                String username = safeGet(body, "username");
                String name = safeGet(body, "name");
                String password = safeGet(body, "password");
                String role = safeGet(body, "role"); // ADMIN o CLIENTE

                if (username.isEmpty() || name.isEmpty() || password.isEmpty() || role.isEmpty()) {
                    writeJson(he, 400, jsonMsg("Missing fields (username, name, password, role)"));
                    status = 400;
                    return;
                }
                if (!role.equals("ADMIN") && !role.equals("CLIENTE")) {
                    writeJson(he, 400, jsonMsg("Role must be ADMIN or CLIENTE"));
                    status = 400;
                    return;
                }

                try (Connection conn = DBConnection.getConnection()) {

                    // obtener role_id
                    int roleId;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM roles WHERE name = ?")) {
                        ps.setString(1, role);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                writeJson(he, 400, jsonMsg("Role not found in roles table"));
                                status = 400;
                                return;
                            }
                            roleId = rs.getInt("id");
                        }
                    }

                    String sql = "INSERT INTO users(username, name, password, user_role) VALUES (?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, username);
                        ps.setString(2, name);
                        ps.setString(3, password);
                        ps.setInt(4, roleId);
                        ps.executeUpdate();
                    }
                }

                writeJson(he, 201, jsonMsg("User created"));
                status = 201;
                return;
            }

            // ============================
            // No route
            // ============================
            status = 404;
            he.sendResponseHeaders(404, -1);

        } catch (Exception e) {
            log.error("Request failed {} {} - {}", method, path, e.toString(), e);
            try { he.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
        } finally {
            log.info("Completed {} {} status={} timeMs={}", method, path, status, (System.currentTimeMillis() - start));
            try { he.close(); } catch (Exception ignored) {}
        }
    }

    // ===== Helpers =====

    private static void applyCors(HttpExchange he) {
        he.getResponseHeaders().set("Access-Control-Allow-Origin", FRONTEND_ORIGIN);
        he.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        he.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        he.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    private static JsonObject readJsonBody(HttpExchange he) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        if (sb.toString().isEmpty()) return new JsonObject();
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }

    private static String safeGet(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString().trim()
                : "";
    }

    private static JsonObject jsonMsg(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        return obj;
    }

    private static void writeJson(HttpExchange he, int code, JsonObject obj) throws Exception {
        byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void writeJson(HttpExchange he, int code, JsonArray arr) throws Exception {
        byte[] bytes = arr.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }

    // (Opcional) protección admin por token:
    // private static boolean requireAdmin(HttpExchange he) throws Exception {
    //     String auth = he.getRequestHeaders().getFirst("Authorization");
    //     if (auth == null || !auth.startsWith("Bearer ")) {
    //         he.sendResponseHeaders(401, -1);
    //         return false;
    //     }
    //     String token = auth.substring("Bearer ".length());
    //     AuthStore.Session s = AuthStore.getSession(token);
    //     if (s == null) {
    //         he.sendResponseHeaders(401, -1);
    //         return false;
    //     }
    //     if (!"ADMIN".equalsIgnoreCase(s.role)) {
    //         he.sendResponseHeaders(403, -1);
    //         return false;
    //     }
    //     return true;
    // }
}