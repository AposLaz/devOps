package lgl.bayern.de.ecertby.config;

import com.eurodyn.qlack.common.exception.QException;
import com.eurodyn.qlack.util.data.filter.JSONFilter;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionControllerAdvisor {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity handleValidationException(MethodArgumentNotValidException exception) {
    return new ResponseEntity<>(JSONFilter
        .filterDefault(exception.getBindingResult().getAllErrors(),
            "defaultMessage,objectName,field,rejectedValue,code"),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(QException.class)
  public ResponseEntity handleException(QException exception) {
    log.warn("Validation message: "+ exception.getMessage());
    EcertBYGeneralException errors = (EcertBYGeneralException) exception.getCause();
    return new ResponseEntity<>(errors.getErrors(),
            HttpStatus.BAD_REQUEST);
  }

}
