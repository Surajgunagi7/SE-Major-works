package view;

import java.io.*;
import java.net.Socket;

public class Client implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    public Client() {
        done = false;
    }

    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 1234);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            Thread inputThread = new Thread(new InputHandler());
            inputThread.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            System.err.println("[CLIENT ERROR] " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        done = true;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null) client.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    private class InputHandler implements Runnable {
        @Override
        public void run() {
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (!done) {
                    String message = inputReader.readLine();
                    if (message.equalsIgnoreCase("/quit")) {
                        out.println(message);
                        shutdown();
                        break;
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
