package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.dto.ResourceDTO;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.dto.UserAuthorityCompanyDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class AuthorityMapper extends BaseMapper<AuthorityDTO, Authority, QAuthority> {

    public abstract OptionDTO mapToOptionDTO(Authority authority);
    public abstract List<OptionDTO> mapToListOptionDTO(List<Authority> authorityList);

    @Mapping(constant = "AUTHORITY", target = "description")
    @Mapping(source = "authorityDTO.id", target = "objectId")
    @Mapping(target = "id", ignore = true)
    public abstract ResourceDTO authorityDTOtoResourceDTO(AuthorityDTO authorityDTO);

    AuthorityDepartment optionDTOToAuthorityDepartment(OptionDTO optionDTO) {
        AuthorityDepartment authorityDepartment = new AuthorityDepartment();
        CatalogValue catalogValue = new CatalogValue();
        catalogValue.setId(optionDTO.getId());
        catalogValue.setData(optionDTO.getName());
        authorityDepartment.setDepartment(catalogValue);
        return authorityDepartment;
    }

    OptionDTO authorityDepartmentToOptionDTO(AuthorityDepartment department) {
        OptionDTO optionDTO = new OptionDTO();
        optionDTO.setId(department.getDepartment().getId());
        optionDTO.setName(department.getDepartment().getData());
        return optionDTO;
    }

    @Mapping(source="authority.name", target = "authorityCompanyName")
    @Mapping(source="authority.id", target = "authorityCompanyId")
    @Mapping(source="userGroup.id", target = "userGroupId")
    @Mapping(source="roleInProcess.data", target = "roleInProcessName")
    @Mapping(source="roleInProcess.id", target = "roleInProcess")
    public abstract UserAuthorityCompanyDTO mapToUserAuthorityCompanyDTO(UserAuthority userAuthority);


    public abstract SelectionFromDDJWTDTO mapToSelectionFromDDJWTDTO (Authority authority);
}
