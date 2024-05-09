package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.QTeam;
import lgl.bayern.de.ecertby.model.Team;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends BaseRepository<Team, QTeam> {


   Team findByNameAndAuthority_Id(String name, String authorityId);

   Team findByNameAndCompany_Id(String name, String companyId);

   Team findByNameAndAuthority_Id_AndIdNot(String name, String authorityId, String teamId);

   Team findByNameAndCompany_Id_AndIdNot(String name, String companyId,  String teamId);

   void deleteTeamByCompany_Id(String companyId);
   List<Team> findAllByCompanyId(String companyId);
   List<Team> findAllByAuthorityId(String authorityId);

   @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
           "FROM TeamDepartment td " +
           "WHERE td.department.id = :id")
   boolean existsTeamDepartmentById(@Param("id") String id);

   @Query("SELECT t FROM Team t " +
           "CROSS JOIN CertificateAssignmentHistory h " +
           "INNER JOIN CertificateAssignmentHistoryTeam ht ON ht.team.id = t.id " +
           "WHERE h.id = :historyId")
   List<Team> findAllPastAssignedCompanyTeamsOfCertificate(@Param("historyId") String historyId);

   @Override
   default void customize(QuerydslBindings bindings, QTeam team) {
      BaseRepository.super.customize(bindings, team);
      bindings.bind(team.department.any().department.id).first((path, value) -> {
         List<String> departmentCatalogueIdList = List.of(value.split(","));
         if (departmentCatalogueIdList.size() == 1) {
            return path.eq(departmentCatalogueIdList.get(0));
         } else {
            return path.in(departmentCatalogueIdList);
         }
      });

   }
}
