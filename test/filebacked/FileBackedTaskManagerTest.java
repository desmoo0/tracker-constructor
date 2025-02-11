package test;

import manager.FileBackedTaskManager;
import manager.ManagerSaveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {
    private File tempFile;
    private FileBackedTaskManager manager;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        tempFile = tempDir.resolve("tasks.csv").toFile();
        manager = new FileBackedTaskManager(tempFile);
        // Создаем файл при инициализации
        if (!tempFile.exists()) {
            tempFile.createNewFile();
        }
    }

    @Test
    void shouldSaveAndLoadEmptyTaskManager() {
        // Сначала сохраняем пустой менеджер
        manager.save();

        // Затем загружаем
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getAllEpics().isEmpty());
        assertTrue(loadedManager.getAllSubtasks().isEmpty());
        assertTrue(loadedManager.getHistory().isEmpty());
    }

    @Test
    void shouldSaveAndLoadTasksWithHistory() {
        Task task = new Task("Задача", "Описание задачи");
        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createTask(task);
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", epic.getId());
        manager.createSubtask(subtask);

        // Создаем историю просмотров
        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(subtask.getId());

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        // Проверяем задачи
        assertEquals(1, loadedManager.getAllTasks().size());
        assertEquals(1, loadedManager.getAllEpics().size());
        assertEquals(1, loadedManager.getAllSubtasks().size());

        // Проверяем историю
        List<Task> history = loadedManager.getHistory();
        assertEquals(3, history.size());
    }

    @Test
    void shouldHandleInvalidFile() {
        // Создаем файл в несуществующей директории
        File invalidFile = new File("/nonexistent/directory/tasks.csv");

        // Проверяем, что попытка загрузки из несуществующего файла вызывает исключение
        // Пользуюсь лямбдой из 8 спринта для упрощения проверки
        ManagerSaveException exception = assertThrows(
                ManagerSaveException.class,
                () -> FileBackedTaskManager.loadFromFile(invalidFile)
        );

        // Проверяем сообщение об ошибке
        assertTrue(exception.getMessage().contains("Файл не найден"));
    }

    @Test
    void shouldSaveAndLoadTasksWithStatus() {
        Task task = new Task("Задача", "Описание");
        manager.createTask(task);
        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Task loadedTask = loadedManager.getTaskById(task.getId());

        assertEquals(TaskStatus.IN_PROGRESS, loadedTask.getStatus());
    }

    @Test
    void shouldSaveAndLoadEpicWithSubtasks() {
        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Epic loadedEpic = loadedManager.getEpicById(epic.getId());
        List<Subtask> loadedSubtasks = loadedManager.getEpicSubtasks(loadedEpic.getId());

        assertEquals(2, loadedSubtasks.size());
    }

    @Test
    void shouldHandleTaskDeletion() {
        Task task = new Task("Задача", "Описание");
        manager.createTask(task);
        manager.getTaskById(task.getId());
        manager.deleteTaskById(task.getId());

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getHistory().isEmpty());
    }

    @Test
    void shouldMaintainTaskIdsAfterReload() {
        Task task = new Task("Задача", "Описание");
        manager.createTask(task);
        int originalId = task.getId();

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Task loadedTask = loadedManager.getAllTasks().get(0);

        assertEquals(originalId, loadedTask.getId());
    }
}
