package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TeamTestData extends BaseTestData {



    @Getter
    OptionDTO product = new OptionDTO();

    @Getter
    String teamNameUpdate = "TestTeam updated";

    public List<TeamDTO> populateTeams() {
        List<TeamDTO> teamList = new ArrayList<>();


        for (int i = 0; i < 7; i++) {
            TeamDTO teamDTO = initializeTeamDTO("TestTeam " + (i + 1));
            teamList.add(teamDTO);
        }
        return teamList;
    }

    @NotNull
    public TeamDTO initializeTeamDTO(String teamName) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);
        OptionDTO userDetail = initializeOption("User1", "32d32458-bf29-416c-8b62-e99a6245d2f2",true);

        TeamDTO teamDTO = new TeamDTO();
        teamDTO.setName(teamName);
        teamDTO.setUserDetailSet(Set.of(userDetail));
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        departmentList.add(department);
        teamDTO.setDepartment(departmentList);


        return teamDTO;
    }
}
