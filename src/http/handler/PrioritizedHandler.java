package http.handler;

import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import task.Task;

import java.util.List;

public class PrioritizedHandler extends UserHandler implements HttpHandler {

    public PrioritizedHandler(TaskManager manager) {
        super(manager);
    }

    @Override
    protected List<Task> getTasks() {
        return manager.getPrioritizedTasks();
    }
}