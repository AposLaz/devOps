package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Attribute;
import lgl.bayern.de.ecertby.model.QAttribute;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends BaseRepository<Attribute, QAttribute> {

    Attribute findByName(String attributeName);

    Attribute findByNameAndIdNot(String attributeName, String attributeId);
}
