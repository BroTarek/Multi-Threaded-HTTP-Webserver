package com.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private HttpStatus status = HttpStatus.OK;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse() {
        // Default headers
        setHeader("Server", "Java-Custom-WebServer/1.0");
        setHeader("Connection", "close");
        setHeader("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    public HttpResponse setStatus(HttpStatus status) {
        this.status = status;
        return this;
    }

    public HttpResponse setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpResponse setContentType(String contentType) {
        setHeader("Content-Type", contentType);
        return this;
    }

    public HttpResponse setBody(byte[] bodyBytes) {
        this.body = bodyBytes;
        setHeader("Content-Length", String.valueOf(bodyBytes.length));
        return this;
    }

    public HttpResponse setBody(String bodyText) {
        return setBody(bodyText.getBytes(StandardCharsets.UTF_8));
    }

    public void send(OutputStream out) throws IOException {
        // Ensure Content-Length is present
        if (!headers.containsKey("Content-Length")) {
            setHeader("Content-Length", String.valueOf(body.length));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(status.getStatusLine()).append("\r\n");

        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        sb.append("\r\n");

        // Write header bytes
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));

        // Write body bytes
        if (body.length > 0) {
            out.write(body);
        }
        out.flush();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public byte[] getBody() {
        return body;
    }
}
