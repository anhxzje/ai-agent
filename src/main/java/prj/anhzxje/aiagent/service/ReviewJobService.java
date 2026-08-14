package prj.anhzxje.aiagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.model.ReviewJob;
import prj.anhzxje.aiagent.dto.ReviewRequest;
import prj.anhzxje.aiagent.dto.ReviewResponse;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ReviewJobService {

    private final ConcurrentHashMap<String, ReviewJob> jobMap = new ConcurrentHashMap<>();

    public ReviewJob createJob(ReviewRequest request) {
        String jobId = UUID.randomUUID().toString();
        ReviewJob job = ReviewJob.builder()
                .jobId(jobId)
                .request(request)
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        jobMap.put(jobId, job);
        return job;
    }

    public ReviewJob getJob(String jobId) {
        return jobMap.get(jobId);
    }

    public void updateJobStatus(String jobId, JobStatus status) {
        ReviewJob job = jobMap.get(jobId);
        if (job != null) {
            job.setStatus(status);
            if (status == JobStatus.COMPLETED || status == JobStatus.ERROR) {
                job.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    public void completeJob(String jobId, ReviewResponse response) {
        ReviewJob job = jobMap.get(jobId);
        if (job != null) {
            job.setResult(response);
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    public void failJob(String jobId, String errorMessage) {
        ReviewJob job = jobMap.get(jobId);
        if (job != null) {
            job.setErrorMessage(errorMessage);
            job.setStatus(JobStatus.ERROR);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * Chạy định kỳ mỗi 1 giờ (3,600,000 ms)
     * Dọn dẹp các Job đã hoàn thành/lỗi được hơn 24 giờ, 
     * hoặc các Job bị treo (PENDING/PROCESSING) hơn 24 giờ.
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int initialSize = jobMap.size();

        jobMap.entrySet().removeIf(entry -> {
            ReviewJob job = entry.getValue();
            // Nếu đã xong (có completedAt), xóa nếu thời gian xong trước ngưỡng 24h
            if (job.getCompletedAt() != null) {
                return job.getCompletedAt().isBefore(threshold);
            }
            // Nếu bị treo (chưa có completedAt), xóa nếu thời gian tạo trước ngưỡng 24h
            return job.getCreatedAt().isBefore(threshold);
        });

        int removedCount = initialSize - jobMap.size();
        if (removedCount > 0) {
            log.info("Đã dọn dẹp {} job cũ khỏi bộ nhớ. Số lượng job hiện tại: {}", removedCount, jobMap.size());
        }
    }
}
