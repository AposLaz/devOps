package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.AttributeDTO;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Attribute;
import lgl.bayern.de.ecertby.service.AttributeService;
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

import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Slf4j
@Validated
@Transactional
@RestController
@RequestMapping("attribute")
@RequiredArgsConstructor
public class AttributeResource {

    private final AttributeService attributeService;


    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch attribute.")
    @OperationAccess(
            operations = {VIEW_ATTRIBUTE}
    )
    public AttributeDTO get(@PathVariable String id) {
        return attributeService.findById(id);
    }


    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve attribute list.")
    @OperationAccess(operations = {VIEW_ATTRIBUTE_LIST})
    public Page<AttributeDTO> findAll(@QuerydslPredicate(root = Attribute.class) Predicate predicate,
                                      Pageable pageable) {
        return attributeService.findAll(predicate, pageable);
    }


    @EmptyPredicateCheck
    @GetMapping(path="getAllList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve attribute list.")
    @OperationAccess(operations = {VIEW_TEMPLATE, EDIT_TEMPLATE})
    public List<OptionDTO> getAllList() {
        return attributeService.getAllAttributeOptionDTO();
    }

    @PostMapping(path = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { NEW_ATTRIBUTE }
    )
    public ResponseEntity create(@Valid @RequestBody AttributeDTO attributeDTO) {
        return ResponseEntity.ok(this.attributeService.saveAttribute(attributeDTO));
    }


    @PostMapping(path = "update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { EDIT_ATTRIBUTE }
    )
    public ResponseEntity update(@Valid @RequestBody AttributeDTO attributeDTO) {
        return ResponseEntity.ok(this.attributeService.saveAttribute(attributeDTO));
    }


    @DeleteMapping(path = "{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete attribute.")
    @OperationAccess(
            operations = { DELETE_ATTRIBUTE }
    )
    public void delete(@PathVariable String id) {
        attributeService.deleteAttribute(id);
    }


    @GetMapping(path = "findAttributeRadioOptions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch radio options.")
    @OperationAccess(operations = {VIEW_ATTRIBUTE, NEW_ATTRIBUTE, EDIT_ATTRIBUTE})
    public List<OptionDTO> getAttributeRadioOptionList() {
        return attributeService.getAttributeRadioOptionList();
    }

}
