package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CertificateAssignmentHistoryDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.CertificateAssignmentHistory;
import lgl.bayern.de.ecertby.model.CertificateAssignmentHistoryTeam;
import lgl.bayern.de.ecertby.model.CertificateTeam;
import lgl.bayern.de.ecertby.model.QCertificateAssignmentHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;


@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CertificateAssignmentHistoryMapper extends BaseMapper<CertificateAssignmentHistoryDTO, CertificateAssignmentHistory, QCertificateAssignmentHistory> {
    @Mapping(source="optionDTO.id", target = "team.id")
    @Mapping(target = "id", ignore = true)
    public abstract CertificateAssignmentHistoryTeam optionDTOToCertificateAssignmentHistoryTeam(OptionDTO optionDTO);

    @Mapping(source="team.id", target = "id")
    @Mapping(source="team.name", target = "name")
    public abstract OptionDTO certificateAssignmentHistoryTeamToOptionDTO(CertificateAssignmentHistoryTeam team);

}
