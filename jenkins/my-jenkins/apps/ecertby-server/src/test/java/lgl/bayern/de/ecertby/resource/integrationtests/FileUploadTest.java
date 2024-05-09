package lgl.bayern.de.ecertby.resource.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.model.util.FileUploadType;
import lgl.bayern.de.ecertby.resource.FileResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.CertificateTestData;
import lgl.bayern.de.ecertby.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FileUploadTest extends BaseTest {
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
    private FileResource fileResource;

    private List<CertificateDTO> certificates = new ArrayList<>();

    @NotNull
    private final CertificateTestData certificateTestData = new CertificateTestData();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;
    private AuthorityDTO savedAuthorityDTO;
    private CompanyDTO savedCompanyDTO;
    private TemplateDTO savedTemplateDTO;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        if (savedAuthorityDTO == null) {
            // to avoid transient entity errors, save to repository once
            savedAuthorityDTO = authorityService.save(certificateTestData.initializeAuthorityDTO("TestAuthority", "test@authority.com"));
        }
        if (savedCompanyDTO == null) {
            savedCompanyDTO = companyService.save(certificateTestData.initializeCompanyDTO("TestCompany", "test@company.com", savedAuthorityDTO));
        }
        if (savedTemplateDTO == null) {
            savedTemplateDTO = templateService.save(certificateTestData.initializeTemplateDTO("TestTemplate", 0));
        }
        certificates = certificateTestData.populateCertificates(savedAuthorityDTO, savedCompanyDTO, savedTemplateDTO);
    }

    @Test
    @DisplayName("Gets Certificate Document By CertificateId - Success")
    void testGetCertificateDocumentSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        certificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"certificate.pdf"));
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        loginAsCompany();
        mockMvc.perform(get("/files/findCertificateDoc/{id}", savedCertificate.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
    @Test
    @DisplayName("Gets PreCertificate Document By CertificateId - Success")
    void testGetCertificatePreCertificateDocumentSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String certificateDtoJSON = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        certificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDtoJSON,"certificate.pdf"));

        CertificateDTO preCertificateDto = certificates.get(1);
        certificateTestData.addDocumentsToCertificate(preCertificateDto,"preCertificateNotes","preCertificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String preCertificateDtoJSON = mapper.writeValueAsString(preCertificateDto);
        CertificateDocumentsDTO preCertificateDocumentsDTO = new CertificateDocumentsDTO();
        preCertificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, preCertificateDtoJSON,"precertificate.pdf"));
        preCertificateDto.setParentCertificate(certificateDTO);

        certificateUtilService.saveCertificate(certificateDTO, certificateDocumentsDTO);
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(preCertificateDto,preCertificateDocumentsDTO);

        loginAsCompany();
        mockMvc.perform(get("/files/findCertificatePreCertificateDocs/{id}/{isPreCertificate}", savedCertificate.getId(), "true")
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
    @Test
    @DisplayName("Gets Certificate Additional Documents By CertificateId - Success")
    void testGetCertificateAdditionalDocumentsSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateAdditionalNotes","certificateAdditionalFilename",FileUploadType.CERTIFICATE_ADDITIONAL_DOCS,2,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        List<MultipartFile> additionalDocsList = new ArrayList<>();
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc1.pdf"));
        additionalDocsList.add(certificateTestData.createDocument(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS, certificateDTOJson,"oldAdditionalDoc2.pdf"));
        certificateDocumentsDTO.setAdditionalDocs(additionalDocsList);
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        loginAsCompany();
        mockMvc.perform(get("/files/findCertificateAdditionalDocs/{id}/false", savedCertificate.getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
    @Test
    @DisplayName("Download File - Success")
    void testDownloadFileSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CertificateDTO certificateDTO = certificates.get(0);
        certificateTestData.addDocumentsToCertificate(certificateDTO,"certificateNotes","certificateFilename",FileUploadType.CERTIFICATE_DOCUMENT,0,null, null);
        String certificateDTOJson = mapper.writeValueAsString(certificateDTO);
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        certificateDocumentsDTO.setCertificateDoc(certificateTestData.createDocument(FileUploadType.CERTIFICATE_DOCUMENT, certificateDTOJson,"certificate.pdf"));
        CertificateDTO savedCertificate = certificateUtilService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        Page<DocumentDTO> document = fileService.getDocumentsByType(savedCertificate.getId(),FileType.CERTIFICATE, false);
        String id = document.getContent().get(0).getId();
        loginAsCompany();
        mockMvc.perform(get("/files/{id}/download", id)
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(certificateDocumentsDTO.getCertificateDoc().getBytes()));
    }
}
