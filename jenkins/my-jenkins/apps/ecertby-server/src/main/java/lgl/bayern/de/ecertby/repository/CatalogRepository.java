package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.model.QCatalog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogRepository extends BaseRepository<Catalog, QCatalog> {
    Catalog findByName(String name);

    @Query("SELECT c.id " +
            "FROM Catalog c " +
            "WHERE c.name = :name")
    String findIdByName(@Param("name") String name);

    Catalog findByNameAndIdNot(String name, String id);

    @Query("SELECT COUNT(v) " +
            "FROM CatalogValue v " +
            "WHERE v.catalog.id = :catalogId")
    int countAllValuesById(@Param("catalogId") String catalogId);

    @Query("SELECT c.mandatory " +
            "FROM Catalog c " +
            "WHERE c.id = :catalogId")
    boolean catalogIsMandatory(@Param("catalogId") String catalogId);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
            "FROM HtmlElement h " +
            "WHERE h.catalog.id = :id")
    boolean catalogIsReferenced(@Param("id") String id);
}
