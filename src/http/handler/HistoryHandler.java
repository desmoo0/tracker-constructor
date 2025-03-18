package http.handler;

import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import task.Task;

import java.util.List;

public class HistoryHandler extends UserHandler implements HttpHandler {

    public HistoryHandler(TaskManager manager) {
        super(manager);
    }

    @Override
    protected List<Task> getTasks() {
        return manager.getHistory();
    }
}