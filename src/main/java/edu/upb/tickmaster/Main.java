package edu.upb.tickmaster;

import edu.upb.tickmaster.grpc.ProtoServer;
import edu.upb.tickmaster.httpserver.ApacheServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ApacheServer apacheServer = new ApacheServer();
        boolean ok = apacheServer.start();
        System.out.println("Start() returned: " + ok);

        try {
            Registarr.registrarBackend();
            System.out.println("Registro al balanceador OK");
        } catch (Exception e) {
            System.out.println("No se pudo registrar al balanceador " + e.getMessage());
            e.printStackTrace();
        }
        ProtoServer protoServer = new ProtoServer();
        protoServer.start();
    }
}
