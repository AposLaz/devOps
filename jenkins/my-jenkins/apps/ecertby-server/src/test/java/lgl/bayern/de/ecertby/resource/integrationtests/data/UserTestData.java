package lgl.bayern.de.ecertby.resource.integrationtests.data;

import lgl.bayern.de.ecertby.dto.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class UserTestData  extends BaseTestData {
    /**
     * Populates the user list with as many users are needed for the tests, without saving them to the repository.<br>
     * Explicitly save to the repository inside a test (when needed) to avoid double-entry errors.
     */
    public List<UserDetailDTO> populateUsers() {
        List<UserDetailDTO> users = new ArrayList<>();
        int REQUIRED_USERS_NUMBER = 6;
        for (int i = 0; i < REQUIRED_USERS_NUMBER; i++) {
            UserDetailDTO userDetailDTO = initializeUserDetailDTO("fn"+(i+1),"ln"+(i+1),"user"+(i+1)+"@test.test");
            users.add(userDetailDTO);
        }
        return users;
    }

    /**
     * Creates a user.
     */
    @NotNull
    public UserDetailDTO initializeUserDetailDTO(String firstName, String lastName, String email) {
        UserDetailDTO userDetailDTO = new UserDetailDTO();
        userDetailDTO.setFirstName(firstName);
        userDetailDTO.setLastName(lastName);
        userDetailDTO.setEmail(email);
        userDetailDTO.setUsername("company_user");
        OptionDTO userType = new OptionDTO();
        userType.setId("COMPANY_USER");
        userDetailDTO.setUserType(userType);
        userDetailDTO.setActive(true);
        OptionDTO department = new OptionDTO();
        department.setName("Lebende Tiere");
        department.setId("bba64051-f49a-44d9-ac99-d4b2d4775e6f");
        SortedSet<OptionDTO> departmentSet = new TreeSet<>();
        departmentSet.add(department);
        userDetailDTO.setDepartment(departmentSet);

        return userDetailDTO;
    }
}
