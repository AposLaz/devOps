package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CertificateDocumentsDTO {
   private MultipartFile certificateDoc;
   private List<MultipartFile> preCertificateDocs;
   private List<MultipartFile> additionalDocs;
   private Map<String, MultipartFile> additionalDocMap;
   private Map<String, MultipartFile> preCertificateDocMap;
   private List<MultipartFile> supplementaryCertificateDocs;
   private List<MultipartFile> externalPreCertificateDocs;
   private Map<String, MultipartFile> supplementaryCertificateDocMap;
   private Map<String, MultipartFile> externalPreCertificateDocMap;
}
