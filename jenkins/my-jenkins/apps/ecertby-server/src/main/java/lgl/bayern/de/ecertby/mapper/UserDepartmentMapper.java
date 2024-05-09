package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.UserDepartmentDTO;
import lgl.bayern.de.ecertby.model.UserDepartment;
import lgl.bayern.de.ecertby.model.QUserDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserDepartmentMapper extends BaseMapper<UserDepartmentDTO, UserDepartment, QUserDepartment> {
}
