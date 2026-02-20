package edu.upb.tickmaster.Handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.DB.DBConnection;
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

    @Override
    public void handle(HttpExchange he) {

        log.debug("DEBUG TEST - Entrando al handler de usuarios");

        long start = System.currentTimeMillis();
        String method = he.getRequestMethod();
        String path = he.getRequestURI() != null ? he.getRequestURI().getPath() : "";
        String query = he.getRequestURI() != null ? he.getRequestURI().getQuery() : null;

        int status = 500;

        try {
            he.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            log.info("Incoming request {} {}{}", method, path, (query != null ? "?" + query : ""));

            // ============================
            // GET
            // ============================
            if ("GET".equalsIgnoreCase(method)) {
                String response;

                try (Connection conn = DBConnection.getConnection()) {

                    // /usuarios?id=5
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.split("=")[1]);

                        String sql = "SELECT id, nombre, email, telefono FROM usuario WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, id);

                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) {
                                    status = 404;
                                    he.sendResponseHeaders(404, -1);
                                    return;
                                }

                                JsonObject obj = new JsonObject();
                                obj.addProperty("id", rs.getInt("id"));
                                obj.addProperty("nombre", rs.getString("nombre"));
                                obj.addProperty("email", rs.getString("email"));
                                obj.addProperty("telefono", rs.getString("telefono"));

                                response = obj.toString();
                            }
                        }

                    } else {
                        // /usuarios
                        String sql = "SELECT id, nombre, email, telefono FROM usuario";
                        JsonArray array = new JsonArray();

                        try (PreparedStatement ps = conn.prepareStatement(sql);
                             ResultSet rs = ps.executeQuery()) {

                            while (rs.next()) {
                                JsonObject obj = new JsonObject();
                                obj.addProperty("id", rs.getInt("id"));
                                obj.addProperty("nombre", rs.getString("nombre"));
                                obj.addProperty("email", rs.getString("email"));
                                obj.addProperty("telefono", rs.getString("telefono"));
                                array.add(obj);
                            }
                        }

                        response = array.toString();
                    }
                }

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                status = 200;
                he.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            // ============================
            // POST
            // ============================
            if ("POST".equalsIgnoreCase(method)) {
                StringBuilder sb = new StringBuilder();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

                String nombre = json.get("nombre").getAsString();
                String email = json.get("email").getAsString();
                String telefono = json.get("telefono").getAsString();

                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "INSERT INTO usuario(nombre, email, telefono) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nombre);
                        ps.setString(2, email);
                        ps.setString(3, telefono);
                        ps.executeUpdate();
                    }
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("status", "OK");
                obj.addProperty("message", "Usuario creado");

                String response = obj.toString();
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                status = 201;
                he.sendResponseHeaders(201, bytes.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            // ============================
            // Otros métodos
            // ============================
            status = 405;
            he.sendResponseHeaders(405, -1);

        } catch (Exception e) {
            log.error("Request failed {} {} - {}", method, path, e.toString(), e);
            try {
                he.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        } finally {
            log.info("Completed {} {} status={} timeMs={}", method, path, status, (System.currentTimeMillis() - start));
            try { he.close(); } catch (Exception ignored) {}
        }
    }
}
