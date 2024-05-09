package lgl.bayern.de.ecertby.dto;

import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SearchCriteriaDTO extends BaseDTO implements ComparableDTO {

    @NotNull
    private String name;

    @NotNull
    @AuditIgnore
    private String criteria;

    @NotNull
    @AuditIgnore
    private Set<GroupType> searchCriteriaGroupDTOSet;

    @AuditIgnore
    private GroupType createdByGroupType;
    @AuditIgnore
    private String createdBy;

}
