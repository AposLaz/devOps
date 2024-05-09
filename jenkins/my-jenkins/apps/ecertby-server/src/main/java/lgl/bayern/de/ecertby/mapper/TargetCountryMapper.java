package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.TargetCountryDTO;
import lgl.bayern.de.ecertby.model.TargetCountry;
import lgl.bayern.de.ecertby.model.QTargetCountry;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TargetCountryMapper extends BaseMapper<TargetCountryDTO, TargetCountry, QTargetCountry> {
}
