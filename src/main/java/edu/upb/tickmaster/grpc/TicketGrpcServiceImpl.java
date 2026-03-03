package edu.upb.tickmaster.grpc;

import edu.upb.tickmaster.DAO.TicketGrpcDAO;
import edu.upb.tickmaster.grpc.Empty;
import edu.upb.tickmaster.grpc.TicketListResponse;
import edu.upb.tickmaster.grpc.TicketItem;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.sql.SQLException;
import java.time.Instant;

public class TicketGrpcServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {

    private final TicketGrpcDAO dao = new TicketGrpcDAO();

    @Override
    public void crearTicket(CrearTicketRequest request,
                            StreamObserver<TicketResponse> responseObserver) {

        String titulo = request.getTitulo().trim();
        String nombreCliente = request.getNombreCliente().trim();

        if (titulo.isEmpty() || nombreCliente.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("titulo y nombre_cliente son obligatorios")
                            .asRuntimeException()
            );
            return;
        }

        try {
            TicketGrpcDAO.TicketRow row = dao.insert(titulo, nombreCliente);

            TicketResponse response = TicketResponse.newBuilder()
                    .setId(String.valueOf(row.getId()))
                    .setTitulo(row.getTitulo())
                    .setNombreCliente(row.getNombreCliente())
                    .setCodigoRespuesta("201_CREATED")
                    .setMensaje("Ticket creado en PostgreSQL (ticketgrpc).")
                    .setTimestamp(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SQLException e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error BD: " + e.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Error inesperado: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void obtenerTicket(IdRequest request,
                              StreamObserver<TicketResponse> responseObserver) {

        int id;

        try {
            id = Integer.parseInt(request.getId().trim());
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("El id debe ser numérico")
                            .asRuntimeException()
            );
            return;
        }

        try {
            TicketGrpcDAO.TicketRow row = dao.findById(id);

            if (row == null) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("No existe ticket con id=" + id)
                                .asRuntimeException()
                );
                return;
            }

            TicketResponse response = TicketResponse.newBuilder()
                    .setId(String.valueOf(row.getId()))
                    .setTitulo(row.getTitulo())
                    .setNombreCliente(row.getNombreCliente())
                    .setCodigoRespuesta("200_OK")
                    .setMensaje("Ticket encontrado.")
                    .setTimestamp(Instant.now().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (SQLException e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error BD: " + e.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Error inesperado: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }
    @Override

    public void listarTickets(Empty request, StreamObserver<TicketListResponse> responseObserver) {
        try {
            java.util.List<TicketGrpcDAO.TicketRow> rows = dao.listAll();

            TicketListResponse.Builder resp = TicketListResponse.newBuilder()
                    .setCodigoRespuesta("200_OK")
                    .setMensaje("Listado de tickets.")
                    .setTimestamp(java.time.Instant.now().toString());

            for (TicketGrpcDAO.TicketRow row : rows) {
                resp.addTickets(
                        TicketItem.newBuilder()
                                .setId(String.valueOf(row.getId()))
                                .setTitulo(row.getTitulo())
                                .setNombreCliente(row.getNombreCliente())
                                .build()
                );
            }

            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();

        } catch (java.sql.SQLException e) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Error BD: " + e.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.UNKNOWN
                            .withDescription("Error inesperado: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}