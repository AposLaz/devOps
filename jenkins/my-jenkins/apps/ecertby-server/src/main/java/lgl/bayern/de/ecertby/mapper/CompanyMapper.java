package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.dto.ResourceDTO;
import java.util.List;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.dto.UserAuthorityCompanyDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CompanyMapper extends BaseMapper<CompanyDTO, Company, QCompany> {

    abstract OptionDTO mapToOptionDTO(Company company);

    public abstract List<OptionDTO> mapToListOptionDTO(List<Company> company);

    @Mapping(constant = "COMPANY", target = "description")
    @Mapping(source = "companyDTO.id", target = "objectId")
    @Mapping(target = "id", ignore = true)
    public abstract ResourceDTO companyDTOtoResourceDTO(CompanyDTO companyDTO);

    CompanyDepartment optionDTOToCompanyDepartment(OptionDTO optionDTO) {
        CompanyDepartment companyDepartment = new CompanyDepartment();
        CatalogValue catalogValue = new CatalogValue();
        catalogValue.setId(optionDTO.getId());
        companyDepartment.setDepartment(catalogValue);
        return companyDepartment;
    }

    OptionDTO companyDepartmentToOptionDTO(CompanyDepartment department) {
        OptionDTO optionDTO = new OptionDTO();
        optionDTO.setId(department.getDepartment().getId());
        optionDTO.setName(department.getDepartment().getData());
        return optionDTO;
    }

    @Mapping(source="company.name", target = "authorityCompanyName")
    @Mapping(source="company.id", target = "authorityCompanyId")
    @Mapping(source="userGroup.id", target = "userGroupId")
    @Mapping(source="roleInProcess.data", target = "roleInProcessName")
    @Mapping(source="roleInProcess.id", target = "roleInProcess")
    public abstract UserAuthorityCompanyDTO mapToUserAuthorityCompanyDTO(UserCompany userCompany);

    public abstract SelectionFromDDJWTDTO mapToSelectionFromDDJWTDTO (Company company);
}
