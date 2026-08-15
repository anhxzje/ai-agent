package prj.anhzxje.aiagent.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import prj.anhzxje.aiagent.enums.TaskType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentTaskRequest {

    @NotNull(message = "Project ID không được để trống")
    private Long projectId;

    @NotNull(message = "Task type không được để trống")
    private TaskType type;

    /**
     * Input dạng JSON string — cấu trúc tùy vào TaskType.
     * Ví dụ CODE_REVIEW: {"projectPath": "D:/projects/myapp", "focus": "security"}
     */
    private String input;
}
