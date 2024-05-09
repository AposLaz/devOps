package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.QTask;
import lgl.bayern.de.ecertby.model.Task;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.TaskAction;
import lgl.bayern.de.ecertby.model.util.TaskType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends BaseRepository<Task, QTask> {

    List<Task> findAllByCertificateId(String certificateId);
    List<Task> findAllByCertificateIdAndCertificateStatusNot(String certificateId, CertificateStatus status);
    List<Task> findAllByCertificateIdAndType(String certificateId, TaskType type);

    @Query("SELECT t " +
           "FROM Task t " +
           "INNER JOIN Certificate c " +
           "   ON t.certificate.parentCertificate.id = c.id " +
           "WHERE c.id = :parentCertificateId " +
           "   AND t.type = :type")
    List<Task> findAllPreCertificateTasksByParentIdAndType(@Param("parentCertificateId")String certificateId, @Param("type")TaskType type);

    @Query("SELECT COUNT(t) > 0 " +
            "FROM Task t " +
            "INNER JOIN Certificate c " +
            "   ON t.certificate.parentCertificate.id = c.id " +
            "WHERE c.id = :parentCertificateId " +
            "   AND t.type = :type " +
            "   AND t.action = :action")
    boolean existsPreCertificateTaskByParentIdAndTypeAndAction(@Param("parentCertificateId")String certificateId, @Param("type")TaskType type, @Param("action")TaskAction action);

    void deleteAllByCertificateId(String certificateId);
}
