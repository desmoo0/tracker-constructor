package http.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {
    private static final int NUM_PARTS_IN_PATH_WITH_ID = 3;

    protected String readText(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected void sendText(HttpExchange exchange, String text) throws IOException {
        byte[] response = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    protected void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, 0);
        exchange.close();
    }

    protected void sendHasInteractions(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(406, 0);
        exchange.close();
    }

    protected void sendCreated(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(201, 0);
        exchange.close();
    }

    protected void sendServerError(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(500, 0);
        exchange.close();
    }

    protected Integer getIdFromPath(String path) {
        final String[] parts = path.split("/");
        Integer id = null;
        if (parts.length >= NUM_PARTS_IN_PATH_WITH_ID) {
            try {
                id = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return id;
    }
}