package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AuthorityDepartmentDTO extends BaseDTO {

    private AuthorityDTO authority;

    private CatalogValueDTO department;
}
