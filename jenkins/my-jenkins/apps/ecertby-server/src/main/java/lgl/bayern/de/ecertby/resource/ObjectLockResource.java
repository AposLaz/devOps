package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;
import lgl.bayern.de.ecertby.model.UserDetail;
import lgl.bayern.de.ecertby.service.ObjectLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("objectlock")
@RequiredArgsConstructor
@Transactional
public class ObjectLockResource {
    private final ObjectLockService objectLockService;

    @DeleteMapping(path = "delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not delete lock")
    public void deleteLock(@PathVariable String id) {
        objectLockService.unlock(id);
    }


    @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not get Lock")
    public boolean checkForLock(@PathVariable String id ,  @RequestParam(name = "type") String type,
        @RequestParam(name = "shouldLock", defaultValue = "false") boolean shouldLock) {
       return objectLockService.checkAndLockIfNeeded(id,type, shouldLock);
    }

    @GetMapping(path = "override/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not override Lock")
    public boolean overrideLock(@PathVariable String id ,  @RequestParam(name = "type") String type) {
       return objectLockService.overrideLock(id,type);
    }

}
