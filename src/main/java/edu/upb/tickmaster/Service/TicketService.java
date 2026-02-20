package edu.upb.tickmaster.Service;

import edu.upb.tickmaster.DAO.EstadoDAO;
import edu.upb.tickmaster.DAO.TicketDAO;
import edu.upb.tickmaster.Model.Ticket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();

    public List<Ticket> listarTickets() throws Exception {
        return ticketDAO.listarTickets();
    }

    // =========================
    // CREAR TICKET (Opción B)
    // =========================
    public Ticket crearTicket(Ticket ticket) throws Exception {

        if (ticket == null) throw new Exception("Ticket vacío");

        if (ticket.getTitulo() == null || ticket.getTitulo().trim().isEmpty())
            throw new Exception("Título obligatorio");

        if (ticket.getUsuarioId() <= 0)
            throw new Exception("Usuario inválido");

        // 🔹 Validar usuario
        if (!usuarioExiste(ticket.getUsuarioId()))
            throw new Exception("Usuario no existe");

        // ✅ fecha_creacion por defecto si no llega en el JSON
        if (ticket.getFecha_creacion() == null) {
            ticket.setFecha_creacion(new java.util.Date());
        }

        // ✅ Opción B: estadoId si viene, si no -> NUEVO
        Integer estadoId = ticket.getEstadoId();
        if (estadoId == null || estadoId <= 0) {
            EstadoDAO estadoDAO = new EstadoDAO();
            int idNuevo = estadoDAO.obtenerIdPorNombre("NUEVO");
            ticket.setEstadoId(idNuevo);
        } else {
            ticket.setEstadoId(estadoId);
        }

        // 🔹 Guardar en BD
        return ticketDAO.crearTicket(ticket);
    }

    // =========================
    // VALIDAR USUARIO
    // =========================
    private boolean usuarioExiste(int usuarioId) {
        try {
            URL url = new URL("http://localhost:1914/usuarios?id=" + usuarioId);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return false;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            br.close();
            conn.disconnect();

            return response.toString().contains("\"id\"");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}