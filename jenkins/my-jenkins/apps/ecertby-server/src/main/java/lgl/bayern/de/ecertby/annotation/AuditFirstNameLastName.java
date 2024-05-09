package lgl.bayern.de.ecertby.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate any field with this annotation to point that it contains a user or a collection of users
 * that has to be audited with their first-name and last-name instead of the ID.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditFirstNameLastName {}
