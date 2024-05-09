package lgl.bayern.de.ecertby.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.*;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.*;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.*;
import lgl.bayern.de.ecertby.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class TaskService extends BaseService<TaskDTO, Task, QTask> {
    private final EntityManager entityManager;
    private final AuditService auditService;
    private final AuthorityService authorityService;
    private final CompanyService companyService;
    private final UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    private final TaskMapper taskMapperInstance = Mappers.getMapper(TaskMapper.class);
    private final CertificateMapper certificateMapperInstance = Mappers.getMapper(CertificateMapper.class);
    private final AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);
    private final CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);

    private final TaskRepository taskRepository;
    private final CertificateRepository certificateRepository;
    private final SecurityService securityService;

    @Resource(name = "messages")
    private Map<String, String> messages;

    private static final String CREATION_LOG = "Creating new task...";

    /**
     * Return all not-completed tasks associated with a specific company.
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @param selectionFromDD The id of the company.
     */
    public Page<TaskDTO> findAllByCompany(Predicate predicate, Pageable pageable, String selectionFromDD) {
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(predicate)
                .and(QTask.task.company.id.eq(selectionFromDD)
                .and(QTask.task.type.eq(TaskType.COMPANY))
                .and(QTask.task.completed.eq(false)));

        return findAll(finalPredicate, pageable);
    }

    /**
     * Return all not-completed tasks associated with a specific authority.
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @param selectionFromDD The id of the authority.
     */
    public Page<TaskDTO> findAllByAuthority(Predicate predicate, Pageable pageable, String selectionFromDD) {
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(predicate)
                .and(QTask.task.authority.id.eq(selectionFromDD)
                .and(QTask.task.type.eq(TaskType.AUTHORITY))
                .and(QTask.task.completed.eq(false)));

        return findAll(finalPredicate, pageable);
    }

    /**
     * Marks a task as completed.
     * @param id The id of the task.
     */
    public void complete(String id, String selectionFromDD) {
        log.info(LOG_PREFIX + "Completing task...");
        TaskDTO taskDTO = findAndCompleteTask(id, selectionFromDD);

        // Log task completion
        if (UserType.COMPANY_USER.toString().equals(this.securityService.getLoggedInUserDetailDTO().getUserType().getId())) {
            log.info(LOG_PREFIX + "Task with id {} successfully completed by Company with id : {}.",
                    id,
                    selectionFromDD);
            auditService.saveCompleteTaskAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), taskDTO);

        } else {
            log.info(LOG_PREFIX + "Task with id {} successfully completed by Authority with id : {}.",
                    id,
                    selectionFromDD);
            auditService.saveCompleteTaskAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), taskDTO);

        }
    }

    private TaskDTO findAndCompleteTask(String id, String selectionFromDD) {
        TaskDTO taskDTO = findById(id);
        if (
                (taskDTO.getType().equals(TaskType.COMPANY) && !taskDTO.getCompany().getId().equals(selectionFromDD)) ||
                (taskDTO.getType().equals(TaskType.AUTHORITY) && !taskDTO.getAuthority().getId().equals(selectionFromDD))
        ) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to complete task with id {}.",
                    selectionFromDD,
                    id);
            throw new NotAllowedException("Task cannot be completed.");
        }

        taskDTO.setCompleted(true);
        taskRepository.save(taskMapperInstance.map(taskDTO));
        return taskDTO;
    }

    /**
     * Marks all task of a Company or Authority as completed.
     * @param selectionFromDD The id of the current Company or Authority .
     */
    public void completeAll(String selectionFromDD) {
        log.info(LOG_PREFIX + "Completing all Tasks...");
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        String userType = userDetailDTO.getUserType().getId();
        BooleanBuilder predicate = getTaskPredicateByUserType(selectionFromDD, userType);
        JPAQuery<String> jpaFactory = new JPAQueryFactory((entityManager)).select(QTask.task.id).from(QTask.task);

        List<String> taskIdsBySelectedResource = jpaFactory.where(predicate).fetch();
        if (!taskIdsBySelectedResource.isEmpty()) {
            taskIdsBySelectedResource.forEach(taskId -> findAndCompleteTask(taskId, selectionFromDD));
        }

        // Log task completion
        if (UserType.COMPANY_USER.toString().equals(userType)) {
            CompanyDTO companyDTO = companyService.findById(selectionFromDD);
            log.info(LOG_PREFIX + "All tasks of Company {} have been marked as completed.", companyDTO.getName());
            auditService.saveCompleteAllTasksAudit(AuditAction.UPDATE, userMapperInstance.map(userDetailDTO), AuditType.COMPANY, companyDTO.getName());
        } else {
            AuthorityDTO authorityDTO = authorityService.findById(selectionFromDD);
            log.info(LOG_PREFIX + "All tasks of Authority {} have been marked as completed.", authorityDTO.getName());
            auditService.saveCompleteAllTasksAudit(AuditAction.UPDATE, userMapperInstance.map(userDetailDTO), AuditType.AUTHORITY, authorityDTO.getName());
        }
    }

    /**
     * Completes older tasks based on new status of a certificate.
     * @param selectionFromDD The resource id of the acting entity
     * @param newStatus The new status of the certificate
     * @param certificate The certificate as an entity
     * @param preCertificates The list with the precertificates if needed, else null
     */
    public void completePreviousTasks(String selectionFromDD, CertificateStatus newStatus, Certificate certificate, List<Certificate> preCertificates) {
        String certificateId = certificate.getId();
        switch (newStatus) {
            case FORWARDED -> // Complete authority precertification tasks
                automaticCompleteAllWithConditions(certificate.getForwardAuthority().getId(), certificateId, TaskType.AUTHORITY, List.of(CertificateStatus.FORWARDED, CertificateStatus.FORWARDED_PRE_CERTIFICATE_REJECTED));
            case PRE_CERTIFICATE_FORWARDED, PRE_CERTIFICATE_EXCLUDED -> // Complete company precertificate rejected tasks
                    automaticCompleteAllWithConditions(selectionFromDD, certificateId, TaskType.COMPANY, List.of(CertificateStatus.PRE_CERTIFICATE_REJECTED));
            case RELEASED, BLOCKED, REJECTED_CERTIFICATE -> {
                // Complete authority forward tasks
                automaticCompleteAllWithConditions(selectionFromDD, certificateId, TaskType.AUTHORITY, List.of(CertificateStatus.FORWARDED));
                // Complete company tasks for forwarding to a new post authority (if there is one)
                automaticCompleteAllWithConditions(certificate.getCompany().getId(), certificateId, TaskType.COMPANY, List.of(CertificateStatus.FORWARDED));
                // Complete company precertificate voted tasks
                for (Certificate precertificate : preCertificates) {
                    // We use the precertificate company id instead of the selectionFromDD, as an authority user doesn't have the right to complete a company task
                    automaticCompleteAllWithConditions(precertificate.getCompany().getId(), precertificate.getId(), TaskType.COMPANY, List.of(CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE));
                }
            }
            case LOST, REVOKED -> // Complete company certificate released tasks
                automaticCompleteAllWithConditions(certificate.getCompany().getId(), certificateId, TaskType.COMPANY, List.of(CertificateStatus.RELEASED));
            case PRE_CERTIFICATE_VOTE_POSITIVE, PRE_CERTIFICATE_REJECTED -> // Complete authority pre forward tasks
                automaticCompleteAllWithConditions(selectionFromDD, certificateId, TaskType.AUTHORITY, List.of(CertificateStatus.PRE_CERTIFICATE_FORWARDED));
            default -> {
                // Base
            }
        }

    }

    /**
     * Marks all tasks satisfying certain conditions as completed.
     * @param selectionFromDD The resource id of the acting entity
     * @param certificateId The id of the to-be-completed task's certificate
     * @param type The type of the to-be-completed task (COMPANY/AUTHORITY)
     * @param statuses The list of all to-be-completed task statuses
     */
    public void automaticCompleteAllWithConditions(String selectionFromDD, String certificateId, TaskType type, List<CertificateStatus> statuses) {
        JPAQuery<String> jpaFactory = new JPAQueryFactory((entityManager)).select(QTask.task.id).from(QTask.task);
        BooleanBuilder predicate = new BooleanBuilder()
                .and(QTask.task.type.eq(type))
                .and(QTask.task.certificateStatus.in(statuses))
                .and(QTask.task.certificate.id.eq(certificateId))
                .andNot(QTask.task.completed);
        List<String> taskIdsBySelectedResource = jpaFactory.where(predicate).fetch();
        if (!taskIdsBySelectedResource.isEmpty()) {
            taskIdsBySelectedResource.forEach(taskId -> {
                findAndCompleteTask(taskId, selectionFromDD);
                log.info(LOG_PREFIX + "Task with id {} was automatically completed.",
                        taskId);
                auditService.saveAutomaticCompleteTaskAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), findById(taskId));
            });
        }

    }

    private BooleanBuilder getTaskPredicateByUserType(String selectionFromDD, String userType) {
        BooleanBuilder predicate = new BooleanBuilder()
                .and(QTask.task.authority.id.eq(selectionFromDD)
                    .or(QTask.task.company.id.eq(selectionFromDD)))
                .and((QTask.task.completed).not());

        if(userType.equals(UserType.COMPANY_USER.toString()) ){
            predicate.and(QTask.task.type.eq(TaskType.COMPANY));
        } else if (userType.equals(UserType.AUTHORITY_USER.toString())) {
            predicate.and(QTask.task.type.eq(TaskType.AUTHORITY));
        }
        return predicate;
    }

    /**
     * Create task for certificate rejection.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the rejection.
     * @param reason The reason of the rejection.
     */
    public void saveRejectCertificateTask(CertificateDTO certificate, AuthorityDTO authority, String reason) {
        saveCompanyRejectTask(certificate, authority, "task_reject_info", reason);
    }

    /**
     * Create task for certificate blocking.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the blocking.
     */
    public void saveBlockCertificateTask(CertificateDTO certificate, AuthorityDTO authority) {
        saveCompanyTask(certificate, authority, "task_block_info");
    }

    /**
     * Create task for certificate release.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the release.
     */
    public void saveReleaseCertificateTask(CertificateDTO certificate, AuthorityDTO authority) {
        saveCompanyTask(certificate, authority, "task_release_info");
    }

    /**
     * Create task for certificate revoke.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the revoke.
     */
    public void saveRevokeCertificateTask(CertificateDTO certificate, AuthorityDTO authority) {
        saveCompanyTask(certificate, authority, "task_revoke_info");
    }

    /**
     * Create task for marking the certificate as lost.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the marking as lost.
     */
    public void saveMarkCertificateAsLostTask(CertificateDTO certificate, AuthorityDTO authority) {
        saveCompanyTask(certificate, authority, "task_mark_as_lost_info");
    }

    /**
     * Create task for precertificate positive voting.
     * @param preCertificate The precertificate as CertificateDTO.
     * @param authority The Authority responsible for the voting.
     */
    public void saveVotePositivePreCertificateTask(CertificateDTO preCertificate, AuthorityDTO authority) {
        saveCompanyVotePositiveTask(preCertificate, authority);
    }

    /**
     * Create task for precertificate rejection.
     * @param preCertificate The precertificate as CertificateDTO.
     * @param authority The Authority responsible for the rejection.
     * @param reason The reason of the rejection.
     */
    public void saveRejectPreCertificateTask(CertificateDTO preCertificate, AuthorityDTO authority, String reason) {
        saveCompanyRejectTask(preCertificate, authority, "task_pre_reject_info", reason);
    }

    /**
     * Create task for certificate forwarding.
     * @param certificate The certificate as CertificateDTO.
     * @param company The Company responsible for the forwarding.
     */
    public void saveForwardCertificateTask(CertificateDTO certificate, CompanyDTO company) {
        saveAuthorityForwardTask(certificate, company, "task_forward_info");
    }
    /**
     * Saves the authority forward certificate task.
     *
     * @param certificate       The certificate DTO object.
     * @param company           The company DTO object.
     * @param previousAuthority The DTO object representing the previous authority.
     * @param currentAuthority  The DTO object representing the current authority.
     * @param reason            The reason for the authority forward.
     */
    public void saveAuthorityForwardCertificateTask(CertificateDTO certificate, CompanyDTO company , AuthorityDTO previousAuthority , AuthorityDTO currentAuthority,String reason , boolean isPreCert) {
        String companyMessage;
        String authorityMessage;
        if(isPreCert) {
            companyMessage ="task_pre_authority_to_authority_forward_company_info";
            authorityMessage = "task_pre_authority_to_authority_forward_auth_info";
        } else {
            companyMessage = "task_authority_to_authority_forward_company_info";
            authorityMessage = "task_authority_to_authority_forward_auth_info";
        }
        saveAuthorityToAuthorityForwardTask(certificate, company, companyMessage,authorityMessage,previousAuthority,currentAuthority,reason);
    }
    /**
     * Create task for precertificate forwarding.
     * @param preCertificate The precertificate as CertificateDTO.
     * @param company The Company responsible for the forwarding.
     */
    public void saveForwardPreCertificateTask(CertificateDTO preCertificate, CompanyDTO company) {
        saveAuthorityPreForwardTask(preCertificate, company, "task_pre_forward_info");
    }

    /**
     * Create task for the start of certificate forwarding process, when precertificates have to be evaluated first.
     * @param certificate The certificate as CertificateDTO.
     * @param company The Company responsible for the initial forwarding.
     */
    public void saveForwardCertificateStartedTask(CertificateDTO certificate, CompanyDTO company) {
        saveAuthorityForwardStartedTask(certificate, company, "task_forward_started_info");
    }

    /**
     * Create task for the completion of certificate forwarding process.
     * @param certificate The certificate as CertificateDTO.
     * @param company The Company responsible for the initial forwarding.
     */
    public void saveForwardCertificateCompletedTask(CertificateDTO certificate, CompanyDTO company) {
        saveAuthorityForwardCompletedTask(certificate, company, "task_forward_completed_info");
    }

    /**
     * Create task for precertificate exclusion.
     * @param preCertificate The precertificate as CertificateDTO.
     * @param company The Company responsible for the exclusion.
     */
    public void saveExcludePreCertificateTask(CertificateDTO preCertificate, CompanyDTO company) {
        saveAuthorityExcludeTask(preCertificate, company, "task_pre_exclude_info");
    }

    /* PRIVATE METHODS */

    /**
     * Saves a rejection task.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the rejection.
     * @param reason The reason of rejection.
     */
    private void saveCompanyRejectTask(CertificateDTO certificate, AuthorityDTO authority, String message, String reason) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createCompanyTask(certificate, authority, message);
        task.setReason(reason);
        task.setAction(TaskAction.EDIT);

        // All company tasks of precertificates with same parent should lead to edit mode
        if (certificate.getParentCertificate() != null) {
            List<Task> relevantTasks = taskRepository.findAllPreCertificateTasksByParentIdAndType(certificate.getParentCertificate().getId(), TaskType.COMPANY);
            for (Task relevantTask : relevantTasks) {
                relevantTask.setAction(TaskAction.EDIT);
                taskRepository.save(relevantTask);
            }
        }
        // All authority tasks of same certificate should lead to view mode
        updateAllTasksActionByCertificate(certificate, TaskType.AUTHORITY, TaskAction.VIEW);

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByAuthority(authority.getId(), savedTask.getId());
    }

    /**
     * Saves a vote positive task.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the rejection.
     */
    private void saveCompanyVotePositiveTask(CertificateDTO certificate, AuthorityDTO authority) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createCompanyTask(certificate, authority, "task_pre_positive_vote_info");

        // If at least one company task of a precertificate with the same parent has edit action, set this task's action to edit as well
        if (taskRepository.existsPreCertificateTaskByParentIdAndTypeAndAction(certificate.getParentCertificate().getId(), TaskType.COMPANY, TaskAction.EDIT)) {
            task.setAction(TaskAction.EDIT);
            taskRepository.save(task);
        }
        // All authority tasks of same certificate should lead to view mode
        updateAllTasksActionByCertificate(certificate, TaskType.AUTHORITY, TaskAction.VIEW);

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByAuthority(authority.getId(), savedTask.getId());
    }

    /**
     * General method for block, revoke, mark as lost & release tasks addressed to companies.
     * @param certificate The certificate as CertificateDTO.
     * @param authority The Authority responsible for the task.
     * @param message The key of the appropriate message.
     */
    private void saveCompanyTask(CertificateDTO certificate, AuthorityDTO authority, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createCompanyTask(certificate, authority, message);
        // All authority tasks of same certificate should lead to view mode
        updateAllTasksActionByCertificate(certificate, TaskType.AUTHORITY, TaskAction.VIEW);

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByAuthority(authority.getId(), savedTask.getId());
    }

    private Task createCompanyTask(CertificateDTO certificate, AuthorityDTO authority, String message) {
        Task task = createGeneralTask(certificate);
        task.setInfo(messages.get(message).formatted(certificate.getId(), authority.getName()));
        task.setAuthority(authorityMapperInstance.map(authority));
        task.setCompany(certificateMapperInstance.map(certificate).getCompany());
        task.setType(TaskType.COMPANY);
        // Default company task action is view
        task.setAction(TaskAction.VIEW);
        task.setCompleted(false);
        return task;
    }

    private void saveAuthorityExcludeTask(CertificateDTO certificate, CompanyDTO company, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createAuthorityTask(certificate, company, message);

        // If this exclusion completes the precertificates flow, this task should lead to view mode
        if (Boolean.TRUE.equals(certificateMapperInstance.map(certificate.getParentCertificate()).getCompletedForward())) {
            task.setAction(TaskAction.VIEW);
        }

        // If no precertificate with the same parent is rejected, all company tasks of those precertificates should lead to view mode
        if (!certificateRepository.existsByParentCertificateIdAndStatus(certificate.getParentCertificate().getId(), CertificateStatus.PRE_CERTIFICATE_REJECTED)) {
            updateAllTasksActionByParentCertificate(certificate, TaskType.COMPANY, TaskAction.VIEW);
        }

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByCompany(company.getId(), savedTask.getId());
    }

    private void saveAuthorityForwardTask(CertificateDTO certificate, CompanyDTO company, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createAuthorityTask(certificate, company, message);

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByCompany(company.getId(), savedTask.getId());
    }
    private void saveAuthorityToAuthorityForwardTask(CertificateDTO certificate, CompanyDTO company, String messageCompany,String messageAuthority , AuthorityDTO previousAuthority , AuthorityDTO currentAuthority,String reason) {
        log.info(LOG_PREFIX + CREATION_LOG);
        //task for Authority
        Task authorityTask = createGeneralTask(certificate);
        authorityTask.setInfo(messages.get(messageAuthority).formatted(certificate.getId(), previousAuthority.getName(),reason));
        authorityTask.setAuthority(authorityMapperInstance.map(currentAuthority));
        authorityTask.setCompany(companyMapperInstance.map(company));
        authorityTask.setType(TaskType.AUTHORITY);
        authorityTask.setAction(TaskAction.EDIT);
        authorityTask.setCompleted(false);
        taskRepository.save(authorityTask);
        //task for Company
        Task companytask = createGeneralTask(certificate);
        companytask.setInfo(messages.get(messageCompany).formatted(certificate.getId(), previousAuthority.getName(),currentAuthority.getName(),reason));
        companytask.setCompany(companyMapperInstance.map(company));
        companytask.setAuthority(authorityMapperInstance.map(previousAuthority));
        companytask.setType(TaskType.COMPANY);
        companytask.setAction(TaskAction.EDIT);
        companytask.setCompleted(false);
        taskRepository.save(companytask);
    }

    private void saveAuthorityPreForwardTask(CertificateDTO certificate, CompanyDTO company, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createAuthorityTask(certificate, company, message);

        // If this forwarding completes the precertificates flow, this task should lead to view mode
        if (Boolean.TRUE.equals(certificateMapperInstance.map(certificate.getParentCertificate()).getCompletedForward())) {
            task.setAction(TaskAction.VIEW);
        // Otherwise all tasks of this precertificate should lead to edit mode
        } else {
            updateAllTasksActionByCertificate(certificate, TaskType.AUTHORITY, TaskAction.EDIT);
        }

        // If no precertificate with the same parent is rejected, all company tasks of those precertificates should lead to view mode
        if (!certificateRepository.existsByParentCertificateIdAndStatus(certificate.getParentCertificate().getId(), CertificateStatus.PRE_CERTIFICATE_REJECTED)) {
            updateAllTasksActionByParentCertificate(certificate, TaskType.COMPANY, TaskAction.VIEW);
        }

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByCompany(company.getId(), savedTask.getId());
    }

    private void saveAuthorityForwardStartedTask(CertificateDTO certificate, CompanyDTO company, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createAuthorityTask(certificate, company, message);
        task.setAction(TaskAction.VIEW);

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByCompany(company.getId(), savedTask.getId());
    }

    private void saveAuthorityForwardCompletedTask(CertificateDTO certificate, CompanyDTO company, String message) {
        log.info(LOG_PREFIX + CREATION_LOG);
        Task task = createAuthorityTask(certificate, company, message);

        // Precertificates flow is finished, therefore all tasks of parent certificate should lead to edit mode
        if (Boolean.TRUE.equals(certificateMapperInstance.map(certificate).getCompletedForward())) {
            updateAllTasksActionByCertificate(certificate, TaskType.AUTHORITY, TaskAction.EDIT);
        }

        Task savedTask = taskRepository.save(task);
        logTaskCreatedByCompany(company.getId(), savedTask.getId());
    }

    /**
     * General method for creating tasks addressed to authorities.
     * @param certificate The certificate as CertificateDTO.
     * @param company     The Company responsible for the task.
     * @param message     The key of the appropriate message.
     */
    private Task createAuthorityTask(CertificateDTO certificate, CompanyDTO company, String message) {
        Task task = createGeneralTask(certificate);
        task.setInfo(messages.get(message).formatted(certificate.getId(), company.getName()));
        task.setAuthority(certificateMapperInstance.map(certificate).getForwardAuthority());
        task.setCompany(companyMapperInstance.map(company));
        task.setType(TaskType.AUTHORITY);
        // Default authority task action is edit
        task.setAction(TaskAction.EDIT);
        task.setCompleted(false);
        return task;
    }

    /**
     * General method for creating the base for a task.
     *
     * @param certificateDTO The certificate as CertificateDTO.
     */
    private Task createGeneralTask(CertificateDTO certificateDTO) {
        Task task = new Task();
        Certificate certificate = certificateMapperInstance.map(certificateDTO);
        task.setDescription(messages.get("country_product").formatted(
                certificate.getTemplate().getTargetCountry().getName(),
                certificate.getTemplate().getProduct().getData()));
        task.setCreatedOn(Instant.now());
        task.setCertificate(certificate);
        task.setCertificateCompanyNumber(certificate.getCompanyNumber());
        task.setCertificateStatus(certificate.getStatus());
        // Update all tasks of current certificate in case company number changed
        updateCertificateInfoInTasks(certificate);
        return task;
    }

    private void updateCertificateInfoInTasks(Certificate certificate) {
        for (Task task : taskRepository.findAllByCertificateId(certificate.getId())) {
            task.setCertificateCompanyNumber(certificate.getCompanyNumber());
        }
    }

    private void updateAllTasksActionByCertificate(CertificateDTO certificate, TaskType type, TaskAction action) {
        for (Task relevantTask : taskRepository.findAllByCertificateIdAndType(certificate.getId(), type)) {
            relevantTask.setAction(action);
            taskRepository.save(relevantTask);
        }
    }

    private void updateAllTasksActionByParentCertificate(CertificateDTO certificate, TaskType type, TaskAction action) {
        List<Task> relevantTasks = taskRepository.findAllPreCertificateTasksByParentIdAndType(certificate.getParentCertificate().getId(), type);
        for (Task relevantTask : relevantTasks) {
            relevantTask.setAction(action);
            taskRepository.save(relevantTask);
        }
    }

    private void logTaskCreatedByCompany(String actingId, String taskId) {
        log.info(LOG_PREFIX + "Task with id {} successfully created by Company with id : {}.",
                taskId,
                actingId);
    }
    private void logTaskCreatedByAuthority(String actingId, String taskId) {
        log.info(LOG_PREFIX + "Task with id {} successfully created by Authority with id : {}.",
                taskId,
                actingId);
    }
}
