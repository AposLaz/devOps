package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class HtmlElementRadioButtonDTO extends BaseDTO implements ComparableDTO {

    @NotNull
    private String name;

    private TemplateElementValueDTO templateElementValue;

    private int sortOrder;

}
