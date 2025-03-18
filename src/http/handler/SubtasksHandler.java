package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpStatus;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Subtask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public SubtasksHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final Integer id = getIdFromPath(exchange.getRequestURI().getPath());
        try (exchange) {
            switch (exchange.getRequestMethod()) {
                case "GET": {
                    if (id == null) {
                        final List<Subtask> subtasks = manager.getAllSubtasks();
                        final String response = gson.toJson(subtasks);
                        try (var outputStream = exchange.getResponseBody()) {
                            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
                            exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), responseBytes.length);
                            outputStream.write(responseBytes);
                        }
                    } else {
                        final Subtask subtask = manager.getSubtaskById(id);
                        if (subtask != null) {
                            final String response = gson.toJson(subtask);
                            try (var outputStream = exchange.getResponseBody()) {
                                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
                                exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), responseBytes.length);
                                outputStream.write(responseBytes);
                            }
                        } else {
                            exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.NOT_FOUND.getCode()), 0);
                        }
                    }
                    break;
                }
                case "POST": {
                    String json;
                    try (var inputStream = exchange.getRequestBody()) {
                        json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    }

                    final Subtask subtask = gson.fromJson(json, Subtask.class);

                    try {
                        if (subtask.getId() != 0) {
                            manager.updateSubtask(subtask);
                        } else {
                            manager.createSubtask(subtask);
                        }
                        exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.CREATED.getCode()), 0);
                    } catch (IllegalStateException illegalException) {
                        exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.NOT_ACCEPTABLE.getCode()), 0);
                    }
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        manager.deleteSubtaskById(id);
                    } else {
                        manager.deleteAllSubtasks();
                    }
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), 0);
                    break;
                }
                default: {
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.METHOD_NOT_ALLOWED.getCode()), 0);
                }
            }
        } catch (Exception exception) {
            exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.INTERNAL_SERVER_ERROR.getCode()), 0);
        }
    }
}