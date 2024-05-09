package lgl.bayern.de.ecertby.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TemplateDocumentsDTO {

   private MultipartFile templateDoc;

   //   private List<MultipartFile> additionalDocs;

//   private Map<String, MultipartFile> additionalDocMap;

}
