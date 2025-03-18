package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpStatus;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class UserHandler extends BaseHttpHandler implements HttpHandler {
    protected final TaskManager manager;
    protected final Gson gson;

    public UserHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                final List<Task> tasks = getTasks();
                final String response = gson.toJson(tasks);
                try (var outputStream = exchange.getResponseBody()) {
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), responseBytes.length);
                    outputStream.write(responseBytes);
                }
            } else {
                exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.METHOD_NOT_ALLOWED.getCode()), 0);
            }
        } catch (Exception exception) {
            exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.INTERNAL_SERVER_ERROR.getCode()), 0);
        }
    }

    protected abstract List<Task> getTasks();
}