package edu.upb.tickmaster.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {
    public static String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}