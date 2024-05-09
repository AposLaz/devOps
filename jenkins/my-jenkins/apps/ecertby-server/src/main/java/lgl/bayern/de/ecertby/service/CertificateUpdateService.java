package lgl.bayern.de.ecertby.service;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CertificateDocumentsDTO;
import lgl.bayern.de.ecertby.dto.CertificateStatusHistoryDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.audit.AuditCertDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.CertificateMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.CertificateStatusHistory;
import lgl.bayern.de.ecertby.model.QCertificate;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.model.util.FileUploadType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.AuthorityRepository;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.repository.TaskRepository;
import lgl.bayern.de.ecertby.validator.CertificateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CertificateUpdateService extends BaseService<CertificateDTO, Certificate, QCertificate>{

    CertificateMapper certificateMapperInstance = Mappers.getMapper(CertificateMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);

    private final SecurityService securityService;

    private final AuditService auditService;

    private final FileService fileService;

    private final CertificateRepository certificateRepository;

    private final AuthorityRepository authorityRepository;

    private final TaskRepository taskRepository;

    private final CertificateValidator certificateValidator;


    /**
     * Saves a certificate and creates the associated certificate files, logging the action.
     * @param certificateDTO The certificate to be created.
     * @param certificateDocumentsDTO The object with certificate documents.
     */
    public CertificateDTO saveCertificate(CertificateDTO certificateDTO, CertificateDocumentsDTO certificateDocumentsDTO) {
        certificateDTO.setCreationDate(Instant.now());
        updateCertificateStatusHistory(certificateDTO);

        CertificateDTO savedCertificateDTO = save(certificateDTO);
        certificateDTO.setId(savedCertificateDTO.getId());
        // LOG CREATION
        auditService.saveAuditForEntity(AuditAction.CREATE, securityService.getLoggedInUserDetail(), savedCertificateDTO.getId(), certificateDTO.getResourceId());

        log.info(LOG_PREFIX + "Handle certificate documents...");
        String certificateMainFolderId = handleCertificateDocuments(certificateDTO, certificateDocumentsDTO, savedCertificateDTO, false, certificateDTO.getResourceId());

        log.info(LOG_PREFIX + "Handle pre-certificate documents...");
        handlePreCertificates(certificateDTO, certificateDocumentsDTO, certificateMainFolderId, savedCertificateDTO, false, certificateDTO.getResourceId());


        log.info(LOG_PREFIX + "Certificate with id {} successfully created by user with id : {}", savedCertificateDTO.getId(), securityService.getLoggedInUserDetailId());
        return savedCertificateDTO;
    }

    /**
     * Updates a certificate and the associated certificate files, logging the action.
     * @param certificateDTO The certificate to be created.
     * @param certificateDocumentsDTO The object with certificate's documents.
     * @param isPreCertificate The function called by pre-certificate or not.
     */
    public CertificateDTO editCertificate(CertificateDTO certificateDTO, CertificateDocumentsDTO certificateDocumentsDTO, boolean isPreCertificate, String resourceId) {
        CertificateDTO oldCertificateDTO = getExistingCertificateById(certificateDTO.getId());

        // If certificate has status deleted throw error
        List<EcertBYErrorException> errors = new ArrayList<>();
        certificateValidator.validateIsCertificateDeleted(oldCertificateDTO, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for saving certificate.", new EcertBYGeneralException(errors));
        }

        // Do not make any changes to oldCertDTOForAudit. Should be used only for auditing providing the old values.
        final CertificateDTO oldCertDTOForAudit = getExistingCertificateById(certificateDTO.getId());
        // Keep the old creation & forward date (else they get updated to null)
        certificateDTO.setCreationDate(oldCertificateDTO.getCreationDate());
        certificateDTO.setForwardDate(oldCertificateDTO.getForwardDate());
        // Same for reference certificate (in case there is one)
        certificateDTO.setReferenceCertificate(oldCertificateDTO.getReferenceCertificate());
        // Keep the connection for assignment history.
        if(!isPreCertificate){
            certificateDTO.setAssignmentHistory(oldCertificateDTO.getAssignmentHistory());
        } else {
            certificateDTO.setAssignmentHistory(null);
        }
        CertificateDTO savedCertificateDTO;

        if(isPreCertificate){
            oldCertificateDTO.setAssignedTeamSet(certificateDTO.getAssignedTeamSet());
            oldCertificateDTO.setAssignedEmployee(certificateDTO.getAssignedEmployee());
            oldCertificateDTO.setCompletedForward(oldCertificateDTO.getCompletedForward());
            savedCertificateDTO = certificateMapperInstance.map(certificateRepository.save(certificateMapperInstance.map(oldCertificateDTO)));
        } else {
            certificateDTO.setCompletedForward(oldCertificateDTO.getCompletedForward());
            savedCertificateDTO = certificateMapperInstance.map(certificateRepository.save(certificateMapperInstance.map(certificateDTO)));
        }

        // LOG UPDATE
        auditService.saveAuditForEntity(AuditAction.UPDATE, securityService.getLoggedInUserDetail(),
                certificateDTO.getId(), oldCertDTOForAudit, savedCertificateDTO, certificateDTO.getResourceId());

        log.info(LOG_PREFIX + "Handle certificate documents...");
        String certificateMainFolderId = handleCertificateDocuments(certificateDTO, certificateDocumentsDTO, savedCertificateDTO, isPreCertificate, resourceId);

        log.info(LOG_PREFIX + "Handle pre-certificate documents...");
        handlePreCertificates(certificateDTO, certificateDocumentsDTO, certificateMainFolderId, savedCertificateDTO, isPreCertificate, resourceId);


        log.info(LOG_PREFIX + "Certificate with id {} successfully updated by user with id : {}", savedCertificateDTO.getId(), securityService.getLoggedInUserDetailId());
        return savedCertificateDTO;
    }

    /**
     * Creates if exists any new pre-certificate documents.
     * @param newFiles The list with new documents' attributes.
     * @param certificateDTO The certificate dto object.
     * @param preCertificateDocuments The pre-certificate documents.
     * @param certificateMainFolderId The certificate folder id.
     * @param isPreCertificate The function called by pre-certificate or not.
     */
    private void createNewPreCertificateDoc(List<DocumentDTO> newFiles, CertificateDTO certificateDTO,
        List<MultipartFile> preCertificateDocuments, String certificateMainFolderId, boolean isPreCertificate, String resourceId){
        if(!newFiles.isEmpty() && preCertificateDocuments != null) {
            for (int j = 0; j < preCertificateDocuments.size(); j++) {
                // In case update certificate performed by a pre-authority.
                String preCertificateId =  certificateDTO.getId();
                // Pre-certificate does not exist. Create pre-certificate.
                if(!isPreCertificate && newFiles.get(j).getPreCertificateId() == null){
                    CertificateDTO newPreCertificate = createPreCertificate(newFiles.get(j).getAuthority().getId(),certificateDTO, CertificateStatus.PRE_CERTIFICATE_DRAFT);
                    log.info(LOG_PREFIX + "Pre-certificate with id : {} created by user with id : {}", preCertificateId, securityService.getLoggedInUserDetailId());
                    preCertificateId = newPreCertificate.getId();
                }
                // If pre-certificate id exists then create the file under the sub-folder for this pre-certificate.
                else if (!isPreCertificate && newFiles.get(j).getPreCertificateId() != null){
                    preCertificateId = newFiles.get(j).getPreCertificateId();
                }

                // Create the subfolder for the new pre-certificate.
                String certificateSubfolder = fileService.getFolderId(preCertificateId, certificateMainFolderId);
                // Create the documents under the pre-certificate folder.
                handleDocument(certificateSubfolder, null, newFiles.get(j).getNotes(),
                        newFiles.get(j).getEditedFilename(), preCertificateDocuments.get(j),
                        preCertificateId, FileType.PRE_CERTIFICATE,
                        "certificate_pre_certificate_file", certificateDTO.getId(),
                        resourceId);
            }
        }
    }

    /**
     * Handles the re-uploaded and new pre-certificate documents.
     * @param preCertificateDocumentsDTO The pre-certificate documents' attributes.
     * @param newFiles The list with new pre-certificate documents.
     * @param certificatePreCertificateFile The pre-certificate document.
     * @return The pre-certificate document if found.
     */
    private MultipartFile handleReUploadAndNewPreCertificate(CertificateDocumentsDTO preCertificateDocumentsDTO, List<DocumentDTO> newFiles, DocumentDTO certificatePreCertificateFile){
        // Find the file id.
        String fileDocId = certificatePreCertificateFile.getId();
        MultipartFile preCertificateDoc = null;
        // If the file re-uploaded or any attribute related to that document updated.
        if(certificatePreCertificateFile.getId() != null && preCertificateDocumentsDTO.getPreCertificateDocMap().containsKey(fileDocId)){
            preCertificateDoc = preCertificateDocumentsDTO.getPreCertificateDocMap().get(fileDocId);
        }
        // If no file id exists keep the document dto object to create the new document.
        else if(fileDocId == null){
            newFiles.add(certificatePreCertificateFile);
        }

        return preCertificateDoc;
    }

    /**
     * Handles the certificate's documents.
     * @param certificateDTO The certificate dto.
     * @param certificateDocumentsDTO The certificate document dto.
     * @param savedCertificateDTO The saved certificate dto.
     * @param isPreCertificate The function called by pre-certificate or not.
     * @return The parent folder id.
     */
    private String handleCertificateDocuments(CertificateDTO certificateDTO, CertificateDocumentsDTO certificateDocumentsDTO, CertificateDTO savedCertificateDTO, boolean isPreCertificate, String resourceId){
        String certificateMainFolderId = null;
        if(isPreCertificate){
            certificateMainFolderId = fileService.getFolderId(savedCertificateDTO.getParentCertificate().getId(), null);
        }

        String currentFolderId = fileService.getFolderId(savedCertificateDTO.getId(), certificateMainFolderId);
        // Certificate documents.
        handleDocument(currentFolderId, certificateDTO.getCertificateFile().getId(), certificateDTO.getCertificateFile().getNotes(),
                certificateDTO.getCertificateFile().getEditedFilename(), certificateDocumentsDTO.getCertificateDoc()
                ,savedCertificateDTO.getId(), FileType.CERTIFICATE, "certificate_certificate_file", savedCertificateDTO.getId(), resourceId);
        // Additional documents.
        List<DocumentDTO> additionalDocuments = certificateDTO.getCertificateAdditionalFiles();
        // Remove additional documents which has no pre-certificate id if on edit of pre-certificate form.
        if(isPreCertificate){
            additionalDocuments.removeIf(document -> Objects.isNull(document.getPreCertificateId()) && Objects.nonNull(document.getId()));
        }
        handleDocumentsByType(savedCertificateDTO.getId(), certificateDTO, certificateDocumentsDTO.getAdditionalDocs(),
                certificateDocumentsDTO.getAdditionalDocMap(), currentFolderId, certificateMainFolderId, FileType.ADDITIONAL_DOCUMENT, additionalDocuments, resourceId);

        if(!isPreCertificate){
            // Supplementary certificate documents.
            handleDocumentsByType(savedCertificateDTO.getId(), certificateDTO, certificateDocumentsDTO.getSupplementaryCertificateDocs(),
                    certificateDocumentsDTO.getSupplementaryCertificateDocMap(), currentFolderId, certificateMainFolderId, FileType.SUPPLEMENTARY_CERTIFICATE,
                certificateDTO.getCertificateSupplementaryFiles(), resourceId);
            // External pre-certificate documents.
            handleDocumentsByType(savedCertificateDTO.getId(), certificateDTO, certificateDocumentsDTO.getExternalPreCertificateDocs(),
                    certificateDocumentsDTO.getExternalPreCertificateDocMap(), currentFolderId, certificateMainFolderId, FileType.EXTERNAL_PRE_CERTIFICATE,
                certificateDTO.getCertificateExternalPreCertificateFiles(), resourceId);
        }

        // If certificate main folder is null then the certificate is new.
        if(certificateMainFolderId == null){
            certificateMainFolderId = currentFolderId;
        }
        return certificateMainFolderId;
    }

    /**
     * Handles operations related to documents associated with the certificate.
     * @param certificateId       The Certificate id.
     * @param certificateDTO      The CertificateDTO containing certificate information.
     * @param documents      List of MultipartFile objects.
     * @param docMap    A mapping of document IDs to corresponding MultipartFile objects.
     * @param currentFolderId     The current folder id.
     * @param certificateMainFolderId The certificate main folder id.
     * @param fileType The file type to check.
     * @param filesToCheck List of Document dto objects.
     */
    private void handleDocumentsByType(String certificateId, CertificateDTO certificateDTO, List<MultipartFile> documents,
                                       Map<String, MultipartFile> docMap, String currentFolderId, String certificateMainFolderId,
        FileType fileType, List<DocumentDTO> filesToCheck, String resourceId) {
        fileService.checkDiffBetweenDocs(certificateId,filesToCheck, certificateMainFolderId, fileType, resourceId);
        List<DocumentDTO> newFiles = new ArrayList<>();

        for (int i = 0; i < filesToCheck.size(); i++) {
            String fileDocId = filesToCheck.get(i).getId();
            MultipartFile document = null;
            if(filesToCheck.get(i).getId() != null && docMap.containsKey(fileDocId)){
                document = docMap.get(fileDocId);
            } else if(filesToCheck.get(i).getId() == null){
                newFiles.add(filesToCheck.get(i));
            }
            handleDocument(currentFolderId, fileDocId, filesToCheck.get(i).getNotes(),
                    filesToCheck.get(i).getEditedFilename(), document, certificateId, fileType,
                    "certificate_file", certificateDTO.getId(), resourceId);
        }
        if(!newFiles.isEmpty()) {
            for (int i = 0; i < documents.size(); i++) {
                handleDocument(currentFolderId, null, newFiles.get(i).getNotes(),
                        newFiles.get(i).getEditedFilename(), documents.get(i), certificateId, fileType,
                        "certificate_file", certificateDTO.getId(), resourceId);
            }
        }

    }

    /**
     * Handles a document.
     * Creates or updates a document and its attributes.
     * @param folderId The folder id.
     * @param fileId The file id.
     * @param notes The notes. It is an attribute.
     * @param editedFileName The edited file name. It is an attribute.
     * @param document The document.
     * @param certificateId The certificate id.
     * @param fileType The file type. Enum FileType
     * @param auditPrefix The prefix of the auditing that declares the type of object that is audited (e.g.: pre-certificate).
     * @param certificateMainFolderId The certificate main folder id.
     */
    private void handleDocument(String folderId, String fileId, String notes, String editedFileName,
                                MultipartFile document, String certificateId, FileType fileType, String auditPrefix, String certificateMainFolderId, String resourceId){
        fileService.handleDocument(folderId, fileId, notes, editedFileName, document, certificateId, fileType, null, auditPrefix, certificateMainFolderId, resourceId);
    }

    /**
     *  Handles the pre-certificates
     * @param certificateDTO The certificate object.
     * @param certificateDocumentsDTO The certificate documents object.
     * @param certificateMainFolderId The main folder id of certificate.
     * @param savedCertificateDTO The saved certificate object.
     * @param isPreCertificate The function called by pre-certificate or not.
     */
    private void handlePreCertificates(CertificateDTO certificateDTO, CertificateDocumentsDTO certificateDocumentsDTO,
        String certificateMainFolderId, CertificateDTO savedCertificateDTO, boolean isPreCertificate, String resourceId){
        if(isPreCertificate){
            fileService.deleteNodeByPreCertificateIdAndAttribute(certificateDTO.getId(), FileType.PRE_CERTIFICATE.toString(),
                savedCertificateDTO.getParentCertificate().getId(), certificateDTO.getCertificatePreCertificateFiles(), resourceId);
        } else {
            // Post-authority user cannot do any action on pre-certificates.
            UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
            if(userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())){
                return;
            }
            // Delete the pre-certificates removed.
            deletePreCertificates(certificateDTO, resourceId);
        }


        List<DocumentDTO> newFiles = new ArrayList<>();

        // For each pre-certificate document.
        for (int j = 0; j < certificateDTO.getCertificatePreCertificateFiles().size(); j++) {
            // Find the file id.
            String fileDocId = certificateDTO.getCertificatePreCertificateFiles().get(j).getId();

            // Find the pre certificate doc.
            MultipartFile preCertificateDoc = handleReUploadAndNewPreCertificate(certificateDocumentsDTO, newFiles, certificateDTO.getCertificatePreCertificateFiles().get(j));

            // If the file should be updated.
            updatePreCertificateFile(fileDocId, certificateMainFolderId, certificateDTO.getCertificatePreCertificateFiles().get(j),
                preCertificateDoc, certificateDTO, savedCertificateDTO, isPreCertificate, resourceId);

        }
        // Check if a new pre-certificate added.
        createNewPreCertificateDoc(newFiles, certificateDTO, certificateDocumentsDTO.getPreCertificateDocs(), certificateMainFolderId, isPreCertificate, resourceId);
    }

    /**
     * Updates the pre-certificate file
     * @param fileDocId The file id.
     * @param certificateMainFolderId The certificate main folder id.
     * @param documentDTO The document object.
     * @param preCertificateDoc The pre-certificate document.
     * @param certificateDTO The certificate object.
     * @param savedCertificateDTO The saved certificate object.
     * @param isPreCertificate The function called by pre-certificate or not.
     */
    private void updatePreCertificateFile(String fileDocId, String certificateMainFolderId, DocumentDTO documentDTO, MultipartFile preCertificateDoc,
                                          CertificateDTO certificateDTO, CertificateDTO savedCertificateDTO, Boolean isPreCertificate, String resourceId){
        if(fileDocId == null) {
            // Update the pre-certificate entity if no document exists.
            if(!isPreCertificate && documentDTO.getPreCertificateId() != null){
                updatePreCertificate(documentDTO.getPreCertificateId(), documentDTO);
            }
            return;
        }
        // Find the pre-certificate id.
        String childrenCertificateId = fileService.findCertificateIdByFileId(fileDocId);

        // Find pre-certificate's subfolder id.
        String certificateSubfolder = fileService.getFolderId(childrenCertificateId, certificateMainFolderId);

        // An Object pair that will be used only for auditing.
        Pair<AuditCertDTO, AuditCertDTO> auditPair = getInitialPreCertificatePair(certificateDTO, childrenCertificateId);

        // Update the pre-certificate entity and the pre-certificate document.
        if(!isPreCertificate){
            updatePreCertificate(childrenCertificateId, documentDTO);
        }

        // set the updated values to the "new" object in the pair.
        auditPair.getRight().setAuthority(getExistingCertificateById(childrenCertificateId).getForwardAuthority().getName());

        fileService.handleDocument(certificateSubfolder, fileDocId, documentDTO.getNotes(),
                documentDTO.getEditedFilename(), preCertificateDoc, childrenCertificateId, FileType.PRE_CERTIFICATE,
                auditPair, "certificate_pre_certificate_file", savedCertificateDTO.getId(), resourceId);
        log.info(LOG_PREFIX + "Pre-certificate files updated by user with id : {}", securityService.getLoggedInUserDetailId());
    }

    /**
     * Deletes pre-certificate entity and subfolder of it.
     * @param certificateDTO The certificate object.
     */
    private void deletePreCertificates(CertificateDTO certificateDTO, String resourceId){
        log.info(LOG_PREFIX + "Delete pre-certificate...");
        List<String> preCertificatesToDelete = new ArrayList<>();
        fileService.checkDiffBetweenPreCertificateDocs(certificateDTO.getId(),certificateDTO.getCertificatePreCertificateFiles(), preCertificatesToDelete, resourceId);

        // Check if there are pre-certificates with no documents
        findPreCertificateToBeRemovedWithNoDocument(certificateDTO,certificateDTO.getCertificatePreCertificateFiles(), preCertificatesToDelete);

        if(!preCertificatesToDelete.isEmpty()){
            preCertificatesToDelete.forEach(preCertificateId -> {
                // tasks have certificate foreign key
                // (all other certificate deletion scenarios happen on draft certs, therefore without tasks)
                taskRepository.deleteAllByCertificateId(preCertificateId);
                certificateRepository.deleteById(preCertificateId);
                log.info(LOG_PREFIX + "Pre-certificate deleted by user with id : {}", securityService.getLoggedInUserDetailId());
            });
        }
    }

    /**
     * Checks if there are removed pre-certificates with no document.
     * @param certificateId The certificate id.
     * @param documentsToBeSaved The documents send from the request.
     * @param preCertificatesIdsToBeDeleted The list with pre-certificate ids to be deleted.
     */
    private void findPreCertificateToBeRemovedWithNoDocument(CertificateDTO certificateDTO ,List<DocumentDTO> documentsToBeSaved, List<String> preCertificatesIdsToBeDeleted){
        log.info(LOG_PREFIX + "Delete pre-certificate with no document...");
        List<String> preCertificatesForCertificate = certificateRepository.findIdByParentCertificateId(certificateDTO.getId());
        List<String> documentsToBeSavedIds = documentsToBeSaved != null ? documentsToBeSaved.stream().map(DocumentDTO::getPreCertificateId).filter(Objects::nonNull).collect(Collectors.toList()) : new ArrayList<>();
        // Find nonCommonIds with no documents
        List<String> nonCommonPreCertificateIds = Stream.concat(preCertificatesForCertificate.stream().filter(id -> !documentsToBeSavedIds.contains(id)),
                        documentsToBeSavedIds.stream().filter(id -> !preCertificatesForCertificate.contains(id)))
                .collect(Collectors.toList());

        for (String id : nonCommonPreCertificateIds) {
            preCertificatesIdsToBeDeleted.add(id);
            auditService.saveFileUploadDeleteAudit(AuditAction.DELETE,userMapperInstance.map(
                    securityService.getLoggedInUserDetailDTO()),id,certificateDTO.getId(), "certificate_pre_certificate_deleted", certificateDTO.getResourceId());
            log.info(LOG_PREFIX + "Pre-certificate with no document deleted by user with id : {}", securityService.getLoggedInUserDetailId());
        }
    }

    /**
     * Updates pre-certificate.
     * @param preCertificateId The pre-certificate id.
     * @param preCertificateFileDTO The pre-certificate document.
     * @return The updated pre-certificate.
     */
    private CertificateDTO updatePreCertificate(String preCertificateId, DocumentDTO preCertificateFileDTO){
        log.info(LOG_PREFIX + "Update pre-certificate...");
        Authority authority = authorityRepository.findById(preCertificateFileDTO.getAuthority().getId()).orElse(null);
        Certificate preCertificate = certificateRepository.findById(preCertificateId).orElse(null);
        // In case that pre-authority changed fields teams and assigned employee set to null.
        if(!preCertificate.getForwardAuthority().equals(authority)){
            preCertificate.setAssignedEmployee(null);
            preCertificate.getAssignedTeamSet().clear();
        }
        preCertificate.setForwardAuthority(authority);
        preCertificate.setIssuingAuthority(authority);
        log.info(LOG_PREFIX + "Pre-certificate updated by user with id : {}", securityService.getLoggedInUserDetailId());
        return certificateMapperInstance.map(certificateRepository.save(preCertificate));
    }


    /**
     * Creates the pre-certificate.
     * @param authorityId The authority id.
     * @param certificateDTO The certificate object.
     * @param certificateStatus The certificate status.
     * @return The saved pre-certificate.
     */
    public CertificateDTO createPreCertificate(String authorityId, CertificateDTO certificateDTO, CertificateStatus certificateStatus){
        log.info(LOG_PREFIX + "Create pre-certificate...");
        CertificateDTO preCertificateDTO = new CertificateDTO();
        BeanUtils.copyProperties(certificateDTO, preCertificateDTO, "statusHistorySet");
        preCertificateDTO.setId(null);
        preCertificateDTO.setPreAuthoritySet(null);
        preCertificateDTO.setIssuingAuthority(null);
        preCertificateDTO.setParentCertificate(certificateDTO);
        preCertificateDTO.setStatus(certificateStatus);
        updateCertificateStatusHistory(preCertificateDTO);
        preCertificateDTO.setAssignmentHistory(null);
        AuthorityDTO authorityDTO = authorityMapperInstance.map(authorityRepository.findById(authorityId).orElse(null));
        preCertificateDTO.setForwardAuthority(authorityDTO);
        preCertificateDTO.setCreationDate(Instant.now());
        return save(preCertificateDTO);
    }

    /**
     * Updates the certificate's documents.
     * @param request The documents.
     * @param certificateId The certificate id.
     * @return The object with the certificate's documents.
     */
    public CertificateDocumentsDTO updateCertificateDocuments(MultipartHttpServletRequest request, String certificateId){
        MultiValueMap<String, MultipartFile> multiFileMap = request.getMultiFileMap();
        CertificateDocumentsDTO certificateDocumentsDTO = new CertificateDocumentsDTO();
        if(multiFileMap.get(FileUploadType.CERTIFICATE_DOCUMENT.getValue()) != null ) {
            if (multiFileMap.get(FileUploadType.CERTIFICATE_DOCUMENT.getValue()).get(0) != null) {
                certificateDocumentsDTO.setCertificateDoc(multiFileMap.get(FileUploadType.CERTIFICATE_DOCUMENT.getValue()).get(0));
            }
        }
        if(multiFileMap.get(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS.getValue()) != null) {
            certificateDocumentsDTO.setAdditionalDocs(multiFileMap.get(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS.getValue()));
        }
        if(multiFileMap.get(FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS.getValue()) != null) {
            certificateDocumentsDTO.setPreCertificateDocs(multiFileMap.get(FileUploadType.CERTIFICATE_PRE_CERTIFICATE_DOCS.getValue()));
        }
        if(multiFileMap.get(FileUploadType.CERTIFICATE_SUPPLEMENTARY_DOCS.getValue()) != null) {
            certificateDocumentsDTO.setSupplementaryCertificateDocs(multiFileMap.get(FileUploadType.CERTIFICATE_SUPPLEMENTARY_DOCS.getValue()));
        }
        if(multiFileMap.get(FileUploadType.CERTIFICATE_EXTERNAL_PRE_CERTIFICATE_DOCS.getValue()) != null) {
            certificateDocumentsDTO.setExternalPreCertificateDocs(multiFileMap.get(FileUploadType.CERTIFICATE_EXTERNAL_PRE_CERTIFICATE_DOCS.getValue()));
        }
        if(certificateId != null){
            certificateDocumentsDTO.setAdditionalDocMap(createIdToMultipartFileMap(certificateDocumentsDTO.getAdditionalDocs(), request, "id"));
            certificateDocumentsDTO.setPreCertificateDocMap(createIdToMultipartFileMap(certificateDocumentsDTO.getPreCertificateDocs(), request, "pre_id"));
            certificateDocumentsDTO.setSupplementaryCertificateDocMap(createIdToMultipartFileMap(certificateDocumentsDTO.getSupplementaryCertificateDocs(), request, "suppl_id"));
            certificateDocumentsDTO.setExternalPreCertificateDocMap(createIdToMultipartFileMap(certificateDocumentsDTO.getExternalPreCertificateDocs(), request, "ext_pre_id"));
        }
        log.info(LOG_PREFIX + "Certificate documents updated by user with id : {}", securityService.getLoggedInUserDetailId());
        return certificateDocumentsDTO;
    }

    /**
     * Creates a mapping of parameter IDs to MultipartFile objects for multiple documents. This map will include only pre-existing documents.
     * Removes also from the document list the re-uploaded document.
     * @param multipleDocs    List of MultipartFile objects representing multiple documents.
     * @param request         The request.
     * @return                A mapping of parameter IDs to corresponding MultipartFile objects.
     */
    private Map<String, MultipartFile> createIdToMultipartFileMap(List<MultipartFile> multipleDocs,MultipartHttpServletRequest  request, String parameterName) {
        log.info(LOG_PREFIX + "Create file map...");
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, MultipartFile> idToMultipartFileMap = new HashMap<>();
        if (multipleDocs != null) {
            String[] parameterIds = parameterMap.get(parameterName);
            if (parameterIds != null) {
                List<MultipartFile> documentsToBeRemoved = new ArrayList<>();
                for (int i = 0; i < parameterIds.length; i++) {
                    String parameterId = parameterIds[i];
                    if (!parameterId.isEmpty()) {
                        MultipartFile multipleDoc = multipleDocs.get(i);
                        idToMultipartFileMap.put(parameterId, multipleDoc);
                        documentsToBeRemoved.add(multipleDoc);
                    }
                }

                for (int i = 0; i < documentsToBeRemoved.size(); i++) {
                    multipleDocs.remove(documentsToBeRemoved.get(i));
                }
            }
        }
        log.info(LOG_PREFIX + "File map created");
        return idToMultipartFileMap;
    }

    /**
     * Initializes the audit pair object.
     * @param certificateDTO - the parent certificate DTO
     * @param childrenCertificateId - the child certificate DTO
     * @return - the object pair
     */
    private Pair<AuditCertDTO, AuditCertDTO> getInitialPreCertificatePair(CertificateDTO certificateDTO, String childrenCertificateId) {
        AuditCertDTO oldValues = new AuditCertDTO()
                .setAuthority(getExistingCertificateById(childrenCertificateId).getForwardAuthority().getName())
                .setLoggingId(certificateDTO.getId());
        AuditCertDTO newValues = new AuditCertDTO()
                .setLoggingId(certificateDTO.getId());
        return new MutablePair<>(oldValues, newValues);
    }

    /**
     * Gets the existing certificate.
     * @param id The id of existing certificate.
     * @return
     */
    private CertificateDTO getExistingCertificateById(String id) {
        return certificateMapperInstance.map(certificateRepository.findById(id).orElse(null));
    }

    /**
     * Sets a new CertificateStatusHistory collection to the Certificate or adds an entry to the existing one
     * @param certificate the Certificate
     */
    public void updateCertificateStatusHistory(Certificate certificate) {
        if (certificate.getStatusHistorySet() == null) {
            CertificateStatusHistory statusHistory = new CertificateStatusHistory(Instant.now(), certificate.getStatus());
            certificate.setStatusHistorySet(new HashSet<>(List.of(statusHistory)));
        } else {
            certificate.getStatusHistorySet().add(new CertificateStatusHistory(Instant.now(), certificate.getStatus()));
        }
    }

    /**
     * Sets a new CertificateStatusHistoryDTO collection to the CertificateDTO or adds an entry to the existing one
     * @param certificateDTO the CertificateDTO
     */
    public void updateCertificateStatusHistory(CertificateDTO certificateDTO) {
        if (certificateDTO.getStatusHistorySet() == null) {
            CertificateStatusHistoryDTO statusHistory = new CertificateStatusHistoryDTO(Instant.now(), certificateDTO.getStatus());
            certificateDTO.setStatusHistorySet(new HashSet<>(List.of(statusHistory)));
        } else {
            certificateDTO.getStatusHistorySet().add(new CertificateStatusHistoryDTO(Instant.now(), certificateDTO.getStatus()));
        }
    }

}
