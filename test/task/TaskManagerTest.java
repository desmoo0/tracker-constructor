package test.task;

import manager.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {
    protected T manager;

    protected abstract T createManager();

    @BeforeEach
    public void setUp() {
        manager = createManager();
    }

    // Тесты из InMemoryTaskManagerTest
    @Test
    void createTaskTest() {
        Task task = new Task("Задача 1", "Мы всей семьёй переедем в другой город!");
        manager.createTask(task);

        Task savedTask = manager.getTaskById(task.getId());

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");

        List<Task> tasks = manager.getAllTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertEquals(1, tasks.size(), "Неверное количество задач.");
        assertEquals(task, tasks.get(0), "Задачи не совпадают.");
    }

    @Test
    void historyManagerTest() {
        Task task = new Task("Тестовое задание", "Погладить собаку");
        manager.createTask(task);

        manager.getTaskById(task.getId());

        List<Task> history = manager.getHistory();
        assertNotNull(history, "История не должна быть пустой (null)");
        assertEquals(1, history.size(), "История должна содержать одну задачу");
        assertEquals(task, history.get(0), "Задача в истории не совпадает с добавленной");
    }

    @Test
    void updateTaskTest() {
        Task task = new Task("Задача 3", "Выбрать новый дом на сайте объявлений");
        manager.createTask(task);

        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);

        Task updatedTask = manager.getTaskById(task.getId());
        assertNotNull(updatedTask, "Обновленная задача не найдена");
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus(), "Статус задачи не обновился");
    }

    @Test
    void deleteTaskTest() {
        Task task = new Task("Задача 4", "Подготовиться к переезду");
        manager.createTask(task);

        manager.deleteTaskById(task.getId());

        assertNull(manager.getTaskById(task.getId()), "Задача не была удалена");
        assertTrue(manager.getAllTasks().isEmpty(), "Список задач должен быть пустым");
    }

    @Test
    void shouldAddAndFindDifferentTaskTypes() {
        Task task = new Task("Задача", "Тест описание");
        Epic epic = new Epic("Эпик", "Тест описание");

        manager.createTask(task);
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Тест описание", epic.getId());
        manager.createSubtask(subtask);

        assertEquals(task, manager.getTaskById(task.getId()));
        assertEquals(epic, manager.getEpicById(epic.getId()));
    }

    @Test
    void shouldHandleCustomAndGeneratedIds() {
        Task task1 = new Task("Задача 1", "Тест описание");
        manager.createTask(task1);
        int generatedId = task1.getId();

        Task task2 = new Task("Задача 2", "Тест описание");
        manager.createTask(task2);

        assertNotNull(manager.getTaskById(generatedId));
        assertNotNull(manager.getTaskById(task2.getId()));
        assertNotEquals(task1.getId(), task2.getId());
    }

    @Test
    void shouldPreserveTaskDataWhenAdding() {
        Task originalTask = new Task("Задача 1", "Тест описание");
        originalTask.setStatus(TaskStatus.IN_PROGRESS);

        manager.createTask(originalTask);
        Task retrievedTask = manager.getTaskById(originalTask.getId());

        assertEquals(originalTask.getName(), retrievedTask.getName());
        assertEquals(originalTask.getStatus(), retrievedTask.getStatus());
    }

    @Test
    void shouldPreserveHistoryData() {
        Task task = new Task("Задача", "Очень важная задача");
        manager.createTask(task);

        Task firstView = manager.getTaskById(task.getId());

        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);

        Task historyTask = manager.getHistory().get(0);

        assertEquals(TaskStatus.NEW, historyTask.getStatus());
    }

    @Test
    void epicShouldNotHaveDeletedSubtaskIds() {
        Epic epic = new Epic("Эпик", "Переезд");
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Упаковать вещи", epic.getId());
        manager.createSubtask(subtask);

        manager.deleteSubtaskById(subtask.getId());
        Epic savedEpic = manager.getEpicById(epic.getId());

        assertFalse(savedEpic.getSubtaskIds().contains(subtask.getId()));
    }

    @Test
    void subtaskShouldNotReferenceDeletedEpic() {
        Epic epic = new Epic("Эпик", "Переезд");
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Упаковать вещи", epic.getId());
        manager.createSubtask(subtask);

        manager.deleteEpicById(epic.getId());
        Subtask savedSubtask = manager.getSubtaskById(subtask.getId());

        assertNull(savedSubtask);
    }

    @Test
    void taskUpdateShouldAffectManager() {
        Task task = new Task("Задача", "Изначальное описание");
        manager.createTask(task);

        task.setDescription("Новое описание");
        manager.updateTask(task);

        Task savedTask = manager.getTaskById(task.getId());
        assertEquals("Новое описание", savedTask.getDescription());
    }

    @Test
    void changingTaskIdBreaksManager() {
        Task task = new Task("Задача", "Описание");
        manager.createTask(task);
        int originalId = task.getId();

        task.setId(999);

        assertNull(manager.getTaskById(999));
        assertNotNull(manager.getTaskById(originalId));
    }

    @Test
    void getHistoryReturnsImmutableCopy() {
        Task task = new Task("Задача", "Тест");
        manager.createTask(task);
        manager.getTaskById(task.getId());

        List<Task> history = manager.getHistory();
        history.clear();

        assertFalse(manager.getHistory().isEmpty());
    }

    @Test
    void nonExistentTaskDeletion() {
        manager.deleteTaskById(999);
    }

    @Test
    void shouldNotAllowOverlappingTasks() {
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        task1.setDuration(Duration.ofHours(2));

        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setStartTime(LocalDateTime.of(2024, 1, 1, 11, 0));
        task2.setDuration(Duration.ofHours(2));

        manager.createTask(task1);

        assertThrows(IllegalStateException.class, () -> manager.createTask(task2));
    }

    @Test
    void shouldReturnSortedTasks() {
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setStartTime(LocalDateTime.of(2024, 1, 1, 12, 0));

        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));

        manager.createTask(task1);
        manager.createTask(task2);

        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(task2, prioritized.get(0));
        assertEquals(task1, prioritized.get(1));
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
        assertEquals(epic.getId(), subtask.getEpicId());
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