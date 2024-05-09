package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserServiceMapper {
    @Mapping(target = "status", constant ="1")
    @Mapping(target = "superadmin", constant  = "false")
    public abstract UserDTO map(UserDTO userDTO);
}
