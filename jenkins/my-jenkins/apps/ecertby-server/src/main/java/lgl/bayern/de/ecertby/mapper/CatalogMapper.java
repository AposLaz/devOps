package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CatalogDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.model.QCatalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CatalogMapper extends BaseMapper<CatalogDTO, Catalog, QCatalog> {

    @Mapping(target = "active", constant = "true")
    public abstract OptionDTO mapToOptionDTO(Catalog catalog);

    @Mapping(target = "active", constant = "true")
    public abstract List<OptionDTO> mapToListOptionDTO(List<Catalog> catalogList);
}
