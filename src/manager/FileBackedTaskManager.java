package manager;

import task.*;
import java.io.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.Duration;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Файл не может быть пустой.");
        }
        this.file = file;

        // Создаем файл и записываем заголовок, если файл не существует
        if (!file.exists()) {
            try {
                file.createNewFile();
                // Записываем заголовок в новый файл
                try (Writer writer = new FileWriter(file)) {
                    writer.write("id,type,name,status,description,epic\n");
                }
            } catch (IOException e) {
                throw new ManagerSaveException("Ошибка при создании файла", e);
            }
        }
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (Writer writer = new FileWriter(file)) {
                // Записываем заголовок
                writer.write("id,type,name,status,description,epic\n");

                // Сохраняем задачи
                for (Task task : getAllTasks()) {
                    writer.write(toString(task) + "\n");
                }

                // Сохраняем эпики
                for (Epic epic : getAllEpics()) {
                    writer.write(toString(epic) + "\n");
                }

                // Сохраняем подзадачи
                for (Subtask subtask : getAllSubtasks()) {
                    writer.write(toString(subtask) + "\n");
                }

                // Сохраняем историю
                writer.write("\n");
                writer.write(historyToString()); // Убрали передачу аргумента
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении в файл", e);
        }
    }

    private String toString(Task task) {
        String startTimeStr = task.getStartTime() != null ? task.getStartTime().toString() : "";
        String durationStr = task.getDuration() != null ? String.valueOf(task.getDuration().toMinutes()) : "0";

        String statusStr = task.getStatus() != null ? task.getStatus().toString() : TaskStatus.NEW.toString();

        return String.format("%d,%s,%s,%s,%s,%s,%s",
            task.getId(),
            task.getClass().getSimpleName().toUpperCase(),
            task.getName(),
            statusStr,
            task.getDescription(),
            startTimeStr,
            durationStr
        );
    }

    private String historyToString() {
        List<Task> history = getHistory();
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Task task : history) {
            sb.append(task.getId()).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Файл не может быть null");
        }

        if (!file.exists()) {
            throw new ManagerSaveException("Файл не найден: " + file.getPath(), null);
        }

        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Пропускаем заголовок
            if (line == null || !line.equals("id,type,name,status,description,epic")) {
                throw new ManagerSaveException("Некорректный формат файла", null);
            }

            while ((line = reader.readLine()) != null && !line.isBlank()) {
                Task task = fromString(line);
                if (task instanceof Epic) {
                    manager.epics.put(task.getId(), (Epic) task);
                } else if (task instanceof Subtask subtask) {
                    manager.subtasks.put(task.getId(), subtask);
                    Epic epic = manager.epics.get(subtask.getEpicId());
                    if (epic != null) {
                        epic.addSubtaskId(subtask.getId());
                    }
                } else {
                    manager.tasks.put(task.getId(), task);
                }
            }

            // Восстанавливаем историю
            String historyLine = reader.readLine();
            if (historyLine != null && !historyLine.isEmpty()) {
                for (String id : historyLine.split(",")) {
                    int taskId = Integer.parseInt(id);
                    if (manager.tasks.containsKey(taskId)) {
                        manager.getTaskById(taskId);
                    } else if (manager.epics.containsKey(taskId)) {
                        manager.getEpicById(taskId);
                    } else if (manager.subtasks.containsKey(taskId)) {
                        manager.getSubtaskById(taskId);
                    }
                }
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при чтении файла", e);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new ManagerSaveException("Некорректные данные в файле", e);
        }

        return manager;
    }

    private static Task fromString(String value) {
        String[] parts = value.split(",");
        int id = Integer.parseInt(parts[0]);
        String type = parts[1];
        String name = parts[2];
        TaskStatus status = parts[3].equals("null") ? TaskStatus.NEW : TaskStatus.valueOf(parts[3]);
        String description = parts[4];

        // Парсим время и продолжительность
        LocalDateTime startTime = parts[5].isEmpty() ? null : LocalDateTime.parse(parts[5]);
        Duration duration = parts[6].isEmpty() ? Duration.ZERO : Duration.ofMinutes(Long.parseLong(parts[6]));

        Task task;
        TaskType taskType = TaskType.valueOf(type);
        switch (taskType) {
            case TASK:
                task = new Task(name, description);
                break;
            case EPIC:
                task = new Epic(name, description);
                break;
            case SUBTASK:
                int epicId = Integer.parseInt(parts[7]);
                task = new Subtask(name, description, epicId);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }

        task.setId(id);
        task.setStatus(status);
        task.setStartTime(startTime);
        task.setDuration(duration);

        return task;
    }

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        save();
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        save();
    }

    @Override
    public void createSubtask(Subtask subtask) {
        if (epics.containsKey(subtask.getEpicId())) {
            super.createSubtask(subtask);
            save();
        }
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = super.getSubtaskById(id);
        save();
        return subtask;
    }

    @Override
    public List<Subtask> getSubtasks() {
        List<Subtask> result = super.getSubtasks();
        save();
        return result;
    }
}

