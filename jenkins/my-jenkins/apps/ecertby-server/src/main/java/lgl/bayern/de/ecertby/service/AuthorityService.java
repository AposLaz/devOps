package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.dto.ResourceDTO;
import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.OperationRepository;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.SelectionFromDDJWTDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.QAuthority;
import lgl.bayern.de.ecertby.model.QUserAuthority;
import lgl.bayern.de.ecertby.model.UserAuthority;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.AuditType;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.AuthorityRepository;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lgl.bayern.de.ecertby.repository.UserAuthorityRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.validator.AuthorityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class AuthorityService extends BaseService<AuthorityDTO, Authority, QAuthority> {
    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final EntityManager entityManager;

    private final EmailService emailService;

    private final UserDetailService userDetailService;

    private final SecurityService securityService;

    private final AuditService auditService;

    private final UserOperationService userOperationService;

    private final ObjectLockService objectLockService;

    private final com.eurodyn.qlack.fuse.aaa.service.ResourceService resourceService;


    private static QUserAuthority qUserAuthority = QUserAuthority.userAuthority;

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserAuthorityRepository userAuthorityRepository;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    private final AuthorityValidator authorityValidator;
    @Autowired
    private OperationRepository operationRepository;

    /**
     * Saves an authority and creates a corresponding user. The authority is linked to the user.
     *
     * @param authorityDTO The AuthorityDTO containing information about the authority to be saved.
     */

    public void saveAuthorityAndCreateUser(AuthorityDTO authorityDTO) {
        Authority savedAuthority = saveAuthority(authorityDTO);
        AuthorityDTO savedAuthorityDTO = authorityMapperInstance.map(savedAuthority);
        createUserDetail(savedAuthorityDTO, authorityDTO);
    }
    /**
     * Saves an authority and links it to an existing user by their email address.
     *
     * @param authorityDTO The AuthorityDTO containing information about the authority to be saved.
     */
    public AuthorityDTO saveAuthorityAndLinkUser(AuthorityDTO authorityDTO) {
        Authority authority = saveAuthority(authorityDTO);
        UserDetail userDetail = userDetailRepository.findByEmail(authorityDTO.getEmail());
        saveUserAuthority(authority, userDetail);
        assignNewAuthorityOperationsToExistingUser(authority, userDetail);
        emailService.sendAuthorityAdditionExistentUserEmail(authorityDTO.getName(), userDetail.getEmail());
        auditService.linkUserAudit(AuditAction.UPDATE,
            userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), userDetail.getEmail(),
            authority.getName(), AuditType.AUTHORITY, false);

        log.info(LOG_PREFIX + "The user with ID: {} has been linked with authority with ID: {}.", userDetail.getId(), authority.getId());

        return authorityMapperInstance.map(authority);
    }

    private void assignNewAuthorityOperationsToExistingUser(Authority authority, UserDetail userDetail) {
        UserDetailDTO userDetailDTO = userMapperInstance.map(userDetail);
        userOperationService.assignOperationsToUser(userDetail.getUser().getId(), userDetailDTO,
            UserRole.AUTHORITY_MAIN_USER.toString(),
            resourceService.getResourceByObjectId(authority.getId()).getId());
    }

    /**
     * Saves an authority based on the provided AuthorityDTO.
     *
     * @param authorityDTO The AuthorityDTO containing information about the authority to be saved.
     * @return The saved authority entity.
     */
    public Authority saveAuthority(AuthorityDTO authorityDTO) {
        AuthorityDTO oldAuthority = null;
        if (!isNull(authorityDTO.getId())) {
            oldAuthority = findById(authorityDTO.getId());
        }

        AuthorityDTO savedAuthorityDTO = save(authorityDTO);

        boolean isCreate = authorityDTO.getId() == null;

        if (isCreate) {
            resourceService.createResource(authorityMapperInstance.authorityDTOtoResourceDTO(savedAuthorityDTO));
            auditService.saveAuthorityAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedAuthorityDTO);
        } else {
            updateAuthorityResource(savedAuthorityDTO);
            auditService.saveAuthorityAudit(AuditAction.UPDATE,
                userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedAuthorityDTO.getName(), oldAuthority, findById(authorityDTO.getId()));
        }
        log.info(LOG_PREFIX + "The authority with ID: {} has been {}.", savedAuthorityDTO.getId(), (isCreate ? "created" : "updated"));
        return authorityMapperInstance.map(savedAuthorityDTO);
    }
    /**
     * Creates a UserDetail entity based on the provided AuthorityDTO and email address.
     *
     * @param authorityDTO The AuthorityDTO containing information about the authority.
     * @return The created UserDetail entity.
     */
    public UserDetail createUserDetail(AuthorityDTO savedAuthorityDTO, AuthorityDTO authorityDTO) {
        UserDetailDTO userDetailDTO = new UserDetailDTO();
        userDetailDTO.setEmail(authorityDTO.getEmail());
        userDetailDTO.setFirstName(authorityDTO.getUserFirstName());
        userDetailDTO.setLastName(authorityDTO.getUserLastName());
        userDetailDTO.setDepartment(new TreeSet<>());
        userDetailDTO.setDepartment(authorityDTO.getUserDepartment());
        userDetailDTO.setUserType(new OptionDTO(UserType.AUTHORITY_USER.toString()));
        UserGroup authorityMainUserGroup = userGroupRepository.findByName(UserRole.AUTHORITY_MAIN_USER.toString());
        userDetailDTO.setRole(new OptionDTO(authorityMainUserGroup.getId()));
        userDetailDTO.setActive(true);
        userDetailDTO.setPrimaryAuthority(savedAuthorityDTO);

        UserDetailDTO savedUserDetailDTO = userDetailService.saveUser(userDetailDTO);

        log.info(LOG_PREFIX + "A new user with ID {} has been created and linked with the authority with ID: {}",
            savedUserDetailDTO.getId(), savedAuthorityDTO.getId());

        return userMapperInstance.map(savedUserDetailDTO);
    }
    /**
     * Saves a UserAuthority entity linking an authority to a user.
     *
     * @param authority   The authority to link to the user.
     * @param userDetail  The user to link with the authority.
     */
    private void saveUserAuthority(Authority authority, UserDetail userDetail) {
        UserAuthority userAuthority = new UserAuthority();
        userAuthority.setAuthority(authority);
        userAuthority.setUserDetail(userDetail);
        UserGroup userGroup = userGroupRepository.findByName(UserRole.AUTHORITY_MAIN_USER.toString());
        userAuthority.setUserGroup(userGroup);
        userAuthorityRepository.save(userAuthority);

        log.info(LOG_PREFIX + "The authority of user with ID: {} has been updated.", userDetail.getId());

    }

    /**
     * Retrieves a list of all authorities as OptionDTOs.
     *
     * @return A list of OptionDTOs representing authorities.
     */
    public List<OptionDTO> getAllAuthorities() {
        return authorityMapperInstance.mapToListOptionDTO(authorityRepository.findAllByOrderByNameAsc());
    }
    /**
     * Retrieves a list of all active authorities as OptionDTOs.
     *
     * @return A list of OptionDTOs representing active authorities.
     */
    public List<OptionDTO> getAllActiveAuthorities() {
        return authorityMapperInstance.mapToListOptionDTO(authorityRepository.findAllByActiveIsTrueOrderByName());
    }
    /**
     * Activates or deactivates an authority based on the given ID and isActive flag.
     *
     * @param id       The ID of the authority to activate or deactivate.
     * @param isActive The flag indicating whether to activate or deactivate the authority.
     * @return True if the operation is successful, false otherwise.
     */
    public void activateAuthority(String id, Boolean isActive) {
        List<EcertBYErrorException> errors = new ArrayList<>();
        objectLockService.checkAndThrowIfLocked(id, ObjectType.AUTHORITY);
            if (Boolean.FALSE.equals(isActive)) {
               authorityValidator.validateDeactivateAuthority(id,errors);
                if (!errors.isEmpty()) {
                    throw new QCouldNotSaveException("Errors for deactivating authority.", new EcertBYGeneralException(errors));
                }
                activate(false, id, Authority.class);
                List<String> userIds = userAuthorityRepository.findUserIdsByAuthorityId(id);
                userIds.forEach(userId -> {
                    long userAuthorityCount = countUserAuthorities(userId);
                    if (userAuthorityCount == 1 || !hasActiveAuthority(userId)) {
                        deactivateUser(userId);
                    } else if(userAuthorityCount > 1){
                       updatePrimaryAuthorityIfNeeded(userId, id);
                    }
                });
                // LOG DEACTIVATION
                auditService.saveAuthorityAudit(AuditAction.DEACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), findById(id));
            } else {
                // Activate the authority
                activate(true, id, Authority.class);
                // LOG ACTIVATION
                auditService.saveAuthorityAudit(AuditAction.ACTIVATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), findById(id));
            }

        log.info(LOG_PREFIX + "The authority with ID: {} has been {}.", id, (isActive ? "activated" : "deactivated"));
    }

    /**
     * Updates the primary authority of a user if their current primary authority matches the specified authority ID.
     *
     * @param userId      The ID of the user for whom to update the primary authority.
     * @param authorityId The ID of the authority to check against the user's current primary authority.
     */
    private void updatePrimaryAuthorityIfNeeded(String userId, String authorityId) {
        Authority firstActiveAuthority = getFirstActiveAuthority(userId);
        Optional<UserDetail> userOptional = userDetailRepository.findById(userId);

        if (userOptional.isPresent()) {
            UserDetail user = userOptional.get();
            if (user.getPrimaryAuthority() != null && user.getPrimaryAuthority().getId().equals(authorityId)) {
                user.setPrimaryAuthority(firstActiveAuthority);
                userDetailRepository.save(user);
                log.info(LOG_PREFIX + "The primary authority of user with ID: {} has been updated to {}.", userId, authorityId);
            }
        }
    }

    /**
     * Retrieves the first active authority for a user.
     *
     * @param userId The ID of the user for whom to retrieve the first active authority.
     * @return The first active authority, or null if none is found.
     */
    private Authority getFirstActiveAuthority(String userId) {
        return authorityRepository.getUserAuthoritiesOrderByName(userId)
                .stream()
                .filter(Authority::isActive)
                .findFirst()
                .orElse(null);
    }
    /**
     * Counts the number of UserAuthorities associated with a specific user.
     *
     * @param userId The ID of the user for which to count UserAuthorities.
     * @return The count of UserAuthorities for the specified user.
     */
    private long countUserAuthorities(String userId) {
        return userAuthorityRepository.countByUserDetail_Id(userId);
    }

    /**
     * Deactivates a user by setting their 'active' flag to false.
     *
     * @param userId The ID of the user to deactivate.
     */
    private void deactivateUser(String userId) {
        UserDetail userDetail = userDetailRepository.findById(userId).orElse(null);
        if (userDetail != null) {
            userDetail.setActive(false);
            userDetailRepository.save(userDetail);
            log.info(LOG_PREFIX + "The user with ID: {} has been deactivated.", userId);
        }
    }
    /**
     * Checks if a user has at least one active authority linked to them.
     *
     * @param userId The ID of the user to check for active authorities.
     * @return True if the user has at least one active authority, false otherwise.
     */
    private boolean hasActiveAuthority(String userId) {
        List<Authority> authorities = new JPAQueryFactory(entityManager)
                .select(qUserAuthority.authority)
                .from(qUserAuthority)
                .where(qUserAuthority.userDetail.id.eq(userId))
                .fetch();

        return authorities.stream().anyMatch(Authority::isActive);
    }
    /**
     * Checks if an authority has a main user linked to it.
     *
     * @param id The ID of the authority to check.
     * @return True if the authority has a main user, false otherwise.
     */
    public boolean hasMainUser(String id) {
        String mainUserGroupId = userGroupRepository.findByName(UserRole.AUTHORITY_MAIN_USER.toString()).getId();
        return userAuthorityRepository.countUsersByAuthorityIdAndGroupId(id, mainUserGroupId) > 0;
    }
    /**
     * Creates or updates an authority. Depending on the provided AuthorityDTO, it may also create a user linked to the authority.
     *
     * @param authorityDTO The AuthorityDTO containing information about the authority to be created or updated.
     */
    public void createUpdateAuthority(AuthorityDTO authorityDTO) {
        if (!authorityValidator.validateCreateEditRequest(authorityDTO, authorityDTO.getResourceId())) {
            log.info(LOG_PREFIX + "Authority/Company with id : {} has no rights to edit authority with id {}.",
                    authorityDTO.getResourceId(),
                    authorityDTO.getId());
            throw new NotAllowedException("Authority cannot be created/updated.");
        }
        objectLockService.checkAndThrowIfLocked(authorityDTO.getId(), ObjectType.AUTHORITY);
        authorityValidator.validateAuthority(authorityDTO);
        if (authorityDTO.isMainUserCreate()) {
            if (!authorityDTO.isEmailAlreadyExists()) {
                saveAuthorityAndCreateUser(authorityDTO);
            } else {
                saveAuthorityAndLinkUser(authorityDTO);
            }
        } else {
            saveAuthority(authorityDTO);
        }
    }

    private void updateAuthorityResource(AuthorityDTO savedAuthorityDTO) {
        ResourceDTO existingResource = resourceService.getResourceByObjectId(savedAuthorityDTO.getId());
        ResourceDTO updatedResourceDTO = authorityMapperInstance.authorityDTOtoResourceDTO(savedAuthorityDTO);
        updatedResourceDTO.setId(existingResource.getId());
        resourceService.updateResource(updatedResourceDTO);
    }

    public List<OptionDTO> getUserAuthorities() {
        UserDetailDTO userDetailDTO = securityService.getLoggedInUserDetailDTO();
        if (userDetailDTO != null) {
            return authorityMapperInstance.mapToListOptionDTO(authorityRepository.getUserAuthoritiesOrderByName(userDetailDTO.getId()));
        }

        return null;
    }

    public List<OptionDTO> getAllAuthoritiesWithMainUser() {
        return authorityMapperInstance.mapToListOptionDTO(authorityRepository.findAuthoritiesWithMainUser());
    }

    public SelectionFromDDJWTDTO mapFirstActiveAuthorityToSelectionAndUpdateJWT(UserDetail userDetail) {
        List<Authority> allUserAuthorities = authorityRepository.getUserAuthoritiesOrderByName(userDetail.getId());
        Authority firstActiveAuthority = allUserAuthorities.stream().filter(Authority::isActive).findFirst().orElse(null);
        SelectionFromDDJWTDTO newAuthoritySelectionJWT = authorityMapperInstance.mapToSelectionFromDDJWTDTO(firstActiveAuthority);
        newAuthoritySelectionJWT.setMessage("authority_inactive_logged_in_user");
        return newAuthoritySelectionJWT;
    }

    public ResponseEntity<AuthorityDTO> validateAndReturnAuthorityById(String id, String selectionFromDD) {
            AuthorityDTO authority = findById(id);
            if(!selectionFromDD.equals(AppConstants.Resource.ADMIN_RESOURCE) && !selectionFromDD.equals(id) ) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(authority);
        }


    public List<OptionDTO> getAllAuthoritiesForAuthorityForward(String certificateId, String selectionFromDD,boolean forPreCertificate) {
        if(!forPreCertificate) {
            return authorityMapperInstance.mapToListOptionDTO(authorityRepository.findAuthoritiesForAuthorityForward(certificateId, selectionFromDD));
        } else {
            return authorityMapperInstance.mapToListOptionDTO(authorityRepository.findAuthoritiesForPreAuthorityForward(certificateId));
        }
    }
}
