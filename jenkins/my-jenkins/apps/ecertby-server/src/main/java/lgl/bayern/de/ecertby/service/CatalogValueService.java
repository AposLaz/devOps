package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.ws.rs.NotAllowedException;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.CatalogMapper;
import lgl.bayern.de.ecertby.mapper.CatalogValueMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.QCatalogValue;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.repository.CatalogValueRepository;
import lgl.bayern.de.ecertby.validator.CatalogValidator;
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

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class CatalogValueService extends BaseService<CatalogValueDTO, CatalogValue, QCatalogValue>{

    private final CatalogValueRepository catalogValueRepository;
    CatalogMapper catalogMapperInstance = Mappers.getMapper(CatalogMapper.class);
    CatalogValueMapper catalogValueMapperInstance = Mappers.getMapper(CatalogValueMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final AuditService auditService;
    private final SecurityService securityService;

    private final CatalogValidator catalogValidator;

    /**
     * Get the catalogue values for the given catalog name.
     * @param catalogName The catalog name to filter the catalogue values.
     * @return The list of catalogue values filtered by the given catalogue name.
     */
    public List<OptionDTO> findByCatalogName(String catalogName) {
        return catalogValueMapperInstance.mapToListOptionDTO(catalogValueRepository.findByCatalog_NameOrderByData(catalogName));
    }

    public Page<CatalogValueDTO> getCatalogValuesByCatalog(String catalogId, Predicate predicate, Pageable pageable) {
        catalogValidator.validateViewRequest(userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), catalogId);

        BooleanBuilder finalPredicate = new BooleanBuilder().and(predicate);
        finalPredicate.and(
                QCatalogValue.catalogValue.catalog.id.eq(catalogId)
        );
        return findAll(finalPredicate, pageable);
    }

    public void createOrUpdateValue(CatalogValueDTO catalogValueDTO) {
        UserDetailDTO loggedInUserDTO = securityService.getLoggedInUserDetailDTO();
        if (!catalogValidator.validateEditRequest(loggedInUserDTO.getRole())) {
            log.info(LOG_PREFIX + "User with id : {} has no rights to edit a catalog.", loggedInUserDTO.getId());
            throw new NotAllowedException("Catalog cannot be edited.");
        }

        CatalogValueDTO oldCatalogValueDTO = new CatalogValueDTO();
        if (catalogValueDTO.getId() == null) {
            log.info(LOG_PREFIX + "Adding catalog value to catalog with id : {}.", catalogValueDTO.getCatalog().getId());
        } else {
            oldCatalogValueDTO = findById(catalogValueDTO.getId());
            log.info(LOG_PREFIX + "Editing catalog value with id {} of catalog with id : {}.", catalogValueDTO.getId(), catalogValueDTO.getCatalog().getId());
        }

        CatalogValue newCatalogValue = new CatalogValue();
        catalogValueMapperInstance.map(catalogValueDTO, newCatalogValue);
        newCatalogValue.setCatalog(catalogMapperInstance.map(catalogValueDTO.getCatalog()));
        // Check if catalog value is valid before saving
        catalogValidator.validateCatalogValue(newCatalogValue, catalogValueDTO.getCatalog().getId());
        CatalogValue savedCatalogValue = catalogValueRepository.save(newCatalogValue);

        if (catalogValueDTO.getId() == null) {
            // Log create
            log.info(LOG_PREFIX + "Catalog value added successfully.");
            auditService.saveCatalogValueAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), catalogValueMapperInstance.map(savedCatalogValue));
        } else {
            // Log update
            log.info(LOG_PREFIX + "Catalog value edited successfully with id: {}.", catalogValueDTO.getId());
            auditService.saveCatalogValueAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), oldCatalogValueDTO, catalogValueDTO);
        }
    }

    public void deleteValue(String id) {
        List<EcertBYErrorException> errors = new ArrayList<>();
        catalogValidator.validateDeleteCatalogValue(id, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for deleting catalog value.", new EcertBYGeneralException(errors));
        }

        CatalogValueDTO deletedCatalogValueDTO = deleteById(id);
        // Log deletion
        log.info(LOG_PREFIX + "Catalog value with id {} deleted successfully.", id);
        auditService.saveCatalogValueAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), deletedCatalogValueDTO);
    }
}
