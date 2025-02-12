package test;

import manager.Managers;
import manager.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = Managers.getDefault();
    }

    @Test
    void createTaskTest() {
        Task task = new Task("Задача 1", "Мы всей семьёй переедем в другой город!");
        taskManager.createTask(task);

        Task savedTask = taskManager.getTaskById(task.getId());

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");

        List<Task> tasks = taskManager.getAllTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertEquals(1, tasks.size(), "Неверное количество задач.");
        assertEquals(task, tasks.get(0), "Задачи не совпадают.");
    }

    @Test
    void historyManagerTest() {
        Task task = new Task("Тестовое задание", "Погладить собаку");
        taskManager.createTask(task);

        // Добавляем в историю через просмотр
        taskManager.getTaskById(task.getId());

        List<Task> history = taskManager.getHistory();
        assertNotNull(history, "История не должна быть пустой (null)");
        assertEquals(1, history.size(), "История должна содержать одну задачу");
        assertEquals(task, history.get(0), "Задача в истории не совпадает с добавленной");
    }

    @Test
    void updateTaskTest() {
        Task task = new Task("Задача 3", "Выбрать новый дом на сайте объявлений");
        taskManager.createTask(task);

        task.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateTask(task);

        Task updatedTask = taskManager.getTaskById(task.getId());
        assertNotNull(updatedTask, "Обновленная задача не найдена");
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus(), "Статус задачи не обновился");
    }

    @Test
    void deleteTaskTest() {
        Task task = new Task("Задача 4", "Подготовиться к переезду");
        taskManager.createTask(task);

        taskManager.deleteTaskById(task.getId());

        assertNull(taskManager.getTaskById(task.getId()), "Задача не была удалена");
        assertTrue(taskManager.getAllTasks().isEmpty(), "Список задач должен быть пустым");
    }


    @Test
    void shouldAddAndFindDifferentTaskTypes() {
        Task task = new Task("Задача", "Тест описание");
        Epic epic = new Epic("Подзадача", "Тест описание");

        taskManager.createTask(task);
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Тест описание", epic.getId());
        taskManager.createSubtask(subtask);

        assertEquals(task, taskManager.getTaskById(task.getId()));
        assertEquals(epic, taskManager.getEpicById(epic.getId()));
        assertEquals(subtask, taskManager.getSubtaskById(subtask.getId()));
    }

    @Test
    void shouldHandleCustomAndGeneratedIds() {
        // Создаем задачу со сгенерированным id
        Task task1 = new Task("Задача 1", "Тест описание");
        taskManager.createTask(task1);
        int generatedId = task1.getId();

        // Создаем вторую задачу
        Task task2 = new Task("Задача 2", "Тест описание");
        taskManager.createTask(task2);

        // Проверяем, что задачи получили разные id
        assertNotNull(taskManager.getTaskById(generatedId),
                "Задача с сгенерированным id должна найдена");
        assertNotNull(taskManager.getTaskById(task2.getId()),
                "Задача с вторым id найдена");
        assertNotEquals(task1.getId(), task2.getId(),
                "Задачи должны иметь разные id");
    }

    @Test
    void shouldPreserveTaskDataWhenAdding() {
        Task originalTask = new Task("Задача 1", "Тест описание");
        originalTask.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.createTask(originalTask);
        Task retrievedTask = taskManager.getTaskById(originalTask.getId());

        assertEquals(originalTask.getName(), retrievedTask.getName());
        assertEquals(originalTask.getStatus(), retrievedTask.getStatus());
    }

    @Test
    void shouldPreserveHistoryData() {
        Task task = new Task("Задача", "Очень важная задача");
        taskManager.createTask(task);

        // Получаем задачу первый раз (добавится в историю)
        Task firstView = taskManager.getTaskById(task.getId());

        // Изменяем оригинальную задачу
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateTask(task);

        // Получаем задачу из истории
        Task historyTask = taskManager.getHistory().get(0);

        // Проверяем, что история сохранила исходное состояние
        assertEquals(TaskStatus.NEW, historyTask.getStatus(), "Статус в истории должен остаться NEW");
    }

    @Test
    void epicShouldNotHaveDeletedSubtaskIds() {
        Epic epic = new Epic("Эпик", "Переезд");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Упаковать вещи", epic.getId());
        taskManager.createSubtask(subtask);

        taskManager.deleteSubtaskById(subtask.getId());
        Epic savedEpic = taskManager.getEpicById(epic.getId());

        assertFalse(savedEpic.getSubtaskIds().contains(subtask.getId()), "Эпик содержит удалённую подзадачу");
    }

    @Test
    void subtaskShouldNotReferenceDeletedEpic() {
        Epic epic = new Epic("Эпик", "Переезд");
        taskManager.createEpic(epic);
        Subtask subtask = new Subtask("Подзадача", "Упаковать вещи", epic.getId());
        taskManager.createSubtask(subtask);

        taskManager.deleteEpicById(epic.getId());
        Subtask savedSubtask = taskManager.getSubtaskById(subtask.getId());

        assertNull(savedSubtask, "Подзадача не удалилась вместе с эпиком");
    }

    @Test
    void taskUpdateShouldAffectManager() {
        Task task = new Task("Задача", "Изначальное описание");
        taskManager.createTask(task);

        task.setDescription("Новое описание");
        taskManager.updateTask(task); //

        Task savedTask = taskManager.getTaskById(task.getId());
        assertEquals("Новое описание", savedTask.getDescription());
    }

    @Test
    void changingTaskIdBreaksManager() {
        Task task = new Task("Задача", "Описание");
        taskManager.createTask(task);
        int originalId = task.getId();

        task.setId(999); // Проверяем на наличие предела

        assertNull(taskManager.getTaskById(999), "Менеджер не должен находить задачу по новому ID");
        assertNotNull(taskManager.getTaskById(originalId), "Оригинальная задача потерялась");
    }

    @Test
    void getHistoryReturnsImmutableCopy() {
        Task task = new Task("Задача", "Тест");
        taskManager.createTask(task);
        taskManager.getTaskById(task.getId());

        List<Task> history = taskManager.getHistory();
        history.clear(); // Попытка изменить копию

        assertFalse(taskManager.getHistory().isEmpty(), "Оригинальная история не должна измениться");
    }

    @Test
    void nonExistentTaskDeletion() {
        taskManager.deleteTaskById(999); // Нет такой задачи
        // Ожидается, что исключений не будет
    }
}