package com.webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RequestHandler {
    private static final String PUBLIC_DIR = "public";
    private static final String UPLOADS_DIR = "uploads";

    public static void handleConnection(Socket clientSocket) {
        try (Socket socket = clientSocket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            HttpRequest request = HttpRequest.parse(in);
            HttpResponse response = new HttpResponse();

            if (!request.isValid()) {
                response.setStatus(HttpStatus.BAD_REQUEST)
                        .setContentType("text/plain")
                        .setBody("400 Bad Request: Malformed HTTP Request");
                response.send(out);
                return;
            }

            System.out.println("[" + Thread.currentThread().getName() + "] " 
                    + request.getMethod() + " " + request.getPath());

            switch (request.getMethod()) {
                case GET:
                    handleGet(request, response);
                    break;
                case HEAD:
                    handleHead(request, response);
                    break;
                case POST:
                    handlePost(request, response);
                    break;
                case PUT:
                    handlePut(request, response);
                    break;
                case DELETE:
                    handleDelete(request, response);
                    break;
                default:
                    response.setStatus(HttpStatus.METHOD_NOT_ALLOWED)
                            .setContentType("text/plain")
                            .setBody("405 Method Not Allowed: " + request.getMethod());
                    break;
            }

            response.send(out);

        } catch (IOException e) {
            System.err.println("Error processing connection: " + e.getMessage());
        }
    }

    private static void handleGet(HttpRequest request, HttpResponse response) {
        String reqPath = request.getPath();
        if (reqPath.equals("/")) {
            reqPath = "/index.html";
        }

        // Prevent path traversal security vulnerability
        Path filePath = resolvePath(PUBLIC_DIR, reqPath);
        if (filePath == null || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            // Check in uploads directory if not found in public
            filePath = resolvePath(UPLOADS_DIR, reqPath);
        }

        if (filePath != null && Files.exists(filePath) && !Files.isDirectory(filePath)) {
            try {
                byte[] content = Files.readAllBytes(filePath);
                String mimeType = getMimeType(filePath.toString());
                response.setStatus(HttpStatus.OK)
                        .setContentType(mimeType)
                        .setBody(content);
            } catch (IOException e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .setContentType("text/plain")
                        .setBody("500 Internal Server Error");
            }
        } else {
            handleNotFound(response);
        }
    }

    private static void handleHead(HttpRequest request, HttpResponse response) {
        handleGet(request, response);
        // Remove body for HEAD request
        response.setBody(new byte[0]);
    }

    private static void handlePost(HttpRequest request, HttpResponse response) {
        ensureDirectoryExists(UPLOADS_DIR);
        String filename = "post_" + System.currentTimeMillis() + ".txt";
        Path targetPath = Paths.get(UPLOADS_DIR, filename);

        try {
            Files.write(targetPath, request.getBody(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String jsonResponse = "{\"message\":\"Resource created successfully\",\"filename\":\"" + filename + "\",\"size\":" + request.getBody().length + "}";
            response.setStatus(HttpStatus.CREATED)
                    .setContentType("application/json")
                    .setBody(jsonResponse);
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .setContentType("text/plain")
                    .setBody("500 Failed to process POST upload");
        }
    }

    private static void handlePut(HttpRequest request, HttpResponse response) {
        ensureDirectoryExists(UPLOADS_DIR);
        String reqPath = request.getPath();
        if (reqPath.equals("/") || reqPath.trim().isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST)
                    .setContentType("text/plain")
                    .setBody("400 Bad Request: Resource path required for PUT");
            return;
        }

        Path targetPath = resolvePath(UPLOADS_DIR, reqPath);
        if (targetPath == null) {
            response.setStatus(HttpStatus.BAD_REQUEST)
                    .setContentType("text/plain")
                    .setBody("400 Invalid Path");
            return;
        }

        boolean exists = Files.exists(targetPath);
        try {
            Files.write(targetPath, request.getBody(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            HttpStatus status = exists ? HttpStatus.OK : HttpStatus.CREATED;
            String msg = exists ? "Resource updated successfully" : "Resource created successfully";
            String jsonResponse = "{\"message\":\"" + msg + "\",\"path\":\"" + reqPath + "\"}";
            response.setStatus(status)
                    .setContentType("application/json")
                    .setBody(jsonResponse);
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .setContentType("text/plain")
                    .setBody("500 Failed to execute PUT operation");
        }
    }

    private static void handleDelete(HttpRequest request, HttpResponse response) {
        String reqPath = request.getPath();
        Path targetPath = resolvePath(UPLOADS_DIR, reqPath);

        if (targetPath == null || !Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            response.setStatus(HttpStatus.NOT_FOUND)
                    .setContentType("text/plain")
                    .setBody("404 File Not Found for deletion: " + reqPath);
            return;
        }

        try {
            Files.delete(targetPath);
            response.setStatus(HttpStatus.OK)
                    .setContentType("application/json")
                    .setBody("{\"message\":\"Resource deleted successfully\",\"path\":\"" + reqPath + "\"}");
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .setContentType("text/plain")
                    .setBody("500 Failed to delete resource");
        }
    }

    private static void handleNotFound(HttpResponse response) {
        Path notFoundPath = Paths.get(PUBLIC_DIR, "404.html");
        if (Files.exists(notFoundPath)) {
            try {
                byte[] content = Files.readAllBytes(notFoundPath);
                response.setStatus(HttpStatus.NOT_FOUND)
                        .setContentType("text/html")
                        .setBody(content);
                return;
            } catch (IOException ignored) {}
        }
        response.setStatus(HttpStatus.NOT_FOUND)
                .setContentType("text/html")
                .setBody("<h1>404 Not Found</h1><p>The requested resource was not found on this server.</p>");
    }

    private static Path resolvePath(String baseDir, String reqPath) {
        try {
            Path base = Paths.get(baseDir).toAbsolutePath().normalize();
            // strip leading slash
            String cleanPath = reqPath.startsWith("/") ? reqPath.substring(1) : reqPath;
            Path resolved = base.resolve(cleanPath).normalize();
            if (resolved.startsWith(base)) {
                return resolved;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
