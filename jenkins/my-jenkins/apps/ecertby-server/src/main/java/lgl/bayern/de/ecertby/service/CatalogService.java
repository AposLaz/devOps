package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.exceptions.CsvException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.CatalogDTO;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.CatalogMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.repository.CatalogRepository;
import lgl.bayern.de.ecertby.repository.CatalogValueRepository;
import lgl.bayern.de.ecertby.utility.CSVUtils;
import lgl.bayern.de.ecertby.validator.CatalogValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CatalogService extends BaseService<CatalogDTO, Catalog, QCatalog> {
    CatalogMapper catalogMapper = Mappers.getMapper(CatalogMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final CatalogValueService catalogValueService;
    private final AuditService auditService;
    private final SecurityService securityService;

    private final CatalogValueRepository catalogValueRepository;
    private final CatalogRepository catalogRepository;

    private final CatalogValidator catalogValidator;

    private static final String DEFAULT_ID_HEADER = "ID";
    private static final String DEFAULT_DATA_HEADER = "Wert";

    public Page<CatalogValueDTO> getCatalogValuesByCatalog(String catalogId, Predicate predicate, Pageable pageable) {
        catalogValidator.validateViewRequest(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), catalogId);

        BooleanBuilder finalPredicate = new BooleanBuilder().and(predicate);
        finalPredicate.and(
                QCatalogValue.catalogValue.catalog.id.eq(catalogId)
        );
        return catalogValueService.findAll(finalPredicate, pageable);
    }

    public void createCatalog(MultipartFile file) throws IOException, CsvException {
        UserDetailDTO loggedInUserDTO = securityService.getLoggedInUserDetailDTO();
        if (!catalogValidator.validateEditRequest(loggedInUserDTO.getRole())) {
            log.info(LOG_PREFIX + "User with id : {} has no rights to create a catalog.", loggedInUserDTO.getId());
            throw new NotAllowedException("Catalog cannot be created.");
        }

        List<EcertBYErrorException> errors = new ArrayList<>();
        catalogValidator.validateCatalogFile(file, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for creating catalog.", new EcertBYGeneralException(errors));
        }
        // Setup new catalog
        CatalogDTO catalog = new CatalogDTO();
        catalog.setName(FilenameUtils.removeExtension(file.getOriginalFilename()));
        catalog.setCreatedOn(Instant.now());
        catalog.setMandatory(false);
        // Check if catalog is valid before saving
        catalogValidator.validateCatalog(catalog);
        // Parse CSV file & validate contents to CatalogValue list
        CsvToBean<CatalogValueDTO> ctb = CSVUtils.getCatalogValueCtB(file);
        final List<CatalogValueDTO> catalogValueList = ctb.parse();
        for (CatalogValueDTO c : catalogValueList) {
            catalogValidator.validateUploadedCatalogValue(c);
        }
        // Save catalog and values
        CatalogDTO savedCatalog = this.save(catalog);
        for (CatalogValueDTO c : catalogValueList) {
            c.setId(null);
            c.setCatalog(savedCatalog);
        }
        catalogValueService.saveAll(catalogValueList);
        // Log catalog creation
        log.info(LOG_PREFIX + "New catalog with id {} successfully created by user with id : {}.", savedCatalog.getId(), securityService.getLoggedInUserDetailId());
        auditService.saveCatalogAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedCatalog);
    }

    public void editCatalog(String catalogId, MultipartFile file) throws IOException, CsvException {
        UserDetailDTO loggedInUserDTO = securityService.getLoggedInUserDetailDTO();
        if (!catalogValidator.validateEditRequest(loggedInUserDTO.getRole())) {
            log.info(LOG_PREFIX + "User with id : {} has no rights to overwrite the catalog with id : {}.", loggedInUserDTO.getId(), catalogId);
            throw new NotAllowedException("Catalog cannot be overwritten.");
        }

        List<EcertBYErrorException> errors = new ArrayList<>();
        catalogValidator.validateCatalogFile(file, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for replacing catalog.", new EcertBYGeneralException(errors));
        }
        CsvToBean<CatalogValueDTO> ctb = CSVUtils.getCatalogValueCtB(file);
        List<CatalogValueDTO> addedValues = ctb.parse();
        CatalogDTO catalog = findById(catalogId);
        for (CatalogValueDTO value : addedValues) {
            Optional<CatalogValue> oldValue = catalogValueRepository.findById(value.getId());
            // An old value is edited or a new one without id is added
            if (oldValue.isPresent() || isNewAndUnique(value)) {
                value.setCatalog(catalog);
                catalogValueService.save(value);
            } else {
                errors.add(new EcertBYErrorException("error_catalog_value_invalid_id", "error_catalog_value_invalid_id", null, null, null, true));
                throw new QCouldNotSaveException("Errors for replacing catalog.", new EcertBYGeneralException(errors));
            }
        }
        // Log catalog update
        log.info(LOG_PREFIX + "Catalog with id {} successfully updated by user with id : {}.", catalogId, securityService.getLoggedInUserDetailId());
        auditService.saveReplaceCatalogAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), catalog, file.getOriginalFilename());
    }

    private boolean isNewAndUnique(CatalogValueDTO value) {
        // Values with id are already accepted, if their id matches an existing one
        if (!value.getId().isEmpty()) return false;
        // Return true if no other value with the same data exist
        return !catalogValueRepository.existsByData(value.getData());
    }

    public ResponseEntity<InputStreamResource> downloadCatalog(String catalogId) throws IOException {
        catalogValidator.validateViewRequest(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), catalogId);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        // Add BOM for UTF-8 encoding
        writer.write('\ufeff');
        final StringBuilder stringBuilder = new StringBuilder();
        // Add header
        List<String> values = List.of(DEFAULT_ID_HEADER, DEFAULT_DATA_HEADER);
        CSVUtils.writeLine(stringBuilder, values);
        // Add entries
        for (CatalogValue value : catalogValueRepository.findAllByCatalog_IdOrderByData(catalogId)) {
            CSVUtils.writeLine(stringBuilder, List.of(value.getId(), value.getData()));
        }
        writer.append(stringBuilder.toString());
        writer.flush();
        writer.close();
        InputStream input = new ByteArrayInputStream(out.toByteArray());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(input));
    }

    public void deleteCatalog(String catalogId) {
        UserDetailDTO loggedInUserDTO = securityService.getLoggedInUserDetailDTO();
        if (!catalogValidator.validateEditRequest(loggedInUserDTO.getRole())) {
            log.info(LOG_PREFIX + "User with id : {} has no rights to delete the catalog with id : {}.", loggedInUserDTO.getId(), catalogId);
            throw new NotAllowedException("Catalog cannot be deleted.");
        }

        List<EcertBYErrorException> errors = new ArrayList<>();
        catalogValidator.validateDeleteCatalog(catalogId, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for deleting catalog.", new EcertBYGeneralException(errors));
        }
        // Delete all values of the catalog
        catalogValueService.deleteByIdIn(catalogValueRepository.findAllCatalogValueIdsFromCatalogId(catalogId));
        // Delete the catalog itself
        CatalogDTO deletedCatalog = this.deleteById(catalogId);
        // Log catalog deletion
        log.info(LOG_PREFIX + "Catalog with id {} successfully deleted by user with id : {}.", catalogId, securityService.getLoggedInUserDetailId());
        auditService.saveCatalogAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), deletedCatalog);
    }

    public List<OptionDTO> getAllCatalogs(){
       return  catalogMapper.mapToListOptionDTO(catalogRepository.findAll());
    }
}
