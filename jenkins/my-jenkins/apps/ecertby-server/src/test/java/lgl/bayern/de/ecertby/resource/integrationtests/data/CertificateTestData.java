package lgl.bayern.de.ecertby.resource.integrationtests.data;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileUploadType;
import org.jetbrains.annotations.NotNull;
import org.springframework.mock.web.MockMultipartFile;

public class CertificateTestData extends BaseTestData {
    /**
     * Populates the company list with as many companies are needed for the tests, without saving them to the repository.<br>
     * Explicitly save to the repository inside a test (when needed) to avoid double-entry errors.
     */
    public List<CertificateDTO> populateCertificates(AuthorityDTO savedAuthorityDTO, CompanyDTO savedCompanyDTO, TemplateDTO savedTemplateDTO) {
        List<CertificateDTO> certificates = new ArrayList<>();
        int REQUIRED_CERTIFICATE_NUMBER = 17;
        for (int i = 0; i < REQUIRED_CERTIFICATE_NUMBER; i++) {
            CertificateDTO certificateDTO = initializeCertificateDTO(savedAuthorityDTO, savedCompanyDTO, savedTemplateDTO);
            certificates.add(certificateDTO);
        }
        return certificates;
    }

    /**
     * Creates a certificate.
     * @param savedAuthorityDTO the DTO of the persisted issuing authority.
     * @param savedCompanyDTO the DTO of the persisted creator's company.
     * @param savedTemplateDTO the DTO of the persisted base template.
     * @return the created certificate as a CertificateDTO.
     */
    @NotNull
    public CertificateDTO initializeCertificateDTO(AuthorityDTO savedAuthorityDTO, CompanyDTO savedCompanyDTO, TemplateDTO savedTemplateDTO) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);
        OptionDTO savedAuthorityDTOAsOption = initializeOption(savedAuthorityDTO.getName(), savedAuthorityDTO.getId(),true);

        CertificateDTO certificateDTO = new CertificateDTO();
        certificateDTO.setTemplate(savedTemplateDTO);
        certificateDTO.setCompany(savedCompanyDTO);
        certificateDTO.setStatus(CertificateStatus.DRAFT);
        certificateDTO.setShippingDate(Instant.now());
        certificateDTO.setCompanyNumber("Test");
        SortedSet<OptionDTO> preAuthoritySet = new TreeSet<>();
        preAuthoritySet.add(savedAuthorityDTOAsOption);
        certificateDTO.setPreAuthoritySet(preAuthoritySet);
        certificateDTO.setIssuingAuthority(savedAuthorityDTO);
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        departmentList.add(department);
        certificateDTO.setDepartmentSet(departmentList);
        certificateDTO.setAssignedTeamSet(new HashSet<>(0));
        certificateDTO.setKeywordSet(savedTemplateDTO.getKeyword());
        certificateDTO.setCertificateFile(new DocumentDTO());
        certificateDTO.setCertificateAdditionalFiles(new ArrayList<>());

        return certificateDTO;
    }
    /**
     * Adds documents to a CertificateDTO based on the specified parameters.
     *
     * @param certificateDTO  The CertificateDTO to which documents will be added.
     * @param notes           The notes for the documents.
     * @param editedFilename  The edited filename for the documents.
     * @param type            The type of documents to add (CERTIFICATE_DOCUMENT or CERTIFICATE_ADDITIONAL_DOCS).
     * @param numberOfDocs The number of additional documents to add.
     * @param versionId       The version ID for the documents.
     */
    public void addDocumentsToCertificate(CertificateDTO certificateDTO, String notes, String editedFilename, FileUploadType type, int numberOfDocs, List<String> versionId, AuthorityDTO authorityDTO) {
        if (type.equals(FileUploadType.CERTIFICATE_DOCUMENT)) {
            DocumentDTO document = new DocumentDTO(null, notes, editedFilename, null, null, null, null, null, null, null, null, null);
            if (versionId != null) {
                document.setId(versionId.get(0));
            }
            certificateDTO.setCertificateFile(document);
        } else if (type.equals(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS)) {
            List<DocumentDTO> additionalDocs = new ArrayList<>();
            for (int i = 0; i < numberOfDocs; i++) {
                DocumentDTO document = new DocumentDTO(null, notes + i, editedFilename + i, null, null, null, null, null, null, null, null, null);
                if (versionId != null) {
                    document.setId(versionId.get(i));
                }
                additionalDocs.add(document);
            }
            certificateDTO.setCertificateAdditionalFiles(additionalDocs);
        }  else if (type.equals(FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS)) {
            List<DocumentDTO> preCertificateDocs = new ArrayList<>();
            for (int i = 0; i < numberOfDocs; i++) {
                DocumentDTO document = new DocumentDTO(null, notes + i, editedFilename + i, null, null, null, null, null, null, null, null, null);
                if (versionId != null) {
                    document.setId(versionId.get(i));
                }
                document.setAuthority(initializeOption(authorityDTO.getName() , authorityDTO.getId(),true));
                preCertificateDocs.add(document);
            }
            certificateDTO.setCertificatePreCertificateFiles(preCertificateDocs);
        }
    }
    /**
     * Creates a MockMultipartFile for the specified type and content.
     *
     * @param type             The type of document to create (CERTIFICATE_DOCUMENT, CERTIFICATE_ADDITIONAL_DOCS, CERTIFICATE_PRE_CERTIFICATE_DOCS, or null).
     * @param certificateDTOJson  The JSON representation of the CertificateDTO
     * @param filename         The filename for the document.
     * @return A MockMultipartFile representing the document.
     */
    public MockMultipartFile createDocument(FileUploadType type, String certificateDTOJson,String filename) {
        if (type != null) {
            if (type.equals(FileUploadType.CERTIFICATE_DOCUMENT)) {
                return new MockMultipartFile(
                        FileUploadType.CERTIFICATE_DOCUMENT.getValue(),
                        filename,
                        "application/pdf",
                        "Certificate Content here".getBytes(StandardCharsets.UTF_8)
                );
            } else if (type.equals(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS)) {
                return new MockMultipartFile(
                        FileUploadType.CERTIFICATE_ADDITIONAL_DOCS.getValue(),
                        filename,
                        "application/pdf",
                        "Additional File content".getBytes(StandardCharsets.UTF_8)
                );
            }  else if (type.equals(FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS)) {
                return new MockMultipartFile(
                        FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS.getValue(),
                        filename,
                        "application/pdf",
                        "Pre-Certificate File content".getBytes(StandardCharsets.UTF_8)
                );
            }
        }
        return new MockMultipartFile(
                "certificateDTO",
                "",
                "application/json",
                certificateDTOJson.getBytes(StandardCharsets.UTF_8)
        );
    }
}

