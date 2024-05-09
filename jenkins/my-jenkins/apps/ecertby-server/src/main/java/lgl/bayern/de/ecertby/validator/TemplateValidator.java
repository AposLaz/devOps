package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class TemplateValidator {

    private final TemplateRepository templateRepository;

    private static final String TEMPLATE_DTO = "templateDTO";

    private static final String ERROR_TARGET_COUNTRY_EXISTS = "error_target_country_product_exists_template";


    private static final String ERROR_TEMPLATE_NAME_EXISTS_TEMPLATE = "error_template_name_exists_template";

    /**
     * Validates the template save for all the business rules.
     * @param templateDTO The template.
     */
    public void validateTemplate(TemplateDTO templateDTO){
        List<EcertBYErrorException> errors = new ArrayList<>();
        // validate title to be unique.
        validateTemplateName(templateDTO, errors);
        // Validate combination of target country and product.
        validateTargetCountryProduct(templateDTO, errors);

        if(!errors.isEmpty()){
            throw new QCouldNotSaveException("Errors for template save action", new EcertBYGeneralException(errors));
        }
    }

    private void validateTemplateName(TemplateDTO templateDTO, List<EcertBYErrorException>  errors){
        if (templateDTO.getId() == null && templateRepository.findByTemplateName(templateDTO.getTemplateName()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TEMPLATE_NAME_EXISTS_TEMPLATE, ERROR_TEMPLATE_NAME_EXISTS_TEMPLATE, "templateName", TEMPLATE_DTO, null, true));
        }
        if (templateDTO.getId() != null && templateRepository.findByTemplateNameAndIdNot(templateDTO.getTemplateName(), templateDTO.getId()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TEMPLATE_NAME_EXISTS_TEMPLATE, ERROR_TEMPLATE_NAME_EXISTS_TEMPLATE, "templateName", TEMPLATE_DTO, null, true));
        }
    }

    private void validateTargetCountryProduct(TemplateDTO templateDTO, List<EcertBYErrorException> errors) {
        if (templateDTO.getId() == null && templateRepository.findByTargetCountryIdAndProductId(templateDTO.getTargetCountry().getId(), templateDTO.getProduct().getId()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TARGET_COUNTRY_EXISTS, ERROR_TARGET_COUNTRY_EXISTS, "targetCountry", TEMPLATE_DTO, null, true));
            errors.add(new EcertBYErrorException(ERROR_TARGET_COUNTRY_EXISTS, ERROR_TARGET_COUNTRY_EXISTS, "product", TEMPLATE_DTO, null, true));
        }
        if (templateDTO.getId() != null && templateRepository.findByTargetCountryIdAndProductIdAndIdNot(templateDTO.getTargetCountry().getId(), templateDTO.getProduct().getId(), templateDTO.getId()) != null) {
            errors.add(new EcertBYErrorException(ERROR_TARGET_COUNTRY_EXISTS, ERROR_TARGET_COUNTRY_EXISTS, "targetCountry", TEMPLATE_DTO, null, true));
            errors.add(new EcertBYErrorException(ERROR_TARGET_COUNTRY_EXISTS, ERROR_TARGET_COUNTRY_EXISTS, "product", TEMPLATE_DTO, null, true));
        }
    }
}
