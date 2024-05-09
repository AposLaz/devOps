package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.Company;
import lgl.bayern.de.ecertby.service.CompanyService;
import lgl.bayern.de.ecertby.service.JwtUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@RestController
@RequestMapping("company")
@RequiredArgsConstructor
@Transactional
public class CompanyResource {
    private final CompanyService companyService;
    private final JwtUpdateService jwtUpdateService;

    /**
     * Find all companies with some criteria.
     * @param predicate The criteria given.
     * @param pageable  The selected page and sorting.
     * @return The requested companies paged and sorted.
     */
    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve companies list.")
    @ResourceAccess(
            operations = { VIEW_COMPANIES_LIST }
    )
    public Page<CompanyDTO> findAll(@QuerydslPredicate(root = Company.class) Predicate predicate,
                                    Pageable pageable,
                                    @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyService.findAllOrAuthorityCompanies(predicate, pageable, selectionFromDD);
    }

    /**
     * Save company.
     * @param companyDTO The object with the information of the company.
     */
    @PostMapping(path = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { NEW_COMPANY }
    )
    public CompanyDTO create(@Valid @RequestBody CompanyDTO companyDTO) {
        if (!companyDTO.isEmailAlreadyExists()) {
            return companyService.saveCompanyAndCreateUser(companyDTO);
        } else {
            return companyService.saveCompanyAndLinkUser(companyDTO);
        }
    }

    /**
     * Update company.
     * @param companyDTO The object with the information of the company.
     */
    @PostMapping(path="update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { EDIT_COMPANY }
    )
    public void update(@Valid @RequestBody CompanyDTO companyDTO) {
        companyService.editCompany(companyDTO);
    }

    /**
     * Get company.
     * @param id The id of the company.
     * @return The company with the given id, as CompanyDTO.
     */
    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch company.")
    @ResourceAccess(
            operations = { VIEW_COMPANY }
    )
    public CompanyDTO get(@PathVariable String id,
                                         @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyService.findCompany(id, selectionFromDD);
    }

    /**
     * Mark company as deleted, also deleting its users if only associated with the deleted company.
     * @param id The id of the company to mark as deleted.
     */
    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { DELETE_COMPANY }
    )
    public void delete(
        @PathVariable String id,
        // Required by Qlack
        @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        companyService.deleteCompany(id, selectionFromDD);
    }

    /**
     * Activates or deactivates company, also deactivating its users if only associated with the company during deactivation.
     * @param id The id of the company to activate/deactivate.
     * @param isActive The new active state.
     */
    @PatchMapping(path = "{id}/activate/{isActive}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { ACTIVATE_COMPANY }
    )
    public void activate(@PathVariable String id, @PathVariable boolean isActive,
        // Required by Qlack
        @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        companyService.activateCompany(id, isActive, selectionFromDD);
    }

    /**
     * Return all companies based on their activation state.
     * @param active The activation state of the returned companies.
     * @return The requested companies as a list of OptionDTOs.
     */
    @GetMapping(path = "findAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve company list.")
    @ResourceAccess(operations = { VIEW_TEAMS_LIST, NEW_USER})
    public List<OptionDTO> getAllCompanies(@RequestParam(name = "active", required = false) boolean active , @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyService.getAllCompanies();
    }

    /**
     * Return all companies the logged-in user is associated with.
     * @return The associated companies as a list of OptionDTOs.
     */
    @GetMapping(path = "findAllUserCompanies", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve user company list.")
    public List<OptionDTO> getUserCompanies(@RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyService.getUserCompanies();
    }

    @GetMapping(path = "isCompanyActive/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch user.")
    @ResourceAccess(
            operations = { EDIT_COMPANY , VIEW_COMPANY}
    )
    public Map<String,Boolean> isCompanyActive(@PathVariable String id, HttpServletRequest request, HttpServletResponse response, @RequestParam(name = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return jwtUpdateService.checkIfCompanyIsActiveAndUpdateJwt(id, request.getHeader(AppConstants.AUTHORIZATION),response);
    }
}
