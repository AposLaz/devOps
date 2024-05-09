package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class NotificationMapper extends BaseMapper<NotificationDTO, Notification, QNotification> {

    /**
     * Sets the user view options and page view options of the entity before mapping from NotificationDTO.
     *
     * @param entity          The Notification entity being mapped to.
     * @param notificationDTO The NotificationDTO being mapped from.
     */
    @BeforeMapping
    protected void beforeMapping(@MappingTarget Notification entity, NotificationDTO notificationDTO) {
        if (notificationDTO.isAuthorityView() && notificationDTO.isCompanyView()) {
            entity.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_ALL);
        } else if (notificationDTO.isCompanyView()) {
            entity.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_COMPANIES);
        } else {
            entity.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES);
        }
    }

    /**
     * Sets the authority view, company view, home page view, and all page view flags of the DTO after mapping from Notification entity.
     *
     * @param entity The Notification entity that has been mapped from.
     * @param dto    The NotificationDTO being mapped to.
     */
    @AfterMapping
    protected void afterMapping(Notification entity, @MappingTarget NotificationDTO dto) {
        if (entity.getUserViewOptions() != null) {
            ViewThreadVisibility userViewOptions = entity.getUserViewOptions();
            dto.setAuthorityView(userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES) || userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_ALL));
            dto.setCompanyView(userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_COMPANIES) || userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_ALL));
        }
    }

}
