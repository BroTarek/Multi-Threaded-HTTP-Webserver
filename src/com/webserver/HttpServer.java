package com.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int QUEUE_CAPACITY = 100;

    private final int port;
    private final ThreadPool threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public HttpServer(int port) {
        this.port = port;
        int numCores = Runtime.getRuntime().availableProcessors();
        System.out.println("[HttpServer] Detected " + numCores + " available CPU core(s)");
        this.threadPool = new ThreadPool(numCores, QUEUE_CAPACITY);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("=================================================");
            System.out.println(" Java Multi-Threaded HTTP Web Server Started!");
            System.out.println(" Listening on port: http://localhost:" + port);
            System.out.println(" Concurrency: Producer-Consumer via BlockingQueue<Runnable>");
            System.out.println("=================================================");

            // Register graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[HttpServer] Shutting down server...");
                stop();
            }));

            // Producer accept loop
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Wrap connection in Runnable task (ClientHandler) and enqueue
                    threadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (!running) {
                        System.out.println("[HttpServer] Server socket closed.");
                        break;
                    }
                    System.err.println("[HttpServer] Error accepting client connection: " + e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("[HttpServer] Listener loop interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[HttpServer] Could not bind to port " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[HttpServer] Error closing ServerSocket: " + e.getMessage());
            }
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port argument. Using default port " + DEFAULT_PORT);
            }
        }

        HttpServer server = new HttpServer(port);
        server.start();
    }
}
