package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.HtmlElementDTO;
import lgl.bayern.de.ecertby.service.HtmlElementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@Transactional
@RestController
@RequestMapping("htmlElement")
@RequiredArgsConstructor
public class HtmlElementResource {

  private final HtmlElementService htmlElementService;

  /**
   * Get template.
   *
   * @param id The id of template.
   * @return The template with the given id.
   */
  @GetMapping(path = "getByTemplateId/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch html elements.")
//todo
  public Page<HtmlElementDTO> get(@PathVariable String id) {
    return htmlElementService.findByTemplateId(id);
  }

  @PostMapping(path = "save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  //todo
  public ResponseEntity save(@Valid @RequestBody List<HtmlElementDTO> htmlElementDTOList, @RequestParam(value = "templateId", required = true) String templateId,
                             @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
    return ResponseEntity.ok(htmlElementService.saveHtmlElements(htmlElementDTOList, templateId, selectionFromDD));
  }

}
