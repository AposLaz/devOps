package lgl.bayern.de.ecertby.resource.integrationtests;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.util.TaskType;
import lgl.bayern.de.ecertby.resource.TaskResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.TaskTestData;
import lgl.bayern.de.ecertby.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TaskTest extends BaseTest {

    private MockMvc mockMvc;

    @Autowired
    private TaskService taskService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private TaskResource taskResource;

    private List<TaskDTO> tasks = new ArrayList<>();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    @NotNull
    private final TaskTestData taskTestData = new TaskTestData();

    private AuthorityDTO savedAuthorityDTO;
    private CompanyDTO savedCompanyDTO;
    private TemplateDTO savedTemplateDTO;
    private CertificateDTO savedCertificateDTO;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();

        if (savedAuthorityDTO==null) {
            // to avoid transient entity errors, save to repository once
            savedAuthorityDTO = authorityService.save(taskTestData.initializeAuthorityDTO("TestAuthority","test@authority.com"));
        }
        if (savedCompanyDTO==null) {
            savedCompanyDTO = companyService.save(taskTestData.initializeCompanyDTO("TestCompany","test@company.com",savedAuthorityDTO));
        }
        if (savedTemplateDTO==null) {
            savedTemplateDTO = templateService.save(taskTestData.initializeTemplateDTO("TestTemplate",0));
        }
        if (savedCertificateDTO==null) {
            savedCertificateDTO = certificateService.save(taskTestData.initializeCertificateDTO(savedAuthorityDTO,savedCompanyDTO,savedTemplateDTO));
        }
        tasks = taskTestData.populateTasks(savedAuthorityDTO, savedCompanyDTO, savedCertificateDTO);
    }

    @Test
    @DisplayName("Gets all Company tasks - Success")
    void testFindAllTasksByCompany() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TaskDTO taskDTO = tasks.get(0);
        taskDTO.setCompany(companyUser.getPrimaryCompany());
        taskDTO.setType(TaskType.COMPANY);

        taskService.save(taskDTO);

        mockMvc.perform(get("/task/company")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].company.name", equalTo(companyUser.getPrimaryCompany().getName())));
    }

    @Test
    @DisplayName("Gets all Authority tasks - Success")
    void testFindAllTasksByAuthority() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TaskDTO taskDTO = tasks.get(1);
        taskDTO.setAuthority(authorityUser.getPrimaryAuthority());
        taskDTO.setType(TaskType.AUTHORITY);

        taskService.save(taskDTO);

        mockMvc.perform(get("/task/authority")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].authority.name", equalTo(authorityUser.getPrimaryAuthority().getName())));
    }

    @Test
    @DisplayName("Completes Task - Success")
    void testReleaseCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TaskDTO taskDTO = tasks.get(2);
        taskDTO.setCompany(companyUser.getPrimaryCompany());
        taskDTO.setType(TaskType.COMPANY);
        TaskDTO savedTaskDTO = taskService.save(taskDTO);

        mockMvc.perform(patch("/task/{taskId}/complete", savedTaskDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(taskService.findById(savedTaskDTO.getId()).isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Completes All Tasks - Success")
    void testCompleteAllTasksSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TaskDTO taskDTO1 = tasks.get(1);
        taskDTO1.setCompany(companyUser.getPrimaryCompany());
        taskDTO1.setType(TaskType.COMPANY);
        TaskDTO taskDTO2 = tasks.get(2);
        taskDTO2.setCompany(companyUser.getPrimaryCompany());
        taskDTO2.setType(TaskType.COMPANY);
        TaskDTO savedTaskDTO1 = taskService.save(taskDTO1);
        TaskDTO savedTaskDTO2 = taskService.save(taskDTO2);

        mockMvc.perform(patch("/task/completeAll")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(taskService.findById(savedTaskDTO1.getId()).isCompleted()).isTrue();
        assertThat(taskService.findById(savedTaskDTO2.getId()).isCompleted()).isTrue();
    }
}
