package edu.upb.tickmaster;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Registarr {

    public static void registrarBackend() throws Exception {
        Properties p = new Properties();
        try (InputStream is = Registarr.class.getClassLoader().getResourceAsStream("backend.properties")) {
            if (is == null) throw new RuntimeException("backend.properties missing");
            p.load(is);
        }

        String ip = p.getProperty("backend.ip", "127.0.0.1");
        int puerto = Integer.parseInt(p.getProperty("backend.puerto", "1914"));
        String proxyIp = p.getProperty("proxy.ip", "127.0.0.1");
        int proxyPuerto = Integer.parseInt(p.getProperty("proxy.puerto", "1916"));

        Map<String, Object> data = new HashMap<>();
        data.put("ip", ip);
        data.put("puerto", puerto);
        byte[] jsonBytes = new Gson().toJson(data).getBytes(StandardCharsets.UTF_8);

        String urlStr = "http://" + proxyIp + ":" + proxyPuerto + "/registrar";
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBytes);
        }

        int code = conn.getResponseCode();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code);
        }

        System.out.println("Registrado: " + ip + ":" + puerto);
    }
}