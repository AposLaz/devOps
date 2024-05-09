package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.CatalogDTO;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.model.util.UserType;
import lgl.bayern.de.ecertby.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

/**
 * Validator for the Catalog.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class CatalogValidator {

    private final CatalogRepository catalogRepository;
    private final CatalogValueRepository catalogValueRepository;
    private final AuthorityRepository authorityRepository;
    private final CompanyRepository companyRepository;
    private final CompanyProfileProductRepository companyProfileProductRepository;
    private final CertificateRepository certificateRepository;
    private final TeamRepository teamRepository;
    private final TemplateRepository templateRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserAuthorityRepository userAuthorityRepository;

    private static final String ERROR_CATALOG_NAME_EXISTS = "error_catalog_name_exists";
    private static final String ERROR_CATALOG_VALUE_NAME_EXISTS = "error_catalog_value_name_exists";
    private static final String ERROR_CATALOG_VALUE_NAME_INVALID = "error_catalog_value_name_invalid";
    private static final String ERROR_CATALOG_HAS_IDS = "error_catalog_has_ids";
    private static final String ERROR_CATALOG_DELETE_MANDATORY = "error_catalog_delete_mandatory";
    private static final String ERROR_CATALOG_DELETE_ACTIVE = "error_catalog_delete_active";
    private static final String ERROR_CATALOG_VALUE_DELETE_ACTIVE = "error_catalog_value_delete_active";
    private static final String ERROR_CATALOG_VALUE_DELETE_LAST = "error_catalog_value_delete_last";
    private static final String ERROR_CATALOG_FILE_INVALID = "error_catalog_file_invalid";

    private static final String GENERAL_ERROR = "Errors for Saving Catalog.";
    private static final String CATALOG_VALUE_DTO = "catalogValueDTO";
    private static final String DEPARTMENT_CATALOG = "Fachbereich";
    private static final String PRODUCT_CATALOG = "Produkt";
    private static final String KEYWORD_CATALOG = "Schlagwort";
    private static final String AUTHORITY_ROLE_PROCESS = "Rolle im Prozess-Behörde";
    private static final String COMPANY_ROLE_PROCESS = "Rolle im Prozess-Betrieb";

    /**
     * Validates the catalog save for all the business rules.
     * @param catalogDTO The Catalog
     */
    public void validateCatalog(CatalogDTO catalogDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();
        // Validate if name already exists.
        catalogNameExists(catalogDTO.getName(), catalogDTO.getId(), errors);
        if(!errors.isEmpty()){
            throw new QCouldNotSaveException(GENERAL_ERROR, new EcertBYGeneralException(errors));
        }
    }

    public void validateCatalogValue(CatalogValue newCatalogValue, String catalogId) {
        List<EcertBYErrorException> errors = new ArrayList<>();
        if (newCatalogValue.getData().isEmpty() || newCatalogValue.getData().length() > 255) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_VALUE_NAME_INVALID, ERROR_CATALOG_VALUE_NAME_INVALID, "name", CATALOG_VALUE_DTO, null, true));
        }
        // Validate if name already exists in catalog.
        catalogValueNameExistsInCatalog(newCatalogValue.getId(), newCatalogValue.getData(), catalogId, errors);
        if(!errors.isEmpty()){
            throw new QCouldNotSaveException(GENERAL_ERROR, new EcertBYGeneralException(errors));
        }
    }

    public void validateUploadedCatalogValue(CatalogValueDTO newCatalogValue) {
        List<EcertBYErrorException> errors = new ArrayList<>();
        if (!newCatalogValue.getId().isEmpty()) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_HAS_IDS, ERROR_CATALOG_HAS_IDS, "name", CATALOG_VALUE_DTO, null, true));
        }
        if(!errors.isEmpty()){
            throw new QCouldNotSaveException(GENERAL_ERROR, new EcertBYGeneralException(errors));
        }
    }

    public void validateCatalogFile(MultipartFile file, List<EcertBYErrorException> errors) {
        boolean emptyFilename = StringUtils.isBlank(FilenameUtils.removeExtension(file.getOriginalFilename()));
        boolean wrongFormat = !"csv".equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()));
        if (emptyFilename || wrongFormat) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_FILE_INVALID, ERROR_CATALOG_FILE_INVALID, null, null, null, true));
        }
    }

    /**
     * Check if name already exists.
     * @param name The name to check.
     * @param catalogId The catalog id.
     * @param errors The errors list to update.
     */
    private void catalogNameExists(String name, String catalogId, List<EcertBYErrorException> errors) {
        if (catalogId == null && catalogRepository.findByName(name) != null) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_NAME_EXISTS, ERROR_CATALOG_NAME_EXISTS, "name", "catalogDTO", null, true));
        }
        if (catalogId != null && catalogRepository.findByNameAndIdNot(name, catalogId) != null) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_NAME_EXISTS, ERROR_CATALOG_NAME_EXISTS, "name", "catalogDTO", null, true));
        }
    }

    private void catalogValueNameExistsInCatalog(String valueId, String name, String catalogId, List<EcertBYErrorException> errors) {
        CatalogValue result = catalogValueRepository.findByDataAndCatalog_Id(name, catalogId);
        if (result != null && !result.getId().equals(valueId)) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_VALUE_NAME_EXISTS, ERROR_CATALOG_VALUE_NAME_EXISTS, "name", CATALOG_VALUE_DTO, null, true));
        }
    }

    public void validateDeleteCatalog(String id, List<EcertBYErrorException> errors) {
        if (catalogRepository.catalogIsMandatory(id)) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_DELETE_MANDATORY, ERROR_CATALOG_DELETE_MANDATORY, null, null, null, true));
        }
        boolean isKeywordCatalog = id.equals(catalogRepository.findIdByName(KEYWORD_CATALOG));
        boolean keywordsAreUsed = templateRepository.templateKeywordsExist() || certificateRepository.certificateKeywordsExist();
        if (
            (isKeywordCatalog && keywordsAreUsed) ||
            catalogRepository.catalogIsReferenced(id)
        ) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_DELETE_ACTIVE, ERROR_CATALOG_DELETE_ACTIVE, null, null, null, true));
        }
    }

    public void validateDeleteCatalogValue(String valueId, List<EcertBYErrorException> errors) {
        String catalogId = catalogValueRepository.findCatalogByCatalogValueId(valueId);
        int catalogSize = catalogRepository.countAllValuesById(catalogId);
        // If the value is the last, validate all that apply to catalog deletion
        if (catalogSize == 1) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_VALUE_DELETE_LAST, ERROR_CATALOG_VALUE_DELETE_LAST, null, null, null, true));
        }
        boolean isInDepartments = catalogId.equals(catalogRepository.findIdByName(DEPARTMENT_CATALOG));
        boolean isInProducts = catalogId.equals(catalogRepository.findIdByName(PRODUCT_CATALOG));
        boolean isInKeywords = catalogId.equals(catalogRepository.findIdByName(KEYWORD_CATALOG));
        boolean isInAuthorityRoleProcess = catalogId.equals(catalogRepository.findIdByName(AUTHORITY_ROLE_PROCESS));
        boolean isInCompanyRoleProcess = catalogId.equals(catalogRepository.findIdByName(COMPANY_ROLE_PROCESS));
        if (
            (isInDepartments && departmentValueIsUsed(valueId)) ||
            (isInProducts && productValueIsUsed(valueId)) ||
            (isInKeywords && keywordValueIsUsed(valueId)) ||
            (isInAuthorityRoleProcess && authorityRoleProcessValueIsUsed(valueId)) ||
            (isInCompanyRoleProcess && companyRoleProcessValueIsUsed(valueId))
        ) {
            errors.add(new EcertBYErrorException(ERROR_CATALOG_VALUE_DELETE_ACTIVE, ERROR_CATALOG_VALUE_DELETE_ACTIVE, null, null, null, true));
        }
    }

    public void validateViewRequest(UserDetail loggedInUser, String catalogId) {
        // Only an admin or support user can view the catalogs
        if (!loggedInUser.getUserType().equals(UserType.ADMIN_USER)) {
            log.info(LOG_PREFIX + "User with id : {} has no rights to view catalog with id {}.", loggedInUser.getId(), catalogId);
            throw new NotAllowedException("Catalog cannot be fetched.");
        }
    }

    public boolean validateEditRequest(OptionDTO role) {
        // Only an admin user can edit the catalogs
        return role.getName().equals("Systemadministrator");
    }

    private boolean departmentValueIsUsed(String valueId) {
        return authorityRepository.existsAuthorityDepartmentById(valueId) || companyRepository.existsCompanyDepartmentById(valueId) || certificateRepository.existsCertificateDepartmentById(valueId) || teamRepository.existsTeamDepartmentById(valueId) || templateRepository.existsTemplateDepartmentById(valueId) || userDepartmentRepository.existsUserDepartmentById(valueId);
    }
    private boolean productValueIsUsed(String valueId) {
        return companyProfileProductRepository.existsCompanyProfileProductById(valueId) || templateRepository.existsTemplateProductById(valueId);
    }
    private boolean keywordValueIsUsed(String valueId) {
        return templateRepository.existsTemplateKeywordById(valueId) || certificateRepository.existsCertificateKeywordById(valueId);
    }
    private boolean authorityRoleProcessValueIsUsed(String valueId) {
        return userAuthorityRepository.existsByRoleInProcessId(valueId);
    }
    private boolean companyRoleProcessValueIsUsed(String valueId) {
        return userCompanyRepository.existsByRoleInProcessId(valueId);
    }
}
