package lgl.bayern.de.ecertby.resource.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.AttributeMapper;
import lgl.bayern.de.ecertby.mapper.HtmlElementMapper;
import lgl.bayern.de.ecertby.mapper.TemplateMapper;
import lgl.bayern.de.ecertby.model.Attribute;
import lgl.bayern.de.ecertby.model.Template;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lgl.bayern.de.ecertby.model.util.PDFElementTypeEnum;
import lgl.bayern.de.ecertby.repository.TemplateRepository;
import lgl.bayern.de.ecertby.resource.HtmlElementResource;
import lgl.bayern.de.ecertby.resource.TemplateResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.TemplateTestData;
import lgl.bayern.de.ecertby.service.AttributeService;
import lgl.bayern.de.ecertby.service.HtmlElementService;
import lgl.bayern.de.ecertby.service.TemplateService;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TemplateTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateResource templateResource;

    @NotNull
    private List<TemplateDTO> templateList = new ArrayList<>();

    @NotNull
    private TemplateTestData templateTestData = new TemplateTestData();

    TemplateMapper templateMapper = Mappers.getMapper(TemplateMapper.class);

    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(templateResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        templateList = templateTestData.populateTemplates();
    }

    @Test
    @DisplayName("Gets template - Success")
    void testGetTemplateSuccess() throws Exception {
        TemplateDTO savedTemplateDTO = templateService.save(templateList.get(0));

        mockMvc.perform(get("/template/{id}", savedTemplateDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateName").value(templateList.get(0).getTemplateName()));
    }

    @Test
    @DisplayName("Gets template by id - Success")
    void testGetTemplateByIdSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();
        TemplateDTO savedTemplateDTO = templateService.save(templateList.get(2));

        mockMvc.perform(get("/template/getTemplateById/{id}", savedTemplateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateName").value(templateList.get(2).getTemplateName()));
    }

    @Test
    @DisplayName("Finds all templates as Page TemplateDTO - Success")
    void testFindAllTemplatesAsPageTemplateDTO() throws Exception {
        templateService.save(templateList.get(1));

        loginAsAdmin();
        mockMvc.perform(get("/template")
                        .param("templateName", "Template 2")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "templateName,asc")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].templateName", equalTo(templateList.get(1).getTemplateName())));
    }

    @Test
    @DisplayName("Creates template - Success")
    @Order(1)
    void testCreateTemplateSuccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String templateAsJSON = mapper.writeValueAsString(templateList.get(9));

        loginAsAdmin();

        MockMultipartFile templateJSON = new MockMultipartFile("templateDTO", null,
                "application/json", templateAsJSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/template/create")
                        .file(templateJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Creates existing template - Exception")
    @Order(2)
    void testCreateExistingTemplate() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String templateAsJSON = mapper.writeValueAsString(templateList.get(9));

        loginAsAdmin();

        MockMultipartFile templateJSON = new MockMultipartFile("templateDTO", null,
                "application/json", templateAsJSON.getBytes(StandardCharsets.UTF_8));

        ResultActions existingTemplate = mockMvc.perform(multipart("/template/create")
                        .file(templateJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest());
        List<EcertBYErrorException> ecertBYErrorExceptions = ((EcertBYGeneralException) existingTemplate.andReturn().getResolvedException().getCause()).getErrors();
        assertTrue(ecertBYErrorExceptions.stream().filter(o -> o.getCode().equals("error_template_name_exists_template")).findAny().isPresent());
        assertTrue(ecertBYErrorExceptions.stream().filter(o -> o.getCode().equals("error_target_country_product_exists_template")).findAny().isPresent());

    }

    @Test
    @DisplayName("Updates template - Success")
    void testUpdateTemplateSuccess() throws Exception {
        TemplateDTO savedTemplateDTO = templateService.save(templateList.get(3));

        savedTemplateDTO.setTemplateName(templateTestData.getTemplateNameUpdate());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String templateAsJSON = mapper.writeValueAsString(savedTemplateDTO);

        loginAsAdmin();

        MockMultipartFile templateJSON = new MockMultipartFile("templateDTO", null,
                "application/json", templateAsJSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/template/update")
                        .file(templateJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        assertThat(templateService.findById(savedTemplateDTO.getId()).getTemplateName()).isEqualTo(templateTestData.getTemplateNameUpdate());
    }

    @Test
    @DisplayName("Deactivates template - Success")
    void testDeactivateTemplateSuccess() throws Exception {
        TemplateDTO templateDTO = templateService.save(templateList.get(4));

        loginAsAdmin();
        mockMvc.perform(patch("/template/{id}/activate/{isActive}", templateDTO.getId(), false)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(templateService.findById(templateDTO.getId()).isActive()).isFalse();
    }

    @Test
    @DisplayName("Activates template - Success")
    void testActivateTemplateSuccess() throws Exception {
        TemplateDTO templateDTO = templateService.save(templateList.get(5));
        templateDTO.setActive(false);
        TemplateDTO savedTemplateDTO = templateService.save(templateDTO);

        loginAsAdmin();
        mockMvc.perform(patch("/template/{id}/activate/{isActive}", savedTemplateDTO.getId(), true)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(templateService.findById(savedTemplateDTO.getId()).isActive()).isTrue();
    }

    @Test
    @DisplayName("Releases template - Success")
    void testReleaseTemplateSuccess() throws Exception {
        TemplateDTO templateDTO = templateService.save(templateList.get(6));

        loginAsAdmin();
        mockMvc.perform(patch("/template/{id}/release", templateDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(templateService.findById(templateDTO.getId()).isRelease()).isTrue();
    }

    @Test
    @DisplayName("Gets template by target country id and product id as TemplateDTO list - Success")
    void testGetTemplateByTargetCountryIdAndProductIdAsTemplateDTOListSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TemplateDTO templateDTO = templateService.save(templateList.get(8));
        templateService.release(templateDTO.getId(), Template.class);

        mockMvc.perform(get("/template/country/{targetCountryId}/product/{productId}", templateTestData.getTargetCountryList().get(8).getId(), templateTestData.getProduct().getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("content")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].templateName", equalTo(templateList.get(8).getTemplateName())));
    }

    @Test
    @DisplayName("Gets a template's keywords by id - Success")
    void testGetTemplateKeywordsByIdSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TemplateDTO templateDTO = templateService.save(templateList.get(8));

        mockMvc.perform(get("/template/keywords/{templateId}", templateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Kontamination")));
    }

    @Test
    @DisplayName("Gets a template's comment by id - Success")
    void testGetTemplateCommentByIdSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TemplateDTO templateDTO = templateList.get(8);
        templateDTO.setComment("comment");
        TemplateDTO savedTemplateDTO = templateService.save(templateDTO);

        mockMvc.perform(get("/template/comment/{templateId}", savedTemplateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(savedTemplateDTO.getComment()));
    }

}
