package lgl.bayern.de.ecertby.mapper;
import lgl.bayern.de.ecertby.dto.ObjectLockDTO;
import lgl.bayern.de.ecertby.model.ObjectLock;
import lgl.bayern.de.ecertby.model.QObjectLock;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class ObjectLockMapper extends BaseMapper<ObjectLockDTO, ObjectLock, QObjectLock> {
}
