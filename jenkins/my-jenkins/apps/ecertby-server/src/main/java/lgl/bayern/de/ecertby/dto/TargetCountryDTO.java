package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TargetCountryDTO extends BaseDTO {

    @NotNull
    private String name;

    @NotNull
    private String isoCode;
}
