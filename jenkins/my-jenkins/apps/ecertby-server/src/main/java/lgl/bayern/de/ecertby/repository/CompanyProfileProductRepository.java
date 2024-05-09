package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.CompanyProfileProduct;
import lgl.bayern.de.ecertby.model.QCompanyProfileProduct;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyProfileProductRepository extends BaseRepository<CompanyProfileProduct, QCompanyProfileProduct> {
    @Query("SELECT CASE WHEN COUNT(cpd) > 0 THEN true ELSE false END " +
            "FROM CompanyProfileProduct cpd " +
            "WHERE cpd.product.id = :id")
    boolean existsCompanyProfileProductById(@Param("id") String id);
}
