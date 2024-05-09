package lgl.bayern.de.ecertby.service;

import java.util.*;

import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class SecurityService {

    private final UserDetailRepository userDetailRepository;

    UserDetailMapper USERDETAIL_SERVICE_MAPPER = Mappers.getMapper(UserDetailMapper.class);

    private final com.eurodyn.qlack.fuse.aaa.service.UserGroupService userGroupService;

    private final UserOperationService userOperationService;

    /**
     * Gets the permitted operations for the user with the given user id.
     *
     * @return The list of all permitted operations for the user.
     */
    public Map<String, Set<String>> getPermittedOperationsForUser() {
        return userOperationService.getUserOperationsPerResource(getLoggedInUserId());
    }

    /**
     * Get the id of logged-in user.
     *
     * @return The id of the logged-in user.
     */
    public String getLoggedInUserId() {
        UserDetail userDetail = userDetailRepository.findByUserUsernameIgnoreCase(getLoggedInUserName());
        if (userDetail != null) {
            return userDetail.getUser().getId();
        }
        return null;
    }


    public String getLoggedInUserDetailId() {
        UserDetail userDetail = userDetailRepository.findByUserUsername(getLoggedInUserName());
        if (userDetail != null) {
            return userDetail.getId();
        }
        return null;
    }

    public UserDetailDTO getLoggedInUserDetailDTO() {
        UserDetailDTO userDetailDTO = USERDETAIL_SERVICE_MAPPER.map(userDetailRepository.findByUserUsername(getLoggedInUserName()));
        return userDetailDTO;
    }

    public UserDetail getLoggedInUserDetail(){
        return userDetailRepository.findByUserUsername(getLoggedInUserName());
    }

    /**
     * Get the username of logged-in user.
     *
     * @return username of logged-in user.
     */
    public String getLoggedInUserUsername() {
        UserDetail userDetail = userDetailRepository.findByUserUsername(getLoggedInUserName());
        if (userDetail != null) {
            return userDetail.getUser().getUsername();
        }
        return null;
    }

    String getLoggedInUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof DefaultSaml2AuthenticatedPrincipal) {
            return ((DefaultSaml2AuthenticatedPrincipal) principal).getName();
        }
        return null;
    }
}
