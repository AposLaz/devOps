package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.ObjectLock;
import lgl.bayern.de.ecertby.model.QObjectLock;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectLockRepository extends BaseRepository<ObjectLock, QObjectLock> {

    ObjectLock findByObjectTypeAndObjectId(ObjectType objectType, String objectId);

    ObjectLock findByObjectId(String objectId);

    List<ObjectLock> findByUserDetail(UserDetail userDetail);
}
