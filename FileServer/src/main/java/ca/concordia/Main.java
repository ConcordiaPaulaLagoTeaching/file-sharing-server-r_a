package ca.concordia;

import ca.concordia.server.FileServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.printf("Hello and welcome!");

        FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);
        // Start the file server
        server.start();
    }
}