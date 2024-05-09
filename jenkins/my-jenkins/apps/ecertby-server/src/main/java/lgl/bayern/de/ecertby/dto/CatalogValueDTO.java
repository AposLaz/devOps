package lgl.bayern.de.ecertby.dto;

import com.opencsv.bean.CsvBindByPosition;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
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
public class CatalogValueDTO extends BaseDTO implements ComparableDTO {
    @AuditTranslationKey(key = "name")
    @CsvBindByPosition(position = 1)
    private String data;
    private CatalogDTO catalog;
}
