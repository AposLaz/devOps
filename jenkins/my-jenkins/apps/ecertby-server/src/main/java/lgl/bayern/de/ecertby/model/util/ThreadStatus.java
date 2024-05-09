package lgl.bayern.de.ecertby.model.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ThreadStatus {
    REQUESTED,
    PUBLISHED,
    REJECTED;


    public static List<String> getEnumValues() {
        return Stream.of(ThreadStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
