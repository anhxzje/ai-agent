package prj.anhzxje.aiagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.agent.CodeReviewAgent;
import prj.anhzxje.aiagent.enums.Category;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.dto.ReviewIssue;
import prj.anhzxje.aiagent.model.ReviewJob;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.dto.ReviewResponse;
import prj.anhzxje.aiagent.enums.Severity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CodeReviewAgent codeReviewAgent;
    private final ReviewJobService jobService;
    private final ObjectMapper objectMapper;

    public void validateRequest(ReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không được để trống");
        }
        validateProjectPath(request.getProjectPath());
    }

    private void validateProjectPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("projectPath không được để trống");
        }
        Path path = Path.of(projectPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Đường dẫn không tồn tại: " + projectPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Đường dẫn không phải là thư mục: " + projectPath);
        }
    }

    @Async("agentTaskExecutor")
    public void processReviewAsync(String jobId, ReviewRequest request) {
        jobService.updateJobStatus(jobId, JobStatus.PROCESSING);
        
        try {
            String projectPath = request.getProjectPath();
            String focus = request.getFocus();
    
            log.info("Bắt đầu review project [JobId: {}]: {}", jobId, projectPath);
            log.info("Focus: {}", focus != null ? focus : "Toàn diện");
    
            String rawResult = codeReviewAgent.reviewPath(projectPath, focus);
    
            log.info("Agent trả về kết quả thô [JobId: {}]: {} ký tự", jobId, rawResult != null ? rawResult.length() : 0);
    
            ReviewResponse response = parseAgentResult(rawResult, projectPath);
            jobService.completeJob(jobId, response);
            
            log.info("Hoàn thành review project [JobId: {}]", jobId);
        } catch (Exception e) {
            log.error("Lỗi khi review project [JobId: {}]", jobId, e);
            jobService.failJob(jobId, e.getMessage());
        }
    }

    private ReviewResponse parseAgentResult(String rawResult, String projectPath) {
        if (rawResult == null || rawResult.isBlank()) {
            return buildErrorResponse(projectPath, "Agent không trả về kết quả");
        }

        String jsonContent = extractJson(rawResult);

        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            String summary = rootNode.has("summary")
                    ? rootNode.get("summary").asText()
                    : "Không có tổng quan";

            List<ReviewIssue> issues = parseIssues(rootNode);
            List<String> suggestions = parseSuggestions(rootNode);

            return ReviewResponse.builder()
                    .projectPath(projectPath)
                    .reviewedAt(LocalDateTime.now())
                    .summary(summary)
                    .totalIssues(issues.size())
                    .issues(issues)
                    .suggestions(suggestions)
                    .build();

        } catch (JsonProcessingException e) {
            log.warn("Không thể parse JSON từ Agent: {}", e.getMessage());
            return buildErrorResponse(projectPath, "Agent trả về kết quả không đúng JSON format");
        }
    }

    private List<ReviewIssue> parseIssues(JsonNode rootNode) {
        List<ReviewIssue> issues = new ArrayList<>();
        if (!rootNode.has("issues") || !rootNode.get("issues").isArray()) {
            return issues;
        }

        for (JsonNode issueNode : rootNode.get("issues")) {
            try {
                ReviewIssue issue = ReviewIssue.builder()
                        .severity(parseSeverity(issueNode))
                        .category(parseCategory(issueNode))
                        .file(getTextOrDefault(issueNode, "file", "unknown"))
                        .description(getTextOrDefault(issueNode, "description", ""))
                        .recommendation(getTextOrDefault(issueNode, "recommendation", ""))
                        .build();
                issues.add(issue);
            } catch (Exception e) {
                log.warn("Bỏ qua issue không parse được: {}", e.getMessage());
            }
        }
        return issues;
    }

    private List<String> parseSuggestions(JsonNode rootNode) {
        List<String> suggestions = new ArrayList<>();
        if (!rootNode.has("suggestions") || !rootNode.get("suggestions").isArray()) {
            return suggestions;
        }
        for (JsonNode node : rootNode.get("suggestions")) {
            suggestions.add(node.asText());
        }
        return suggestions;
    }

    private Severity parseSeverity(JsonNode issueNode) {
        String value = getTextOrDefault(issueNode, "severity", "MEDIUM");
        try {
            return Severity.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Severity không hợp lệ '{}', sử dụng MEDIUM", value);
            return Severity.MEDIUM;
        }
    }

    private Category parseCategory(JsonNode issueNode) {
        String value = getTextOrDefault(issueNode, "category", "CODE_QUALITY");
        try {
            return Category.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Category không hợp lệ '{}', sử dụng CODE_QUALITY", value);
            return Category.CODE_QUALITY;
        }
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBacktick = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return trimmed;
    }

    private ReviewResponse buildErrorResponse(String projectPath, String errorMessage) {
        return ReviewResponse.builder()
                .projectPath(projectPath)
                .reviewedAt(LocalDateTime.now())
                .summary("LỖI: " + errorMessage)
                .totalIssues(0)
                .issues(Collections.emptyList())
                .suggestions(Collections.emptyList())
                .build();
    }
}