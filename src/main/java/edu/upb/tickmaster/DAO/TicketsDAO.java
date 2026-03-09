package edu.upb.tickmaster.DAO;

import java.sql.*;

public class TicketsDAO {

    public int countSold(Connection conn, int ticketTypeId) throws Exception {
        String sql = "SELECT COUNT(*) AS c FROM tickets WHERE ticket_type_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("c");
            }
        }
    }

    public static class InsertTicketRow {
        public final int ticketId;
        public final Timestamp purchaseDate;
        public InsertTicketRow(int ticketId, Timestamp purchaseDate) {
            this.ticketId = ticketId;
            this.purchaseDate = purchaseDate;
        }
    }

    public InsertTicketRow insertTicket(Connection conn, int eventId, int userId, int ticketTypeId) throws Exception {
        String sql =
                "INSERT INTO tickets(event_id, user_id, ticket_type_id) " +
                        "VALUES (?, ?, ?) " +
                        "RETURNING id, purchase_date";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ps.setInt(3, ticketTypeId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new InsertTicketRow(rs.getInt("id"), rs.getTimestamp("purchase_date"));
            }
        }
    }
}