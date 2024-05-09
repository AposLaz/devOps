package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.model.CompanyProfile;
import lgl.bayern.de.ecertby.model.QCompanyProfile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyProfileRepository extends BaseRepository<CompanyProfile, QCompanyProfile> {
    CompanyProfile findByProfileNameAndCompanyId(String name , String companyId);

    @Query("SELECT p " +
            "FROM CompanyProfile p " +
            "WHERE " +
            "   p.id = :companyProfileId AND" +
            "  (p.company.id = :actingId OR " +
            "   p.company.preResponsibleAuthority.id = :actingId OR " +
            "   p.company.postResponsibleAuthority.id = :actingId OR " +
            "   p.company.responsibleAuthority.id = :actingId)")
    CompanyProfile findCompanyProfileByIdWithValidations(@Param("companyProfileId") String companyProfileId, @Param("actingId") String actingId);

}
