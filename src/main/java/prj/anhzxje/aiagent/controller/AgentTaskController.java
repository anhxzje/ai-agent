package prj.anhzxje.aiagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prj.anhzxje.aiagent.dto.task.AgentTaskRequest;
import prj.anhzxje.aiagent.dto.task.AgentTaskResponse;
import prj.anhzxje.aiagent.service.AgentTaskService;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    @PostMapping
    public ResponseEntity<AgentTaskResponse> createTask(
            @Valid @RequestBody AgentTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agentTaskService.createTask(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentTaskResponse> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(agentTaskService.getTask(id));
    }

    @GetMapping
    public ResponseEntity<List<AgentTaskResponse>> getMyTasks() {
        return ResponseEntity.ok(agentTaskService.getMyTasks());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<AgentTaskResponse>> getTasksByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(agentTaskService.getTasksByProject(projectId));
    }
}
