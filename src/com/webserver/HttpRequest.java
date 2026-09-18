package com.webserver;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private HttpMethod method;
    private String path;
    private String version;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;
    private boolean valid = false;

    private HttpRequest() {}

    public static HttpRequest parse(InputStream inputStream) throws IOException {
        HttpRequest request = new HttpRequest();
        BufferedInputStream in = new BufferedInputStream(inputStream);

        // Read request line
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.trim().isEmpty()) {
            return request;
        }

        String[] parts = requestLine.split("\\s+");
        if (parts.length < 3) {
            return request;
        }

        request.method = HttpMethod.fromString(parts[0]);
        try {
            request.path = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            request.path = parts[1];
        }
        request.version = parts[2];

        // Read headers
        String headerLine;
        while ((headerLine = readLine(in)) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim().toLowerCase();
                String value = headerLine.substring(colonIndex + 1).trim();
                request.headers.put(name, value);
            }
        }

        // Read body if Content-Length header is present
        String contentLengthStr = request.getHeader("content-length");
        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength > 0) {
                    byte[] bodyBytes = new byte[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int read = in.read(bodyBytes, totalRead, contentLength - totalRead);
                        if (read == -1) break;
                        totalRead += read;
                    }
                    request.body = bodyBytes;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (request.body == null) {
            request.body = new byte[0];
        }

        request.valid = true;
        return request;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        boolean seenCR = false;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                seenCR = true;
            } else if (c == '\n') {
                break;
            } else {
                if (seenCR) {
                    baos.write('\r');
                    seenCR = false;
                }
                baos.write(c);
            }
        }
        if (c == -1 && baos.size() == 0) {
            return null;
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public String getHeader(String headerName) {
        return headers.get(headerName.toLowerCase());
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public boolean isValid() {
        return valid;
    }
}
