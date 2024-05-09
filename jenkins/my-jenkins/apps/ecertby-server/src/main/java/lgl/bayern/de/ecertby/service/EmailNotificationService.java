package lgl.bayern.de.ecertby.service;

import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.mapper.CertificateMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.EmailNotificationType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.CertificateTeamRepository;
import lgl.bayern.de.ecertby.repository.EmailNotificationRepository;
import lgl.bayern.de.ecertby.repository.TeamRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class EmailNotificationService {

    private final EmailService emailService;
    private final TeamService teamService;

    private final UserDetailRepository userDetailRepository;
    private final TeamRepository teamRepository;
    private final CertificateTeamRepository certificateTeamRepository;
    private final EmailNotificationRepository emailNotificationRepository;

    private final CertificateMapper certificateMapperInstance = Mappers.getMapper(CertificateMapper.class);

    public void notifyAdminUsersOnFeatureBoardEntry(String entryId) {
        for (UserDetail user : emailNotificationRepository.findAllByUserTypeAndNotificationType(UserType.ADMIN_USER, EmailNotificationType.FEATUREBOARD_ENTRY_ADDED)) {
            emailService.sendFeatureboardEntryAddedEmail(entryId, user.getEmail());
        }
    }

    public void notifyAssignedUsers(CertificateDTO certificateDTO, Certificate oldCertificate) {
        Certificate certificate = certificateMapperInstance.map(certificateDTO);
        if (oldCertificate == null) {
            notifyAssignedUsersOnCreate(certificate);
        } else {
            notifyAssignedUsersOnUpdate(certificate, oldCertificate);
        }
    }

    public void notifyCompanyUsersOnRelease(String certificateId, String certificateCompanyId, String historyId) {
        for (UserDetail user : findInterestedUsers(certificateId, certificateCompanyId, historyId)) {
            emailService.sendCertificateReleaseEmail(certificateId, user.getEmail());
        }
    }

    public void notifyCompanyUsersOnCertificateReject(String certificateId, String certificateCompanyId, String historyId) {
        for (UserDetail user : findInterestedUsers(certificateId, certificateCompanyId, historyId)) {
            emailService.sendCertificateRejectEmail(certificateId, user.getEmail());
        }
    }

    public void notifyCompanyUsersOnVotePositive(String certificateId, String parentCertificateId, String certificateCompanyId, String historyId) {
        for (UserDetail user : findInterestedUsers(parentCertificateId, certificateCompanyId, historyId)) {
            emailService.sendPrecertificateVotePositiveEmail(certificateId, parentCertificateId, user.getEmail());
        }
    }

    public void notifyCompanyUsersOnPrertificateReject(String certificateId, String parentCertificateId, String certificateCompanyId, String historyId) {
        for (UserDetail user : findInterestedUsers(parentCertificateId, certificateCompanyId, historyId)) {
            emailService.sendPrecertificateRejectEmail(certificateId, parentCertificateId, user.getEmail());
        }
    }

    public void notifyForwardAuthorityUsers(CertificateDTO certificateDTO, boolean isCompleteForward) {
        if (certificateDTO.getParentCertificate() == null) {
            if (isCompleteForward) {
                notifyCompleteForward(certificateDTO);
            } else {
                notifyRegularForward(certificateDTO);
            }
        } else {
            List<UserDetail> users = emailNotificationRepository.findAllUsersInAuthorityOfCertificateWithNotificationType(certificateDTO.getForwardAuthority().getId(), EmailNotificationType.CERTIFICATE_FORWARDED);
            for (UserDetail user : users) {
                emailService.sendPreCertificateForwardedEmail(certificateDTO, user.getEmail());
            }
        }
    }

    private void notifyCompleteForward(CertificateDTO certificateDTO) {
        List<UserDetail> completeForwardUsers = emailNotificationRepository.findAllUsersInAuthorityOfCertificateWithNotificationType(certificateDTO.getForwardAuthority().getId(), EmailNotificationType.CERTIFICATE_COMPLETE_FORWARDED);
        for (UserDetail user : completeForwardUsers) {
            emailService.sendCertificateForwardedEndEmail(certificateDTO, user.getEmail());
        }
        List<UserDetail> forwardUsers;
        if (completeForwardUsers.isEmpty()) {
            forwardUsers = emailNotificationRepository.findAllUsersInAuthorityOfCertificateWithNotificationType(certificateDTO.getForwardAuthority().getId(), EmailNotificationType.CERTIFICATE_FORWARDED);
        } else {
            forwardUsers = emailNotificationRepository.findAllUsersInAuthorityOfCertificateWithNotificationTypeIgnoring(certificateDTO.getForwardAuthority().getId(), EmailNotificationType.CERTIFICATE_FORWARDED, completeForwardUsers);
        }
        for (UserDetail user : forwardUsers) {
            emailService.sendCertificateForwardedEmail(certificateDTO, user.getEmail());
        }
    }

    private void notifyRegularForward(CertificateDTO certificateDTO) {
        List<UserDetail> users = emailNotificationRepository.findAllUsersInAuthorityOfCertificateWithNotificationType(certificateDTO.getForwardAuthority().getId(), EmailNotificationType.CERTIFICATE_COMPLETE_FORWARDED);
        for (UserDetail user : users) {
            emailService.sendPrecertificationStartedEmail(certificateDTO, user.getEmail());
        }
    }

    private List<UserDetail> findInterestedUsers(String certificateId, String certificateCompanyId, String historyId) {
        // Find the interested assigned employee
        UserDetail assignedEmployee = emailNotificationRepository.findCompanyAssignedEmployeeOfCertificateWithNotificationType(historyId, EmailNotificationType.CERTIFICATE_COMPANY_USER);
        // Find all interested members of the assigned teams, that are not assigned employees
        List<UserDetail> teamMembers = findInterestedTeamMembers(certificateId, historyId, assignedEmployee);
        // Find all interested members of the company that are not assigned employees nor members of the assigned teams
        List<UserDetail> allUsers = findInterestedCompanyMembers(certificateCompanyId, teamMembers);
        allUsers.addAll(teamMembers);
        return allUsers;
    }

    private List<UserDetail> findInterestedTeamMembers(String certificateId, String historyId, UserDetail assignedUser) {
        List<Team> companyTeams = new ArrayList<>();
        if (historyId.isEmpty()) {
            List<CertificateTeam> certificateTeams = certificateTeamRepository.findCertificateTeamsByCertificateId(certificateId);
            for (CertificateTeam certificateTeam : certificateTeams) {
                companyTeams.add(certificateTeam.getTeam());
            }
        } else companyTeams = teamRepository.findAllPastAssignedCompanyTeamsOfCertificate(historyId);
        List<UserDetail> teamMembers;
        if (assignedUser != null) {
            teamMembers = emailNotificationRepository.findAllUsersInTeamsWithNotificationTypeIgnoring(companyTeams, EmailNotificationType.CERTIFICATE_COMPANY_TEAM, assignedUser);
            teamMembers.add(assignedUser);
        } else {
            teamMembers = emailNotificationRepository.findAllUsersInTeamsWithNotificationType(companyTeams, EmailNotificationType.CERTIFICATE_COMPANY_TEAM);
        }
        return teamMembers;
    }

    private List<UserDetail> findInterestedCompanyMembers(String certificateCompanyId, List<UserDetail> teamUsers) {
        List<UserDetail> companyMembers;
        if (!teamUsers.isEmpty()) {
            companyMembers = emailNotificationRepository.findAllUsersInCompanyOfCertificateWithNotificationTypeIgnoring(certificateCompanyId, EmailNotificationType.CERTIFICATE_COMPANY_GENERAL, teamUsers);
        } else {
            companyMembers = emailNotificationRepository.findAllUsersInCompanyOfCertificateWithNotificationType(certificateCompanyId, EmailNotificationType.CERTIFICATE_COMPANY_GENERAL);
        }
        return companyMembers;
    }

    private void notifyAssignedUsersOnCreate(Certificate certificate) {
        // Notify assigned employee
        UserDetail assignedEmployee = certificate.getAssignedEmployee();
        if (assignedEmployee != null) {
            Optional<UserDetail> optionalUserDetail = userDetailRepository.findById(assignedEmployee.getId());
            if (optionalUserDetail.isPresent() && userHasCertificateAssignedNotification(assignedEmployee.getId())) {
                emailService.sendCertificateUserAssignedEmail(certificate, optionalUserDetail.get().getEmail());
            }
        }
        // Notify members of assigned teams
        Set<CertificateTeam> assignedTeamSet = certificate.getAssignedTeamSet();
        if (assignedTeamSet == null) return;
        for (CertificateTeam certificateTeam : assignedTeamSet) {
            Team team = teamService.findEntityById(certificateTeam.getTeam().getId());
            for (UserTeam userTeam : team.getUserTeamSet()) {
                UserDetail user = userTeam.getUserDetail();
                if (userHasCertificateAssignedNotification(user.getId())) {
                    emailService.sendCertificateTeamAssignedEmail(certificate, team.getName(), user.getEmail());
                }
            }
        }
    }

    private void notifyAssignedUsersOnUpdate(Certificate certificate, Certificate oldCertificate) {
        // We use the old certificate entry to send emails only to newly added users and teams
        UserDetail assignedEmployee = certificate.getAssignedEmployee();
        boolean isPrecertificate = oldCertificate.getParentCertificate() != null;
        notifyNewlyAssignedEmployee(assignedEmployee, oldCertificate.getAssignedEmployee(), certificate, isPrecertificate);
        notifyNewlyAssignedTeamMembers(certificate, oldCertificate.getAssignedTeamSet(), isPrecertificate);
    }

    private void notifyNewlyAssignedEmployee(UserDetail assignedUser, UserDetail oldAssignedUser, Certificate certificate, boolean isPrecertificate) {
        boolean assignedUserHasChanged = assignedUser!= null && (oldAssignedUser == null || !assignedUser.getId().equals(oldAssignedUser.getId()));
        if (assignedUserHasChanged) {
            Optional<UserDetail> optionalUserDetail = userDetailRepository.findById(assignedUser.getId());
            if (optionalUserDetail.isPresent() && userHasCertificateAssignedNotification(assignedUser.getId())) {
                if (!isPrecertificate) {
                    emailService.sendCertificateUserAssignedEmail(certificate, optionalUserDetail.get().getEmail());
                } else {
                    emailService.sendPreCertificateUserAssignedEmail(certificate, optionalUserDetail.get().getEmail());
                }
            }
        }
    }

    private void notifyNewlyAssignedTeamMembers(Certificate certificate, Set<CertificateTeam> oldAssignedTeamSet, boolean isPrecertificate) {
        Set<CertificateTeam> currentTeams = certificate.getAssignedTeamSet();
        Set<Team> oldTeams = oldAssignedTeamSet.stream().map(CertificateTeam::getTeam).collect(Collectors.toSet());
        if (currentTeams == null) return;
        for (CertificateTeam certificateTeam : currentTeams) {
            Team team = teamService.findEntityById(certificateTeam.getTeam().getId());
            if (!oldTeams.contains(team)) {
                for (UserTeam userTeam : team.getUserTeamSet()) {
                    notifyNewlyAssignedTeamMember(userTeam.getUserDetail(), certificate, team.getName(), isPrecertificate);
                }
            }
        }
    }

    private void notifyNewlyAssignedTeamMember(UserDetail user, Certificate certificate, String teamName, boolean isPrecertificate) {
        if (userHasCertificateAssignedNotification(user.getId())) {
            if (!isPrecertificate) {
                emailService.sendCertificateTeamAssignedEmail(certificate, teamName, user.getEmail());
            } else {
                emailService.sendPreCertificateTeamAssignedEmail(certificate, teamName, user.getEmail());
            }
        }
    }

    private boolean userHasCertificateAssignedNotification(String userId) {
        return emailNotificationRepository.userHasNotificationType(userId, EmailNotificationType.CERTIFICATE_ASSIGNED);
    }
}