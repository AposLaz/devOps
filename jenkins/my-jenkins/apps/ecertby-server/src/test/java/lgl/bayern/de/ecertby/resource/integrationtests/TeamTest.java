package lgl.bayern.de.ecertby.resource.integrationtests;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.inject.Inject;
import lgl.bayern.de.ecertby.config.ExceptionControllerAdvisor;
import lgl.bayern.de.ecertby.dto.TeamDTO;
import lgl.bayern.de.ecertby.dto.UserDetailDTO;
import lgl.bayern.de.ecertby.resource.TeamResource;
import lgl.bayern.de.ecertby.resource.integrationtests.data.TeamTestData;
import lgl.bayern.de.ecertby.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.IncludeTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IncludeTags("IntegrationTest")
@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TeamTest extends BaseTest {

    @Autowired
    private TeamResource teamResource;
    @Autowired
    private TeamService teamService;

    private MockMvc mockMvc;


    private List<TeamDTO> teamList = new ArrayList<>();

    @jakarta.validation.constraints.NotNull
    private TeamTestData teamTestData = new TeamTestData();


    @Inject
    private QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamResource)
                .setControllerAdvice(new ExceptionControllerAdvisor())
                .setCustomArgumentResolvers(querydslPredicateArgumentResolver, new PageableHandlerMethodArgumentResolver())
                .build();
        teamList = teamTestData.populateTeams();
    }


    @Test
    @DisplayName("Gets team - Success")
    void testGetTeamSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TeamDTO teamDTO = teamList.get(0);
        teamDTO.setAuthority(authorityUser.getPrimaryAuthority());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);

        mockMvc.perform(get("/team/{id}", savedTeamDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(teamList.get(0).getName()));
    }


    @Test
    @DisplayName("Finds all teams as Page TeamDTO - Success")
    void testFindAllTeamsAsPageTeamDTO() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();
        TeamDTO teamDTO = teamList.get(1);
        teamDTO.setAuthority(authorityUser.getPrimaryAuthority());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);

        mockMvc.perform(get("/team")
                        .param("name", savedTeamDTO.getName())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc")
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].name", equalTo(teamList.get(1).getName())));
    }


    @Test
    @DisplayName("Creates team - Success")
    void testCreateTeamSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TeamDTO teamDTO = teamList.get(2);
        teamDTO.setResourceId(authorityUser.getPrimaryAuthority().getId());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String teamAsJSON = mapper.writeValueAsString(teamDTO);

        mockMvc.perform(post("/team/create")
                        .content(teamAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Updates team - Success")
    void testUpdateTeamSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TeamDTO teamDTO = teamList.get(3);
        teamDTO.setAuthority(authorityUser.getPrimaryAuthority());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);
        savedTeamDTO.setResourceId(authorityUser.getPrimaryAuthority().getId());
        savedTeamDTO.setName(teamTestData.getTeamNameUpdate());
        savedTeamDTO.getUserDetailSet().forEach(user -> user.setActive(true));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String teamAsJSON = mapper.writeValueAsString(savedTeamDTO);

        mockMvc.perform(post("/team/create")
                        .content(teamAsJSON).accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertThat(teamService.findById(savedTeamDTO.getId()).getName()).isEqualTo(teamTestData.getTeamNameUpdate());
    }


    @Test
    @DisplayName("Deletes team - Success")
    void testDeleteTeamSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TeamDTO teamDTO = teamList.get(4);
        teamDTO.setAuthority(authorityUser.getPrimaryAuthority());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);

        mockMvc.perform(delete("/team/{id}", savedTeamDTO.getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        assertNotNull(savedTeamDTO.getId());
        String savedTeamDTOId = savedTeamDTO.getId();
        assertThrows(QDoesNotExistException.class, () -> teamService.findById(savedTeamDTOId));
    }

    @Test
    @DisplayName("Gets teams by Company id - Success")
    void testGetTeamsByCompanyIdSuccess() throws Exception {
        UserDetailDTO companyUser = loginAsCompany();

        TeamDTO teamDTO = teamList.get(5);
        teamDTO.setCompany(companyUser.getPrimaryCompany());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);

        mockMvc.perform(get("/team/findByCompany/{companyId}", companyUser.getPrimaryCompany().getId())
                        .param("selectionFromDD", companyUser.getPrimaryCompany().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(savedTeamDTO.getName()));
    }

    @Test
    @DisplayName("Gets teams by Authority id - Success")
    void testGetTeamsByAuthorityIdSuccess() throws Exception {
        UserDetailDTO authorityUser = loginAsAuthority();

        TeamDTO teamDTO = teamList.get(6);
        teamDTO.setAuthority(authorityUser.getPrimaryAuthority());
        TeamDTO savedTeamDTO = teamService.saveTeam(teamDTO);

        mockMvc.perform(get("/team/findByAuthority/{authorityId}", authorityUser.getPrimaryAuthority().getId())
                        .param("selectionFromDD", authorityUser.getPrimaryAuthority().getId())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(savedTeamDTO.getName()));
    }
}
