package lgl.bayern.de.ecertby.repository;

import java.util.List;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import lgl.bayern.de.ecertby.model.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuthorityRepository extends BaseRepository<UserAuthority, QUserAuthority> {
    long countByUserDetail_Id(String userId);

    @Query("SELECT ua.userDetail.id FROM UserAuthority ua WHERE ua.authority.id = :authorityId")
    List<String> findUserIdsByAuthorityId(@Param("authorityId") String authorityId);

    @Query("SELECT ua.userDetail FROM UserAuthority ua WHERE ua.authority.id = :authorityId")
    List<UserDetail> findUsersByAuthorityId(@Param("authorityId") String authorityId);

    @Query("SELECT uc.authority FROM UserAuthority uc WHERE uc.userDetail.id = :userDetailId")
    List<Authority> findAuthoritiesByUserId(@Param("userDetailId") String userDetailId);

    @Query("SELECT ua.userDetail FROM UserAuthority ua " +
            "INNER JOIN UserGroup ug ON ua.userGroup.id=ug.id " +
            "WHERE ua.authority.id = :authorityId " +
            "AND ua.userDetail.active = true " +
            "AND ug.name= :userGroupName")
    List<UserDetail> findActiveUsersByAuthorityIdAndUserGroupName(@Param("authorityId") String authorityId, @Param("userGroupName") String userGroupName);

    @Query("SELECT count(*) FROM UserAuthority ua WHERE ua.authority.id = :authorityId AND ua.userGroup.id = :groupId")
    Integer countUsersByAuthorityIdAndGroupId(@Param("authorityId") String authorityId, @Param("groupId") String groupId);

    List<UserAuthority> findUserAuthoritiesByUserDetailId(String userDetailId);

    UserAuthority findUserAuthorityByAuthorityIdAndUserDetailId(String authorityId, String userDetailId);

    @Query("SELECT ua.userGroup FROM UserAuthority ua WHERE ua.authority.id = :authorityId AND ua.userDetail.id = :userDetailId")
    UserGroup findUserAuthorityUserGroup(String authorityId, String userDetailId);

    @Query("SELECT ua.searchCriteria.id FROM UserAuthority ua " +
            "WHERE ua.authority.id = :authorityId and ua.userDetail.id = :userDetailId")
    String findSearchCriteriaIdByAuthorityIdAndUserDetailId(@Param("authorityId") String authorityId, @Param("userDetailId") String userDetailId);

    @Modifying
    @Query("UPDATE UserAuthority SET searchCriteria = null  WHERE searchCriteria.id = :searchCriteriaId")
    int deleteSearchCriteriaReferences(@Param("searchCriteriaId") String searchCriteriaId);

    boolean existsByRoleInProcessId(String roleInProcessId);
}
