package lgl.bayern.de.ecertby.service;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.cm.service.DocumentService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotAllowedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CertificateAssignmentHistoryDTO;
import lgl.bayern.de.ecertby.dto.CertificateAuthorityToAuthorityForwardDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CertificateDocumentsDTO;
import lgl.bayern.de.ecertby.dto.CertificateForwardAuthorityDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.CatalogValueMapper;
import lgl.bayern.de.ecertby.mapper.CertificateAssignmentHistoryMapper;
import lgl.bayern.de.ecertby.mapper.CertificateMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.CertificateAssignmentHistory;
import lgl.bayern.de.ecertby.model.CertificateAssignmentHistoryTeam;
import lgl.bayern.de.ecertby.model.CertificateDepartment;
import lgl.bayern.de.ecertby.model.CertificateKeyword;
import lgl.bayern.de.ecertby.model.CertificatePreAuthority;
import lgl.bayern.de.ecertby.model.CertificateTeam;
import lgl.bayern.de.ecertby.model.QCertificate;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileType;
import lgl.bayern.de.ecertby.model.util.FileUploadType;
import lgl.bayern.de.ecertby.model.util.TaskType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.CertificateAssignmentHistoryRepository;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.validator.CertificateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CertificateService extends BaseService<CertificateDTO, Certificate, QCertificate> {

    private final EntityManager entityManager;

    @Value("${scheduler.recycle-bin-days-to-maintain:30}")
    private int recycleBinDaysToMaintain;

    CertificateMapper certificateMapperInstance = Mappers.getMapper(CertificateMapper.class);
    CertificateAssignmentHistoryMapper certificateAssignmentHistoryMapper = Mappers.getMapper(CertificateAssignmentHistoryMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    CatalogValueMapper catalogValueMapperInstance = Mappers.getMapper(CatalogValueMapper.class);
    private final AuthorityService authorityService;
    private final CompanyService companyService;
    private final SecurityService securityService;
    private final AuditService auditService;
    private final FileService fileService;
    private final DocumentService documentService;
    private final TaskService taskService;
    private final CertificateUpdateService certificateUpdateService;
    private final EmailService emailService;
    private final EmailNotificationService emailNotificationService;

    private static final QCertificate Q_CERTIFICATE = QCertificate.certificate;

    private final CertificateRepository certificateRepository;
    private final CertificateAssignmentHistoryRepository certificateAssignmentHistoryRepository;
    private final CertificateValidator certificateValidator;

    /**
     * Fetches all certificates with meet the given criteria.
     * @param predicate Predicate object.
     * @param pageable Page and sort order.
     * @param shippingDateFrom Shipping date from.
     * @param shippingDateTo Shipping date to.
     * @param printedDateFrom Printed date from.
     * @param printedDateTo Printed date to.
     * @param transferredDateFrom Transferred date from.
     * @param transferredDateTo transferred date to.
     * @param selectionFromDD selection from DD.
     * @return The list of certificates.
     */
    public Page<CertificateDTO> findAll(Predicate predicate, Pageable pageable,
                                  Instant shippingDateFrom, Instant shippingDateTo,
                                  Instant printedDateFrom, Instant printedDateTo,
                                  Instant transferredDateFrom, Instant transferredDateTo,
                                  Instant statusChangeDateFrom, Instant statusChangeDateTo,
                                  String selectionFromDD) {
        log.info(LOG_PREFIX + "Find all certificates...");
        BooleanBuilder finalPredicate = manipulatePredicate(predicate, shippingDateFrom, shippingDateTo, printedDateFrom, printedDateTo, transferredDateFrom, transferredDateTo, statusChangeDateFrom, statusChangeDateTo);

        // Adds filtering in the results according to logged-in user.
        UserDetailDTO dto = securityService.getLoggedInUserDetailDTO();

        // Company user filtered by the selected company
        if (dto.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                finalPredicate.and(Q_CERTIFICATE.company.id.eq(selectionFromDD));
                finalPredicate.and(Q_CERTIFICATE.status.notIn(CertificateStatus.getExcludedStatuses()));
            }
        }
        // Authority user filtered by the selected authority and certificate status different to Draft.
        else if(dto.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())){
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                finalPredicate.and(Q_CERTIFICATE.forwardAuthority.id.eq(selectionFromDD)
                        .and(Q_CERTIFICATE.status.notIn(CertificateStatus.getIssuingAuthorityExcludedStatuses())));

            }
        }
        // System administrator users can see certificates with status released, revoked, marked as lost.
        else if(dto.getUserType().getId().equals(UserType.ADMIN_USER.toString())){
            finalPredicate.and(Q_CERTIFICATE.status.notIn(CertificateStatus.getExcludedStatusesForAdmin()));
        }
        log.info(LOG_PREFIX + "All certificates found.");
        return findAll(finalPredicate, pageable);
    }

    /**
     * Fetches all deleted certificates that meet the given criteria.
     * @param predicate Predicate object.
     * @param pageable Page and sort order.
     * @param shippingDateFrom Shipping date from.
     * @param shippingDateTo Shipping date to.
     * @param printedDateFrom Printed date from.
     * @param printedDateTo Printed date to.
     * @param transferredDateFrom Transferred date from.
     * @param transferredDateTo Transferred date to.
     * @param selectionFromDD Selection from DD.
     * @return The list of certificates.
     */
    public Page<CertificateDTO> findAllDeleted(Predicate predicate, Pageable pageable,
                                        Instant shippingDateFrom, Instant shippingDateTo,
                                        Instant printedDateFrom, Instant printedDateTo,
                                        Instant transferredDateFrom, Instant transferredDateTo,
                                        Instant statusChangeDateFrom, Instant statusChangeDateTo,
                                        String selectionFromDD) {
        log.info(LOG_PREFIX + "Find all deleted certificates...");
        BooleanBuilder finalPredicate = manipulatePredicate(predicate, shippingDateFrom, shippingDateTo, printedDateFrom, printedDateTo, transferredDateFrom, transferredDateTo, statusChangeDateFrom, statusChangeDateTo);
        // Adds filtering in the results according to logged-in user.
        UserDetailDTO dto = securityService.getLoggedInUserDetailDTO();

        // Company user filtered by the selected company
        if (dto.getUserType().getId().equals(UserType.COMPANY_USER.toString()) && selectionFromDD != null && !selectionFromDD.isEmpty()) {
                finalPredicate.and(Q_CERTIFICATE.company.id.eq(selectionFromDD));
                finalPredicate.and(Q_CERTIFICATE.status.eq(CertificateStatus.DELETED));
        }
        log.info(LOG_PREFIX + "All deleted certificates found.");
        return findAll(finalPredicate, pageable);
    }

    /**
     * Creates a certificate using the provided certificateDTO and associated files.
     *
     * @param certificateDTO    The CertificateDTO containing certificate information.
     * @param request           The MultipartHttpServletRequest containing file attachments.
     */
    public CertificateDTO createCertificate(CertificateDTO certificateDTO, MultipartHttpServletRequest request) {
        log.info(LOG_PREFIX + "Create certificate...");
        CertificateDocumentsDTO certificateDocumentsDTO =  certificateUpdateService.updateCertificateDocuments(request, null);
        CertificateDTO savedCertificateDTO = certificateUpdateService.saveCertificate(certificateDTO,certificateDocumentsDTO);
        // Send email to interested users
        emailNotificationService.notifyAssignedUsers(savedCertificateDTO, null);

        return savedCertificateDTO;
    }

    /**
     * Updates a certificate using the provided certificateDTO, associated files, and documents mapping.
     *
     * @param certificateDTO    The CertificateDTO containing certificate information.
     * @param request           The MultipartHttpServletRequest containing file attachments and parameter mapping.
     */
    public CertificateDTO updateCertificate(CertificateDTO certificateDTO, MultipartHttpServletRequest request) {
        if (!certificateValidator.validateRequest(certificateDTO, certificateDTO.getResourceId(), securityService.getLoggedInUserDetailDTO().getUserType().getId())) {
            log.info("Entity with id : {} has no rights to edit certificate with id {}.",
                    certificateDTO.getResourceId(),
                    certificateDTO.getId());
            throw new NotAllowedException("Certificate cannot be edited.");
        }
        List<EcertBYErrorException> errors = new ArrayList<>();
        certificateValidator.validateIsEmployeeActive(certificateDTO, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for saving certificate.", new EcertBYGeneralException(errors));
        }
        log.info(LOG_PREFIX + "Update certificate...");
        // Send email to interested users
        Certificate oldCertificate = findEntityById(certificateDTO.getId());
        emailNotificationService.notifyAssignedUsers(certificateDTO, oldCertificate);

        CertificateDocumentsDTO certificateDocumentsDTO =  certificateUpdateService.updateCertificateDocuments(request, certificateDTO.getId());
        return certificateUpdateService.editCertificate(certificateDTO,certificateDocumentsDTO, CertificateStatus.getPreCertificateStatuses().contains(certificateDTO.getStatus()),
            certificateDTO.getResourceId());
    }


    /**
     * Release the certificate, logging the action.
     * @param id The given certificate id.
     */
    public void release(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Release certificate...");
        // Send email to all interested users
        Certificate oldCertificate = findEntityById(id);
        CertificateAssignmentHistory assignmentHistory = oldCertificate.getAssignmentHistory();
        if (assignmentHistory != null) {
            emailNotificationService.notifyCompanyUsersOnRelease(id, oldCertificate.getCompany().getId(), assignmentHistory.getId());
        } else {
            emailNotificationService.notifyCompanyUsersOnRelease(id, oldCertificate.getCompany().getId(), "");
        }

        Certificate certificate = genericAction(id, CertificateStatus.RELEASED);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.RELEASED, certificate, certificateRepository.findByParentCertificateId(id));
        // Create task
        taskService.saveReleaseCertificateTask(certificateMapperInstance.map(certificate), authorityService.findById(selectionFromDD));
        // Log release
        auditService.saveReleaseCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully released by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Reject the certificate, logging the action.
     * @param id The given certificate id.
     */
    public void reject(String id, String reason, String selectionFromDD) {
        log.info(LOG_PREFIX + "Reject certificate...");
        // Send email to all interested users
        Certificate oldCertificate = findEntityById(id);
        CertificateAssignmentHistory assignmentHistory = oldCertificate.getAssignmentHistory();
        if (assignmentHistory != null) {
            emailNotificationService.notifyCompanyUsersOnCertificateReject(id, oldCertificate.getCompany().getId(), assignmentHistory.getId());
        } else {
            emailNotificationService.notifyCompanyUsersOnCertificateReject(id, oldCertificate.getCompany().getId(), "");
        }

        Certificate certificate = genericAction(id, CertificateStatus.REJECTED_CERTIFICATE, reason);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.REJECTED_CERTIFICATE, certificate, certificateRepository.findByParentCertificateId(id));
        // Create task
        taskService.saveRejectCertificateTask(certificateMapperInstance.map(certificate), authorityService.findById(selectionFromDD), reason);
        // Log reject
        auditService.saveRejectCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), reason, selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully rejected by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Mark the certificate as lost, logging the action.
     * @param id The given certificate id.
     */
    public void markAsLost(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Mark as lost certificate...");
        Certificate certificate = genericAction(id, CertificateStatus.LOST);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.LOST, certificate, null);
        // Create task
        taskService.saveMarkCertificateAsLostTask(certificateMapperInstance.map(certificate), authorityService.findById(selectionFromDD));
        // Log mark as lost
        auditService.saveMarkCertificateAsLostAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully marked as lost by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Revoke the certificate, logging the action.
     * @param id The given certificate id.
     */
    public void revoke(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Revoke certificate...");
        Certificate certificate = genericAction(id, CertificateStatus.REVOKED);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.REVOKED, certificate, null);
        // Create task
        taskService.saveRevokeCertificateTask(certificateMapperInstance.map(certificate), authorityService.findById(selectionFromDD));
        // Log revoke
        auditService.saveRevokeCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully revoked by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Block the certificate, logging the action.
     * @param id The given certificate id.
     */
    public void block(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Block certificate...");
        Certificate certificate = genericAction(id, CertificateStatus.BLOCKED);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.BLOCKED, certificate, certificateRepository.findByParentCertificateId(id));
        // Create task
        taskService.saveBlockCertificateTask(certificateMapperInstance.map(certificate),authorityService.findById(selectionFromDD));
        // Log block
        auditService.saveBlockCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully blocked by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Mark the certificate as deleted, logging the action.
     * @param id The given certificate id.
     */
    public void delete(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Delete certificate...");
        Certificate certificate = genericAction(id, CertificateStatus.DELETED);
        // Also delete all its precertificates
        for (String precertificateId : certificateRepository.findIdByParentCertificateId(id)) {
            genericAction(precertificateId, CertificateStatus.PRE_CERTIFICATE_DELETED);
        }
        // Log delete
        auditService.saveAuditForEntity(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully deleted by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Votes positive the pre-certificate, logging the action.
     * @param id The given pre-certificate id.
     */
    public void votePositive(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Vote positive certificate...");
        // Send email to all interested users
        Certificate oldCertificate = findEntityById(id);
        CertificateAssignmentHistory assignmentHistory = oldCertificate.getParentCertificate().getAssignmentHistory();
        if (assignmentHistory != null) {
            emailNotificationService.notifyCompanyUsersOnVotePositive(id, oldCertificate.getParentCertificate().getId(), oldCertificate.getCompany().getId(), assignmentHistory.getId());
        } else {
            emailNotificationService.notifyCompanyUsersOnVotePositive(id, oldCertificate.getParentCertificate().getId(), oldCertificate.getCompany().getId(), "");
        }

        Certificate preCertificate = genericAction(id, CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE, preCertificate, null);
        // Create task
        taskService.saveVotePositivePreCertificateTask(certificateMapperInstance.map(preCertificate),authorityService.findById(selectionFromDD));

        // Find if all pre-certificates are voted positive and change the complete-forward to parent certificate.
        updateCertificateCompleteForward(preCertificate.getParentCertificate().getId(), selectionFromDD);

        // Log vote positive
        auditService.saveVotePositivePreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), preCertificate.getId(),preCertificate.getParentCertificate().getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully voted positive by user with id : {}", id, securityService.getLoggedInUserDetailId());
    }

    /**
     * Rejects a pre-certificate, logging the action.
     * @param id The given pre-certificate id.
     */
    public void rejectPreCertificate(String id, String reason, String selectionFromDD){
        log.info(LOG_PREFIX + "Reject pre-certificate...");
        // Send email to all interested users
        Certificate oldCertificate = findEntityById(id);
        CertificateAssignmentHistory assignmentHistory = oldCertificate.getParentCertificate().getAssignmentHistory();
        if (assignmentHistory != null) {
            emailNotificationService.notifyCompanyUsersOnPrertificateReject(id, oldCertificate.getParentCertificate().getId(), oldCertificate.getCompany().getId(), assignmentHistory.getId());
        } else {
            emailNotificationService.notifyCompanyUsersOnPrertificateReject(id, oldCertificate.getParentCertificate().getId(), oldCertificate.getCompany().getId(), "");
        }

        Certificate preCertificate = genericAction(id, CertificateStatus.PRE_CERTIFICATE_REJECTED, reason);
        // Update rejected pre-certificate field on parent certificate.
        updateCertificateRejectedPreCertificate(preCertificate.getParentCertificate().getId());
        // Send email to automatically reassigned users
        emailNotificationService.notifyAssignedUsers(certificateMapperInstance.map(preCertificate.getParentCertificate()), null);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.PRE_CERTIFICATE_REJECTED, preCertificate, null);
        // Create task
        taskService.saveRejectPreCertificateTask(certificateMapperInstance.map(preCertificate),authorityService.findById(selectionFromDD), reason);
        // Log rejection
        auditService.saveRejectPreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
            preCertificate.getId(),preCertificate.getParentCertificate().getId(), reason, selectionFromDD);
        log.info(LOG_PREFIX + "Pre-certificate with id : {} successfully rejected by user with id : {}", id, securityService.getLoggedInUserDetailId());
    }

    /**
     * Updates the certificate to rejected by pre-certificate.
     * @param parentCertificateId The id of certificate id. Parent of rejected pre-certificate.
     */
    private void updateCertificateRejectedPreCertificate(String parentCertificateId){
        log.info(LOG_PREFIX + "Update certificate status from rejected pre-certificate...");
        Certificate certificate = certificateRepository.findById(parentCertificateId).orElse(null);
        // If certificate is not defined as rejected by pre-certificate set it.
        if(certificate != null && !certificate.getStatus().equals(CertificateStatus.FORWARDED_PRE_CERTIFICATE_REJECTED)) {
            certificate.setStatus(CertificateStatus.FORWARDED_PRE_CERTIFICATE_REJECTED);
            certificateUpdateService.updateCertificateStatusHistory(certificate);
            // Reset values for assigned employee and teams set by company.
            if(certificate.getAssignmentHistory() != null){
                certificate.setAssignedEmployee(certificate.getAssignmentHistory().getAssignedEmployee());
                certificate.getAssignedTeamSet().clear();
                if(!certificate.getAssignmentHistory().getAssignedTeamSet().isEmpty()){
                    certificate.getAssignmentHistory().getAssignedTeamSet().forEach(assignedTeam ->{
                        CertificateTeam certificateTeam = new CertificateTeam();
                        certificateTeam.setTeam(assignedTeam.getTeam());
                        certificate.getAssignedTeamSet().add(certificateTeam);
                    });
                    certificateAssignmentHistoryRepository.deleteById(certificate.getAssignmentHistory().getId());
                    certificate.setAssignmentHistory(null);
                }
            }

            certificateMapperInstance.map(certificateRepository.save(certificate));
            log.info(LOG_PREFIX + "certificate with id : {} successfully updated by user with id : {}", parentCertificateId, securityService.getLoggedInUserDetailId());
        }
    }

    /**
     * Update parent certificate complete forward if all pre-certificates are not forwarded or rejected.
     * @param parentCertificateId The parent certificate id.
     */
    private void updateCertificateCompleteForward(String parentCertificateId, String selectionFromDD){
        log.info(LOG_PREFIX + "Check if certificate has pre-certificates with status PRE_CERTIFICATE_FORWARDED or PRE_CERTIFICATE_REJECTED...");
        // Check if there is pre-certificate with parent id with status PRE_CERTIFICATE_FORWARDED or PRE_CERTIFICATE_REJECTED
        List<String> forwardedPreCertificate = new JPAQueryFactory((entityManager)).select(Q_CERTIFICATE.id).from(Q_CERTIFICATE)
                .where(Q_CERTIFICATE.parentCertificate.id.eq(parentCertificateId).and(Q_CERTIFICATE.status.eq(CertificateStatus.PRE_CERTIFICATE_FORWARDED).or(Q_CERTIFICATE.status.eq(CertificateStatus.PRE_CERTIFICATE_REJECTED)))).fetch();
        // Update the parent certificate's complete forward to true.
        if(forwardedPreCertificate.isEmpty()){
            log.info(LOG_PREFIX + "Update certificate to forwarded...");
            Certificate certificate = certificateRepository.findById(parentCertificateId).orElse(null);
            if(certificate != null) {
                certificate.setCompletedForward(true);
                if (!CertificateStatus.FORWARDED.equals(certificate.getStatus())) {
                    certificate.setStatus(CertificateStatus.FORWARDED);
                    certificateUpdateService.updateCertificateStatusHistory(certificate);
                }
                certificateMapperInstance.map(certificateRepository.save(certificate));

                taskService.completePreviousTasks(selectionFromDD, CertificateStatus.FORWARDED, certificate, null);
                // Create task for post authority
                taskService.saveForwardCertificateCompletedTask(certificateMapperInstance.map(certificate), certificateMapperInstance.map(certificate).getCompany());
                // Send email to end authority
                emailNotificationService.notifyForwardAuthorityUsers(certificateMapperInstance.map(certificate), true);
                log.info(LOG_PREFIX + "Certificate with id {} successfully forwarded by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
            }
        }
    }

    private void updateCertificateToForward(String parentCertificateId){
        log.info(LOG_PREFIX + "Check if certificate has pre-certificates with status PRE_CERTIFICATE_REJECTED...");
        // Check if there is pre-certificate with parent id with status PRE_CERTIFICATE_REJECTED
        List<String> forwardedPreCertificate = new JPAQueryFactory((entityManager)).select(Q_CERTIFICATE.id).from(Q_CERTIFICATE)
                .where(Q_CERTIFICATE.parentCertificate.id.eq(parentCertificateId).and(Q_CERTIFICATE.status.eq(CertificateStatus.PRE_CERTIFICATE_REJECTED))).fetch();
        // Update the parent certificate's status to forward.
        if(forwardedPreCertificate.isEmpty()){
            log.info(LOG_PREFIX + "Update certificate to forwarded...");
            Certificate certificate = certificateRepository.findById(parentCertificateId).orElse(null);
            if(certificate != null) {
                certificate.setStatus(CertificateStatus.FORWARDED);
                certificateUpdateService.updateCertificateStatusHistory(certificate);
                if(!certificate.getAssignedTeamSet().isEmpty() || certificate.getAssignedEmployee() != null) {
                    CertificateAssignmentHistory history = new CertificateAssignmentHistory();
                    history.setAssignedEmployee(certificate.getAssignedEmployee());
                    history.setAssignedTeamSet(new HashSet<>());
                    certificate.getAssignedTeamSet().forEach(team -> {
                        CertificateAssignmentHistoryTeam historyTeam = new CertificateAssignmentHistoryTeam();
                        historyTeam.setTeam(team.getTeam());
                        history.getAssignedTeamSet().add(historyTeam);
                    });
                    certificate.setAssignmentHistory(certificateAssignmentHistoryRepository.save(history));
                }
                certificate.getAssignedTeamSet().clear();
                certificate.setAssignedEmployee(null);
                certificateMapperInstance.map(certificateRepository.save(certificate));
                log.info(LOG_PREFIX + "Certificate with id {} successfully forwarded by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
            }
        }
    }
    /**
     * Forwards a pre-certificate, logging the action.
     * @param id The given pre-certificate id.
     */
    public void forwardPreCertificate(String id, String selectionFromDD){
        log.info(LOG_PREFIX + "Forward pre-certificate...");
        Certificate preCertificate = genericAction(id, CertificateStatus.PRE_CERTIFICATE_FORWARDED);
        updateCertificateToForward(preCertificate.getParentCertificate().getId());
        // Send email to precertificate forward authority
        emailNotificationService.notifyForwardAuthorityUsers(certificateMapperInstance.map(preCertificate), false);
        // Send email to automatically reassigned users
        emailNotificationService.notifyAssignedUsers(certificateMapperInstance.map(preCertificate), null);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.PRE_CERTIFICATE_FORWARDED, preCertificate, null);
        // Create task
        taskService.saveForwardPreCertificateTask(certificateMapperInstance.map(preCertificate),companyService.findById(selectionFromDD));
        // Log Forward
        auditService.saveForwardPreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), preCertificate.getId(),preCertificate.getParentCertificate().getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Pre-certificate with id {} successfully forwarded by user with id : {}", preCertificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Excludes a pre-certificate, logging the action.
     * @param id The given pre-certificate id.
     */
    public void excludePreCertificate(String id, String selectionFromDD){
        log.info(LOG_PREFIX + "Exclude pre-certificate...");
        Certificate preCertificate = genericAction(id, CertificateStatus.PRE_CERTIFICATE_EXCLUDED);

        taskService.completePreviousTasks(selectionFromDD, CertificateStatus.PRE_CERTIFICATE_EXCLUDED, preCertificate, null);
        // Create task
        taskService.saveExcludePreCertificateTask(certificateMapperInstance.map(preCertificate),companyService.findById(selectionFromDD));

        // Update rejected pre-certificate field on parent certificate
        updateCertificateCompleteForward(preCertificate.getParentCertificate().getId(), selectionFromDD);
        // Update parent certificate if the other pre-certificates are no rejected.
        updateCertificateToForward(preCertificate.getParentCertificate().getId());

        // Log exclusion
        auditService.saveExcludePreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), preCertificate.getId(),preCertificate.getParentCertificate().getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Pre-certificate with id {} successfully excluded by user with id : {}", preCertificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Restore a deleted certificate, logging the action.
     * @param id The given certificate id.
     */
    public void restoreDraft(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Restore certificate...");
        Certificate certificate = genericAction(id, CertificateStatus.DRAFT);
        // Also restore all its pre-certificates
        for (String preCertificateId : certificateRepository.findIdByParentCertificateId(id)) {
            genericAction(preCertificateId, CertificateStatus.PRE_CERTIFICATE_DRAFT);
        }

        // Log restoration
        CompanyDTO actingCompany = companyService.findById(selectionFromDD);
        auditService.saveRestoreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificate.getId(), actingCompany.getName(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully restored by user with id : {}", certificate.getId(), securityService.getLoggedInUserDetailId());
    }

    /**
     * Handles rejection related actions, saving the updated certificate.
     * @param id The certificate's id.
     * @param status The status of the certificate after successful update.
     * @return The certificate entity.
     */
    private Certificate genericAction(String id, CertificateStatus status, String reason) {
        CertificateDTO certificateDTO = findById(id);
        certificateDTO.setReason(reason);
        certificateValidator.validateStatusTransition(certificateDTO,status,true);
        log.info(LOG_PREFIX + "Perform change of status and update fields according the status for an item...");
        updateFieldsBasedOnStatus(status, certificateDTO);

        return certificateRepository.save(certificateMapperInstance.map(certificateDTO));
    }

    /**
     * Handles all status-change related actions, saving the updated certificate.
     * @param id The certificate's id.
     * @param status The status of the certificate after successful update.
     * @return The certificate entity.
     */
    private Certificate genericAction(String id, CertificateStatus status) {
        CertificateDTO certificateDTO = findById(id);

        log.info(LOG_PREFIX + "Perform change of status and update fields according the status for an item...");
        certificateValidator.validateStatusTransition(certificateDTO,status, false);
        updateFieldsBasedOnStatus(status, certificateDTO);

        Certificate certificate = certificateRepository.save(certificateMapperInstance.map(certificateDTO));
        log.info(LOG_PREFIX + "Generic action for item with id {} successfully performed by user with id : {}", certificateDTO.getId(), securityService.getLoggedInUserDetailId());
        return certificate;
    }

    private CertificateAssignmentHistoryDTO saveCertificateAssignmentHistory(CertificateDTO certificateDTO){
        // Check if already exists something in db if yes remove it.
        if(certificateDTO.getAssignmentHistory() != null){
            certificateAssignmentHistoryRepository.deleteById(certificateDTO.getAssignmentHistory().getId());
        }
        if(certificateDTO.getAssignedEmployee() != null || !certificateDTO.getAssignedTeamSet().isEmpty()){
            CertificateAssignmentHistoryDTO certAssignmentHistoryDTO = new CertificateAssignmentHistoryDTO();
            certAssignmentHistoryDTO.setAssignedEmployee(certificateDTO.getAssignedEmployee());
            certAssignmentHistoryDTO.setAssignedTeamSet(certificateDTO.getAssignedTeamSet());
            CertificateAssignmentHistory certificateAssignmentHistory = certificateAssignmentHistoryRepository.save(certificateAssignmentHistoryMapper.map(certAssignmentHistoryDTO));
            return certificateAssignmentHistoryMapper.map(certificateAssignmentHistory);
        }
        return null;
    }

    private void updateFieldsBasedOnStatus(CertificateStatus status, CertificateDTO certificateDTO) {
        certificateDTO.setStatus(status);
        certificateUpdateService.updateCertificateStatusHistory(certificateDTO);

        // On release set completedForward to null.
        if (status.equals(CertificateStatus.RELEASED)){
            certificateDTO.setCompletedForward(null);
            saveHistoryAndClearCertificateFields(certificateDTO);
        // On forward actions update last forward date and clear rejection reason.
        } else if (status.equals(CertificateStatus.FORWARDED) || status.equals(CertificateStatus.PRE_CERTIFICATE_FORWARDED)) {
            certificateDTO.setForwardDate(Instant.now());
            certificateDTO.setPreCertificateActionOn(null);
            certificateDTO.setPreCertificateActionBy(null);
            certificateDTO.setReason(null);
        // On block set completedForward to null and update the closing date
        } else if (status.equals(CertificateStatus.BLOCKED)) {
            certificateDTO.setCompletedForward(null);
            certificateDTO.setClosingDate(Instant.now());
            saveHistoryAndClearCertificateFields(certificateDTO);
        // On lost and revoked update the closing date
        } else if (status.equals(CertificateStatus.LOST) || status.equals(CertificateStatus.REVOKED)) {
            certificateDTO.setClosingDate(Instant.now());
        }
        // In reject certificate set the complete forward to false.
        if(status == CertificateStatus.REJECTED_CERTIFICATE) {
            certificateDTO.setCompletedForward(false);
            saveHistoryAndClearCertificateFields(certificateDTO);
        }
        // when deleting a certificate, add date for recycle bin checks
        if (status.equals(CertificateStatus.DELETED)) certificateDTO.setAnnulmentDate(Instant.now());
        else if (status.equals(CertificateStatus.DRAFT)) certificateDTO.setAnnulmentDate(null);
        if(status.equals(CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE)
                || status.equals(CertificateStatus.PRE_CERTIFICATE_REJECTED)
                || status.equals(CertificateStatus.PRE_CERTIFICATE_EXCLUDED) ){
            certificateDTO.setPreCertificateActionOn(Instant.now());
            certificateDTO.setPreCertificateActionBy(securityService.getLoggedInUserDetailDTO());
        }
        // Clear the assigned employee and teams for certificate for pre-certificate.
        if(status.equals(CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE)
                || status.equals(CertificateStatus.PRE_CERTIFICATE_EXCLUDED) ){
            certificateDTO.setAssignedEmployee(null);
            certificateDTO.setAssignedTeamSet(new HashSet<>());
        }
    }

    private void saveHistoryAndClearCertificateFields(CertificateDTO certificateDTO) {
        certificateDTO.setAssignmentHistory(saveCertificateAssignmentHistory(certificateDTO));
        certificateDTO.setAssignedEmployee(null);
        certificateDTO.setAssignedTeamSet(new HashSet<>());
    }

    /**
     * Get certificate with files.
     * @param id certificate id.
     * @return The requested certificate.
     */
    public CertificateDTO getCertificateWithFiles(String id, String selectionFromDD) {
        CertificateDTO certificateDTO = findById(id);
        // On statuses rejected, released, blocked, revoked and lost check if form will display the history employee and teams.
        if(certificateDTO.getStatus().equals(CertificateStatus.REJECTED_CERTIFICATE) || certificateDTO.getStatus().equals(CertificateStatus.RELEASED) ||
                certificateDTO.getStatus().equals(CertificateStatus.BLOCKED) || certificateDTO.getStatus().equals(CertificateStatus.REVOKED) ||
                certificateDTO.getStatus().equals(CertificateStatus.LOST)){
            // If user is authority or admin display the history data on fields employee and teams.
            UserDetailDTO dto = securityService.getLoggedInUserDetailDTO();
            if ((dto.getUserType().getId().equals(UserType.AUTHORITY_USER.toString()) ||  dto.getUserType().getId().equals(UserType.ADMIN_USER.toString()))
                    && certificateDTO.getAssignmentHistory() != null){
                certificateDTO.setAssignedEmployee(certificateDTO.getAssignmentHistory().getAssignedEmployee());
                certificateDTO.setAssignedTeamSet(certificateDTO.getAssignmentHistory().getAssignedTeamSet());
            }

        }
        if (!certificateValidator.validateRequest(certificateDTO, selectionFromDD, securityService.getLoggedInUserDetailDTO().getUserType().getId())) {
            log.info("Entity with id : {} has no rights to view certificate with id {}.",
                    selectionFromDD,
                    id);
            throw new NotAllowedException("Certificate cannot be viewed.");
        }
        return certificateDTO;
    }

    /**
     * Return the keywords associated with a certificate.
     * @param certificateId The id of the certificate.
     * @return The keywords as a list of OptionDTOs.
     */
    public List<OptionDTO> getCertificateKeywordsById(String certificateId) {
        List<CatalogValue> results = certificateRepository.findCertificateKeywordsById(certificateId)
                .stream()
                .map(CertificateKeyword::getKeyword)
                .toList();
        return catalogValueMapperInstance.mapToListOptionDTO(results);
    }
    /**
     * Forwards a certificate to another post authority.
     * @param id The id of certificate.
     * @param certificateForwardAuthority The authority and reason for certificate.
     */

    public void forwardToAnotherAuthority(String id, CertificateAuthorityToAuthorityForwardDTO certificateForwardAuthority ,boolean isPreCert) {
        AuthorityDTO actingAuthority = authorityService.findById(certificateForwardAuthority.getResourceId());
        CertificateDTO certificateDTO = findById(id);
        AuthorityDTO oldForwardAuthority = certificateDTO.getForwardAuthority();
        certificateDTO.setForwardDate(Instant.now());
        certificateDTO.setForwardAuthority(certificateForwardAuthority.getAuthority());
        certificateDTO.setSigningEmployee(null);
        certificateDTO.setAssignmentHistory(null);
        certificateDTO.setAssignedEmployee(null);
        certificateDTO.setAssignedTeamSet(new HashSet<>());
        if(!isPreCert) {
            certificateDTO.setPrintedDate(null);
            certificateDTO.setTransferredDate(null);
            certificateDTO.setSecurityPaper(false);
            certificateDTO.setPaperNumbers(null);
        }
        Certificate certificate = certificateRepository.save(certificateMapperInstance.map(certificateDTO));
        certificateUpdateService.updateCertificateStatusHistory(certificate);
        if(certificate.getAssignmentHistory() != null){
            certificateAssignmentHistoryRepository.deleteById(certificateDTO.getAssignmentHistory().getId());
        }

        // Complete old authority's forward task
        taskService.automaticCompleteAllWithConditions(oldForwardAuthority.getId(), id, TaskType.AUTHORITY, List.of(CertificateStatus.FORWARDED , CertificateStatus.PRE_CERTIFICATE_FORWARDED));
        taskService.saveAuthorityForwardCertificateTask(certificateDTO, certificateDTO.getCompany() , actingAuthority,certificateForwardAuthority.getAuthority(), certificateForwardAuthority.getReason() ,isPreCert);
        auditService.saveAuthorityForwardCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificateDTO.getId(), actingAuthority, certificateForwardAuthority.getAuthority(),
                certificateForwardAuthority.getReason(), certificateForwardAuthority.getResourceId() ,isPreCert);

    }

    /**
     * Forwards a certificate to the post authority.
     * @param id The id of certificate.
     * @param certificateForwardAuthorityDTO The pre-authorities and post-authority for certificate.
     */
    public void forward(String id, CertificateForwardAuthorityDTO certificateForwardAuthorityDTO) {
        CompanyDTO actingCompany = companyService.findById(certificateForwardAuthorityDTO.getResourceId());
        CertificateDTO certificateDTO = findById(id);
        certificateDTO.setForwardDate(Instant.now());
        certificateDTO.setStatus(CertificateStatus.FORWARDED);
        certificateUpdateService.updateCertificateStatusHistory(certificateDTO);
        // Keep the values of current assigned employee and teams in history table.
        saveHistoryAndClearCertificateFields(certificateDTO);
        // After forwarding, any prior rejection reasons should be cleared
        certificateDTO.setReason(null);

        // Case 1: The pre authority is equal to post authority. Update the existing certificate.
        if(certificateForwardAuthorityDTO.getPreAuthorityList().size() == 1 &&
                certificateForwardAuthorityDTO.getPreAuthorityList().iterator().next().getId().equals(certificateForwardAuthorityDTO.getPostAuthority().getId())){
            certificateDTO.setForwardAuthority(certificateForwardAuthorityDTO.getPostAuthority());
            certificateDTO.setCompletedForward(true);
            certificateRepository.save(certificateMapperInstance.map(certificateDTO));
            // Create task
            taskService.saveForwardCertificateTask(certificateDTO, actingCompany);
            // Log forward
            auditService.saveForwardCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificateDTO.getId(), certificateForwardAuthorityDTO.getResourceId());
            // Send email to certificate forward authority
            emailNotificationService.notifyForwardAuthorityUsers(certificateDTO, true);
            return;
        }

        // Case 2: The pre-authority/ pre-authorities is different to post authority. Update the existing certificate.
        List<Certificate> preCertificates = certificateRepository.findByParentCertificateId(id);
        for (int i = 0; i < preCertificates.size(); i++) {
            preCertificates.get(i).setStatus(CertificateStatus.PRE_CERTIFICATE_FORWARDED);
            certificateUpdateService.updateCertificateStatusHistory(preCertificates.get(i));
            preCertificates.get(i).setForwardDate(Instant.now());
            preCertificates.get(i).setPreCertificateActionBy(null);
            preCertificates.get(i).setPreCertificateActionOn(null);
            Certificate savedPreCertificate = certificateRepository.save(preCertificates.get(i));
            // Create task
            taskService.saveForwardPreCertificateTask(certificateMapperInstance.map(savedPreCertificate), actingCompany);
            // Log forward
            auditService.saveForwardCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), preCertificates.get(i).getId(), certificateForwardAuthorityDTO.getResourceId());
        }

        // Check if pre-authorities exists as pre-certificates otherwise create it.
        // In this case it means that the selected pre-authority does not have a pre-certificate on the parent-certificate.
        List<AuthorityDTO> nonPreCertificateAuthority = certificateForwardAuthorityDTO.getPreAuthorityList().stream()
                .filter(authorityDTO ->
                        preCertificates.stream()
                                .map(preCertificate -> preCertificate.getForwardAuthority())
                                .noneMatch(forwardAuthority -> Objects.equals(forwardAuthority.getId(), authorityDTO.getId())))
                .collect(Collectors.toList());

        for(int i = 0; i < nonPreCertificateAuthority.size(); i++){
            CertificateDTO savedPreCertificate = certificateUpdateService.createPreCertificate(nonPreCertificateAuthority.get(i).getId(), certificateDTO, CertificateStatus.PRE_CERTIFICATE_FORWARDED);
            log.info(LOG_PREFIX + "Pre-certificate with id : {} created by user with id : {}", savedPreCertificate.getId(), securityService.getLoggedInUserDetailId());
            fileService.getFolderId(savedPreCertificate.getId(), fileService.getFolderId(certificateDTO.getId(), null));
            // Create task
            taskService.saveForwardPreCertificateTask(savedPreCertificate, actingCompany);
            // Log forward
            auditService.saveCreatePreCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedPreCertificate.getId(),savedPreCertificate.getParentCertificate().getId(),
                certificateForwardAuthorityDTO.getResourceId());
            // Send email to precertificate forward authority
            emailNotificationService.notifyForwardAuthorityUsers(savedPreCertificate, false);
        }

        // In all cases the forward authority in parent-certificate is the post authority.
        certificateDTO.setForwardAuthority(certificateForwardAuthorityDTO.getPostAuthority());
        // Case 2 and case 3: Update the completedForward to false.
        certificateDTO.setCompletedForward(false);
        // Send email to end authority to inform them that the precertification process started
        emailNotificationService.notifyForwardAuthorityUsers(certificateDTO, false);
        // Create the general post authority task to inform them that the precertificates process started
        taskService.saveForwardCertificateStartedTask(certificateDTO, actingCompany);
        // Save the parent-certificate to get the changes did in case 2 and 3 case.
        certificateRepository.save(certificateMapperInstance.map(certificateDTO));
        auditService.saveForwardCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificateDTO.getId(), certificateForwardAuthorityDTO.getResourceId());
    }

    /**
     * Gets certificate signing employee by id.
     * @param certificateId Certificate id.
     * @return The Signing employees.
     */
    public OptionDTO getCertificateSigningEmployeeById(String certificateId) {
        UserDetail signingEmployee = this.certificateRepository.findCertificateSigningEmployeeById(certificateId);
        if (signingEmployee != null) {
            return userMapperInstance.mapToOptionDTO(signingEmployee);
        }
        return null;
    }

    /**
     * Clears deleted certificates
     */
    public void clearDeletedCertificates() {
        List<String> certificateIdList;
        BooleanBuilder predicate = new BooleanBuilder();
        JPAQuery<String> factory = new JPAQueryFactory((entityManager)).select(Q_CERTIFICATE.id).from(Q_CERTIFICATE);


        predicate.and(Q_CERTIFICATE.status.eq(CertificateStatus.DELETED))
                // ChronoUnits.MONTHS is not supported by Instant
                .and(Q_CERTIFICATE.annulmentDate.loe(Instant.now().minus(recycleBinDaysToMaintain, ChronoUnit.DAYS)));

        certificateIdList = factory.where(predicate).fetch();
        for (String id : certificateIdList) {
            // delete files
            String folderId = fileService.findParentNodeIdByDocumentID(id, null);
            if (folderId != null) {
                documentService.deleteFolder(folderId, null);
            }
            // Delete all associated pre-certificates and their files first
            deletedByParentCertificateId(id);
            // Remove references from copies of the soon-to-be deleted certificate
            this.certificateRepository.updateReferenceCertificate(id);
        }
        this.deleteByIdIn(certificateIdList);
    }

    /**
     * Deletes all associated pre-certificates and their files first
     * @param parentId The parent id.
     */
    private void deletedByParentCertificateId(String parentId) {
        for (String precertificateId : this.certificateRepository.findIdByParentCertificateId(parentId)) {
            String folderId = fileService.findParentNodeIdByDocumentID(precertificateId, null);
            if (folderId != null) {
                documentService.deleteFolder(folderId, null);
            }
            this.deleteById(precertificateId);
        }
    }

    private BooleanBuilder manipulatePredicate(Predicate predicate,
                                               Instant shippingDateFrom, Instant shippingDateTo,
                                               Instant printedDateFrom, Instant printedDateTo,
                                               Instant transferredDateFrom, Instant transferredDateTo,
                                               Instant statusChangeDateFrom, Instant statusChangeDateTo) {
        BooleanBuilder finalPredicate = new BooleanBuilder().and(predicate);

        // Shipping date criteria.
        if (shippingDateFrom != null) finalPredicate.and(Q_CERTIFICATE.shippingDate.goe(shippingDateFrom));
        if (shippingDateTo != null) finalPredicate.and(Q_CERTIFICATE.shippingDate.loe(shippingDateTo).or(Q_CERTIFICATE.transferredDate.isNull()));

        // Printed date criteria.
        if (printedDateFrom != null) finalPredicate.and(Q_CERTIFICATE.printedDate.goe(printedDateFrom));
        if (printedDateTo != null) finalPredicate.and(Q_CERTIFICATE.printedDate.loe(printedDateTo).or(Q_CERTIFICATE.transferredDate.isNull()));

        // Transferred date criteria.
        if (transferredDateFrom != null) finalPredicate.and(Q_CERTIFICATE.transferredDate.goe(transferredDateFrom));
        if (transferredDateTo != null) finalPredicate.and(Q_CERTIFICATE.transferredDate.loe(transferredDateTo).or(Q_CERTIFICATE.transferredDate.isNull()));

        // Status change date criteria.
        if(statusChangeDateFrom != null) finalPredicate.and(Q_CERTIFICATE.statusHistorySet.any().modifiedDate.after(statusChangeDateFrom));
        if(statusChangeDateTo != null) finalPredicate.and(Q_CERTIFICATE.statusHistorySet.any().modifiedDate.before(statusChangeDateTo));

        return finalPredicate;
    }

    /**
     * Gets forwarded certificates.
     * @param userAuthorities User authorities.
     * @return The requested certificates.
     */
    public List<CertificateDTO> getForwardedCertificates(List<String> userAuthorities) {
        List<CertificateDTO> forwardedCertificates = new ArrayList<>();
        for (String id : userAuthorities) {
            List<Certificate> certificatesForAuthority = certificateRepository.findCertificateByStatusAndForwardedAuthority(CertificateStatus.FORWARDED, id);

            for (Certificate certificate : certificatesForAuthority) {
                CertificateDTO certificateDTO = certificateMapperInstance.map(certificate);
                // Set the authority ID in the DTO
                certificateDTO.setResourceId(id);
                forwardedCertificates.add(certificateDTO);
            }
        }

        return forwardedCertificates;
    }

    /**
     * saves offline docs and links them to certificates.
     * @param additionalDocuments additional Docs saved offline.
     * @param request The request containing the actual files.
     */
    public Map<String, Boolean> saveAdditionalDocumentsFromOffline(List<DocumentDTO> additionalDocuments, MultipartHttpServletRequest request, String selectionFromDD) {
        List<MultipartFile> files = request.getFiles(FileUploadType.CERTIFICATE_ADDITIONAL_DOCS.getValue());
        Map<String, Boolean> fileStatus = new HashMap<>();
        for (int i = 0 ; i < additionalDocuments.size() ; i++) {
            DocumentDTO document = additionalDocuments.get(i);
            String certificateId = document.getCertificateId();
            try {
                String folder = fileService.getFolderId(certificateId, null);
                DocumentDTO dto= fileService.createNewFileAndVersion(files.get(i), document.getNotes(), document.getEditedFilename(), document.getCertificateId(), document.getType(), folder);
                fileStatus.put(document.getEditedFilename(), dto == null);
                if(dto != null) {
                    auditService.saveFileUploadCreateAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                            files.get(i).getOriginalFilename(), certificateId, "certificate_file" + "_created", selectionFromDD);
                }
            } catch (Exception ex) {
                emailService.sendFileUploadFailedEmail(certificateId,securityService.getLoggedInUserDetailDTO().getEmail());
            }
        }
        return fileStatus;
    }

    public CertificateDTO copy(String certificateId, String selectionFromDD) {
        CertificateDTO originalCertificateDTO = findById(certificateId);
        if (!certificateValidator.validateCopyRequest(originalCertificateDTO, selectionFromDD, securityService.getLoggedInUserDetailDTO().getUserType().getId())) {
            log.info("Entity with id : {} has no rights to copy certificate with id {}.",
                    originalCertificateDTO.getResourceId(),
                    originalCertificateDTO.getId());
            throw new NotAllowedException("Certificate cannot be copied.");
        }

        Certificate originalCertificate = certificateMapperInstance.map(originalCertificateDTO);
        // Copy certificate fields
        String[] excludedProperties = new String[]{
                "id", "forwardDate", "completedForward", "reason", "forwardAuthority",
                "preCertificateActionOn", "preCertificateActionBy", "closingDate",
                "assignedTeamSet", "departmentSet", "keywordSet",
                "assignmentHistory", "statusHistorySet"
        };
        Certificate copiedCertificate = new Certificate();
        BeanUtils.copyProperties(originalCertificate, copiedCertificate, excludedProperties);
        copiedCertificate.setReferenceCertificate(originalCertificate);
        copiedCertificate.setCreationDate(Instant.now());
        copiedCertificate.setStatus(CertificateStatus.DRAFT);
        certificateUpdateService.updateCertificateStatusHistory(copiedCertificate);
        
        setCopiedSets(copiedCertificate, originalCertificate);
        Certificate savedCertificate = this.certificateRepository.save(copiedCertificate);
        // Copy additional and external files
        String folderId = fileService.createCertificateParentFolder(savedCertificate.getId(), null);
        List<DocumentDTO> additionalList = Optional.ofNullable(this.fileService.getDocumentsByType(originalCertificate.getId(), FileType.ADDITIONAL_DOCUMENT, false)).orElse(Page.empty()).stream().toList();
        for (DocumentDTO document : additionalList) {
            this.fileService.copyFileAndVersionFromDocument(folderId, savedCertificate.getId(), document, FileType.ADDITIONAL_DOCUMENT);
        }
        List<DocumentDTO> externalList = Optional.ofNullable(this.fileService.getDocumentsByType(originalCertificate.getId(), FileType.EXTERNAL_PRE_CERTIFICATE, false)).orElse(Page.empty()).stream().toList();
        for (DocumentDTO document : externalList) {
            this.fileService.copyFileAndVersionFromDocument(folderId, savedCertificate.getId(), document, FileType.EXTERNAL_PRE_CERTIFICATE);
        }

        auditService.saveCopyCertificateAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), certificateId, savedCertificate.getId(), selectionFromDD);
        log.info(LOG_PREFIX + "Certificate with id {} successfully copied by user with id : {}", originalCertificate.getId(), securityService.getLoggedInUserDetailId());

        return certificateMapperInstance.map(savedCertificate);
    }

    private void setCopiedSets(Certificate copiedCertificate, Certificate originalCertificate) {
        Set<CertificateTeam> copiedAssignedTeamSet = new HashSet<>();
        for (CertificateTeam team : originalCertificate.getAssignedTeamSet()) {
            CertificateTeam copiedTeam = new CertificateTeam();
            BeanUtils.copyProperties(team, copiedTeam, "id");
            copiedAssignedTeamSet.add(copiedTeam);
        }
        copiedCertificate.setAssignedTeamSet(copiedAssignedTeamSet);

        Set<CertificateDepartment> departmentSet = new HashSet<>();
        for (CertificateDepartment department : originalCertificate.getDepartmentSet()) {
            CertificateDepartment copiedDepartment = new CertificateDepartment();
            BeanUtils.copyProperties(department, copiedDepartment, "id");
            departmentSet.add(copiedDepartment);
        }
        copiedCertificate.setDepartmentSet(departmentSet);

        Set<CertificateKeyword> keywordSet = new HashSet<>();
        for (CertificateKeyword keyword : originalCertificate.getKeywordSet()) {
            CertificateKeyword copiedKeyword = new CertificateKeyword();
            BeanUtils.copyProperties(keyword, copiedKeyword, "id");
            keywordSet.add(copiedKeyword);
        }
        copiedCertificate.setKeywordSet(keywordSet);

        Set<CertificatePreAuthority> preAuthoritySet = new HashSet<>();
        for (CertificatePreAuthority preAuthority : originalCertificate.getPreAuthoritySet()) {
            CertificatePreAuthority copiedPreAuthority = new CertificatePreAuthority();
            BeanUtils.copyProperties(preAuthority, copiedPreAuthority, "id");
            preAuthoritySet.add(copiedPreAuthority);
        }
        copiedCertificate.setPreAuthoritySet(preAuthoritySet);
    }
}
