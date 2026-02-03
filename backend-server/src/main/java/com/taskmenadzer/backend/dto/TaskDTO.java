package com.taskmenadzer.backend.dto;

import com.taskmenadzer.backend.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private boolean isCompleted;
    private Task.Priority priority;
    private Task.Category category;
    private UUID userId;
}
