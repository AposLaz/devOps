package lgl.bayern.de.ecertby.model.util;

import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum NotificationType {
    ERROR_NOTIFICATION,
    INFO_NOTIFICATION,
    WARNING_NOTIFICATION;

    public static List<String> getEnumValues() {
        return Stream.of(NotificationType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
