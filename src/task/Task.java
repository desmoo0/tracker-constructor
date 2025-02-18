package task;

import java.util.Objects;
import java.time.LocalDateTime;
import java.time.Duration;

public class Task {
    protected String name;
    protected String description;
    protected int id;
    protected TaskStatus status;
    protected Duration duration;
    protected LocalDateTime startTime;

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = TaskStatus.NEW;
    }

    public Task(String name, String description, Duration duration, LocalDateTime startTime) {
        this.name = name;
        this.description = description;
        this.status = TaskStatus.NEW;
        this.duration = duration;
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    // Переопределим стандартный метод equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Переопределим метод, и для удобства - названия переменных будем писать на английском
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + " " +
                ", description='" + description + " " +
                ", status=" + status +
                '}';
    }

    public Task copy() {
        Task copy = new Task(this.name, this.description);
        copy.setId(this.id);
        copy.setStatus(this.status != null ? this.status : TaskStatus.NEW);
        copy.setStartTime(this.startTime);
        copy.setDuration(this.duration);

        return copy;
}

    public LocalDateTime getEndTime() {
        if (startTime == null || duration == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getEpicId() {
        return "";
    }
}
