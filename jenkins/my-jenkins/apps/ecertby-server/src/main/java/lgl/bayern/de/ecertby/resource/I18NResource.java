package lgl.bayern.de.ecertby.resource;

import com.eurodyn.qlack.common.exception.QExceptionWrapper;
import com.eurodyn.qlack.util.data.exceptions.ExceptionWrapper;

import java.util.Map;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("i18n")
public class I18NResource {

  @Resource(name = "messages")
  private Map<String, String> messages;

  @GetMapping(path = "{locale}")
  @ExceptionWrapper(wrapper = QExceptionWrapper.class, logMessage = "Could not obtain translations.")
  public Map<String, String> process(@PathVariable String locale) {
  //  MessageConfig.getValue("home"); //To translate one message
    return messages;
  }
}
