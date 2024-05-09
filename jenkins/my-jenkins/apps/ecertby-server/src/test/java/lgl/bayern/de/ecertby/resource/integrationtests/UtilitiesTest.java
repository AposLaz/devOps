package lgl.bayern.de.ecertby.resource.integrationtests;

import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.resource.UtilitiesResource;
import lgl.bayern.de.ecertby.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UtilitiesTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private TemplateService templateService;
    @Autowired
    private UtilitiesResource utilitiesResource;

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(utilitiesResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Gets catalog values filtered by catalog enum - Success")
    void testGetCatalogValuesSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/utilities/catalog/{catalogEnum}", "DEPARTMENT_CATALOG")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
    }

    @Test
    @DisplayName("Gets enum list filtered by name - Success")
    void testGetEnumListSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/utilities/enumList/{enumName}", "CertificateStatus")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));
    }

    @Test
    @DisplayName("Gets parent user groups - Success")
    void testGetParentGroupsSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/utilities/groupListParent")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Gets child user groups - Success")
    void testGetGroupsSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/utilities/groupList")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)));
    }

    @Test
    @DisplayName("Gets target countries - Success")
    void testGetTargetCountryListSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        mockMvc.perform(get("/utilities/targetCountries")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(253)));
    }

    @Test
    @DisplayName("Gets available target countries - Success")
    void testGetAvailableTargetCountryListSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        TemplateDTO templateDto = baseTestData.initializeTemplateDTO("TempTemplate", 0);
        templateDto.setRelease(true);
        templateService.saveTemplate(templateDto, null);

        mockMvc.perform(get("/utilities/availableTargetCountries")
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].name", equalTo(templateDto.getTargetCountry().getName())));
    }

    @Test
    @DisplayName("Gets available products - Success")
    void testGetAvailableProductListSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        TemplateDTO templateDto = baseTestData.initializeTemplateDTO("TempTemplate2", 1);
        templateDto.setRelease(true);

        templateService.saveTemplate(templateDto, null);

        mockMvc.perform(get("/utilities/availableProducts/{targetCountryId}", templateDto.getTargetCountry().getId())
                        .param("selectionFromDD", resourceName)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo(templateDto.getProduct().getName())));
    }
}
