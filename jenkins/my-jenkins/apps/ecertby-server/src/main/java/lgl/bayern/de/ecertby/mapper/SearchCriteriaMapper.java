package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.model.QSearchCriteria;
import lgl.bayern.de.ecertby.model.SearchCriteria;
import lgl.bayern.de.ecertby.model.SearchCriteriaGroup;
import lgl.bayern.de.ecertby.model.util.GroupType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class SearchCriteriaMapper extends BaseMapper<SearchCriteriaDTO, SearchCriteria, QSearchCriteria> {

    @Mapping(target = "searchCriteriaGroupSet", source = "searchCriteriaGroupDTOSet")
    @Mapping(target = "createdByGroupType", source = "createdByGroupType")
    public abstract SearchCriteria mapToEntity(SearchCriteriaDTO dto);

    @Mapping(target = "searchCriteriaGroupDTOSet", source = "searchCriteriaGroupSet")
    public abstract SearchCriteriaDTO mapToDto(SearchCriteria entity);

    protected SearchCriteriaGroup mapGroupTypeToSearchCriteriaGroup(GroupType groupType) {
        if (groupType == null) {
            return null;
        }
        SearchCriteriaGroup searchCriteriaGroup = new SearchCriteriaGroup();
        searchCriteriaGroup.setGroupType(groupType);
        return searchCriteriaGroup;
    }

    protected GroupType mapSearchCriteriaGroupToGroupType(SearchCriteriaGroup searchCriteriaGroup) {
        if (searchCriteriaGroup == null) {
            return null;
        }
        return searchCriteriaGroup.getGroupType();
    }

    protected Set<SearchCriteriaGroup> mapGroupTypeSetToSearchCriteriaGroupSet(Set<GroupType> groupTypeSet) {
        if (groupTypeSet == null) {
            return null;
        }
        return groupTypeSet.stream()
                .map(this::mapGroupTypeToSearchCriteriaGroup)
                .collect(Collectors.toSet());
    }

    protected Set<GroupType> mapSearchCriteriaGroupSetToGroupTypeSet(Set<SearchCriteriaGroup> searchCriteriaGroupSet) {
        if (searchCriteriaGroupSet == null) {
            return null;
        }
        return searchCriteriaGroupSet.stream()
                .map(this::mapSearchCriteriaGroupToGroupType)
                .collect(Collectors.toSet());
    }
}
