package prj.anhzxje.aiagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import prj.anhzxje.aiagent.model.ReviewJob;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.service.ReviewJobService;
import prj.anhzxje.aiagent.service.ReviewService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewJobService jobService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitReviewJob(
            @Valid @RequestBody ReviewRequest request) {

        log.info("Nhận request submit review job: {}", request.getProjectPath());

        // Validate đường dẫn thực tế trước khi tạo Job
        reviewService.validateRequest(request);

        // Tạo Job
        ReviewJob job = jobService.createJob(request);

        // Chạy ngầm Async
        reviewService.processReviewAsync(job.getJobId(), request);

        // Trả về Job ID ngay lập tức (HTTP 202 Accepted)
        return ResponseEntity.accepted().body(
                Map.of(
                        "jobId", job.getJobId(),
                        "message", "Job đã được đưa vào hàng đợi xử lý."
                )
        );
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ReviewJob> getJobStatus(@PathVariable String jobId) {
        ReviewJob job = jobService.getJob(jobId);
        
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(job);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Request không hợp lệ");

        log.warn("Lỗi validation request body: {}", errorMessage);

        return ResponseEntity.badRequest().body(
                Map.of("error", errorMessage)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException e) {

        log.warn("Request không hợp lệ: {}", e.getMessage());

        return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
        );
    }
}