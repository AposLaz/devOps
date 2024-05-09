package lgl.bayern.de.ecertby.validator;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Validator for Certificates.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class CertificateValidator {
    private static final String ERROR_AT_LEAST_ONE_SELECTION_INACTIVE = "error_at_least_one_selection_inactive";
    public boolean validateRequest(CertificateDTO certificateDTO, String actingId, String userType) {
        // Validations according to filterings of findAll in CertificateService
        if (userType.equals(UserType.COMPANY_USER.toString())) {
            return certificateDTO.getCompany().getId().equals(actingId) &&
                    !CertificateStatus.getExcludedStatuses().contains(certificateDTO.getStatus());
        }
        else if(userType.equals(UserType.AUTHORITY_USER.toString())){
            return certificateDTO.getForwardAuthority() != null && (
                    certificateDTO.getForwardAuthority().getId().equals(actingId) &&
                            !CertificateStatus.getIssuingAuthorityExcludedStatuses().contains(certificateDTO.getStatus())
            );
        }
        else if(userType.equals(UserType.ADMIN_USER.toString())){
            return !CertificateStatus.getExcludedStatusesForAdmin().contains(certificateDTO.getStatus());
        }
        return false;
    }

    public boolean validateCopyRequest(CertificateDTO certificateDTO, String actingId, String userType) {
        return userType.equals(UserType.COMPANY_USER.toString()) &&
                certificateDTO.getCompany().getId().equals(actingId) &&
                !CertificateStatus.getExcludedStatuses().contains(certificateDTO.getStatus());
    }

    public void validateIsEmployeeActive(CertificateDTO certificateDTO, List<EcertBYErrorException> errors) {
        if(certificateDTO.getAssignedEmployee() != null && !certificateDTO.getAssignedEmployee().isActive()) {
            errors.add(new EcertBYErrorException(ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, "assignedEmployee", "certificateDTO", null, true));
        }
    }

    public void validateIsCertificateDeleted(CertificateDTO certificateDTO, List<EcertBYErrorException> errors) {
        if (certificateDTO.getStatus().equals(CertificateStatus.DELETED)) {
            errors.add(new EcertBYErrorException("error_update_certificate_deleted", "error_update_certificate_deleted", null, null, null, true));
        }

    }

    public void validateStatusTransition(CertificateDTO certificateDTO , CertificateStatus nextStatus , boolean isActionRejection) {
        boolean isPreCert = certificateDTO.getParentCertificate() != null;
        CertificateStatus currentStatus = certificateDTO.getStatus();
        if(isActionRejection) {
            validateStatusTransitionToRejected(certificateDTO,currentStatus,isPreCert);
        } else {
            if (isPreCert) {
                validateStatusTransitionPreCert(certificateDTO.getId(),nextStatus, currentStatus);
            } else {
                validateStatusTransitionCert(certificateDTO, nextStatus, currentStatus);
            }
        }
    }


    private void validateStatusTransitionToRejected(CertificateDTO certificateDTO, CertificateStatus currentStatus, boolean isPreCert) {
        if(isPreCert) {
            if(!currentStatus.equals(CertificateStatus.PRE_CERTIFICATE_FORWARDED)) {
                log.info("Attempting to reject a pre-certificate with : id {}  with invalid status. Current status: {}", certificateDTO.getId(),currentStatus);
                throw new NotAllowedException("Invalid status transition. Pre certificate must be in forwarded status to be rejected.");
            }
        } else {
            if(!currentStatus.equals(CertificateStatus.FORWARDED) || !certificateDTO.getCompletedForward()) {
                log.info("Attempting to reject a certificate with : id {}  with invalid status. Current status: {}", certificateDTO.getId(),currentStatus);
                throw new NotAllowedException("Invalid status transition. Certificate must be in Forwarded status to be Rejected.");
            }
        }
    }

    private void validateStatusTransitionCert(CertificateDTO certificateDTO, CertificateStatus nextStatus , CertificateStatus currentStatus) {
        if((nextStatus.equals(CertificateStatus.RELEASED)
                || (nextStatus.equals(CertificateStatus.BLOCKED)))
                && (!currentStatus.equals(CertificateStatus.FORWARDED) || !certificateDTO.getCompletedForward())) {
            log.info("Attempting to release or block Certificate with id : {} Current status: {}, Next status: {}", certificateDTO.getId(),currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Certificate must be in Forwarded status to be Released,Blocked.");
        }
        if((nextStatus.equals(CertificateStatus.LOST) || nextStatus.equals(CertificateStatus.REVOKED)) && !currentStatus.equals(CertificateStatus.RELEASED)) {
            log.info("Attempting to revoke or mark Certificate  with id : {} as lost. Current status: {}, Next status: {}", certificateDTO.getId(),currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Certificate must be in released status to be marked as lost or revoked.");
        }
        if(nextStatus.equals(CertificateStatus.FORWARDED) && (!currentStatus.equals(CertificateStatus.FORWARDED_PRE_CERTIFICATE_REJECTED) && !currentStatus.equals(CertificateStatus.DRAFT))) {
            log.info("Attempting to forward Certificate with id : {} Current status: {}, Next status: {}", certificateDTO.getId(),currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Certificate must be in Draft or Forwarded Precertificate Rejected status to be forwarded.");
        }
        if(nextStatus.equals(CertificateStatus.DELETED) && !currentStatus.equals(CertificateStatus.DRAFT)) {
            log.info("Attempting to delete Certificate with id : {} Current status: {}, Next status: {}", certificateDTO.getId(),currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Certificate must be in Draft status to be deleted.");
        }
        if(nextStatus.equals(CertificateStatus.DRAFT) && !currentStatus.equals(CertificateStatus.DELETED)) {
            log.info("Attempting to make  Certificate with id : {} draft. Current status: {}, Next status: {}", certificateDTO.getId(),currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Certificate must be in Deleted status to be draft.");
        }

    }
    private void validateStatusTransitionPreCert(String certificateId ,CertificateStatus nextStatus,CertificateStatus currentStatus) {
        if(nextStatus.equals(CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE) && !currentStatus.equals(CertificateStatus.PRE_CERTIFICATE_FORWARDED)) {
            log.info("Attempting to vote positive on preCertificate with id : {} Current status: {}, Next status: {}", certificateId,currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Pre certificate must be in forwarded status to be voted positive.");
        }
        if(nextStatus.equals(CertificateStatus.PRE_CERTIFICATE_EXCLUDED) && !currentStatus.equals(CertificateStatus.PRE_CERTIFICATE_REJECTED)) {
            log.info("Attempting to exclude preCertificate with id : {} Current status: {}, Next status: {}", certificateId,currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Pre certificate must be in rejected status to be excluded.");
        }
        if(nextStatus.equals(CertificateStatus.PRE_CERTIFICATE_FORWARDED) && (!currentStatus.equals(CertificateStatus.PRE_CERTIFICATE_DRAFT) && !currentStatus.equals(CertificateStatus.PRE_CERTIFICATE_REJECTED))) {
            log.info("Attempting to forward  preCertificate with id : {} Current status: {}, Next status: {}", certificateId,currentStatus, nextStatus);
            throw new NotAllowedException("Invalid status transition. Pre certificate must be in rejected or draft status to be forwarded.");
        }
    }
}
