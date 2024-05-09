package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.model.util.NotificationStatus;
import lgl.bayern.de.ecertby.model.util.NotificationType;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class NotificationTestData extends BaseTestData{

    public List<NotificationDTO> populateNotifications() {
        List<NotificationDTO> notificationList = new ArrayList<>();
        Instant startDate = Instant.now();
        for (int i = 0; i < 10;  i++) {
            Instant validFrom = startDate.plus(i, ChronoUnit.DAYS);
            Instant validTo = validFrom.plus(1, ChronoUnit.DAYS);
            NotificationDTO notificationDTO = initializeNotificationDTO("TestNotification"+(i+1), "TestNotificationContent"+(i+1),validFrom,validTo);
            notificationList.add(notificationDTO);
            startDate = validTo;
        }
        return notificationList;
    }

    @NotNull
    public NotificationDTO initializeNotificationDTO(String title, String content,Instant validFrom ,Instant validTo) {
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setTitle(title);
        notificationDTO.setContent(content);
        notificationDTO.setActive(true);
        notificationDTO.setStatus(NotificationStatus.DRAFT_NOTIFICATION);
        notificationDTO.setType(NotificationType.INFO_NOTIFICATION);
        notificationDTO.setAuthorityView(true);
        notificationDTO.setCompanyView(true);
        notificationDTO.setValidFrom(validFrom);
        notificationDTO.setValidTo(validTo);
        notificationDTO.setPageViewOptions(ViewThreadVisibility.VISIBLE_TO_ALL_PAGES);

        return notificationDTO;
    }
}
