package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.CompanyProfile;
import lgl.bayern.de.ecertby.repository.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

/**
 * Validator for the CompanyProfile
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class CompanyProfileValidator {

    private final CompanyProfileRepository companyProfileRepository;

    private final String ERROR_NAME_EXISTS_PROFILE = "error_name_exists_profile";
    /**
     * Validates the Profile save for all the business rules.
     * @param companyProfileDTO The Profile
     */
    public void validateCompanyProfile(CompanyProfileDTO companyProfileDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();

        // Validate if profile name already exists.
        nameExists(companyProfileDTO.getProfileName(), companyProfileDTO.getId(),companyProfileDTO.getCompanyId() ,errors);
        
        if(!errors.isEmpty()){
            throw new QCouldNotSaveException("Errors for Saving Company Profile", new EcertBYGeneralException(errors));
        }
    }

    /**
     * Check if name already exists.
     * @param name The name to check.
     * @param companyProfileId The companyProfile id.
     * @param errors The errors list to update.
     */
    private void nameExists(String name, String companyProfileId, String companyId, List<EcertBYErrorException> errors) {

        CompanyProfile existingProfile = companyProfileRepository.findByProfileNameAndCompanyId(name, companyId);
        if (companyProfileId == null && existingProfile != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_PROFILE, ERROR_NAME_EXISTS_PROFILE, "profileName", "companyProfileDTO", null, true));
        }
        if (companyProfileId != null  && existingProfile != null && !existingProfile.getId().equals(companyProfileId)) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS_PROFILE, ERROR_NAME_EXISTS_PROFILE, "profileName", "companyProfileDTO", null, true));
        }
    }

    public boolean validateRequest(CompanyDTO companyDTO, String actingId) {
        // An admin, a responsible authority or the company itself can act on the profile
        return actingId.equals(ADMIN_RESOURCE) || actingId.equals(companyDTO.getId()) ||
                (
                        actingId.equals(companyDTO.getResponsibleAuthority().getId()) ||
                                actingId.equals(companyDTO.getPreResponsibleAuthority().getId()) ||
                                actingId.equals(companyDTO.getPostResponsibleAuthority().getId())
                );
    }
}
