package lgl.bayern.de.ecertby.model.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum NotificationStatus {
    DRAFT_NOTIFICATION,
    PUBLISHED_NOTIFICATION;


    public static List<String> getEnumValues() {
        return Stream.of(NotificationStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
