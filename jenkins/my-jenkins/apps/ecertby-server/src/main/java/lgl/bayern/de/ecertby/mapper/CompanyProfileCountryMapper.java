package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CompanyProfileCountryDTO;
import lgl.bayern.de.ecertby.model.CompanyProfileCountry;
import lgl.bayern.de.ecertby.model.QCompanyProfileCountry;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CompanyProfileCountryMapper extends BaseMapper<CompanyProfileCountryDTO, CompanyProfileCountry, QCompanyProfileCountry> {

}
