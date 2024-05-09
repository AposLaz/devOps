package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ViewThreadPermissionsDTO {
    private boolean authorityVisible;
    private boolean companyVisible;
    private boolean anonymous;

}
