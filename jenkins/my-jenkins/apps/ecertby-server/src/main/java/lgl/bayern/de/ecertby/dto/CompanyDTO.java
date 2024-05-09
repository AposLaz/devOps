package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.SortedSet;
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
public class CompanyDTO extends BaseDTO implements ComparableDTO {
    @NotNull
    private String name;
    @NotNull
    private String address;
    private String email;
    private String telephone;

    @AuditTranslationKey(key = "responsible_authority")
    private AuthorityDTO responsibleAuthority;

    @AuditTranslationKey(key = "pre_responsible_authority")
    private AuthorityDTO preResponsibleAuthority;

    @AuditTranslationKey(key = "post_responsible_authority")
    private AuthorityDTO postResponsibleAuthority;

    @NotNull
    private boolean active;
    @NotNull
    private boolean deleted;

    @Valid
    @NotEmpty
    private SortedSet<OptionDTO> department;

    @AuditTranslationKey(key = "registration_date")
    private Instant registrationDate;
    @AuditTranslationKey(key = "deregistration_date")
    private Instant deregistrationDate;

    private boolean emailAlreadyExists;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private SortedSet<OptionDTO> userDepartment;
    private String userRole;

    private UserGroupDTO userGroup;

    @ResourceId
    private String resourceId;

    public CompanyDTO(String id) {
        setId(id);
    }
}
