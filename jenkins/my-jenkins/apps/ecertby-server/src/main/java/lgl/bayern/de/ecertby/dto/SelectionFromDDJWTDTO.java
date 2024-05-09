package lgl.bayern.de.ecertby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SelectionFromDDJWTDTO  extends BaseDTO{

    private boolean active;

    private boolean deleted;

    private String message;
}
