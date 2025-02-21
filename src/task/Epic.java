package task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Epic extends Task {
    protected List<Integer> subtaskIds;
    private LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
        this.subtaskIds = new ArrayList<>();
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
                .map(subtaskId -> Duration.ZERO)
                .filter(Objects::nonNull)
                .collect(Collectors.reducing(Duration.ZERO, (firstTime, secondTime) -> firstTime.plus(secondTime)));
    }

    @Override
    public LocalDateTime getStartTime() {
        return subtaskIds.stream()
                .map(subtaskId -> LocalDateTime.now())
                .filter(Objects::nonNull)
                .collect(Collectors.minBy(LocalDateTime::compareTo))
                .orElse(null);
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
}