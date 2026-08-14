package prj.anhzxje.aiagent.model;

import lombok.Builder;
import lombok.Data;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.dto.ReviewResponse;
import prj.anhzxje.aiagent.enums.JobStatus;

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
