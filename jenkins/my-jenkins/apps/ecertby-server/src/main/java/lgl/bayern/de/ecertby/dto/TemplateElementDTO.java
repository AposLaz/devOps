package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.model.util.PDFElementTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TemplateElementDTO extends BaseDTO {

    @NotNull
    @AuditIgnore
    private String name;

    @NotNull
    @AuditIgnore
    private PDFElementTypeEnum elementType;

    @AuditIgnore
    private Set<TemplateElementValueDTO> templateElementValueDTOSet;

}
