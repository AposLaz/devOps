package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.TeamDepartmentDTO;
import lgl.bayern.de.ecertby.model.TeamDepartment;
import lgl.bayern.de.ecertby.model.QTeamDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TeamDepartmentMapper extends BaseMapper<TeamDepartmentDTO, TeamDepartment, QTeamDepartment> {
}
