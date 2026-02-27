package edu.upb.tickmaster.DAO;

import edu.upb.tickmaster.DB.DBConnection;

import java.sql.*;

public class IdempotencyDAO {

    public static class IdemRow {
        public String idemKey;
        public String endpoint;
        public String requestHash;
        public String status;
        public Integer responseCode;
        public String responseBody;
    }

    public IdemRow find(String idemKey) throws Exception {
        String sql = "SELECT idem_key, endpoint, request_hash, status, response_code, response_body " +
                "FROM idempotency WHERE idem_key = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idemKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                IdemRow r = new IdemRow();
                r.idemKey = rs.getString("idem_key");
                r.endpoint = rs.getString("endpoint");
                r.requestHash = rs.getString("request_hash");
                r.status = rs.getString("status");
                int code = rs.getInt("response_code");
                r.responseCode = rs.wasNull() ? null : code;
                r.responseBody = rs.getString("response_body");
                return r;
            }
        }
    }

    // Intenta reservar la key. Si ya existe, lanza SQLException (duplicate key).
    public void insertInProgress(String idemKey, String endpoint, String requestHash) throws Exception {
        String sql = "INSERT INTO idempotency(idem_key, endpoint, request_hash, status) " +
                "VALUES (?, ?, ?, 'IN_PROGRESS')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idemKey);
            ps.setString(2, endpoint);
            ps.setString(3, requestHash);
            ps.executeUpdate();
        }
    }

    public void markDone(String idemKey, int code, String body) throws Exception {
        String sql = "UPDATE idempotency SET status='DONE', response_code=?, response_body=? WHERE idem_key=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, code);
            ps.setString(2, body);
            ps.setString(3, idemKey);
            ps.executeUpdate();
        }
    }
}