package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PDFElementTypeEnum {

    /**
     * The textfield.
     */
    TEXT_FIELD,

    /**
     * The checkbox.
     */
    CHECKBOX,

    /**
     * The radiogroup.
     */
    RADIO_GROUP,

    /**
     * The signature.
     */
    SIGNATURE,

    /**
     * The radiobutton.
     */
    RADIO_BUTTON;

    /**
     * Gets all the values of enum to a list of String
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(PDFElementTypeEnum.values()).map(Enum::name).collect(Collectors.toList());
    }

}
