package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.AuditDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.model.Audit;
import lgl.bayern.de.ecertby.model.QAudit;
import lgl.bayern.de.ecertby.model.UserDetail;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class AuditMapper extends BaseMapper<AuditDTO, Audit, QAudit> {

    @Override
    @Mapping(source = "audit", target = "userResource", qualifiedByName = "mapUserResource")
    public abstract AuditDTO map(Audit audit);

    @Named("mapUserResource")
    protected String mapUserResource(Audit audit) {
        if (audit.getUserCompany() != null) {
            return audit.getUserCompany().getName();
        } else if (audit.getUserAuthority() != null) {
            return audit.getUserAuthority().getName();
        }
        return null;
    }

    protected UserDetailDTO userDetailToUserDetailDTO(UserDetail userDetail) {
        if ( userDetail == null ) {
            return null;
        }

        UserDetailDTO userDetailDTO = new UserDetailDTO();

        if ( userDetail.getId() != null ) {
            userDetailDTO.setId( userDetail.getId() );
        }
        if ( userDetail.getFirstName() != null ) {
            userDetailDTO.setFirstName( userDetail.getFirstName() );
        }
        if ( userDetail.getLastName() != null ) {
            userDetailDTO.setLastName( userDetail.getLastName() );
        }
        if (userDetail.getFirstName() != null && userDetail.getLastName() != null) {
            userDetailDTO.setFullName(userDetail.getFirstName() + " " + userDetail.getLastName());
        }

        return userDetailDTO;
    }

    @AfterMapping
    public void checkNullUser(@MappingTarget AuditDTO auditDTO) {
        if (auditDTO.getUserDetail() == null) {
            UserDetailDTO userDetailDTO = new UserDetailDTO();
            userDetailDTO.setFullName(auditDTO.getFirstName() + " " + auditDTO.getLastName());
            auditDTO.setUserDetail(userDetailDTO);
        }
    }
}
