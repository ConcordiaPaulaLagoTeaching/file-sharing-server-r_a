package helpers;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.Instant;

public class ServerRunner {
    private Process process;

    public void start() throws IOException, InterruptedException {
        process = new ProcessBuilder("java", "-cp", "target/classes", "ca.concordia.Main")
                .redirectErrorStream(true)
                .start();
        // Wait for port to become available (server ready)
        Instant start = Instant.now();
        while (!isPortOpen("localhost", 12345)) {
            if (Duration.between(start, Instant.now()).getSeconds() > 10)
                throw new RuntimeException("Server failed to start within timeout");
            Thread.sleep(200);
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }


    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}

