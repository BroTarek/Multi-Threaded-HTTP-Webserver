package com.webserver;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    UNKNOWN;

    public static HttpMethod fromString(String methodStr) {
        if (methodStr == null) {
            return UNKNOWN;
        }
        try {
            return HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
