package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Subtask;

import java.io.IOException;
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
        try {
            final Integer id = getIdFromPath(exchange.getRequestURI().getPath());

            switch (exchange.getRequestMethod()) {
                case "GET": {
                    if (id == null) {
                        // Получение всех подзадач
                        final List<Subtask> subtasks = manager.getAllSubtasks();
                        final String response = gson.toJson(subtasks);
                        sendText(exchange, response);
                    } else {
                        // Получение подзадачи по ID
                        final Subtask subtask = manager.getSubtaskById(id);
                        if (subtask != null) {
                            final String response = gson.toJson(subtask);
                            sendText(exchange, response);
                        } else {
                            sendNotFound(exchange);
                        }
                    }
                    break;
                }
                case "POST": {
                    String json = readText(exchange);
                    final Subtask subtask = gson.fromJson(json, Subtask.class);

                    try {
                        if (subtask.getId() != 0) {
                            // Обновление существующей подзадачи
                            manager.updateSubtask(subtask);
                        } else {
                            // Создание новой подзадачи
                            manager.createSubtask(subtask);
                        }
                        sendCreated(exchange);
                    } catch (IllegalStateException e) {
                        sendHasInteractions(exchange);
                    }
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        // Удаление подзадачи по ID
                        manager.deleteSubtaskById(id);
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    } else {
                        // Удаление всех подзадач
                        manager.deleteAllSubtasks();
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