package com.taskmenadzer.backend.controller;

import com.taskmenadzer.backend.dto.TaskDTO;
import com.taskmenadzer.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(@RequestHeader("X-User-ID") UUID userId) {
        return ResponseEntity.ok(taskService.getAllTasks(userId));
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO taskDTO, 
                                              @RequestHeader("X-Firebase-UID") String firebaseUid) {
        return ResponseEntity.ok(taskService.createTask(taskDTO, firebaseUid));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable UUID id, @RequestBody TaskDTO taskDTO) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }
}
