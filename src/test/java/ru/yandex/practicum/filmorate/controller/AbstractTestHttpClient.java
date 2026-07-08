package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

abstract class AbstractTestHttpClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;
    private String url = "http://localhost";

    protected ApiResponse sendJson(String method, String path, String body) throws Exception {
        return sendJsonRequest(path, method, body);
    }


    protected ApiListResponse getJsonList(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + ":" + port + path))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        List<Map<String, Object>> responseBody = response.body().isBlank()
                ? List.of()
                : OBJECT_MAPPER.readValue(response.body(), LIST_TYPE);

        return new ApiListResponse(response.statusCode(), responseBody);
    }

    private ApiResponse sendJsonRequest(String path, String method, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + ":" + port + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> responseBody = response.body().isBlank()
                ? Map.of()
                : OBJECT_MAPPER.readValue(response.body(), MAP_TYPE);

        return new ApiResponse(response.statusCode(), responseBody);
    }

    protected record ApiResponse(int statusCode, Map<String, Object> body) {
    }

    protected record ApiListResponse(int statusCode, List<Map<String, Object>> body) {
    }
}
