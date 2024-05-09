package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ElementType {
    TEXT_FIELD,
    TEXT_AREA,
    DATE,
    NUMBER,
    CHECKBOX,
    RADIO_GROUP,
    DROPDOWN;

    /**
     * Gets all the values of enum to a list of String
     *
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(ElementType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
