package lgl.bayern.de.ecertby.repository;

import com.eurodyn.qlack.common.repository.QlackBaseRepository;
import com.eurodyn.qlack.fuse.fd.model.ThreadMessage;
import com.eurodyn.qlack.fuse.fd.model.Vote;
import com.eurodyn.qlack.fuse.fd.util.Reaction;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
public interface FeatureBoardRepository extends QlackBaseRepository<ThreadMessage, String> {

    @Modifying
    @Query(value = "UPDATE fd_thread_message " +
            "SET first_name = :firstName, last_name = :lastName ,email = :email , author = null " +
            "WHERE author = :authorId", nativeQuery = true)
    int updateFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName,@Param("email") String email, @Param("authorId") String authorId);

    @Query(value = "SELECT first_name FROM fd_thread_message " +
            "WHERE id = :id", nativeQuery = true)
    String findFirstNamebyId(@Param("id") String id);

    @Query(value = "SELECT last_name FROM fd_thread_message " +
            "WHERE id = :id", nativeQuery = true)
    String findLastNamebyId(@Param("id") String id);

    @Query("SELECT COUNT(v) FROM Vote v " +
            "WHERE v.threadMessage.id = :threadId " +
            "AND v.voterId = :userId " +
            "AND v.reaction = :reaction")
    long findVotesByStatusAndUser(String threadId , String userId , Reaction reaction);
}
