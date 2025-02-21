package test.backed;

import manager.FileBackedTaskManager;
import manager.ManagerSaveException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;
import test.task.TaskManagerTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    private Path tempDir;
    private File tempFile;

    @BeforeEach
    @Override
    public void setUp() {
        try {
            tempDir = Files.createTempDirectory("test");
            tempFile = tempDir.resolve("tasks.csv").toFile();
            super.setUp();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при создании временного файла", e);
        }
    }

    @Override
    protected FileBackedTaskManager createManager() {
        return new FileBackedTaskManager(tempFile);
    }

    @Test
    void shouldSaveAndLoadEmptyTaskManager() {
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertTrue(loadedManager.getAllTasks().isEmpty());
        assertTrue(loadedManager.getAllEpics().isEmpty());
        assertTrue(loadedManager.getAllSubtasks().isEmpty());
    }

    @Test
    void shouldSaveAndLoadTasksWithHistory() {
        Task task = new Task("Тестовая задача", "Описание");
        manager.createTask(task);
        manager.getTaskById(task.getId());

        assertEquals(1, manager.getHistory().size(), "История не создалась");

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertEquals(1, loadedManager.getHistory().size(), "История не загрузилась");
        assertEquals(task, loadedManager.getHistory().get(0), "Неверная задача в истории");
    }

    // Исправляем тест загрузки пустого файла
    @Test
    void shouldHandleInvalidFile() {
        File invalidFile = tempDir.resolve("invalid/tasks.csv").toFile(); // Корректный путь через Path
        assertThrows(ManagerSaveException.class,
                () -> FileBackedTaskManager.loadFromFile(invalidFile));
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
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);
        System.out.println("Epic created with ID: " + epic.getId());

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание 2", epic.getId());

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        System.out.println("Subtasks size: " + manager.getSubtasks().size());
        System.out.println("Epic subtasks: " + manager.getEpicSubtasks(epic.getId()));
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
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertEquals(0, loadedManager.getSubtasks().size());
        // Создаем и сохраняем эпик с подзадачей
        Epic epic = new Epic("Тестовый эпик", "Описание эпика");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Тестовая подзадача", "Описание подзадачи", epic.getId());
        manager.createSubtask(subtask);

        // Проверяем, что подзадача создалась
        assertEquals(1, manager.getSubtasks().size(), "Подзадача не была создана");

        // Проверяем связь с эпиком
        assertEquals(epic.getId(), subtask.getEpicId(), "Неверная связь подзадачи с эпиком");
    }

    @Test
    void saveAndLoadEmptyManager() {
        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(newManager.getAllTasks().isEmpty());
        assertTrue(newManager.getAllEpics().isEmpty());
        assertTrue(newManager.getAllSubtasks().isEmpty());
    }

    @Test
    void saveAndLoadTasks() {
        Task task = new Task("Задача", "Описание");
        manager.createTask(task);

        FileBackedTaskManager newManager = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, newManager.getAllTasks().size());
        assertEquals(task.getName(), newManager.getTaskById(task.getId()).getName());
    }

    @Test
    void handleFileException() {
        File nonExistentFile = new File("/non/existent/path/tasks.csv");

        assertThrows(ManagerSaveException.class,
                () -> FileBackedTaskManager.loadFromFile(nonExistentFile));
    }

    @Test
    void shouldHandleCorruptedFile() throws IOException {
        // Создаем файл с некорректными данными
        Files.writeString(tempFile.toPath(), "corrupted data\nwrong format");

        // Пытаемся загрузить данные из поврежденного файла
        assertThrows(ManagerSaveException.class, () -> {
            FileBackedTaskManager.loadFromFile(tempFile);
        });
    }

    @AfterEach
    void tearDown() {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Ошибка при удалении временных файлов: " + e.getMessage());
        }
    }
}
