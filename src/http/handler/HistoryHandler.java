package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Task;

import java.io.IOException;
import java.util.List;

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public HistoryHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Получение истории просмотров
                final List<Task> history = manager.getHistory();
                final String response = gson.toJson(history);
                sendText(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}