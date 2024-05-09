package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.fuse.fd.dto.ThreadMessageDTO;
import com.eurodyn.qlack.fuse.fd.util.ThreadStatus;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class FeatureBoardValidator {

    public boolean validateGetRequest(ThreadMessageDTO thread, UserDetail currentUser) {
        return isThreadVisibleToUser(thread, currentUser);
    }

    private boolean isThreadVisibleToUser(ThreadMessageDTO thread, UserDetail currentUser) {
        if (currentUser.getId().equals(thread.getAuthor()) || UserType.ADMIN_USER.equals(currentUser.getUserType())) {
            return true;
        }

        if (ThreadStatus.PUBLISHED.equals(thread.getStatus())) {
            return isThreadPublishedToUserType(currentUser, thread);
        }

        return false;
    }

    private boolean isThreadPublishedToUserType(UserDetail currentUser, ThreadMessageDTO thread) {
        if (currentUser.getUserType().equals(UserType.AUTHORITY_USER)) {
            return isPublishedToAuthorities(thread);
        } else if (currentUser.getUserType().equals(UserType.COMPANY_USER)) {
            return isPublishedToCompanies(thread);
        } else {
            return true;
        }
    }

    private boolean isPublishedToAuthorities(ThreadMessageDTO thread) {
        return ViewThreadVisibility.VISIBLE_TO_ALL.getValue().equals(thread.getAttributesMask()) ||
                ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.getValue().equals(thread.getAttributesMask());
    }

    private boolean isPublishedToCompanies(ThreadMessageDTO thread) {
        return ViewThreadVisibility.VISIBLE_TO_ALL.getValue().equals(thread.getAttributesMask()) ||
                ViewThreadVisibility.VISIBLE_TO_COMPANIES.getValue().equals(thread.getAttributesMask());
    }
}
