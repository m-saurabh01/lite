package com.aircraft.emms.ui.service;

import com.aircraft.emms.ui.model.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP API client for communicating with the backend over localhost.
 */
public class ApiClient {

    private static final String BASE_URL = "http://127.0.0.1:8096/api";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final ApiClient INSTANCE = new ApiClient();

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private HttpRequest.Builder newRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT);
        String token = SessionManager.getInstance().getToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpRequest.Builder newRequest(String path, String contentType, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", contentType)
                .timeout(timeout);
        String token = SessionManager.getInstance().getToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    // --- GET ---

    public <T> T get(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest request = newRequest(path).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return objectMapper.readValue(response.body(), typeRef);
    }

    // --- POST with JSON body ---

    public <T> T post(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        String json = body != null ? objectMapper.writeValueAsString(body) : "{}";
        HttpRequest request = newRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return objectMapper.readValue(response.body(), typeRef);
    }

    // --- POST without body ---

    public <T> T post(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return post(path, null, typeRef);
    }

    // --- PUT ---

    public <T> T put(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        String json = body != null ? objectMapper.writeValueAsString(body) : "{}";
        HttpRequest request = newRequest(path)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return objectMapper.readValue(response.body(), typeRef);
    }

    // --- DELETE ---

    public <T> T delete(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest request = newRequest(path).DELETE().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return objectMapper.readValue(response.body(), typeRef);
    }

    // --- Multipart file upload ---

    public <T> T uploadFile(String path, Path filePath, TypeReference<T> typeRef) throws IOException, InterruptedException {
        String boundary = "----EmmsUpload" + System.currentTimeMillis();
        String fileName = filePath.getFileName().toString();

        byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

        String prefix = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/zip\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        byte[] prefixBytes = prefix.getBytes();
        byte[] suffixBytes = suffix.getBytes();
        byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);

        HttpRequest request = newRequest(path, "multipart/form-data; boundary=" + boundary, Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);
        return objectMapper.readValue(response.body(), typeRef);
    }

    // --- Health check ---

    public boolean isBackendReady() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkStatus(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 400) {
            // Try to parse error message from response body
            try {
                ApiResponse<?> apiResp = objectMapper.readValue(response.body(),
                        new TypeReference<ApiResponse<Object>>() {});
                if (apiResp.getMessage() != null) {
                    throw new IOException(apiResp.getMessage());
                }
            } catch (IOException parseError) {
                if (parseError.getMessage() != null && !parseError.getMessage().startsWith("{")) {
                    throw parseError;
                }
            }
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}
