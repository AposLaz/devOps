package lgl.bayern.de.ecertby.resource.integrationtests.data;

import com.eurodyn.qlack.fuse.aaa.service.ResourceService;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class AuthorityTestData  extends  BaseTestData{

    @Getter
    public static final String AUTHORITY_MAIN_USER_ROLE_NAME = "AUTHORITY_MAIN_USER";

    @Getter
    public static final String AUTHORITY_MAIN_USER_ROLE_ID = "33037450-b70b-4102-bb92-20c65f822602";

    public List<AuthorityDTO> populateAuthorities() {
        List<AuthorityDTO> authorityDTOS = new ArrayList<>();
        int REQUIRED_AUTHORITY_NUMBER = 12;
        for (int i = 0; i < REQUIRED_AUTHORITY_NUMBER; i++) {
            AuthorityDTO companyDTO = initializeAuthorityDTO("TestAuthority"+(i+1),"user"+(i+1)+"@authority.com");
            authorityDTOS.add(companyDTO);
        }
        return authorityDTOS;
    }

    public UserDetailDTO initializeAuthorityUserDetailDTO(String email, AuthorityService authorityService, ResourceService resourceService, AuthorityMapper authorityMapperInstance) {
        UserDetailDTO authorityUserDTO = new UserDetailDTO();
        authorityUserDTO.setUsername("authority_user");
        authorityUserDTO.setEmail(email);
        OptionDTO userType = new OptionDTO();
        userType.setId("AUTHORITY_USER");
        authorityUserDTO.setUserType(userType);
        authorityUserDTO.setRole(getAuthorityMainUserRole());
        authorityUserDTO.setActive(true);
        AuthorityDTO savedAuthorityDTOExisting = authorityService.save(initializeAuthorityDTO("Existing Authority", email));
        authorityUserDTO.setPrimaryAuthority(savedAuthorityDTOExisting);
        resourceService.createResource(authorityMapperInstance.authorityDTOtoResourceDTO(savedAuthorityDTOExisting));

        return authorityUserDTO;
    }
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

    public OptionDTO initializeDepartment() {
        OptionDTO department = new OptionDTO();
        department.setName("Lebende Tiere");
        department.setId("bba64051-f49a-44d9-ac99-d4b2d4775e6f");
        return department;
    }

    public OptionDTO getAuthorityMainUserRole() {
        return new OptionDTO().setName(AUTHORITY_MAIN_USER_ROLE_NAME).setId(AUTHORITY_MAIN_USER_ROLE_ID);
    }
}
