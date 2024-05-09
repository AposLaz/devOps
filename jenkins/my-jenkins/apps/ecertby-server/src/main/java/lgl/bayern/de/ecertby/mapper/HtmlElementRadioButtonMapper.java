package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.HtmlElementRadioButtonDTO;
import lgl.bayern.de.ecertby.model.HtmlElementRadioButton;
import lgl.bayern.de.ecertby.model.QHtmlElementRadioButton;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class HtmlElementRadioButtonMapper extends BaseMapper<HtmlElementRadioButtonDTO, HtmlElementRadioButton, QHtmlElementRadioButton> {

}
