package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.service.SecurityService;
import lgl.bayern.de.ecertby.service.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("security")
@RequiredArgsConstructor
@Transactional
public class SecurityResource {

    private final SecurityService securityService;

    private final SecurityUtils securityUtils;

    /**
     * Gets the permitted operations for the user with the given user id.
     * @return The list of all permitted operations for the user.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch operations for the user id.")
    public ResponseEntity getPermittedOperationsForUser() {
        return ResponseEntity.ok(securityService.getPermittedOperationsForUser());
    }


    /**
     * Get the user detail id of logged-in user.
     * @return user detail id.
     */
    @GetMapping(path="user-detail-id",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch logged in user detail id.")
    public ResponseEntity getLoggedInUserDetailId() {
        return ResponseEntity.ok(securityService.getLoggedInUserDetailId());
    }

    @GetMapping(path="user-detail",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch logged in user detail.")
    public UserDetailDTO getLoggedInUserDetail() {
        return securityService.getLoggedInUserDetailDTO();
    }

    @PostMapping(path = "/refresh-token", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void refreshtoken(@RequestBody String refreshToken, HttpServletRequest request, HttpServletResponse response) throws Exception {

        try {
            securityUtils.refreshToken(refreshToken, request.getHeader(AppConstants.AUTHORIZATION), response);
        } catch (Exception e) {
            //Refresh token operation cannot be completed. Either the token is invalid or the user has reached max refresh times
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("The user with username {} has an invalid refresh token.", securityService.getLoggedInUserUsername());
            throw new QExceptionWrapper(e.getMessage());
        }
    }

    @GetMapping(path="checkIfActive",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not check if user is active.")
    public boolean checkIfActive(HttpServletRequest request, HttpServletResponse response) {
        log.debug("initial check if active from ui");
        String jwt = request.getHeader(AppConstants.AUTHORIZATION);
        return securityUtils.checkIfUserIsActiveAndUpdateJwt(response,securityService.getLoggedInUserUsername(),jwt);
    }

}
