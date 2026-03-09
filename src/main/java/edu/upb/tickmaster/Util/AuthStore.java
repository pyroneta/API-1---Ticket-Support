package edu.upb.tickmaster.Util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthStore {

    public static class Session {
        public final int userId;
        public final String role;

        public Session(int userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static String createToken(int userId, String role) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(userId, role));
        return token;
    }

    public static Session getSession(String token) {
        return sessions.get(token);
    }
}