package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceAccess;
import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import com.eurodyn.qlack.util.querydsl.EmptyPredicateCheck;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.model.CompanyProfile;
import lgl.bayern.de.ecertby.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static lgl.bayern.de.ecertby.config.AppConstants.Operations.*;

@Validated
@RestController
@RequestMapping("profile")
@RequiredArgsConstructor
@Transactional
public class CompanyProfileResource {
    private final CompanyProfileService companyProfileService;

    @EmptyPredicateCheck
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not retrieve company profiles list.")
    @PostMapping(path= "update" , consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { EDIT_PROFILE, VIEW_PROFILE }
    )
    public Page<CompanyProfileDTO> findAll(@QuerydslPredicate(root = CompanyProfile.class) Predicate predicate,
                                           Pageable pageable,@RequestParam(name = "companyId", required = false) String companyId ,
                                           @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyProfileService.findAll(predicate, pageable );
    }


    @PostMapping(path= "create" , consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { NEW_PROFILE }
    )
    public void create(@Valid @RequestBody CompanyProfileDTO companyProfileDTO ) {
        companyProfileService.saveProfile(companyProfileDTO);
    }


    @PostMapping(path= "update" , consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            operations = { EDIT_PROFILE }
    )
    public CompanyProfileDTO update(@Valid @RequestBody CompanyProfileDTO companyProfileDTO) {
        return  companyProfileService.saveProfile(companyProfileDTO);
    }
    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not fetch company profile.")
    @ResourceAccess(
            operations = { VIEW_PROFILE }
    )
    public CompanyProfileDTO get(@PathVariable String id,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyProfileService.findProfile(id,selectionFromDD);
    }

    @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete company profile.")
    @ResourceAccess(
            operations = { DELETE_PROFILE }
    )
    public void delete(@PathVariable String id, @RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        companyProfileService.deleteProfileById(id,selectionFromDD);
    }

    @PatchMapping(path = "{id}/activate/{isActive}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not update company profile.")
    @ResourceAccess(
            operations = { ACTIVATE_PROFILE }
    )
    public boolean activate(@PathVariable String id, @PathVariable boolean isActive ,@RequestParam(value = "selectionFromDD", required = false) @ResourceId String selectionFromDD) {
        return companyProfileService.activateProfile(isActive,id,selectionFromDD);
    }
}
