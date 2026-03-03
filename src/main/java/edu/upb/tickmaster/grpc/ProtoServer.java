package edu.upb.tickmaster.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

public class ProtoServer {

    public void start() throws IOException, InterruptedException {

        Server server = ServerBuilder.forPort(8081)
                .addService(new ProductoServiceImpl())
                .addService(new TicketGrpcServiceImpl()) // ✅ IMPORTANTE
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("Iniciando servidor gRPC en el puerto 8081...");
        server.start();
        System.out.println("Servidor escuchando...");
        server.awaitTermination();
    }
}