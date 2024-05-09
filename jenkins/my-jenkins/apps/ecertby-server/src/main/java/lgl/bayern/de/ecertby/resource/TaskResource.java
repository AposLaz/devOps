package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.*;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import lgl.bayern.de.ecertby.dto.TaskDTO;
import lgl.bayern.de.ecertby.model.Task;
import lgl.bayern.de.ecertby.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@Transactional
@RestController
@RequestMapping("task")
@RequiredArgsConstructor
public class TaskResource {
    private final TaskService taskService;

    /**
     * Find all tasks associated with a specific Company.
     *
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested audits paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(path="company", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve tasks list for company.")
    @ResourceAccess(
            operations = {VIEW_TASKS_LIST}
    )
    public Page<TaskDTO> findAllByCompany(@QuerydslPredicate(root = Task.class) Predicate predicate,
                                 Pageable pageable,
                                 @RequestParam(value = "selectionFromDD") @ResourceId String selectionFromDD) {
        return taskService.findAllByCompany(predicate, pageable, selectionFromDD);
    }

    /**
     * Find all tasks associated with a specific Authority.
     *
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested audits paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(path="authority", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve tasks list for authority.")
    @ResourceAccess(
            operations = {VIEW_TASKS_LIST}
    )
    public Page<TaskDTO> findAllByAuthority(@QuerydslPredicate(root = Task.class) Predicate predicate,
                                 Pageable pageable,
                                 @RequestParam(value = "selectionFromDD") @ResourceId String selectionFromDD) {
        return taskService.findAllByAuthority(predicate, pageable, selectionFromDD);
    }

    /**
     * Mark task as completed.
     * @param id The id of the task.
     */
    @PatchMapping(path = "{id}/complete",consumes = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not complete task.")
    @ResourceAccess(
            operations = { COMPLETE_TASK }
    )
    public void complete(@PathVariable String id,
                         @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        taskService.complete(id, selectionFromDD);
    }

    /**
     * Mark all tasks of a Company or Authority as completed.
     * @param selectionFromDD The id of the current Company or Authority .
     */
    @PatchMapping(path = "/completeAll",consumes = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not complete all tasks.")
    @ResourceAccess(
            operations = { COMPLETE_TASK }
    )
    public void completeAll(@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        taskService.completeAll(selectionFromDD);
    }
}
