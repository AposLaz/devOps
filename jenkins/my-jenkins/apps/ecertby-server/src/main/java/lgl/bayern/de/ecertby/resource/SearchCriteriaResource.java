package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.model.SearchCriteria;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lgl.bayern.de.ecertby.service.SearchCriteriaService;
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
import java.util.Set;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;
@Slf4j
@Validated
@Transactional
@RestController
@RequestMapping("search-criteria")
@RequiredArgsConstructor
public class SearchCriteriaResource {

    private final SearchCriteriaService searchCriteriaService;

    @PostMapping(path = "create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = {NEW_SEARCH_CRITERIA}
    )
    public ResponseEntity create(@RequestBody @Valid SearchCriteriaDTO searchCriteriaDTO,  @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return ResponseEntity.ok(searchCriteriaService.saveSearchCriteria(searchCriteriaDTO, selectionFromDD));
    }

    @PostMapping(path = "update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = {EDIT_SEARCH_CRITERIA}
    )
    public ResponseEntity update(@RequestBody @Valid SearchCriteriaDTO searchCriteriaDTO, @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return ResponseEntity.ok(searchCriteriaService.saveSearchCriteria(searchCriteriaDTO, selectionFromDD));
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch search-criteria.")
    @OperationAccess(
            operations = {VIEW_SEARCH_CRITERIA}
    )
    public SearchCriteriaDTO get(@PathVariable String id) {
        return searchCriteriaService.getById(id);
    }

    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve  list.")
    @OperationAccess(
            operations = {VIEW_SEARCH_CRITERIA_LIST}
    )
    public Page<SearchCriteriaDTO> findAll(@QuerydslPredicate(root = SearchCriteria.class) Predicate predicate,
                                           @RequestParam(value = "searchCriteriaGroupDTOSet", required = false) Set<GroupType> groupTypeSet,
                                           Pageable pageable) {
        return searchCriteriaService.getAll(predicate, pageable, groupTypeSet);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete search-criteria.")
    @ResourceAccess(
            operations = {DELETE_SEARCH_CRITERIA}
    )
    public void delete(@PathVariable String id, @ResourceId String selectionFromDD) {
        searchCriteriaService.deleteSearchCriteria(id);
    }

    @GetMapping(path = "findByUser", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve search criteria list for user.")
    @ResourceAccess(
            operations = {VIEW_SEARCH_CRITERIA}
    )
    public List<SearchCriteriaDTO> findUserSearchCriteria(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return searchCriteriaService.findUserSearchCriteria(selectionFromDD);
    }

    @GetMapping(path = "{id}/markAsDefault", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not mark as default search-criteria.")
    @ResourceAccess(
            operations = {VIEW_SEARCH_CRITERIA}
    )
    public void makeDefault(@PathVariable String id, @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        searchCriteriaService.setDefaultSearchCriteria(id,selectionFromDD);
    }

    @GetMapping(path = "findDefaultCriteria",produces = MediaType.TEXT_PLAIN_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch search-criteria.")
    @ResourceAccess(
            operations = {VIEW_SEARCH_CRITERIA}
    )
    public ResponseEntity<String> findDefault(@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return ResponseEntity.ok(searchCriteriaService.findDefaultCriteriaForUser(selectionFromDD));
    }
}
