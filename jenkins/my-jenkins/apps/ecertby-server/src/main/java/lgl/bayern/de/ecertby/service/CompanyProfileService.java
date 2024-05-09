package lgl.bayern.de.ecertby.service;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.mapper.CompanyProfileMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.model.CompanyProfile;
import lgl.bayern.de.ecertby.model.CompanyProfileProduct;
import lgl.bayern.de.ecertby.model.QCompanyProfile;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.repository.CatalogRepository;
import lgl.bayern.de.ecertby.repository.CompanyProfileRepository;
import lgl.bayern.de.ecertby.validator.CompanyProfileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CompanyProfileService extends BaseService<CompanyProfileDTO, CompanyProfile, QCompanyProfile> {

    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final CompanyService companyService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ObjectLockService objectLockService;

    @Autowired
    private final CompanyProfileRepository companyProfileRepository;

    @Autowired
    private final CatalogRepository catalogRepository;

    private final CompanyProfileValidator companyProfileValidator;

    private final CompanyProfileMapper profileMapper = Mappers.getMapper(CompanyProfileMapper.class);

    /**
     * Saves or updates a company profile based on the given CompanyProfileDTO.
     *
     * @param companyProfileDTO The CompanyProfileDTO containing profile information to be saved or updated.
     */
    public CompanyProfileDTO saveProfile(CompanyProfileDTO companyProfileDTO) {
        if (!companyProfileValidator.validateRequest(companyService.findById(companyProfileDTO.getCompanyId()), companyProfileDTO.getResourceId())) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to edit profile with id : {}.",
                    companyProfileDTO.getResourceId(),
                    companyProfileDTO.getCompanyId());
           throw new NotAllowedException("Profile Edit not allowed.");
        }
        objectLockService.checkAndThrowIfLocked(companyProfileDTO.getCompanyId(), ObjectType.COMPANY);
        companyProfileValidator.validateCompanyProfile(companyProfileDTO);
        CompanyDTO byId = companyService.findById(companyProfileDTO.getCompanyId());
        companyProfileDTO.setCompany(byId);
        CompanyProfile companyProfile = profileMapper.map(companyProfileDTO);
        if(companyProfile.getProduct() != null) {
            for (CompanyProfileProduct product : companyProfile.getProduct()) {
                Catalog catalog = catalogRepository.findByName(AppConstants.CatalogNames.PRODUCT);
                product.getProduct().setCatalog(catalog);
            }
        }

        CompanyProfileDTO oldProfile = null;
        if (!isNull(companyProfileDTO.getId())) {
            oldProfile = profileMapper.map(companyProfileRepository.findById(companyProfileDTO.getId()).get());
        }

        CompanyProfile savedProfile = companyProfileRepository.save(companyProfile);

        if (companyProfileDTO.getId() == null) {
            // LOG CREATION
            auditService.saveProfileAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), profileMapper.map(savedProfile));
        } else {
            // LOG UPDATE
            auditService.saveProfileAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), companyProfileDTO.getProfileName(),
                companyProfileDTO.getCompany().getName(), oldProfile, profileMapper.map(companyProfileRepository.findById(companyProfileDTO.getId()).get()));
        }
        log.info("Profile with id : {} saved successfully by user with id : {}." , savedProfile.getId(),securityService.getLoggedInUserDetailId());
        return profileMapper.map(savedProfile);
    }
    /**
     * Deletes a company profile by its ID and logs the deletion action.
     *
     * @param id The ID of the profile to be deleted.
     */
    public void deleteProfileById(String id,String selectionFromDD) {
        CompanyProfileDTO companyProfileDTO = findById(id);
        if (!companyProfileValidator.validateRequest(companyProfileDTO.getCompany(), selectionFromDD)) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to delete profile with id : {}.",
                    selectionFromDD,
                    companyProfileDTO.getId());
            throw new NotAllowedException("Profile deletion not allowed.");
        }
        objectLockService.checkAndThrowIfLocked(companyProfileDTO.getCompany().getId(), ObjectType.COMPANY);
        // LOG DELETION
        auditService.saveProfileAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), companyProfileDTO);

        deleteById(id);
        log.info("Profile with id : {} deleted successfully by user with id : {}." , id,securityService.getLoggedInUserDetailId());
    }
    /**
     * Activates or deactivates a company profile based on the given parameters and logs the action.
     *
     * @param isActive True to activate, false to deactivate the profile.
     * @param id The ID of the profile to be activated or deactivated.
     * @return True if the profile was successfully activated or deactivated, false otherwise.
     */
    public boolean activateProfile(boolean isActive, String id,String selectionFromDD) {
        CompanyProfileDTO companyProfileDTO = findById(id);
        if (!companyProfileValidator.validateRequest(companyProfileDTO.getCompany(), selectionFromDD)) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to deactivate/activate profile with id : {}.",
                    selectionFromDD,
                    companyProfileDTO.getId());
            throw new NotAllowedException("Profile activation/deactivation not allowed.");
        }
        objectLockService.checkAndThrowIfLocked(companyProfileDTO.getCompany().getId(), ObjectType.COMPANY);
        if (isActive) {
            // LOG ACTIVATION
            auditService.saveProfileAudit(AuditAction.ACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), companyProfileDTO);
        } else {
            // LOG DEACTIVATION
            auditService.saveProfileAudit(AuditAction.DEACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), companyProfileDTO);
        }

        activate(isActive,id, CompanyProfile.class);
        log.info("Profile with id : {} {} successfully by user with id : {}." , id , isActive ? "activated" : "deactivated",securityService.getLoggedInUserDetailId());
        return true;
    }

    public CompanyProfileDTO findProfile(String id,String selectionFromDD) {
        if (selectionFromDD.equals(ADMIN_RESOURCE)) return findById(id);
        CompanyProfileDTO profileDTO = profileMapper.map(companyProfileRepository.findCompanyProfileByIdWithValidations(id, selectionFromDD));
        if (profileDTO != null) return profileDTO;
        throw new NotAllowedException("Company Profile cannot be viewed.");
    }
}
