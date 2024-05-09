package lgl.bayern.de.ecertby.service;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.dto.ResourceDTO;
import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.AuditType;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.CertificateRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lgl.bayern.de.ecertby.repository.FeatureBoardRepository;
import lgl.bayern.de.ecertby.repository.TeamRepository;
import lgl.bayern.de.ecertby.repository.UserCompanyRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.validator.CompanyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CompanyService extends BaseService<CompanyDTO, Company, QCompany> {
    CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final UserDetailService userDetailService;
    private final EmailService emailService;
    private final SecurityService securityService;
    private final AuditService auditService;
    private final UserOperationService userOperationService;
    private final ObjectLockService objectLockService;
    private final com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;

    private final UserDetailRepository userDetailRepository;
    private final FeatureBoardRepository featureBoardRepository;

    private final UserCompanyRepository userCompanyRepository;

    private final CompanyRepository companyRepository;
    private final CompanyValidator companyValidator;

    private final UserGroupRepository userGroupRepository;
    private final TeamRepository teamRepository;

    /**
     * Saves company and creates a new user to assign to it as administrator.
     * @param companyDTO The given company object.
     */
    public CompanyDTO saveCompanyAndCreateUser(CompanyDTO companyDTO) {
        log.info(LOG_PREFIX + "Saving new company and creating user...");
        companyValidator.validateCompany(companyDTO);
        Company savedCompany = saveCompany(companyDTO);
        CompanyDTO savedCompanyDTO = companyMapperInstance.map(savedCompany);
        createUserDetail(savedCompanyDTO, companyDTO);
        return savedCompanyDTO;
    }

    /**
     * Saves company and assigns an existing user as its administrator.
     * @param companyDTO The given company object.
     */
    public CompanyDTO saveCompanyAndLinkUser(CompanyDTO companyDTO) {
        log.info(LOG_PREFIX + "Saving new company and linking existing user...");
        companyValidator.validateCompany(companyDTO);
        Company company = saveCompany(companyDTO);
        UserDetail userDetail = userDetailRepository.findByEmail(companyDTO.getUserEmail());
        saveUserCompany(company, userDetail);
        assignNewCompanyOperationsToExistingUser(company, userDetail);
        emailService.sendCompanyAdditionExistentUserEmail(companyDTO.getName(), userDetail.getEmail());
        auditService.linkUserAudit(AuditAction.UPDATE,
            userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), userDetail.getEmail(),
            company.getName(), AuditType.COMPANY, false);
        log.info(LOG_PREFIX + "Linked existing user with id : {} to created company with id : {}.", userDetail.getId(), company.getId());
        return companyMapperInstance.map(company);
    }

    /**
     * Edits existing company.
     * @param companyDTO The given company object.
     */
    public void editCompany(CompanyDTO companyDTO) {
        if (!companyValidator.validateEditRequest(companyDTO, companyDTO.getResourceId())) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to edit company with id {}.",
                    companyDTO.getResourceId(),
                    companyDTO.getId());
            throw new NotAllowedException("Company cannot be edited.");
        }
        List<EcertBYErrorException> errors = new ArrayList<>();
        companyValidator.validateIsAuthorityActive(companyDTO, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for saving company.", new EcertBYGeneralException(errors));
        }
        log.info(LOG_PREFIX + "Editing company with id : {} ",companyDTO.getId());
        objectLockService.checkAndThrowIfLocked(companyDTO.getId(), ObjectType.COMPANY);
        companyValidator.validateCompany(companyDTO);
        Company company = new Company();
        companyMapperInstance.map(companyDTO, company);
        CompanyDTO oldCompany = companyMapperInstance.map(companyRepository.findById(companyDTO.getId()).get());
        companyRepository.save(company);
        updateCompanyResource(companyDTO);
        log.info(LOG_PREFIX + "Company edited successfully with id: {}.", companyDTO.getId());
        // LOG UPDATE
        auditService.saveCompanyAudit(AuditAction.UPDATE, userMapperInstance
            .map(securityService.getLoggedInUserDetailDTO()), company.getName(),
            oldCompany, companyMapperInstance.map(companyRepository.findById(companyDTO.getId()).get()));
    }

    /**
     * Activates or deactivates company, also deactivating its users if only associated with the company during deactivation.
     * @param id The id of the company to activate/deactivate.
     * @param isActive The new active state.
     */
    public void activateCompany(String id, boolean isActive, String selectionFromDD) {
        if (!companyValidator.validateRequest(findById(id), selectionFromDD)) {
            log.info(LOG_PREFIX + "Authority with id : {} has no rights to activate/deactivate company with id {}.",
                    selectionFromDD,
                    id);
            throw new NotAllowedException("Company cannot be activated/deactivated.");
        }

        objectLockService.checkAndThrowIfLocked(id, ObjectType.COMPANY);
        List<EcertBYErrorException> errors = new ArrayList<>();
        Company company = companyMapperInstance.map(findById(id));
        if (!isActive) {
            // Deactivate the company
            log.info(LOG_PREFIX + "Deactivating company with id {}", id);

            companyValidator.validateDeactivateCompany(id, errors);
            if (!errors.isEmpty()) {
                throw new QCouldNotSaveException("Errors for deactivating company.", new EcertBYGeneralException(errors));
            }

            activate(false, id, Company.class);
            deactivateCompanyUsers(id);
            // LOG DEACTIVATION
            log.info(LOG_PREFIX + "Company with id : {} deactivated by user with id : {} successfully.", id,securityService.getLoggedInUserDetailId());
            auditService.saveCompanyAudit(AuditAction.DEACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), company);
        } else {
            companyValidator.validateActivateCompany(id, errors);
            // Activate the company
            if (!errors.isEmpty()) {
                throw new QCouldNotSaveException("Errors for activating company.", new EcertBYGeneralException(errors));
            } else {
                activate(true, id, Company.class);
                log.info(LOG_PREFIX + "Company with id {} activated successfully.", id);
                auditService.saveCompanyAudit(AuditAction.ACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),company);
            }
        }
    }

    private void deactivateCompanyUsers(String id) {
        // Find all userDetailsId connected with the company
        List<UserDetail> users = userCompanyRepository.findAllByCompany(id);
        // Deactivate users
        users.forEach(userDetail -> {
            long userCompanyCount = userCompanyRepository.countByUserDetailAndCompanyActive(userDetail, true);
            if(userDetail != null) {
                if (userCompanyCount == 0) {
                    userDetail.setActive(false);
                    userDetailRepository.save(userDetail);
                    log.info(LOG_PREFIX + "Deactivated user with id : {} as there are no active companies associated with them.", userDetail.getId());
                } else if (userCompanyCount > 0) {
                    updatePrimaryCompanyIfNeeded(userDetail, id);
                }
            }
        });
    }

    /**
     * Updates the primary company of the user if needed.
     * If the user's primary company is the same as the provided companyId,
     * it updates the primary company to the first active company found for the user.
     *
     * @param user      The UserDetail object representing the user.
     * @param companyId The ID of the company to be checked against the user's primary company.
     */
    private void updatePrimaryCompanyIfNeeded(UserDetail user, String companyId) {
        Company firstActiveCompany = getFirstActiveCompany(user.getId());
        if(firstActiveCompany == null && user.getPrimaryCompany().getId().equals(companyId)) {
            Optional<Company> firstInactiveCompany = companyRepository.getFirstInactiveAndNotDeletedCompany(user.getId());
            firstInactiveCompany.ifPresent(user::setPrimaryCompany);
            user.setActive(false);
            log.info(LOG_PREFIX + "Set the first inactive company with id : {} as the primary company for user with id : {} and deactivated them.",firstInactiveCompany.get().getId(), user.getId());
        } else if (user.getPrimaryCompany() != null && user.getPrimaryCompany().getId().equals(companyId)) {
                user.setPrimaryCompany(firstActiveCompany);
                userDetailRepository.save(user);
            log.info(LOG_PREFIX + "Set the first active company with id : {} as the primary company for user with id : {}.",firstActiveCompany.getId(), user.getId());
       }
    }

    /**
     * Retrieves the first active company for a given user ID.
     *
     * @param userId The ID of the user for whom to retrieve the first active company.
     * @return The first active Company object for the specified user, or null if none is found.
     */
    private Company getFirstActiveCompany(String userId) {
        return companyRepository.getUserCompanies(userId)
                .stream()
                .filter(Company::isActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * Mark company as deleted, also deleting its users if only associated with the deleted company.
     * @param id The id of the company to mark as deleted.
     */
    public void deleteCompany(String id, String selectionFromDD) {
        if (!companyValidator.validateRequest(findById(id), selectionFromDD)) {
            log.info(LOG_PREFIX + "Authority with id : {} has no rights to delete company with id {}.",
                    selectionFromDD,
                    id);
            throw new NotAllowedException("Company cannot be deleted.");
        }

        log.info(LOG_PREFIX + "Deleting company with id : {} ",id);
        objectLockService.checkAndThrowIfLocked(id, ObjectType.COMPANY);
        List<EcertBYErrorException> errors = new ArrayList<>();
        companyValidator.validateDeleteCompany(id, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for deleting company.", new EcertBYGeneralException(errors));
        }

        // Set Company's deleted field to true
        delete(id, Company.class);
        // Delete all teams of this company
        deleteCompanyTeams(id);
        activate(false, id, Company.class);
        log.info(LOG_PREFIX + "Company with id : {} deleted by user with id : {} successfully.",id,securityService.getLoggedInUserDetailId());
        Company company = companyMapperInstance.map(findById(id));
        // Find all userDetailsId connected with the company
        List<UserDetail> users = userCompanyRepository.findAllByCompany(id);
        // Delete users
        users.forEach(userDetail -> {
            userCompanyRepository.deleteUserCompanyByCompanyAndUserDetail(company, userDetail);
            userOperationService.removeExistingOperationsFromUser(userDetail.getUser().getId(), company.getId());
            long userCompanyCount = userCompanyRepository.countByUserDetailAndCompanyDeleted(userDetail, false);
            if (userCompanyCount == 0) {
                featureBoardRepository.updateFirstNameAndLastName(userDetail.getFirstName(), userDetail.getLastName(), userDetail.getEmail(), userDetail.getId());
                auditService.addNameForDeletedUser(userDetail);
                userOperationService.removeAllOperationsFromUser(userDetail.getUser().getId());
                log.info(LOG_PREFIX + "Deleted user with id {} because they were only linked with the deleted company.",userDetail.getId() );
                userDetailService.deleteUser(userDetail);
            } else if(userCompanyCount > 0) {
                updatePrimaryCompanyIfNeeded(userDetail,id);
            }
        });
        // LOG DELETION
        auditService.saveCompanyAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), company);
    }

    public void deleteCompanyTeams(String companyId){
        teamRepository.deleteTeamByCompany_Id(companyId);
    }

    public CompanyDTO findCompany(String id, String selectionFromDD) {
        if (selectionFromDD.equals(ADMIN_RESOURCE)) return findById(id);
        CompanyDTO companyDTO = companyMapperInstance.map(companyRepository.findCompanyByIdWithValidations(id, selectionFromDD));
        if (companyDTO != null) return companyDTO;
        throw new NotAllowedException("Company cannot be viewed.");
    }

    /**
     * Return all companies or only those associated with their authority, in case of authority users.
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @param selectionFromDD The resource id associated with the acting user.
     * @return The requested companies paged and sorted.
     */
    public Page<CompanyDTO> findAllOrAuthorityCompanies(Predicate predicate, Pageable pageable, String selectionFromDD) {
        BooleanBuilder finalPredicate = new BooleanBuilder().and(predicate);
        if (selectionFromDD != null && !selectionFromDD.equals(ADMIN_RESOURCE)) finalPredicate.andAnyOf(
            QCompany.company.preResponsibleAuthority.id.eq(selectionFromDD),
            QCompany.company.postResponsibleAuthority.id.eq(selectionFromDD),
            QCompany.company.responsibleAuthority.id.eq(selectionFromDD)
        );
        return findAll(finalPredicate, pageable);
    }

    /**
     * Get all companies.
     * @return The companies as a list of OptionDTOs.
     */
    public List<OptionDTO> getAllCompanies() {
        return companyMapperInstance.mapToListOptionDTO(companyRepository.findAll());
    }

    /**
     * Return all companies the logged-in user is associated with.
     * @return The associated companies as a list of OptionDTOs.
     */
    public List<OptionDTO> getUserCompanies() {
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        if (userDetailDTO != null && userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            return companyMapperInstance.mapToListOptionDTO(companyRepository.getUserCompanies(userDetailDTO.getId()));
        }

        return null;
    }

    /**
     * Return all active companies, sorted by name.
     * @return The companies as a list of OptionDTOs.
     */
    public List<OptionDTO> getAllActiveCompanies() {
        return companyMapperInstance.mapToListOptionDTO(companyRepository.findAllByActiveIsTrueOrderByName());
    }

    /* PRIVATE METHODS */

    private void assignNewCompanyOperationsToExistingUser(Company company, UserDetail userDetail) {
        UserDetailDTO userDetailDTO = userMapperInstance.map(userDetail);
        userOperationService.assignOperationsToUser(userDetail.getUser().getId(), userDetailDTO,
                UserRole.COMPANY_MAIN_USER.toString(),
                resourceService.getResourceByObjectId(company.getId()).getId());
        log.info(LOG_PREFIX + "New admin operations assigned to user with id : {}." , userDetailDTO.getId());
    }

    private void updateCompanyResource(CompanyDTO companyDTO) {
        ResourceDTO existingResource = resourceService.getResourceByObjectId(companyDTO.getId());
        ResourceDTO updatedResourceDTO = companyMapperInstance.companyDTOtoResourceDTO(companyDTO);
        updatedResourceDTO.setId(existingResource.getId());
        resourceService.updateResource(updatedResourceDTO);
        log.info(LOG_PREFIX + "Resource updated for company with id : {}.",companyDTO.getId());
    }

    private Company saveCompany(CompanyDTO companyDTO) {
        CompanyDTO savedCompanyDTO = save(companyDTO);
        resourceService.createResource(companyMapperInstance.companyDTOtoResourceDTO(savedCompanyDTO));
        // LOG CREATION
        auditService.saveCompanyAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), companyMapperInstance.map(savedCompanyDTO));
        log.info(LOG_PREFIX + "Company with id : {} created successfully.",savedCompanyDTO.getId());
        return companyMapperInstance.map(savedCompanyDTO);
    }

    private UserDetail createUserDetail(CompanyDTO savedCompanyDTO, CompanyDTO companyDTO) {
        UserDetailDTO userDetailDTO = new UserDetailDTO();
        userDetailDTO.setEmail(companyDTO.getUserEmail());
        userDetailDTO.setFirstName(companyDTO.getUserFirstName());
        userDetailDTO.setLastName(companyDTO.getUserLastName());
        userDetailDTO.setDepartment(new TreeSet<>());
        userDetailDTO.setDepartment(companyDTO.getUserDepartment());
        userDetailDTO.setUserType(new OptionDTO(UserType.COMPANY_USER.toString()));
        UserGroup companyMainUserGroup = userGroupRepository.findByName(UserRole.COMPANY_MAIN_USER.toString());
        userDetailDTO.setRole(new OptionDTO(companyMainUserGroup.getId()));
        userDetailDTO.setActive(true);
        userDetailDTO.setPrimaryCompany(savedCompanyDTO);
        userDetailDTO.setResourceId(companyDTO.getResourceId());
        UserDetail savedUser = userMapperInstance.map(userDetailService.saveUser(userDetailDTO));
        log.info(LOG_PREFIX + "User created with id : {}.",savedUser.getId());
        return savedUser;
    }

    private void saveUserCompany(Company company, UserDetail userDetail) {
        UserCompany userCompany = new UserCompany();
        userCompany.setCompany(company);
        userCompany.setUserDetail(userDetail);
        UserGroup userGroup = userGroupRepository.findByName(UserRole.COMPANY_MAIN_USER.toString());
        userCompany.setUserGroup(userGroup);
        userCompanyRepository.save(userCompany);
        log.info(LOG_PREFIX + "UserCompany saved for user with id : {} and company with id : {}." , userDetail.getId() , company.getId());
    }

    public SelectionFromDDJWTDTO mapFirstActiveCompanyToSelectionAndUpdateJWT(UserDetail userDetail, Company company) {
        List<Company> allUserCompanies = companyRepository.getUserCompanies(userDetail.getId());
        Company firstActiveCompany = allUserCompanies.stream().filter(Company::isActive).findFirst().orElse(null);
        SelectionFromDDJWTDTO newCompanySelectionJWT = companyMapperInstance.mapToSelectionFromDDJWTDTO(firstActiveCompany);
        if (!company.isActive()) {
            newCompanySelectionJWT.setMessage("company_inactive_logged_in_user");
        }
        if (company.isDeleted()) {
            newCompanySelectionJWT.setMessage("company_deleted_logged_in_user");
        }
        log.info(LOG_PREFIX + "Found the first active company with id : {}." ,firstActiveCompany != null ? firstActiveCompany.getId() : "None");
        return newCompanySelectionJWT;
    }
}
