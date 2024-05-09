package lgl.bayern.de.ecertby.model.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CertificateStatus {
    DRAFT,
    FORWARDED,
    FORWARDED_PRE_CERTIFICATE_REJECTED,
    RELEASED,
    REJECTED_CERTIFICATE,
    REVOKED,
    LOST,
    BLOCKED,
    DELETED,
    PRE_CERTIFICATE_DRAFT,
    PRE_CERTIFICATE_FORWARDED,
    PRE_CERTIFICATE_REJECTED,
    PRE_CERTIFICATE_EXCLUDED,
    PRE_CERTIFICATE_VOTE_POSITIVE,
    PRE_CERTIFICATE_DELETED;

    public static List<String> getEnumValues() {
        return Stream.of(CertificateStatus.values()).map(Enum::name).toList();
    }
    public static Set getExcludedStatusesForAdmin(){
        return Arrays.stream(new CertificateStatus[]
                        {CertificateStatus.DELETED,
                                CertificateStatus.DRAFT,
                                CertificateStatus.PRE_CERTIFICATE_DELETED,
                                CertificateStatus.PRE_CERTIFICATE_DRAFT,
                                CertificateStatus.PRE_CERTIFICATE_FORWARDED,
                                CertificateStatus.PRE_CERTIFICATE_REJECTED,
                                CertificateStatus.PRE_CERTIFICATE_EXCLUDED,
                                CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE})
                .collect(Collectors.toSet());
    }

    public static Set getExcludedStatuses(){
        return Arrays.stream(new CertificateStatus[]
                        {CertificateStatus.DELETED,
                         CertificateStatus.PRE_CERTIFICATE_DELETED,
                         CertificateStatus.PRE_CERTIFICATE_DRAFT,
                         CertificateStatus.PRE_CERTIFICATE_FORWARDED,
                         CertificateStatus.PRE_CERTIFICATE_REJECTED,
                         CertificateStatus.PRE_CERTIFICATE_EXCLUDED,
                         CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE})
                .collect(Collectors.toSet());
    }

    public static Set getIssuingAuthorityExcludedStatuses(){
        return Arrays.stream(new CertificateStatus[]
                        {CertificateStatus.DRAFT,
                         CertificateStatus.PRE_CERTIFICATE_DRAFT,
                         CertificateStatus.DELETED,
                         CertificateStatus.PRE_CERTIFICATE_DELETED})
                .collect(Collectors.toSet());
    }

    public static Set getPreCertificateStatuses(){
        return Arrays.stream(new CertificateStatus[]
                        {CertificateStatus.PRE_CERTIFICATE_DRAFT,
                                CertificateStatus.PRE_CERTIFICATE_FORWARDED,
                                CertificateStatus.PRE_CERTIFICATE_REJECTED,
                                CertificateStatus.PRE_CERTIFICATE_EXCLUDED,
                                CertificateStatus.PRE_CERTIFICATE_VOTE_POSITIVE,
                                CertificateStatus.PRE_CERTIFICATE_DELETED})
                .collect(Collectors.toSet());
    }
}
