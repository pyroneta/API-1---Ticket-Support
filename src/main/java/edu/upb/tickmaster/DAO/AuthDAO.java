package edu.upb.tickmaster.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthDAO {

    public static class LoginRow {
        public final int userId;
        public final String role;
        public LoginRow(int userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }

    public LoginRow login(Connection conn, String username, String password) throws Exception {
        String sql =
                "SELECT u.id, r.name AS role " +
                        "FROM users u " +
                        "JOIN roles r ON r.id = u.user_role " +
                        "WHERE u.username = ? AND u.password = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new LoginRow(rs.getInt("id"), rs.getString("role"));
            }
        }
    }
}