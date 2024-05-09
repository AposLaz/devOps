package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import lgl.bayern.de.ecertby.dto.AuditDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.model.Audit;
import lgl.bayern.de.ecertby.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@Transactional
@RestController
@RequestMapping("audit")
@RequiredArgsConstructor
public class AuditResource {
    private final AuditService auditService;

    /**
     * Find all audits with some criteria.
     *
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested audits paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve audits list.")
    @OperationAccess(
            operations = {VIEW_PROTOCOL}
    )
    public Page<AuditDTO> findAll(@QuerydslPredicate(root = Audit.class) Predicate predicate,
                                  Pageable pageable,
                                  @RequestParam(value = "dateFrom", required = false) Instant dateFrom,
                                  @RequestParam(value = "dateTo", required = false) Instant dateTo) {
        return auditService.findAll(predicate, pageable, dateFrom, dateTo);
    }


    /**
     * Find all certificate audits with some criteria.
     *
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested audits paged and sorted.
     */
    @GetMapping(path = "findAllCertificateAudit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch audit.")
    @ResourceAccess(
            operations = {NEW_CERTIFICATE, VIEW_CERTIFICATE, EDIT_CERTIFICATE}
    )
    public Page<AuditDTO> findAllCertificateAudit(@QuerydslPredicate(root = Audit.class) Predicate predicate,
                                                  Pageable pageable,
                                                  @RequestParam(value = "certificateId", required = false) String certificateId,
                                                  @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return auditService.findAllCertificateAudit(predicate, pageable, certificateId, selectionFromDD);
    }

    @OperationAccess(
            operations = {VIEW_LAST_MODIFIED_BY}
    )
    @GetMapping(path = "findAuditForEntity/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch audit.")
    public AuditDTO findAuditForEntity(@PathVariable String entityId) {
        return auditService.findAuditForEntity(entityId);
    }

}
