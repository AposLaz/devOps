package lgl.bayern.de.ecertby.service;

import static java.util.Objects.isNull;
import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;
import static org.springframework.web.util.HtmlUtils.htmlEscape;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lgl.bayern.de.ecertby.annotation.AuditFirstNameLastName;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.UserAuthorityCompanyDTO;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.exception.EcertBYAuditingException;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DiffService<T1 extends ComparableDTO> {

    @Resource(name = "messages")
    private Map<String, String> messages;

    private final UserGroupRepository userGroupRepository;
    private final UserDetailRepository userDetailRepository;

    //random UUID so it can be unique. It should not be printed anywhere.
    private final String PARENT_IDENTIFIER = UUID.randomUUID().toString();

    private final String EMPTY_OBJECT = " <i>Null</i> ";
    private final String LIST_DELIMITER = ", ";

    private final String VALUE_REPRESENTATION = "<b>%s</b> (%s → %s), ";
    private final String SECONDARY_VALUE_REPRESENTATION = "\"%s\" <b>%s</b>: %s";

    private final String GENERAL_COMPARISON_ERROR = "Error in object comparison. Auditing logs may be incomplete. Please advice application logs.";
    private final String NO_ANNOTATED_LIST_FIELD_ERROR = "This arraylist cannot be compared because it contains objects that do not include fields annotated with @AuditIdentifier.";
    private final String IDENTIFIER_FIELD_WITH_EMPTY_VALUE_ERROR = "The identifier field has empty value.";

    private final String DATETIME_FORMATTER_PATTERN = "dd.MM.yyyy";


    /**
     * A custom comparison service mostly designed to compare DTOs for auditing purposes.
     * Please read carefully before implementing any new DTOs:
     * <p>
     * This service works with the following annotations:
     * <p>
     * <b>@AuditTranslationKey</b>: If the translation key of the field is different from its name, the
     * translation key should be provided using this annotation. This will be used for the translation
     * process during auditing.
     * <p>
     * <b>@AuditIdentifier</b>: This indicates the attribute from which the value will be displayed in the auditing.
     * There should be only one identifier for each object, including their parent classes.
     * <p>
     * <b>@AuditIgnore</b>: Use this annotation to ignore the annotated fields from the comparison.
     *<p>
     * @param oldObj - The old version of the object
     * @param newObj - The new version of the object
     * @param introMessage - The message that will be appended at the beginning of the audit log.
     * @return - A String message containing all the changes to be logged afterwards.
     */

    public String compare(T1 oldObj, T1 newObj, String introMessage) {
        Map<String, List<String>> changesMap = new HashMap<>();

        try {
            compareObjects(oldObj, newObj, changesMap, PARENT_IDENTIFIER, null);
        } catch (Exception e) {
            log.warn(GENERAL_COMPARISON_ERROR, e);
            return introMessage;
        }

        if (changesMap.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        builder.append(introMessage.trim()).append(" ")
            .append(messages.get("following_fields_changed")).append(" ");


        for (Map.Entry<String, List<String>> entry : changesMap.entrySet()) {
            if (!entry.getKey().equals(PARENT_IDENTIFIER) && entry.getValue().size() > 1) {
                builder.append(entry.getKey()).append(" ");
            }
            for (String change : entry.getValue()) {
                builder.append(change);
            }
        }

        log.info(LOG_PREFIX + "Comparison of objects of type {} completed.",
            (!isNull(oldObj) ? oldObj.getClass().getSimpleName() : newObj.getClass().getSimpleName()));

        //replace at the final result ending (,) with (.).
        return builder.toString().trim().replaceAll(",$", ".");
    }

    /**
     * Compares each object and their child objects recursively.
     * The child objects <b>MUST HAVE</b> a field annotated with @AuditIdentifier.
     * @param oldObj - The old version of the object.
     * @param newObj - The new version of the object.
     * @param changesMap  - The map where the changes will be added.
     * @param parentObjIdentifier - The identifier of the object.
     * @throws Exception - Exception.
     */
    private void compareObjects(Object oldObj, Object newObj, Map<String, List<String>> changesMap,
        String parentObjIdentifier, Field parentField)
        throws Exception {

        if (isNull(oldObj) && isNull(newObj)) {
            return;
        }

        Class<?> clazz = !isNull(oldObj) ? oldObj.getClass() : newObj.getClass();
        List<Field> fields = getAllFields(new ArrayList<>(), clazz);

        if (!parentObjIdentifier.equals(PARENT_IDENTIFIER)) {
            Field identifierField = getIdentifierField(fields, null);

            if (!isNull(identifierField)){
                compareObject(oldObj, newObj, changesMap, parentObjIdentifier, identifierField, parentObjIdentifier, parentField);
                return;
            }
        }

        for (Field field : fields) {
            compareObject(oldObj, newObj, changesMap, parentObjIdentifier, field, null, null);
        }
    }

    private void compareObject(Object oldObj, Object newObj, Map<String, List<String>> changesMap,
        String objIdentifier, Field field, String parentObjIdentifier, Field parentField) throws Exception {

        field.setAccessible(true);
        Object value1 = !isNull(oldObj) && !isNull(field.get(oldObj)) ? field.get(oldObj) : null;
        Object value2 = !isNull(newObj) && !isNull(field.get(newObj)) ? field.get(newObj) : null;

        if (!Objects.equals(value1, value2)) {

            // We need to examine the object class, but there might be a case where one of them is null
            // and at this point we don't know which one is it.
            Object objectSample = !isNull(value1) ? value1 : value2;

            if (field.getType().isPrimitive() || Boolean.class.isAssignableFrom(objectSample.getClass()) || field.getType().equals(String.class) || field.getType().isEnum()) {
                addResultToMap(changesMap, objIdentifier, parentObjIdentifier, field, value1, value2, parentField);

            } else if (Collection.class.isAssignableFrom(objectSample.getClass())){
                compareCollections(value1, value2, changesMap, objIdentifier, field, parentObjIdentifier);

            } else if (LocalDateTime.class.isAssignableFrom(objectSample.getClass())){
                compareDateTimes(value1, value2, changesMap, objIdentifier, field, parentObjIdentifier);

            } else if (Instant.class.isAssignableFrom(objectSample.getClass())){
                compareInstants(value1, value2, changesMap, objIdentifier, field, parentObjIdentifier);

            } else {
                    compareObjects(value1, value2, changesMap, getFieldName(field), field);
            }
        }
    }

    private void compareInstants(Object inst1, Object inst2, Map<String, List<String>> changesMap,
        String objIdentifier, Field field, String parentField) {

        // The instants will be manipulated in the front-end. They return with their raw value at this point.
        addResultToMap(changesMap, objIdentifier, parentField, field,
            !isNull(inst1) ? inst1.toString() : null, !isNull(inst2) ? inst2.toString() : null, null);
    }

    private void compareDateTimes(Object date1, Object date2, Map<String, List<String>> changesMap,
        String objIdentifier, Field field, String parentField) {

        LocalDateTime parsedDate1 = !isNull(date1) ? (LocalDateTime) date1 : null;
        LocalDateTime parsedDate2 = !isNull(date2) ? (LocalDateTime) date2 : null;

        compareDates(parsedDate1, parsedDate2, changesMap, objIdentifier, field, parentField);
    }

    private void compareDates(LocalDateTime parsedDate1, LocalDateTime parsedDate2, Map<String, List<String>> changesMap,
        String objIdentifier, Field field, String parentField) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMATTER_PATTERN);

        addResultToMap(changesMap, objIdentifier, parentField, field,
            !isNull(parsedDate1) ? formatter.format(parsedDate1) : null,
            !isNull(parsedDate2) ? formatter.format(parsedDate2) : null, null);

    }

    private void compareCollections(Object list1, Object list2, Map<String, List<String>> changesMap,
        String objIdentifier, Field field, String parentField) throws IllegalAccessException {

        Collection<String> plainCollection1 = convertObjectListToStringList((Collection<Object>) list1, field);
        Collection<String> plainCollection2 = convertObjectListToStringList((Collection<Object>) list2, field);

        //Check if both lists contain the same elements and returns if true (nothing to compare).
        if (plainCollection1.containsAll(plainCollection2) && plainCollection2.containsAll(plainCollection1)) {
            return;
        }

        String list1Content = convertStringCollectionToString(plainCollection1);
        String list2Content = convertStringCollectionToString(plainCollection2);

        addResultToMap(changesMap, objIdentifier, parentField, field,
            !list1Content.isEmpty() ? list1Content : null, !list2Content.isEmpty() ? list2Content : null, field);
    }

    private String convertStringCollectionToString(Collection<String> stringCollection) {
        return stringCollection.stream().map(Object::toString).collect(Collectors.joining(LIST_DELIMITER));
    }

    private Collection<String> convertObjectListToStringList(Collection<Object> objList, Field field) throws IllegalAccessException {
        List<String> result = new ArrayList<>();

        if (isNull(objList) || objList.isEmpty()) {
            return result;
        }

        for (Object obj : objList) {
            String resultStr = getPlainValue(obj, field);

            if (!isNull(resultStr)) {
                result.add(resultStr);
            }
        }

        return result;
    }

    private String getPlainValue(Object objToGetFieldValue, Field field) throws IllegalAccessException {

        if (objToGetFieldValue.getClass().isPrimitive() || objToGetFieldValue.getClass().equals(String.class)) {
            return objToGetFieldValue.toString();
        }

        List<Field> fields = getAllFields(new ArrayList<>(), objToGetFieldValue.getClass());
        Field identifierField = getIdentifierField(fields, objToGetFieldValue);

        if (isNull(identifierField)) {
            throw new EcertBYAuditingException(NO_ANNOTATED_LIST_FIELD_ERROR);
        }

        identifierField.setAccessible(true);
        Object identifierValue = identifierField.get(objToGetFieldValue);

        if (isNull(identifierValue)) {
            throw new EcertBYAuditingException(IDENTIFIER_FIELD_WITH_EMPTY_VALUE_ERROR);
        }

        return handleSpecialCases(objToGetFieldValue, identifierValue.toString(), field);
    }

    /**
     * This is the only non-generic implementation that is needed to handle some special cases, where
     * the value does not exist in the required form inside the compared objects.
     * @param parentObj - the parent object
     * @param identifier - the identifier of the object
     * @return - The same identifier if no special case, or the identifier modified as required.
     */
    private String handleSpecialCases(Object parentObj, String identifier, Field field) {

        if (parentObj instanceof UserAuthorityCompanyDTO casted) {
            Optional<UserGroup> valueOpt = userGroupRepository.findById(casted.getUserGroupId());

            if (valueOpt.isPresent()) {
                return String.format(SECONDARY_VALUE_REPRESENTATION,
                    htmlEscape(identifier),
                    messages.get("role"),
                    htmlEscape(valueOpt.get().getDescription()));
            }
        }

        String userDetailName = checkIfUserComparison(field, identifier);

        if (!isNull(userDetailName)) {
            return userDetailName;
        }

        return identifier;
    }

    private String checkIfUserComparison(Field field, String identifier) {
        // Special case where the identifier contains a username that needs to be translated to a
        // more human-readable form.
        if (!isNull(field) && field.isAnnotationPresent(AuditFirstNameLastName.class)) {
            UserDetail user = userDetailRepository.findByUserUsernameIgnoreCase(identifier);

            if (!isNull(user)) {
                return user.getFirstName().trim() + " " + user.getLastName().trim();
            }
        }

        return null;
    }

    /**
     * This function checks the existence of a field inside a List of fields and returns it if exists.
     * Ideally, every comparableDTO should have a field annotated with @AuditIdentifier to determine how
     * to compare it if found in a collection of objects.
     * To prevent errors if such field does not exist, two fallbacks are also checked for "name" or "id" field.
     * In case an object is provided, it is checked to see if the given object has any value, in the given field.
     * @param fields - The set of fields to search for identifiers.
     * @param objToGetFieldValue - The object which might be examined if contains value for the selected field.
     *                              In case on null, the value check is skipped.
     * @return - The field (if found), null otherwise.
     * @throws IllegalAccessException - Required exception for field.get().
     */
    private Field getIdentifierField(List<Field> fields, Object objToGetFieldValue) throws IllegalAccessException {
        return getIdentifierWithValueCheck(objToGetFieldValue,
            fields.stream().filter(field -> field.isAnnotationPresent(AuditIdentifier.class)).findFirst().orElse(null),
            fields.stream().filter(field -> "name".equalsIgnoreCase(field.getName())).findFirst().orElse(null),
            fields.stream().filter(field -> "id".equalsIgnoreCase(field.getName())).findFirst().orElse(null)
            );
    }

    private Field getIdentifierWithValueCheck(Object objToGetFieldValue, Field... fields) throws IllegalAccessException {
        for (Field field : fields) {
            if (!isNull(field)) {
                if (!isNull(objToGetFieldValue)) {
                    field.setAccessible(true);
                    if (!isNull(field.get(objToGetFieldValue))) {
                        return field;
                    }
                } else {
                    return field;
                }
            }
        }
        return null;
    }

    private void addResultToMap(Map<String, List<String>>changesMap, String objIdentifier,
        String parentObjectIdentifier, Field field, Object value1, Object value2, Field parentField) {

        if (isNull(changesMap.get(objIdentifier))) {
            changesMap.put(objIdentifier, new ArrayList<>());
        }

        changesMap.get(objIdentifier)
            .add(String.format(VALUE_REPRESENTATION,
                !isNull(parentObjectIdentifier) ? parentObjectIdentifier : getFieldName(field),
                getStringValue(value1, parentField),
                getStringValue(value2, parentField)
            ));
    }

    private String getStringValue(Object value, Field field) {
        if (isNull(value)) {
            return EMPTY_OBJECT;
        }
        
        if (Boolean.class.isAssignableFrom(value.getClass())) {
            return htmlEscape(((Boolean) value) ? messages.get("yes") : messages.get("no"));
        }
        if (Enum.class.isAssignableFrom(value.getClass())) {
            return getFieldTranslation(value.toString());
        }
        String userDetailName = checkIfUserComparison(field, value.toString());

        if (!isNull(userDetailName)) {
            return htmlEscape(userDetailName);
        }

        return Pattern.compile("<[^>]+>").matcher(value.toString()).find() ? value.toString() : htmlEscape(value.toString());
    }

    private String getFieldName(Field field) {
        String translationKey = field.getName();

        if (field.isAnnotationPresent(AuditTranslationKey.class)) {
            String keyFromAnnotation = field.getAnnotation(AuditTranslationKey.class).key();

            if (!isNull(keyFromAnnotation)) {
                translationKey = keyFromAnnotation;
            }
        }

        return getFieldTranslation(translationKey);
    }

    private String getFieldTranslation(String translationKey) {
        String translation = messages.get(translationKey);
        return !isNull(translation) ? translation : translationKey;
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isAnnotationPresent(AuditIgnore.class)).toList());

        if (!isNull(type.getSuperclass())) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

}
