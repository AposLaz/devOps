package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.AuthorityDepartmentDTO;
import lgl.bayern.de.ecertby.model.AuthorityDepartment;
import lgl.bayern.de.ecertby.model.QAuthorityDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class AuthorityDepartmentMapper extends BaseMapper<AuthorityDepartmentDTO, AuthorityDepartment, QAuthorityDepartment> {
}
