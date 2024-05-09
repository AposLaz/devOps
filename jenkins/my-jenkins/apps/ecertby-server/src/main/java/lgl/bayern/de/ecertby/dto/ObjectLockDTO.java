package lgl.bayern.de.ecertby.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ObjectLockDTO extends BaseDTO {

    @NotNull
    private UserDetail userDetail;

    @NotNull
    private String objectId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

}
