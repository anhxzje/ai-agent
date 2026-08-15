package prj.anhzxje.aiagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prj.anhzxje.aiagent.dto.project.ProjectRequest;
import prj.anhzxje.aiagent.dto.project.ProjectResponse;
import prj.anhzxje.aiagent.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
