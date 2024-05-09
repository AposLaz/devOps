package lgl.bayern.de.ecertby.resource.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.util.EmailNotificationType;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.repository.UserAuthorityRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.resource.UserResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.AuthorityTestData;
import lgl.bayern.de.ecertby.resource.integrationtests.data.CompanyTestData;
import lgl.bayern.de.ecertby.resource.integrationtests.data.UserTestData;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CompanyService;
import lgl.bayern.de.ecertby.service.UserDetailService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserTest extends BaseTest {

    private MockMvc mockMvc;

    @Autowired
    private UserResource userResource;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;

    CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);

    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);

    UserDetailMapper userDetailMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final UserTestData userTestData = new UserTestData();

    private final CompanyTestData companyTestData = new CompanyTestData();

    private final AuthorityTestData authorityTestData = new AuthorityTestData();

    private List<UserDetailDTO> users;

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private UserAuthorityRepository userAuthorityRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @BeforeEach
    void initOwners(){
        mockMvc = MockMvcBuilders.standaloneSetup(userResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        users = userTestData.populateUsers();
    }

    @Test
    @DisplayName("Gets User with id - Success")
    void testGetUserSuccess() throws Exception {
        String resource = loginAsAdmin();

        mockMvc.perform(get("/user/{userId}", baseTestData.getUserDetailIdAdmin())
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(baseTestData.getUserDetailIdAdmin()))
                .andExpect(jsonPath("$.username").value("ECERT_ADMIN"))
                .andExpect(jsonPath("$.email").value("evita.kakoura@eurodyn.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Gets User my-account with id - Success")
    void testGetMyAccountSuccess() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/user/my-account/{userId}", baseTestData.getUserDetailIdAdmin())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(baseTestData.getUserDetailIdAdmin()))
                .andExpect(jsonPath("$.username").value("ECERT_ADMIN"))
                .andExpect(jsonPath("$.email").value("evita.kakoura@eurodyn.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Find All Users - Success")
    void testFindAllUsers() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/user")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "username,asc")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(2))));
    }

    @Test
    @DisplayName("Creates User - Success")
    void testCreateUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        UserDetailDTO userDetailDTO = users.get(0);
        userDetailDTO.setResourceId(resourceName);
        userDetailDTO.setRole(baseTestData.getSystemAdministratorRole());
        userDetailDTO.setUserType(baseTestData.getAdminUserType());
        ObjectMapper mapper = new ObjectMapper();
        String userAsJSON = mapper.writeValueAsString(userDetailDTO);

        mockMvc.perform(post("/user/create")
                        .content(userAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Updates User - Success")
    void testUpdateUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();
        UserDetailDTO userDetailDTO = users.get(1);
        userDetailDTO.setRole(baseTestData.getSystemAdministratorRole());
        userDetailDTO.setUserType(baseTestData.getAdminUserType());
        UserDetailDTO savedUserDetailDTO = userDetailService.saveUser(userDetailDTO);
        savedUserDetailDTO.setResourceId(resourceName);
        savedUserDetailDTO.setFirstName("EditedName");
        ObjectMapper mapper = new ObjectMapper();
        String userAsJSON = mapper.writeValueAsString(savedUserDetailDTO);

        mockMvc.perform(post("/user/update")
                        .content(userAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(userDetailService.findById(savedUserDetailDTO.getId()).getFirstName()).isEqualTo("EditedName");
    }

    @Test
    @DisplayName("Deactivates User - Success")
    @Order(2)
    void testDeactivateUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        UserDetailDTO userDetail = userDetailService.findByEmail(users.get(5).getEmail());

        mockMvc.perform(patch("/user/{userId}/activate/{isActive}", userDetail.getId(), false)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(userDetailService.findById(userDetail.getId()).isActive()).isFalse();
    }

    @Test
    @DisplayName("Activates User - Success")
    @Order(3)
    void testActivateUserSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        UserDetailDTO userDetail = userDetailService.findByEmail(users.get(5).getEmail());

        mockMvc.perform(patch("/user/{userId}/activate/{isActive}", userDetail.getId(), true)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(userDetailService.findById(userDetail.getId()).isActive()).isTrue();
    }

    @Test
    @DisplayName("Gets users by Company or Authority as OptionDTOs - Success")
    void testGetUsersByCompanyOrAuthorityAsOptionsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        mockMvc.perform(get("/user/getUsers")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Gets users by email - Success")
    void testGetUserByEmailSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(post("/user/findByEmail")
                        .content("evita.kakoura@eurodyn.com")
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ECERT_ADMIN"));
    }

    @Test
    @DisplayName("Links existing user with existing company - Success")
    @Order(1)
    void testLinkCompanyUser () throws Exception {
        String resourceName = loginAsAdmin();
        // Create two company users.
        UserDetailDTO companyUser1 = createUserCompany(resourceName, users.get(4), "CompanyTest1");
        UserDetailDTO companyUser2 = createUserCompany(resourceName, users.get(5), "CompanyTest2");

        // Update company user 1 to link with the company of user 2
        String resourceName2 = loginAsAdmin();
        companyUser1.setPrimaryCompany(companyUser2.getPrimaryCompany());
        OptionDTO role = new OptionDTO();
        role.setId(baseTestData.roleMainCompanyUserId);
        companyUser1.setRole(role);
        companyUser1.setRoleName(UserRole.COMPANY_MAIN_USER.toString());
        companyUser1.setResourceId(resourceName2);
        ObjectMapper mapper = new ObjectMapper();
        String userAsJSON = mapper.writeValueAsString(companyUser1);
        mockMvc.perform(post("/user/link")
                        .content(userAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    private UserDetailDTO createUserCompany(String resourceName, UserDetailDTO userDetailDTO, String companyName){
        AuthorityDTO authorityForCompanyDTO = authorityService.save(companyTestData.initializeAuthorityDTO("TestAuthority","test@authority.com"));
        CompanyDTO companyDTO = companyTestData.initializeCompanyDTO(companyName,userDetailDTO.getEmail(), authorityForCompanyDTO);
        companyDTO.setResourceId(resourceName);
        companyDTO.setEmailAlreadyExists(false);

        CompanyDTO savedCompanyDTO = companyService.save(companyDTO);
        resourceService.createResource(companyMapperInstance.companyDTOtoResourceDTO(savedCompanyDTO));

        userDetailDTO.setPrimaryCompany(savedCompanyDTO);
        userDetailDTO.setResourceId(baseTestData.adminResourceName);
        OptionDTO role = new OptionDTO();
        role.setId(baseTestData.roleMainCompanyUserId);
        userDetailDTO.setRole(role);
        userDetailDTO.setRoleName(UserRole.COMPANY_MAIN_USER.toString());
        return userDetailService.saveUser(userDetailDTO);
    }

    @Test
    @DisplayName("Links existing user with existing authority - Success")
    void testLinkAuthorityUser () throws Exception {
        String resourceName = loginAsAdmin();
        AuthorityDTO savedAuthorityDTO = authorityService.save(companyTestData.initializeAuthorityDTO("TestAuthority2","test2@authority.com"));
        UserDetailDTO userDTO = authorityTestData.initializeAuthorityUserDetailDTO("user1@authority.com",
            authorityService, resourceService, authorityMapperInstance);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        OptionDTO department = baseTestData.initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);
        SortedSet<OptionDTO> departmentSet = new TreeSet<>();
        departmentSet.add(department);
        userDTO.setDepartment(departmentSet);
        userDTO.setPrimaryAuthority(savedAuthorityDTO);
        resourceService.createResource(authorityMapperInstance.authorityDTOtoResourceDTO(savedAuthorityDTO));
        UserDetailDTO savedUserDTO = userDetailService.saveUser(userDTO);
        savedAuthorityDTO.setResourceId(resourceName);
        savedUserDTO.setPrimaryAuthority(savedAuthorityDTO);
        savedUserDTO.setResourceId(baseTestData.adminResourceName);
        OptionDTO role = new OptionDTO();
        role.setId(baseTestData.roleMainAuthorityUserId);
        savedUserDTO.setRole(role);
        savedUserDTO.setRoleName(UserRole.AUTHORITY_MAIN_USER.toString());
        ObjectMapper mapper = new ObjectMapper();
        String userAsJSON = mapper.writeValueAsString(savedUserDTO);

        mockMvc.perform(post("/user/link")
                        .content(userAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        OptionDTO role2 = new OptionDTO();
        role2.setId(baseTestData.roleStandardAuthorityUserId);
        savedUserDTO.setRole(role2);
        savedUserDTO.setRoleName("AUTHORITY_STANDARD_USER");
        savedUserDTO.setSelectionFromDD(savedAuthorityDTO.getId());
        assertDoesNotThrow(() -> userDetailService.saveUser(savedUserDTO));
    }


    @Test
    @DisplayName("Links existing user with existing authority - Success")
    void testSaveMyAccount () throws Exception {
        String resourceName = loginAsAdmin();

        UserDetailProfileDTO userDetailProfileDTO = (UserDetailProfileDTO) userDetailService.findById(baseTestData.userDetailIdAdmin);

        ObjectMapper mapper = new ObjectMapper();
        String userAsJSON = mapper.writeValueAsString(userDetailProfileDTO);
        mockMvc.perform(post("/user/my-account")
                        .content(userAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                       .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setCurrentPassword("ecert_admin1");
        resetPasswordDTO.setNewPassword("ecert_admin2");
        resetPasswordDTO.setResourceId(baseTestData.adminResourceName);
        String resetPasswordAsJSON = mapper.writeValueAsString(resetPasswordDTO);

        mockMvc.perform(post("/user/update-password")
                        .content(resetPasswordAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        //reset password back

        resetPasswordDTO.setCurrentPassword("ecert_admin2");
        resetPasswordDTO.setNewPassword("ecert_admin1");
        resetPasswordAsJSON = mapper.writeValueAsString(resetPasswordDTO);

        mockMvc.perform(post("/user/update-password")
                        .content(resetPasswordAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());


    }

    @Test
    @DisplayName("Saves email notification settings - Success")
    void testSaveEmailNotificationSettings() throws Exception {
        String resourceName = loginAsAdmin();

        EmailNotificationDTO emailNotificationDTO = new EmailNotificationDTO();
        emailNotificationDTO.setResourceId(resourceName);
        emailNotificationDTO.setEmailNotificationList(List.of(EmailNotificationType.FEATUREBOARD_ENTRY_ADDED));

        ObjectMapper mapper = new ObjectMapper();
        String notificationSettingsAsJSON = mapper.writeValueAsString(emailNotificationDTO);
        mockMvc.perform(post("/user/update-email-notifications")
                        .content(notificationSettingsAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gets a user's email notification settings - Success")
    void testGetEmailNotificationSettings() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        EmailNotificationDTO emailNotificationDTO = new EmailNotificationDTO();
        emailNotificationDTO.setEmailNotificationList(List.of(EmailNotificationType.CERTIFICATE_ASSIGNED));
        userDetailService.updateEmailNotificationSettings(emailNotificationDTO);

        mockMvc.perform(get("/user/my-account/email-notifications")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNotificationList", Matchers.contains(EmailNotificationType.CERTIFICATE_ASSIGNED.toString())));
    }
}