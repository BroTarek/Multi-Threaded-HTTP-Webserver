package com.webserver;

import java.net.Socket;

/**
 * ClientHandler represents the unit of work for each client connection.
 * Explicitly implements java.lang.Runnable so it can be enqueued into
 * a BlockingQueue<Runnable> and executed by worker threads in the pool.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        RequestHandler.handleConnection(clientSocket);
    }
}
