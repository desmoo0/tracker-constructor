package test;

import history.InMemoryHistoryManager;
import task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class InMemoryHistoryManagerTest {
    private InMemoryHistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    private Task createTask(int id) {
        Task task = new Task("Задача " + id, "Описание");
        task.setId(id);
        return task;
    }

    @Test
    void addOrRemoveDuplicates() {
        Task task = new Task("Дубликат", "Тест");
        task.setId(1);

        historyManager.add(task);
        historyManager.add(task); // Повторное добавление

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "Дубликаты не допускаются!");
        assertEquals(task, history.get(0));
    }

    @Test
    void removeFromCenter() {
        Task task1 = createTask(1);
        Task task2 = createTask(2);
        Task task3 = createTask(3);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(2);
        List<Task> history = historyManager.getHistory();

        assertEquals(List.of(task1, task3), history, "Удаление из середины");
    }

    @Test
    void ifAfterAddAndRemove() {
        Task task1 = createTask(1);
        Task task2 = createTask(2);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.remove(1);
        historyManager.add(task1);

        assertEquals(List.of(task2, task1), historyManager.getHistory());
    }
}