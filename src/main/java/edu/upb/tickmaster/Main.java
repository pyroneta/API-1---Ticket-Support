package edu.upb.tickmaster;

import edu.upb.tickmaster.httpserver.ApacheServer;

public class    Main {
    public static void main(String[] args) {
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
    }

}
