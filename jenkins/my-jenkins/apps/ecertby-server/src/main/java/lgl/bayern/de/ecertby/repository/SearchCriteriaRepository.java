package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.QSearchCriteria;
import lgl.bayern.de.ecertby.model.SearchCriteria;
import lgl.bayern.de.ecertby.model.util.GroupType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchCriteriaRepository extends BaseRepository<SearchCriteria, QSearchCriteria> {


    @Query(
            "SELECT sc " +
                    "FROM SearchCriteria sc " +
                    "LEFT OUTER JOIN sc.searchCriteriaGroupSet scg " +
                    "WHERE (scg.groupType = lgl.bayern.de.ecertby.model.util.GroupType.PERSONAL AND sc.createdBy = :userId) " +
                    "OR (scg.groupType = lgl.bayern.de.ecertby.model.util.GroupType.GLOBAL ) " +
                    "OR (scg.groupType = lgl.bayern.de.ecertby.model.util.GroupType.ADMIN AND sc.createdBy IS NULL)"
    )
    List<SearchCriteria> findUserSearchCriteriaForAdmin(@Param("userId") String userId);


    @Query(
            "SELECT sc " +
                    "FROM SearchCriteria sc " +
                    "LEFT OUTER JOIN sc.searchCriteriaGroupSet scg " +
                    "WHERE (scg.groupType = lgl.bayern.de.ecertby.model.util.GroupType.PERSONAL AND sc.createdBy = :userId) " +
                    "OR (scg.groupType = lgl.bayern.de.ecertby.model.util.GroupType.GLOBAL ) " +
                    "OR (scg.groupType = :groupType AND sc.createdBy = :selectionFromDD)" +
                    "OR (scg.groupType = :groupType AND sc.createdByGroupType = lgl.bayern.de.ecertby.model.util.GroupType.ADMIN)"
    )
    List<SearchCriteria> findUserSearchCriteria(@Param("userId") String userId, @Param("selectionFromDD") String selectionFromDD, @Param("groupType") GroupType groupType);
}
