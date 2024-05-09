package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Font {
    //TODO Add fonts

    FONT_1,

    FONT_2,
    
    FONT_3;

    /**
     * Gets all the values of enum to a list of String
     *
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(Font.values()).map(Enum::name).collect(Collectors.toList());
    }
}
