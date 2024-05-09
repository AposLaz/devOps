package lgl.bayern.de.ecertby.repository;

import com.eurodyn.qlack.fuse.aaa.model.UserHasOperation;
import com.eurodyn.qlack.fuse.aaa.repository.AAARepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHasOperationCustomRepository extends AAARepository<UserHasOperation, String>
{
    List<UserHasOperation> findByUserId(String aaaUserId);

}
