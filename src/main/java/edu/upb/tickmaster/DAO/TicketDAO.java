package edu.upb.tickmaster.DAO;

import edu.upb.tickmaster.DB.DBConnection;
import edu.upb.tickmaster.Model.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    // =========================
    // INSERTAR TICKET
    // =========================
    public Ticket crearTicket(Ticket ticket) throws Exception {

        String sql =
                "INSERT INTO ticket " +
                        "(titulo, descripcion, fecha_creacion, usuario_id, empleado_id, estado_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "RETURNING id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitulo());
            ps.setString(2, ticket.getDescripcion());
            ps.setTimestamp(3, new Timestamp(ticket.getFecha_creacion().getTime()));
            ps.setInt(4, ticket.getUsuarioId());
            ps.setInt(5, ticket.getEmpleadoId());
            ps.setInt(6, ticket.getEstadoId());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ticket.setId(rs.getInt("id"));
            }

            return ticket;

        } catch (SQLException e) {
            throw new Exception("Error insertando ticket", e);
        }
    }

    // =========================
    // LISTAR TICKETS
    // =========================
    public List<Ticket> listarTickets() throws Exception {

        String sql = "SELECT * FROM ticket";

        List<Ticket> lista = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Ticket t = new Ticket();

                t.setId(rs.getInt("id"));
                t.setTitulo(rs.getString("titulo"));
                t.setDescripcion(rs.getString("descripcion"));
                t.setFecha_creacion(rs.getTimestamp("fecha_creacion"));
                t.setFecha_cierre(rs.getTimestamp("fecha_cierre"));
                t.setUsuarioId(rs.getInt("usuario_id"));
                t.setEmpleadoId(rs.getInt("empleado_id"));
                t.setEstadoId(rs.getInt("estado_id"));

                lista.add(t);
            }

            return lista;

        } catch (SQLException e) {
            throw new Exception("Error listando tickets", e);
        }
    }

    // =========================
    // BUSCAR POR ID
    // =========================
    public Ticket buscarPorId(int id) throws Exception {

        String sql = "SELECT * FROM ticket WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                Ticket t = new Ticket();

                t.setId(rs.getInt("id"));
                t.setTitulo(rs.getString("titulo"));
                t.setDescripcion(rs.getString("descripcion"));
                t.setFecha_creacion(rs.getTimestamp("fecha_creacion"));
                t.setFecha_cierre(rs.getTimestamp("fecha_cierre"));
                t.setUsuarioId(rs.getInt("usuario_id"));
                t.setEmpleadoId(rs.getInt("empleado_id"));
                t.setEstadoId(rs.getInt("estado_id"));

                return t;
            }

            throw new Exception("Ticket no encontrado");

        } catch (SQLException e) {
            throw new Exception("Error buscando ticket", e);
        }
    }
}
