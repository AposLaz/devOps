package lgl.bayern.de.ecertby.resource.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.model.util.FileUploadType;
import lgl.bayern.de.ecertby.resource.CertificateResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.CertificateTestData;
import lgl.bayern.de.ecertby.resource.integrationtests.data.CompanyTestData;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CertificateService;
import lgl.bayern.de.ecertby.service.CertificateUpdateService;
import lgl.bayern.de.ecertby.service.CompanyService;
import lgl.bayern.de.ecertby.service.FileService;
import lgl.bayern.de.ecertby.service.TemplateService;
import lgl.bayern.de.ecertby.service.UserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CertificateTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateUpdateService certificateUtilService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CertificateResource certificateResource;
    @Autowired
    private UserDetailService userDetailService;

    private List<CertificateDTO> certificates = new ArrayList<>();

    CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);

    @Autowired
    private com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;

    @NotNull
    private final CertificateTestData certificateTestData = new CertificateTestData();

    @NotNull
    private final CompanyTestData companyTestData = new CompanyTestData();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    private AuthorityDTO savedAuthorityDTO;
    private CompanyDTO savedCompanyDTO;
    private TemplateDTO savedTemplateDTO;

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(certificateResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        if (savedAuthorityDTO==null) {
            // to avoid transient entity errors, save to repository once
            savedAuthorityDTO = authorityService.save(certificateTestData.initializeAuthorityDTO("TestAuthority","test@authority.com"));
        }
        if (savedCompanyDTO==null) {
            savedCompanyDTO = companyService.save(certificateTestData.initializeCompanyDTO("TestCompany","test@company.com",savedAuthorityDTO));
        }
        if (savedTemplateDTO==null) {
            savedTemplateDTO = templateService.save(certificateTestData.initializeTemplateDTO("TestTemplate",0));
        }
        certificates = certificateTestData.populateCertificates(savedAuthorityDTO,savedCompanyDTO,savedTemplateDTO);
    }

    @Test
    @DisplayName("Gets all Certificates of an Authority - Success")
    void testFindAllAuthorityCertificatesSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(8);
        certificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "companyNumber,asc")
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].companyNumber", equalTo(certificates.get(8).getCompanyNumber())));
    }
    @Test
    @DisplayName("Gets all Certificates of a Company - Success")
    void testFindAllCompanyCertificatesSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(8);
        certificateDTO.setCompanyNumber("AAA");
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "companyNumber,asc")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].companyNumber", equalTo(savedCertificateDTO.getCompanyNumber())));
    }
    @Test
    @DisplayName("Gets all Certificates of an Admin - Success")
    void testFindAllAdminCertificatesSuccess() throws Exception {
        String resource = loginAsAdmin();

        CertificateDTO certificateDTO = certificates.get(8);
        certificateDTO.setCompanyNumber("AAA");
        certificateDTO.setStatus(CertificateStatus.RELEASED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "companyNumber,asc")
                        .param("selectionFromDD", resource)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].companyNumber", equalTo(savedCertificateDTO.getCompanyNumber())));
    }
    @Test
    @DisplayName("Gets all deleted Certificates - Success")
    void testFindAllDeletedCertificatesSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(8);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateDTO.setStatus(CertificateStatus.DELETED);
        certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate/recycle_bin")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "companyNumber,asc")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("content[0].companyNumber", equalTo(certificates.get(8).getCompanyNumber())));
    }
    @Test
    @DisplayName("Creates Certificate - Success")
    void testCreateCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String certificateAsJSON = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateJSON = new MockMultipartFile("certificateDTO", null,
                "application/json", certificateAsJSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/certificate/create")
                        .file(certificateJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
    }
    @Test
    @DisplayName("Updates Certificate - Success")
    void testUpdateCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(1);
        // To give rights to companyUser for this specific certificate
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);
        savedCertificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        savedCertificateDTO.setCompanyNumber("EditedCompanyNumber");
        savedCertificateDTO.setCertificateFile(new DocumentDTO());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String certificateAsJSON = mapper.writeValueAsString(savedCertificateDTO);
        MockMultipartFile certificateJSON = new MockMultipartFile("certificateDTO", null,
                "application/json", certificateAsJSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/certificate/update")
                        .file(certificateJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getCompanyNumber()).isEqualTo("EditedCompanyNumber");
    }
    @Test
    @DisplayName("Gets Certificate - Success")
    void testGetCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(2);
        certificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate/{certificateId}", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyNumber").value(certificates.get(2).getCompanyNumber()));
    }
    @Test
    @DisplayName("Releases Certificate - Success")
    void testReleaseCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(9);
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateDTO.setCompletedForward(true);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/release", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.RELEASED);
    }
    @Test
    @DisplayName("Rejects Certificate - Success")
    void testRejectCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(3);
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateDTO.setCompletedForward(true);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/reject", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .content("Rejections reason for the test.")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.REJECTED_CERTIFICATE);
    }
    @Test
    @DisplayName("Marks Certificate as Lost - Success")
    void testMarkCertificateAsLostSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(6);
        certificateDTO.setStatus(CertificateStatus.RELEASED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/mark_as_lost", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.LOST);
    }
    @Test
    @DisplayName("Revokes Certificate - Success")
    void testRevokeCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(5);
        certificateDTO.setStatus(CertificateStatus.RELEASED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/revoke", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.REVOKED);
    }
    @Test
    @DisplayName("Blocks Certificate - Success")
    void testBlockCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(4);
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateDTO.setCompletedForward(true);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/block", savedCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.BLOCKED);
    }
    @Test
    @DisplayName("Deletes Certificate - Success")
    void testDeleteCertificateSuccess() throws Exception {
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(7));
        CertificateDTO preCertificateDTO = certificates.get(7);
        preCertificateDTO.setParentCertificate(savedCertificateDTO);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_DRAFT);
        certificateService.save(preCertificateDTO);

        UserDetailDTO companyUser = loginAsCompany();
        mockMvc.perform(patch("/certificate/{certificateId}/delete", savedCertificateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.DELETED);
    }
    @Test
    @DisplayName("Votes PreCertificate positive - Success")
    void testVotePositivePreCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO parentCertificateDTO = certificates.get(13);
        parentCertificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        CertificateDTO savedParentCertificateDTO = certificateService.save(parentCertificateDTO);
        CertificateDTO preCertificateDTO = certificates.get(14);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_FORWARDED);
        preCertificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        preCertificateDTO.setParentCertificate(savedParentCertificateDTO);
        CertificateDTO savedPreCertificateDTO = certificateService.save(preCertificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/vote_positive", savedPreCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedPreCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE);
    }
    @Test
    @DisplayName("Rejects PreCertificate - Success")
    void testRejectPreCertificateSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO parentCertificateDTO = certificateService.save(certificates.get(13));
        CertificateDTO preCertificateDTO = certificates.get(14);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_FORWARDED);
        preCertificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        preCertificateDTO.setParentCertificate(parentCertificateDTO);
        CertificateDTO savedPreCertificateDTO = certificateService.save(preCertificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/pre_certificate_reject", savedPreCertificateDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .content("Rejections reason for the test.")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedPreCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.PRE_CERTIFICATE_REJECTED);
    }
    @Test
    @DisplayName("Forwards PreCertificate - Success")
    void testForwardPreCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO parentCertificateDTO = certificateService.save(certificates.get(13));
        CertificateDTO preCertificateDTO = certificates.get(14);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_DRAFT);
        preCertificateDTO.setForwardAuthority(preCertificateDTO.getIssuingAuthority());
        preCertificateDTO.setParentCertificate(parentCertificateDTO);
        CertificateDTO savedPreCertificateDTO = certificateService.save(preCertificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/pre_certificate_forwarded", savedPreCertificateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedPreCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.PRE_CERTIFICATE_FORWARDED);
    }
    @Test
    @DisplayName("Excludes PreCertificate - Success")
    void testExcludePreCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO parentCertificateDTO = certificates.get(13);
        parentCertificateDTO.setForwardAuthority(parentCertificateDTO.getIssuingAuthority());
        CertificateDTO savedParentCertificateDTO = certificateService.save(parentCertificateDTO);
        CertificateDTO preCertificateDTO = certificates.get(14);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_REJECTED);
        preCertificateDTO.setForwardAuthority(preCertificateDTO.getIssuingAuthority());
        preCertificateDTO.setParentCertificate(savedParentCertificateDTO);
        CertificateDTO savedPreCertificateDTO = certificateService.save(preCertificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/pre_certificate_exclude", savedPreCertificateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedPreCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.PRE_CERTIFICATE_EXCLUDED);
    }
    @Test
    @DisplayName("Restores Certificate - Success")
    void testRestoreCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(7);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateDTO.setStatus(CertificateStatus.DELETED);
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(7));
        CertificateDTO preCertificateDTO = certificates.get(7);
        preCertificateDTO.setParentCertificate(savedCertificateDTO);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_DELETED);
        certificateService.save(preCertificateDTO);

        mockMvc.perform(patch("/certificate/{certificateId}/restore_draft", savedCertificateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.DRAFT);
    }
    @Test
    @DisplayName("Copies Certificate - Success")
    void testCopyCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Create original certificate with additional file
        CertificateDTO certificateDTO = certificates.get(15);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateAdditionalDocs1 =  certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc1.pdf");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/create")
                        .file(certificateDTOPart)
                        .file(certificateAdditionalDocs1)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        String savedCertificateId = JsonPath.read(resultAsString, "$.id");
        String savedCertificateCompanyNumber = JsonPath.read(resultAsString, "$.companyNumber");

        mockMvc.perform(patch("/certificate/{originalCertificateId}/copy", savedCertificateId)
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyNumber").value(savedCertificateCompanyNumber));
    }
    @Test
    @DisplayName("Gets a Certificate's keywords - Success")
    void testGetCertificateKeywordsByIdSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        CertificateDTO certificateDTO = certificates.get(10);
        Set<OptionDTO> keywordSet = new HashSet<>(1);
        keywordSet.add(baseTestData.initializeOption("WOAH", "46fed0f5-5c30-490d-ae8a-f73cb4bead22",true));
        certificateDTO.setKeywordSet(keywordSet);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate/keywords/{certificateId}", savedCertificateDTO.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("WOAH")));
    }
    @Test
    @DisplayName("Forwards Certificate (case 1) - Success")
    void testForwardCertificateCaseOneSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(11));

        CertificateForwardAuthorityDTO forwardAuthorityDTO = new CertificateForwardAuthorityDTO();
        AuthorityDTO savedAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward","post@forward.com"));
        forwardAuthorityDTO.setPostAuthority(savedAuthorityDTO);
        Set<AuthorityDTO> preAuthoritySet = new HashSet<>(1);
        preAuthoritySet.add(savedAuthorityDTO);
        forwardAuthorityDTO.setPreAuthorityList(preAuthoritySet);
        forwardAuthorityDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        String forwardAuthorityAsJSON = mapper.writeValueAsString(forwardAuthorityDTO);

        mockMvc.perform(post("/certificate/{certificateId}/forward", savedCertificateDTO.getId())
                        .content(forwardAuthorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.FORWARDED);
    }
    @Test
    @DisplayName("Forwards Certificate (case 2) - Success")
    void testForwardCertificateCaseTwoSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(11));

        CertificateForwardAuthorityDTO forwardAuthorityDTO = new CertificateForwardAuthorityDTO();
        AuthorityDTO savedPostAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward","post@forward.com"));
        forwardAuthorityDTO.setPostAuthority(savedPostAuthorityDTO);
        AuthorityDTO savedPreAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PreForward","pre@forward.com"));
        Set<AuthorityDTO> preAuthoritySet = new HashSet<>(1);
        preAuthoritySet.add(savedPreAuthorityDTO);

        CertificateDTO preCertificateDTO = certificates.get(7);
        preCertificateDTO.setParentCertificate(savedCertificateDTO);
        preCertificateDTO.setStatus(CertificateStatus.PRE_CERTIFICATE_DRAFT);
        preCertificateDTO.setForwardAuthority(savedPreAuthorityDTO);
        certificateService.save(preCertificateDTO);

        forwardAuthorityDTO.setPreAuthorityList(preAuthoritySet);
        forwardAuthorityDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        String forwardAuthorityAsJSON = mapper.writeValueAsString(forwardAuthorityDTO);

        mockMvc.perform(post("/certificate/{certificateId}/forward", savedCertificateDTO.getId())
                        .content(forwardAuthorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.FORWARDED);
    }
    @Test
    @DisplayName("Forwards Certificate (case 3) - Success")
    void testForwardCertificateCaseThreeSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(11));

        CertificateForwardAuthorityDTO forwardAuthorityDTO = new CertificateForwardAuthorityDTO();
        AuthorityDTO savedPostAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward","post@forward.com"));
        forwardAuthorityDTO.setPostAuthority(savedPostAuthorityDTO);
        AuthorityDTO savedPreAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PreForward","pre@forward.com"));
        Set<AuthorityDTO> preAuthoritySet = new HashSet<>(1);
        preAuthoritySet.add(savedPreAuthorityDTO);


        forwardAuthorityDTO.setPreAuthorityList(preAuthoritySet);
        forwardAuthorityDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        String forwardAuthorityAsJSON = mapper.writeValueAsString(forwardAuthorityDTO);

        mockMvc.perform(post("/certificate/{certificateId}/forward", savedCertificateDTO.getId())
                        .content(forwardAuthorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.FORWARDED);
    }

    @Test
    @DisplayName("Authority to Authority Forward Certificate - Success")
    void testAuthorityForwardCertificateSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(16));

        CertificateForwardAuthorityDTO forwardAuthorityDTO = new CertificateForwardAuthorityDTO();
        AuthorityDTO savedAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward","post@forward.com"));
        forwardAuthorityDTO.setPostAuthority(savedAuthorityDTO);
        Set<AuthorityDTO> preAuthoritySet = new HashSet<>(1);
        preAuthoritySet.add(savedAuthorityDTO);
        forwardAuthorityDTO.setPreAuthorityList(preAuthoritySet);
        forwardAuthorityDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        ObjectMapper mapper = new ObjectMapper();
        String forwardAuthorityAsJSON = mapper.writeValueAsString(forwardAuthorityDTO);

        mockMvc.perform(post("/certificate/{certificateId}/forward", savedCertificateDTO.getId())
                        .content(forwardAuthorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.FORWARDED);

        UserDetailDTO authorityUser = loginAsAuthority();
        CertificateAuthorityToAuthorityForwardDTO forwardAuthorityToAuthorityDTO = new CertificateAuthorityToAuthorityForwardDTO();
        AuthorityDTO savedNewAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward2","post2@forward.com"));
        forwardAuthorityToAuthorityDTO.setAuthority(savedNewAuthorityDTO);
        forwardAuthorityToAuthorityDTO.setReason("TEST REASON");
        forwardAuthorityToAuthorityDTO.setResourceId(authorityUser.getPrimaryAuthority().getId());
        String forwardAuthorityToAuthorityAsJSON = mapper.writeValueAsString(forwardAuthorityToAuthorityDTO);

        mockMvc.perform(post("/certificate/{certificateId}/forwardAuthority", savedCertificateDTO.getId())
                        .content(forwardAuthorityToAuthorityAsJSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getStatus()).isEqualTo(CertificateStatus.FORWARDED);
        assertEquals(savedNewAuthorityDTO.getName(),certificateService.findById(savedCertificateDTO.getId()).getForwardAuthority().getName());
    }
    //Tests for file upload
    @Test
    @DisplayName("Creates Certificate With Certificate Document  - Success")
    void testCreateCertificateWithCertificateDocumentSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateFile = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"certificate.pdf");

        // Perform the request with both parts
        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/create")
                        .file(certificateDTOPart)
                        .file(certificateFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Creates Certificate With Additional Documents- Success")
    void testCreateCertificateWithCertificateAdditionalDocsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        //Initialize Additional Docs
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateAdditionalDocs1 =  certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc1.pdf");
        MockMultipartFile certificateAdditionalDocs2 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc2.pdf");

        // Perform the request with both parts
        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/create")
                        .file(certificateDTOPart)
                        .file(certificateAdditionalDocs1)
                        .file(certificateAdditionalDocs2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Creates Certificate With Pre-Certificate Document- Success")
    void testCreateCertificateWithPreCertificateDocSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        //Initialize Pre-certificate Doc
        AuthorityDTO savedPostAuthorityDTO = authorityService.save(baseTestData.initializeAuthorityDTO("PostForward","post@forward.com"));
        certificateTestData.addDocumentsToCertificate(certificateDTO,null,"preCertificateFilename",FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS,1,null, savedPostAuthorityDTO);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificatePreCertificateDoc =  certificateTestData.createDocument(FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS, certificateDTOJson,"preCertificateDoc.pdf");

        // Perform the request with both parts
        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/create")
                        .file(certificateDTOPart)
                        .file(certificatePreCertificateDoc)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Creates Certificate With Additional Documents and Certificate Doc- Success")
    void testCreateCertificateWithCertificateAdditionalDocsAndCertificateDocSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateDocument = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"Certificate.pdf");
        MockMultipartFile certificateAdditionalDocs1 =  certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc1.pdf");
        MockMultipartFile certificateAdditionalDocs2 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc2.pdf");

        // Perform the request with both parts
        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/create")
                        .file(certificateDTOPart)
                        .file(certificateDocument)
                        .file(certificateAdditionalDocs1)
                        .file(certificateAdditionalDocs2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Updates Certificate With Certificate Document- Success")
    void testUpdateCertificateWithCertificateDocSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        CertificateDTO certificateDTO = certificates.get(1);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);
        savedCertificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateTestData.addDocumentsToCertificate(savedCertificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        savedCertificateDTO.setCompanyNumber("EditedCompanyNumber");
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(savedCertificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateDocument = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"certificateDoc.pdf");

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(certificateDTOPart)
                        .file(certificateDocument)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getCompanyNumber()).isEqualTo("EditedCompanyNumber");

        Page<DocumentDTO> page = fileService.getDocumentsByType(savedCertificateDTO.getId(), FileType.CERTIFICATE, false);
        assertNotNull(page, "Page should not be null");
        assertEquals(1, page.getContent().size(), "Expected 1 document in the page");
    }

    @Test
    @DisplayName("Updates Certificate With Certificate Additional Docs- Success")
    void testUpdateCertificateWithCertificateAdditionalDocsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        CertificateDTO certificateDTO = certificates.get(1);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);
        savedCertificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateTestData.addDocumentsToCertificate(savedCertificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,3,null, null);
        certificateTestData.addDocumentsToCertificate(savedCertificateDTO,null,null,FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        savedCertificateDTO.setCompanyNumber("EditedCompanyNumber");
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(savedCertificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificateDocument1 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc1.pdf");
        MockMultipartFile certificateDocument2 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc2.pdf");
        MockMultipartFile certificateDocument3 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc3.pdf");

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(certificateDTOPart)
                        .file(certificateDocument1)
                        .file(certificateDocument2)
                        .file(certificateDocument3)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getCompanyNumber()).isEqualTo("EditedCompanyNumber");

        Page<DocumentDTO> page = fileService.getDocumentsByType(savedCertificateDTO.getId(), FileType.ADDITIONAL_DOCUMENT, false);
        assertNotNull(page, "Page should not be null");
        assertEquals(3, page.getContent().size(), "Expected 3 document in the page");
    }

    @Test
    @DisplayName("Updates Certificate With Certificate Additional Docs And CertificateDocument- Success")
    void testUpdateCertificateWithCertificateAdditionalAndCertificateDocsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        CertificateDTO certificateDTO = certificates.get(1);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);
        savedCertificateDTO.setResourceId(companyUser.getPrimaryCompany().getId());
        certificateTestData.addDocumentsToCertificate(savedCertificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,3,null, null);
        certificateTestData.addDocumentsToCertificate(savedCertificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        savedCertificateDTO.setCompanyNumber("EditedCompanyNumber");
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(savedCertificateDTO);
        MockMultipartFile certificateDTOPart = certificateTestData.createDocument(null, certificateDTOJson,null);
        MockMultipartFile certificate = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"document.pdf");
        MockMultipartFile certificateDocument1 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson, "additionalDoc1.pdf");
        MockMultipartFile certificateDocument2 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc2.pdf");
        MockMultipartFile certificateDocument3 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"additionalDoc3.pdf");

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(certificateDTOPart)
                        .file(certificate)
                        .file(certificateDocument1)
                        .file(certificateDocument2)
                        .file(certificateDocument3)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(certificateService.findById(savedCertificateDTO.getId()).getCompanyNumber()).isEqualTo("EditedCompanyNumber");
        //Assertions For additional Docs
        Page<DocumentDTO> additionalDocsPage = fileService.getDocumentsByType(savedCertificateDTO.getId(), FileType.ADDITIONAL_DOCUMENT, false);
        assertNotNull(additionalDocsPage, "Page should not be null");
        assertEquals(3, additionalDocsPage.getContent().size(), "Expected 3 document in the page");

        //Assertions for certificate
        Page<DocumentDTO> certificatepage = fileService.getDocumentsByType(savedCertificateDTO.getId(), FileType.CERTIFICATE, false);
        assertNotNull(certificatepage, "Page should not be null");
        assertEquals(1, certificatepage.getContent().size(), "Expected 1 document in the page");
    }


    @Test
    @DisplayName("Overwrite Certificate Document")
    void testUpdateCertificateOverwriteSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //save a new certificate with a document
        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        certificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"oldFile.pdf"));
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        savedCertificate.setResourceId(companyUser.getPrimaryCompany().getId());

        Page<DocumentDTO> file = fileService.getDocumentsByType(savedCertificate.getId(),FileType.CERTIFICATE, false);
        List<String> documentVersionId = new ArrayList<>();
        documentVersionId.add(file.getContent().get(0).getId());
        //Update the saved certificate and overwrite the document
        certificateTestData.addDocumentsToCertificate(savedCertificate,"certificateOverwrittenNotes", "certificateOverwrittenFilename", FileUploadType.CERTIFICATE_DOCUMENT,0,documentVersionId, null);
        String savedCertificateDTOJson = mapper.writeValueAsString(savedCertificate);
        MockMultipartFile savedCertificateDTOPart = certificateTestData.createDocument(null, savedCertificateDTOJson,null);
        MockMultipartFile savedCertificateDoc = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, savedCertificateDTOJson,"newFile.pdf");

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(savedCertificateDTOPart)
                        .file(savedCertificateDoc)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        //Assertions For additional Docs
        Page<DocumentDTO> additionalDocsPage = fileService.getDocumentsByType(savedCertificate.getId(), FileType.CERTIFICATE, false);
        List<DocumentDTO> foundDoc = additionalDocsPage.get().collect(Collectors.toList());
        assertEquals("certificateOverwrittenFilename" , foundDoc.get(0).getEditedFilename());
        assertNotNull(additionalDocsPage, "Page should not be null");
        assertEquals(1, additionalDocsPage.getContent().size(), "Expected 1 document in the page");
    }


    @Test
    @DisplayName("Overwrite Certificate Additional Documents")
    void testUpdateCertificateAdditionalDocsOverwriteSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //save a new certificate with a document and additional Docs
        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        certificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"oldFile.pdf"));
        List<MultipartFile> additionalDocsList = new ArrayList<>();
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc1.pdf"));
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc2.pdf"));
        certificateDocumentsDTO.setAdditionalDocs(additionalDocsList);
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        savedCertificate.setResourceId(companyUser.getPrimaryCompany().getId());

        //Fetch the saved Docs IDs
        Page<DocumentDTO> certicatePage = fileService.getDocumentsByType(savedCertificate.getId(),FileType.CERTIFICATE, false);
        List<String> documentVersionId = new ArrayList<>();
        documentVersionId.add(certicatePage.getContent().get(0).getId());

        Page<DocumentDTO> additionalDocsPage = fileService.getDocumentsByType(savedCertificate.getId(),FileType.ADDITIONAL_DOCUMENT, false);
        List<String> additionalVersionId = new ArrayList<>();
        additionalVersionId.add(additionalDocsPage.getContent().get(0).getId());
        additionalVersionId.add(additionalDocsPage.getContent().get(1).getId());


        //Update the saved certificate and overwrite the documents
        certificateTestData.addDocumentsToCertificate(savedCertificate,"certificateOverwrittenNotes", "certificateOverwrittenFilename", FileUploadType.CERTIFICATE_DOCUMENT,0,documentVersionId, null);
        certificateTestData.addDocumentsToCertificate(savedCertificate,"certificateAdditionalNotesOverwritten","certificateAdditionalFilenameOverwritten",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,additionalVersionId, null);
        String savedCertificateDTOJson = mapper.writeValueAsString(savedCertificate);
        MockMultipartFile savedCertificateDTOPart = certificateTestData.createDocument(null, savedCertificateDTOJson,null);
        MockMultipartFile savedCertificateDoc = certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, savedCertificateDTOJson,"newFile.pdf");
        MockMultipartFile savedAdditional1 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, savedCertificateDTOJson,"newAdditionalDoc1.pdf");
        MockMultipartFile savedAdditional2 = certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, savedCertificateDTOJson,"newAdditionalDoc2.pdf");

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(savedCertificateDTOPart)
                        .file(savedCertificateDoc)
                        .file(savedAdditional1)
                        .file(savedAdditional2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("id", additionalVersionId.get(0),additionalVersionId.get(1))
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        //Assertions For Certificate
        Page<DocumentDTO> finalCertificatePage = fileService.getDocumentsByType(savedCertificate.getId(), FileType.CERTIFICATE, false);
        List<DocumentDTO> foundDoc = finalCertificatePage.get().collect(Collectors.toList());
        assertEquals("certificateOverwrittenFilename" , foundDoc.get(0).getEditedFilename());
        assertNotNull(additionalDocsPage, "Page should not be null");
        assertEquals(1, finalCertificatePage.getContent().size(), "Expected 1 document in the page");

        //Assertions For AdditionalDocs
        Page<DocumentDTO> finalCertificateAdditionalPage = fileService.getDocumentsByType(savedCertificate.getId(), FileType.ADDITIONAL_DOCUMENT, false);
        assertEquals("certificateAdditionalFilenameOverwritten0" , finalCertificateAdditionalPage.getContent().get(0).getEditedFilename());
        assertEquals("certificateAdditionalFilenameOverwritten1" , finalCertificateAdditionalPage.getContent().get(1).getEditedFilename());
        assertNotNull(additionalDocsPage, "Page should not be null");
        assertEquals(2, finalCertificateAdditionalPage.getContent().size(), "Expected 2 document in the page");
    }


    @Test
    @DisplayName("Delete Certificate Additional Documents")
    void testDeleteCertificateAdditionalDocsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //save a new certificate with  additional Docs
        CertificateDTO certificateDTO = certificates.get(0);
        certificateDTO.setCompany(companyUser.getPrimaryCompany());
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        mapper.registerModule(new JavaTimeModule());
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        List<MultipartFile> additionalDocsList = new ArrayList<>();
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc1.pdf"));
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc2.pdf"));
        certificateDocumentsDTO.setAdditionalDocs(additionalDocsList);
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        savedCertificate.setResourceId(companyUser.getPrimaryCompany().getId());

        //Update the saved certificate and overwrite the documents
        certificateTestData.addDocumentsToCertificate(savedCertificate,null, null, FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String savedCertificateDTOJson = mapper.writeValueAsString(savedCertificate);
        MockMultipartFile savedCertificateDTOPart = certificateTestData.createDocument(null, savedCertificateDTOJson,null);

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/certificate/update")
                        .file(savedCertificateDTOPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
        //Assertions For AdditionalDocs
        Page<DocumentDTO> finalCertificateAdditionalPage = fileService.getDocumentsByType(savedCertificate.getId(), FileType.ADDITIONAL_DOCUMENT, false);
        assertNull( finalCertificateAdditionalPage, "Expected 0 document in the page");
    }

    @Test
    @DisplayName("Gets a Certificate's signing employee - Success")
    void testGetCertificateSigningEmployeeByIdSuccess() throws Exception {
        String resourceName = loginAsAdmin();

        CertificateDTO certificateDTO = certificates.get(12);
        UserDetailDTO companyUserDTO = new UserDetailDTO();
        companyUserDTO.setUsername("company_user");
        companyUserDTO.setEmail("company_user@mail.com");
        OptionDTO userType = new OptionDTO();
        userType.setId("COMPANY_USER");
        companyUserDTO.setUserType(userType);
        companyUserDTO.setActive(true);
        CompanyDTO savedCompanyDTOExisting = companyService.save(baseTestData.initializeCompanyDTO("TestForDeactivation","extra2@authority.com", savedAuthorityDTO));
        companyUserDTO.setPrimaryCompany(savedCompanyDTOExisting);
        resourceService.createResource(companyMapperInstance.companyDTOtoResourceDTO(savedCompanyDTOExisting));
        companyUserDTO.setRole(companyTestData.getCompanyMainUserRole());
        UserDetailDTO savedUserDetailDTO = userDetailService.saveUser(companyUserDTO);
        certificateDTO.setSigningEmployee(savedUserDetailDTO);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate/signing_employee/{certificateId}", savedCertificateDTO.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedUserDetailDTO.getId())));
    }
    @Test
    @DisplayName("Gets a Certificate's signing employee - Failure")
    void testGetCertificateSigningEmployeeByIdFailure() throws Exception {
        String resourceName = loginAsAdmin();
        CertificateDTO savedCertificateDTO = certificateService.save(certificates.get(12));

        mockMvc.perform(get("/certificate/signing_employee/{certificateId}", savedCertificateDTO.getId())
                        .param("selectionFromDD", resourceName)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("Gets forwarded certificates for Authority - Success")
    void testGetForwardedCertificatesSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        CertificateDTO certificateDTO = certificates.get(12);
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateDTO.setForwardAuthority(authorityUser.getPrimaryAuthority());
        certificateDTO.setCompletedForward(true);
        CertificateDTO savedCertificateDTO = certificateService.save(certificateDTO);

        mockMvc.perform(get("/certificate/getForwardedCertificates")
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .param("userAuthorities", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", equalTo(savedCertificateDTO.getId())));
    }
}
