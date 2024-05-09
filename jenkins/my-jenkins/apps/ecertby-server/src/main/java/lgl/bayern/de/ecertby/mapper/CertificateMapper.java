package lgl.bayern.de.ecertby.mapper;

import lgl.bayern.de.ecertby.dto.CertificateAssignmentHistoryDTO;
import lgl.bayern.de.ecertby.dto.CertificateDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;


@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class CertificateMapper extends BaseMapper<CertificateDTO, Certificate, QCertificate> {
    @Mapping(source="optionDTO.id", target = "department.id")
    @Mapping(target = "id", ignore = true)
    public abstract CertificateDepartment optionDTOToCertificateDepartment(OptionDTO optionDTO);

    @Mapping(source="department.id", target = "id")
    @Mapping(source="department.data", target = "name")
    public abstract OptionDTO certificateDepartmentToOptionDTO(CertificateDepartment department);

    @Mapping(source="optionDTO.id", target = "keyword.id")
    @Mapping(target = "id", ignore = true)
    public abstract CertificateKeyword optionDTOToCertificateKeyword(OptionDTO optionDTO);

    @Mapping(source="keyword.id", target = "id")
    @Mapping(source="keyword.data", target = "name")
    public abstract OptionDTO certificateKeywordToOptionDTO(CertificateKeyword keyword);

    @Mapping(source="optionDTO.id", target = "preAuthority.id")
    @Mapping(target = "id", ignore = true)
    public abstract CertificatePreAuthority optionDTOToCertificatePreAuthority(OptionDTO optionDTO);

    @Mapping(source="preAuthority.id", target = "id")
    @Mapping(source="preAuthority.name", target = "name")
    public abstract OptionDTO certificatePreAuthorityToOptionDTO(CertificatePreAuthority preAuthority);

    @Mapping(source="optionDTO.id", target = "team.id")
    @Mapping(target = "id", ignore = true)
    public abstract CertificateTeam optionDTOToCertificateTeam(OptionDTO optionDTO);

    @Mapping(source="team.id", target = "id")
    @Mapping(source="team.name", target = "name")
    public abstract OptionDTO certificateTeamToOptionDTO(CertificateTeam team);

    @Mapping(source="catalogValue.id", target = "id")
    @Mapping(source="catalogValue.data", target = "name")
    public abstract OptionDTO catalogValueToOptionDTO(CatalogValue catalogValue);

    @Mapping(source="optionDTO.id", target = "id")
    @Mapping(source="optionDTO.name", target = "data")
    public abstract CatalogValue optionDTOToCatalogValue(OptionDTO optionDTO);


    @Mapping(source="team.id", target = "id")
    @Mapping(source="team.name", target = "name")
    public abstract OptionDTO certificateAssignmentHistoryTeamToOptionDTO(CertificateAssignmentHistoryTeam team);
}
