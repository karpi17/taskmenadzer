package com.taskmenadzer.backend.service;

import com.taskmenadzer.backend.dto.TaskDTO;
import com.taskmenadzer.backend.model.Task;
import com.taskmenadzer.backend.model.User;
import com.taskmenadzer.backend.repository.TaskRepository;
import com.taskmenadzer.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public List<TaskDTO> getAllTasks(UUID userId) {
        return taskRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO, String firebaseUid) {
        // Find or create user based on Firebase UID (simplified for demo)
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> userRepository.save(User.builder()
                        .firebaseUid(firebaseUid)
                        .email("placeholder@email.com") // In real app, extract from token
                        .build()));

        Task task = Task.builder()
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .dueDate(taskDTO.getDueDate())
                .priority(taskDTO.getPriority())
                .category(taskDTO.getCategory())
                .user(user)
                .isCompleted(false)
                .build();

        Task savedTask = taskRepository.save(task);
        return mapToDTO(savedTask);
    }

    public TaskDTO updateTask(UUID taskId, TaskDTO taskDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setCompleted(taskDTO.isCompleted());
        task.setDueDate(taskDTO.getDueDate());
        
        return mapToDTO(taskRepository.save(task));
    }

    public void deleteTask(UUID taskId) {
        taskRepository.deleteById(taskId);
    }

    private TaskDTO mapToDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .isCompleted(task.isCompleted())
                .priority(task.getPriority())
                .category(task.getCategory())
                .userId(task.getUser().getId())
                .build();
    }
}
