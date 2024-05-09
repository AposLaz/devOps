package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.CompanyProfileCountry;
import lgl.bayern.de.ecertby.model.QCompanyProfileCountry;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyProfileCountryRepository extends BaseRepository<CompanyProfileCountry, QCompanyProfileCountry> {
}
