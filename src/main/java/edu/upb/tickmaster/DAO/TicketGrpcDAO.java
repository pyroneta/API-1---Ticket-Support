package edu.upb.tickmaster.DAO;

import edu.upb.tickmaster.DB.DBConnection;

import java.sql.*;

public class TicketGrpcDAO {

    public TicketRow insert(String titulo, String nombreCliente) throws SQLException {
        String sql = "INSERT INTO ticketgrpc (titulo, nombre_cliente) VALUES (?, ?) RETURNING id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, titulo);
            ps.setString(2, nombreCliente);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    return new TicketRow(id, titulo, nombreCliente);
                }
                throw new SQLException("No se devolvió id (RETURNING)");
            }
        }
    }

    public TicketRow findById(int id) throws SQLException {
        String sql = "SELECT id, titulo, nombre_cliente FROM ticketgrpc WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new TicketRow(
                        rs.getInt("id"),
                        rs.getString("titulo"),
                        rs.getString("nombre_cliente")
                );
            }
        }
    }

    public java.util.List<TicketRow> listAll() throws java.sql.SQLException{
        String sql = "SELECT id, titulo, nombre_cliente FROM ticketgrpc ORDER BY id ASC";

        try(Connection conn = DBConnection.getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery()) {
            java.util.List<TicketRow> out = new java.util.ArrayList<>();
            while (rs.next()) {
                out.add(new TicketRow(
                        rs.getInt("id"),
                        rs.getString("titulo"),
                        rs.getString("nombre_cliente")
                ));
            }
            return out;
        }
    }

    public static class TicketRow {
        private int id;
        private String titulo;
        private String nombreCliente;

        public TicketRow(int id, String titulo, String nombreCliente) {
            this.id = id;
            this.titulo = titulo;
            this.nombreCliente = nombreCliente;
        }

        public int getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getNombreCliente() { return nombreCliente; }
    }
}