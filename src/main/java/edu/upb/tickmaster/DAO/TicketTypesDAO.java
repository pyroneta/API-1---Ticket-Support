package edu.upb.tickmaster.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TicketTypesDAO {

    public static class TicketTypeRow {
        public final int id;
        public final int eventId;
        public final int quota;
        public final boolean active;
        public TicketTypeRow(int id, int eventId, int quota, boolean active) {
            this.id = id; this.eventId = eventId; this.quota = quota; this.active = active;
        }
    }

    public TicketTypeRow lockByIdForUpdate(Connection conn, int ticketTypeId) throws Exception {
        String sql =
                "SELECT id, event_id, quota, is_active " +
                        "FROM ticket_types " +
                        "WHERE id = ? " +
                        "FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new TicketTypeRow(
                        rs.getInt("id"),
                        rs.getInt("event_id"),
                        rs.getInt("quota"),
                        rs.getBoolean("is_active")
                );
            }
        }
    }
}