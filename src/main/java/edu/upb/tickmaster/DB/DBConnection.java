package edu.upb.tickmaster.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/ticketmaster";
    private static final String USER = "postgres";
    private static final String PASS = "NuevaContraseñaFuerte123!";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
