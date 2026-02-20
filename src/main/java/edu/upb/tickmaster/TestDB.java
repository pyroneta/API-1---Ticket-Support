package edu.upb.tickmaster;
import edu.upb.tickmaster.DB.DBConnection;
import java.sql.Connection;

public class TestDB {

    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("CONEXIÓN A POSTGRES OK");
        } catch (Exception e) {
            System.out.println("ERROR DE CONEXIÓN");
            e.printStackTrace();
        }
    }
}
