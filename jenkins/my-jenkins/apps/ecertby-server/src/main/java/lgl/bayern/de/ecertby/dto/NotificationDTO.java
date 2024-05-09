package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.fd.util.ThreadStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.NotificationStatus;
import lgl.bayern.de.ecertby.model.util.NotificationType;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationDTO extends BaseDTO implements ComparableDTO {

    @NotNull
    @AuditTranslationKey(key = "title")
    private String title;

    @NotNull
    @AuditTranslationKey(key = "notification_content")
    private String content;

    @NotNull
    @AuditTranslationKey(key = "valid_from")
    private Instant validFrom;

    @AuditTranslationKey(key = "valid_to")
    private Instant validTo;

    @AuditIgnore
    private boolean active;

    private NotificationType type;

    private NotificationStatus status;

    @AuditTranslationKey(key = "notification_authority_view")
    private boolean authorityView;
    @AuditTranslationKey(key = "notification_company_view")
    private boolean companyView;
    @AuditIgnore
    private String userVisibilityMask;
    @AuditIgnore
    private String pageVisibilityMask;

    @AuditTranslationKey(key ="display")
    private ViewThreadVisibility pageViewOptions;
    @AuditIgnore
    private ViewThreadVisibility userViewOptions;
}
