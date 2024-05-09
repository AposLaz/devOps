package lgl.bayern.de.ecertby.repository;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import lgl.bayern.de.ecertby.model.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCompanyRepository extends BaseRepository<UserCompany, QUserCompany> {
    @Query("SELECT u.userDetail FROM UserCompany u WHERE u.company.id = :companyId")
    List<UserDetail> findAllByCompany(@Param("companyId") String companyId);

    @Query("SELECT uc.company FROM UserCompany uc WHERE uc.userDetail.id = :userDetailId")
    List<Company> findCompaniesByUserId(@Param("userDetailId") String userDetailId);

    @Query("SELECT uc.company.id FROM UserCompany uc WHERE uc.userDetail.id = :userDetailId")
    List<String> findCompanyIdsByUserId(@Param("userDetailId") String userDetailId);

    long countByUserDetailAndCompanyActive(UserDetail userDetail, boolean active);

    long countByUserDetailAndCompanyDeleted(UserDetail userDetail, boolean active);

    void deleteUserCompanyByCompanyAndUserDetail(Company company, UserDetail userDetail);

    boolean existsByRoleInProcessId(String roleInProcessId);

    @Query("SELECT uc.userDetail FROM UserCompany uc " +
            "INNER JOIN UserGroup ug ON uc.userGroup.id=ug.id " +
            "WHERE uc.company.id = :companyId " +
            "AND uc.userDetail.active = true " +
            "AND ug.name= :userGroupName")
    List<UserDetail> findActiveUsersByCompanyIdAndUserGroupName(@Param("companyId") String companyId, @Param("userGroupName") String userGroupName);

    List<UserCompany> findUserCompaniesByUserDetailId(String userDetailId);

    UserCompany findUserCompanyByCompanyIdAndUserDetailId(String companyId, String userDetailId);


    @Query("SELECT uc.userGroup FROM UserCompany uc WHERE uc.company.id = :companyId AND uc.userDetail.id = :userDetailId")
    UserGroup findUserCompanyUserGroup(String companyId, String userDetailId);

    @Query("SELECT uc.searchCriteria.id FROM UserCompany uc " +
            "WHERE uc.company.id = :companyId and uc.userDetail.id = :userDetailId")
    String findSearchCriteriaIdByCompanyIdAndUserDetailId(@Param("companyId") String companyId, @Param("userDetailId") String userDetailId);

    @Modifying
    @Query("UPDATE UserCompany SET searchCriteria = null  WHERE searchCriteria.id = :searchCriteriaId")
    int deleteSearchCriteriaReferences(@Param("searchCriteriaId") String searchCriteriaId);

}
