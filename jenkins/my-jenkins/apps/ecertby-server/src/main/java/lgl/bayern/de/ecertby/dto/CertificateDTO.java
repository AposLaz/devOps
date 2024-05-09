package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lgl.bayern.de.ecertby.annotation.AuditFirstNameLastName;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CertificateDTO extends BaseDTO implements ComparableDTO {
    private CertificateDTO parentCertificate;
    @AuditIgnore
    private CertificateDTO referenceCertificate;
    private Instant creationDate;
    @NotNull
    private CertificateStatus status;
    private TemplateDTO template;
    private CompanyDTO company;
    @NotNull
    @AuditTranslationKey(key = "shipping_date")
    private Instant shippingDate;
    @NotNull
    @AuditTranslationKey(key = "company_number")
    private String companyNumber;
    @AuditTranslationKey(key = "issuing_authority")
    private AuthorityDTO issuingAuthority;
    @NotNull
    @AuditTranslationKey(key = "responsible_authority_fullname")
    private AuthorityDTO responsibleAuthority;
    @AuditFirstNameLastName
    @AuditTranslationKey(key = "signing_employee")
    private UserDetailDTO signingEmployee;
    private AuthorityDTO forwardAuthority;
    @AuditTranslationKey(key = "security_paper")
    private boolean securityPaper;
    @AuditTranslationKey(key = "paper_numbers")
    private String paperNumbers;
    @AuditTranslationKey(key = "printed_date")
    private Instant printedDate;
    @AuditTranslationKey(key = "transferred_date")
    private Instant transferredDate;
    private Instant forwardDate;
    @AuditIgnore
    private Instant closingDate;
    @AuditTranslationKey(key = "keyword")
    private Set<OptionDTO> keywordSet;
    @Valid
    @NotEmpty
    @AuditTranslationKey(key = "pre-authority")
    private Set<OptionDTO> preAuthoritySet;
    @Valid
    @NotEmpty
    @AuditTranslationKey(key = "department")
    private Set<OptionDTO> departmentSet;
    @AuditTranslationKey(key = "assigned_teams")
    private Set<OptionDTO> assignedTeamSet;
    @AuditFirstNameLastName
    @AuditTranslationKey(key = "assigned_employee")
    private UserDetailDTO assignedEmployee;
    private CertificateAssignmentHistoryDTO assignmentHistory;
    private Instant annulmentDate;
    private DocumentDTO certificateFile;
    private List<DocumentDTO> certificateAdditionalFiles = new ArrayList<>(0);
    private List<DocumentDTO> certificatePreCertificateFiles = new ArrayList<>(0);
    private List<DocumentDTO> certificateSupplementaryFiles = new ArrayList<>(0);
    private List<DocumentDTO>  certificateExternalPreCertificateFiles = new ArrayList<>(0);
    @ResourceId
    private String resourceId;
    // This field is used for the case 2 and 3 flow of certificate. In case 1 would be true.
    @AuditIgnore
    private Boolean completedForward;
    private String reason;

    @AuditIgnore
    private Set<CertificateStatusHistoryDTO> statusHistorySet;

    private UserDetailDTO preCertificateActionBy;

    private Instant preCertificateActionOn;
}
