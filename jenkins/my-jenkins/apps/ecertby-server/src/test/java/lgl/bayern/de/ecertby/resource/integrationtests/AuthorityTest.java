package lgl.bayern.de.ecertby.resource.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.resource.AuthorityResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.AuthorityTestData;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CompanyService;
import lgl.bayern.de.ecertby.service.UserDetailService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.IncludeTags;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthorityTest extends BaseTest{

    private MockMvc mockMvc;
    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private AuthorityResource authorityResource;

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @Autowired
    private com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;

    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);

    @NotNull
    private final AuthorityTestData authorityTestData = new AuthorityTestData();
    private  List<AuthorityDTO> authorities = new ArrayList<>();
    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(authorityResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
       authorities = authorityTestData.populateAuthorities();
    }

    @Test
    @DisplayName("Creates Authority with Existing User - Success")
    void testCreateAuthorityWithExistingUserSuccess() throws Exception {
        loginAsAdmin();

        UserDetailDTO userDetailDTO = authorityTestData.initializeAuthorityUserDetailDTO(authorities.get(0).getEmail(),
            authorityService, resourceService, authorityMapperInstance);
        userDetailService.saveUser(userDetailDTO);
        AuthorityDTO authorityDTO = authorities.get(0);
        authorityDTO.setEmailAlreadyExists(true);
        authorityDTO.setMainUserCreate(true);
        authorityDTO.setResourceId(AppConstants.Resource.ADMIN_RESOURCE);
        ObjectMapper mapper = new ObjectMapper();
        String authorityAsJSON = mapper.writeValueAsString(authorityDTO);

        mockMvc.perform(post("/authority/create")
                        .content(authorityAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Creates Authority without Existing User - Success")
    void testCreateAuthorityWithoutExistingUserSuccess() throws Exception {
        loginAsAdmin();

        ObjectMapper mapper = new ObjectMapper();
        AuthorityDTO authorityDTO = authorities.get(11);
        authorityDTO.setResourceId(AppConstants.Resource.ADMIN_RESOURCE);
        String authorityAsJSON = mapper.writeValueAsString(authorityDTO);

        mockMvc.perform(post("/authority/create")
                        .content(authorityAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Creates Authority without Existing User (main) - Success")
    void testCreateAuthorityWithoutExistingUserMainSuccess() throws Exception {
        String resource = loginAsAdmin();

        AuthorityDTO authorityDTO = authorities.get(1);
        authorityDTO.setResourceId(resource);
        authorityDTO.setEmail("extra1@authority.com");
        authorityDTO.setUserFirstName("Test");
        authorityDTO.setUserLastName("Name");
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        OptionDTO department = authorityTestData.initializeDepartment();
        departmentList.add(department);
        authorityDTO.setUserDepartment(departmentList);
        authorityDTO.setEmailAlreadyExists(false);
        authorityDTO.setMainUserCreate(true);

        ObjectMapper mapper = new ObjectMapper();
        String authorityAsJSON = mapper.writeValueAsString(authorityDTO);

        mockMvc.perform(post("/authority/create")
                        .content(authorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Updates Authority - Success")
    void testUpdateAuthoritySuccess() throws Exception {
        String resourceName = loginAsAdmin();

        Authority savedAuthority = authorityService.saveAuthority(authorities.get(2));

        AuthorityDTO savedAuthorityDTO = authorityService.findById(savedAuthority.getId());
        savedAuthorityDTO.setName("EditedName");
        savedAuthorityDTO.setResourceId(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        String authorityAsJSON = mapper.writeValueAsString(savedAuthorityDTO);

        mockMvc.perform(post("/authority/update")
                        .content(authorityAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(authorityService.findById(savedAuthorityDTO.getId()).getName()).isEqualTo("EditedName");
    }

    @Test
    @DisplayName("Gets Authority - Success")
    void testGetAuthoritySuccess() throws Exception {
        String resourceName = loginAsAdmin();

        AuthorityDTO savedAuthorityDTO = authorityService.save(authorities.get(3));

        mockMvc.perform(get("/authority/{id}", savedAuthorityDTO.getId())
                        .param("selectionFromDD",resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(authorities.get(3).getName()));
    }

    @Test
    @DisplayName("Deactivates Authority - Success")
    void testDeactivateAuthoritySuccess() throws Exception {
        loginAsAdmin();

        UserDetailDTO userDetailDTO = authorityTestData.initializeAuthorityUserDetailDTO(authorities.get(4).getEmail(),
            authorityService, resourceService, authorityMapperInstance);
        userDetailService.saveUser(userDetailDTO);
        AuthorityDTO savedAuthorityDTO = authorityService.saveAuthorityAndLinkUser(authorities.get(4));

        mockMvc.perform(patch("/authority/{id}/activate/{isActive}", savedAuthorityDTO.getId(), false)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(authorityService.findById(savedAuthorityDTO.getId()).isActive()).isFalse();
    }

    @Test
    @DisplayName("Deactivates Authority - Failure")
    void testDeactivateAuthorityFailure() throws Exception {
        loginAsAdmin();

        AuthorityDTO savedAuthorityDTO = authorityService.save(authorities.get(4));
        companyService.save(baseTestData.initializeCompanyDTO("TestForDeactivation","extra2@authority.com", savedAuthorityDTO));

        mockMvc.perform(patch("/authority/{id}/activate/{isActive}", savedAuthorityDTO.getId(), false)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Activates Authority - Success")
    void testActivateAuthoritySuccess() throws Exception {
        loginAsAdmin();

        AuthorityDTO savedAuthorityDTO = authorityService.save(authorities.get(5));

        mockMvc.perform(patch("/authority/{id}/activate/{isActive}", savedAuthorityDTO.getId(), true)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(authorityService.findById(savedAuthorityDTO.getId()).isActive()).isTrue();
    }

    @Test
    @DisplayName("Finds all Authorities as Page AuthoritiesDTO - Success")
    void testFindAllAuthoritiesAsPageAuthorityDTO() throws Exception {
        loginAsAdmin();

        AuthorityDTO savedAuthorityDTO = authorityService.save(authorities.get(6));

        mockMvc.perform(get("/authority")
                        .param("name", savedAuthorityDTO.getName()) // custom bindings(FE name field is a DD)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].name", equalTo(authorities.get(6).getName())));
    }

    @Test
    @DisplayName("Gets Authorities of User as OptionDTOs - Success")
    void testGetUserAuthoritiesAsOptionsSuccess() throws Exception {
        String resource = loginAsAdmin();

        UserDetailDTO userDetailDTO = authorityTestData.initializeAuthorityUserDetailDTO(authorities.get(7).getEmail(),
            authorityService, resourceService, authorityMapperInstance);
        UserDetailDTO savedUserDetailDTO = userDetailService.saveUser(userDetailDTO);
        authorityService.saveAuthorityAndLinkUser(authorities.get(7));

        UserDetailDTO authorityUser = loginAsAuthority(savedUserDetailDTO.getId(), false);
        mockMvc.perform(get("/authority/findAllUserAuthorities")
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", equalTo("Existing Authority")))
                .andExpect(jsonPath("$[1].name", equalTo(authorities.get(7).getName())));
    }

    @Test
    @DisplayName("Gets Authorities of User as OptionDTOs - Failure")
    void testGetUserAuthoritiesAsOptionsFailure() throws Exception {
        String resource = loginAsAdmin();

        mockMvc.perform(get("/authority/findAllUserAuthorities")
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }


    @Test
    @DisplayName("Gets all Authorities as OptionDTOs - Success")
    void testGetAllAuthoritiesAsOptionsSuccess() throws Exception {
        String resource = loginAsAdmin();

        authorityService.save(authorities.get(8));

        mockMvc.perform(get("/authority/findAll")
                        .param("active", "false")
                        .param("hasMainUser", "false")
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Gets all active Authorities as OptionDTOs - Success")
    void testGetAllActiveAuthoritiesAsOptionsSuccess() throws Exception {
        String resource = loginAsAdmin();

        authorityService.save(authorities.get(10));

        mockMvc.perform(get("/authority/findAll")
                        .param("active", "true")
                        .param("hasMainUser", "false")
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Returns whether an Authority is linked with a main user - Success")
    void testHasMainUserSuccess() throws Exception {
        String resource = loginAsAdmin();

        AuthorityDTO savedAuthorityDTO = authorityService.save(authorities.get(9));

        mockMvc.perform(get("/authority/{authorityId}/hasMainUser", savedAuthorityDTO.getId())
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
