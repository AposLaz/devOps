package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Stream;

public enum AuditType {
    AUTHORITY,
    COMPANY,
    USER,
    TEAM,
    CERTIFICATE,
    TEMPLATE,
    NOTIFICATION,
    TASK,
    SEARCH_CRITERIA,
    ATTRIBUTE,
    CATALOG;

    /**
     * Gets all the values of enum to a list of String
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(AuditType.values()).map(Enum::name).toList();
    }
}
