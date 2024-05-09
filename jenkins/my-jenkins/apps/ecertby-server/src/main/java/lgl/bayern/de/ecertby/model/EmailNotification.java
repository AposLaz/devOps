package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import lgl.bayern.de.ecertby.model.util.EmailNotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EmailNotification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_DETAIL", nullable = false)
    private UserDetail userDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_TYPE" , nullable= false)
    private EmailNotificationType notificationType;
}
