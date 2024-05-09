package lgl.bayern.de.ecertby.model.util;

public enum FileUploadType {

    TEMPLATE,
    CERTIFICATE_DOCUMENT,
    CERTIFICATE_PRE_CERTIFICATE_DOCS,
    CERTIFICATE_ADDITIONAL_DOCS,
    CERTIFICATE_SUPPLEMENTARY_DOCS,
    CERTIFICATE_EXTERNAL_PRE_CERTIFICATE_DOCS;

    public String getValue() {
        switch (this) {
            case TEMPLATE:
                return "template";
            case CERTIFICATE_DOCUMENT:
                return "certificate_document";
            case CERTIFICATE_PRE_CERTIFICATE_DOCS:
                return "certificate_pre_certificate_documents";
            case CERTIFICATE_ADDITIONAL_DOCS:
                return "certificate_additional_documents";
            case CERTIFICATE_SUPPLEMENTARY_DOCS:
                return "certificate_supplementary_documents";
            case CERTIFICATE_EXTERNAL_PRE_CERTIFICATE_DOCS:
                return "certificate_external_pre_certificate_documents";
            default:
                throw new IllegalArgumentException("Unknown FileUpload type");
        }
    }
}
