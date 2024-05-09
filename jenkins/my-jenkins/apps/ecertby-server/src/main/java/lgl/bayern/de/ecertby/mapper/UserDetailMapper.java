package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.*;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserDetailMapper extends BaseMapper<UserDetailDTO, UserDetail, QUserDetail> {

    @Mapping(target = "role", qualifiedByName = "userGroupsToRole", source = "user.userGroups")
    @Mapping(expression = "java((userDetail.getUser().getUserGroups() != null && !userDetail.getUser().getUserGroups().isEmpty()) ? userDetail.getUser().getUserGroups().get(0).getName() : \"\") ", target = "roleName")
    @Override
    public abstract UserDetailDTO map(UserDetail userDetail);

    @Named("userGroupsToRole")
    OptionDTO userGroupsToRole(List<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) {
            return new OptionDTO();
        } else {
            OptionDTO optionDTO = new OptionDTO();
            optionDTO.setName(MessageConfig.getValue(userGroups.get(0).getName()));
            optionDTO.setId(userGroups.get(0).getId());
            optionDTO.setFilterId(userGroups.get(0).getParent().getId());
            return optionDTO;
        }
    }

    UserDepartment optionDTOToUserDepartment(OptionDTO optionDTO) {
        UserDepartment userDepartment = new UserDepartment();
        CatalogValue catalogValue = new CatalogValue();
        catalogValue.setId(optionDTO.getId());
        userDepartment.setDepartment(catalogValue);
        return userDepartment;
    }

    OptionDTO userDepartmentToOptionDTO(UserDepartment department) {
        OptionDTO optionDTO = new OptionDTO();
        optionDTO.setId(department.getDepartment().getId());
        optionDTO.setName(department.getDepartment().getData());
        return optionDTO;
    }

    @Mapping(source="userDetailProfileDTO.salutation", target = "salutation")
    @Mapping(source="userDetailProfileDTO.firstName", target = "firstName")
    @Mapping(source="userDetailProfileDTO.lastName", target = "lastName")
    @Mapping(source="userDetailProfileDTO.fullName", target = "fullName")
    @Mapping(source="userDetailProfileDTO.username", target = "username")
    @Mapping(source="userDetailProfileDTO.email", target = "email")
    @Mapping(source="userDetailProfileDTO.emailExtern", target = "emailExtern")
    @Mapping(source="userDetailProfileDTO.telephone", target = "telephone")
    @Mapping(source="userDetailProfileDTO.mobileNumber", target = "mobileNumber")
    @Mapping(source="userDetailProfileDTO.mobileDienstnummer", target = "mobileDienstnummer")
    @Mapping(source="userDetailProfileDTO.additionalContactInfo", target = "additionalContactInfo")
    @Mapping(source="userDetailProfileDTO.user", target = "user")
    @Mapping(source="userDetailProfileDTO.id", target = "id")
    @Mapping(source="userDetailProfileDTO.primaryCompany", target = "primaryCompany")
    @Mapping(source="userDetailProfileDTO.primaryAuthority", target = "primaryAuthority")
    @Mapping(source = "userDetailProfileDTO.department", target="department")
    public abstract UserDetailDTO map(UserDetailDTO userDetailDTO, UserDetailProfileDTO userDetailProfileDTO);

    public abstract List<OptionDTO> mapToListOptionDTO(List<UserDetail> userDetailList);

    @Mapping(target = "id", source = "userDetail.id")
    @Mapping(target = "name",  expression = "java(combineFirstNameLastName(userDetail))")
    @Mapping(target = "active" ,source = "userDetail.active")
    public abstract OptionDTO mapToOptionDTO(UserDetail userDetail);

    @Named("combineFirstNameLastName")
    protected String combineFirstNameLastName(UserDetail userDetail) {
        StringBuilder fullName =  new StringBuilder();
        fullName.append(userDetail.getFirstName()).append(" ").append(userDetail.getLastName());
        if(!StringUtils.isEmpty(userDetail.getSalutation())){
            fullName.append(" (").append(userDetail.getSalutation()).append(")");
        }
        return fullName.toString();
    }

    @Mapping(target = "primaryCompany.id", source = "primaryCompany.id")
    @Mapping(target = "primaryAuthority.id", source = "primaryAuthority.id")
    public abstract UserDetailJWTDTO mapToUserDetailJWTDTO (UserDetail userDetail);

    @Mapping(source="product.id", target = "id")
    @Mapping(source="product.data", target = "name")
    public abstract OptionDTO catalogValueToOptionDTO(CatalogValue product);

    @Mapping(source="optionDTO.id", target = "id")
    @Mapping(source="optionDTO.name", target = "data")
    public abstract CatalogValue optionDTOToCatalogValue(OptionDTO optionDTO);
}
