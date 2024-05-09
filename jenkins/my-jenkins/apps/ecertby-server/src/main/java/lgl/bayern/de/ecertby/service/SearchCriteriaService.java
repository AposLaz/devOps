package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lgl.bayern.de.ecertby.dto.SearchCriteriaDTO;
import lgl.bayern.de.ecertby.mapper.SearchCriteriaMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.SearchCriteriaRepository;
import lgl.bayern.de.ecertby.repository.UserAuthorityRepository;
import lgl.bayern.de.ecertby.repository.UserCompanyRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.validator.SearchCriteriaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class SearchCriteriaService extends BaseService<SearchCriteriaDTO, SearchCriteria, QSearchCriteria> {

    SearchCriteriaMapper searchCriteriaMapper = Mappers.getMapper(SearchCriteriaMapper.class);

    private final SecurityService securityService;

    private final AuditService auditService;

    private final SearchCriteriaRepository searchCriteriaRepository;

    private final UserCompanyRepository userCompanyRepository;

    private final UserDetailRepository userDetailRepository;

    private final UserAuthorityRepository userAuthorityRepository;

    private final SearchCriteriaValidator searchCriteriaValidator;

    private final ObjectLockService objectLockService;

    private final UserDetailService userDetailService;

    public SearchCriteriaDTO saveSearchCriteria(SearchCriteriaDTO searchCriteriaDTO, String selectionFromDD) {
        UserDetail currentUser = securityService.getLoggedInUserDetail();
        SearchCriteria searchCriteria = searchCriteriaMapper.mapToEntity(searchCriteriaDTO);
        setSearchCriteriaMandatoryFields(searchCriteria, currentUser, selectionFromDD);

        searchCriteriaValidator.validateSearchCriteria(searchCriteria, currentUser, selectionFromDD);
        SearchCriteriaDTO oldSearchCriteriaDTO = null;
        if (isNull(searchCriteriaDTO.getId())) {
            log.info(LOG_PREFIX + "Creating search criteria...");
        } else {
            log.info(LOG_PREFIX + "Updating search criteria...");
            oldSearchCriteriaDTO = searchCriteriaMapper.mapToDto(searchCriteriaRepository.findById(searchCriteriaDTO.getId()).get());
        }


        SearchCriteria persistedSearchCriteria = searchCriteriaRepository.save(searchCriteria);
        SearchCriteriaDTO newSearchCriteriaDTO = searchCriteriaMapper.mapToDto(persistedSearchCriteria);

        if (searchCriteriaDTO.getId() == null) {
            // LOG CREATION
            auditService.saveSearchCriteria(AuditAction.CREATE, currentUser, persistedSearchCriteria.getName(), persistedSearchCriteria.getId());
            log.info(LOG_PREFIX + "Search criteria with id {} successfully created by user with id : {}.",
                    persistedSearchCriteria.getId(),
                    currentUser.getId());
        } else {
            // LOG UPDATE
            auditService.saveSearchCriteria(AuditAction.UPDATE, currentUser,
                    searchCriteriaDTO.getName(), oldSearchCriteriaDTO, newSearchCriteriaDTO);
            log.info(LOG_PREFIX + "Search criteria id {} successfully updated by user with id : {}.",
                    searchCriteriaDTO.getId(),
                    currentUser.getId());
        }

        return newSearchCriteriaDTO;
    }


    private void setSearchCriteriaMandatoryFields(SearchCriteria searchCriteria, UserDetail currentUser, String selectionFromDD) {
        GroupType createdByGroupType;
        String createdBy = null;
        boolean isUserRelated = searchCriteria.getSearchCriteriaGroupSet().stream().anyMatch(o -> (o.getGroupType().equals(GroupType.PERSONAL)));

        if (currentUser.getUserType().equals(UserType.COMPANY_USER)) {
            createdByGroupType = GroupType.COMPANY;
        } else if (currentUser.getUserType().equals(UserType.AUTHORITY_USER)) {
            createdByGroupType = GroupType.AUTHORITY;
        } else {
            createdByGroupType = GroupType.ADMIN;
        }

        if (createdByGroupType.equals(GroupType.ADMIN)) {
            if (isUserRelated) {
                if (!currentUser.getId().equals(searchCriteria.getCreatedBy())) {
                    searchCriteria.setId(null);
                }
                createdBy = currentUser.getId();
            }
        } else {
            if (searchCriteria.getCreatedByGroupType().equals(GroupType.ADMIN)) {
                searchCriteria.setId(null);
            }
            if (isUserRelated) {
                if (!currentUser.getId().equals(searchCriteria.getCreatedBy())) {
                    searchCriteria.setId(null);
                }
                createdBy = currentUser.getId();
            } else {
                createdBy = selectionFromDD;
            }

        }

        searchCriteria.setCreatedByGroupType(createdByGroupType);
        searchCriteria.setCreatedBy(createdBy);
    }

    public void setDefaultSearchCriteria(String searchCriteriaUuid, String selectionFromDD) {
        UserDetail currentUser = securityService.getLoggedInUserDetail();
        SearchCriteria searchCriteria = searchCriteriaRepository.findById(searchCriteriaUuid).orElse(null);
        if (searchCriteria != null) {
            // Set a search criteria as default for a user. For company and authority users the information is stored
            // in the UserAuthority UserCompany entities in order to allow a default search criteria per authority or company.
            if (currentUser.getUserType() != null) {
                if (currentUser.getUserType().equals(UserType.COMPANY_USER)) {
                    UserCompany userCompany = userCompanyRepository.findUserCompanyByCompanyIdAndUserDetailId(selectionFromDD, currentUser.getId());
                    if (!isNull(userCompany.getSearchCriteria()) && userCompany.getSearchCriteria().getId().equals(searchCriteria.getId())) {
                        // reset default
                        userCompany.setSearchCriteria(null);
                    } else {
                        userCompany.setSearchCriteria(searchCriteria);
                    }
                    userCompanyRepository.save(userCompany);
                } else if (currentUser.getUserType().equals(UserType.AUTHORITY_USER)) {
                    UserAuthority userAuthority = userAuthorityRepository.findUserAuthorityByAuthorityIdAndUserDetailId(selectionFromDD, currentUser.getId());
                    if (!isNull(userAuthority.getSearchCriteria()) && userAuthority.getSearchCriteria().getId().equals(searchCriteria.getId())) {
                        // reset default
                        userAuthority.setSearchCriteria(null);
                    } else {
                        userAuthority.setSearchCriteria(searchCriteria);
                    }
                    userAuthorityRepository.save(userAuthority);
                } else {
                    if (!isNull(currentUser.getSearchCriteria()) && currentUser.getSearchCriteria().getId().equals(searchCriteria.getId())) {
                        // reset default
                        currentUser.setSearchCriteria(null);
                    } else {
                        currentUser.setSearchCriteria(searchCriteria);
                    }
                    userDetailRepository.save(currentUser);
                }
            }
        } else {
            throw new QExceptionWrapper("Unable to set default search criteria with id: " + searchCriteriaUuid);
        }
    }

    public SearchCriteriaDTO getById(String id) {
        Optional<SearchCriteria> searchCriteriaOptional = searchCriteriaRepository.findById(id);
        if (searchCriteriaOptional.isPresent()) {
            return searchCriteriaMapper.mapToDto(searchCriteriaOptional.get());
        } else {
            return null;
        }
    }

    public Page<SearchCriteriaDTO> getAll(Predicate predicate, Pageable pageable, Set<GroupType> groupTypeSet) {
        BooleanBuilder booleanBuilder = new BooleanBuilder(predicate);

        String loggedInUserDetailId = securityService.getLoggedInUserDetailId();
        BooleanExpression condition = null;

        if (groupTypeSet != null) {
            for (GroupType groupType : groupTypeSet) {
                condition = QSearchCriteria.searchCriteria.searchCriteriaGroupSet.any().groupType.eq(groupType).or(QSearchCriteria.searchCriteria.searchCriteriaGroupSet.any().groupType.eq(GroupType.GLOBAL));
                booleanBuilder.and(condition);
            }
        } else {
            booleanBuilder.and(QSearchCriteria.searchCriteria.createdByGroupType.eq(GroupType.ADMIN).and(QSearchCriteria.searchCriteria.createdBy.isNull()));
        }

        return searchCriteriaRepository.findAll(booleanBuilder, pageable).map(searchCriteriaMapper::mapToDto);
    }

    public void deleteSearchCriteria(String id) {
        SearchCriteriaDTO searchCriteriaDTO = findById(id);
        log.info(LOG_PREFIX + "Deleting Search Criteria...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.SEARCH_CRITERIA);
        SearchCriteriaDTO deletedSearchCriteria = deleteById(id);
        UserDetail userDetail = securityService.getLoggedInUserDetail();

        // Delete references pointing to this search criteria
        deleteAllReferencesToSearchCriteria(id);

        // LOG DELETION
        auditService.saveSearchCriteria(AuditAction.DELETE, userDetail,
                searchCriteriaDTO.getName(), searchCriteriaDTO.getId());
        log.info(LOG_PREFIX + "SearchCriteria with id {} successfully deleted by user with id : {}",
                deletedSearchCriteria.getId(),
                securityService.getLoggedInUserDetailId());
    }

    private void deleteAllReferencesToSearchCriteria(String searchCriteriaId) {
        userCompanyRepository.deleteSearchCriteriaReferences(searchCriteriaId);
        userAuthorityRepository.deleteSearchCriteriaReferences(searchCriteriaId);
        userDetailRepository.deleteSearchCriteriaReferences(searchCriteriaId);
    }


    public List<SearchCriteriaDTO> findUserSearchCriteria(String selectionFromDD) {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();
        UserDetail currentUser = securityService.getLoggedInUserDetail();
        if (UserType.ADMIN_USER.equals(currentUser.getUserType())) {
            searchCriteriaList = searchCriteriaRepository.findUserSearchCriteriaForAdmin(currentUser.getId());
        } else {
            GroupType groupType = (UserType.AUTHORITY_USER.equals(currentUser.getUserType())) ? GroupType.AUTHORITY : GroupType.COMPANY;
            searchCriteriaList = searchCriteriaRepository.findUserSearchCriteria(currentUser.getId(), selectionFromDD, groupType);
        }
        List<SearchCriteriaDTO> searchCriteriaDTOList = searchCriteriaList.stream()
                .map(searchCriteria -> searchCriteriaMapper.mapToDto(searchCriteria))
                .collect(Collectors.toList());
        return searchCriteriaDTOList;
    }


    public String findDefaultCriteriaForUser(String selectionFromDD) {
        UserDetail currentUser = securityService.getLoggedInUserDetail();
        // Default user search criteria is stored in UserAuthority for authority users
        // UserCompany for company users, UserDetail for the rest of the user types.
        // For company/authority user a default search can be selected per organization
        if (currentUser.getUserType() != null) {
            if (currentUser.getUserType().equals(UserType.COMPANY_USER)) {
                String defaultSearchCriteriaId = userCompanyRepository.findSearchCriteriaIdByCompanyIdAndUserDetailId(selectionFromDD, currentUser.getId());
                if (defaultSearchCriteriaId != null) {
                    return defaultSearchCriteriaId;
                }
            } else if (currentUser.getUserType().equals(UserType.AUTHORITY_USER)) {
                String defaultSearchCriteriaId = userAuthorityRepository.findSearchCriteriaIdByAuthorityIdAndUserDetailId(selectionFromDD, currentUser.getId());
                if (defaultSearchCriteriaId != null) {
                    return defaultSearchCriteriaId;
                }
            } else {
                if (currentUser.getSearchCriteria() != null) {
                    return currentUser.getSearchCriteria().getId();
                }
            }
        }
        return null;
    }
}

