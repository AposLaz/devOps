package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CatalogDTO extends BaseDTO {
    @NotNull
    private String name;
    private Instant createdOn;
    private boolean mandatory;
}
