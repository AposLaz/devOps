package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.*;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CertificateStatusHistoryMapper extends BaseMapper<CertificateStatusHistoryDTO, CertificateStatusHistory, QCertificateStatusHistory> {


}
