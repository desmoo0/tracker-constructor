package manager;

import java.time.LocalDateTime;
import java.util.*;

import history.HistoryManager;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    private int nextId = 1;
    protected Map<Integer, Task> tasks;
    protected Map<Integer, Epic> epics;
    protected Map<Integer, Subtask> subtasks;
    private final HistoryManager historyManager;

    public InMemoryTaskManager() {
        tasks = new HashMap<>();
        epics = new HashMap<>();
        subtasks = new HashMap<>();
        historyManager = Managers.getDefaultHistory();
    }

    private final Set<Task> prioritizedTasks = new TreeSet<>((task, taskToCompare) -> {
        if (task.getStartTime() == null && taskToCompare.getStartTime() == null) return 0;
        if (task.getStartTime() == null) return 1;
        if (taskToCompare.getStartTime() == null) return -1;
        return task.getStartTime().compareTo(taskToCompare.getStartTime());
    });

    // Присвоим айди следующей задаче, увеличенной на +1
    public int generateId() {
        return nextId++;
    }

    // Получим все задачи, эпики, подзадачи
    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void createTask(Task task) {
        // Проверяем пересечения со всеми существующими задачами
        boolean hasOverlap = prioritizedTasks.stream()
                .anyMatch(existingTask -> checkTasksOverlap(task, existingTask));

        if (hasOverlap) {
            throw new IllegalStateException("Задача пересекается по времени с существующей задачей");
        }

        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    public void createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
    }

    @Override
    public void createSubtask(Subtask subtask) {
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            return;
        }

        // Проверяем пересечения
        boolean hasOverlap = prioritizedTasks.stream()
                .anyMatch(existingTask -> checkTasksOverlap(subtask, existingTask));

        if (hasOverlap) {
            throw new IllegalStateException("Подзадача пересекается по времени с существующей задачей");
        }

        int id = generateId();
        subtask.setId(id);
        subtasks.put(id, subtask);
        epic.addSubtaskId(id);
        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
        }
        updateEpicStatus(epic);
    }

    @Override
    public void updateTask(Task task) {
        if (!tasks.containsKey(task.getId())) {
            return;
        }

        // Удаляем старую версию из приоритетного списка
        prioritizedTasks.remove(tasks.get(task.getId()));

        // Проверяем пересечения с другими задачами
        boolean hasOverlap = prioritizedTasks.stream()
                .anyMatch(existingTask -> checkTasksOverlap(task, existingTask));

        if (hasOverlap) {
            // Возвращаем старую версию обратно
            if (tasks.get(task.getId()).getStartTime() != null) {
                prioritizedTasks.add(tasks.get(task.getId()));
            }
            throw new IllegalStateException("Задача пересекается по времени с существующей задачей");
        }

        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            // Сохраняем список подзадач старого эпика
            List<Integer> subtaskIds = new ArrayList<>(epics.get(epic.getId()).getSubtaskIds());

            epic.getSubtaskIds().clear();
            epic.getSubtaskIds().addAll(subtaskIds);
            epics.put(epic.getId(), epic);
            updateEpicStatus(epic);
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (!subtasks.containsKey(subtask.getId())) {
            return;
        }

        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            return;
        }

        // Удаляем старую версию из приоритетного списка
        prioritizedTasks.remove(subtasks.get(subtask.getId()));

        // Проверяем пересечения
        boolean hasOverlap = prioritizedTasks.stream()
                .anyMatch(existingTask -> checkTasksOverlap(subtask, existingTask));

        if (hasOverlap) {
            // Возвращаем старую версию обратно
            if (subtasks.get(subtask.getId()).getStartTime() != null) {
                prioritizedTasks.add(subtasks.get(subtask.getId()));
            }
            throw new IllegalStateException("Подзадача пересекается по времени с существующей задачей");
        }

        subtasks.put(subtask.getId(), subtask);
        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
        }
        updateEpicStatus(epic);
    }

    @Override
    public void deleteTaskById(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
        }
        historyManager.remove(id);
    }

    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (Integer subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId); // Удаляем подзадачи из истории
            }
            historyManager.remove(id); // Удаляем эпик из истории
        }
    }

    // Обновим статусы NEW, DONE, IN_PROGRESS
    public void updateEpicStatus(Epic epic) {
        if (epic.getSubtaskIds().isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (Integer subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask == null) continue;

            if (subtask.getStatus() != TaskStatus.NEW) {
                allNew = false;
            }
            if (subtask.getStatus() != TaskStatus.DONE) {
                allDone = false;
            }
        }

        if (allNew) {
            epic.setStatus(TaskStatus.NEW);
        } else if (allDone) {
            epic.setStatus(TaskStatus.DONE);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatus(epic);
            }
            prioritizedTasks.remove(subtask);
        }
        historyManager.remove(id);
    }

    @Override
    public void deleteAllTasks() {
        for (Integer id : tasks.keySet()) {
            historyManager.remove(id);
        }
        tasks.clear();
    }

    @Override
    public void deleteAllEpics() {
        List<Integer> epicIds = new ArrayList<>(epics.keySet());
        List<Integer> subtaskIds = new ArrayList<>(subtasks.keySet());

        for (Integer id : epicIds) {
            historyManager.remove(id);
        }
        for (Integer id : subtaskIds) {
            historyManager.remove(id);
        }

        subtasks.clear();
        epics.clear();
    }

    @Override
    public void deleteAllSubtasks() {
        List<Integer> subtaskIds = new ArrayList<>(subtasks.keySet());
        for (Integer id : subtaskIds) {
            historyManager.remove(id);
        }
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.getSubtaskIds().clear();
            updateEpicStatus(epic);
        }
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        List<Subtask> epicSubtasks = new ArrayList<>();
        Epic epic = epics.get(epicId);
        if (epic != null) {
            for (Integer subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.get(subtaskId);
                if (subtask != null) {
                    epicSubtasks.add(subtask);
                }
            }
        }
        return epicSubtasks;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    private boolean checkTasksOverlap(Task task1, Task task2) {
        if (task1.getStartTime() == null || task2.getStartTime() == null
                || task1.getEndTime() == null || task2.getEndTime() == null) {
            return false;
        }

        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime start2 = task2.getStartTime();
        LocalDateTime end2 = task2.getEndTime();

        return !end1.isBefore(start2) && !end2.isBefore(start1);
    }

    @Override
    public Task getTask(int id) {
        return tasks.get(id);
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return tasks.values().stream()
                .filter(task -> task.getStartTime() != null)
                .sorted(Comparator.comparing(Task::getStartTime))
                .collect(Collectors.toList());
    }

    public List<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks.values());
    }
}
