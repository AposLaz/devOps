package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.annotation.Resource;
import jakarta.ws.rs.NotAllowedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.dto.AttributeDTO;
import lgl.bayern.de.ecertby.dto.AuditDTO;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CatalogDTO;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.dto.TaskDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.audit.AuditCertDTO;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.mapper.AuditMapper;
import lgl.bayern.de.ecertby.model.Audit;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.QAudit;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.AuditType;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.TaskType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.AuditRepository;
import lgl.bayern.de.ecertby.repository.AuthorityRepository;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class AuditService extends BaseService<AuditDTO, Audit, QAudit> {

    @Resource(name = "messages")
    private Map<String, String> messages;

    private final AuditRepository auditRepository;
    private final CertificateRepository certificateRepository;
    private final DiffService<ComparableDTO> diffService;
    private final SecurityService securityService;
    private final CompanyRepository companyRepository;
    private final AuthorityRepository authorityRepository;

    AuditMapper auditMapperInstance = Mappers.getMapper(AuditMapper.class);

    private static final String USER_INTRO_MESSAGE = "user_intro_message";

    private static final String FEMININE = "feminine";
    private static final String MASCULINE = "masculine";

    /**
     * Audit the authority actions.
     *
     * @param action      The action performed.
     * @param actingUser  The user performed the action.
     * @param authorityDTO The authority dto.
     */
    public void saveAuthorityAudit(AuditAction action, UserDetail actingUser, AuthorityDTO authorityDTO) {
        StringJoiner messageIntro = new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("AUTHORITY")).add(authorityDTO.getName());
        saveAuditWithSuffix(AuditType.AUTHORITY, action, actingUser, messageIntro.toString(), authorityDTO.getId());
    }

    public void saveAuthorityAudit(AuditAction action, UserDetail actingUser, String authorityId, AuthorityDTO oldObj, AuthorityDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("AUTHORITY")).add(authorityId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.AUTHORITY, action, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the company actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     * @param company  The  the company dto.
     */
    public void saveCompanyAudit(AuditAction action, UserDetail actingUser, Company company) {
        StringJoiner messageIntro = new StringJoiner(" ").add(messages.get(MASCULINE)).add(messages.get("COMPANY")).add(company.getName());
        saveAuditWithSuffix(AuditType.COMPANY, action, actingUser, messageIntro.toString(), company.getId());
    }

    public void saveCompanyAudit(AuditAction action, UserDetail actingUser, String companyId, CompanyDTO oldObj, CompanyDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add(messages.get(MASCULINE)).add(messages.get("COMPANY")).add(companyId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.COMPANY, action, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the company profile actions.
     *
     * @param action        The action performed.
     * @param actingUser    The user performed the action.
     */
    public void saveProfileAudit(AuditAction action, UserDetail actingUser, CompanyProfileDTO companyProfileDTO) {
        saveAuditWithSuffix(AuditType.COMPANY, action, actingUser, messages.get("profile_intro_message").formatted( companyProfileDTO.getProfileName(), companyProfileDTO.getCompany().getName()) , companyProfileDTO.getId());
    }

    public void saveProfileAudit(AuditAction action, UserDetail actingUser, String profileId, String profileParent, CompanyProfileDTO oldObj, CompanyProfileDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(messages.get("profile_intro_message").formatted(profileId, profileParent));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.COMPANY, action, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the user actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveUserAudit(AuditAction action, UserDetail actingUser, UserDetailDTO userDetailDTO) {
        saveAuditWithSuffix(AuditType.USER, action, actingUser, messages.get(USER_INTRO_MESSAGE).formatted(userDetailDTO.getEmail()), userDetailDTO.getId());
    }

    public void saveUserAudit(AuditAction action, UserDetail actingUser, String userId, UserDetailDTO oldObj, UserDetailDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action)).formatted(messages.get(USER_INTRO_MESSAGE).formatted(userId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.USER, action, actingUser, messageDetail, newObj.getId());
    }

    public void saveUserEmailNotificationsAudit(UserDetail actingUser) {
        String message = messages.get("user_updated_email_notifications");
        saveAudit(AuditType.USER, AuditAction.UPDATE, actingUser, message, null);
    }

    /**
     * Audits the save company/authority with linking to existing user.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     * @param userId     The signifier for the user.
     * @param linkedWith the nane of the company/authority.
     * @param type       A type to identify if the action is performed on a company or authority.
     */
    public void linkUserAudit(AuditAction action, UserDetail actingUser, String userId,
                              String linkedWith, AuditType type, boolean isExisting) {

        String prefix = messages.get(USER_INTRO_MESSAGE).formatted(userId);
        String suffix = type.equals(AuditType.COMPANY)
                ? (isExisting ? messages.get("existing_user_linked_with_company") : messages.get("user_linked_with_company"))
                : (isExisting ? messages.get("existing_user_linked_with_authority") : messages.get("user_linked_with_authority"));
        saveAudit(AuditType.USER, action, actingUser, suffix.formatted(prefix, linkedWith), null);
    }

    /**
     * Audit actions related to 'Mein Konto'.
     *
     * @param actingUser The user performed the action.
     * @param userId     The signifier for the user.
     */
    public void saveMyAccountAudit(UserDetail actingUser, String userId, UserDetailDTO oldObj, UserDetailDTO newObj) {
        String introMessage = messages.get("account_update_message").formatted(userId);
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.USER, AuditAction.UPDATE, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the template actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveTemplateAudit(AuditAction action, UserDetail actingUser, TemplateDTO templateDTO) {
        StringJoiner messageIntro = new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("TEMPLATE")).add(templateDTO.getTemplateName());
        saveAuditWithSuffix(AuditType.TEMPLATE, action, actingUser, messageIntro.toString(), templateDTO.getId());
    }

    public void saveTemplateAudit(AuditAction action, UserDetail actingUser, String templateId, TemplateDTO oldObj, TemplateDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("TEMPLATE")).add(templateId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.TEMPLATE, action, actingUser, messageDetail, newObj.getId());
    }


    /**
     * Audit the Complete Task action.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveCompleteTaskAudit(AuditAction action, UserDetail actingUser, TaskDTO taskDTO) {
        String message;
        if (taskDTO.getType().equals(TaskType.COMPANY)) {
            message = messages.get("task_company_complete_message")
                    .formatted(taskDTO.getId(), taskDTO.getCompany().getName(), taskDTO.getInfo());
        } else {
            message = messages.get("task_authority_complete_message")
                    .formatted(taskDTO.getId(), taskDTO.getAuthority().getName(), taskDTO.getInfo());
        }

        saveAudit(AuditType.TASK, action, actingUser, message, taskDTO.getId());
    }

    /**
     * Audit the automatic Complete Task action.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveAutomaticCompleteTaskAudit(AuditAction action, UserDetail actingUser, TaskDTO taskDTO) {
        String message;
        if (taskDTO.getType().equals(TaskType.COMPANY)) {
            message = messages.get("task_company_automatic_complete_message")
                    .formatted(taskDTO.getId(), taskDTO.getCompany().getName(), taskDTO.getInfo());
        } else {
            message = messages.get("task_authority_automatic_complete_message")
                    .formatted(taskDTO.getId(), taskDTO.getAuthority().getName(), taskDTO.getInfo());
        }

        saveAudit(AuditType.TASK, action, actingUser, message, taskDTO.getId());
    }

    /**
     * Audit the Complete All Tasks action.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveCompleteAllTasksAudit(AuditAction action, UserDetail actingUser, AuditType auditType, String resourceName) {
        String message;
        if (auditType.equals(AuditType.COMPANY)) {
            message = messages.get("task_company_complete_all_message").formatted(resourceName);
        } else {
            message = messages.get("task_authority_complete_all_message").formatted(resourceName);
        }

        saveAudit(AuditType.TASK, action, actingUser, message, null);
    }

    /**
     * Audit the template release action.
     *
     * @param actingUser The user performed the action.
     * @param templateDTO The template dto
     */
    public void saveReleaseTemplateAudit(UserDetail actingUser, TemplateDTO templateDTO ) {
        String message = messages.get("template_release_message").formatted(templateDTO.getTemplateName());
        saveAudit(AuditType.TEMPLATE, AuditAction.UPDATE, actingUser, message, templateDTO.getId());
    }

    /**
     * Audit the notification actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveNotificationAudit(AuditAction action, UserDetail actingUser, NotificationDTO notificationDTO) {
        StringJoiner messageIntro = new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("NOTIFICATION")).add(notificationDTO.getTitle());
        saveAuditWithSuffix(AuditType.NOTIFICATION, action, actingUser, messageIntro.toString(), notificationDTO.getId());
    }


    public void saveNotificationAudit(AuditAction action, UserDetail actingUser, String title, NotificationDTO oldObj, NotificationDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add(messages.get(FEMININE)).add(messages.get("NOTIFICATION")).add(title));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.NOTIFICATION, action, actingUser, messageDetail, newObj.getId());
    }

    public void saveAttributeAudit(AuditAction action, UserDetail actingUser, AttributeDTO attributeDTO) {
        StringJoiner messageIntro = new StringJoiner(" ").add(messages.get(MASCULINE)).add(messages.get("ATTRIBUTE")).add(attributeDTO.getName());
        saveAuditWithSuffix(AuditType.ATTRIBUTE, action, actingUser, messageIntro.toString(), attributeDTO.getId());
    }


    public void saveAttributeAudit(AuditAction action, UserDetail actingUser, String title, AttributeDTO oldObj, AttributeDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add(messages.get(MASCULINE)).add(messages.get("ATTRIBUTE")).add(title));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.ATTRIBUTE, action, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the catalog actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveCatalogAudit(AuditAction action, UserDetail actingUser, CatalogDTO catalogDTO) {
        StringJoiner messageDetail = new StringJoiner(" ").add(messages.get(MASCULINE)).add(messages.get("CATALOG")).add(catalogDTO.getName());
        saveAuditWithSuffix(AuditType.CATALOG, action, actingUser, messageDetail.toString(), catalogDTO.getId());
    }

    public void saveCatalogValueAudit(AuditAction action, UserDetail actingUser, CatalogValueDTO catalogValueDTO) {
        String messageDetail;
        if (action.equals(AuditAction.CREATE)) messageDetail = messages.get("catalog_value_audit_add").formatted(catalogValueDTO.getData(), catalogValueDTO.getCatalog().getName());
        else messageDetail = messages.get("catalog_value_audit_delete").formatted(catalogValueDTO.getData(), catalogValueDTO.getCatalog().getName());
        saveAudit(AuditType.CATALOG, action, actingUser, messageDetail, catalogValueDTO.getId());
    }

    public void saveReplaceCatalogAudit(AuditAction action, UserDetail actingUser, CatalogDTO catalogDTO, String filename) {
        String messageDetail = messages.get("upload_catalog_audit").formatted(catalogDTO.getName(), filename);
        saveAudit(AuditType.CATALOG, action, actingUser, messageDetail, catalogDTO.getId());
    }

    public void saveCatalogValueAudit(AuditAction action, UserDetail actingUser, CatalogValueDTO oldCatalogValueDTO, CatalogValueDTO newCatalogValueDTO) {
        String introMessage = messages.get("catalog_value_audit_edit").formatted(newCatalogValueDTO.getData(), newCatalogValueDTO.getCatalog().getName());
        String messageDetail = diffService.compare(oldCatalogValueDTO, newCatalogValueDTO, introMessage);
        saveAudit(AuditType.CATALOG, action, actingUser, messageDetail, newCatalogValueDTO.getId());
    }

    /**
     * Audit the notification release action.
     *
     * @param actingUser The user performed the action.
     * @param notificationDTO The template dto
     */
    public void savePublishNotificationAudit(UserDetail actingUser, NotificationDTO notificationDTO ) {
        String message = messages.get("notification_publish_message").formatted(notificationDTO.getTitle());
        saveAudit(AuditType.NOTIFICATION, AuditAction.UPDATE, actingUser, message, notificationDTO.getId());
    }
    /**
     * Audit the certificate actions (standard actions).
     *
     * @param action        The action performed.
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveAuditForEntity(AuditAction action, UserDetail actingUser, String certificateId, String resourceId) {
        StringJoiner messageIntro = new StringJoiner(" ").add("Das").add(messages.get("CERTIFICATE")).add(certificateId);

        // Save audit with suffix
        saveAudit(AuditType.CERTIFICATE, action, actingUser,
            messages.get(AuditAction.getAUDIT_MESSAGES().get(action)).formatted(messageIntro.toString()),
            certificateId, resourceId);
    }

    public void saveAuditForEntity(AuditAction action, UserDetail actingUser, String certificateId,
        CertificateDTO oldObj, CertificateDTO newObj, String resourceId) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add("Das").add(messages.get("CERTIFICATE")).add(certificateId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.CERTIFICATE, action, actingUser, messageDetail, certificateId, resourceId);
    }

    /**
     * Audit the certificate release.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveReleaseCertificateAudit(UserDetail actingUser, String certificateId, String resourceId) {
        String message = messages.get("certificate_release_message").formatted(certificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }


    /**
     * Audit the certificate rejection.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveRejectCertificateAudit(UserDetail actingUser, String certificateId, String reason, String resourceId) {
        String message = messages.get("certificate_reject_message").formatted(certificateId, reason);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the certificate marking as lost.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveMarkCertificateAsLostAudit(UserDetail actingUser, String certificateId, String resourceId) {
        String message = messages.get("certificate_mark_as_lost_message").formatted(certificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the certificate revocation.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveRevokeCertificateAudit(UserDetail actingUser, String certificateId, String resourceId) {
        String message = messages.get("certificate_revoke_message").formatted(certificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the pre-certificate vote positive.
     *
     * @param actingUser          The user performed the action.
     * @param certificateId       The signifier for the certificate.
     * @param parentCertificateId The parent certificate signifier.
     */
    public void saveVotePositivePreCertificateAudit(UserDetail actingUser, String certificateId, String parentCertificateId, String resourceId) {
        String message = messages.get("pre_certificate_vote_positive_message").formatted(certificateId, parentCertificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the pre-certificate reject.
     *
     * @param actingUser          The user performed the action.
     * @param certificateId       The signifier for the certificate.
     * @param parentCertificateId The parent certificate signifier.
     */
    public void saveRejectPreCertificateAudit(UserDetail actingUser, String certificateId, String parentCertificateId, String reason, String resourceId) {
        String message = messages.get("pre_certificate_reject_message").formatted(certificateId, parentCertificateId, reason);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the pre-certificate forwarded.
     *
     * @param actingUser          The user performed the action.
     * @param certificateId       The signifier for the certificate.
     * @param parentCertificateId The parent certificate signifier.
     */
    public void saveForwardPreCertificateAudit(UserDetail actingUser, String certificateId, String parentCertificateId, String resourceId) {
        String message = messages.get("pre_certificate_forward_message").formatted(certificateId, parentCertificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    public void saveCreatePreCertificateAudit(UserDetail actingUser, String certificateId, String parentCertificateId, String resourceId) {
        String message = messages.get("pre_certificate_created_message").formatted(certificateId, parentCertificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.CREATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the pre-certificate excluded.
     *
     * @param actingUser          The user performed the action.
     * @param certificateId       The signifier for the certificate.
     * @param parentCertificateId The parent certificate signifier.
     */
    public void saveExcludePreCertificateAudit(UserDetail actingUser, String certificateId, String parentCertificateId, String resourceId) {
        String message = messages.get("pre_certificate_exclude_message").formatted(certificateId, parentCertificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the certificate blocking.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveBlockCertificateAudit(UserDetail actingUser, String certificateId, String resourceId) {
        String message = messages.get("certificate_block_message").formatted(certificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the certificate forwarding.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveForwardCertificateAudit(UserDetail actingUser, String certificateId, String resourceId) {
        String message = messages.get("certificate_forward_message").formatted(certificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }
    /**
     * Saves the audit trail for forwarding authority on a certificate.
     *
     * @param actingUser       The user performing the action.
     * @param certificateId    The ID of the certificate.
     * @param prevAuthority    The DTO object representing the previous authority.
     * @param currentAuthority The DTO object representing the current authority.
     * @param reason           The reason for the authority forward.
     */
    public void saveAuthorityForwardCertificateAudit(UserDetail actingUser, String certificateId,AuthorityDTO prevAuthority ,
        AuthorityDTO currentAuthority , String reason, String resourceId , boolean isPreCert) {
        String message;
        if(!isPreCert) {
          message = "certificate_forward_authority_message";
        } else {
            message = "pre_certificate_forward_authority_message";
        }
        message = messages.get(message).formatted(certificateId,prevAuthority.getName(),currentAuthority.getName(),reason);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the deleted certificate restoration.
     *
     * @param actingUser    The user performed the action.
     * @param certificateId The signifier for the certificate.
     */
    public void saveRestoreCertificateAudit(UserDetail actingUser, String certificateId, String companyId, String resourceId) {
        String message = messages.get("certificate_restore_message").formatted(certificateId, companyId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.UPDATE, actingUser, message, certificateId, resourceId);
    }

    public void saveCopyCertificateAudit(UserDetail actingUser, String originalCertificateId, String copiedCertificateId, String resourceId) {
        String message = messages.get("certificate_copy_message").formatted(copiedCertificateId, originalCertificateId);
        saveAudit(AuditType.CERTIFICATE, AuditAction.CREATE, actingUser, message, copiedCertificateId, resourceId);
    }

    /**
     * Audit the team actions.
     *
     * @param action     The action performed.
     * @param actingUser The user performed the action.
     */
    public void saveTeamAudit(AuditAction action, UserDetail actingUser,  String teamName, String teamId ) {
        StringJoiner messageIntro = new StringJoiner(" ").add("Das").add(messages.get("TEAM")).add(teamName);
        saveAuditWithSuffix(AuditType.TEAM, action, actingUser, messageIntro.toString(), teamId);
    }

    public void saveTeamAudit(AuditAction action, UserDetail actingUser, String teamId, TeamDTO oldObj, TeamDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add("Das").add(messages.get("TEAM")).add(teamId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.TEAM, action, actingUser, messageDetail, newObj.getId());
    }


    public void saveSearchCriteria(AuditAction action, UserDetail actingUser, String searchCriteriaName, String searchCriteriaId ) {
        StringJoiner messageIntro = new StringJoiner(" ").add("Die").add(messages.get("SEARCH_CRITERIA")).add(searchCriteriaName);
        saveAuditWithSuffix(AuditType.SEARCH_CRITERIA, action, actingUser, messageIntro.toString(), searchCriteriaId);
    }

    public void saveSearchCriteria(AuditAction action, UserDetail actingUser, String searchCriteriaId, SearchCriteriaDTO oldObj, SearchCriteriaDTO newObj) {
        String introMessage = messages.get(AuditAction.getAUDIT_MESSAGES().get(action))
                .formatted(new StringJoiner(" ").add("Die").add(messages.get("SEARCH_CRITERIA")).add(searchCriteriaId));
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.SEARCH_CRITERIA, action, actingUser, messageDetail, newObj.getId());
    }

    /**
     * Audit the certificate file upload create.
     *
     * @param action        The action performed.
     * @param actingUser    The user performed the action.
     * @param fileUploadId  The signifier for the file upload name.
     * @param certificateId The signifier for the certificate id.
     */
    public void saveFileUploadCreateAudit(AuditAction action, UserDetail actingUser, String fileUploadId, String certificateId, String auditPrefix, String resourceId) {
        String message = messages.get(auditPrefix).formatted(fileUploadId, certificateId);
        saveAudit(AuditType.CERTIFICATE, action, actingUser, message, certificateId, resourceId);
    }

    /**
     * Audit the certificate file upload update.
     *
     * @param action        The action performed.
     * @param actingUser    The user performed the action.
     * @param fileUploadId  The signifier for the file upload name.
     * @param certificateId The signifier for the certificate id.
     */
    public <T1 extends AuditCertDTO> void saveFileUploadEditAudit(AuditAction action, UserDetail actingUser, String fileUploadId, String certificateId,
        Pair<T1, T1> auditPair, String auditPrefix, String resourceId) {
        saveFileUploadEditAudit(action, actingUser, fileUploadId, certificateId, auditPair.getLeft(), auditPair.getRight(), auditPrefix, resourceId);
    }

    public <T1 extends ComparableDTO> void saveFileUploadEditAudit(AuditAction action, UserDetail actingUser, String fileUploadId, String certificateId,
        T1 oldObj, T1 newObj, String auditPrefix, String resourceId) {
        String introMessage = messages.get(auditPrefix).formatted(fileUploadId, certificateId);
        String messageDetail = diffService.compare(oldObj, newObj, introMessage);
        saveAudit(AuditType.CERTIFICATE, action, actingUser, messageDetail, certificateId, resourceId);
    }

    /**
     * Audit the certificate file upload delete.
     *
     * @param action        The action performed.
     * @param actingUser    The user performed the action.
     * @param fileUploadId  The signifier for the file upload name.
     * @param certificateId The signifier for the certificate id.
     */
    public void saveFileUploadDeleteAudit(AuditAction action, UserDetail actingUser, String fileUploadId, String certificateId,
        String auditPrefix, String resourceId) {
        String message = messages.get(auditPrefix).formatted(fileUploadId, certificateId);
        saveAudit(AuditType.CERTIFICATE, action, actingUser, message, certificateId, resourceId);
    }

    /**
     * Find all audits based on filters and take date fields not present in Audit model into account.
     *
     * @param predicate The predicate with all filters except for dateFrom, dateTo.
     * @param pageable  The common pageable.
     * @param dateFrom  The earliest date. Could be null.
     * @param dateTo    The latest date. Could be null.
     * @return The results as a page of AuditDTOs.
     */
    public Page<AuditDTO> findAll(Predicate predicate, Pageable pageable, Instant dateFrom, Instant dateTo) {
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(predicate);
        if (dateFrom != null) finalPredicate.and(QAudit.audit.createdOn.goe(dateFrom));
        if (dateTo != null) finalPredicate.and(QAudit.audit.createdOn.lt(dateTo.plus(1, ChronoUnit.DAYS)));


        return findAll(finalPredicate, pageable);
    }

    public Page<AuditDTO> findAllCertificateAudit(Predicate predicate, Pageable pageable, String certificateId, String selectionFromDD) {
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(predicate);
        if (certificateId != null) {
            if (!validateRequest(certificateId, selectionFromDD)) {
                log.info("Entity with id : {} has no rights to view the audits of certificate with id {}.",
                        selectionFromDD,
                        certificateId);
                throw new NotAllowedException("Audit cannot be viewed.");
            }

            List<String> certificateIds = certificateRepository.findMainAndPrecertificateIds(certificateId);
            finalPredicate.and(QAudit.audit.entityId.in(certificateIds));
        }

        Page<AuditDTO> resultPage = findAll(finalPredicate, pageable);

        // userDetail should only be visible to admins. In any other case, it should be empty.
        if (!ADMIN_RESOURCE.equals(selectionFromDD)) {
            List<AuditDTO> modifiedContent = resultPage.getContent()
                .stream()
                .peek(auditDTO -> auditDTO.setUserDetail(null)) // Set userDetail to null
                .collect(Collectors.toList());

            // Create a new Page with modified content
            return new PageImpl<>(modifiedContent, pageable, resultPage.getTotalElements());
        }

        return resultPage;
    }

    public AuditDTO findAuditForEntity(String entityId) {
        return auditMapperInstance.map(auditRepository.findFirstByEntityIdOrderByCreatedOnDesc(entityId));
    }

    /**
     * Adds value to the firstName & lastName fields in case the corresponding User is about to be deleted.
     *
     * @param userDetail the soon-to-be deleted User
     */
    public void addNameForDeletedUser(UserDetail userDetail) {
        for (Audit audit : auditRepository.findAllByUserDetail(userDetail)) {
            audit.setFirstName(userDetail.getFirstName());
            audit.setLastName(userDetail.getLastName());
            audit.setUserDetail(null);
            auditMapperInstance.map(audit);
            auditRepository.save(audit);
        }
    }

    /* PRIVATE METHODS */

    /**
     * Generic method to audit common actions.
     *
     * @param type         The type of the audited entity.
     * @param action       The action to audit.
     * @param actingUser   The user that performed the action.
     * @param messageIntro The message intro, consisting of article, entity name and related information (e.g. entity id)
     */
    public void saveAuditWithSuffix(AuditType type, AuditAction action, UserDetail actingUser, String messageIntro, String entityId) {
        saveAudit(type, action, actingUser, messages.get(AuditAction.getAUDIT_MESSAGES().get(action)).formatted(messageIntro), entityId);
    }

    private void saveAudit(AuditType type, AuditAction action, UserDetail actingUser, String detailMessage,  String entityId) {
        saveAudit(type, action, actingUser, detailMessage, entityId, null);
    }
    private void saveAudit(AuditType type, AuditAction action, UserDetail actingUser, String detailMessage,  String entityId, String resourceId) {
        Audit audit = createAuditEntity(type, action, actingUser, detailMessage);
        if (audit == null) return;

        if (!isNull(entityId)) {
            audit.setEntityId(entityId);
        }

        if (!isNull(resourceId)) {
            Optional<Company> company = companyRepository.findById(resourceId);
            Optional<Authority> authority = authorityRepository.findById(resourceId);

            if (company.isPresent() && authority.isPresent()) {
                // very, very rare case, where the same UUID exists on both company and authority.
                log.error("The UUID %s exists in both Authority and Company tables".formatted(resourceId));
                // do nothing else on this case.
            } else if (company.isPresent()) {
                audit.setUserCompany(company.get());
            } else if (authority.isPresent()) {
                audit.setUserAuthority(authority.get());
            } else {
                log.error("The UUID %s does not exist in Authority or Company tables".formatted(resourceId));
                // do nothing else on this case.
            }
        }

        auditRepository.save(audit);
        log.info(LOG_PREFIX + "Changes detected and audited successfully for an object of type {} "
                + "caused by user with ID: {}.", type, actingUser.getId());
    }

    private static Audit createAuditEntity(AuditType type, AuditAction action, UserDetail actingUser, String detailMessage) {
        if (isNull(detailMessage)) {
            return null;
        }

        Audit audit = new Audit();
        audit.setAuditType(type);
        audit.setAuditAction(action);
        audit.setUserDetail(actingUser);
        audit.setCreatedOn(Instant.now());

        audit.setDetail(detailMessage);
        return audit;
    }

    private boolean validateRequest(String certificateId, String selectionFromDD) {
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        String userType = userDetailDTO.getUserType().getId();

        Optional<Certificate> optionalCertificate = certificateRepository.findById(certificateId);
        Certificate certificate;
        if (optionalCertificate.isPresent()) {
            certificate = optionalCertificate.get();
        } else return false;

        // Validations according to filterings of findAll in CertificateService
        if (userType.equals(UserType.COMPANY_USER.toString())) {
            return certificate.getCompany().getId().equals(selectionFromDD) &&
                    !CertificateStatus.getExcludedStatuses().contains(certificate.getStatus());
        } else if (userType.equals(UserType.AUTHORITY_USER.toString())) {
            return certificate.getForwardAuthority() != null && (
                    certificate.getForwardAuthority().getId().equals(selectionFromDD) &&
                            !CertificateStatus.getIssuingAuthorityExcludedStatuses().contains(certificate.getStatus())
            );
        } else if (userType.equals(UserType.ADMIN_USER.toString())) {
            return !CertificateStatus.getExcludedStatusesForAdmin().contains(certificate.getStatus());
        }
        return false;
    }
}
