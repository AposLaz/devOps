package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.QAuthority;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorityRepository extends BaseRepository<Authority, QAuthority> {

    List<Authority> findAllByOrderByNameAsc();
    List<Authority> findAllByActiveIsTrueOrderByName();

    @Query("SELECT authority " +
            "FROM Authority authority " +
            "WHERE authority.active = true and authority.id NOT IN " +
            "(SELECT c.forwardAuthority.id " +
            "FROM Certificate c " +
            "WHERE (c.id = :certificateId AND c.forwardAuthority.id = :selectionFromDD) OR c.parentCertificate.id = :certificateId)  " +
            "ORDER BY authority.name")
    List<Authority> findAuthoritiesForAuthorityForward(String certificateId, String selectionFromDD);


    @Query("SELECT authority " +
                  "FROM Authority authority " +
                  "WHERE authority.active = true " +
                  "AND authority.id NOT IN " +
                  "(SELECT cert.forwardAuthority.id " +
                  "FROM Certificate cert " +
                  "WHERE cert.parentCertificate.id = :parentCertificateId " +
                  "AND cert.forwardAuthority IS NOT NULL) " +
                  "AND authority.id <> (SELECT cert.forwardAuthority.id FROM Certificate cert WHERE cert.id = :parentCertificateId) " +
                  "ORDER BY authority.name")
    List<Authority> findAuthoritiesForPreAuthorityForward(String parentCertificateId);



    @Query("SELECT DISTINCT a FROM UserAuthority ua " +
            "JOIN ua.authority a " +
            "JOIN ua.userDetail ud " +
            "JOIN ua.userGroup ug " +
            "WHERE ug.name = 'AUTHORITY_MAIN_USER' " +
            "ORDER BY ua.authority.name")
    List<Authority> findAuthoritiesWithMainUser();

    @Query("SELECT authority " +
            "FROM Authority authority " +
            "INNER JOIN UserAuthority userAuthority ON userAuthority.authority.id = authority.id " +
            "WHERE authority.active = true AND userAuthority.userDetail.id = :userDetailId " +
            "ORDER BY authority.name")
    List<Authority> getUserAuthoritiesOrderByName(@Param("userDetailId") String userDetailId);

    Authority findByName(String name);

    Authority findByNameAndIdNot(String name, String authorityId);

    @Query("SELECT CASE WHEN COUNT(ad) > 0 THEN true ELSE false END " +
            "FROM AuthorityDepartment ad " +
            "WHERE ad.department.id = :id")
    boolean existsAuthorityDepartmentById(@Param("id") String id);

    @Override
    default void customize(QuerydslBindings bindings, QAuthority authority) {
        // Call the common customization first
        BaseRepository.super.customize(bindings, authority);
        bindings.bind(authority.department.any().department.id).first((path, value) -> {
            List<String> departmentCatalogueIdList = List.of(value.split(","));
            if (departmentCatalogueIdList.size() == 1) {
                return path.eq(departmentCatalogueIdList.get(0));
            } else {
                return path.in(departmentCatalogueIdList);
            }
        });


    }
}
