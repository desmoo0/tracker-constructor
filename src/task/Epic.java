package task;

import manager.TaskManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Epic extends Task {
    protected List<Integer> subtaskIds;
    private LocalDateTime endTime;
    private TaskManager taskManager;

    public Epic(String name, String description) {
        super(name, description);
        this.subtaskIds = new ArrayList<>();
    }

    public Epic(String name, String description, Duration duration, LocalDateTime startTime, TaskManager taskManager) {
        super(name, description, duration, startTime);
        this.subtaskIds = new ArrayList<>();
        this.taskManager = taskManager;
    }

    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    public void addSubtaskId(Integer subtaskId) {
        subtaskIds.add(subtaskId);
    }

    public void removeSubtaskId(Integer subtaskId) {
        subtaskIds.remove(subtaskId);
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", name='" + name + " " +
                ", description='" + description + " " +
                ", status=" + status +
                ", subtaskIds=" + subtaskIds +
                '}';
    }

    @Override
    public Epic copy() {
        Epic copy = new Epic(this.name, this.description);
        copy.setId(this.id);
        copy.setStatus(this.status);
        copy.getSubtaskIds().addAll(this.subtaskIds);
        return copy;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public Duration getDuration() {
        return subtaskIds.stream()
                .map(subtaskId -> Duration.ZERO) // здесь должна быть логика получения Duration для каждой подзадачи
                .filter(Objects::nonNull)
                .collect(Collectors.reducing(Duration.ZERO, (d1, d2) -> d1.plus(d2)));
    }

    @Override
    public LocalDateTime getStartTime() {
        return subtaskIds.stream()
                .map(subtaskId -> LocalDateTime.now()) // здесь должна быть логика получения StartTime для каждой подзадачи
                .filter(Objects::nonNull)
                .collect(Collectors.minBy(LocalDateTime::compareTo))
                .orElse(null);
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
}