package history;

import task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {
    private static class Node {
        Task currentTask;
        Node previousTask;
        Node nextTask;

        Node(Task task) {
            this.currentTask = task;
        }
    }

    private final Map<Integer, Node> nodeMap = new HashMap<>();
    private Node head;
    private Node tail;

    @Override
    public void add(Task task) {
        if (task == null) return;
        remove(task.getId());
        linkLast(task.copy()); // Добавляем копию задачи
    }

    private void linkLast(Task task) {
        Node newNode = new Node(task);
        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.nextTask = newNode;
            newNode.previousTask = tail;
            tail = newNode;
        }
        nodeMap.put(task.getId(), newNode);
    }

    private void removeNode(Node node) {
        if (node == null) return;

        if (node.previousTask != null) {
            node.previousTask.nextTask = node.nextTask;
        } else {
            head = node.nextTask;
        }

        if (node.nextTask != null) {
            node.nextTask.previousTask = node.previousTask;
        } else {
            tail = node.previousTask;
        }

        nodeMap.remove(node.currentTask.getId());
    }

    @Override
    public void remove(int id) {
        Node node = nodeMap.get(id);
        removeNode(node);
    }

    @Override
    public List<Task> getHistory() {
        List<Task> history = new ArrayList<>();
        Node current = head;
        while (current != null) {
            history.add(current.currentTask);
            current = current.nextTask;
        }
        return history;
    }
}