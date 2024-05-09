package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.model.CertificateAssignmentHistory;
import lgl.bayern.de.ecertby.model.QCertificate;
import lgl.bayern.de.ecertby.model.QCertificateAssignmentHistory;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateAssignmentHistoryRepository extends BaseRepository<CertificateAssignmentHistory, QCertificateAssignmentHistory> {


}
