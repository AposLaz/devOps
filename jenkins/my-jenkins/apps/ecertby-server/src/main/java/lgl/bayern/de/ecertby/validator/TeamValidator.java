package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.Team;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Validator for the Team.
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class TeamValidator {

    private final TeamRepository teamRepository;

    private static final String ERROR_NAME_EXISTS_TEAM = "error_name_exists";
    private static final String ERROR_AT_LEAST_ONE_SELECTION_INACTIVE = "error_at_least_one_selection_inactive";
    public void validateTeam(TeamDTO teamDTO,  UserDetailDTO userDetailDTO) {
        List<EcertBYErrorException> errors = new ArrayList<>();

        // Validate if name already exists.
        nameExists(teamDTO, errors, userDetailDTO);

        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for Saving Team", new EcertBYGeneralException(errors));
        }
    }

    public boolean validateRequest(TeamDTO teamDTO, String actingId) {
        return (teamDTO.getAuthority() != null && teamDTO.getAuthority().getId().equals(actingId)) ||
                (teamDTO.getCompany() != null && teamDTO.getCompany().getId().equals(actingId));
    }
    public void validateIsUserActive(TeamDTO teamDTO, List<EcertBYErrorException> errors) {
        if (!teamDTO.getUserDetailSet().stream().allMatch(OptionDTO::isActive)) {
            errors.add(new EcertBYErrorException(ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, "userDetailSet", "teamDTO", null, true));
        }
    }
    public boolean validateEditRequest(TeamDTO teamDTO) {
        if (teamDTO.getId() == null) return true;
        // We have to get the team entity that is saved to the DB, as the DTO already has the new values (permitted or not)
        Optional<Team> optionalTeam = teamRepository.findById(teamDTO.getId());
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            return (savedTeam.getAuthority() != null && savedTeam.getAuthority().getId().equals(teamDTO.getResourceId())) ||
                    (savedTeam.getCompany() != null && savedTeam.getCompany().getId().equals(teamDTO.getResourceId())
            );
        }
        return false;
    }


    private void nameExists(TeamDTO teamDTO, List<EcertBYErrorException> errors,  UserDetailDTO userDetailDTO) {
        Team team = null;
        Team teamNotIn = null;
        if(userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())){
            team = teamRepository.findByNameAndCompany_Id(teamDTO.getName(), teamDTO.getResourceId());
            teamNotIn = teamRepository.findByNameAndCompany_Id_AndIdNot(teamDTO.getName(), teamDTO.getResourceId(),  teamDTO.getId());
        }else{
            team = teamRepository.findByNameAndAuthority_Id(teamDTO.getName(), teamDTO.getResourceId());
            teamNotIn = teamRepository.findByNameAndAuthority_Id_AndIdNot(teamDTO.getName(), teamDTO.getResourceId(),  teamDTO.getId());
        }
        if (teamDTO.getId() == null && team != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_TEAM, ERROR_NAME_EXISTS_TEAM, "name", "teamDTO", null, true));
        }
        if (teamDTO.getId() != null && teamNotIn != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_TEAM, ERROR_NAME_EXISTS_TEAM, "name", "teamDTO", null, true));
        }
    }
}
