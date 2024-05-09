package lgl.bayern.de.ecertby.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import java.time.Instant;
import java.util.Set;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Certificate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_PARENT_CERTIFICATE")
    private Certificate parentCertificate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REFERENCE_CERTIFICATE", nullable = false)
    private Certificate referenceCertificate;
    private Instant creationDate;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable= false)
    private CertificateStatus status;

    @Formula("case status when 'DRAFT' then '"+ AppConstants.EnumTranslations.DRAFT
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
    private String statusText;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_TEMPLATE", nullable = false)
    private Template template;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_COMPANY", nullable = false)
    private Company company;
    @NotNull
    private Instant shippingDate;
    private String companyNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ISSUING_AUTHORITY")
    private Authority issuingAuthority;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_RESPONSIBLE_AUTHORITY")
    private Authority responsibleAuthority;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SIGNING_EMPLOYEE")
    private UserDetail signingEmployee;
    // If the parentCertificate is null forwardAuthority is the post authority, otherwise, forwardAuthority is the pre authority.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_FORWARD_AUTHORITY")
    private Authority forwardAuthority;
    private boolean securityPaper;
    private String paperNumbers;
    private Instant printedDate;
    private Instant transferredDate;
    private Instant forwardDate;
    private Instant closingDate;
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_CERTIFICATE", nullable = false)
    private Set<CertificatePreAuthority> preAuthoritySet;
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_CERTIFICATE_KEYWORD", nullable = false)
    private Set<CertificateKeyword> keywordSet;
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_CERTIFICATE_DEPARTMENT", nullable = false)
    private Set<CertificateDepartment> departmentSet;
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_CERTIFICATE_TEAM", nullable = false)
    private Set<CertificateTeam> assignedTeamSet;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ASSIGNED_EMPLOYEE")
    private UserDetail assignedEmployee;
    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "FK_ASSIGNMENT_HISTORY")
    private CertificateAssignmentHistory assignmentHistory;
    // This field is used for the case 2 and 3 flow of certificate. In case 1 would be true.
    private Instant annulmentDate;
    // This field is used for the case 2 and 3 flow of certificate. In case 1 must be null.
    private Boolean completedForward;
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_PRE_CERTIFICATE_ACTION_BY")
    private UserDetail preCertificateActionBy;

    private Instant preCertificateActionOn;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name="FK_CERTIFICATE", nullable = false)
    private Set<CertificateStatusHistory> statusHistorySet;

}
