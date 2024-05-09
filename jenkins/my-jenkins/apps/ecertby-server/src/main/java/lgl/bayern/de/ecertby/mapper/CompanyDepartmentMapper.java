package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CompanyDepartmentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.CompanyDepartment;
import lgl.bayern.de.ecertby.model.QCompanyDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.Set;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CompanyDepartmentMapper extends BaseMapper<CompanyDepartmentDTO, CompanyDepartment, QCompanyDepartment> {




    @Mapping(target = "name", source = "department.data")
    @Mapping(target = "value", source = "id")
    abstract Set<OptionDTO> departmentToOptionDTO(Set<CompanyDepartment> department);

}
