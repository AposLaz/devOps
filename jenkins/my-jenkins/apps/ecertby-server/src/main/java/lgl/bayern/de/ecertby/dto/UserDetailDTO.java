package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class UserDetailDTO extends UserDetailProfileDTO implements ComparableDTO {

    private OptionDTO role;

    @AuditIgnore
    private String roleName;

    private OptionDTO roleInProcess;

    @AuditIgnore
    private String roleInProcessName;

    private Boolean mainUser;

    private String newPassword;

    private boolean active;

    private String selectionFromDD;

    @NotNull
    private OptionDTO userType;

    // Required by Qlack
    @ResourceId
    protected String resourceId;

    @AuditTranslationKey(key = "department_company_list")
    List<UserAuthorityCompanyDTO> userAuthorityCompanyDTOList;
}
