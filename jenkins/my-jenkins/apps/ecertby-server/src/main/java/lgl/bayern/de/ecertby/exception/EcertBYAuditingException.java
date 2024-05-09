package lgl.bayern.de.ecertby.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * An exception only used by the auditing process.
 */
@Getter
@Setter
@AllArgsConstructor
public class EcertBYAuditingException extends RuntimeException{
    public EcertBYAuditingException(String message) {
        super(message);
    }
}
