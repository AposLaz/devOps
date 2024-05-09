package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.model.Team;

import lgl.bayern.de.ecertby.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;


@Validated
@RestController
@RequestMapping("team")
@RequiredArgsConstructor
@Transactional
public class TeamResource {
    private final TeamService teamService;


    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve teams list.")
    @ResourceAccess(operations = { VIEW_TEAMS_LIST })
    public Page<TeamDTO> findAll(@QuerydslPredicate(root = Team.class) Predicate predicate,
                                 Pageable pageable,@ResourceId String selectionFromDD) {
        return teamService.findAll(predicate, pageable,selectionFromDD);
    }

    @PostMapping(path = "create" , consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(operations = { NEW_TEAM, EDIT_TEAM })
    public void save(  @RequestBody TeamDTO teamDTO) {
        teamService.saveTeam(teamDTO);
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch team.")
    @ResourceAccess(operations = { NEW_TEAM, EDIT_TEAM, VIEW_TEAM})
    public TeamDTO get(@PathVariable String id,@ResourceId String selectionFromDD) {
        return teamService.getTeam(id, selectionFromDD);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete team.")
    @ResourceAccess(
            operations = { DELETE_TEAM }
    )
    public void delete(@PathVariable String id, @ResourceId String selectionFromDD) {
        teamService.deleteTeam(id, selectionFromDD);
    }

    @GetMapping(path = "findByCompany/{companyId}",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve team list for company.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public List<OptionDTO> getTeamsByCompanyId(@PathVariable String companyId, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return teamService.findByCompanyId(companyId);
    }

    @GetMapping(path = "findByAuthority/{authorityId}",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve team list for authority.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public List<OptionDTO> getTeamsByAuthorityId(@PathVariable String authorityId, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return teamService.findByAuthorityId(authorityId);
    }


    @PostMapping(path = "existsCertificateTeamForTeam", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch user.")
//    @ResourceAccess(
//            operations = { NEW_COMPANY, EDIT_COMPANY, NEW_AUTHORITY, EDIT_AUTHORITY, NEW_USER, EDIT_USER }
//    )
    public boolean existsCertificateTeamForTeam(@Valid @RequestBody String teamId) {
        return   teamService.existsCertificateTeamForTeam(teamId);
    }
}
