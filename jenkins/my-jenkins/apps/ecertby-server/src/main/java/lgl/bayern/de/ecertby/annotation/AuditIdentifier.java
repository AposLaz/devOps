package lgl.bayern.de.ecertby.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation marks the attribute from which the value will be displayed in the auditing.
 * There should be ONLY ONE identifier for each class (including their super classes).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditIdentifier {}
