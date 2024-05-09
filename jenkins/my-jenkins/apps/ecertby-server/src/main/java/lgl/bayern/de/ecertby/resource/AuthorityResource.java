package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.JwtUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@RestController
@RequestMapping("authority")
@RequiredArgsConstructor
@Transactional
public class AuthorityResource {
    private final AuthorityService authorityService;
    private final JwtUpdateService jwtUpdateService;
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve authorities list.")
    @OperationAccess(
            operations = { VIEW_AUTHORITIES_LIST }
    )
    public Page<AuthorityDTO> findAll(@QuerydslPredicate(root = Authority.class) Predicate predicate,
                                      Pageable pageable) {
        return authorityService.findAll(predicate, pageable);
    }

    @GetMapping(path = "findAll",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve authorities list.")
    @ResourceAccess(
            operations = { VIEW_FEATURE_BOARD_LIST, NEW_COMPANY, EDIT_COMPANY, VIEW_COMPANY, VIEW_COMPANIES_LIST,
                            NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST,
                            NEW_USER, EDIT_USER, VIEW_USER }
    )
    public List<OptionDTO> getAllAuthorities(@RequestParam(name = "active", required = false) Boolean active ,@RequestParam(name = "hasMainUser" , required = false) Boolean hasMainuser,
                                             @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        if(Boolean.TRUE.equals(hasMainuser)) return authorityService.getAllAuthoritiesWithMainUser();
        return authorityService.getAllAuthorities();
    }

    @GetMapping(path = "{certificateId}/findAllAuthoritiesForAuthorityForward/{isPreCert}",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve authorities list.")
    @ResourceAccess(
            operations = { AUTHORITY_FORWARD_CERTIFICATE }
    )
    public List<OptionDTO> getAllAuthsForAuthorityForward(@PathVariable String certificateId , @PathVariable boolean isPreCert,
                                                          @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return authorityService.getAllAuthoritiesForAuthorityForward(certificateId,selectionFromDD,isPreCert);
    }

    @PostMapping(path = "create" , consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { NEW_AUTHORITY }
    )
    public void create(@Valid @RequestBody AuthorityDTO authorityDTO) {
            authorityService.createUpdateAuthority(authorityDTO);
    }

    @PostMapping(path = "update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { EDIT_AUTHORITY }
    )
    public void update(@Valid @RequestBody AuthorityDTO authorityDTO) {
            authorityService.createUpdateAuthority(authorityDTO);
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch authority.")
    @ResourceAccess(
            operations = { VIEW_AUTHORITY }
    )
    public ResponseEntity<AuthorityDTO> get(@PathVariable String id,
                                // Required by QLack
                                @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return authorityService.validateAndReturnAuthorityById(id, selectionFromDD);
    }


    @PatchMapping(path = "{id}/activate/{isActive}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { ACTIVATE_AUTHORITY }
    )
    public void activate(@PathVariable String id, @PathVariable boolean isActive) {
       authorityService.activateAuthority(id,isActive);
    }

    @GetMapping(path = "{id}/hasMainUser", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch user.")
    @ResourceAccess(
            operations = { EDIT_AUTHORITY}
    )
    public boolean hasMainUser(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return authorityService.hasMainUser(id);
    }

    @GetMapping(path = "findAllUserAuthorities", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve user authority list.")
    @ResourceAccess(
            operations = { ACTIVATE_COMPANY, ACTIVATE_PROFILE, ACTIVATE_USER,
                    BLOCK_CERTIFICATE, COMMENT, CREATE_THREAD, DELETE_COMPANY, DELETE_PROFILE, DELETE_TEAM,
                    EDIT_AUTHORITY, EDIT_CERTIFICATE, EDIT_COMPANY, EDIT_PROFILE, EDIT_TEAM,
                    EDIT_USER, MARK_CERTIFICATE_AS_LOST, NEW_COMPANY, NEW_PROFILE, NEW_TEAM, NEW_USER, REJECT_CERTIFICATE,
                    RELEASE_CERTIFICATE, REVOKE_CERTIFICATE, VIEW_AUTHORITY, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST,
                    VIEW_COMPANIES_LIST, VIEW_COMPANY, VIEW_FEATURE_BOARD_LIST, VIEW_PROFILE, VIEW_TEAM,
                    VIEW_TEAMS_LIST, VIEW_THREAD, VIEW_USER, VIEW_USERS_LIST, VOTE}
    )
    public List<OptionDTO> getUserAuthorities(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return authorityService.getUserAuthorities();
    }
    @GetMapping(path = "isAuthorityActive/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch user.")
    @ResourceAccess(
            operations = { EDIT_AUTHORITY , VIEW_AUTHORITY}
    )
    public Map<String,Boolean> isAuthorityActive(@PathVariable String id, HttpServletRequest request, HttpServletResponse response, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return jwtUpdateService.checkIfAuthorityIsActiveAndUpdateJwt(id,request.getHeader(AppConstants.AUTHORIZATION),response);
    }
}
