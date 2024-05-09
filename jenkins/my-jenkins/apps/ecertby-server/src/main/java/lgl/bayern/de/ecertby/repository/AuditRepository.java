package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Audit;
import lgl.bayern.de.ecertby.model.QAudit;
import lgl.bayern.de.ecertby.model.UserDetail;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends BaseRepository<Audit, QAudit> {
    List<Audit> findAllByUserDetail(UserDetail userDetail);

    Audit findFirstByEntityIdOrderByCreatedOnDesc(String entityId);

}
