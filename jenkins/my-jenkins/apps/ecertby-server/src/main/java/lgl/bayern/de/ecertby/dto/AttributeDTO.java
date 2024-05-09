package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AttributeDTO extends BaseDTO implements ComparableDTO {
    @NotNull
    @AuditTranslationKey(key = "name")
    private String name;
    @AuditTranslationKey(key = "attribute_elementType")
    @NotNull
    private ElementType elementType;
    @AuditTranslationKey(key = "attribute_required")
    private boolean isRequired;
    @AuditTranslationKey(key = "attribute_selectedForRelease")
    private boolean selectedForRelease;
    @AuditTranslationKey(key = "attribute_companyRelated")
    private boolean companyRelated;
    @AuditTranslationKey(key = "attribute_defaultValue")
    private String defaultValue;
    @AuditTranslationKey(key = "attribute_defaultValue")
    private String defaultTextAreaValue;
    @AuditTranslationKey(key = "attribute_dateFormat")
    private String dateFormat;
    @AuditTranslationKey(key = "attribute_decimalSeparator")
    private String decimalSeparator;
    @AuditTranslationKey(key = "attribute_decimalDigits")
    private String decimalDigits;
    @AuditTranslationKey(key = "attribute_thousandSeparator")
    private String thousandSeparator;
    @AuditTranslationKey(key = "attribute_catalog")
    private OptionDTO catalog;
    @AuditTranslationKey(key = "attribute_htmlElementName")
    @NotNull
    private String htmlElementName;
    @AuditTranslationKey(key = "RADIO_GROUP")
    private List<OptionDTO> radioOptionList;

}
