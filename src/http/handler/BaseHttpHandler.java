package http.handler;

import com.sun.net.httpserver.HttpExchange;
import http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {
    private static final int NUM_PARTS_IN_PATH_WITH_ID = 3;

    int OK = Integer.parseInt(HttpStatus.OK.getCode()); //200
    int CREATED = Integer.parseInt(HttpStatus.CREATED.getCode()); //201
    int NOT_FOUND = Integer.parseInt(HttpStatus.NOT_FOUND.getCode()); //404
    int NOT_ACCEPTABLE = Integer.parseInt(HttpStatus.NOT_ACCEPTABLE.getCode()); //406
    int INTERNAL_SERVER_ERROR = Integer.parseInt(HttpStatus.INTERNAL_SERVER_ERROR.getCode()); //500

    protected String readText(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected void sendText(HttpExchange exchange, String text) throws IOException {
        byte[] response = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(OK, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    protected void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(NOT_FOUND, 0);
        exchange.close();
    }

    protected void sendHasInteractions(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(NOT_ACCEPTABLE, 0);
        exchange.close();
    }

    protected void sendCreated(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(CREATED, 0);
        exchange.close();
    }

    protected void sendServerError(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(INTERNAL_SERVER_ERROR, 0);
        exchange.close();
    }

    protected Integer getIdFromPath(String path) {
        final String[] parts = path.split("/");
        Integer id = null;
        if (parts.length >= NUM_PARTS_IN_PATH_WITH_ID) {
            try {
                id = Integer.parseInt(parts[2]);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return id;
    }
}