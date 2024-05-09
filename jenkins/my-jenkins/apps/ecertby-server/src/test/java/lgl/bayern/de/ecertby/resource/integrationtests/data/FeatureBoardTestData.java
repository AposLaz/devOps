package lgl.bayern.de.ecertby.resource.integrationtests.data;


import com.eurodyn.qlack.fuse.fd.dto.ThreadMessageDTO;
import com.eurodyn.qlack.fuse.fd.util.ThreadStatus;
import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.Authority;
import lgl.bayern.de.ecertby.service.AuthorityService;
import lgl.bayern.de.ecertby.service.SecurityService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

public class FeatureBoardTestData extends BaseTestData {
    public List<ThreadMessageDTO> populateFeatureBoard() {
        List<ThreadMessageDTO> featureBoardThreads = new ArrayList<>();
        int REQUIRED_THREAD_NUMBER = 9;
        for (int i = 0; i < REQUIRED_THREAD_NUMBER; i++) {
            ThreadMessageDTO threadDTO = initializeThreadDTO("Title "+(i+1), "Body " +(i+1));
            featureBoardThreads.add(threadDTO);
        }
        return featureBoardThreads;
    }

    @NotNull
    public ThreadMessageDTO initializeThreadDTO(String title,String body) {
        ThreadMessageDTO dto = new FeatureBoardThreadDTO();
        dto.setTitle(title);
        dto.setBody(body);
        dto.setStatus(ThreadStatus.REQUESTED);
        dto.setStatusComment("Angefordert");
        return dto;
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
    private OptionDTO initializeDepartment() {
        OptionDTO department = new OptionDTO();
        department.setName("Lebende Tiere");
        department.setId("bba64051-f49a-44d9-ac99-d4b2d4775e6f");
        return department;
    }
}

