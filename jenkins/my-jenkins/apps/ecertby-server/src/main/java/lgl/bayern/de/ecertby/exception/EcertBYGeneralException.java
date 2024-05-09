package lgl.bayern.de.ecertby.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EcertBYGeneralException extends Exception {
    private final transient List<EcertBYErrorException> errors;

}
