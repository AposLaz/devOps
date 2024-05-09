package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CompanyProfileProductDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CompanyProfileProductMapper extends BaseMapper<CompanyProfileProductDTO, CompanyProfileProduct, QCompanyProfileProduct> {
}
