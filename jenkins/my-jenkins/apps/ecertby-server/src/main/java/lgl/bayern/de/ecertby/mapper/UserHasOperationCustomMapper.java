package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.model.UserHasOperation;
import java.util.List;
import lgl.bayern.de.ecertby.dto.UserHasOperationCustomDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserHasOperationCustomMapper {

    @Mapping(source = "operation.name", target = "operationName")
    @Mapping(source = "resource.objectId", target = "resourceId")
    public abstract UserHasOperationCustomDTO toCustomDTO(UserHasOperation userHasOperation);

    public abstract List<UserHasOperationCustomDTO> toCustomDTOList(List<UserHasOperation> userHasOperation);
}
