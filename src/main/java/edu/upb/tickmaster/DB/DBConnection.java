package edu.upb.tickmaster.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null) ? defaultValue : value;
    }

    public static Connection getConnection() throws SQLException {

        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "5432");
        String db   = getEnv("DB_NAME", "ticketmaster");
        String user = getEnv("DB_USER", "postgres");
        String pass = getEnv("DB_PASSWORD", "");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;

        return DriverManager.getConnection(url, user, pass);
    }
}