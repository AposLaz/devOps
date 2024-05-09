package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.Notification;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lgl.bayern.de.ecertby.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class NotificationValidator {
    private final NotificationRepository notificationRepository;

    private static final String ERROR_TITLE_EXISTS_NOTIFICATION = "error_template_name_exists_template";
    private static final String ERROR_OVERLAPPING_NOTIFICATION = "error_overlapping_notification";

    public void validateNotification(NotificationDTO notificationDTO) {
        List<EcertBYErrorException> errors = new ArrayList<>();

        nameExists(notificationDTO, errors);
        validateDateBetween(notificationDTO, errors);

        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for Saving Notification", new EcertBYGeneralException(errors));
        }
    }

    /**
     * Validates whether the date range of the given notification overlaps with any existing notifications.
     * If an overlap is found, appropriate errors are added to the provided list of errors.
     *
     * @param notificationDTO The notification DTO to validate.
     * @param errors          The list of errors to which any validation errors will be added.
     */
    private void validateDateBetween(NotificationDTO notificationDTO, List<EcertBYErrorException> errors) {
        Instant validFrom = notificationDTO.getValidFrom();
        Instant validTo = notificationDTO.getValidTo();
        boolean isAuthorityView = notificationDTO.isAuthorityView();
        boolean isCompanyView = notificationDTO.isCompanyView();
        ViewThreadVisibility notificationVisibility;
        if(isAuthorityView && isCompanyView) {
            notificationVisibility = ViewThreadVisibility.VISIBLE_TO_ALL;
        } else if(isAuthorityView) {
            notificationVisibility = ViewThreadVisibility.VISIBLE_TO_AUTHORITIES;
        } else {
            notificationVisibility = ViewThreadVisibility.VISIBLE_TO_COMPANIES;
        }
        // Find notifications whose date ranges overlap with the given dates
        List<Notification> overlappingNotifications;
        overlappingNotifications = notificationRepository.findOverlappingNotifications(validTo, validFrom, notificationVisibility);

        // Filter out the current notification being edited (if it's present in the overlapping notifications)
        overlappingNotifications = overlappingNotifications.stream()
                .filter(notification -> !notification.getId().equals(notificationDTO.getId()))
                .collect(Collectors.toList());

        if (!overlappingNotifications.isEmpty()) {
            errors.add(new EcertBYErrorException(ERROR_OVERLAPPING_NOTIFICATION, ERROR_OVERLAPPING_NOTIFICATION, "validFrom", "notificationDTO", null, true));
            errors.add(new EcertBYErrorException(ERROR_OVERLAPPING_NOTIFICATION, ERROR_OVERLAPPING_NOTIFICATION, "validTo", "notificationDTO", null, true));
        }
    }

    /**
     * Validates whether a notification with the same title already exists in the database.
     * If a notification with the same title exists, appropriate errors are added to the provided list of errors.
     *
     * @param notificationDTO The notification DTO to validate.
     * @param errors          The list of errors to which any validation errors will be added.
     */
    private void nameExists(NotificationDTO notificationDTO, List<EcertBYErrorException> errors) {
        if (notificationDTO.getId() == null && notificationRepository.findByTitle(notificationDTO.getTitle()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TITLE_EXISTS_NOTIFICATION, ERROR_TITLE_EXISTS_NOTIFICATION, "title", "notificationDTO", null, true));
        }
        if (notificationDTO.getId() != null && notificationRepository.findByTitleAndIdNot(notificationDTO.getTitle(), notificationDTO.getId()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TITLE_EXISTS_NOTIFICATION, ERROR_TITLE_EXISTS_NOTIFICATION, "title", "notificationDTO", null, true));
        }
    }
}
