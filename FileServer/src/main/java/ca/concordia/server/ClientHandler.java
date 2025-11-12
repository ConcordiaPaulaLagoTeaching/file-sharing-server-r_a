package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler implements Runnable {
    // Create a thread of type CLientHandler
    // Thread calls run method when threadname.start() is called

    private final Socket clientSocket;
    private final FileSystemManager fsManager;

    public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
    }

    // code execute by the thread
    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Optionally remove this println; Only to test if thread is created and runs as expected
            writer.println("Client Thread running");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.split(" ");
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "CREATE":
                        fsManager.createFile(parts[1]);
                        writer.println("SUCCESS: File '" + parts[1] + "' created.");
                        writer.flush();
                        break;
                    //TODO: Implement other commands READ, WRITE, DELETE, LIST
                    // should call the corresponding functions in FileSystemManager
                    // implement synchronization (lock or semaphore)
                    // parts[1] should represent file name
                    case "READ":
                        // data is an array of bytes, representing the raw contents of the file being passed
                        // do if parts[1] is not null
                        if (parts[1] != null){
                            try{
                                byte[] data =  fsManager.readFile(parts[1]);
                                writer.println("CONTENTS: " + new String(data));
                            } catch (Exception e){
                                writer.println("ERROR: " + e.getMessage());
                            }
                        }
                        break;
                    case "DELETE":
                        if (parts[1] != null) fsManager.deleteFile(parts[1]);
                        writer.println("SUCCESS: File '" + parts[1] + "' deleted.");
                        break;
                    case "WRITE":
                        fsManager.writeFile(parts[1], contents.getBytes());
                        writer.println("SUCCESS: File '" + parts[1] + "' written.");
                        break;
                    case "LIST":
                        String[] lsFiles;
                        if (parts[1] != null) lsFiles = fsManager.listFiles(parts[1]);
                        writer.println("SUCCESS: List of Files: " + Arrays.toString(lsFiles));
                        break;
                    case "QUIT":
                        writer.println("SUCCESS: Disconnecting.");
                        return;
                    default:
                        writer.println("ERROR: Unknown command.");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
