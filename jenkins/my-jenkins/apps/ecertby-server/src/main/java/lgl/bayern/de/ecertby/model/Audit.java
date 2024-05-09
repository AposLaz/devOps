package lgl.bayern.de.ecertby.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.AuditType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Audit extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type" , nullable= false)
    private AuditType auditType;

    @Formula("case type when 'AUTHORITY' then '"+ AppConstants.EnumTranslations.AUTHORITY
            +"' when 'COMPANY' then '"+ AppConstants.EnumTranslations.COMPANY
            +"' when 'USER' then '"+ AppConstants.EnumTranslations.USER
            +"' when 'TEAM' then '"+ AppConstants.EnumTranslations.TEAM
            +"' when 'CERTIFICATE' then '"+ AppConstants.EnumTranslations.CERTIFICATE
            +"' when 'TEMPLATE' then '"+ AppConstants.EnumTranslations.TEMPLATE
            +"' end")
    private String auditTypeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "action" , nullable= false)
    private AuditAction auditAction;

    @Formula("case action when 'CREATE' then '"+ AppConstants.EnumTranslations.CREATE
            +"' when 'UPDATE' then '"+ AppConstants.EnumTranslations.UPDATE
            +"' when 'ACTIVATE' then '"+ AppConstants.EnumTranslations.ACTIVATE
            +"' when 'DEACTIVATE' then '"+ AppConstants.EnumTranslations.DEACTIVATE
            +"' when 'DELETE' then '"+ AppConstants.EnumTranslations.DELETE
            +"' when 'ACCOUNT_UPDATE' then '"+ AppConstants.EnumTranslations.ACCOUNT_UPDATE
            +"' end")
    private String auditActionText;

    private Instant createdOn;

    @NotNull
    private String detail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_DETAIL", nullable = false)
    private UserDetail userDetail;
    @ManyToOne
    @JoinColumn(name = "FK_USER_AUTHORITY")
    private Authority userAuthority;
    @ManyToOne
    @JoinColumn(name = "FK_USER_COMPANY")
    private Company userCompany;
    private String firstName;
    private String lastName;

    @Column(name="ENTITY_ID", nullable = true)
    private String entityId;

}
