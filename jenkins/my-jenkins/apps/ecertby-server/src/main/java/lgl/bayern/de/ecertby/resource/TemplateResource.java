package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.model.Template;
import lgl.bayern.de.ecertby.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Slf4j
@Validated
@Transactional
@RestController
@RequestMapping("template")
@RequiredArgsConstructor
public class TemplateResource {

  private final TemplateService templateService;

  /**
   * Get template.
   *
   * @param id The id of template.
   * @return The template with the given id.
   */
  @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch template.")
  @OperationAccess(
          operations = { VIEW_TEMPLATE }
  )
  public TemplateDTO get(@PathVariable String id) {
    return templateService.findById(id);
  }

  /**
   * Get template.
   *
   * @param id The id of template.
   * @return The template with the given id.
   *
   * This function is declared twice because of the resourceaccess/operationaccess restrictions
   */
  @GetMapping(path = "getTemplateById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch template.")
  @ResourceAccess(
          operations = { NEW_CERTIFICATE }
  )
  public TemplateDTO getTemplateById(@PathVariable String id, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return templateService.findById(id);
  }

  /**
   * Find all templates with some criteria.
   *
   * @param predicate The criteria given.
   * @param pageable  The selected page and sorting.
   * @return The requested templates paged and sorted.
   */
  @EmptyPredicateCheck
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
  @OperationAccess(
          operations = { VIEW_TEMPLATE_LIST }
  )
  public Page<TemplateDTO> findAll(@QuerydslPredicate(root = Template.class) Predicate predicate,
                                     Pageable pageable) {
    return templateService.findAll(predicate, pageable);
  }

  /**
   * Save the template.
   *
   * @param templateDTO The object with the information with the template.
   * @return The saved template.
   */
  @PostMapping(path = "create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @OperationAccess(
          operations = { NEW_TEMPLATE }
  )
  public ResponseEntity create(@RequestPart("templateDTO") TemplateDTO templateDTO,
                               MultipartHttpServletRequest request) {
    return ResponseEntity.ok(templateService.saveTemplate(templateDTO, request));
  }

  /**
   * Save the template.
   *
   * @param templateDTO The object with the information with the template.
   * @return The saved template.
   */
  @PostMapping(path = "update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @OperationAccess(
          operations = { EDIT_TEMPLATE }
  )
  public ResponseEntity update(@RequestPart("templateDTO") TemplateDTO templateDTO,
                               MultipartHttpServletRequest request) {
    return ResponseEntity.ok(templateService.saveTemplate(templateDTO, request));
  }

  /**
   * Activate template.
   * @param id The id of the template to activate.
   * @param isActive Defines if template will activate or deactivate.
   * @return True if flow completed successfully. False in any other case.
   */
  @PatchMapping(path = "{id}/activate/{isActive}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not activate or deactivate template.")
  @OperationAccess(
          operations = { ACTIVATE_TEMPLATE }
  )
  public boolean activate(@PathVariable String id, @PathVariable boolean isActive) {
    return templateService.activateTemplate(isActive, id);
  }

  /**
   * Release template.
   * @param id The id of the template to release.
   * @return True if flow completed successfully. False in any other case.
   */
  @PatchMapping(path = "{id}/release",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not release template.")
  @OperationAccess(
          operations = { RELEASE_TEMPLATE }
  )
  public boolean release(@PathVariable String id) {
    return templateService.releaseTemplate(id);
  }

  /**
   * Get the active and released templates associated with a country and a product, as long as they are valid or will be valid in the future.
   * @param targetCountryId The id of the country.
   * @param productId The id of the product.
   * @return The respective templates as a page of TemplateDTOs.
   */
  @GetMapping(path = "country/{targetCountryId}/product/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch products.")
  @ResourceAccess(
          operations = { NEW_CERTIFICATE }
  )
  public Page<TemplateDTO> getTemplateByTargetCountryIdAndProductId(@PathVariable String targetCountryId,
                                                                    @PathVariable String productId,
                                                                    @QuerydslPredicate(root = Template.class) Predicate predicate,
                                                                    Pageable pageable,
                                                                    @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return templateService.getTemplateByTargetCountryIdAndProductId(targetCountryId, productId, predicate, pageable);
  }

  /**
   * Get the keywords associated with a template.
   * @param templateId The id of the template.
   * @return The keywords as a list of OptionDTOs.
   */
  @GetMapping(path = "keywords/{templateId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch keywords.")
  @ResourceAccess(
          operations = { NEW_CERTIFICATE }
  )
  public List<OptionDTO> getTemplateKeywordsById(@PathVariable String templateId,
                                                 @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return templateService.getTemplateKeywordsById(templateId);
  }

  /**
   * Get the comment of a template.
   * @param templateId The id of the template.
   * @return The comment as a string.
   */
  @GetMapping(path = "comment/{templateId}", produces = MediaType.TEXT_PLAIN_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch comment.")
  @ResourceAccess(
          operations = { NEW_CERTIFICATE }
  )
  public String getTemplateCommentById(@PathVariable String templateId,
                                       @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return templateService.getTemplateCommentById(templateId);
  }

  @GetMapping(path = "getTemplateElementsAndValues", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch template elements.")
  @OperationAccess(
          operations = { VIEW_TEMPLATE, EDIT_TEMPLATE}
  )
  public Map<String, List> getTemplateElementsAndValuesByTemplateId(@RequestParam(value = "templateId", required = true) String templateId) {
    return templateService.getTemplateElementsAndValuesByTemplateId(templateId);
  }

}
