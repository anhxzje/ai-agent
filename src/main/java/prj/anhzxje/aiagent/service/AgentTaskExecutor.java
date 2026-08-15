package prj.anhzxje.aiagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.agent.CodeReviewAgent;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.enums.TaskType;
import prj.anhzxje.aiagent.repository.AgentTaskRepository;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service độc lập chịu trách nhiệm thực thi các AI Agent Task bất đồng bộ (True Async).
 * Sử dụng AgentTaskRepository trực tiếp để tránh phụ thuộc vòng (Circular Dependency).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskExecutor {

    private final CodeReviewAgent codeReviewAgent;
    private final AgentTaskRepository agentTaskRepository;
    private final ObjectMapper objectMapper;

    /**
     * Danh sách thư mục được phép truy cập — chống Path Traversal.
     * Cấu hình qua application.yml: agent.allowed-paths
     * Mặc định cho phép thư mục hiện tại nếu không cấu hình.
     */
    @Value("${agent.allowed-paths:#{T(java.util.Collections).emptyList()}}")
    private List<String> allowedPaths;

    @Async("agentThreadPoolExecutor")
    public void executeTaskAsync(Long taskId, String projectPath, String inputJson, TaskType taskType) {
        log.info("Bắt đầu thực thi AgentTask [id={}, type={}] trên Thread ngầm...", taskId, taskType);
        updateStatus(taskId, JobStatus.PROCESSING);

        try {
            String focus = null;
            if (inputJson != null && !inputJson.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(inputJson);
                    if (node.has("focus")) {
                        focus = node.get("focus").asText();
                    }
                    // Chỉ override projectPath nếu giá trị hợp lệ VÀ vượt qua path validation
                    if (node.has("projectPath") && !node.get("projectPath").asText().isBlank()) {
                        String overridePath = node.get("projectPath").asText();
                        if (isPathAllowed(overridePath)) {
                            projectPath = overridePath;
                        } else {
                            log.warn("AgentTask [id={}]: projectPath từ input JSON bị từ chối (không nằm trong allowed-paths): {}",
                                    taskId, overridePath);
                        }
                    }
                } catch (Exception e) {
                    log.warn("AgentTask [id={}]: Không thể parse input JSON: {}", taskId, e.getMessage());
                }
            }

            // Validate projectPath cuối cùng trước khi thực thi
            if (projectPath == null || projectPath.isBlank()) {
                throw new IllegalArgumentException("projectPath không được để trống");
            }
            if (!isPathAllowed(projectPath)) {
                throw new SecurityException(
                        "projectPath không nằm trong danh sách thư mục được phép truy cập: " + projectPath);
            }

            // Dispatch theo TaskType thay vì luôn gọi code review
            String result = dispatchTask(taskType, projectPath, focus);
            completeTask(taskId, result);
            log.info("AgentTask [id={}] đã hoàn thành thành công!", taskId);
        } catch (Exception e) {
            log.error("Lỗi khi thực thi AgentTask [id={}]: {}", taskId, e.getMessage(), e);
            failTask(taskId, e.getMessage());
        }
    }

    /**
     * Dispatch task tới agent phù hợp dựa trên TaskType.
     * Hiện tại CODE_REVIEW được hỗ trợ đầy đủ; các type khác sẽ log cảnh báo
     * và fallback sang code review (mở rộng trong tương lai).
     */
    private String dispatchTask(TaskType taskType, String projectPath, String focus) {
        return switch (taskType) {
            case CODE_REVIEW -> codeReviewAgent.reviewPath(projectPath, focus);

            case CODE_FIX -> {
                log.warn("TaskType CODE_FIX chưa có agent riêng — fallback sang CODE_REVIEW");
                yield codeReviewAgent.reviewPath(projectPath, focus);
            }
            case CODE_EXPLAIN -> {
                log.warn("TaskType CODE_EXPLAIN chưa có agent riêng — fallback sang CODE_REVIEW");
                yield codeReviewAgent.reviewPath(projectPath, focus);
            }
            case TEST_GENERATION -> {
                log.warn("TaskType TEST_GENERATION chưa có agent riêng — fallback sang CODE_REVIEW");
                yield codeReviewAgent.reviewPath(projectPath, focus);
            }
        };
    }

    /**
     * Kiểm tra xem đường dẫn có nằm trong danh sách thư mục được phép không.
     * Chống Path Traversal bằng cách normalize path và so sánh prefix.
     *
     * @param pathStr đường dẫn cần kiểm tra
     * @return true nếu đường dẫn hợp lệ và nằm trong allowed-paths
     */
    private boolean isPathAllowed(String pathStr) {
        // Nếu chưa cấu hình allowed-paths → cho phép tất cả (backward compatible, dev mode)
        if (allowedPaths == null || allowedPaths.isEmpty()) {
            log.warn("agent.allowed-paths chưa được cấu hình — cho phép tất cả đường dẫn (KHÔNG AN TOÀN cho production!)");
            return true;
        }

        try {
            Path normalizedTarget = Path.of(pathStr).toAbsolutePath().normalize();

            // Chặn path chứa ".." sau khi normalize (phòng thủ sâu)
            if (normalizedTarget.toString().contains("..")) {
                return false;
            }

            for (String allowed : allowedPaths) {
                Path normalizedAllowed = Path.of(allowed).toAbsolutePath().normalize();
                if (normalizedTarget.startsWith(normalizedAllowed)) {
                    return true;
                }
            }
        } catch (InvalidPathException e) {
            log.warn("Đường dẫn không hợp lệ: {}", pathStr);
            return false;
        }

        return false;
    }

    private void updateStatus(Long taskId, JobStatus status) {
        agentTaskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            agentTaskRepository.save(task);
        });
    }

    private void completeTask(Long taskId, String outputJson) {
        agentTaskRepository.findById(taskId).ifPresent(task -> {
            task.setOutput(outputJson);
            task.setStatus(JobStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            agentTaskRepository.save(task);
        });
    }

    private void failTask(Long taskId, String errorMessage) {
        agentTaskRepository.findById(taskId).ifPresent(task -> {
            task.setErrorMessage(errorMessage);
            task.setStatus(JobStatus.ERROR);
            task.setCompletedAt(LocalDateTime.now());
            agentTaskRepository.save(task);
        });
    }
}
