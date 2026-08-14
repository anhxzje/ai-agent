package prj.anhzxje.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho API review.
 *
 * Chỉ nhận projectPath (đường dẫn tới project), KHÔNG nhận raw code string.
 * Điều này buộc Agent phải tự khám phá codebase bằng các tool (Glob/Grep/ReadFile)
 * trước khi kết luận — đây là điểm khác biệt cốt lõi so với việc gọi LLM trực tiếp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    /**
     * Đường dẫn tuyệt đối tới thư mục project cần review trên máy.
     * Ví dụ: "D:/projects/identity-service"
     *
     * Bắt buộc — không được null hoặc rỗng.
     */
    private String projectPath;

    /**
     * (Tùy chọn) Mục tiêu focus chính khi review.
     * Ví dụ: "security", "performance", "code_quality", "bug"
     *
     * Nếu null hoặc rỗng, Agent sẽ review toàn diện tất cả các khía cạnh.
     */
    private String focus;
}