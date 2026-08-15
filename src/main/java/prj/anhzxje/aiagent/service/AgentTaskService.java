package prj.anhzxje.aiagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.dto.task.AgentTaskRequest;
import prj.anhzxje.aiagent.dto.task.AgentTaskResponse;
import prj.anhzxje.aiagent.entity.AgentTask;
import prj.anhzxje.aiagent.entity.Project;
import prj.anhzxje.aiagent.entity.User;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.exception.ResourceNotFoundException;
import prj.anhzxje.aiagent.repository.AgentTaskRepository;
import prj.anhzxje.aiagent.repository.ProjectRepository;
import prj.anhzxje.aiagent.security.CustomUserDetails;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AgentTaskService quản lý lifecycle của AgentTask (persistence layer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskService {

    private final AgentTaskRepository agentTaskRepository;
    private final ProjectRepository projectRepository;
    private final AgentTaskExecutor agentTaskExecutor;

    /**
     * Tạo task mới (trạng thái PENDING) và kích hoạt AI Agent chạy ngầm trên Thread riêng.
     */
    public AgentTaskResponse createTask(AgentTaskRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy project với id: " + request.getProjectId()));

        // Kiểm tra quyền sở hữu project
        if (!project.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy project với id: " + request.getProjectId());
        }

        AgentTask task = AgentTask.builder()
                .user(currentUser)
                .project(project)
                .type(request.getType())
                .status(JobStatus.PENDING)
                .input(request.getInput())
                .build();

        AgentTask saved = agentTaskRepository.save(task);
        log.info("Đã tạo AgentTask [id={}, type={}, project={}]",
                saved.getId(), saved.getType(), project.getName());

        // Kích hoạt Agent xử lý ngầm (True Async via AgentTaskExecutor Spring Bean)
        agentTaskExecutor.executeTaskAsync(saved.getId(), project.getPath(), request.getInput(), request.getType());

        return toResponse(saved);
    }

    /**
     * Lấy chi tiết task (kiểm tra quyền sở hữu).
     */
    public AgentTaskResponse getTask(Long taskId) {
        AgentTask task = findTaskAndVerifyOwner(taskId);
        return toResponse(task);
    }

    /**
     * Lấy tất cả task của user hiện tại.
     */
    public List<AgentTaskResponse> getMyTasks() {
        User currentUser = getCurrentUser();
        return agentTaskRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả task thuộc một project.
     */
    public List<AgentTaskResponse> getTasksByProject(Long projectId) {
        User currentUser = getCurrentUser();

        // Kiểm tra project tồn tại và thuộc về user
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy project với id: " + projectId));

        if (!project.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy project với id: " + projectId);
        }

        return agentTaskRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────

    private AgentTask findTaskAndVerifyOwner(Long taskId) {
        User currentUser = getCurrentUser();
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy task với id: " + taskId));

        if (!task.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy task với id: " + taskId);
        }
        return task;
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private AgentTaskResponse toResponse(AgentTask task) {
        return AgentTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .type(task.getType())
                .status(task.getStatus())
                .input(task.getInput())
                .output(task.getOutput())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
