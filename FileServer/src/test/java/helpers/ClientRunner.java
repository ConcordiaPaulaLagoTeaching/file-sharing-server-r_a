package helpers;

import java.io.*;
import java.net.*;

public class ClientRunner {
    public static String send(String command) throws IOException {
        try (Socket s = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            out.println(command);
            return in.readLine();
        }
    }
}

