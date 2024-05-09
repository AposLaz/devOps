package lgl.bayern.de.ecertby.resource.integrationtests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.aaa.dto.UserGroupHasOperationDTO;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.repository.UserDetailRepository;
import lgl.bayern.de.ecertby.resource.ObjectLockResource;
import lgl.bayern.de.ecertby.service.ObjectLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.IncludeTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ObjectLockTest extends BaseTest {
    private MockMvc mockMvc;

    @Autowired
    private ObjectLockService objectLockService;

    @Autowired
    private ObjectLockResource objectLockResource;

    @Autowired
    private UserDetailRepository userDetailRepository;

    private final String type = "COMPANY";
    private final String testObjectId = "598ff464-676b-11ee-8c99-0242ac120002";

    @BeforeEach
    void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(objectLockResource)
            .setControllerAdvice(new ExceptionControllerAdvisor())
            .build();
    }

    @Test
    @DisplayName("Gets object lock and deletes the old lock - Success")
    void testOverrideObjectLockWhenNoLockExistsSuccess() throws Exception {
        loginAsAdmin();

        mockMvc.perform(get("/objectlock/override/{testObjectId}", testObjectId)
                .param("type", type))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gets object lock without lock existing - Success")
    void testOverrideObjectLockWhenLockExistsSuccess() throws Exception {
        loginAsAdmin(baseTestData.getUserDetailIdAdmin());

        objectLockService.checkAndLockIfNeeded(testObjectId, type, true);

        mockMvc.perform(get("/objectlock/override/{testObjectId}", testObjectId)
                .param("type", type))
            .andExpect(status().isOk());

        // Check lock with different user
        loginAsAuthority(baseTestData.getUserDetailIdAuthority(), false);

        assertThrows(QCouldNotSaveException.class, () -> objectLockService.checkAndThrowIfLocked(testObjectId, ObjectType.COMPANY));

        loginAsAdmin(baseTestData.getUserDetailIdAdmin());

        //delete user lock
        mockMvc.perform(delete("/objectlock/delete/{testObjectId}", testObjectId)
                        .param("type", type))
                .andExpect(status().isOk());

        assertDoesNotThrow(() -> objectLockService.checkAndThrowIfLocked(testObjectId, ObjectType.COMPANY));

        //create lock, then delete all locks
        loginAsAdmin(baseTestData.getUserDetailIdAdmin());

        objectLockService.checkAndLockIfNeeded(testObjectId, type, true);

        assertDoesNotThrow(() -> objectLockService.deleteAllUserLocks(baseTestData.adminName));

    }

}
