package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.cm.dto.VersionDTO;
import lgl.bayern.de.ecertby.annotation.AuditIdentifier;
import lgl.bayern.de.ecertby.annotation.AuditIgnore;
import lgl.bayern.de.ecertby.annotation.AuditTranslationKey;
import lgl.bayern.de.ecertby.dto.audit.ComparableDTO;
import lgl.bayern.de.ecertby.model.util.CertificateStatus;
import lgl.bayern.de.ecertby.model.util.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class DocumentDTO extends VersionDTO implements ComparableDTO {
    @Getter
    @AuditIdentifier
    private String name;
    private String notes;
    @AuditTranslationKey(key = "name")
    private String editedFilename;
    private String certificateId;
    private String preCertificateId;
    @AuditIgnore
    private FileType type;

    private OptionDTO authority;

    private CertificateStatus status;
    private String rejectionReason;

    private String preCertificateActionByFirstName;

    private String preCertificateActionByLastName;

    private Instant preCertificateActionOn;

    @Override
    public void setName(String name) {
        super.setName(name);
    }
}
