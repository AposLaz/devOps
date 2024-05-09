package lgl.bayern.de.ecertby.resource.integrationtests;

import static java.util.Objects.isNull;
import static org.mockito.Mockito.when;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import com.eurodyn.qlack.fuse.aaa.repository.UserGroupRepository;
import com.eurodyn.qlack.fuse.aaa.service.ResourceService;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import lgl.bayern.de.ecertby.dto.AuthorityDTO;
import lgl.bayern.de.ecertby.dto.CompanyDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.mapper.AuthorityMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.model.UserAuthority;
import lgl.bayern.de.ecertby.model.util.UserRole;
import lgl.bayern.de.ecertby.repository.CompanyRepository;
import lgl.bayern.de.ecertby.repository.UserAuthorityRepository;
import lgl.bayern.de.ecertby.resource.integrationtests.data.BaseTestData;
import lgl.bayern.de.ecertby.service.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import javax.sql.DataSource;

@ActiveProfiles("test")
@SpringBootTest()
@Testcontainers
public abstract class BaseTest  {

    @Autowired
    DataSource dataSource;

    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private AuthorityMapper authorityMapper;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserOperationService userOperationService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    SecurityService securityService;

    @Autowired
    private UserDetailMapper userDetailMapper;
    @Autowired
    UserAuthorityRepository userAuthorityRepository;

    @Autowired
    UserGroupRepository userGroupRepository;

    @NotNull
    BaseTestData baseTestData = new BaseTestData();


    public String loginAsAdmin(String userId) {
        UserDetailDTO loggedInUser = userDetailService.findById(userId == null ? baseTestData.getUserDetailIdAdmin() : userId);
        setupOperationAccessAuthentication(loggedInUser.getUsername(), baseTestData.getAaaUserIdAdmin());
        return baseTestData.getAdminResourceName();
    }

    public String loginAsAdmin() {
       return loginAsAdmin(null);
    }

    public UserDetailDTO loginAsAuthority(String userId, boolean withUserAuthority) {
        UserDetailDTO authorityUser = userDetailService.findById(userId == null ? baseTestData.getUserDetailIdAuthority() : userId);
        setupOperationAccessAuthentication(authorityUser.getUsername(), baseTestData.getAaaUserIdAuthority());
        // set a primary authority
        AuthorityDTO authorityDTO = baseTestData.initializeAuthorityDTO("DefaultAuthority", authorityUser.getEmail());
        Authority savedAuthority = authorityService.saveAuthority(authorityDTO);
        AuthorityDTO savedAuthorityDTO = authorityMapper.map(savedAuthority);
        authorityUser.setPrimaryAuthority(savedAuthorityDTO);
        if (withUserAuthority) {
            UserAuthority userAuthority = new UserAuthority();
            userAuthority.setAuthority(savedAuthority);
            userAuthority.setUserDetail(userDetailMapper.map(authorityUser));
            userAuthority.setUserGroup(userGroupRepository.findByName(UserRole.AUTHORITY_MAIN_USER.toString()));
            userAuthorityRepository.save(userAuthority);
        }
        // assign the appropriate operations
        String aaaUserId = authorityUser.getUser().getId();
        String loggedInAuthorityId = authorityUser.getPrimaryAuthority().getId();
        String loggedInAuthorityResourceId = resourceService.getResourceByObjectId(loggedInAuthorityId).getId();
        userOperationService.assignOperationsToUser(aaaUserId, authorityUser, UserRole.AUTHORITY_MAIN_USER.name(), loggedInAuthorityResourceId);
        return authorityUser;
    }

    public UserDetailDTO loginAsAuthority() {
        return loginAsAuthority(null, false);
    }

    public UserDetailDTO loginAsCompany(String userId) {
        UserDetailDTO companyUser = userDetailService.findById(userId == null ? baseTestData.getUserDetailIdCompany() : userId);
        setupOperationAccessAuthentication(companyUser.getUsername(), baseTestData.getAaaUserIdCompany());
        // to avoid saving the same company twice
        if (companyRepository.findByName("DefaultCompany") == null) {

            // set a primary company
            if (isNull(companyUser.getPrimaryCompany())) {
                AuthorityDTO authorityDTO = baseTestData.initializeAuthorityDTO("DefaultAuthority", "default@authority.com");
                AuthorityDTO savedAuthorityDTO = authorityMapper.map(authorityService.saveAuthority(authorityDTO));
                CompanyDTO companyDTO = baseTestData.initializeCompanyDTO("DefaultCompany", companyUser.getEmail(), savedAuthorityDTO);
                CompanyDTO savedCompanyDTO = companyService.saveCompanyAndLinkUser(companyDTO);
                companyUser.setPrimaryCompany(savedCompanyDTO);
                userDetailService.save(companyUser);
            }

            // assign the appropriate operations
            String aaaUserId = companyUser.getUser().getId();
            String loggedInCompanyId = companyUser.getPrimaryCompany().getId();
            String loggedInCompanyResourceId = resourceService.getResourceByObjectId(loggedInCompanyId).getId();

            userOperationService.assignOperationsToUser(aaaUserId, companyUser, UserRole.COMPANY_MAIN_USER.name(), loggedInCompanyResourceId);
        }
        return companyUser;
    }

    public UserDetailDTO loginAsCompany() {
       return loginAsCompany(null);
    }

    /**
     * Needed to pass the @OperationAccess annotation of QLACK.
     *
     * @param aaaId the aaaUser id of the role that executes the test
     */
    private DefaultSaml2AuthenticatedPrincipal setupOperationAccessAuthentication(String username, String aaaId) {
        // Create a real instance of DefaultSaml2AuthenticatedPrincipal
        DefaultSaml2AuthenticatedPrincipal saml2Principal = new DefaultSaml2AuthenticatedPrincipal(
                username.toLowerCase(),
                Collections.singletonMap("Role", Collections.singletonList("user"))
        );
        saml2Principal.setRelyingPartyRegistrationId("ecertby");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(saml2Principal);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        return saml2Principal;
    }


}
