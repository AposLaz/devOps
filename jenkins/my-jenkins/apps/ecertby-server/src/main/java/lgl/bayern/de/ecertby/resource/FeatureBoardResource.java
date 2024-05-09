package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.fd.util.Reaction;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.FeatureBoard;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.service.FeatureBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@RestController
@RequestMapping("featureboard")
@RequiredArgsConstructor
@Transactional
public class FeatureBoardResource {

    private final FeatureBoardService featureBoardService;
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve feature board thread list.")
    @ResourceAccess(
            operations = { VIEW_FEATURE_BOARD_LIST }
    )
    public Page<FeatureBoardThreadDTO> findAll(@QuerydslPredicate(root = UserDetail.class) Predicate userDetailPredicate,
                                               @QuerydslPredicate(root = FeatureBoard.class) Predicate threadPredicate,
                                               @RequestParam(value = "dateFrom", required = false) Instant dateFrom,
                                               @RequestParam(value = "dateTo", required = false) Instant dateTo,
                                               @RequestParam(value = "titleFilter", required = false) String titleFilter,
                                               @RequestParam(value = "firstNameFilter", required = false) String firstNameFilter,
                                               @RequestParam(value = "lastNameFilter", required = false) String lastNameFilter,
                                               @RequestParam(value = "emailFilter", required = false) String emailFilter,
                                               @RequestParam(value = "authorityFilter", required = false) String authority,
                                               @RequestParam(value ="anonymous" , required = false ) String anonymous,
                                               @RequestParam(value ="userViewOptions" , required = false ) String userViewOptions,
                                               @RequestParam(value = "companyFilter", required = false) String company, Pageable pageable,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
      return featureBoardService.getAll(userDetailPredicate,threadPredicate,dateFrom,dateTo,titleFilter,firstNameFilter,lastNameFilter,emailFilter, authority,company,pageable,anonymous,userViewOptions,selectionFromDD);
    }

    @PostMapping(path = "create" , produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could save feature board thread.")
    @ResourceAccess(
            operations = { CREATE_THREAD }
    )
    public void save(@Valid @RequestBody FeatureBoardThreadDTO threadMessageDTO ) {
        featureBoardService.saveThread(threadMessageDTO);
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch thread.")
    @ResourceAccess(
            operations = { VIEW_THREAD }
    )
    public ResponseEntity<FeatureBoardThreadDTO> get(@PathVariable String id, @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return featureBoardService.findById(id);
    }


    @PatchMapping(path = "{id}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not publish thread.")
    @ResourceAccess(
            operations = { PUBLISH_THREAD }
    )
    public void publish(@PathVariable String id , @Valid @RequestBody ViewThreadPermissionsDTO viewThreadPermissionsDTO,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        featureBoardService.publish(id , viewThreadPermissionsDTO);
    }

    @PatchMapping(path = "{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not reject Thread.")
    @ResourceAccess(
            operations = { REJECT_THREAD }
    )
    public void reject(@PathVariable String id , @RequestBody String reason,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        featureBoardService.reject(id,reason);
    }
    @GetMapping(path = "{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch comments.")
    @ResourceAccess(
            operations = { COMMENT }
    )
    public List<FeatureBoardThreadDTO> findComments(@PathVariable String id,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return featureBoardService.getComments(id);
    }


    @PostMapping(path = "{id}/updateComment", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not update comment.")
    @ResourceAccess(
            operations = { COMMENT }
    )
    public void updateComment(@PathVariable String id,@RequestBody String commentText,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        featureBoardService.editComment(id,commentText);
    }
    @PostMapping(path = "{id}/addComment", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not add comment.")
    @ResourceAccess(
            operations = { COMMENT }
    )
    public FeatureBoardThreadDTO addComment(@PathVariable String id , @RequestBody String commentText,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return featureBoardService.saveComment(id,commentText);
    }
    @PatchMapping(path = "{id}/upvote", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not upvote thread.")
    @ResourceAccess(
            operations = { VOTE }
    )
    public void upvote(@PathVariable String id,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        featureBoardService.vote(id, Reaction.LIKE);
    }

    @PatchMapping(path = "{id}/downvote", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not downvote thread.")
    @ResourceAccess(
            operations = { VOTE }
    )
    public void downvote(@PathVariable String id,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        featureBoardService.vote(id,Reaction.DISLIKE);
    }

}
