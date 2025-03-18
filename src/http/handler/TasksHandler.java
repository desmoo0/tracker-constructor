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

public class TasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public TasksHandler(TaskManager manager) {
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
                        final List<Task> tasks = manager.getAllTasks();
                        final String response = gson.toJson(tasks);
                        try (var outputStream = exchange.getResponseBody()) {
                            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
                            exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), responseBytes.length);
                            outputStream.write(responseBytes);
                        }
                    } else {
                        final Task task = manager.getTaskById(id);
                        if (task != null) {
                            final String response = gson.toJson(task);
                            try (var outputStream = exchange.getResponseBody()) {
                                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
                                exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), responseBytes.length);
                                outputStream.write(responseBytes);
                            }
                        } else {
                            sendNotFound(exchange);
                        }
                    }
                    break;
                }
                case "POST": {
                    String json;
                    try (var inputStream = exchange.getRequestBody()) {
                        json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    }

                    Task task;
                    try {
                        task = gson.fromJson(json, Task.class);
                    } catch (com.google.gson.JsonSyntaxException e) {
                        sendServerError(exchange);
                        return;
                    }

                    try {
                        if (task.getId() != 0) {
                            manager.updateTask(task);
                        } else {
                            manager.createTask(task);
                        }
                        sendCreated(exchange);
                    } catch (IllegalStateException illegalException) {
                        sendHasInteractions(exchange);
                    }
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        manager.deleteTaskById(id);
                    } else {
                        manager.deleteAllTasks();
                    }
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), 0);
                    try (var outputStream = exchange.getResponseBody()) {
                        break;
                    }
                }
                default: {
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.METHOD_NOT_ALLOWED.getCode()), 0);
                }
            }
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }
}