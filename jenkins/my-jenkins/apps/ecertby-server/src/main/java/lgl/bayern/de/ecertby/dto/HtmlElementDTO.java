package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lgl.bayern.de.ecertby.model.util.Font;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class HtmlElementDTO extends BaseDTO implements ComparableDTO {

    @NotNull
    private String name;

    private String tooltip;

    private TemplateDTO template;

    private TemplateElementDTO templateElement;

    private CatalogDTO catalog;

    private AttributeDTO attribute;

    private String defaultValue;

    private int sortOrder;

    private int maxChars;

    private String dateFormat;

    private String decimalSeparator;

    private String decimalDigits;

    private String thousandSeparator;

    private boolean required;

    private boolean bold;

    private boolean italics;

    private boolean underline;

    private boolean visible;

    private String fontSize;

    private Font font;

    private String layout;

    private boolean companyRelated;

    private boolean selectedForRelease;

    private Set<HtmlElementRadioButtonDTO> radioButtons;

    private ElementType elementType;

}
