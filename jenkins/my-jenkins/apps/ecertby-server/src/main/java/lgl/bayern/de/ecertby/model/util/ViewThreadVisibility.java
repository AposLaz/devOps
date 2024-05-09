package lgl.bayern.de.ecertby.model.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ViewThreadVisibility {
    VISIBLE_TO_ALL,
    VISIBLE_TO_AUTHORITIES,
    VISIBLE_TO_COMPANIES,
    ANONYMOUS,
    NOT_ANONYMOUS,
    VISIBLE_TO_HOME_PAGE,
    VISIBLE_TO_ALL_PAGES;

    public String getValue() {
        switch (this) {
            case VISIBLE_TO_ALL:
                return "0b0000";
            case VISIBLE_TO_AUTHORITIES:
                return "0b0001";
            case VISIBLE_TO_COMPANIES:
                return "0b0010";
            case ANONYMOUS:
                return "0b0011";
            case VISIBLE_TO_HOME_PAGE:
                return "0b0100";
            case VISIBLE_TO_ALL_PAGES:
                return "0b0101";
            case NOT_ANONYMOUS:
                return "0b0110";
            default:
                throw new IllegalArgumentException("Unknown visibility option");
        }
    }
    public static List<String> getEnumValues() {
        return Stream.of(ViewThreadVisibility.values()).map(Enum::name).collect(Collectors.toList());
    }
}
