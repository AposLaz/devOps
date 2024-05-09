package lgl.bayern.de.ecertby.service;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.mapper.NotificationMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Notification;
import lgl.bayern.de.ecertby.model.QNotification;
import lgl.bayern.de.ecertby.model.util.*;
import lgl.bayern.de.ecertby.repository.NotificationRepository;
import lgl.bayern.de.ecertby.validator.NotificationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class NotificationService extends BaseService<NotificationDTO, Notification, QNotification> {
    private final NotificationRepository notificationRepository;
    private final ObjectLockService objectLockService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final NotificationValidator notificationValidator;

    NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    /**
     * Save the notification.
     *
     * @param notificationDTO The given notification object.
     * @return The saved notification object.
     */
    public NotificationDTO saveNotification(NotificationDTO notificationDTO) {
        objectLockService.checkAndThrowIfLocked(notificationDTO.getId(), ObjectType.NOTIFICATION);
        notificationValidator.validateNotification(notificationDTO);
        NotificationDTO oldNotification = null;
        if (notificationDTO.getId() == null) {
            notificationDTO.setStatus(NotificationStatus.DRAFT_NOTIFICATION);
        } else {
            oldNotification = findById(notificationDTO.getId());
        }

        NotificationDTO savedNotificationDTO = notificationMapper.map(notificationRepository.save(notificationMapper.map(notificationDTO)));
        if (notificationDTO.getId() == null) {
            log.info(LOG_PREFIX + "New Notification with id {} successfully created by user with id : {}.", savedNotificationDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveNotificationAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedNotificationDTO);
        } else {
            log.info(LOG_PREFIX + "Notification with id {} successfully updated by user with id : {}.", savedNotificationDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveNotificationAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                    notificationDTO.getTitle(), oldNotification, savedNotificationDTO);
        }
        return savedNotificationDTO;
    }

    /**
     * Publish the notification, logging the action.
     *
     * @param id The given template id.
     * @return True if Publish was successful, False otherwise.
     */
    public NotificationDTO publishNotification(String id) {
        log.info(LOG_PREFIX + "Publishing notification...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.NOTIFICATION);
        NotificationDTO notificationDTO = findById(id);

        if (notificationDTO.getStatus().equals(NotificationStatus.PUBLISHED_NOTIFICATION)) {
            log.info(LOG_PREFIX + "Notification with id : {} cannot be published.", id);
            throw new NotAllowedException("Notification cannot be published.");
        }
        notificationDTO.setStatus(NotificationStatus.PUBLISHED_NOTIFICATION);
        NotificationDTO savedNotificationDTO = save(notificationDTO);
        // LOG PUBLISH
        auditService.savePublishNotificationAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), notificationDTO);
        log.info(LOG_PREFIX + "Notification with id {} successfully published by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        return savedNotificationDTO;
    }

    /**
     * Activate/Deactivate the notification, logging the action.
     *
     * @param isActive The new active state.
     * @param id       The given notification id.
     * @return True if activation/deactivation was successful, False otherwise.
     */
    public boolean activateNotification(boolean isActive, String id) {
        log.info(LOG_PREFIX + "Activate notification...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.NOTIFICATION);
        NotificationDTO notificationDTO = findById(id);

        if (isActive) {
            if (notificationDTO.isActive()) {
                log.info(LOG_PREFIX + "Notification with id : {} cannot be activated.", id);
                throw new NotAllowedException("Notification cannot be activated.");
            }
            // LOG ACTIVATION
            auditService.saveNotificationAudit(AuditAction.ACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), notificationDTO);
            log.info(LOG_PREFIX + "Notification with id {} successfully activate by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        } else {
            if (!notificationDTO.isActive()) {
                log.info(LOG_PREFIX + "Notification with id : {} cannot be deactivated.", id);
                throw new NotAllowedException("Notification cannot be deactivated.");
            }
            // LOG DEACTIVATION
            auditService.saveNotificationAudit(AuditAction.DEACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), notificationDTO);
            log.info(LOG_PREFIX + "Notification with id {} successfully deactivate by user with id : {}.", id, securityService.getLoggedInUserDetailId());
        }

        return activate(isActive, id, Notification.class);
    }

    /**
     * Deletes a notification by its ID.
     *
     * @param id The ID of the notification to be deleted.
     */
    public void deleteNotification(String id) {
        NotificationDTO notificationDTO = findById(id);
        log.info(LOG_PREFIX + "Deleting Notification...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.NOTIFICATION);
        NotificationDTO deletedNotification = deleteById(id);

        // LOG DELETION
        auditService.saveNotificationAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), notificationDTO);
        log.info(LOG_PREFIX + "Notification with id {} successfully deleted by user with id : {}",
                deletedNotification.getId(),
                securityService.getLoggedInUserDetailId());
    }

    /**
     * Retrieves the current published notification based on the specified display and the user's type.
     *
     * @param display The display type for which the notification is retrieved.
     * @return The current published notification DTO, or null if no notification is found.
     */
    public NotificationDTO findCurrentPublishedNotification(ViewThreadVisibility display) {
        UserType userType = securityService.getLoggedInUserDetail().getUserType();
        if (display.equals(ViewThreadVisibility.VISIBLE_TO_HOME_PAGE)) {
            if (userType.equals(UserType.AUTHORITY_USER)) {
                return notificationMapper.map(notificationRepository.findCurrentPublishedForHomePage(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES));
            } else if (userType.equals(UserType.COMPANY_USER)) {
                return notificationMapper.map(notificationRepository.findCurrentPublishedForHomePage(ViewThreadVisibility.VISIBLE_TO_COMPANIES));
            }
        } else if (display.equals(ViewThreadVisibility.VISIBLE_TO_ALL_PAGES)) {
            if (userType.equals(UserType.AUTHORITY_USER)) {
                return notificationMapper.map(notificationRepository.findCurrentPublishedForAllPages(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES));
            } else if (userType.equals(UserType.COMPANY_USER)) {
                return notificationMapper.map(notificationRepository.findCurrentPublishedForAllPages(ViewThreadVisibility.VISIBLE_TO_COMPANIES));
            }
        }
        return null;
    }
}
