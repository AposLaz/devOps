package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ResetPasswordDTO {
    private String currentPassword;

    private String newPassword;

    private String otp;

    // Required by Qlack
    @ResourceId
    private String resourceId;
}
