package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.dto.TemplateElementDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TemplateMapper extends BaseMapper<TemplateDTO, Template, QTemplate> {

    @Mapping(target = "templateElementDTOSet", source = "templateElementSet")
    public abstract TemplateDTO map(Template entity);

    @Mapping(target = "templateElementSet", source = "templateElementDTOSet")
    public abstract Template map(TemplateDTO dto);

    @Mapping(target = "templateElementValueDTOSet", source = "templateElementValueSet")
    public abstract TemplateElementDTO mapTemplateElement(TemplateElement templateElement);

    @Mapping(target = "templateElementValueSet", source = "templateElementValueDTOSet")
    public abstract TemplateElement mapTemplateElement(TemplateElementDTO templateElementDTO);

    @Mapping(source="optionDTO.id", target = "department.id")
    @Mapping(target = "id", ignore = true)
    public abstract TemplateDepartment optionDTOToTemplateDepartment(OptionDTO optionDTO);

    @Mapping(source="department.id", target = "id")
    @Mapping(source="department.data", target = "name")
    public abstract OptionDTO templateDepartmentToOptionDTO(TemplateDepartment department);

    @Mapping(source="optionDTO.id", target = "keyword.id")
    @Mapping(target = "id", ignore = true)
    public abstract TemplateKeyword optionDTOToTemplateKeyword(OptionDTO optionDTO);

    @Mapping(source="keyword.id", target = "id")
    @Mapping(source="keyword.data", target = "name")
    public abstract OptionDTO templateKeywordToOptionDTO(TemplateKeyword keyword);

    @Mapping(source="product.id", target = "id")
    @Mapping(source="product.data", target = "name")
    public abstract OptionDTO catalogValueToOptionDTO(CatalogValue product);

    @Mapping(source="optionDTO.id", target = "id")
    @Mapping(source="optionDTO.name", target = "data")
    public abstract CatalogValue optionDTOToCatalogValue(OptionDTO optionDTO);

    @Mapping(source="elementType", target = "filterId")
    @Mapping(target = "active", constant="true")
    public abstract OptionDTO templateElementToOptionDTO(TemplateElement templateElement);

    public abstract List<OptionDTO> templateElementSetToOptionDTO(Set<TemplateElement> templateElementSet);

}
