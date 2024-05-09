package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserDetailJWTDTO extends BaseDTO {

    private boolean active;

    private OptionDTO userType;

    private String firstName;

    private String lastName;

    private String username;

    private BaseDTO  primaryCompany;

    private BaseDTO primaryAuthority;

}
