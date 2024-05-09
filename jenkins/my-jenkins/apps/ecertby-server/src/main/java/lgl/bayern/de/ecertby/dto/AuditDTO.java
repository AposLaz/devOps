package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.AuditType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AuditDTO extends BaseDTO {
    private AuditType auditType;
    private AuditAction auditAction;
    private Instant createdOn;
    @NotNull
    private String detail;
    private UserDetailDTO userDetail;
//    private CompanyDTO userCompany;
//    private AuthorityDTO userAuthority;
    private String userResource;
    private String firstName;
    private String lastName;
}
