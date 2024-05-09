package lgl.bayern.de.ecertby.resource.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.resource.CompanyProfileResource;

import lgl.bayern.de.ecertby.resource.integrationtests.data.CompanyTestData;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CompanyProfileService;
import lgl.bayern.de.ecertby.service.CompanyService;
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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CompanyProfileTest extends BaseTest {

    private MockMvc mockMvc;
    @Autowired
    private CompanyProfileService profileService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private CompanyProfileResource profileResource;
    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);
    private List<CompanyProfileDTO> profiles = new ArrayList<>();

    @NotNull
    private final CompanyTestData companyTestData = new CompanyTestData();
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    private CompanyDTO savedCompanyDTO;
    private AuthorityDTO savedAuthorityDTO;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        if (savedAuthorityDTO==null) {
            // to avoid transient entity errors, save to repository once
            savedAuthorityDTO = authorityService.save(companyTestData.initializeAuthorityDTO("TestAuthority","test@authority.com"));
        }
        if(savedCompanyDTO == null) {
            savedCompanyDTO = companyService.save(companyTestData.initializeCompanyDTO("TestCompany", "test@company.com", savedAuthorityDTO));
        }
        profiles = companyTestData.populateProfiles(savedCompanyDTO);
    }

    @Test
    @DisplayName("Creates Profile - Success")
    void testCreateCompanyProfile() throws Exception {
        String resourceName = loginAsAdmin();
        ObjectMapper mapper = new ObjectMapper();
        profiles.get(0).setResourceId(resourceName);
        String profileAsJSON = mapper.writeValueAsString(profiles.get(0));

        mockMvc.perform(post("/profile/create")
                        .content(profileAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }


    @Test
    @DisplayName("Updates Profile - Success")
    void testUpdateCompanyProfile() throws Exception {
        String resourceName = loginAsAdmin();
        ObjectMapper mapper = new ObjectMapper();
        CompanyProfileDTO profile = profiles.get(1);
        profile.setResourceId(AppConstants.Resource.ADMIN_RESOURCE);
        CompanyProfileDTO dto = profileService.saveProfile(profile);
        dto.setProfileName("Edited");
        dto.setCompanyId(savedCompanyDTO.getId());
        dto.setCompany(null);
        dto.setResourceId(resourceName);
        String profileAsJSON = mapper.writeValueAsString(dto);

        mockMvc.perform(post("/profile/update")
                        .content(profileAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        assertThat(profileService.findById(dto.getId()).getProfileName()).isEqualTo("Edited");

    }

    @Test
    @DisplayName("Gets Profile - Success")
    void testGetCompanyProfileSuccess() throws Exception {
        String resourceName = loginAsAdmin();
        profiles.get(2).setResourceId(resourceName);
        CompanyProfileDTO savedProfile = profileService.saveProfile(profiles.get(2));
        mockMvc.perform(get("/profile/{id}", savedProfile.getId())
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Gets all Profiles - Success")
    void testFindAllCompanyProfilesSuccess() throws Exception {
        String resourceName = loginAsAdmin();
        profiles.get(6).setResourceId(resourceName);
        CompanyProfileDTO savedProfile = profileService.saveProfile(profiles.get(6));
        mockMvc.perform(get("/profile")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));

    }

    @Test
    @DisplayName("Deletes Company Profile- Success")
    void testDeleteCompanyProfileSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        profiles.get(3).setResourceId(resourceName);
        CompanyProfileDTO savedProfile = profileService.saveProfile(profiles.get(3));

        mockMvc.perform(delete("/profile/{id}", savedProfile.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deactivate Company Profile - Success")
    void testDeactivateCompanyProfileSuccess() throws Exception {
        String resourceName = loginAsAdmin();
        profiles.get(4).setResourceId(resourceName);
        CompanyProfileDTO savedProfile = profileService.saveProfile(profiles.get(4));

        mockMvc.perform(patch("/profile/{id}/activate/{isActive}", savedProfile.getId() , false)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))

                .andExpect(status().isOk());
        assertThat(profileService.findById(savedProfile.getId()).isActive()).isFalse();
    }



    @Test
    @DisplayName("activate Company Profile - Success")
    void testActivateCompanyProfileSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        profiles.get(5).setResourceId(resourceName);
        CompanyProfileDTO savedProfile = profileService.saveProfile(profiles.get(5));
        savedProfile.setActive(false);

        mockMvc.perform(patch("/profile/{id}/activate/{isActive}", savedProfile.getId() , true)
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))

                .andExpect(status().isOk());
        assertThat(profileService.findById(savedProfile.getId()).isActive()).isTrue();
    }
}
