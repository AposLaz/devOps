package lgl.bayern.de.ecertby.resource.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.SearchCriteriaMapper;
import lgl.bayern.de.ecertby.model.SearchCriteria;
import lgl.bayern.de.ecertby.model.UserCompany;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lgl.bayern.de.ecertby.repository.UserAuthorityRepository;
import lgl.bayern.de.ecertby.repository.UserCompanyRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.resource.SearchCriteriaResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.SeachCriteriaTestData;
import lgl.bayern.de.ecertby.service.SearchCriteriaService;

import org.junit.jupiter.api.*;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;
import com.eurodyn.qlack.common.exception.QDoesNotExistException;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SearchCriteriaTest extends BaseTest {

        private MockMvc mockMvc;

        @Autowired
        private SearchCriteriaResource searchCriteriaResource;
        private SearchCriteriaMapper searchCriteriaMapper = Mappers.getMapper(SearchCriteriaMapper.class);

        @Autowired
        private SearchCriteriaService searchCriteriaService;
        @Autowired
        private UserDetailRepository userDetailRepository;

        @Autowired
        private UserCompanyRepository userCompanyRepository;
        private List<SearchCriteriaDTO> searchCriteriaByAdmin = new ArrayList<>();
        private List<SearchCriteriaDTO> searchCriteriaByAuthority = new ArrayList<>();
        private List<SearchCriteriaDTO> searchCriteriaByCompany = new ArrayList<>();

        @Inject
        private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

        @jakarta.validation.constraints.NotNull
        private SeachCriteriaTestData seachCriteriaTestData = new SeachCriteriaTestData();

        @BeforeEach
        void init() {
                mockMvc = MockMvcBuilders.standaloneSetup(searchCriteriaResource)
                                .setControllerAdvice(new ExceptionControllerAdvisor())
                                .setCustomArgumentResolvers(querydslPredicateArgumentResolver,
                                                new PageableHandlerMethodArgumentResolver())
                                .build();
                searchCriteriaByAdmin = seachCriteriaTestData.initializeSearchCriteriaDTOByAdmin();
                searchCriteriaByAuthority = seachCriteriaTestData.initializeSearchCriteriaDTOByAuthority();
                searchCriteriaByCompany = seachCriteriaTestData.initializeSearchCriteriaDTOByCompany();
        }

        @AfterEach
        void clearDB() {
                searchCriteriaService.findAll().stream()
                                .forEach(dto -> searchCriteriaService.deleteSearchCriteria(dto.getId()));
        }

        @Test
        @DisplayName("Admin Creates search criteria - Success")
        void testAdminCreateSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                ObjectMapper mapper = new ObjectMapper();
                String searCriteria = mapper.writeValueAsString(searchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/create")
                                .content(searCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(searchCriteriaByAdmin.get(0).getName()));
        }

        @Test
        @DisplayName("Authority creates search criteria - Success")
        void testAuthorityCreateSearchCriteriaSuccess() throws Exception {
                UserDetailDTO ud = loginAsAuthority(baseTestData.getUserDetailIdAuthority(), true);
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAuthority.get(0);
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(searchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/create")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", ud.getPrimaryAuthority().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(searchCriteriaByAuthority.get(0).getName()));
        }

        @Test
        @DisplayName("Company creates search criteria - Success")
        void testCompanyCreateSearchCriteriaSuccess() throws Exception {
                UserDetailDTO ud = loginAsCompany();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByCompany.get(0);
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(searchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/create")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", ud.getPrimaryCompany().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(searchCriteriaByCompany.get(0).getName()));
        }

        @Test
        @DisplayName("Admin Updates search criteria - Success")
        void testUpdateSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                savedSearchCriteriaDTO.setName("Name Edited");
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(savedSearchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/update")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Name Edited"));
        }

        @Test
        @DisplayName("Authority Updates own search criteria - Success")
        void testAuthorityUpdateSearchCriteriaSuccess() throws Exception {
                UserDetailDTO authorityUser = loginAsAuthority();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAuthority.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                authorityUser.getPrimaryAuthority().getId());
                savedSearchCriteriaDTO.setName("Name Edited");
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(savedSearchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/update")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Name Edited"));
                assertEquals(1, searchCriteriaService.findAll().size());
        }

        @Test
        @DisplayName("Company Updates own search criteria - Success")
        void testCompanyUpdateSearchCriteriaSuccess() throws Exception {
                UserDetailDTO companyUser = loginAsCompany();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByCompany.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                companyUser.getPrimaryCompany().getId());
                savedSearchCriteriaDTO.setName("Name Edited");
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(savedSearchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/update")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Name Edited"));
                assertEquals(1, searchCriteriaService.findAll().size());
        }

        @Test
        @DisplayName("Authority Updates Admin's search criteria - Success")
        void testAuthorityUpdateAdminSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                UserDetailDTO authorityUser = loginAsAuthority();
                savedSearchCriteriaDTO.setName("Name Edited");
                savedSearchCriteriaDTO.setSearchCriteriaGroupDTOSet(new HashSet<>());
                savedSearchCriteriaDTO.getSearchCriteriaGroupDTOSet().add(GroupType.AUTHORITY);
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(savedSearchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/update")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Name Edited"));
                assertEquals(2, searchCriteriaService.findAll().size());
        }

        @Test
        @DisplayName("Company Updates Admin's search criteria - Success")
        void testCompanyUpdateAdminSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                UserDetailDTO companyUser = loginAsCompany();
                savedSearchCriteriaDTO.setName("Name Edited");
                savedSearchCriteriaDTO.setSearchCriteriaGroupDTOSet(new HashSet<>());
                savedSearchCriteriaDTO.getSearchCriteriaGroupDTOSet().add(GroupType.COMPANY);
                ObjectMapper mapper = new ObjectMapper();
                String searchCriteria = mapper.writeValueAsString(savedSearchCriteriaDTO);
                mockMvc.perform(post("/search-criteria/update")
                                .content(searchCriteria).accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Name Edited"));
                assertEquals(2, searchCriteriaService.findAll().size());
        }

        @Test
        @DisplayName("Gets search criteria  - Success")
        void testGetSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                mockMvc.perform(get("/search-criteria/{id}", savedSearchCriteriaDTO.getId())
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(searchCriteriaByAdmin.get(0).getName()));
        }

        @Test
        @DisplayName("Deletes search criteria - Success")
        void testDeleteSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                mockMvc.perform(delete("/search-criteria/{id}", savedSearchCriteriaDTO.getId())
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk());
                assertThrows(QDoesNotExistException.class, () -> {
                        searchCriteriaService.findById(savedSearchCriteriaDTO.getId());
                });
        }

        @Test
        @DisplayName("Get search criteria for Admin- Success")
        void testGetSearchCriteriaForAdminSuccess() throws Exception {
                loginAsAdmin();
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(0), "ADMIN_RESOURCE");
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(1), "ADMIN_RESOURCE");
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(4), "ADMIN_RESOURCE");
                MvcResult result = mockMvc.perform(get("/search-criteria/findByUser")
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andReturn();
                String jsonResponse = result.getResponse().getContentAsString();
                List<SearchCriteriaDTO> responseList = new ObjectMapper().readValue(jsonResponse, List.class);
                assertEquals(3, responseList.size());
        }

        @Test
        @DisplayName("Get search criteria for Authority- Success")
        void testGetSearchCriteriaForAuthoritySuccess() throws Exception {
                loginAsAdmin();
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(2), "ADMIN_RESOURCE");
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(4), "ADMIN_RESOURCE");
                UserDetailDTO authorityUser = loginAsAuthority();
                for (int i = 0; i <= 1; i++) {
                        searchCriteriaService.saveSearchCriteria(searchCriteriaByAuthority.get(i),
                                        authorityUser.getPrimaryAuthority().getId());
                }
                MvcResult result = mockMvc.perform(get("/search-criteria/findByUser")
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andReturn();
                String jsonResponse = result.getResponse().getContentAsString();
                List<SearchCriteriaDTO> responseList = new ObjectMapper().readValue(jsonResponse, List.class);
                assertEquals(4, responseList.size());
        }

        @Test
        @DisplayName("Get search criteria for Company User- Success")
        void testGetSearchCriteriaForCompanySuccess() throws Exception {
                loginAsAdmin();
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(3), "ADMIN_RESOURCE");
                searchCriteriaService.saveSearchCriteria(searchCriteriaByAdmin.get(4), "ADMIN_RESOURCE");
                UserDetailDTO companyUser = loginAsCompany();
                for (int i = 0; i <= 1; i++) {
                        searchCriteriaService.saveSearchCriteria(searchCriteriaByCompany.get(i),
                                        companyUser.getPrimaryCompany().getId());
                }
                MvcResult result = mockMvc.perform(get("/search-criteria/findByUser")
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andReturn();
                String jsonResponse = result.getResponse().getContentAsString();
                List<SearchCriteriaDTO> responseList = new ObjectMapper().readValue(jsonResponse, List.class);
                assertEquals(4, responseList.size());
        }

        @Test
        @DisplayName("Mark as default search criteria - Success")
        void testMarkAsDefaultSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                mockMvc.perform(get("/search-criteria/{id}/markAsDefault", savedSearchCriteriaDTO.getId())
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk());
                assertEquals(savedSearchCriteriaDTO.getId(),
                                userDetailRepository.findById(baseTestData.getUserDetailIdAdmin()).get()
                                                .getSearchCriteria().getId());
        }

        @Test
        @DisplayName("Unmark as default search criteria - Success")
        void testUnMarkAsDefaultSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);
                SearchCriteria savedSearchCriteria = searchCriteriaMapper.mapToEntity(savedSearchCriteriaDTO);
                UserDetail user = userDetailRepository.findById(baseTestData.getUserDetailIdAdmin()).get();
                user.setSearchCriteria(savedSearchCriteria);
                userDetailRepository.save(user);
                mockMvc.perform(get("/search-criteria/{id}/markAsDefault", savedSearchCriteriaDTO.getId())
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk());
                assertNull(
                                userDetailRepository.findById(baseTestData.getUserDetailIdAdmin()).get()
                                                .getSearchCriteria());
        }

        @Test
        @DisplayName("find user's default search criteria - Success")
        void testFindAdminDefaultSearchCriteriaSuccess() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                                null);

                SearchCriteria savedSearchCriteria = searchCriteriaMapper.mapToEntity(savedSearchCriteriaDTO);
                UserDetail user = userDetailRepository.findById(baseTestData.getUserDetailIdAdmin()).get();
                user.setSearchCriteria(savedSearchCriteria);
                userDetailRepository.save(user);
                mockMvc.perform(get("/search-criteria/findDefaultCriteria")
                                .param("selectionFromDD", "ADMIN_RESOURCE")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(savedSearchCriteriaDTO.getId()));
        }

        @Test
        @DisplayName("find Company's default search criteria - Success")
        void testFindCompanyDefaultSearchCriteriaSuccess() throws Exception {
                UserDetailDTO companyUser = loginAsCompany();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByCompany.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                        companyUser.getPrimaryCompany().getId());
                SearchCriteria savedSearchCriteria = searchCriteriaMapper.mapToEntity(savedSearchCriteriaDTO);
                UserCompany userCompanyByCompanyIdAndUserDetailId = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(companyUser.getPrimaryCompany().getId(), companyUser.getId());
                userCompanyByCompanyIdAndUserDetailId.setSearchCriteria(savedSearchCriteria);
                userCompanyRepository.save(userCompanyByCompanyIdAndUserDetailId);
                mockMvc.perform(get("/search-criteria/findDefaultCriteria")
                                .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(savedSearchCriteriaDTO.getId()));
        }

        @Test
        @DisplayName("Finds all Search Criteria as Page SearchCriteriaDTO - Success")
        void testFindAllTemplatesAsPageTemplateDTO() throws Exception {
                loginAsAdmin();
                SearchCriteriaDTO searchCriteriaDTO = searchCriteriaByAdmin.get(0);
                SearchCriteriaDTO savedSearchCriteriaDTO = searchCriteriaService.saveSearchCriteria(searchCriteriaDTO,
                        null);

                mockMvc.perform(get("/search-criteria")
                                .param("name", "searchCriteriaByAdminForAdmin")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "name,asc")
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("content", hasSize(1)))
                        .andExpect(jsonPath("content[0].name", equalTo(searchCriteriaByAdmin.get(0).getName())));
        }
}
