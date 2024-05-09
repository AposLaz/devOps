package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CompanyProfileMapper extends BaseMapper<CompanyProfileDTO, CompanyProfile, QCompanyProfile> {

    @Mapping(source="product.id", target = "id")
    @Mapping(source="product.data", target = "name")
    public abstract OptionDTO companyProfileProductToOptionDTO(CompanyProfileProduct product);


    @Mapping(source="optionDTO.id", target = "product.id")
    @Mapping(source = "optionDTO.name" , target = "product.data")
    @Mapping(target = "id", ignore = true)
    public abstract CompanyProfileProduct optionDTOToCompanyProfileProduct(OptionDTO optionDTO);

    @Mapping(source="targetCountry.id", target = "id")
    @Mapping(source="targetCountry.name", target = "name")
    public abstract OptionDTO companyProfileCountryToOptionDTO(CompanyProfileCountry product);


    @Mapping(source="optionDTO.id", target = "targetCountry.id")
    @Mapping(target = "id", ignore = true)
    public abstract CompanyProfileCountry optionDTOToCompanyProfileCountry(OptionDTO optionDTO);


}
