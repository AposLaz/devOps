package lgl.bayern.de.ecertby.validator;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.service.UserGroupService;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lgl.bayern.de.ecertby.config.AppConstants.Operations;
import lgl.bayern.de.ecertby.config.MessageConfig;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.service.SecurityService;
import lgl.bayern.de.ecertby.service.UserOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Validates the user rights
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class UserRightsValidator {

    private final SecurityService securityService;

    private final UserOperationService userOperationService;

    private final UserGroupService userGroupService;

    private static final String LOG_IDENTIFIER = "UserRightsValidator";

    private static final String NEW_USER_ID = "NEW_USER";

    private boolean isNewUser;

    private UserDetailDTO targetUser;

    private UserDetailDTO actingUser;

    private String actingUserType;

    private List<EcertBYErrorException> errors;

    private final static String CANNOT_ACCESS_USER = "cannot_access_user";

    /**
     * Validates if the acting user is entitled businesswise to create / modify / view another user.
     * @param targetUser - The target user.
     */
    public void checkIfActingUserIsEntitledToEditUser(@NotNull UserDetailDTO targetUser){
        this.targetUser = targetUser;
        checkIfActingUserIsEntitled();
    }

    /**
     * Validates if the acting user is entitled businesswise to create / modify / view another user.
     * @param targetUser - The target user.
     * @param errors - A list containing errors
     */
    public void checkIfActingUserIsEntitledToEditUser(@NotNull UserDetailDTO targetUser, @NotNull List<EcertBYErrorException> errors){
        this.targetUser = targetUser;
        this.errors = errors;
        checkIfActingUserIsEntitled();
    }

    /**
     * Checks if the authority or the company of the user are null.
     * In that case, it most probably means that the given user ID does not belong to the required
     * company / authority selected in the drop-down un the UI.
     * @param authorityCompany - The object to check if is null.
     */
    public void checkUserAuthorityOrCompanyIfNull(Object authorityCompany, String id,
        String selectionFromDD, boolean isCompany) {

        if (isNull(authorityCompany)) {
            String errorMessage = MessageConfig.getValue(isCompany ? "user_not_in_company" : "user_not_in_authority").formatted(id, selectionFromDD);
            logError("User with ID: %s does not belong to %s with ID: %s."
                .formatted(id, (isCompany ? "company" : "authority"), selectionFromDD), errorMessage);
        }
    }

    // PRIVATE HELPER METHODS //
    private void checkIfActingUserIsEntitled(){

        // initialize acting user.
        this.actingUser = securityService.getLoggedInUserDetailDTO();
        this.actingUserType = actingUser.getUserType().getId();

        // Admins can modify / view / create all types of users.
        if (UserType.ADMIN_USER.name().equals(actingUserType)) {
            return;
        }

        checkIfUserCanUpdateOtherUser();
    }

    private void checkIfUserCanUpdateOtherUser() {
        // initialize target user
        final String USER_ID = !isNull(targetUser.getId()) ? targetUser.getId() : NEW_USER_ID;
        this.isNewUser = USER_ID.equals(NEW_USER_ID);
        final String targetUserType = targetUser.getUserType().getId();

        // Check rights for Company and Authority users.
        if (UserType.COMPANY_USER.name().equals(targetUserType) || UserType.AUTHORITY_USER.name().equals(targetUserType)) {

            if (UserType.AUTHORITY_USER.name().equals(actingUserType) && UserType.COMPANY_USER.name().equals(targetUserType)) {
                // At this point the acting user is an Authority user that tries to create / view a Company user.
                checkIfAuthorityUserCanAccessCompanyUser(userGroupService.getGroupByID(targetUser.getRole().getId(), false)
                    .getName());

            } else if (UserType.COMPANY_USER.name().equals(actingUserType) && UserType.AUTHORITY_USER.name().equals(targetUserType)) {
                // Company users are now allowed to view / create / update Authority users.
                String logMessage = "Company user with ID: %s is not allowed to access: %s. User was not created."
                        .formatted(actingUser.getId(), actingUserType);
                String errorMessage = CANNOT_ACCESS_USER;
                logError(logMessage, errorMessage);
            }
        } else {
            // In case of an admin, only admins are able to modify them.
            String logMessage = "User with ID: %s does not have rights to edit user with ID: %s."
                .formatted(actingUser.getId(), USER_ID);
            String errorMessage = CANNOT_ACCESS_USER;
            logError(logMessage, errorMessage);
        }
    }

    private void checkIfAuthorityUserCanAccessCompanyUser(String targetUserRole) {

        // Authority users are not allowed to modify or view existing company users.
        if (!isNewUser) {
            String logMessage = "Acting user: %s does not have the required role to access user: %s."
                 .formatted(actingUser.getId(), targetUser.getId());
            String errorMessage = CANNOT_ACCESS_USER;
            logError(logMessage, errorMessage);
            return;
        }

        // Authority main users should be able to create Company users if they create a new company belonging to the same authority.
        // For this case, we check the acting user if they have this right on their own authority.
        if (isRequiredRoleMissing(actingUser, Operations.NEW_COMPANY)) {
            String logMessage = "Acting user: %s does not have the required role to create new company users."
                    .formatted(actingUser.getId());
            String errorMessage = CANNOT_ACCESS_USER;
            logError(logMessage, errorMessage);

        } else {
            // Acting user has the appropriate authorities, check if the user created is a company main user
            if (!UserRole.COMPANY_MAIN_USER.name().equals(targetUserRole)) {
                String logMessage = "Acting user: %s can only create %s. User was not created"
                    .formatted(actingUser.getId(), UserRole.COMPANY_MAIN_USER.name());
                String errorMessage = CANNOT_ACCESS_USER;
                logError(logMessage, errorMessage);
            }
        }
    }

    /**
     * Checks if the acting user has the given role.
     * @param user - The acting user
     * @param requiredRole - The required role
     * @return - True if the role is missing, false otherwise.
     */
    private boolean isRequiredRoleMissing(UserDetailDTO user, String requiredRole) {
        String resourceId = targetUser.getResourceId();
        return (resourceId == null) || !userOperationService.getUserOperationsPerResource(user.getUser().getId())
            .get(resourceId).contains(requiredRole);
    }

    private void logError(String logMessage, String errorMessage) {
        EcertBYErrorException error = new EcertBYErrorException(errorMessage, errorMessage, "", LOG_IDENTIFIER, null, true);
        log.warn("%s: %s - %s".formatted(LOG_PREFIX, LOG_IDENTIFIER, logMessage));
        if (!isNull(errors)) {
            errors.add(error);
        }else {
            throw new QCouldNotSaveException("User validation errors", new EcertBYGeneralException(List.of(error)));
        }
    }
}

