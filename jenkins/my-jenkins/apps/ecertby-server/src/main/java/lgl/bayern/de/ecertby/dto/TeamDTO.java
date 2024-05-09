package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.SortedSet;
import lgl.bayern.de.ecertby.annotation.AuditFirstNameLastName;
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
public class TeamDTO extends BaseDTO implements ComparableDTO {
    @NotNull
    private String name;

    private CompanyDTO company;
    private AuthorityDTO authority;

    @Valid
    @NotEmpty
    private SortedSet<OptionDTO> department;

    @Valid
    @NotEmpty
    @AuditFirstNameLastName
    @AuditTranslationKey(key = "team_users")
    private Set<OptionDTO> userDetailSet;

    @Valid
    @NotEmpty
    @AuditIgnore
    private Set<OptionDTO> userTeamSet;

    // Required by Qlack
    @ResourceId
    protected String resourceId;
}
