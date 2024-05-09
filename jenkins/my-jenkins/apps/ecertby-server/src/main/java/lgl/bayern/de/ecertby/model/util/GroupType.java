package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Stream;

public enum GroupType {
    GLOBAL,
    AUTHORITY,
    COMPANY,
    ADMIN,
    PERSONAL;

    /**
     * Gets all the values of enum to a list of String
     *
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(GroupType.values()).map(Enum::name).toList();
    }
}
