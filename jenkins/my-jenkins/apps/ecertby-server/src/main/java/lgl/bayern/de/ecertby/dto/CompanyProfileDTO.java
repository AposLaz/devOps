package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
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
public class CompanyProfileDTO extends BaseDTO implements ComparableDTO {

    @NotNull
    @AuditTranslationKey(key = "description")
    private String profileName;

    @NotNull
    private String address;

    private Set<OptionDTO> product;

    @AuditTranslationKey(key = "target_country")
    private Set<OptionDTO> targetCountry;

    @NotNull
    private boolean active;

    private String companyId;

    private CompanyDTO company;

    @ResourceId
    private String resourceId;
}
