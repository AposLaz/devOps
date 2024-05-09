package lgl.bayern.de.ecertby.mapper;

import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TargetCountryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class OptionMapper {
    @Mapping(target = "id", source = "value")
    @Mapping(target = "name", source = "value")
    @Mapping(target = "active" , expression = "java(true)" )
    abstract OptionDTO mapToOptionDTO(String value);

    public abstract List<OptionDTO> mapToListOptionDTO(List<String> items);

    @Mapping(target="filterId", source="parentId")
    @Mapping(target = "active" , expression = "java(true)" )
    abstract OptionDTO userGroupDTOmapToOptionDTO(UserGroupDTO userGroupDTO);

    public abstract List<OptionDTO> userGroupDTOmapToListOptionDTO(List<UserGroupDTO> userGroupList);


    public abstract List<OptionDTO> targetCountryDTOmapToListOptionDTO(List<TargetCountryDTO> targetCountryList);
    
    @Mapping(target = "filterId", source = "isoCode")
    abstract OptionDTO targetCountryDTOmapToOptionDTO(TargetCountryDTO targetCountryDTO);
}
