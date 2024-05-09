package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE;
import static lgl.bayern.de.ecertby.config.AppConstants.Resource.ADMIN_RESOURCE_ID;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupHasOperationDTO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.UserHasOperationCustomDTO;
import lgl.bayern.de.ecertby.mapper.UserHasOperationCustomMapper;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.UserHasOperationCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserOperationService {

    private final com.eurodyn.qlack.fuse.aaa.service.OperationService qlackOperationService;
    private final com.eurodyn.qlack.fuse.aaa.service.UserGroupService qlackUserGroupService;
    private final com.eurodyn.qlack.fuse.aaa.service.ResourceService qlackResourceService;

    private final UserHasOperationCustomMapper userHasOperationCustomMapper;

    private final UserHasOperationCustomRepository userHasOperationCustomRepository;


    /**
     * Removes existing roles from a user for the same resource (company, authority, system)
     * @param userDetailDTO - The userDetailDTO.
     */
    public void removeExistingOperationsFromUser(UserDetailDTO userDetailDTO) {
        String userID = userDetailDTO.getUser().getId();
        String objectId = findUserObjectId(userDetailDTO);
        removeExistingOperationsFromUser(userID, objectId);
    }

    private void removeExistingOperationsFromUser(String userId, String objectId, String resourceId) {
        Set<String> existingOperations = qlackOperationService.getPermittedOperationsForUser(userId, objectId, false);

        for (String operationName : existingOperations) {
            qlackOperationService.removeOperationFromUser(userId, operationName, resourceId);
        }
    }

    public void removeExistingOperationsFromUserGroup(String userId) {
        Set<String> existingOperations = qlackOperationService.getPermittedOperationsForUser(userId, false);

        for (String operationName : existingOperations) {
            qlackOperationService.removeOperationFromUser(userId, operationName);
        }
    }

    public void removeExistingOperationsFromUser(String userId, String objectId) {
        String resourceId = qlackResourceService.getResourceByObjectId(objectId).getId();
        removeExistingOperationsFromUser(userId, objectId, resourceId);
    }

    public void removeAllOperationsFromUser(String userId) {
        Set<String> existingOperations = qlackOperationService.getPermittedOperationsForUser(userId, false);

        for (String operationName : existingOperations) {
            qlackOperationService.removeOperationFromUser(userId, operationName);
        }
    }

    /**
     * Assigns the appropriate operations to user.
     * Fetches the operations from the appropriate group depending on user's role.
     * @param userDetailDTO - The userDetailDTO.
     */
    public void assignOperationsToUser(String userId, UserDetailDTO userDetailDTO) {
        if (userDetailDTO.getRole() == null) {
            return;
        }

        String userRoleName = getUserRoleName(userDetailDTO);
        String resourceId = findUserNewResourceId(userDetailDTO);
        assignOperationsToUser(userId, userDetailDTO, userRoleName, resourceId);
    }

    public void assignOperationsToUserObjectId(String aaaUserId, String roleName, String objectId) {
        String resourceId = qlackResourceService.getResourceByObjectId(objectId).getId();
        List<UserGroupHasOperationDTO> operationDTOS = qlackOperationService.getGroupOperations(roleName);
        for (UserGroupHasOperationDTO operationDTO : operationDTOS) {
            qlackOperationService.addOperationToUser(aaaUserId, operationDTO.getOperationDTO().getName(), resourceId, false);
        }
    }

    public void assignOperationsToUser(String userId, UserDetailDTO userDetailDTO, String userRoleName, String resourceId) {
        if (userDetailDTO.getRole() == null) {
            return;
        }

        List<UserGroupHasOperationDTO> operationDTOS = qlackOperationService.getGroupOperations(userRoleName);

        for (UserGroupHasOperationDTO operationDTO : operationDTOS) {
            if (UserType.ADMIN_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                assignOperationsToAdmin(userId, operationDTO, resourceId);
            } else {
                qlackOperationService.addOperationToUser(userId, operationDTO.getOperationDTO().getName(), resourceId, false);
            }
        }
    }

    /**
     * Returns a map with operations split by resourceId.
     * In case of Admins, some operations will still be handled on OperationAccess level, therefore,
     * they don't have a resourceId. In that case, the empty IDs are filled with the ADMIN_RESOURCE
     * constant, to satisfy the needs of the front-end.
     * @param userId the user ID.
     * @return - the result map.
     */
    public Map<String, Set<String>> getUserOperationsPerResource(String userId) {
        return userHasOperationCustomMapper
            .toCustomDTOList(userHasOperationCustomRepository.findByUserId(userId)).stream()
            .map(op -> (isNull(op.getResourceId())) ? op.setResourceId(ADMIN_RESOURCE) : op.setResourceId(op.getResourceId()))
            .collect(Collectors.groupingBy(UserHasOperationCustomDTO::getResourceId,
                Collectors.mapping(UserHasOperationCustomDTO::getOperationName, Collectors.toSet())));
    }

    /**
     * Handles some rules regarding which admin actions are required to have a resourceId
     */
    private void assignOperationsToAdmin(String userId, UserGroupHasOperationDTO operationDTO, String resourceId) {
        if (AppConstants.Operations.EXCLUDE_OPS.stream().noneMatch(op -> operationDTO.getOperationDTO().getName().equals(op))) {
            qlackOperationService.addOperationToUser(userId, operationDTO.getOperationDTO().getName(), resourceId, false);
        } else {
            qlackOperationService.addOperationToUser(userId, operationDTO.getOperationDTO().getName(), false);
        }
    }

    /**
     * Fetches the user's role name from the DTO if available, from the DB otherwise.
     * @param userDetailDTO - The userDetailDTO.
     * @return - The roleName as String
     */
    private String getUserRoleName(UserDetailDTO userDetailDTO) {
        String userRoleName = userDetailDTO.getRole().getName();

        if (isNull(userRoleName)) {
            userRoleName = qlackUserGroupService.getGroupByID(userDetailDTO.getRole().getId(), false).getName();
        }

        return userRoleName;
    }

    /**
     * Fetches the ID of the appropriate user resource based on the user type.
     * If the user is System user (ADMIN_USER) it should return null.
     * @throws QCouldNotSaveException if the user has a different UserType from the expected.
     * @param userDetailDTO - The userDetailDTO.
     * @return - the resource ID.
     */
    private String findUserObjectId(UserDetailDTO userDetailDTO) {
        if (userDetailDTO.getSelectionFromDD() != null && !userDetailDTO.getSelectionFromDD().isEmpty()) {
            return userDetailDTO.getSelectionFromDD();
        } else {
            if (UserType.AUTHORITY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                return userDetailDTO.getPrimaryAuthority().getId();
            } else if (UserType.COMPANY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                return userDetailDTO.getPrimaryCompany().getId();
            } else if (UserType.ADMIN_USER.toString().equals(userDetailDTO.getUserType().getId())) {
                return ADMIN_RESOURCE;
            } else {
                throw new QCouldNotSaveException("AAA user did not created. Invalid user type.");
            }
        }
    }
    private String findUserNewResourceId(UserDetailDTO userDetailDTO) {
        if (userDetailDTO.getSelectionFromDD() != null && !userDetailDTO.getSelectionFromDD().isEmpty()) {
            return qlackResourceService.getResourceByObjectId(userDetailDTO.getSelectionFromDD()).getId();
        }
        return findUserExistingResourceId(userDetailDTO);
    }

    private String findUserExistingResourceId(UserDetailDTO userDetailDTO) {
        if (UserType.AUTHORITY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
            return qlackResourceService.getResourceByObjectId(userDetailDTO.getPrimaryAuthority().getId()).getId();
        } else if (UserType.COMPANY_USER.toString().equals(userDetailDTO.getUserType().getId())) {
            return qlackResourceService.getResourceByObjectId(userDetailDTO.getPrimaryCompany().getId()).getId();
        } else if (UserType.ADMIN_USER.toString().equals(userDetailDTO.getUserType().getId())) {
            return ADMIN_RESOURCE_ID;
        } else {
            throw new QCouldNotSaveException("AAA user did not created. Invalid user type.");
        }
    }

}
