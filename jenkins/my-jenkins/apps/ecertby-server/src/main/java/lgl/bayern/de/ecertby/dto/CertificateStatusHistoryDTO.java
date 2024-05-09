package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
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
public class CertificateStatusHistoryDTO extends BaseDTO implements ComparableDTO {
    @NotNull
    private Instant modifiedDate;

    @NotNull
    @AuditIdentifier
    private CertificateStatus status;
}
