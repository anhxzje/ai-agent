package prj.anhzxje.aiagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prj.anhzxje.aiagent.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);
}
