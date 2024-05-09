package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.SortedSet;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TemplateDTO extends BaseDTO implements ComparableDTO {
    @NotNull
    @AuditIdentifier
    @AuditTranslationKey(key = "title")
    private String templateName;
    @NotNull
    @AuditTranslationKey(key = "target_country")
    private OptionDTO targetCountry;
    @NotNull
    private OptionDTO product;
    @NotNull
    private SortedSet<OptionDTO> department;
    private SortedSet<OptionDTO> keyword;

    @NotNull
    @AuditTranslationKey(key = "valid_from")
    private Instant validFrom;
    @AuditTranslationKey(key = "valid_to")
    private Instant validTo;

    private boolean active;
    private boolean release;
    @AuditTranslationKey(key = "template_comment")
    private String comment;

    @AuditIgnore
    private Set<TemplateElementDTO> templateElementDTOSet;

    private Set<HtmlElementDTO> htmlElementDTOSet;

    private DocumentDTO templateFile;
}
