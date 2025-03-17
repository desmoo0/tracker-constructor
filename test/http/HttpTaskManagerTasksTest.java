package http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import http.adapter.DurationAdapter;
import http.adapter.LocalDateTimeAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import manager.TaskManager;
import manager.InMemoryTaskManager;
import org.junit.jupiter.api.Test;
import task.Task;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpTaskManagerTasksTest {
    TaskManager manager;
    HttpTaskServer taskServer;
    Gson gson;
    HttpClient client;

    public HttpTaskManagerTasksTest() throws IOException {
        manager = new InMemoryTaskManager();
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();
    }

    @BeforeEach
    public void setUp() throws IOException {
        manager.deleteAllTasks();
        manager.deleteAllSubtasks();
        manager.deleteAllEpics();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
    }

    @AfterEach
    public void shutDown() {
        taskServer.stop();
    }

    @Test
    public void createTask() throws IOException, InterruptedException {
        Task task = new Task("Тестовая задача", "Описание тестовой задачи",
                Duration.ofMinutes(30), LocalDateTime.now());

        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        List<Task> tasks = manager.getAllTasks();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("Тестовая задача", tasks.get(0).getName());
    }

    @Test
    public void getAllTasks() throws IOException, InterruptedException {
        Task task1 = new Task("Тест 1", "Описание 1", Duration.ofMinutes(10), LocalDateTime.of(2025, 3, 17, 10, 0));
        Task task2 = new Task("Тест 2", "Описание 2", Duration.ofMinutes(20), LocalDateTime.of(2025, 3, 17, 10, 30));

        manager.createTask(task1);
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<ArrayList<Task>>(){}.getType();
        List<Task> tasksFromResponse = gson.fromJson(response.body(), listType);

        assertEquals(2, tasksFromResponse.size());
    }

    @Test
    public void getTaskById() throws IOException, InterruptedException {
        Task task = new Task("Тест получения по айди", "Описание задачи", Duration.ofMinutes(10), LocalDateTime.of(2025, 3, 17, 10, 0));
        manager.createTask(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + task.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Task taskFromResponse = gson.fromJson(response.body(), Task.class);
        assertEquals(task.getId(), taskFromResponse.getId());
        assertEquals("Тест получения по айди", taskFromResponse.getName());
    }

    @Test
    public void taskNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testAddTask() throws IOException, InterruptedException {
        Task task = new Task("Тестовая задача", "Описание тестовой задачи",
                Duration.ofMinutes(30), LocalDateTime.now());

        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        List<Task> tasks = manager.getAllTasks();
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("Тестовая задача", tasks.get(0).getName());
    }

    @Test
    public void updateTask() throws IOException, InterruptedException {
        Task task = new Task("Старое название", "Старое описание", Duration.ofMinutes(30), LocalDateTime.now());
        manager.createTask(task);

        Task updatedTask = new Task("Обновленное название", "Обновленное описание", Duration.ofMinutes(45), LocalDateTime.now().plusHours(1));
        updatedTask.setId(task.getId());

        String taskJson = gson.toJson(updatedTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        Task retrievedTask = manager.getTaskById(task.getId());
        assertEquals("Обновленное название", retrievedTask.getName());
        assertEquals("Обновленное описание", retrievedTask.getDescription());
        assertEquals(Duration.ofMinutes(45), retrievedTask.getDuration());
    }

    @Test
    public void deleteTaskById() throws IOException, InterruptedException {
        Task task = new Task("Задача для удаления", "Описание", Duration.ofMinutes(30), LocalDateTime.now());
        manager.createTask(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + task.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    public void deleteAllTasks() throws IOException, InterruptedException {
        Task task1 = new Task("Задача 1", "Описание 1", Duration.ofMinutes(30), LocalDateTime.now());
        Task task2 = new Task("Задача 2", "Описание 2", Duration.ofMinutes(30), LocalDateTime.now().plusHours(1));
        manager.createTask(task1);
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    public void testTaskHistory() throws IOException, InterruptedException {
        Task task = new Task("Задача для истории", "Описание", Duration.ofMinutes(30), LocalDateTime.now());
        manager.createTask(task);

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + task.getId()))
                .GET()
                .build();

        client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest historyRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(historyRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<ArrayList<Task>>(){}.getType();
        List<Task> history = gson.fromJson(response.body(), listType);

        assertEquals(1, history.size());
        assertEquals(task.getId(), history.get(0).getId());
    }

    @Test
    public void testPrioritizedTasks() throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        Task task1 = new Task("Задача 1", "Описание 1", Duration.ofMinutes(30), now.plusHours(2));
        Task task2 = new Task("Задача 2", "Описание 2", Duration.ofMinutes(30), now.plusHours(1));
        manager.createTask(task1);
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Type listType = new TypeToken<ArrayList<Task>>(){}.getType();
        List<Task> prioritizedTasks = gson.fromJson(response.body(), listType);

        assertEquals(2, prioritizedTasks.size());
        // Первой должна быть задача с более ранним временем
        assertEquals(task2.getId(), prioritizedTasks.get(0).getId());
    }

    @Test
    public void createTaskWithTimeConflict() throws IOException, InterruptedException {
        LocalDateTime startTime = LocalDateTime.of(2025, 3, 17, 10, 0);
        Task task1 = new Task("Задача 1", "Описание 1", Duration.ofMinutes(60), startTime);
        manager.createTask(task1);

        // Создаем задачу с пересекающимся временем
        Task task2 = new Task("Задача 2", "Описание 2", Duration.ofMinutes(30), startTime.plusMinutes(30));
        String taskJson = gson.toJson(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode()); // Ожидаем код 406 при конфликте времени
    }

    @Test
    public void updateTaskWithTimeConflict() throws IOException, InterruptedException {
        LocalDateTime startTime1 = LocalDateTime.of(2025, 3, 17, 10, 0);
        LocalDateTime startTime2 = LocalDateTime.of(2025, 3, 17, 12, 0);

        Task task1 = new Task("Задача 1", "Описание 1", Duration.ofMinutes(60), startTime1);
        Task task2 = new Task("Задача 2", "Описание 2", Duration.ofMinutes(60), startTime2);

        manager.createTask(task1);
        manager.createTask(task2);

        // Обновляем вторую задачу с пересекающимся временем
        task2.setStartTime(startTime1.plusMinutes(30));
        String taskJson = gson.toJson(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode()); // Ожидаем код 406 при конфликте времени
    }

    @Test
    public void unsupportedMethodTest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    public void createMultipleTasksAndCheckOrder() throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        Task task1 = new Task("Последняя задача", "Описание", Duration.ofMinutes(30), now.plusHours(3));
        Task task2 = new Task("Первая задача", "Описание", Duration.ofMinutes(30), now.plusHours(1));
        Task task3 = new Task("Средняя задача", "Описание", Duration.ofMinutes(30), now.plusHours(2));

        manager.createTask(task1);
        manager.createTask(task2);
        manager.createTask(task3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Type listType = new TypeToken<ArrayList<Task>>(){}.getType();
        List<Task> prioritizedTasks = gson.fromJson(response.body(), listType);

        assertEquals(3, prioritizedTasks.size());
        assertEquals(task2.getId(), prioritizedTasks.get(0).getId()); // Первая задача (самое раннее время)
        assertEquals(task3.getId(), prioritizedTasks.get(1).getId()); // Средняя задача
        assertEquals(task1.getId(), prioritizedTasks.get(2).getId()); // Последняя задача
    }

    @Test
    public void invalidJsonFormatTest() throws IOException, InterruptedException {
        String invalidJson = "{\"name\":\"Тестовая задача\", \"description:\"Обрезанный JSON}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
    }
}