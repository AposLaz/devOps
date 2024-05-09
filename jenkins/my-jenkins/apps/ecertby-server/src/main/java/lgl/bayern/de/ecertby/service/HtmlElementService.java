package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.util.data.optional.ReturnOptional;
import com.querydsl.core.BooleanBuilder;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.mapper.HtmlElementMapper;
import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.repository.HtmlElementRepository;
import lgl.bayern.de.ecertby.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class HtmlElementService extends BaseService<HtmlElementDTO, HtmlElement, QHtmlElement> {

    private final TemplateService templateService;

    private final HtmlElementMapper htmlElementMapper;

    private final HtmlElementRepository htmlElementRepository;

    private final TemplateRepository templateRepository;

    public Page<HtmlElementDTO> findByTemplateId(String templateId) {
        log.info(LOG_PREFIX + "Get html elements by template id...");
        BooleanBuilder finalPredicate = new BooleanBuilder()
                .and(QHtmlElement.htmlElement.template.id.eq(templateId));
        Pageable pageable = Pageable.unpaged();
        Page<HtmlElementDTO> response = findAll(finalPredicate, PageRequest.of(0, Integer.MAX_VALUE, Sort.by("sortOrder")));
        log.info(LOG_PREFIX + "Html Elements by templateid found.");

        return response;
    }

    public boolean saveHtmlElements(List<HtmlElementDTO> htmlElementDTOList, String templateId, String selectionFromDD) {
        TemplateDTO templateDTO = templateService.findById(templateId);
        Template template = templateRepository.findById(templateId).orElse(null);
        template.getHtmlElements().clear();
        for (int i = 0; i < htmlElementDTOList.size(); i++) {
            HtmlElementDTO htmlElementDTO = htmlElementDTOList.get(i);
            htmlElementDTO.setTemplate(templateDTO);
            htmlElementDTO.setSortOrder(i);
            HtmlElement htmlElement = htmlElementMapper.map(htmlElementDTO);
            template.getHtmlElements().add(htmlElement);
        }
        templateRepository.save(template);
        return true;
    }
}
