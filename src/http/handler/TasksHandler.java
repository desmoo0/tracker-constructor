package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Task;

import java.io.IOException;
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
        try {
            final Integer id = getIdFromPath(exchange.getRequestURI().getPath());

            switch (exchange.getRequestMethod()) {
                case "GET": {
                    if (id == null) {
                        // Получение всех задач
                        final List<Task> tasks = manager.getAllTasks();
                        final String response = gson.toJson(tasks);
                        sendText(exchange, response);
                    } else {
                        // Получение задачи по ID
                        final Task task = manager.getTaskById(id);
                        if (task != null) {
                            final String response = gson.toJson(task);
                            sendText(exchange, response);
                        } else {
                            sendNotFound(exchange);
                        }
                    }
                    break;
                }
                case "POST": {
                    String json = readText(exchange);
                    final Task task = gson.fromJson(json, Task.class);

                    try {
                        if (task.getId() != 0) {
                            // Обновление существующей задачи
                            manager.updateTask(task);
                        } else {
                            // Создание новой задачи
                            manager.createTask(task);
                        }
                        sendCreated(exchange);
                    } catch (IllegalStateException e) {
                        sendHasInteractions(exchange);
                    }
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        // Удаление задачи по ID
                        manager.deleteTaskById(id);
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    } else {
                        // Удаление всех задач
                        manager.deleteAllTasks();
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    }
                    break;
                }
                default: {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.close();
                }
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}