package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserHasOperationCustomDTO extends BaseDTO {

    String resourceId;
    String operationName;

}
