package prj.anhzxje.aiagent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.dto.ReviewResponse;
import prj.anhzxje.aiagent.service.ReviewService;

import java.util.Map;

/**
 * REST Controller cho API Code Review.
 *
 * Trách nhiệm:
 * - Nhận HTTP request.
 * - Chuyển request cho ReviewService.
 * - Trả kết quả về HTTP response.
 *
 * Không chứa business logic và không gọi Agent trực tiếp.
 */
@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Thực hiện code review cho một project.
     *
     * POST /api/review
     *
     * Body:
     * {
     *     "projectPath": "D:/projects/identity-service",
     *     "focus": "security"
     * }
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> reviewProject(
            @RequestBody ReviewRequest request) {

        log.info("Nhận request review project: {}", request.getProjectPath());

        ReviewResponse response = reviewService.reviewProject(request);

        log.info("Review hoàn tất. Tổng issues: {}", response.getTotalIssues());

        return ResponseEntity.ok(response);
    }

    /**
     * Xử lý request không hợp lệ.
     *
     * Đây là xử lý lỗi cơ bản cho MVP.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException e) {

        log.warn("Request không hợp lệ: {}", e.getMessage());

        return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
        );
    }
}