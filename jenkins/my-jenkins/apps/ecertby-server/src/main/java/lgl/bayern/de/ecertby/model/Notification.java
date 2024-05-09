package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.model.util.NotificationStatus;
import lgl.bayern.de.ecertby.model.util.NotificationType;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Notification extends BaseEntity {
    @NotNull
    private String title;

    @NotNull
    private String content;

    private Instant validFrom;

    private Instant validTo;

    @NotNull
    private boolean active;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private ViewThreadVisibility pageViewOptions;
    @Enumerated(EnumType.STRING)
    private ViewThreadVisibility userViewOptions;
}
