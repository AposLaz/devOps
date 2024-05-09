package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.opencsv.exceptions.CsvException;
import com.querydsl.core.types.Predicate;
import lgl.bayern.de.ecertby.dto.CatalogDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Catalog;
import lgl.bayern.de.ecertby.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@RestController
@RequestMapping("catalog")
@RequiredArgsConstructor
@Transactional
public class CatalogResource {
    private final CatalogService catalogService;

    /**
     * Find all catalogs with some criteria.
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested catalogs paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve catalog list.")
    @OperationAccess(
            operations = { VIEW_CATALOG_LIST }
    )
    public Page<CatalogDTO> findAll(@QuerydslPredicate(root = Catalog.class) Predicate predicate,
                                    Pageable pageable) {
        return catalogService.findAll(predicate, pageable);
    }

    @PostMapping(path = "{id}/overwrite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationAccess(
            operations = { EDIT_CATALOG }
    )
    public void editCatalog(@PathVariable String id,
                            @RequestPart("catalogCSV") MultipartFile file) throws IOException, CsvException {
        catalogService.editCatalog(id, file);
    }

    @PostMapping(path = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationAccess(
            operations = { NEW_CATALOG }
    )
    public void createCatalog(@RequestPart("catalogCSV") MultipartFile file) throws IOException, CsvException {
        catalogService.createCatalog(file);
    }

    @GetMapping(path = "download/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not download catalog.")
    @OperationAccess(
            operations = { VIEW_CATALOG }
    )
    public ResponseEntity<InputStreamResource> downloadCSV(@PathVariable String id) throws IOException {
        return catalogService.downloadCatalog(id);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { DELETE_CATALOG }
    )
    public void delete(@PathVariable String id) {
        catalogService.deleteCatalog(id);
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch catalog.")
    @OperationAccess(operations = {VIEW_CATALOG})
    public CatalogDTO get(@PathVariable String id) {
        return catalogService.findById(id);
    }

    @GetMapping(path = "findAllCatalogs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch catalogs.")
    @OperationAccess(operations = {VIEW_ATTRIBUTE, NEW_ATTRIBUTE, EDIT_ATTRIBUTE})
    public List<OptionDTO> getAllCatalogs() {
        return catalogService.getAllCatalogs();
    }
}
