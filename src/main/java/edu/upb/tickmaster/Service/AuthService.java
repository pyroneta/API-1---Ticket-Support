package edu.upb.tickmaster.Service;

import edu.upb.tickmaster.DAO.AuthDAO;
import edu.upb.tickmaster.DB.DBConnection;
import edu.upb.tickmaster.Util.AuthStore;

import java.sql.Connection;

public class AuthService {

    private final AuthDAO authDAO = new AuthDAO();

    public static class LoginResult {
        public final String token;
        public final int userId;
        public final String role;
        public LoginResult(String token, int userId, String role) {
            this.token = token;
            this.userId = userId;
            this.role = role;
        }
    }

    public LoginResult login(String username, String password) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            AuthDAO.LoginRow row = authDAO.login(conn, username, password);
            if (row == null) return null;

            String token = AuthStore.createToken(row.userId, row.role);
            return new LoginResult(token, row.userId, row.role);
        }
    }
}