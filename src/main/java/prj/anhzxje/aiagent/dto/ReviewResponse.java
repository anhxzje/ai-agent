package prj.anhzxje.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response body chứa kết quả code review từ Agent.
 *
 * Bao gồm thông tin project, metadata của lần review,
 * danh sách issues và các đề xuất cải thiện.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    /**
     * Đường dẫn project đã được review.
     */
    private String projectPath;

    /**
     * Thời điểm thực hiện review.
     */
    private LocalDateTime reviewedAt;

    /**
     * Tổng quan ngắn gọn về kết quả review.
     */
    private String summary;

    /**
     * Tổng số issues phát hiện được.
     */
    private int totalIssues;

    /**
     * Danh sách các vấn đề cụ thể được phát hiện.
     */
    private List<ReviewIssue> issues;

    /**
     * Các đề xuất cải thiện tổng thể cho project.
     */
    private List<String> suggestions;
}