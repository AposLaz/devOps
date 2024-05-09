package lgl.bayern.de.ecertby.resource.integrationtests;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.mapper.CatalogMapper;
import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.repository.CatalogRepository;
import lgl.bayern.de.ecertby.resource.CatalogValueResource;
import lgl.bayern.de.ecertby.service.CatalogService;
import lgl.bayern.de.ecertby.service.CatalogValueService;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CatalogValueTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private CatalogValueResource catalogValueResource;
    @Autowired
    private CatalogValueService catalogValueService;
    @Autowired
    private CatalogRepository catalogRepository;
    CatalogMapper catalogMapper = Mappers.getMapper(CatalogMapper.class);

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogValueResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Find Catalog values - Success")
    void testFindCatalogValuesByCatalogSuccess() throws Exception {
        loginAsAdmin();

        String catalogId = catalogRepository.findIdByName(AppConstants.CatalogNames.DEPARTMENT);
        mockMvc.perform(get("/catalog-value/catalog/{id}", catalogId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "data,asc")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Get Catalog value - Success")
    void testGetCatalogValueSuccess() throws Exception {
        loginAsAdmin();

        String valueId = catalogValueService.findByCatalogName(AppConstants.CatalogNames.DEPARTMENT).get(0).getId();
        mockMvc.perform(get("/catalog-value/{id}", valueId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("Edit Catalog value - Success")
    void testEditCatalogValueSuccess() throws Exception {
        loginAsAdmin();

        CatalogValueDTO catalogValueDTO = catalogValueService.findById(catalogValueService.findByCatalogName(AppConstants.CatalogNames.DEPARTMENT).get(0).getId());
        catalogValueDTO.setData("EditedDepartmentValue");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String catalogValueAsJSON = mapper.writeValueAsString(catalogValueDTO);

        mockMvc.perform(post("/catalog-value/update")
                        .content(catalogValueAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(catalogValueService.findByCatalogName(AppConstants.CatalogNames.DEPARTMENT).get(0).getName()).isEqualTo("EditedDepartmentValue");
    }

    @Test
    @DisplayName("Deletes Catalog value - Success")
    void testDeleteCatalogValueSuccess() throws Exception {
        loginAsAdmin();

        Catalog catalog = catalogRepository.findByName(AppConstants.CatalogNames.DEPARTMENT);
        CatalogValueDTO catalogValueDTO = new CatalogValueDTO();
        catalogValueDTO.setData("TestValue");
        catalogValueDTO.setCatalog(catalogMapper.map(catalog));
        CatalogValueDTO savedCatalogValueDTO = catalogValueService.save(catalogValueDTO);
        String savedCatalogValueId = savedCatalogValueDTO.getId();
        assertThat(catalogValueService.findById(savedCatalogValueId).getData()).isEqualTo("TestValue");

        mockMvc.perform(delete("/catalog-value/{id}", savedCatalogValueDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThrows(QDoesNotExistException.class, () -> catalogValueService.findById(savedCatalogValueId));
    }
}
