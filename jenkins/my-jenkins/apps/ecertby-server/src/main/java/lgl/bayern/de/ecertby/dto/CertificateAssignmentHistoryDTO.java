package lgl.bayern.de.ecertby.dto;

import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CertificateAssignmentHistoryDTO extends BaseDTO implements ComparableDTO {
    private Set<OptionDTO> assignedTeamSet;
    private UserDetailDTO assignedEmployee;
}
