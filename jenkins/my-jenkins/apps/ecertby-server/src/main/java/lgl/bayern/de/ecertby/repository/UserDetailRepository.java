package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.QUserDetail;
import lgl.bayern.de.ecertby.model.UserDetail;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDetailRepository extends BaseRepository<UserDetail, QUserDetail>
{
    UserDetail findByUserId(String aaaUserId);

    UserDetail findByEmail(String email);

    UserDetail findByEmailAndIdNot(String email, String userDetailId);

    UserDetail findByUserUsername(String username);

    int countByUserUsername(String username);

    UserDetail findByUserUsernameIgnoreCase(String username);

    UserDetail findByUserUsernameAndIdNot(String username, String userDetailId);

    UserDetail findByUserUsernameIgnoreCaseAndIdNot(String username, String userDetailId);


    @Query("SELECT userDetail FROM UserDetail userDetail " +
            "INNER JOIN UserAuthority userAuthority on userAuthority.userDetail.id = userDetail.id " +
            "WHERE userAuthority.authority.id = :authorityId")
    List<UserDetail> findAllUsersByAuthority(@Param("authorityId") String authorityId);

    @Query("SELECT userDetail FROM UserDetail userDetail " +
            "INNER JOIN UserCompany userCompany on userCompany.userDetail.id = userDetail.id " +
            "WHERE userCompany.company.id = :companyId" )
    List<UserDetail> findAllUsersByCompany(@Param("companyId") String companyId);

    @Modifying
    @Query("UPDATE UserDetail SET refreshToken = :refreshToken, refreshCounter = :refreshCounter WHERE id = :userId")
    int updateRefreshTokenAndCounter(@Param("refreshToken") String refreshToken,@Param("refreshCounter") int refreshCounter, @Param("userId") String userId);

    @Query("SELECT u FROM UserDetail u WHERE u.id = :userId")
    UserDetail getRefreshToken(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE UserDetail SET searchCriteria = null  WHERE searchCriteria.id = :searchCriteriaId")
    int deleteSearchCriteriaReferences(@Param("searchCriteriaId") String searchCriteriaId);


    @Override
    default void customize(QuerydslBindings bindings, QUserDetail userDetail) {
        // Call the common customization first
        BaseRepository.super.customize(bindings, userDetail);
        bindings.bind(userDetail.department.any().department.id).first((path, value) -> {
            List<String> departmentCatalogueIdList = List.of(value.split(","));
            if(departmentCatalogueIdList.size() == 1){
                return path.eq(departmentCatalogueIdList.get(0));
            } else {
                return path.in(departmentCatalogueIdList);
            }
        });
        bindings.bind(userDetail.user.userGroups.any().id).first((path, value) -> {
            return userDetail.userAuthoritySet.any().userGroup.id.eq(value)
                    .or(userDetail.userCompanySet.any().userGroup.id.eq(value))
                    .or(path.eq(value));
        });
    }
}
