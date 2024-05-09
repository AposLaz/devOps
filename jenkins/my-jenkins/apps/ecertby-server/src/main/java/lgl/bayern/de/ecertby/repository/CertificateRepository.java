package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.*;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends BaseRepository<Certificate, QCertificate> {

    @Query("SELECT c.keywordSet " +
            "FROM Certificate c " +
            "WHERE c.id = :certificateId")
    List<CertificateKeyword> findCertificateKeywordsById(@Param("certificateId") String certificateId);

    @Query("SELECT c.signingEmployee " +
            "FROM Certificate c " +
            "WHERE c.id = :certificateId")
    UserDetail findCertificateSigningEmployeeById(@Param("certificateId") String certificateId);

    @Query("SELECT c "+
            "FROM Certificate c " +
            "WHERE c.status = :status and  c.forwardAuthority.id=:forwardedAuth and c.completedForward = true " )
    List<Certificate> findCertificateByStatusAndForwardedAuthority(@Param("status") CertificateStatus status,@Param("forwardedAuth") String forwardedAuth);

    @Query("SELECT c " +
            "FROM Certificate c " +
            "WHERE c.company.id = :companyId " +
            "AND c.status IN :statusList")
    List<Certificate> findCertificateByCompanyAndStatus(
            @Param("companyId") String companyId,
            @Param("statusList") List<CertificateStatus> statusList
    );

    @Query("SELECT c " +
            "FROM Certificate c " +
            "WHERE c.company.id = :companyId ")
    List<Certificate> findCertificateByCompany(
            @Param("companyId") String companyId
    );

    @Query("SELECT c.id "+
            "FROM Certificate c " +
            "WHERE c.parentCertificate.id=:parentCertificateId" )
    List<String> findIdByParentCertificateId(@Param("parentCertificateId")String parentCertificateId);

    boolean existsByParentCertificateIdAndStatus(@Param("parentCertificateId")String parentCertificateId, @Param("status")CertificateStatus certificateStatus);

    List<Certificate> findByParentCertificateId(String parentCertificateId);
    @Query("SELECT c.parentCertificate.id "+
            "FROM Certificate c " +
            "WHERE c.id=:id" )
    String findParentCertificateIdById(String id);

    @Query("SELECT CASE WHEN COUNT(cd) > 0 THEN true ELSE false END " +
            "FROM CertificateDepartment cd " +
            "WHERE cd.department.id = :id")
    boolean existsCertificateDepartmentById(@Param("id") String id);

    @Modifying
    @Query("UPDATE Certificate " +
            "SET referenceCertificate = null " +
            "WHERE referenceCertificate.id = :deletedCertificateId")
    void updateReferenceCertificate(@Param("deletedCertificateId")String deletedCertificateId);

    boolean existsByForwardAuthorityIdAndStatusIn(String authorityId, List<CertificateStatus> statusList);

    @Query ( "SELECT cert.id from Certificate cert " +
            "WHERE cert.id = :certificateId or cert.parentCertificate.id =  :certificateId")
    List<String> findMainAndPrecertificateIds(@Param("certificateId")String certificateId);

    @Query("SELECT CASE WHEN COUNT(ck) > 0 THEN true ELSE false END " +
            "FROM CertificateKeyword ck")
    boolean certificateKeywordsExist();

    @Query("SELECT CASE WHEN COUNT(ck) > 0 THEN true ELSE false END " +
            "FROM CertificateKeyword ck " +
            "WHERE ck.keyword.id = :id")
    boolean existsCertificateKeywordById(@Param("id") String id);

    @Override
    default void customize(QuerydslBindings bindings, QCertificate certificate) {
        // Call the common customization first
        BaseRepository.super.customize(bindings, certificate);
        bindings.bind(certificate.keywordSet.any().keyword.id).first((path, value) -> {
            List<String> keywordCatalogueIdList = List.of(value.split(","));
            if(keywordCatalogueIdList.size() == 1){
                return path.eq(keywordCatalogueIdList.get(0));
            } else {
                return path.in(keywordCatalogueIdList);
            }
        });
        bindings.bind(certificate.preAuthoritySet.any().preAuthority.id).first((path, value) -> {
            List<String> preAuthorityIdList = List.of(value.split(","));
            if(preAuthorityIdList.size() == 1){
                return path.eq(preAuthorityIdList.get(0));
            } else {
                return path.in(preAuthorityIdList);
            }
        });
    }
}
