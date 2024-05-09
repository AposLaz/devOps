package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

public class TemplateTestData extends BaseTestData{

    @Getter
    List<OptionDTO> targetCountryList = new ArrayList<>();

    @Getter
    OptionDTO product = new OptionDTO();

    @Getter
    String templateNameUpdate = "TestTemplate updated";

    public List<TemplateDTO> populateTemplates() {
        List<TemplateDTO> templateList = new ArrayList<>();
        targetCountryList = populateUniqueTargetCountryList();

        for (int i = 0; i < targetCountryList.size(); i++) {
            TemplateDTO templateDTO = initializeTemplateDTO("TestTemplate "+(i+1), targetCountryList.get(i) );
            templateList.add(templateDTO);
        }
        return templateList;
    }

    @NotNull
    public TemplateDTO initializeTemplateDTO(String templateName, OptionDTO targetCountry) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);
        product = initializeOption("Bedarfsgegenstände", "facfb025-5661-42cb-bf26-a640c0242c2e",true);
        OptionDTO keyword = initializeOption("Kontamination", "6a37e313-d224-457c-92c1-9e9b2e7487df",true);

        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setTemplateName(templateName);
        templateDTO.setTargetCountry(targetCountry);
        templateDTO.setActive(true);
        templateDTO.setRelease(false);
        SortedSet<OptionDTO> departmentSet = new TreeSet<>();
        departmentSet.add(department);
        templateDTO.setDepartment(departmentSet);
        templateDTO.setProduct(product);
        SortedSet<OptionDTO> keywordSet = new TreeSet<>();
        keywordSet.add(keyword);
        templateDTO.setKeyword(keywordSet);
        templateDTO.setValidFrom(Instant.now());
        templateDTO.setTemplateFile(new DocumentDTO());
        templateDTO.setValidTo(Instant.now().plusSeconds(86400));
        templateDTO.setTemplateElementDTOSet(new HashSet<>());

        return templateDTO;
    }
}
