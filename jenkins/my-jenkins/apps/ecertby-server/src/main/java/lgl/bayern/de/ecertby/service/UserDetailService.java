package lgl.bayern.de.ecertby.service;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;

import com.eurodyn.qlack.common.exception.QAuthorisationException;
import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.dto.UserDTO;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.mapper.UserServiceMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.*;
import lgl.bayern.de.ecertby.repository.*;
import lgl.bayern.de.ecertby.utility.UsernamePasswordUtil;
import lgl.bayern.de.ecertby.validator.UserDetailValidator;
import lgl.bayern.de.ecertby.validator.UserRightsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
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
public class UserDetailService extends BaseService<UserDetailDTO, UserDetail, QUserDetail> {
    private final UserDetailRepository userDetailRepository;
    private final UserDetailValidator userValidator;
    private final UserRightsValidator userRightsValidator;
    private final com.eurodyn.qlack.fuse.aaa.service.UserService userService;
    private final com.eurodyn.qlack.fuse.aaa.service.UserGroupService userGroupService;
    private final EmailService emailService;
    private final SecurityService securityService;
    private final KeycloakService keycloakService;
    private final LogoutService logoutService;
    private final AuditService auditService;
    private final UserOperationService userOperationService;
    private final ObjectLockService objectLockService;
    private final UserAuthorityRepository userAuthorityRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final AuthorityRepository authorityRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final CatalogValueRepository catalogValueRepository;
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);
    CompanyMapper companyMapperInstance = Mappers.getMapper(CompanyMapper.class);
    AuthorityMapper authorityMapperInstance = Mappers.getMapper(AuthorityMapper.class);
    UserServiceMapper mapperInstanceUser = Mappers.getMapper(UserServiceMapper.class);
    private static final QUserDetail Q_USER_DETAIL = QUserDetail.userDetail;
    private static final QUserCompany Q_USER_COMPANY = QUserCompany.userCompany;
    private static final QUserAuthority Q_USER_AUTHORITY = QUserAuthority.userAuthority;
    private static final String USER_DEACTIVATE_ACTION_ERRORS = "Errors for user detail deactivate action";
    private static final String USER_DELETE_ACTION_ERRORS = "Errors for user detail delete action";
    private final UserGroupRepository userGroupRepository;

    @Value("${ecert.domain-url}")
    private String domainUrl;

    /**
     * Find all users with the selected criteria.
     * @param predicate The criteria selected.
     * @param pageable The page and the amount of users to be returned.
     * @return The list of users.
     */
    public Page<UserDetailDTO> findAll(Predicate predicate,
                                       Pageable pageable , String selectionFromDD,String role, String roleInProcess) {
        UserDetailDTO dto = securityService.getLoggedInUserDetailDTO();
        BooleanBuilder booleanBuilder = new BooleanBuilder(predicate);

        if(dto.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())){
            BooleanExpression rolePredicate = role != null ? Q_USER_AUTHORITY.userGroup.id.eq(role) : null;
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                booleanBuilder.and(Q_USER_DETAIL.id.in(
                        JPAExpressions.select(Q_USER_AUTHORITY.userDetail.id)
                                .from(Q_USER_AUTHORITY)
                                .where(Q_USER_AUTHORITY.authority.id.eq(selectionFromDD).and(rolePredicate))
                ));
            }

        } else if (dto.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            BooleanExpression rolePredicate = role != null ? Q_USER_COMPANY.userGroup.id.eq(role) : null;
            if (selectionFromDD != null && !selectionFromDD.isEmpty()) {
                booleanBuilder.and(Q_USER_DETAIL.id.in(
                        JPAExpressions.select(Q_USER_COMPANY.userDetail.id)
                                .from(Q_USER_COMPANY)
                                .where(Q_USER_COMPANY.company.id.eq(selectionFromDD).and(rolePredicate))
                ));
            }
        }

        if (roleInProcess != null) {
            booleanBuilder.and(Q_USER_DETAIL.id.in(JPAExpressions.select(Q_USER_AUTHORITY.userDetail.id)
                                    .from(Q_USER_AUTHORITY)
                                    .where((Q_USER_AUTHORITY.roleInProcess.id).eq(roleInProcess)))
                            .or(Q_USER_DETAIL.id.in(JPAExpressions.select(Q_USER_COMPANY.userDetail.id)
                                    .from(Q_USER_COMPANY)
                                    .where((Q_USER_COMPANY.roleInProcess.id).eq(roleInProcess)))));
        }

        Page<UserDetail> all = this.userDetailRepository.findAll(booleanBuilder, pageable);
        Page<UserDetailDTO> allUsers = this.userMapperInstance.map(all);
        allUsers.map(o -> o.setRoleInProcessName(constructRoleInProcess(o, selectionFromDD)));
        return allUsers.map(o -> o.setRoleName(constructRole(o, selectionFromDD)));
    }
    private String constructRole(UserDetailDTO userDetailDTO, String selectionFromDD) {
        if (userDetailDTO.getUserType().getId().equals(UserType.ADMIN_USER.toString())) {
            return userDetailDTO.getRole().getName();
        } else if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            List<UserAuthority> userAuthorityList = userAuthorityRepository.findUserAuthoritiesByUserDetailId(userDetailDTO.getId());
            if (selectionFromDD != null && !ADMIN_RESOURCE.equals(selectionFromDD)) {
                return userAuthorityList.stream().filter(o -> o.getAuthority().getId().equals(selectionFromDD))
                        .map(o -> o.getUserGroup().getDescription()).findFirst().orElse("");
            } else {
                List<String> userGroupDescriptionList = userAuthorityList.stream().map(o -> o.getUserGroup().getDescription()).sorted().collect(Collectors.toList());
                return String.join(", ", userGroupDescriptionList);
            }
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            List<UserCompany> userCompanyList = userCompanyRepository.findUserCompaniesByUserDetailId(userDetailDTO.getId());
            if (selectionFromDD != null && !ADMIN_RESOURCE.equals(selectionFromDD)) {
                return userCompanyList.stream().filter(o -> o.getCompany().getId().equals(selectionFromDD))
                        .map(o -> o.getUserGroup().getDescription()).findFirst().orElse("");
            } else {
                List<String> userGroupDescriptionList = userCompanyList.stream().map(o -> o.getUserGroup().getDescription()).sorted().collect(Collectors.toList());
                return String.join(", ", userGroupDescriptionList);
            }
        }
        return "";
    }
 private String constructRoleInProcess(UserDetailDTO userDetailDTO, String selectionFromDD) {
        if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            List<UserAuthority> userAuthorityList = userAuthorityRepository.findUserAuthoritiesByUserDetailId(userDetailDTO.getId());
            if (selectionFromDD != null && !ADMIN_RESOURCE.equals(selectionFromDD)) {
                return userAuthorityList.stream()
                        .filter(o -> o.getAuthority().getId().equals(selectionFromDD) && o.getRoleInProcess() !=null )
                        .map(o -> o.getRoleInProcess().getData()).findFirst().orElse("");
            } else {
                List<String> roleInProcessNameList = userAuthorityList.stream()
                        .filter(o ->o.getRoleInProcess() !=null)
                        .map(o -> o.getRoleInProcess().getData()).sorted().collect(Collectors.toList());
                return String.join(", ", roleInProcessNameList);
            }
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            List<UserCompany> userCompanyList = userCompanyRepository.findUserCompaniesByUserDetailId(userDetailDTO.getId());
            if (selectionFromDD != null && !ADMIN_RESOURCE.equals(selectionFromDD)) {
                return userCompanyList.stream().
                        filter(o -> o.getCompany().getId().equals(selectionFromDD) && o.getRoleInProcess() !=null)
                        .map(o -> o.getRoleInProcess().getData()).findFirst().orElse("");
            } else {
                List<String> roleInProcessNameList = userCompanyList.stream()
                        .filter(o ->o.getRoleInProcess() !=null)
                        .map(o -> o.getRoleInProcess().getData()).sorted().collect(Collectors.toList());
                return String.join(", ", roleInProcessNameList);
            }
        }
     return "";
    }

    /**
     * Saves a user to the system.
     * @param userDetailDTO The given dto object to create user.
     * @return The created user in the system.
     */
    public UserDetailDTO saveUser(UserDetailDTO userDetailDTO) {
        objectLockService.checkAndThrowIfLocked(userDetailDTO.getId(), ObjectType.USER);

        UserDetailDTO oldUser = null;
        if (userDetailDTO.getId() != null) {
            log.info(LOG_PREFIX + "Updating user...");
            oldUser = getUser(userDetailDTO.getId(), false, ADMIN_RESOURCE);

        } else log.info(LOG_PREFIX + "Creating new user...");

        UserDetailDTO savedUserDetailDTO = saveUser(userDetailDTO, null, null);

        // LOG CREATION
        if (userDetailDTO.getId() == null) {
            auditService.saveUserAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedUserDetailDTO);
            log.info(LOG_PREFIX + "New user with id {} successfully created by user with id : {}", savedUserDetailDTO.getId(), securityService.getLoggedInUserDetailId());
        } else {
            auditService.saveUserAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                savedUserDetailDTO.getEmail(), oldUser, getUser(userDetailDTO.getId(), false, ADMIN_RESOURCE));
            log.info(LOG_PREFIX + "User with id {} successfully updated by user with id : {}", userDetailDTO.getId(), securityService.getLoggedInUserDetailId());
        }

        return savedUserDetailDTO;
    }

    /**
     * Saves a user to the system.
     * @param userDetailDTO The given dto object to create user.
     * @param response The http servlet response
     * @return The created user in the system.
     */
    public UserDetailDTO saveUser(UserDetailDTO userDetailDTO, HttpServletResponse response, HttpServletRequest request) {
        // Set a variable to see if in edit mode.
        boolean editMode = true;
        // Keep the old username;
        String usernameDB = null;
        UserDetail userDetailDB = null;

        if(userDetailDTO.getId() == null) {
            userDetailDTO.setUser(new UserDTO());
            userDetailDTO.setUsername(generateUniqueUsername());
            userDetailDTO.getUser().setUsername(userDetailDTO.getUsername().toLowerCase());
            userDetailDTO.setNewPassword(UsernamePasswordUtil.generateUsernameOrPassword(false));
            editMode = false;
        }

        // Check if is valid according to business rules.
        log.info(LOG_PREFIX + "Checking if user info is valid...");
        userValidator.validateUserDetail(userDetailDTO);

        // Check if user is main user.
        if (userDetailDTO.getMainUser() != null && userDetailDTO.getMainUser()){
            UserDetailDTO mainUser = findMainUser(userDetailDTO);
            if(mainUser != null){
                // If user exists in the system return the user. No update can be performed.
                return mainUser;
            }
        }

        if(editMode){
            userDetailDB = userDetailRepository.findByUserId(userDetailDTO.getUser().getId());
            usernameDB = userDetailDB.getUser().getUsername();
            if (request == null) {
                //primarycompany / primaryauthority are not present when editing another user, retrieve them from db
                log.info(LOG_PREFIX + "Retrieving primary company / authority...");
                userDetailDTO.setPrimaryCompany(companyMapperInstance.map(userDetailDB.getPrimaryCompany()));
                userDetailDTO.setPrimaryAuthority(authorityMapperInstance.map(userDetailDB.getPrimaryAuthority()));
            }
            if (userDetailDTO.getUserAuthorityCompanyDTOList() == null || userDetailDTO.getUserAuthorityCompanyDTOList().isEmpty()) {
                updateAuthorityCompanyRoles(userDetailDTO);
            }
        }

        userDetailDTO.getUser().setUsername(userDetailDTO.getUsername().toLowerCase());

        log.info(LOG_PREFIX + "Setting user's rights...");
        saveAAAUserAndRights(userDetailDTO);

        // Save user details.
        UserDetail userDetail = userDetailRepository.save(userMapperInstance.map(userDetailDTO));

        saveUserAuthorityOrCompany(editMode, userDetailDTO, userDetail);

        // Save keycloack user.
        try {
           saveKeycloakUser(userDetailDTO, usernameDB);
        } catch (Exception ex) {
            throw new QCouldNotSaveException("Something wrong went with keycloak user.");
        }

        if(userDetailDTO.getId() == null) {
            // Send email.
            log.info(LOG_PREFIX + "Sending email to created user...");
            emailService.sendCreateUserEmail(userDetailDTO.getUser().getUsername(), userDetailDTO.getNewPassword(), domainUrl + "/user/my-account", userDetailDTO.getEmail());
        }

        if (editMode && !usernameDB.equals(userDetailDTO.getUser().getUsername()) && response != null) {
            try {
                logoutService.logout(userDetailDTO.getUser().getUsername(), request);
            } catch (IOException | ServletException ex) {
                throw new QExceptionWrapper("Something wrong went with redirecting to logout.");
            }
        }
        return userMapperInstance.map(userDetail);
    }

    /**
     * Saves aaa user and rights
     *
     * @param userDetailDTO the userDetailDTO to be saved
     */
    private void saveAAAUserAndRights(UserDetailDTO userDetailDTO) {
        String aaaUserId = saveAAAUser(userDetailDTO);
        if (userDetailDTO.getUserAuthorityCompanyDTOList() != null && !userDetailDTO.getUserAuthorityCompanyDTOList().isEmpty()) {
            // admin case -> save one role for each authority or company
            saveUserAuthorityCompanyDTOList(userDetailDTO, aaaUserId);
        } else {
            // simple case -> save one role for user
            saveUserRightsAndRoles(userDetailDTO, aaaUserId);
        }
        userDetailDTO.getUser().setId(aaaUserId);
    }

    private void updateAuthorityCompanyRoles(UserDetailDTO userDetailDTO) {
        if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            UserAuthority userAuthority = userAuthorityRepository.findUserAuthorityByAuthorityIdAndUserDetailId(userDetailDTO.getSelectionFromDD(), userDetailDTO.getId());
            if (userAuthority != null) {
                userAuthority.setRoleInProcess(userMapperInstance.optionDTOToCatalogValue(userDetailDTO.getRoleInProcess()));
                userAuthorityRepository.save(userAuthority);
            }
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            UserCompany userCompany = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(userDetailDTO.getSelectionFromDD(), userDetailDTO.getId());
            if (userCompany != null) {
                userCompany.setRoleInProcess(userMapperInstance.optionDTOToCatalogValue(userDetailDTO.getRoleInProcess()));
                userCompanyRepository.save(userCompany);
            }
        }
    }

    private void saveUserAuthorityOrCompany(boolean editMode, UserDetailDTO userDetailDTO, UserDetail userDetail) {
        //Save User Authority
        if(!editMode && userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())
                && userDetailDTO.getPrimaryAuthority() != null && userDetailDTO.getPrimaryAuthority().getId() != null) {
            // Find user group to assign to authority.
            UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);

            UserAuthority userAuthority = new UserAuthority();
            Authority authority = new Authority();
            authority.setId(userDetailDTO.getPrimaryAuthority().getId());
            userAuthority.setAuthority(authority);
            userAuthority.setUserDetail(userDetail);
            userAuthority.setUserGroup(userGroup);
            userAuthority.setRoleInProcess(userMapperInstance.optionDTOToCatalogValue(userDetailDTO.getRoleInProcess()));
            userAuthorityRepository.save(userAuthority);
        }

        //Save User Company
        if(!editMode && userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())
                && userDetailDTO.getPrimaryCompany() != null && userDetailDTO.getPrimaryCompany().getId() != null) {
            // Find user group to assign to company.
            UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);

            UserCompany userCompany = new UserCompany();
            Company company = new Company();
            company.setId(userDetailDTO.getPrimaryCompany().getId());
            userCompany.setCompany(company);
            userCompany.setUserDetail(userDetail);
            userCompany.setUserGroup(userGroup);
            userCompany.setRoleInProcess(userMapperInstance.optionDTOToCatalogValue(userDetailDTO.getRoleInProcess()));
            userCompanyRepository.save(userCompany);
        }

    }

    // Ensure the username is unique. Each time a random username is generated, it is cross checked with the existing usernames in the db.
    // In case a username already exists, a new username is generated.
    private String generateUniqueUsername(){
        String username;
        do{
            log.info(LOG_PREFIX + "Generating username...");
            username =  UsernamePasswordUtil.generateUsernameOrPassword(true);
        }while(userDetailRepository.countByUserUsername(username) > 0 );

        return username;
    }

    /**
     * Links an existing user with an authority or company.
     * @param userDetailDTO The given dto object to create user.
     * @return The linked user in the system.
     */
    public UserDetailDTO linkUser(UserDetailDTO userDetailDTO) {
        log.info(LOG_PREFIX + "Linking user...");
        userRightsValidator.checkIfActingUserIsEntitledToEditUser(userDetailDTO);
        // Check if is valid according to business rules.
        UserDetail userDetailDB = userDetailRepository.findByEmail(userDetailDTO.getEmail());

        if (!userDetailDB.getUserType().toString().equals(userDetailDTO.getUserType().getId().toString())) {
            EcertBYErrorException exception = new EcertBYErrorException("error_already_exists", "error_already_exists", "email", "userDetailDTO", null, true);
            throw new QCouldNotSaveException("Errors for user detail save action", new EcertBYGeneralException(Collections.singletonList(exception)));
        }

        if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())
            && userDetailDTO.getPrimaryAuthority() != null && userDetailDTO.getPrimaryAuthority().getId() != null) {
            List<UserAuthority> userAuthorities = userAuthorityRepository.findUserAuthoritiesByUserDetailId(userDetailDTO.getId());

            if (userAuthorities.stream().filter(o -> o.getAuthority().getId().equals(userDetailDTO.getPrimaryAuthority().getId())).collect(Collectors.toList()).isEmpty()) {
                //pair user - authority does not exist, add it
                UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);

                Optional<Authority> authority = authorityRepository.findById(userDetailDTO.getPrimaryAuthority().getId());
                UserAuthority userAuthority = new UserAuthority();
                userAuthority.setAuthority(authority.get());
                userAuthority.setUserDetail(userDetailDB);
                userAuthority.setUserGroup(userGroup);
                userAuthorityRepository.save(userAuthority);

                userOperationService.assignOperationsToUser(userDetailDTO.getUser().getId(), userDetailDTO);
                emailService.sendExistingAuthorityExistingUserEmail(authority.get().getName(), userDetailDB.getEmail());
                auditService.linkUserAudit(AuditAction.UPDATE,
                    userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), userDetailDB.getEmail(),
                    authority.get().getName(), AuditType.AUTHORITY, true);
                log.info(LOG_PREFIX + "User with id {} successfully linked to Authority with id {} by user with id : {}.",
                        userDetailDTO.getId(),
                        authority.get().getId(),
                        securityService.getLoggedInUserDetailId());
            }
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())
                && userDetailDTO.getPrimaryCompany() != null && userDetailDTO.getPrimaryCompany().getId() != null) {
            List<UserCompany> userCompanies = userCompanyRepository.findUserCompaniesByUserDetailId(userDetailDTO.getId());

            if (userCompanies.stream().filter(o -> o.getCompany().getId().equals(userDetailDTO.getPrimaryCompany().getId())).collect(Collectors.toList()).isEmpty()) {

                //pair user - company does not exist, add it
                UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);

                Optional<Company> company = companyRepository.findById(userDetailDTO.getPrimaryCompany().getId());
                UserCompany userCompany = new UserCompany();
                userCompany.setCompany(company.get());
                userCompany.setUserDetail(userDetailDB);
                userCompany.setUserGroup(userGroup);
                userCompanyRepository.save(userCompany);

                userOperationService.assignOperationsToUser(userDetailDTO.getUser().getId(), userDetailDTO);
                emailService.sendExistingCompanyExistingUserEmail(company.get().getName(), userDetailDB.getEmail());
                auditService.linkUserAudit(AuditAction.UPDATE,
                    userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), userDetailDB.getEmail(),
                    company.get().getName(), AuditType.COMPANY, true);
                log.info(LOG_PREFIX + "User with id {} successfully linked to Company with id {} by user with id : {}.",
                        userDetailDTO.getId(),
                        company.get().getId(),
                        securityService.getLoggedInUserDetailId());
            }
        }
        return userDetailDTO;
    }

    /**
     * Save my account.
     * @param userDetailProfileDTO Object with the information of user.
     * @return The update user.
     */
    public UserDetailDTO saveMyAccount(UserDetailProfileDTO userDetailProfileDTO, HttpServletResponse response, HttpServletRequest request){
        log.info(LOG_PREFIX + "Updating account...");
        objectLockService.checkAndThrowIfLocked(userDetailProfileDTO.getId(), ObjectType.USER);
        if(!userDetailProfileDTO.getId().equals(securityService.getLoggedInUserDetailId())){
            throw new QCouldNotSaveException("You can update only your account from this API call.");
        }
        UserDetailDTO userDetailDTO = this.findById(userDetailProfileDTO.getId());

        String loggedInUserDetailId = securityService.getLoggedInUserDetailId();

        UserDetailDTO savedUser = saveUser(userMapperInstance.map(userDetailDTO, userDetailProfileDTO), response, request);

        // LOG ACCOUNT CHANGE
        auditService.saveMyAccountAudit(userMapperInstance.map(userDetailDTO), userDetailDTO.getEmail(), userDetailDTO, this.findById(userDetailProfileDTO.getId()));
        log.info(LOG_PREFIX + "User with id : {} successfully updated their account.", loggedInUserDetailId);
        return savedUser;
    }

    /**
     * Update password.
     * @param resetPasswordDTO The object with the passwords.
     * @return True if everything went good.
     */
    public boolean updatePassword(ResetPasswordDTO resetPasswordDTO, HttpServletRequest request) {
        log.info(LOG_PREFIX + "Updating password...");
        try {
            String username = securityService.getLoggedInUserUsername();
            keycloakService.updatePassword(securityService.getLoggedInUserUsername(), resetPasswordDTO);
            log.info(LOG_PREFIX + "User with id : {} successfully updated their password.",
                    securityService.getLoggedInUserDetailId());
            try {
                logoutService.logout(username, request);
                return true;
            } catch (IOException | ServletException ex) {
                throw new QExceptionWrapper("Something wrong went with redirecting to logout.");
            }
        } catch (EcertBYGeneralException ex) {
            throw new QCouldNotSaveException("The given password/OTP is not valid", ex);
        } catch (Exception ex) {
            throw new QCouldNotSaveException("Something wrong went with keycloak reset password.");
        }
    }

    public EmailNotificationDTO getEmailNotificationSettings() {
        log.info(LOG_PREFIX + "Getting email notification settings...");
        List<EmailNotificationType> allNotifications = emailNotificationRepository.findEmailNotificationTypesByUserDetail(securityService.getLoggedInUserDetail());
        EmailNotificationDTO emailNotificationDTO = new EmailNotificationDTO();
        emailNotificationDTO.setEmailNotificationList(allNotifications);
        return emailNotificationDTO;
    }

    public void updateEmailNotificationSettings(EmailNotificationDTO emailNotificationsDTO) {
        log.info(LOG_PREFIX + "Updating email notification settings...");
        UserDetail userDetail = securityService.getLoggedInUserDetail();
        emailNotificationRepository.deleteAllByUserDetail(userDetail);
        for (EmailNotificationType notificationType : emailNotificationsDTO.getEmailNotificationList()) {
            EmailNotification emailNotification = new EmailNotification();
            emailNotification.setUserDetail(userDetail);
            emailNotification.setNotificationType(notificationType);
            emailNotificationRepository.save(emailNotification);
        }
        auditService.saveUserEmailNotificationsAudit(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()));
        log.info(LOG_PREFIX + "User with id : {} successfully updated their email notification settings.",
                securityService.getLoggedInUserDetailId());
    }

    public UserDetailDTO getUser(String id, boolean myaccount, String selectionFromDD) {
        if (myaccount) {
            String loggedInId = securityService.getLoggedInUserDetailId();
            if (!id.equals(loggedInId)) {
                throw new QAuthorisationException("You can only modify your account");
            }
        }
        UserDetailDTO userDetailDTO = findById(id);
        if (!myaccount) {
            if (selectionFromDD != null && !ADMIN_RESOURCE.equals(selectionFromDD)) {
                userDetailDTO = getAuthorityOrCompanyUser(userDetailDTO, id, selectionFromDD);
            } else {
                if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
                    userDetailDTO.setUserAuthorityCompanyDTOList(getUserAuthoritiesAndRoles(userDetailDTO.getId()));
                } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
                    userDetailDTO.setUserAuthorityCompanyDTOList(getUserCompaniesAndRoles(userDetailDTO.getId()));
                }
            }
        }
        if (userDetailDTO == null) {
            throw new QAuthorisationException("User does not exist or you don't have access on user");
        }
        return userDetailDTO;
    }

    private UserDetailDTO getAuthorityOrCompanyUser(UserDetailDTO userDetailDTO, String id, String selectionFromDD) {
        if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            UserAuthority userAuthority = userAuthorityRepository.findUserAuthorityByAuthorityIdAndUserDetailId(selectionFromDD, id);
            userRightsValidator.checkUserAuthorityOrCompanyIfNull(userAuthority, id, selectionFromDD, false);
            OptionDTO userGroupOptionDTO = new OptionDTO();
            userGroupOptionDTO.setId(userAuthority.getUserGroup().getId());
            userGroupOptionDTO.setName(userAuthority.getUserGroup().getDescription());
            userDetailDTO.setRole(userGroupOptionDTO);
            userDetailDTO.setRoleName(userAuthority.getUserGroup().getDescription());
            userDetailDTO.setRoleInProcess(userMapperInstance.catalogValueToOptionDTO(userAuthority.getRoleInProcess()));
        } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            UserCompany userCompany = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(selectionFromDD, id);
            userRightsValidator.checkUserAuthorityOrCompanyIfNull(userCompany, id, selectionFromDD, true);
            OptionDTO userGroupOptionDTO = new OptionDTO();
            userGroupOptionDTO.setId(userCompany.getUserGroup().getId());
            userGroupOptionDTO.setName(userCompany.getUserGroup().getDescription());
            userDetailDTO.setRole(userGroupOptionDTO);
            userDetailDTO.setRoleName(userCompany.getUserGroup().getDescription());
            userDetailDTO.setRoleInProcess(userMapperInstance.catalogValueToOptionDTO(userCompany.getRoleInProcess()));
        } else {
            return null;
        }
        return userDetailDTO;
    }

    private List<UserAuthorityCompanyDTO> getUserAuthoritiesAndRoles(String userDetailId) {
        List<UserAuthority> userAuthorities = userAuthorityRepository.findUserAuthoritiesByUserDetailId(userDetailId);
        List<UserAuthorityCompanyDTO> userAuthorityCompanyDTOList = new ArrayList<>();
        for (UserAuthority userAuthority : userAuthorities) {
            UserAuthorityCompanyDTO userAuthorityCompanyDTO = authorityMapperInstance.mapToUserAuthorityCompanyDTO(userAuthority);
            userAuthorityCompanyDTOList.add(userAuthorityCompanyDTO);
        }
        return userAuthorityCompanyDTOList;
    }

    private List<UserAuthorityCompanyDTO> getUserCompaniesAndRoles(String userDetailId) {
        List<UserCompany> userCompanyList = userCompanyRepository.findUserCompaniesByUserDetailId(userDetailId);
        List<UserAuthorityCompanyDTO> userAuthorityCompanyDTOList = new ArrayList<>();
        for (UserCompany userCompany : userCompanyList) {
            UserAuthorityCompanyDTO userAuthorityCompanyDTO = companyMapperInstance.mapToUserAuthorityCompanyDTO(userCompany);
            userAuthorityCompanyDTOList.add(userAuthorityCompanyDTO);
        }
        return userAuthorityCompanyDTOList;
    }

    private UserDetailDTO findMainUser(UserDetailDTO userDetailDTO){
        return userMapperInstance.map(userDetailRepository.findByEmail(userDetailDTO.getEmail()));
    }

    private void saveKeycloakUser(UserDetailDTO userDetailDTO, String usernameDB){
        if(usernameDB == null){
            keycloakService.createKeycloakUser(userDetailDTO);
            return;
        }
        keycloakService.updateKeycloakUser(userDetailDTO, usernameDB);
    }

    private String saveAAAUser(UserDetailDTO userDetailDTO){
        String aaaUserId = userDetailDTO.getUser().getId();
        if(userDetailDTO.getUser().getId() != null) {
            userService.updateUser(mapperInstanceUser.map(userDetailDTO.getUser()), false, false);
        } else {
            // Create user in aaa.
            aaaUserId = userService.createUser(mapperInstanceUser.map(userDetailDTO.getUser()), null);
            if(aaaUserId == null){
                throw new QCouldNotSaveException("AAA user did not created");
            }
        }
        return aaaUserId;
    }

    /**
     * Save / Update user's roles for selected company/authority and for admin users
     *
     * @param userDetailDTO the UserDetailDTO
     * @param aaaUserId the user's qlack id
     */
    private void saveUserRightsAndRoles(UserDetailDTO userDetailDTO, String aaaUserId) {
        if (userDetailDTO.getRole() != null && userDetailDTO.getRole().getId() != null) {
            boolean roleChanged = false;
            if (userDetailDTO.getUser().getId() != null) {
                // In case of update, remove existing role/operations to add the correct ones later.
                if (UserType.ADMIN_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                    roleChanged = saveUserRightsAndRolesAdmin(userDetailDTO, aaaUserId);
                }
                else if (UserType.AUTHORITY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                    roleChanged = saveUserRightsAndRolesAuthority(userDetailDTO);
                } else if (UserType.COMPANY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                    roleChanged = saveUserRightsAndRolesCompany(userDetailDTO);
                }
            }
            saveUserRightsAndRolesHandleOperations(roleChanged, userDetailDTO, aaaUserId);
        }
    }

    /**
     * Removes and reassigns roles / groups to user
     *
     * @param roleChanged if the role has changed, in order to remove previous operations
     * @param userDetailDTO the UserDetailDTO
     * @param aaaUserId the user's qlack id
     */
    private void saveUserRightsAndRolesHandleOperations(boolean roleChanged, UserDetailDTO userDetailDTO, String aaaUserId) {
        if (roleChanged) {
            userOperationService.removeExistingOperationsFromUser(userDetailDTO);
        }
        // Assign the appropriate role/operations to user.
        if (UserType.ADMIN_USER.toString().equals(userDetailDTO.getUserType().getId())
                && (roleChanged || userDetailDTO.getUser().getId() == null)) {
            userService.addUserGroups(Collections.singleton(userDetailDTO.getRole().getId()), aaaUserId);
        }
        if (roleChanged || userDetailDTO.getUser().getId() == null) {
            userOperationService.assignOperationsToUser(aaaUserId, userDetailDTO);
        }

    }

    /**
     * Save / Update admin's user roles
     *
     * @param userDetailDTO the UserDetailDTO
     * @param aaaUserId the user's qlack id
     * @return
     */
    private boolean saveUserRightsAndRolesAdmin(UserDetailDTO userDetailDTO, String aaaUserId) {
        boolean roleChanged = false;
        Set<String> groupsIds = userGroupService.getUserGroupsIds(aaaUserId);
        if (!groupsIds.isEmpty() && !groupsIds.iterator().next().equals(userDetailDTO.getRole().getId())) {
            roleChanged = true;
            userService.removeUserGroups(groupsIds, aaaUserId);
            userOperationService.removeExistingOperationsFromUserGroup(aaaUserId);
        }
        return roleChanged;
    }

    /**
     * Save / Update user's roles for selected authority
     *
     * @param userDetailDTO the UserDetailDTO
     * @return
     */
    private boolean saveUserRightsAndRolesAuthority(UserDetailDTO userDetailDTO) {
        boolean roleChanged = false;
        UserAuthority userAuthority = userAuthorityRepository.findUserAuthorityByAuthorityIdAndUserDetailId(userDetailDTO.getSelectionFromDD(), userDetailDTO.getId());
        if (!userAuthority.getUserGroup().getId().equals(userDetailDTO.getRole().getId())) {
            roleChanged = true;
            UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);
            userAuthority.setUserGroup(userGroup);
            userAuthorityRepository.save(userAuthority);
        }
        return roleChanged;
    }


    /**
     * Save / Update user's roles for selected company/authority and for admin users
     *
     * @param userDetailDTO the UserDetailDTO
     * @return
     */
    private boolean saveUserRightsAndRolesCompany(UserDetailDTO userDetailDTO) {
        boolean roleChanged = false;
        UserCompany userCompany = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(userDetailDTO.getSelectionFromDD(), userDetailDTO.getId());
        if (!userCompany.getUserGroup().getId().equals(userDetailDTO.getRole().getId())) {
            roleChanged = true;
            UserGroup userGroup = userGroupRepository.findById(userDetailDTO.getRole().getId()).orElse(null);
            userCompany.setUserGroup(userGroup);
            userCompanyRepository.save(userCompany);
        }
        return roleChanged;
    }

    /**
     * admin case: save one role for each authority or company
     *
     * @param userDetailDTO the UserDetailDTO
     * @param aaaUserId the user's qlack id
     */
    private void saveUserAuthorityCompanyDTOList(UserDetailDTO userDetailDTO, String aaaUserId) {
        for (UserAuthorityCompanyDTO userAuthorityCompanyDTO : userDetailDTO.getUserAuthorityCompanyDTOList()) {
            if (UserType.AUTHORITY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                UserAuthority userAuthority = userAuthorityRepository.findUserAuthorityByAuthorityIdAndUserDetailId(userAuthorityCompanyDTO.getAuthorityCompanyId(), userDetailDTO.getId());
                if (!userAuthority.getUserGroup().getId().equals(userAuthorityCompanyDTO.getUserGroupId())) {
                    userOperationService.removeExistingOperationsFromUser(aaaUserId, userAuthorityCompanyDTO.getAuthorityCompanyId());
                    UserGroup userGroup = userGroupRepository.findById(userAuthorityCompanyDTO.getUserGroupId()).orElse(null);
                    userAuthority.setUserGroup(userGroup);
                    userAuthorityRepository.save(userAuthority);
                    userOperationService.assignOperationsToUserObjectId(aaaUserId, userAuthority.getUserGroup().getName(), userAuthorityCompanyDTO.getAuthorityCompanyId());
                }
                if ((userAuthority.getRoleInProcess() == null || !userAuthority.getRoleInProcess().getId().equals(userAuthorityCompanyDTO.getRoleInProcess())) &&
                        userAuthorityCompanyDTO.getRoleInProcess() != null) {
                    CatalogValue roleInProcessCatalogValue = catalogValueRepository.findById(userAuthorityCompanyDTO.getRoleInProcess()).orElse(null);
                    userAuthority.setRoleInProcess(roleInProcessCatalogValue);
                    userAuthorityRepository.save(userAuthority);
                }
            } else if (UserType.COMPANY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                UserCompany userCompany = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(userAuthorityCompanyDTO.getAuthorityCompanyId(), userDetailDTO.getId());
                if (!userCompany.getUserGroup().getId().equals(userAuthorityCompanyDTO.getUserGroupId())) {
                    userOperationService.removeExistingOperationsFromUser(aaaUserId, userAuthorityCompanyDTO.getAuthorityCompanyId());
                    UserGroup userGroup = userGroupRepository.findById(userAuthorityCompanyDTO.getUserGroupId()).orElse(null);
                    userCompany.setUserGroup(userGroup);
                    userCompanyRepository.save(userCompany);
                    userOperationService.assignOperationsToUserObjectId(aaaUserId, userCompany.getUserGroup().getName(), userAuthorityCompanyDTO.getAuthorityCompanyId());
                }
                if ((userCompany.getRoleInProcess() == null || !userCompany.getRoleInProcess().getId().equals(userAuthorityCompanyDTO.getRoleInProcess())) &&
                        userAuthorityCompanyDTO.getRoleInProcess() != null) {
                    CatalogValue roleInProcessCatalogValue = catalogValueRepository.findById(userAuthorityCompanyDTO.getRoleInProcess()).orElse(null);
                    userCompany.setRoleInProcess(roleInProcessCatalogValue);
                    userCompanyRepository.save(userCompany);
                }
            }
        }
    }

    public List<OptionDTO> getUsers(String selectionFromDD) {
        List<UserDetail> userList = new ArrayList<>();
        UserDetailDTO userDetail = securityService.getLoggedInUserDetailDTO();
        if (userDetail.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            List<Company> actingUserCompanies = companyRepository.getUserCompanies(userDetail.getId());
            Optional<Company> companyOptional = actingUserCompanies.stream().filter(o -> o.getId().equals(selectionFromDD)).findFirst();
            if (!companyOptional.isPresent()) {
                String errorMessage = "Selected Company %s does not belong to the user's %s company list.".
                                formatted(selectionFromDD, userDetail.getId());
                log.warn(LOG_PREFIX + errorMessage);
                throw new QAuthorisationException(errorMessage);
            }
            userList = userDetailRepository.findAllUsersByCompany(selectionFromDD);
        } else if (userDetail.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            List<Authority> actingUserAuthorities = authorityRepository.getUserAuthoritiesOrderByName(userDetail.getId());
            Optional<Authority> authorityOptional = actingUserAuthorities.stream().filter(o -> o.getId().equals(selectionFromDD)).findFirst();
            if (!authorityOptional.isPresent()) {
                String errorMessage = "Selected Authority %s does not belong to the user's %s authority list.".
                                formatted(selectionFromDD, userDetail.getId());
                log.warn(LOG_PREFIX + errorMessage);
                throw new QAuthorisationException(errorMessage);
            }
            userList = userDetailRepository.findAllUsersByAuthority(selectionFromDD);
        }
        return userMapperInstance.mapToListOptionDTO(userList);
    }

    public UserDetailDTO findByEmail(String email) {
        return userMapperInstance.map(userDetailRepository.findByEmail(email));
    }

    public boolean activateUser(boolean isActive, String id) {
        boolean result;
        objectLockService.checkAndThrowIfLocked(id, ObjectType.USER);
        List<EcertBYErrorException> errors = new ArrayList<>();
        if (!isActive) {
            log.info(LOG_PREFIX + "Deactivating user...");
            if (securityService.getLoggedInUserDetailId().equals(id)) {
                throw new QCouldNotSaveException(USER_DEACTIVATE_ACTION_ERRORS, new EcertBYGeneralException(Collections.singletonList(new EcertBYErrorException("cannot_deactivate_same_user", "cannot_deactivate_same_user", null, null, null, true))));
            }
            if (AppConstants.ROOT_UUID.equals(id)) {
                Optional<UserDetail> rootUser = userDetailRepository.findById(id);
                String message = MessageConfig.getValue("cannot_deactivate_root_user", new Object[] {rootUser.get().getFirstName() + " " + rootUser.get().getLastName()});
                throw new QCouldNotSaveException(USER_DEACTIVATE_ACTION_ERRORS, new EcertBYGeneralException(Collections.singletonList(new EcertBYErrorException(message, message, null, null, Collections.singletonList(rootUser.get().getFirstName() + " " + rootUser.get().getLastName()), true))));
            }
            // Validate if user can deactivate
            log.info(LOG_PREFIX + "Validating deactivation...");
            userValidator.validateDeactivateUser(id, errors);
            if (!errors.isEmpty()) {
                throw new QCouldNotSaveException(USER_DEACTIVATE_ACTION_ERRORS, new EcertBYGeneralException(errors));
            }
            result = activate(false, id, UserDetail.class);
            if (result) {
                log.info(LOG_PREFIX + "User with id {} successfully deactivated by user with id : {}",
                        id, securityService.getLoggedInUserDetailId());
            }
        } else {
            log.info(LOG_PREFIX + "Activating user...");
            // Validate if user can be activated
            log.info(LOG_PREFIX + "Validating activation...");
            userValidator.validateActivateUser(id, errors);
            if (!errors.isEmpty()) {
                throw new QCouldNotSaveException("Errors for user detail activate action", new EcertBYGeneralException(errors));
            }
            updatePrimarySelectionIfNeeded(id);
            result = activate(true, id, UserDetail.class);
            if (result) {
                log.info(LOG_PREFIX + "User with id {} successfully activated by user with id : {}",
                        id, securityService.getLoggedInUserDetailId());
            }
        }
        return result;
    }

    private void updatePrimarySelectionIfNeeded(String id) {
        UserDetail userDetail = findEntityById(id);

        if(userDetail.getUserType().equals(UserType.AUTHORITY_USER)) {
            Optional<Authority> primaryAuthority = authorityRepository.findById(userDetail.getPrimaryAuthority().getId());
            if(primaryAuthority.isPresent()){
                if(!primaryAuthority.get().isActive()) {
                    log.info(LOG_PREFIX + "Updating primary authority...");
                    List<Authority> allUserAuthorities = authorityRepository.getUserAuthoritiesOrderByName(userDetail.getId());
                    Authority firstActiveAuthority = allUserAuthorities.stream().findFirst().orElse(null);
                    userDetail.setPrimaryAuthority(firstActiveAuthority);
                }
            }
        } else if(userDetail.getUserType().equals(UserType.COMPANY_USER)) {
            Optional<Company> primaryCompany = companyRepository.findById(userDetail.getPrimaryCompany().getId());
            if(primaryCompany.isPresent()){
                if(!primaryCompany.get().isActive()) {
                    log.info(LOG_PREFIX + "Updating primary company...");
                    List<Company> allUserCompanies = companyRepository.getUserCompanies(userDetail.getId());
                    Company firstActiveAuthority = allUserCompanies.stream().findFirst().orElse(null);
                    userDetail.setPrimaryCompany(firstActiveAuthority);
                }
            }
        }
    }

    public void deleteUser(UserDetail userDetail) {
        // User rights should be validated where this method is called.
        // Check if deletes own user
        if (securityService.getLoggedInUserDetailId().equals(userDetail.getId())) {
            String errorMessage = LOG_PREFIX + "Deleting your own user is not allowed.";
            throw new QCouldNotSaveException(USER_DELETE_ACTION_ERRORS,
                new EcertBYGeneralException(Collections.singletonList(
                    new EcertBYErrorException("cannot_delete_same_user", "cannot_delete_same_user", null, null, null, true))));
        }
        // delete user
        log.info(LOG_PREFIX + "Deleting user...");
        userDetailRepository.delete(userDetail);
        // delete aaa user
        userService.deleteUser(userDetail.getUser().getId());
        // delete keycloak user
        keycloakService.deleteUserFromKeycloak(userDetail.getUser().getUsername());
        log.info(LOG_PREFIX + "User with id {} successfully deleted by user with id : {}.",
                userDetail.getId(),
                securityService.getLoggedInUserDetailId());
    }

    public UserGroupDTO findRoleFromSelection(String selectionFromDD) {
        UserDetail loggedInUser = securityService.getLoggedInUserDetail();
        UserGroup userGroup = null;
        if(loggedInUser.getUserType().equals(UserType.AUTHORITY_USER)) {
            userGroup =  userAuthorityRepository.findUserAuthorityUserGroup(selectionFromDD, loggedInUser.getId());
        } else if(loggedInUser.getUserType().equals(UserType.COMPANY_USER)) {
            userGroup =  userCompanyRepository.findUserCompanyUserGroup(selectionFromDD, loggedInUser.getId());
        }

        if(userGroup != null) {
            UserGroupDTO dto = new UserGroupDTO();
            dto.setName(userGroup.getName());
            dto.setDescription(userGroup.getDescription());
            dto.setId(userGroup.getId());
            return dto;
        } else {
            return null;
        }
    }

}
