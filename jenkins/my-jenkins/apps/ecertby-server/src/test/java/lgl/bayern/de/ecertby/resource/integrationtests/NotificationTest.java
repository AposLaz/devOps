package lgl.bayern.de.ecertby.resource.integrationtests;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.NotificationDTO;
import lgl.bayern.de.ecertby.dto.TemplateDTO;
import lgl.bayern.de.ecertby.model.util.NotificationStatus;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import lgl.bayern.de.ecertby.resource.NotificationResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.NotificationTestData;
import lgl.bayern.de.ecertby.resource.integrationtests.data.TemplateTestData;
import lgl.bayern.de.ecertby.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.Serializers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class NotificationTest extends BaseTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationResource notificationResource;
    @NotNull
    private List<NotificationDTO> notificationDTOS = new ArrayList<>();

    @NotNull
    private NotificationTestData notificationTestData = new NotificationTestData();

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
      notificationDTOS = notificationTestData.populateNotifications();
    }

    @Test
    @DisplayName("Gets Notification - Success")
    void testGetNotification() throws Exception {
        loginAsAdmin();
        NotificationDTO savedNotificationDTO = notificationService.saveNotification(notificationDTOS.get(0));
        mockMvc.perform(get("/notification/{id}", savedNotificationDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(notificationDTOS.get(0).getTitle()));
    }

    @Test
    @DisplayName("Finds all Notifications as Page NotificationDTO - Success")
    void testFindAllNotificationsAsPageNotificationDTO() throws Exception {
        loginAsAdmin();
        notificationService.saveNotification(notificationDTOS.get(1));
        mockMvc.perform(get("/notification")
                        .param("title", "TestNotification2")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "title,asc")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].title", equalTo(notificationDTOS.get(1).getTitle())));
    }
    @Test
    @DisplayName("Creates Notification - Success")
    @Order(1)
    void testCreateNotificationSuccess() throws Exception {
        loginAsAdmin();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String notificationAsJson = mapper.writeValueAsString(notificationDTOS.get(2));

        mockMvc.perform(post("/notification/create")
                        .content(notificationAsJson).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Update Notification - Success")
    void testUpdateNotificationSuccess() throws Exception {
        loginAsAdmin();
        NotificationDTO savedNotificationDTO = notificationService.saveNotification(notificationDTOS.get(4));

        savedNotificationDTO.setTitle("Updated Title");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String notificationAsJson = mapper.writeValueAsString(savedNotificationDTO);
        mockMvc.perform(post("/notification/update")
                        .content(notificationAsJson).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertEquals("Updated Title",notificationService.findById(savedNotificationDTO.getId()).getTitle());
    }

    @Test
    @DisplayName("Deactivates Notification - Success")
    void testDeactivateNotificationSuccess() throws Exception {
        loginAsAdmin();
        NotificationDTO savedNotificationDTO = notificationService.saveNotification(notificationDTOS.get(6));

        mockMvc.perform(patch("/notification/{id}/activate/{isActive}", savedNotificationDTO.getId(), false)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertFalse(notificationService.findById(savedNotificationDTO.getId()).isActive());
    }

    @Test
    @DisplayName("Activates Notification - Success")
    void testActivateNotificationSuccess() throws Exception {
        loginAsAdmin();
        NotificationDTO notificationDTO = notificationService.saveNotification(notificationDTOS.get(7));
        notificationDTO.setActive(false);
        NotificationDTO savedNotificationDTO = notificationService.saveNotification(notificationDTO);

        mockMvc.perform(patch("/notification/{id}/activate/{isActive}", savedNotificationDTO.getId(), true)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertTrue(notificationService.findById(savedNotificationDTO.getId()).isActive());
    }

    @Test
    @DisplayName("Publish Notification - Success")
    void testPublishNotificationSuccess() throws Exception {
        loginAsAdmin();
        NotificationDTO notificationDTO = notificationService.saveNotification(notificationDTOS.get(8));

        mockMvc.perform(patch("/notification/{id}/publish", notificationDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertEquals( NotificationStatus.PUBLISHED_NOTIFICATION,notificationService.findById(notificationDTO.getId()).getStatus());
    }
    @Test
    @DisplayName("Delete Notification - Success")
    void testDeleteNotificationSuccess() throws Exception {
        loginAsAdmin();
        NotificationDTO notificationDTO = notificationService.saveNotification(notificationDTOS.get(9));

        mockMvc.perform(delete("/notification/{id}", notificationDTO.getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());
        assertThrows(QDoesNotExistException.class, () -> {
            notificationService.findById(notificationDTO.getId());
        });
    }
}
