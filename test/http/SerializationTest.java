package http;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import task.Task;

import java.time.Duration;
import java.time.LocalDateTime;

public class SerializationTest {
    Gson gson = HttpTaskServer.getGson();

    @Test
    public void serializationTest() {
        Task task = new Task("Test 1", "Описание 1", Duration.ofMinutes(30), LocalDateTime.of(2025, 3, 17, 10, 0));
        String jsonStr = gson.toJson(task);
        System.out.println(jsonStr);
        Task task1 = gson.fromJson(jsonStr, Task.class);
        Assert.assertEquals(task, task1);
    }
}