package edu.upb.tickmaster.Model;


import java.util.Date;

public class Ticket {

    private int id;
    private String titulo;
    private String descripcion;
    private Date fecha_creacion;
    private Date fecha_cierre;
    private int usuarioId;
    private int empleadoId;
    private int estadoId;

    public Ticket() {
    }

    public Ticket(int id, String titulo, String descripcion,
                  Date fecha_creacion, Date fecha_cierre,
                  int usuarioId, int empleadoId,
                  int estadoId) {

        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha_creacion = fecha_creacion;
        this.fecha_cierre = fecha_cierre;
        this.usuarioId = usuarioId;
        this.empleadoId = empleadoId;
        this.estadoId = estadoId;
    }

    // =============================
    // GETTERS Y SETTERS
    // =============================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Date getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(Date fecha_creacion) { this.fecha_creacion = fecha_creacion; }

    public Date getFecha_cierre() { return fecha_cierre; }
    public void setFecha_cierre(Date fecha_cierre) { this.fecha_cierre = fecha_cierre; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }

    public int getEstadoId() { return estadoId; }
    public void setEstadoId(int estadoId) { this.estadoId = estadoId; }
}
