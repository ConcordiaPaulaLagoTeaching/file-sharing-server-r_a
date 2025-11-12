package tests;

import helpers.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class ThreadManagementTests {
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
    void testMultipleClientsConcurrentAccess() throws Exception {
        int numClients = 20;
        ExecutorService pool = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);
        ConcurrentLinkedQueue<String> responses = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numClients; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String res = ClientRunner.send("CREATE file" + id);
                    responses.add(res);
                } catch (Exception e) {
                    fail("Client " + id + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Server did not handle concurrent clients in time");
        assertEquals(numClients, responses.size(), "Not all clients received responses");
    }

    @Test
    void testReadersAndWritersSynchronization() throws Exception {
        ClientRunner.send("CREATE shared");
        ClientRunner.send("WRITE shared hello");

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        // Start readers
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                try {
                    String res = ClientRunner.send("READ shared");
                    assertTrue(res.contains("hello"));
                } catch (Exception e) {
                    fail("Reader failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Start one writer (should not conflict with readers)
        pool.submit(() -> {
            try {
                ClientRunner.send("WRITE shared world");
            } catch (Exception e) {
                fail("Writer failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Threads did not complete properly — possible deadlock");
    }

    @Test
    void testDeadlockPreventionUnderStress() throws Exception {
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String cmd;
                    if (id % 3 == 0) cmd = "WRITE shared data" + id;
                    else cmd = "READ shared";
                    ClientRunner.send(cmd);
                } catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Possible deadlock: not all threads finished");
    }






}
