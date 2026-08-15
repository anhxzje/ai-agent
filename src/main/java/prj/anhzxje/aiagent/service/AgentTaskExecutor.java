package prj.anhzxje.aiagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.agent.CodeReviewAgent;
import prj.anhzxje.aiagent.entity.AgentTask;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.repository.AgentTaskRepository;

import java.time.LocalDateTime;

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

    @Async("agentThreadPoolExecutor")
    public void executeTaskAsync(Long taskId, String projectPath, String inputJson) {
        log.info("Bắt đầu thực thi AgentTask [id={}] trên Thread ngầm...", taskId);
        updateStatus(taskId, JobStatus.PROCESSING);

        try {
            String focus = null;
            if (inputJson != null && !inputJson.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(inputJson);
                    if (node.has("focus")) {
                        focus = node.get("focus").asText();
                    }
                    if (node.has("projectPath") && !node.get("projectPath").asText().isBlank()) {
                        projectPath = node.get("projectPath").asText();
                    }
                } catch (Exception ignored) {
                }
            }

            String result = codeReviewAgent.reviewPath(projectPath, focus);
            completeTask(taskId, result);
            log.info("AgentTask [id={}] đã hoàn thành thành công!", taskId);
        } catch (Exception e) {
            log.error("Lỗi khi thực thi AgentTask [id={}]: {}", taskId, e.getMessage(), e);
            failTask(taskId, e.getMessage());
        }
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
