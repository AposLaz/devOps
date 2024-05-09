package lgl.bayern.de.ecertby.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate and provide a different translation key in cases where the translation key is different
 * from the attribute's name.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditTranslationKey {
    String key();
}
