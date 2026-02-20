package edu.upb.tickmaster.Handler;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.DB.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HealthHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(HealthHandler.class);
    private static final Path DISK_PATH = Paths.get(".");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            ex.close();
            return;
        }

        boolean dbOk = checkDb();
        boolean diskOk = checkDisk();
        boolean systemOk = dbOk && diskOk;

        JsonObject out = new JsonObject();
        out.addProperty("status", systemOk ? "OK" : "DOWN");
        out.addProperty("db", dbOk);
        out.addProperty("disk", diskOk);

        byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(systemOk ? 200 : 503, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();

        log.info("Health check status={} dbOk={} diskOk={}", systemOk ? "OK" : "DOWN", dbOk, diskOk);
    }

    private boolean checkDb() {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (Exception e) {
            log.warn("DB health failed: {}", e.toString());
            return false;
        }
    }

    private boolean checkDisk() {
        try {
            return Files.getFileStore(DISK_PATH).getUsableSpace() > 0;
        } catch (Exception e) {
            log.warn("Disk health failed: {}", e.toString());
            return false;
        }
    }
}
