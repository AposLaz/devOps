package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.QCompany;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends BaseRepository<Company, QCompany> {

    @Query("SELECT c " +
            "FROM Company c " +
            "WHERE " +
            "   c.id = :companyId AND " +
            "   c.deleted = false AND " +
            "  (c.id = :actingId OR " +
            "   c.preResponsibleAuthority.id = :actingId OR " +
            "   c.postResponsibleAuthority.id = :actingId OR " +
            "   c.responsibleAuthority.id = :actingId)")
    Company findCompanyByIdWithValidations(@Param("companyId") String companyId, @Param("actingId") String actingId);

    @Query("SELECT CASE WHEN COUNT(company) > 0 THEN true ELSE false END " +
            "FROM Company company " +
            "WHERE :authorityId IN (" +
            "       SELECT a.id FROM Authority a WHERE " +
            "       a = company.postResponsibleAuthority OR " +
            "       a = company.preResponsibleAuthority OR " +
            "       a = company.responsibleAuthority" +
            "       ) " +
            "       AND company.active = true AND company.deleted = false")
    boolean existsCompaniesLinkedWithAuthority(@Param("authorityId") String authorityId);

    @Query("SELECT CASE WHEN COUNT(company) = 0 THEN true ELSE false END " +
            "FROM Company company " +
            "WHERE company.id = :companyId AND (" +
            "company.preResponsibleAuthority.active = false OR " +
            "company.postResponsibleAuthority.active = false OR " +
            "company.responsibleAuthority.active = false)")
    boolean areAllAuthoritiesOfCompanyActive(@Param("companyId") String companyId);

    @Query("SELECT company " +
            "FROM Company company " +
            "INNER JOIN UserCompany userCompany ON userCompany.company.id = company.id " +
            "WHERE company.active = true AND userCompany.userDetail.id = :userDetailId " +
            "ORDER BY company.name ASC")
    List<Company> getUserCompanies(@Param("userDetailId") String userDetailId);

    @Query(value = "SELECT company " +
            "FROM Company company " +
            "INNER JOIN UserCompany userCompany ON userCompany.company.id = company.id " +
            "WHERE company.active = false  AND company.deleted = false AND userCompany.userDetail.id = :userDetailId " +
            "ORDER BY company.name ASC limit 1")
    Optional<Company> getFirstInactiveAndNotDeletedCompany(@Param("userDetailId") String userDetailId);

    Company findByName(String name);

    Company findByNameAndIdNot(String name, String companyId);

    List<Company> findAllByActiveIsTrueOrderByName();

    @Query("SELECT CASE WHEN COUNT(cd) > 0 THEN true ELSE false END " +
            "FROM CompanyDepartment cd " +
            "WHERE cd.department.id = :id")
    boolean existsCompanyDepartmentById(@Param("id") String id);

    @Override
    default void customize(QuerydslBindings bindings, QCompany company) {
        BaseRepository.super.customize(bindings, company);
        bindings.bind(company.department.any().department.id).first((path, value) -> {
            List<String> departmentCatalogueIdList = List.of(value.split(","));
            if (departmentCatalogueIdList.size() == 1) {
                return path.eq(departmentCatalogueIdList.get(0));
            } else {
                return path.in(departmentCatalogueIdList);
            }
        });
    }
}
