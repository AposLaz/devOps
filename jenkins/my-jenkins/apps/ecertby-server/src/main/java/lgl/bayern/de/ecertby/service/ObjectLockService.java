package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import java.util.List;
import lgl.bayern.de.ecertby.dto.ObjectLockDTO;
import lgl.bayern.de.ecertby.model.ObjectLock;
import lgl.bayern.de.ecertby.model.QObjectLock;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.repository.CompanyProfileRepository;
import lgl.bayern.de.ecertby.repository.ObjectLockRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class ObjectLockService extends BaseService<ObjectLockDTO, ObjectLock, QObjectLock> {

    @Autowired
    private ObjectLockRepository objectLockRepository;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private final CompanyProfileRepository companyProfileRepository;

    private final SecurityService securityService;


    public void unlock(String id) {
        String userId = securityService.getLoggedInUserDetailId();
        ObjectLock existingLock = objectLockRepository.findByObjectId(id);
        if (existingLock != null && existingLock.getUserDetail().getId().equals(userId)) {
            objectLockRepository.delete(existingLock);
        }
    }


    public void checkAndThrowIfLocked(String id , ObjectType objectType) {
        ObjectLock existingLock = getObjectLock(objectType.toString(), id);
        if (existingLock != null) {
            String myId = securityService.getLoggedInUserDetailId();
            String existingId = existingLock.getUserDetail().getId();
            if (!existingId.equals(myId)) {
                log.info(LOG_PREFIX + "The item is locked.");
                throw new QCouldNotSaveException(
                    String.format("This object is modified by another user. Requesting user: %s "
                        + "Existing user: %s, type: %s object ID: i%s",
                        myId, existingId, objectType, id));
            }
        }
    }

    public boolean checkAndLockIfNeeded(String id , String type, boolean shouldLock) {
        if (getActualObjectType(type) == null) {
            return false;
        }

        ObjectLock existingLock = getObjectLock(type, id);
        if (existingLock != null) {
            // The resource is already locked by someone else.
            return !existingLock.getUserDetail().getId().equals(securityService.getLoggedInUserDetailId());
        }
        // Lock the resource since it's not currently locked.
        if (shouldLock) {
            lock(id, type);
        }

        return false;
    }

    public void deleteAllUserLocks(String username) {
        UserDetail userDetail = userDetailRepository.findByUserUsername(username);
        if (userDetail != null) {
            List<ObjectLock> userLockList = objectLockRepository.findByUserDetail(userDetail);
            objectLockRepository.deleteAll(userLockList);
        }
    }

    public void deleteLoggedInUserLocks() {
        UserDetail loggedInUser = securityService.getLoggedInUserDetail();
        if (loggedInUser != null) {
            List<ObjectLock> userLockList = objectLockRepository.findByUserDetail(loggedInUser);
            objectLockRepository.deleteAll(userLockList);
        }
    }

    public void deleteAllLocks() {
            objectLockRepository.deleteAll();

    }

    public boolean overrideLock(String id , String type) {
        ObjectType actualObjectType = getActualObjectType(type);

        if (actualObjectType == null) {
            return true;
        }

        ObjectLock existingLock = getObjectLock(actualObjectType.toString(), id);

        // Deleting the existing lock if exists.
        if (existingLock != null) {
            objectLockRepository.delete(existingLock);
        }
        // Locks again with the new user ID.
        lock(id,type);
        return true;
    }

    // private helper methods
    private void lock(String id, String type) {
        ObjectType actualObjectType = getActualObjectType(type);
        if (actualObjectType == null) {
            // log
            return;
        }

        ObjectLock lock = new ObjectLock();
        lock.setObjectType(getRequiredObjectType(actualObjectType));
        lock.setObjectId(getRequiredObjectId(actualObjectType, id));
        UserDetail userDetail = securityService.getLoggedInUserDetail();
        if (userDetail != null) {
            lock.setUserDetail(userDetail);
            objectLockRepository.save(lock);
        }
    }

    /**
     * In case of COMPANY_PROFILE we should modify the parent company's lock instead.
     * e.g.1: If a user edits a company, all company profiles should be locked.
     * e.g.2: if a user edits a company profile, all profiles and the parent company should be locked.
     * @param actualType - The actual type of the object (if COMPANY_PROFILE, should be converted to COMPANY)
     * @param actualObjectId - The actual object id. If this ID belongs to a COMPANY_PROFILE, we should
     *                         retrieve the parent company's ID instead.
     * @return - The appropriate object lock based on the above-mentioned scenarios.
     */
    private ObjectLock getObjectLock(String actualType, String actualObjectId) {
        ObjectType requiredObjectType = getRequiredObjectType(getActualObjectType(actualType));
        String requiredObjectId = getRequiredObjectId(getActualObjectType(actualType), actualObjectId);

        return objectLockRepository.findByObjectTypeAndObjectId(requiredObjectType, requiredObjectId);
    }

    private String getRequiredObjectId(ObjectType actualObjectType, String objectId) {
        if (isCompanyProfileType(actualObjectType)) {
            return companyProfileRepository.findById(objectId).orElseThrow().getCompany().getId();
        }
        return objectId;
    }

    private ObjectType getRequiredObjectType(ObjectType actualObjectType) {
        if (isCompanyProfileType(actualObjectType)) {
            return ObjectType.COMPANY;
        }
        return actualObjectType;
    }

    private boolean isCompanyProfileType(ObjectType actualObjectType) {
        return actualObjectType != null && actualObjectType.equals(ObjectType.COMPANY_PROFILE);
    }

    private ObjectType getActualObjectType(String type) {
        try {
            return ObjectType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
