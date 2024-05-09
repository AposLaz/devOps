package lgl.bayern.de.ecertby.resource.integrationtests;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.repository.CatalogRepository;
import lgl.bayern.de.ecertby.resource.CatalogResource;
import lgl.bayern.de.ecertby.service.CatalogService;
import lgl.bayern.de.ecertby.service.CatalogValueService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CatalogTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private CatalogResource catalogResource;
    @Autowired
    private CatalogService catalogService;
    @Autowired
    private CatalogValueService catalogValueService;
    @Autowired
    private CatalogRepository catalogRepository;

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Find All Catalogs - Success")
    void testFindAllCatalogs() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/catalog")
                        .param("name", AppConstants.CatalogNames.DEPARTMENT)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].name", equalTo(AppConstants.CatalogNames.DEPARTMENT)));
    }

    @Test
    @DisplayName("Gets All Catalogs as OptionDTOs - Success")
    void testGetAllCatalogsAsOptionsSuccess() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/catalog/findAllCatalogs")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Gets Catalog - Success")
    void testGetCatalogSuccess() throws Exception {
        loginAsAdmin();
        OptionDTO catalog = catalogService.getAllCatalogs().get(0);

        mockMvc.perform(get("/catalog/{catalogId}", catalog.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(catalog.getName()));
    }

    @Test
    @Order(1)
    @DisplayName("Creates Catalog - Success")
    void testCreateCatalogSuccess() throws Exception {
        loginAsAdmin();
        MockMultipartFile csv = new MockMultipartFile(
                "catalogCSV",
                "test.csv",
                "text/csv",
                "\"ID\";\"Wert\"\n;\"Test1\";".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/catalog/upload")
                        .file(csv)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("Downloads Catalog - Success")
    void testDownloadCatalogSuccess() throws Exception {
        loginAsAdmin();

        Catalog catalog = catalogRepository.findByName("test");

        mockMvc.perform(get("/catalog/download/{id}", catalog.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsString("Test1")));
    }

    @Test
    @Order(3)
    @DisplayName("Overwrites Catalog - Success")
    void testOverwriteCatalogSuccess() throws Exception {
        loginAsAdmin();

        Catalog catalog = catalogRepository.findByName("test");
        String valueId = catalogValueService.findByCatalogName(catalog.getName()).get(0).getId();

        String content = "\"ID\";\"Wert\"\n\"" + valueId + "\";\"EditedTest1\";";
        MockMultipartFile csv = new MockMultipartFile(
                "catalogCSV",
                "test.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/catalog/{id}/overwrite", catalog.getId())
                        .file(csv)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(catalogValueService.findByCatalogName(catalog.getName()).get(0).getName()).isEqualTo("EditedTest1");
    }

    @Test
    @Order(4)
    @DisplayName("Deletes Catalog - Success")
    void testDeleteCatalogSuccess() throws Exception {
        loginAsAdmin();

        Catalog catalog = catalogRepository.findByName("test");
        String catalogId = catalog.getId();
        mockMvc.perform(delete("/catalog/{id}", catalogId)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThrows(QDoesNotExistException.class, () -> catalogService.findById(catalogId));
    }
}
