package prj.anhzxje.aiagent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewJob {
    private String jobId;
    private JobStatus status;
    private ReviewRequest request;
    private ReviewResponse result;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
