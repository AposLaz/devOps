package lgl.bayern.de.ecertby.resource.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.resource.CompanyResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.CompanyTestData;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CompanyService;
import lgl.bayern.de.ecertby.service.UserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CompanyTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;
    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private CompanyResource companyResource;
    @Autowired
    private com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;

    private List<CompanyDTO> companies = new ArrayList<>();

    @NotNull
    private final CompanyTestData companyTestData = new CompanyTestData();

    CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    private AuthorityDTO savedAuthorityDTO;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(companyResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        if (savedAuthorityDTO==null) {
            // to avoid transient entity errors, save to repository once
            savedAuthorityDTO = authorityService.save(companyTestData.initializeAuthorityDTO("TestAuthority","test@authority.com"));
        }
        companies = companyTestData.populateCompanies(savedAuthorityDTO);
    }

    @Test
    @DisplayName("Creates Company without Existing User - Success")
    void testCreateCompanyWithoutExistingUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        ObjectMapper mapper = new ObjectMapper();
        CompanyDTO companyDTO = companies.get(0);
        companyDTO.setResourceId(resourceName);
        String companyAsJSON = mapper.writeValueAsString(companyDTO);

        mockMvc.perform(post("/company/create")
                        .content(companyAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Creates Company with Existing User - Success")
    void testCreateCompanyWithExistingUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();
        UserDetailDTO userDTO = companyTestData.initializeCompanyUserDetailDTO(companies.get(1),
            authorityService, companyService, resourceService, companyMapperInstance);

        userDetailService.saveUser(userDTO);

        CompanyDTO companyDTO = companies.get(1);
        companyDTO.setResourceId(resourceName);
        companyDTO.setEmailAlreadyExists(true);
        ObjectMapper mapper = new ObjectMapper();
        String companyAsJSON = mapper.writeValueAsString(companyDTO);

        mockMvc.perform(post("/company/create")
                        .content(companyAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Updates Company - Success")
    void testUpdateCompanySuccess() throws Exception {
        String resourceName = loginAsAdmin();

        CompanyDTO savedCompanyDTO = companyService.saveCompanyAndCreateUser(companies.get(2));
        savedCompanyDTO.setResourceId(resourceName);
        savedCompanyDTO.setName("EditedName");
        ObjectMapper mapper = new ObjectMapper();
        String companyAsJSON = mapper.writeValueAsString(savedCompanyDTO);

        mockMvc.perform(post("/company/update")
                        .content(companyAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(companyService.findById(savedCompanyDTO.getId()).getName()).isEqualTo("EditedName");
    }

    @Test
    @DisplayName("Gets Company - Success")
    void testGetCompanySuccess() throws Exception {
        String resourceName = loginAsAdmin();

        CompanyDTO savedCompanyDTO = companyService.saveCompanyAndCreateUser(companies.get(3));

        mockMvc.perform(get("/company/{companyId}", savedCompanyDTO.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(companies.get(3).getName()));
    }

    @Test
    @DisplayName("Deletes Company - Success")
    void testDeleteCompanySuccess() throws Exception {
        String resourceName = loginAsAdmin();
        userDetailService.saveUser(companyTestData.initializeCompanyUserDetailDTO(companies.get(4),
            authorityService, companyService, resourceService, companyMapperInstance));
        CompanyDTO savedCompanyDTO = companyService.saveCompanyAndLinkUser(companies.get(4));

        mockMvc.perform(delete("/company/{companyId}", savedCompanyDTO.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(companyService.findById(savedCompanyDTO.getId()).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Deactivates Company - Success")
    void testDeactivateCompanySuccess() throws Exception {
        String resourceName = loginAsAdmin();
        userDetailService.saveUser(companyTestData.initializeCompanyUserDetailDTO(companies.get(5),
            authorityService, companyService, resourceService, companyMapperInstance));
        CompanyDTO savedCompanyDTO = companyService.saveCompanyAndLinkUser(companies.get(5));

        mockMvc.perform(patch("/company/{companyId}/activate/{isActive}", savedCompanyDTO.getId(), false)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(companyService.findById(savedCompanyDTO.getId()).isActive()).isFalse();
    }

    @Test
    @DisplayName("Activates Company - Success")
    void testActivateCompanySuccess() throws Exception {
        String resourceName = loginAsAdmin();

        CompanyDTO company = companies.get(6);
        company.setActive(false);
        CompanyDTO savedCompanyDTO = companyService.save(company);

        mockMvc.perform(patch("/company/{companyId}/activate/{isActive}", savedCompanyDTO.getId(), true)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(companyService.findById(savedCompanyDTO.getId()).isActive()).isTrue();
    }

    @Test
    @DisplayName("Gets All Companies as OptionDTOs - Success")
    void testGetAllCompaniesAsOptionsSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/company/findAll")
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Gets All active Companies as OptionDTOs - Success")
    void testGetAllActiveCompaniesAsOptionsSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/company/findAll")
                        .param("active", "true")
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Gets Companies of User as OptionDTOs - Success")
    void testGetUserCompaniesAsOptionsSuccess() throws Exception {

        String resource = loginAsAdmin();
        UserDetailDTO userDetailDTO = companyTestData.initializeCompanyUserDetailDTO(
            companies.get(7), authorityService, companyService, resourceService, companyMapperInstance);
        UserDetailDTO userDTO = userDetailService.saveUser(userDetailDTO);
        companyService.saveCompanyAndLinkUser(companies.get(7));

        UserDetailDTO companyUser = loginAsCompany(userDTO.getId());

        mockMvc.perform(get("/company/findAllUserCompanies")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", equalTo("DefaultCompany")));
    }

    @Test
    @DisplayName("Find All Companies - Success")
    void testFindAllCompanies() throws Exception {
        String resourceName = loginAsAdmin();
        userDetailService.saveUser(companyTestData.initializeCompanyUserDetailDTO(companies.get(8),
            authorityService, companyService, resourceService, companyMapperInstance));
        companyService.saveCompanyAndLinkUser(companies.get(8));

        mockMvc.perform(get("/company")
                        .param("name", companies.get(8).getName())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].name", equalTo(companies.get(8).getName())));
    }

    @Test
    @DisplayName("Find All active Companies - Success")
    void testFindAllActiveCompanies() throws Exception {
        String resourceName = loginAsAdmin();
        userDetailService.saveUser(companyTestData.initializeCompanyUserDetailDTO(companies.get(9),
            authorityService, companyService, resourceService, companyMapperInstance));
        companyService.saveCompanyAndLinkUser(companies.get(9));

        mockMvc.perform(get("/company")
                        .param("name", companies.get(9).getName())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .param("active", "true")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].name", equalTo(companies.get(9).getName())));
    }
}
