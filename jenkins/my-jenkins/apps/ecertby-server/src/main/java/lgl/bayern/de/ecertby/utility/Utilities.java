package lgl.bayern.de.ecertby.utility;

import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;

import java.util.ArrayList;
import java.util.List;

public class Utilities {

    public static EcertBYGeneralException createErrorObject(String message, String field, String object, List<String> params){
        List<EcertBYErrorException> list = new ArrayList<>();
        list.add(new EcertBYErrorException(message, message, field, object, params, true));
       return new EcertBYGeneralException(list);
    }
}
