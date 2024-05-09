package lgl.bayern.de.ecertby.service;

import lgl.bayern.de.ecertby.dto.TargetCountryDTO;
import lgl.bayern.de.ecertby.model.TargetCountry;
import lgl.bayern.de.ecertby.model.QTargetCountry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class TargetCountryService extends BaseService<TargetCountryDTO, TargetCountry, QTargetCountry> {
}
