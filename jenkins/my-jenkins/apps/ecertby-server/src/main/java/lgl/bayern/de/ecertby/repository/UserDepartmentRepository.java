package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.QUserDepartment;
import lgl.bayern.de.ecertby.model.UserDepartment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDepartmentRepository extends BaseRepository<UserDepartment, QUserDepartment> {
    @Query("SELECT CASE WHEN COUNT(ud) > 0 THEN true ELSE false END " +
            "FROM UserDepartment ud " +
            "WHERE ud.department.id = :id")
    boolean existsUserDepartmentById(@Param("id") String id);
}
