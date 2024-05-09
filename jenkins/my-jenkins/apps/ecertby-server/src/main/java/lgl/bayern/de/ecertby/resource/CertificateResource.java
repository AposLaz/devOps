package lgl.bayern.de.ecertby.resource;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.AUTHORITY_FORWARD_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.BLOCK_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.DELETE_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EXCLUDE_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.FORWARD_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.MARK_CERTIFICATE_AS_LOST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.REJECT_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.RELEASE_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.REVOKE_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_CERTIFICATES_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_RECYCLE_BIN;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VOTE_POSITIVE;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lgl.bayern.de.ecertby.dto.CertificateAuthorityToAuthorityForwardDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CertificateForwardAuthorityDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Certificate;
import lgl.bayern.de.ecertby.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Validated
@Transactional
@RestController
@RequestMapping("certificate")
@RequiredArgsConstructor
public class CertificateResource {
    private final CertificateService certificateService;

    /**
     * Fetches certificates according the given criteria.
     * @param predicate The given criteria.
     * @param pageable The page to fetch.
     * @param shippingDateFrom The 'from' shipping date.
     * @param shippingDateTo The 'to' shipping date.
     * @param printedDateFrom The 'from' printed date.
     * @param printedDateTo The 'to' printed date.
     * @param transferredDateFrom The 'from' transferred date.
     * @param transferredDateTo The 'to' transferred date.
     * @param statusChangeDateFrom The 'from' status change date.
     * @param statusChangeDateTo The 'to' status change date.
     * @param selectionFromDD The selected authority/company of the logged-in user.
     * @return The certificate list.
     */
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve certificates list.")
    @ResourceAccess(
            operations = { VIEW_CERTIFICATES_LIST }
    )
    public Page<CertificateDTO> findAll(@QuerydslPredicate(root = Certificate.class) Predicate predicate,
                                        Pageable pageable,
                                        @RequestParam(value = "shippingDateFrom", required = false) Instant shippingDateFrom,
                                        @RequestParam(value = "shippingDateTo", required = false) Instant shippingDateTo,
                                        @RequestParam(value = "printedDateFrom", required = false) Instant printedDateFrom,
                                        @RequestParam(value = "printedDateTo", required = false) Instant printedDateTo,
                                        @RequestParam(value = "transferredDateFrom", required = false) Instant transferredDateFrom,
                                        @RequestParam(value = "transferredDateTo", required = false) Instant transferredDateTo,
                                        @RequestParam(value = "statusChangeDateFrom", required = false) Instant statusChangeDateFrom,
                                        @RequestParam(value = "statusChangeDateTo", required = false) Instant statusChangeDateTo,
                                        @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.findAll(predicate, pageable, shippingDateFrom, shippingDateTo, printedDateFrom, printedDateTo, transferredDateFrom, transferredDateTo, statusChangeDateFrom, statusChangeDateTo, selectionFromDD);
    }

    /**
     * Fetches deleted certificates according to given criteria.
    */
    @EmptyPredicateCheck
    @GetMapping(path="recycle_bin", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve recycle bin.")
    @ResourceAccess(
            operations = { VIEW_RECYCLE_BIN }
    )
    public Page<CertificateDTO> findAllDeleted(@QuerydslPredicate(root = Certificate.class) Predicate predicate,
                                               Pageable pageable,
                                               @RequestParam(value = "shippingDateFrom", required = false) Instant shippingDateFrom,
                                               @RequestParam(value = "shippingDateTo", required = false) Instant shippingDateTo,
                                               @RequestParam(value = "printedDateFrom", required = false) Instant printedDateFrom,
                                               @RequestParam(value = "printedDateTo", required = false) Instant printedDateTo,
                                               @RequestParam(value = "transferredDateFrom", required = false) Instant transferredDateFrom,
                                               @RequestParam(value = "transferredDateTo", required = false) Instant transferredDateTo,
                                               @RequestParam(value = "statusChangeDateFrom", required = false) Instant statusChangeDateFrom,
                                               @RequestParam(value = "statusChangeDateTo", required = false) Instant statusChangeDateTo,
                                               @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.findAllDeleted(predicate, pageable, shippingDateFrom, shippingDateTo, printedDateFrom, printedDateTo, transferredDateFrom, transferredDateTo, statusChangeDateFrom, statusChangeDateTo, selectionFromDD);
    }

    /**
     * Save certificate.
     * @param certificateDTO The object with the information of the certificate.
     * @param request
     */
    @PostMapping(path = "create",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResourceAccess(
            operations = { NEW_CERTIFICATE }
    )
    public CertificateDTO create(@RequestPart("certificateDTO") CertificateDTO certificateDTO,
                       MultipartHttpServletRequest request) {
        return certificateService.createCertificate(certificateDTO,request);
    }

    /**
     * Update certificate.
     * @param certificateDTO The object with the information of the certificate.
     * @param request
     */
    @PostMapping(path = "update",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE }
    )
    public CertificateDTO update(@RequestPart("certificateDTO") CertificateDTO certificateDTO,
                       MultipartHttpServletRequest request) {
        return certificateService.updateCertificate(certificateDTO,request);
    }


    @PostMapping(path = "additionalDocs/offline/save",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE }
    )
    public Map<String, Boolean> saveAdditionalDocsFromOffline(@RequestPart("documents") List<DocumentDTO> documents,
                                                              MultipartHttpServletRequest request ,  @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.saveAdditionalDocumentsFromOffline(documents,request, selectionFromDD);
    }
    /**
     * Get certificate.
     * @param id The id of the certificate.
     * @return The certificate with the given id, as CertificateDTO.
     */
    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch certificate.")
    @ResourceAccess(
            operations = { VIEW_CERTIFICATE }
    )
    public CertificateDTO get(@PathVariable String id,
                                             @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.getCertificateWithFiles(id, selectionFromDD);
    }

    /**
     * Release certificate.
     * @param id The id of the certificate to be released.
     */
    @PatchMapping(path = "{id}/release", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not release certificate.")
    @ResourceAccess(
            operations = { RELEASE_CERTIFICATE }
    )
    public void release(@PathVariable String id,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.release(id, selectionFromDD);
    }

    /**
     * Reject certificate.
     * @param id The id of the certificate to reject.
     */
    @PatchMapping(path = "{id}/reject",consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not reject certificate.")
    @ResourceAccess(
            operations = { REJECT_CERTIFICATE }
    )
    public void reject(@PathVariable String id,
                       @RequestBody String reason,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.reject(id, reason, selectionFromDD);
    }

    /**
     * Mark certificate as lost.
     * @param id The id of the certificate to mark.
     */
    @PatchMapping(path = "{id}/mark_as_lost",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not mark certificate as lost.")
    @ResourceAccess(
            operations = { MARK_CERTIFICATE_AS_LOST }
    )
    public void markAsLost(@PathVariable String id,
                           @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.markAsLost(id, selectionFromDD);
    }

    /**
     * Revoke certificate.
     * @param id The id of the certificate to revoke.
     */
    @PatchMapping(path = "{id}/revoke",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not revoke certificate.")
    @ResourceAccess(
            operations = { REVOKE_CERTIFICATE }
    )
    public void revoke(@PathVariable String id,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.revoke(id, selectionFromDD);
    }

    /**
     * Block certificate.
     * @param id The id of the certificate to block.
     */
    @PatchMapping(path = "{id}/block",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not block certificate.")
    @ResourceAccess(
            operations = { BLOCK_CERTIFICATE }
    )
    public void block(@PathVariable String id,
                      @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.block(id, selectionFromDD);
    }

    /**
     * Mark certificate as deleted.
     * @param id The id of the certificate to mark as deleted.
     */
    @PatchMapping(path = "{id}/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete certificate.")
    @ResourceAccess(
            operations = { DELETE_CERTIFICATE }
    )
    public void delete(@PathVariable String id,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.delete(id, selectionFromDD);
    }

    /**
     * Votes positive pre-certificate.
     * @param id The id of the pre-certificate to vote positive.
     */
    @PatchMapping(path = "{id}/vote_positive",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not vote positive pre-certificate.")
    @ResourceAccess(
            operations = { VOTE_POSITIVE }
    )
    public void votePositive(@PathVariable String id,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.votePositive(id, selectionFromDD);
    }

    /**
     * Rejects pre-certificate
     * @param id The id of the pre-certificate to reject.
     * @param selectionFromDD Selection from dd.
     */
    @PatchMapping(path = "{id}/pre_certificate_reject",consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not reject pre-certificate.")
    @ResourceAccess(
            operations = { REJECT_CERTIFICATE }
    )
    public void rejectPreCertificate(@PathVariable String id,
                                     @RequestBody String reason,
                                     @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.rejectPreCertificate(id, reason, selectionFromDD);
    }

    /**
     * Forwards a pre-certificate.
     * @param id The id of the pre-certificate to forward.
     * @param selectionFromDD Selection from dd.
     */
    @PatchMapping(path = "{id}/pre_certificate_forwarded",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not forward pre-certificate.")
    @ResourceAccess(
            operations = { FORWARD_CERTIFICATE }
    )
    public void forwardPreCertificate(@PathVariable String id,
                                     @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.forwardPreCertificate(id, selectionFromDD);
    }

    /**
     * Excludes a pre-certificate.
     * @param id The id of the pre-certificate to forward.
     * @param selectionFromDD Selection from dd.
     */
    @PatchMapping(path = "{id}/pre_certificate_exclude",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not exclude pre-certificate.")
    @ResourceAccess(
            operations = { EXCLUDE_CERTIFICATE }
    )
    public void excludePreCertificate(@PathVariable String id,
                                     @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.excludePreCertificate(id, selectionFromDD);
    }

    /**
     * Restore deleted certificate.
     * @param id The id of the certificate to restore.
     */
    @PatchMapping(path = "{id}/restore_draft", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not restore draft certificate.")
    @ResourceAccess(
            operations = { DELETE_CERTIFICATE }
    )
    public void restore(@PathVariable String id,
                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        certificateService.restoreDraft(id, selectionFromDD);
    }

    /**
     * Get the keywords associated with a certificate.
     * @param certificateId The id of the certificate.
     * @return The keywords as a list of OptionDTOs.
     */
    @GetMapping(path = "keywords/{certificateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch keywords.")
    @ResourceAccess(
            operations = { VIEW_CERTIFICATE }
    )
    public List<OptionDTO> getCertificateKeywordsById(@PathVariable String certificateId,
                                                      @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.getCertificateKeywordsById(certificateId);
    }

    /**
     * Forward certificate.
     * @param id The id of the certificate to forward.
     */
    @PostMapping(path = "{id}/forward", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { FORWARD_CERTIFICATE }
    )
    public void forward(@Valid @RequestBody CertificateForwardAuthorityDTO certificateForwardAuthority,
                        @PathVariable String id) {
        certificateService.forward(id, certificateForwardAuthority);
    }

    /**
     * Endpoint for forwarding from authority to authority on a certificate.
     *
     * @param id                          The ID of the certificate.
     * @param certificateForwardAuthority The DTO containing information for forwarding authority.
     */
    @PostMapping(path = "{id}/forwardAuthority/{isPreCert}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { AUTHORITY_FORWARD_CERTIFICATE }
    )
    public void forwardAuthority(@PathVariable String id,@PathVariable boolean isPreCert,
                        @Valid @RequestBody CertificateAuthorityToAuthorityForwardDTO certificateForwardAuthority) {
        certificateService.forwardToAnotherAuthority(id, certificateForwardAuthority, isPreCert);
    }

    /**
     * Get the signing employee associated with a certificate.
     * @param certificateId The id of the certificate.
     * @return The employee as a one-sized list of OptionDTOs.
     */
    @GetMapping(path = "signing_employee/{certificateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch signing employee.")
    @ResourceAccess(
            operations = { EDIT_CERTIFICATE, VIEW_CERTIFICATE }
    )
    public OptionDTO getCertificateSigningEmployeeById(@PathVariable String certificateId,
                                                       @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.getCertificateSigningEmployeeById(certificateId);
    }

    /**
     * Get the list  of forwarded Certificates post Authority.
     * @return list of forwarded certificates.
     */
    @GetMapping(path = "getForwardedCertificates", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch forwarded Certificates.")
    @ResourceAccess(
            operations = { VIEW_CERTIFICATE , EDIT_CERTIFICATE }
    )
    public List<CertificateDTO> getForwardedCertificates(@RequestParam(name = "userAuthorities") List<String> userAuthorities,  @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.getForwardedCertificates(userAuthorities);
    }

    @PatchMapping(path = "{id}/copy", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not copy certificate.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE }
    )
    public CertificateDTO copy(@PathVariable String id,
                    @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return certificateService.copy(id, selectionFromDD);
    }
}
