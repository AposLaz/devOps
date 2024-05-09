package lgl.bayern.de.ecertby.resource;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_AUTHORITY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_COMPANY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_PROFILE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_TEMPLATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_USER;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_AUTHORITY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_COMPANY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_PROFILE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_TEMPLATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.NEW_USER;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_AUTHORITIES_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_AUTHORITY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_CERTIFICATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_CERTIFICATES_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_COMPANIES_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_COMPANY;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_FEATURE_BOARD_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_PROFILE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_PROTOCOL;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_TEAMS_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_TEMPLATE;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_TEMPLATE_LIST;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_USER;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_USERS_LIST;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.service.CatalogValueService;
import lgl.bayern.de.ecertby.service.UtilitiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("utilities")
@RequiredArgsConstructor
@Transactional
public class UtilitiesResource {

    private final CatalogValueService catalogValueService;

    private final UtilitiesService utilitiesService;

    @Resource(name = "messages")
    private Map<String, String> messages;

    /**
     * Gets the catalog values filtered by the given catalog enum.
     * @param catalogName The given catalogue name to filter catalogue values.
     * @return The filtered list of catalogue values.
     */

    @GetMapping(path = "catalog/{catalogName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch catalogue list.")
    @ResourceAccess(
            operations = { NEW_AUTHORITY, EDIT_AUTHORITY, VIEW_AUTHORITIES_LIST, VIEW_AUTHORITY,
                    NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE,
                    NEW_COMPANY, EDIT_COMPANY, VIEW_COMPANY, VIEW_COMPANIES_LIST,
                    VIEW_TEAMS_LIST,
                    NEW_USER, EDIT_USER, VIEW_USER, VIEW_USERS_LIST,
                    NEW_PROFILE, EDIT_PROFILE, VIEW_PROFILE,
                    NEW_TEMPLATE, EDIT_TEMPLATE, VIEW_TEMPLATE, VIEW_TEMPLATE_LIST }
    )
    public List<OptionDTO> getCatalogList(@PathVariable String catalogName, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD){
        return catalogValueService.findByCatalogName(messages.get(catalogName));
    }

    /**
     * Gets the list of enum with the given enum name.
     * @param enumName The enum name to fetch data.
     * @return All values of the given enum name.
     */
    //certificate list, feature board list, protocol, template lsit
    @GetMapping(path = "enumList/{enumName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch enum list.")
    @ResourceAccess(
            operations = { VIEW_CERTIFICATES_LIST, VIEW_FEATURE_BOARD_LIST, VIEW_TEMPLATE_LIST}
    )
    public List<OptionDTO> getEnumList(@PathVariable String enumName, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return this.utilitiesService.getEnumList(enumName);
    }

    /**
     * Gets the list of enum with the given enum name.
     * @param enumName The enum name to fetch data.
     * @return All values of the given enum name.
     */
    @GetMapping(path = "enumListProtocol/{enumName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch enum list.")
    @OperationAccess(
            operations = { VIEW_PROTOCOL }
    )
    public List<OptionDTO> getEnumListProtocol(@PathVariable String enumName) {
        return this.utilitiesService.getEnumList(enumName);
    }

    @GetMapping(path = "groupListParent", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch filtered enum list.")
    @ResourceAccess(
            operations = { NEW_USER, EDIT_USER, VIEW_USER}
    )
    public List<OptionDTO> getParentGroups(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return utilitiesService.getParentGroupList();
    }

    /**
     * Gets group list of the system.
     * @return The list of groups.
     */
    @GetMapping(path = "groupList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch group list.")
    @ResourceAccess(
            operations = { NEW_USER, EDIT_USER, VIEW_USER, VIEW_USERS_LIST,
            NEW_COMPANY, EDIT_COMPANY, VIEW_COMPANY, VIEW_COMPANIES_LIST }
    )
    public List<OptionDTO> getGroupList(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD){
        return this.utilitiesService.getGroupList();
    }

    @GetMapping(path = "/targetCountries", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch target countries.")
    @ResourceAccess(
            operations = { NEW_PROFILE, EDIT_PROFILE, VIEW_PROFILE,
                    NEW_TEMPLATE, EDIT_TEMPLATE, VIEW_TEMPLATE, VIEW_TEMPLATE_LIST }
    )
    public List<OptionDTO> getTargetCountryList(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD){
        return this.utilitiesService.getTargetCountries(false);
    }

    /**
     * Returns all target countries with active, released and valid templates.
     * @return The list of countries as OptionDTOs.
     */
    @GetMapping(path = "/availableTargetCountries", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch available target countries.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST }
    )
    public List<OptionDTO> getAvailableTargetCountryList(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD){
        return this.utilitiesService.getTargetCountries(true);
    }

    /**
     * Gets all products associated with a country when in active, released and valid templates.
     * @param targetCountryId The id of the country.
     * @return The products as a list of OptionDTOs.
     */
    @GetMapping(path = "/availableProducts/{targetCountryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch available products.")
    @ResourceAccess(
            operations = { NEW_CERTIFICATE, EDIT_CERTIFICATE, VIEW_CERTIFICATE, VIEW_CERTIFICATES_LIST }
    )
    public List<OptionDTO> getAvailableProductList(@PathVariable String targetCountryId, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD){
        return this.utilitiesService.getAvailableProductList(targetCountryId);
    }

    @GetMapping()
    public void proceedToAuthentication() {
        log.trace("proceed to authentication");
        //first call to create jwt
    }
}
