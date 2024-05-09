package lgl.bayern.de.ecertby.dto;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode()
public class UserAuthorityCompanyDTO {

    private String authorityCompanyId;

    @AuditIdentifier
    private String authorityCompanyName;

    private String userGroupId;

    private String roleInProcess;

    @AuditIdentifier
    private String roleInProcessName;

}
