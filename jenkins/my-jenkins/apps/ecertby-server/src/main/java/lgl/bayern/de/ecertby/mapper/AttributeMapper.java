package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.AttributeDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class AttributeMapper extends BaseMapper<AttributeDTO, Attribute, QAttribute> {

    @Mapping(source="optionDTO.id", target = "id")
    public abstract Catalog optionDTOToCatalog(OptionDTO optionDTO);

    @Mapping(source="id", target = "id")
    @Mapping(source="name", target = "name")
    public abstract OptionDTO catalogToOptionDTO(Catalog catalog);


    @Mapping(source="optionDTO.id", target = "id")
    public abstract List<AttributeRadioOption> optionDTOToCatalog(List<OptionDTO> optionDTO);

    @Mapping(source="id", target = "id")
    @Mapping(source="name", target = "name")
    public abstract List<OptionDTO> attributeRadioOptionToOptionDTO(List<AttributeRadioOption> attributeRadioOptionList);



    @Mapping(source="id", target = "id")
    @Mapping(source="name", target = "name")
    @Mapping(source="elementType", target = "filterId")
    @Mapping(constant="true", target = "active")
    public abstract OptionDTO attributeToOptionDTO(Attribute attributeList);

}
