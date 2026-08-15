package prj.anhzxje.aiagent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import prj.anhzxje.aiagent.dto.project.ProjectRequest;
import prj.anhzxje.aiagent.dto.project.ProjectResponse;
import prj.anhzxje.aiagent.entity.Project;
import prj.anhzxje.aiagent.entity.User;
import prj.anhzxje.aiagent.exception.ResourceNotFoundException;
import prj.anhzxje.aiagent.repository.ProjectRepository;
import prj.anhzxje.aiagent.security.CustomUserDetails;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * Lấy danh sách project của user hiện tại.
     */
    public List<ProjectResponse> getMyProjects() {
        User currentUser = getCurrentUser();
        return projectRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết 1 project (kiểm tra quyền sở hữu).
     */
    public ProjectResponse getProject(Long projectId) {
        Project project = findProjectAndVerifyOwner(projectId);
        return toResponse(project);
    }

    /**
     * Tạo project mới cho user hiện tại.
     */
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = getCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .path(request.getPath())
                .language(request.getLanguage())
                .user(currentUser)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Đã tạo project '{}' cho user '{}'", saved.getName(), currentUser.getUsername());
        return toResponse(saved);
    }

    /**
     * Cập nhật project (kiểm tra quyền sở hữu).
     */
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = findProjectAndVerifyOwner(projectId);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setPath(request.getPath());
        project.setLanguage(request.getLanguage());

        Project saved = projectRepository.save(project);
        log.info("Đã cập nhật project '{}'", saved.getName());
        return toResponse(saved);
    }

    /**
     * Xóa project (kiểm tra quyền sở hữu).
     */
    public void deleteProject(Long projectId) {
        Project project = findProjectAndVerifyOwner(projectId);
        projectRepository.delete(project);
        log.info("Đã xóa project id={}", projectId);
    }

    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────

    private Project findProjectAndVerifyOwner(Long projectId) {
        User currentUser = getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project với id: " + projectId));

        if (!project.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy project với id: " + projectId);
        }
        return project;
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .path(project.getPath())
                .language(project.getLanguage())
                .ownerUsername(project.getUser().getUsername())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
