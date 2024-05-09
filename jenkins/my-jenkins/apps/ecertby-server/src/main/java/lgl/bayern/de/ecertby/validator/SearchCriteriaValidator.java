package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.SearchCriteria;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class SearchCriteriaValidator {

    private static final String ERROR_SEARCH_CRITERIA_WRONG_ADMIN_SELECTION = "error_search_criteria_wrong_admin_selection";

    public void validateSearchCriteria(SearchCriteria searchCriteria, UserDetail currentUser, String selectionFromDD) {
        List<EcertBYErrorException> errors = new ArrayList<>();

        if(currentUser.getUserType().equals(UserType.ADMIN_USER)) {
            validateAdminVisibility(searchCriteria, errors);
        }

        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for Saving Search Criteria", new EcertBYGeneralException(errors));
        }
    }


    private void validateAdminVisibility(SearchCriteria searchCriteria, List<EcertBYErrorException> errors) {
        boolean hasAdminView =  searchCriteria.getSearchCriteriaGroupSet().stream().anyMatch( set -> set.getGroupType().equals(GroupType.ADMIN));
        boolean hasUserView =  searchCriteria.getSearchCriteriaGroupSet().stream().anyMatch( set -> set.getGroupType().equals(GroupType.PERSONAL));
        if(hasAdminView && hasUserView) {
            errors.add(new EcertBYErrorException(ERROR_SEARCH_CRITERIA_WRONG_ADMIN_SELECTION, ERROR_SEARCH_CRITERIA_WRONG_ADMIN_SELECTION, null ,null, null, true));
        }
    }

}
