package prj.anhzxje.aiagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prj.anhzxje.aiagent.entity.AgentTask;
import prj.anhzxje.aiagent.enums.TaskType;

import java.util.List;

@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    List<AgentTask> findByProjectId(Long projectId);
    List<AgentTask> findByUserId(Long userId);
    List<AgentTask> findByProjectIdAndType(Long projectId, TaskType type);
}
