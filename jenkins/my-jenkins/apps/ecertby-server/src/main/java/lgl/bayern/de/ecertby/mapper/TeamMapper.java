package lgl.bayern.de.ecertby.mapper;

import java.util.List;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.model.QTeam;
import lgl.bayern.de.ecertby.model.Team;
import lgl.bayern.de.ecertby.model.TeamDepartment;
import lgl.bayern.de.ecertby.model.UserTeam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TeamMapper extends BaseMapper<TeamDTO, Team, QTeam> {


    @Mapping(target = "userTeamSet", qualifiedByName = "OptionDTOtoUserTeam", source="userDetailSet")
    @Override
    public abstract Team map(TeamDTO teamDTO);

    @Mapping(target = "userDetailSet", qualifiedByName = "UserTeamToOptionDTO", source="userTeamSet")
    @Override
    public abstract TeamDTO map(Team team);


    @Mapping(source="optionDTO.id", target = "department.id")
    @Mapping(target = "id", ignore = true)
    public abstract TeamDepartment optionDTOToTeamDepartment(OptionDTO optionDTO);


    @Mapping(source="department.id", target = "id")
    @Mapping(source="department.data", target = "name")
    public abstract OptionDTO teamDepartmentToOptionDTO(TeamDepartment department);

    @Named("UserTeamToOptionDTO")
    @Mapping(source="userDetail.id", target = "id")
    @Mapping(source="userDetail.username", target = "name")
    @Mapping(source="userDetail.active" ,target = "active")
    public abstract OptionDTO userTeamToOptionDTO(UserTeam userTeam);

    @Named("OptionDTOtoUserTeam")
    @Mapping(source="id", target = "userDetail.id")
    @Mapping(target = "id",   ignore = true)
    public abstract UserTeam optionDTOToUserTeam(OptionDTO optionDTO);

    @Mapping(target = "active" , expression = "java(true)")
    public abstract OptionDTO mapToOptionDTO(Team team);
    public abstract List<OptionDTO> mapToListOptionDTO(List<Team> teamList);
}
