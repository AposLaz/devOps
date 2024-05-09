package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

public class TaskTestData extends BaseTestData {
    /**
     * Populates the task list with as many companies are needed for the tests, without saving them to the repository.<br>
     * Explicitly save to the repository inside a test (when needed) to avoid double-entry errors.
     */
    public List<TaskDTO> populateTasks(AuthorityDTO savedAuthorityDTO, CompanyDTO savedCompanyDTO, CertificateDTO savedCertificateDTO) {
        List<TaskDTO> tasks = new ArrayList<>();
        int REQUIRED_TASKS_NUMBER = 3;
        for (int i = 0; i < REQUIRED_TASKS_NUMBER; i++) {
            TaskDTO taskDTO = initializeTaskDTO(savedAuthorityDTO, savedCompanyDTO, savedCertificateDTO);
            tasks.add(taskDTO);
        }
        return tasks;
    }

    /**
     * Creates a task.
     * @param savedAuthorityDTO the DTO of the persisted authority that created/is responsible for the task.
     * @param savedCompanyDTO the DTO of the persisted company that created/is responsible for the task.
     * @param savedCertificateDTO the DTO of the persisted certificate corresponding to the task.
     * @return the created task as a TaskDTO.
     */
    @NotNull
    public TaskDTO initializeTaskDTO(AuthorityDTO savedAuthorityDTO, CompanyDTO savedCompanyDTO, CertificateDTO savedCertificateDTO) {
        TaskDTO taskDTO = new TaskDTO();

        taskDTO.setAuthority(savedAuthorityDTO);
        taskDTO.setCompany(savedCompanyDTO);
        taskDTO.setCertificate(savedCertificateDTO);
        taskDTO.setCreatedOn(Instant.now());
        taskDTO.setInfo("Placeholder text");
        taskDTO.setCompleted(false);

        return taskDTO;
    }
}
