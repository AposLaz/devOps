package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.cm.dto.VersionDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class FileMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "mimetype", target = "mimetype")
    @Mapping(source = "filename", target = "filename")
    public abstract DocumentDTO map(VersionDTO versionDTO);
}
