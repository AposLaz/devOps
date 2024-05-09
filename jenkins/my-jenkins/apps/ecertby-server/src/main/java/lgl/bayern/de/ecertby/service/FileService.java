package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.cm.dto.CreateFileAndVersionStatusDTO;
import com.eurodyn.qlack.fuse.cm.dto.FileDTO;
import com.eurodyn.qlack.fuse.cm.dto.FolderDTO;
import com.eurodyn.qlack.fuse.cm.dto.NodeAttributeDTO;
import com.eurodyn.qlack.fuse.cm.dto.NodeDTO;
import com.eurodyn.qlack.fuse.cm.dto.VersionAttributeDTO;
import com.eurodyn.qlack.fuse.cm.dto.VersionDTO;
import com.eurodyn.qlack.fuse.cm.mapper.NodeMapper;
import com.eurodyn.qlack.fuse.cm.model.Node;
import com.eurodyn.qlack.fuse.cm.model.QNode;
import com.eurodyn.qlack.fuse.cm.model.QNodeAttribute;
import com.eurodyn.qlack.fuse.cm.model.QVersion;
import com.eurodyn.qlack.util.av.api.dto.VirusScanDTO;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.audit.AuditCertDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.FileMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class FileService {

    @Autowired
    private final com.eurodyn.qlack.fuse.cm.service.DocumentService documentService;
    @Autowired
    private final com.eurodyn.qlack.fuse.cm.service.VersionService versionService;

    private final com.eurodyn.qlack.util.av.api.service.AvService antivirusService;
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    private final SecurityService securityService;

    @Autowired
    private CertificateRepository certificateRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EntityManager em;

    FileMapper fileMapper = Mappers.getMapper(FileMapper.class);
    NodeMapper nodeMapperInstance = Mappers.getMapper(NodeMapper.class);
    AuthorityMapper authorityMapper = Mappers.getMapper(AuthorityMapper.class);
    private QVersion Q_VERSION = QVersion.version;
    private QNodeAttribute Q_NODE_ATTRIBUTE = QNodeAttribute.nodeAttribute;
    private QNode Q_NODE = QNode.node;
    private final static String NOTES = "NOTES";

    /**
     * Creates a parent folder for a certificate based on its ID.
     *
     * @param certificateId The ID of the certificate.
     * @return The ID of the created folder.
     */
    public String createCertificateParentFolder(String certificateId, String parentId) {
        String userDetailId = securityService.getLoggedInUserDetailId();
        FolderDTO folder = new FolderDTO();
        folder.setName(certificateId);
        folder.setParentId(parentId);
        folder.setAttributes(new HashSet<>());
        return documentService.createFolder(folder, userDetailId, null);
    }

    /**
     * Creates a new file and version for a certificate.
     *
     * @param file              The file to be created.
     * @param notes             The notes for the file creation.
     * @param fileName          The name of the file.
     * @param certificateID     The ID of the certificate.
     * @param fileType          The type of the file.
     * @param certificateFolder The folder where the certificate is stored.
     * @return A DocumentDTO representing the created document.
     */
    public DocumentDTO createNewFileAndVersion(MultipartFile file, String notes, String fileName, String certificateID, FileType fileType, String certificateFolder) {
        String userDetailId = securityService.getLoggedInUserDetailId();
        byte[] content = null;

        try {
            content = file.getBytes();
            VirusScanDTO virusScanDTO = antivirusService.virusScan(content);
                if (!virusScanDTO.isVirusFree()) {
                    emailService.sendFileHasVirusEmail(certificateID,file.getOriginalFilename(), securityService.getLoggedInUserDetail().getEmail());
                    return null;
                }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create the node for the file
        FileDTO fileDTO = new FileDTO();
        fileDTO.setCreatedOn(new Date().getTime());
        fileDTO.setParentId(certificateFolder);
        fileDTO.setName(fileName);

        VersionDTO versionDTO = createNewVersion(file, notes, fileName);

        // Create node and version
        CreateFileAndVersionStatusDTO createFileAndVersionStatusDTO = documentService
                .createFileAndVersion(fileDTO, versionDTO, content, userDetailId, null);
        documentService.createAttribute(createFileAndVersionStatusDTO.getFileID(), fileType.toString(), certificateID, userDetailId, null);

        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setFilename(fileName);
        documentDTO.setNotes(notes);
        documentDTO.setId(createFileAndVersionStatusDTO.getFileID());
        return documentDTO;

    }

    /**
     * Copies a file and version.
     *
     * @param certificateFolderId   Parent folder where the files will be stored..
     * @param certificateId         Id of the certificate.
     * @param document              The document to be copied.
     * @param fileType              The type of the file.
     */
    public void copyFileAndVersionFromDocument(String certificateFolderId, String certificateId, DocumentDTO document, FileType fileType) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setCreatedOn(new Date().getTime());
        fileDTO.setParentId(certificateFolderId);
        fileDTO.setName(document.getFilename());

        VersionDTO versionDTO = new VersionDTO();
        versionDTO.setCreatedOn(new Date().getTime());
        versionDTO.setName(document.getEditedFilename());
        versionDTO.setFilename(document.getFilename());

        VersionAttributeDTO notesAttribute = new VersionAttributeDTO();
        notesAttribute.setName(NOTES);
        notesAttribute.setValue(document.getNotes());
        versionDTO.setAttributes(new HashSet<>());
        versionDTO.getAttributes().add(notesAttribute);

        CreateFileAndVersionStatusDTO createFileAndVersionStatusDTO = documentService
                .createFileAndVersion(fileDTO, versionDTO, this.getDocumentForDownload(document.getId()), securityService.getLoggedInUserId(), null);
        documentService.createAttribute(createFileAndVersionStatusDTO.getFileID(), fileType.toString(), certificateId, securityService.getLoggedInUserId(), null);
    }

    /**
     * Creates a new version for a file.
     *
     * @param file          The file to create a version for.
     * @param notes         The notes for the version.
     * @param fileName      The name of the file.
     * @return A VersionDTO representing the created version.
     */
    private VersionDTO createNewVersion(MultipartFile file, String notes, String fileName) {
        VersionDTO versionDTO = new VersionDTO();
        versionDTO.setCreatedOn(new Date().getTime());
        versionDTO.setName(file.getOriginalFilename());
        if(fileName!= null) {
            versionDTO.setFilename(fileName);
        } else {
            throw new QCouldNotSaveException("Error uploading  file", new EcertBYGeneralException(Arrays.asList(new EcertBYErrorException("file_name_not_exists", "file_name_not_exists", "TODO", "TODO", null, true))));
        }
        versionDTO.setAttributes(new HashSet<>());
        if (StringUtils.isNoneEmpty(notes)) {
            VersionAttributeDTO notesAttribute = new VersionAttributeDTO();
            notesAttribute.setName(NOTES);
            notesAttribute.setValue(notes);
            versionDTO.getAttributes().add(notesAttribute);
        }
        return versionDTO;
    }

    /**
     * Retrieves a page of document entities of a specific FileType associated with a document.
     *
     * @param id              The ID of the document.
     * @param fileType        The FileType of the documents to retrieve.
     * @param isPreCertificate The pre-certificate boolean to identify which type is the certificate.
     * @return A Page containing DocumentDTO objects matching the specified FileType, or null if no documents are found.
     */
    public Page<DocumentDTO> getDocumentsByType(String id, FileType fileType, boolean isPreCertificate) {
        String folderId = null;
        String originalId = id;
        if(isPreCertificate && (fileType == FileType.PRE_CERTIFICATE)) {
            String parentFolderId = findParentNodeIdByDocumentID(certificateRepository.findParentCertificateIdById(id), null);
            folderId = findParentNodeIdByDocumentID(id, parentFolderId);
        }

        if(isPreCertificate && fileType  == FileType.ADDITIONAL_DOCUMENT) {
            folderId = findParentNodeIdByDocumentID(certificateRepository.findParentCertificateIdById(id), null);
            id = certificateRepository.findParentCertificateIdById(id);
        }

        if (!isPreCertificate) {
            folderId = findParentNodeIdByDocumentID(id, null);
            if((fileType == FileType.CERTIFICATE ||
                    fileType == FileType.EXTERNAL_PRE_CERTIFICATE || fileType == FileType.SUPPLEMENTARY_CERTIFICATE)
                    && certificateRepository.findParentCertificateIdById(id) != null){
                folderId = findParentNodeIdByDocumentID(certificateRepository.findParentCertificateIdById(id), null);
                id = certificateRepository.findParentCertificateIdById(id);
            }
        }

        List<DocumentDTO> documentDTOS = getDocumentDTOList(isPreCertificate, folderId, id, fileType, originalId);
        // If pre_certificate type and not pre-certificate view get empty rows od certificates too.
        if (fileType.equals(FileType.PRE_CERTIFICATE)) {
            getPreCertificatesWithNoFile(documentDTOS, id);
        }
        if (documentDTOS.size() > 0) {
            return new PageImpl<>(documentDTOS);
        } else {
            return null;
        }
    }

    /**
     * Retrieves a list of documents of a specific FileType associated with a certificate.
     *
     * @param id              The ID of the certificate.
     * @param fileType        The FileType of the documents to retrieve.
     * @param isPreCertificate The pre-certificate boolean to identify which type is the certificate.
     * @param folderId The id of the document folder.
     * @return A list of DocumentDTOs
     */
    private List<DocumentDTO> getDocumentDTOList(boolean isPreCertificate, String folderId, String id, FileType fileType, String originalId) {

        Map<String, String> attributes = new HashMap<>();
        attributes.put(fileType.toString(), id);

        List<NodeDTO> foundNodes = documentService.getNodeByAttributes(folderId, attributes);
        if(!isPreCertificate){
            includeChildrenDocument(id, fileType, foundNodes);
        }

        // On pre-certificate form the addition documents of specific pre-authority should be added in the list.
        List<NodeDTO> preCertificateAdditionalNodes = null;
        if(isPreCertificate && fileType == FileType.ADDITIONAL_DOCUMENT){
            String parentFolderId = findParentNodeIdByDocumentID(certificateRepository.findParentCertificateIdById(originalId), null);
            String preCertificateFolderId = findParentNodeIdByDocumentID(originalId, parentFolderId);
            preCertificateAdditionalNodes = includePreCertificateAdditionalDocuments(originalId, fileType, preCertificateFolderId);
        }
        
        List<DocumentDTO> documentDTOS = new ArrayList<>();
        if (!foundNodes.isEmpty()) {
            for (NodeDTO foundNode : foundNodes) {
                if(includeDocument(foundNode.getAttributes(), isPreCertificate)){
                    DocumentDTO documentDTO = getDocumentDTO(foundNode);
                    documentDTO.setCertificateId(id);
                    if (fileType.equals(FileType.PRE_CERTIFICATE)) {
                        getPreCertificateFields(foundNode.getAttributes(), documentDTO);
                    }
                    documentDTO.setType(fileType);
                    documentDTOS.add(documentDTO);
                }
            }
        }
        // If on pre-certificate form add additional documents with pre-certificate id field filled-in.
        if (preCertificateAdditionalNodes != null) {
            for (NodeDTO foundNode : preCertificateAdditionalNodes) {
                DocumentDTO documentDTO = getDocumentDTO(foundNode);
                documentDTO.setCertificateId(id);
                documentDTO.setPreCertificateId(originalId);
                documentDTO.setType(fileType);
                documentDTOS.add(documentDTO);
            }
        }
        return documentDTOS;
    }

    private List<NodeDTO> includePreCertificateAdditionalDocuments(String id, FileType fileType,String certificateFolderId){
        Map<String, String> attributes = new HashMap<>();
        attributes.put(fileType.toString(), id);
        return documentService.getNodeByAttributes(certificateFolderId, attributes);
    }

    /**
     * Gets pre-certificates with no pre-certificate file but with certificate folder.
     * @param documentDTOList The list with pre-certificate documents.
     * @param parentCertificateId The parenti certificate id.
     */
    private void getPreCertificatesWithNoFile(List<DocumentDTO> documentDTOList, String parentCertificateId){
        List<Certificate> preCertificates = certificateRepository.findByParentCertificateId(parentCertificateId);

        List<Certificate> preCertificatesToAdd = preCertificates.stream()
                .filter(preCertificate ->
                        !documentDTOList.stream()
                                .map(DocumentDTO::getPreCertificateId).collect(Collectors.toList()).contains(preCertificate.getId())
                )
                .collect(Collectors.toList());
        preCertificatesToAdd.forEach(preCertificate -> {
            // Find if logged-in user is an authority user.
            UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
            if(userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString()) && preCertificate.getStatus() == CertificateStatus.PRE_CERTIFICATE_EXCLUDED){
                return;
            }
            DocumentDTO documentDTO = new DocumentDTO();
            documentDTO.setAuthority(authorityMapper.mapToOptionDTO(preCertificate.getForwardAuthority()));
            documentDTO.setStatus(preCertificate.getStatus());
            if (preCertificate.getPreCertificateActionBy() != null) {
                documentDTO.setPreCertificateActionByFirstName(preCertificate.getPreCertificateActionBy().getFirstName());
                documentDTO.setPreCertificateActionByLastName(preCertificate.getPreCertificateActionBy().getLastName());
                documentDTO.setPreCertificateActionOn(preCertificate.getPreCertificateActionOn());
            }
            documentDTO.setRejectionReason(preCertificate.getReason());
            documentDTO.setPreCertificateId(preCertificate.getId());
            documentDTO.setCertificateId(parentCertificateId);
            documentDTOList.add(documentDTO);
        });
    }

    /**
     * Get fields related with pre-certificate documents.
     * @param attributes Attributes' file.
     * @param documentDTO Nodes to retrieve.
     */
    private void getPreCertificateFields(Set<NodeAttributeDTO> attributes, DocumentDTO documentDTO){
        attributes.forEach(attr -> {
            if(attr.getName().equals(FileType.PRE_CERTIFICATE.name())){
                Certificate certificateStatusForwardAuthority = certificateRepository.findById(attr.getValue().toString()).orElse(null);
                documentDTO.setAuthority(authorityMapper.mapToOptionDTO(certificateStatusForwardAuthority.getForwardAuthority()));
                if (certificateStatusForwardAuthority.getPreCertificateActionBy() != null) {
                    documentDTO.setPreCertificateActionByFirstName(certificateStatusForwardAuthority.getPreCertificateActionBy().getFirstName());
                    documentDTO.setPreCertificateActionByLastName(certificateStatusForwardAuthority.getPreCertificateActionBy().getLastName());
                    documentDTO.setPreCertificateActionOn(certificateStatusForwardAuthority.getPreCertificateActionOn());
                }
                documentDTO.setStatus(certificateStatusForwardAuthority.getStatus());
                documentDTO.setRejectionReason(certificateStatusForwardAuthority.getReason());
                documentDTO.setPreCertificateId(certificateStatusForwardAuthority.getId());
            }
        });
    }

    /**
     * Checks if a document would be added in documents list.
     * Any document which is pre-certificate with status excluded or addition document which belongs to this pre-certificate is not visible to post-authority users.
     * @param attributes Node attributes.
     * @param isPreCertificate If call is from a pre-certificate or not.
     * @return If the document whould be included or not.
     */
    private boolean includeDocument(Set<NodeAttributeDTO> attributes, boolean isPreCertificate){
        if(isPreCertificate){
            return true;
        }
        // Find if logged-in user is an authority user.
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        if(userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())){
            for(NodeAttributeDTO attr: attributes){
                if(attr.getName().equals(FileType.PRE_CERTIFICATE.name()) || attr.getName().equals(FileType.ADDITIONAL_DOCUMENT.name())){
                    Certificate certificate = certificateRepository.findById(attr.getValue().toString()).orElse(null);
                    if(certificate.getStatus().equals(CertificateStatus.PRE_CERTIFICATE_EXCLUDED)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Includes documents from children folders.
     * @param id Certificate id.
     * @param fileType Filte type.
     * @param foundNodes Nodes to retrieve.
     */
    private void includeChildrenDocument(String id, FileType fileType, List<NodeDTO> foundNodes){
        List<String> childrenCertificateIds = certificateRepository.findIdByParentCertificateId(id);
        if(!childrenCertificateIds.isEmpty()) {
            foundNodes.addAll( nodeMapperInstance.mapToDTO(
                    new JPAQueryFactory((em)).select(Q_NODE_ATTRIBUTE.node)
                            .from(Q_NODE_ATTRIBUTE)
                            .where(Q_NODE_ATTRIBUTE.name.eq(fileType.name())
                                    .and(Q_NODE_ATTRIBUTE.value.in(childrenCertificateIds)))
                            .fetch()));
        }
    }

    /**
     * Converts a NodeDTO representing a document into a DocumentDTO by retrieving the latest version and associated notes.
     *
     * @param node The NodeDTO representing the document.
     * @return A DocumentDTO with the document information, including notes, filename, and other details.
     */
    private DocumentDTO getDocumentDTO(NodeDTO node) {
        VersionDTO latestVersion = versionService.getFileLatestVersion(node.getId());
        return getDocumentDTO(latestVersion);
    }
    private DocumentDTO getDocumentDTO(VersionDTO versionDTO) {
        //Attach the notes to the dto.
        String notes = versionDTO.getAttributes().stream()
                .filter(attribute -> attribute.getValue() != null && NOTES.equals(attribute.getName()))
                .map(VersionAttributeDTO::getValue)
                .findFirst()
                .orElse(null);
        DocumentDTO documentDTO = fileMapper.map(versionDTO);
        documentDTO.setNotes(notes);
        documentDTO.setEditedFilename(versionDTO.getFilename());
        documentDTO.setFilename(versionDTO.getName());
        return documentDTO;
    }

    public ResponseEntity<byte[]> getDocumentBytes(@PathVariable String id) {
        final VersionDTO provisioningDTO = versionService.getVersionById(id);
        String encodedFileName = URLEncoder.encode(provisioningDTO.getName(), StandardCharsets.UTF_8);
        byte[] documentBytes = getDocumentForDownload(id);
        HttpHeaders headers = createHeaders(provisioningDTO, encodedFileName);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(provisioningDTO.getSize())
                .body(documentBytes);
    }

    /**
     * Finds the parent node ID based on the document ID.
     *
     * @param documentId The ID of the document.
     * @return The ID of the parent node, or null if not found.
     */
    public String findParentNodeIdByDocumentID(String documentId, String parentId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("NAME", documentId);
        List<NodeDTO> dto = documentService.getNodeByAttributes(parentId, attributes);
        if (!dto.isEmpty()) {
            return dto.get(0).getId();
        } else {
            return null;
        }
    }

    /**
     * Same functionality as its overload, accepting an extra auditPair required by some cases of auditing.
     * @param auditPair The object pair (old values - new values) that will be used by auditing.
     */
    public void handleDocument(String folderId, String fileId, String notes, String editedFileName,
                               MultipartFile document, String documentId, FileType fileType,
        Pair<AuditCertDTO, AuditCertDTO> auditPair, String auditPrefix, String mainFolderId, String resourceId){
        //Case 1 : Document null: update notes and edited name
        //Case 2 : Document is not null: overwrite document and update notes and edited name
        if(fileId != null) {
            handleDocumentOperation(fileId,
                    notes,
                    editedFileName,
                    document ,
                    documentId,
                    auditPair,
                    auditPrefix + "_edited",
                    resourceId);

        }
        //Case 3 : Document has no id: Create the document and the attributes of it.
        else if(document != null && fileId == null){
            createNewFileAndVersion(document, notes, editedFileName, documentId, fileType, folderId);
            auditService.saveFileUploadCreateAudit(AuditAction.UPDATE ,userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                    document.getOriginalFilename(),mainFolderId, auditPrefix + "_created", resourceId);
            //todo certif
            String type = (FileType.TEMPLATE.equals(fileType)) ? "Template" : "Certificate";
            log.info(LOG_PREFIX + type + " document under main folder id {} successfully updated by user with id : {}", mainFolderId, securityService.getLoggedInUserDetailId());
        }
    }

    /**
     * Finds certificate id by file id.
     * @param fileId File id.
     * @return The certificate id
     */
    public String findCertificateIdByFileId(String fileId){
        return new JPAQueryFactory((em)).select(Q_NODE_ATTRIBUTE.value).
        from(Q_NODE_ATTRIBUTE).where(Q_NODE_ATTRIBUTE.name.eq(FileType.PRE_CERTIFICATE.toString()).and(
                Q_NODE_ATTRIBUTE.node.id.eq(new JPAQueryFactory((em)).select(Q_VERSION.node.id).
                        from(Q_VERSION).where(Q_VERSION.id.eq(fileId)).fetch().get(0)))).fetchOne();
    }


    /**
     * Gets the folder id. If it does not exist it creates a new folder and returns the id.
     * @param documentId The saved document id.
     * @param parentId The parent id.
     * @return The id of created folder.
     */
    public String getFolderId(String documentId, String parentId) {
        String mainFolder = findParentNodeIdByDocumentID(documentId, parentId);

        if (mainFolder == null) {
            return createCertificateParentFolder(documentId, parentId);
        }
        return mainFolder;
    }
    /**
     * Checks if a document of the specified type exists and overwrites it with a new document.
     *
     * @param notes          The notes for the document.
     * @param filename       The name of the file.
     * @param file           The new file to be created.
     */
    public void handleDocumentOperation(String documentId, String notes, String filename, MultipartFile file,String certificateId,
        Pair<AuditCertDTO, AuditCertDTO> auditPair, String auditPrefix, String resourceId) {
        Node node = new JPAQueryFactory((em)).selectFrom(Q_VERSION).where(Q_VERSION.id.eq(documentId)).fetchOne().getNode();
        VersionDTO version = versionService.getFileLatestVersion(node.getId());
        //check if there is a file overwrite
        if (file != null) {
            if(version.getName().equals(file.getOriginalFilename())) {
                try {
                    String userId = securityService.getLoggedInUserDetailId();
                    version.setFilename(filename);
                    versionService.updateVersion(node.getId(),version,file.getBytes(),userId,true,null);
                    versionService.updateAttribute(node.getId(),NOTES,notes,userId,null,version.getName());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                version.setName(file.getName());
                versionService.deleteVersion(documentId, null);
                VersionDTO newVersion = createNewVersion(file, notes, filename);
                try {
                    VirusScanDTO virusScanDTO = antivirusService.virusScan(file.getBytes());
                    if (!virusScanDTO.isVirusFree()) {
                        String errorMessage = MessageConfig.getValue("file_upload_has_virus", new Object[]{file.getOriginalFilename()});
                        throw new QCouldNotSaveException("Error uploading  file ", new EcertBYGeneralException(
                            Arrays.asList(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true))));
                    }
                    versionService.createVersion(node.getId(), newVersion, filename, file.getBytes(), securityService.getLoggedInUserDetailId(), null);
                    auditService.saveFileUploadCreateAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                        newVersion.getName(), certificateId, auditPrefix, resourceId);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            handleEditedDocumentOperation(version, filename, notes, node, auditPair, certificateId, auditPrefix, resourceId);
        }
    }

    private void handleEditedDocumentOperation(VersionDTO version, String filename, String notes,
                                             Node node, Pair<AuditCertDTO, AuditCertDTO> auditPair, String certificateId, String auditPrefix, String resourceId) {
       List<VersionAttributeDTO> existingNotesAttributeList = version.getAttributes().stream()
                .filter(versionAttributeDTO -> NOTES.equals(versionAttributeDTO.getName())).collect(Collectors.toList());
        VersionAttributeDTO existingNotesAttribute = (existingNotesAttributeList.isEmpty()) ? null : existingNotesAttributeList.get(0);

        //Check if the notes or filename has been edited
        if((notes != null && ((existingNotesAttribute != null && !notes.equals(existingNotesAttribute.getValue())) || existingNotesAttribute == null))
                || !version.getFilename().equals(filename) ) {
            if(notes != null){
                VersionAttributeDTO notesAttribute = new VersionAttributeDTO();
                notesAttribute.setName(NOTES);
                notesAttribute.setValue(notes);
                version.getAttributes().add(notesAttribute);
            }
            version.setFilename(filename);

            // get the old object for auditing
            DocumentDTO oldDocument = getDocumentDTO(versionService.getFileLatestVersion(node.getId()));

            versionService.updateVersion(node.getId(), version, null, securityService.getLoggedInUserDetailId(), true, null);

            // get the updated object for auditing
            DocumentDTO newDocument = getDocumentDTO(versionService.getFileLatestVersion(node.getId()));

            if (!isNull(auditPair)) {
                auditPair.getLeft().setFileName(oldDocument.getEditedFilename());
                auditPair.getRight().setFileName(newDocument.getEditedFilename());
                auditService.saveFileUploadEditAudit(AuditAction.UPDATE ,userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                        version.getName(), auditPair.getLeft().getLoggingId(), auditPair, "certificate_pre_certificate_file_edited", resourceId);
            } else {
                auditService.saveFileUploadEditAudit(AuditAction.UPDATE ,userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                        version.getName(), certificateId, oldDocument, newDocument, auditPrefix, resourceId);
            }
        }
    }

    /**
     * Get document for download.
     * @param id The document id.
     * @return The requested document.
     */
    public byte[] getDocumentForDownload(String id) {
        String node = new JPAQueryFactory((em)).selectFrom(Q_VERSION).where(Q_VERSION.id.eq(id)).fetchOne().getNode().getId();
        return versionService.getBinContent(node);
    }

    /**
     * Compares the list of documents to be saved with the existing documents associated with a certificate.
     * Delete documents that are no longer present in the list of documents to be saved.
     *
     * @param certificateId      The ID of the certificate.
     * @param documentsToBeSaved List of DocumentDTO objects representing documents to be saved or updated.
     * @param fileType The file type to check.
     */
    public void checkDiffBetweenDocs(String certificateId , List<DocumentDTO> documentsToBeSaved,
        String parentId, FileType fileType, String resourceId) {
        String parentNode = findParentNodeIdByDocumentID(certificateId, parentId);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(fileType.toString(), certificateId);
        List<NodeDTO> foundDocs = documentService.getNodeByAttributes(parentNode, attributes);
        // If parent id in null then we have a parent certificate.
        if(parentId == null){
            includeChildrenDocument(certificateId, fileType, foundDocs);
        }

        List<String> nonCommonIds = getNonCommonId(foundDocs, documentsToBeSaved);

        for (String id : nonCommonIds) {
            List<VersionDTO> versionDTOS = versionService.getFileVersions(id);
            for(VersionDTO versionDTO : versionDTOS){
                auditService.saveFileUploadDeleteAudit(AuditAction.DELETE,userMapperInstance.map(
                    securityService.getLoggedInUserDetailDTO()),versionDTO.getName(),certificateId,
                    "certificate_file_deleted", resourceId);
            }
            documentService.deleteFile(id, null);
        }
    }

    /**
     * Checks the difference between the pre-certificate documents.
     * Deletes the pre-certificate folder if the pre-certificate file do not exist.
     * Sets the list of pre-certificates to be deleted.
     * @param certificateId The certificate id.
     * @param documentsToBeSaved The documents send from the request.
     * @param preCertificatesIdsToBeDeleted The list with pre-certificate ids to be deleted.
     */
    public void checkDiffBetweenPreCertificateDocs(String certificateId ,List<DocumentDTO> documentsToBeSaved,
        List<String> preCertificatesIdsToBeDeleted, String resourceId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FileType.PRE_CERTIFICATE.toString(), certificateId);
        List<NodeDTO> foundDocs = new ArrayList<>();
        includeChildrenDocument(certificateId, FileType.PRE_CERTIFICATE, foundDocs);

        List<String> nonCommonIds = getNonCommonId(foundDocs, documentsToBeSaved);

        for (String id : nonCommonIds) {
            List<VersionDTO> versionDTOS = versionService.getFileVersions(id);
            for(VersionDTO versionDTO : versionDTOS){
                auditService.saveFileUploadDeleteAudit(AuditAction.DELETE,userMapperInstance.map(
                    securityService.getLoggedInUserDetailDTO()),versionDTO.getName(),certificateId,
                    "certificate_pre_certificate_file_deleted", resourceId);
            }
            updatePreCertificateIdsToDelete(id, preCertificatesIdsToBeDeleted);
            deleteFolderByFileNodeId(id);
        }
    }

    /**
     * Gets the non-common ids between the saved and retrieved documents.
     * @param foundDocs The saved documents.
     * @param documentsToBeSaved The retrieved documents.
     * @return
     */
    private List<String> getNonCommonId(List<NodeDTO> foundDocs, List<DocumentDTO> documentsToBeSaved){
        List<String> savedIds = foundDocs != null ? foundDocs.stream().map(NodeDTO::getId).collect(Collectors.toList()) : new ArrayList<>();

        List<String> idsToBeSaved = documentsToBeSaved.stream()
                .filter(document -> document.getId() != null)
                .map(document -> new JPAQueryFactory(em).selectFrom(Q_VERSION).where(Q_VERSION.id.eq(document.getId())).fetchOne().getNode().getId())
                .collect(Collectors.toList());
        return Stream.concat(savedIds.stream().filter(id -> !idsToBeSaved.contains(id)),
                        idsToBeSaved.stream().filter(id -> !savedIds.contains(id)))
                .collect(Collectors.toList());
    }

    /**
     * Updates the list of pre-certificate ids which will be deleted.
     * @param nodeFileId The node file id.
     * @param preCertificatesIdsToBeDeleted The list with pre-certificate ids.
     */
    public void updatePreCertificateIdsToDelete(String nodeFileId, List<String> preCertificatesIdsToBeDeleted){

        String id = new JPAQueryFactory(em).select(Q_NODE_ATTRIBUTE.value).from(Q_NODE_ATTRIBUTE)
                .where(Q_NODE_ATTRIBUTE.name.eq(FileType.PRE_CERTIFICATE.name()).and(Q_NODE_ATTRIBUTE.node.id.eq(nodeFileId))
                ).fetchOne();

        preCertificatesIdsToBeDeleted.add(id);
    }

    /**
     * Deletes a folder by file note id.
     * @param nodeFileId The file note id.
     */
    public void deleteFolderByFileNodeId(String nodeFileId){
        String folderId = new JPAQueryFactory(em).select(Q_NODE.parent.id).from(Q_NODE).where(Q_NODE.id.eq(nodeFileId)).fetchOne();
        documentService.deleteFolder(folderId, null);
    }

    /**
     * Checks file for virus.
     * @param file The file to check.
     * @return If the file has virus.
     */
    public boolean checkFileForVirus(MultipartFile file) {
        byte[] content = null;

        try {
            content = file.getBytes();
            VirusScanDTO virusScanDTO = antivirusService.virusScan(content);
            if (!virusScanDTO.isVirusFree()) {
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    return  false;
    }

    public void deleteNodeByPreCertificateIdAndAttribute(String certificateId, String attributeName, String parentCertificateId,
        List<DocumentDTO> documentsToBeSaved, String resourceId){
        String parentFolderId = findParentNodeIdByDocumentID(parentCertificateId, null);
        String subFolderId = findParentNodeIdByDocumentID(certificateId, parentFolderId);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(attributeName, certificateId);
        List<NodeDTO> node =  documentService.getNodeByAttributes(subFolderId, attributes);
        List<String> nonCommonIds = getNonCommonId(node, documentsToBeSaved);

        for (String id : nonCommonIds) {
            List<VersionDTO> versionDTOS = versionService.getFileVersions(id);
            for(VersionDTO versionDTO : versionDTOS){
                auditService.saveFileUploadDeleteAudit(AuditAction.DELETE,userMapperInstance.map(
                        securityService.getLoggedInUserDetailDTO()),versionDTO.getName(),certificateId,
                    "certificate_pre_certificate_file_deleted", resourceId);
            }
            documentService.deleteFolder(node.get(0).getId(), null);
        }
    }

    public HttpHeaders createHeaders(VersionDTO provisioningDTO, String encodedFileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName);

        if (provisioningDTO.getName().endsWith(".pdf") || provisioningDTO.getMimetype().equals("application/pdf")) {
            headers.setContentType(MediaType.APPLICATION_PDF);
        } else if (provisioningDTO.getName().endsWith(".png") || provisioningDTO.getMimetype().equals("image/png")) {
            headers.setContentType(MediaType.IMAGE_PNG);
        } else if(provisioningDTO.getName().endsWith(".jpg") || provisioningDTO.getName().endsWith(".jpeg") || provisioningDTO.getMimetype().equals("image/jpg")) {
            headers.setContentType(MediaType.IMAGE_JPEG);
        }
        return headers;
    }
}
