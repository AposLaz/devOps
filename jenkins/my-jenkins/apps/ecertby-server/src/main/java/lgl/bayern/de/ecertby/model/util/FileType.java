package lgl.bayern.de.ecertby.model.util;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FileType {

    // Under template folder contains only the pdf template
    TEMPLATE,

    // Under certificate folder contains the certificate pdf but may also include a PRE_CERTIFICATE folder as also ADDITIONAL_DOCUMENT folder
    CERTIFICATE,

    // Under pre_certificate folder are all the precertificate pdfs of a specific certificate. A PRE_CERTIFICATE may also include an ADDITIONAL_DOCUMENT folder
    PRE_CERTIFICATE,

    // Under additional_document are additional documents of a certificate or a precertificate
    ADDITIONAL_DOCUMENT,

    SUPPLEMENTARY_CERTIFICATE,

    EXTERNAL_PRE_CERTIFICATE;



    /**
     * Gets all the values of enum to a list of String
     *
     * @return All the values of enum
     */
    public static List<String> getEnumValues() {
        return Stream.of(FileType.values()).map(Enum::name).collect(Collectors.toList());
    }
}



