package prj.anhzxje.aiagent.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prj.anhzxje.aiagent.enums.JobStatus;
import prj.anhzxje.aiagent.enums.TaskType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentTaskResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private TaskType type;
    private JobStatus status;
    private String input;
    private String output;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
