package http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import manager.InMemoryTaskManager;
import manager.TaskManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    private final TaskManager manager;
    private final Gson gson;
    private final HttpClient client;
    private HttpTaskServer taskServer;

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

    @Nested
    class TaskCreationTests {
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

            assertEquals(Integer.parseInt(HttpStatus.CREATED.getCode()), response.statusCode());

            List<Task> tasks = manager.getAllTasks();
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("Тестовая задача", tasks.get(0).getName());
            System.out.println("Задача создана успешно.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.CREATED.getCode()), response.statusCode());

            List<Task> tasks = manager.getAllTasks();
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("Тестовая задача", tasks.get(0).getName());
            System.out.println("Добавлена задача успешно.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.NOT_ACCEPTABLE.getCode()), response.statusCode());
            System.out.println("Задача не создана из-за конфликта времени.");
            System.out.println("Тест пройден ✔");
        }
    }

    @Nested
    class TaskRetrievalTests {
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

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());

            Type listType = new TypeToken<ArrayList<Task>>() {
            }.getType();
            List<Task> tasksFromResponse = gson.fromJson(response.body(), listType);

            assertEquals(2, tasksFromResponse.size());
            System.out.println("Задачи успешно получены.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());

            Task taskFromResponse = gson.fromJson(response.body(), Task.class);
            assertEquals(task.getId(), taskFromResponse.getId());
            assertEquals("Тест получения по айди", taskFromResponse.getName());
            System.out.println("Задача успешно получена по айди.");
            System.out.println("Тест пройден ✔");
        }

        @Test
        public void taskNotFound() throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/999"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(Integer.parseInt(HttpStatus.NOT_FOUND.getCode()), response.statusCode());
            System.out.println("Задача не найдена.");
            System.out.println("Тест пройден ✔");
        }
    }

    @Nested
    class TaskUpdateTests {
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

            assertEquals(Integer.parseInt(HttpStatus.CREATED.getCode()), response.statusCode());

            Task retrievedTask = manager.getTaskById(task.getId());
            assertEquals("Обновленное название", retrievedTask.getName());
            assertEquals("Обновленное описание", retrievedTask.getDescription());
            assertEquals(Duration.ofMinutes(45), retrievedTask.getDuration());
            System.out.println("Задача успешно обновлена.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.NOT_ACCEPTABLE.getCode()), response.statusCode());
            System.out.println("Задача не обновлена из-за конфликта времени.");
            System.out.println("Тест пройден ✔");
        }
    }

    @Nested
    class TaskDeletionTests {
        @Test
        public void deleteTaskById() throws IOException, InterruptedException {
            Task task = new Task("Задача для удаления", "Описание", Duration.ofMinutes(30), LocalDateTime.now());
            manager.createTask(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/" + task.getId()))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());
            assertTrue(manager.getAllTasks().isEmpty());
            System.out.println("Задача успешно удалена.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());
            assertTrue(manager.getAllTasks().isEmpty());
            System.out.println("Все задачи успешно удалены.");
            System.out.println("Тест пройден ✔");
        }
    }

    @Nested
    class TaskSpecialFeaturesTests {
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

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());

            Type listType = new TypeToken<ArrayList<Task>>() {
            }.getType();
            List<Task> history = gson.fromJson(response.body(), listType);

            assertEquals(1, history.size());
            assertEquals(task.getId(), history.get(0).getId());
            System.out.println("История задач успешно получена.");
            System.out.println("Тест пройден ✔");
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

            assertEquals(Integer.parseInt(HttpStatus.OK.getCode()), response.statusCode());

            Type listType = new TypeToken<ArrayList<Task>>() {
            }.getType();
            List<Task> prioritizedTasks = gson.fromJson(response.body(), listType);

            assertEquals(2, prioritizedTasks.size());
            // Первой должна быть задача с более ранним временем
            assertEquals(task2.getId(), prioritizedTasks.get(0).getId());
            System.out.println("Задачи успешно отсортированы по времени начала.");
            System.out.println("Тест пройден ✔");
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

            Type listType = new TypeToken<ArrayList<Task>>() {
            }.getType();
            List<Task> prioritizedTasks = gson.fromJson(response.body(), listType);

            assertEquals(3, prioritizedTasks.size());
            assertEquals(task2.getId(), prioritizedTasks.get(0).getId()); // Первая задача (самое раннее время)
            assertEquals(task3.getId(), prioritizedTasks.get(1).getId()); // Средняя задача
            assertEquals(task1.getId(), prioritizedTasks.get(2).getId()); // Последняя задача
            System.out.println("Задачи успешно отсортированы по времени начала.");
            System.out.println("Тест пройден ✔");
        }
    }

    @Nested
    class ErrorHandlingTests {
        @Test
        public void unsupportedMethodTest() throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks"))
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(Integer.parseInt(HttpStatus.METHOD_NOT_ALLOWED.getCode()), response.statusCode());
            System.out.println("Сервер вернул ошибку при использовании не поддерживаемого метода.");
            System.out.println("Тест пройден ✔");
        }

        @Test
        public void invalidJsonFormatTest() throws IOException, InterruptedException {
            String invalidJson = "{\"name\":\"Тестовая задача\", \"description:\"Обрезанный JSON}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks"))
                    .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(Integer.parseInt(HttpStatus.INTERNAL_SERVER_ERROR.getCode()), response.statusCode());
            System.out.println("Сервер вернул внутреннюю ошибку при неверном формате JSON.");
            System.out.println("Тест пройден ✔");
        }
    }
}