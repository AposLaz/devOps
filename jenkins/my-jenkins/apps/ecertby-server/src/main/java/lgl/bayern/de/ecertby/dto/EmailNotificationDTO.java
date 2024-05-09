package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import lgl.bayern.de.ecertby.model.util.EmailNotificationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class EmailNotificationDTO extends BaseDTO {
    private List<EmailNotificationType> emailNotificationList;

    // Required by Qlack
    @ResourceId
    private String resourceId;
}