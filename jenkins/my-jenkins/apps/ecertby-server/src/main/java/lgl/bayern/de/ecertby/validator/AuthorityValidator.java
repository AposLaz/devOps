package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.repository.AuthorityRepository;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

/**
 * Validator for the Authority.
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class AuthorityValidator {

    private final AuthorityRepository authorityRepository;
    private final CompanyRepository companyRepository;
    private final CertificateRepository certificateRepository;

    private static final String ERROR_NAME_EXISTS_AUTHORITY = "error_name_exists_authority";

    /**
     * Validates the authority save for all the business rules.
     * @param authorityDTO The Authority
     */
    public void validateAuthority(AuthorityDTO authorityDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();

        // Validate if authority name already exists.
        nameExists(authorityDTO.getName(), authorityDTO.getId(), errors);
        
        if(!errors.isEmpty()){
            throw new QCouldNotSaveException("Errors for Saving Authority", new EcertBYGeneralException(errors));
        }
    }

    /**
     * Check if name already exists.
     * @param name The name to check.
     * @param authorityId The authority id.
     * @param errors The errors list to update.
     */
    private void nameExists(String name, String authorityId, List<EcertBYErrorException> errors) {
        if (authorityId == null && authorityRepository.findByName(name) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_AUTHORITY, ERROR_NAME_EXISTS_AUTHORITY, "name", "authorityDTO", null, true));
        }
        if (authorityId != null && authorityRepository.findByNameAndIdNot(name, authorityId) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_AUTHORITY, ERROR_NAME_EXISTS_AUTHORITY, "name", "authorityDTO", null, true));
        }
    }

    public void validateDeactivateAuthority(String id,List<EcertBYErrorException> errors) {
        boolean isLinkedWithCompany = companyRepository.existsCompaniesLinkedWithAuthority(id);

        List<CertificateStatus> blockingStatusList = List.of(
                CertificateStatus.FORWARDED, CertificateStatus.PRE_CERTIFICATE_FORWARDED);
        boolean isBlockedByCertificate = certificateRepository.existsByForwardAuthorityIdAndStatusIn(id, blockingStatusList);

        if (isLinkedWithCompany) {
            String errorMessage = "error_deactivate_authority_company_exist";
            errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
        } else if (isBlockedByCertificate) {
            String errorMessage = "error_authority_deactivate_linked_with_certificate";
            errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
        }

    }
    public boolean validateCreateEditRequest(AuthorityDTO authorityDTO, String actingId) {
        // An admin or the authority itself can edit an authority
        return actingId.equals(ADMIN_RESOURCE) || actingId.equals(authorityDTO.getId());
    }
}
