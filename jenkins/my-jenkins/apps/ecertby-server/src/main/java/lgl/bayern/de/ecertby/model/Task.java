package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.TaskAction;
import lgl.bayern.de.ecertby.model.util.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

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
public class Task extends BaseEntity {

    private String description;

    @NotNull
    private Instant createdOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_COMPANY", nullable = true)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_AUTHORITY", nullable = true)
    private Authority authority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_CERTIFICATE", nullable = true)
    private Certificate certificate;
    private String certificateCompanyNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "certificateStatus" , nullable= false)
    private CertificateStatus certificateStatus;

    @Formula("case certificate_status when 'DRAFT' then '"+ AppConstants.EnumTranslations.DRAFT
            +"' when 'FORWARDED' then '"+ AppConstants.EnumTranslations.FORWARDED
            +"' when 'FORWARDED_PRE_CERTIFICATE_REJECTED' then '"+ AppConstants.EnumTranslations.FORWARDED_PRE_CERTIFICATE_REJECTED
            +"' when 'RELEASED' then '"+ AppConstants.EnumTranslations.RELEASED
            +"' when 'REJECTED_CERTIFICATE' then '"+ AppConstants.EnumTranslations.REJECTED_CERTIFICATE
            +"' when 'REVOKED' then '"+ AppConstants.EnumTranslations.REVOKED
            +"' when 'LOST' then '"+ AppConstants.EnumTranslations.LOST
            +"' when 'BLOCKED' then '"+ AppConstants.EnumTranslations.BLOCKED
            +"' when 'DELETED' then '"+ AppConstants.EnumTranslations.DELETED
            +"' when 'PRE_CERTIFICATE_DRAFT' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_DRAFT
            +"' when 'PRE_CERTIFICATE_FORWARDED' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_FORWARDED
            +"' when 'PRE_CERTIFICATE_REJECTED' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_REJECTED
            +"' when 'PRE_CERTIFICATE_EXCLUDED' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_EXCLUDED
            +"' when 'PRE_CERTIFICATE_VOTE_POSITIVE' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_VOTE_POSITIVE
            +"' when 'PRE_CERTIFICATE_DELETED' then '"+ AppConstants.EnumTranslations.PRE_CERTIFICATE_DELETED
            +"' end")
    private String certificateStatusText;

    @NotNull
    private String info;
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type" , nullable= false)
    private TaskType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private TaskAction action;

    @NotNull
    private boolean completed;
}
