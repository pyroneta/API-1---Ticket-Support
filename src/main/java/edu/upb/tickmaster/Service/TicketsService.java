package edu.upb.tickmaster.Service;

import edu.upb.tickmaster.DAO.TicketTypesDAO;
import edu.upb.tickmaster.DAO.TicketsDAO;
import edu.upb.tickmaster.DB.DBConnection;

import java.sql.Connection;
import java.sql.Timestamp;

public class TicketsService {

    private final TicketTypesDAO ticketTypesDAO = new TicketTypesDAO();
    private final TicketsDAO ticketsDAO = new TicketsDAO();

    public static class BuyResult {
        public final int ticketId;
        public final int eventId;
        public final int userId;
        public final int ticketTypeId;
        public final Timestamp purchaseDate;

        public BuyResult(int ticketId, int eventId, int userId, int ticketTypeId, Timestamp purchaseDate) {
            this.ticketId = ticketId;
            this.eventId = eventId;
            this.userId = userId;
            this.ticketTypeId = ticketTypeId;
            this.purchaseDate = purchaseDate;
        }
    }

    public BuyResult buyTicket(int userId, int ticketTypeId) throws Exception {
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            TicketTypesDAO.TicketTypeRow tt = ticketTypesDAO.lockByIdForUpdate(conn, ticketTypeId);
            if (tt == null) {
                conn.rollback();
                return null; // handler lo traduce a 404
            }

            if (!tt.active) {
                conn.rollback();
                throw new IllegalStateException("ticket_type is not active");
            }

            int sold = ticketsDAO.countSold(conn, ticketTypeId);
            if (sold >= tt.quota) {
                conn.rollback();
                throw new IllegalStateException("Sold out");
            }

            TicketsDAO.InsertTicketRow ins = ticketsDAO.insertTicket(conn, tt.eventId, userId, ticketTypeId);

            conn.commit();
            return new BuyResult(ins.ticketId, tt.eventId, userId, ticketTypeId, ins.purchaseDate);

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            throw e;

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }
}