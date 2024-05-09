package lgl.bayern.de.ecertby.model.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum UserType {

    /**
     * The System Administrator.
     */
    ADMIN_USER,

    /**
     * The Authority User.
     */
    AUTHORITY_USER,

    /**
     * The Company User.
     */
    COMPANY_USER;

    /**
     * Gets all the values of enum to a list of String
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(UserType.values()).map(Enum::name).collect(Collectors.toList());
    }

    /**
     * Returns the enumeration of a string value
     * @return UserType enum
     */
    public static UserType getEnum(String value) {
        Optional<UserType> userType = Arrays.stream(UserType.values()).filter(o -> o.toString().equals(value)).findFirst();
        return userType.orElse(null);
    }

}
