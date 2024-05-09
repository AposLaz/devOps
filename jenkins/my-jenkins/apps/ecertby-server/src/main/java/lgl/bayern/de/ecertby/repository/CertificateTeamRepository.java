package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.CertificateTeam;
import lgl.bayern.de.ecertby.model.QCertificateTeam;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateTeamRepository extends BaseRepository<CertificateTeam, QCertificateTeam> {
   

   List<CertificateTeam> findByTeam_Id(String teamId);

   @Query("SELECT c.assignedTeamSet " +
           "FROM Certificate c " +
           "WHERE c.id = :certificateId")
   List<CertificateTeam> findCertificateTeamsByCertificateId(@Param("certificateId") String certificateId);

}
