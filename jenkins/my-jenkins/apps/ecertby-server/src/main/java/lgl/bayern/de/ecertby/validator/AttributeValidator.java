package lgl.bayern.de.ecertby.validator;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.AttributeDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.repository.AttributeRepository;
import lgl.bayern.de.ecertby.repository.HtmlElementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;


/**
 * Validator for the Attribute.
 */
@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class AttributeValidator {

    private final AttributeRepository attributeRepository;
    private final HtmlElementRepository htmlElementRepository;

    private static final String ERROR_NAME_EXISTS = "error_name_exists";
    private static final String ERROR_ATTRIBUTE_ACTIVE = "error_attribute_active";

    public void validateAttribute(AttributeDTO attributeDTO) {
        List<EcertBYErrorException> errors = new ArrayList<>();

        // Validate if name already exists.
        nameExists(attributeDTO, errors);

        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for saving attribute", new EcertBYGeneralException(errors));
        }
    }


    private void nameExists(AttributeDTO attributeDTO, List<EcertBYErrorException> errors) {
        if (attributeDTO.getId() == null && attributeRepository.findByName(attributeDTO.getName()) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS, ERROR_NAME_EXISTS, "templateName", null, null, true));
        }
        if (attributeDTO.getId() != null && attributeRepository.findByNameAndIdNot(attributeDTO.getName(), attributeDTO.getId()) != null) {
            errors.add(new EcertBYErrorException(ERROR_NAME_EXISTS, ERROR_NAME_EXISTS, "templateName", null, null, true));
        }
    }

    public void validateDelete(String id, List<EcertBYErrorException> errors) {
        if (htmlElementRepository.existsByAttributeId(id)) {
            errors.add(new EcertBYErrorException(ERROR_ATTRIBUTE_ACTIVE, ERROR_ATTRIBUTE_ACTIVE, null, null, null, true));
        }
    }
}
