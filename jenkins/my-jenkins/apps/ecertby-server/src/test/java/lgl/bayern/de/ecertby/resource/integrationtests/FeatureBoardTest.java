package lgl.bayern.de.ecertby.resource.integrationtests;

import com.eurodyn.qlack.fuse.fd.dto.ThreadMessageDTO;
import com.eurodyn.qlack.fuse.fd.util.ThreadStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.FeatureBoard;
import lgl.bayern.de.ecertby.repository.FeatureBoardRepository;
import lgl.bayern.de.ecertby.resource.AuthorityResource;
import lgl.bayern.de.ecertby.resource.FeatureBoardResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.FeatureBoardTestData;
import lgl.bayern.de.ecertby.service.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.IncludeTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FeatureBoardTest extends BaseTest {

    private MockMvc mockMvc;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private FeatureBoardService featureBoardService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private AuthorityResource authorityResource;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private FeatureBoardResource featureBoardResource;

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @NotNull
    private FeatureBoardTestData featureBoardTestData = new FeatureBoardTestData();
    private List<ThreadMessageDTO> threads = new ArrayList<>();

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(featureBoardResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        threads = featureBoardTestData.populateFeatureBoard();
    }

    @Test
    @DisplayName("Finds all threads as Page ThreadDTO - Success")
    void testFindAllThreadsAsPageThreadDTO() throws Exception {
        String resource = loginAsAdmin();

        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(0);
        thread.setResourceId(authority.getId());
        featureBoardService.saveThread(thread);

        mockMvc.perform(get("/featureboard")
                        .param("title", "Title 1")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdOn,desc")
                        .param("selectionFromDD", resource)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].title", equalTo(threads.get(0).getTitle())));
    }

    @Test
    @DisplayName("Gets Thread - Success")
    void testGetThreadSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(1);
        thread.setResourceId(resourceName);
        FeatureBoard fb = featureBoardService.saveThread(thread);

        mockMvc.perform(get("/featureboard/{id}", fb.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(threads.get(1).getTitle()));
    }

    @Test
    @DisplayName("Saves Thread - Success")
    void testSavesThreadSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        FeatureBoardThreadDTO dto = (FeatureBoardThreadDTO) threads.get(2);
        dto.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        String threadAsJSON = mapper.writeValueAsString(threads.get(2));

        mockMvc.perform(post("/featureboard/create")
                        .content(threadAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Upvote - Success")
    void testUpvoteThreadSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        FeatureBoardThreadDTO fb =(FeatureBoardThreadDTO) threads.get(4);
        fb.setResourceId(companyUser.getPrimaryCompany().getId());
        FeatureBoard featureBoard = featureBoardService.saveThread(fb);
        ViewThreadPermissionsDTO viewThreadPermissionsDTO = new ViewThreadPermissionsDTO();
        viewThreadPermissionsDTO.setCompanyVisible(true);
        viewThreadPermissionsDTO.setAuthorityVisible(true);
        featureBoardService.publish(featureBoard.getId(), viewThreadPermissionsDTO);

        UserDetailDTO authorityUser = loginAsAuthority();
        mockMvc.perform(patch("/featureboard/{id}/upvote", featureBoard.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }


    @Test
    @DisplayName("Downvote - Success")
    void testDownvoteThreadSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        FeatureBoardThreadDTO fb =(FeatureBoardThreadDTO) threads.get(4);
        fb.setResourceId(companyUser.getPrimaryCompany().getId());
        FeatureBoard featureBoard = featureBoardService.saveThread(fb);
        ViewThreadPermissionsDTO viewThreadPermissionsDTO = new ViewThreadPermissionsDTO();
        viewThreadPermissionsDTO.setCompanyVisible(true);
        viewThreadPermissionsDTO.setAuthorityVisible(true);
        featureBoardService.publish(featureBoard.getId(), viewThreadPermissionsDTO);

        UserDetailDTO authorityUser = loginAsAuthority();
        mockMvc.perform(patch("/featureboard/{id}/downvote", featureBoard.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Publish - Success")
    void testPublishThreadSuccess() throws Exception {
        String resourceName  = loginAsAdmin();

        ObjectMapper mapper = new ObjectMapper();
        ViewThreadPermissionsDTO viewThreadPermissionsDTO = new ViewThreadPermissionsDTO();
        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(5);
        thread.setResourceId(authority.getId());
        FeatureBoard fb = featureBoardService.saveThread(thread);

        mockMvc.perform(patch("/featureboard/{id}/publish", fb.getId())
                        .param("selectionFromDD", resourceName)
                        .content(mapper.writeValueAsString(viewThreadPermissionsDTO))
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Reject - Success")
    void testRejectThreadSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(6);
        thread.setResourceId(authority.getId());
        FeatureBoard fb =featureBoardService.saveThread(thread);

        mockMvc.perform(patch("/featureboard/{id}/reject", fb.getId())
                        .param("selectionFromDD", resourceName)
                        .content("123").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Add comment - Success")
    void testAddCommentSuccess() throws Exception {
        String resourceName  = loginAsAdmin();

        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(7);
        thread.setResourceId(authority.getId());
        FeatureBoard fb = featureBoardService.saveThread(thread);

        mockMvc.perform(post("/featureboard/{id}/addComment", fb.getId())
                        .param("selectionFromDD", resourceName)
                        .content("testComment").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("testComment"));
    }

    @Test
    @DisplayName("Find comments - Success")
    void testFindCommentsSuccess() throws Exception {
        String resourceName  = loginAsAdmin();


        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(7);
        thread.setResourceId(authority.getId());
        FeatureBoard fb = featureBoardService.saveThread(thread);
        featureBoardService.saveComment(fb.getId(),"123");

        mockMvc.perform(get("/featureboard/{id}/comments", fb.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].body").value("123"));
    }


    @Test
    @DisplayName("Update comment - Success")
    void testUpdateCommentSuccess() throws Exception {
        String resourceName  = loginAsAdmin();

        Authority authority = authorityService.saveAuthority(featureBoardTestData.initializeAuthorityDTO("Test" , "user@das.gr"));
        FeatureBoardThreadDTO thread = (FeatureBoardThreadDTO) threads.get(8);
        thread.setResourceId(authority.getId());
        FeatureBoard fb =featureBoardService.saveThread(thread);
        FeatureBoardThreadDTO comment = featureBoardService.saveComment(fb.getId(),"123");

        mockMvc.perform(post("/featureboard/{id}/updateComment", comment.getId())
                        .param("selectionFromDD", resourceName)
                        .content("editedComment").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }
}
