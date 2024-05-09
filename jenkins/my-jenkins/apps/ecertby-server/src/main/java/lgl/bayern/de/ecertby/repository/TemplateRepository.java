package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TemplateRepository extends BaseRepository<Template, QTemplate> {
    Template findByTemplateName(String templateName);

    @Query("SELECT template.product " +
           "FROM Template template " +
           "WHERE template.targetCountry.id = :targetCountryId")
    List<CatalogValue> getProductsByTargetCountryId(@Param("targetCountryId") String targetCountryId);

    Template findByTemplateNameAndIdNot(String templateName, String templateId);

    Template findByTargetCountryIdAndProductId(String targetCountryId, String productId);

    Template findByTargetCountryIdAndProductIdAndIdNot(String targetCountryId, String productId, String templateId);

    @Query("SELECT t.keyword " +
            "FROM Template t " +
            "WHERE t.id = :templateId")
    List<TemplateKeyword> findTemplateKeywordsById(@Param("templateId") String templateId);
    @Query("SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END " +
            "FROM TemplateKeyword tk")
    boolean templateKeywordsExist();


    @Query("SELECT t.comment " +
            "FROM Template t " +
            "WHERE t.id = :templateId")
    String findTemplateCommentById(@Param("templateId") String templateId);

    @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
            "FROM TemplateDepartment td " +
            "WHERE td.department.id = :id")
    boolean existsTemplateDepartmentById(@Param("id") String id);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Template t " +
            "WHERE t.product.id = :id")
    boolean existsTemplateProductById(@Param("id") String id);

    @Query("SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END " +
            "FROM TemplateKeyword tk " +
            "WHERE tk.keyword.id = :id")
    boolean existsTemplateKeywordById(@Param("id") String id);

    @Override
    default void customize(QuerydslBindings bindings, QTemplate template) {
        // Call the common customization first
        BaseRepository.super.customize(bindings, template);
        bindings.bind(template.department.any().department.id).first((path, value) -> {
            List<String> departmentCatalogueIdList = List.of(value.split(","));
            if(departmentCatalogueIdList.size() == 1){
                return path.eq(departmentCatalogueIdList.get(0));
            } else {
                return path.in(departmentCatalogueIdList);
            }
        });
        bindings.bind(template.validFrom).first((path, value) ->
                template.validFrom.goe(value));
        bindings.bind(template.validTo).first((path, value) ->
                template.validTo.loe(value).or(template.validTo.isNull()));
    }
}
