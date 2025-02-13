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

    @Test
    void shouldCorrectlyLoadDataFromFile() {
        // Создаем и сохраняем начальное состояние
        Task task = new Task("Задача", "Описание задачи");
        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createTask(task);
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", epic.getId());
        manager.createSubtask(subtask);

        // Изменяем статусы
        task.setStatus(TaskStatus.IN_PROGRESS);
        manager.updateTask(task);
        subtask.setStatus(TaskStatus.DONE);
        manager.updateSubtask(subtask);

        // Создаем историю просмотров
        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(subtask.getId());

        // Загружаем данные в новый менеджер
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        // Проверяем количество задач каждого типа
        assertEquals(1, loadedManager.getAllTasks().size(), "Неверное количество задач");
        assertEquals(1, loadedManager.getAllEpics().size(), "Неверное количество эпиков");
        assertEquals(1, loadedManager.getAllSubtasks().size(), "Неверное количество подзадач");

        // Проверяем соответствие загруженных задач
        Task loadedTask = loadedManager.getTaskById(task.getId());
        Epic loadedEpic = loadedManager.getEpicById(epic.getId());
        Subtask loadedSubtask = loadedManager.getSubtaskById(subtask.getId());

        // Проверяем данные задач
        assertNotNull(loadedTask, "Задача не загружена");
        assertEquals(task.getName(), loadedTask.getName(), "Имена задач не совпадают");
        assertEquals(task.getDescription(), loadedTask.getDescription(), "Описания задач не совпадают");
        assertEquals(task.getStatus(), loadedTask.getStatus(), "Статусы задач не совпадают");

        // Проверяем данные эпика
        assertNotNull(loadedEpic, "Эпик не загружен");
        assertEquals(epic.getName(), loadedEpic.getName(), "Имена эпиков не совпадают");
        assertEquals(epic.getDescription(), loadedEpic.getDescription(), "Описания эпиков не совпадают");

        // Проверяем данные подзадачи
        assertNotNull(loadedSubtask, "Подзадача не загружена");
        assertEquals(subtask.getName(), loadedSubtask.getName(), "Имена подзадач не совпадают");
        assertEquals(subtask.getDescription(), loadedSubtask.getDescription(), "Описания подзадач не совпадают");
        assertEquals(subtask.getStatus(), loadedSubtask.getStatus(), "Статусы подзадач не совпадают");
        assertEquals(subtask.getEpicId(), loadedSubtask.getEpicId(), "ID эпиков подзадач не совпадают");

        // Проверяем историю
        List<Task> history = loadedManager.getHistory();
        assertEquals(3, history.size(), "Неверное количество записей в истории");
        assertEquals(task.getId(), history.get(0).getId(), "Неверный порядок в истории: задача");
        assertEquals(epic.getId(), history.get(1).getId(), "Неверный порядок в истории: эпик");
        assertEquals(subtask.getId(), history.get(2).getId(), "Неверный порядок в истории: подзадача");

        // Проверяем связь эпика с подзадачей
        assertTrue(loadedEpic.getSubtaskIds().contains(subtask.getId()),
                "Эпик не содержит ссылку на подзадачу");
    }
}
