package http.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http.HttpTaskServer;
import manager.TaskManager;
import task.Task;

import java.io.IOException;
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
                sendText(exchange, response);
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    protected abstract List<Task> getTasks();
}