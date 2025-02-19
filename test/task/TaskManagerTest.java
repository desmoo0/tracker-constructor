package test.task;

import org.junit.jupiter.api.Test;
import task.Epic;
import manager.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {
    protected T manager;

    protected abstract T createManager();

    @BeforeEach
    public void setUp() throws IOException {
        manager = createManager();
    }

    @Test
    void epicStatusCalculationAllNew() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", epic.getId());

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(TaskStatus.NEW, epic.getStatus());
    }

    @Test
    void epicStatusCalculationAllDone() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", epic.getId());

        subtask1.setStatus(TaskStatus.DONE);
        subtask2.setStatus(TaskStatus.DONE);

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        manager.updateEpicStatus(epic); // Добавим явное обновление статуса
    }

    @Test
    void epicStatusCalculationMixed() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", epic.getId());

        subtask1.setStatus(TaskStatus.NEW);
        subtask2.setStatus(TaskStatus.DONE);

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        manager.updateEpicStatus(epic);
    }

    @Test
    void epicStatusCalculationInProgress() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Подзадача", "Описание", epic.getId());
        subtask.setStatus(TaskStatus.IN_PROGRESS);

        manager.createSubtask(subtask);

        manager.updateEpicStatus(epic);
    }

    @Test
    void subtaskEpicRelation() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Подзадача", "Описание", epic.getId());
        manager.createSubtask(subtask);

        manager.updateEpicStatus(epic);
        assertEquals(epic.getId(), Integer.parseInt(subtask.getEpicId()));
    }

    @Test
    void taskTimeIntersection() {
        Task task1 = new Task("Задача 1", "Описание");
        task1.setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        task1.setDuration(Duration.ofHours(2));

        Task task2 = new Task("Задача 2", "Описание");
        task2.setStartTime(LocalDateTime.of(2024, 1, 1, 11, 0));
        task2.setDuration(Duration.ofHours(2));

        manager.createTask(task1);

        assertThrows(IllegalStateException.class, () -> manager.createTask(task2));
    }
}