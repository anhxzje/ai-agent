package prj.anhzxje.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prj.anhzxje.aiagent.enums.Category;
import prj.anhzxje.aiagent.enums.Severity;

/**
 * Đại diện cho một vấn đề được phát hiện trong quá trình code review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {

    /**
     * Mức độ nghiêm trọng của issue.
     */
    private Severity severity;

    /**
     * Phân loại issue.
     */
    private Category category;

    /**
     * File chứa vấn đề.
     */
    private String file;

    /**
     * Mô tả vấn đề.
     */
    private String description;

    /**
     * Đề xuất cách khắc phục.
     */
    private String recommendation;
}