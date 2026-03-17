package edu.upb.tickmaster.Handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WebhookHmacHandler implements HttpHandler {

    private static final String SECRET = "vivaeltigre";

    @Override
    public void handle(HttpExchange exchange) {

        try {

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String firma = exchange.getRequestHeaders().getFirst("x-firma");

            if (firma == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }


            byte[] body = readBody(exchange);
            String bodyTexto = new String(body, StandardCharsets.UTF_8);
            System.out.println("Body recibido: " + bodyTexto);

            String hmac_esperado = hmacSha256(body);
            System.out.println("Firma recibida:  " + firma);
            System.out.println("Firma calculada: " + hmac_esperado);

            // comparar firmas
            boolean comparar_firmas = MessageDigest.isEqual(
                    hmac_esperado.getBytes(StandardCharsets.UTF_8),
                    firma.getBytes(StandardCharsets.UTF_8)
            );

            String response;

            if (comparar_firmas) {
                response = "firma valida";
                exchange.sendResponseHeaders(200, response.length());
            } else {
                response = "firma invalida";
                exchange.sendResponseHeaders(401, response.length());
            }

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String hmacSha256(byte[] data) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(key);

        byte[] raw = mac.doFinal(data);

        StringBuilder hex = new StringBuilder();

        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
    private byte[] readBody(HttpExchange exchange) throws Exception {

        java.io.InputStream is = exchange.getRequestBody();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

        byte[] data = new byte[1024];
        int nRead;

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}