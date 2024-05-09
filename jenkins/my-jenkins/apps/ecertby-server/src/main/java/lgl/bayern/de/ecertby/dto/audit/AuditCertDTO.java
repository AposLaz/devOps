package lgl.bayern.de.ecertby.dto.audit;

import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A DTO only used for auditing of certificates
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class AuditCertDTO implements ComparableDTO {

    @AuditIgnore
    String loggingId;

    String authority;

    @AuditTranslationKey(key = "name")
    String fileName;
}
