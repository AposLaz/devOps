package lgl.bayern.de.ecertby.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate any field with this annotation to be ignored by the auditing comparison process.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditIgnore {}
