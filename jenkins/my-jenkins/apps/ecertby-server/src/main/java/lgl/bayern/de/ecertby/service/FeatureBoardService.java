package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.fuse.fd.dto.ThreadMessageDTO;
import com.eurodyn.qlack.fuse.fd.model.QVote;
import com.eurodyn.qlack.fuse.fd.model.ThreadMessage;
import com.eurodyn.qlack.fuse.fd.model.Vote;
import com.eurodyn.qlack.fuse.fd.util.Reaction;
import com.eurodyn.qlack.fuse.fd.util.ThreadStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.FeatureBoardThreadDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.dto.ViewThreadPermissionsDTO;
import lgl.bayern.de.ecertby.model.FeatureBoard;
import lgl.bayern.de.ecertby.model.QAuthority;
import lgl.bayern.de.ecertby.model.QCompany;
import lgl.bayern.de.ecertby.model.QFeatureBoard;
import lgl.bayern.de.ecertby.model.QUserDetail;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lgl.bayern.de.ecertby.repository.FeatureBoardRepository;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.validator.FeatureBoardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;


@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class FeatureBoardService {
    private final com.eurodyn.qlack.fuse.fd.service.ThreadService qfeatureBoardService;
    private final com.eurodyn.qlack.fuse.fd.service.VoteService qVoteService;
    private final com.eurodyn.qlack.fuse.fd.repository.VoteRepository voteRepository;
    private final com.eurodyn.qlack.fuse.fd.mapper.ThreadMessageMapper featureDashboardMapper;
    private final EmailService emailService;
    private final AuthorityService authorityService;
    private final CompanyService companyService;
    private final EmailNotificationService emailNotificationService;
    private final FeatureBoardRepository featureBoardRepository;
    private final FeatureBoardValidator featureBoardValidator;
    private final QVote qVote = QVote.vote;
    private final QUserDetail qUserDetail = QUserDetail.userDetail;
    @Resource(name = "messages")
    private Map<String, String> messages;
    private final EntityManager entityManager;
    private final SecurityService securityService;
    private final UserDetailRepository userDetailRepository;

    /**
     * Saves a new feature board thread based on the given ThreadMessageDTO and selectionFromDD.
     *
     * @param threadMessageDTO The DTO representing the thread message to be saved.

     * @return The saved FeatureBoard instance.
     */
    public FeatureBoard saveThread(FeatureBoardThreadDTO threadMessageDTO) {
        log.info(LOG_PREFIX + "Saving new thread...");
        String userType = securityService.getLoggedInUserDetailDTO().getUserType().getId();
        String loggedInUser = securityService.getLoggedInUserDetailId();
        FeatureBoard featureBoard = new FeatureBoard();
        if ("AUTHORITY_USER".equals(userType)) {
            featureBoard.setAuthority(authorityService.findEntityById(threadMessageDTO.getResourceId()));
        } else if ("COMPANY_USER".equals(userType)) {
            featureBoard.setCompany(companyService.findEntityById(threadMessageDTO.getResourceId()));
        }
        featureBoard.setCreatedOn(Instant.now());
        featureBoard.setAuthor(loggedInUser);
        featureBoard.setTitle(threadMessageDTO.getTitle());
        featureBoard.setLastUpdate(Instant.now());
        featureBoard.setStatus(ThreadStatus.REQUESTED);
        featureBoard.setStatusComment("Angefordert");
        featureBoard.setBody(threadMessageDTO.getBody());
        featureBoard = featureBoardRepository.save(featureBoard);
        log.info(LOG_PREFIX + "Thread with id {} saved successfully by user with id : {}.",featureBoard.getId(),loggedInUser);
        // Send email to interested admin users
        emailNotificationService.notifyAdminUsersOnFeatureBoardEntry(featureBoard.getId());
        return featureBoard;
    }

    /**
     * Retrieves a paginated list of FeatureBoardThreadDTOs based on various filtering criteria and sorting options.
     *
     * @param userDetailPredicate Predicate for filtering based on user details.
     * @param threadPredicate     Predicate for filtering based on thread details.
     * @param dateFrom            Filter for messages created on or after this date.
     * @param dateTo              Filter for messages created on or before this date.
     * @param authority           Filter for authority.
     * @param company             Filter for company names.
     * @param pageable            Pageable object for pagination and sorting.
     * @param selectionFromDD     The selection identifier used to determine thread visibility.
     * @return A paginated list of FeatureBoardThreadDTOs.
     */
    public Page<FeatureBoardThreadDTO> getAll(Predicate userDetailPredicate,
                                              Predicate threadPredicate,
                                              Instant dateFrom,
                                              Instant dateTo,
                                              String titleFilter,
                                              String firstNameFilter,
                                              String lastNameFilter,
                                              String emailFilter,
                                              String authority,
                                              String company,
                                              Pageable pageable,
                                              String anonymous,
                                              String userViewOptions,
                                              String selectionFromDD) {
        log.info(LOG_PREFIX + "Fetching all feature board threads with filters: " +
                        "userDetailPredicate: {}, " +
                        "threadPredicate: {}, " +
                        "dateFrom: {}, " +
                        "dateTo: {}, " +
                        "titleFilter: {}, " +
                        "firstNameFilter: {}, " +
                        "lastNameFilter: {}, " +
                        "emailFilter: {}, " +
                        "authority: {}, " +
                        "company: {}, " +
                        "anonymous: {}, " +
                        "userViewOptions: {}, " +
                        "pageable: {}, " +
                        "selectionFromDD: {}",
                userDetailPredicate,
                threadPredicate,
                dateFrom,
                dateTo,
                titleFilter,
                firstNameFilter,
                lastNameFilter,
                emailFilter,
                authority,
                company,
                anonymous,
                userViewOptions,
                pageable,
                selectionFromDD);
        //Create a merged predicate with all the values from the filters
        BooleanBuilder predicate = constructPredicate(userDetailPredicate, threadPredicate, dateFrom, dateTo,titleFilter,firstNameFilter,lastNameFilter,emailFilter, authority, company,anonymous,userViewOptions,selectionFromDD);

        //Create the query and apply sorting
        JPAQuery<Tuple> query = constructQuery(predicate, pageable);

        // Fetch the total count of items without pagination
        long totalCount = query.fetch().size();

        // Apply pagination to the query
        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());

        // Fetch the actual data for the current page
        List<FeatureBoardThreadDTO> threadMessageDTOs = constructDtos(query);

        // Create a Page object with the fetched data and total count
        return new PageImpl<>(threadMessageDTOs, pageable, totalCount);


    }
    /**
     * Constructs a BooleanBuilder predicate based on the filtering criteria.
     *
     * @param userDetailPredicate Predicate for filtering based on user details.
     * @param threadPredicate     Predicate for filtering based on thread details.
     * @param dateFrom            Filter for messages created on or after this date.
     * @param dateTo              Filter for messages created on or before this date.
     * @param authority           Filter for authority identifiers.
     * @param company             Filter for company names.
     * @param selectionFromDD     The selection identifier used to determine thread visibility.
     * @return The constructed BooleanBuilder predicate.
     */
    private BooleanBuilder constructPredicate(Predicate userDetailPredicate,
                                              Predicate threadPredicate,
                                              Instant dateFrom,
                                              Instant dateTo,
                                              String titleFilter,
                                              String firstNameFilter,
                                              String lastNameFilter,
                                              String emailFilter,
                                              String authority,
                                              String company,String anonymous ,String userViewOptions ,String selectionFromDD) {
        log.info(LOG_PREFIX + "Constructing predicate with userDetailPredicate: {}, " +
                        "threadPredicate: {}, " +
                        "dateFrom: {}, " +
                        "dateTo: {}, " +
                        "titleFilter: {}, " +
                        "firstNameFilter: {}, " +
                        "lastNameFilter: {}, " +
                        "emailFilter: {}, " +
                        "authority: {}, " +
                        "company: {}, " +
                        "anonymous: {}, " +
                        "userViewOptions: {}, " +
                        "selectionFromDD: {}",
                userDetailPredicate,
                threadPredicate,
                dateFrom,
                dateTo,
                titleFilter,
                firstNameFilter,
                lastNameFilter,
                emailFilter,
                authority,
                company,
                anonymous
                ,userViewOptions,
                selectionFromDD);
        BooleanBuilder mergedPredicate = new BooleanBuilder()
                .and(userDetailPredicate).and(threadPredicate);

        if(titleFilter!= null) {
            mergedPredicate.and(QFeatureBoard.featureBoard.title.toLowerCase().contains(titleFilter.toLowerCase()));
        }
        if(anonymous != null) {
            if(anonymous.equals("yes")) {
                mergedPredicate.and(QFeatureBoard.featureBoard.ownershipMask.eq(ViewThreadVisibility.ANONYMOUS.getValue()));
            } else {
                mergedPredicate.and(QFeatureBoard.featureBoard.ownershipMask.eq(ViewThreadVisibility.NOT_ANONYMOUS.getValue()));
            }
        }

        if(userViewOptions != null) {
            if(userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_COMPANIES.toString())) {
                mergedPredicate.and(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_COMPANIES.getValue()));
            } else if(userViewOptions.equals(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.toString())) {
                mergedPredicate.and(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.getValue()));
            } else {
                mergedPredicate.and(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_ALL.getValue()));
            }
        }
        if (dateFrom != null) {
            mergedPredicate.and(QFeatureBoard.featureBoard.createdOn.goe(dateFrom));
        }
        if (dateTo != null) {
            mergedPredicate.and(QFeatureBoard.featureBoard.createdOn.lt(dateTo.plus(1, ChronoUnit.DAYS)));
        }

        if (authority != null) {
            mergedPredicate.and(QFeatureBoard.featureBoard.authority.id.eq(authority));
        }
        if (company != null) {
            mergedPredicate.and(QFeatureBoard.featureBoard.company.name.toLowerCase().contains(company.toLowerCase()));
        }
        if(firstNameFilter != null){
            mergedPredicate.and(QUserDetail.userDetail.firstName.toLowerCase().contains(firstNameFilter.toLowerCase()).or(QFeatureBoard.featureBoard.firstName.toLowerCase().contains(firstNameFilter.toLowerCase())));
        }
        if(lastNameFilter != null){
            mergedPredicate.and(QUserDetail.userDetail.lastName.toLowerCase().contains(lastNameFilter.toLowerCase()).or(QFeatureBoard.featureBoard.firstName.toLowerCase().contains(lastNameFilter.toLowerCase())));
        }
        if(emailFilter != null){
            mergedPredicate.and(QUserDetail.userDetail.email.toLowerCase().contains(emailFilter.toLowerCase()).or(QFeatureBoard.featureBoard.firstName.toLowerCase().contains(emailFilter.toLowerCase())));
        }
        mergedPredicate.and(QFeatureBoard.featureBoard.parentThreadMessage.isNull());
        if (securityService.getLoggedInUserDetailDTO().getUserType().getId().equals(UserType.AUTHORITY_USER.toString())) {
            mergedPredicate.and(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.getValue())
                    .or(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_ALL.getValue())).or(QFeatureBoard.featureBoard.author.eq(securityService.getLoggedInUserDetailId()).and(QFeatureBoard.featureBoard.authority.id.eq(selectionFromDD))));
        } else if (securityService.getLoggedInUserDetailDTO().getUserType().getId().equals(UserType.COMPANY_USER.toString())) {
            mergedPredicate.and(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_COMPANIES.getValue())
                    .or(QFeatureBoard.featureBoard.attributesMask.eq(ViewThreadVisibility.VISIBLE_TO_ALL.getValue()).or(QFeatureBoard.featureBoard.author.eq(securityService.getLoggedInUserDetailId()).and(QFeatureBoard.featureBoard.company.id.eq(selectionFromDD)))));
        }

        return mergedPredicate;
    }
    /**
     * Constructs a JPAQuery with sorting based on the given predicate and pageable options.
     *
     * @param predicate The BooleanBuilder predicate for filtering.
     * @param pageable  Pageable object for pagination and sorting.
     * @return A JPAQuery object with sorting applied.
     */
    private JPAQuery<Tuple> constructQuery(Predicate predicate, Pageable pageable) {
        log.info(LOG_PREFIX + "Constructing JPA query with predicate: {} and pageable: {} " , predicate , pageable);
        JPAQuery<Tuple> query = new JPAQueryFactory(entityManager)
                .select(
                        QFeatureBoard.featureBoard.id,
                        QFeatureBoard.featureBoard.title,
                        QFeatureBoard.featureBoard.createdOn,
                        QFeatureBoard.featureBoard.status,
                        QFeatureBoard.featureBoard.ownershipMask,
                        QFeatureBoard.featureBoard.author,
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(QVote.vote.id.count())
                                        .from(QVote.vote)
                                        .where(
                                                QVote.vote.threadMessage.id.eq(QFeatureBoard.featureBoard.id)
                                                        .and(QVote.vote.reaction.eq(Reaction.LIKE))
                                        ),
                                "positiveReviewCount"
                        ),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(QVote.vote.id.count())
                                        .from(QVote.vote)
                                        .where(
                                                QVote.vote.threadMessage.id.eq(QFeatureBoard.featureBoard.id)
                                                        .and(QVote.vote.reaction.eq(Reaction.DISLIKE))
                                        ),
                                "negativeReviewCount"
                        ),
                        qUserDetail.firstName,
                        qUserDetail.lastName,
                        qUserDetail.email,
                       QFeatureBoard.featureBoard.authority.name,
                        QFeatureBoard.featureBoard.company.name,
                        ExpressionUtils.as(JPAExpressions
                                        .selectOne()
                                        .from(QVote.vote)
                                        .where(
                                                QVote.vote.threadMessage.id.eq(QFeatureBoard.featureBoard.id)
                                                        .and(QVote.vote.voterId.eq(securityService.getLoggedInUserDetailId())).and(QVote.vote.reaction.eq(Reaction.LIKE))
                                        )
                                        .exists(),
                                "userUpvoted"),
                        ExpressionUtils.as(JPAExpressions
                                        .selectOne()
                                        .from(QVote.vote)
                                        .where(
                                                QVote.vote.threadMessage.id.eq(QFeatureBoard.featureBoard.id)
                                                        .and(QVote.vote.voterId.eq(securityService.getLoggedInUserDetailId())).and(QVote.vote.reaction.eq(Reaction.DISLIKE))
                                        )
                                        .exists(),
                                "userDownvoted"),
                        QFeatureBoard.featureBoard.firstName,
                        QFeatureBoard.featureBoard.lastName,
                        QFeatureBoard.featureBoard.email,
                        QFeatureBoard.featureBoard.attributesMask
                )
                .from(QFeatureBoard.featureBoard)
                .leftJoin(QUserDetail.userDetail)
                .on(QUserDetail.userDetail.id.eq(QFeatureBoard.featureBoard.author))
                .leftJoin(QFeatureBoard.featureBoard.authority , QAuthority.authority)
                .leftJoin(QFeatureBoard.featureBoard.company , QCompany.company)
                .where(predicate);


        //This is a map where each field of the dto is linked with a Path for sorting purposes
        Map<String, PathBuilder<?>> pathMap = new HashMap<>();
        pathMap.put("title", new PathBuilder<>(QFeatureBoard.class, "featureBoard.title"));
        pathMap.put("firstName", new PathBuilder<>(QUserDetail.class, "userDetail.firstName"));
        pathMap.put("lastName", new PathBuilder<>(QUserDetail.class, "userDetail.lastName"));
        pathMap.put("createdOn", new PathBuilder<>(QFeatureBoard.class, "featureBoard.createdOn"));
        pathMap.put("status", new PathBuilder<>(QFeatureBoard.class, "featureBoard.statusComment"));
        pathMap.put("email", new PathBuilder<>(QUserDetail.class, "userDetail.email"));
        pathMap.put("authorityFilter", new PathBuilder<>(QFeatureBoard.class, "featureBoard.authority.name"));
        pathMap.put("companyFilter", new PathBuilder<>(QFeatureBoard.class, "featureBoard.company.name"));
        pathMap.put("positiveReviews", new PathBuilder<>(QVote.class, "positiveReviewCount"));
        pathMap.put("negativeReviews", new PathBuilder<>(QVote.class, "negativeReviewCount"));
        pathMap.put("anonymous", new PathBuilder<>(QFeatureBoard.class, "ownershipMask"));
        pathMap.put("userViewOptions", new PathBuilder<>(QFeatureBoard.class, "attributesMask"));
        if (pageable.getSort() != null && !pageable.getSort().isEmpty()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                    PathBuilder<?> path = pathMap.get(property);

                    if (path != null) {
                        Expression<?> expression = (Expression<?>) path;
                        OrderSpecifier<?> orderSpecifier;
                        if (order.getDirection() == Sort.Direction.ASC) {
                            orderSpecifier = new OrderSpecifier<>(Order.ASC, (Expression) expression , OrderSpecifier.NullHandling.NullsLast);
                        } else {
                            orderSpecifier = new OrderSpecifier<>(Order.DESC, (Expression) expression,OrderSpecifier.NullHandling.NullsLast);
                        }
                        query.orderBy(orderSpecifier);
                    }
            });
        } else {
            query.orderBy(new OrderSpecifier<>(Order.DESC, (Expression) pathMap.get("createdOn")));
        }

        return query;
    }

    /**
     * Constructs a list of FeatureBoardThreadDTOs from the results of a JPAQuery.
     *
     * @param query The JPAQuery containing the query results.
     * @return A list of FeatureBoardThreadDTOs.
     */
    private List<FeatureBoardThreadDTO> constructDtos(JPAQuery<Tuple> query) {
        return query.fetch()
                .stream()
                .map(tuple -> {
                    FeatureBoardThreadDTO dto = new FeatureBoardThreadDTO();
                    dto.setId(tuple.get(QFeatureBoard.featureBoard.id));
                    dto.setTitle(tuple.get(QFeatureBoard.featureBoard.title));
                    dto.setCreatedOn(tuple.get(QFeatureBoard.featureBoard.createdOn));
                    dto.setStatus(tuple.get(QFeatureBoard.featureBoard.status));
                    dto.setPositiveReviews(tuple.get(6, Long.class));
                    dto.setNegativeReviews(tuple.get(7, Long.class));
                    String ownershipMask = tuple.get(QFeatureBoard.featureBoard.ownershipMask);
                    String attributesMask = tuple.get(QFeatureBoard.featureBoard.attributesMask);
                    String author = tuple.get(QFeatureBoard.featureBoard.author);
                    if (ownershipMask != null) {
                        constructDTOAnonimity(dto, ownershipMask);
                    } else {
                        ownershipMask ="";
                    }
                    if(author != null) {
                        if ((!ownershipMask.equals(ViewThreadVisibility.ANONYMOUS.getValue()) ||
                                securityService.getLoggedInUserDetailDTO().getUserType().getId().equals(UserType.ADMIN_USER.toString())) ||
                                author.equals(securityService.getLoggedInUserDetailId())) {
                            dto.setAuthor(tuple.get(QFeatureBoard.featureBoard.author));
                            dto.setFirstNameFilter(tuple.get(QUserDetail.userDetail.firstName));
                            dto.setLastNameFilter(tuple.get(QUserDetail.userDetail.lastName));
                            dto.setEmailFilter(tuple.get(QUserDetail.userDetail.email));
                            dto.setAuthorityFilter(tuple.get(QFeatureBoard.featureBoard.authority.name));
                            dto.setCompanyFilter(tuple.get(QFeatureBoard.featureBoard.company.name));
                        } else {
                            dto.setFirstNameFilter(messages.get("anonymous"));
                            dto.setLastNameFilter(messages.get("users"));
                        }
                    } else {
                        dto.setFirstNameFilter(tuple.get(QFeatureBoard.featureBoard.firstName));
                        String deletedUserString = tuple.get(QFeatureBoard.featureBoard.lastName) + " (" + messages.get("deleted_user") + ")";
                        dto.setLastNameFilter(deletedUserString);
                        dto.setEmailFilter(tuple.get(QFeatureBoard.featureBoard.email));
                    }
                    if(attributesMask != null) {
                        constructUserVisibility(dto, attributesMask);
                    }
                    dto.setUserUpvoted(Boolean.TRUE.equals(tuple.get(13, Boolean.class)));
                    dto.setUserDownvoted(Boolean.TRUE.equals(tuple.get(14, Boolean.class)));
                    return dto;

                })
                .collect(Collectors.toList());
    }

    private void constructDTOAnonimity(FeatureBoardThreadDTO dto, String ownershipMask) {
        if(ownershipMask.equals(ViewThreadVisibility.ANONYMOUS.getValue())) {
            dto.setAnonymous(messages.get("yes"));
        } else {
            dto.setAnonymous(messages.get("no"));
        }
    }

    private static void constructUserVisibility(FeatureBoardThreadDTO dto, String attributesMask) {
        if (attributesMask.equals(ViewThreadVisibility.VISIBLE_TO_COMPANIES.getValue())) {
            dto.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_COMPANIES.toString());
        } else if (attributesMask.equals(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.getValue())) {
            dto.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.toString());
        } else {
            dto.setUserViewOptions(ViewThreadVisibility.VISIBLE_TO_ALL.toString());
        }
    }

    /**
     * Publishes a feature board thread with the specified visibility and ownership options.
     *
     * @param id                     The ID of the thread message to be published.
     * @param viewThreadPermissionsDTO The permissions DTO containing visibility and ownership settings.
     */
    public void publish(String id, ViewThreadPermissionsDTO viewThreadPermissionsDTO) {
        UserDetailDTO loggedInUser = securityService.getLoggedInUserDetailDTO();

        ThreadMessage threadMessage = featureBoardRepository.fetchById(id);
        if(threadMessage.getStatus().equals(ThreadStatus.REQUESTED)) {
            log.info(LOG_PREFIX + "Publishing thread with id: {} by user with id : {}", id, loggedInUser.getId());
            if (viewThreadPermissionsDTO.isAuthorityVisible() && viewThreadPermissionsDTO.isCompanyVisible()) {
                threadMessage.setAttributesMask(ViewThreadVisibility.VISIBLE_TO_ALL.getValue());
            } else if (viewThreadPermissionsDTO.isAuthorityVisible()) {
                threadMessage.setAttributesMask(ViewThreadVisibility.VISIBLE_TO_AUTHORITIES.getValue());
            } else if (viewThreadPermissionsDTO.isCompanyVisible()) {
                threadMessage.setAttributesMask(ViewThreadVisibility.VISIBLE_TO_COMPANIES.getValue());
            }

            if (viewThreadPermissionsDTO.isAnonymous()) {
                threadMessage.setOwnershipMask(ViewThreadVisibility.ANONYMOUS.getValue());
            } else {
                threadMessage.setOwnershipMask(ViewThreadVisibility.NOT_ANONYMOUS.getValue());
            }
            threadMessage.setStatus(ThreadStatus.PUBLISHED);
            threadMessage.setStatusComment(AppConstants.EnumTranslations.PUBLISHED);
            featureBoardRepository.save(threadMessage);
            UserDetail user = userDetailRepository.findById(threadMessage.getAuthor()).get();
            emailService.sendThreadPublishedEMail(threadMessage.getTitle(), user.getEmail());
            log.info(LOG_PREFIX + "Sent email to user with id : {} for published thread with id : {} successfully", user.getId(), threadMessage.getId());
            log.info(LOG_PREFIX + "Thread published successfully.");
        } else {
            log.info(LOG_PREFIX + "Thread with id : {} cannot be published.",
                    id);
            throw new NotAllowedException("Thread cannot be published.");
        }
    }
    /**
     * Rejects a feature board thread with a reason and sends an email notification to the author.
     *
     * @param id     The ID of the thread message to be rejected.
     * @param reason The reason for rejecting the thread.
     */
    public void reject(String id, String reason) {
        UserDetailDTO loggedInUser = securityService.getLoggedInUserDetailDTO();
        ThreadMessage threadMessage = featureBoardRepository.fetchById(id);

        if(threadMessage.getStatus().equals(ThreadStatus.REQUESTED)) {
            log.info(LOG_PREFIX + "Rejecting thread with id: {} by user with id : {} ", id, loggedInUser.getId());
            UserDetail user = userDetailRepository.findById(threadMessage.getAuthor()).get();
            threadMessage.setStatus(ThreadStatus.REJECTED);
            threadMessage.setStatusComment(AppConstants.EnumTranslations.REJECTED);
            featureBoardRepository.save(threadMessage);
            emailService.sendThreadRejectedEMail(reason, threadMessage.getTitle(), user.getEmail());
            log.info(LOG_PREFIX + "Sent email to user with id : {} for rejected thread with id : {} successfully.", user.getId(), threadMessage.getId());
        } else {
            log.info(LOG_PREFIX + "Thread with id {} cannot be rejected.",
                    id);
            throw new NotAllowedException("Thread cannot be rejected.");
        }
    }
    /**
     * Records a vote (like or dislike) for a feature board thread.
     *
     * @param id       The ID of the thread message to be voted on.
     * @param reaction The type of reaction (like or dislike) to record.
     */
    public void vote(String id, Reaction reaction) {
        UserDetailDTO loggedInUser = securityService.getLoggedInUserDetailDTO();
        log.info(LOG_PREFIX + "Creating a new vote...");
        ThreadMessage threadMessage = featureBoardRepository.fetchById(id);
        if (threadMessage.getStatus().equals(ThreadStatus.PUBLISHED)
                && !loggedInUser.getId().equals(threadMessage.getAuthor())) {
            Vote existingVote = getUserVote(id, loggedInUser);
            if (existingVote != null) {
                deleteExistingVote(existingVote, loggedInUser);
            }

            if (existingVote == null || !existingVote.getReaction().equals(reaction)) {
                saveUserVote(id, reaction, threadMessage, loggedInUser);
            }
        } else {
            log.info(LOG_PREFIX + "User with id : {} cannot vote on thread with id {}.", loggedInUser.getId(), id);
            throw new NotAllowedException("Thread cannot be voted.");
        }
    }

    private Vote getUserVote(String id, UserDetailDTO loggedInUser) {
        return new JPAQueryFactory(entityManager)
                .select(QVote.vote)
                .from(QVote.vote)
                .where(qVote.voterId.eq(loggedInUser.getId())
                        .and(QVote.vote.threadMessage.id.eq(id)))
                .fetchOne();
    }

    private void deleteExistingVote(Vote existingVote, UserDetailDTO loggedInUser) {
        voteRepository.delete(existingVote);
        log.info(LOG_PREFIX + "Previous vote by user {} deleted", loggedInUser);
    }

    private void saveUserVote(String id, Reaction reaction, ThreadMessage threadMessage, UserDetailDTO loggedInUser) {
        Vote vote = new Vote();
        vote.setCreatedOn(Instant.now());
        vote.setReaction(reaction);
        vote.setThreadMessage(threadMessage);
        vote.setVoterId(loggedInUser.getId());
        voteRepository.save(vote);
        log.info(LOG_PREFIX + "User with id : {} {} thread with id: {} successfully.", loggedInUser, reaction == Reaction.LIKE ? "liked" : "disliked", id);
    }

    /**
     * Retrieves a FeatureBoardThreadDTO for a specific feature board thread by its ID.
     *
     * @param id The ID of the feature board thread to retrieve.
     * @return The FeatureBoardThreadDTO for the specified thread.
     */
    public ResponseEntity<FeatureBoardThreadDTO> findById(String id) {
        log.info(LOG_PREFIX + "Finding thread by ID: {}", id);
        UserDetail loggedUser = securityService.getLoggedInUserDetail();
        ThreadMessageDTO thread = qfeatureBoardService.findById(id);

        if (!featureBoardValidator.validateGetRequest(thread, loggedUser)) {
            log.info("User with id : {} has no rights to view Feature Board Thread with id {}.", loggedUser.getId(), id);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        long positiveReviews = new JPAQuery<>(entityManager).select(QVote.vote)
                .from(QVote.vote)
                .where(
                        QVote.vote.threadMessage.id.eq(id)
                                .and(QVote.vote.reaction.eq(Reaction.LIKE))
                ).fetchCount();

        long negativeReviews = new JPAQuery<>(entityManager).select(QVote.vote)
                .from(QVote.vote)
                .where(
                        QVote.vote.threadMessage.id.eq(id)
                                .and(QVote.vote.reaction.eq(Reaction.DISLIKE))
                ).fetch().size();

        boolean userUpvoted = featureBoardRepository.findVotesByStatusAndUser(id, securityService.getLoggedInUserDetailId(),Reaction.LIKE)> 0;
        boolean userDownvoted = featureBoardRepository.findVotesByStatusAndUser(id, securityService.getLoggedInUserDetailId(),Reaction.DISLIKE) > 0;

        FeatureBoardThreadDTO dto = new FeatureBoardThreadDTO();
        if (thread.getOwnershipMask() != null) {
            constructDTOAnonimity(dto, thread.getOwnershipMask());
        }
        if(thread.getAttributesMask() != null) {
            constructUserVisibility(dto, thread.getAttributesMask());
        }
        dto.setStatus(thread.getStatus());
        dto.setTitle(thread.getTitle());
        dto.setBody(thread.getBody());
        dto.setCreatedOn(thread.getCreatedOn());
        dto.setUserDownvoted(userDownvoted);
        dto.setUserUpvoted(userUpvoted);
        dto.setPositiveReviews(positiveReviews);
        dto.setNegativeReviews(negativeReviews);
        dto.setAuthor(thread.getAuthor());
        log.info(LOG_PREFIX + "Thread with id {} found successfully.",id);
        return ResponseEntity.ok(dto);
    }
    /**
     * Retrieves a list of comments associated with a specific feature board thread.
     *
     * @param id The ID of the main feature board thread for which comments are retrieved.
     * @return A list of FeatureBoardThreadDTOs representing comments on the main thread.
     */
    public List<FeatureBoardThreadDTO> getComments(String id) {
        log.info(LOG_PREFIX + "Getting comments for thread with ID: {}", id);
        ThreadMessage mainThread = featureBoardRepository.findById(id).get();
        boolean isAnonymous ;
        if(mainThread.getStatus().equals(ThreadStatus.PUBLISHED) && mainThread.getOwnershipMask() != null ) {
            isAnonymous = mainThread.getOwnershipMask().equals(ViewThreadVisibility.ANONYMOUS.getValue());
        } else {
            isAnonymous = false;
        }
        List<FeatureBoardThreadDTO> mainThreadComments = new ArrayList<>();
        List<ThreadMessage> commentsOfComments = qfeatureBoardService.findChildrenThreads(mainThread);

        for (ThreadMessage comment : commentsOfComments) {
            if (comment.getParentThreadMessage() != null) {
                FeatureBoardThreadDTO dto = new FeatureBoardThreadDTO();
                dto.setCreatedOn(comment.getCreatedOn());
                dto.setBody(comment.getBody());
                checkThreadAnonimity(isAnonymous, comment, dto);
                dto.setId(comment.getId());
                dto.setNestedComments(new ArrayList<>());
                dto.setLastUpdate(comment.getLastUpdate());
                mainThreadComments.add(dto);

                // Recursively populate nested comments of the comment
                populateComments(comment, dto.getNestedComments(),isAnonymous);
            }
        }
        log.info(LOG_PREFIX + "Comments retrieved successfully.");
        return mainThreadComments;
    }

    /**
     * Recursively populates the list of FeatureBoardThreadDTOs with nested comments of a thread message.
     *
     * @param thread The parent thread message for which nested comments are being populated.
     * @param dtos   The list of FeatureBoardThreadDTOs to which nested comments are added.
     * @param isAnonymous Indicates whether comments should be treated as anonymous.
     */
    private void populateComments(ThreadMessage thread, List<FeatureBoardThreadDTO> dtos,boolean isAnonymous) {
        log.debug("Populating comments for thread with ID: {}", thread.getId());
        List<ThreadMessage> childComments = qfeatureBoardService.findChildrenThreads(thread);
        for (ThreadMessage comment : childComments ) {
            FeatureBoardThreadDTO reply = new FeatureBoardThreadDTO();
            reply.setCreatedOn(comment.getCreatedOn());
            reply.setBody(comment.getBody());
            reply.setId(comment.getId());
            reply.setLastUpdate(comment.getLastUpdate());
            checkThreadAnonimity(isAnonymous, comment, reply);
            reply.setNestedComments(new ArrayList<>());
            dtos.add(reply);

            // Recursively populate nested comments of the comment
            populateComments(comment, reply.getNestedComments(),isAnonymous);
        }
        log.debug("Comments populated for thread with ID: {}", thread.getId());
    }
    /**
     * Checks whether a thread message should be treated as anonymous and sets the author in the FeatureBoardThreadDTO accordingly.
     *
     * @param isAnonymous Indicates whether the thread should be treated as anonymous.
     * @param comment     The thread message to check and extract the author.
     * @param dto         The FeatureBoardThreadDTO to which the author is set.
     */

    private void checkThreadAnonimity(boolean isAnonymous, ThreadMessage comment, FeatureBoardThreadDTO dto) {
        log.debug("Checking thread anonymity for comment with ID: {}", comment.getId());
        if (comment.getAuthor() == null) {
            if (!isAnonymous ||securityService.getLoggedInUserDetailDTO().getUserType().getId().equals(UserType.ADMIN_USER.toString())) {
                setDeletedUserDetails(dto, comment.getId());
            } else {
                dto.setFirstNameFilter(messages.get("anonymous_user"));
            }
        } else if (!isAnonymous || securityService.getLoggedInUserDetailDTO().getUserType().getId().equals(UserType.ADMIN_USER.toString()) || comment.getAuthor().equals(securityService.getLoggedInUserDetailId())) {
            setUserDetails(dto, comment.getAuthor());
        } else {
            dto.setFirstNameFilter(messages.get("anonymous_user"));
        }
        log.debug("Thread anonymity checked for comment with ID: {}.", comment.getId());
    }

    private void setDeletedUserDetails(FeatureBoardThreadDTO dto, String commentId) {
        String firstName = featureBoardRepository.findFirstNamebyId(commentId);
        String lastName = featureBoardRepository.findLastNamebyId(commentId);
        dto.setFirstNameFilter(firstName);
        dto.setLastNameFilter(lastName + " (" + messages.get("deleted_user") + ')');
    }

    private void setUserDetails(FeatureBoardThreadDTO dto, String authorId) {
        Optional<UserDetail> userDetail = userDetailRepository.findById(authorId);
        userDetail.ifPresent(detail -> {
            dto.setAuthor(detail.getUsername());
            dto.setFirstNameFilter(detail.getFirstName());
            dto.setLastNameFilter(detail.getLastName());
        });
    }

    /**
     * Saves a new comment on a feature board thread and returns the corresponding FeatureBoardThreadDTO.
     *
     * @param id          The ID of the main feature board thread to which the comment is added.
     * @param commentText The text content of the comment.
     * @return The FeatureBoardThreadDTO representing the saved comment.
     */
public FeatureBoardThreadDTO saveComment(String id, String commentText) {
    log.info(LOG_PREFIX + "Saving comment for thread with ID: {}", id);
       UserDetailDTO author = securityService.getLoggedInUserDetailDTO();

       ThreadMessageDTO thread = qfeatureBoardService.addComment(id,commentText,author.getId());
       FeatureBoardThreadDTO dto = new FeatureBoardThreadDTO();
       dto.setLastUpdate(thread.getLastUpdate());
       dto.setCreatedOn(thread.getCreatedOn());
       dto.setAuthor(author.getUsername());
        dto.setFirstNameFilter(author.getFirstName());
        dto.setLastNameFilter(author.getLastName());
       dto.setBody(thread.getBody());
       dto.setId(thread.getId());
       log.info(LOG_PREFIX + "Comment saved successfully for thread with ID: {} by user with id : {}.", id,author.getId());
       return dto;
}
    /**
     * Edits the text content of a comment associated with a feature board thread.
     *
     * @param id   The ID of the comment to be edited.
     * @param text The new text content for the comment.
     */
    public void editComment(String id ,String text) {
        log.info(LOG_PREFIX + "Editing comment for thread with ID: {}", id);
        String loggedInUserId = securityService.getLoggedInUserDetailId();
        Optional<ThreadMessage> threadComment = featureBoardRepository.findById(id);
        if(threadComment.isPresent()) {
            if(threadComment.get().getAuthor().equals(loggedInUserId)) {
                threadComment.get().setBody(text);
                threadComment.get().setLastUpdate(Instant.now());
                featureBoardRepository.save(threadComment.get());
                log.info(LOG_PREFIX + "Comment edited successfully for thread with ID: {}.", id);
            } else {
                log.info(LOG_PREFIX + "User with id : {} has no rights to edit on the comment with id {}.",
                        loggedInUserId,
                        id);
                throw new NotAllowedException("Comment cannot be edited.");
            }
        }

    }

}
