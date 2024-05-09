package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.*;

import java.util.List;

public interface HtmlElementRepository extends BaseRepository<HtmlElement, QHtmlElement> {

    List<HtmlElement> findByTemplateId(String templateId);

    boolean existsByAttributeId(String attributeId);
}
