package lgl.bayern.de.ecertby.resource.integrationtests;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.TemplateMapper;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lgl.bayern.de.ecertby.model.util.PDFElementTypeEnum;
import lgl.bayern.de.ecertby.repository.TemplateRepository;
import lgl.bayern.de.ecertby.resource.HtmlElementResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.TemplateTestData;
import lgl.bayern.de.ecertby.service.AttributeService;
import lgl.bayern.de.ecertby.service.HtmlElementService;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HtmlElementTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private HtmlElementResource htmlElementResource;

    @NotNull
    private List<TemplateDTO> templateList = new ArrayList<>();

    @NotNull
    private TemplateTestData templateTestData = new TemplateTestData();

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private HtmlElementService htmlElementService;

    @Autowired
    private TemplateRepository templateRepository;

    TemplateMapper templateMapper = Mappers.getMapper(TemplateMapper.class);


    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(htmlElementResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        templateList = templateTestData.populateTemplates();
    }

    @Test
    @DisplayName("Adds Template Elements and Html Elements to Template")
    void testSetTemplateElements() throws Exception {

        loginAsAdmin();

        TemplateDTO templateDTO = templateList.get(8);

        TemplateElementValueDTO templateElementValueDTO = new TemplateElementValueDTO();
        templateElementValueDTO.setValue("option1");
        TemplateElementValueDTO templateElementValueDTO2 = new TemplateElementValueDTO();
        templateElementValueDTO2.setValue("option2");
        TemplateElementDTO templateElementDTO = new TemplateElementDTO();
        templateElementDTO.setName("element1");
        templateElementDTO.setElementType(PDFElementTypeEnum.RADIO_GROUP);
        templateElementDTO.setTemplateElementValueDTOSet(new HashSet<>(Arrays.asList(templateElementValueDTO, templateElementValueDTO2)));
        TemplateElementDTO templateElementDTO2 = new TemplateElementDTO();
        templateElementDTO2.setName("element2");
        templateElementDTO2.setElementType(PDFElementTypeEnum.TEXT_FIELD);

        templateDTO.setTemplateElementDTOSet(new HashSet<>(Arrays.asList(templateElementDTO, templateElementDTO2)));

        TemplateDTO savedTemplateDTO = templateMapper.map(templateRepository.save(templateMapper.map(templateDTO)));

        AttributeDTO attributeDTO = new AttributeDTO();
        attributeDTO.setElementType(ElementType.TEXT_FIELD);
        attributeDTO.setName("attr1");
        attributeDTO.setHtmlElementName("attr1");
        AttributeDTO savedAttributeDTO = attributeService.saveAttribute(attributeDTO);

        HtmlElementDTO htmlElementDTO = new HtmlElementDTO();
        htmlElementDTO.setName("htmlelement");
        htmlElementDTO.setElementType(ElementType.TEXT_FIELD);
        htmlElementDTO.setTemplateElementDTO(templateElementDTO2);
        htmlElementDTO.setAttributeDTO(savedAttributeDTO);
        htmlElementDTO.setTemplateDTO(savedTemplateDTO);
        htmlElementDTO.setSortOrder(8);

        HtmlElementDTO savedHtmlElementDTO = htmlElementService.save(htmlElementDTO);


        AttributeDTO attribute2DTO = new AttributeDTO();
        attribute2DTO.setElementType(ElementType.RADIO_GROUP);
        attribute2DTO.setName("attr2");
        attribute2DTO.setHtmlElementName("attr2");
        AttributeDTO savedAttribute2DTO = attributeService.saveAttribute(attribute2DTO);

        HtmlElementDTO htmlElementDTO2 = new HtmlElementDTO();
        htmlElementDTO2.setName("htmlelement");
        htmlElementDTO2.setElementType(ElementType.RADIO_GROUP);
        htmlElementDTO2.setTemplateElementDTO(templateElementDTO2);
        htmlElementDTO2.setAttributeDTO(savedAttribute2DTO);
        htmlElementDTO2.setTemplateDTO(savedTemplateDTO);
        htmlElementDTO2.setSortOrder(1);
        HtmlElementRadioButtonDTO radioButtonDTO = new HtmlElementRadioButtonDTO();
        radioButtonDTO.setName("radio1");
        radioButtonDTO.setSortOrder(1);
        radioButtonDTO.setTemplateElementValueDTO(templateElementValueDTO);

        HtmlElementRadioButtonDTO radioButtonDTO2 = new HtmlElementRadioButtonDTO();
        radioButtonDTO2.setName("radio2");
        radioButtonDTO2.setSortOrder(2);
        radioButtonDTO2.setTemplateElementValueDTO(templateElementValueDTO2);

        htmlElementDTO2.setRadioButtonDTOSet(new HashSet<>(Arrays.asList(radioButtonDTO, radioButtonDTO2)));

        HtmlElementDTO savedHtmlElementDTO2 = htmlElementService.save(htmlElementDTO2);

        mockMvc.perform(get("/htmlElement/getByTemplateId/{templateId}", savedTemplateDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));


    }

}
