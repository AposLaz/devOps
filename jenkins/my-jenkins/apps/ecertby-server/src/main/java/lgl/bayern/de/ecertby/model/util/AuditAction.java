package lgl.bayern.de.ecertby.model.util;


import lombok.Getter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum AuditAction {

    /**
     * Create Action.
     */
    CREATE,

    /**
     * Update Action.
     */
    UPDATE,

    /**
     * Activate Action.
     */
    ACTIVATE,

    /**
     * Deactivate Action.
     */
    DEACTIVATE,

    /**
     * Delete Action.
     */
    DELETE
   ;

    @Getter
    private static final Map<AuditAction,String> AUDIT_MESSAGES = new EnumMap<>(AuditAction.class);

    static {
        AUDIT_MESSAGES.put(CREATE, "create_message");
        AUDIT_MESSAGES.put(UPDATE,"update_message");
        AUDIT_MESSAGES.put(ACTIVATE,"activate_message");
        AUDIT_MESSAGES.put(DEACTIVATE,"deactivate_message");
        AUDIT_MESSAGES.put(DELETE,"delete_message");
    }

    /**
     * Gets all the values of enum to a list of String
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(AuditAction.values()).map(Enum::name).toList();
    }
}
