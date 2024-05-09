package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.QCatalogValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogValueRepository  extends BaseRepository<CatalogValue, QCatalogValue> {

    List<CatalogValue> findByCatalog_NameOrderByData(String catalogName);
    @Query("SELECT cv.id " +
            "FROM CatalogValue cv " +
            "WHERE cv.catalog.id = :catalogId")
    List<String> findAllCatalogValueIdsFromCatalogId(@Param("catalogId") String catalogId);
    List<CatalogValue> findAllByCatalog_IdOrderByData(String catalogId);
    @Query("SELECT cv.catalog.id " +
            "FROM CatalogValue cv " +
            "WHERE cv.id = :valueId")
    String findCatalogByCatalogValueId(String valueId);
    boolean existsByData(String data);
    CatalogValue findByDataAndCatalog_Id(String data, String catalogId);
}
