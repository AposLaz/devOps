package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.ws.rs.NotAllowedException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.CatalogValueMapper;
import lgl.bayern.de.ecertby.mapper.TemplateMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.*;
import lgl.bayern.de.ecertby.repository.TemplateRepository;
import lgl.bayern.de.ecertby.validator.TemplateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class TemplateService extends BaseService<TemplateDTO, Template, QTemplate> {
    private final TemplateRepository templateRepository;

    TemplateMapper templateMapper = Mappers.getMapper(TemplateMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    CatalogValueMapper catalogValueMapperInstance = Mappers.getMapper(CatalogValueMapper.class);

    private final TemplateValidator templateValidator;

    private final AuditService auditService;

    private final AttributeService attributeService;

    private final SecurityService securityService;

    private final ObjectLockService objectLockService;

    private final FileService fileService;

    /**
     * Save the template.
     * @param templateDTO The given template object.
     * @return The saved template object.
     */
    public TemplateDTO saveTemplate(TemplateDTO templateDTO, MultipartHttpServletRequest request){
        objectLockService.checkAndThrowIfLocked(templateDTO.getId(), ObjectType.TEMPLATE);
        templateValidator.validateTemplate(templateDTO);

        TemplateDTO oldTemplate = null;
        if (!isNull(templateDTO.getId())) {
            log.info(LOG_PREFIX + "Create template...");
            oldTemplate = templateMapper.map(templateRepository.findById(templateDTO.getId()).get());
        } else {
            log.info(LOG_PREFIX + "Update template...");
        }

        TemplateDocumentsDTO templateDocumentsDTO =  updateTemplateDocuments(request, null);

        retrieveTemplateElements(templateDTO);

        TemplateDTO savedTemplateDTO = templateMapper.map(templateRepository.save(templateMapper.map(templateDTO)));

        log.info(LOG_PREFIX + "Handle template documents...");
        String templateMainFolderId = handleTemplateDocuments(templateDTO, templateDocumentsDTO, savedTemplateDTO, null);

        // Auditing for the create and edit action of template.
        if (templateDTO.getId() == null) {
            log.info(LOG_PREFIX + "New template with id {} successfully created by user with id : {}.", savedTemplateDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveTemplateAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedTemplateDTO);
        } else {
            log.info(LOG_PREFIX + "Template with id {} successfully updated by user with id : {}.", savedTemplateDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveTemplateAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                templateDTO.getTemplateName(), oldTemplate, templateMapper.map(templateRepository.findById(templateDTO.getId()).get()));
        }
        return savedTemplateDTO;
    }

    private void retrieveTemplateElements(TemplateDTO templateDTO) {
        Set<TemplateElementDTO> templateElementDTOSet = new HashSet<>();

        //TODO

        templateDTO.setTemplateElementDTOSet(templateElementDTOSet);
    }

    /**
     * Handles the template's documents.
     * @param templateDTO The template dto.
     * @param templateDocumentsDTO The template documents dto.
     * @param savedTemplateDTO The saved template dto.
     * @return The parent folder id.
     */
    private String handleTemplateDocuments(TemplateDTO templateDTO, TemplateDocumentsDTO templateDocumentsDTO, TemplateDTO savedTemplateDTO, String resourceId){
        String templateMainFolderId = null;

        String currentFolderId = fileService.getFolderId(savedTemplateDTO.getId(), templateMainFolderId);
        // Template documents.
        if (templateDTO.getTemplateFile() != null) {
            fileService.handleDocument(currentFolderId, templateDTO.getTemplateFile().getId(), templateDTO.getTemplateFile().getNotes(),
                    templateDTO.getTemplateFile().getEditedFilename(), templateDocumentsDTO.getTemplateDoc()
                    , savedTemplateDTO.getId(), FileType.TEMPLATE, null, "template_file", savedTemplateDTO.getId(), resourceId);
        }
        // Additional documents.
        //List<DocumentDTO> additionalDocuments = templateDTO.getTemplateAdditionalFiles();
        // Remove additional documents which has no pre-certificate id if on edit of pre-certificate form.
        /*handleDocumentsByType(savedTemplateDTO.getId(), templateDTO, templateDocumentsDTO.getAdditionalDocs(),
                templateDocumentsDTO.getAdditionalDocMap(), currentFolderId, certificateMainFolderId, FileType.ADDITIONAL_DOCUMENT, additionalDocuments);
*/
        // If template main folder is null then the template is new.
        if(templateMainFolderId == null){
            templateMainFolderId = currentFolderId;
        }
        return templateMainFolderId;
    }

    /**
     * Updates the template's documents.
     * @param request The documents.
     * @param templateId The template id.
     * @return The object with the template's documents.
     */
    public TemplateDocumentsDTO updateTemplateDocuments(MultipartHttpServletRequest request, String templateId){
        TemplateDocumentsDTO templateDocumentsDTO = new TemplateDocumentsDTO();
        if (request != null) {
            MultiValueMap<String, MultipartFile> multiFileMap = request.getMultiFileMap();
            if (multiFileMap.get(FileUploadType.TEMPLATE.getValue()) != null) {
                if (multiFileMap.get(FileUploadType.TEMPLATE.getValue()).get(0) != null) {
                    templateDocumentsDTO.setTemplateDoc(multiFileMap.get(FileUploadType.TEMPLATE.getValue()).get(0));
                }
            }
            if (templateId != null) {
//            certificateDocumentsDTO.setAdditionalDocMap(createIdToMultipartFileMap(certificateDocumentsDTO.getAdditionalDocs(), request, "id"));
            }
            log.info(LOG_PREFIX + "Template documents updated by user with id : {}", securityService.getLoggedInUserDetailId());
        }
        return templateDocumentsDTO;
    }

    /**
     * Release the template, logging the action.
     * @param id The given template id.
     * @return True if release was successful, False otherwise.
     */
    public boolean releaseTemplate(String id) {
        log.info(LOG_PREFIX + "Release template...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.TEMPLATE);
        TemplateDTO templateDTO = findById(id);

        if(templateDTO.isRelease()){
            log.info(LOG_PREFIX + "Template with id : {} cannot be released.", id);
            throw new NotAllowedException("Template cannot be released.");
        }

        // LOG RELEASE
        auditService.saveReleaseTemplateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), templateDTO);
        log.info(LOG_PREFIX + "Template with id {} successfully released by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        return release(id, Template.class);
    }

    /**
     * Activate/Deactivate the template, logging the action.
     * @param isActive The new active state.
     * @param id The given template id.
     * @return True if activation/deactivation was successful, False otherwise.
     */
    public boolean activateTemplate(boolean isActive, String id) {
        log.info(LOG_PREFIX + "Activate template...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.TEMPLATE);
        TemplateDTO templateDTO = findById(id);

        if (isActive) {
            if(templateDTO.isActive()){
                log.info(LOG_PREFIX + "Template with id : {} cannot be activated.", id);
                throw new NotAllowedException("Template cannot be activated.");
            }
            // LOG ACTIVATION
            auditService.saveTemplateAudit(AuditAction.ACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), templateDTO);
            log.info(LOG_PREFIX + "Template with id {} successfully activate by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        } else {
            if(!templateDTO.isActive()){
                log.info(LOG_PREFIX + "Template with id : {} cannot be deactivated.", id);
                throw new NotAllowedException("Template cannot be deactivated.");
            }
            // LOG DEACTIVATION
            auditService.saveTemplateAudit(AuditAction.DEACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), templateDTO);
            log.info(LOG_PREFIX + "Template with id {} successfully deactivate by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        }

        return activate(isActive, id, Template.class);
    }

    /**
     * Return the active and released templates associated with a country and a product, as long as they are valid or will be valid in the future.
     * @param targetCountryId The id of the country.
     * @param productId The id of the product.
     * @return The respective templates as a page of TemplateDTOs.
     */
    public Page<TemplateDTO> getTemplateByTargetCountryIdAndProductId(String targetCountryId, String productId, Predicate predicate, Pageable pageable) {
        log.info(LOG_PREFIX + "Get templates by target country id and product id...");
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(predicate)
                .and(QTemplate.template.targetCountry.id.eq(targetCountryId))
                .and(QTemplate.template.product.id.eq(productId))
                .and(QTemplate.template.active)
                .and(QTemplate.template.release)
                .andAnyOf(
                        (QTemplate.template.validFrom.goe(Instant.now())),
                        (QTemplate.template.validFrom.loe(Instant.now()).and(QTemplate.template.validTo.goe(Instant.now()))),
                        (QTemplate.template.validFrom.loe(Instant.now())).and(QTemplate.template.validTo.isNull())
                );

        Page<TemplateDTO> response = findAll(finalPredicate,pageable);
        log.info(LOG_PREFIX + "Templates by target country id and product id found.");

        return response;
    }

    /**
     * Return the keywords associated with a template.
     * @param templateId The id of the template.
     * @return The keywords as a list of OptionDTOs.
     */
    public List<OptionDTO> getTemplateKeywordsById(String templateId) {
        log.info(LOG_PREFIX + "Get template's keywords by id...");
        List<CatalogValue> results = templateRepository.findTemplateKeywordsById(templateId)
                .stream()
                .map(TemplateKeyword::getKeyword)
                .toList();
        List<OptionDTO> response = catalogValueMapperInstance.mapToListOptionDTO(results);
        log.info(LOG_PREFIX + "Template's keywords by id found.");

        return response;
    }

    /**
     * Return the comment of a template.
     * @param templateId The id of the template.
     * @return The comment as a string.
     */
    public String getTemplateCommentById(String templateId) {
        log.info(LOG_PREFIX + "Get template's comment by id...");
        return templateRepository.findTemplateCommentById(templateId);
    }

    public Map<String, List> getTemplateElementsAndValuesByTemplateId(String templateId) {
        Map<String, List> templateElementsAndValuesMap = new HashMap<>();
        Template template = templateRepository.findById(templateId).orElse(null);
        if (template != null) {
            List<OptionDTO> templateElements = getTemplateElementsByTemplate(template);
            templateElementsAndValuesMap.put("TEMPLATE_ELEMENTS", templateElements);
            List<OptionDTO> templateElementValues = getTemplateElementValuesByTemplate(template);
            templateElementsAndValuesMap.put("TEMPLATE_ELEMENT_VALUES", templateElementValues);
        } else {
            templateElementsAndValuesMap.put("TEMPLATE_ELEMENTS", new ArrayList());
            templateElementsAndValuesMap.put("TEMPLATE_ELEMENT_VALUES", new ArrayList());
        }
        return templateElementsAndValuesMap;
    }

    public List<OptionDTO> getTemplateElementsByTemplate(Template template) {
        List<OptionDTO> templateElementsList = templateMapper.templateElementSetToOptionDTO(template.getTemplateElementSet());
        return  templateElementsList;
    }


    public List<OptionDTO> getTemplateElementValuesByTemplate(Template template) {
        List<OptionDTO> templateElementValuesList = new ArrayList<>();
        List<TemplateElement> radioGroupList = template.getTemplateElementSet().stream().filter(o -> PDFElementTypeEnum.RADIO_GROUP.equals(o.getElementType())).collect(Collectors.toList());

        for (TemplateElement radioGroup :radioGroupList) {
            for (TemplateElementValue radioButton : radioGroup.getTemplateElementValueSet()) {
                OptionDTO templateElementValue = new OptionDTO();
                templateElementValue.setId(radioButton.getId());
                templateElementValue.setName(radioButton.getValue());
                templateElementValue.setFilterId(radioButton.getId());
                templateElementValue.setActive(true);
                templateElementValuesList.add(templateElementValue);
            }
        }

        return  templateElementValuesList;
    }
}
