package de.unistuttgart.isw.sfsc.plc4x.example.endpoints;

import org.eclipse.milo.examples.server.ExampleServer;

public class StartMiloServer {
    public static void main(String[] args){


        try {
            ExampleServer testServer = new ExampleServer();
            testServer.startup().get();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
