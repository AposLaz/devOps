package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.model.Notification;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lgl.bayern.de.ecertby.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Slf4j
@Validated
@Transactional
@RestController
@RequestMapping("notification")
@RequiredArgsConstructor
public class NotificationResource {

    private final NotificationService notificationService;


    /**
     * Get notification.
     *
     * @param id The id of notification.
     * @return The notification with the given id.
     */
    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch notification.")
    @OperationAccess(
            operations = { VIEW_NOTIFICATION }
    )
    public NotificationDTO get(@PathVariable String id) {
        return notificationService.findById(id);
    }

    /**
     * Find all notifications with some criteria.
     *
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested notifications paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve list.")
    @OperationAccess(
            operations = { VIEW_NOTIFICATION_LIST }
    )
    public Page<NotificationDTO> findAll(@QuerydslPredicate(root = Notification.class) Predicate predicate,

                                     Pageable pageable) {
        return notificationService.findAll(predicate, pageable);
    }

    /**
     * Find all published notifications.
     *
     * @return The requested notifications
     */
    @GetMapping(path ="/getNotification/{display}" ,produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve list.")
    public NotificationDTO findPublished(@PathVariable ViewThreadVisibility display) {
        return notificationService.findCurrentPublishedNotification(display);
    }

    /**
     * Save the notification.
     *
     * @param notificationDTO The object with the information with the notification.
     * @return The saved notification.
     */
    @PostMapping(path = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { NEW_NOTIFICATION }
    )
    public ResponseEntity create(@Valid @RequestBody NotificationDTO notificationDTO) {
        return ResponseEntity.ok(this.notificationService.saveNotification(notificationDTO));
    }


    /**
     * Save the notification.
     *
     * @param notificationDTO The object with the information with the notification.
     * @return The saved notification.
     */
    @PostMapping(path = "update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { EDIT_NOTIFICATION }
    )
    public ResponseEntity update(@Valid @RequestBody NotificationDTO notificationDTO) {
        return ResponseEntity.ok(this.notificationService.saveNotification(notificationDTO));
    }

    /**
     * Activate notification.
     * @param id The id of the notification to activate.
     * @param isActive Defines if notification will activate or deactivate.
     * @return True if flow completed successfully. False in any other case.
     */
    @PatchMapping(path = "{id}/activate/{isActive}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not activate or deactivate notification.")
    @OperationAccess(
            operations = { ACTIVATE_NOTIFICATION }
    )
    public boolean activate(@PathVariable String id, @PathVariable boolean isActive) {
        return notificationService.activateNotification(isActive, id);
    }

    /**
     * Release notification.
     * @param id The id of the template to notification.
     * @return True if flow completed successfully. False in any other case.
     */
    @PatchMapping(path = "{id}/publish",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not release notification.")
    @OperationAccess(
            operations = {PUBLISH_NOTIFICATION }
    )
    public NotificationDTO publish(@PathVariable String id) {
        return notificationService.publishNotification(id);
    }

    /**
     * Delete notification.
     * @param id The id of the template to notification.
     * @return True if flow completed successfully. False in any other case.
     */
    @DeleteMapping(path = "{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete notification.")
    @OperationAccess(
            operations = { DELETE_NOTIFICATION }
    )
    public void delete(@PathVariable String id, @ResourceId String selectionFromDD) {
        notificationService.deleteNotification(id);
    }

}
