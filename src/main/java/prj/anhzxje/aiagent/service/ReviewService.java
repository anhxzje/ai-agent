package prj.anhzxje.aiagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.agent.CodeReviewAgent;
import prj.anhzxje.aiagent.dto.Category;
import prj.anhzxje.aiagent.dto.ReviewIssue;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.dto.ReviewResponse;
import prj.anhzxje.aiagent.dto.Severity;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Thực hiện code review cho project.
     *
     * Flow:
     * ReviewRequest
     *      ↓
     * Validate projectPath
     *      ↓
     * CodeReviewAgent
     *      ↓
     * Raw JSON result
     *      ↓
     * Parse + mapping
     *      ↓
     * ReviewResponse
     */
    public ReviewResponse reviewProject(ReviewRequest request) {

        validateRequest(request);

        String projectPath = request.getProjectPath();
        String focus = request.getFocus();

        log.info("Bắt đầu review project: {}", projectPath);
        log.info("Focus: {}", focus != null ? focus : "Toàn diện");

        // Agent chịu trách nhiệm reasoning + tool calling.
        String rawResult = codeReviewAgent.reviewPath(
                projectPath,
                focus
        );

        log.info(
                "Agent trả về kết quả thô: {} ký tự",
                rawResult != null ? rawResult.length() : 0
        );

        // Service chịu trách nhiệm parse + mapping.
        return parseAgentResult(rawResult, projectPath);
    }

    /**
     * Validate request đầu vào.
     */
    private void validateRequest(ReviewRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request không được để trống"
            );
        }

        validateProjectPath(request.getProjectPath());
    }

    /**
     * Kiểm tra projectPath:
     * - Không null
     * - Không rỗng
     * - Tồn tại
     * - Là thư mục
     */
    private void validateProjectPath(String projectPath) {

        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException(
                    "projectPath không được để trống"
            );
        }

        Path path = Path.of(projectPath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "Đường dẫn không tồn tại: " + projectPath
            );
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    "Đường dẫn không phải là thư mục: " + projectPath
            );
        }
    }

    /**
     * Parse JSON thô từ Agent thành ReviewResponse.
     */
    private ReviewResponse parseAgentResult(
            String rawResult,
            String projectPath) {

        if (rawResult == null || rawResult.isBlank()) {
            return buildErrorResponse(
                    projectPath,
                    "Agent không trả về kết quả"
            );
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

            log.warn(
                    "Không thể parse JSON từ Agent: {}",
                    e.getMessage()
            );

            return buildErrorResponse(
                    projectPath,
                    "Agent trả về kết quả không đúng JSON format"
            );
        }
    }

    /**
     * Parse danh sách issues.
     */
    private List<ReviewIssue> parseIssues(JsonNode rootNode) {

        List<ReviewIssue> issues = new ArrayList<>();

        if (!rootNode.has("issues")
                || !rootNode.get("issues").isArray()) {
            return issues;
        }

        for (JsonNode issueNode : rootNode.get("issues")) {

            try {

                ReviewIssue issue = ReviewIssue.builder()
                        .severity(parseSeverity(issueNode))
                        .category(parseCategory(issueNode))
                        .file(getTextOrDefault(
                                issueNode,
                                "file",
                                "unknown"
                        ))
                        .description(getTextOrDefault(
                                issueNode,
                                "description",
                                ""
                        ))
                        .recommendation(getTextOrDefault(
                                issueNode,
                                "recommendation",
                                ""
                        ))
                        .build();

                issues.add(issue);

            } catch (Exception e) {

                log.warn(
                        "Bỏ qua issue không parse được: {}",
                        e.getMessage()
                );
            }
        }

        return issues;
    }

    /**
     * Parse danh sách suggestions.
     */
    private List<String> parseSuggestions(JsonNode rootNode) {

        List<String> suggestions = new ArrayList<>();

        if (!rootNode.has("suggestions")
                || !rootNode.get("suggestions").isArray()) {
            return suggestions;
        }

        for (JsonNode node : rootNode.get("suggestions")) {
            suggestions.add(node.asText());
        }

        return suggestions;
    }

    /**
     * Parse severity string thành enum.
     */
    private Severity parseSeverity(JsonNode issueNode) {

        String value = getTextOrDefault(
                issueNode,
                "severity",
                "MEDIUM"
        );

        try {
            return Severity.valueOf(
                    value.toUpperCase().trim()
            );

        } catch (IllegalArgumentException e) {

            log.warn(
                    "Severity không hợp lệ '{}', sử dụng MEDIUM",
                    value
            );

            return Severity.MEDIUM;
        }
    }

    /**
     * Parse category string thành enum.
     */
    private Category parseCategory(JsonNode issueNode) {

        String value = getTextOrDefault(
                issueNode,
                "category",
                "CODE_QUALITY"
        );

        try {
            return Category.valueOf(
                    value.toUpperCase().trim()
            );

        } catch (IllegalArgumentException e) {

            log.warn(
                    "Category không hợp lệ '{}', sử dụng CODE_QUALITY",
                    value
            );

            return Category.CODE_QUALITY;
        }
    }

    /**
     * Lấy text từ JsonNode.
     */
    private String getTextOrDefault(
            JsonNode node,
            String field,
            String defaultValue) {

        return node.has(field)
                ? node.get(field).asText()
                : defaultValue;
    }

    /**
     * Loại bỏ markdown code block nếu LLM trả:
     *
     * ```json
     * {...}
     * ```
     */
    private String extractJson(String raw) {

        String trimmed = raw.trim();

        if (trimmed.startsWith("```")) {

            int firstNewline = trimmed.indexOf('\n');
            int lastBacktick = trimmed.lastIndexOf("```");

            if (firstNewline > 0
                    && lastBacktick > firstNewline) {

                return trimmed
                        .substring(
                                firstNewline + 1,
                                lastBacktick
                        )
                        .trim();
            }
        }

        return trimmed;
    }

    /**
     * Tạo response khi Agent không trả về kết quả hợp lệ.
     */
    private ReviewResponse buildErrorResponse(
            String projectPath,
            String errorMessage) {

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