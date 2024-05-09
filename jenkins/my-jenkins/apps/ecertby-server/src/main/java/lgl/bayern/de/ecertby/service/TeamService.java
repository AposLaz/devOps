package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.TeamMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.CertificateTeam;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.QTeam;
import lgl.bayern.de.ecertby.model.Team;
import lgl.bayern.de.ecertby.model.UserTeam;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.AuthorityRepository;
import lgl.bayern.de.ecertby.repository.CertificateTeamRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lgl.bayern.de.ecertby.repository.TeamRepository;
import lgl.bayern.de.ecertby.validator.TeamValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class TeamService extends BaseService<TeamDTO, Team, QTeam> {

    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    TeamMapper teamMapper = Mappers.getMapper(TeamMapper.class);

    private final SecurityService securityService;
    private final AuditService auditService;
    private final ObjectLockService objectLockService;
    private final EmailService emailService;
    private final UserDetailService userDetailService;

    private final TeamRepository teamRepository;
    private final AuthorityRepository authorityRepository;
    private final CompanyRepository companyRepository;
    private final CertificateTeamRepository certificateTeamRepository;

    private final TeamValidator teamValidator;

    private static final QTeam Q_TEAM = QTeam.team;

    public TeamDTO getTeam(String teamId, String actingId) {
        TeamDTO teamDTO = findById(teamId);
        if (!teamValidator.validateRequest(teamDTO, actingId)) {
            log.info(LOG_PREFIX + "Entity with id : {} has no rights to view team with id {}.",
                    teamDTO.getResourceId(),
                    teamDTO.getId());
            throw new NotAllowedException("Team cannot be viewed.");
        }
        return teamDTO;
    }

    public TeamDTO saveTeam(TeamDTO teamDTO) {
        if (!teamValidator.validateEditRequest(teamDTO)) {
            log.info(LOG_PREFIX + "Entity with id : {} has no rights to edit team with id {}.",
                    teamDTO.getResourceId(),
                    teamDTO.getId());
            throw new NotAllowedException("Team cannot be edited.");
        }
        List<EcertBYErrorException> errors = new ArrayList<>();
        teamValidator.validateIsUserActive(teamDTO, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for saving team.", new EcertBYGeneralException(errors));
        }
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        log.info(LOG_PREFIX + "Validating team info...");
        teamValidator.validateTeam(teamDTO, userDetailDTO);

        Team newTeam = teamMapper.map(teamDTO);
        if (teamDTO.getAuthority() != null) {
            Authority authority = authorityRepository.findById(teamDTO.getAuthority().getId()).orElse(null);
            newTeam.setAuthority(authority);
        }
        if (teamDTO.getCompany() != null) {
            Company company = companyRepository.findById(teamDTO.getCompany().getId()).orElse(null);
            newTeam.setCompany(company);
        }

        TeamDTO oldTeam = null;
        if (!isNull(teamDTO.getId())) {
            log.info(LOG_PREFIX + "Creating team...");
            oldTeam = teamMapper.map(teamRepository.findById(teamDTO.getId()).get());
        } else log.info(LOG_PREFIX + "Updating team...");


        Team peristedTeam =  teamRepository.save(newTeam);

        // Send email to users linked with this team
        for (UserTeam userTeam : peristedTeam.getUserTeamSet()) {
            if (teamDTO.getId() == null || (teamDTO.getId() != null && userNotExistsInPersistedTeam(userTeam.getUserDetail().getId(), oldTeam.getUserDetailSet()))) {
                log.info(LOG_PREFIX + "Sending emails to new team members...");
                emailService.sendTeamMembersEmail(peristedTeam.getName(), userDetailService.findById(userTeam.getUserDetail().getId()).getEmail());
            }
        }


        if (teamDTO.getId() == null) {
            // LOG CREATION
            auditService.saveTeamAudit(AuditAction.CREATE, userMapperInstance.map(userDetailDTO), peristedTeam.getName(), peristedTeam.getId());
            log.info(LOG_PREFIX + "Team with id {} successfully created by user with id : {}.",
                    peristedTeam.getId(),
                    securityService.getLoggedInUserDetailId());
        } else {
            // LOG UPDATE
            auditService.saveTeamAudit(AuditAction.UPDATE, userMapperInstance.map(userDetailDTO),
                    teamDTO.getName(), oldTeam, teamMapper.map(teamRepository.findById(teamDTO.getId()).get()));
            log.info(LOG_PREFIX + "Team with id {} successfully updated by user with id : {}.",
                    teamDTO.getId(),
                    securityService.getLoggedInUserDetailId());
        }

        return teamMapper.map(peristedTeam);
    }

    // Check if user exists already in team or not in order to send email.
    boolean userNotExistsInPersistedTeam(String userId, Set<OptionDTO> userDetailSet) {
        for (OptionDTO option : userDetailSet) {
            if (option.getId().equals(userId)) {
                return false;
            }
        }
        return true;
    }

    public Page<TeamDTO> findAll(Predicate predicate,
                                 Pageable pageable, String selectionFromDD) {
        UserDetailDTO dto = securityService.getLoggedInUserDetailDTO();
        BooleanBuilder booleanBuilder = new BooleanBuilder(predicate);

        if (dto.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                booleanBuilder.and(Q_TEAM.authority.id.eq(selectionFromDD));
            }
        } else if (dto.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                booleanBuilder.and(Q_TEAM.company.id.eq(selectionFromDD));
            }
        }

        Page<Team> all = this.teamRepository.findAll(booleanBuilder, pageable);
        return this.teamMapper.map(all);
    }

    public List<OptionDTO> findByCompanyId(String companyId) {
        return teamMapper.mapToListOptionDTO(teamRepository.findAllByCompanyId(companyId));
    }

    public List<OptionDTO> findByAuthorityId(String authorityId) {
        return teamMapper.mapToListOptionDTO(teamRepository.findAllByAuthorityId(authorityId));
    }

    public void deleteTeam(String id, String selectionFromDD) {
        TeamDTO teamDTO = findById(id);
        if (!teamValidator.validateRequest(teamDTO, selectionFromDD)) {
            log.info(LOG_PREFIX + "Entity with id : {} has no rights to delete team with id {}.",
                    selectionFromDD,
                    id);
            throw new NotAllowedException("Team cannot be deleted.");
        }

        log.info(LOG_PREFIX + "Deleting team...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.TEAM);
        TeamDTO deletedTeam = deleteById(id);

        // LOG DELETION
        auditService.saveTeamAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), teamDTO.getName(), teamDTO.getId());
        log.info(LOG_PREFIX + "Team with id {} successfully deleted by user with id : {}",
                deletedTeam.getId(),
                securityService.getLoggedInUserDetailId());
    }

    public boolean existsCertificateTeamForTeam(String teamId) {
        log.info(LOG_PREFIX + "Checking if team with id {} is assigned to any certificates...", teamId);
        List<CertificateTeam> certTeamList = certificateTeamRepository.findByTeam_Id(teamId);
        return !certTeamList.isEmpty();
    }
}
