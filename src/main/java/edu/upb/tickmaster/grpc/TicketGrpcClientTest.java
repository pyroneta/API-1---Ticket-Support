package edu.upb.tickmaster.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class TicketGrpcClientTest {

    public static void main(String[] args) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", 8081)  // ✅ gRPC PORT
                .usePlaintext()
                .build();

        // Este stub es el "cliente"
        TicketServiceGrpc.TicketServiceBlockingStub stub =
                TicketServiceGrpc.newBlockingStub(channel);

        try {
            // 1) INVOCAR crearTicket
            TicketResponse created = stub.crearTicket(
                    CrearTicketRequest.newBuilder()
                            .setTitulo("No funciona el login")
                            .setNombreCliente("Fernando")
                            .build()
            );

            System.out.println("CREATED => " + created);

            // 2) INVOCAR obtenerTicket
            TicketResponse fetched = stub.obtenerTicket(
                    IdRequest.newBuilder()
                            .setId(created.getId())
                            .build()
            );

            System.out.println("FETCHED => " + fetched);

        } finally {
            channel.shutdown();
        }
    }
}