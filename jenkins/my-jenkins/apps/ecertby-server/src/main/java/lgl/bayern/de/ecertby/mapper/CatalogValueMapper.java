package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.CatalogValue;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import lgl.bayern.de.ecertby.model.QCatalogValue;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CatalogValueMapper extends BaseMapper<CatalogValueDTO, CatalogValue, QCatalogValue> {

    @Mapping(target = "name", source = "data")
    @Mapping(target = "active" ,  expression="java(true)")
    abstract OptionDTO mapToOptionDTO(CatalogValue catalogValue);

    public abstract List<OptionDTO> mapToListOptionDTO(List<CatalogValue> catalogValue);
}
