package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CertificateForwardAuthorityDTO extends BaseDTO{
    @NotEmpty
    private Set<AuthorityDTO> preAuthorityList;

    @NotNull
    private AuthorityDTO postAuthority;

    @ResourceId
    private String resourceId;
}
