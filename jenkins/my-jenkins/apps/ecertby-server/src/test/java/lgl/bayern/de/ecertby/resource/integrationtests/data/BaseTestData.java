package lgl.bayern.de.ecertby.resource.integrationtests.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.CompanyProfileDTO;
import lgl.bayern.de.ecertby.dto.DocumentDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class BaseTestData {
    @Getter
    public String aaaUserIdAdmin ="bdc15070-d89f-497a-b64b-f9a6e71cf7e2";

    @Getter
    public String userDetailIdAdmin = "eb38afa4-de72-4c1c-b2ae-cbdff9b042b0";

    @Getter
    public String adminResourceName = "ADMIN_RESOURCE";

    @Getter
    public String adminName = "ecert_admin";

    @Getter
    public String aaaUserIdAuthority ="fcf09d73-a526-47d0-b07c-7898f10297df";

    @Getter
    public String userDetailIdAuthority = "32d32458-bf29-416c-8b62-e99a6245d2f2";

    @Getter
    public String aaaUserIdCompany = "03797da6-8a9f-496d-b513-3a4735172d03";

    @Getter
    public String userDetailIdCompany = "dce76e34-94af-4d22-ba8e-e73e7c84596e";

    @Getter
    public String roleMainCompanyUserId = "585687bb-0a79-45ad-b3d8-936d6e62c5c8";

    @Getter
    public String roleMainAuthorityUserId = "33037450-b70b-4102-bb92-20c65f822602";

    @Getter
    public String roleStandardAuthorityUserId = "dbfc4a65-d38f-4c66-9702-206e10fc2606";

    @Getter
    public String roleStandardCompanyUserId = "82fe2c2e-ca9f-47f5-acaa-701c2854b500";

    @Getter
    public static final String SYSTEM_ADMINISTRATOR_ROLE_ID = "4989ee35-4f41-4c4b-8720-2a8d3acb54e4";

    @Getter
    public static final String SYSTEM_ADMINISTRATOR_NAME = "SYSTEM_ADMINISTRATOR";

    @Getter
    public static final String ADMIN_USER_TYPE_ID = "ADMIN_USER";

    public List<OptionDTO> populateUniqueTargetCountryList(){
        List<OptionDTO> targetCountryList = new ArrayList<>();
        targetCountryList.add(initializeOption("Afghanistan", "9b001264-f03b-46c4-838b-9c64c6194a15",true));
        targetCountryList.add(initializeOption("Ägypten", "1bd9cbd5-3a6a-49b3-9a21-c1da66ad0ea1",true));
        targetCountryList.add(initializeOption("Åland", "da663b4d-0ea1-43e4-9698-c176d684e603",true));
        targetCountryList.add(initializeOption("Albanien", "b46107cf-4df5-4d68-ac3d-80beee2b4d8a",true));
        targetCountryList.add(initializeOption("Algerien", "c10270d8-351d-4130-afc4-fe7fb709eda9",true));
        targetCountryList.add(initializeOption("Amerikanische Jungferninseln", "0c79aa59-69c9-454a-b10c-00b853cafd9f",true));
        targetCountryList.add(initializeOption("Amerikanisch-Ozeanien","21510993-aa1a-4d83-a6af-90fc2eb18f74",true));
        targetCountryList.add(initializeOption("Amerikanisch-Samoa","c2d5d61c-15b4-4edf-be7f-f2e5bda77a03",true));
        targetCountryList.add(initializeOption("Andorra", "40bb326f-81a5-4aa9-aa76-c0da682c4eda",true));
        targetCountryList.add(initializeOption("Vereinigte Arabische Emirate", "9ee6fa1f-1d57-4d57-a7e3-34ae7b4b9fb8",true));
        return targetCountryList;
    }

    /**
     * Creates an option.
     * @param name the name field of the option.
     * @param id the id field of the option.
     * @return the created option as OptionDTO.
     */
    public OptionDTO initializeOption(String name, String id,boolean active) {
        OptionDTO optionDTO = new OptionDTO();
        optionDTO.setName(name);
        optionDTO.setId(id);
        optionDTO.setActive(active);
        return optionDTO;
    }

    /**
     * Creates the 'Lebende Tiere' department.
     * @return The created department as OptionDTO.
     */
    private OptionDTO initializeDepartment() {
        OptionDTO department = new OptionDTO();
        department.setName("Lebende Tiere");
        department.setId("bba64051-f49a-44d9-ac99-d4b2d4775e6f");
        return department;
    }

    /**
     * Creates an authority.
     * @param name the name of the authority.
     * @param userEmail the email of the main user.
     * @return the created authority as AuthorityDTO.
     */
    @NotNull
    public AuthorityDTO initializeAuthorityDTO(String name, String userEmail) {
        OptionDTO department = initializeDepartment();
        AuthorityDTO authorityDTO = new AuthorityDTO();
        authorityDTO.setName(name);
        authorityDTO.setAddress("Street 12");
        authorityDTO.setCommunityCode("1234,1235");
        authorityDTO.setActive(true);
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        departmentList.add(department);
        authorityDTO.setDepartment(departmentList);
        authorityDTO.setEmail(userEmail);
        authorityDTO.setEmailAlreadyExists(false);

        return authorityDTO;
    }

    /**
     * Creates a company.
     * @param name the name of the company.
     * @param userEmail the email of the main user.
     * @return the created company as a CompanyDTO.
     */
    @NotNull
    public CompanyDTO initializeCompanyDTO(String name, String userEmail, AuthorityDTO savedAuthorityDTO) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);

        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setName(name);
        companyDTO.setAddress("Street 12");
        companyDTO.setResponsibleAuthority(savedAuthorityDTO);
        companyDTO.setPreResponsibleAuthority(savedAuthorityDTO);
        companyDTO.setPostResponsibleAuthority(savedAuthorityDTO);
        companyDTO.setUserRole("COMPANY_USER");
        companyDTO.setActive(true);
        companyDTO.setDeleted(false);
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        departmentList.add(department);
        companyDTO.setDepartment(departmentList);

        companyDTO.setUserEmail(userEmail);
        companyDTO.setUserDepartment(departmentList);
        companyDTO.setEmailAlreadyExists(false);

        return companyDTO;
    }

    @NotNull
    public CompanyProfileDTO initializeProfileDTO(String name, String address , CompanyDTO companyDTO, int targetCountryIndex) {
        OptionDTO product = initializeOption("Bedarfsgegenstände", "facfb025-5661-42cb-bf26-a640c0242c2e",true);
       CompanyProfileDTO profile = new CompanyProfileDTO();
        profile.setCompanyId(companyDTO.getId());
        profile.setProfileName(name);
        SortedSet<OptionDTO> productSet = new TreeSet<>();
        productSet.add(product);
        profile.setProduct(productSet);
        SortedSet<OptionDTO> countrySet = new TreeSet<>();
        countrySet.add(populateUniqueTargetCountryList().get(targetCountryIndex));
        profile.setTargetCountry(countrySet);
        profile.setActive(true);
        profile.setAddress(address);

        return profile;
    }


    /**
     * Creates a template.
     * @param templateName The name of the template.
     * @param targetCountryIndex The index of the target country (corresponding to uniqueTargetCountryList).
     * @return The created template as a TemplateDTO.
     */
    @NotNull
    public TemplateDTO initializeTemplateDTO(String templateName, int targetCountryIndex) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);
        OptionDTO product = initializeOption("Bedarfsgegenstände", "facfb025-5661-42cb-bf26-a640c0242c2e",true);
        OptionDTO keyword = initializeOption("Kontamination", "6a37e313-d224-457c-92c1-9e9b2e7487df",true);

        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setTemplateName(templateName);
        templateDTO.setTargetCountry(populateUniqueTargetCountryList().get(targetCountryIndex));
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

        return templateDTO;
    }

    /**
     * Creates a certificate.
     * @param savedAuthorityDTO the DTO of the persisted issuing authority.
     * @param savedCompanyDTO the DTO of the persisted creator's company.
     * @param savedTemplateDTO the DTO of the persisted base template.
     * @return the created certificate as a CertificateDTO.
     */
    @NotNull
    public CertificateDTO initializeCertificateDTO(AuthorityDTO savedAuthorityDTO, CompanyDTO savedCompanyDTO, TemplateDTO savedTemplateDTO) {
        OptionDTO department = initializeOption("Lebende Tiere", "bba64051-f49a-44d9-ac99-d4b2d4775e6f",true);

        CertificateDTO certificateDTO = new CertificateDTO();
        certificateDTO.setTemplate(savedTemplateDTO);
        certificateDTO.setCompany(savedCompanyDTO);
        certificateDTO.setStatus(CertificateStatus.DRAFT);
        certificateDTO.setShippingDate(Instant.now());
        certificateDTO.setCompanyNumber("Test");
        certificateDTO.setIssuingAuthority(savedAuthorityDTO);
        SortedSet<OptionDTO> departmentList = new TreeSet<>();
        departmentList.add(department);
        certificateDTO.setDepartmentSet(departmentList);
        certificateDTO.setAssignedTeamSet(new HashSet<>(0));
        certificateDTO.setKeywordSet(savedTemplateDTO.getKeyword());
        certificateDTO.setCertificateFile(new DocumentDTO());
        certificateDTO.setCertificateAdditionalFiles(new ArrayList<>());

        return certificateDTO;
    }

    public OptionDTO getSystemAdministratorRole() {
        return new OptionDTO().setName(SYSTEM_ADMINISTRATOR_NAME).setId(SYSTEM_ADMINISTRATOR_ROLE_ID);
    }

    public OptionDTO getAdminUserType() {
        return new OptionDTO().setId(ADMIN_USER_TYPE_ID);
    }
}
