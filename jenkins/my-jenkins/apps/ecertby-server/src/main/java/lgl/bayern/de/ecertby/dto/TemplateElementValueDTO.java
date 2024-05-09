package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TemplateElementValueDTO extends BaseDTO {
    @NotNull
    @AuditIgnore
    private String value;
}
