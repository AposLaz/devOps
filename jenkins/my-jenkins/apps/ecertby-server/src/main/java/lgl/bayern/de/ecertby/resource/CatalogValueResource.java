package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.OperationAccess;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.service.CatalogValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.EDIT_CATALOG;
import static lgl.bayern.de.ecertby.config.AppConstants.Operations.VIEW_CATALOG;

@Validated
@RestController
@RequestMapping("catalog-value")
@RequiredArgsConstructor
@Transactional
public class CatalogValueResource {
    private final CatalogValueService catalogValueService;
    @GetMapping(path = "catalog/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch catalog values.")
    @OperationAccess(
            operations = { VIEW_CATALOG }
    )
    public Page<CatalogValueDTO> getCatalogValuesByCatalog(@QuerydslPredicate(root = CatalogValue.class) Predicate predicate,
                                                           Pageable pageable,
                                                           @PathVariable String id) {
        return catalogValueService.getCatalogValuesByCatalog(id, predicate, pageable);
    }

    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch catalog value.")
    @OperationAccess(
            operations = { VIEW_CATALOG }
    )
    public CatalogValueDTO getCatalogValue(@PathVariable String id) {
        return catalogValueService.findById(id);
    }

    @PostMapping(path="update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { EDIT_CATALOG }
    )
    public void updateCatalogValue(@Valid @RequestBody CatalogValueDTO catalogValueDTO) {
        catalogValueService.createOrUpdateValue(catalogValueDTO);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationAccess(
            operations = { EDIT_CATALOG }
    )
    public void deleteCatalogValue(@PathVariable String id) {
        catalogValueService.deleteValue(id);
    }
}
