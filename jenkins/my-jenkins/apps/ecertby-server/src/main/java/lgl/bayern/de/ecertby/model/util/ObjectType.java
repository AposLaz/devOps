package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ObjectType {

    /**
     * The certificate.
     */
    CERTIFICATE,

    /**
     * The template.
     */
    TEMPLATE,

    /**
     * The company.
     */
    COMPANY,

    /**
     * The user.
     */
    USER,

    /**
     * The authority.
     */
    AUTHORITY,

    /**
     * The company profile.
     */
    COMPANY_PROFILE,

    /**
     * The team.
     */
    TEAM ,
    /**
     * The Notification.
     */
    NOTIFICATION ,
    /**
     * The Search Criteria.
     */
    SEARCH_CRITERIA,

    /**
     * The attribute.
     */
    ATTRIBUTE,

    /**
     * The catalog value
     */
    CATALOG_VALUE;


    /**
     * Gets all the values of enum to a list of String
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(ObjectType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
