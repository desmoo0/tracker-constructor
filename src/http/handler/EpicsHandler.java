package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Epic;
import task.Subtask;

import java.io.IOException;
import java.util.List;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public EpicsHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            final Integer id = getIdFromPath(path);

            // Проверяем специальный случай для получения подзадач эпика
            if (path.contains("/subtasks") && id != null) {
                if (exchange.getRequestMethod().equals("GET")) {
                    Epic epic = manager.getEpicById(id);
                    if (epic != null) {
                        List<Subtask> subtasks = manager.getEpicSubtasks(id);
                        final String response = gson.toJson(subtasks);
                        sendText(exchange, response);
                    } else {
                        sendNotFound(exchange);
                    }
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.close();
                }
                return;
            }

            switch (exchange.getRequestMethod()) {
                case "GET": {
                    if (id == null) {
                        // Получение всех эпиков
                        final List<Epic> epics = manager.getAllEpics();
                        final String response = gson.toJson(epics);
                        sendText(exchange, response);
                    } else {
                        // Получение эпика по ID
                        final Epic epic = manager.getEpicById(id);
                        if (epic != null) {
                            final String response = gson.toJson(epic);
                            sendText(exchange, response);
                        } else {
                            sendNotFound(exchange);
                        }
                    }
                    break;
                }
                case "POST": {
                    String json = readText(exchange);
                    final Epic epic = gson.fromJson(json, Epic.class);

                    if (epic.getId() != 0) {
                        // Обновление существующего эпика
                        manager.updateEpic(epic);
                    } else {
                        // Создание нового эпика
                        manager.createEpic(epic);
                    }
                    sendCreated(exchange);
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        // Удаление эпика по ID
                        manager.deleteEpicById(id);
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    } else {
                        // Удаление всех эпиков
                        manager.deleteAllEpics();
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