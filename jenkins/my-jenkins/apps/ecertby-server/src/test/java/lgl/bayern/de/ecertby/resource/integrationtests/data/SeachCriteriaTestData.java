package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.model.util.GroupType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SeachCriteriaTestData extends BaseTestData {

        public List<SearchCriteriaDTO> initializeSearchCriteriaDTOByAdmin() {
                List<SearchCriteriaDTO> searchCriteriaDTOByAdminList = new ArrayList<>();
                searchCriteriaDTOByAdminList
                                .add(createSearchCriteriaDTO("searchCriteriaByAdminForAdmin", GroupType.ADMIN,
                                                GroupType.ADMIN));
                searchCriteriaDTOByAdminList
                                .add(createSearchCriteriaDTO("searchCriteriaByAdminForSelf", GroupType.ADMIN,
                                                GroupType.PERSONAL));
                searchCriteriaDTOByAdminList
                                .add(createSearchCriteriaDTO("searchCriteriaByAdminForAuths", GroupType.ADMIN,
                                                GroupType.AUTHORITY));
                searchCriteriaDTOByAdminList
                                .add(createSearchCriteriaDTO("searchCriteriaByAdminForComps", GroupType.ADMIN,
                                                GroupType.COMPANY));
                searchCriteriaDTOByAdminList
                                .add(createSearchCriteriaDTO("searchCriteriaByAdminForAll", GroupType.ADMIN,
                                                GroupType.GLOBAL));
                return searchCriteriaDTOByAdminList;
        }

        public List<SearchCriteriaDTO> initializeSearchCriteriaDTOByAuthority() {
                List<SearchCriteriaDTO> searchCriteriaDTOByAuthList = new ArrayList<>();
                searchCriteriaDTOByAuthList
                                .add(createSearchCriteriaDTO("searchCriteriaByAuthForAuth", GroupType.AUTHORITY,
                                                GroupType.AUTHORITY));
                searchCriteriaDTOByAuthList
                                .add(createSearchCriteriaDTO("searchCriteriaByAuthForSelf", GroupType.AUTHORITY,
                                                GroupType.PERSONAL));
                return searchCriteriaDTOByAuthList;
        }

        public List<SearchCriteriaDTO> initializeSearchCriteriaDTOByCompany() {
                List<SearchCriteriaDTO> searchCriteriaDTOByCompList = new ArrayList<>();
                searchCriteriaDTOByCompList
                                .add(createSearchCriteriaDTO("searchCriteriaByCompForComp", GroupType.COMPANY,
                                                GroupType.COMPANY));
                searchCriteriaDTOByCompList
                                .add(createSearchCriteriaDTO("searchCriteriaByCompForSelf", GroupType.COMPANY,
                                                GroupType.PERSONAL));
                return searchCriteriaDTOByCompList;
        }

        private SearchCriteriaDTO createSearchCriteriaDTO(String name, GroupType createdByGroupType,
                        GroupType... searchCriteriaGroupTypes) {
                SearchCriteriaDTO searchCriteriaDTO = new SearchCriteriaDTO();
                searchCriteriaDTO.setName(name);
                searchCriteriaDTO.setCriteria(
                                "{\"companyNumber\":\"DUMMY_VALUE\",\"shippingDateFrom\":null,\"shippingDateTo\":null,\"transferredDateFrom\":null,\"transferredDateTo\":null,\"statusChangeDateFrom\":null,\"statusChangeDateTo\":null,\"printedDateFrom\":null,\"printedDateTo\":null}");
                searchCriteriaDTO.setSearchCriteriaGroupDTOSet(new HashSet<>());
                searchCriteriaDTO.setCreatedByGroupType(createdByGroupType);
                searchCriteriaDTO.setCreatedBy("");
                for (GroupType groupType : searchCriteriaGroupTypes) {
                        searchCriteriaDTO.getSearchCriteriaGroupDTOSet().add(groupType);
                }
                return searchCriteriaDTO;
        }

}
