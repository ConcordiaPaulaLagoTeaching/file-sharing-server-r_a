import helpers.ClientRunner;
import helpers.ServerRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerTests {

    static ServerRunner server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new ServerRunner();
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    void testServerRecoversAfterErrorCommand() throws Exception {
        // Send malformed command
        String bad = ClientRunner.send("WRIT invalid");
        assertTrue(bad.startsWith("ERROR"), "Server should report an error");

        // Send valid command afterward to verify server still responsive
        String ok = ClientRunner.send("LIST");
        assertNotNull(ok, "Server did not respond after error");
    }

    @Test
    void testMalformedInputDoesNotCrashServer() throws Exception {
        for (String cmd : new String[]{"", "BADCOMMAND", "CREATE", "WRITE", "READ", "DELETE"}) {
            try {
                String res = ClientRunner.send(cmd);
                assertTrue(res.startsWith("ERROR"), "Server should reject malformed input: " + cmd);
            } catch (IOException e) {
                fail("Server crashed on malformed input: " + cmd);
            }
        }
    }

    @Test
    @Timeout(15)
    void testHandlesHundredsOfClientsQuickly() throws Exception {
        int n = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try { ClientRunner.send("LIST"); }
                catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Server scaled poorly under 100 clients");
    }

    @Test
    void testServerRestartPersistence() throws Exception {
        // Step 1: Start server
        ServerRunner server = new ServerRunner();
        server.start();

        ClientRunner.send("CREATE persist");
        ClientRunner.send("WRITE persist saveddata");
        server.stop();

        // Step 2: Restart server
        ServerRunner server2 = new ServerRunner();
        server2.start();

        String response = ClientRunner.send("READ persist");
        assertTrue(response.contains("saveddata"), "File data not persisted across restart");

        server2.stop();
    }
}
