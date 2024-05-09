package lgl.bayern.de.ecertby.model.util;

import java.util.List;
import java.util.stream.Stream;

public enum TrueFalseOption {

    YES,
    NO;

    public static List<String> getEnumValues() {
        return Stream.of(TrueFalseOption.values()).map(Enum::name).map(String::toLowerCase).toList();
    }
}
