package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.HtmlElementDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)//, uses = {AttributeMapper.class, TemplateMapper.class})
public abstract class HtmlElementMapper extends BaseMapper<HtmlElementDTO, HtmlElement, QHtmlElement> {


}
