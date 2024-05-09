package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CompanyProfileCountryDTO extends BaseDTO {
    private CompanyProfileDTO companyProfile;
    private TargetCountryDTO targetCountry;
}
