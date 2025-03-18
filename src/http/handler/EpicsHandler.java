package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpStatus;
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
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            final Integer id = getIdFromPath(path);
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
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.METHOD_NOT_ALLOWED.getCode()), 0);
                }
                return;
            }
            switch (exchange.getRequestMethod()) {
                case "GET": {
                    if (id == null) {
                        final List<Epic> epics = manager.getAllEpics();
                        final String response = gson.toJson(epics);
                        sendText(exchange, response);
                    } else {
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
                        manager.updateEpic(epic);
                    } else {
                        manager.createEpic(epic);
                    }
                    sendCreated(exchange);
                    break;
                }
                case "DELETE": {
                    if (id != null) {
                        manager.deleteEpicById(id);
                    } else {
                        manager.deleteAllEpics();
                    }
                    exchange.sendResponseHeaders(Integer.parseInt(HttpStatus.OK.getCode()), 0);
                    break;
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