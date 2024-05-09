package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

/**
 * Validator for the Company.
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class CompanyValidator {

    private final CompanyRepository companyRepository;
    private final CertificateRepository certificateRepository;

    private static final String ERROR_NAME_EXISTS_COMPANY = "error_name_exists_company";
    private static final String ERROR_AT_LEAST_ONE_SELECTION_INACTIVE = "error_at_least_one_selection_inactive";

    /**
     * Validates the company save for all the business rules.
     * @param companyDTO The Authority
     */
    public void validateCompany(CompanyDTO companyDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();

        // Validate if name already exists.
        nameExists(companyDTO.getName(), companyDTO.getId(), errors);

        if(!errors.isEmpty()){
            throw new QCouldNotSaveException("Errors for Saving Company", new EcertBYGeneralException(errors));
        }
    }

    /**
     * Check if name already exists.
     * @param name The name to check.
     * @param companyId The company id.
     * @param errors The errors list to update.
     */
    private void nameExists(String name, String companyId, List<EcertBYErrorException> errors) {
        if (companyId == null && companyRepository.findByName(name) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_COMPANY, ERROR_NAME_EXISTS_COMPANY, "name", "companyDTO", null, true));
        }
        if (companyId != null && companyRepository.findByNameAndIdNot(name, companyId) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_COMPANY, ERROR_NAME_EXISTS_COMPANY, "name", "companyDTO", null, true));
        }
    }
    public void validateIsAuthorityActive(CompanyDTO company, List<EcertBYErrorException> errors) {
       if(!company.getPostResponsibleAuthority().isActive()) {
           errors.add(new EcertBYErrorException(ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, "postResponsibleAuthority", "companyDTO", null, true));
       }
        if(!company.getResponsibleAuthority().isActive()) {
            errors.add(new EcertBYErrorException(ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, "responsibleAuthority", "companyDTO", null, true));
        }
        if(!company.getPreResponsibleAuthority().isActive()) {
            errors.add(new EcertBYErrorException(ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, ERROR_AT_LEAST_ONE_SELECTION_INACTIVE, "preResponsibleAuthority", "companyDTO", null, true));
        }
    }
    public void validateDeleteCompany(String id,List<EcertBYErrorException> errors) {
        if (!certificateRepository.findCertificateByCompany(id).isEmpty()) {
            String errorMessage = "error_company_delete_linked_with_certificate";
            errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
        }
    }

    public void validateDeactivateCompany(String id, List<EcertBYErrorException> errors) {
        List<CertificateStatus> statusesToCheck = Arrays.asList(CertificateStatus.FORWARDED,
                CertificateStatus.PRE_CERTIFICATE_FORWARDED);
        if (!certificateRepository.findCertificateByCompanyAndStatus(id, statusesToCheck).isEmpty()) {
            String errorMessage = "error_company_deactivate_linked_with_certificate";
            errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
        }
    }

    public void validateActivateCompany(String id, List<EcertBYErrorException> errors) {
        boolean areAllAuthoritiesActive = companyRepository.areAllAuthoritiesOfCompanyActive(id);
        if (!areAllAuthoritiesActive) {
            String errorMessage = "error_company_activate_with_inactive_authority";
            errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
        }
    }

    public boolean validateEditRequest(CompanyDTO companyDTO, String actingId) {
        Optional<Company> optionalCompany = companyRepository.findById(companyDTO.getId());
        // An admin, a responsible authority or the company itself can edit a not-deleted company
        return optionalCompany.filter(company ->
                actingId.equals(ADMIN_RESOURCE) || actingId.equals(companyDTO.getId()) ||
                (
                    (actingId.equals(companyDTO.getResponsibleAuthority().getId()) ||
                    actingId.equals(companyDTO.getPreResponsibleAuthority().getId()) ||
                    actingId.equals(companyDTO.getPostResponsibleAuthority().getId())) &&
                    !company.isDeleted()
                )
        ).isPresent();
    }

    public boolean validateRequest(CompanyDTO companyDTO, String actingId) {
        // An admin or a responsible authority can activate/deactivate/delete a company
        return actingId.equals(ADMIN_RESOURCE) ||
                (
                        actingId.equals(companyDTO.getResponsibleAuthority().getId()) ||
                        actingId.equals(companyDTO.getPreResponsibleAuthority().getId()) ||
                        actingId.equals(companyDTO.getPostResponsibleAuthority().getId())
                );
    }
}
