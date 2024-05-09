package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.fuse.aaa.dto.UserGroupDTO;
import com.eurodyn.qlack.fuse.aaa.mapper.UserGroupMapper;
import com.eurodyn.qlack.fuse.aaa.model.QUserGroup;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TargetCountryDTO;
import lgl.bayern.de.ecertby.mapper.CatalogValueMapper;
import lgl.bayern.de.ecertby.mapper.OptionMapper;
import lgl.bayern.de.ecertby.mapper.TargetCountryMapper;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.QTargetCountry;
import lgl.bayern.de.ecertby.model.QTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UtilitiesService {

    private final EntityManager entityManager;

    private static QUserGroup qUserGroup = QUserGroup.userGroup;


    private static QTargetCountry qTargetCountry = QTargetCountry.targetCountry;

    private final OptionMapper OPTION_MAPPER_INSTANT = Mappers.getMapper(OptionMapper.class);

    private final TargetCountryMapper TARGET_COUNTRY_MAPPER_INSTANT = Mappers.getMapper(TargetCountryMapper.class);
    private final CatalogValueMapper CATALOG_VALUE_MAPPER_INSTANT = Mappers.getMapper(CatalogValueMapper.class);
    private final UserGroupMapper USER_GROUP_MAPPER_INSTANT = Mappers.getMapper(UserGroupMapper.class);

    @Resource(name = "messages")
    private Map<String, String> messages;

    /**
     * Gets the enum to a list format.
     *
     * @param enumName the enum object to get.
     * @return A list with all the values of enum object.
     */
    public List<OptionDTO> getEnumList(String enumName) {

        List<OptionDTO> enumList = null;
        try {
            Class<?> objectClass = Class.forName("lgl.bayern.de.ecertby.model.util." + enumName);
            java.lang.reflect.Method refEntryMethod =
                    objectClass.getDeclaredMethod("getEnumValues");

            enumList = getAlphabeticallySortedByTranslationList(OPTION_MAPPER_INSTANT.mapToListOptionDTO((List<String>) refEntryMethod.invoke(null, null)));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            log.error("Enumeration List for {} could not be retrieved.", enumName);
        }
        return enumList;
    }

    /**
     * Get group list.
     *
     * @return A list with all groups of the system.
     */
    public List<OptionDTO> getGroupList() {
        BooleanExpression predicate;
        predicate = qUserGroup.parent.id.isNotNull();
        List<UserGroupDTO> userGroupList = USER_GROUP_MAPPER_INSTANT.mapToDTO(new JPAQueryFactory(entityManager)
                .select(qUserGroup)
                .from(qUserGroup)
                .where(predicate).fetch(), false);

        return getAlphabeticallySortedByTranslationList(OPTION_MAPPER_INSTANT.userGroupDTOmapToListOptionDTO(userGroupList));
    }

    /**
     * Returns all target countries or, if availableOnly is true, those with active, released and valid templates.
     * @param availableOnly Specify whether target countries should be filtered based on above criteria.
     * @return The list of countries as OptionDTOs.
     */
    public List<OptionDTO> getTargetCountries(boolean availableOnly) {
        List<TargetCountryDTO> targetCountryDTOList;
        if (availableOnly) {
            targetCountryDTOList = TARGET_COUNTRY_MAPPER_INSTANT.map(new JPAQueryFactory((entityManager))
                    .selectFrom(qTargetCountry)
                    .innerJoin(QTemplate.template)
                    .on(qTargetCountry.id.eq(QTemplate.template.targetCountry.id))
                    .where(
                        new BooleanBuilder()
                        .and(qTargetCountry.eq(QTemplate.template.targetCountry))
                        .and(QTemplate.template.active)
                        .and(QTemplate.template.release)
                        .andAnyOf(
                                (QTemplate.template.validFrom.goe(Instant.now())),
                                (QTemplate.template.validFrom.loe(Instant.now()).and(QTemplate.template.validTo.goe(Instant.now()))),
                                (QTemplate.template.validFrom.loe(Instant.now())).and(QTemplate.template.validTo.isNull())
                        )
                    )
                    .fetch()
            );
        } else {
            targetCountryDTOList = TARGET_COUNTRY_MAPPER_INSTANT.map(new JPAQueryFactory((entityManager)).selectFrom(qTargetCountry).fetch());
        }

        return getTargetCountryAlphabeticallySortedList(OPTION_MAPPER_INSTANT.targetCountryDTOmapToListOptionDTO(targetCountryDTOList));
    }

    /**
     * Gets all products associated with a country through a template or, if availableOnly is true, those in active, released and valid templates.
     * @param targetCountryId The id of the country.
     * @return The products as a list of OptionDTOs.
     */
    public List<OptionDTO> getAvailableProductList(String targetCountryId) {
        List<CatalogValue> productDTOList;
        BooleanBuilder predicate = new BooleanBuilder();
        JPAQuery<CatalogValue> factory = new JPAQueryFactory((entityManager)).select(QTemplate.template.product).from(QTemplate.template);

        predicate.and(QTemplate.template.active)
                .and(QTemplate.template.release)
                .andAnyOf(
                        (QTemplate.template.validFrom.goe(Instant.now())),
                        (QTemplate.template.validFrom.loe(Instant.now()).and(QTemplate.template.validTo.goe(Instant.now()))),
                        (QTemplate.template.validFrom.loe(Instant.now())).and(QTemplate.template.validTo.isNull()));

        if(!targetCountryId.equals("undefined")) {
            predicate.and(QTemplate.template.targetCountry.id.eq(targetCountryId));
        }

        productDTOList = factory.where(predicate).fetch();

        return getAlphabeticallySortedList(CATALOG_VALUE_MAPPER_INSTANT.mapToListOptionDTO(productDTOList));
    }

    public List<OptionDTO> getParentGroupList() {
        BooleanExpression predicate;
        predicate = qUserGroup.parent.id.isNull();
        List<OptionDTO> userGroupList = new JPAQueryFactory(entityManager)
                .select(Projections.fields(OptionDTO.class,
                        QUserGroup.userGroup.id.as(("filterId")),
                        QUserGroup.userGroup.name.as("name"),
                        QUserGroup.userGroup.name.as("id")))
                .from(QUserGroup.userGroup)
                .where(predicate).fetch();
        userGroupList.forEach(optionDTO -> optionDTO.setActive(true));
        return getAlphabeticallySortedByTranslationList(userGroupList);
    }

    private List<OptionDTO> getAlphabeticallySortedList(List<OptionDTO> list) {
        return list.stream().sorted(Comparator.comparing(OptionDTO::getName)).toList();
    }


    private List<OptionDTO> getTargetCountryAlphabeticallySortedList(List<OptionDTO> list) {
        return list.stream().sorted(Comparator.comparing(OptionDTO::getFilterId)).toList();
    }

    private List<OptionDTO> getAlphabeticallySortedByTranslationList(List<OptionDTO> list) {
        return list.stream().sorted(Comparator.comparing(a -> messages.get(a.getName()))).toList();
    }
}
