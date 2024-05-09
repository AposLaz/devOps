package lgl.bayern.de.ecertby.validator;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.UserAuthorityCompanyDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.UserDetailProfileDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.model.UserAuthority;
import lgl.bayern.de.ecertby.model.UserCompany;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Validator for the user detail resource.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class UserDetailValidator {

    private final UserRightsValidator userRightsValidator;

    private final UserDetailRepository userDetailRepository;

    private final UserGroupRepository userGroupRepository;

    private final UserAuthorityRepository userAuthorityRepository;

    private final UserCompanyRepository userCompanyRepository;

    private final CompanyRepository companyRepository;

    private final AuthorityRepository authorityRepository;

    private static final String USERDETAILDTO = "userDetailDTO";

    private static final String EMAIL_ALREADY_EXISTS = "email_already_exists";

    private static final String NEW_USER_ID = "NEW_USER";

    /**
     * Validates the user detail save for all the business rules.
     * @param userDetailDTO The object with user's information.
     */
    public void validateUserDetail(UserDetailDTO userDetailDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();
        final String USER_ID = !isNull(userDetailDTO.getId()) ? userDetailDTO.getId() : NEW_USER_ID;

        // Validate DTO fields
        dtoValidation(userDetailDTO, errors);

        // Validate username
        validateUsername(userDetailDTO, errors);

        // Validate if acting user is entitled businesswise to create / update user.
        userRightsValidator.checkIfActingUserIsEntitledToEditUser(userDetailDTO, errors);

        // Validate if user's email already exists.
        emailExists(userDetailDTO.getEmail(), userDetailDTO.getId(), errors);

        // Validate if user's username already exists.
        usernameExists(userDetailDTO.getUsername(), userDetailDTO.getId(), errors);

        // Validate user roles
        validateUserRoles(userDetailDTO, errors);

        if(!errors.isEmpty()){
            throw new QCouldNotSaveException("Errors for user detail save action", new EcertBYGeneralException(errors));
        } else {
            // Log successful validation.
            log.info("Validation of user with ID: {} was successful.", USER_ID);
        }
    }

    /**
     * A first level validation to confirm that the mandatory fields are present.
     * Should reflect the respective validations of the front-end.
     * @param userDetailDTO - The user detail DTO.
     * @param errors - The list of errors to append if any.
     */
    private void dtoValidation(UserDetailDTO userDetailDTO, List<EcertBYErrorException> errors) {
        // Initialize data
        final String USER_ID = !isNull(userDetailDTO.getId()) ? userDetailDTO.getId() : NEW_USER_ID;
        boolean isNewUser = USER_ID.equals(NEW_USER_ID);
        String userType = userDetailDTO.getUserType().getId();

        // Any new user should have a role
        if (isNewUser && isNull(userDetailDTO.getRole())) {
            String errorMessage = LOG_PREFIX + "New users should have a role. User was not created.";
            log.warn(errorMessage);
            errors.add(new EcertBYErrorException("user_role_missing", "user_role_missing", "role", USERDETAILDTO, null, true));
        }

        // Check if new user has any valid type
        if (UserType.getEnumValues().stream().noneMatch(v -> v.equals(userType))) {
            String errorMessage = LOG_PREFIX + USER_ID
                + " does not have a valid user type. User was not created.";
            log.warn(errorMessage);
            errors.add(new EcertBYErrorException("user_user_type_missing", "user_user_type_missing", "userType", USERDETAILDTO, null, true));
        }

        // Company user validation
        if (UserType.COMPANY_USER.name().equals(userType)) {
            dtoValidationCompanyUser(isNewUser, userDetailDTO, errors);
        }

        // Authority user validation
        if (UserType.AUTHORITY_USER.name().equals(userType)) {
            dtoValidationAuthorityUser(isNewUser, userDetailDTO, errors);
        }
    }

    private void dtoValidationCompanyUser(boolean isNewUser, UserDetailDTO userDetailDTO, List<EcertBYErrorException> errors) {
        // New company user should have an active primary company
        if (isNewUser && isNull(userDetailDTO.getPrimaryCompany())) {
            String errorMessage = LOG_PREFIX + UserType.COMPANY_USER.name()
                    + " should have a primary company. User was not created.";
            log.warn(errorMessage);
            errors.add(new EcertBYErrorException("user_primary_company_missing", "user_primary_company_missing", "primaryCompany", USERDETAILDTO, null, true));
        } else if (isNewUser && !isNull(userDetailDTO.getPrimaryCompany())) {
            Optional<Company> company = companyRepository.findById(userDetailDTO.getPrimaryCompany().getId());
            if (!(company.isPresent() && company.get().isActive())) {
                String errorMessage = LOG_PREFIX + UserType.COMPANY_USER.name()
                        + " should have an active primary company. User was not created.";
                log.warn(errorMessage);
                errors.add(new EcertBYErrorException("user_active_primary_company_missing", "user_active_primary_company_missing", "primaryCompany", USERDETAILDTO, null, true));
            }
        }
    }

    private void dtoValidationAuthorityUser(boolean isNewUser, UserDetailDTO userDetailDTO, List<EcertBYErrorException> errors) {
        // New authority user should have an active primary authority
        if (isNewUser && isNull(userDetailDTO.getPrimaryAuthority())) {
            String errorMessage = LOG_PREFIX + UserType.AUTHORITY_USER.name()
                    + " should have a primary authority. User was not created.";
            log.warn(errorMessage);
            errors.add(new EcertBYErrorException("user_primary_authority_missing", "user_primary_authority_missing", "primaryAuthority", USERDETAILDTO, null, true));
        } else if (isNewUser && !isNull(userDetailDTO.getPrimaryAuthority())) {
            Optional<Authority> authority = authorityRepository.findById(userDetailDTO.getPrimaryAuthority().getId());
            if (!(authority.isPresent() && authority.get().isActive())) {
                String errorMessage = LOG_PREFIX + UserType.AUTHORITY_USER.name()
                        + " should have an active primary authority. User was not created.";
                log.warn(errorMessage);
                errors.add(new EcertBYErrorException("user_active_primary_authority_missing", "user_active_primary_authority_missing", "primaryAuthority", USERDETAILDTO, null, true));
            }
        }
    }

    /**
     * Checks if user role change can be performed
     *
     * @param userDetailDTO The UserDetailDTO
     * @param errors A list of errors
     */
    private void validateUserRoles(UserDetailDTO userDetailDTO, List<EcertBYErrorException> errors) {
       if (userDetailDTO.getUserAuthorityCompanyDTOList() != null && !userDetailDTO.getUserAuthorityCompanyDTOList().isEmpty()) {
           UserGroup userGroupAuthorityMainUser = userGroupRepository.findByName(UserRole.AUTHORITY_MAIN_USER.toString());
           UserGroup userGroupCompanyMainUser = userGroupRepository.findByName(UserRole.COMPANY_MAIN_USER.toString());
           List<UserAuthorityCompanyDTO> userAuthorityCompanyDTOList = userDetailDTO.getUserAuthorityCompanyDTOList().stream()
                   .filter(o -> !o.getUserGroupId().equals(userGroupCompanyMainUser.getId()) && !o.getUserGroupId().equals(userGroupAuthorityMainUser.getId()))
                   .collect(Collectors.toList());
           checkIfLastAuthorityCompanyUser(userAuthorityCompanyDTOList, userDetailDTO, errors);
       }
    }

    /**
     * Checks if user role change can be performed
     *
     * @param userAuthorityCompanyDTOList A List with the user's authorities or companies
     * @param userDetailDTO The UserDetailDTO
     * @param errors A list of errors
     */
    private void checkIfLastAuthorityCompanyUser(List<UserAuthorityCompanyDTO> userAuthorityCompanyDTOList, UserDetailDTO userDetailDTO, List<EcertBYErrorException> errors) {
        for (UserAuthorityCompanyDTO userAuthorityCompanyDTO : userAuthorityCompanyDTOList) {
            if (userDetailDTO.getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
                List<UserDetail> userDetailList = userAuthorityRepository.findActiveUsersByAuthorityIdAndUserGroupName(userAuthorityCompanyDTO.getAuthorityCompanyId(), UserRole.AUTHORITY_MAIN_USER.toString());
                if (userDetailList.size() == 1 && userDetailList.get(0).getId().equals(userDetailDTO.getId())) {
                    String errorMessage = MessageConfig.getValue("error_authority_last_main_user", new Object[] {userAuthorityCompanyDTO.getAuthorityCompanyName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage, "", USERDETAILDTO, null, true));
                }
            } else if (userDetailDTO.getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
                List<UserDetail> userDetailList = userCompanyRepository.findActiveUsersByCompanyIdAndUserGroupName(userAuthorityCompanyDTO.getAuthorityCompanyId(), UserRole.COMPANY_MAIN_USER.toString());
                if (userDetailList.size() == 1 && userDetailList.get(0).getId().equals(userDetailDTO.getId())) {
                    String errorMessage = MessageConfig.getValue("error_company_last_main_user", new Object[] {userAuthorityCompanyDTO.getAuthorityCompanyName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage,  "", USERDETAILDTO, null, true));
                }
            }
        }
    }

    /**
     * Check if email already exists.
     * @param email The email to check.
     * @param userDetailId The user detail id.
     * @param errors The errors list to update.
     */
    private void emailExists(String email, String userDetailId, List<EcertBYErrorException> errors) {
        if(userDetailId == null && userDetailRepository.findByEmail(email) != null) {
            errors.add(new EcertBYErrorException(EMAIL_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, "email", USERDETAILDTO, null, true));
        }
        if(userDetailId != null && userDetailRepository.findByEmailAndIdNot(email, userDetailId) != null){
            errors.add(new EcertBYErrorException(EMAIL_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, "email", USERDETAILDTO, null, true));
        }
    }

    /**
     * Check if username already exists.
     * @param username The username to check.
     * @param userDetailId The user detail id.
     * @param errors The errors list to update.
     */
    private void usernameExists(String username, String userDetailId, List<EcertBYErrorException> errors){
        if(userDetailId != null && userDetailRepository.findByUserUsernameIgnoreCaseAndIdNot(username, userDetailId) != null){
            errors.add(new EcertBYErrorException("error_username_exists", "error_username_exists", "user.username", USERDETAILDTO, null, true));
        }
    }

    /**
     * Validate the username not to contains first or last name.
     * @param userDetailProfileDTO The object the user's detail information.
     * @param errors The errors list to update.
     */
    private void validateUsername(UserDetailProfileDTO userDetailProfileDTO, List<EcertBYErrorException> errors){
        if (userDetailProfileDTO.getUser() == null){
            return;
        }
        StringBuilder regexp = new StringBuilder();
        regexp.append("^(?=.*(?:").append(userDetailProfileDTO.getFirstName()).append("|").append(userDetailProfileDTO.getLastName()).append(")).*$");
        Pattern pattern = Pattern.compile(regexp.toString());
        if(pattern.matcher(userDetailProfileDTO.getUser().getUsername()).find()){
            errors.add(new EcertBYErrorException("error_username_wrong_format","error_username_wrong_format" , "user.username", USERDETAILDTO, null, true));
        }
    }

    public void validateDeactivateUser(String id, List<EcertBYErrorException> errors) {
        Optional<UserDetail> userDetail = userDetailRepository.findById(id);
        if (userDetail.isPresent() && userDetail.get().getUserType().equals(UserType.COMPANY_USER)) {
            List<UserCompany> userCompanyList = userCompanyRepository.findUserCompaniesByUserDetailId(id);
            for (UserCompany userCompany : userCompanyList) {
                List<UserDetail> userDetailList = userCompanyRepository.findActiveUsersByCompanyIdAndUserGroupName(userCompany.getCompany().getId(), UserRole.COMPANY_MAIN_USER.toString());
                if (userDetailList.size() == 1 && userDetailList.get(0).getId().equals(id)) {
                    String errorMessage = MessageConfig.getValue("error_deactivate_company_user", new Object[] {userCompany.getCompany().getName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
                }
            }
        } else if (userDetail.isPresent() && userDetail.get().getUserType().equals(UserType.AUTHORITY_USER)) {
            List<UserAuthority> userAuthorityList = userAuthorityRepository.findUserAuthoritiesByUserDetailId(id);
            for (UserAuthority userAuthority : userAuthorityList) {
                List<UserDetail> userDetailList = userAuthorityRepository.findActiveUsersByAuthorityIdAndUserGroupName(userAuthority.getAuthority().getId(), UserRole.AUTHORITY_MAIN_USER.toString());
                if (userDetailList.size() == 1 && userDetailList.get(0).getId().equals(id)) {
                    String errorMessage = MessageConfig.getValue("error_deactivate_authority_user", new Object[] {userAuthority.getAuthority().getName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
                }
            }
        }
    }

    public void validateActivateUser(String id, List<EcertBYErrorException> errors) {
        Optional<UserDetail> userDetail = userDetailRepository.findById(id);
        if(userDetail.isPresent()) {
            if (userDetail.get().getUserType().equals(UserType.COMPANY_USER)) {
                List<Company> companyList = userCompanyRepository.findCompaniesByUserId(id);
                boolean notActiveCompanies = companyList.stream().noneMatch(Company::isActive);
                if (notActiveCompanies) {
                    String errorMessage = MessageConfig.getValue("error_activate_company_user", new Object[]{userDetail.get().getFirstName(), userDetail.get().getLastName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
                }
            } else if (userDetail.get().getUserType().equals(UserType.AUTHORITY_USER)) {
                List<Authority> authorityList = userAuthorityRepository.findAuthoritiesByUserId(id);
                boolean notActiveAuthorities = authorityList.stream().noneMatch(Authority::isActive);
                if (notActiveAuthorities) {
                    String errorMessage = MessageConfig.getValue("error_activate_authority_user", new Object[]{userDetail.get().getFirstName(), userDetail.get().getLastName()});
                    errors.add(new EcertBYErrorException(errorMessage, errorMessage, null, null, null, true));
                }
            }
        }
    }
}
