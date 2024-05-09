package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;

import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.service.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@Transactional
@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserResource {

  private final UserDetailService userDetailService;

  /**
   * Get user.
   * @param id The id of detail user.
   * @return The user with the given id.
   */
  @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { VIEW_USER, EDIT_USER }
  )
  public UserDetailDTO get(@PathVariable String id, @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.getUser(id, false, selectionFromDD);
  }


  @GetMapping(path = "/findRole", produces = MediaType.APPLICATION_JSON_VALUE)
  public UserGroupDTO getRole(@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.findRoleFromSelection(selectionFromDD);
  }

  /**
   * Get logged in user's account.
   * @param id The id of detail user.
   * @return The user with the given id.
   */
  @GetMapping(path = "my-account/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public UserDetailDTO getMyAccount(@PathVariable String id) {
    return userDetailService.getUser(id, true, null);
  }

  /**
   * Find all users with some criteria.
   * @param predicate The criteria given.
   * @param pageable The selected page and sorting.
   * @return The requested users paged and sorted.
   */
  @EmptyPredicateCheck
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { VIEW_USERS_LIST }
  )
  public Page<UserDetailDTO> findAll(@QuerydslPredicate(root = UserDetail.class) Predicate predicate,
                                     Pageable pageable,
                                     @RequestParam(value = "user.userGroups.id", required = false) String role,
                                     @RequestParam(value = "roleInProcess", required = false) String roleInProcess,
                                     @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.findAll(predicate, pageable, selectionFromDD, role, roleInProcess);
  }

  /**
   * Save the user.
   * @param userDetailDTO The object with the information with the user.
   * @return The saved user.
   */
  @PostMapping(path = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { NEW_USER }
  )
  public ResponseEntity create(@Valid @RequestBody UserDetailDTO userDetailDTO) {
    return ResponseEntity.ok(userDetailService.saveUser(userDetailDTO));
  }


  /**
   * Link an existing user with company or authority.
   * @param userDetailDTO The object with the information with the user.
   * @return The saved user.
   */
  @PostMapping(path = "link", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { NEW_USER }
  )
  public ResponseEntity link(@Valid @RequestBody UserDetailDTO userDetailDTO) {
    return ResponseEntity.ok(userDetailService.linkUser(userDetailDTO));
  }

  /**
   * Save the user.
   * @param userDetailDTO The object with the information with the user.
   * @return The saved user.
   */
  @PostMapping(path = "update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { EDIT_USER }
  )
  public ResponseEntity update(@Valid @RequestBody UserDetailDTO userDetailDTO) {
    return ResponseEntity.ok(userDetailService.saveUser(userDetailDTO));
  }

  /**
   * Save my account.
   * @param userDetailDTO The object with the information with the user.
   * @return The updated account.
   */
  @PostMapping(path="my-account",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity saveMyAccount(@Valid @RequestBody UserDetailProfileDTO userDetailDTO, HttpServletResponse response, HttpServletRequest request) {
    return ResponseEntity.ok(userDetailService.saveMyAccount(userDetailDTO, response, request));
  }

  /**
   * Activate user.
   * @param id The id of the user to activate.
   * @param isActive Defines if user will activate or deactivate.
   * @return True if flow completed successfully. False in any other case.
   */
  @PatchMapping(path = "{id}/activate/{isActive}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { ACTIVATE_USER }
  )
  public boolean activate(@PathVariable String id, @PathVariable boolean isActive,
   // Required by Qlack
   @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.activateUser(isActive, id);
  }

  /**
   * Update user's password
   * @param resetPasswordDTO The object with the current and new password.
   * @return True if flow completed successfully. False in any other case.
   */
  @PostMapping(path="update-password",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity updatePassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO, HttpServletResponse response, HttpServletRequest request) {
    return ResponseEntity.ok(userDetailService.updatePassword(resetPasswordDTO, request));
  }

  /**
   * Get user's email notification setings.
   */
  @GetMapping(path="my-account/email-notifications", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { VIEW_EMAIL_NOTIFICATION }
  )
  public EmailNotificationDTO getEmailNotifications(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.getEmailNotificationSettings();
  }

  /**
   * Update user's email notification setings.
   * @param emailNotificationsDTO The object with the new email notification settings.
   */
  @PostMapping(path="update-email-notifications",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResourceAccess(
          operations = { VIEW_EMAIL_NOTIFICATION }
  )
  public void updateEmailNotifications(@Valid @RequestBody EmailNotificationDTO emailNotificationsDTO) {
    userDetailService.updateEmailNotificationSettings(emailNotificationsDTO);
  }

  @GetMapping(path = "getUsers",produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve user list.")
  @ResourceAccess(
          operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE, NEW_TEAM, EDIT_TEAM, VIEW_TEAM }
  )
  public List<OptionDTO> getUsers(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.getUsers(selectionFromDD);
  }

  @PostMapping(path = "findByEmail", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch user.")
  @ResourceAccess(
          operations = { NEW_COMPANY, EDIT_COMPANY, NEW_AUTHORITY, EDIT_AUTHORITY, NEW_USER, EDIT_USER }
  )
  public UserDetailDTO getUserByEmail(@Valid @RequestBody String email, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return userDetailService.findByEmail(email);
  }
}
