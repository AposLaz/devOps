package lgl.bayern.de.ecertby.dto;

import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.TaskAction;
import lgl.bayern.de.ecertby.model.util.TaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TaskDTO extends BaseDTO {

    private String description;

    private Instant createdOn;

    private CompanyDTO company;

    private AuthorityDTO authority;

    private CertificateDTO certificate;
    private String certificateCompanyNumber;
    private CertificateStatus certificateStatus;

    private String info;
    private String reason;

    private TaskType type;
    private TaskAction action;

    private boolean completed;
}
