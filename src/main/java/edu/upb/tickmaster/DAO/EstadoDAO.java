package edu.upb.tickmaster.DAO;
import edu.upb.tickmaster.DB.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// EstadoDAO.java
public class EstadoDAO
{

    public int obtenerIdPorNombre(String nombre) throws Exception {
        String sql = "SELECT id FROM estado WHERE nombre = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }

            throw new Exception("Estado no existe: " + nombre);
        }
    }
}
