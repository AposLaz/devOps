package lgl.bayern.de.ecertby.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EcertBYErrorException {

    private String code;

    private String defaultMessage;
    private String field;

    private String objectName;

    private List<String> params;

    private boolean applicationError;


}
