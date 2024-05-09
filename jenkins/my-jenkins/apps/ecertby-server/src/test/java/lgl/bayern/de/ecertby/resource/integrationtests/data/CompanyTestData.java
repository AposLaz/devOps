package lgl.bayern.de.ecertby.resource.integrationtests.data;

import com.eurodyn.qlack.fuse.aaa.service.ResourceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.CompanyMapper;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.CompanyService;
import lombok.Getter;

public class CompanyTestData extends BaseTestData {

    @Getter
    public static final String COMPANY_MAIN_USER_ROLE_NAME = "COMPANY_MAIN_USER";

    @Getter
    public static final String COMPANY_MAIN_USER_ROLE_ID = "585687bb-0a79-45ad-b3d8-936d6e62c5c8";

    @Getter
    public static final String DEFAULT_COMPANY_NAME = "DefaultCompany";

    @Getter
    public static final String COMPANY_USER_TYPE_ID = "COMPANY_USER";

    /**
     * Populates the company list with as many companies are needed for the tests, without saving them to the repository.<br>
     * Explicitly save to the repository inside a test (when needed) to avoid double-entry errors.
     */
    public List<CompanyDTO> populateCompanies(AuthorityDTO savedAuthorityDTO) {
        List<CompanyDTO> companies = new ArrayList<>();
        int REQUIRED_COMPANY_NUMBER = 10;
        for (int i = 0; i < REQUIRED_COMPANY_NUMBER; i++) {
            CompanyDTO companyDTO = initializeCompanyDTO("TestCompany"+(i+1),"user"+(i+1)+"@company.com", savedAuthorityDTO);
            companies.add(companyDTO);
        }
        return companies;
    }

    public List<CompanyProfileDTO> populateProfiles(CompanyDTO companyDTO) {
        List<CompanyProfileDTO> profiles = new ArrayList<>();
        int REQUIRED_PROFILE_NUMBER = 9;
        for (int i = 0; i < REQUIRED_PROFILE_NUMBER; i++) {
            CompanyProfileDTO profileDTO = initializeProfileDTO("TestCompany"+(i+1),"address"+(i+1), companyDTO,i);
            profiles.add(profileDTO);
        }
        return profiles;
    }

    /**
     *
     * @param companyDTO - The company DTO for the user
     * @param authorityService - The Authority service to save the company's authority
     * @param companyService - The Company service to save the company.
     * @param resourceService - Resource service to set user resources.
     * @param companyMapperInstance - Needed for resourceService.
     * @return A company user.
     */
    public UserDetailDTO initializeCompanyUserDetailDTO(CompanyDTO companyDTO,
        AuthorityService authorityService, CompanyService companyService, ResourceService resourceService,
        CompanyMapper companyMapperInstance) {

        UserDetailDTO companyUserDTO = new UserDetailDTO();
        companyUserDTO.setUsername("company_user");
        companyUserDTO.setEmail(companyDTO.getUserEmail());
        companyUserDTO.setUserType(getCompanyUserType());
        companyUserDTO.setActive(true);

        companyUserDTO.setRole(getCompanyMainUserRole());
        companyUserDTO.setPrimaryCompany(getPrimaryCompany(companyUserDTO, companyService, authorityService, resourceService, companyMapperInstance));

        return companyUserDTO;
    }

    public OptionDTO getCompanyMainUserRole() {
        return new OptionDTO().setName(COMPANY_MAIN_USER_ROLE_NAME).setId(COMPANY_MAIN_USER_ROLE_ID);
    }

    public CompanyDTO getPrimaryCompany(UserDetailDTO userDetailDTO,
        CompanyService companyService, AuthorityService authorityService,
        ResourceService resourceService, CompanyMapper companyMapperInstance) {

        CompanyDTO companyDTO = initializeCompanyDTO(DEFAULT_COMPANY_NAME, userDetailDTO.getEmail(), getAuthorityForCompany(authorityService));
        CompanyDTO savedCompanyDTO = companyService.save(companyDTO);
        resourceService.createResource(companyMapperInstance.companyDTOtoResourceDTO(savedCompanyDTO));

        return savedCompanyDTO;
    }

    public AuthorityDTO getAuthorityForCompany(AuthorityService authorityService) {
        AuthorityDTO authorityDTO = new AuthorityTestData().initializeAuthorityDTO("Authority for primary company",
            "randomAuthority" + new Random().nextInt(100) + "@randomMail.com");

        return authorityService.save(authorityDTO);
    }

    public OptionDTO getCompanyUserType() {
        return new OptionDTO().setId(COMPANY_USER_TYPE_ID);
    }
}
