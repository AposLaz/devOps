package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class AuthorityDTO extends BaseDTO implements ComparableDTO {
    @AuditIdentifier
    @NotNull
    private String name;

    @AuditTranslationKey(key = "community_code")
    private String communityCode;

    private String address;

    @NotNull
    private boolean active;

    private String email;

    private String userFirstName;
    private String userLastName;
    private SortedSet<OptionDTO> userDepartment;

    @Valid
    @NotEmpty
    private SortedSet<OptionDTO> department;

    private boolean mainUserCreate;

    private boolean emailAlreadyExists;

    @AuditIgnore
    private UserGroupDTO userGroup;

    // Required by Qlack
    @ResourceId
    private String resourceId;

    public AuthorityDTO(String id) {
        setId(id);
    }
}
