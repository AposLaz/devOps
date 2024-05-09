package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.EmailNotification;
import lgl.bayern.de.ecertby.model.QEmailNotification;
import lgl.bayern.de.ecertby.model.Team;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.EmailNotificationType;
import lgl.bayern.de.ecertby.model.util.UserType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationRepository extends BaseRepository<EmailNotification, QEmailNotification>  {
    @Query("SELECT en.notificationType FROM EmailNotification en " +
            "WHERE en.userDetail = :user")
    List<EmailNotificationType> findEmailNotificationTypesByUserDetail(@Param("user") UserDetail user);

    void deleteAllByUserDetail(UserDetail user);

    @Query("SELECT uc.userDetail FROM UserCompany uc " +
            "INNER JOIN EmailNotification en ON uc.userDetail.id = en.userDetail.id " +
            "WHERE uc.company.id = :certificateCompanyId " +
            "   AND en.notificationType = :notificationType")
    List<UserDetail> findAllUsersInCompanyOfCertificateWithNotificationType(@Param("certificateCompanyId") String certificateCompanyId, @Param("notificationType")EmailNotificationType notificationType);

    @Query("SELECT uc.userDetail FROM UserCompany uc " +
            "INNER JOIN EmailNotification en ON uc.userDetail.id = en.userDetail.id " +
            "WHERE uc.company.id = :certificateCompanyId " +
            "   AND en.notificationType = :notificationType " +
            "   AND uc.userDetail NOT IN :ignoreUsers ")
    List<UserDetail> findAllUsersInCompanyOfCertificateWithNotificationTypeIgnoring(@Param("certificateCompanyId") String certificateCompanyId, @Param("notificationType")EmailNotificationType notificationType, @Param("ignoreUsers") List<UserDetail> ignoreUsers);

    @Query("SELECT ua.userDetail FROM UserAuthority ua " +
            "INNER JOIN EmailNotification en ON ua.userDetail.id = en.userDetail.id " +
            "WHERE ua.authority.id = :certificateForwardAuthorityId " +
            "   AND en.notificationType = :notificationType")
    List<UserDetail> findAllUsersInAuthorityOfCertificateWithNotificationType(@Param("certificateForwardAuthorityId") String certificateForwardAuthorityId, @Param("notificationType") EmailNotificationType notificationType);

    @Query("SELECT ua.userDetail FROM UserAuthority ua " +
            "INNER JOIN EmailNotification en ON ua.userDetail.id = en.userDetail.id " +
            "WHERE ua.authority.id = :certificateForwardAuthorityId " +
            "   AND en.notificationType = :notificationType " +
            "   AND ua.userDetail NOT IN :ignoredUsers")
    List<UserDetail> findAllUsersInAuthorityOfCertificateWithNotificationTypeIgnoring(@Param("certificateForwardAuthorityId") String certificateForwardAuthorityId, @Param("notificationType") EmailNotificationType notificationType, @Param("ignoredUsers") List<UserDetail> ignoredUsers);

    @Query("SELECT u FROM UserDetail u " +
            "CROSS JOIN Team t " +
            "INNER JOIN UserTeam ut ON ut.userDetail.id = u.id " +
            "INNER JOIN EmailNotification n ON u.id = n.userDetail.id " +
            "WHERE t IN :teamList " +
            "   AND ut MEMBER OF t.userTeamSet " +
            "   AND n.notificationType = :notificationType")
    List<UserDetail> findAllUsersInTeamsWithNotificationType(@Param("teamList")List<Team> teamList, @Param("notificationType") EmailNotificationType notificationType);

    @Query("SELECT u FROM UserDetail u " +
            "CROSS JOIN Team t " +
            "INNER JOIN UserTeam ut ON ut.userDetail.id = u.id " +
            "INNER JOIN EmailNotification n ON u.id = n.userDetail.id " +
            "WHERE t IN :teamList " +
            "   AND ut MEMBER OF t.userTeamSet " +
            "   AND n.notificationType = :notificationType " +
            "   AND u != :ignoreUser")
    List<UserDetail> findAllUsersInTeamsWithNotificationTypeIgnoring(@Param("teamList")List<Team> teamList, @Param("notificationType") EmailNotificationType notificationType, @Param("ignoreUser") UserDetail ignoreUser);

    @Query("SELECT h.assignedEmployee FROM CertificateAssignmentHistory h " +
            "INNER JOIN EmailNotification n ON n.userDetail.id = h.assignedEmployee.id " +
            "WHERE h.id = :historyId " +
            "   AND h.assignedEmployee.primaryAuthority IS NUll " +
            "   AND n.notificationType = :notificationType")
    UserDetail findCompanyAssignedEmployeeOfCertificateWithNotificationType(@Param("historyId") String historyId, @Param("notificationType") EmailNotificationType notificationType);

    @Query("SELECT u FROM UserDetail u " +
            "INNER JOIN EmailNotification n ON n.userDetail.id = u.id " +
            "WHERE u.userType = :userType " +
            "   AND n.notificationType = :notificationType")
    List<UserDetail> findAllByUserTypeAndNotificationType(@Param("userType") UserType userType, @Param("notificationType") EmailNotificationType notificationType);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM UserDetail u " +
            "INNER JOIN EmailNotification n ON n.userDetail.id = u.id " +
            "WHERE u.id = :userId " +
            "   AND n.notificationType = :notificationType")
    boolean userHasNotificationType(@Param("userId") String userId, @Param("notificationType") EmailNotificationType notificationType);
}
